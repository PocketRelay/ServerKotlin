package com.jacobtread.kme.data.retriever

import com.jacobtread.blaze.*
import com.jacobtread.blaze.packet.Packet
import com.jacobtread.kme.data.blaze.Commands
import com.jacobtread.kme.data.blaze.Components
import io.netty.channel.ChannelHandlerContext
import io.netty.channel.ChannelInboundHandlerAdapter
import java.util.*

object OriginDetailsRetriever {

    data class OriginDetails(
        val email: String,
        val displayName: String,
        val token: String
    )

    fun fetch(token: String): OriginDetails {
        var originDetails: OriginDetails? = null
        val serverChannel = Retriever.createOfficialChannel(object : ChannelInboundHandlerAdapter() {
            override fun channelRead(ctx: ChannelHandlerContext, msg: Any) {
                try {
                    if (msg !is Packet) {
                        super.channelRead(ctx, msg)
                        return
                    }

                    // Authentication response
                    if (msg.component == Components.AUTHENTICATION
                        && msg.command == Commands.ORIGIN_LOGIN
                    ) {
                        // Parse the authentication details
                        val sessionGroup = msg.group("SESS")
                        val email = sessionGroup.text("MAIL")

                        val personaData = sessionGroup.group("PDTL")
                        val displayName = personaData.text("DSNM")

                        // Set the origin details
                        originDetails = OriginDetails(
                            email,
                            displayName,
                            token
                        )

                        // Close to mark as finished
                        ctx.close()
                    }
                } catch (e: MissingTdfException) {
                    ctx.close()
                } catch (e: InvalidTdfException) {
                    ctx.close()
                }
            }
        })

        if (serverChannel != null) {

            // Send the origin authentication packet
            val packet = clientPacket(
                Components.AUTHENTICATION,
                Commands.ORIGIN_LOGIN,
                0x1
            ) {
                text("AUTH", token)
                number("TYPE", 0x1)
            }

            serverChannel.writeAndFlush(packet)
            serverChannel.closeFuture()
                .sync()
        }

        return if (originDetails == null) {
            createDefaultValue(token)
        } else {
            originDetails!!
        }
    }

    private fun createDefaultValue(token: String): OriginDetails {
        val uuid = UUID.randomUUID()
        val displayName = "Origin User ($uuid)"
        return OriginDetails(
            displayName,
            displayName.take(99),
            token
        )
    }
}