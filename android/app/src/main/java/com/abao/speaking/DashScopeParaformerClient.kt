package com.abao.speaking

import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import okhttp3.WebSocket
import okhttp3.WebSocketListener
import okio.ByteString.Companion.toByteString
import org.json.JSONObject
import java.util.UUID
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

/**
 * 百炼 Paraformer 实时语音识别（WebSocket）。
 * 文档：https://help.aliyun.com/zh/model-studio/websocket-for-paraformer-real-time-service
 */
class DashScopeParaformerClient {
    private val wsUrl = "wss://dashscope.aliyuncs.com/api-ws/v1/inference"
    private val client = OkHttpClient.Builder()
        .readTimeout(90, TimeUnit.SECONDS)
        .connectTimeout(20, TimeUnit.SECONDS)
        .build()

    fun recognize(wavBytes: ByteArray): String {
        val apiKey = AliyunConfig.DASHSCOPE_API_KEY
        if (!AliyunConfig.isDashScopeConfigured()) {
            throw IllegalStateException("请先在 AliyunConfig.kt 填写 DASHSCOPE_API_KEY（sk- 开头）")
        }
        val pcm = extractPcm16Mono(wavBytes)
        if (pcm.isEmpty()) {
            throw IllegalStateException("录音数据为空，请重试")
        }

        val taskId = UUID.randomUUID().toString()
        val latch = CountDownLatch(1)
        var latestText = ""
        var finalText = ""
        var error: Exception? = null

        val request = Request.Builder()
            .url(wsUrl)
            .addHeader("Authorization", "Bearer $apiKey")
            .build()

        val webSocket = client.newWebSocket(request, object : WebSocketListener() {
            override fun onOpen(webSocket: WebSocket, response: Response) {
                webSocket.send(buildRunTask(taskId))
            }

            override fun onMessage(webSocket: WebSocket, text: String) {
                val json = JSONObject(text)
                val header = json.optJSONObject("header") ?: return
                when (header.optString("event")) {
                    "task-started" -> {
                        sendPcmStream(webSocket, pcm)
                        webSocket.send(buildFinishTask(taskId))
                    }
                    "result-generated" -> {
                        val sentence = json.optJSONObject("payload")
                            ?.optJSONObject("output")
                            ?.optJSONObject("sentence") ?: return
                        val part = sentence.optString("text", "")
                        if (part.isNotEmpty()) {
                            latestText = part
                            if (sentence.optBoolean("sentence_end")) {
                                finalText = part
                            }
                        }
                    }
                    "task-finished" -> {
                        if (finalText.isBlank()) finalText = latestText
                        latch.countDown()
                        webSocket.close(1000, "done")
                    }
                    "task-failed" -> {
                        val message = header.optString("error_message")
                            .ifBlank { json.optString("message", "百炼语音识别失败") }
                        error = IllegalStateException(message)
                        latch.countDown()
                    }
                }
            }

            override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
                error = IllegalStateException(t.message ?: "连接百炼语音识别失败")
                latch.countDown()
            }
        })

        if (!latch.await(90, TimeUnit.SECONDS)) {
            webSocket.close(1000, "timeout")
            throw IllegalStateException("百炼语音识别超时，请重试")
        }
        error?.let { throw it }
        return finalText.trim()
    }

    private fun buildRunTask(taskId: String): String = JSONObject()
        .put("header", JSONObject()
            .put("action", "run-task")
            .put("task_id", taskId)
            .put("streaming", "duplex"))
        .put("payload", JSONObject()
            .put("task_group", "audio")
            .put("task", "asr")
            .put("function", "recognition")
            .put("model", "paraformer-realtime-v2")
            .put("parameters", JSONObject()
                .put("format", "pcm")
                .put("sample_rate", 16000)
                .put("disfluency_removal_enabled", false)
                .put("language_hints", org.json.JSONArray().put("zh").put("en")))
            .put("input", JSONObject()))
        .toString()

    private fun buildFinishTask(taskId: String): String = JSONObject()
        .put("header", JSONObject()
            .put("action", "finish-task")
            .put("task_id", taskId)
            .put("streaming", "duplex"))
        .put("payload", JSONObject().put("input", JSONObject()))
        .toString()

    private fun sendPcmStream(webSocket: WebSocket, pcm: ByteArray) {
        val chunkSize = 3200 // 约 100ms @16kHz 16bit mono
        var offset = 0
        while (offset < pcm.size) {
            val end = minOf(offset + chunkSize, pcm.size)
            webSocket.send(pcm.toByteString(offset, end - offset))
            offset = end
            if (offset < pcm.size) {
                Thread.sleep(100)
            }
        }
    }

    private fun extractPcm16Mono(wavBytes: ByteArray): ByteArray {
        if (wavBytes.size < 44) return wavBytes
        val riff = String(wavBytes, 0, 4, Charsets.US_ASCII)
        val wave = String(wavBytes, 8, 4, Charsets.US_ASCII)
        return if (riff == "RIFF" && wave == "WAVE") {
            wavBytes.copyOfRange(44, wavBytes.size)
        } else {
            wavBytes
        }
    }
}
