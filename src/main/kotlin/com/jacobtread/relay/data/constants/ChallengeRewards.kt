package com.jacobtread.relay.data.constants

/**
 * This enum contains all the challenge reward banners that are
 * known.
 *
 * Custom verison of the banners loaded from the HTTP server
 * can be made using GIMP. Export as .dds with the compression format
 * set to "BC3 / DXT5"
 *
 * @property id The csreward value for this banner
 * @property rewardName The name of this challenge
 * @property text The text displayed on the banner
 * @property iamgeName The name of the banner image
 */
@Suppress("MemberVisibilityCanBePrivate", "unused")
enum class ChallengeRewards(
    val id: Int,
    val rewardName: String,
    val text: String,
    val iamgeName: String?,
) {

    // No Reward Banner
    NONE(0, "NONE", "NONE", null),

    MASS_EFFECT(100, "Mass Effect", "Best of the Best", "MEMstr"),

    // Female Version
    MASS_EFFECT_ALT(207, "Mass Effect", "Best of the Best", "MEMstrF"),


    SQUAD_ELITE(104, "Squad Elite", "Operator", "SqadMstr"),
    SPECTRE_MASTERY(105, "Spectre Mastery", "Hardcore", "SpecMstr"),

    SOLO_MASTERY(106, "Solo Mastery", "Lone Wolf", "SoloMstr"),
    SOLO_MASTERY_ALT(214, "Solo Mastery", "Lone Wolf", "SoloMstr"),

    // Weapons
    SHOTGUN_MASTERY(107, "Shotgun Mastery", "Street Sweeper", "ShtgMstr"),
    ASSAULT_RIFLE_MASTERY(108, "Assault Rifle Mastery", "Marksman", "AsltMstr"),
    PISTOL_MASTERY(109, "Pistol Mastery", "Gun Slinger", "PstlMstr"),
    SMG_MASTERY(110, "SMG Mastery", "SMG Mastery", "SmgMstr"),
    SNIPER_RIFLE_MASTERY(111, "Sniper Rifle Mastery", "Sniper", "SnpMstr"),

    RESURGENCE_MASTERY(112, "Resurgence Mastery", "Insurgent", "RsrgMstr"),
    REBELLION_MASTERY(113, "Rebellion Mastery", "Rebel", "ReblMstr"),
    EARTH_MASTERY(114, "Earth Mastery", "Survivor", "EarthMstr"),
    EARTH_MASTERY_ALT(217, "Earth Mastery", "Survivor", "EarthMstr"),
    RETALIATION_MASTERY(115, "Retaliation Mastery", "Defender", "InvsnMstr"),
    RECKONING_MASTERY(206, "Reckoning Mastery", "Punisher", "MstrReckoning"),

    // Enemies
    COMBAT_MASTERY(116, "Combat Mastery", "Dog of War", "CmbtMstr"),
    COMBAT_MASTERY_ALT_1(211, "Combat Mastery", "Dog of War", "CmbtMstr"),
    COMBAT_MASTERY_ALT_2(212, "Combat Mastery", "Dog of War", "CmbtMstr"),


    REAPER_MASTERY(117, "Reaper Mastery", "Death Guard", "ReapMstr"),
    CERBERUS_MASTERY(118, "Cerberus Mastery", "War Fighter", "CrbrsMstr"),
    CERBERUS_MASTERY_ALT(218, "Cerberus Mastery", "War Fighter", "CrbrsMstr"),
    GETH_MASTERY(119, "Geth Mastery", "Cyber Warrior", "GethMstr"),
    COLLECTOR_MASTERY(120, "Collector Mastery", "Collector General", "CollctrMstr"),

    BLOOD_PACK_MASTERY(121, "Blood Pack Mastery", "Monster", "BloodPak"),
    COMMANDO_MASTERY(122, "Commando Mastery", "Council Operative", "CmdoMstr"),
    COMMANDO_MASTERY_ALT(216, "Commando Mastery", "Council Operative", "CmdoMstr"),
    MACHINE_MASTERY(123, "Machine Mastery", "Machinist", "MachMstr"),
    OUTSIDER_MASTERY(124, "Outsider Mastery", "Outsider", "Outsider"),

    N7_MASTERY(125, "N7 Mastery", "N7", "N7Mstr"),
    N7_MASTERY_ALT(213, "N7 Mastery", "N7", "N7Mstr"),

    MAP_MASTERY(126, "Map Mastery", "Nomad", "MapMstr"),
    BIOTIC_MASTERY(127, "Biotic Mastery", "Biotic God", "BioticMstr"),
    BIOTIC_MASTERY_ALT(215, "Biotic Mastery", "Biotic God", "BioticMstr"),

    TECH_MASTERY(128, "Tech Mastery", "Mathemagician", "TechMstr"),
    TECH_MASTERY_ALT_1(209, "Tech Mastery", "Mathemagician", "TechMstr"),
    TECH_MASTERY_ALT_2(210, "Tech Mastery", "Mathemagician", "TechMstr"),

    HALLOWEEN_CHALLENGE(150, "Halloween Challenge", "Hallowed Hero", "Halloween"),
    N7_DAY_ALLIANCE_CHALLENGE(153, "N7 Day Alliance Challenge", "Layalist", "N7Day"),

    // Banner for Staff
    BIOWARE_STAFF(154, "BioWare", "BioWare", "Bioware"),

    OPERATION_PRIVATEER(155, "Operation Privateer", "Corsair", "Corsair"),
    OPERATION_PROPHENCY(157, "Operation Prophecy", "Hero of the Last Days", "Prophecy"),
    OPERATION_TRIBUTE(208, "Operation Tribute", "Operation Tribute", "Zaeed"),
    OPERATION_HEARTBREAKER(219, "Operation Heartbreaker", "Heartbreaker", "VolusLover"),
    OPERATION_LODESTAR(220, "Operation Lodestart", "Stood Fast, Stood Strong, Stood Together", "FullTeam"),

    TURIAL_REBEL(221, "Turial Rebel", "Turial Rebel", null),
    ASARI_SCIENTIST(222, "Asari Scientist", "Asari Scientist", null)
}
