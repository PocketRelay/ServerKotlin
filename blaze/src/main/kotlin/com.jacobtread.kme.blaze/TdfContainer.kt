package com.jacobtread.kme.blaze

import com.jacobtread.kme.blaze.tdf.Tdf
import com.jacobtread.kme.blaze.tdf.TdfValue

interface TdfContainer {

    fun getTdfByLabel(label: String): Tdf?

    fun <C : Tdf> getTdf(type: Class<C>, label: String): C {
        val value = getTdfByLabel(label) ?: throw MissingTdfException(label)
        if (!value.javaClass.isAssignableFrom(type)) throw InvalidTdfException(label, type, value.javaClass)
        return type.cast(value)
    }

    fun <C : Tdf> getTdfOrNull(type: Class<C>, label: String): C? {
        val value = getTdfByLabel(label)
        if (value == null || !value.javaClass.isAssignableFrom(type)) return null
        return type.cast(value)
    }


    @Throws(InvalidTdfException::class)
    fun <C : TdfValue<T>, T> get(type: Class<C>, label: String): T {
        val value = getTdfByLabel(label) ?: throw MissingTdfException(label)
        if (!value.javaClass.isAssignableFrom(type)) throw InvalidTdfException(label, type, value.javaClass)
        return type.cast(value).value

    }


    fun <C : TdfValue<T>, T> getOrNull(type: Class<C>, label: String): T? {
        val value = getTdfByLabel(label)
        if (value == null || !value.javaClass.isAssignableFrom(type)) return null
        return type.cast(value).value
    }
}

inline fun <reified C : Tdf> TdfContainer.getTdf(label: String): C = getTdf(C::class.java, label)
inline fun <reified C : Tdf> TdfContainer.getTdfOrNull(label: String): C? = getTdfOrNull(C::class.java, label)
inline operator fun <reified C : TdfValue<T>, T> TdfContainer.get(label: String): T = get(C::class.java, label)
inline fun <reified C : TdfValue<T>, T> TdfContainer.getOrNull(label: String): T? = getOrNull(C::class.java, label)
