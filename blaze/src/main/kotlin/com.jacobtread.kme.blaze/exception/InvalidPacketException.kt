package com.jacobtread.kme.blaze.exception

class InvalidPacketException(message: String) : RuntimeException(message)
class UnexpectBlazePairException() : RuntimeException("Unexpected pairing of Component and Command")