package com.jacobtread.kme.data

import com.jacobtread.blaze.TdfBuilder
import com.jacobtread.blaze.group
import com.jacobtread.kme.Environment
import com.jacobtread.kme.utils.logging.Logger
import java.io.IOException
import java.nio.file.Paths
import kotlin.io.path.writeText

/**
 * Data Pre constructed data and retrieval of data that's used throughout the app
 * some data is stored in chunk files and has to be loaded
 *
 * @constructor Create empty Data
 */
object Data {

    //region ME3 Data

    /**
     * createUserEntitlements Creates a List of user entitlements
     * this is the same for all players
     */
    fun createUserEntitlements(builder: TdfBuilder) {
        builder.list("NLST", listOf(
            group {
                text("DEVI", "")
                text("GDAY", "2013-03-04T22:16Z")
                text("GNAM", "ME3PCOffers")
                number("ID", 0xe962a115d7)
                number("ISCO", 0x0)
                number("PID", 0x0)
                text("PJID", "300241")
                number("PRCA", 0x2)
                text("PRID", "OFB-MASS:59712")
                number("STAT", 0x1)
                number("STRC", 0x0)
                text("TAG", "ME3_PRC_MP5")
                text("TDAY", "")
                number("TYPE", 0x5)
                number("UCNT", 0x0)
                number("VER", 0x0)
            },
            group {
                text("DEVI", "")
                text("GDAY", "2012-12-15T16:15Z")
                text("GNAM", "ME3PCOffers")
                number("ID", 0xe91655d5d7)
                number("ISCO", 0x0)
                number("PID", 0x0)
                text("PJID", "300241")
                number("PRCA", 0x2)
                text("PRID", "OFB-MASS:47872")
                number("STAT", 0x1)
                number("STRC", 0x0)
                text("TAG", "ME3_PRC_RESURGENCE")
                text("TDAY", "")
                number("TYPE", 0x5)
                number("UCNT", 0x0)
                number("VER", 0x0)
            },
            group {
                text("DEVI", "")
                text("GDAY", "2012-12-14T13:32Z")
                text("GNAM", "ME3GenOffers")
                number("ID", 0xe915dbc3d7)
                number("ISCO", 0x0)
                number("PID", 0x0)
                text("PJID", "303107")
                number("PRCA", 0x0)
                text("PRID", "")
                number("STAT", 0x1)
                number("STRC", 0x0)
                text("TAG", "ONLINE_ACCESS_GAW_PC")
                text("TDAY", "")
                number("TYPE", 0x1)
                number("UCNT", 0x0)
                number("VER", 0x0)
            },
            group {
                text("DEVI", "")
                text("GDAY", "2012-12-14T13:5Z")
                text("GNAM", "ME3PCOffers")
                number("ID", 0xe915aaefd7)
                number("ISCO", 0x0)
                number("PID", 0x0)
                text("PJID", "300241")
                number("PRCA", 0x2)
                text("PRID", "OFB-MASS:51074")
                number("STAT", 0x1)
                number("STRC", 0x0)
                text("TAG", "ME3_PRC_EXTENDEDCUT")
                text("TDAY", "")
                number("TYPE", 0x5)
                number("UCNT", 0x0)
                number("VER", 0x0)
            },
            group {
                text("DEVI", "")
                text("GDAY", "2012-12-14T13:5Z")
                text("GNAM", "ME3PCOffers")
                number("ID", 0xe915a7e297)
                number("ISCO", 0x0)
                number("PID", 0x0)
                text("PJID", "308426")
                number("PRCA", 0x2)
                text("PRID", "OFB-EAST:56562")
                number("STAT", 0x1)
                number("STRC", 0x0)
                text("TAG", "MET_BONUS_CONTENT")
                text("TDAY", "")
                number("TYPE", 0x5)
                number("UCNT", 0x0)
                number("VER", 0x0)
            },
            group {
                text("DEVI", "")
                text("GDAY", "2012-12-14T13:5Z")
                text("GNAM", "ME3PCOffers")
                number("ID", 0xe915a1c817)
                number("ISCO", 0x0)
                number("PID", 0x0)
                text("PJID", "303107")
                number("PRCA", 0x2)
                text("PRID", "DR:229644400")
                number("STAT", 0x1)
                number("STRC", 0x0)
                text("TAG", "PROJECT10_CODE_CONSUMED")
                text("TDAY", "")
                number("TYPE", 0x1)
                number("UCNT", 0x0)
                number("VER", 0x0)
            },
            group {
                text("DEVI", "")
                text("GDAY", "2012-12-14T13:5Z")
                text("GNAM", "ME3PCOffers")
                number("ID", 0xe9159ebad7)
                number("ISCO", 0x0)
                number("PID", 0x0)
                text("PJID", "303107")
                number("PRCA", 0x2)
                text("PRID", "DR:229644400")
                number("STAT", 0x1)
                number("STRC", 0x0)
                text("TAG", "ONLINE_ACCESS")
                text("TDAY", "")
                number("TYPE", 0x1)
                number("UCNT", 0x0)
                number("VER", 0x0)
            },
            group {
                text("DEVI", "")
                text("GDAY", "2012-12-15T16:16Z")
                text("GNAM", "ME3PCOffers")
                number("ID", 0xe910353b57)
                number("ISCO", 0x0)
                number("PID", 0x0)
                text("PJID", "300241")
                number("PRCA", 0x2)
                text("PRID", "OFB-MASS:49465")
                number("STAT", 0x1)
                number("STRC", 0x0)
                text("TAG", "ME3_PRC_REBELLION")
                text("TDAY", "")
                number("TYPE", 0x5)
                number("UCNT", 0x0)
                number("VER", 0x0)
            },
            group {
                text("DEVI", "")
                text("GDAY", "2012-12-15T16:16Z")
                text("GNAM", "ME3PCOffers")
                number("ID", 0xe90c3cff17)
                number("ISCO", 0x0)
                number("PID", 0x0)
                text("PJID", "300241")
                number("PRCA", 0x2)
                text("PRID", "OFB-MASS:51073")
                number("STAT", 0x1)
                number("STRC", 0x0)
                text("TAG", "ME3_PRC_EARTH")
                text("TDAY", "")
                number("TYPE", 0x5)
                number("UCNT", 0x0)
                number("VER", 0x0)
            },
            group {
                text("DEVI", "")
                text("GDAY", "2012-12-15T16:16Z")
                text("GNAM", "ME3PCOffers")
                number("ID", 0xe90b85e417)
                number("ISCO", 0x0)
                number("PID", 0x0)
                text("PJID", "300241")
                number("PRCA", 0x2)
                text("PRID", "OFB-MASS:52000")
                number("STAT", 0x1)
                number("STRC", 0x0)
                text("TAG", "ME3_PRC_GOBIG")
                text("TDAY", "")
                number("TYPE", 0x5)
                number("UCNT", 0x0)
                number("VER", 0x0)
            }
        ))
    }

