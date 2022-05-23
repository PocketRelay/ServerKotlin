package com.jacobtread.kme.data

import com.jacobtread.kme.Config
import com.jacobtread.kme.blaze.lazyPacketBody
import com.jacobtread.kme.blaze.group
import io.netty.buffer.ByteBuf
import java.io.BufferedReader
import java.io.IOException
import kotlin.random.Random

/**
 * Data Pre constructed data and retrieval of data that's used throughout the app
 * some data is stored in chunk files and has to be loaded
 *
 * @constructor Create empty Data
 */
object Data {

    const val TELE_DISA =
        "AD,AF,AG,AI,AL,AM,AN,AO,AQ,AR,AS,AW,AX,AZ,BA,BB,BD,BF,BH,BI,BJ,BM,BN,BO,BR,BS,BT,BV,BW,BY,BZ,CC,CD,CF,CG,CI,CK,CL,CM,CN,CO,CR,CU,CV,CX,DJ,DM,DO,DZ,EC,EG,EH,ER,ET,FJ,FK,FM,FO,GA,GD,GE,GF,GG,GH,GI,GL,GM,GN,GP,GQ,GS,GT,GU,GW,GY,HM,HN,HT,ID,IL,IM,IN,IO,IQ,IR,IS,JE,JM,JO,KE,KG,KH,KI,KM,KN,KP,KR,KW,KY,KZ,LA,LB,LC,LI,LK,LR,LS,LY,MA,MC,MD,ME,MG,MH,ML,MM,MN,MO,MP,MQ,MR,MS,MU,MV,MW,MY,MZ,NA,NC,NE,NF,NG,NI,NP,NR,NU,OM,PA,PE,PF,PG,PH,PK,PM,PN,PS,PW,PY,QA,RE,RS,RW,SA,SB,SC,SD,SG,SH,SJ,SL,SM,SN,SO,SR,ST,SV,SY,SZ,TC,TD,TF,TG,TH,TJ,TK,TL,TM,TN,TO,TT,TV,TZ,UA,UG,UM,UY,UZ,VA,VC,VE,VG,VN,VU,WF,WS,YE,YT,ZM,ZW,ZZ"
    const val NAT_TYPE = 4
    val SKEY = String(
        byteArrayOf(
            94, -118, -53, -35, -8, -20, -63, -107, -104, -103, -7, -108, -64, -83, -18,
            -4, -50, -92, -121, -34, -118, -90, -50, -36, -80, -18, -24, -27, -77, -11,
            -83, -102, -78, -27, -28, -79, -103, -122, -57, -114, -101, -80, -12, -64, -127,
            -93, -89, -115, -100, -70, -62, -119, -45, -61, -84, -104, -106, -92, -32, -64,
            -127, -125, -122, -116, -104, -80, -32, -52, -119, -109, -58, -52, -102, -28, -56,
            -103, -29, -126, -18, -40, -105, -19, -62, -51, -101, -41, -52, -103, -77, -27,
            -58, -47, -21, -78, -90, -117, -72, -29, -40, -60, -95, -125, -58, -116, -100,
            -74, -16, -48, -63, -109, -121, -53, -78, -18, -120, -107, -46, -128, -128
        ),
        Charsets.UTF_8
    )
    const val SKEY2 = "11229301_9b171d92cc562b293e602ee8325612e7"
    val CIDS = listOf(1, 25, 4, 28, 7, 9, 63490, 30720, 15, 30721, 30722, 30723, 30725, 30726, 2000)

