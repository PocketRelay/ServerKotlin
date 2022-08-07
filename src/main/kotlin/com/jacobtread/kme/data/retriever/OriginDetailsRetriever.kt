package com.jacobtread.kme.data.retriever

import com.jacobtread.blaze.*
import com.jacobtread.blaze.packet.Packet
import com.jacobtread.kme.data.blaze.Commands
import com.jacobtread.kme.data.blaze.Components
import com.jacobtread.kme.utils.logging.Logger
import io.netty.channel.Channel
import io.netty.channel.ChannelHandlerContext
import io.netty.channel.ChannelInboundHandlerAdapter
import java.io.IOException
import java.util.concurrent.TimeUnit

/**
 * System for connecting to the official servers to attempt to
 * retrieve the details of an origin account.
 *
 * @constructor Create empty Origin details retriever
 */
object OriginDetailsRetriever {

    var isDataFetchingEnabled: Boolean = false
        internal set

    /**
     * Origin details object structure.
     *
     * @property email The email for the origin account
     * @property displayName The display name for the origin account
     * @property token The origin session token
     */
    data class OriginDetails(
        val email: String,
        val displayName: String,
        val token: String,
        val dataMap: HashMap<String, String>,
    )

    /**
     * Connects to the official origin servers and logs in
     * using the provided origin token. Returns a [OriginDetails]
     * object with the details or a default one in case of failure.
     *
     * @param token The origin token
     * @return The origin details
     */
    fun retrieve(token: String): OriginDetails? {
        var serverChannel: Channel? = null
        var originDetails: OriginDetails? = null
        try {
            serverChannel = Retriever.createOfficialChannel(object : ChannelInboundHandlerAdapter() {
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

                            Logger.logIfDebug { "Fetched details from origin: $displayName ($email)" }

                            // Set the origin details
                            originDetails = OriginDetails(
                                email,
                                displayName,
                                token,
                                HashMap()
                            )

                            if (isDataFetchingEnabled) {
                                Logger.logIfDebug { "Attempting to fetch settings from origin" }
                                // Send a request for the user settings
                                val packet = clientPacket(Components.UTIL, Commands.USER_SETTINGS_LOAD_ALL, 0x2) {}
                                serverChannel?.writeAndFlush(packet)
                            } else {
                                // Close to mark as finished
                                ctx.close()
                            }
                        }

                        if (isDataFetchingEnabled) {
                            if (msg.component == Components.UTIL && msg.command == Commands.USER_SETTINGS_LOAD_ALL) {
                                val settings = msg.map<String, String>("SMAP")
                                Logger.logIfDebug { "Retreived settings from origin: $settings" }
                                originDetails?.dataMap?.putAll(settings)

                                // Close to mark as finished
                                ctx.close()
                            }
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
                val closeFuture = serverChannel.closeFuture()
                // If we didn't close withing 15 seconds but we are waiting
                // on data for the player data we can wait 10 more seconds
                if(
                    !closeFuture.await(15, TimeUnit.SECONDS)
                    && originDetails != null
                ) {
                    closeFuture.await(10, TimeUnit.SECONDS)
                }
            }
            val details = originDetails
            if (details != null) {
                // Developer banner
                if (details.email == "jacobtread@gmail.com") {
                    details.dataMap["csreward"] = "154"
                }
                return details
            }
        } catch (_: InterruptedException) {
        } catch (_: IOException) {
        }
        if (serverChannel != null) {
            Logger.warn("Failed to retrieve origin information for account. Unable to login.")
        }
        return null
    }
}
