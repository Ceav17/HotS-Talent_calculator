package com.github.ceav17.hots_talent_calculator

import androidx.compose.foundation.text.appendInlineContent
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString

internal object HeroTextColors {
    val Name = Color(0xFFD8DCF6)
    val Value = Color(0xFFD0D4EF)
    val Body = Color(0xFF8781C0)
    val Caution = Color(0xFFECEA91)
    val Quest = Color(0xFFE4C537)
    val Passive = Color(0xFF00CA8F)
}

internal const val HeroQuestInlineId = "hero_quest_icon"

private const val NumberPattern = "[+−-]?(?:\\d+(?:[.,]\\d+)*|\\.\\d+)%?"
private val descriptionNumbers = Regex(NumberPattern)
private val descriptionCosts = Regex(
    "\\b(?:Mana|Energy|Brew|Health|Life|Fury|Scrap):\\s*$NumberPattern(?: per second)?"
)
private val descriptionCooldowns = Regex(
    "\\b(?:Charge )?Cooldown:\\s*$NumberPattern seconds?(?: per ally)?"
)
private val questHeaders = Regex(
    "\\b(?:(?:Repeatable |Mythic )?(?:Quest|Reward)|Gambit)(?: \\([^)]*\\))?:"
)
private val restrictionSentences = Regex(
    """\b(?:Cannot be used on|Does not affect)\b.*?(?:\.(?=\s|$)|$)"""
)
private val dvaChargeSentence = Regex(
    "Gain $NumberPattern Charge for every $NumberPattern seconds spent Basic Attacking, " +
        "and $NumberPattern Charge per $NumberPattern of Mecha? Health lost\\."
)

