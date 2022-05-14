import com.jacobtread.kme.blaze.utils.BigEndian
import com.jacobtread.kme.data.Data
import java.io.ByteArrayOutputStream
import java.nio.file.Paths
import java.util.*
import java.util.zip.GZIPOutputStream
import kotlin.io.path.createFile
import kotlin.io.path.exists
import kotlin.io.path.writeText

/**
 * main Small function for converting the ME3BINI into the compressed
 * and base64 version so it doesn't need to happen at runtime
 *
 */
fun main() {
    val contents = Data.getResource("data/bini.bin")
    val gc: ByteArray = ByteArrayOutputStream()
        .apply {
            val gz = GZIPOutputStream(this)
            gz.write(contents)
            gz.flush()
        }
        .toByteArray()
    val final: ByteArray = ByteArrayOutputStream(gc.size + 16)
        .apply {
            write(BigEndian.uint32ToBytes(1))
            write(BigEndian.uint32ToBytes(gc.size))
            write(BigEndian.uint32ToBytes(contents.size))
            write(gc)
            flush()
        }
        .toByteArray()
    val base64 = Base64.getEncoder().encodeToString(final)

    val chunks = base64.chunked(255)

    val outBuilder = StringBuilder()

    outBuilder.append("SIZE:")
        .append(base64.length)
        .append('\n')

    chunks.forEachIndexed { index, value ->
        outBuilder.append("CHUNK_")
            .append(index)
            .append(":")
            .append(value)
            .append('\n')
    }


    val outFile = Paths.get("run/data/bini.txt")
    if (!outFile.exists()) outFile.createFile()
    outFile.writeText(outBuilder.toString().dropLast(1))
}