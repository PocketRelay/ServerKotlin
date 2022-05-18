package com.jacobtread.kme.blaze.tdf

import com.jacobtread.kme.blaze.exception.InvalidTdfException
import io.netty.buffer.ByteBuf
import kotlin.reflect.KClass
import kotlin.reflect.cast
import kotlin.system.measureNanoTime

class StructTdf(label: String, val start2: Boolean, override val value: List<Tdf>) : Tdf(label, STRUCT), TdfValue<List<Tdf>> {
    companion object {
        fun from(label: String, input: ByteBuf): StructTdf {
            val out = ArrayList<Tdf>()
            var start2 = false
            var byte: Int
            while (true) {
                byte = input.readUnsignedByte().toInt()
                if (byte == 0) break
                if (byte == 2) {
                    start2 = true
                } else {
                    input.readerIndex(input.readerIndex() - 1)
                }
                out.add(read(input))
            }
            return StructTdf(label, start2, out)
        }
    }

    inline fun <reified C: Tdf> get(label: String): C = get(C::class.java, label)

    fun <C : Tdf> get(type: Class<C>, label: String): C {
        val value = this.value.find { it.label == label }
        if (value == null || !value.javaClass.isAssignableFrom(type)) throw InvalidTdfException(label, "No tdf found")
        try {
            return type.cast(value)
        } catch (e: ClassCastException) {
            throw InvalidTdfException(label, "Failed to cast tdf to: ${value.javaClass.simpleName}")
        }
    }

    inline fun <reified C: Tdf> getOrNull(label: String): C? = getOrNull(C::class.java, label)

    fun <C : Tdf> getOrNull(type: Class<C>, label: String): C? {
        val value = this.value.find { it.label == label }
        if (value == null || !value.javaClass.isAssignableFrom(type)) return null
        return type.cast(value)
    }

    inline fun <reified C: TdfValue<T>, T> getValue(label: String): T = getValue(C::class.java, label)

    @Throws(InvalidTdfException::class)
    fun <C : TdfValue<T>, T> getValue(type: Class<C>, label: String): T {
        val value = this.value.find { it.label == label } ?: throw InvalidTdfException(label, "No value found")
        if (!value.javaClass.isAssignableFrom(type)) throw InvalidTdfException(label, "Value not of type: ${value.javaClass.simpleName}")
        try {
            return type.cast(value).value
        } catch (e: ClassCastException) {
            throw InvalidTdfException(label, "Failed to cast value to: ${value.javaClass.simpleName}")
        }
    }

    inline fun <reified C: TdfValue<T>, T> getValueOrNull(label: String): T? = getValueOrNull(C::class.java, label)

    fun <C : TdfValue<T>, T> getValueOrNull(type: Class<C>, label: String): T? {
        val value = this.value.find { it.label == label }
        if (value == null || !value.javaClass.isAssignableFrom(type)) return null
        return try {
            type.cast(value).value
        } catch (e: ClassCastException) {
            null
        }
    }

    override fun write(out: ByteBuf) {
        if (start2) out.writeByte(2)
        value.forEach {
            it.writeFully(out)
        }
        out.writeByte(0)
    }

    override fun toString(): String = "Struct($label: $value)"
}