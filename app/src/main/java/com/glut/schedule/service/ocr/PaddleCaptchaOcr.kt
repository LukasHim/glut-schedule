package com.glut.schedule.service.ocr

import ai.onnxruntime.OnnxTensor
import ai.onnxruntime.OrtEnvironment
import ai.onnxruntime.OrtSession
import ai.onnxruntime.TensorInfo
import android.content.Context
import android.graphics.Bitmap
import android.util.Log
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import java.nio.FloatBuffer
import kotlin.math.roundToInt

/** 验证码识别入口。实现必须可安全地从多个 ViewModel 并发调用。 */
interface CaptchaOcr {
    /** @return 识别出的文本；初始化失败、图片无效或结果不可信时返回 null。 */
    suspend fun recognize(bitmap: Bitmap): String?
}

/**
 * 基于 PaddleOCR PP-OCRv5 识别模型的验证码 OCR。
 *
 * 模型与字库随 APK 一起打包（`assets/paddleocr`），完全在本机推理：
 * 验证码图片不会上传到任何第三方 OCR 服务。ONNX Runtime 的 native 库只加载一次，
 * 模型在第一次真正需要识别时才初始化，避免影响冷启动。
 *
 * 只做「识别」不做「检测」：学校验证码都是一行四个字符，直接整图缩放到模型高度
 * 即可；省掉检测模型既减小包体，也少一次推理。
 */
class PaddleCaptchaOcr(context: Context) : CaptchaOcr {
    private val appContext = context.applicationContext
    private val mutex = Mutex()

    @Volatile
    private var engine: Engine? = null

    /** 初始化失败通常意味着 native 库与 ABI 不匹配或资源缺失，重试只会反复闪退。 */
    @Volatile
    private var initializationFailure: Throwable? = null

    override suspend fun recognize(bitmap: Bitmap): String? = withContext(Dispatchers.Default) {
        mutex.withLock {
            try {
                val engine = engine ?: ensureEngine() ?: return@withLock null
                engine.recognize(bitmap)
                    ?.takeIf { it.confidence >= MIN_CONFIDENCE }
                    ?.text
                    ?.trim()
                    ?.takeIf { it.isNotEmpty() }
            } catch (cancellation: CancellationException) {
                throw cancellation
            } catch (error: Throwable) {
                // 单张图片识别失败不应该让后续验证码也一起失效，因此不写 initializationFailure。
                Log.w(TAG, "验证码识别失败", error)
                null
            }
        }
    }

    private suspend fun ensureEngine(): Engine? {
        engine?.let { return it }
        initializationFailure?.let { return null }
        return withContext(Dispatchers.IO) {
            runCatching { Engine.load(appContext) }
                .onFailure { error ->
                    initializationFailure = error
                    Log.w(TAG, "PaddleOCR 模型初始化失败，验证码将只能手动输入", error)
                }
                .getOrNull()
        }?.also { engine = it }
    }

    private class Engine(
        private val environment: OrtEnvironment,
        private val session: OrtSession,
        private val inputName: String,
        private val labels: List<String>
    ) {
        fun recognize(bitmap: Bitmap): CaptchaRecognition? {
            val input = bitmap.toInputTensor(environment) ?: return null
            input.use {
                session.run(mapOf(inputName to input)).use { result ->
                    val output = result.get(0) as? OnnxTensor ?: return null
                    val shape = (output.info as? TensorInfo)?.shape ?: return null
                    if (shape.size != 3) return null
                    val timesteps = shape[1].toInt()
                    val classCount = shape[2].toInt()
                    if (timesteps <= 0 || classCount <= 0) return null
                    val probabilities = FloatArray(classCount * timesteps)
                    val buffer = output.floatBuffer
                    if (buffer.remaining() < probabilities.size) return null
                    buffer.get(probabilities)
                    return PaddleOcrDecoder.decode(
                        probabilities = probabilities,
                        timesteps = timesteps,
                        classCount = classCount,
                        labels = labels
                    )
                }
            }
        }

        companion object {
            private const val MODEL_ASSET = "paddleocr/captcha_rec.onnx"
            private const val DICT_ASSET = "paddleocr/captcha_dict.txt"

            fun load(context: Context): Engine {
                val labels = buildList {
                    // PaddleOCR 的 CTC 解码约定：0 是 blank，字典末尾补一个空格类别。
                    add("")
                    context.assets.open(DICT_ASSET).bufferedReader(Charsets.UTF_8).useLines { lines ->
                        lines.forEach { add(it) }
                    }
                    add(" ")
                }
                check(labels.size > 2) { "验证码字库为空" }

                val modelBytes = context.assets.open(MODEL_ASSET).use { it.readBytes() }
                check(modelBytes.isNotEmpty()) { "验证码识别模型为空" }

                val environment = OrtEnvironment.getEnvironment()
                val options = OrtSession.SessionOptions().apply {
                    setIntraOpNumThreads(
                        Runtime.getRuntime().availableProcessors().coerceIn(1, 4)
                    )
                    setOptimizationLevel(OrtSession.SessionOptions.OptLevel.ALL_OPT)
                }
                val session = try {
                    environment.createSession(modelBytes, options)
                } finally {
                    options.close()
                }
                return Engine(
                    environment = environment,
                    session = session,
                    inputName = session.inputNames.firstOrNull() ?: error("模型没有输入节点"),
                    labels = labels
                )
            }
        }
    }

