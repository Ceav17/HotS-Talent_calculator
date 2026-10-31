package com.github.ceav17.hots_talent_calculator

import android.annotation.SuppressLint
import android.content.Context
import android.icu.lang.UCharacter
import android.icu.lang.UProperty
import android.icu.text.BreakIterator
import java.io.IOException
import java.util.Locale
import java.util.UUID
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject

internal const val MaxHeroBuilds = 10
internal const val MaxBuildNameCharacters = 32

internal class DuplicateHeroBuildNameException : IllegalArgumentException("Build name already exists for this hero")

internal data class HeroBuildTalentSelection(
    val preferred: String,
    val alternatives: Set<String> = emptySet()
)

internal data class HeroBuild(
    val id: String,
    val name: String,
    val talents: Map<Int, HeroBuildTalentSelection> = emptyMap()
) {
    fun toggleTalent(level: Int, talentId: String): HeroBuild {
        require(level > 0 && talentId.isNotBlank())
        val selection = talents[level]
        val updated = when {
            selection == null -> talents + (level to HeroBuildTalentSelection(talentId))
            selection.preferred == talentId -> talents - level
            talentId in selection.alternatives ->
                talents + (level to selection.copy(alternatives = selection.alternatives - talentId))
            else -> talents + (level to selection.copy(alternatives = selection.alternatives + talentId))
        }
        return copy(talents = updated)
    }
}

internal fun buildNameCharacterCount(name: String): Int {
    val characters = BreakIterator.getCharacterInstance(Locale.ROOT)
    characters.setText(name)
    var count = 0
    while (characters.next() != BreakIterator.DONE) count++
    return count
}

internal fun isValidBuildName(name: String): Boolean {
    if (name.isEmpty() || name.length > 2048 || buildNameCharacterCount(name) > MaxBuildNameCharacters) return false
    var visible = false
    var offset = 0
    while (offset < name.length) {
        val point = name.codePointAt(offset)
        val type = Character.getType(point)
        if (Character.isISOControl(point) || type == Character.SURROGATE.toInt() ||
            type == Character.LINE_SEPARATOR.toInt() || type == Character.PARAGRAPH_SEPARATOR.toInt()
        ) return false
        // Preserve emoji/joined scripts, but exclude invisible direction-control sequences.
        if (type == Character.FORMAT.toInt() && point != 0x200C && point != 0x200D &&
            point !in 0xE0020..0xE007F
        ) return false
        if (!Character.isWhitespace(point) && !Character.isSpaceChar(point) && point != 0x2800 &&
            !UCharacter.hasBinaryProperty(point, UProperty.DEFAULT_IGNORABLE_CODE_POINT) &&
            type != Character.NON_SPACING_MARK.toInt() && type != Character.COMBINING_SPACING_MARK.toInt() &&
            type != Character.ENCLOSING_MARK.toInt()
        ) visible = true
        offset += Character.charCount(point)
    }
    return visible
}

internal class HeroBuildsStore(context: Context, preferenceName: String = "hero_builds") {
    private val appContext = context.applicationContext
    private val preferences by lazy { appContext.getSharedPreferences(preferenceName, Context.MODE_PRIVATE) }


    suspend fun load(heroId: String): List<HeroBuild> = UserDataAccess.withLock(appContext) {
        withContext(Dispatchers.IO) { readBuilds(heroId) }
    }

    internal fun readAllBuilds(): Map<String, List<HeroBuild>> =
        preferences.all.keys.sorted().associateWith(::readBuilds)

    private fun readBuilds(heroId: String): List<HeroBuild> {
        try {
            val data = JSONArray(preferences.getString(heroId, "[]"))
            val builds = List(data.length()) { index ->
                val item = data.getJSONObject(index)
                HeroBuild(item.getString("id"), item.getString("name"), readTalents(item))
            }
            if (builds.size > MaxHeroBuilds || builds.any { it.id.isBlank() || !isValidBuildName(it.name) } ||
                builds.map { it.id }.distinct().size != builds.size
            ) throw IOException("Invalid saved builds for $heroId")
            return builds
        } catch (error: JSONException) {
            // Do not replace unreadable saved data with an empty list.
            throw IOException("Could not read builds for $heroId", error)
        }
    }

