import com.jacobtread.kme.blaze.tdf.Tdf
import com.jacobtread.kme.blaze.tdf.VarIntTdf
import io.netty.buffer.Unpooled
import kotlin.random.Random
import kotlin.random.nextULong
import kotlin.test.Test

/**
 * TdfTest JUnit tests for TDF functionality to ensure
 * that TDFs can be both encoded and decoded to produce
 * the same value
 *
 * @constructor Create empty TdfTest
 */
internal class TdfTest {

    companion object {
        const val LABEL = "TEST"
        const val ITERATIONS = 100
    }

    /**
     * test varint Tests the VarInt encoding against
     * a variety of different number values ensuring
     * that the output is always the same as the input
     */
    @Test
    fun `test varint`() {
        val maxValue = ULong.MAX_VALUE
        val minValue = ULong.MIN_VALUE

        val buffer = Unpooled.buffer()
        for (i in 0 until ITERATIONS) {
            // Reset the buffer position
            buffer.setIndex(0, 0)
            // Select a random value for the tdf
            val value = Random.nextULong(minValue..maxValue)
            val tdf = VarIntTdf(LABEL, value)
            tdf.writeFully(buffer)

            val readTdf = Tdf.read(buffer)
            assert(readTdf.label == tdf.label)
            assert(readTdf is VarIntTdf)
            assert(readTdf.value == tdf.value)
        }
    }

}