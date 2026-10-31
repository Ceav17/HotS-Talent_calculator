package com.github.ceav17.hots_talent_calculator

private val GameBuildHeroNames = mapOf(
    "li-ming" to "LiMing",
    "li-li" to "LiLi",
    "guldan" to "Guldan",
    "e-t-c" to "ETC",
    "dva" to "DVa",
    "zuljin" to "Zuljin",
    "the-butcher" to "TheButcher",
    "anubarak" to "Anubarak",
    "kel-thuzad" to "KelThuzad",
    "kaelthas" to "Kaelthas",
    "the-lost-vikings" to "LostVikings",
    "lt-morales" to "LtMorales",
    "lucio" to "Lucio",
    "malganis" to "MalGanis",
    "xalatath" to "Xalatath",
    "sgt-hammer" to "SgtHammer",
    "deckard-cain" to "Deckard"
)

private val BuildCodePattern = Regex(
    "\\[T([0-5]{7}),(?:([0-9A-V]{7}),)?([A-Za-z]+)\\]",
    RegexOption.IGNORE_CASE
)

internal fun heroBuildCodeName(heroId: String): String =
    GameBuildHeroNames[heroId] ?: heroId.replaceFirstChar { it.uppercaseChar() }

internal enum class HeroBuildCodeFormat { Game, App }

internal fun formatHeroBuildCode(
    heroId: String,
    tiers: List<HeroTalentTier>,
    selections: Map<Int, HeroBuildTalentSelection>,
    format: HeroBuildCodeFormat = HeroBuildCodeFormat.Game
): String {
    val preferred = StringBuilder()
    val alternatives = StringBuilder()
    tiers.sortedBy { it.level }.forEach { tier ->
        val selection = selections[tier.level]
        val position = tier.talents.indexOfFirst { it.id == selection?.preferred } + 1
        val choice = position.takeIf { it in 1..5 } ?: 0
        preferred.append(choice)
        if (format == HeroBuildCodeFormat.App) {
            var mask = 0
            // Removed talents must not create an invalid code or alternatives without a preferred talent.
            if (choice != 0 && selection != null) {
                tier.talents.forEachIndexed { index, talent ->
                    if (index < 5 && index != choice - 1 && talent.id in selection.alternatives) {
                        mask = mask or (1 shl index)
                    }
                }
            }
            alternatives.append(mask.digitToChar(32).uppercaseChar())
        }
    }
    val alternativeField = if (format == HeroBuildCodeFormat.App) "$alternatives," else ""
    return "[T$preferred,$alternativeField${heroBuildCodeName(heroId)}]"
}

internal fun parseHeroBuildCode(
    text: String,
    catalog: HeroAbilityCatalog
): Map<Int, HeroBuildTalentSelection>? {
    val match = BuildCodePattern.matchEntire(text.trim()) ?: return null
    if (!match.groupValues[3].equals(heroBuildCodeName(catalog.heroId), ignoreCase = true)) return null
    val alternativesCode = match.groupValues[2]
    val tiers = catalog.talentTiers.sortedBy { it.level }
    if (tiers.size != 7 || tiers.map { it.level }.distinct().size != 7) return null
    val result = mutableMapOf<Int, HeroBuildTalentSelection>()
    tiers.forEachIndexed { index, tier ->
        val choice = match.groupValues[1][index].digitToInt()
        // Each optional base-32 digit stores positions 1..5 as bits 0..4.
        val alternativeMask = alternativesCode.getOrNull(index)?.digitToInt(32) ?: 0
        if (choice == 0 && alternativeMask != 0) return null
        if (choice != 0) {
            // Use the displayed order, not exported positions or the talent's ID.
            val talent = tier.talents.getOrNull(choice - 1) ?: return null
            if (alternativeMask and (1 shl (choice - 1)) != 0) return null
            val alternatives = (0..4)
                .filter { position -> alternativeMask and (1 shl position) != 0 }
                .map { position -> tier.talents.getOrNull(position)?.id ?: return null }
                .toSet()
            result[tier.level] = HeroBuildTalentSelection(talent.id, alternatives)
        }
    }
    // An empty map is a valid reset. Null means no changes may be saved.
    return result
}
