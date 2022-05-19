@file:Suppress("NOTHING_TO_INLINE")
package com.jacobtread.kme.blaze

import com.jacobtread.kme.blaze.tdf.*
import com.jacobtread.kme.utils.VarPair
import com.jacobtread.kme.utils.VarTripple

interface TdfContainer {

    fun getTdfByLabel(label: String): Tdf<*>?

    fun <C : Tdf<*>> getTdf(type: Class<C>, label: String): C {
        val value = getTdfByLabel(label) ?: throw MissingTdfException(label)
        if (!value.javaClass.isAssignableFrom(type)) throw InvalidTdfException(label, type, value.javaClass)
        return type.cast(value)
    }

    fun <C : Tdf<*>> getTdfOrNull(type: Class<C>, label: String): C? {
        val value = getTdfByLabel(label)
        if (value == null || !value.javaClass.isAssignableFrom(type)) return null
        return type.cast(value)
    }

    @Throws(InvalidTdfException::class)
    fun <T, C : Tdf<T>> get(type: Class<C>, label: String): T {
        val value = getTdfByLabel(label) ?: throw MissingTdfException(label)
        if (!value.javaClass.isAssignableFrom(type)) throw InvalidTdfException(label, type, value.javaClass)
        return type.cast(value).value

    }

    fun <T, C : Tdf<T>> getOrNull(type: Class<C>, label: String): T? {
        val value = getTdfByLabel(label)
        if (value == null || !value.javaClass.isAssignableFrom(type)) return null
        return type.cast(value).value
    }
}

//region Helper Functions

// Tdf Struct-Like Helpers

inline fun TdfContainer.struct(label: String): StructTdf = getTdf(StructTdf::class.java, label)
inline fun TdfContainer.union(label: String): UnionTdf = getTdf(UnionTdf::class.java, label)

inline fun TdfContainer.structOrNull(label: String): StructTdf? = getTdfOrNull(StructTdf::class.java, label)
inline fun TdfContainer.unionOrNull(label: String): UnionTdf? = getTdfOrNull(UnionTdf::class.java, label)

// Non-nullable Helpers

inline fun TdfContainer.text(label: String): String = getTdf(StringTdf::class.java, label).value
inline fun TdfContainer.number(label: String): Long = getTdf(VarIntTdf::class.java, label).value
inline fun TdfContainer.numberInt(label: String): Int = number(label).toInt()
inline fun TdfContainer.float(label: String): Float =  getTdf(FloatTdf::class.java, label).value
inline fun TdfContainer.blob(label: String): ByteArray = getTdf(BlobTdf::class.java, label).value
inline fun TdfContainer.unionValue(label: String): Tdf<*>? = getTdf(UnionTdf::class.java, label).value
inline fun TdfContainer.tripple(label: String): VarTripple = getTdf(TrippleTdf::class.java, label).value
inline fun TdfContainer.pair(label: String): VarPair = getTdf(PairTdf::class.java, label).value
inline fun TdfContainer.varIntList(label: String): List<Long> = getTdf(VarIntList::class.java, label).value
inline fun TdfContainer.list(label: String): List<Any> = getTdf(ListTdf::class.java, label).value
inline fun TdfContainer.map(label: String): Map<*, *> = getTdf(MapTdf::class.java, label).value

// Nullable Helpers

inline fun TdfContainer.textOrNull(label: String): String? = getTdfOrNull(StringTdf::class.java, label)?.value
inline fun TdfContainer.numberOrNull(label: String): Long? = getTdfOrNull(VarIntTdf::class.java, label)?.value
inline fun TdfContainer.numberIntOrNull(label: String): Int? = numberOrNull(label)?.toInt()
inline fun TdfContainer.floatOrNull(label: String): Float? =  getTdfOrNull(FloatTdf::class.java, label)?.value
inline fun TdfContainer.blobOrNull(label: String): ByteArray? = getTdfOrNull(BlobTdf::class.java, label)?.value
inline fun TdfContainer.unionValueOrNull(label: String): Tdf<*>? = getTdfOrNull(UnionTdf::class.java, label)?.value
inline fun TdfContainer.trippleOrNull(label: String): VarTripple? = getTdfOrNull(TrippleTdf::class.java, label)?.value
inline fun TdfContainer.pairOrNull(label: String): VarPair? = getTdfOrNull(PairTdf::class.java, label)?.value
inline fun TdfContainer.varIntListOrNull(label: String): List<Long>? = getTdfOrNull(VarIntList::class.java, label)?.value
inline fun TdfContainer.listOrNull(label: String): List<Any>? = getTdfOrNull(ListTdf::class.java, label)?.value
inline fun TdfContainer.mapOrNull(label: String): Map<*, *>? = getTdfOrNull(MapTdf::class.java, label)?.value

//endregion