import io.netty.buffer.Unpooled
import java.io.ByteArrayOutputStream
import java.util.*
import java.util.zip.Deflater
import kotlin.io.path.*

/**
 * main function for converting the ME3BINI into the compressed
 * and base64 version, so it doesn't need to happen at runtime
 *
 */
fun main() {
    val contents = Path("data/bini.bin").readBytes()
    val deflater = Deflater()
    deflater.setLevel(6)
    deflater.setInput(contents)

    val gc: ByteArray = ByteArrayOutputStream(contents.size).use { outputStream ->
        deflater.finish()
        val buffer = ByteArray(1024)
        while (!deflater.finished()) {
            val count = deflater.deflate(buffer)
            outputStream.write(buffer, 0, count)
        }
        outputStream.toByteArray()
    }
    val underlying = ByteArray(16 + gc.size)
    val buffer = Unpooled.wrappedBuffer(underlying)
    buffer.writerIndex(0)
    buffer.writeByte('N'.code)
    buffer.writeByte('I'.code)
    buffer.writeByte('B'.code)
    buffer.writeByte('C'.code)
    buffer.writeIntLE(1)
    buffer.writeIntLE(gc.size)
    buffer.writeIntLE(contents.size)
    buffer.writeBytes(gc)

    val base64 = Base64.getEncoder().encodeToString(underlying)

    val chunks = base64.chunked(255)

    val keys = ArrayList<String>(chunks.size)
    val values = ArrayList<String>(chunks.size)

    chunks.forEachIndexed { index, value ->
        keys.add("CHUNK_$index")
        values.add(value)
    }

    var run = true
    while (run) {
        run = false
        var tmp: String
        for (i in 0 until keys.size - 1) {
            if (keys[i] > keys[i + 1]) {
                tmp = keys[i]
                keys[i] = keys[i + 1]
                keys[i + 1] = tmp
                tmp = values[i]
                values[i] = values[i + 1]
                values[i + 1] = tmp
                run = true
            }
        }
    }


    val outBuilder = StringBuilder()

    for (i in 0 until keys.size) {
        val key = keys[i]
        val value = values[i]
        outBuilder
            .append(key)
            .append(":")
            .append(value)
            .append('\n')
    }


    outBuilder.append("CHUNK_SIZE:255\nDATA_SIZE:")
        .append(base64.length)
        .append('\n')
    val outFile = Path("data/bini.bin.chunked")
    if (!outFile.exists()) outFile.createFile()
    outFile.writeText(outBuilder.toString().dropLast(1))
}