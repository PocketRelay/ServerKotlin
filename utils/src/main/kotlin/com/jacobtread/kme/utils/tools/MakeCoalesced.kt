import com.jacobtread.kme.utils.tools.ResourceProcessing
import kotlin.io.path.Path
import kotlin.io.path.createFile
import kotlin.io.path.exists

/**
 * main function for converting the ME3BINI into the compressed
 * and base64 version, so it doesn't need to happen at runtime
 *
 */
fun main() {
    val inFile = Path("data/bini.bin")
    val outFile = Path("data/bini.bin.chunked")
    ResourceProcessing.processCoalesced(inFile, outFile)
}