package com.github.ceav17.hots_talent_calculator

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.absoluteOffset
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.viewmodel.compose.viewModel
import java.text.NumberFormat
import java.time.Instant
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlinx.coroutines.delay

private val LeaderboardTextStyle = TextStyle(
    fontFamily = MenuFontFamily, fontWeight = FontWeight.Bold,
    color = Color.White, fontSize = 14.sp, lineHeight = 18.sp, letterSpacing = 0.sp
)
private val LeaderboardDateFormat = DateTimeFormatter.ofPattern("MMM d, yyyy, HH:mm 'UTC'", Locale.ENGLISH)
    .withZone(ZoneOffset.UTC)
private val LeaderboardCellPadding = 1.5.dp
private val LeaderboardUnselectedTextColor = Color(0xFF9A9A9A)

private data class LeaderboardTableLayout(
    val style: TextStyle,
    val statsWidths: List<Dp>,
    val roleWidths: List<Dp>
)

@Composable
internal fun LeaderboardScreen(
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    model: LeaderboardViewModel = viewModel()
) {
    val state by model.state.collectAsState()
    val lifecycle = LocalLifecycleOwner.current.lifecycle
    DisposableEffect(lifecycle, model) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) model.refresh()
        }
        lifecycle.addObserver(observer)
        if (lifecycle.currentState.isAtLeast(Lifecycle.State.RESUMED)) model.refresh()
        onDispose { lifecycle.removeObserver(observer) }
    }
    val snapshot = state.result.snapshot
    LaunchedEffect(state.result.cached, snapshot?.updatedAt) {
        if (state.result.cached && snapshot != null) {
            delay((snapshot.updatedAt + LEADERBOARD_CACHE_MAX_AGE_MS - System.currentTimeMillis()).coerceAtLeast(0))
            model.refresh()
        }
    }
    LeaderboardContent(state, model::selectRegion, model::refresh, onBack, modifier)
}

@Composable
internal fun LeaderboardContent(
    state: LeaderboardUiState,
    onRegion: (LeaderboardRegion) -> Unit,
    onRefresh: () -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val snapshot = state.result.snapshot
    ResponsiveScreen(
        backgroundResId = R.drawable.storm_loading_generic,
        modifier = modifier.testTag("leaderboards"),
        // Let the crown span 90% of the viewport even on wide tablets.
        maxContentWidth = Dp.Unspecified
    ) {
        val backTop = calculatorBackIconOffset(maxWidth, maxHeight).y
        val titleLeft = 24.dp + CalculatorBackIconSize + 8.dp
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier.absoluteOffset(titleLeft, backTop)
                .size((maxWidth - titleLeft - 24.dp).coerceAtLeast(1.dp), CalculatorBackIconSize)
        ) {
            OutlinedText(stringResource(R.string.leaderboards_title), LeaderboardTextStyle.copy(fontSize = 26.sp, lineHeight = 32.sp), maxLines = 1)
        }
        val contentModifier = Modifier.fillMaxSize().padding(top = backTop + CalculatorBackIconSize + 8.dp)
        if (snapshot != null) {
            key(state.region) {
                LeaderboardPlayers(snapshot.players, contentModifier) {
                    LeaderboardControls(state, onRegion, onRefresh)
                }
            }
        } else {
            Column(contentModifier) {
                LeaderboardControls(state, onRegion, onRefresh)
                if (!state.loading) {
                    Box(Modifier.fillMaxWidth().weight(1f).padding(24.dp), contentAlignment = Alignment.TopCenter) {
                        Text(stringResource(R.string.leaderboards_need_connection), style = LeaderboardTextStyle, textAlign = TextAlign.Center)
                    }
                }
            }
        }
        Image(
            painterResource(R.drawable.storm_ui_glues_button_close_normal),
            contentDescription = stringResource(R.string.leaderboards_back), contentScale = ContentScale.Fit,
            modifier = Modifier.absoluteOffset(24.dp, backTop).size(CalculatorBackIconSize)
                .clickable(interactionSource = null, indication = null, role = Role.Button, onClick = onBack)
        )
    }
}