    /**
     * createDataConfig Creates a "data" configuration this contains information
     * such as the image hosting url and the galaxy at war http server host. This
     * is generated based on the environment config. There is also other configurations
     * here however I haven't made use of/documented the rest of these
     *
     * @return The map of the data configuration
     */
    fun createDataConfig(): Map<String, String> {
        val address = Environment.externalAddress
        val port = Environment.httpPort
        val host = if (port != 80) "$address:$port" else address
        return mapOf(
            "GAW_SERVER_BASE_URL" to "http://$host/gaw",
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

    fun getEntitlementMap(): Map<String, String> {
        val out = LinkedHashMap<String, String>()
        val inputStream = Data::class.java.getResourceAsStream("/data/entitlements.properties")
            ?: Logger.fatal("Missing entitlements file... Try redownloading the server")
        val reader = inputStream.bufferedReader(Charsets.UTF_8)
        reader.use {
            while (true) {
                val line = reader.readLine() ?: break
                val parts = line.split('=', limit = 2)
                if (parts.size < 2) continue
                out[parts[0]] = parts[1]
            }
        }
        return out
    }

    //endregion

    fun createDimeResponse(): Map<String, String> {
        try {
            val stream = Data::class.java.getResourceAsStream("/data/dime.xml")
                ?: throw IOException("Missing internal resource: data/dime.xml")
            val dimeBytes = stream.use { stream.readAllBytes() }
            return mapOf("Config" to String(dimeBytes, Charsets.UTF_8))
        } catch (e: IOException) {
            throw IOException("Missing internal resource: data/dime.xml", e)
        }
    }

    fun loadBiniCompressed(): Map<String, String> {
        val value = loadChunkedFile("data/bini.bin.chunked")
        return value ?: emptyMap()
    }

    fun getTalkFileConfig(lang: String): Map<String, String> {
        var map = loadChunkedFile("data/tlk/$lang.tlk.chunked")
        if (map == null) map = loadChunkedFile("data/tlk/default.tlk.chunked")
        if (map == null) map = emptyMap()
        return map
    }

    private fun loadChunkedFile(path: String): Map<String, String>? {
        val inputStream = Data::class.java.getResourceAsStream("/$path")
            ?: return null
        val out = LinkedHashMap<String, String>()
        val reader = inputStream.bufferedReader(Charsets.UTF_8)
        reader.use {
            while (true) {
                val line = reader.readLine() ?: break
                val parts = line.split(':', limit = 2)
                if (parts.size < 2) continue
                out[parts[0]] = parts[1]
            }
        }
        return out
    }
}
