import com.jacobtread.kme.utils.BigEndian
import java.io.ByteArrayOutputStream
import java.nio.file.Paths
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

    val final =
        BigEndian.uint32ToBytes(1) +
                BigEndian.uint32ToBytes(gc.size) +
                BigEndian.uint32ToBytes(contents.size) +
                gc

    val base64 = Base64.getEncoder().encodeToString(final)

    val chunks = base64.chunked(255)

    val outBuilder = StringBuilder()

    chunks.forEachIndexed { index, value ->
        outBuilder.append("CHUNK_")
            .append(index)
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