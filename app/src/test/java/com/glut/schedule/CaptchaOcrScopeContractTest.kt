package com.glut.schedule

import java.io.File
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * 验证码自动识别只允许出现在财务平台与体测平台的登录弹窗。
 *
 * 教务直连（南宁 / 研究生）的验证码仍由用户手动输入：那两处的验证码经常被
 * 学校用作「人工确认」而非纯机器校验，自动回填会掩盖登录失败的原因。
 */
class CaptchaOcrScopeContractTest {
    private fun page(name: String): String {
        val module = File("src/main/java/com/glut/schedule/ui/pages/$name")
        return (if (module.exists()) module else
            File("app/src/main/java/com/glut/schedule/ui/pages/$name")).readText()
    }

    private val mainActivity by lazy {
        val module = File("src/main/java/com/glut/schedule/MainActivity.kt")
        (if (module.exists()) module else
            File("app/src/main/java/com/glut/schedule/MainActivity.kt")).readText()
    }

    @Test
    fun academicDirectLoginDoesNotRunCaptchaOcr() {
        val viewModel = page("DirectLoginViewModel.kt")
        val screen = page("DirectLoginScreen.kt")

        assertFalse(viewModel.contains("recognizeCaptcha"))
        assertFalse(viewModel.contains("captchaOcr"))
        assertFalse(screen.contains("isCaptchaRecognizing"))
        assertFalse(screen.contains("captchaAutoRecognized"))
        assertFalse(screen.contains("正在自动识别验证码"))
    }

    @Test
    fun financeLoginRecognizesCaptcha() {
        val viewModel = page("FinanceViewModel.kt")
        val screen = page("FinanceScreen.kt")

        assertTrue(viewModel.contains("recognizeCaptcha("))
        assertTrue(viewModel.contains("captchaOcr"))
        assertTrue(screen.contains("login.isCaptchaRecognizing"))
        assertTrue(screen.contains("login.captchaAutoRecognized"))
    }

    @Test
    fun fitnessLoginStillRecognizesCaptcha() {
        val viewModel = page("FitnessScoreViewModel.kt")
        assertTrue(viewModel.contains("recognizeCaptcha("))
        assertTrue(viewModel.contains("captchaOcr"))
    }

    @Test
    fun onlyFinanceAndFitnessFactoriesReceiveTheOcrEngine() {
        val ocrWiring = mainActivity.lineSequence().count { line ->
            line.contains("captchaOcr = container.captchaOcr")
        }
        assertTrue("财务与体测两个工厂都应注入 OCR", ocrWiring >= 2)

        val directFactory = mainActivity
            .substringAfter("factory = DirectLoginViewModelFactory(")
            .substringBefore(")")
        assertFalse("教务直连工厂不应再注入 OCR", directFactory.contains("captchaOcr"))
    }
}
