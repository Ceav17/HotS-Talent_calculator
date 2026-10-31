package com.github.ceav17.hots_talent_calculator

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.compose.foundation.Image
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.selection.toggleable
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.Paint
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.toggleableState
import androidx.compose.ui.state.ToggleableState
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

private val FilterButtonWidth = 56.dp
private val FilterButtonHeight = 60.dp
private val FilterColumnSpacing = 4.dp
internal val HeroFiltersWidth = FilterButtonWidth * 2 + FilterColumnSpacing

private data class HeroFilter(
    val value: String,
    @param:StringRes val label: Int,
    @param:DrawableRes val offIcon: Int,
    @param:DrawableRes val onIcon: Int
)

private val roleFilters = listOf(
    HeroFilter("Tank", R.string.filter_tank, R.drawable.storm_ui_play_filter_tank_off, R.drawable.storm_ui_play_filter_tank_on),
    HeroFilter("Bruiser", R.string.filter_bruiser, R.drawable.storm_ui_play_filter_bruiser_off, R.drawable.storm_ui_play_filter_bruiser_on),
    HeroFilter("Ranged Assassin", R.string.filter_ranged, R.drawable.storm_ui_play_filter_ranged_off, R.drawable.storm_ui_play_filter_ranged_on),
    HeroFilter("Melee Assassin", R.string.filter_melee, R.drawable.storm_ui_play_filter_melee_off, R.drawable.storm_ui_play_filter_melee_on),
    HeroFilter("Healer", R.string.filter_healer, R.drawable.storm_ui_play_filter_healer_off, R.drawable.storm_ui_play_filter_healer_on),
    HeroFilter("Support", R.string.filter_support, R.drawable.storm_ui_play_filter_support_off, R.drawable.storm_ui_play_filter_support_on)
)

private val universeFilters = listOf(
    HeroFilter("Warcraft", R.string.filter_warcraft, R.drawable.storm_ui_play_filter_wc_off, R.drawable.storm_ui_play_filter_wc_on),
    HeroFilter("StarCraft", R.string.filter_starcraft, R.drawable.storm_ui_play_filter_sc_off, R.drawable.storm_ui_play_filter_sc_on),
    HeroFilter("Diablo", R.string.filter_diablo, R.drawable.storm_ui_play_filter_d_off, R.drawable.storm_ui_play_filter_d_on),
    HeroFilter("Overwatch", R.string.filter_overwatch, R.drawable.storm_ui_play_filter_ow_off, R.drawable.storm_ui_play_filter_ow_on),
    HeroFilter("Hots", R.string.filter_heroes, R.drawable.storm_ui_play_filter_b_off, R.drawable.storm_ui_play_filter_b_on)
)

private val favoriteFilter = HeroFilter(
    "favorites", R.string.filter_favorites,
    R.drawable.storm_ui_play_filter_favorite_off, R.drawable.storm_ui_play_filter_favorite_on
)

