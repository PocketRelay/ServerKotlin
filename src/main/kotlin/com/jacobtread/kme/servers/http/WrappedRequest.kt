package com.jacobtread.kme.servers.http

import com.jacobtread.kme.data.Data
import io.netty.buffer.ByteBuf
import io.netty.buffer.Unpooled
import io.netty.handler.codec.http.*
import kotlinx.serialization.DeserializationStrategy
import kotlinx.serialization.SerializationException
import kotlinx.serialization.SerializationStrategy
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.redundent.kotlin.xml.Node
import org.redundent.kotlin.xml.PrintOptions
import java.net.URLDecoder
import java.nio.file.Files
import java.nio.file.Paths

class WrappedRequest(private val http: HttpRequest) {

    companion object {
        private val XML_PRINT_OPTIONS = PrintOptions(singleLineTextElements = true, pretty = false)
    }

    val method: HttpMethod get() = http.method()

    val tokens: List<String>
    val params = HashMap<String, String>()

    val url: String
    private val queryString: String?

    private var _query: Map<String, String>? = null

    private var responseCode: HttpResponseStatus = HttpResponseStatus.NOT_FOUND
    private var responseBuffer: ByteBuf? = null
    private var responseHeaders: MutableMap<String, String>? = null
    private var contentType: String = "text;charset=UTF-8"

    init {
        val parts = http.uri().split('?', limit = 1)
        val url = parts[0].removePrefix("/").removeSuffix("/")
        this.url = url
        this.tokens = url.split('/')
        queryString = if (parts.size > 1) parts[1] else null
    }

    fun createResponse(): DefaultFullHttpResponse {
        val response: DefaultFullHttpResponse
        val contentLength: Int
        val contentBuffer = responseBuffer
        if (contentBuffer != null) {
            response = DefaultFullHttpResponse(
                HttpVersion.HTTP_1_1,
                responseCode,
                contentBuffer
            )
            contentLength = contentBuffer.readableBytes()
        } else {
            response = DefaultFullHttpResponse(
                HttpVersion.HTTP_1_1,
                responseCode
            )
            contentLength = 0
        }
        val headers = response.headers()
        // Content description headers
        headers.add(HttpHeaderNames.CONTENT_TYPE, contentType)
        headers.add(HttpHeaderNames.CONTENT_LENGTH, contentLength)
        // CORS so that requests can be accessed in the browser
        headers.add(HttpHeaderNames.ACCESS_CONTROL_ALLOW_ORIGIN, "*")
        responseHeaders?.forEach { headers.add(it.key, it.value) }
        return response
    }

    private fun parseQuery(): Map<String, String> {
        if (queryString == null) return emptyMap()
        val out = HashMap<String, String>()
        queryString.split('&').forEach { keyValue ->
            val parts = keyValue.split('=', limit = 2)
            if (parts.size > 1) {
                out[URLDecoder.decode(parts[0], Charsets.UTF_8)] = URLDecoder.decode(parts[1], Charsets.UTF_8)
            } else {
                out[URLDecoder.decode(parts[0], Charsets.UTF_8)] = ""
            }
        }
        return out
    }

    fun query(key: String): String? {
        if (_query == null) _query = parseQuery()
        return _query!![key]
    }

    fun setHeader(key: String, value: String) {
        var headers = this.responseHeaders
        if (headers == null) headers = HashMap()
        headers[key] = value
        this.responseHeaders = headers
    }

    fun xml(content: Node) {
        responseCode = HttpResponseStatus.OK
        contentType = "text/xml;charset=UTF-8"
        responseBuffer = Unpooled.copiedBuffer(content.toString(XML_PRINT_OPTIONS), Charsets.UTF_8)
    }

    fun <V> contentJson(serializer: DeserializationStrategy<V>): V? {
        if (http !is FullHttpRequest) return null
        val contentBuffer = http.content()
        val bytes = ByteArray(contentBuffer.readableBytes())
        contentBuffer.readBytes(bytes)
        contentBuffer.release()
        return try {
            Json.decodeFromString(serializer, bytes.decodeToString())
        } catch (e: SerializationException) {
            null
        }
    }

    fun <V> json(serializer: SerializationStrategy<V>, value: V) {
        try {
            responseCode = HttpResponseStatus.OK
            responseBuffer = Unpooled.copiedBuffer(Json.encodeToString(serializer, value), Charsets.UTF_8)
        } catch (e: SerializationException) {
            responseCode = HttpResponseStatus.INTERNAL_SERVER_ERROR
            responseBuffer = null
        }
    }

    fun response(code: HttpResponseStatus, content: ByteBuf?) {
        responseCode = code
        responseBuffer = content
    }

    inline fun <reified V> json(value: V) {
        try {
            response(HttpResponseStatus.OK, Unpooled.copiedBuffer(Json.encodeToString(value), Charsets.UTF_8))
        } catch (e: SerializationException) {
            response(HttpResponseStatus.INTERNAL_SERVER_ERROR, null)
        }
    }

    fun bytes(content: ByteArray, contentType: String) {
        responseCode = HttpResponseStatus.OK
        this.contentType = contentType
        responseBuffer = Unpooled.wrappedBuffer(content)
    }

    fun text(content: String, contentType: String = this.contentType) {
        responseCode = HttpResponseStatus.OK
        this.contentType = contentType
        responseBuffer = Unpooled.copiedBuffer(content, Charsets.UTF_8)
    }

    fun html(content: String) {
        responseCode = HttpResponseStatus.OK
        this.contentType = "text/html;charset=UTF-8"
        responseBuffer = Unpooled.copiedBuffer(content, Charsets.UTF_8)
    }

    fun static(fileName: String, path: String) {
        val resource = Data.getResourceOrNull("$path/$fileName")
        if (resource == null) {
            responseCode = HttpResponseStatus.NOT_FOUND
        } else {
            responseCode = HttpResponseStatus.OK
            contentType = if (fileName.endsWith(".js")) {
                "text/javascript"
            } else if (fileName.endsWith(".css")) {
                "text/css"
            } else {
                Files.probeContentType(Paths.get(fileName))
            }
            responseBuffer = Unpooled.wrappedBuffer(resource)
        }
    }
}