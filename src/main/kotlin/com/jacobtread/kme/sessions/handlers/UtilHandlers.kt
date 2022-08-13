package com.jacobtread.kme.sessions.handlers

import com.jacobtread.blaze.*
import com.jacobtread.blaze.annotations.PacketHandler
import com.jacobtread.blaze.packet.Packet
import com.jacobtread.kme.Environment
import com.jacobtread.kme.data.Data
import com.jacobtread.kme.data.blaze.Commands
import com.jacobtread.kme.data.blaze.Components
import com.jacobtread.kme.sessions.Session
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
        Data.getTalkFileConfig(lang)
    } else {
        when (type) {
            "ME3_DATA" -> Data.createDataConfig() // Configurations for GAW, images and others
            "ME3_MSG" -> emptyMap() // Custom multiplayer messages
            "ME3_ENT" -> Data.getEntitlementMap() // Entitlements
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