    private fun readTalents(item: JSONObject): Map<Int, HeroBuildTalentSelection> {
        // Builds created before talent selection have only an ID and a name.
        if (!item.has("talents")) return emptyMap()
        val data = item.getJSONObject("talents")
        return buildMap {
            data.keys().forEach { key ->
                val level = key.toIntOrNull()?.takeIf { it > 0 }
                    ?: throw IOException("Invalid saved talent level")
                val row = data.getJSONObject(key)
                val preferred = row.getString("preferred")
                val alternativesData = row.getJSONArray("alternatives")
                val alternatives = List(alternativesData.length()) { alternativesData.getString(it) }
                if (containsKey(level) || preferred.isBlank() || alternatives.any { it.isBlank() } ||
                    preferred in alternatives || alternatives.distinct().size != alternatives.size
                ) throw IOException("Invalid saved talent selection")
                put(level, HeroBuildTalentSelection(preferred, alternatives.toSet()))
            }
        }
    }

    suspend fun create(heroId: String, name: String): List<HeroBuild> = UserDataAccess.withLock(appContext) {
        val trimmedName = name.trim()
        require(isValidBuildName(trimmedName))
        withContext(Dispatchers.IO) {
            val current = readBuilds(heroId)
            // Check under the write lock, so two simultaneous creates cannot use the same name.
            if (current.any { it.name.trim().equals(trimmedName, ignoreCase = true) }) {
                throw DuplicateHeroBuildNameException()
            }
            check(current.size < MaxHeroBuilds) { "Maximum number of hero builds reached" }
            // Names are JSON values, never paths or identifiers; punctuation and emoji are safe.
            val updated = current + HeroBuild(UUID.randomUUID().toString(), trimmedName)
            persist(heroId, updated)
            updated
        }
    }

    suspend fun rename(heroId: String, buildId: String, name: String): List<HeroBuild> = UserDataAccess.withLock(appContext) {
        val trimmedName = name.trim()
        require(isValidBuildName(trimmedName))
        withContext(Dispatchers.IO) {
            val current = readBuilds(heroId)
            val index = current.indexOfFirst { it.id == buildId }
            if (index < 0) throw IOException("Build not found: $buildId")
            if (current.any { it.id != buildId && it.name.trim().equals(trimmedName, ignoreCase = true) }) {
                throw DuplicateHeroBuildNameException()
            }
            val build = current[index]
            // Read the latest selections under the same lock used by talent edits.
            val updated = current.toMutableList().apply { this[index] = build.copy(name = trimmedName) }
            if (build.name != trimmedName) persist(heroId, updated)
            updated
        }
    }

    suspend fun delete(heroId: String, buildId: String): List<HeroBuild> = UserDataAccess.withLock(appContext) {
        withContext(Dispatchers.IO) {
            val current = readBuilds(heroId)
            val updated = current.filterNot { it.id == buildId }
            if (updated.size != current.size) persist(heroId, updated)
            updated
        }
    }

    suspend fun toggleTalent(heroId: String, buildId: String, level: Int, talentId: String): HeroBuild =
        UserDataAccess.withLock(appContext) {
            withContext(Dispatchers.IO) {
                val current = readBuilds(heroId)
                val index = current.indexOfFirst { it.id == buildId }
                if (index < 0) throw IOException("Build not found: $buildId")
                val updated = current[index].toggleTalent(level, talentId)
                persist(heroId, current.toMutableList().apply { this[index] = updated })
                updated
            }
        }

    suspend fun replaceTalents(
        heroId: String,
        buildId: String,
        talents: Map<Int, HeroBuildTalentSelection>
    ): HeroBuild = UserDataAccess.withLock(appContext) {
        withContext(Dispatchers.IO) {
            val current = readBuilds(heroId)
            val index = current.indexOfFirst { it.id == buildId }
            if (index < 0) throw IOException("Build not found: $buildId")
            // Replace all seven tiers in one commit, preserving this build's name and ID.
            val updated = current[index].copy(talents = talents.toMap())
            if (updated != current[index]) {
                persist(heroId, current.toMutableList().apply { this[index] = updated })
            }
            updated
        }
    }

    @SuppressLint("UseKtx", "ApplySharedPref") // Writes and rollback run synchronously on IO before updating the UI.
    private fun persist(heroId: String, builds: List<HeroBuild>) {
        val previous = preferences.getString(heroId, null)
        val data = heroBuildsJson(builds)
        if (!preferences.edit().putString(heroId, data.toString()).commit()) {
            // Failed commits still change memory; restore it so a retry performs a real write.
            preferences.edit().putString(heroId, previous).commit()
            throw IOException("Could not persist builds for $heroId")
        }
    }
}
