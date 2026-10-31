package com.github.ceav17.hots_talent_calculator

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.widget.Toast
import androidx.core.net.toUri

// Original hero presentations from the official @BlizzHeroes channel, checked on 2026-10-21.
// https://www.youtube.com/@BlizzHeroes/search?query=spotlight
// https://www.youtube.com/playlist?list=PL9WtwA1r0_Tbdy0Tb6Yk1nN-mgUox7uHt
internal val HeroSpotlightVideoIds: Map<String, String> = mapOf(
    "alarak" to "hep-bsMW1rU",
    "alexstrasza" to "y-aq0b3AzbQ",
    "ana" to "V6cWYGTYFXQ",
    "anduin" to "OgEPC3Vb2eM",
    "artanis" to "oxRwiV44EtU",
    "auriel" to "8rCeejaaYlg",
    "blaze" to "KeASyfPmpo8",
    "cassia" to "NZkdtQ1fOZY",
    "cho" to "FD5ug5jD-q4",
    "chromie" to "x79UvuRCWGY",
    "deathwing" to "LVhxQrg19lg",
    "deckard-cain" to "Yx0sZIIJ5Bc",
    "dehaka" to "1c5SJV7-Q9A",
    "dva" to "HlrAD_fkZ74",
    "fenix" to "YiLiBMcXxj0",
    "gall" to "FD5ug5jD-q4",
    "garrosh" to "s-_qEVW9I_A",
    "genji" to "-SJgvzPRFEI",
    "greymane" to "Dt_Tv850m8M",
    "guldan" to "7dsqSWKfHBA",
    "hanzo" to "t6wRlSIcF2Y",
    "hogger" to "9r5d3z4xGio",
    "imperius" to "oHMOYSdiPUo",
    "johanna" to "rz0owFn1Vq0",
    "junkrat" to "PoolAOfCPRE",
    "kaelthas" to "qyoSLycPAPY",
    "kel-thuzad" to "Uv35ukl8c-w",
    "kharazim" to "E1tzJ9lDY7Y",
    "leoric" to "rGVYgvRkBBA",
    "li-ming" to "6SqmKqFUwnM",
    "lt-morales" to "KePCgX8D9Uk",
    "lucio" to "MRlkCiPVmII",
    "lunara" to "4OqrfLfaOnc",
    "maiev" to "Nb-ibABP9gs",
    "malganis" to "nd9aKlWu75k",
    "malthael" to "qSBHR77CqTE",
    "medivh" to "eDIHAJnrG3M",
    "mei" to "qX_YBY_tRJI",
    "mephisto" to "m0nt7SJKfwo",
    "orphea" to "WVoeUUeYehY",
    "probius" to "vEH40-PaDD8",
    "qhira" to "Y9tD26PPI0E",
    "ragnaros" to "uEGmJXmavB0",
    "rexxar" to "7SDdVEFgczM",
    "samuro" to "B6xoRBkoZWQ",
    "stukov" to "KBBUVUmzxKk",
    "the-butcher" to "aA-n7Nmwwwg",
    "tracer" to "LzaMOGjKMXw",
    "valeera" to "sULZ2SQUbuw",
    "varian" to "Ef_JyPJskGo",
    "whitemane" to "HdpqWokf3_w",
    "xalatath" to "QwXQEtGlkc4",
    "xul" to "uC7AdjaAE2o",
    "yrel" to "DJ7j0MPoyrA",
    "zarya" to "f7KPPdfRs-o",
    "zuljin" to "_anDLgrR2Tc",
)

internal fun openHeroSpotlight(context: Context, videoId: String) {
    val intent = Intent(Intent.ACTION_VIEW, "https://www.youtube.com/watch?v=$videoId".toUri())
        .addCategory(Intent.CATEGORY_BROWSABLE)
        .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    try {
        context.startActivity(intent)
    } catch (_: ActivityNotFoundException) {
        Toast.makeText(context, R.string.hero_spotlight_no_app, Toast.LENGTH_SHORT).show()
    } catch (_: SecurityException) {
        Toast.makeText(context, R.string.hero_spotlight_no_app, Toast.LENGTH_SHORT).show()
    }
}
