package com.github.ceav17.hots_talent_calculator

import androidx.compose.foundation.Image
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.absoluteOffset
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.text.InlineTextContent
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.Placeholder
import androidx.compose.ui.text.PlaceholderVerticalAlign
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp

internal enum class HeroDetailSection { Abilities, Talents, VoiceLines }

private val HeroDescriptionStyle = TextStyle(
    color = HeroTextColors.Body,
    fontFamily = MenuFontFamily,
    fontWeight = FontWeight.Bold,
    fontSize = 16.sp,
    lineHeight = 22.sp,
    letterSpacing = 0.sp
)

@Composable
internal fun HeroDetails(
    catalog: HeroAbilityCatalog,
    abilities: List<HeroAbility>,
    images: Map<String, ImageBitmap>,
    section: HeroDetailSection,
    tierBarWidth: Dp
) {
    var selectedLevel by rememberSaveable(catalog.heroId) { mutableIntStateOf(1) }
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(20.dp),
        modifier = Modifier.widthIn(max = 640.dp).fillMaxWidth().padding(bottom = 24.dp)
    ) {
        if (section == HeroDetailSection.VoiceLines) {
            HeroVoiceLines(catalog.heroId)
        } else if (section == HeroDetailSection.Abilities) {
            abilities.forEach { ability ->
                HeroDescriptionRow(
                    heroId = catalog.heroId,
                    entryId = ability.id,
                    name = ability.name,
                    description = ability.description,
                    image = images.getValue(ability.iconPath),
                    charges = heroAbilityCharges(ability),
                    modifier = Modifier.testTag("hero_ability_description_${ability.id}")
                )
            }
        } else {
            HeroTalentTierBar(
                tiers = catalog.talentTiers,
                selectedLevel = selectedLevel,
                onSelect = { selectedLevel = it },
                modifier = Modifier.width(tierBarWidth).testTag("hero_talent_tiers")
            )
            catalog.talentTiers.firstOrNull { it.level == selectedLevel }?.talents?.forEach { talent ->
                HeroDescriptionRow(
                    heroId = catalog.heroId,
                    entryId = talent.id,
                    name = talent.name,
                    description = talent.description,
                    image = images.getValue(talent.iconPath),
                    modifier = Modifier.testTag("hero_talent_description_${talent.id}")
                )
            }
        }
    }
}

@Composable
internal fun HeroDescriptionRow(
    heroId: String,
    entryId: String,
    name: String,
    description: String,
    image: ImageBitmap,
    modifier: Modifier = Modifier,
    charges: Int? = null
) {
    val formattedDescription = remember(heroId, entryId, description) {
        heroDescriptionText(heroId, entryId, description)
    }
    val inlineContent = remember {
        mapOf(HeroQuestInlineId to InlineTextContent(
            Placeholder(1.em, 1.em, PlaceholderVerticalAlign.TextCenter)
        ) {
            Image(
                painter = painterResource(R.drawable.storm_ui_hud_log_icon_quest),
                contentDescription = null,
                modifier = Modifier.fillMaxSize()
            )
        })
    }
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        modifier = modifier.fillMaxWidth().padding(horizontal = 8.dp)
    ) {
        HeroAbilityArtwork(
            image = image,
            charges = charges,
            modifier = Modifier.size(64.dp)
        )
        Column(
            verticalArrangement = Arrangement.spacedBy(4.dp),
            modifier = Modifier.weight(1f)
        ) {
            Text(text = name, style = HeroDescriptionStyle.copy(color = HeroTextColors.Name))
            Text(text = formattedDescription, style = HeroDescriptionStyle, inlineContent = inlineContent)
        }
    }
}

@Composable
private fun HeroTalentTierBar(
    tiers: List<HeroTalentTier>,
    selectedLevel: Int,
    onSelect: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        modifier = modifier.selectableGroup()
    ) {
        tiers.forEach { tier ->
            HeroTalentLevelButton(
                level = tier.level,
                selected = tier.level == selectedLevel,
                onSelect = { onSelect(tier.level) },
                modifier = Modifier.weight(1f).aspectRatio(84f / 104f)
                    .testTag("hero_talent_level_${tier.level}")
            )
        }
    }
}

@Composable
internal fun HeroTalentLevelButton(
    level: Int,
    selected: Boolean,
    onSelect: () -> Unit,
    modifier: Modifier = Modifier
) {
    val normalFrame = painterResource(R.drawable.storm_ui_play_heroattributes_panel_talenttier)
    val selectedFrame = painterResource(R.drawable.storm_ui_play_heroattributes_panel_talenttier_selected)
    val textMeasurer = rememberTextMeasurer()
    val numberStyle = HeroDescriptionStyle.copy(fontSize = 18.sp)
    val description = stringResource(R.string.hero_talent_level, level)
    BoxWithConstraints(
        contentAlignment = Alignment.Center,
        modifier = modifier.selectable(
            selected = selected,
            interactionSource = remember { MutableInteractionSource() },
            indication = null,
            role = Role.Tab,
            onClick = onSelect
        ).semantics { contentDescription = description }
            .drawWithCache {
                val label = textMeasurer.measure(level.toString(), numberStyle, maxLines = 1)
                val position = Offset(
                    (size.width - label.size.width) / 2f,
                    (size.height - label.size.height) / 2f
                )
                val outline = Stroke(width = 1.5.dp.toPx())
                onDrawWithContent {
                    drawContent()
                    drawText(label, color = Color.Black, topLeft = position, drawStyle = outline)
                    drawText(
                        label,
                        color = if (selected) Color.White else Color(0xFF9A9A9A),
                        topLeft = position,
                        drawStyle = Fill
                    )
                }
            }
    ) {
        // Balance the gaps around the selected frame, whose glow is asymmetric.
        val frameOffset = if (selected) 0f else -8f
        Image(
            painter = if (selected) selectedFrame else normalFrame,
            contentDescription = null,
            modifier = Modifier
                .requiredSize(maxWidth * (132f / 84f), maxHeight * (136f / 104f))
                .absoluteOffset(x = maxWidth * (frameOffset / 84f))
        )
    }
}
