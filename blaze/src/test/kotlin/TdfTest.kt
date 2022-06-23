import com.jacobtread.kme.blaze.tdf.*
import com.jacobtread.kme.utils.VarPair
import com.jacobtread.kme.utils.VarTripple
import io.netty.buffer.Unpooled
import kotlin.random.Random
import kotlin.random.nextInt
import kotlin.random.nextULong
import kotlin.test.Test
import kotlin.test.assertEquals

typealias Generator<T> = () -> T

/**
 * TdfTest JUnit tests for TDF functionality to ensure
 * that TDFs can be both encoded and decoded to produce
 * the same value
 *
 * @constructor Create empty TdfTest
 */
internal class TdfTest {

    companion object {
        const val ITERATIONS = 100

        private const val ABC = "abcdefghijklmnopqrstuvwxyz"
        private const val NUMBERS = "0123456789"
        private const val SYMBOLS = "\\/[]{}()_+!`~@#\$%^&*<>,.?;'\":|"
        private val CHARS: CharArray = (ABC + ABC.uppercase() + NUMBERS + SYMBOLS).toCharArray()
        private val BUFFER = Unpooled.buffer();

        fun createRandomString(length: Int): String {
            return buildString { repeat(length) { append(CHARS.random()) } }
        }

        val LabelGen: Generator<String> = {
            val possibleChars = ABC.uppercase().toCharArray() + Char.MIN_VALUE
            val chars = CharArray(4) { possibleChars.random() }
            chars.toString()
        }

        private val ULongGen: Generator<ULong> = {
            val maxValue = ULong.MAX_VALUE
            val minValue = ULong.MIN_VALUE
            Random.nextULong(minValue..maxValue)
        }
        private val StringGen: Generator<String> = {
            val minLength = 0
            val maxLength = 1000
            val length = Random.nextInt(minLength..maxLength)
            createRandomString(length)
        }
        private val TrippleGen: Generator<VarTripple> = { VarTripple(ULongGen(), ULongGen(), ULongGen()) }
        private val PairGen: Generator<VarPair> = { VarPair(ULongGen(), ULongGen()) }
        private val FloatGen: Generator<Float> = { Random.nextFloat() }

        private val VarIntTdfGen: Generator<VarIntTdf> = { VarIntTdf(LabelGen(), ULongGen()) }
        private val StringTdfGen: Generator<StringTdf> = { StringTdf(LabelGen(), StringGen()) }

        private val BlobTdfGen: Generator<BlobTdf> = {
            val minLength = 0
            val maxLength = 500
            val arrayLength = Random.nextInt(minLength..maxLength)
            val value = Random.nextBytes(arrayLength)
            BlobTdf(LabelGen(), value)
        }

        private val GroupTdfGen: Generator<GroupTdf> = {
            val minValues = 0
            val maxValues = 20
            val values = Random.nextInt(minValues..maxValues)
            val valueList = ArrayList<Tdf<*>>(values)
            repeat(values) { valueList.add(createRandomTdf()) }
            val isTwo = Random.nextBoolean()
            GroupTdf(LabelGen(), isTwo, valueList)
        }

        private val ListTdfGen: Generator<ListTdf> = {
            val minValues = 0
            val maxValues = 20
            val values = Random.nextInt(minValues..maxValues)
            val valueType = Random.nextInt(0..3)
            val listContents = ArrayList<Any>(values)
            val listType: Int = when (valueType) {
                0 -> {
                    repeat(values) { listContents.add(ULongGen()) }
                    Tdf.VARINT
                }
                1 -> {
                    repeat(values) { listContents.add(StringGen()) }
                    Tdf.STRING
                }
                2 -> {
                    repeat(values) { listContents.add(GroupTdfGen()) }
                    Tdf.GROUP
                }
                3 -> {
                    repeat(values) { listContents.add(TrippleGen()) }
                    Tdf.TRIPPLE
                }
                else -> throw IllegalArgumentException("Not expecting value greater than 3")
            }
            ListTdf(LabelGen(), listType, listContents)
        }

        private val MapTdfGen: Generator<MapTdf> = {
            val minValues = 0
            val maxValues = 20
            val values = Random.nextInt(minValues..maxValues)
            val keyTypeRaw = Random.nextInt(0..1)
            val keyType = when (keyTypeRaw) {
                0 -> Tdf.VARINT
                1 -> Tdf.STRING
                else -> throw IllegalArgumentException("Unexpected value")
            }
            val valueTypeRaw = Random.nextInt(0..3)
            val valueType = when (valueTypeRaw) {
                0 -> Tdf.VARINT
                1 -> Tdf.STRING
                2 -> Tdf.GROUP
                3 -> Tdf.FLOAT
                else -> throw IllegalArgumentException("Unexpected value")
            }
            val map = LinkedHashMap<Any, Any>()

            repeat(values) {
                val key = when (keyTypeRaw) {
                    0 -> ULongGen()
                    1 -> StringGen()
                    else -> throw IllegalArgumentException("Unexpected value")
                }
                val value: Any = when (valueTypeRaw) {
                    0 -> ULongGen()
                    1 -> StringGen()
                    2 -> GroupTdfGen()
                    3 -> FloatGen()
                    else -> throw IllegalArgumentException("Unexpected value")
                }
                map[key] = value
            }
            MapTdf(LabelGen(), keyType, valueType, map)
        }

        private val OptionalTdfGen: Generator<OptionalTdf> = {
            val type = Random.nextInt(0 until 100)
            val value = createRandomTdf()
            OptionalTdf(LabelGen(), type, value)
        }

        private val VarIntListTdfGen: Generator<VarIntList> = {
            val minValues = 0
            val maxValues = 20
            val values = Random.nextInt(minValues..maxValues)
            val valuesList = ArrayList<ULong>(values)
            repeat(values) {valuesList.add(ULongGen())}
            VarIntList(LabelGen(), valuesList)
        }

        private fun createRandomTdf(): Tdf<*> {
            val random = Random.nextInt(0..10)
            val generator: Generator<Tdf<*>> = when (random) {
                0 -> VarIntTdfGen
                1 -> StringTdfGen
                2 -> BlobTdfGen
                3 -> GroupTdfGen
                4 -> ListTdfGen
                5 -> MapTdfGen
                6 -> OptionalTdfGen
                7 -> VarIntListTdfGen
                else -> throw IllegalArgumentException("Unexpected value")
            }
            return generator()
        }

        private fun testTdfIterations(generator: Generator<Tdf<*>>) {
            repeat(ITERATIONS) { // Repeat for the total iterations
                BUFFER.discardReadBytes() // Discard the existing buffer contents and reset
                val value = generator() // Generate a tdf value
                value.writeFully(BUFFER) // Write the tdf to the buffer
                val readTdf = Tdf.read(BUFFER) // Read the tdf from the buffer
                assertEquals(value, readTdf) // Assert the read value matches
            }
        }
    }

    /**
     * test varint Tests the VarInt encoding against
     * a variety of different number values
     */
    @Test
    fun `test varint`() = testTdfIterations(VarIntTdfGen)


    /**
     * test string Tests the string encoding against
     * a randomized string between 0 and 1000 characters
     * long
     */
    @Test
    fun `test string`() = testTdfIterations(StringTdfGen)

    /**
     * test blob Tests the blob (ByteArray) encoding on random
     * byte arrays with lengths between 0 and 500
     */
    @Test
    fun `test blob`() = testTdfIterations(BlobTdfGen)

    /**
     * test group Tests the group encoding using random Tdf values
     */
    @Test
    fun `test group`() = testTdfIterations(GroupTdfGen)
}