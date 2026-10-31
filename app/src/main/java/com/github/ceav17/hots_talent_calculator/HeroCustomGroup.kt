package com.github.ceav17.hots_talent_calculator

import android.annotation.SuppressLint
import android.content.Context
import android.icu.lang.UCharacter
import android.icu.lang.UProperty
import android.icu.text.BreakIterator
import java.io.IOException
import java.util.Locale
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

internal val DefaultCustomGroupLabels = listOf("S", "A", "B", "C")
internal const val MaxCustomGroups = 4

internal data class HeroCustomGroup(
    val label: String = "S",
    val heroIds: Set<String> = emptySet()
)

internal data class HeroCustomGroups(
    val groups: List<HeroCustomGroup> = DefaultCustomGroupLabels.map { HeroCustomGroup(label = it) },
    val visibleCount: Int = 1
) {
    val visibleGroups: List<HeroCustomGroup> get() = groups.take(visibleCount)

    fun matchingHeroIds(selectedGroups: Set<Int>): Set<String>? {
        val selected = selectedGroups.filter { it in 0 until visibleCount }
        // Hidden filters have no effect; selected personal groups are combined with OR.
        return if (selected.isEmpty()) null else selected.flatMap { groups[it].heroIds }.toSet()
    }
}

internal fun isValidCustomGroupLabel(text: String): Boolean {
    // Count visible characters, not UTF-16 units: accents and emoji can contain several units.
    if (text.isEmpty() || text.length > 64) return false
    val characters = BreakIterator.getCharacterInstance(Locale.ROOT)
    characters.setText(text)
    if (characters.next() != text.length) return false

    var visible = false
    var offset = 0
    while (offset < text.length) {
        val codePoint = text.codePointAt(offset)
        val type = Character.getType(codePoint)
        if (Character.isISOControl(codePoint) || Character.isWhitespace(codePoint) ||
            Character.isSpaceChar(codePoint) || codePoint == 0x2800 ||
            type == Character.SURROGATE.toInt() ||
            !Character.isDefined(codePoint)
        ) return false
        // Joiners and emoji tags may belong to one glyph; reject unrelated direction controls.
        if (type == Character.FORMAT.toInt() && codePoint != 0x200C && codePoint != 0x200D &&
            codePoint !in 0xE0020..0xE007F
        ) return false
        if (!UCharacter.hasBinaryProperty(codePoint, UProperty.DEFAULT_IGNORABLE_CODE_POINT) &&
            type != Character.NON_SPACING_MARK.toInt() &&
            type != Character.COMBINING_SPACING_MARK.toInt() &&
            type != Character.ENCLOSING_MARK.toInt()
        ) visible = true
        offset += Character.charCount(codePoint)
    }
    return visible
}

internal class HeroCustomGroupStore(context: Context) {
    private val appContext = context.applicationContext
    private val preferences by lazy {
        appContext.getSharedPreferences("hero_custom_group", Context.MODE_PRIVATE)
    }

    suspend fun load(): HeroCustomGroups = withContext(Dispatchers.IO) {
        UserDataAccess.withLock(appContext) { readGroups() }
    }

    internal fun readGroups(): HeroCustomGroups = HeroCustomGroups(
        groups = DefaultCustomGroupLabels.mapIndexed { index, defaultLabel ->
            HeroCustomGroup(
                label = preferences.getString(key("label", index), defaultLabel)
                    ?.takeIf(::isValidCustomGroupLabel) ?: defaultLabel,
                heroIds = preferences.getStringSet(key("hero_ids", index), emptySet()).orEmpty().toSet()
            )
        },
        visibleCount = preferences.getInt("visible_count", 1).coerceIn(0, MaxCustomGroups)
    )

    // Keep the existing S filter's keys so upgrades preserve its label and heroes without a migration.
    private fun key(name: String, index: Int): String = if (index == 0) name else "${name}_$index"

    @SuppressLint("UseKtx") // Check that the write succeeded before updating the UI.
    suspend fun saveGroup(index: Int, group: HeroCustomGroup): HeroCustomGroups = withContext(Dispatchers.IO) {
        require(index in 0 until MaxCustomGroups)
        require(isValidCustomGroupLabel(group.label))
        UserDataAccess.withLock(appContext) {
            if (!preferences.edit().putString(key("label", index), group.label)
                    .putStringSet(key("hero_ids", index), group.heroIds).commit()
            ) throw IOException("Could not persist the custom hero group")
            readGroups()
        }
    }

    @SuppressLint("UseKtx") // A hidden group must keep all of its saved data.
    suspend fun setVisibleCount(count: Int): HeroCustomGroups = withContext(Dispatchers.IO) {
        require(count in 0..MaxCustomGroups)
        UserDataAccess.withLock(appContext) {
            if (!preferences.edit().putInt("visible_count", count).commit()) {
                throw IOException("Could not persist the personal filter count")
            }
            readGroups()
        }
    }
}
