package com.github.ceav17.hots_talent_calculator

import androidx.compose.ui.unit.Dp
import kotlin.time.Duration.Companion.seconds
import androidx.annotation.DrawableRes
import androidx.compose.foundation.Image
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyGridState
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay

internal val HeroGridHeaderHeight = 64.dp

private data class HeroPortrait(
    val id: String?,
    val name: String,
    @param:DrawableRes val icon: Int
)

// The a_random asset precedes the alphabetical hero list.
private val randomPortrait = HeroPortrait(null, "Random", R.drawable.hero_a_random)
private val heroPortraits = listOf(
    HeroPortrait("abathur", "Abathur", R.drawable.hero_abathur),
    HeroPortrait("alarak", "Alarak", R.drawable.hero_alarak),
    HeroPortrait("alexstrasza", "Alexstrasza", R.drawable.hero_alexstrasza),
    HeroPortrait("ana", "Ana", R.drawable.hero_ana),
    HeroPortrait("anduin", "Anduin", R.drawable.hero_anduin),
    HeroPortrait("anubarak", "Anub'arak", R.drawable.hero_anubarak),
    HeroPortrait("artanis", "Artanis", R.drawable.hero_artanis),
    HeroPortrait("arthas", "Arthas", R.drawable.hero_arthas),
    HeroPortrait("auriel", "Auriel", R.drawable.hero_auriel),
    HeroPortrait("azmodan", "Azmodan", R.drawable.hero_azmodan),
    HeroPortrait("blaze", "Blaze", R.drawable.hero_blaze),
    HeroPortrait("brightwing", "Brightwing", R.drawable.hero_brightwing),
    HeroPortrait("cassia", "Cassia", R.drawable.hero_cassia),
    HeroPortrait("chen", "Chen", R.drawable.hero_chen),
    HeroPortrait("cho", "Cho", R.drawable.hero_cho),
    HeroPortrait("chromie", "Chromie", R.drawable.hero_chromie),
    HeroPortrait("deathwing", "Deathwing", R.drawable.hero_deathwing),
    HeroPortrait("deckard-cain", "Deckard", R.drawable.hero_deckard),
    HeroPortrait("dehaka", "Dehaka", R.drawable.hero_dehaka),
    HeroPortrait("diablo", "Diablo", R.drawable.hero_diablo),
    HeroPortrait("dva", "D.Va", R.drawable.hero_dva),
    HeroPortrait("e-t-c", "E.T.C.", R.drawable.hero_etc),
    HeroPortrait("falstad", "Falstad", R.drawable.hero_falstad),
    HeroPortrait("fenix", "Fenix", R.drawable.hero_fenix),
    HeroPortrait("gall", "Gall", R.drawable.hero_gall),
    HeroPortrait("garrosh", "Garrosh", R.drawable.hero_garrosh),
    HeroPortrait("gazlowe", "Gazlowe", R.drawable.hero_gazlowe),
    HeroPortrait("genji", "Genji", R.drawable.hero_genji),
    HeroPortrait("greymane", "Greymane", R.drawable.hero_graymane),
    HeroPortrait("guldan", "Gul'dan", R.drawable.hero_guldan),
    HeroPortrait("hanzo", "Hanzo", R.drawable.hero_hanzo),
    HeroPortrait("hogger", "Hogger", R.drawable.hero_hogger),
    HeroPortrait("illidan", "Illidan", R.drawable.hero_illidan),
    HeroPortrait("imperius", "Imperius", R.drawable.hero_imperius),
    HeroPortrait("jaina", "Jaina", R.drawable.hero_jaina),
    HeroPortrait("johanna", "Johanna", R.drawable.hero_johanna),
    HeroPortrait("junkrat", "Junkrat", R.drawable.hero_junkrat),
    HeroPortrait("kaelthas", "Kael'thas", R.drawable.hero_kaelthas),
    HeroPortrait("kel-thuzad", "Kel'Thuzad", R.drawable.hero_kelthuzad),
    HeroPortrait("kerrigan", "Kerrigan", R.drawable.hero_kerrigan),
    HeroPortrait("kharazim", "Kharazim", R.drawable.hero_kharazim),
    HeroPortrait("leoric", "Leoric", R.drawable.hero_leoric),
    HeroPortrait("li-li", "Li Li", R.drawable.hero_li_li),
    HeroPortrait("li-ming", "Li-Ming", R.drawable.hero_li_ming),
    HeroPortrait("lt-morales", "Lt. Morales", R.drawable.hero_lt_morales),
    HeroPortrait("lucio", "Lúcio", R.drawable.hero_lucio),
    HeroPortrait("lunara", "Lunara", R.drawable.hero_lunara),
    HeroPortrait("maiev", "Maiev", R.drawable.hero_maiev),
    HeroPortrait("malganis", "Mal'Ganis", R.drawable.hero_malganis),
    HeroPortrait("malfurion", "Malfurion", R.drawable.hero_malfurion),
    HeroPortrait("malthael", "Malthael", R.drawable.hero_malthael),
    HeroPortrait("medivh", "Medivh", R.drawable.hero_medivh),
    HeroPortrait("mei", "Mei", R.drawable.hero_mei),
    HeroPortrait("mephisto", "Mephisto", R.drawable.hero_mephisto),
    HeroPortrait("muradin", "Muradin", R.drawable.hero_muradin),
    HeroPortrait("murky", "Murky", R.drawable.hero_murky),
    HeroPortrait("nazeebo", "Nazeebo", R.drawable.hero_nazeebo),
    HeroPortrait("nova", "Nova", R.drawable.hero_nova),
    HeroPortrait("orphea", "Orphea", R.drawable.hero_orphea),
    HeroPortrait("probius", "Probius", R.drawable.hero_probius),
    HeroPortrait("qhira", "Qhira", R.drawable.hero_qhira),
    HeroPortrait("ragnaros", "Ragnaros", R.drawable.hero_ragnaros),
    HeroPortrait("raynor", "Raynor", R.drawable.hero_raynor),
    HeroPortrait("rehgar", "Rehgar", R.drawable.hero_rehgar),
    HeroPortrait("rexxar", "Rexxar", R.drawable.hero_rexxar),
    HeroPortrait("samuro", "Samuro", R.drawable.hero_samuro),
    HeroPortrait("sgt-hammer", "Sgt. Hammer", R.drawable.hero_sgt_hammer),
    HeroPortrait("sonya", "Sonya", R.drawable.hero_sonya),
    HeroPortrait("stitches", "Stitches", R.drawable.hero_stitches),
    HeroPortrait("stukov", "Stukov", R.drawable.hero_stukov),
    HeroPortrait("sylvanas", "Sylvanas", R.drawable.hero_sylvanas),
    HeroPortrait("tassadar", "Tassadar", R.drawable.hero_tassadar),
    HeroPortrait("the-butcher", "The Butcher", R.drawable.hero_the_butcher),
    HeroPortrait("the-lost-vikings", "The Lost Vikings", R.drawable.hero_the_lost_vikings),
    HeroPortrait("thrall", "Thrall", R.drawable.hero_thrall),
    HeroPortrait("tracer", "Tracer", R.drawable.hero_tracer),
    HeroPortrait("tychus", "Tychus", R.drawable.hero_tychus),
    HeroPortrait("tyrael", "Tyrael", R.drawable.hero_tyrael),
    HeroPortrait("tyrande", "Tyrande", R.drawable.hero_tyrande),
    HeroPortrait("uther", "Uther", R.drawable.hero_uther),
    HeroPortrait("valeera", "Valeera", R.drawable.hero_valeera),
    HeroPortrait("valla", "Valla", R.drawable.hero_valla),
    HeroPortrait("varian", "Varian", R.drawable.hero_varian),
    HeroPortrait("whitemane", "Whitemane", R.drawable.hero_whitemane),
    HeroPortrait("xalatath", "Xal'atath", R.drawable.hero_xalatath),
    HeroPortrait("xul", "Xul", R.drawable.hero_xul),
    HeroPortrait("yrel", "Yrel", R.drawable.hero_yrel),
    HeroPortrait("zagara", "Zagara", R.drawable.hero_zagara),
    HeroPortrait("zarya", "Zarya", R.drawable.hero_zarya),
    HeroPortrait("zeratul", "Zeratul", R.drawable.hero_zeratul),
    HeroPortrait("zuljin", "Zul'jin", R.drawable.hero_zuljin)
)

