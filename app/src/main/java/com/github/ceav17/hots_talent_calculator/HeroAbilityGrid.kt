package com.github.ceav17.hots_talent_calculator

import androidx.annotation.DrawableRes
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.verticalScroll
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
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

internal val HeroAbilitySectionGap = 16.dp
private val PassiveHotkeyColor = Color(0xFF9A9A9A)

private data class HeroAbilityImages(
    val catalog: HeroAbilityCatalog,
    val forms: HeroAbilityForms,
    val utilities: HeroUtilityAbilities,
    val images: Map<String, ImageBitmap>
)

@Composable
internal fun HeroAbilityGrid(
    heroId: String,
    modifier: Modifier = Modifier,
    onBuilds: () -> Unit = {},
    scrollable: Boolean = true
) {
    val assets = LocalContext.current.assets
    val content by produceState<HeroAbilityImages?>(null, heroId, assets) {
        value = withContext(Dispatchers.IO) {
            val catalog = loadHeroAbilityCatalog(assets, heroId)
            val forms = heroAbilityForms(catalog)
            val utilities = heroUtilityAbilities(catalog)
            val pages = listOfNotNull(forms.normal, forms.alternate)
            val paths = ((pages.flatMap { page -> page.rows.flatMap { it.slots } + page.descriptions } +
                listOf(utilities.mount, utilities.hearthstone))
                .filterNotNull().map { it.iconPath } +
                catalog.talentTiers.flatMap { tier -> tier.talents.map { it.iconPath } }).distinct()
            val images = paths.associateWith { path -> loadAssetImage(assets, path) }
            HeroAbilityImages(catalog, forms, utilities, images)
        }
    }
    val loaded = content ?: return
    var selectedSection by rememberSaveable(heroId) { mutableStateOf(HeroDetailSection.Abilities) }
    var alternateSelected by rememberSaveable(heroId) { mutableStateOf(false) }
    val page = if (alternateSelected) loaded.forms.alternate ?: loaded.forms.normal else loaded.forms.normal
    BoxWithConstraints(modifier = modifier.padding(horizontal = 8.dp)) {
        val iconSize = minOf(maxWidth / 6, 80.dp)
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier
                .fillMaxWidth()
                .then(
                    if (scrollable) Modifier.verticalScroll(rememberScrollState()).testTag("hero_abilities")
                    else Modifier
                )
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                page.rows.forEachIndexed { rowIndex, row ->
                    Row {
                        row.slots.forEachIndexed { column, ability ->
                            if (ability == null) {
                                Spacer(Modifier.size(iconSize))
                            } else {
                                HeroAbilityIcon(
                                    ability = ability,
                                    image = loaded.images.getValue(ability.iconPath),
                                    iconSize = iconSize,
                                    keyColor = hotkeyColor(ability, loaded.catalog),
                                    charges = heroAbilityCharges(ability),
                                    talentRequirement = when (ability.abilityId) {
                                        "FirebatBunkerDropFortifiedBunkerOilSpill" -> "20(R)"
                                        "DVaPilotTorpedoDash", "DVaPilotConcussivePulse" -> "13"
                                        else -> null
                                    },
                                    modifier = Modifier.testTag("hero_ability_${rowIndex}_$column")
                                )
                            }
                        }
                    }
                }
            }
            Spacer(Modifier.height(HeroAbilitySectionGap))
            HeroControls(
                loaded = loaded,
                iconSize = iconSize,
                selectedSection = selectedSection,
                onSectionSelected = { selectedSection = it },
                alternateSelected = alternateSelected,
                onFormSelected = { alternateSelected = it },
                onBuilds = onBuilds
            )
            Spacer(Modifier.height(HeroAbilitySectionGap))
            HeroDetails(
                catalog = loaded.catalog,
                abilities = page.descriptions,
                images = loaded.images,
                section = selectedSection,
                tierBarWidth = iconSize * 6
            )
        }
    }
}