@Composable
private fun LeaderboardControls(
    state: LeaderboardUiState,
    onRegion: (LeaderboardRegion) -> Unit,
    onRefresh: () -> Unit
) {
    val snapshot = state.result.snapshot
    Column(Modifier.fillMaxWidth()) {
        val warning = leaderboardWarning(state.result)
        if (warning != null) {
            Text(
                warning, style = LeaderboardTextStyle.copy(color = Color(0xFFFF7777), fontSize = 12.sp, lineHeight = 16.sp),
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp).testTag("leaderboard_warning")
            )
        }
        Image(
            painter = painterResource(R.drawable.leaderboard_crown),
            contentDescription = null,
            contentScale = ContentScale.Fit,
            modifier = Modifier.align(Alignment.CenterHorizontally)
                .fillMaxWidth(0.9f).aspectRatio(876f / 173f)
        )
        Row(
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp)
        ) {
            LeaderboardRegion.entries.forEach { region ->
                val selected = state.region == region
                MainMenuItem(
                    imageResId = if (selected) R.drawable.storm_standardbutton_blue_hover else R.drawable.storm_standardbutton_blue_normal,
                    label = stringResource(region.label()), fontSize = 15.sp, textPadding = 4.dp,
                    textColor = if (selected) Color.White else LeaderboardUnselectedTextColor,
                    modifier = Modifier.weight(1f).height(48.dp).testTag("leaderboard_region_${region.slug}")
                        .selectable(selected, interactionSource = null, indication = null, role = Role.Tab, onClick = { onRegion(region) })
                )
            }
        }
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp)
        ) {
            Column(modifier = Modifier.weight(1f)) {
                snapshot?.let {
                    Text(it.season, style = LeaderboardTextStyle)
                    Text(
                        stringResource(R.string.leaderboards_updated, LeaderboardDateFormat.format(Instant.ofEpochMilli(it.updatedAt))),
                        style = LeaderboardTextStyle.copy(fontSize = 11.sp, lineHeight = 15.sp, color = Color(0xFFD0D4EF))
                    )
                }
            }
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier.heightIn(min = 48.dp).padding(start = 8.dp)
                    .clickable(enabled = !state.loading, interactionSource = null, indication = null, role = Role.Button, onClick = onRefresh)
                    .testTag("leaderboard_refresh")
            ) {
                OutlinedText(
                    stringResource(if (state.loading) R.string.leaderboards_loading else R.string.leaderboards_refresh),
                    LeaderboardTextStyle.copy(color = if (state.loading) Color.Gray else Color(0xFF88CCFF))
                )
            }
        }
        if (state.loading) LinearProgressIndicator(modifier = Modifier.fillMaxWidth().height(2.dp))
    }
}

