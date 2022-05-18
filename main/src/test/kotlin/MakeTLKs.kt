import java.util.*
import kotlin.io.path.*


fun main() {
    val tlkDir = Path("tlk")
    val outDir = tlkDir / "processed"
    if(!outDir.exists()) outDir.createDirectories()
    tlkDir.forEachDirectoryEntry {
        val fileName= it.fileName.toString()
        if (fileName.endsWith(".tlk")) {
            val outFile = outDir / (fileName.dropLast(3) + "txt")
            val contents = it.readBytes()
            val base64 = Base64.getEncoder().encodeToString(contents)
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
            if (!outFile.exists()) outFile.createFile()
            outFile.writeText(outBuilder.toString().dropLast(1))
        }
    }
}