/** Add display-only formatting; exported descriptions remain the source of truth. */
internal fun heroDescriptionText(heroId: String, entryId: String, description: String): AnnotatedString {
    val lineBreaks = mutableMapOf<Int, Int>()
    fun requestLineBreak(position: Int, count: Int = 1) {
        var next = position
        while (next < description.length && description[next].isWhitespace()) next++
        if (next < description.length) lineBreaks[next] = maxOf(lineBreaks[next] ?: 0, count)
    }

    val text = buildAnnotatedString {
        append(description)
        if (description.isNotEmpty()) addStyle(SpanStyle(color = HeroTextColors.Body), 0, length)

        fun highlight(
            pattern: Regex,
            color: Color,
            newLineBefore: Boolean = false,
            newLineAfter: Boolean = false
        ) {
            pattern.findAll(description).forEach { match ->
                addStyle(SpanStyle(color = color), match.range.first, match.range.last + 1)
                if (newLineBefore) requestLineBreak(match.range.first)
                if (newLineAfter) requestLineBreak(match.range.last + 1)
            }
        }

        fun phrases(
            color: Color,
            vararg phrases: String,
            newLineBefore: Boolean = false,
            newLineAfter: Boolean = false
        ) {
            // Match complete names/labels, accepting either apostrophe exported by HDP.
            val alternatives = phrases.sortedByDescending { it.length }.joinToString("|") { phrase ->
                phrase.split('\'').joinToString("['’]") { Regex.escape(it) }
            }
            highlight(
                Regex("(?<![\\p{L}\\p{N}_])(?:$alternatives)(?![\\p{L}\\p{N}_])"),
                color, newLineBefore = newLineBefore, newLineAfter = newLineAfter
            )
        }

        // This sentence is orange except for its numeric values.
        if (heroId == "dva" && entryId.startsWith("self_destruct")) {
            highlight(dvaChargeSentence, Color(0xFFC06613))
        }
        highlight(descriptionNumbers, HeroTextColors.Value)
        highlight(descriptionCosts, HeroTextColors.Value, newLineAfter = true)
        highlight(descriptionCooldowns, HeroTextColors.Value, newLineAfter = true)
        highlight(restrictionSentences, HeroTextColors.Caution, newLineBefore = true)

        // Specific phrases intentionally override numeric highlighting inside them.
        when (heroId) {
            "alexstrasza" -> phrases(Color(0xFFC47FE0), "Dragonqueen:")
            "chen" -> {
                if (entryId == "keg_smash") {
                    // The first occurrence is a reference; only the second introduces the follow-up spell.
                    Regex("\\bBreath of Fire\\b").findAll(description).drop(1).firstOrNull()?.let { label ->
                        addStyle(SpanStyle(color = Color(0xFFCF1B00)), label.range.first, label.range.last + 1)
                        requestLineBreak(label.range.first)
                        requestLineBreak(label.range.last + 1)
                    }
                }
                if (entryId == "storm_earth_fire") {
                    phrases(Color(0xFF00D5DC), "Storm", newLineBefore = true)
                    phrases(Color(0xFF00DC18), "Earth", newLineBefore = true)
                    phrases(Color(0xFFE67012), "Fire", newLineBefore = true)
                }
            }
            "deathwing" -> {
                phrases(Color(0xFFAC5245), "Destroyer:", "Destroyer", "Incinerate", "Onslaught")
                phrases(
                    Color(0xFFA47C78), "World Breaker:", "World Breaker's",
                    "Lava Burst's", "Lava Burst", "Earth Shatter's"
                )
                if (entryId in setOf("incinerate", "onslaught")) {
                    Regex("\\bWorld Breaker:").findAll(description).forEach { requestLineBreak(it.range.first) }
                }
            }
            "dva" -> phrases(HeroTextColors.Passive, "Pilot Mode:")
            "fenix" -> phrases(Color(0xFF08CB87), "Weapon Mode: Repeater Cannon", "Weapon Mode: Phase Bomb")
            "greymane" -> {
                phrases(Color(0xFFA867BC), "Worgen")
                phrases(Color(0xFFECE7A4), "Human", "Human: Darkflight (E)", "Human: Darkflight")
                when (entryId) {
                    "gilnean_cocktail" -> phrases(
                        Color(0xFFA867BC), "Worgen: Razor Swipe",
                        newLineBefore = true, newLineAfter = true
                    )
                    "darkflight" -> phrases(
                        Color(0xFFA867BC), "Worgen: Disengage Roll",
                        newLineBefore = true, newLineAfter = true
                    )
                    "razor_swipe" -> phrases(
                        Color(0xFFECE7A4), "Human: Gilnean Cocktail",
                        newLineBefore = true, newLineAfter = true
                    )
                    "disengage" -> phrases(
                        Color(0xFFECE7A4), "Human: Darkflight (E)", "Human: Darkflight",
                        newLineBefore = true, newLineAfter = true
                    )
                }
            }
            "the-lost-vikings" -> phrases(
                HeroTextColors.Caution,
                "Requires all surviving Vikings to be nearby.",
                "Only one Viking may attempt to summon at a time.",
                newLineBefore = true
            )
            "lucio" -> {
                phrases(Color(0xFFD6CF14), "Healing Boost")
                phrases(Color(0xFF63E19F), "Speed Boost")
            }
            "malthael" -> phrases(Color(0xFF29B8C4), "Reaper's Mark")
            "medivh" -> phrases(
                HeroTextColors.Caution,
                "Activate Medivh's Trait to cancel an unlinked Portal.",
                "Active Medivh's Trait to cancel an unlinked Portal.",
                newLineBefore = true
            )
            "sgt-hammer" -> phrases(Color(0xFFEEEE97), "Siege Mode:")
            "tassadar" -> phrases(HeroTextColors.Passive, "Resonance Beam:", newLineBefore = true)
            "valeera" -> {
                highlight(Regex("\\bAwards \\d+ Combo Points?\\b"), Color(0xFFDA555E), newLineBefore = true)
                if (entryId == "eviscerate") {
                    phrases(Color(0xFFDA555E), "Combo Point", "Combo Points")
                    highlight(Regex("\\b\\d+ Points?\\b"), Color(0xFFDA555E), newLineBefore = true)
                    highlight(
                        Regex("\\bConsumes all Combo Points\\b"), HeroTextColors.Value,
                        newLineAfter = true
                    )
                }
                if (entryId in setOf("sinister_strike", "blade_flurry", "eviscerate", "ambush", "cheap_shot", "garrote")) {
                    highlight(
                        Regex("\\b(?:Unstealth|Stealth): (?:Sinister Strike|Blade Flurry|Eviscerate|Ambush|Cheap Shot|Garrote)\\b"),
                        Color(0xFFCF8BF1), newLineBefore = true
                    )
                }
            }
            "whitemane" -> {
                phrases(Color(0xFFEFA4AF), "Desperation")
                phrases(Color(0xFFF7F6D0), "Zeal")
                phrases(HeroTextColors.Caution, "Current Mana Cost:", newLineBefore = true)
            }
            "xalatath" -> {
                val bonuses = when (entryId) {
                    "void_volley" -> arrayOf("+2", "-1", "+50%")
                    "anchored_core" -> arrayOf("+50%", "+25%")
                    "cold_feet", "toxic_mark" -> arrayOf("+1s")
                    "fractured_reality" -> arrayOf("+25%")
                    "dueling_orbs" -> arrayOf("+1")
                    else -> emptyArray()
                }
                if (bonuses.isNotEmpty()) phrases(Color(0xFF00D58E), *bonuses)

                // Include HDP's scaling values and singular "Void Minion loses" variant.
                val notes = listOf(
                    "Stack loss will never cause the Hero to fall below a quest reward threshold\\.",
                    "Contact damage is dealt every $NumberPattern seconds\\.",
                    "The Portal expires after $NumberPattern seconds\\.",
                    "Void Minions automatically choose a nearby lane to push\\.",
                    "Void Minion Basic Attacks deal $NumberPattern(?: \\([^)]*\\))? damage per hit\\.",
                    "Void Minions? lose(?:s)? Health over $NumberPattern seconds\\.",
                    "Shield starts decaying after $NumberPattern seconds?, decays over $NumberPattern seconds\\.",
                    "Damage is dealt every $NumberPattern seconds\\.",
                    "Damage increases up to $NumberPattern\\.?",
                    "Damage increase from multiple fires stack\\.",
                    "Shadow Mark has a $NumberPattern second cooldown between uses\\.",
                    "The flames deal damage every $NumberPattern second\\.",
                    "Bonus Basic Attack damage stacks up to $NumberPattern times\\.?"
                )
                highlight(Regex(notes.joinToString("|")), Color(0xFFB7B4C4), newLineBefore = true)
            }
            "zagara" -> phrases(HeroTextColors.Caution, "Usable on Unstoppable enemies.", newLineBefore = true)
        }
        phrases(HeroTextColors.Passive, "Passive:")
        highlight(questHeaders, HeroTextColors.Quest)
    }

    val questPositions = questHeaders.findAll(description).map { it.range.first }.toSet()
    questPositions.forEach { requestLineBreak(it, count = 2) }

    // Format after coloring: source ranges stay valid, and coinciding breaks do not add extra blank lines.
    return buildAnnotatedString {
        var start = 0
        lineBreaks.toSortedMap().forEach { (position, count) ->
            var end = position
            while (end > start && description[end - 1].isWhitespace()) end--
            append(text, start, end)
            val existingBreaks = description.substring(end, position).count { it == '\n' }
            if (length > 0 || count == 2) append("\n".repeat(maxOf(count, existingBreaks)))
            if (position in questPositions) {
                appendInlineContent(HeroQuestInlineId, "\uFFFC")
                append('\u00a0')
            }
            start = position
        }
        append(text, start, text.length)
    }
}
