package com.jacobtread.kme.blaze

import com.jacobtread.kme.blaze.tdf.Tdf
import io.netty.buffer.ByteBuf

open class Packet(
    val component: Int,
    val command: Int,
    val error: Int,
    val qtype: Int,
    val id: Int,
    val contentBuffer: ByteBuf,
) : TdfContainer {

    val content: List<Tdf<*>> by lazy {
        val values = ArrayList<Tdf<*>>()
        try {
            while (contentBuffer.readableBytes() > 4) {
                values.add(Tdf.read(contentBuffer))
            }
        } catch (e: Throwable) {
            println(values.last())
            e.printStackTrace()
        }
        contentBuffer.release()
        values
    }

    override fun getTdfByLabel(label: String): Tdf<*>? = content.find { it.label == label }

    override fun toString(): String {
        return "Packet (Component: $component, Command: $command, Error; $error, QType: $qtype, Id: $id, Content: [${content.joinToString(", ") { it.toString() }})"
    }
}