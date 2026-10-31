package com.github.ceav17.hots_talent_calculator

import android.content.res.AssetManager
import android.util.LruCache
import org.json.JSONArray
import org.json.JSONObject
import java.util.Locale

private val heroCatalogCache = LruCache<Pair<AssetManager, String>, HeroAbilityCatalog>(6)

internal fun loadHeroAbilityCatalog(assets: AssetManager, heroId: String): HeroAbilityCatalog =
    synchronized(heroCatalogCache) {
        val key = assets to heroId
        heroCatalogCache.get(key) ?: readHeroAbilityCatalog(assets, heroId).also { heroCatalogCache.put(key, it) }
    }

private fun readHeroAbilityCatalog(assets: AssetManager, heroId: String): HeroAbilityCatalog {
    val document = assets.open("data/Heroes/$heroId.json").bufferedReader().use {
        JSONObject(it.readText())
    }
    val abilities = document.getJSONArray("abilities").objects().mapNotNull { ability ->
        if (ability.isNull("icon") || ability.isNull("hotkey")) return@mapNotNull null
        HeroAbility(
            id = ability.getString("id"),
            name = ability.getString("name"),
            iconPath = ability.getString("icon"),
            hotkey = ability.getString("hotkey").uppercase(Locale.ROOT),
            unitId = ability.getString("unit_id"),
            linkId = ability.getString("link_id"),
            parentLinks = ability.getJSONArray("parent_link_ids").let { links ->
                List(links.length()) { links.getString(it) }
            },
            activeOrigin = ability.optionalString("active_origin"),
            hotkeySource = ability.optionalString("hotkey_source"),
            availableWithoutTalents = ability.getJSONArray("unlock_conditions").let { conditions ->
                (0 until conditions.length()).any { conditions.getJSONArray(it).length() == 0 }
            },
            abilityId = ability.optionalString("ability_id"),
            description = ability.getString("description")
        )
    }
    val tiers = document.getJSONArray("talent_tiers").objects().sortedBy { it.getInt("level") }
    val heroicTalentIcons = tiers
        .flatMap { tier -> tier.getJSONArray("talents").objects().sortedBy { it.getInt("position") } }
        .filter { it.optString("hotkey") == "R" && !it.isNull("icon") }
        .associate { it.getString("link_id") to it.getString("icon") }
    return HeroAbilityCatalog(
        heroId, abilities, heroicTalentIcons,
        hasHearthstone = document.getJSONObject("hero").getBoolean("has_hearthstone"),
        talentTiers = tiers.map { tier ->
            val level = tier.getInt("level")
            HeroTalentTier(
                // HDP exports the standard tier levels even for Chromie.
                level = if (heroId == "chromie" && level > 1) level - 2 else level,
                talents = tier.getJSONArray("talents").objects()
                    .sortedBy { it.getInt("position") }
                    .map { talent ->
                        HeroTalent(
                            id = talent.getString("id"),
                            name = talent.getString("name"),
                            description = talent.getString("description"),
                            iconPath = talent.getString("icon")
                        )
                    }
            )
        }
    )
}

private fun JSONArray.objects(): List<JSONObject> = List(length()) { getJSONObject(it) }

private fun JSONObject.optionalString(key: String): String? = if (isNull(key)) null else getString(key)
