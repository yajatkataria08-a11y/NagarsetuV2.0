package com.nagarsetu.roadwatch.data.tflite

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Color
import android.util.Log
import org.tensorflow.lite.Interpreter
import java.io.FileInputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.channels.FileChannel
import javax.inject.Inject
import javax.inject.Singleton

data class PotholeResult(
    val detected: Boolean,
    val confidence: Float,          // 0.0 – 1.0
    val source: String = "model"    // "model" | "stub"
)

@Singleton
class PotholeDetector @Inject constructor(
    private val context: Context
) {
    companion object {
        private const val TAG = "PotholeDetector"
        private const val MODEL_FILE = "pothole_model.tflite"
        private const val INPUT_SIZE = 224
        private const val THRESHOLD = 0.50f
    }

    private var interpreter: Interpreter? = null

    init {
        interpreter = tryLoadModel()
    }

    // ── Public API ────────────────────────────────────────────────────────────

    fun analyze(bitmap: Bitmap): PotholeResult {
        return if (interpreter != null) {
            runModelInference(bitmap)
        } else {
            runStubInference(bitmap)
        }
    }

    fun isModelLoaded(): Boolean = interpreter != null

    /**
     * Compatibility bridge for RoadWatchRepository.
     * Maps the internal PotholeResult to the domain model PotholeDetection.
     */
    fun detect(bitmap: Bitmap): com.nagarsetu.roadwatch.domain.model.PotholeDetection {
        val result = analyze(bitmap)
        return com.nagarsetu.roadwatch.domain.model.PotholeDetection(
            confidence = result.confidence,
            depthEstimateMm = if (result.detected) result.confidence * 45f else 0f,
            criticality = when {
                !result.detected -> com.nagarsetu.roadwatch.domain.model.Severity.LOW
                result.confidence > 0.85f -> com.nagarsetu.roadwatch.domain.model.Severity.CRITICAL
                result.confidence > 0.70f -> com.nagarsetu.roadwatch.domain.model.Severity.HIGH
                result.confidence > 0.50f -> com.nagarsetu.roadwatch.domain.model.Severity.MEDIUM
                else -> com.nagarsetu.roadwatch.domain.model.Severity.LOW
            }
        )
    }

    // ── Model Loading ─────────────────────────────────────────────────────────

    private fun tryLoadModel(): Interpreter? {
        return try {
            val assetFileDescriptor = context.assets.openFd(MODEL_FILE)
            val inputStream = FileInputStream(assetFileDescriptor.fileDescriptor)
            val fileChannel = inputStream.channel
            val startOffset = assetFileDescriptor.startOffset
            val declaredLength = assetFileDescriptor.declaredLength
            val mappedBuffer = fileChannel.map(
                FileChannel.MapMode.READ_ONLY, startOffset, declaredLength
            )
            val options = Interpreter.Options().apply {
                numThreads = 2
                useNNAPI = false    // set true for Pixel devices with NNAPI support
            }
            Log.i(TAG, "TFLite model loaded successfully from assets/$MODEL_FILE")
            Interpreter(mappedBuffer, options)
        } catch (e: Exception) {
            Log.w(TAG, "TFLite model not found or failed to load ($MODEL_FILE): ${e.message}. Using stub inference.")
            null
        }
    }

    // ── Real Model Inference ──────────────────────────────────────────────────

    private fun runModelInference(bitmap: Bitmap): PotholeResult {
        return try {
            val inputBuffer = preprocessBitmap(bitmap)

            // Output shape: [1, 2] → [no_pothole_prob, pothole_prob]
            // Adjust if your model output shape differs (e.g., [1,1] sigmoid)
            val output = Array(1) { FloatArray(2) }

            interpreter!!.run(inputBuffer, output)

            val confidence = output[0][1]  // index 1 = pothole class probability
            Log.d(TAG, "Model inference: confidence=$confidence")

            PotholeResult(
                detected = confidence >= THRESHOLD,
                confidence = confidence,
                source = "model"
            )
        } catch (e: Exception) {
            Log.e(TAG, "Model inference failed: ${e.message}. Falling back to stub.")
            runStubInference(bitmap)
        }
    }

    // ── Stub Inference (when model is missing) ────────────────────────────────
    // Uses image darkness as a rough proxy for road damage visibility.
    // Dark patches in road images often correlate with potholes/shadows.

    private fun runStubInference(bitmap: Bitmap): PotholeResult {
        val resized = Bitmap.createScaledBitmap(bitmap, 64, 64, true)
        var darkPixelCount = 0
        val total = 64 * 64

        for (x in 0 until 64) {
            for (y in 0 until 64) {
                val px = resized.getPixel(x, y)
                val luminance = (0.299 * Color.red(px) +
                                 0.587 * Color.green(px) +
                                 0.114 * Color.blue(px))
                if (luminance < 85) darkPixelCount++
            }
        }

        val darkRatio = darkPixelCount.toFloat() / total
        // Heuristic: if >35% of image is dark → likely pothole/shadow
        val confidence = (darkRatio * 2f).coerceIn(0.1f, 0.9f)

        Log.d(TAG, "Stub inference: darkRatio=$darkRatio confidence=$confidence")

        return PotholeResult(
            detected = confidence >= THRESHOLD,
            confidence = confidence,
            source = "stub"
        )
    }

    // ── Preprocessing ─────────────────────────────────────────────────────────

    private fun preprocessBitmap(src: Bitmap): ByteBuffer {
        val resized = Bitmap.createScaledBitmap(src, INPUT_SIZE, INPUT_SIZE, true)

        // Float32 input: 1 × 224 × 224 × 3 channels
        val byteBuffer = ByteBuffer.allocateDirect(
            4 * INPUT_SIZE * INPUT_SIZE * 3
        ).apply { order(ByteOrder.nativeOrder()) }

        for (y in 0 until INPUT_SIZE) {
            for (x in 0 until INPUT_SIZE) {
                val px = resized.getPixel(x, y)
                // Normalize to [0, 1]
                byteBuffer.putFloat(Color.red(px) / 255f)
                byteBuffer.putFloat(Color.green(px) / 255f)
                byteBuffer.putFloat(Color.blue(px) / 255f)
            }
        }

        byteBuffer.rewind()
        return byteBuffer
    }
}