@Composable
private fun HeroControls(
    loaded: HeroAbilityImages,
    iconSize: Dp,
    selectedSection: HeroDetailSection,
    onSectionSelected: (HeroDetailSection) -> Unit,
    alternateSelected: Boolean,
    onFormSelected: (Boolean) -> Unit,
    onBuilds: () -> Unit
) {
    val context = LocalContext.current
    val spotlightVideoId = HeroSpotlightVideoIds[loaded.catalog.heroId]
    val tabSize = iconSize * 1.8816f
    val utilityScale = 0.75f
    val slotWidth = iconSize * 1.5f
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier.width(iconSize * 6)
    ) {
        Row(
            horizontalArrangement = Arrangement.Start,
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth().testTag("hero_utilities")
        ) {
            listOf(loaded.utilities.mount, loaded.utilities.hearthstone).forEach { ability ->
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier.size(slotWidth, iconSize)
                ) {
                    if (ability != null) {
                        val image = loaded.images.getValue(ability.iconPath)
                        val aspectRatio = image.width.toFloat() / image.height
                        val width = minOf(slotWidth, iconSize * aspectRatio) * utilityScale
                        val height = width / aspectRatio
                        HeroAbilityIcon(
                            ability = ability,
                            image = image,
                            iconSize = iconSize,
                            imageWidth = width,
                            imageHeight = height,
                            keyAtBottom = true,
                            keyColor = hotkeyColor(ability, loaded.catalog),
                            modifier = Modifier.testTag("hero_utility_${ability.hotkey}")
                        )
                    }
                }
            }
            if (loaded.forms.alternate != null) {
                Box(contentAlignment = Alignment.Center, modifier = Modifier.weight(1f)) {
                    HeroFormSelector(
                        heroId = loaded.catalog.heroId,
                        alternateSelected = alternateSelected,
                        iconSize = iconSize,
                        onSelect = onFormSelected
                    )
                }
            }
        }
        HeroBuildsButton(
            onClick = onBuilds,
            modifier = Modifier.fillMaxWidth(0.96f)
        )
        Row(
            horizontalArrangement = Arrangement.Start,
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth()
                .horizontalScroll(rememberScrollState())
                .selectableGroup()
                .testTag("hero_controls")
        ) {
            HeroSectionTab(
                selected = selectedSection == HeroDetailSection.Abilities,
                normalIcon = R.drawable.storm_ui_play_heroattributes_button_abilities_normal,
                selectedIcon = R.drawable.storm_ui_play_heroattributes_button_abilities_selected,
                description = stringResource(R.string.hero_tab_abilities),
                iconSize = tabSize,
                onClick = { onSectionSelected(HeroDetailSection.Abilities) },
                modifier = Modifier.size(slotWidth, tabSize).testTag("hero_tab_abilities")
            )
            HeroSectionTab(
                selected = selectedSection == HeroDetailSection.Talents,
                normalIcon = R.drawable.storm_ui_play_heroattributes_button_talents_normal,
                selectedIcon = R.drawable.storm_ui_play_heroattributes_button_talents_selected,
                description = stringResource(R.string.hero_tab_talents),
                iconSize = tabSize,
                onClick = { onSectionSelected(HeroDetailSection.Talents) },
                modifier = Modifier.size(slotWidth, tabSize).testTag("hero_tab_talents")
            )
            HeroSectionTab(
                selected = selectedSection == HeroDetailSection.VoiceLines,
                normalIcon = R.drawable.storm_heroselect_loadout_typeicon_voice_normal,
                selectedIcon = R.drawable.storm_heroselect_loadout_typeicon_voice_active,
                description = stringResource(R.string.hero_tab_voice_lines),
                // This sprite has no outer padding, unlike the two 156px tab images.
                iconSize = tabSize * (128f / 156f),
                onClick = { onSectionSelected(HeroDetailSection.VoiceLines) },
                modifier = Modifier.size(slotWidth, tabSize).testTag("hero_tab_voice_lines")
            )
            if (spotlightVideoId != null) {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .size(slotWidth, tabSize)
                        .testTag("hero_spotlight_youtube")
                        .clickable(
                            interactionSource = null,
                            indication = null,
                            role = Role.Button,
                            onClick = { openHeroSpotlight(context, spotlightVideoId) }
                        )
                ) {
                    Image(
                        painter = painterResource(R.drawable.yt_icon_red_digital),
                        contentDescription = stringResource(R.string.hero_spotlight_youtube),
                        // Match the tab artwork while preserving the logo's transparent margins.
                        modifier = Modifier.requiredSize(tabSize)
                    )
                }
            }
        }
    }
}

@Composable
private fun HeroSectionTab(
    selected: Boolean,
    @DrawableRes normalIcon: Int,
    @DrawableRes selectedIcon: Int,
    description: String,
    iconSize: Dp,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier
            .selectable(
                selected = selected,
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                role = Role.Tab,
                onClick = onClick
            )
            .semantics { contentDescription = description }
    ) {
        // Transparent PNG padding can extend beyond the fixed-width slot.
        Image(
            painter = painterResource(normalIcon),
            contentDescription = null,
            modifier = Modifier.requiredSize(iconSize).zIndex(if (selected) 0f else 1f)
        )
        Image(
            painter = painterResource(selectedIcon),
            contentDescription = null,
            modifier = Modifier.requiredSize(iconSize).zIndex(if (selected) 1f else 0f)
        )
    }
}

private fun hotkeyColor(ability: HeroAbility, catalog: HeroAbilityCatalog) =
    if (hasPassiveHotkey(ability, catalog)) PassiveHotkeyColor else Color.White

@Composable
private fun HeroAbilityIcon(
    ability: HeroAbility,
    image: ImageBitmap,
    iconSize: Dp,
    modifier: Modifier = Modifier,
    keyColor: Color = Color.White,
    charges: Int? = null,
    imageWidth: Dp = iconSize,
    imageHeight: Dp = iconSize,
    keyAtBottom: Boolean = false,
    talentRequirement: String? = null
) {
    val chargeDescription = charges?.let { pluralStringResource(R.plurals.hero_ability_charges, it, it) }
    Box(
        modifier = modifier
            .size(imageWidth, imageHeight)
            .clearAndSetSemantics {
                contentDescription = "${ability.name} (${ability.hotkey})" +
                    (chargeDescription?.let { ", $it" } ?: "") +
                    (talentRequirement?.let { ", $it" } ?: "")
            }
            .heroAbilityHotkey(
                hotkey = ability.hotkey,
                iconSize = iconSize,
                color = keyColor,
                // Utility keys follow the short bottom edge of their slanted frame.
                horizontalAnchor = when {
                    !keyAtBottom -> 0.28f
                    "_mount" in ability.iconPath -> 0.36f
                    "_miscrune" in ability.iconPath -> 0.64f
                    else -> 0.5f
                },
                verticalAnchor = if (keyAtBottom) 0.78f else 0.75f
            )
            .then(
                if (talentRequirement == null) Modifier else Modifier.heroAbilityHotkey(
                    hotkey = talentRequirement,
                    iconSize = iconSize,
                    horizontalAnchor = 0.72f,
                    verticalAnchor = 0.25f,
                    fontScale = if (talentRequirement.length > 2) 0.18f else 0.25f
                )
            )
    ) {
        HeroAbilityArtwork(
            image = image,
            charges = charges,
            modifier = Modifier.fillMaxSize()
        )
    }
}
