package com.github.ceav17.hots_talent_calculator

import java.io.ByteArrayOutputStream
import java.io.IOException
import java.io.InputStream
import java.nio.ByteBuffer
import java.nio.charset.CodingErrorAction
import org.json.JSONArray
import org.json.JSONObject
import org.json.JSONTokener

// Allow full catalogs with maximum-length Unicode names, including JSON escaping.
internal const val MaxBackupBytes = 16 * 1024 * 1024
private const val BackupFormat = "hots-talent-calculator-data"

internal class InvalidBackupException(cause: Throwable? = null) : IOException("Invalid backup", cause)
internal class UnsupportedBackupVersionException : IOException("Unsupported backup version")
internal class BackupTooLargeException : IOException("Backup is too large")

internal data class UserDataBackup(
    val favoriteIds: Set<String>,
    val customGroups: HeroCustomGroups,
    val builds: Map<String, List<HeroBuild>>
) {
    val buildCount: Int get() = builds.values.sumOf { it.size }
}

internal fun readBackupBytes(input: InputStream, limit: Int = MaxBackupBytes): ByteArray {
    val output = ByteArrayOutputStream()
    val buffer = ByteArray(8192)
    while (true) {
        val count = input.read(buffer)
        if (count < 0) break
        if (count > limit - output.size()) throw BackupTooLargeException()
        output.write(buffer, 0, count)
    }
    return output.toByteArray()
}

internal fun heroBuildsJson(builds: List<HeroBuild>): JSONArray = JSONArray().apply {
    builds.forEach { build ->
        val talents = JSONObject()
        build.talents.toSortedMap().forEach { (level, selection) ->
            talents.put(level.toString(), JSONObject().put("preferred", selection.preferred)
                .put("alternatives", JSONArray(selection.alternatives.sorted())))
        }
        put(JSONObject().put("id", build.id).put("name", build.name).put("talents", talents))
    }
}

internal fun encodeUserDataBackup(data: UserDataBackup): ByteArray {
    val builds = JSONObject()
    data.builds.toSortedMap().forEach { (hero, entries) -> builds.put(hero, heroBuildsJson(entries)) }
    val groups = JSONArray()
    data.customGroups.groups.forEach { group ->
        groups.put(JSONObject().put("label", group.label).put("heroes", JSONArray(group.heroIds.sorted())))
    }
    return JSONObject().put("format", BackupFormat).put("version", 1)
        .put("favorites", JSONArray(data.favoriteIds.sorted()))
        .put("personalFilters", JSONObject().put("visibleCount", data.customGroups.visibleCount).put("groups", groups))
        .put("builds", builds).toString().toByteArray(Charsets.UTF_8).also {
            if (it.size > MaxBackupBytes) throw BackupTooLargeException()
        }
}

internal fun decodeUserDataBackup(bytes: ByteArray): UserDataBackup {
    if (bytes.size > MaxBackupBytes) throw BackupTooLargeException()
    try {
        val text = Charsets.UTF_8.newDecoder().onMalformedInput(CodingErrorAction.REPORT)
            .onUnmappableCharacter(CodingErrorAction.REPORT).decode(ByteBuffer.wrap(bytes)).toString()
        // Bound nesting before JSONObject's recursive parser sees untrusted input.
        var depth = 0
        var quoted = false
        var escaped = false
        text.forEach { char ->
            when {
                escaped -> escaped = false
                quoted && char == '\\' -> escaped = true
                char == '"' -> quoted = !quoted
                !quoted && (char == '{' || char == '[') -> { depth++; require(depth <= 16) }
                !quoted && (char == '}' || char == ']') -> { depth--; require(depth >= 0) }
            }
        }
        require(depth == 0 && !quoted)
        val parser = JSONTokener(text.removePrefix("\uFEFF"))
        val root = parser.nextValue() as? JSONObject ?: throw InvalidBackupException()
        require(parser.nextClean() == '\u0000')
        require(root.get("format") == BackupFormat)
        if (root.get("version") != 1) throw UnsupportedBackupVersionException()
        val filters = root.getJSONObject("personalFilters")
        val visible = filters.get("visibleCount")
        require(visible is Int && visible in 0..MaxCustomGroups)
        val groups = filters.getJSONArray("groups")
        require(groups.length() == MaxCustomGroups)
        val personal = HeroCustomGroups(List(groups.length()) { index ->
            val group = groups.getJSONObject(index)
            HeroCustomGroup(backupString(group.get("label"), 64), backupIds(group.getJSONArray("heroes"), 512))
        }, visible)
        val allBuilds = root.getJSONObject("builds")
        require(allBuilds.length() <= 512)
        val builds = buildMap {
            allBuilds.keys().forEach { hero ->
                backupString(hero, 128)
                val array = allBuilds.getJSONArray(hero)
                require(array.length() <= MaxHeroBuilds)
                val entries = List(array.length()) { index ->
                    val item = array.getJSONObject(index)
                    val talents = item.getJSONObject("talents")
                    require(talents.length() <= 7)
                    val levels = if (hero == "chromie") setOf(1, 2, 5, 8, 11, 14, 18)
                        else setOf(1, 4, 7, 10, 13, 16, 20)
                    val selections = buildMap {
                        talents.keys().forEach { key ->
                            val level = key.toIntOrNull()
                            require(level in levels && key == level.toString())
                            val selection = talents.getJSONObject(key)
                            val preferred = backupString(selection.get("preferred"), 256)
                            val alternatives = backupIds(selection.getJSONArray("alternatives"), 4)
                            require(preferred !in alternatives)
                            put(requireNotNull(level), HeroBuildTalentSelection(preferred, alternatives))
                        }
                    }
                    HeroBuild(backupString(item.get("id"), 128), backupString(item.get("name"), 2048), selections)
                }
                require(entries.map { it.id }.distinct().size == entries.size)
                entries.forEachIndexed { index, build ->
                    require(entries.take(index).none { it.name.trim().equals(build.name.trim(), ignoreCase = true) })
                }
                put(hero, entries)
            }
        }
        return UserDataBackup(backupIds(root.getJSONArray("favorites"), 512), personal, builds)
    } catch (error: UnsupportedBackupVersionException) {
        throw error
    } catch (error: Exception) {
        throw InvalidBackupException(error)
    }
}

private fun backupString(value: Any, maxLength: Int): String {
    require(value is String && value.isNotBlank() && value.length <= maxLength)
    require(value.none { it.isISOControl() })
    return value
}

private fun backupIds(array: JSONArray, maxCount: Int): Set<String> {
    require(array.length() <= maxCount)
    val values = List(array.length()) { backupString(array.get(it), 256) }
    require(values.distinct().size == values.size)
    return values.toSet()
}
