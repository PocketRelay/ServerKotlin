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
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

const val XML_CONTENT_TYPE = "text/xml;charset=UTF-8"
const val PLAIN_TEXT_CONTENT_TYPE = "text/plain;charset=UTF-8"
const val HTML_CONTENT_TYPE = "text/html;charset=UTF-8"
const val JSON_CONTENT_TYPE = "application/json;charset=UTF-8"

/**
 * Type alias for a full http response this is type aliased because it
 * is the only type of http response used throughout the application, and
 * it's much shorter than writing DefaultFullHttpResponse everywhere
 */
typealias HttpResponse = DefaultFullHttpResponse

/**
 * setHeaders Sets a map of key value pairs as headers on the request
 * that it was invoked upon
 *
 * @param headers The map of headers to set
 * @return The response that this was called on (So that it can be used like a builder)
 */
fun HttpResponse.setHeaders(headers: Map<String, String>): HttpResponse {
    val httpHeaders = headers()
    headers.forEach { (key, value) -> httpHeaders.add(key, value) }
    return this
}

/**
 * setHeader Sets a header on the headers list for
 * the request  that it was invoked upon
 *
 * @param key The key of the header to set
 * @param value The value of the header to set
 * @return The response that this was called on (So that it can be used like a builder)
 */
fun HttpResponse.setHeader(key: String, value: String): HttpResponse {
    val httpHeaders = headers()
    httpHeaders.add(key, value)
    return this
}

/**
 * responseXml Creates an XML response that initializes the xml node tree
 * using the provided init function which is passed the root node with the
 * provided node name as the receiver
 *
 * Automatically sets the encoding and XML version for the root node so
 * that that information is included in the built xml
 *
 * @param rootName The name of the root node
 * @param init The initializer function
 * @receiver The newly created root node
 * @return The HttpResponse created from the encoded XML
 */
inline fun responseXml(rootName: String, init: Node.() -> Unit): HttpResponse {
    val rootNode = Node(rootName)
    rootNode.encoding = "UTF-8"
    rootNode.version = XmlVersion.V10
    rootNode.init()
    return response(HttpResponseStatus.OK, rootNode.toString(false).copiedBuffer(), XML_CONTENT_TYPE)
}

/**
 * responseBytes Creates a raw bytes' response with an optional
 * content type parameter
 *
 * @param bytes The raw byte array contents of this response
 * @param contentType The optional content type of this response null for none
 * @return The created HttpResponse from the bytes
 */
fun responseBytes(bytes: ByteArray, contentType: String? = null): HttpResponse =
    response(HttpResponseStatus.OK, Unpooled.wrappedBuffer(bytes), contentType)

/**
 * responseText Creates a text response with an optional
 * content type parameter by default this is text/plain
 *
 * @param content The string contents to use for the response
 * @param contentType The optional content type of the response
 * @return The created HttpResponse from the text
 */
fun responseText(content: String, contentType: String = PLAIN_TEXT_CONTENT_TYPE): HttpResponse =
    response(HttpResponseStatus.OK, content.copiedBuffer(), contentType)

/**
 * responseHtml Creates a html response with the content
 * type of text/html from the provided content string
 *
 * @param content The html content
 * @return The created HttpResponse from the html
 */
fun responseHtml(content: String): HttpResponse =
    response(HttpResponseStatus.OK, content.copiedBuffer(), HTML_CONTENT_TYPE)

/**
 * responseStatic Creates a response from a static file stored inside the
 * jar resources if the file doesn't exist or is not a valid file name then
 * the fallback file and path will be used instead. If the fallback is also
 * invalid an empty response with the NOT_FOUND status will be used instead
 *
 * @param fileName The name of the file to respond with
 * @param path The root path of the file in the resources directory
 * @param fallbackFileName The fallback file name to use if the file was not found
 * @param fallbackPath The fallback path to use if the fallback name is used
 * @return The created HttpResponse from the file
 */
fun responseStatic(
    fileName: String,
    path: String,
    fallbackFileName: String = "404.html",
    fallbackPath: String = "public",
): HttpResponse {
    if (fileName.isBlank()) return responseStatic(fallbackFileName, fallbackPath)
    val resource = Data.getResourceOrNull("$path/$fileName")
    if (resource == null) {
        if (fileName != fallbackFileName) return responseStatic(fallbackFileName, fallbackPath)
        return response(HttpResponseStatus.NOT_FOUND)
    }
    val contentType: String = when (fileName.substringAfterLast('.')) {
        "js" -> "text/javascript"
        "css" -> "text/css"
        "html" -> HTML_CONTENT_TYPE
        else -> PLAIN_TEXT_CONTENT_TYPE
    }
    val buffer = Unpooled.wrappedBuffer(resource)
    return response(HttpResponseStatus.OK, buffer, contentType)
}

/**
 * responseJson Creates a JSON serialized response from
 * the provided value if a serialization exception is
 * thrown it will be caught by the router which will
 * send a INTERNAL_SERVER_ERROR instead of the response
 *
 * @param V The type of the object to serialize
 * @param value The object to serialize
 * @return The created HttpResponse
 */
inline fun <reified V> responseJson(value: V): HttpResponse {
    val content = Json.encodeToString(value).copiedBuffer()
    return response(HttpResponseStatus.OK, content, JSON_CONTENT_TYPE)
}

/**
 * response Creates a http response from the provided status, content
 * and optional content type. Adds the appropriate headers for content
 * type and length as well as the cross-origin access control headers
 * so that browsers can make POST requests to the server
 *
 * @param status The status of the http response
 * @param content The content to use for the response empty buffer by default
 * @param contentType The type of the content stored in the buffer
 * @return The created HttpResponse
 */
fun response(
    status: HttpResponseStatus,
    content: ByteBuf = Unpooled.EMPTY_BUFFER,
    contentType: String? = null,
): HttpResponse {
    val out = HttpResponse(HttpVersion.HTTP_1_1, status, content)
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