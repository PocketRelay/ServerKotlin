package com.jacobtread.kme.servers.http.exceptions

class InvalidQueryException(key: String) : RuntimeException("Missing query parameter: $key")