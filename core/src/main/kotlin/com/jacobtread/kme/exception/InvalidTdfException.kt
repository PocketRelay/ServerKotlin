package com.jacobtread.kme.exception

class InvalidTdfException(label: String, reason: String) : RuntimeException("Couldn't get tdf with label: $label because: $reason")