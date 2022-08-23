package com.jacobtread.relay.utils

import java.util.concurrent.CompletableFuture

typealias Future<T> = CompletableFuture<T>
typealias VoidFuture = Future<Void>