@Composable
internal fun HeroFilters(
    selectedRoles: Set<String>,
    selectedUniverses: Set<String>,
    favoritesOnly: Boolean,
    customGroups: List<HeroCustomGroup>,
    selectedCustomGroups: Set<Int>,
    customGroupActionsEnabled: Boolean,
    onRoleChange: (String, Boolean) -> Unit,
    onUniverseChange: (String, Boolean) -> Unit,
    onFavoritesChange: (Boolean) -> Unit,
    onCustomGroupChange: (Int, Boolean) -> Unit,
    onEditCustomGroup: (Int) -> Unit,
    modifier: Modifier = Modifier,
    onFilterClick: () -> Unit
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Row(horizontalArrangement = Arrangement.spacedBy(FilterColumnSpacing)) {
            FilterColumn(roleFilters, selectedRoles) { value, selected ->
                onRoleChange(value, selected)
                onFilterClick()
            }
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                FilterColumn(universeFilters, selectedUniverses) { value, selected ->
                    onUniverseChange(value, selected)
                    onFilterClick()
                }
                FilterButton(favoriteFilter, selected = favoritesOnly) { selected ->
                    onFavoritesChange(selected)
                    onFilterClick()
                }
            }
        }
        if (customGroups.isNotEmpty()) {
            Spacer(Modifier.height(FilterButtonHeight))
            customGroups.chunked(2).forEachIndexed { rowIndex, row ->
                Row(horizontalArrangement = Arrangement.spacedBy(FilterColumnSpacing)) {
                    row.forEachIndexed { columnIndex, group ->
                        val index = rowIndex * 2 + columnIndex
                        CustomGroupFilterButton(
                            index = index,
                            label = group.label,
                            selected = index in selectedCustomGroups,
                            enabled = customGroupActionsEnabled,
                            onClick = {
                                onCustomGroupChange(index, index !in selectedCustomGroups)
                                onFilterClick()
                            },
                            onLongClick = {
                                onFilterClick()
                                onEditCustomGroup(index)
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun FilterColumn(
    filters: List<HeroFilter>,
    selectedValues: Set<String>,
    onSelectionChange: (String, Boolean) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        filters.forEach { filter ->
            FilterButton(filter, selected = filter.value in selectedValues) { selected ->
                onSelectionChange(filter.value, selected)
            }
        }
    }
}

@Composable
private fun FilterButton(
    filter: HeroFilter,
    selected: Boolean,
    onSelectionChange: (Boolean) -> Unit
) {
    val label = stringResource(filter.label)

    Box(
        modifier = Modifier
            .size(width = FilterButtonWidth, height = FilterButtonHeight)
            .toggleable(
                value = selected,
                role = Role.Checkbox,
                interactionSource = null,
                indication = null,
                onValueChange = onSelectionChange
            )
            .semantics { contentDescription = label },
        contentAlignment = Alignment.Center
    ) {
        FilterBackground(selected)
        if (selected) FilterIcon(filter.offIcon)
        FilterIcon(if (selected) filter.onIcon else filter.offIcon)
    }
}

@Composable
private fun FilterBackground(selected: Boolean) {
    val backgroundOff = R.drawable.storm_ui_play_filter_background_off
    val backgroundOn = R.drawable.storm_ui_play_filter_background_on
    Image(
        painter = painterResource(if (selected) backgroundOff else backgroundOn),
        contentDescription = null,
        contentScale = ContentScale.Fit,
        modifier = Modifier.fillMaxSize()
    )
    Image(
        painter = painterResource(if (selected) backgroundOn else backgroundOff),
        contentDescription = null,
        contentScale = ContentScale.Fit,
        modifier = Modifier.fillMaxSize()
    )
}

@Composable
private fun CustomGroupFilterButton(
    index: Int,
    label: String,
    selected: Boolean,
    enabled: Boolean,
    onClick: () -> Unit,
    onLongClick: () -> Unit
) {
    val description = stringResource(R.string.filter_custom_group, label)
    val editLabel = stringResource(R.string.custom_group_edit)
    val density = LocalDensity.current
    val textMeasurer = rememberTextMeasurer()
    val style = TextStyle(fontFamily = MenuFontFamily, fontWeight = FontWeight.Bold, fontSize = 32.sp)
    val labelSize = remember(label, textMeasurer) {
        textMeasurer.measure(label, style, softWrap = false, maxLines = 1).size
    }
    val fontSize = with(density) {
        val scale = minOf(1f, 36.dp.toPx() / maxOf(labelSize.width, labelSize.height).coerceAtLeast(1))
        (32.sp.toPx() * scale).toSp()
    }
    val labelPaint = remember(selected) {
        Paint().apply { colorFilter = ColorFilter.tint(if (selected) Color.White else Color(0xFF9A9A9A)) }
    }
    Box(
        modifier = Modifier.size(FilterButtonWidth, FilterButtonHeight)
            .testTag("custom_group_filter_$index")
            .semantics {
                contentDescription = description
                toggleableState = if (selected) ToggleableState.On else ToggleableState.Off
            }
            .combinedClickable(
                enabled = enabled,
                role = Role.Checkbox,
                interactionSource = null,
                indication = null,
                onLongClickLabel = editLabel,
                onLongClick = onLongClick,
                onClick = onClick
            ),
        contentAlignment = Alignment.Center
    ) {
        FilterBackground(selected)
        Text(
            text = label,
            style = style.copy(fontSize = fontSize, color = Color.White),
            maxLines = 1,
            softWrap = false,
            // Tint the glyph too when the system fallback font draws a colored emoji.
            modifier = Modifier.drawWithContent {
                drawContext.canvas.saveLayer(Rect(Offset.Zero, size), labelPaint)
                drawContent()
                drawContext.canvas.restore()
            }
        )
    }
}

@Composable
private fun FilterIcon(@DrawableRes icon: Int) {
    val painter = painterResource(icon)
    Image(
        painter = painter,
        contentDescription = null,
        contentScale = ContentScale.Fit,
        // Match the original 56 x 60 background canvas, including each icon's transparent padding.
        modifier = Modifier.size(painter.intrinsicSize.width.dp, painter.intrinsicSize.height.dp)
    )
}
