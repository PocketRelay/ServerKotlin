package com.jacobtread.kme.servers.http.exceptions

class InvalidParamException(key: String) : RuntimeException("Missing url parameter: $key")