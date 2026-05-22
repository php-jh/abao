package com.abao.speaking

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import java.io.ByteArrayOutputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder

class WavRecorder {
    private val sampleRate = 16000
    private val channel = AudioFormat.CHANNEL_IN_MONO
    private val format = AudioFormat.ENCODING_PCM_16BIT

    private var recorder: AudioRecord? = null
    private var worker: Thread? = null
    @Volatile
    private var recording = false
    private val pcm = ByteArrayOutputStream()

    fun start(context: Context) {
        if (context.checkSelfPermission(Manifest.permission.RECORD_AUDIO)
            != PackageManager.PERMISSION_GRANTED
        ) {
            throw IllegalStateException("麦克风权限未授权")
        }
        val minBuffer = AudioRecord.getMinBufferSize(sampleRate, channel, format)
        val bufferSize = maxOf(minBuffer, sampleRate)
        recorder = AudioRecord(
            MediaRecorder.AudioSource.MIC,
            sampleRate,
            channel,
            format,
            bufferSize
        )
        pcm.reset()
        recording = true
        recorder?.startRecording()
        worker = Thread({
            val buffer = ByteArray(bufferSize)
            while (recording) {
                val read = recorder?.read(buffer, 0, buffer.size) ?: 0
                if (read > 0) pcm.write(buffer, 0, read)
            }
        }, "wav-recorder").also { it.start() }
    }

    fun stop(): ByteArray {
        recording = false
        worker?.let {
            try {
                it.join(1200)
            } catch (_: InterruptedException) {
                Thread.currentThread().interrupt()
            }
        }
        recorder?.let {
            try {
                it.stop()
            } catch (_: IllegalStateException) {
            }
            it.release()
        }
        recorder = null
        return toWav(pcm.toByteArray())
    }

    private fun toWav(pcmBytes: ByteArray): ByteArray {
        val totalLen = 44 + pcmBytes.size
        val header = ByteBuffer.allocate(44).order(ByteOrder.LITTLE_ENDIAN)
        putAscii(header, "RIFF")
        header.putInt(totalLen - 8)
        putAscii(header, "WAVE")
        putAscii(header, "fmt ")
        header.putInt(16)
        header.putShort(1)
        header.putShort(1)
        header.putInt(sampleRate)
        header.putInt(sampleRate * 2)
        header.putShort(2)
        header.putShort(16)
        putAscii(header, "data")
        header.putInt(pcmBytes.size)
        return ByteArrayOutputStream(totalLen).apply {
            write(header.array())
            write(pcmBytes)
        }.toByteArray()
    }

    private fun putAscii(buffer: ByteBuffer, value: String) {
        value.forEach { buffer.put(it.code.toByte()) }
    }
}
