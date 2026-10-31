package com.github.ceav17.hots_talent_calculator

import android.annotation.SuppressLint
import android.content.Context
import java.io.IOException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

internal class HeroFavoritesStore(context: Context) {
    private val appContext = context.applicationContext
    private val preferences by lazy { appContext.getSharedPreferences("hero_favorites", Context.MODE_PRIVATE) }

    suspend fun load(): Set<String> = withContext(Dispatchers.IO) {
        UserDataAccess.withLock(appContext) { preferences.getStringSet("hero_ids", emptySet()).orEmpty().toSet() }
    }

    @SuppressLint("UseKtx") // The KTX helper discards the commit success flag.
    suspend fun setFavorite(heroId: String, favorite: Boolean): Set<String> = withContext(Dispatchers.IO) {
        UserDataAccess.withLock(appContext) {
            val current = preferences.getStringSet("hero_ids", emptySet()).orEmpty().toSet()
            val updated = if (favorite) current + heroId else current - heroId
            // Publish the change only after it reaches disk: Exit terminates the app process.
            if (!preferences.edit().putStringSet("hero_ids", updated).commit()) {
                throw IOException("Could not persist favorite heroes")
            }
            updated
        }
    }
}
