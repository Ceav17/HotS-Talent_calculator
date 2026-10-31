package com.github.ceav17.hots_talent_calculator

import androidx.annotation.DrawableRes
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.absoluteOffset
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalWindowInfo
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntRect
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Popup
import androidx.compose.ui.window.PopupPositionProvider
import androidx.compose.ui.window.PopupProperties

@Composable
internal fun HeroFavoriteBadge(heroId: String, portraitSize: Dp, glowVisible: Boolean) {
    val badgeSize = portraitSize * 0.40f
    // The badge center lies on the circle at 55 minutes (11 o'clock).
    Box(
        modifier = Modifier
            .absoluteOffset(
                x = portraitSize * 0.25f - badgeSize / 2,
                y = portraitSize * 0.067f - badgeSize / 2
            )
            .size(badgeSize)
            .testTag("hero_favorite_badge_$heroId")
    ) {
        if (glowVisible) {
            Image(
                painter = painterResource(R.drawable.storm_ui_play_favorite_logoglow),
                contentDescription = null,
                modifier = Modifier.fillMaxSize().testTag("hero_favorite_glow_$heroId")
            )
        }
        Image(
            painter = painterResource(R.drawable.storm_ui_play_favorite_logo),
            contentDescription = null,
            modifier = Modifier.fillMaxSize()
        )
    }
}

@Composable
internal fun HeroPortraitMenu(
    favorite: Boolean,
    heroId: String,
    customGroups: List<HeroCustomGroup>,
    favoriteEnabled: Boolean,
    customGroupEnabled: Boolean,
    onDismiss: () -> Unit,
    onFavoriteClick: () -> Unit,
    onCustomGroupClick: (Int) -> Unit
) {
    val margin = with(LocalDensity.current) { 8.dp.roundToPx() }
    val positionProvider = remember(margin) { FavoriteMenuPositionProvider(margin) }
    val windowSize = LocalWindowInfo.current.containerSize
    val maxHeight = with(LocalDensity.current) {
        (windowSize.height.toDp() - 32.dp).coerceAtLeast(54.dp)
    }
    Popup(
        popupPositionProvider = positionProvider,
        onDismissRequest = onDismiss,
        properties = PopupProperties(focusable = true)
    ) {
        Column(Modifier.heightIn(max = maxHeight).verticalScroll(rememberScrollState())) {
            HeroMenuAction(
                label = stringResource(if (favorite) R.string.hero_favorite_remove else R.string.hero_favorite_add),
                icon = if (favorite) R.drawable.storm_ui_cursor_favorite_remove
                    else R.drawable.storm_ui_cursor_favorite_add,
                enabled = favoriteEnabled,
                tag = "hero_favorite_action",
                onClick = onFavoriteClick
            )
            customGroups.forEachIndexed { index, group ->
                val inGroup = heroId in group.heroIds
                HeroMenuAction(
                    label = stringResource(
                        if (inGroup) R.string.hero_custom_group_remove else R.string.hero_custom_group_add,
                        group.label
                    ),
                    icon = if (inGroup) R.drawable.hud_topbar_death_cross_red else null,
                    enabled = customGroupEnabled,
                    tag = "hero_custom_group_action_$index",
                    onClick = { onCustomGroupClick(index) }
                )
            }
        }
    }
}

@Composable
internal fun HeroMenuAction(
    label: String,
    enabled: Boolean,
    tag: String,
    @DrawableRes icon: Int? = null,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier.size(264.dp, 54.dp)
            .testTag(tag)
            .clickable(
                interactionSource = null,
                indication = null,
                enabled = enabled,
                role = Role.Button,
                onClick = onClick
            )
    ) {
        Image(
            painter = painterResource(R.drawable.storm_ui_ingame_talentpanel_talentavailable),
            contentDescription = null,
            contentScale = ContentScale.FillBounds,
            modifier = Modifier.fillMaxSize()
        )
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.fillMaxSize().padding(horizontal = 14.dp)
        ) {
            if (icon != null) Image(
                painter = painterResource(icon),
                contentDescription = null,
                modifier = Modifier.size(24.dp)
            )
            Text(
                text = label,
                color = Color.White,
                fontFamily = MenuFontFamily,
                fontWeight = FontWeight.Bold,
                fontSize = 16.sp,
                lineHeight = 20.sp
            )
        }
    }
}

internal class FavoriteMenuPositionProvider(private val margin: Int) : PopupPositionProvider {
    override fun calculatePosition(
        anchorBounds: IntRect,
        windowSize: IntSize,
        layoutDirection: LayoutDirection,
        popupContentSize: IntSize
    ): IntOffset {
        val x = (anchorBounds.left + (anchorBounds.width - popupContentSize.width) / 2)
            .coerceIn(margin, (windowSize.width - popupContentSize.width - margin).coerceAtLeast(margin))
        val below = anchorBounds.bottom + margin
        val y = if (below + popupContentSize.height <= windowSize.height - margin) below
        else anchorBounds.top - popupContentSize.height - margin
        return IntOffset(
            x,
            y.coerceIn(margin, (windowSize.height - popupContentSize.height - margin).coerceAtLeast(margin))
        )
    }
}