@DrawableRes
internal fun heroSelectionPortrait(heroId: String): Int? =
    heroPortraits.firstOrNull { it.id == heroId }?.icon

@Composable
internal fun HeroGrid(
    searchEntries: Map<String, HeroSearchEntry>,
    searchQuery: String,
    selectedRoles: Set<String>,
    selectedUniverses: Set<String>,
    favoriteIds: Set<String>,
    favoritesOnly: Boolean,
    favoriteActionsEnabled: Boolean,
    customGroups: HeroCustomGroups,
    selectedCustomGroups: Set<Int>,
    customGroupActionsEnabled: Boolean,
    onFavoriteChange: (String, Boolean) -> Unit,
    onCustomGroupChange: (Int, HeroCustomGroup) -> Unit,
    onFavoriteMenuOpen: () -> Unit,
    onHeroSelected: (String) -> Unit,
    modifier: Modifier = Modifier,
    headerHeight: Dp = HeroGridHeaderHeight
) {
    // Keep trailing spaces in nonblank queries; blank queries disable the name filter.
    val query = searchQuery.ifBlank { "" }
    val customGroupIds = remember(customGroups, selectedCustomGroups) {
        customGroups.matchingHeroIds(selectedCustomGroups)
    }
    val filteredHeroes = remember(
        query, selectedRoles, selectedUniverses, searchEntries, favoritesOnly, favoriteIds,
        customGroupIds
    ) {
        heroPortraits.filter { hero ->
            searchEntries[hero.id]?.matches(
                query, selectedRoles, selectedUniverses, favoriteIds.takeIf { favoritesOnly },
                customGroupIds
            ) == true
        }
    }
    val visiblePortraits = remember(filteredHeroes) {
        if (filteredHeroes.size > 1) listOf(randomPortrait) + filteredHeroes else filteredHeroes
    }
    // Reset scrolling only when filters change; returning from a hero restores it.
    val gridState = rememberSaveable(
        query, selectedRoles, selectedUniverses, favoritesOnly, selectedCustomGroups, saver = LazyGridState.Saver
    ) {
        LazyGridState()
    }

    var menuHeroId by remember { mutableStateOf<String?>(null) }
    val hasFavorites = filteredHeroes.any { it.id in favoriteIds }
    val glowVisible by produceState(true, hasFavorites) {
        value = true
        if (hasFavorites) {
            while (true) {
                delay(1.seconds)
                value = !value
            }
        }
    }

    BoxWithConstraints(modifier = modifier) {
        val columnCount = ((maxWidth + 8.dp) / 96.dp).toInt().coerceIn(3, 6)
        Box(
            modifier = Modifier.fillMaxWidth().height(headerHeight),
            contentAlignment = Alignment.Center
        ) {
            if (filteredHeroes.isNotEmpty()) {
                OutlinedText(
                    text = filteredHeroes.size.toString(),
                    style = TextStyle(
                        color = Color.White,
                        fontFamily = MenuFontFamily,
                        fontWeight = FontWeight.Bold,
                        fontSize = 20.sp,
                        lineHeight = 26.sp
                    ),
                    modifier = Modifier.testTag("hero_result_count"),
                    maxLines = 1
                )
            }
        }
        // Leave space for the first row's badges to straddle the portraits without clipping.
        val badgeOverflow = ((maxWidth - 8.dp * (columnCount - 1)) / columnCount * 0.14f).coerceAtLeast(0.dp)
        LazyVerticalGrid(
            columns = GridCells.Fixed(columnCount),
            modifier = Modifier.fillMaxSize().padding(top = headerHeight),
            state = gridState,
            contentPadding = PaddingValues(top = badgeOverflow),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(visiblePortraits, key = { it.icon }) { hero ->
                val favorite = hero.id in favoriteIds
                val menuOpen = hero.id != null && menuHeroId == hero.id
                val actionLabel = stringResource(R.string.hero_actions)
                val favoriteDescription = stringResource(R.string.hero_favorite_state)
                BoxWithConstraints(
                    modifier = Modifier.fillMaxWidth().aspectRatio(1f)
                        .testTag("hero_portrait_${hero.id ?: "random"}")
                        .semantics { if (favorite) stateDescription = favoriteDescription }
                        .combinedClickable(
                            interactionSource = null,
                            indication = null,
                            role = Role.Button,
                            onLongClickLabel = actionLabel.takeIf { hero.id != null },
                            onLongClick = if (hero.id != null && (favoriteActionsEnabled || customGroupActionsEnabled)) {
                                {
                                    onFavoriteMenuOpen()
                                    menuHeroId = hero.id
                                }
                            } else null,
                            onClick = {
                                // Random always uses the same filtered list displayed in the grid.
                                val selectedId = hero.id ?: filteredHeroes.randomOrNull()?.id
                                selectedId?.let(onHeroSelected)
                            }
                        )
                ) {
                    Image(
                        painter = painterResource(hero.icon),
                        contentDescription = hero.name,
                        contentScale = ContentScale.Fit,
                        modifier = Modifier.fillMaxSize()
                    )
                    if (menuOpen) {
                        Image(
                            painter = painterResource(R.drawable.storm_ui_hud_event_lunar_2017_ring_border),
                            contentDescription = null,
                            contentScale = ContentScale.Fit,
                            modifier = Modifier.fillMaxSize().testTag("hero_menu_highlight_${hero.id}")
                        )
                    }
                    if (favorite) {
                        HeroFavoriteBadge(
                            heroId = requireNotNull(hero.id),
                            portraitSize = maxWidth,
                            glowVisible = glowVisible
                        )
                    }
                    if (hero.id != null && menuHeroId == hero.id) {
                        HeroPortraitMenu(
                            favorite = favorite,
                            heroId = hero.id,
                            customGroups = customGroups.visibleGroups,
                            favoriteEnabled = favoriteActionsEnabled,
                            customGroupEnabled = customGroupActionsEnabled,
                            onDismiss = { menuHeroId = null },
                            onFavoriteClick = {
                                menuHeroId = null
                                onFavoriteChange(hero.id, !favorite)
                            },
                            onCustomGroupClick = { index ->
                                menuHeroId = null
                                val group = customGroups.groups[index]
                                onCustomGroupChange(index, group.copy(
                                    heroIds = if (hero.id in group.heroIds) group.heroIds - hero.id
                                        else group.heroIds + hero.id
                                ))
                            }
                        )
                    }
                }
            }
        }
    }
}
