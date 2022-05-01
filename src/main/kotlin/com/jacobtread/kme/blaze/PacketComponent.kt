package com.jacobtread.kme.blaze


enum class PacketComponent(val id: Int) {
    AUTHENTICATION(0x1),
    EXAMPLE(0x3),
    GAME_MANAGER(0x4),
    REDIRECTOR(0x5),
    PLAY_GROUPS(0x6),
    STATS(0x7),
    UTIL(0x9),
    CENSUS_DATA(0xA),
    CLUBS(0xB),
    GAME_REPORT_LEGACY(0xC),
    LEAGUE(0xD),
    MAIL(0xE),
    MESSAGING(0xF),
    LOCKER(0x14),
    ROOMS(0x15),
    TOURNAMENTS(0x17),
    COMMERCE_INFO(0x18),
    ASSOCIATION_LISTS(0x19),
    GPS_CONTENT_CONTROLLER(0x1B),
    GAME_REPORTING(0x1C),
    DYNAMIC_FILTER(0x7D0),
    RSP(0x801),
    USER_SESSIONS(0x7802);

    companion object {
        private val LOOKUP: HashMap<Int, PacketComponent>
        init {
            val values = values()
            LOOKUP = HashMap(values.size)
            values.forEach { LOOKUP[it.id] = it }
        }

        fun from(value: Int): PacketComponent? = LOOKUP[value]
    }
}