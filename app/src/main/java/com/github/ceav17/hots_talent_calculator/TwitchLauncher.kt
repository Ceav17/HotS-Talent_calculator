package com.github.ceav17.hots_talent_calculator

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.widget.Toast
import androidx.core.net.toUri

private const val HEROES_TWITCH_URL =
    "https://www.twitch.tv/directory/category/heroes-of-the-storm?sort=VIEWER_COUNT"

internal fun openHeroesOnTwitch(context: Context) {
    val uri = HEROES_TWITCH_URL.toUri()
    for (packageName in listOf("com.github.andreyasadchy.xtra", "tv.twitch.android.app")) {
        val intent = Intent(Intent.ACTION_VIEW, uri)
            .addCategory(Intent.CATEGORY_BROWSABLE)
            .setPackage(packageName)
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        if (tryOpenTwitchLink(context, intent)) return
    }

    val browserIntent = Intent(Intent.ACTION_VIEW, uri).apply {
        addCategory(Intent.CATEGORY_BROWSABLE)
        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        // Resolve a generic HTTPS URL so another Twitch client cannot intercept the fallback.
        selector = Intent(Intent.ACTION_VIEW, "https://".toUri())
            .addCategory(Intent.CATEGORY_BROWSABLE)
    }
    if (!tryOpenTwitchLink(context, browserIntent)) {
        Toast.makeText(context, R.string.twitch_no_app, Toast.LENGTH_SHORT).show()
    }
}

private fun tryOpenTwitchLink(context: Context, intent: Intent): Boolean = try {
    context.startActivity(intent)
    true
} catch (_: ActivityNotFoundException) {
    false
} catch (_: SecurityException) {
    false
}
