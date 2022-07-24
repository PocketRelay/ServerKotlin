package com.jacobtread.kme.servers.main

import com.jacobtread.blaze.NotAuthenticatedException
import com.jacobtread.blaze.PacketEncoder
import com.jacobtread.blaze.TdfBuilder
import com.jacobtread.blaze.annotations.PacketHandler
import com.jacobtread.blaze.annotations.PacketProcessor
import com.jacobtread.blaze.group
import com.jacobtread.blaze.packet.Packet
import com.jacobtread.blaze.tdf.GroupTdf
import com.jacobtread.blaze.tdf.OptionalTdf
import com.jacobtread.kme.data.Commands
import com.jacobtread.kme.data.Components
import com.jacobtread.kme.database.entities.PlayerEntity
import com.jacobtread.kme.game.Game
import com.jacobtread.kme.game.match.Matchmaking
import io.netty.channel.Channel
import java.util.concurrent.atomic.AtomicInteger

@PacketProcessor
class Session(channel: Channel) {

    /**
     * The unique identifier for this session. Retrieves from the
     * atomic integer value and increases it for the next session
     */
    val sessionId = nextSessionId.getAndIncrement()

    /**
     * The socket channel that this session belongs to
     */
    private val socketChannel: Channel = channel

    /**
     * Encoded external ip address. This is the ip address which is
     * used when connecting from the outer world.
     */
    private var externalAddress: ULong = 0uL

    /**
     * This is the port used when other clients connect to this
     * client from the outer world
     */
    private var externalPort: ULong = 0uL

    /**
     * Encoded internal ip address. This is the ip address which is
     * used when connecting from the same network.
     */
    private var internalAddress: ULong = 0uL

    /**
     * This is the port used when other clients connect to this
     * client from the same network
     */
    private var internalPort: ULong = 0uL

    private val isNetworkingUnset: Boolean get() = externalAddress == 0uL || externalPort == 0uL || internalAddress == 0uL || internalPort == 0uL

    private var dbps: ULong = 0uL
    private var nattType: ULong = 0uL
    private var ubps: ULong = 0uL

    private var hardwareFlag: Int = 0
    private var pslm: List<ULong> = listOf(0xfff0fffu, 0xfff0fffu, 0xfff0fffu)

    private var game: Game? = null
    private var gameSlot: Int = 0

    private var matchmaking: Boolean = false
    private var matchmakingId: ULong = 1uL

    /**
     * The unix timestamp in miliseconds from when this session entered
     * the matchmaking queue. Used to calcualte whether a session should
     * timeout from matchmaking
     */
    private var startedMatchmaking: Long = 1L

    /**
     * References the player entity that this session is currently
     * authenticated as.
     */
    private var playerEntity: PlayerEntity? = null

    fun resetMatchmakingState() {
        matchmaking = false
        startedMatchmaking = -1L
    }


    /**
     * Updates the encoder context string that is stored as
     * a channel attribute. This context string provides
     * additional information about channel networking and
     * is useful when debugging to see who sent which packets
     * and who recieved what
     */
    private fun updateEncoderContext() {
        val builder = StringBuilder()
        val remoteAddress = socketChannel.remoteAddress()

        builder.append("Session: (ID: ")
            .append(sessionId)
            .append(", ADDRESS: ")
            .append(remoteAddress)
            .appendLine(')')

        val playerEntity = playerEntity
        if (playerEntity != null) {
            builder.append("Player: (NAME: ")
                .append(playerEntity.displayName)
                .append(", ID: ")
                .append(playerEntity.playerId)
                .appendLine(')')
        }

        // Update encoder context value
        socketChannel
            .attr(PacketEncoder.ENCODER_CONTEXT_KEY)
            .set(builder.toString())
    }

    // region Packet Handlers

    @PacketHandler(Components.USER_SESSIONS, Commands.UPDATE_NETWORK_INFO)
    fun updateNetworkInfo(packet: Packet) {

    }

    // endregion

    // region Packet Generators

    private fun createExternalNetGroup(): GroupTdf {
        return group("EXIP") {
            number("IP", externalAddress)
            number("PORT", externalPort)
        }
    }

    private fun createInternalNetGroup(): GroupTdf {
        return group("INIP") {
            number("IP", internalAddress)
            number("PORT", externalPort)
        }
    }

    private fun createNetworkingTdf(label: String): OptionalTdf {
        return if (isNetworkingUnset) { // If networking information hasn't been provided
            OptionalTdf(label)
        } else {
            OptionalTdf(label, 0x02u, group("VALU") {
                +createExternalNetGroup()
                +createInternalNetGroup()
            })
        }
    }

    /**
     * Appends details about this session to the provided
     * tdf builder.
     *
     * @param builder
     */
    fun appendDetailsTo(builder: TdfBuilder) {
        val playerEntity = playerEntity ?: throw NotAuthenticatedException()
        with(builder) {
            number("BUID", playerEntity.playerId)
            number("FRST", 0)
            text("KEY", "11229301_9b171d92cc562b293e602ee8325612e7")
            number("LLOG", 0)
            text("MAIL", playerEntity.email) // Player Email
            +createPersonaGroup()
            number("UID", playerEntity.playerId) // Player ID
        }
    }

    /**
     * Create's a "persona" group tdf value for this session. The EA system has a
     * whole "persona" system but there's no need for that system to be implemented
     * in this project so instead the details are just filled with the player details
     *
     * @return The created persona group tdf
     */
    private fun createPersonaGroup(): GroupTdf {
        val playerEntity = playerEntity ?: throw NotAuthenticatedException()
        return group("PDTL") {
            text("DSNM", playerEntity.displayName) // Player Display Name
            number("LAST", 0) // Last login time (Ignored)
            number("PID", playerEntity.playerId) // Player ID
            number("STAS", 0)
            number("XREF", 0)
            number("XTYP", 0)
        }
    }

    // endregion

    /**
     * Removes the player from the game it is currently in. (If it exists)
     * and then sets the current game to null
     */
    private fun removeFromGame() {
        game?.removePlayer(this)
        game = null
    }

    fun dispose() {
        playerEntity = null
        removeFromGame()
        if (matchmaking) Matchmaking.removeFromQueue(this)
        // TODO: REMOVE ALL REFERENCES TO THIS OBJECT SO IT CAN BE GARBAGE COLLECTED
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is Session) return false
        if (sessionId != other.sessionId) return false
        return true
    }

    override fun hashCode(): Int {
        return sessionId.hashCode()
    }


    companion object {

        val nextSessionId = AtomicInteger(0)

    }

}