package com.jacobtread.kme.data

import com.jacobtread.blaze.TdfBuilder
import com.jacobtread.blaze.group
import com.jacobtread.blaze.tdf.types.GroupTdf
import com.jacobtread.kme.Environment
import com.jacobtread.kme.utils.logging.Logger
import java.io.IOException

/**
 * Data Pre constructed data and retrieval of data that's used throughout the app
 * some data is stored in chunk files and has to be loaded
 *
 * @constructor Create empty Data
 */
object Data {

    //region ME3 Data

    private fun createEntitlement(
        name: String,
        id: Long,
        pjid: String,
        prca: Int,
        prid: String,
        tag: String,
        type: Int,
    ): GroupTdf {
        return group {
            text("DEVI", "")
            text("GDAY", "2012-12-15T16:15Z")
            text("GNAM", name)
            number("ID", id)
            number("ISCO", 0)
            number("PID", 0)
            text("PJID", pjid)
            number("PRCA", prca)
            text("PRID", prid)
            number("STAT", 1)
            number("STRC", 0)
            text("TAG", tag)
            text("TDAY", "")
            number("TYPE", type)
            number("UCNT", 0)
            number("VER", 0)
        }
    }

    /**
     * createUserEntitlements Creates a List of user entitlements
     * this is the same for all players
     */
    fun createUserEntitlements(builder: TdfBuilder) {
        val pcTag = "ME3PCOffers"
        val generalTag = "ME3GenOffers"

        builder.list(
            "NLST", listOf(
                // Project 10 = $10 Multiplayer Pass (Entitlement Required For Online Access)
                createEntitlement(pcTag, 0xec5090c43f, "303107", 2, "DR:229644400", "PROJECT10_CODE_CONSUMED", 1),
                createEntitlement(pcTag, 0xec3e4d793f, "304141", 2, "DR:230773600", "PROJECT10_CODE_CONSUMED_LE1", 1),

                // Jeeze so many online pass entitlements
                createEntitlement(pcTag, 0xec50b255ff, "300241", 2, "OFB-MASS:44370", "ONLINE_ACCESS", 1),
                createEntitlement(pcTag, 0xec50a620ff, "300241", 2, "OFB-MASS:49465", "ONLINE_ACCESS", 1),
                createEntitlement(pcTag, 0xec508db6ff, "303107", 2, "DR:229644400", "ONLINE_ACCESS", 1),
                createEntitlement(pcTag, 0xec3e5393bf, "300241", 2, "OFB-EAST:46112", "ONLINE_ACCESS", 1),
                createEntitlement(pcTag, 0xec3e50867f, "304141", 2, "DR:230773600", "ONLINE_ACCESS", 1),
                createEntitlement(generalTag, 0xec4495bfff, "303107", 0, "", "ONLINE_ACCESS_GAW_PC", 1),
                createEntitlement(generalTag, 0xea234c3e7f, "303107", 2, "", "ONLINE_ACCESS_GAW_XBL2", 1),

                // Singleplayer DLC
                createEntitlement(pcTag, 0xec3e62d5ff, "300241", 2, "OFB-MASS:51074", "ME3_PRC_EXTENDEDCUT", 5), // Extended Cut DLC
                createEntitlement(pcTag, 0xec50b5633f, "300241", 2, "OFB-MASS:44370", "ME3_PRC_PROTHEAN", 5), // From Ashes DLC
                createEntitlement(pcTag, 0xec3e56a0ff, "300241", 2, "OFB-EAST:46112", "ME3_PRC_PROTHEAN", 5), // From Ashes DLC
                createEntitlement(pcTag, 0xec50b8707f, "300241", 2, "OFB-MASS:52001", "ME3_PRC_LEVIATHAN", 5), // Leviathan DLC
                createEntitlement(pcTag, 0xec50ac3b7f, "300241", 2, "OFB-MASS:55146", "ME3_PRC_OMEGA", 5), // Omega DLC
                createEntitlement(pcTag, 0xec50af48bf, "300241", 2, "OFB-MASS:57550", "ME3_PRC_CITADEL", 5), // Citadel DLC
                createEntitlement(pcTag, 0xec5093d17f, "300241", 2, "OFB-EAST:58040", "MET_BONUS_CONTENT_DW", 5),

                // Singleplayer Packs
                createEntitlement(pcTag, 0xec50bb7dbf, "300241", 2, "OFB-MASS:56984", "ME3_MTX_APP01", 5), // Alternate Appearance Pack 1
                createEntitlement(pcTag, 0xec5099ebff, "300241", 2, "OFB-MASS:49032", "ME3_MTX_GUN01", 5), // Firefight Pack
                createEntitlement(pcTag, 0xec50c1983f, "300241", 2, "OFB-MASS:55147", "ME3_MTX_GUN02", 5), // Groundside Resistance Pack

                // Multiplayer DLC
                createEntitlement(pcTag, 0xec50a0067f, "300241", 2, "OFB-MASS:47872", "ME3_PRC_RESURGENCE", 5), // Resurgence DLC
                createEntitlement(pcTag, 0xec50a92e3f, "300241", 2, "OFB-MASS:49465", "ME3_PRC_REBELLION", 5), // Rebellion DLC
                createEntitlement(pcTag, 0xec5096debf, "300241", 2, "OFB-MASS:51073", "ME3_PRC_EARTH", 5), // Earth DLC
                createEntitlement(pcTag, 0xec509cf93f, "300241", 2, "OFB-MASS:52000", "ME3_PRC_GOBIG", 5), // Retaliation DLC
                createEntitlement(pcTag, 0xec50a313bf, "300241", 2, "OFB-MASS:59712", "ME3_PRC_MP5", 5), // Recokoning DLC

                // Collectors Edition
                createEntitlement(pcTag, 0xec3e5fc8bf, "300241", 2, "OFB-MASS:46484", "ME3_MTX_COLLECTORS_EDITION", 5),
                createEntitlement(pcTag, 0xec3e5cbb7f, "300241", 2, "OFB-MASS:46483", "ME3_MTX_DIGITAL_ART_BOOKS", 5),
                createEntitlement(generalTag, 0xec3e59ae3f, "300241", 2, "OFB-MASS:46482", "ME3_MTX_SOUNDTRACK", 5),

                // Darkhorse Redeem Code (Character boosters and Collector Assault Rifle)
                createEntitlement(pcTag, 0xec50be8aff, "300241", 2, "OFB-MASS:61524", "ME3_PRC_DARKHORSECOMIC", 5),
            )
        )
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
