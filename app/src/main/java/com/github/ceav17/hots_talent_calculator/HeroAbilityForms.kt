package com.github.ceav17.hots_talent_calculator

internal data class HeroAbilityPage(
    val rows: List<HeroAbilityRow>,
    val descriptions: List<HeroAbility>
)

internal data class HeroAbilityForms(
    val normal: HeroAbilityPage,
    val alternate: HeroAbilityPage? = null
)

internal fun heroAbilityForms(catalog: HeroAbilityCatalog): HeroAbilityForms {
    val alternateIds = when (catalog.heroId) {
        "abathur" -> setOf("stab", "spike_burst", "carapace")
        "alexstrasza" -> catalog.abilities.filter {
            it.unitId == "HeroAlexstraszaDragon" && it.hotkey in setOf("Q", "W", "E", "D")
        }.map { it.id }.toSet()
        "blaze" -> catalog.abilities.filter {
            it.unitId == "FirebatBunkerDropBunkerUnit" && it.hotkey in setOf("Q", "W")
        }.map { it.id }.toSet()
        "chen" -> setOf("storm", "earth", "fire")
        "dva" -> setOf("torpedo_dash", "concussive_pulse", "call_mech", "big_shot", "pilot_mode")
        "deathwing" -> setOf("lava_burst", "earth_shatter")
        "greymane" -> setOf("razor_swipe", "disengage")
        "ragnaros" -> setOf("molten_swing", "meteor_shower", "explosive_rune")
        "tychus" -> setOf("annihilate", "ragnarok_missiles", "thrusters")
        "valeera" -> setOf("ambush", "cheap_shot", "garrote")
        else -> return HeroAbilityForms(HeroAbilityPage(buildHeroAbilityRows(catalog), heroBaseAbilities(catalog)))
    }
    val alternatives = catalog.abilities.filter { it.id in alternateIds }
    val normalCatalog = catalog.copy(abilities = catalog.abilities.filterNot { it.id in alternateIds })
    val normal = HeroAbilityPage(buildHeroAbilityRows(normalCatalog), heroBaseAbilities(normalCatalog))
    val alternateByKey = alternatives.associateBy { it.hotkey }
    val isolatedForm = catalog.heroId in setOf("abathur", "blaze", "chen", "dva", "tychus")

    val alternate = if (isolatedForm) {
        val trait = if (catalog.heroId == "abathur") normal.descriptions.firstOrNull { it.hotkey == "D" }
            else null
        val entries = alternatives + listOfNotNull(trait)
        val order = listOf("Q", "W", "E", "R", "D")
        val slots = MutableList<HeroAbility?>(6) { null }
        entries.forEach { ability ->
            val column = when (ability.hotkey) {
                "Q" -> 0
                "W" -> 1
                "E" -> 2
                "R" -> 3
                "D" -> 5
                else -> return@forEach
            }
            slots[column] = ability
        }
        HeroAbilityPage(listOf(HeroAbilityRow(slots)), entries.sortedBy { order.indexOf(it.hotkey) })
    } else {
        val mainIds = normal.descriptions.map { it.id }.toSet()
        fun replace(ability: HeroAbility?): HeroAbility? = when {
            ability == null -> null
            catalog.heroId == "valeera" && ability.hotkey == "D" -> null
            ability.id in mainIds -> alternateByKey[ability.hotkey] ?: ability
            else -> ability
        }
        HeroAbilityPage(
            normal.rows.map { row -> HeroAbilityRow(row.slots.map(::replace)) }
                .filter { row -> row.slots.any { it != null } },
            normal.descriptions.mapNotNull(::replace)
        )
    }
    return HeroAbilityForms(normal, alternate)
}
