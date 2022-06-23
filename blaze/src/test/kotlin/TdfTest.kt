import com.jacobtread.kme.blaze.tdf.StringTdf
import com.jacobtread.kme.blaze.tdf.Tdf
import com.jacobtread.kme.blaze.tdf.VarIntTdf
import io.netty.buffer.ByteBuf
import io.netty.buffer.Unpooled
import kotlin.random.Random
import kotlin.random.nextInt
import kotlin.random.nextULong
import kotlin.test.Test
import kotlin.test.assertEquals

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

        private const val ABC = "abcdefghijklmnopqrstuvwxyz"
        private const val NUMBERS = "0123456789"
        private const val SYMBOLS = "\\/[]{}()_+!`~@#\$%^&*<>,.?;'\":|"
        private val CHARS: CharArray = (ABC + ABC.uppercase() + NUMBERS + SYMBOLS).toCharArray()

        fun createRandomString(length: Int): String {
            return buildString { repeat(length) { append(CHARS.random()) } }
        }

        fun testTdf(buffer: ByteBuf, tdf: Tdf<*>) {
            tdf.writeFully(buffer)
            val readTdf = Tdf.read(buffer)
            assertEquals(tdf, readTdf)
        }

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
        repeat(ITERATIONS) {
            // Reset the buffer position
            buffer.discardReadBytes()
            // Select a random value for the tdf
            val value = Random.nextULong(minValue..maxValue)
            val tdf = VarIntTdf(LABEL, value)
            testTdf(buffer, tdf)
        }
    }

    /**
     * test string Tests the string encoding against
     * a randomized string between 0 and 1000 characters
     * long and ensures the output is always the same as
     * the input
     */
    @Test
    fun `test string`() {
        val minLength = 0
        val maxLength = 1000

        val buffer = Unpooled.buffer()
        repeat(ITERATIONS) {
            // Reset the buffer position
            buffer.discardReadBytes()
            val valueLength = Random.nextInt(minLength..maxLength)
            val value = createRandomString(valueLength)
            val tdf = StringTdf(LABEL, value)
            testTdf(buffer, tdf)
        }
    }

}