package com.jacobtread.kme.data.constants

enum class ChallengeRewards(
    val value: Int,
    val rewardName: String,
    val text: String = rewardName,
) {

    // No Reward Banner
    NONE(0, "NONE"),

    MASS_EFFECT(100, "Mass Effect", "Best of the Best"),


    SQUAD_ELITE(104, "Squad Elite", "Operator"),
    SPECTRE_MASTERY(105, "Spectre Mastery", "Hardcore"),
    SOLO_MASTERY(106, "Solo Mastery", "Lone Wolf"),

    // Weapons
    SHOTGUN_MASTERY(107, "Shotgun Mastery", "Street Sweeper"),
    ASSAULT_RIFLE_MASTERY(108, "Assault Rifle Mastery", "Assault Rifle"),
    PISTOL_MASTERY(109, "Pistol Mastery", "Gun Slinger"),
    SMG_MASTERY(110, "SMG Mastery", "SMG Mastery"),
    SNIPER_RIFLE_MASTERY(111, "Sniper Rifle Mastery", "Sniper Rifle"),

    RESURGENCE_MASTERY(112, "Resurgence Mastery", "Insurgent"),
    REBELLION_MASTERY(113, "Rebellion Mastery", "Rebel"),
    EARTH_MASTERY(114, "Earth Mastery", "Survivor"),
    RETALIATION_MASTERY(115, "Retaliation Mastery", "Defender"),

    // Enemies
    COMBAT_MASTERY(116, "Combat Mastery", "Dog of War"),
    REAPER_MASTERY(117, "Reaper Mastery", "Death Guard"),
    CERBERUS_MASTERY(118, "Cerberus Mastery", "War Fighter"),
    GETH_MASTERY(119, "Geth Mastery", "Cyber Warrior"),
    COLLECTOR_MASTERY(120, "Collector Mastery", "Collector General"),

    BLOOD_PACK_MASTERY(121, "Blood Pack Mastery", "Monster"),
    COMMANDO_MASTERY(122, "Commando Mastery", "Council Operative"),
    MACHINE_MASTERY(123, "Machine Mastery", "Machinist"),
    OUTSIDER_MASTERY(124, "Outsider Mastery", "Outsider"),

    N7_MASTERY(125, "N7 Mastery", "N7"),
    MAP_MASTERY(126, "Map Mastery", "Nomad"),
    BIOTIC_MASTERY(127, "Biotic Mastery", "Biotic God"),
    TECH_MASTERY(128, "Tech Mastery", "Mathemagician"),


    HALLOWEEN_CHALLENGE(150, "Halloween Challenge", "Hallowed Hero"),

    N7_DAY_ALLIANCE_CHALLENGE(153, "N7 Day Alliance Challenge", "Layalist"),

    // Banner for Staff
    BIOWARE_STAFF(154, "BioWare", "BioWare"),

    OPERATION_PRIVATEER(155, "Operation Privateer", "Corsair"),

    OPERATION_PROPHENCY(157, "Operation Prophecy", "Hero of the Last Days"),

    RECKONING_MASTERY(206, "Reckoning Mastery", "Punisher"),
    MASS_EFFECT_ALT(207, "Mass Effect", "Best of the Best"),
    OPERATION_TRIBUTE(208, "Operation Tribute"),

    TECH_MASTERY_ALT_1(209, "Tech Mastery", "Mathemagician"),
    TECH_MASTERY_ALT_2(210, "Tech Mastery", "Mathemagician"),

    COMBAT_MASTERY_ALT_1(211, "Combat Mastery", "Dog of War"),
    COMBAT_MASTERY_ALT_2(212, "Combat Mastery", "Dog of War"),

    N7_MASTERY_ALT(213, "N7 Mastery", "N7"),
    SOLO_MASTERY_ALT(214, "Solo Mastery", "Lone Wolf"),
    BIOTIC_MASTERY_ALT(215, "Biotic Mastery", "Biotic God"),

    COMMANDO_MASTERY_ALT(216, "Commando Mastery", "Council Operative"),
    EARTH_MASTERY_ALT(217, "Earth Mastery", "Survivor"),

    CERBERUS_MASTERY_ALT(218, "Cerberus Mastery", "War Fighter"),
    OPERATION_HEARTBREAKER(219, "Operation Heartbreaker", "Heartbreaker"),
    OPERATION_LODESTAR(220, "Operation Lodestart", "Stood Fast, Stood Strong, Stood Together"),

    TURIAL_REBEL(221, "Turial Rebel"),
    ASARI_SCIENTIST(222, "Asari Scientist")

}