package com.jacobtread.kme.blaze

import com.jacobtread.kme.blaze.exception.InvalidTdfException
import com.jacobtread.kme.blaze.tdf.Tdf
import com.jacobtread.kme.blaze.tdf.TdfValue

interface TdfContainer {

    fun getTdfByLabel(label: String): Tdf?

    fun <C : Tdf> get(type: Class<C>, label: String): C {
        val value = getTdfByLabel(label)
        if (value == null || !value.javaClass.isAssignableFrom(type)) throw InvalidTdfException(label, "No tdf found")
        try {
            return type.cast(value)
        } catch (e: ClassCastException) {
            throw InvalidTdfException(label, "Failed to cast tdf to: ${value.javaClass.simpleName}")
        }
    }

    fun <C : Tdf> getOrNull(type: Class<C>, label: String): C? {
        val value = getTdfByLabel(label)
        if (value == null || !value.javaClass.isAssignableFrom(type)) return null
        return type.cast(value)
    }


    @Throws(InvalidTdfException::class)
    fun <C : TdfValue<T>, T> getValue(type: Class<C>, label: String): T {
        val value = getTdfByLabel(label) ?: throw InvalidTdfException(label, "No value found")
        if (!value.javaClass.isAssignableFrom(type)) throw InvalidTdfException(label, "Value not of type: ${value.javaClass.simpleName}")
        try {
            return type.cast(value).value
        } catch (e: ClassCastException) {
            throw InvalidTdfException(label, "Failed to cast value to: ${value.javaClass.simpleName}")
        }
    }


    fun <C : TdfValue<T>, T> getValueOrNull(type: Class<C>, label: String): T? {
        val value = getTdfByLabel(label)
        if (value == null || !value.javaClass.isAssignableFrom(type)) return null
        return try {
            type.cast(value).value
        } catch (e: ClassCastException) {
            null
        }
    }
}

inline fun <reified C : Tdf> TdfContainer.get(label: String): C = get(C::class.java, label)
inline fun <reified C : Tdf> TdfContainer.getOrNull(label: String): C? = getOrNull(C::class.java, label)
inline fun <reified C : TdfValue<T>, T> TdfContainer.getValue(label: String): T = getValue(C::class.java, label)
inline fun <reified C : TdfValue<T>, T> TdfContainer.getValueOrNull(label: String): T? = getValueOrNull(C::class.java, label)