@Composable
private fun leaderboardWarning(result: LeaderboardResult): String? {
    val problem = result.problem ?: return null
    val message = when (problem) {
        LeaderboardProblem.OFFLINE -> if (result.cached) R.string.leaderboards_offline_cached else R.string.leaderboards_offline
        LeaderboardProblem.NETWORK -> if (result.cached) R.string.leaderboards_network_cached else R.string.leaderboards_network
        LeaderboardProblem.FORMAT -> if (result.cached) R.string.leaderboards_format_cached else R.string.leaderboards_format
        LeaderboardProblem.CACHE_WRITE -> R.string.leaderboards_cache_failed
    }
    return stringResource(message) + if (result.expired) " " + stringResource(R.string.leaderboards_cache_expired) else ""
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun LeaderboardPlayers(
    players: List<LeaderboardPlayer>,
    modifier: Modifier = Modifier,
    controls: @Composable () -> Unit
) {
    val horizontal = rememberScrollState()
    val number = remember { NumberFormat.getIntegerInstance(Locale.ENGLISH) }
    var showRoles by rememberSaveable { mutableStateOf(false) }
    var sortColumn by rememberSaveable { mutableStateOf(LeaderboardSortColumn.RANK) }
    var sortDescending by rememberSaveable { mutableStateOf(false) }
    val listState = rememberLazyListState()
    val sortedPlayers = remember(players, sortColumn, sortDescending) {
        sortLeaderboardPlayers(players, sortColumn, sortDescending)
    }
    val selectSort: (LeaderboardSortColumn) -> Unit = { column ->
        sortDescending = if (sortColumn == column) !sortDescending else column.defaultDescending
        sortColumn = column
        // Controls and podium precede the header. Show the new first row instead of anchoring the old player's key.
        listState.requestScrollToItem(index = 2)
    }
    val labels = listOf(R.string.leaderboards_rank, R.string.leaderboards_battletag, R.string.leaderboards_highest_rank,
        R.string.leaderboards_points, R.string.leaderboards_games, R.string.leaderboards_wins).map { stringResource(it) }
    val percentUnit = stringResource(R.string.leaderboards_percentage_unit)
    val rows = remember(players, number) {
        players.associate { player ->
            player.rank to listOf(player.rank.toString(), player.battleTag, player.highestRank?.toString() ?: "—",
                number.format(player.points), number.format(player.games), number.format(player.wins))
        }
    }
    val samples = remember(rows, labels) {
        List(6) { index ->
            (rows.values.map { it[index] } + labels[index].split(' ') + when (index) {
                0, 2 -> listOf("1000")
                // Reserve the widest repeated Latin capital, and measure actual Unicode names too.
                1 -> listOf("W".repeat(12))
                else -> emptyList()
            }).distinct()
        }
    }
    val density = LocalDensity.current
    val measurer = rememberTextMeasurer(cacheSize = 0)
    LaunchedEffect(showRoles) { horizontal.scrollTo(0) }

    BoxWithConstraints(modifier.fillMaxWidth()) {
        val availableWidth = (maxWidth - 8.dp).coerceAtLeast(1.dp)
        val layout = remember(samples, availableWidth, density, measurer, percentUnit) {
            var chosen: LeaderboardTableLayout? = null
            // Match the per-side pixel rounding used by Modifier.padding.
            val cellPadding = with(density) { (LeaderboardCellPadding.roundToPx() * 2).toDp() }
            for (size in 14 downTo 12) {
                val style = LeaderboardTextStyle.copy(fontSize = size.sp, lineHeight = (size + 4).sp)
                fun width(text: String): Dp = with(density) {
                    measurer.measure(text, style = style, softWrap = false, maxLines = 1).size.width.toDp()
                }
                val stats = samples.map { values -> values.maxOf { width(it) } + cellPadding }
                val roleWidth = maxOf(24.dp, width("100"), width(percentUnit)) + cellPadding
                val roles = stats.take(2) + List(6) { roleWidth }
                fun total(widths: List<Dp>) = widths.fold(0.dp) { sum, value -> sum + value }
                fun expand(widths: List<Dp>): List<Dp> {
                    val extra = (availableWidth - total(widths)).coerceAtLeast(0.dp) / (widths.size - 2)
                    // Keep Rank and BattleTag in exactly the same positions on both pages.
                    return widths.mapIndexed { index, value -> if (index < 2) value else value + extra }
                }
                chosen = LeaderboardTableLayout(style, expand(stats), expand(roles))
                if (maxOf(total(stats), total(roles)) <= availableWidth) break
            }
            requireNotNull(chosen)
        }
        val widths = if (showRoles) layout.roleWidths else layout.statsWidths
        LazyColumn(
            state = listState,
            contentPadding = PaddingValues(bottom = 20.dp),
            verticalArrangement = Arrangement.spacedBy(2.dp),
            modifier = Modifier.fillMaxSize().testTag("leaderboard_players")
        ) {
            item(key = "controls") { controls() }
            if (players.isNotEmpty()) {
                item(key = "top_players") {
                    val gap = maxWidth * 0.04f
                    val cardWidth = (maxWidth - gap * 4) / 3
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(gap),
                        modifier = Modifier.fillMaxWidth().padding(horizontal = gap, vertical = 8.dp)
                    ) {
                        players.take(3).forEach { player -> TopLeaderboardPlayer(player, cardWidth, number) }
                    }
                }
                stickyHeader(key = "table_header") {
                    Column(Modifier.fillMaxWidth().background(Color(0xFF1E123A))) {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp)
                        ) {
                            listOf(false, true).forEach { roles ->
                                val selected = showRoles == roles
                                MainMenuItem(
                                    imageResId = if (selected) R.drawable.storm_standardbutton_blue_hover else R.drawable.storm_standardbutton_blue_normal,
                                    label = stringResource(if (roles) R.string.leaderboards_roles else R.string.leaderboards_stats),
                                    fontSize = 14.sp, textPadding = 4.dp,
                                    textColor = if (selected) Color.White else LeaderboardUnselectedTextColor,
                                    modifier = Modifier.weight(1f).height(48.dp)
                                        .testTag(if (roles) "leaderboard_roles_tab" else "leaderboard_stats_tab")
                                        .selectable(selected, interactionSource = null, indication = null, role = Role.Tab, onClick = { showRoles = roles })
                                )
                            }
                        }
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth().horizontalScroll(horizontal).padding(horizontal = 4.dp)
                        ) {
                            labels.take(if (showRoles) 2 else 6).forEachIndexed { index, label ->
                                val column = LeaderboardSortColumn.entries[index]
                                LeaderboardSortHeader(
                                    column, label, widths[index], layout.style,
                                    selected = sortColumn == column, descending = sortDescending,
                                    onClick = { selectSort(column) }
                                )
                            }
                            if (showRoles) {
                                LeaderboardSortColumn.entries.filter { it.role != null }.forEachIndexed { index, column ->
                                    LeaderboardSortHeader(
                                        column, stringResource(requireNotNull(column.role).label()), widths[index + 2], layout.style,
                                        selected = sortColumn == column, descending = sortDescending,
                                        onClick = { selectSort(column) }
                                    )
                                }
                            }
                        }
                        Spacer(Modifier.height(8.dp))
                    }
                }
                items(sortedPlayers, key = { player -> player.rank }) { player ->
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth().horizontalScroll(horizontal)
                            .background(if (player.rank <= 3) Color(0xAA341A58) else if (player.rank % 2 == 0) Color(0x771A1635) else Color(0x550B081D))
                            .padding(horizontal = 4.dp).testTag("leaderboard_player_" + player.rank)
                    ) {
                        rows.getValue(player.rank).take(if (showRoles) 2 else 6).forEachIndexed { index, value ->
                            LeaderboardCell(value, widths[index], layout.style, alignLeft = index == 1)
                        }
                        if (showRoles) {
                            LeaderboardRole.entries.forEachIndexed { index, role ->
                                LeaderboardCell(player.rolePercentage(role).toString(), widths[index + 2], layout.style, role.color())
                            }
                        }
                    }
                }
            } else {
                item { Text(stringResource(R.string.leaderboards_empty), style = LeaderboardTextStyle, modifier = Modifier.padding(24.dp)) }
            }
        }
    }
}

