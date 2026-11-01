package com.glut.schedule

import java.io.File
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class AboutAndFaqCopyTest {
    @Test
    fun aboutPageLinksMaintainerAndContributorProfiles() {
        val source = readSource("AboutScreen.kt")

        assertTrue(source.contains("维护者"))
        assertTrue(source.contains("24人工智能 hezh"))
        assertTrue(source.contains("uriHandler.openUri(\"https://github.com/hzhkdh\")"))
        assertTrue(source.contains("label = \"贡献者\""))
        assertTrue(source.contains("24人工智能 mzjia"))
        assertTrue(source.contains("uriHandler.openUri(\"https://github.com/m-z-jia\")"))
        assertFalse(source.contains("Made with"))
    }

    @Test
    fun aboutPageLinksNewGithubIssueForFeedback() {
        val source = readSource("AboutScreen.kt")

        assertTrue(source.contains("LazyColumn("))
        assertTrue(source.contains("提交问题"))
        assertTrue(source.contains("GitHub Issues"))
        assertTrue(source.contains("uriHandler.openUri(\"https://github.com/LukasHim/glut-schedule/issues/new\")"))
    }

    @Test
    fun faqCopyMatchesAcceptedTextChanges() {
        val source = readSource("FaqScreen.kt")

        assertTrue(source.contains("更新逻辑"))
        // 原本还断言过「感谢前辈大佬，致敬开源！」——它随「关于项目」整段一起被产品删除了。
        // 注意：那句话和三个参考项目的链接是 App 里**唯一**的开源致谢，AboutScreen 里并没有备份；
        // 如果之后决定把致谢挪到别处，这条断言应该跟着挪过去，而不是在这里复活。
        assertFalse(source.contains("感谢前辈大佬，致敬开源！"))
        assertFalse(source.contains("为什么更新下载这么慢？"))
        assertFalse(source.contains("为什么想到开发这个项目？"))
        assertFalse(source.contains("他们的时代可没有 AI"))
    }

    private fun readSource(fileName: String): String {
        val appModulePath = File("src/main/java/com/glut/schedule/ui/pages/$fileName")
        if (appModulePath.exists()) return appModulePath.readText()
        return File("app/src/main/java/com/glut/schedule/ui/pages/$fileName").readText()
    }
}
