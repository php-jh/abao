package com.abao.speaking.audio

import android.content.Context
import android.content.res.AssetFileDescriptor
import android.media.MediaPlayer
import android.os.Build
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import android.util.Log
import com.abao.speaking.logic.WebScript
import java.util.Locale
import java.util.concurrent.CopyOnWriteArrayList

/**
 * 对应 script.js 的 speak / speakWithBrowserVoice / playLocalAudio。
 *
 * - 有预录 wav（assets/audio/{slug}.wav）时优先播放，与网页 playLocalAudio 一致
 * - 否则使用 TTS（speakWithBrowserVoice），失败时再尝试 wav
 */
class SpeechPlayer(context: Context) : TextToSpeech.OnInitListener {
    private val appContext = context.applicationContext
    private var tts: TextToSpeech? = TextToSpeech(appContext, this)
    private var mediaPlayer: MediaPlayer? = null
    private var ttsReady = false
    private val pendingTexts = CopyOnWriteArrayList<String>()

    /** script.js: speak(text) */
    fun speak(text: String) {
        val trimmed = text.trim()
        if (trimmed.isEmpty()) return
        stop()
        if (playLocalAudio(trimmed)) return
        if (ttsReady) {
            speakWithBrowserVoice(trimmed)
        } else {
            pendingTexts.add(trimmed)
        }
    }

    fun stop() {
        runCatching { mediaPlayer?.stop() }
        mediaPlayer?.release()
        mediaPlayer = null
        tts?.stop()
    }

    fun shutdown() {
        stop()
        tts?.shutdown()
        tts = null
        ttsReady = false
        pendingTexts.clear()
    }

    override fun onInit(status: Int) {
        val engine = tts ?: return
        if (status != TextToSpeech.SUCCESS) {
            Log.w(TAG, "TTS init failed: $status")
            flushPendingWithAudioOrSkip()
            return
        }
        var langResult = engine.setLanguage(Locale.US)
        if (langResult == TextToSpeech.LANG_MISSING_DATA || langResult == TextToSpeech.LANG_NOT_SUPPORTED) {
            langResult = engine.setLanguage(Locale.ENGLISH)
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            val voice = engine.voices
                ?.filter { it.locale.language.startsWith("en") && !it.isNetworkConnectionRequired }
                ?.maxByOrNull { if (it.locale == Locale.US) 2 else 1 }
            if (voice != null) engine.voice = voice
        }
        engine.setSpeechRate(0.9f)
        engine.setPitch(1.0f)
        ttsReady = langResult != TextToSpeech.LANG_NOT_SUPPORTED
        flushPendingWithAudioOrSkip()
    }

    private fun flushPendingWithAudioOrSkip() {
        val queue = pendingTexts.toList()
        pendingTexts.clear()
        queue.forEach { text ->
            if (!playLocalAudio(text) && ttsReady) {
                speakWithBrowserVoice(text)
            }
        }
    }

    /** script.js: speakWithBrowserVoice(text) */
    private fun speakWithBrowserVoice(text: String) {
        val engine = tts ?: return
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            engine.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
                override fun onStart(utteranceId: String?) = Unit
                override fun onDone(utteranceId: String?) = Unit
                override fun onError(utteranceId: String?) {
                    playLocalAudio(text)
                }
                @Deprecated("Deprecated in Java")
                override fun onError(utteranceId: String?, errorCode: Int) {
                    playLocalAudio(text)
                }
            })
            engine.speak(text, TextToSpeech.QUEUE_FLUSH, null, utteranceId(text))
        } else {
            @Suppress("DEPRECATION")
            engine.speak(text, TextToSpeech.QUEUE_FLUSH, null)
        }
    }

    /** script.js: playLocalAudio(text) -> ./assets/audio/${audioSlug(text)}.wav */
    private fun playLocalAudio(text: String): Boolean {
        val path = "audio/${WebScript.audioSlug(text)}.wav"
        var afd: AssetFileDescriptor? = null
        return try {
            afd = appContext.assets.openFd(path)
            val descriptor = afd!!
            mediaPlayer = MediaPlayer().apply {
                setDataSource(descriptor.fileDescriptor, descriptor.startOffset, descriptor.length)
                setOnCompletionListener { mp ->
                    runCatching { descriptor.close() }
                    mp.release()
                    if (mediaPlayer === mp) mediaPlayer = null
                }
                setOnErrorListener { mp, _, _ ->
                    runCatching { descriptor.close() }
                    mp.release()
                    if (mediaPlayer === mp) mediaPlayer = null
                    false
                }
                prepare()
                start()
            }
            true
        } catch (_: Exception) {
            runCatching { afd?.close() }
            false
        }
    }

    private fun utteranceId(text: String) = "abao-${text.hashCode()}"

    companion object {
        private const val TAG = "SpeechPlayer"
    }
}
