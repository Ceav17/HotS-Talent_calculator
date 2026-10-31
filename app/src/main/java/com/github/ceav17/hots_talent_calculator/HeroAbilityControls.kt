package com.github.ceav17.hots_talent_calculator

internal data class HeroUtilityAbilities(
    val mount: HeroAbility?,
    val hearthstone: HeroAbility?
)

internal fun heroUtilityAbilities(catalog: HeroAbilityCatalog): HeroUtilityAbilities {
    val mounts = catalog.abilities.filter { it.hotkey == "Z" && it.availableWithoutTalents }
    val generic = mounts.firstOrNull { it.iconPath == "Heroes/storm_ui_icon_generic_mount.png" }
    val specific = mounts.firstOrNull {
        it != generic && "_mount" in it.iconPath.substringAfterLast('/')
    }
    val mount = when (catalog.heroId) {
        "junkrat" -> generic
        "dva" -> mounts.firstOrNull { it.unitId == "HeroDVaMech" }
        // Shove uses Z, but its icon name has no mount suffix.
        "gall" -> mounts.firstOrNull { it.abilityId == "GallShove" }
        else -> specific ?: generic ?: mounts.firstOrNull()
    }
    val hearthstone = if (catalog.hasHearthstone) {
        catalog.abilities.firstOrNull { it.hotkey == "B" }
    } else null
    return HeroUtilityAbilities(mount, hearthstone)
}

// These effects have ability IDs in HDP even though they trigger automatically.
private val automaticTraitIds = setOf(
    ":PASSIVE:",
    "AbathurSpawnLocusts",
    "AnubarakLegionOfBeetlesVisual",
    "ArtanisShieldOverloadDummy",
    "RockstarDummy",
    "FalstadTailwindCooldownDisplay",
    "FenixShieldCapacitorVisual",
    "JunkratTotalMayhemDirtyTrickster",
    "TassadarResonanceBeamArcDischargeDummy",
    "ButcherFreshMeatDummy",
    "TyraelArchangelsWrathDummy",
    "VarianHeroicStrike",
    "NecromancerRaiseSkeletonDisplayAbility"
)

internal fun hasPassiveHotkey(ability: HeroAbility, catalog: HeroAbilityCatalog): Boolean {
    if (ability.hotkey == "Z") return catalog.heroId in setOf("lucio", "lunara")
    if (ability.hotkey != "D") return false
    // Include talent variants before icon deduplication, even when the talent changes the icon.
    return catalog.abilities.filter {
        it.hotkey == "D" && (it.name == ability.name || it.iconPath == ability.iconPath)
    }.all { it.abilityId in automaticTraitIds }
}
