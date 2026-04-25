package com.indolearn.app

import android.annotation.SuppressLint
import android.app.DownloadManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.speech.tts.TextToSpeech
import android.webkit.*
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.browser.customtabs.CustomTabsIntent
import androidx.core.content.ContextCompat
import org.json.JSONObject
import java.net.URL
import java.util.Locale
import kotlin.concurrent.thread

/**
 * IndoLearn Android App
 *
 * Architecture:
 * ┌─────────────────────────────────────────────────────┐
 * │  LocalServer  ←  assets/indolearn.html              │
 * │  (localhost:8765)                                   │
 * │         ↓  WebView loads from localhost             │
 * │  MainActivity  ↔  AndroidBridge (JS interface)      │
 * │         ↓  startGmailOAuth() → Chrome Custom Tab    │
 * │  Google OAuth  → deep link  com.indolearn.app://…   │
 * │         ↓  onNewIntent extracts access_token        │
 * │  speak() → Android TextToSpeech (Indonesian)        │
 * │  checkForUpdate() → version.json on CDN/domain      │
 * │  downloadUpdate() → DownloadManager → install       │
 * └─────────────────────────────────────────────────────┘
 */
class MainActivity : AppCompatActivity() {

    private lateinit var webView: WebView
    private lateinit var server: LocalServer
    private val serverPort = 8765
    private val appUrl = "http://localhost:$serverPort/indolearn.html"

    // Native TTS — much more reliable than WebView speechSynthesis for Indonesian
    private var tts: TextToSpeech? = null
    private var ttsReady = false

    @SuppressLint("SetJavaScriptEnabled")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Init TextToSpeech with Indonesian locale
        tts = TextToSpeech(this) { status ->
            if (status == TextToSpeech.SUCCESS) {
                val result = tts?.setLanguage(Locale("id", "ID"))
                ttsReady = (result != TextToSpeech.LANG_MISSING_DATA
                        && result != TextToSpeech.LANG_NOT_SUPPORTED)
            }
        }

        // Start local HTTP server serving assets/
        server = LocalServer(applicationContext, serverPort)
        server.start()

        // Build WebView
        webView = WebView(this).also { setContentView(it) }

        webView.settings.apply {
            javaScriptEnabled = true
            domStorageEnabled = true       // localStorage
            databaseEnabled = true
            allowFileAccess = false
            mixedContentMode = WebSettings.MIXED_CONTENT_NEVER_ALLOW
            cacheMode = WebSettings.LOAD_DEFAULT
            setSupportZoom(false)
            useWideViewPort = true
            loadWithOverviewMode = true
            // Explicitly set UTF-8 — some Chinese ROM WebViews default to Latin-1,
            // causing all Chinese / emoji text to appear as mojibake.
            defaultTextEncodingName = "UTF-8"
        }

        // JavaScript bridge
        webView.addJavascriptInterface(AndroidBridge(), "AndroidBridge")

        webView.webChromeClient = object : WebChromeClient() {
            override fun onConsoleMessage(m: ConsoleMessage): Boolean = true
        }

        webView.webViewClient = object : WebViewClient() {
            override fun shouldOverrideUrlLoading(view: WebView, request: WebResourceRequest): Boolean {
                val uri = request.url
                if (uri.host == "localhost") return false
                startActivity(Intent(Intent.ACTION_VIEW, uri))
                return true
            }
        }