@Composable
private fun LeaderboardSortHeader(
    column: LeaderboardSortColumn,
    label: String,
    width: Dp,
    style: TextStyle,
    selected: Boolean,
    descending: Boolean,
    onClick: () -> Unit
) {
    val direction = stringResource(if (descending) R.string.leaderboards_sort_descending else R.string.leaderboards_sort_ascending)
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.width(width)
            .selectable(selected, interactionSource = null, indication = null, role = Role.Button, onClick = onClick)
            .semantics { if (selected) stateDescription = direction }
            .testTag("leaderboard_sort_" + column.name.lowercase(Locale.ROOT))
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier.fillMaxWidth().height(48.dp).padding(horizontal = LeaderboardCellPadding)
        ) {
            val role = column.role
            if (role == null) {
                Text(
                    label.replace(' ', '\n'), style = style.copy(color = Color(0xFFD8DCF6)),
                    maxLines = 2, overflow = TextOverflow.Ellipsis, textAlign = TextAlign.Center
                )
            } else {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Image(painterResource(role.icon()), label, modifier = Modifier.size(24.dp))
                    Text(stringResource(R.string.leaderboards_percentage_unit), style = style.copy(color = role.color()))
                }
            }
        }
        // Reserve the arrow's space under every column to keep all headings aligned.
        Box(Modifier.fillMaxWidth().height(22.dp), contentAlignment = Alignment.Center) {
            if (selected) {
                // The up arrow represents each column's default ordering.
                val showUpArrow = if (column.defaultDescending) descending else !descending
                Image(
                    painterResource(if (showUpArrow) R.drawable.storm_ui_glues_replaypanel_icon_sortarrow_up
                        else R.drawable.storm_ui_glues_replaypanel_icon_sortarrow_down),
                    contentDescription = null, contentScale = ContentScale.Fit,
                    modifier = Modifier.size(26.dp, 18.dp)
                )
            }
        }
    }
}