    /**
     * USER_ENTITLEMENTS List of user entitlements this is the same for all players
     * and gets requested multiple times so for performance sake this is created and
     * converted to a byte array upon first access to prevent this large content
     * from needing to be rebuilt over and over again every request
     */
    @Suppress("SpellCheckingInspection")
    val USER_ENTITLEMENTS: ByteBuf by lazyPacketBody {
        list("NLST", listOf(
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


    //region ME3 Data

    fun createDataConfig(config: Config): Map<String, String> {
        val address = config.address
        val port = config.ports.http
        val host = if (port != 80) {
            "$address:$port"
        } else {
            address
        }
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
            "TEL_DISABLE" to "AD,AF,AG,AI,AL,AM,AN,AO,AQ,AR,AS,AW,AX,AZ,BA,BB,BD,BF,BH,BI,BJ,BM,BN,BO,BR,BS,BT,BV,BW,BY,BZ,CC,CD,CF,CG,CI,CK,CL,CM,CN,CO,CR,CU,CV,CX,DJ,DM,DO,DZ,EC,EG,EH,ER,ET,FJ,FK,FM,FO,GA,GD,GE,GF,GG,GH,GI,GL,GM,GN,GP,GQ,GS,GT,GU,GW,GY,HM,HN,HT,ID,IL,IM,IN,IO,IQ,IR,IS,JE,JM,JO,KE,KG,KH,KI,KM,KN,KP,KR,KW,KY,KZ,LA,LB,LC,LI,LK,LR,LS,LY,MA,MC,MD,ME,MG,MH,ML,MM,MN,MO,MP,MQ,MR,MS,MU,MV,MW,MY,MZ,NA,NC,NE,NF,NG,NI,NP,NR,NU,OM,PA,PE,PF,PG,PH,PK,PM,PN,PS,PW,PY,QA,RE,RS,RW,SA,SB,SC,SD,SG,SH,SJ,SL,SM,SN,SO,SR,ST,SV,SY,SZ,TC,TD,TF,TG,TH,TJ,TK,TL,TM,TN,TO,TT,TV,TZ,UA,UG,UM,UY,UZ,VA,VC,VE,VG,VN,VU,WF,WS,YE,YT,ZM,ZW,ZZ",
            "TEL_DOMAIN" to "pc/masseffect-3-pc-anon",
            "TEL_FILTER" to "-UION/****",
            "TEL_PORT" to "9988",
            "TEL_SEND_DELAY" to "15000",
            "TEL_SEND_PCT" to "75",
            "TEL_SERVER" to "159.153.235.32",
        )
    }

    fun createServerMessage(): Map<String, String> = mapOf(
        "MSG_1_endDate" to "10:03:2025",
        "MSG_1_image" to "Promo_n7.dds",
        "MSG_1_message" to "KME Server is working!!",
        "MSG_1_message_de" to "KME Server is working!!",
        "MSG_1_message_es" to "KME Server is working!!",
        "MSG_1_message_fr" to "KME Server is working!!",
        "MSG_1_message_it" to "KME Server is working!!",
        "MSG_1_message_ja" to "KME Server is working!!",
        "MSG_1_message_pl" to "KME Server is working!!",
        "MSG_1_message_ru" to "KME Server is working!!",
        "MSG_1_priority" to "201",
        "MSG_1_title" to "KME Server",
        "MSG_1_title_de" to "KME Server",
        "MSG_1_title_es" to "KME Server",
        "MSG_1_title_fr" to "KME Server",
        "MSG_1_title_it" to "KME Server",
        "MSG_1_title_ja" to "KME Server",
        "MSG_1_title_pl" to "KME Server",
        "MSG_1_title_ru" to "KME Server",
        "MSG_1_trackingId" to Random.nextInt(0, 15).toString(),
        "MSG_1_type" to "8",
    )

    fun createEntitlementMap(): Map<String, String> = linkedMapOf(
        "CERBERUS_OFFER_ID" to "101",
        "ENT_100_entitlement" to "ME3_PRC_MP5",
        "ENT_100_group" to "ME3PCContent",
        "ENT_100_ID" to "3225",
        "ENT_100_param" to "MP_DLC=TRUE&stringref=782577",
        "ENT_101_entitlement" to "BWE_ANCILLARY_DH_SX3_FIGHTER",
        "ENT_101_group" to "ME3GenAncillary",
        "ENT_101_ID" to "1250033",
        "ENT_102_entitlement" to "BWE_ANCILLARY_TALI_BISHOUJO",
        "ENT_102_group" to "ME3GenAncillary",
        "ENT_102_ID" to "1250034",
        "ENT_103_entitlement" to "BWE_ANCILLARY_N7_WATCH",
        "ENT_103_group" to "ME3GenAncillary",
        "ENT_103_ID" to "1250035",
        "ENT_104_entitlement" to "BWE_ANCILLARY_N7_SNEAKER",
        "ENT_104_group" to "ME3GenAncillary",
        "ENT_104_ID" to "1250036",
        "ENT_105_entitlement" to "BWE_ANCILLARY_NORMANDY_SNOWBOARD",
        "ENT_105_group" to "ME3GenAncillary",
        "ENT_105_ID" to "1250037",
        "ENT_106_entitlement" to "BWE_ANCILLARY_GARRUS_BUST",
        "ENT_106_group" to "ME3GenAncillary",
        "ENT_106_ID" to "1250038",
        "ENT_107_entitlement" to "BWE_ANCILLARY_MORDIN_BUST",
        "ENT_107_group" to "ME3GenAncillary",
        "ENT_107_ID" to "1250039",
        "ENT_108_entitlement" to "ME3_PRO_COMMEND_21",
        "ENT_108_group" to "ME3GenContent",
        "ENT_108_ID" to "1500021",
        "ENT_109_entitlement" to "ME3_PRO_CHALLENGE_21",
        "ENT_109_group" to "ME3PCOffers",
        "ENT_109_ID" to "1500121",
        "ENT_10_entitlement" to "ME3_PRO_MAGICAL_01",
        "ENT_10_group" to "ME3GenOffers",
        "ENT_10_ID" to "2000000",
        "ENT_110_entitlement" to "ME3_PRO_COMMEND_22",
        "ENT_110_group" to "ME3GenContent",
        "ENT_110_ID" to "1500022",
        "ENT_111_entitlement" to "ME3_PRO_CHALLENGE_22",
        "ENT_111_group" to "ME3PCOffers",
        "ENT_111_ID" to "1500122",
        "ENT_112_entitlement" to "ME3_PRO_COMMEND_23",
        "ENT_112_group" to "ME3GenContent",
        "ENT_112_ID" to "1500023",
        "ENT_113_entitlement" to "ME3_PRO_CHALLENGE_23",
        "ENT_113_group" to "ME3PCOffers",
        "ENT_113_ID" to "1500123",
        "ENT_114_entitlement" to "BWE_ANCILLARY_ME3_FEMSHEP_BISHOUJO",
        "ENT_114_group" to "ME3GenAncillary",
        "ENT_114_ID" to "1250040",
        "ENT_11_entitlement" to "ME3_PRO_COMMEND_09",
        "ENT_11_group" to "ME3GenContent",
        "ENT_11_ID" to "1500008",
        "ENT_12_entitlement" to "ME3_PRO_COMMEND_10",
        "ENT_12_group" to "ME3GenContent",
        "ENT_12_ID" to "1500009",
        "ENT_13_entitlement" to "ME3_CS_CREDITS_01",
        "ENT_13_group" to "ME3GenContent",
        "ENT_13_ID" to "1300000",
        "ENT_14_entitlement" to "ME3_PRC_REBELLION",
        "ENT_14_group" to "ME3PCContent",
        "ENT_14_ID" to "2500",
        "ENT_14_param" to "MP_DLC=TRUE&stringref=735694&MP_Maps=BioP_MPJngl&MP_Maps=BioP_MPThes",
        "ENT_15_entitlement" to "ME3_REINF_PACK_PURCHASED",
        "ENT_15_group" to "ME3PCOffers",
        "ENT_15_ID" to "4000112",
        "ENT_15_param" to "Grant:Group=ME3PCOffers&Grant:Tag=ME3_REINF_PACK_PURCHASED",
        "ENT_16_entitlement" to "ME3_PRO_RECRU_REPACK_LOYALTYPACK_01",
        "ENT_16_group" to "ME3GenContent",
        "ENT_16_ID" to "1100001",
        "ENT_17_entitlement" to "ME3_PRO_RECRU_REPACK_LOYALTYPACK_02",
        "ENT_17_group" to "ME3GenContent",
        "ENT_17_ID" to "1100002",
        "ENT_18_entitlement" to "ME3_PRO_RECRU_REPACK_LOYALTYPACK_03",
        "ENT_18_group" to "ME3GenContent",
        "ENT_18_ID" to "1100003",
        "ENT_19_entitlement" to "ME3_PRO_RECRU_REPACK_LOYALTYPACK_04",
        "ENT_19_group" to "ME3GenContent",
        "ENT_19_ID" to "1100004",
        "ENT_1_group" to "ME3PCOffers",
        "ENT_20_entitlement" to "ME3_PRO_RECRU_REPACK_LOYALTYPACK_05",
        "ENT_20_group" to "ME3GenContent",
        "ENT_20_ID" to "1100005",
        "ENT_21_entitlement" to "BWE_ANCILLARY_LIARA_STATUE",
        "ENT_21_group" to "ME3GenAncillary",
        "ENT_21_ID" to "1250001",
        "ENT_22_entitlement" to "BWE_ANCILLARY_N7_MOUSE",
        "ENT_22_group" to "ME3GenAncillary",
        "ENT_22_ID" to "1250002",
        "ENT_23_entitlement" to "BWE_ANCILLARY_N7_KEYBOARD",
        "ENT_23_group" to "ME3GenAncillary",
        "ENT_23_ID" to "1250003",
        "ENT_24_entitlement" to "BWE_ANCILLARY_N7_MOUSEPAD",
        "ENT_24_group" to "ME3GenAncillary",
        "ENT_24_ID" to "1250004",
        "ENT_25_entitlement" to "BWE_ANCILLARY_N7_XBOX_CONTROLLER",
        "ENT_25_group" to "ME3GenAncillary",
        "ENT_25_ID" to "1250005",
        "ENT_26_entitlement" to "BWE_ANCILLARY_N7_XBOX_HEADSET",
        "ENT_26_group" to "ME3GenAncillary",
        "ENT_26_ID" to "1250006",
        "ENT_27_entitlement" to "BWE_ANCILLARY_N7_LAPTOP_BAG",
        "ENT_27_group" to "ME3GenAncillary",
        "ENT_27_ID" to "1250007",
        "ENT_28_entitlement" to "BWE_ANCILLARY_N7_IPHONE_CASE",
        "ENT_28_group" to "ME3GenAncillary",
        "ENT_28_ID" to "1250008",
        "ENT_29_entitlement" to "BWE_ANCILLARY_N7_IPAD_CASE",
        "ENT_29_group" to "ME3GenAncillary",
        "ENT_29_ID" to "1250009",
        "ENT_2_group" to "ME3PCContent",
        "ENT_30_entitlement" to "BWE_ANCILLARY_MIRANDA_FIGURE",
        "ENT_30_group" to "ME3GenAncillary",
        "ENT_30_ID" to "1250010",
        "ENT_31_entitlement" to "BWE_ANCILLARY_MORDIN_FIGURE",
        "ENT_31_group" to "ME3GenAncillary",
        "ENT_31_ID" to "1250011",
        "ENT_32_entitlement" to "BWE_ANCILLARY_LEGION_FIGURE",
        "ENT_32_group" to "ME3GenAncillary",
        "ENT_32_ID" to "1250012",
        "ENT_33_entitlement" to "BWE_ANCILLARY_GARRUS_FIGURE",
        "ENT_33_group" to "ME3GenAncillary",
        "ENT_33_ID" to "1250013",
        "ENT_34_entitlement" to "BWE_ANCILLARY_TALI_FIGURE",
        "ENT_34_group" to "ME3GenAncillary",
        "ENT_34_ID" to "1250014",
        "ENT_35_entitlement" to "BWE_ANCILLARY_GRUNT_FIGURE",
        "ENT_35_group" to "ME3GenAncillary",
        "ENT_35_ID" to "1250015",
        "ENT_36_entitlement" to "BWE_ANCILLARY_THANE_FIGURE",
        "ENT_36_group" to "ME3GenAncillary",
        "ENT_36_ID" to "1250016",
        "ENT_37_entitlement" to "BWE_ANCILLARY_SHEPARD_FIGURE",
        "ENT_37_group" to "ME3GenAncillary",
        "ENT_37_ID" to "1250017",
        "ENT_38_entitlement" to "BWE_ANCILLARY_APPAREL",
        "ENT_38_group" to "ME3GenAncillary",
        "ENT_38_ID" to "1250018",
        "ENT_39_entitlement" to "BWE_ANCILLARY_DH_ARTBOOK",
        "ENT_39_group" to "ME3GenAncillary",
        "ENT_39_ID" to "1250019",
        "ENT_3_group" to "ME3GenOffers",
        "ENT_40_entitlement" to "ME3_PRC_RESURGENCE",
        "ENT_40_group" to "ME3PCContent",
        "ENT_40_ID" to "2300",
        "ENT_40_param" to "MP_DLC=TRUE&stringref=728589&MP_Maps=BioP_MPGeth&MP_Maps=BioP_MPMoon",
        "ENT_41_entitlement" to "ME3_PRO_MAGICAL_02",
        "ENT_41_group" to "ME3GenOffers",
        "ENT_41_ID" to "2000001",
        "ENT_42_entitlement" to "BW_ANCILLARY_ALLIANCE_NORMANDY",
        "ENT_42_group" to "ME3GenAncillary",
        "ENT_42_ID" to "1250020",
        "ENT_43_entitlement" to "BW_ANCILLARY_CERBERUS_NORMANDY",
        "ENT_43_group" to "ME3GenAncillary",
        "ENT_43_ID" to "1250021",
        "ENT_44_entitlement" to "BWE_ANCILLARY_TRIFORCE_M8",
        "ENT_44_group" to "ME3GenAncillary",
        "ENT_44_ID" to "1250022",
        "ENT_45_entitlement" to "BWE_ANCILLARY_CALIBUR11_CONSOLE",
        "ENT_45_group" to "ME3GenAncillary",
        "ENT_45_ID" to "1250023",
        "ENT_46_entitlement" to "BWE_ANCILLARY_SILVER_NORMANDY",
        "ENT_46_group" to "ME3GenAncillary",
        "ENT_46_ID" to "1250024",
        "ENT_47_entitlement" to "BWE_ANCILLARY_DH_DIGARTBOOK",
        "ENT_47_group" to "ME3GenAncillary",
        "ENT_47_ID" to "1250025",
        "ENT_48_entitlement" to "BWE_ANCILLARY_DH_DIGINVASION",
        "ENT_48_group" to "ME3GenAncillary",
        "ENT_48_ID" to "1250026",
        "ENT_49_entitlement" to "ME3_PRO_COMMEND_01",
        "ENT_49_group" to "ME3GenContent",
        "ENT_49_ID" to "1500000",
        "ENT_4_group" to "ME3GenContent",
        "ENT_50_entitlement" to "ME3_PRO_COMMEND_O2",
        "ENT_50_group" to "ME3GenContent",
        "ENT_50_ID" to "1500001",
        "ENT_51_entitlement" to "ME3_PRO_COMMEND_03",
        "ENT_51_group" to "ME3GenContent",
        "ENT_51_ID" to "1500002",
        "ENT_52_entitlement" to "ME3_PRO_COMMEND_04",
        "ENT_52_group" to "ME3GenContent",
        "ENT_52_ID" to "1500003",
        "ENT_53_entitlement" to "ME3_PRO_COMMEND_05",
        "ENT_53_group" to "ME3GenContent",
        "ENT_53_ID" to "1500004",
        "ENT_54_entitlement" to "ME3_PRO_COMMEND_06",
        "ENT_54_group" to "ME3GenContent",
        "ENT_54_ID" to "1500005",
        "ENT_55_entitlement" to "ME3_PRO_COMMEND_07",
        "ENT_55_group" to "ME3GenContent",
        "ENT_55_ID" to "1500006",
        "ENT_56_entitlement" to "ME3_PRO_COMMEND_08",
        "ENT_56_group" to "ME3GenContent",
        "ENT_56_ID" to "1500007",
        "ENT_57_entitlement" to "ME3_PRC_EARTH",
        "ENT_57_group" to "ME3PCContent",
        "ENT_57_ID" to "2700",
        "ENT_57_param" to "MP_DLC=TRUE&stringref=747202&MP_Maps=BioP_MPHosp&MP_Maps=BioP_MPOcean&MP_Maps=BioP_MPRoad",
        "ENT_58_entitlement" to "ME3_MP2_PRESSUNLOCK",
        "ENT_58_group" to "ME3GenContent",
        "ENT_58_ID" to "1300001",
        "ENT_59_entitlement" to "BWE_ANCILLARY_FEMSHEP_STATUE",
        "ENT_59_group" to "ME3GenAncillary",
        "ENT_59_ID" to "1250028",
        "ENT_5_group" to "ME3GenAncillary",
        "ENT_60_entitlement" to "BWE_ANCILLARY_SHEPARD_STATUE",
        "ENT_60_group" to "ME3GenAncillary",
        "ENT_60_ID" to "1250029",
        "ENT_61_entitlement" to "BWE_ANCILLARY_TRIFORCE_M3",
        "ENT_61_group" to "ME3GenAncillary",
        "ENT_61_ID" to "1250030",
        "ENT_62_entitlement" to "ME3_PRC_GOBIG",
        "ENT_62_group" to "ME3PCContent",
        "ENT_62_ID" to "3050",
        "ENT_62_param" to "MP_DLC=TRUE&stringref=768382&MP_Maps=BioP_MPNov2&MP_Maps=BioP_MPCer2&MP_Maps=BioP_MPDish2&MP_Maps=BioP_MPRctr2&MP_Maps=BioP_MPSlum2&MP_Maps=BioP_MPTowr2&MP_Maps=BioP_MPTowr3&MP_Maps=BioP_MPNov3&MP_Maps=BioP_MPCer3",
        "ENT_63_entitlement" to "BWE_ANCILLARY_ASHLEY_STATUE",
        "ENT_63_group" to "ME3GenAncillary",
        "ENT_63_ID" to "1250031",
        "ENT_64_entitlement" to "ME3_CS_CREDITS_02",
        "ENT_64_group" to "ME3GenContent",
        "ENT_64_ID" to "1300005",
        "ENT_65_entitlement" to "ME3_CS_CREDITS_03",
        "ENT_65_group" to "ME3GenContent",
        "ENT_65_ID" to "1300006",
        "ENT_66_entitlement" to "ME3_PRO_COMMEND_11",
        "ENT_66_group" to "ME3GenContent",
        "ENT_66_ID" to "1500010",
        "ENT_67_entitlement" to "ME3_PRO_COMMEND_12",
        "ENT_67_group" to "ME3GenContent",
        "ENT_67_ID" to "1500011",
        "ENT_68_entitlement" to "ME3_PRO_COMMEND_13",
        "ENT_68_group" to "ME3GenContent",
        "ENT_68_ID" to "1500012",
        "ENT_69_entitlement" to "ME3_PRO_COMMEND_14",
        "ENT_69_group" to "ME3GenContent",
        "ENT_69_ID" to "1500013",
        "ENT_6_entitlement" to "BF3:PC:ADDSVETRANK",
        "ENT_6_group" to "AddsVetRank",
        "ENT_6_ID" to "1150000",
        "ENT_70_entitlement" to "ME3_PRO_COMMEND_15",
        "ENT_70_group" to "ME3GenContent",
        "ENT_70_ID" to "1500014",
        "ENT_71_entitlement" to "ME3_PRO_COMMEND_16",
        "ENT_71_group" to "ME3GenContent",
        "ENT_71_ID" to "1500015",
        "ENT_72_entitlement" to "ME3_PRO_COMMEND_17",
        "ENT_72_group" to "ME3GenContent",
        "ENT_72_ID" to "1500016",
        "ENT_73_entitlement" to "ME3_PRO_COMMEND_18",
        "ENT_73_group" to "ME3GenContent",
        "ENT_73_ID" to "1500017",
        "ENT_74_entitlement" to "ME3_PRO_COMMEND_19",
        "ENT_74_group" to "ME3GenContent",
        "ENT_74_ID" to "1500018",
        "ENT_75_entitlement" to "ME3_PRO_COMMEND_20",
        "ENT_75_group" to "ME3GenContent",
        "ENT_75_ID" to "1500019",
        "ENT_76_entitlement" to "ME3_PRO_CHALLENGE_01",
        "ENT_76_group" to "ME3PCOffers",
        "ENT_76_ID" to "1500101",
        "ENT_77_entitlement" to "ME3_PRO_CHALLENGE_02",
        "ENT_77_group" to "ME3PCOffers",
        "ENT_77_ID" to "1500102",
        "ENT_78_entitlement" to "ME3_PRO_CHALLENGE_03",
        "ENT_78_group" to "ME3PCOffers",
        "ENT_78_ID" to "1500103",
        "ENT_79_entitlement" to "ME3_PRO_CHALLENGE_04",
        "ENT_79_group" to "ME3PCOffers",
        "ENT_79_ID" to "1500104",
        "ENT_7_entitlement" to "PROJECT10_CODE_CONSUMED",
        "ENT_7_group" to "ME3PCOffers",
        "ENT_7_ID" to "101",
        "ENT_80_entitlement" to "ME3_PRO_CHALLENGE_05",
        "ENT_80_group" to "ME3PCOffers",
        "ENT_80_ID" to "1500105",
        "ENT_81_entitlement" to "ME3_PRO_CHALLENGE_06",
        "ENT_81_group" to "ME3PCOffers",
        "ENT_81_ID" to "1500106",
        "ENT_82_entitlement" to "ME3_PRO_CHALLENGE_07",
        "ENT_82_group" to "ME3PCOffers",
        "ENT_82_ID" to "1500107",
        "ENT_83_entitlement" to "ME3_PRO_CHALLENGE_08",
        "ENT_83_group" to "ME3PCOffers",
        "ENT_83_ID" to "1500108",
        "ENT_84_entitlement" to "ME3_PRO_CHALLENGE_09",
        "ENT_84_group" to "ME3PCOffers",
        "ENT_84_ID" to "1500109",
        "ENT_85_entitlement" to "ME3_PRO_CHALLENGE_10",
        "ENT_85_group" to "ME3PCOffers",
        "ENT_85_ID" to "1500110",
        "ENT_86_entitlement" to "ME3_PRO_CHALLENGE_11",
        "ENT_86_group" to "ME3PCOffers",
        "ENT_86_ID" to "1500111",
        "ENT_87_entitlement" to "ME3_PRO_CHALLENGE_12",
        "ENT_87_group" to "ME3PCOffers",
        "ENT_87_ID" to "1500112",
        "ENT_88_entitlement" to "ME3_PRO_CHALLENGE_13",
        "ENT_88_group" to "ME3PCOffers",
        "ENT_88_ID" to "1500113",
        "ENT_89_entitlement" to "ME3_PRO_CHALLENGE_14",
        "ENT_89_group" to "ME3PCOffers",
        "ENT_89_ID" to "1500114",
        "ENT_8_entitlement" to "PROJECT10_CODE_CONSUMED_LE1",
        "ENT_8_group" to "ME3PCOffers",
        "ENT_8_ID" to "102",
        "ENT_8_param" to "BWID=101",
        "ENT_90_entitlement" to "ME3_PRO_CHALLENGE_15",
        "ENT_90_group" to "ME3PCOffers",
        "ENT_90_ID" to "1500115",
        "ENT_91_entitlement" to "ME3_PRO_CHALLENGE_16",
        "ENT_91_group" to "ME3PCOffers",
        "ENT_91_ID" to "1500116",
        "ENT_92_entitlement" to "ME3_PRO_CHALLENGE_17",
        "ENT_92_group" to "ME3PCOffers",
        "ENT_92_ID" to "1500117",
        "ENT_93_entitlement" to "ME3_PRO_CHALLENGE_18",
        "ENT_93_group" to "ME3PCOffers",
        "ENT_93_ID" to "1500118",
        "ENT_94_entitlement" to "ME3_PRO_CHALLENGE_19",
        "ENT_94_group" to "ME3PCOffers",
        "ENT_94_ID" to "1500119",
        "ENT_95_entitlement" to "ME3_PRO_CHALLENGE_20",
        "ENT_95_group" to "ME3PCOffers",
        "ENT_95_ID" to "1500120",
        "ENT_96_entitlement" to "ME3_CS_SERVICE_01",
        "ENT_96_group" to "ME3GenContent",
        "ENT_96_ID" to "1400000",
        "ENT_97_entitlement" to "ME3_CS_SERVICE_02",
        "ENT_97_group" to "ME3GenContent",
        "ENT_97_ID" to "1400001",
        "ENT_98_entitlement" to "ME3_CS_SERVICE_03",
        "ENT_98_group" to "ME3GenContent",
        "ENT_98_ID" to "1400002",
        "ENT_99_entitlement" to "BWE_ANCILLARY_ME_ANIME_MOVIE",
        "ENT_99_group" to "ME3GenAncillary",
        "ENT_99_ID" to "1250032",
        "ENT_9_entitlement" to "PROJECT10_CODE_CONSUMED_LE2",
        "ENT_9_group" to "ME3PCOffers",
        "ENT_9_ID" to "103",
        "ENT_9_param" to "BWID=101",
        "ENT_ENC" to "0051CCF2;00A35612;0163AA2E;01BCA74E;01D861E1;0237E3EE;02CE0DC0;0326FDCE;03C48303;04260DF1;04646BA5;04B2FED2;055C7130;05D148A4;0662323D;07035FB9;07A7AE9E;08229B6E;0889BCD8;092AEA54;0977008C;09D88B7A;0A2BB64D;0A2DA8F0;0A7ED87B;0B232351;0BC5F591;0C416811;0C9CAC4E;0D2F61F6;0D54EC69;0D6AB3C8;0D6E9BF3;0DDF15BB;0E73A766;0EE87EDA;0F9E1B18;0FB1A093;10214AD0;10CC021B;117D8A56;1198ADA6;11B02663;120F98D1;12CFBC47;12D38455;135C4DD3;13ABDEF7;1449DF3C;146157F9;146E963C;152176F4;15DF374C;16993403;16D72471;170A8EEF;1717CAB7;17B1CEC8;18231C67;18E3AABD;191525F6;198EE18D;1A061217;1A6DC5F8;1AF8925F;1B78A2C0;1BE4A383;1C46E07A;1CB7F53B;1D6DFD62;1DFB6F25;1E6BE8ED;1F2C55F1;1F368004;1F7A05F1;2035983D;20526BDF;20F8233E;2171DED5;21E90F5F;227367E9;22ECF94B;2308F10C;23115D87;235800A6;23E0CA24;24A2FDD0;24FFC609;25AC206A;266C43E0;2723BABE;2724F2C8;27949D05;28086089;285E36BF;28A39D8E;2906B6E2;29582956;299520D6;29AB3B75;29F96D75;2A28A0D2;2A5F9098;2A6CCEDB;2B25F583;2BBFD879;2C399410;2CB0C49A;2D18787B;2DA344E2;2E235543;2E8F5606;2EF192FD;2F62A7BE;2FE1485F;30088EA8;3074D615;30B3C215;310CBF35;31A08A94;31ABB3DB;3255B7D1;33162B72;332BA248;33D158DF;344B49F9;348584B1;348AF848;352BAAD2;3544BBB5;35BE18AC;363A6D46;36C0CF87;37622054;37C3AB42;383FFFDC;38E5B673;395FA78D;39853200;3A45A5A1;3A834631;3ABDCDD5;3ACB0C18;3B39D7DC;3B920685;3C09370F;3C938F99;3D0D20FB;3D2918BC;3D318537;3D782856;3E00F1D4;3EC32580;3F1FEDB9;3FCC481A;408C6B90;4143E26E;41451A78;41B4C4B5;42288839;427E5E6F;42C3C53E;4326DE92;43785106;43B54886;43CB6325;44199525;4448C882;447FB848;448CF68B;45461D33;45E00029;4659BBC0;46D0EC4A;4738A02B;47C36C92;48437CF3;48AF7DB6;4911BAAD;4982CF6E;4A01700F;4A28B658;4A94FDC5;4AD3E9C5;4B2CE6E5;4BC0B244;4C5485FD;4CFE89F3;4DBEFD94;4DD4746A;4E7A2B01;4EF41C1B;4F2E56D3;4F33CA6A;4FD47CF4;4FED8DD7;5066EACE;50E33F68;5169A1A9;520AF276;526C7D64;52E8D1FE;538E8895;540879AF;542E0422;54EE77C3;54FBB606;552E21A0;5546604F;55F3F579;56603CE6;570FC91C;5735538F;574B1AEE;574F0319;57BF7CE1;58540E8C;58C8E600;597E823E;599207B9;5A01B1F6;5AAC6941;5B5DF17C;5B7914CC;5B908D89;5BEFFFF7;5CB0236D;5CB3EB7B;5D3CB4F9;5D8C461D;5E2A4662;5E41BF1F;5E4EFD62;5F01DE1A;5FBF9E72;60799B29;60B78B97;60EAF615;60F831DD;60F974DF;61BBA89C;61EA4AC5;6277BC88;62E83650;63A8A354;63B2CD67;63F65354;64B1E5A0;64F402A8;652FBDA6;65A9793D;6620A9C7;66AB0251;672493B3;67408B74;6748F7EF;678F9B0E;6818648C;68DA9838;69376071;69E3BAD2;6AA3DE48;6B5B5526;6B5C8D30;6BCC376D;6C3FFAF1;6C95D127;6CDB37F6;6D3E514A;6DF05C34;6EB0C938;6F72FCF5;6FD61649;7071ADA3;70D9C83A;714E9FAE;71C85B45;723F8BCF;72A73FB0;73320C17;73B21C78;741E1D3B;74805A32;7498C9D7;74E65942;750D9F8B;7579E6F8;75B8D2F8;7611D018;76A59B77;76FE2104;77A824FA;7868989B;787E0F71;7923C608;799DB722;79D7F1DA;79DD6571;7A7E17FB;7A9728DE;7B1085D5;7B8CDA6F;7C133CB0;7CB48D7D;7D16186B;7D926D05;7E38239C;7EB214B6;7ED79F29;7F9812CA;7FD5B35A;7FFD9DA3;8028D283;802A728D;80D807B7;814F3841;81D990CB;8253222D;826F19EE;82778669;82BE2988;8346F306;840926B2;8465EEEB;8512494C;85D26CC2;8689E3A0;868B1BAA;86FAC5E7;876E896B;87C45FA1;8809C670;886CDFC4;89269055;89E3AAF8;8A58826C;8A9672DA;8AC9DD58;8AD71920;8AE44CA8;8B433902;8BC60AA0;8C537C63;8CC3F62B;8D84632F;8D8E8D42;8DD2132F;8E8DA57B;8ECFC283;8F8EFE50;9008B9E7;901C5C05;906DE525;912E3941;91873661;91A2F0F4;91BB7942;9251A314;92AA9322;93481857;93DACDFF;94337698;947EBF38;950F2197;951FDCC8;955F1FA3;960CB4CD;96890967;96EF8DFA;9790BB76;98350A5B;98AFF72B;99171895;99B84611;9A045C49;9A65E737;9AB9120A;9ABB04AD;9AFF0BB6;9B00AAD5;9B59536E;9BD4C5EE;9C4BF678;9CB3AA59;9D3E76C0;9DBE8721;9E2A87E4;9ECCB6A6;9F386BD2;9FB70C73;9FDE52BC;A04A9A29;A0898629;A0E28349;A1764EA8;A193224A;A23D2640;A2FD99E1;A31310B7;A334DC86;A33E56B1;A3B72CDA;A410DFB1;A4C0E63D;A4FB20F5;A5914AC7;A627993D;A6A0275E;A7198455;A795D8EF;A81C3B30;A8BD8BFD;A91F16EB;A99B6B85;AA41221C;AABB1336;AAE09DA9;AB1B3E37;AB6C6DC2;AC0A1123;AC6EB672;ACE66C23;AD56E5EB;ADEB7796;AE604F0A;AF15EB48;AF2970C3;AF991B00;B043D24B;B0F55A86;B1107DD6;B127F693;B1876901;B2478C77;B24B5485;B2D41E03;B323AF27;B3C1AF6C;B3D92829;B3E6666C;B4994724;B557077C;B6110433;B64EF4A1;B6825F1F;B68F9AE7;B6F8F289;B7AB911B;B7C309D8;B856C098;B8A17851;B92EEA14;B99F63DC;BA5FD0E0;BA69FAF3;BAAD80E0;BB69132C;BBAB3034;BBCDD1FE;BC478D95;BC5B2FB3;BCACB8D3;BD6D0CEF;BDC60A0F;BE4C0F9B;BE9A14D6;BF303EA8;BF892EB6;C026B3EB;C0883ED9;C0C69C8D;C1152FBA;C1BEA218;C233798C;C2C46325;C36590A1;C409DF86;C484CC56;C4EBEDC0;C58D1B3C;C5D93174;C5DB2417;C6537805;C65CF230;C68F5DCA;C6BBF9BE;C70A8CEB;C766EF03;C7B0232E;C7FB6BCE;C804B977;C8664465;C90D80FE;C9AED1CB;CA3C438E;CAACBD56;CB414F01;CBB62675;CC6BC2B3;CC7F482E;CCEEF26B;CD99A9B6;CE4B31F1;CE665541;CE7DCDFE;CEDD406C;CF9D63E2;CFA12BF0;D029F56E;D0798692;D11786D7;D12EFF94;D1B490B1;D214C6A2;D2D44A00;D38A9C39;D3B9CF96;D3F0BF5C;D4B22BC9;D4DC84B3;D4FFBA7F;D5BFDDF5;D639998C;D6B0CA16;D7187DF7;D7A34A5E;D8235ABF;D88F5B82;D8F19879;D90A081E;D9CC33C0;DA59A583;DACA1F4B;DB5EB0F6;DBD3886A;DC8924A8;DC9CAA23;DD0C5460;DDB70BAB;DE6893E6;DE83B736;DE9B2FF3;DEFAA261;DFBAC5D7;DFBE8DE5;E0475763;E096E887;E134E8CC;E14C6189;E1D1F2A6;E2314870;E2C882C1;E317880F;E360BC3A;E3D36CF2;E4106472;E4939586;E4D41C0C;E5618DCF;E5D20797;E692749B;E69C9EAE;E6E0249B;E79BB6E7;E7DDD3EF;E84B9FF7;E8C55B8E;E93C8C18;E9C6E4A2;EA407604;EA5C6DC5;EA64DA40;EAAB7D5F;EB3446DD;EBF67A89;EC5342C2;ECFF9D23;EDBFC099;EE773777;EE786F81;EEE819BE;EF5BDD42;EFB1B378;EFF71A47;F05A339B;F07AF187;F085FC0E;F0E25BAB;F15E37F5;F1DD7D0D;F1E829B2;F20A218E;F2979351;F3080D19;F3C87A1D;F3D2A430;F4162A1D;F4D1BC69;F513D971;F57967D0;F5F32367;F66A53F1;F6F4AC7B;F76E3DDD;F78A359E;F792A219;F7D94538;F8620EB6;F9244262;F9810A9B;FA2D64FC;FAED8872;FBA4FF50;FBA6375A;FC15E197;FC89A51B;FCDF7B51;FD24E220;FD87FB74;FE3F913B;FEC245C3;FF05CBB0;FF41B104;FF79FCE9;FFB0ECAF;0001002AA846;000100A1D8D0;000101098CB1;000101945918;000102146979;000102806A3C;000102E2A733;000102FB16D8;000103098FB8;00010397017B;000104077B43;000104C7E847;000104D2125A;000105159847;000105D12A93;00010657301F;000106AF9D92;000107295929;000107A089B3;000108083D94;0001089309FB;000109131A5C;0001097F1B1F;000109E15816;00010A0DE63D;00010A5B75A8;00010A82BBF1;00010AEF035E;00010B6DBE98;00010C243EB9;00010C7243F4;00010D086DC6;00010D615DD4;00010DFEE309;00010E606DF7;00010E9ECBAB;00010EED5ED8;00010F96D136;0001100BA8AA;0001109C9243;0001113DBFBF;000111E20EA4;0001125CFB74;000112C41CDE;000113654A5A;000113B16092;00011412EB80;000114661653;000115104F29;00011528F7CB;000115778AF8;0001159CF350;000115A66D7B;0001162F943F;0001169FBA5F;000116DB9FB3;0001171018F1;000117B467D6;000117FFB076;00011876E100;00011901398A;0001197ACAEC;00011996C2AD;0001199F2F28;000119E5D247;00011A6E9BC5;00011B30CF71;00011B8D97AA;00011C39F20B;00011CFA1581;00011DB18C5F;00011DB2C469;00011E226EA6;00011E96322A;00011EEC0860;00011F316F2F;00011F948883;0001204E3914;0001210B53B7;000121802B2B;000121BE1B99;000121F18617;000121FEC1DF;00012298C5F0;000122D85DC9;000122E30A6E;0001237614E7;0001240386AA;000124740072;000125346D76;0001253E9789;000125821D76;0001263DAFC2;0001267FCCCA;0001268AF173;00012704AD0A;0001277BDD94;00012806361E;0001287FC780;0001289BBF41;000128A42BBC;000128EACEDB;000129739859;00012A35CC05;00012A92943E;00012B3EEE9F;00012BFF1215;00012CB688F3;00012CB7C0FD;00012D276B3A;00012D9B2EBE;00012DF104F4;00012E366BC3;00012E998517;00012F4B9001;0001300BFD05;000130CE30C2;000131314A16;000131A7A548;00013241883E;000132E4C1DC;000133599950;000133D354E7;0001344A8571;000134B23952;0001353D05B9;000135BD161A;0001362916DD;0001368B53D4;000136A3C379;000136A56DD4;00013732DF97;000137A3595F;00013837EB0A;000138ACC27E;000139625EBC;00013975E437;000139E58E74;00013A9045BF;00013B41CDFA;00013B5CF14A;00013B746A07;00013BD3DC75;00013C93FFEB;00013C97C7F9;00013D209177;00013D70229B;00013E0E22E0;00013E259B9D;00013EAB2CBA;00013EF52B12;00013FA9DE87;00013FCA7F7B;000140423537;000140F1F622;0001418E0549;00014207C0E0;0001427EF16A;000142E6A54B;0001437171B2;000143F18213;0001445D82D6;000144BFBFCD;000144D82F72;000145565A54;000145E3CC17;0001465445DF;000146E8D78A;0001475DAEFE;000148134B3C;00014826D0B7;000148967AF4;00014941323F;000149F2BA7A;00014A0DDDCA;00014A255687;00014A84C8F5;00014B44EC6B;00014B48B479;00014BD17DF7;00014C210F1B;00014CBF0F60;00014CD6881D;00014CFC1290;00014D67D0C7;00014E15A240;00014E9BFC84;00014F5CF037;0001500E94E2;000150308CBE;000150BDFE81;0001512E7849;000151EEE54D;000151F90F60;0001523C954D;000152F82799;0001533A44A1;00015372249A;000153EBE031;0001546310BB;000154CAC49C;000155559103;000155D5A164;00015641A227;000156681731;000156C0A394;000157261FF7;000157B391BA;000158240B82;000158E47886;000158EEA299;000159322886;000159EDBAD2;00015A4BE46D;00015A73CEB6;00015AED8A4D;00015B550BE4;00015BCC7AA8;00015C812E1D;00015D0CD60E;00015DAD755D;00015E2F0EDD;00015E337A22;00015E6FCB7B;00015F101B61;00015F1486A6;00015FAB1D92;00015FFDCCC5;000160A56558;0001612E05F1;000161759C78;0001622F1343;000162EFC8F4;00016319D50B;000163CDEAAE;000163E58B28;0001643B615E;000164579397;0001648421BE;000164D7A5B4;000164EE10E0;00016501B2FE;000165533C1E;000165E9D30A;00016642D02A;000166AA51C1;00016721C085;000167D673FA;000168621BEB;00016902BB3A;0001696BC425;00016A15293A;00016A24A3B1;00016A8A5236;00016AC6A38F;00016B68D251;00016BE2472A;00016BF29001;00016C0F63A3;00016C64E0EE;00016C6C2CDF;00016D0144F5;00016DC2851A;00016E6CD268;00016EDE2FF6;00016F531E87;00016FFEB8E5;0001704F3B34;000170D38AE0;00017192C313;000171A66531;000171F7EE51;0001725617EC;000172AF150C;0001731696A3;0001738E0567;00017442B8DC;000174CE60CD;0001756F001C;000175E57C9D;000176798885;000176A9CD04;0001771CBD77;0001778E1B05;000178378D63;00017898EC05;000179425E63;000179E815C2;00017A479724;00017A536D07;00017A72D689;00017AA57862;00017B06D704;00017B5F4477;00017B90B68B;00017C141447;00017CBD795C;00017D29AF83;00017D439E65;00017D574083;00017DA8C9A3;00017E05EB64;00017E5EE884;00017EC66A1B;00017F3DD8DF;00017FF28C54;0001807E3445;0001811ED394;0001813B05CD;000181A85E59;000182149480;0001829E1E21;000182B161ED;000182E941E6;000183382F45;000183C241B5;000184838CBF;000184E1B65A;0001853FDFF5;000185A58E7A;000186393917;000186AD2060;00018702F696;00018796D0AE;000187C5D017;000188664793;000189064EF8;000189BFC5C3;000189D367E1;00018A24F101;00018AC71FC3;00018B201CE3;00018BCA20D9;00018C8A947A;00018CA00B50;00018D45C1E7;00018DBFB301;00018DF9EDB9;00018DFF6150;00018EA013DA;00018EB924BD;00018F3281B4;00018FAED64E;00019035388F;000190D6895C;00019138144A;000191B468E4;0001925A1F7B;000192D41095;000192F99B08;000193115427;00019336DE9A;000193F7523B;000194A273F4;000194E7BCF6;0001950950F7;000195422BD2;0001957C80F0;000195F5C2ED;00019628F785;00019667E385;0001967F83FF;000196D348E0;0001974D8635;000197C4B526;00019859CD3C;000198BC2376;000199281114;0001999E8D95;00019A083E0D;00019A9B14E6;00019B1EF04B;00019B4131D5;00019B4D07B8;00019BB6B830;00019C226D5C;00019C784392;00019CDC0346;00019D06D520;00019DC78AD1;00019E3D9C12;00019EA9E37F;00019F289EB9;00019F508902;00019F9E8E3D;00019FD76918;0001A011BE36;0001A08B0033;0001A0BE34CB;0001A0FAF42F;0001A1151C34;0001A1835FE8;0001A1F0B874;0001A2235A4D;0001A29A893E;0001A2B15644;0001A33ADFE5;0001A3E6E2BB;0001A47AAE1A;0001A4FC479A;0001A54CC9E9;0001A5D2CF75;0001A5FCDB8C;0001A64BC8EB;0001A670C992;0001A69A4E0B;0001A6C94D74;0001A6FABF88;0001A7844058;0001A834DBB0;0001A8A1231D;0001A91FDE57;0001A9944260;0001A9EF8F9F;0001AA85B971;0001AADEA97F;0001AB7C2EB4;0001ABDDB9A2;0001AC1C1756;0001AC6AAA83;0001AD141CE1;0001AD88F455;0001AE19DDEE;0001AEBB0B6A;0001AF5F5A4F;0001AFDA471F;0001B0416889;0001B0E29605;0001B12EAC3D;0001B190372B;0001B1E361FE;0001B1E554A1;0001B21FA9BF;0001B2C0FA8C;0001B32BB2EE;0001B3D860A2;0001B46FB05E;0001B491445F;0001B4CA1F3A;0001B5047458;0001B57DB655;0001B5B0EAED;0001B62AA684;0001B69ED829;0001B71D5A39;0001B72CD4B0;0001B7E354D1;0001B7EAA0C2;0001B87D535E;0001B916BA04;0001B97C3062;0001B9CB1DC1;0001BA2D73FB;0001BAB7866B;0001BB7A08E8;0001BC2E1E8B;0001BC8F7D2D;0001BCC47079;0001BCD7B445;0001BCE1FCCA;0001BD1C846E;0001BDDDC493;0001BDE38836;0001BE4FCFA3;0001BEA0B926;0001BEABE26D;0001BEF9E7A8;0001BF32C283;0001BF6D17A1;0001BFE6599E;0001C0198E36;0001C0D77F2E;0001C17AF605;0001C18B2A37;0001C1B8C7EA;0001C2278DDA;0001C28F1E04;0001C317BE9D;0001C3BCD9DB;0001C41E387D;0001C4317C49;0001C4C5F86D;0001C56BAFCC;0001C58697C6;0001C5B169A0;0001C5E06909;0001C6549AAE;0001C6D7F86A;0001C6E44B77;0001C7A56BB8;0001C8479A7A;0001C84D5E1D;0001C8B9A58A;0001C90A8F0D;0001C9ACBDCF;0001C9FAC30A;0001CA90ECDC;0001CAE9DCEA;0001CB87621F;0001CBE8ED0D;0001CC274AC1;0001CC75DDEE;0001CD1F504C;0001CD9427C0;0001CE251159;0001CEC63ED5;0001CF6A8DBA;0001CFE57A8A;0001D04C9BF4;0001D0EDC970;0001D139DFA8;0001D19B6A96;0001D1EE9569;0001D1F0880C;0001D22ADD2A;0001D234649D;0001D2CE51DF;0001D3460790;0001D37EE26B;0001D3B93789;0001D4327986;0001D465AE1E;0001D4E20658;0001D4F8D35E;0001D5AFA689;0001D632D79D;0001D6D54843;0001D75AABC0;0001D819E78D;0001D86FBDC3;0001D897A80C;0001D8F0346F;0001D9999984;0001DA232325;0001DA783032;0001DA93182C;0001DAAED2BF;0001DB04716B;0001DB972407;0001DBFFC57F;0001DC78EBDA;0001DCED1D7F;0001DD96C2B9;0001DE030A26;0001DE81C560;0001DE95D314;0001DEE3D84F;0001DF7A0221;0001DFD2F22F;0001E0707764;0001E0D20252;0001E1106006;0001E15EF333;0001E2086591;0001E27D3D05;0001E30E269E;0001E3AF541A;0001E453A2FF;0001E4CE8FCF;0001E535B139;0001E5D6DEB5;0001E622F4ED;0001E6847FDB;0001E6D7AAAE;0001E781E384;0001E79A8C26;0001E806B458;0001E84E1A76;0001E8DB8C39;0001E94C0601;0001E9E097AC;0001EA556F20;0001EB0B0B5E;0001EB1E90D9;0001EB8E3B16;0001EC38F261;0001ECEA7A9C;0001ED059DEC;0001ED1D16A9;0001ED7C8917;0001EE3CAC8D;0001EE758768;0001EEAFDC86;0001EEFE6FB3;0001EF439F57;0001EFAFC789;0001EFF72DA7;0001F092A186;0001F1024BC3;0001F1760F47;0001F1CBE57D;0001F2114C4C;0001F27465A0;0001F295238C;0001F2A02E13;0001F31FA6A0;0001F3422E61;0001F367A279;0001F3C2E2D7;0001F456AE36;0001F4D069CD;0001F5479A57;0001F5AF4E38;0001F63A1A9F;0001F6BA2B00;0001F7262BC3;0001F7BCC2AF;0001F84F9988;0001F89D28F3;0001F92A9AB6;0001F99B147E;0001FA2FA629;0001FAA47D9D;0001FB5A19DB;0001FB6D9F56;0001FBDD4993;0001FC8800DE;0001FD398919;0001FD54AC69;0001FD6C2526;0001FDCB9794;0001FE8BBB0A;0001FEC495E5;0001FEFEEB03;0001FF4D7E30;0001FF92ADD4;0001FFFED606;000200463C24;000200E1B003;000201515A40;000201C51DC4;0002021AF3FA;000202605AC9;000202C3741D;000203757F07;00020435EC0B;000204F81FC8;0002055B391C;0002056D9869;0002057604E4;000205993AB0;000205FC5404;000206687C36;000206AFE254;000207198059;000207A6F21C;000208176BE4;000208D7D8E8;000208E202FB;0002092588E8;000209E11B34;000209EF3751;00020A8302B0;00020AFCBE47;00020B73EED1;00020BFE475B;00020C77D8BD;00020C93D07E;00020C9C3CF9;00020CE2E018;00020D6BA996;00020E2DDD42;00020E8AA57B;00020F36FFDC;00020FF72352;000210AE9A30;000210C1DFD2;000211394E96;0002118282C1;000211CDCB61;000211F33F79;0002124E7FD7;000212E24B36;000212E61344;0002136EDCC2;000213BE6DE6;0002145C6E2B;00021473E6E8;00021481252B;0002153405E3;000215F1C63B;000216ABC2F2;000216E9B360;0002171D1DDE;0002172A59A6;00021751A4E7;0002180EA604;000218341A1C;0002188F5A7A;0002192325D9;0002199CE170;00021A1411FA;00021A7BC5DB;00021B069242;00021B86A2A3;00021BF2A366;00021C893A52;00021D1C112B;00021DDE3CCD;00021E6BAE90;00021EDC2858;00021F70BA03;00021FE59177;0002209B2DB5;000220AEB330;0002211E5D6D;000221C914B8;0002227A9CF3;00022295C043;000222AD3900;0002230CAB6E;000223CCCEE4;00022405A9BF;0002243FFEDD;0002248E920A;000224D3C1AE;0002253FE9E0;000225874FFE;00022622C3DD;000226926E1A;00022706319E;0002275C07D4;000227A16EA3;0002280487F7;000228BE3888;0002297B532B;000229F02A9F;00022A2E1B0D;00022A61858B;00022A6EC153;00022A912FEF;00022B08E5AB;00022B139250;00022B390668;00022B9446C6;00022C281225;00022CA1CDBC;00022D18FE46;00022D80B227;00022E0B7E8E;00022E8B8EEF;00022EF78FB2;00022F8E269E;00023020FD77;00023022A7D2;000230B01995;00023120935D;000231B52508;00023229FC7C;000232DF98BA;000232F31E35;00023362C872;0002340D7FBD;000234BF07F8;000234DA2B48;000234F1A405;000235511673;0002361139E9;0002361501F7;0002369DCB75;000236ED5C99;0002378B5CDE;000237A2D59B;0002382866B8;000238889CA9;000239482007;000239FE7240;00023A2DA59D;00023A649563;00023A8FCA43;00023B4AD26A;00023BAAC980;00023C248517;00023C9BB5A1;00023D036982;00023D8E35E9;00023E0E464A;00023E7A470D;00023EDC8404;00023EF4F3A9;00023F5E91AE;00023FEC0371;0002405C7D39;000240F10EE4;00024165E658;0002421B8296;0002422F0811;0002429EB24E;000243496999;000243FAF1D4;000244161524;0002442D8DE1;0002448D004F;0002454D23C5;00024585FEA0;000245C053BE;0002460EE6EB;00024654168F;000246C03EC1;00024707A4DF;000247A318BE;00024812C2FB;00024886867F;000248DC5CB5;00024921C384;00024984DCD8;000249A59AC4;000249B0A54B;000249BF61F6;00024A3F7257;00024ABD50B8;00024B6641C9;00024C1DA935;00024CAB1AF8;00024D1B94C0;00024DDC01C4;00024DE62BD7;00024E29B1C4;00024EE54410;00024EF3602D;00024FA9E04E;000250239BE5;0002509ACC6F;0002512524F9;0002519EB65B;000251BAAE1C;000251C31A97;00025209BDB6;000252928734;00025354BAE0;000253B18319;0002545DDD7A;0002551E00F0;000255D577CE;000255E8BD70;000256602C34;000256A9605F;000256F4A8FF;0002571A1D17;000257755D75;0002580928D4;0002580CF0E2;00025895BA60;000258E54B84;000259834BC9;0002599AC486;000259A802C9;00025A5AE381;00025B18A3D9;00025BD2A090;00025C1090FE;00025C43FB7C;00025C513744;00025C639691;00025C6C030C;00025C8F38D8;00025CE6FE00;00025D746FC3;00025DE4E98B;00025EA5568F;00025EAF80A2;00025EF3068F;00025FAE98DB;00025FBCB4F8;00025FC7DE3F;0002604199D6;000260A91B6D;000261208A31;000261D53DA6;00026260E597;0002630184E6;0002635A8206;000263D9DA18;0002649B2522;000264F39295;0002655C340D;0002659DC588;00026627D7F8;0002663FB8FA;0002664EE523;000266A41EA0;0002670674DA;0002679FDB80;000267F8610D;0002683A36B1;000268DF51EF;0002694A8567;000269EDEDF3;00026A1A7C1A;00026A6D2B4D;00026AAC7AC6;00026AC01CE4;00026B11A604;00026B9A469D;00026BF87038;00026C5FF1CF;00026CD76093;00026D8C1408;00026E17BBF9;00026EB85B48;00026F4507E1;00026F7CE7DA;00026FD5743D;00027093AE47;00027119B3D3;00027143BFEA;000272041406;000272536711;0002726B4813;0002732CD33A;00027338A91D;000273893473;000273F2E4EB;0002747FED86;00027494528C;0002752AE978;000275323569;0002753EFA0F;000275AB3036;000275F2C6BD;0002765A4854;000276D1B718;000277866A8D;00027812127E;000278B2B1CD;00027954E08F;000279E8B448;00027A5264C0;00027A90A31B;00027B071F9C;00027B819F00;00027C3480FE;00027CEB5429;00027D03352B;00027DB20C70;00027E0745ED;00027E5461B9;00027EC327A9;00027F4A973F;00027FA3945F;0002804CF803;000280FB02E9;000281A467FE;00028219568F;000282411A73;00028254BC91;000282A645B1;0002831411B9;0002836D0ED9;0002841712CF;000284D78670;000284ECFD46;00028592B3DD;0002860CA4F7;00028646DFAF;0002864C5346;000286ED05D0;0002870616B3;0002877F73AA;000287FBC844;000288822A85;000289237B52;000289850640;00028A015ADA;00028AA71171;00028B21028B;00028B468CFE;00028B5E461D;00028B7A064E;00028BF578CE;00028C297B31;00028C2C255D;00028CCD51DC;00028D1BE509;00028D3C4194;00028D68BC47;00028DA19722;00028DDBEC40;00028E552E3D;00028E8862D5;00028F2B4D42;00028F967817;00028FC6BC96;000290196BC9;0002909CE141;000290F71FEC;00029152763B;000291F75A87;0002921F1E6B;0002922E4A94;00029289A0E3;000292AEA18A;0002935AA460;000293EFBC76;000294922D1C;000294A02E7D;000294B7CEF7;000294F715F7;000294FAB4D5;0002955D0B0F;00029575935D;000295AE6E38;000295E8C356;000296620553;0002969539EB;000296FAD463;0002978FF41F;000297E0766E;000298745086;000298FC9E0E;0002990A9F6F;00029965F5BE;000299855F40;000299EB0DC5;00029A7ED924;00029AB6B91D;00029AE34744;00029B14F857;00029B53D1F7;00029B7D5670;00029BE706E8;00029C195D5C;00029C71CACF;00029D1B2E73;00029D7684C2;00029DF77042;00029E304B1D;00029E6AA03B;00029EE3E238;00029F1716D0;00029F5602D0;00029F5D4EC1;00029F9AB405;00029FD9FB05;0002A05129F6;0002A0ACF1E8;0002A0C1C29C;0002A1820F96;0002A1918A0D;0002A19CAEB6;0002A19CB3F3;0002A20A7FFB;0002A2529401;0002A2FC065F;0002A3030F36;0002A3539185;0002A3D21395;0002A48893B6;0002A549B3F7;0002A559FCCE;0002A5B97EDB;0002A5F259B6;0002A62CAED4;0002A6A5F0D1;0002A6D92569;0002A7181169;0002A7924EBE;0002A7E824F4;0002A83AD427;0002A8F1A752;0002A909DD9A;0002A97CCE0D;0002A9D9EFCE;0002A9E0E842;0002AA9F224C;0002AAB9112E;0002AB22C1A6;0002AB295F18;0002AB40FF92;0002ABD3B22E;0002ABE2DE57;0002ACA4697E;0002ACDC4977;0002ACDFED5A;0002AD358C06;0002ADA31D93;0002ADDBF86E;0002AE164D8C;0002AE8F8F89;0002AEC2C421;0002AF01B021;0002AF7BED76;0002AFD1C3AC;0002B02472DF;0002B0DB460A;0002B0F37C52;0002B1666CC5;0002B1C38E86;0002B1CA86FA;0002B288C104;0002B2A2AFE6;0002B30C605E;0002B312FDD0;0002B32A9E4A;0002B3BD50E6;0002B3CC7D0F;0002B48E0836;0002B4C5E82F;0002B4C98C12;0002B51F2ABE;0002B58CBC4B;0002B5C59726;0002B5FFEC44;0002B6792E41;0002B6AC62D9;0002B6DC9FE6;0002B715D9EF;0002B79E2777;0002B8095AEF;0002B818369B;0002B83468D4;0002B8D4B8BA;0002B8F4223C;0002B96B512D;0002B97599B2;0002B9E1CFD9;0002BA9B46A4;0002BB24C774;0002BB3EB656;0002BB9E37B8;0002BC21AD30;0002BC9C2C94;0002BCD3F5D3;0002BD337735;0002BDBBB5E0;0002BE455575;0002BE7E3050;0002BEB8856E;0002BF31C76B;0002BF64FC03;0002C0178958;0002C0889E19;0002C10212F2;0002C19DAA0C;0002C223AF98;0002C280D159;0002C2E65FB8;0002C33BDD03;0002C3FCB6D9;0002C42E28ED;0002C4E79FB8;0002C5000F5D;0002C5C05C57;0002C61A9B02;0002C6AD71DB;0002C7161353;0002C72AE407;0002C76A3380;0002C79D29F2;0002C7DEBB6D;0002C82DA24D;0002C8667D28;0002C8A0D246;0002C91A1443;0002C94D48DB;0002C9C70472;0002CA131021;0002CAD45B2B;0002CB4DD004;0002CBA27C52;0002CC4B6D63;0002CCA5AC0E;0002CD63AB51;0002CDFA423D;0002CE25F896;0002CE42CC38;0002CE7E8736;0002CE90A993;0002CE9D6E39;0002CF46E097;0002CF5209DE;0002D01354E8;0002D04505FB;0002D0D9821F;0002D19A37D0;0002D224266C;0002D25D0147;0002D2975665;0002D3109862;0002D343CCFA;0002D3808C5E;0002D43EC668;0002D4A65692;0002D546A07B;0002D5AF6C4D;0002D6239DF2;0002D63F5885;0002D658FDF2;0002D66006C9;0002D7228946;0002D78CB498;0002D7F66510;0002D870E474;0002D8F0E162;0002D9743F1E;0002D9CE7DC9;0002DA82936C;0002DAF3A82D;0002DBAA284E;0002DC3DF3AD;0002DC567BFB;0002DC8F56D6;0002DCC9ABF4;0002DD42EDF1;0002DD762289;0002DE190CF6;0002DE35E098;0002DEBB4415;0002DF60FB74;0002DFE42C88;0002E0414E49;0002E0A6C4A7;0002E0E85622;0002E16362B4;0002E19F1DB2;0002E21BF448;0002E24C38C7;0002E28B8840;0002E3399326;0002E3991488;0002E407DA78;0002E442621C;0002E4CAA0C7;0002E5386CCF;0002E593F8BA;0002E5AC8108;0002E5E55BE3;0002E61FB101;0002E698F2FE;0002E6CC2796;0002E7487FD0;0002E7ADF62E;0002E7B0B20E;0002E8249957;0002E8B9157B;0002E94733F2;0002E986836B;0002EA05DB7D;0002EA4D7204;0002EB0B7147;0002EBB128A6;0002EC03D7D9;0002EC7CFE34;0002EC844A25;0002ECEDFA9D;0002ED417E93;0002EDD549F2;0002EE3E15C4;0002EE68E79E;0002EEA221A7;0002EEBAEB44;0002EEF3C61F;0002EF2E1B3D;0002EFA75D3A;0002EFDA91D2;0002F040B3F1;0002F09DD5B2;0002F0A4DE89;0002F153B5CE;0002F1D7138A;0002F277C614;0002F2A45195;0002F2CDD60E;0002F2D81E93;0002F36AD12F;0002F3E3F78A;0002F44C9902;0002F4F0018E;0002F5AF39C1;0002F617BEED;0002F69DC479;0002F73E7703;0002F777B10C;0002F7D01E7F;0002F81F14EB;0002F8B4EF88;0002F8EDCA63;0002F9281F81;0002F9A1617E;0002F9D49616;0002FA4E51AD;0002FA9ED3FC;0002FB066426;0002FBA6B40C;0002FC0812AE;0002FC1C2062;0002FCC7116A;0002FD17872B;0002FD2662D7;0002FD9A4A20;0002FE2E1DD9;0002FE32891E;0002FE7C45B5;0002FE833E29;0002FF078DD5;0002FF0E2B47;0002FF5D18A6;0002FFADA3FC;0002FFC20902;0002FFCD3249;0003001B24D1;00030053FFAC;0003008E54CA;0003010796C7;0003013ACB5F;000301B486F6;000302050945;0003026C996F;0003030CE955;0003036E47F7;0003038255AB;0003042D46B3;0003047DBC74;0003048C9820;000305007F69;000305945322;00030598BE67;000305E27AFE;000305E97372;0003066DC31E;000306746090;000306C34DEF;00030713D945;000307283E4B;000307336792;00030747683B;000307B3AFA8;00030804992B;0003082053BE;0003086E58F9;0003090482CB;0003095D72D9;000309FAF80E;00030A5C82FC;00030A9AE0B0;00030AE973DD;00030B92E63B;00030C07BDAF;00030C98A748;00030D39D4C4;00030DDE23A9;00030E591079;00030EC031E3;00030F615F5F;00030FAD7597;0003100F0085;000310622B58;0003110C642E;000311250CD0;000311913502;000311D89B20;000312278200;00031297FBC8;0003132C8D73;000313A164E7;000314570125;0003146A86A0;000314DA30DD;00031584E828;000316367063;0003165193B3;000316690C70;000316C87EDE;00031788A254;000317C17D2F;000317FBD24D;0003184A657A;0003188F951E;000318FBBD50;00031943236E;000319D2B1D1;00031A566CA2;00031A7EBFCB;00031AD49601;00031B19FCD0;00031B7D1624;00031B9799CB;00031C1A4E53;00031C5DD440;00031CDAC0C0;00031D17B840;00031D8F6DFC;00031E248C73;00031E7391C1;00031EC9FDFD;00031F43B994;00031FBAEA1E;000320229DFF;000320AD6A66;0003212D7AC7;000321997B8A;0003222D55A2;000322E3D5C3;000323495226;000323D6C3E9;000324473DB1;000324DBCF5C;00032550A6D0;00032606430E;00032619C889;0003268972C6;000327342A11;000327E5B24C;00032800D59C;000328184E59;00032877C0C7;00032937E43D;00032970BF18;000329AB1436;000329F9A763;00032A3ED707;00032AAAFF39;00032AF26557;00032B81F3BA;00032C05AE8B;00032C2E01B4;00032C83D7EA;00032CC93EB9;00032D2C580D;00032DB7FFFE;00032DF4F77E;00032E0B121D;00032E3898B1;00032E67CC0E;00032E9EBBD4;00032EB19153;00032EF7EC12;00032F8D0A89;00032FDC0FD7;000330327C13;000330AC37AA;000331236834;0003318B1C15;00033215E87C;00033295F8DD;00033301F9A0;00033395D3B8;0003344C53D9;000335025C00;0003358FCDC3;00033600478B;00033694D936;00033709B0AA;000337BF4CE8;000337D2D263;000338427CA0;000338ED33EB;0003399EBC26;000339B9DF76;000339D15833;00033A30CAA1;00033AF0EE17;00033B29C8F2;00033B641E10;00033BB2B13D;00033BF7E0E1;00033C640913;00033CAB6F31;00033D3AFD94;00033DBEB865;00033DE70B8E;00033E3CE1C4;00033E824893;00033EE561E7;00033F18967F;00033FD90383;0003409B3740;000340FE5094;00034189F885;000341C6F005;000341DD0AA4;000341F551B5;000342B78572;000343009923;000343C2C4C5;000344503688;000344C0B050;000345811D54;0003458B4767;000345CECD54;0003468A5FA0;0003474AB3BC;000347598F68;000347D34AFF;0003484A7B89;000348D4D413;0003494E6575;0003496A5D36;00034972C9B1;000349B96CD0;00034A42364E;00034B0469FA;00034B613233;00034C0D8C94;00034CCDB00A;00034D8526E8;00034D986C8A;00034E0FDB4E;00034E590F79;00034EA45819;00034EC9CC31;00034F250C8F;00034F7B78CB;00034F86C27E;000350134318;000350BFD6A7;0003515DD6EC;000351754FA9;000351F23C29;000352A51CE1;00035362DD39;0003541CD9F0;000354948FAC;000354C7FA2A;000354D535F2;000355081BB5;0003552B77B0;000355362455;000355CB42CC;0003561A481A;00035670B456;000356EA6FED;00035761A077;000357C95458;0003585420BF;000358D43120;0003594031E3;000359D40BFB;000359DF3542;00035A2CC4AD;00035ABA3670;00035B2AB038;00035BBF41E3;00035C341957;00035CE9B595;00035CFD3B10;00035D6CE54D;00035E179C98;00035EC924D3;00035EE44823;00035EFBC0E0;00035F5B334E;0003601B56C4;00036054319F;0003608E86BD;000360DD19EA;00036122498E;0003618E71C0;000361D5D7DE;000362656641;000362E92112;00036311743B;000363674A71;000363ACB140;0003640FCA94;000364CCF9F9;0003658A149C;000365FEEC10;00036676A1CC;000366AA0C4A;000366B74812;00036777212D;0003682B7A77;000368A33033;000368ADDCD8;00036942FB4F;00036992009D;000369E86CD9;00036A622870;00036AD958FA;00036B410CDB;00036BCBD942;00036C4BE9A3;00036CB7EA66;00036D4BC47E;00036D56EDC5;00036DC08BCA;00036E4DFD8D;00036EBE7755;00036F530900;00036FC7E074;0003707D7CB2;00037091022D;00037100AC6A;000371AB63B5;0003725CEBF0;000372780F40;0003728F87FD;000372EEFA6B;000373AF1DE1;000373E7F8BC;000374224DDA;00037470E107;000374B610AB;0003752238DD;000375699EFB;000375F92D5E;00037668D79B;000376DC9B1F;000377327155;00037777D824;000377DAF178;0003788CFC62;0003794D6966;00037A0F9D23;00037A72B677;00037A7884AC;00037B02DD36;00037B7A92F2;00037B853F97;00037BFEFB2E;00037C762BB8;00037CDDDF99;00037D68AC00;00037DE8BC61;00037E54BD24;00037EE8973C;00037EF3C083;00037FB5EC25;000380435DE8;000380B3D7B0;00038148695B;000381BD40CF;00038272DD0D;000382866288;000382F60CC5;000383A0C410;000384524C4B;0003846D6F9B;00038484E858;000384E45AC6;000385A47E3C;000385DD5917;00038617AE35;000386664162;000386AB7106;000387179938;0003875EFF56;000387EE8DB9;0003885E37F6;000388D1FB7A;00038927D1B0;0003896D387F;000389D051D3;00038A87E79A;00038B0A9C22;00038B4E220F;00038BEA79DE;00038C522DBF;00038C9C2768;00038D08FC6F;00038DBB9B01;00038E355698;00038EAC8722;00038F143B03;00038F9F076A;0003901F17CB;0003908B188E;0003911EF2A6;0003912A1BED;0003912BC648;000391B9380B;000391F212E6;0003922C6804;000392A5AA01;000392D8DE99;0003935799D3;000393EAA5D2;000393F31E08;00039456DDBC;000394ACB3F2;0003950EF0E9;0003954192C2;00039559C90A;000395A8BF76;000395B51283;0003962F4FD8;000396C35BC0;0003977CD28B;000397E557B7;00039834AAC2;000398517E64;000398BED6F0;000399277868;000399CAEF3F;00039A61862B;00039A6749CE;00039AD3913B;00039B247ABE;00039B4AEFC8;00039B98F503;00039C2F1ED5;00039C880EE3;00039D259418;00039D871F06;00039DC57CBA;00039E140FE7;00039EBD8245;00039F3259B9;00039FC34352;0003A06470CE;0003A108BFB3;0003A183AC83;0003A1EACDED;0003A28BFB69;0003A2D811A1;0003A3399C8F;0003A38CC762;0003A4370038;0003A44FA8DA;0003A48CC98E;0003A52D6A5D;0003A5BADC20;0003A5F3B6FB;0003A62E0C19;0003A6A74E16;0003A6DA82AE;0003A7593DE8;0003A8198AE2;0003A8461909;0003A8B45CBD;0003A8F63261;0003A96D6152;0003A99F1265;0003AA588930;0003AAAEF56C;0003AB4F3F55;0003AB5637C9;0003AC17C2F0;0003AC920045;0003AD092F36;0003AD180AE2;0003AD823634;0003ADADEC8D;0003ADC622D5;0003AE4D926B;0003AE6737D8;0003AEA1C4A9;0003AF0E0C16;0003AF5EF599;0003AFC13290;0003B04AD225;0003B083AD00;0003B0BE021E;0003B137441B;0003B16A78B3;0003B1BB6236;0003B24574A6;0003B2CF4EAE;0003B3860A04;0003B3E195EF;0003B4130803;0003B4963917;0003B5423BED;0003B5EB9F91;0003B69A52A3;0003B74343B4;0003B76B2DFD;0003B7F37B85;0003B84C78A5;0003B89CEE66;0003B8FE4D08;0003B9B76B1B;0003BA24C3A7;0003BA804F92;0003BAD2FEC5;0003BB3AE434;0003BBA72BA1;0003BBF81524;0003BC8BEF3C;0003BCE73C7B;0003BD201756;0003BD5A6C74;0003BDD3AE71;0003BE06E309;0003BEC4D401;0003BF1D4AFA;0003BF728477;0003C0128BDC;0003C0960154;0003C0D36698;0003C176DD6F;0003C1E7F230;0003C223AD2E;0003C278597C;0003C31BC208;0003C36E713B;0003C37C729C;0003C4032D42;0003C4963941;0003C499DD24;0003C4B7CFD1;0003C4BF1BC2;0003C57FF598;0003C62510D6;0003C65CD37B;0003C6C91AE8;0003C71A046B;0003C7782E06;0003C7C63341;0003C85C5D13;0003C8B54D21;0003C952D256;0003C9B45D44;0003C9F2BAF8;0003CA414E25;0003CAEAC083;0003CB5F97F7;0003CBF08190;0003CC91AF0C;0003CD35FDF1;0003CDB0EAC1;0003CE180C2B;0003CEB939A7;0003CF054FDF;0003CF66DACD;0003CFBA05A0;0003CFBBF843;0003CFF64D61;0003D0AF084E;0003D0CFA44F;0003D12C0667;0003D1D9EB05;0003D212C5E0;0003D24D1AFE;0003D2C65CFB;0003D2F99193;0003D329CEA0;0003D3A850B0;0003D3B29935;0003D440B7AC;0003D465B853;0003D4F8C452;0003D506E06F;0003D5347E22;0003D5D99960;0003D63F27BF;0003D68FB315;0003D6C12529;0003D726D3AE;0003D7CC8B0D;0003D7F18BB4;0003D8443AE7;0003D8610E89;0003D8CC395E;0003D90E5666;0003D948DE0A;0003D9B0C379;0003DA1D0AE6;0003DA6DF469;0003DAD03160;0003DB1E369B;0003DB571176;0003DB916694;0003DC0AA891;0003DC3DDD29;0003DCBA3563;0003DCD26BAB;0003DD667793;0003DDD22CBF;0003DE02713E;0003DE1741F2;0003DEBC263E;0003DF0B7949;0003DF589515;0003DFC3C88D;0003E05793EC;0003E061DC71;0003E10DDF47;0003E1BDA032;0003E228CB07;0003E246BDB4;0003E246C2F1;0003E24CCA94;0003E2C34715;0003E2CBBF4B;0003E332C09F;0003E36B9B7A;0003E3A5F098;0003E41F3295;0003E452672D;0003E504F482;0003E54D0888;0003E550A766;0003E5FC41C4;0003E6AB1909;0003E6DDBAE2;0003E76AC37D;0003E7E3E9D8;0003E837AEB9;0003E879845D;0003E8E7C811;0003E9A6021B;0003EA31591D;0003EAF29942;0003EB1799E9;0003EB59B6F1;0003EBC10B28;0003EC14D009;0003EC8B4C8A;0003ECB0BA1D;0003ED5A7CB6",
        "STORE_CATALOG_ID" to "13332",
    )

    //endregion

    fun getResource(name: String): ByteArray {
        val stream = Data::class.java.getResourceAsStream("/$name")
            ?: throw IOException("Missing internal resource: $name")
        return stream.readAllBytes()
    }

    fun getResourceOrNull(name: String): ByteArray? {
        try {
            val stream = Data::class.java.getResourceAsStream("/$name")
                ?: return null
            return stream.readAllBytes()
        } catch (e: IOException) {
            return null
        }
    }

    private fun getResourceReader(name: String): BufferedReader {
        val stream = Data::class.java.getResourceAsStream("/$name")
            ?: throw IllegalStateException("Missing internal resource: $name")
        return stream.bufferedReader()
    }

    fun createDimeResponse(): Map<String, String> {
        val dime = getResource("data/dime.xml")
            .toString(Charsets.UTF_8);
        return mapOf("Config" to dime)
    }

    fun loadBiniCompressed(): Map<String, String> = loadChunkedFile("data/bini.bin.chunked")

    private fun loadChunkedFile(path: String): Map<String, String> {
        val reader = getResourceReader(path)
        val lines = reader.readLines()
        val out = LinkedHashMap<String, String>(lines.size + 1)
        lines.forEach {
            val parts = it.split(':', limit = 2)
            if (parts.size > 2) throw RuntimeException("ERR TOO MANY VALUES")
            out[parts[0]] = parts[1]
        }
        return out
    }

    fun loadTLK(name: String = "default"): Map<String, String> {
        return try {
            loadChunkedFile("data/tlk/$name.tlk.chunked")
        } catch (e: IOException) {
            loadChunkedFile("data/tlk/default.tlk.chunked")
        }
    }
}