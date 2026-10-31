package com.github.ceav17.hots_talent_calculator

import androidx.annotation.DrawableRes
import androidx.compose.foundation.Image
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.zIndex

private data class HeroFormIcons(
    @DrawableRes val normalOff: Int,
    @DrawableRes val normalOn: Int,
    @DrawableRes val alternateOff: Int,
    @DrawableRes val alternateOn: Int
)

private fun heroFormIcons(heroId: String): HeroFormIcons? = when (heroId) {
    "abathur" -> HeroFormIcons(R.drawable.hero_form_abathur_normal_off, R.drawable.hero_form_abathur_normal_on,
        R.drawable.hero_form_abathur_alternate_off, R.drawable.hero_form_abathur_alternate_on)
    "alexstrasza" -> HeroFormIcons(R.drawable.hero_form_alexstrasza_normal_off, R.drawable.hero_form_alexstrasza_normal_on,
        R.drawable.hero_form_alexstrasza_alternate_off, R.drawable.hero_form_alexstrasza_alternate_on)
    "blaze" -> HeroFormIcons(R.drawable.hero_form_blaze_normal_off, R.drawable.hero_form_blaze_normal_on,
        R.drawable.hero_form_blaze_alternate_off, R.drawable.hero_form_blaze_alternate_on)
    "chen" -> HeroFormIcons(R.drawable.hero_form_chen_normal_off, R.drawable.hero_form_chen_normal_on,
        R.drawable.hero_form_chen_alternate_off, R.drawable.hero_form_chen_alternate_on)
    "dva" -> HeroFormIcons(R.drawable.hero_form_dva_normal_off, R.drawable.hero_form_dva_normal_on,
        R.drawable.hero_form_dva_alternate_off, R.drawable.hero_form_dva_alternate_on)
    "deathwing" -> HeroFormIcons(R.drawable.hero_form_deathwing_normal_off, R.drawable.hero_form_deathwing_normal_on,
        R.drawable.hero_form_deathwing_alternate_off, R.drawable.hero_form_deathwing_alternate_on)
    "greymane" -> HeroFormIcons(R.drawable.hero_form_greymane_normal_off, R.drawable.hero_form_greymane_normal_on,
        R.drawable.hero_form_greymane_alternate_off, R.drawable.hero_form_greymane_alternate_on)
    "ragnaros" -> HeroFormIcons(R.drawable.hero_form_ragnaros_normal_off, R.drawable.hero_form_ragnaros_normal_on,
        R.drawable.hero_form_ragnaros_alternate_off, R.drawable.hero_form_ragnaros_alternate_on)
    "tychus" -> HeroFormIcons(R.drawable.hero_form_tychus_normal_off, R.drawable.hero_form_tychus_normal_on,
        R.drawable.hero_form_tychus_alternate_off, R.drawable.hero_form_tychus_alternate_on)
    "valeera" -> HeroFormIcons(R.drawable.hero_form_valeera_normal_off, R.drawable.hero_form_valeera_normal_on,
        R.drawable.hero_form_valeera_alternate_off, R.drawable.hero_form_valeera_alternate_on)
    else -> null
}

@Composable
internal fun HeroFormSelector(
    heroId: String,
    alternateSelected: Boolean,
    iconSize: Dp,
    onSelect: (Boolean) -> Unit
) {
    val icons = heroFormIcons(heroId) ?: return
    val formSize = iconSize * 1.25f
    val formWidth = formSize * (116f / 132f)
    // Exclude transparent padding and shadows so the solid hexagon borders touch.
    val slotWidth = formSize * (94f / 132f)
    Row(Modifier.selectableGroup().testTag("hero_form_selector")) {
        listOf(false, true).forEach { alternate ->
            val selected = alternateSelected == alternate
            val description = stringResource(
                if (alternate) R.string.hero_form_alternate else R.string.hero_form_normal
            )
            val drawable = when {
                alternate && selected -> icons.alternateOn
                alternate -> icons.alternateOff
                selected -> icons.normalOn
                else -> icons.normalOff
            }
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier.size(slotWidth, formSize)
                    .zIndex(if (selected) 1f else 0f)
                    .selectable(
                        selected = selected,
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        role = Role.Tab,
                        onClick = { onSelect(alternate) }
                    )
                    .semantics { contentDescription = description }
                    .testTag(if (alternate) "hero_form_alternate" else "hero_form_normal")
            ) {
                Image(
                    painter = painterResource(drawable),
                    contentDescription = null,
                    modifier = Modifier.requiredSize(formWidth, formSize).then(
                        if (alternate && heroId in setOf("blaze", "chen", "tychus")) {
                            Modifier.heroAbilityHotkey(hotkey = "R", iconSize = iconSize)
                        } else Modifier
                    )
                )
            }
        }
    }
}