@Composable
private fun LeaderboardCell(text: String, width: Dp, style: TextStyle, color: Color = Color.White, alignLeft: Boolean = false) {
    Box(
        Modifier.width(width).heightIn(min = 44.dp).padding(horizontal = LeaderboardCellPadding, vertical = 8.dp),
        contentAlignment = if (alignLeft) Alignment.CenterStart else Alignment.Center
    ) {
        Text(text, style = style.copy(color = color), maxLines = if ('\n' in text) 2 else 1, overflow = TextOverflow.Ellipsis,
            textAlign = if (alignLeft) TextAlign.Start else TextAlign.Center)
    }
}

@Composable
private fun TopLeaderboardPlayer(player: LeaderboardPlayer, width: Dp, number: NumberFormat) {
    Box(Modifier.width(width).heightIn(min = width * (257f / 175f))) {
        Image(painterResource(R.drawable.leaderboard_top_highlight), null, contentScale = ContentScale.FillBounds, modifier = Modifier.matchParentSize())
        Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp, vertical = 4.dp)) {
            val ringSize = width * 0.88f
            Box(Modifier.size(ringSize), contentAlignment = Alignment.Center) {
                Image(painterResource(R.drawable.leaderboard_top_ring), null, modifier = Modifier.fillMaxSize())
                val rankSize = (width.value * 0.25f).coerceIn(22f, 34f)
                // Desktop website: 50 px top padding + 26 px half-line-height on a 175 px ring.
                OutlinedText(
                    player.rank.toString(),
                    LeaderboardTextStyle.copy(fontSize = rankSize.sp, lineHeight = (rankSize + 6).sp),
                    modifier = Modifier.absoluteOffset(y = ringSize * (76f / 175f - 0.5f))
                )
            }
            Text(player.battleTag, style = LeaderboardTextStyle, maxLines = 1, overflow = TextOverflow.Ellipsis, textAlign = TextAlign.Center)
            Text(stringResource(R.string.leaderboards_player_points, number.format(player.points)),
                style = LeaderboardTextStyle.copy(fontSize = 12.sp, lineHeight = 16.sp, color = Color(0xFFD8DCF6)), maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(pluralStringResource(R.plurals.leaderboards_player_wins, player.wins, player.wins),
                style = LeaderboardTextStyle.copy(fontSize = 12.sp, lineHeight = 16.sp), maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
    }
}

@StringRes
private fun LeaderboardRegion.label() = when (this) {
    LeaderboardRegion.EUROPE -> R.string.leaderboards_europe
    LeaderboardRegion.AMERICAS -> R.string.leaderboards_americas
    LeaderboardRegion.ASIA -> R.string.leaderboards_asia
}

@StringRes
private fun LeaderboardRole.label() = when (this) {
    LeaderboardRole.TANK -> R.string.filter_tank
    LeaderboardRole.BRUISER -> R.string.filter_bruiser
    LeaderboardRole.SUPPORT -> R.string.filter_support
    LeaderboardRole.HEALER -> R.string.filter_healer
    LeaderboardRole.MELEE_ASSASSIN -> R.string.filter_melee
    LeaderboardRole.RANGED_ASSASSIN -> R.string.filter_ranged
}

@DrawableRes
private fun LeaderboardRole.icon() = when (this) {
    LeaderboardRole.TANK -> R.drawable.leaderboard_role_tank
    LeaderboardRole.BRUISER -> R.drawable.leaderboard_role_bruiser
    LeaderboardRole.SUPPORT -> R.drawable.leaderboard_role_support
    LeaderboardRole.HEALER -> R.drawable.leaderboard_role_healer
    LeaderboardRole.MELEE_ASSASSIN -> R.drawable.leaderboard_role_melee
    LeaderboardRole.RANGED_ASSASSIN -> R.drawable.leaderboard_role_ranged
}

private fun LeaderboardRole.color() = when (this) {
    LeaderboardRole.TANK -> Color(0xFFFFEE99)
    LeaderboardRole.BRUISER -> Color(0xFFFF9966)
    LeaderboardRole.SUPPORT -> Color(0xFF99CCFF)
    LeaderboardRole.HEALER -> Color(0xFF99FFBB)
    LeaderboardRole.MELEE_ASSASSIN -> Color(0xFFFF6666)
    LeaderboardRole.RANGED_ASSASSIN -> Color(0xFFCC99FF)
}
