package com.jacobtread.kme.servers.http.router

import com.jacobtread.kme.blaze.utils.copiedBuffer
import com.jacobtread.kme.data.Data
import com.jacobtread.xml.Node
import com.jacobtread.xml.XmlVersion
import io.netty.buffer.ByteBuf
import io.netty.buffer.Unpooled
import io.netty.handler.codec.http.DefaultFullHttpResponse
import io.netty.handler.codec.http.HttpHeaderNames
import io.netty.handler.codec.http.HttpResponseStatus
import io.netty.handler.codec.http.HttpVersion
import kotlinx.serialization.SerializationException
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

typealias RequestResponse = DefaultFullHttpResponse

fun RequestResponse.setHeaders(headers: Map<String, String>): RequestResponse {
    val httpHeaders = headers()
    headers.forEach { (key, value) -> httpHeaders.add(key, value) }
    return this
}

fun RequestResponse.setHeader(key: String, value: String): RequestResponse {
    val httpHeaders = headers()
    httpHeaders.add(key, value)
    return this
}

fun Node.createBuffer(): ByteBuf = toString(false).copiedBuffer()

inline fun responseXml(rootName: String, init: Node.() -> Unit): RequestResponse {
    val rootNode = Node(rootName)
    rootNode.encoding = "UTF-8"
    rootNode.version = XmlVersion.V10
    rootNode.init()
    return response(HttpResponseStatus.OK, "text/xml;charset=UTF-8", rootNode.createBuffer())
}

fun responseBytes(bytes: ByteArray, contentType: String? = null): RequestResponse =
    response(HttpResponseStatus.OK, contentType, Unpooled.wrappedBuffer(bytes))

fun responseText(content: String, contentType: String = "text/plain;charset=UTF-8"): RequestResponse =
    response(HttpResponseStatus.OK, contentType, contentType.copiedBuffer())

fun responseHtml(content: String): RequestResponse =
    response(HttpResponseStatus.OK, "text/html;charset=UTF-8")

fun responseStatic(
    fileName: String,
    path: String,
    fallback: String = "404.html",
    fallbackPath: String = "public",
): RequestResponse {
    if (fileName.isBlank()) return responseStatic(fallback, fallbackPath)
    val resource = Data.getResourceOrNull("$path/$fileName")
    if (resource == null) {
        if (fileName != fallback) return responseStatic(fallback, fallbackPath)
        return response()
    }
    val contentType: String = when (fileName.substringAfterLast('.')) {
        "js" -> "text/javascript"
        "css" -> "text/css"
        "html" -> "text/html"
        else -> "text"
    }
    val buffer = Unpooled.wrappedBuffer(resource)
    return response(HttpResponseStatus.OK, contentType, buffer)
}

inline fun <reified V> responseJson(value: V): DefaultFullHttpResponse {
    return try {
        val content = Json.encodeToString(value).copiedBuffer()
        response(HttpResponseStatus.OK, "application/json;charset=UTF-8", content)
    } catch (e: SerializationException) {
        response(HttpResponseStatus.INTERNAL_SERVER_ERROR)
    }
}

fun response(
    status: HttpResponseStatus = HttpResponseStatus.NOT_FOUND,
    contentType: String? = null,
    content: ByteBuf = Unpooled.EMPTY_BUFFER,
): DefaultFullHttpResponse {
    val out = DefaultFullHttpResponse(HttpVersion.HTTP_1_1, status, content)
    val contentLength = content.readableBytes()
    val httpHeaders = out.headers()
    // Content description headers
    if (contentType != null) httpHeaders.add(HttpHeaderNames.CONTENT_TYPE, contentType)
    httpHeaders.add(HttpHeaderNames.CONTENT_LENGTH, contentLength)
    // CORS so that requests can be accessed in the browser
    httpHeaders.add(HttpHeaderNames.ACCESS_CONTROL_ALLOW_ORIGIN, "*")
    httpHeaders.add(HttpHeaderNames.ACCESS_CONTROL_ALLOW_HEADERS, "*")
    httpHeaders.add(HttpHeaderNames.ACCESS_CONTROL_ALLOW_METHODS, "*")
    return out
}