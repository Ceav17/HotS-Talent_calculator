package com.github.ceav17.hots_talent_calculator

internal data class HeroAbility(
    val id: String,
    val name: String,
    val iconPath: String,
    val hotkey: String,
    val unitId: String,
    val linkId: String,
    val parentLinks: List<String> = emptyList(),
    val activeOrigin: String? = null,
    val hotkeySource: String? = null,
    val availableWithoutTalents: Boolean = true,
    val abilityId: String? = null,
    val description: String = ""
)

internal data class HeroAbilityCatalog(
    val heroId: String,
    val abilities: List<HeroAbility>,
    // In talent level/position order, including upgrades for resolving parent links.
    val heroicTalentIcons: Map<String, String> = emptyMap(),
    val hasHearthstone: Boolean = true,
    val talentTiers: List<HeroTalentTier> = emptyList()
)

internal data class HeroTalent(
    val id: String,
    val name: String,
    val description: String,
    val iconPath: String
)

internal data class HeroTalentTier(val level: Int, val talents: List<HeroTalent>)

internal data class HeroAbilityRow(val slots: List<HeroAbility?>)

internal fun heroBaseAbilities(catalog: HeroAbilityCatalog): List<HeroAbility> {
    val mainUnit = catalog.abilities.firstOrNull {
        it.hotkey in listOf("Q", "W", "E", "R", "D") && it.parentLinks.isEmpty()
    }?.unitId
    val keyOrder = listOf("Q", "W", "E", "R", "D", "1", "2", "3", "4", "5", "6")
    // These form descriptions are part of the displayed kit, with an explicit reading order.
    val variantsAfter = when (catalog.heroId) {
        "abathur" -> mapOf("toxic_nest" to listOf("stab", "spike_burst", "carapace"))
        "alexstrasza" -> mapOf(
            "gift_of_life" to listOf("breath_of_life"),
            "abundance" to listOf("preservation"),
            "flame_buffet" to listOf("wing_buffet")
        )
        "chen" -> mapOf("stagger" to listOf("breath_of_fire"))
        "deathwing" -> mapOf("onslaught" to listOf("lava_burst", "earth_shatter"))
        "greymane" -> mapOf("gilnean_cocktail" to listOf("razor_swipe"), "darkflight" to listOf("disengage"))
        "valeera" -> mapOf(
            "sinister_strike" to listOf("ambush"),
            "blade_flurry" to listOf("cheap_shot"),
            "eviscerate" to listOf("garrote")
        )
        else -> emptyMap()
    }
    val variantIds = variantsAfter.values.flatten().toSet()
    val baseAbilities = buildHeroAbilityRows(catalog).flatMap { it.slots }.filterNotNull()
        .filter { ability ->
            when {
                ability.id in variantIds -> false
                ability.hotkey.toIntOrNull() != null ->
                    ability.activeOrigin == "initial" && ability.availableWithoutTalents
                // Kharazim chooses his trait at level one instead of having a generic D.
                catalog.heroId == "kharazim" && ability.hotkey == "D" -> true
                ability.parentLinks.isNotEmpty() -> false
                catalog.heroId == "the-lost-vikings" -> true
                else -> ability.unitId == mainUnit
            }
        }
        .sortedBy { keyOrder.indexOf(it.hotkey) }
    val byId = catalog.abilities.associateBy { it.id }
    return baseAbilities.flatMap { ability ->
        listOf(ability) + variantsAfter[ability.id].orEmpty().mapNotNull(byId::get)
    }
}

