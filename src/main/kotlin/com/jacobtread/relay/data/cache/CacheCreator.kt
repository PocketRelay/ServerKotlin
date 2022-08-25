package com.jacobtread.relay.data.cache

fun interface CacheCreator<T> {

    fun createItem(): T

}