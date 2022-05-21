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
//            val outBuilder = StringBuilder()
//            chunks.forEachIndexed { index, value ->
//                outBuilder.append("CHUNK_")
//                    .append(index)
//                    .append(":")
//                    .append(value)
//                    .append('\n')
//            }
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
                    if (keys[i].compareTo(keys[i + 1]) > 0) {
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
            if (!outFile.exists()) outFile.createFile()
            outFile.writeText(outBuilder.toString().dropLast(1))
        }
    }
}