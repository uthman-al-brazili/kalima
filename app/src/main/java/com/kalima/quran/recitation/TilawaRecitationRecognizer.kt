package com.kalima.quran.recitation

import ai.onnxruntime.OnnxTensor
import ai.onnxruntime.OrtEnvironment
import ai.onnxruntime.OrtSession
import android.annotation.SuppressLint
import android.content.Context
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import android.os.Handler
import android.os.Looper
import android.util.Log
import java.io.FileInputStream
import java.nio.channels.FileChannel
import java.nio.FloatBuffer
import java.nio.LongBuffer
import java.util.concurrent.Executors
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicInteger
import org.json.JSONObject

enum class RecitationRecognizerState {
    Loading,
    Listening,
    Processing,
}

/** Runs Tilawa's FastConformer CTC model entirely on the device. */
class TilawaRecitationRecognizer(context: Context) {
    private val applicationContext = context.applicationContext
    private val mainHandler = Handler(Looper.getMainLooper())
    private val captureExecutor = Executors.newSingleThreadExecutor()
    private val inferenceExecutor = Executors.newSingleThreadExecutor()
    private val generation = AtomicInteger()
    private val recording = AtomicBoolean(false)
    private val previewInferenceQueued = AtomicBoolean(false)
    private var audioRecord: AudioRecord? = null
    private var ortSession: OrtSession? = null
    private var decoder: TilawaCtcDecoder? = null
    private var onState: (RecitationRecognizerState) -> Unit = {}
    private var onTranscript: (String, Boolean) -> Unit = { _, _ -> }
    private var onError: (Throwable) -> Unit = {}

    fun start(
        onState: (RecitationRecognizerState) -> Unit,
        onTranscript: (String, final: Boolean) -> Unit,
        onError: (Throwable) -> Unit,
    ) {
        stop()
        this.onState = onState
        this.onTranscript = onTranscript
        this.onError = onError
        val activeGeneration = generation.incrementAndGet()
        previewInferenceQueued.set(false)
        recording.set(true)
        postState(RecitationRecognizerState.Loading, activeGeneration)
        inferenceExecutor.execute {
            try {
                ensureSession()
                if (isActive(activeGeneration)) {
                    captureExecutor.execute { capture(activeGeneration) }
                }
            } catch (error: Throwable) {
                recording.set(false)
                postError(error, activeGeneration)
            }
        }
    }

    fun stop() {
        if (!recording.getAndSet(false)) return
        runCatching { audioRecord?.stop() }
        postState(RecitationRecognizerState.Processing, generation.get())
    }

    fun destroy() {
        stop()
        generation.incrementAndGet()
        captureExecutor.shutdownNow()
        inferenceExecutor.shutdownNow()
        ortSession?.close()
        ortSession = null
    }

    @SuppressLint("MissingPermission")
    private fun capture(activeGeneration: Int) {
        val minimumBuffer = AudioRecord.getMinBufferSize(
            SAMPLE_RATE,
            AudioFormat.CHANNEL_IN_MONO,
            AudioFormat.ENCODING_PCM_16BIT,
        )
        if (minimumBuffer <= 0) {
            recording.set(false)
            postError(IllegalStateException("No compatible microphone input"), activeGeneration)
            return
        }
        val recorder = try {
            AudioRecord.Builder()
                .setAudioSource(MediaRecorder.AudioSource.VOICE_RECOGNITION)
                .setAudioFormat(
                    AudioFormat.Builder()
                        .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                        .setSampleRate(SAMPLE_RATE)
                        .setChannelMask(AudioFormat.CHANNEL_IN_MONO)
                        .build(),
                )
                .setBufferSizeInBytes(
                    maxOf(minimumBuffer, CAPTURE_BUFFER_SAMPLES * Short.SIZE_BYTES),
                )
                .build()
        } catch (error: Throwable) {
            recording.set(false)
            postError(error, activeGeneration)
            return
        }
        if (recorder.state != AudioRecord.STATE_INITIALIZED) {
            recorder.release()
            recording.set(false)
            postError(IllegalStateException("Microphone initialization failed"), activeGeneration)
            return
        }

        audioRecord = recorder
        val samples = ArrayList<Float>(SAMPLE_RATE * 10)
        val buffer = ShortArray(CAPTURE_BUFFER_SAMPLES)
        var nextPreviewAt = PREVIEW_INTERVAL_SAMPLES
        try {
            recorder.startRecording()
            postState(RecitationRecognizerState.Listening, activeGeneration)
            while (isActive(activeGeneration) && samples.size < MAX_RECORDING_SAMPLES) {
                val count = recorder.read(buffer, 0, buffer.size, AudioRecord.READ_BLOCKING)
                if (count <= 0) {
                    if (!recording.get()) break
                    continue
                }
                repeat(count) { index -> samples += buffer[index] / PCM_SCALE }
                if (samples.size >= nextPreviewAt) {
                    if (previewInferenceQueued.compareAndSet(false, true)) {
                        submitInference(samples.toFloatArray(), final = false, activeGeneration)
                    }
                    nextPreviewAt += PREVIEW_INTERVAL_SAMPLES
                }
            }
        } catch (error: Throwable) {
            if (isActive(activeGeneration)) postError(error, activeGeneration)
        } finally {
            runCatching { recorder.stop() }
            recorder.release()
            if (audioRecord === recorder) audioRecord = null
            recording.set(false)
        }

        if (!isGenerationCurrent(activeGeneration)) return
        postState(RecitationRecognizerState.Processing, activeGeneration)
        if (samples.isEmpty()) {
            postTranscript("", final = true, activeGeneration)
        } else {
            submitInference(samples.toFloatArray(), final = true, activeGeneration)
        }
    }

