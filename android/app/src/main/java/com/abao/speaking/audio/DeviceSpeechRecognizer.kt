package com.abao.speaking.audio

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer

/** 未配置阿里云 ASR 时，使用系统 SpeechRecognizer 将语音转成英文文本。 */
class DeviceSpeechRecognizer(context: Context) {
    private val appContext = context.applicationContext
    private var speechRecognizer: SpeechRecognizer? = null
    private var listening = false

    private var onPartial: ((String) -> Unit)? = null
    private var onFinal: ((String) -> Unit)? = null
    private var onError: ((String) -> Unit)? = null

    fun isAvailable(): Boolean = SpeechRecognizer.isRecognitionAvailable(appContext)

    fun start(
        onPartial: (String) -> Unit,
        onFinal: (String) -> Unit,
        onError: (String) -> Unit
    ) {
        if (!isAvailable()) {
            onError("本设备不支持语音识别，请直接输入英文。")
            return
        }
        this.onPartial = onPartial
        this.onFinal = onFinal
        this.onError = onError
        release()
        speechRecognizer = SpeechRecognizer.createSpeechRecognizer(appContext).also { recognizer ->
            recognizer.setRecognitionListener(recognitionListener)
        }
        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, "en-US")
            putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
            putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 1)
        }
        listening = true
        speechRecognizer?.startListening(intent)
    }

    fun stopListening() {
        if (!listening) return
        speechRecognizer?.stopListening()
    }

    fun cancel() {
        listening = false
        speechRecognizer?.cancel()
    }

    fun release() {
        listening = false
        speechRecognizer?.destroy()
        speechRecognizer = null
        onPartial = null
        onFinal = null
        onError = null
    }

    private val recognitionListener = object : RecognitionListener {
        override fun onReadyForSpeech(params: Bundle?) = Unit
        override fun onBeginningOfSpeech() = Unit
        override fun onRmsChanged(rmsdB: Float) = Unit
        override fun onBufferReceived(buffer: ByteArray?) = Unit
        override fun onEndOfSpeech() = Unit

        override fun onError(error: Int) {
            listening = false
            onError?.invoke(mapError(error))
        }

        override fun onResults(results: Bundle?) {
            listening = false
            val text = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)?.firstOrNull().orEmpty()
            onFinal?.invoke(text)
        }

        override fun onPartialResults(partialResults: Bundle?) {
            val text = partialResults?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)?.firstOrNull()
            if (!text.isNullOrBlank()) {
                onPartial?.invoke(text)
            }
        }

        override fun onEvent(eventType: Int, params: Bundle?) = Unit
    }

    private fun mapError(error: Int): String = when (error) {
        SpeechRecognizer.ERROR_AUDIO -> "录音出错，请重试。"
        SpeechRecognizer.ERROR_CLIENT -> "语音识别客户端异常。"
        SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS -> "需要麦克风权限才能录入。"
        SpeechRecognizer.ERROR_NETWORK -> "需要网络才能识别，请检查网络或配置阿里云 ASR。"
        SpeechRecognizer.ERROR_NETWORK_TIMEOUT -> "识别超时，请重试。"
        SpeechRecognizer.ERROR_NO_MATCH -> "未识别到内容，请重试或直接输入英文。"
        SpeechRecognizer.ERROR_RECOGNIZER_BUSY -> "识别服务忙，请稍后再试。"
        SpeechRecognizer.ERROR_SERVER -> "识别服务异常，请重试。"
        SpeechRecognizer.ERROR_SPEECH_TIMEOUT -> "未检测到语音，请重试。"
        else -> "语音识别失败，请重试或直接输入英文。"
    }
}
