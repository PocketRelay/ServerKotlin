package com.jacobtread.kme.sessions.handlers

import com.jacobtread.blaze.*
import com.jacobtread.blaze.annotations.PacketHandler
import com.jacobtread.blaze.packet.Packet
import com.jacobtread.kme.Environment
import com.jacobtread.kme.data.blaze.Commands
import com.jacobtread.kme.data.blaze.Components
import com.jacobtread.kme.data.getMapData
import com.jacobtread.kme.data.getTextData
import com.jacobtread.kme.sessions.Session
import com.jacobtread.kme.utils.logging.Logger
import com.jacobtread.kme.utils.unixTimeSeconds

/**
 * Handles the pre authentication packet this includes information about the
 * client such as location, version, platform, etc. This response with information
 * about the current server configuration. This function updates [Session.location]
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
fun Session.handlePreAuth(packet: Packet) {
    val infoGroup = packet.group("CINF")
    location = infoGroup.ulong("LOC")

    push(packet.respond {
        number("ANON", 0x0)
        text("ASRC", "303107")
        list(
            // Component IDS? (They match up so assumptions...)
            "CIDS", listOf(
                Components.AUTHENTICATION,
                Components.ASSOCIATION_LISTS,
                Components.GAME_MANAGER,
                Components.GAME_REPORTING,
                Components.STATS,
                Components.UTIL,
                63490,
                30720,
                Components.MESSAGING,
                30721,
                Components.USER_SESSIONS,
                30723,
                30725,
                30726,
                Components.DYNAMIC_FILTER
            )
        )
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

            val serverGroup = group("BWPS") {
                text("PSA", Environment.externalAddress)
                number("PSP", Environment.httpPort)
                text("SNA", "prod-sjc")
            }

            +serverGroup
            number("LNP", 0xA)
            map(
                "LTPS", mapOf(
                    "ea-sjc" to serverGroup
                )
            )
            number("SVID", 0x45410805)
        }
        text("RSRC", "303107")
        text("SVER", "Blaze 3.15.08.0 (CL# 1629389)") // Blaze Server Version
    })
}

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
fun Session.handleFetchClientConfig(packet: Packet) {
    val type = packet.text("CFID")
    val conf: Map<String, String> = if (type.startsWith("ME3_LIVE_TLK_PC_")) {
        val lang = type.substring(16)
        var map = getMapData("tlk/$lang.tlk.dmap")
        if (map == null) map = getMapData("tlk/default.tlk.dmap")
        if (map == null) map = emptyMap()
        map
    } else {
        when (type) {
            "ME3_DATA" -> createDataConfig()
            "ME3_MSG" -> emptyMap()
            "ME3_ENT" -> {
                getMapData("/entitlements.dmap")
                    ?: Logger.fatal("Missing entitlements data. Try redownloading the server")
            }
            "ME3_DIME" -> {
                val dime = getTextData("dime.xml")
                    ?: Logger.fatal("Missing dime data. Try redownloading the server")
                mapOf("Config" to dime)
            }
            "ME3_BINI_VERSION" -> mapOf("SECTION" to "BINI_PC_COMPRESSED", "VERSION" to "40128")
            "ME3_BINI_PC_COMPRESSED" -> {
                getMapData("coalesced.dmap")
                    ?: Logger.fatal("Missing server coalesced. Try redownloading the server")
            }

            else -> emptyMap()
        }
    }
    push(packet.respond {
        map("CONF", conf)
    })
}

/**
 * createDataConfig Creates a "data" configuration this contains information
 * such as the image hosting url and the galaxy at war http server host. This
 * is generated based on the environment config. There is also other configurations
 * here however I haven't made use of/documented the rest of these
 *
 * @return The map of the data configuration
 */
private fun createDataConfig(): Map<String, String> {
    val address = Environment.externalAddress
    val port = Environment.httpPort
    val host = if (port != 80) "$address:$port" else address
    return mapOf(
        // Replaces: https://wal.tools.gos.ea.com/wal/masseffect-gaw-pc
        "GAW_SERVER_BASE_URL" to "http://$host/gaw",
        // Replaces: http://eaassets-a.akamaihd.net/gameplayservices/prod/MassEffect/3/
        "IMG_MNGR_BASE_URL" to "http://$host/content/",
        "IMG_MNGR_MAX_BYTES" to "1048576",
        "IMG_MNGR_MAX_IMAGES" to "5",
        "JOB_THROTTLE_0" to "0",
        "JOB_THROTTLE_1" to "0",
        "JOB_THROTTLE_2" to "0",
        "MATCH_MAKING_RULES_VERSION" to "5",
        "MULTIPLAYER_PROTOCOL_VERSION" to "3",
        "TEL_DISABLE" to "**",
        "TEL_DOMAIN" to "pc/masseffect-3-pc-anon",
        "TEL_FILTER" to "-UION/****",
        "TEL_PORT" to "9988",
        "TEL_SEND_DELAY" to "15000",
        "TEL_SEND_PCT" to "75",
        "TEL_SERVER" to "127.0.0.1",
    )
}

/**
 * Handles the post authentication packet which responds with
 * information about the ticker and telemetry servers
 *
 * @param packet The packet requesting the post auth information
 */
@PacketHandler(Components.UTIL, Commands.POST_AUTH)
fun Session.handlePostAuth(packet: Packet) {
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

        +group("TELE") {
            text("ADRS", "127.0.0.1") // Server Address
            number("ANON", 0)
            text("DISA", "**")
            text("FILT", "-UION/****") // Telemetry filter?
            number("LOC", 1701725253)
            text("NOOK", "US,CA,MX")
            number("PORT", 9988)
            number("SDLY", 15000)
            text("SESS", "JMhnT9dXSED")
            text("SKEY", "")
            number("SPCT", 0x4B)
            text("STIM", "")
        }

        +group("TICK") {
            text("ADRS", "127.0.0.1")
            number("port", 9988)
            text("SKEY", "823287263,10.23.15.2:8999,masseffect-3-pc,10,50,50,50,50,0,12")
        }

        +group("UROP") {
            number("TMOP", 0x1)
            number("UID", sessionId)
        }
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
fun Session.handlePing(packet: Packet) {
    lastPingTime = System.currentTimeMillis()
    push(packet.respond {
        number("STIM", unixTimeSeconds())
    })
}

/**
 * Handles suspend user pings. The purpose of this is not yet understood,
 * and it requires further investigation before it can be documented
 *
 * @param packet The packet for suspend user ping
 */
@PacketHandler(Components.UTIL, Commands.SUSPEND_USER_PING)
fun Session.handleSuspendUserPing(packet: Packet) {
    push(
        when (packet.ulong("TVAL")) {
            0x1312D00uL -> packet.error(0x12D)
            0x55D4A80uL -> packet.error(0x12E)
            else -> packet.respond()
        }
    )
}


/**
 * Handles updating an individual setting provided by the client in the
 * form of a key value pair named KEY and DATA. This is for updating any
 * data stored on the player such as inventory, characters, classes etc
 *
 * @param packet The packet updating the setting
 */
@PacketHandler(Components.UTIL, Commands.USER_SETTINGS_SAVE)
fun Session.handleUserSettingsSave(packet: Packet) {
    val playerEntity = player ?: throw NotAuthenticatedException()
    val value = packet.text("DATA")
    val key = packet.text("KEY")
    playerEntity.setPlayerData(key, value)
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
fun Session.handleUserSettingsLoadAll(packet: Packet) {
    val playerEntity = player ?: throw NotAuthenticatedException()
    push(packet.respond {
        map("SMAP", playerEntity.createSettingsMap())
    })
}