internal fun buildHeroAbilityRows(catalog: HeroAbilityCatalog): List<HeroAbilityRow> {
    val numberSlots = MutableList<HeroAbility?>(6) { null }
    catalog.abilities.forEach { ability ->
        // Older exports mistook the pilot's bomb charge meter for a usable numeric command.
        if (catalog.heroId == "dva" && ability.abilityId == "DVaMechPilotSelfDestruct") return@forEach
        val number = ability.hotkey.toIntOrNull()
        val isInitial = ability.activeOrigin == "initial" && ability.availableWithoutTalents
        val isSelection = ability.hotkeySource == "selection_binding"
        if (number in 1..6 && (isInitial || isSelection)) {
            val column = requireNotNull(number) - 1
            if (numberSlots[column] == null) numberSlots[column] = ability
        }
    }

    val letters = catalog.abilities
        .filter { it.hotkey in listOf("Q", "W", "E", "R", "D") }
        .sortedBy { it.parentLinks.isNotEmpty() }
        // The same unchanged button can be exported for several controlled units.
        .distinctBy { it.hotkey to it.iconPath.substringAfterLast('/') }
    if (letters.isEmpty()) {
        return if (numberSlots.any { it != null }) listOf(HeroAbilityRow(numberSlots)) else emptyList()
    }
    val mainUnit = letters.first().unitId

    fun formGroup(ability: HeroAbility): String {
        if (catalog.heroId == "deathwing") {
            // HDP links ground E spells to the landing command, not to their ground stance.
            if (ability.id in setOf("onslaught", "earth_shatter")) return "base"
            if (ability.id in setOf("destroyer", "world_breaker", "skyfall")) return "flight"
        }
        if (ability.parentLinks.isEmpty()) {
            // The Vikings controller owns R/D; Q/W/E are exported on each Viking.
            return if (ability.unitId == mainUnit || catalog.heroId == "the-lost-vikings") {
                "base"
            } else {
                "unit:${ability.unitId}"
            }
        }
        // Kharazim's three level-one traits replace the missing generic trait icon.
        if (catalog.heroId == "kharazim" && ability.hotkey == "D" &&
            ability.parentLinks.any { it.endsWith("|Level1") }
        ) return "base"
        return "${ability.unitId}:${ability.parentLinks.sorted().joinToString()}"
    }

    val groups = letters.groupBy(::formGroup)
    val base = groups["base"].orEmpty()
    val heroicIconOrder = catalog.heroicTalentIcons.values.distinct()
    val baseHeroics = base.filter { it.hotkey == "R" }.sortedBy { ability ->
        heroicIconOrder.indexOf(ability.iconPath).takeIf { it >= 0 } ?: Int.MAX_VALUE
    }
    val byLink = catalog.abilities.groupBy { it.linkId }

    fun heroicAncestor(link: String, visited: MutableSet<String>): Int? {
        if (!visited.add(link)) return null
        val direct = baseHeroics.indexOfFirst {
            it.linkId == link || it.iconPath == catalog.heroicTalentIcons[link]
        }
        if (direct >= 0) return direct
        return byLink[link].orEmpty().firstNotNullOfOrNull { ability ->
            ability.parentLinks.firstNotNullOfOrNull { heroicAncestor(it, visited) }
        }
    }

    fun column(ability: HeroAbility, group: List<HeroAbility>): Int = when (ability.hotkey) {
        "Q" -> 0
        "W" -> 1
        "E" -> 2
        "D" -> 5
        else -> {
            val baseIndex = baseHeroics.indexOf(ability).takeIf { it >= 0 }
            val parentIndex = ability.parentLinks.firstNotNullOfOrNull {
                heroicAncestor(it, mutableSetOf())
            }
            val localIndex = group.filter { it.hotkey == "R" }.indexOf(ability)
            3 + (baseIndex ?: parentIndex ?: localIndex.coerceAtLeast(0)) % 2
        }
    }

    fun stackGroup(group: List<HeroAbility>): List<MutableList<HeroAbility?>> {
        val rows = mutableListOf<MutableList<HeroAbility?>>()
        group.forEach { ability ->
            val column = column(ability, group)
            val row = rows.firstOrNull { it[column] == null }
                ?: MutableList<HeroAbility?>(6) { null }.also(rows::add)
            row[column] = ability
        }
        return rows
    }

    val baseRows = stackGroup(base)
    val variants = baseRows.drop(1).toMutableList()
    val alternateGroups = groups.filterKeys { it != "base" }.values.map(::stackGroup)
    val orderedGroups = if (catalog.heroId == "chen") {
        // Keep Breath of Fire before the heroic form, following the catalog order.
        alternateGroups
    } else {
        // Keep a form's Q/W/E together before fitting independent follow-up commands.
        alternateGroups.sortedByDescending { rows -> rows.first().count { it != null } }
    }
    orderedGroups.forEach { groupRows ->
        val firstRow = (0..variants.size).first { start ->
            groupRows.withIndex().all { (offset, row) ->
                val existing = variants.getOrNull(start + offset)
                existing == null || row.indices.all { row[it] == null || existing[it] == null }
            }
        }
        while (variants.size < firstRow + groupRows.size) {
            variants.add(MutableList(6) { null })
        }
        groupRows.forEachIndexed { offset, row ->
            row.forEachIndexed { column, ability ->
                if (ability != null) variants[firstRow + offset][column] = ability
            }
        }
    }

    return buildList {
        if (numberSlots.any { it != null }) add(HeroAbilityRow(numberSlots))
        baseRows.firstOrNull()?.let { add(HeroAbilityRow(it)) }
        variants.forEach { add(HeroAbilityRow(it)) }
    }
}