    private fun submitInference(audio: FloatArray, final: Boolean, activeGeneration: Int) {
        inferenceExecutor.execute {
            try {
                val transcript = runInference(audio)
                postTranscript(transcript, final, activeGeneration)
            } catch (error: Throwable) {
                if (final) postError(error, activeGeneration)
            } finally {
                if (!final) previewInferenceQueued.set(false)
            }
        }
    }

    private fun ensureSession() {
        if (ortSession != null && decoder != null) return
        val environment = OrtEnvironment.getEnvironment()
        OrtSession.SessionOptions().use { options ->
            applicationContext.assets.openFd(MODEL_ASSET).use { asset ->
                FileInputStream(asset.fileDescriptor).channel.use { channel ->
                    val modelBuffer = channel.map(
                        FileChannel.MapMode.READ_ONLY,
                        asset.startOffset,
                        asset.declaredLength,
                    )
                    ortSession = environment.createSession(modelBuffer, options)
                }
            }
        }
        val vocabText = applicationContext.assets.open(VOCAB_ASSET)
            .bufferedReader(Charsets.UTF_8)
            .use { it.readText() }
        val vocabJson = JSONObject(vocabText)
        decoder = TilawaCtcDecoder(
            vocab = buildMap {
                vocabJson.keys().forEach { key -> put(key.toInt(), vocabJson.getString(key)) }
            },
            blankTokenId = BLANK_TOKEN_ID,
        )
    }

    private fun runInference(audio: FloatArray): String {
        val environment = OrtEnvironment.getEnvironment()
        val session = checkNotNull(ortSession) { "Tilawa session is not initialized" }
        OnnxTensor.createTensor(
            environment,
            FloatBuffer.wrap(audio),
            longArrayOf(1, audio.size.toLong()),
        ).use { audioTensor ->
            OnnxTensor.createTensor(
                environment,
                LongBuffer.wrap(longArrayOf(audio.size.toLong())),
                longArrayOf(1),
            ).use { lengthTensor ->
                session.run(
                    mapOf(
                        AUDIO_INPUT_NAME to audioTensor,
                        LENGTH_INPUT_NAME to lengthTensor,
                    ),
                ).use { results ->
                    val output = results[0] as OnnxTensor
                    val shape = output.info.shape
                    check(shape.size == 3 && shape[0] == 1L) {
                        "Unexpected Tilawa output shape: ${shape.contentToString()}"
                    }
                    val values = output.floatBuffer
                    return checkNotNull(decoder).decode(
                        values = values,
                        timeSteps = shape[1].toInt(),
                        vocabSize = shape[2].toInt(),
                    )
                }
            }
        }
    }

    private fun isActive(activeGeneration: Int): Boolean =
        recording.get() && isGenerationCurrent(activeGeneration)

    private fun isGenerationCurrent(activeGeneration: Int): Boolean =
        generation.get() == activeGeneration

    private fun postState(state: RecitationRecognizerState, activeGeneration: Int) {
        mainHandler.post {
            if (isGenerationCurrent(activeGeneration)) onState(state)
        }
    }

    private fun postTranscript(transcript: String, final: Boolean, activeGeneration: Int) {
        mainHandler.post {
            if (isGenerationCurrent(activeGeneration)) onTranscript(transcript, final)
        }
    }

    private fun postError(error: Throwable, activeGeneration: Int) {
        Log.e(LOG_TAG, "Offline recitation recognition failed", error)
        mainHandler.post {
            if (isGenerationCurrent(activeGeneration)) onError(error)
        }
    }

    private companion object {
        const val SAMPLE_RATE = 16_000
        const val CAPTURE_BUFFER_SAMPLES = 4_800
        const val PREVIEW_INTERVAL_SAMPLES = SAMPLE_RATE * 3
        const val MAX_RECORDING_SAMPLES = SAMPLE_RATE * 45
        const val PCM_SCALE = 32_768f
        const val BLANK_TOKEN_ID = 1024
        const val MODEL_ASSET = "tilawa_fastconformer_full_mixed.onnx"
        const val VOCAB_ASSET = "tilawa_vocab.json"
        const val AUDIO_INPUT_NAME = "audio_signal"
        const val LENGTH_INPUT_NAME = "length"
        const val LOG_TAG = "KalimaTilawa"
    }
}

internal class TilawaCtcDecoder(
    private val vocab: Map<Int, String>,
    private val blankTokenId: Int,
) {
    fun decode(values: FloatBuffer, timeSteps: Int, vocabSize: Int): String {
        val tokenIds = ArrayList<Int>()
        var previous = -1
        repeat(timeSteps) { timeStep ->
            val offset = timeStep * vocabSize
            var bestId = 0
            var bestValue = values.get(offset)
            for (tokenId in 1 until vocabSize) {
                val value = values.get(offset + tokenId)
                if (value > bestValue) {
                    bestId = tokenId
                    bestValue = value
                }
            }
            if (bestId != previous && bestId != blankTokenId) tokenIds += bestId
            previous = bestId
        }
        return tokenIds.asSequence()
            .mapNotNull(vocab::get)
            .filterNot { it == "<unk>" || it == "<blank>" }
            .joinToString("")
            .replace(WORD_PREFIX, " ")
            .trim()
    }

    private companion object {
        const val WORD_PREFIX = "▁"
    }
}
