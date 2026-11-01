package com.glut.schedule

import com.glut.schedule.service.UpdateDownloadPolicy
import com.glut.schedule.service.downloadFile
import com.glut.schedule.service.parseGitHubRelease
import com.glut.schedule.service.isTrustedApkIdentity
import java.io.File
import kotlinx.coroutines.runBlocking
import okhttp3.OkHttpClient
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import okio.Buffer
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test

class UpdateSecurityTest {

    @Test
    fun downloadPolicyOnlyAllowsKnownHttpsHosts() {
        assertTrue(UpdateDownloadPolicy.isAllowedDownloadUrl("https://github.com/LukasHim/glut-schedule/releases/download/v1/app.apk"))
        assertTrue(UpdateDownloadPolicy.isAllowedDownloadUrl("https://objects.githubusercontent.com/app.apk"))
        assertFalse(UpdateDownloadPolicy.isAllowedDownloadUrl("http://github.com/LukasHim/glut-schedule/releases/download/v1/app.apk"))
        // 旧的静态更新站已下线，不再是受信任来源。
        assertFalse(UpdateDownloadPolicy.isAllowedDownloadUrl("https://update.999314.xyz/app.apk"))
        assertFalse(UpdateDownloadPolicy.isAllowedDownloadUrl("https://github.com.evil.example/app.apk"))
        assertFalse(UpdateDownloadPolicy.isAllowedDownloadUrl("https://attacker.example/app.apk"))
    }

    @Test
    fun githubReleaseParsingRequiresAnApkAssetFromAnAllowedUrl() {
        val valid = """
            {
              "tag_name": "v0.26.0",
              "html_url": "https://github.com/LukasHim/glut-schedule/releases/tag/v0.26.0",
              "body": "安全更新",
              "assets": [
                {
                  "name": "glutShedule_0.26.0.apk",
                  "browser_download_url": "https://github.com/LukasHim/glut-schedule/releases/download/v0.26.0/glutShedule_0.26.0.apk"
                }
              ]
            }
        """.trimIndent()

        val info = parseGitHubRelease(valid, "0.25.0")

        assertNotNull(info)
        assertEquals("0.26.0", info?.latestVersion)
        assertTrue(info?.isNewer == true)
        // 非白名单主机的资产地址一律拒绝，不把用户引到外域下载。
        assertNull(
            parseGitHubRelease(
                valid.replace("https://github.com", "https://evil.example"),
                "0.25.0"
            )
        )
        // 没有 .apk 资产的发布不算可用更新。
        assertNull(parseGitHubRelease("""{"tag_name":"v0.26.0","assets":[]}""", "0.25.0"))
    }

    @Test
    fun updateCheckOnlyQueriesTheGitHubReleasesApi() {
        val checker = source("service/UpdateChecker.kt")

        assertTrue(checker.contains("private val repoOwner: String = \"LukasHim\""))
        assertTrue(checker.contains("https://api.github.com/repos/"))
        // 静态 JSON 渠道已下线，避免两个版本来源互相矛盾。
        assertFalse(checker.contains("update.999314.xyz"))
        assertFalse(checker.contains("github.io"))
    }

    @Test
    fun oversizedDownloadIsRejectedAndDeleted() = runBlocking {
        MockWebServer().use { server ->
            val payload = ByteArray(32) { it.toByte() }
            val target = File.createTempFile("secure-update", ".apk").apply { delete() }

            server.enqueue(MockResponse().setBody(Buffer().write(payload)))
            assertThrows(Exception::class.java) {
                runBlocking {
                    downloadFile(
                        client = OkHttpClient(),
                        url = server.url("/too-large.apk").toString(),
                        target = target,
                        maxBytes = 16,
                        urlValidator = { true },
                        onProgress = { _, _ -> }
                    )
                }
            }
            assertFalse(target.exists())
        }
    }

    @Test
    fun apkIdentityRequiresSamePackageNewerVersionAndSameSigner() {
        assertTrue(
            isTrustedApkIdentity(
                currentPackageName = "com.glut.schedule",
                currentVersionCode = 121,
                currentSignerDigests = setOf("signer-a"),
                archivePackageName = "com.glut.schedule",
                archiveVersionCode = 122,
                archiveSignerDigests = setOf("signer-a")
            )
        )
        assertFalse(
            isTrustedApkIdentity(
                "com.glut.schedule", 121, setOf("signer-a"),
                "com.attacker.app", 999, setOf("signer-a")
            )
        )
        assertFalse(
            isTrustedApkIdentity(
                "com.glut.schedule", 121, setOf("signer-a"),
                "com.glut.schedule", 122, setOf("signer-b")
            )
        )
        assertFalse(
            isTrustedApkIdentity(
                "com.glut.schedule", 121, setOf("signer-a"),
                "com.glut.schedule", 121, setOf("signer-a")
            )
        )
    }

    private fun source(relative: String): String {
        val module = File("src/main/java/com/glut/schedule/$relative")
        return (if (module.exists()) module else File("app/src/main/java/com/glut/schedule/$relative"))
            .readText()
    }
}
