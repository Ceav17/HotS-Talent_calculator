package com.github.ceav17.hots_talent_calculator

import android.content.res.AssetManager
import android.util.JsonReader

internal data class HeroSearchEntry(
    val id: String,
    val name: String,
    val teamRole: String? = null,
    val universe: String? = null
) {
    // Search aliases leave the original ID intact for linking portraits and JSON data.
    private val searchTerms = buildList {
        add(
            when (id) {
                "deckard-cain", "kel-thuzad", "lt-morales", "sgt-hammer",
                "the-butcher", "the-lost-vikings" -> id.replace('-', ' ')
                "e-t-c" -> id.replace("-", "")
                else -> id
            }
        )
        add(name)
        if (id == "the-lost-vikings") {
            addAll(listOf("TLV", "Olaf", "Erik", "Baleog"))
        }
    }

    fun matches(
        query: String,
        selectedRoles: Set<String> = emptySet(),
        selectedUniverses: Set<String> = emptySet(),
        favoriteIds: Set<String>? = null,
        customGroupIds: Set<String>? = null
    ): Boolean =
        // Choices within a group use OR; groups and the search query use AND.
        (selectedRoles.isEmpty() || teamRole in selectedRoles) &&
            (selectedUniverses.isEmpty() || universe in selectedUniverses) &&
            // Null disables this filter; an empty favorites set must match no heroes.
            (favoriteIds == null || id in favoriteIds) &&
            (customGroupIds == null || id in customGroupIds) &&
            searchTerms.any { it.contains(query, ignoreCase = true) }
}

internal fun loadHeroSearchEntries(assets: AssetManager): Map<String, HeroSearchEntry> = buildMap {
    for (fileName in assets.list("data/Heroes").orEmpty()) {
        if (fileName.endsWith(".json", ignoreCase = true)) {
            val hero = readHeroSearchEntry(assets, fileName)
            put(hero.id, hero)
        }
    }
}

private fun readHeroSearchEntry(assets: AssetManager, fileName: String): HeroSearchEntry {
    JsonReader(assets.open("data/Heroes/$fileName").bufferedReader()).use { reader ->
        reader.beginObject()
        while (reader.hasNext()) {
            if (reader.nextName() != "hero") {
                reader.skipValue()
                continue
            }
            var id: String? = null
            var name: String? = null
            var role: String? = null
            var universe: String? = null
            reader.beginObject()
            while (reader.hasNext()) {
                when (reader.nextName()) {
                    "id" -> id = reader.nextString()
                    "name" -> name = reader.nextString()
                    "team_role" -> role = reader.nextString()
                    "universe" -> universe = reader.nextString()
                    else -> reader.skipValue()
                }
            }
            reader.endObject()
            // Search needs only the header, not all ability/talent descriptions for 91 heroes.
            return HeroSearchEntry(requireNotNull(id), requireNotNull(name), requireNotNull(role), requireNotNull(universe))
        }
    }
    error("Missing hero metadata: $fileName")
}
