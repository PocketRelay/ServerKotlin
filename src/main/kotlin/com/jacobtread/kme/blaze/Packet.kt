package com.jacobtread.kme.blaze

import io.netty.buffer.Unpooled


class Packet(
    val component: Int,
    val command: Int,
    val error: Int,
    val qtype: Int,
    val id: Int,
    val content: ByteArray,
) {

    fun componentName(): String = IdentifierLookups.ComponentNames.getOrElse(component) { "Unknown" }
    fun commandName(): String = IdentifierLookups.CommandNames.getOrElse(command) { "Unknown" }

    fun allocate() {
        val buf = Unpooled.buffer()

    }

    override fun toString(): String {
        return "Packet (Component: ${componentName()} ($component), Command: ${commandName()} ($command), Error; $error, QType: $qtype, Id: $id, Content: ${content.contentToString()})"
    }

}