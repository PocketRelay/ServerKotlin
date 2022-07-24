package com.jacobtread.kme.servers.main

import com.jacobtread.blaze.*
import com.jacobtread.blaze.annotations.PacketHandler
import com.jacobtread.blaze.annotations.PacketProcessor
import com.jacobtread.blaze.data.VarTripple
import com.jacobtread.blaze.packet.Packet
import com.jacobtread.blaze.tdf.GroupTdf
import com.jacobtread.blaze.tdf.OptionalTdf
import com.jacobtread.kme.data.Commands
import com.jacobtread.kme.data.Components
import com.jacobtread.kme.database.entities.PlayerEntity
import com.jacobtread.kme.game.Game
import io.netty.channel.Channel
import java.util.concurrent.atomic.AtomicInteger

@PacketProcessor
class Session(channel: Channel) : PacketPushable {

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

    private var location: ULong = 0x64654445uL // RETRIEVE FROM PREAUTH

    private var game: Game? = null
    private var gameSlot: Int = 0
    private val gameId: ULong get() = game?.id ?: 1uL

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

    init {
        updateEncoderContext() // Set the initial encoder context
    }

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

    override fun push(packet: Packet) {
        val eventLoop = socketChannel.eventLoop()
        if (eventLoop.inEventLoop()) { // If the push was made inside the event loop
            // Write the packet and flush
            socketChannel.write(packet)
            socketChannel.flush()
        } else { // If the push was made outside the event loop
            eventLoop.execute { // Execute write and flush on event loop
                socketChannel.write(packet)
                socketChannel.flush()
            }
        }
    }

    override fun pushAll(vararg packets: Packet) {
        val eventLoop = socketChannel.eventLoop()
        if (eventLoop.inEventLoop()) { // If the push was made inside the event loop
            // Write the packets and flush
            packets.forEach { socketChannel.write(it) }
            socketChannel.flush()
        } else { // If the push was made outside the event loop
            eventLoop.execute { // Execute write and flush on event loop
                packets.forEach { socketChannel.write(it) }
                socketChannel.flush()
            }
        }
    }

    fun setPlayerEntity(playerEntity: PlayerEntity?) {
        val existing = this.playerEntity
        if (existing != playerEntity) {
            removeFromGame()
        }
        this.playerEntity = playerEntity
        // Update the encoder context because player has changed
        updateEncoderContext()
    }


    // region Packet Handlers

    @PacketHandler(Components.USER_SESSIONS, Commands.UPDATE_NETWORK_INFO)
    fun updateNetworkInfo(packet: Packet) {

    }

    // endregion

    // region Packet Generators

    private fun notifyMatchmakingFailed() {
        resetMatchmakingState()
        val playerEntity = playerEntity ?: return
        push(
            unique(Components.GAME_MANAGER, Commands.NOTIFY_MATCHMAKING_FAILED) {
                number("MAXF", 0x5460)
                number("MSID", matchmakingId)
                number("RSLT", 0x4)
                number("USID", playerEntity.playerId)
            }
        )
    }

    private fun notifyMatchmakingStatus() {
        val playerEntity = playerEntity ?: return
        push(
            unique(
                Components.GAME_MANAGER,
                Commands.NOTIFY_MATCHMAKING_ASYNC_STATUS
            ) {
                list("ASIL", listOf(
                    group {
                        +group("CGS") {
                            number("EVST", if (matchmaking) 0x6 else 0x0)
                            number("MMSN", 0x1)
                            number("NOMP", 0x0)
                        }
                        +group("CUST") {
                        }
                        +group("DNFS") {
                            number("MDNF", 0x0)
                            number("XDNF", 0x0)
                        }
                        +group("FGS") {
                            number("GNUM", 0x0)
                        }
                        +group("GEOS") {
                            number("DIST", 0x0)
                        }
                        map(
                            "GRDA", mapOf(
                                "ME3_gameDifficultyRule" to group {
                                    text("NAME", "ME3_gameDifficultyRule")
                                    list("VALU", listOf("difficulty3"))
                                },
                                "ME3_gameEnemyTypeRule" to group {
                                    text("NAME", "ME3_gameEnemyTypeRule")
                                    list("VALU", listOf("enemy4"))
                                },
                                "ME3_gameMapMatchRule" to group {
                                    text("NAME", "ME3_gameMapMatchRule")
                                    list(
                                        "VALU",
                                        listOf(
                                            "map0", "map1", "map2", "map3", "map4", "map5", "map6",
                                            "map7", "map8", "map9", "map10", "map11", "map12", "map13",
                                            "map14", "map15", "map16", "map17", "map18", "map19", "map20",
                                            "map21", "map22", "map23", "map24", "map25", "map26", "map27",
                                            "map28", "map29", "random", "abstain"
                                        )
                                    )
                                },
                                "ME3_gameStateMatchRule" to group {
                                    text("NAME", "ME3_gameStateMatchRule")
                                    list("VALU", listOf("IN_LOBBY", "IN_LOBBY_LONGTIME", "IN_GAME_STARTING", "abstain"))
                                },
                                "ME3_rule_dlc2300" to group {
                                    text("NAME", "ME3_rule_dlc2300")
                                    list("VALU", listOf("required", "preferred"))
                                },
                                "ME3_rule_dlc2500" to group {
                                    text("NAME", "ME3_rule_dlc2500")
                                    list("VALU", listOf("required", "preferred"))
                                },
                                "ME3_rule_dlc2700" to group {
                                    text("NAME", "ME3_rule_dlc2700")
                                    list("VALU", listOf("required", "preferred"))
                                },
                                "ME3_rule_dlc3050" to group {
                                    text("NAME", "ME3_rule_dlc3050")
                                    list("VALU", listOf("required", "preferred"))
                                },
                                "ME3_rule_dlc3225" to group {
                                    text("NAME", "ME3_rule_dlc3225")
                                    list("VALU", listOf("required", "preferred"))
                                },
                            )
                        )
                        +group("GSRD") {
                            number("PMAX", 0x4)
                            number("PMIN", 0x2)
                        }
                        +group("HBRD") {
                            number("BVAL", 0x1)
                        }
                        +group("HVRD") {
                            number("VVAL", 0x0)
                        }
                        +group("PSRS") {
                        }
                        +group("RRDA") {
                            number("RVAL", 0x0)
                        }
                        +group("TSRS") {
                            number("TMAX", 0x0)
                            number("TMIN", 0x0)
                        }
                        map(
                            "UEDS", mapOf(
                                "ME3_characterSkill_Rule" to group {
                                    number("AMAX", 0x1f4)
                                    number("AMIN", 0x0)
                                    number("MUED", 0x1f4)
                                    text("NAME", "ME3_characterSkill_Rule")
                                },
                            )
                        )
                        +group("VGRS") {
                            number("VVAL", 0x0)
                        }
                    }
                ))
                number("MSID", matchmakingId)
                number("USID", playerEntity.playerId)
            }
        )
    }

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

