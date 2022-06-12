package com.jacobtread.kme.blaze

import com.jacobtread.kme.blaze.tdf.Tdf
import com.jacobtread.kme.utils.logging.Logger
import io.netty.buffer.ByteBuf
import io.netty.util.ReferenceCounted

/**
 * Packet Represents a Blaze packet
 *
 * @property component The component of the packet this is a sort of global specifier which says which
 * group of handlers this packet belongs to. IDs for components can be found in the Components object
 * @property command The command of the packet this is a child to the component and each set of commands
 * is unique to the component that is provided. IDs for commands can be found in the Commands object
 * @property error The error code for this packet if this packet has an error this is NO_ERROR (zero) if there
 * is no error
 * @property type The type of the packet (INCOMING, RESPONDING, UNIQUE, ERROR) constants are listed in the
 * companion object
 * @property id The unique ID of this packet this is how the client is told which packet response packets
 * are responding to
 * @property contentBuffer The byte buffer containing the encoded tdf contents this is released once its
 * lazy consumed by the content lazy property
 * @constructor Create empty Packet
 *
 * @see Components For the IDs of the available components
 * @see Commands For the IDs of the available commands
 */
open class Packet(
    val component: Int,
    val command: Int,
    val error: Int,
    val type: Int,
    val id: Int,
    val contentBuffer: ByteBuf,
) : TdfContainer {

    companion object {
        const val INCOMING_TYPE = 0x0000 // Packet type representing client -> server
        const val RESPONSE_TYPE = 0x1000 // Packet type representing a response to client -> server from server
        const val UNIQUE_TYPE = 0x2000 // Packet type representing a server -> client message that's not a response
        const val ERROR_TYPE = 0x3000 // Packet type representing a packet with an error

        const val NO_ERROR = 0 // No error error code
    }

    /**
     * content Lazy value for reading the contents of this packet from the
     * content buffer. This will release the content buffer after it has
     * read all the TDF values from it
     */
    val content: List<Tdf<*>> by lazy {
        val values = ArrayList<Tdf<*>>()
        try {
            while (contentBuffer.readableBytes() > 4) {
                values.add(Tdf.read(contentBuffer))
            }
            contentBuffer.release() // Only release the content if we managed to read it
        } catch (e: Throwable) {
            Logger.error("Failed to read packet contents", e)
            if (values.isNotEmpty()) {
                Logger.error("Last tdf in contents was: " + values.last())
            }
            throw e
        }
        values
    }


    override fun getTdfByLabel(label: String): Tdf<*>? = content.find { it.label == label }

    override fun toString(): String {
        return "Packet (Component: $component, Command: $command, Error; $error, QType: $type, Id: $id, Content: [${content.joinToString(", ") { it.toString() }})"
    }

    fun release() {
        if (contentBuffer.refCnt() > 0) {
            contentBuffer.release()
        }
    }
}
