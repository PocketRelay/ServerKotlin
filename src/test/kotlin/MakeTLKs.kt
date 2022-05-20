import java.util.*
import kotlin.io.path.*


fun main() {
    val tlkDir = Path("data/tlk")
    val outDir = tlkDir / "processed"
    if(!outDir.exists()) outDir.createDirectories()
    tlkDir.forEachDirectoryEntry {
        val fileName= it.fileName.toString()
        if (fileName.endsWith(".tlk")) {
            val newName = when(fileName) {
                "ME3TLK.tlk" -> "default.tlk.chunked"
                else -> {
                    val locale = fileName.substring(7, fileName.length - 4)
                    "$locale.tlk.chunked"
                }
            }
            val outFile = outDir / newName
            val contents = it.readBytes()
            val base64 = Base64.getEncoder().encodeToString(contents)
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
            if (!outFile.exists()) outFile.createFile()
            outFile.writeText(outBuilder.toString().dropLast(1))
        }
    }
}