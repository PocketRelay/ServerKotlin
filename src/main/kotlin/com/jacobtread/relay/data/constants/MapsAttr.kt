package com.jacobtread.relay.data.constants

enum class MapsAttr(
    val mapName: String,
    val location: String,
    val key: String,
) {
    // Random Map
    UNKNOWN("Unknown Location", "?", "map0"),
    RANDOM("Unknown Location", "?", "random"),

    // Standard Maps
    FIREBASE_DAGGER("Firebase Dagger", "Ontarom", "map2"),
    FIREBASE_GHOST("Firebase Ghost", "Benning", "map3"),
    FIREBASE_GIANT("Firebase Giant", "Tuchanka", "map4"),
    FIREBASE_REACTOR("Firebase Reactor", "Cyone", "map5"),
    FIREBASE_GLACIER("Firebase Glacier", "Sanctum", "map7"),
    FIREBASE_WHITE("Firebase White", "Noveria", "map8"),

    // Resurgence Pack Maps
    FIREBASE_CONDOR("Firebase Condor", "Palaven", "map9"),
    FIREBASE_HYDRA("Firebase Hydra", "Arvuna", "map10"),

    // Rebellion Pack Maps
    FIREBASE_JADE("Firebase Jade", "Sur'Kesh", "map11"),
    FIREBASE_GODDESS("Firebase Goddess", "Thessia", "map13"),

    // Earth Maps
    FIREBASE_RIO("Firebase Rio", "Earth", "map14"),
    FIREBASE_VANCOUVER("Firebase Vancouver", "Earth", "map15"),
    FIREBASE_LONDON("Firebase London", "Earth", "map16"),

    // Retaliation Hazard Maps
    FIREBASE_GLACIER_HAZARD("☣ Firebase Glacier ☣", "Sanctum", "map17"),
    FIREBASE_DAGGER_HAZARD("☣ Firebase Dagger ☣", "Ontarom", "map18"),
    FIREBASE_REACTOR_HAZARD("☣ Firebase Reactor ☣", "Cyone", "map19"),
    FIREBASE_GHOST_HAZARD("☣ Firebase Ghost ☣", "Benning", "map20"),
    FIREBASE_GIANT_HAZARD("☣ Firebase Giant ☣", "Tuchanka", "map21"),
    FIREBASE_WHITE_HAZARD("☣ Firebase White ☣", "Noveria", "map22"),

    // Unknown Maps
    MAP_1("Map 1", "?", "map1"),
    MAP_6("Map 6", "?", "map6"),
    MAP_12("Map 12", "?", "map12"),
    MAP_23("Map 23", "?", "map23"),
    MAP_24("Map 24", "?", "map24"),
    MAP_25("Map 25", "?", "map25"),
    MAP_26("Map 26", "?", "map26"),
    MAP_27("Map 27", "?", "map27"),
    MAP_28("Map 28", "?", "map28"),
    MAP_29("Map 29", "?", "map29");

    companion object {
        private const val MAP_ATTR = "ME3map"
        const val MAP_RULE = "ME3_gameMapMatchRule"

        fun getFromAttr(map: Map<String, String>): MapsAttr {
            val value = map[MAP_ATTR] ?: return UNKNOWN
            return values().firstOrNull { it.key == value } ?: UNKNOWN
        }
    }
}
