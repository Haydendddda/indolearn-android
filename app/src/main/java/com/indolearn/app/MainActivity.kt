package com.indolearn.app

import android.annotation.SuppressLint
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.webkit.*
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.browser.customtabs.CustomTabsIntent

/**
 * IndoLearn Android App
 *
 * Architecture:
 * ┌─────────────────────────────────────────────────────┐
 * │  LocalServer  ←  assets/indolearn.html              │
 * │  (localhost:8765)                                   │
 * │         ↓  WebView loads from localhost             │
 * │  MainActivity  ↔  AndroidBridge (JS interface)      │
 * │         ↓  startGmailOAuth() opens Chrome Custom Tab│
 * │  Google OAuth  → deep link  com.indolearn.app://…   │
 * │         ↓  onNewIntent extracts access_token        │
 * │  WebView.evaluateJavascript("onGmailToken…")        │
 * └─────────────────────────────────────────────────────┘
 *
 * Setup required (one-time):
 *   Google Cloud Console → OAuth 2.0 Client ID (Web application)
 *   → Authorised redirect URIs → add: com.indolearn.app://oauth2redirect
 */
class MainActivity : AppCompatActivity() {

    private lateinit var webView: WebView
    private lateinit var server: LocalServer
    private val serverPort = 8765
    private val appUrl = "http://localhost:$serverPort/indolearn.html"

    @SuppressLint("SetJavaScriptEnabled")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Start local HTTP server
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
        }

        // JavaScript bridge so the web page can trigger native OAuth
        webView.addJavascriptInterface(AndroidBridge(), "AndroidBridge")

        webView.webChromeClient = object : WebChromeClient() {
            override fun onConsoleMessage(m: ConsoleMessage): Boolean {
                // Uncomment for debugging: android.util.Log.d("IndoLearn", m.message())
                return true
            }
        }

        webView.webViewClient = object : WebViewClient() {
            override fun shouldOverrideUrlLoading(view: WebView, request: WebResourceRequest): Boolean {
                val uri = request.url
                // Let localhost requests pass through
                if (uri.host == "localhost") return false
                // Open external links in system browser
                startActivity(Intent(Intent.ACTION_VIEW, uri))
                return true
            }
        }

        webView.loadUrl(appUrl)

        // Handle OAuth redirect if app launched via deep link
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

        // The access_token is in the URI fragment: #access_token=TOKEN&token_type=Bearer&...
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
                webView.evaluateJavascript("onGmailTokenReceived('')", null)
            }
            return
        }

        // Pass token back to the web app
        runOnUiThread {
            webView.evaluateJavascript("onGmailTokenReceived('$token')", null)
        }
    }

    override fun onBackPressed() {
        if (webView.canGoBack()) webView.goBack() else super.onBackPressed()
    }

    override fun onDestroy() {
        super.onDestroy()
        server.stop()
        webView.destroy()
    }

    // ── JavaScript Bridge ──────────────────────────────────────────────────────

    inner class AndroidBridge {

        /**
         * Called from JavaScript: window.AndroidBridge.startGmailOAuth(clientId)
         * Opens Google OAuth in Chrome Custom Tab; result returns via deep link.
         */
        @JavascriptInterface
        fun startGmailOAuth(clientId: String) {
            if (clientId.isBlank()) {
                runOnUiThread {
                    Toast.makeText(this@MainActivity,
                        "请先在设置中填写 Google OAuth Client ID", Toast.LENGTH_LONG).show()
                }
                return
            }

            val redirectUri = "com.indolearn.app://oauth2redirect"
            val scope = "https://www.googleapis.com/auth/gmail.readonly"

            val authUrl = Uri.parse("https://accounts.google.com/o/oauth2/v2/auth")
                .buildUpon()
                .appendQueryParameter("client_id", clientId)
                .appendQueryParameter("redirect_uri", redirectUri)
                .appendQueryParameter("response_type", "token")
                .appendQueryParameter("scope", scope)
                .appendQueryParameter("include_granted_scopes", "true")
                .build()

            runOnUiThread {
                val tabIntent = CustomTabsIntent.Builder()
                    .setShowTitle(false)
                    .build()
                tabIntent.launchUrl(this@MainActivity, authUrl)
            }
        }

        /**
         * Called from JavaScript: window.AndroidBridge.startDriveOAuth(clientId)
         */
        @JavascriptInterface
        fun startDriveOAuth(clientId: String) {
            if (clientId.isBlank()) return
            val redirectUri = "com.indolearn.app://oauth2redirect"
            val scope = "https://www.googleapis.com/auth/drive.appdata"

            val authUrl = Uri.parse("https://accounts.google.com/o/oauth2/v2/auth")
                .buildUpon()
                .appendQueryParameter("client_id", clientId)
                .appendQueryParameter("redirect_uri", redirectUri)
                .appendQueryParameter("response_type", "token")
                .appendQueryParameter("scope", scope)
                .build()

            runOnUiThread {
                CustomTabsIntent.Builder().build()
                    .launchUrl(this@MainActivity, authUrl)
            }
        }

        @JavascriptInterface
        fun showToast(message: String) {
            runOnUiThread { Toast.makeText(this@MainActivity, message, Toast.LENGTH_SHORT).show() }
        }
    }
}
