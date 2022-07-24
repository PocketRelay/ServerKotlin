package com.jacobtread.kme.servers.main

import com.jacobtread.blaze.*
import com.jacobtread.blaze.annotations.PacketHandler
import com.jacobtread.blaze.annotations.PacketProcessor
import com.jacobtread.blaze.data.VarTripple
import com.jacobtread.blaze.packet.Packet
import com.jacobtread.blaze.tdf.GroupTdf
import com.jacobtread.blaze.tdf.OptionalTdf
import com.jacobtread.kme.Environment
import com.jacobtread.kme.data.Commands
import com.jacobtread.kme.data.Components
import com.jacobtread.kme.data.Data
import com.jacobtread.kme.data.LoginError
import com.jacobtread.kme.database.entities.MessageEntity
import com.jacobtread.kme.database.entities.PlayerEntity
import com.jacobtread.kme.game.Game
import com.jacobtread.kme.tools.unixTimeSeconds
import io.netty.channel.Channel
import java.io.IOException
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
    private var pslm: ArrayList<ULong> = arrayListOf(0xfff0fffu, 0xfff0fffu, 0xfff0fffu)

    private var location: ULong = 0x64654445uL // RETRIEVE FROM PREAUTH

    private var lastPingTime: Long = -1L

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

    // region User Sessions Handlers

    /**
     * Handles resuming a session that was present on a previous run or
     * that was logged out. This is done using the session key that was
     * provided to that session upon authenticating. The session key
     * provided by this packet is looked up in the database and if a
     * player is found with a matching one they become authenticated
     *
     * @param packet The packet requesting the session resumption
     */
    @PacketHandler(Components.USER_SESSIONS, Commands.RESUME_SESSION)
    fun handleResumeSession(packet: Packet) {
        val sessionToken = packet.text("SKEY")
        val playerEntity = PlayerEntity.bySessionToken(sessionToken)
        if (playerEntity == null) {
            push(LoginError.INVALID_INFORMATION(packet))
            return
        }
        setPlayerEntity(playerEntity)
        push(packet.respond())
    }

    /**
     * The packet recieved from the client contains networking information
     * including the external and internal ip addresses and ports along with
     * the natt type. All of this information is stored. This handler responds
     * with a set session packet to update the clients view of its session
     *
     * @param packet The packet containing the network update information
     */
    @PacketHandler(Components.USER_SESSIONS, Commands.UPDATE_NETWORK_INFO)
    fun updateNetworkInfo(packet: Packet) {
        val addr = packet.unionValue("ADDR") as GroupTdf
        setNetworkingFromGroup(addr)

        val nqos = packet.group("NQOS")
        dbps = nqos.number("DBPS")
        nattType = nqos.number("NATT")
        ubps = nqos.number("UBPS")

        val nlmp = packet.map<String, ULong>("NLMP")
        pslm[0] = nlmp.getOrDefault("ea-sjc", 0xfff0fffu)
        pslm[1] = nlmp.getOrDefault("rs-iad", 0xfff0fffu)
        pslm[2] = nlmp.getOrDefault("rs-lhr", 0xfff0fffu)

        push(packet.respond())
        push(createSetSessionPacket())
    }

    /**
     * Handles updating the hardware flag using the value provided by the
     * client using this packet. This handler responds with a set session
     * packet to update the clients view of its session
     *
     * @param packet The packet containing the hardware flag
     */
    @PacketHandler(Components.USER_SESSIONS, Commands.UPDATE_HARDWARE_FLAGS)
    fun updateHardwareFlag(packet: Packet) {
        hardwareFlag = packet.number("HWFG").toInt()
        push(packet.respond())
        push(createSetSessionPacket())
    }

    // endregion

    // region Util Handlers

    /**
     * Handles retrieving / creating of conf files and returning them
     * to the client. This includes talk files as well as other data
     * about the server
     *
     * - ME3_LIVE_TLK_PC_LANGUAGE: Talk files for the game
     * - ME3_DATA: Configurations and http server locations
     * - ME3_MSG: The current message this can be on the menu or in multiplayer
     * - ME3_ENT: Map of user entitlements (includes online access entitlement)
     * - ME3_DIME: Shop contents / Currency definition
     * - ME3_BINI_VERSION: BINI version information
     * - ME3_BINI_PC_COMPRESSED: ME3 BINI
     *
     * @param packet
     */
    @PacketHandler(Components.UTIL, Commands.FETCH_CLIENT_CONFIG)
    fun handleFetchClientConfig(packet: Packet) {
        val type = packet.text("CFID")
        val conf: Map<String, String> = if (type.startsWith("ME3_LIVE_TLK_PC_")) {
            val lang = type.substring(16)
            try {
                Data.loadChunkedFile("data/tlk/$lang.tlk.chunked")
            } catch (e: IOException) {
                Data.loadChunkedFile("data/tlk/default.tlk.chunked")
            }
        } else {
            when (type) {
                "ME3_DATA" -> Data.createDataConfig() // Configurations for GAW, images and others
                "ME3_MSG" -> MessageEntity.createMessageMap() // Custom multiplayer messages
                "ME3_ENT" -> Data.createEntitlementMap() // Entitlements
                "ME3_DIME" -> Data.createDimeResponse() // Shop contents?
                "ME3_BINI_VERSION" -> mapOf(
                    "SECTION" to "BINI_PC_COMPRESSED",
                    "VERSION" to "40128"
                )
                "ME3_BINI_PC_COMPRESSED" -> Data.loadBiniCompressed() // Loads the chunked + compressed bini
                else -> emptyMap()
            }
        }
        push(packet.respond {
            map("CONF", conf)
        })
    }

    /**
     * Handles responding to pings from the client. Responsd with the
     * server time in the response body.
     *
     * Currently this does nothing but update the last ping time
     * variable
     *
     * TODO: Implement actual ping timeout
     *
     * @param packet The ping packet
     */
    @PacketHandler(Components.UTIL, Commands.PING)
    fun handlePing(packet: Packet) {
        lastPingTime = System.currentTimeMillis()
        push(packet.respond {
            number("STIM", unixTimeSeconds())
        })
    }

    /**
     * Handles the pre authentication packet this includes information about the
     * client such as location, version, platform, etc. This response with information
     * about the current server configuration. This function updates [location]
     *
     * Other CINF Fields:
     *
     * - PLAT: The platform the game is running on (e.g. Windows)
     * - MAC: The mac address of the computer
     * - BSDK: The Blaze SDK version used in the client
     * - CVER: The mass effect client version
     * - ENV: The client environment type
     *
     * @param packet
     */
    @PacketHandler(Components.UTIL, Commands.PRE_AUTH)
    fun handlePreAuth(packet: Packet) {

        val infoGroup = packet.group("CINF")
        location = infoGroup.number("LOC")

        push(packet.respond {
            number("ANON", 0x0)
            text("ASRC", "303107")
            list("CIDS", listOf(1, 25, 4, 28, 7, 9, 63490, 30720, 15, 30721, 30722, 30723, 30725, 30726, 2000))
            text("CNGN", "")
            +group("CONF") {
                map(
                    "CONF", mapOf(
                        "pingPeriod" to "15s", // The delay between each ping to the server
                        "voipHeadsetUpdateRate" to "1000", // The rate at which headsets are updated
                        "xlspConnectionIdleTimeout" to "300" // The xlsp connection idle timeout
                    )
                )
            }
            text("INST", "masseffect-3-pc") // The type of server?
            number("MINR", 0x0)
            text("NASP", "cem_ea_id")
            text("PILD", "")
            text("PLAT", "pc") // Platform
            text("PTAG", "")
            // The following addresses have all been redirected to localhost to be ignored
            +group("QOSS") {
                +group("BWPS") {
                    text("PSA", "127.0.0.1")   // Server Address (formerly gossjcprod-qos01.ea.com)
                    number("PSP", 17502)  // Server Port
                    text("SNA", "prod-sjc")  // Server name?
                }

                number("LNP", 0xA)
                map("LTPS", mapOf(
                    "ea-sjc" to group {
                        text("PSA", "127.0.0.1")  // Server Address (formerly gossjcprod-qos01.ea.com)
                        number("PSP", 17502)  // Server Port
                        text("SNA", "prod-sjc") // Server name?
                    },
                    "rs-iad" to group {
                        text("PSA", "127.0.0.1") // Server Address (formerly gosiadprod-qos01.ea.com)
                        number("PSP", 17502)  // Server Port
                        text("SNA", "bio-iad-prod-shared") // Server name?
                    },
                    "rs-lhr" to group {
                        text("PSA", "127.0.0.1") // Server Address (formerly gosgvaprod-qos01.ea.com)
                        number("PSP", 17502) // Server Port
                        text("SNA", "bio-dub-prod-shared") // Server name?
                    }
                ))
                number("SVID", 0x45410805)
            }
            text("RSRC", "303107")
            text("SVER", "Blaze 3.15.08.0 (CL# 1629389)") // Blaze Server Version
        })
    }

    /**
     * Handles the post authentication packet which responds with
     * information about the ticker and telemetry servers
     *
     * @param packet The packet requesting the post auth information
     */
    @PacketHandler(Components.UTIL, Commands.POST_AUTH)
    fun handlePostAuth(packet: Packet) {
        push(packet.respond {
            +group("PSS") { // Player Sync Service
                text("ADRS", "playersyncservice.ea.com") // Host / Address
                blob("CSIG")
                text("PJID", "303107")
                number("PORT", 443) // Port
                number("RPRT", 0xF)
                number("TIID", 0x0)
            }

            //  telemetryAddress = "reports.tools.gos.ea.com:9988"
            //  tickerAddress = "waleu2.tools.gos.ea.com:8999"
            val address = Environment.externalAddress
            val port = Environment.discardPort

            +group("TELE") {
                text("ADRS", address) // Server Address
                number("ANON", 0)
                text("DISA", "**")
                text("FILT", "-UION/****") // Telemetry filter?
                number("LOC", 1701725253)
                text("NOOK", "US,CA,MX")
                number("PORT", port)
                number("SDLY", 15000)
                text("SESS", "JMhnT9dXSED")
                text("SKEY", "")
                number("SPCT", 0x4B)
                text("STIM", "")
            }

            +group("TICK") {
                text("ADRS", address)
                number("port", port)
                text("SKEY", "823287263,10.23.15.2:8999,masseffect-3-pc,10,50,50,50,50,0,12")
            }

            +group("UROP") {
                number("TMOP", 0x1)
                number("UID", sessionId)
            }
        })
    }

    /**
     * Handles updating an individual setting provided by the client in the
     * form of a key value pair named KEY and DATA. This is for updating any
     * data stored on the player such as inventory, characters, classes etc
     *
     * @param packet The packet updating the setting
     */
    @PacketHandler(Components.UTIL, Commands.USER_SETTINGS_SAVE)
    fun handleUserSettingsSave(packet: Packet) {
        val playerEntity = playerEntity ?: throw NotAuthenticatedException()
        val value = packet.textOrNull("DATA")
        val key = packet.textOrNull("KEY")
        if (value != null && key != null) {
            playerEntity.setSetting(key, value)
        }
        push(packet.respond())
    }

    /**
     * Handles loading all the user settings for the authenticated users. This
     * loads all the player entity settings from the database and puts them
     * as key value pairs into a map which is sent to the client.
     *
     * @param packet The packet requesting all the settings
     */
    @PacketHandler(Components.UTIL, Commands.USER_SETTINGS_LOAD_ALL)
    fun handleUserSettingsLoadAll(packet: Packet) {
        val playerEntity = playerEntity ?: throw NotAuthenticatedException()
        push(packet.respond {
            map("SMAP", playerEntity.createSettingsMap())
        })
    }

    /**
     * Handles suspend user pings. The purpose of this is not yet understood,
     * and it requires further investigation before it can be documented
     *
     * @param packet The packet for suspend user ping
     */
    @PacketHandler(Components.UTIL, Commands.SUSPEND_USER_PING)
    fun handleSuspendUserPing(packet: Packet) {
        push(
            when (packet.numberOrNull("TVAL")) {
                0x1312D00uL -> packet.error(0x12D)
                0x55D4A80uL -> packet.error(0x12E)
                else -> packet.respond()
            }
        )
    }

    // endregion

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