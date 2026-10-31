package com.github.ceav17.hots_talent_calculator

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.absoluteOffset
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.key
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.github.ceav17.hots_talent_calculator.ui.theme.TalentCalculatorHotSTheme

@Composable
internal fun HeroScreen(
    hero: HeroSearchEntry?,
    heroNames: List<String>,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    onBuilds: () -> Unit = {}
) {
    val density = LocalDensity.current
    val textMeasurer = rememberTextMeasurer()
    val titleStyle = TextStyle(
        color = Color.White,
        fontFamily = MenuFontFamily,
        fontWeight = FontWeight.Bold,
        fontSize = 32.sp,
        letterSpacing = 0.sp,
        textAlign = TextAlign.Center
    )
    // Measure every JSON name with the actual font, so all heroes use the same size.
    val widestName = remember(heroNames, textMeasurer, density) {
        heroNames.maxOfOrNull { name ->
            textMeasurer.measure(name, titleStyle, softWrap = false, maxLines = 1).size.width
        }?.coerceAtLeast(1) ?: 1
    }

    ResponsiveScreen(
        backgroundResId = R.drawable.storm_ui_draft_background,modifier = modifier.fillMaxSize()) {
        val headerOffset = calculatorBackIconOffset(maxWidth, maxHeight)
        val iconLeft = 24.dp
        Image(
            painter = painterResource(R.drawable.storm_ui_glues_button_close_normal),
            contentDescription = stringResource(R.string.hero_page_back),
            contentScale = ContentScale.Fit,
            modifier = Modifier
                .absoluteOffset(x = iconLeft, y = headerOffset.y)
                .size(CalculatorBackIconSize)
                .clickable(
                    interactionSource = null,
                    indication = null,
                    role = Role.Button,
                    onClick = onBack
                )
        )

        if (hero != null) {
            val portraitLeft = headerOffset.x + CalculatorBackIconSize + 12.dp
            val portraitWidth = minOf(340.dp, maxWidth - portraitLeft - 16.dp).coerceAtLeast(1.dp)
            val portraitHeight = portraitWidth * (124f / 340f)
            val portraitTop = headerOffset.y + (CalculatorBackIconSize - portraitHeight) / 2
            Image(
                painter = painterResource(HeroPagePortraits.getValue(hero.id)),
                contentDescription = stringResource(R.string.hero_page_portrait, hero.name),
                contentScale = ContentScale.Fit,
                modifier = Modifier
                    .absoluteOffset(x = portraitLeft, y = portraitTop)
                    .size(portraitWidth, portraitHeight)
                    .testTag("hero_portrait")
            )

            val nameTop = maxOf(portraitTop + portraitHeight, headerOffset.y + CalculatorBackIconSize) + 20.dp
            val nameRowWidth = (maxWidth - iconLeft).coerceAtLeast(1.dp)
            val textWidth = (nameRowWidth - CalculatorBackIconSize - 24.dp).coerceAtLeast(1.dp)
            val fontSize = (32f * with(density) { textWidth.toPx() } / widestName * 0.98f).coerceAtMost(36f).sp
            val nameRowHeight = maxOf(CalculatorBackIconSize, with(density) { fontSize.toDp() } * 1.2f)
            key(hero.id) {
                Column(
                    modifier = Modifier.fillMaxSize().padding(top = nameTop)
                        .verticalScroll(rememberScrollState()).testTag("hero_abilities")
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .padding(start = iconLeft)
                            .fillMaxWidth().height(nameRowHeight)
                    ) {
                        Box(modifier = Modifier.size(CalculatorBackIconSize), contentAlignment = Alignment.Center) {
                            // The full texture includes a soft glow around the universe symbol.
                            Image(
                                painter = painterResource(heroUniverseIcon(hero)),
                                contentDescription = when {
                                    hero.id == "the-lost-vikings" -> stringResource(R.string.hero_universe_classic)
                                    hero.universe == "Hots" -> stringResource(R.string.hero_universe_nexus)
                                    else -> hero.universe
                                },
                                contentScale = ContentScale.Fit,
                                modifier = Modifier.requiredSize(96.dp)
                            )
                        }
                        Box(
                            modifier = Modifier.weight(1f).padding(horizontal = 12.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            OutlinedText(
                                text = hero.name,
                                style = titleStyle.copy(fontSize = fontSize, lineHeight = fontSize * 1.2f),
                                maxLines = 1,
                                softWrap = false,
                                modifier = Modifier.fillMaxWidth().testTag("hero_name")
                            )
                        }
                    }
                    Spacer(Modifier.height(HeroAbilitySectionGap))
                    HeroAbilityGrid(
                        heroId = hero.id,
                        onBuilds = onBuilds,
                        scrollable = false,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        }
    }
}

@Preview(name = "Hero - Small Phone", widthDp = 360, heightDp = 400)
@Preview(name = "Hero - Phone", widthDp = 387, heightDp = 864)
@Preview(name = "Hero - Tablet", widthDp = 1280, heightDp = 800)
@Composable
private fun HeroScreenPreview() {
    TalentCalculatorHotSTheme {
        HeroScreen(
            hero = HeroSearchEntry("the-lost-vikings", "The Lost Vikings", "Support", "Hots"),
            heroNames = listOf("The Lost Vikings", "Abathur"),
            onBack = {}
        )
    }
}
