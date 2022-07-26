package com.jacobtread.kme.database.old.tables

import org.jetbrains.exposed.dao.id.IntIdTable

object PlayerCharactersTable : IntIdTable("player_characters") {
    val player = reference("player_id", PlayersTable)

    val index = integer("index")
    val kitName = varchar("kit_name", length = 128)
    val name = varchar("name", length = 128)
    val tint1 = integer("tint_1")
    val tint2 = integer("tint_2")
    val pattern = integer("pattern")
    val patternColor = integer("patternColor")
    val phong = integer("phong")
    val emissive = integer("emissive")
    val skinTone = integer("skin_tone")
    val secondsPlayed = long("seconds_played")

    val timeStampYear = integer("ts_year")
    val timeStampMonth = integer("ts_month")
    val timeStampDay = integer("ts_day")
    val timeStampSeconds = integer("ts_seconds")

    val powers = text("powers")
    val hotkeys = text("hotkeys")
    val weapons = text("weapons")
    val weaponMods = text("weapon_mods")
    val deployed = bool("deployed")
    val leveledUp = bool("leveled_up")
}