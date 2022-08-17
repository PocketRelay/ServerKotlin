package com.jacobtread.relay.data

import com.jacobtread.relay.Environment
import java.io.InputStream

/**
 * Retrieves an input stream to an internal resource
 * inside the server jar file relative to the  /data
 * directory
 *
 * @param path The relative path to the file
 * @return The input stream or null
 */
fun getDataStream(path: String): InputStream? {
    return Environment::class.java.getResourceAsStream("/data/$path")
}

/**
 * Retrieves the text contents of a file stored in
 * /data or null if the file contents couldn't be
 * read or didn't exist
 *
 * @param path The relative path to the file
 * @return The file contents as a string or null
 */
fun getTextData(path: String): String? {
    val stream = getDataStream(path) ?: return null
    val bytes = stream.readAllBytes()
    return String(bytes, Charsets.UTF_8)
}

/**
 * Map resources are maps of data stored in key value
 * pairs seperated by an equals sign. These MUST retain
 * the order they were written in.
 *
 * @param path The path to the map resource
 * @return The map loaded from the resource or null on failure
 */
fun getMapData(path: String): Map<String, String>? {
    val out = LinkedHashMap<String, String>()
    val resourceStream = getDataStream(path) ?: return null
    val reader = resourceStream.bufferedReader(Charsets.UTF_8)
    reader.forEachLine {
        val parts = it.split('=', limit = 2)
        if (parts.size == 2) {
            out[parts[0]] = parts[1]
        }
    }
    return out
}
