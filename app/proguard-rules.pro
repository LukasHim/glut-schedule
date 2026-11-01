# ── 通用警告抑制 ──
-dontwarn com.google.errorprone.annotations.**

# ── Jsoup (HTML 解析) ──
-keep class org.jsoup.** { *; }

# ── Room 数据库实体 ──
-keep class com.glut.schedule.data.local.*Entity { *; }
-keep class com.glut.schedule.data.local.*Entity$** { *; }

# ── Glance Widget ──
-keep class com.glut.schedule.widget.** { *; }

# ── OkHttp / Okio ──
-dontwarn okhttp3.**
-dontwarn okio.**
-keep class okhttp3.** { *; }

# ── JSON (org.json) ──
-keep class org.json.** { *; }

# ── ONNX Runtime / PaddleOCR 验证码识别 ──
# JNI 通过类名查找 Java 侧的类型，混淆后 native 层会找不到入口。
-keep class ai.onnxruntime.** { *; }
-dontwarn ai.onnxruntime.**

# ── 序列化相关保留 ──
-keepclassmembers class * {
    @com.google.gson.annotations.SerializedName <fields>;
}

# ── Kotlin 协程 ──
-keepnames class kotlinx.coroutines.internal.MainDispatcherFactory {}
-keepnames class kotlinx.coroutines.CoroutineExceptionHandler {}
