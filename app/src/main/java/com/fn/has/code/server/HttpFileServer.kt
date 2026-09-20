package com.fn.has.code.server

import android.content.Context
import android.net.Uri
import androidx.documentfile.provider.DocumentFile
import java.io.BufferedInputStream
import java.io.ByteArrayOutputStream
import java.io.InputStream
import java.io.OutputStream
import java.io.PrintWriter
import java.net.ServerSocket
import java.net.Socket
import java.net.URLDecoder
import java.util.Base64
import kotlin.concurrent.thread

class HttpFileServer(
    private val context: Context,
    private val port: Int,
    var sharedFolderUri: Uri? = null,
    var isAuthEnabled: Boolean = false,
    var username: String = "admin",
    var password: String = "1234"
) {

    private var serverSocket: ServerSocket? = null
    private var isRunning = false

    fun start() {
        if (isRunning) return
        isRunning = true

        thread(start = true, name = "HttpFileServerThread") {
            try {
                serverSocket = ServerSocket(port)
                while (isRunning) {
                    val clientSocket = serverSocket?.accept() ?: break
                    handleClient(clientSocket)
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    private fun handleClient(socket: Socket) {
        thread {
            try {
                val inputStream = BufferedInputStream(socket.getInputStream())
                val outputStream = socket.getOutputStream()
                val writer = PrintWriter(outputStream)

                val requestLine = readLine(inputStream) ?: return@thread
                val parts = requestLine.split(" ")
                if (parts.size < 2) {
                    send404(writer)
                    return@thread
                }

                val method = parts[0]
                val rawPath = URLDecoder.decode(parts[1], "UTF-8")

                // قراءة Headers
                val headers = HashMap<String, String>()
                while (true) {
                    val line = readLine(inputStream) ?: break
                    if (line.isEmpty()) break
                    val headerParts = line.split(":", limit = 2)
                    if (headerParts.size == 2) {
                        headers[headerParts[0].trim().lowercase()] = headerParts[1].trim()
                    }
                }

                // التحقق من المصادقة (HTTP Basic Auth)
                if (isAuthEnabled && !checkAuthentication(headers)) {
                    send401Unauthorized(writer)
                    return@thread
                }

                val rootTree = sharedFolderUri?.let { DocumentFile.fromTreeUri(context, it) }
                if (rootTree == null || !rootTree.exists()) {
                    sendHtmlResponse(writer, buildNoFolderSelectedPage())
                    return@thread
                }

                if (method == "GET") {
                    handleGetRequest(rawPath, rootTree, writer, outputStream)
                } else if (method == "POST" && rawPath.startsWith("/upload")) {
                    handleFileUpload(inputStream, headers, rootTree, rawPath, writer)
                } else {
                    send404(writer)
                }

            } catch (e: Exception) {
                e.printStackTrace()
            } finally {
                socket.close()
            }
        }
    }

    private fun checkAuthentication(headers: Map<String, String>): Boolean {
        val authHeader = headers["authorization"] ?: return false
        if (!authHeader.startsWith("Basic ", ignoreCase = true)) return false

        return try {
            val base64Credentials = authHeader.substring(6).trim()
            val credentials = String(Base64.getDecoder().decode(base64Credentials), Charsets.UTF_8)
            val expectedCredentials = "$username:$password"
            credentials == expectedCredentials
        } catch (e: Exception) {
            false
        }
    }

    private fun send401Unauthorized(writer: PrintWriter) {
        val html = """
            <!DOCTYPE html>
            <html dir="rtl" lang="ar">
            <head><meta charset="UTF-8"><title>401 Unauthorized</title></head>
            <body style="background:#0F172A;color:#fff;text-align:center;padding:50px;font-family:sans-serif;">
                <h1 style="color:#EF4444;">401 - الوصول مرفوض</h1>
                <p>يلزم تسجيل الدخول باستخدام اسم المستخدم وكلمة المرور المحددة في التطبيق.</p>
            </body>
            </html>
        """.trimIndent()

        writer.println("HTTP/1.1 401 Unauthorized")
        writer.println("WWW-Authenticate: Basic realm=\"FlowNet Secure Local Server\"")
        writer.println("Content-Type: text/html; charset=utf-8")
        writer.println("Content-Length: ${html.toByteArray(Charsets.UTF_8).size}")
        writer.println("Connection: close")
        writer.println()
        writer.println(html)
        writer.flush()
    }

    private fun handleGetRequest(
        rawPath: String,
        rootTree: DocumentFile,
        writer: PrintWriter,
        outputStream: OutputStream
    ) {
        val targetFile = resolvePath(rootTree, rawPath)
        if (targetFile == null || !targetFile.exists()) {
            send404(writer)
        } else if (targetFile.isDirectory) {
            sendHtmlResponse(writer, buildDirectoryListingPage(targetFile, rawPath))
        } else {
            sendFileResponse(outputStream, targetFile)
        }
    }

    private fun handleFileUpload(
        inputStream: InputStream,
        headers: Map<String, String>,
        rootTree: DocumentFile,
        rawPath: String,
        writer: PrintWriter
    ) {
        val contentType = headers["content-type"] ?: ""
        val contentLength = headers["content-length"]?.toLongOrNull() ?: 0L

        if (!contentType.contains("multipart/form-data")) {
            sendHtmlResponse(writer, "<h3>خطأ: صيغة الطلب غير مدعومة</h3>")
            return
        }

        val targetDirPath = rawPath.substringAfter("dir=", "/").ifEmpty { "/" }
        val targetDir = resolvePath(rootTree, targetDirPath) ?: rootTree

        try {
            var line: String?
            var filename = "uploaded_${System.currentTimeMillis()}"

            while (true) {
                line = readLine(inputStream) ?: break
                if (line.contains("filename=")) {
                    filename = line.substringAfter("filename=\"").substringBefore("\"")
                }
                if (line.isEmpty()) break
            }

            if (filename.isNotBlank()) {
                val mimeType = context.contentResolver.getType(Uri.parse(filename)) ?: "application/octet-stream"
                val newFile = targetDir.createFile(mimeType, filename)

                newFile?.let { createdFile ->
                    context.contentResolver.openOutputStream(createdFile.uri)?.use { fileOut ->
                        val buffer = ByteArray(8192)
                        var bytesRead: Int
                        var totalRead = 0L

                        while (totalRead < contentLength) {
                            bytesRead = inputStream.read(buffer, 0, buffer.size.coerceAtMost((contentLength - totalRead).toInt()))
                            if (bytesRead == -1) break
                            fileOut.write(buffer, 0, bytesRead)
                            totalRead += bytesRead
                        }
                    }
                }
            }

            writer.println("HTTP/1.1 302 Found")
            writer.println("Location: $targetDirPath")
            writer.println("Connection: close")
            writer.println()
            writer.flush()

        } catch (e: Exception) {
            sendHtmlResponse(writer, "<h3>فشل رفع الملف: ${e.localizedMessage}</h3>")
        }
    }

    private fun resolvePath(root: DocumentFile, path: String): DocumentFile? {
        if (path == "/" || path.isBlank()) return root
        val cleanPath = path.substringBefore("?")
        val segments = cleanPath.trim('/').split('/').filter { it.isNotBlank() }
        var current: DocumentFile = root

        for (segment in segments) {
            val next = current.listFiles().find { it.name == segment } ?: return null
            current = next
        }
        return current
    }

    private fun readLine(inputStream: InputStream): String? {
        val baos = ByteArrayOutputStream()
        var c: Int
        while (inputStream.read().also { c = it } != -1) {
            if (c == '\r'.code) {
                val next = inputStream.read()
                if (next == '\n'.code || next == -1) break
                baos.write(c)
                baos.write(next)
            } else if (c == '\n'.code) {
                break
            } else {
                baos.write(c)
            }
        }
        if (baos.size() == 0 && c == -1) return null
        return baos.toString("UTF-8")
    }

    private fun sendFileResponse(output: OutputStream, file: DocumentFile) {
        val mimeType = file.type ?: "application/octet-stream"
        val inputStream = context.contentResolver.openInputStream(file.uri) ?: return
        val writer = PrintWriter(output)

        writer.println("HTTP/1.1 200 OK")
        writer.println("Content-Type: $mimeType")
        writer.println("Content-Length: ${file.length()}")
        writer.println("Content-Disposition: inline; filename=\"${file.name}\"")
        writer.println("Connection: close")
        writer.println()
        writer.flush()

        inputStream.use { input ->
            val buffer = ByteArray(8192)
            var bytesRead: Int
            while (input.read(buffer).also { bytesRead = it } != -1) {
                output.write(buffer, 0, bytesRead)
            }
            output.flush()
        }
    }

    private fun sendHtmlResponse(writer: PrintWriter, html: String) {
        writer.println("HTTP/1.1 200 OK")
        writer.println("Content-Type: text/html; charset=utf-8")
        writer.println("Content-Length: ${html.toByteArray(Charsets.UTF_8).size}")
        writer.println("Connection: close")
        writer.println()
        writer.println(html)
        writer.flush()
    }

    private fun send404(writer: PrintWriter) {
        val html = "<html><body style='background:#0F172A;color:#fff;'><h1>404 - العنصر غير موجود</h1></body></html>"
        sendHtmlResponse(writer, html)
    }

    private fun buildDirectoryListingPage(dir: DocumentFile, currentPath: String): String {
        val files = dir.listFiles()
        val itemsHtml = StringBuilder()

        if (currentPath != "/") {
            val parentPath = currentPath.substringBeforeLast('/', "").ifEmpty { "/" }
            itemsHtml.append("<li><a href=\"$parentPath\">📁 [المجلد الأعلى]</a></li>")
        }

        for (item in files) {
            val name = item.name ?: "Unknown"
            val relativeLink = if (currentPath.endsWith("/")) "$currentPath$name" else "$currentPath/$name"
            val icon = if (item.isDirectory) "📁" else "📄"
            val size = if (item.isFile) "(${item.length() / 1024} KB)" else ""
            itemsHtml.append("<li><a href=\"$relativeLink\">$icon $name</a> $size</li>")
        }

        return """
            <!DOCTYPE html>
            <html dir="rtl" lang="ar">
            <head>
                <meta charset="UTF-8">
                <meta name="viewport" content="width=device-width, initial-scale=1.0">
                <title>FlowNet Storage - ${dir.name}</title>
                <style>
                    body { font-family: system-ui, sans-serif; background: #0F172A; color: #F8FAFC; padding: 20px; margin: 0; }
                    .container { max-width: 800px; margin: 0 auto; }
                    h2 { color: #00E5FF; border-bottom: 1px solid #334155; padding-bottom: 10px; }
                    .upload-card { background: #1E293B; border: 1px solid #334155; padding: 16px; border-radius: 12px; margin-bottom: 20px; }
                    .upload-card input[type="file"] { margin-bottom: 10px; color: #94A3B8; }
                    .upload-card button { background: #00E5FF; color: #000; border: none; padding: 10px 20px; border-radius: 8px; font-weight: bold; cursor: pointer; }
                    ul { list-style: none; padding: 0; }
                    li { padding: 12px; border-bottom: 1px solid #1E293B; display: flex; align-items: center; justify-content: space-between; }
                    a { color: #38BDF8; text-decoration: none; font-size: 16px; }
                    a:hover { text-decoration: underline; }
                </style>
            </head>
            <body>
                <div class="container">
                    <h2>المجلد الحالي: ${dir.name}</h2>
                    
                    <div class="upload-card">
                        <form action="/upload?dir=$currentPath" method="POST" enctype="multipart/form-data">
                            <h3>⬆️ رفع ملف إلى هذا المجلد</h3>
                            <input type="file" name="file" required /><br/>
                            <button type="submit">بدء الرفع</button>
                        </form>
                    </div>

                    <ul>$itemsHtml</ul>
                </div>
            </body>
            </html>
        """.trimIndent()
    }

    private fun buildNoFolderSelectedPage(): String {
        return """
            <!DOCTYPE html>
            <html dir="rtl" lang="ar">
            <head>
                <meta charset="UTF-8">
                <title>FlowNet Storage</title>
                <style>
                    body { font-family: sans-serif; background: #0F172A; color: #F8FAFC; text-align: center; padding: 50px; }
                    h1 { color: #EF4444; }
                </style>
            </head>
            <body>
                <h1>لم يتم تحديد مجلد للمشاركة</h1>
                <p>يرجى اختيار مجلد من داخل تطبيق FlowNet لمنح الإذن بالرفع والاستعراض.</p>
            </body>
            </html>
        """.trimIndent()
    }

    fun stop() {
        isRunning = false
        try {
            serverSocket?.close()
            serverSocket = null
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}
