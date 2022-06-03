package com.jacobtread.kme.servers.http

import com.jacobtread.kme.data.Data
import com.jacobtread.kme.servers.http.exceptions.InvalidParamException
import com.jacobtread.kme.servers.http.exceptions.InvalidQueryException
import com.jacobtread.xml.Node
import com.jacobtread.xml.XmlVersion
import io.netty.buffer.ByteBuf
import io.netty.buffer.Unpooled
import io.netty.handler.codec.http.*
import kotlinx.serialization.*
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

    init {
        val parts = http.uri().split('?', limit = 2)
        val url = parts[0].removePrefix("/").removeSuffix("/")
        this.url = url
        this.tokens = url.split('/')
        queryString = if (parts.size > 1) parts[1] else null
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

    fun contentsToBytes(): ByteArray? {
        if (http !is FullHttpRequest) return null
        val contentBuffer = http.content()
        val bytes = ByteArray(contentBuffer.readableBytes())
        contentBuffer.readBytes(bytes)
        return bytes
    }


    inline fun <reified V> contentJson(): V? {
        val contents = contentsToBytes() ?: return null
        return try {
            Json.decodeFromString<V>(contents.decodeToString())
        } catch (e: SerializationException) {
            null
        }
    }
}