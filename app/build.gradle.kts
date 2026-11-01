import com.android.build.gradle.internal.api.BaseVariantOutputImpl
import java.io.ByteArrayOutputStream
import java.util.Properties

plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.plugin.compose")
    id("com.google.devtools.ksp")
}

android {
    namespace = "com.glut.schedule"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.glut.schedule.md"
        minSdk = 26
        targetSdk = 36
        versionCode = 127
        versionName = "0.26.0"
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

    }

    val keystorePropertiesFile = rootProject.file("keystore.properties")
    val keystoreProperties = Properties().apply {
        if (keystorePropertiesFile.exists()) {
            keystorePropertiesFile.inputStream().use(::load)
        }
    }

    signingConfigs {
        // 没有 keystore.properties 时（例如只跑 debug / 单元测试的机器）不创建 release
        // 签名配置，避免配置阶段直接失败；真正发布时仍会使用该文件签名。
        if (keystorePropertiesFile.exists()) {
            create("release") {
                storeFile = rootProject.file(
                    keystoreProperties.getProperty("storeFile")
                        ?: error("Missing storeFile in keystore.properties")
                )
                storePassword = keystoreProperties.getProperty("storePassword")
                    ?: error("Missing storePassword in keystore.properties")
                keyAlias = keystoreProperties.getProperty("keyAlias")
                    ?: error("Missing keyAlias in keystore.properties")
                keyPassword = keystoreProperties.getProperty("keyPassword")
                    ?: error("Missing keyPassword in keystore.properties")
            }
        }
    }

    buildTypes {
        debug {
            // Debug 包保留 Workers.dev 联调入口，正式包使用自定义域名。
            buildConfigField(
                "String",
                "SCHEDULE_SHARE_BASE_URL",
                "\"https://schedule-share-worker.hezh.workers.dev\""
            )
        }
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            signingConfig = signingConfigs.findByName("release")
            buildConfigField(
                "String",
                "SCHEDULE_SHARE_BASE_URL",
                "\"https://share-schedule.999314.xyz\""
            )
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    kotlin {
        compilerOptions {
            jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_17)
        }
    }

    buildFeatures {
        compose = true
        buildConfig = true
    }

    applicationVariants.all {
        if (buildType.name == "release") {
            outputs.all {
                (this as BaseVariantOutputImpl).outputFileName = "glutShedule_$versionName.apk"
            }
        }
    }
}

ksp {
    arg("room.schemaLocation", "$projectDir/schemas")
}

dependencies {
    val composeBom = platform("androidx.compose:compose-bom:2026.04.01")
    implementation(composeBom)

    implementation("androidx.activity:activity-compose:1.13.0")
    implementation("androidx.compose.foundation:foundation")
    implementation("androidx.compose.material:material-icons-extended")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.ui:ui")
    implementation("androidx.lifecycle:lifecycle-runtime-compose:2.10.0")
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.10.0")
    implementation("androidx.room:room-runtime:2.8.4")
    implementation("androidx.room:room-ktx:2.8.4")
    implementation("androidx.datastore:datastore-preferences:1.2.1")
    implementation("androidx.exifinterface:exifinterface:1.4.2")
    implementation("androidx.glance:glance-appwidget:1.1.1")
    implementation("androidx.work:work-runtime-ktx:2.10.0")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.10.2")
    implementation("com.squareup.okhttp3:okhttp:5.3.2")
    // security-crypto 1.1.0 尚无稳定版，alpha06 的 MasterKey API 在多个 alpha 版本间
    // 保持兼容（FinanceStore/FitnessStore 均依赖此 API）。alpha 标签不影响本应用的
    // 加密安全性——加密算法本身（AES-256 GCM/SIV）与稳定版一致。
    implementation("androidx.security:security-crypto:1.1.0-alpha06")
    implementation("org.jsoup:jsoup:1.21.2")
    // 验证码识别使用 PaddleOCR 的 PP-OCRv5 识别模型（ONNX 格式）。
    // mobile 包只保留标准算子，体积约为 android 完整包的 1/4，足够跑 CNN + CTC 识别模型。
    implementation("com.microsoft.onnxruntime:onnxruntime-mobile:1.18.0")

    ksp("androidx.room:room-compiler:2.8.4")

    debugImplementation("androidx.compose.ui:ui-tooling")

    testImplementation("junit:junit:4.13.2")
    testImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:1.10.2")
    testImplementation("org.json:json:20240303")
    testImplementation("com.squareup.okhttp3:mockwebserver:5.3.2")

    androidTestImplementation("androidx.test:core:1.7.0")
    androidTestImplementation("androidx.test.ext:junit:1.3.0")
    androidTestImplementation("androidx.test:runner:1.7.0")
}

// ── Publishing ──
// Test builds: use .\gradlew.bat assembleDebug or assembleRelease (no publishing)
// Release:     use .\gradlew.bat publishUpdate (builds + GitHub Release)
tasks.register("publishUpdate") {
    group = "publishing"
    description = "Build release APK and create a GitHub Release"

    dependsOn("assembleRelease")

    doLast {
        val versionName = android.defaultConfig.versionName
        val apkDir = layout.buildDirectory.dir("outputs/apk/release").get().asFile
        val apkFile = File(apkDir, "glutShedule_$versionName.apk")
            .takeIf(File::isFile)
            ?: error("Release APK not found: ${File(apkDir, "glutShedule_$versionName.apk")}")
        val rootDir = file("${rootProject.projectDir}")

        fun gitStatus(directory: File): String {
            val output = ByteArrayOutputStream()
            exec {
                workingDir = directory
                commandLine("git", "status", "--porcelain")
                standardOutput = output
            }
            return output.toString(Charsets.UTF_8).trim()
        }

        if (gitStatus(rootDir).isNotBlank()) {
            throw GradleException("scheduleApp 工作区存在未提交修改，拒绝发布")
        }

        // 客户端只从 GitHub Releases API 读取更新（见 UpdateChecker），
        // 所以发布只需要创建 Release：APK 作为 Release 资产就能被客户端发现。
        exec {
            workingDir = rootDir
            commandLine(
                "gh", "release", "create", "v$versionName", apkFile.absolutePath,
                "--title", "v$versionName",
                "--notes", "Release v$versionName"
            )
        }
        println("✅ v$versionName published: GitHub Release")
    }
}
