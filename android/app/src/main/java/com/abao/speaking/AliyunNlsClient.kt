package com.abao.speaking

import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder
import java.nio.charset.StandardCharsets

class AliyunNlsClient {
    fun recognize(wavBytes: ByteArray): String {
        val token = AliyunConfig.NLS_TOKEN
        if (token.startsWith("填写")) {
            throw IllegalStateException("请先在 AliyunConfig.kt 填写 NLS_TOKEN")
        }
        val query = buildString {
            append("appkey=").append(encode(AliyunConfig.NLS_APP_KEY))
            append("&format=wav")
            append("&sample_rate=16000")
            append("&enable_punctuation_prediction=true")
            append("&enable_inverse_text_normalization=true")
        }
        val conn = (URL("${AliyunConfig.NLS_ASR_URL}?$query").openConnection() as HttpURLConnection).apply {
            requestMethod = "POST"
            connectTimeout = 15000
            readTimeout = 30000
            doOutput = true
            setRequestProperty("X-NLS-Token", token)
            setRequestProperty("Content-Type", "application/octet-stream")
        }
        conn.outputStream.use { it.write(wavBytes) }
        val code = conn.responseCode
        val stream = if (code in 200..299) conn.inputStream else conn.errorStream
        val body = stream.bufferedReader(StandardCharsets.UTF_8).use { it.readText() }
        val json = JSONObject(body)
        if (code != 200 || json.optInt("status") != 20000000) {
            throw IllegalStateException(json.optString("message", "阿里云语音识别失败"))
        }
        return json.optString("result", "")
    }

    private fun encode(value: String) = URLEncoder.encode(value, "UTF-8")
}
