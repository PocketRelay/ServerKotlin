package com.jacobtread.kme.blaze


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

    override fun toString(): String {
        return "Packet (Component: ${componentName()} ($component), Command: ${commandName()} ($command), Error; $error, QType: $qtype, Id: $id, Content: ${content.contentToString()})"
    }

}