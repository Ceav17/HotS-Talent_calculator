package com.github.ceav17.hots_talent_calculator

import android.annotation.SuppressLint
import android.content.Context
import java.io.IOException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject

private val UserPreferenceNames = listOf("hero_favorites", "hero_custom_group", "hero_builds")

internal object UserDataAccess {
    private val mutex = Mutex()

    suspend fun <T> withLock(context: Context, block: suspend () -> T): T = withContext(Dispatchers.IO) {
        mutex.withLock {
            recover(context)
            block()
        }
    }

    private fun journal(context: Context) = UserDataImportJournal(context.noBackupFilesDir)

    // All three stores use this lock. A process interrupted during import restores its old data first.
    private fun recover(context: Context) {
        val file = journal(context)
        val bytes = file.read() ?: return
        val previous = JSONObject(String(bytes, Charsets.UTF_8))
        writePreferences(context, previous)
        file.clear()
    }

    fun replaceLocked(context: Context, data: UserDataBackup) {
        val previous = JSONObject()
        UserPreferenceNames.forEach { name ->
            val values = JSONObject()
            preferences(context, name).all.forEach { (key, value) ->
                values.put(key, when (value) {
                    is Set<*> -> JSONArray(value.toList())
                    is String, is Int -> value
                    else -> throw IOException("Unexpected stored preference type")
                })
            }
            previous.put(name, values)
        }
        val file = journal(context)
        file.prepare(previous.toString().toByteArray(Charsets.UTF_8))
        try {
            writePreferences(context, backupPreferences(data))
            file.clear()
        } catch (error: Exception) {
            try {
                recover(context)
            } catch (recoveryError: Exception) {
                error.addSuppressed(recoveryError)
            }
            throw error
        }
    }

    @SuppressLint("UseKtx", "ApplySharedPref") // A checked, synchronous commit is required for rollback.
    private fun writePreferences(context: Context, values: JSONObject) {
        UserPreferenceNames.forEach { name ->
            val stored = values.getJSONObject(name)
            val editor = preferences(context, name).edit().clear()
            stored.keys().forEach { key ->
                when (val value = stored.get(key)) {
                    is String -> editor.putString(key, value)
                    is Int -> editor.putInt(key, value)
                    is JSONArray -> editor.putStringSet(key, List(value.length()) { value.getString(it) }.toSet())
                    else -> throw IOException("Invalid recovery data")
                }
            }
            if (!editor.commit()) throw IOException("Could not persist imported data")
        }
    }

    private fun preferences(context: Context, name: String) = context.getSharedPreferences(name, Context.MODE_PRIVATE)

    private fun backupPreferences(data: UserDataBackup): JSONObject {
        val groups = JSONObject().put("visible_count", data.customGroups.visibleCount)
        data.customGroups.groups.forEachIndexed { index, group ->
            val suffix = if (index == 0) "" else "_$index"
            groups.put("label$suffix", group.label).put("hero_ids$suffix", JSONArray(group.heroIds.sorted()))
        }
        val builds = JSONObject()
        data.builds.forEach { (hero, entries) -> builds.put(hero, heroBuildsJson(entries).toString()) }
        return JSONObject()
            .put("hero_favorites", JSONObject().put("hero_ids", JSONArray(data.favoriteIds.sorted())))
            .put("hero_custom_group", groups).put("hero_builds", builds)
    }
}

internal class UserDataStore(context: Context) {
    private val appContext = context.applicationContext

    suspend fun export(): UserDataBackup = UserDataAccess.withLock(appContext) {
        UserDataBackup(
            appContext.getSharedPreferences("hero_favorites", Context.MODE_PRIVATE)
                .getStringSet("hero_ids", emptySet()).orEmpty().toSet(),
            HeroCustomGroupStore(appContext).readGroups(),
            HeroBuildsStore(appContext).readAllBuilds()
        )
    }

    fun validate(data: UserDataBackup) {
        // Apply the same Unicode/grapheme rules as the existing build and filter editors.
        if (data.customGroups.groups.any { !isValidCustomGroupLabel(it.label) } ||
            data.builds.values.flatten().any { !isValidBuildName(it.name) }
        ) throw InvalidBackupException()
    }

    suspend fun restore(data: UserDataBackup) = UserDataAccess.withLock(appContext) {
        validate(data)
        UserDataAccess.replaceLocked(appContext, data)
    }
}
