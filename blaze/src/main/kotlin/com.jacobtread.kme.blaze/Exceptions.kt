package com.jacobtread.kme.blaze

class MissingTdfException(label: String) : RuntimeException("Missing tdf $label")
class InvalidTdfException(label: String, expected: Class<*>, got: Class<*>) : RuntimeException("Expected $label to be of type ${expected.simpleName} but got ${got.simpleName} instead")