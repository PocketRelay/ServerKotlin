package com.jacobtread.kme.blaze

object Components {
    const val AUTHENTICATION = 0x1
    const val EXAMPLE = 0x3
    const val GAME_MANAGER = 0x4
    const val REDIRECTOR = 0x5
    const val PLAY_GROUPS = 0x6
    const val STATS = 0x7
    const val UTIL = 0x9
    const val CENSUS_DATA = 0xA
    const val CLUBS = 0xB
    const val GAME_REPORT_LEGACY = 0xC
    const val LEAGUE = 0xD
    const val MAIL = 0xE
    const val MESSAGING = 0xF
    const val LOCKER = 0x14
    const val ROOMS = 0x15
    const val TOURNAMENTS = 0x17
    const val COMMERCE_INFO = 0x18
    const val ASSOCIATION_LISTS = 0x19
    const val GPS_CONTENT_CONTROLLER = 0x1B
    const val GAME_REPORTING = 0x1C
    const val DYNAMIC_FILTER = 0x7D0
    const val RSP = 0x801
    const val USER_SESSIONS = 0x7802

    private val NAME_LOOKUP = mapOf(
        AUTHENTICATION to "AUTHENTICATION",
        EXAMPLE to "EXAMPLE",
        GAME_MANAGER to "GAME_MANAGER",
        REDIRECTOR to "REDIRECTOR",
        PLAY_GROUPS to "PLAY_GROUPS",
        STATS to "STATS",
        UTIL to "UTIL",
        CENSUS_DATA to "CENSUS_DATA",
        CLUBS to "CLUBS",
        GAME_REPORT_LEGACY to "GAME_REPORT_LEGACY",
        LEAGUE to "LEAGUE",
        MAIL to "MAIL",
        MESSAGING to "MESSAGING",
        LOCKER to "LOCKER",
        ROOMS to "ROOMS",
        TOURNAMENTS to "TOURNAMENTS",
        COMMERCE_INFO to "COMMERCE_INFO",
        ASSOCIATION_LISTS to "ASSOCIATION_LISTS",
        GPS_CONTENT_CONTROLLER to "GPS_CONTENT_CONTROLLER",
        GAME_REPORTING to "GAME_REPORTING",
        DYNAMIC_FILTER to "DYNAMIC_FILTER",
        RSP to "RSP",
        USER_SESSIONS to "USER_SESSIONS",
    )

    fun getName(component: Int): String {
        return NAME_LOOKUP.getOrElse(component) {
            "UNKNOWN (0x${component.toString(16)})"
        }
    }

    fun hasName(component: Int): Boolean = NAME_LOOKUP.containsKey(component)

}