package com.glut.schedule

import com.glut.schedule.service.ocr.PaddleOcrDecoder
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class PaddleOcrDecoderTest {
    private val labels = listOf("", "A", "B", " ")

    @Test
    fun `merges repeated labels and skips blank`() {
        val probabilities = floatArrayOf(
            0.10f, 0.80f, 0.05f, 0.05f,
            0.10f, 0.80f, 0.05f, 0.05f,
            0.90f, 0.05f, 0.03f, 0.02f,
            0.10f, 0.05f, 0.80f, 0.05f,
            0.10f, 0.05f, 0.05f, 0.80f
        )

        val result = PaddleOcrDecoder.decode(
            probabilities = probabilities,
            timesteps = 5,
            classCount = 4,
            labels = labels
        )

        assertEquals("AB ", result?.text)
        assertEquals(0.8f, result?.confidence ?: 0f, 0.0001f)
    }

    @Test
    fun `same label separated by blank is kept twice`() {
        val probabilities = floatArrayOf(
            0.05f, 0.90f, 0.03f, 0.02f,
            0.90f, 0.05f, 0.03f, 0.02f,
            0.05f, 0.90f, 0.03f, 0.02f
        )

        val result = PaddleOcrDecoder.decode(probabilities, 3, 4, labels)

        assertEquals("AA", result?.text)
    }

    @Test
    fun `all blank returns null`() {
        val probabilities = floatArrayOf(
            0.90f, 0.05f, 0.03f, 0.02f,
            0.90f, 0.05f, 0.03f, 0.02f
        )

        assertNull(PaddleOcrDecoder.decode(probabilities, 2, 4, labels))
    }

    @Test
    fun `short buffer is rejected`() {
        assertNull(
            PaddleOcrDecoder.decode(
                probabilities = floatArrayOf(0.9f, 0.1f),
                timesteps = 3,
                classCount = 4,
                labels = labels
            )
        )
    }

    @Test
    fun `empty or single class input is rejected`() {
        assertNull(PaddleOcrDecoder.decode(floatArrayOf(), 0, 4, labels))
        assertNull(PaddleOcrDecoder.decode(floatArrayOf(1f), 1, 1, labels))
    }

    @Test
    fun `index without label does not crash`() {
        val probabilities = floatArrayOf(
            0.05f, 0.05f, 0.90f, 0.02f
        )

        assertNull(PaddleOcrDecoder.decode(probabilities, 1, 4, listOf("", "A")))
    }
}
