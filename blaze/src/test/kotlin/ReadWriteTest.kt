import com.jacobtread.kme.blaze.utils.asString
import com.jacobtread.kme.blaze.utils.readVarInt
import com.jacobtread.kme.blaze.utils.writeVarInt
import io.netty.buffer.Unpooled

fun main() {
    testReadWriteVarInt()
}

fun testReadWriteVarInt() {
    val buffer = Unpooled.buffer()
    buffer.writeVarInt(64uL)

    val bufferString = buffer.asString()

    val value = buffer.readVarInt()
    assert(value == 64uL)

    println(value)
    println(bufferString)
}
