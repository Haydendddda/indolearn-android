package com.indolearn.app

import android.content.Context
import java.io.BufferedReader
import java.io.InputStreamReader
import java.net.ServerSocket
import java.net.Socket
import kotlin.concurrent.thread

/**
 * Minimal HTTP server that serves files from Android assets.
 * Needed because Google OAuth2 blocks file:// origins —
 * serving from localhost:PORT is required.
 */
class LocalServer(private val context: Context, val port: Int = 8765) {

    private var serverSocket: ServerSocket? = null
    @Volatile private var running = false

    fun start() {
        if (running) return
        running = true
        thread(name = "IndoLearnServer") {
            try {
                serverSocket = ServerSocket(port)
                while (running) {
                    val client = serverSocket!!.accept()
                    thread { handleClient(client) }
                }
            } catch (_: Exception) {}
        }
    }

    fun stop() {
        running = false
        serverSocket?.close()
    }

    private fun handleClient(socket: Socket) {
        try {
            val reader = BufferedReader(InputStreamReader(socket.getInputStream()))
            val requestLine = reader.readLine() ?: return
            val parts = requestLine.split(" ")
            val rawPath = if (parts.size >= 2) parts[1] else "/"
            val path = rawPath.substringBefore("?").trimStart('/')

            val out = socket.getOutputStream()

            // Special route: OAuth2 relay page
            // Google redirects here with #access_token=... in the fragment.
            // This page reads the fragment via JS and bounces it back to the
            // app's deep-link scheme so onNewIntent() can pick it up.
            if (path == "oauth2redirect") {
                val html = """<!DOCTYPE html>
<html><head><meta charset="utf-8"><title>授权中...</title></head>
<body>
<p>正在完成授权，请稍候...</p>
<script>
var hash = window.location.hash;
var search = window.location.search;
if (hash && hash.length > 1) {
  window.location.href = 'com.indolearn.app://oauth2redirect' + hash;
} else if (search) {
  window.location.href = 'com.indolearn.app://oauth2redirect' + search;
} else {
  document.body.innerText = '授权失败：未收到令牌';
}
</script>
</body></html>""".trimIndent()
                val bytes = html.toByteArray(Charsets.UTF_8)
                val header = "HTTP/1.1 200 OK\r\nContent-Type: text/html; charset=utf-8\r\n" +
                        "Content-Length: ${bytes.size}\r\nCache-Control: no-cache\r\nConnection: close\r\n\r\n"
                out.write(header.toByteArray(Charsets.UTF_8))
                out.write(bytes)
                out.flush()
                return
            }

            val fileName = when {
                path.isEmpty() || path == "indolearn.html" -> "indolearn.html"
                else -> path
            }

            try {
                val bytes = context.assets.open(fileName).readBytes()
                val mime = mimeType(fileName)
                val header = "HTTP/1.1 200 OK\r\nContent-Type: $mime\r\n" +
                        "Content-Length: ${bytes.size}\r\n" +
                        "Cache-Control: no-cache\r\n" +
                        "Connection: close\r\n\r\n"
                out.write(header.toByteArray(Charsets.UTF_8))
                out.write(bytes)
            } catch (_: Exception) {
                val body = "404 Not Found"
                val header = "HTTP/1.1 404 Not Found\r\nContent-Length: ${body.length}\r\nConnection: close\r\n\r\n"
                out.write((header + body).toByteArray())
            }
            out.flush()
        } finally {
            socket.close()
        }
    }

    private fun mimeType(name: String) = when {
        name.endsWith(".html") -> "text/html; charset=utf-8"
        name.endsWith(".js")   -> "application/javascript; charset=utf-8"
        name.endsWith(".css")  -> "text/css; charset=utf-8"
        name.endsWith(".png")  -> "image/png"
        name.endsWith(".ico")  -> "image/x-icon"
        else -> "application/octet-stream"
    }
}