        webView.loadUrl(appUrl)
        handleOAuthIntent(intent)
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        handleOAuthIntent(intent)
    }

    /**
     * Called when Google OAuth redirects to com.indolearn.app://oauth2redirect#access_token=…
     */
    private fun handleOAuthIntent(intent: Intent?) {
        val uri: Uri = intent?.data ?: return
        if (uri.scheme != "com.indolearn.app" || uri.host != "oauth2redirect") return

        val fragment = uri.fragment ?: run {
            runOnUiThread { Toast.makeText(this, "授权失败：无法读取令牌", Toast.LENGTH_SHORT).show() }
            return
        }

        val params = fragment.split("&").associate { part ->
            val idx = part.indexOf('=')
            if (idx >= 0) part.substring(0, idx) to part.substring(idx + 1) else part to ""
        }

        val token = params["access_token"]
        if (token.isNullOrEmpty()) {
            val error = params["error"] ?: "未知错误"
            runOnUiThread {
                Toast.makeText(this, "授权失败: $error", Toast.LENGTH_SHORT).show()
                // Can't parse state without token, default to gmail callback
                webView.evaluateJavascript("onGmailTokenReceived('')", null)
            }
            return
        }

        // Route token to the correct JS callback based on the OAuth state parameter.
        // Gmail and Drive both use the same deep-link redirect URI; state tells us which flow.
        val callbackFn = when (params["state"]) {
            "drive" -> "onDriveTokenReceived"
            else    -> "onGmailTokenReceived"
        }
        runOnUiThread {
            webView.evaluateJavascript("$callbackFn('$token')", null)
        }
    }

    override fun onBackPressed() {
        if (webView.canGoBack()) webView.goBack() else super.onBackPressed()
    }

    override fun onDestroy() {
        super.onDestroy()
        server.stop()
        webView.destroy()
        tts?.stop()
        tts?.shutdown()
    }

    // ── JavaScript Bridge ──────────────────────────────────────────────────────

    inner class AndroidBridge {

        /** Opens Gmail OAuth in Chrome Custom Tab; result returns via deep link → onGmailTokenReceived() */
        @JavascriptInterface
        fun startGmailOAuth(clientId: String) {
            if (clientId.isBlank()) {
                runOnUiThread {
                    Toast.makeText(this@MainActivity,
                        "请先在设置中填写 Google OAuth Client ID", Toast.LENGTH_LONG).show()
                }
                return
            }
            val redirectUri = "http://localhost:$serverPort/oauth2redirect"
            val authUrl = Uri.parse("https://accounts.google.com/o/oauth2/v2/auth")
                .buildUpon()
                .appendQueryParameter("client_id", clientId)
                .appendQueryParameter("redirect_uri", redirectUri)
                .appendQueryParameter("response_type", "token")
                .appendQueryParameter("scope", "https://www.googleapis.com/auth/gmail.readonly")
                .appendQueryParameter("include_granted_scopes", "true")
                .appendQueryParameter("state", "gmail")
                .build()
            runOnUiThread {
                CustomTabsIntent.Builder().setShowTitle(false).build()
                    .launchUrl(this@MainActivity, authUrl)
            }
        }

        @JavascriptInterface
        fun startDriveOAuth(clientId: String) {
            if (clientId.isBlank()) return
            val redirectUri = "http://localhost:$serverPort/oauth2redirect"
            val authUrl = Uri.parse("https://accounts.google.com/o/oauth2/v2/auth")
                .buildUpon()
                .appendQueryParameter("client_id", clientId)
                .appendQueryParameter("redirect_uri", redirectUri)
                .appendQueryParameter("response_type", "token")
                .appendQueryParameter("scope", "https://www.googleapis.com/auth/drive.appdata")
                .appendQueryParameter("state", "drive")
                .build()
            runOnUiThread {
                CustomTabsIntent.Builder().build().launchUrl(this@MainActivity, authUrl)
            }
        }

        /**
         * Returns true if native Indonesian TTS is available and ready.
         * JS checks this before calling speak() so it can fall back to Web Speech API.
         */
        @JavascriptInterface
        fun isTtsReady(): Boolean = ttsReady

        /**
         * Native TTS via Android TextToSpeech — reliable Indonesian pronunciation.
         * JS: window.AndroidBridge.speak(text)
         */
        @JavascriptInterface
        fun speak(text: String) {
            if (!ttsReady) return
            tts?.speak(text, TextToSpeech.QUEUE_FLUSH, null, "tts_${System.currentTimeMillis()}")
        }

        /** Returns the current app version name, e.g. "1.0.4" */
        @JavascriptInterface
        fun getVersion(): String = BuildConfig.VERSION_NAME

        /**
         * Fetches updateUrl JSON: { "version_code": 5, "version": "1.0.5", "apk_url": "...", "changelog": "..." }
         * If version_code > current BuildConfig.VERSION_CODE → calls onUpdateAvailable() in JS.
         */
        @JavascriptInterface
        fun checkForUpdate(updateUrl: String) {
            if (updateUrl.isBlank()) return
            thread {
                try {
                    val json = JSONObject(URL(updateUrl).readText())
                    val latestCode = json.optInt("version_code", 0)
                    val latestName = json.optString("version", "")
                    val apkUrl = json.getString("apk_url")
                    val changelog = json.optString("changelog", "").replace("'", "\\'")
                    if (latestCode > BuildConfig.VERSION_CODE) {
                        runOnUiThread {
                            webView.evaluateJavascript(
                                "onUpdateAvailable('$latestName','$apkUrl','$changelog')", null)
                        }
                    }
                } catch (_: Exception) { /* silent — no internet or malformed JSON */ }
            }
        }

        /**
         * Downloads APK via system DownloadManager; install prompt appears on completion.
         * JS: window.AndroidBridge.downloadUpdate(apkUrl, version)
         */
        @JavascriptInterface
        fun downloadUpdate(apkUrl: String, version: String) {
            runOnUiThread {
                Toast.makeText(this@MainActivity, "正在后台下载 v$version...", Toast.LENGTH_SHORT).show()
            }
            val request = DownloadManager.Request(Uri.parse(apkUrl))
                .setTitle("IndoLearn $version")
                .setDescription("正在下载更新...")
                .setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
                .setDestinationInExternalFilesDir(this@MainActivity, null, "IndoLearn-$version.apk")
                .setMimeType("application/vnd.android.package-archive")
            val dm = getSystemService(DOWNLOAD_SERVICE) as DownloadManager
            val downloadId = dm.enqueue(request)
            val receiver = object : BroadcastReceiver() {
                override fun onReceive(context: Context, intent: Intent) {
                    val id = intent.getLongExtra(DownloadManager.EXTRA_DOWNLOAD_ID, -1)
                    if (id != downloadId) return
                    // Use getUriForDownloadedFile() — returns content:// URI that works on all
                    // Android versions without FileProvider, unlike file:// from COLUMN_LOCAL_URI.
                    val contentUri = dm.getUriForDownloadedFile(downloadId)
                    if (contentUri != null) {
                        runOnUiThread { installApk(contentUri) }
                    } else {
                        runOnUiThread {
                            Toast.makeText(this@MainActivity,
                                "下载完成，请在通知栏点击安装", Toast.LENGTH_LONG).show()
                        }
                    }
                    try { unregisterReceiver(this) } catch (_: Exception) {}
                }
            }
            // ACTION_DOWNLOAD_COMPLETE is a protected system broadcast → RECEIVER_EXPORTED on API 33+
            ContextCompat.registerReceiver(
                this@MainActivity, receiver,
                IntentFilter(DownloadManager.ACTION_DOWNLOAD_COMPLETE),
                ContextCompat.RECEIVER_EXPORTED
            )
        }

        @JavascriptInterface
        fun showToast(message: String) {
            runOnUiThread { Toast.makeText(this@MainActivity, message, Toast.LENGTH_SHORT).show() }
        }
    }

    /**
     * Launch the APK installer.
     *
     * On Android 8+ (API 26+) the user must explicitly grant "Install unknown apps" permission
     * to this app — unlike the old global "Unknown sources" toggle. If not granted yet, we open
     * the per-app settings page so they can enable it, then ask them to retry the update.
     */
    private fun installApk(contentUri: Uri) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            if (!packageManager.canRequestPackageInstalls()) {
                Toast.makeText(
                    this,
                    "请在接下来的设置页面开启「允许安装未知应用」权限，然后重新点击更新",
                    Toast.LENGTH_LONG
                ).show()
                startActivity(
                    Intent(Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES,
                        Uri.parse("package:$packageName"))
                )
                return
            }
        }
        val intent = Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(contentUri, "application/vnd.android.package-archive")
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_GRANT_READ_URI_PERMISSION
        }
        try {
            startActivity(intent)
        } catch (_: Exception) {
            Toast.makeText(this, "无法启动安装程序，请到通知栏手动点击安装", Toast.LENGTH_LONG).show()
        }
    }
}
