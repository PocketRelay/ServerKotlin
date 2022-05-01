package com.jacobtread.kme.blaze


class Packet(
    val component: PacketComponent,
    val command: PacketCommand,
    val error: Int,
    val qtype: Int,
    val id: Int,
    val content: ByteArray,
) {
    override fun toString(): String {
        return "Packet (Component: $component (${component.id}), Command: $command ($command.id), Error; $error, QType: $qtype, Id: $id, Content: ${content.contentToString()})"
    }

}