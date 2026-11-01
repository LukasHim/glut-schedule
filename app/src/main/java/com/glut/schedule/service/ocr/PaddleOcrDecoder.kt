package com.glut.schedule.service.ocr

/**
 * 一次验证码识别结果。
 *
 * [confidence] 是 CTC 各有效时间步最大概率的均值，只用于丢弃明显不可信的识别，
 * 不向用户展示——验证码本身没有「准确率」概念，最终仍以服务端校验为准。
 */
data class CaptchaRecognition(
    val text: String,
    val confidence: Float
)

/**
 * PaddleOCR 识别模型输出的贪心 CTC 解码。
 *
 * 模型输出形状为 `[1, T, C]`，`C` 个类别里第 0 类固定是 blank；PaddleOCR 的字典
 * 在末尾额外补了一个空格类别，因此 [labels] 的长度必须等于 `C`。
 *
 * 解码规则与 PaddleOCR `CTCLabelDecode` 一致：
 * 1. 每个时间步取概率最大的类别；
 * 2. 跳过 blank；
 * 3. 合并连续重复的类别（`a a blank a` → `aa`）。
 *
 * 抽成纯函数是为了能在 JVM 单元测试里覆盖重复字符、全 blank、越界等边界，
 * 不需要加载 native ONNX Runtime。
 */
object PaddleOcrDecoder {
    private const val BLANK_INDEX = 0

    fun decode(
        probabilities: FloatArray,
        timesteps: Int,
        classCount: Int,
        labels: List<String>
    ): CaptchaRecognition? {
        if (timesteps <= 0 || classCount <= 1) return null
        if (probabilities.size < timesteps * classCount) return null

        val text = StringBuilder()
        var confidenceSum = 0f
        var keptCount = 0
        var previous = BLANK_INDEX

        for (step in 0 until timesteps) {
            val offset = step * classCount
            var bestIndex = 0
            var bestProbability = probabilities[offset]
            for (index in 1 until classCount) {
                val probability = probabilities[offset + index]
                if (probability > bestProbability) {
                    bestProbability = probability
                    bestIndex = index
                }
            }
            if (bestIndex != BLANK_INDEX && bestIndex != previous) {
                val label = labels.getOrNull(bestIndex)
                if (!label.isNullOrEmpty()) {
                    text.append(label)
                    confidenceSum += bestProbability.coerceIn(0f, 1f)
                    keptCount++
                }
            }
            previous = bestIndex
        }

        if (text.isEmpty() || keptCount == 0) return null
        return CaptchaRecognition(
            text = text.toString(),
            confidence = confidenceSum / keptCount
        )
    }
}
