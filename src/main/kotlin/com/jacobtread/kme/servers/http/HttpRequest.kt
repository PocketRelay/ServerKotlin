package com.jacobtread.kme.servers.http

import com.jacobtread.kme.servers.http.router.BadRequestException
import io.netty.handler.codec.http.FullHttpRequest
import io.netty.handler.codec.http.HttpMethod
import kotlinx.serialization.SerializationException
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json
import java.net.URLDecoder
import io.netty.handler.codec.http.HttpRequest as NettyHttpRequest

class HttpRequest(private val http: NettyHttpRequest) {

    private var _params: HashMap<String, String>? = null
    private val params: HashMap<String, String>
        get() {
            if (_params == null) _params = HashMap()
            return _params!!
        }


    val method: HttpMethod get() = http.method()

    val tokens: List<String>
    val url: String
    private val query: Map<String, String>

    init {
        val parts = http.uri().split('?', limit = 2)
        val url = parts[0].removePrefix("/").removeSuffix("/")
        this.url = url
        this.tokens = url.split('/')
        query = if (parts.size < 2) {
            emptyMap()
        } else {
            createQueryMap(parts[1])
        }
    }


    private fun createQueryMap(queryString: String): Map<String, String> {
        if (queryString.isEmpty()) return emptyMap()
        val query = HashMap<String, String>()
        queryString.split('&').forEach { keyValue ->
            val parts = keyValue.split('=', limit = 2)
                .map { URLDecoder.decode(it, Charsets.UTF_8) }
            if (parts.size == 2) {
                query[parts[0]] = parts[1]
            } else if (parts.size == 1) {
                query[parts[0]] = ""
            }
        }
        return query
    }

    fun setParam(key: String, value: String) {
        params[key] = value
    }

    fun param(key: String): String = params[key] ?: throw BadRequestException()
    fun paramInt(key: String, radix: Int): Int = param(key).toIntOrNull(radix) ?: throw BadRequestException()
    fun queryInt(key: String, radix: Int = 10): Int = query[key]?.toIntOrNull(radix) ?: throw BadRequestException()
    fun queryInt(key: String, default: Int, radix: Int = 10): Int = query[key]?.toIntOrNull(radix) ?: default

    fun contentBytes(): ByteArray {
        if (http !is FullHttpRequest) throw BadRequestException()
        val contentBuffer = http.content()
        val bytes = ByteArray(contentBuffer.readableBytes())
        contentBuffer.readBytes(bytes)
        return bytes
    }

    fun contentString(): String = contentBytes().decodeToString()

    inline fun <reified V> contentJson(): V = try {
        Json.decodeFromString(contentString())
    } catch (e: SerializationException) {
        throw BadRequestException()
    }

}