    private companion object {
        const val TAG = "PaddleCaptchaOcr"

        /**
         * 低于该均值就把结果当作不可信、不回填。
         *
         * 真实样本上正确识别的置信度大多在 0.9 以上；带干扰线的图片偶尔会掉到 0.6。
         * 阈值只负责挡住「整图没有文字」这类输出，最终仍由用户核对。
         */
        const val MIN_CONFIDENCE = 0.35f
    }
}

/** PP-OCRv5 识别模型固定输入高度，取自模型输入张量 [1, 3, 48, W]。 */
private const val TARGET_HEIGHT = 48
private const val MIN_TARGET_WIDTH = 8
private const val MAX_TARGET_WIDTH = 960

/**
 * 把验证码 Bitmap 转成模型要的 NCHW float 张量。
 *
 * 归一化与 PaddleOCR 一致：`(pixel / 255 - 0.5) / 0.5`，通道顺序 RGB。
 * 图片按高度等比缩放，宽度保持动态——模型输入的第二维是动态的，不需要补齐到 320。
 */
private fun Bitmap.toInputTensor(
    environment: OrtEnvironment
): OnnxTensor? {
    if (width <= 0 || height <= 0 || isRecycled) return null
    val source = if (config == Bitmap.Config.ARGB_8888) {
        this
    } else {
        copy(Bitmap.Config.ARGB_8888, false) ?: return null
    }
    val targetHeight = TARGET_HEIGHT
    val targetWidth = (width.toFloat() * targetHeight / height)
        .roundToInt()
        .coerceIn(MIN_TARGET_WIDTH, MAX_TARGET_WIDTH)
    val scaled = Bitmap.createScaledBitmap(source, targetWidth, targetHeight, true)
    val pixels = IntArray(targetWidth * targetHeight)
    scaled.getPixels(pixels, 0, targetWidth, 0, 0, targetWidth, targetHeight)
    if (scaled !== source) scaled.recycle()
    if (source !== this) source.recycle()

    val planeSize = targetWidth * targetHeight
    val values = FloatArray(planeSize * 3)
    for (index in pixels.indices) {
        val color = pixels[index]
        val alpha = (color ushr 24) and 0xFF
        // 透明背景的验证码（部分接口返回 PNG）要先合成到白底，否则黑色透明区会被当成文字。
        val red = blendChannel((color shr 16) and 0xFF, alpha)
        val green = blendChannel((color shr 8) and 0xFF, alpha)
        val blue = blendChannel(color and 0xFF, alpha)
        values[index] = (red - 0.5f) / 0.5f
        values[index + planeSize] = (green - 0.5f) / 0.5f
        values[index + planeSize * 2] = (blue - 0.5f) / 0.5f
    }
    return OnnxTensor.createTensor(
        environment,
        FloatBuffer.wrap(values),
        longArrayOf(1, 3, targetHeight.toLong(), targetWidth.toLong())
    )
}

private fun blendChannel(channel: Int, alpha: Int): Float {
    val normalized = channel / 255f
    val opacity = alpha / 255f
    return normalized * opacity + (1f - opacity)
}