    private fun setNetworkingFromGroup(group: GroupTdf) {
        val exip = group.group("EXIP")
        externalAddress = exip.number("IP")
        externalPort = exip.number("PORT")

        val inip = group.group("INIP")
        internalAddress = inip.number("IP")
        internalPort = inip.number("PORT")
    }

    private fun updateSessionFor(session: Session) {
        val playerEntity = playerEntity ?: return
        val sessionDetailsPacket = unique(
            Components.USER_SESSIONS,
            Commands.SESSION_DETAILS
        ) {
            +createSessionDataGroup()
            +group("USER") {
                number("AID", playerEntity.playerId)
                number("ALOC", location)
                blob("EXBB")
                number("EXID", 0)
                number("ID", playerEntity.playerId)
                text("NAME", playerEntity.displayName)
            }
        }

        val identityPacket = unique(
            Components.USER_SESSIONS,
            Commands.UPDATE_EXTENDED_DATA_ATTRIBUTE
        ) {
            number("FLGS", 0x3uL)
            number("ID", playerEntity.playerId)
        }

        session.pushAll(sessionDetailsPacket, identityPacket)
    }

    private fun createSessionDataGroup(): GroupTdf {
        return group("DATA") {
            +createNetworkingTdf("ADDR")
            text("BPS", "ea-sjc")
            text("CTY")
            varList("CVAR")
            map("DMAP", mapOf(0x70001 to 0x409a))
            number("HWFG", hardwareFlag)
            list("PSLM", pslm)
            +group("QDAT") {
                number("DBPS", dbps)
                number("NATT", nattType)
                number("UBPS", ubps)
            }
            number("UATT", 0)
            list("ULST", listOf(VarTripple(4u, 1u, gameId)))
        }
    }

    fun createSetSessionPacket(): Packet {
        return unique(
            Components.USER_SESSIONS,
            Commands.SET_SESSION
        ) {
            +createSessionDataGroup()
            number("USID", playerEntity?.playerId ?: 1)
        }
    }

    /**
     * Creates player data group this is used by games and
     * contains information about the player and the session
     * this includes networking information
     *
     * @return The created group tdf
     */
    fun createPlayerDataGroup(): GroupTdf? {
        val playerEntity = playerEntity ?: return null
        val playerId = playerEntity.playerId
        return group("PDAT") {
            blob("BLOB")
            number("EXID", 0x0)
            number("GID", gameId) // Current game ID
            number("LOC", location) // Encoded Location
            text("NAME", playerEntity.displayName) // Player Display Name
            number("PID", playerId)  // Player ID
            +createNetworkingTdf("PNET") // Player Network Information
            number("SID", gameSlot) // Player Slot Index/ID
            number("SLOT", 0x0)
            number("STAT", 0x2)
            number("TIDX", 0xffff)
            number("TIME", 0x0)
            tripple("UGID", 0x0, 0x0, 0x0)
            number("UID", playerId) // Player ID
        }
    }

    /**
     * Creates an authenticated response message for the provided
     * packet and returns the created message
     *
     * @param packet The packet to create the response for
     * @return The created response
     *
     * @throws NotAuthenticatedException If the session is not authenticated
     */
    private fun createAuthenticatedResponse(packet: Packet): Packet {
        val playerEntity = playerEntity ?: throw NotAuthenticatedException()
        return packet.respond {
            text("LDHT", "")
            number("NTOS", 0)
            text("PCTK", playerEntity.sessionToken) // PC Session Token
            list("PLST", listOf(createPersonaGroup())) // Persona List
            text("PRIV", "")
            text("SKEY", SKEY)
            number("SPAM", 0)
            text("THST", "")
            text("TSUI", "")
            text("TURI", "")
            number("UID", playerEntity.playerId) // Player ID
        }
    }

    /**
     * Appends details about this session to the provided
     * tdf builder.
     *
     * @param builder The builder to append to
     */
    private fun appendDetailsTo(builder: TdfBuilder) {
        val playerEntity = playerEntity ?: throw NotAuthenticatedException()
        with(builder) {
            number("BUID", playerEntity.playerId)
            number("FRST", 0)
            text("KEY", SKEY)
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
        resetMatchmakingState()
//        game?.removePlayer(this)
        game = null
    }

    fun dispose() {
        setPlayerEntity(null)
//        if (matchmaking) Matchmaking.removeFromQueue(this)
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

        private const val SKEY = "11229301_9b171d92cc562b293e602ee8325612e7"
        private val nextSessionId = AtomicInteger(0)

    }

}