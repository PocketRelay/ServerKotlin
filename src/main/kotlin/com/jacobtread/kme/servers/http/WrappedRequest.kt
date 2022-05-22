package com.jacobtread.kme.servers.http

import com.jacobtread.kme.data.Data
import com.jacobtread.kme.servers.http.exceptions.InvalidParamException
import com.jacobtread.kme.servers.http.exceptions.InvalidQueryException
import com.jacobtread.xml.Node
import com.jacobtread.xml.XmlVersion
import io.netty.buffer.ByteBuf
import io.netty.buffer.Unpooled
import io.netty.handler.codec.http.*
import kotlinx.serialization.DeserializationStrategy
import kotlinx.serialization.SerializationException
import kotlinx.serialization.SerializationStrategy
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.net.URLDecoder
import java.nio.file.Files
import java.nio.file.Paths

class WrappedRequest(private val http: HttpRequest) {

    val method: HttpMethod get() = http.method()

    val tokens: List<String>

    val url: String
    private val queryString: String?

    private var _params: MutableMap<String, String>? = null
    private var _query: MutableMap<String, String>? = null

    private var responseStatus: HttpResponseStatus = HttpResponseStatus.NOT_FOUND
    private var responseBuffer: ByteBuf? = null
    private var responseHeaders: MutableMap<String, String>? = null
    private var contentType: String = "text;charset=UTF-8"

    init {
        val parts = http.uri().split('?', limit = 2)
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
                responseStatus,
                contentBuffer
            )
            contentLength = contentBuffer.readableBytes()
        } else {
            response = DefaultFullHttpResponse(
                HttpVersion.HTTP_1_1,
                responseStatus
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

    private fun parseQuery(): HashMap<String, String>? {
        if (queryString == null) return null
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

    fun setParam(key: String, value: String) {
        if (_params == null) _params = HashMap()
        _params!![key] = value
    }

    fun param(key: String): String = _params?.get(key) ?: throw InvalidParamException(key)
    fun paramInt(key: String, radix: Int): Int = param(key).toIntOrNull(radix) ?: throw InvalidParamException(key)

    fun query(key: String): String {
        if (_query == null) _query = parseQuery()
        return _query?.get(key) ?: throw InvalidQueryException(key)
    }

    fun queryOrNull(key: String): String? {
        if (_query == null) _query = parseQuery()
        return _query?.get(key)
    }

    fun queryInt(key: String, radix: Int = 10): Int = query(key).toIntOrNull(radix) ?: throw InvalidQueryException(key)
    fun queryInt(key: String, default: Int, radix: Int = 10): Int = queryOrNull(key)?.toIntOrNull(radix) ?: default

    fun setHeader(key: String, value: String) {
        var headers = this.responseHeaders
        if (headers == null) headers = HashMap()
        headers[key] = value
        this.responseHeaders = headers
    }

    fun xml(content: Node) {
        responseStatus = HttpResponseStatus.OK
        contentType = "text/xml;charset=UTF-8"
        responseBuffer = Unpooled.copiedBuffer(content.toString(false), Charsets.UTF_8)
    }

    inline fun xml(root: String, init: Node.() -> Unit) {
        val rootNode = Node(root)
        rootNode.encoding = "UTF-8"
        rootNode.version = XmlVersion.V10
        rootNode.init()
        xml(rootNode)
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
            responseStatus = HttpResponseStatus.OK
            responseBuffer = Unpooled.copiedBuffer(Json.encodeToString(serializer, value), Charsets.UTF_8)
        } catch (e: SerializationException) {
            responseStatus = HttpResponseStatus.INTERNAL_SERVER_ERROR
            responseBuffer = null
        }
    }

    fun response(status: HttpResponseStatus, content: ByteBuf? = null) {
        responseStatus = status
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
        responseStatus = HttpResponseStatus.OK
        this.contentType = contentType
        responseBuffer = Unpooled.wrappedBuffer(content)
    }

    fun text(content: String, contentType: String = this.contentType) {
        responseStatus = HttpResponseStatus.OK
        this.contentType = contentType
        responseBuffer = Unpooled.copiedBuffer(content, Charsets.UTF_8)
    }

    fun html(content: String) {
        responseStatus = HttpResponseStatus.OK
        this.contentType = "text/html;charset=UTF-8"
        responseBuffer = Unpooled.copiedBuffer(content, Charsets.UTF_8)
    }

    fun static(
        fileName: String,
        path: String,
        fallback: String = "404.html",
        fallbackPath: String = "public",
    ) {
        if (fileName.isEmpty()) {
            return static(fallback, fallbackPath)
        } else {
            val resource = Data.getResourceOrNull("$path/$fileName")
            if (resource == null) {
                responseStatus = HttpResponseStatus.NOT_FOUND
                if (fileName != fallback) static(fallback, fallbackPath)
            } else {
                responseStatus = HttpResponseStatus.OK
                contentType = if (fileName.endsWith(".js")) {
                    "text/javascript"
                } else if (fileName.endsWith(".css")) {
                    "text/css"
                } else if (fileName.endsWith(".html")) {
                    "text/html"
                } else {
                    Files.probeContentType(Paths.get(fileName))
                }
                responseBuffer = Unpooled.wrappedBuffer(resource)
            }
        }
    }
}