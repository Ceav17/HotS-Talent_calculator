package com.github.ceav17.hots_talent_calculator

import androidx.compose.foundation.layout.width
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.absoluteOffset
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Slider
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.AbsoluteAlignment
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.roundToInt

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun SettingsScreen(
    visibleCount: Int,
    enabled: Boolean,
    savingCount: Boolean,
    onCountChange: (Int) -> Unit,
    onDataTransfer: () -> Unit,
    dataTransferEnabled: Boolean,
    onAbout: () -> Unit,
    onLegalNotices: () -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    var previewCount by rememberSaveable { mutableIntStateOf(visibleCount) }
    var interacting by remember { mutableStateOf(false) }
    LaunchedEffect(visibleCount, savingCount, interacting) {
        if (!savingCount && !interacting) previewCount = visibleCount
    }
    val sliderDescription = stringResource(R.string.settings_personal_filters)
    ResponsiveScreen(
        backgroundResId = R.drawable.storm_ui_homescreenbackground_nexus,
        backgroundAlignment = AbsoluteAlignment.BottomLeft,
        modifier = modifier
    ) {
        val backOffset = calculatorBackIconOffset(maxWidth, maxHeight)
        val barWidth = minOf(560.dp, maxWidth * 0.9f)
        val barHeight = barWidth * (52f / 602f)
        val menuButtonWidth = minOf(292.dp, barWidth)
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier.align(Alignment.TopCenter)
                .padding(top = backOffset.y + CalculatorBackIconSize + 32.dp)
                .width(barWidth)
                .verticalScroll(rememberScrollState())
                .padding(bottom = 24.dp)
        ) {
            Slider(
                value = previewCount.toFloat(),
                onValueChange = {
                    interacting = true
                    previewCount = it.roundToInt().coerceIn(0, MaxCustomGroups)
                },
                onValueChangeFinished = {
                    onCountChange(previewCount)
                    interacting = false
                },
                enabled = enabled,
                valueRange = 0f..MaxCustomGroups.toFloat(),
                steps = MaxCustomGroups - 1,
                thumb = {},
                track = {
                    Box(Modifier.fillMaxWidth().height(barHeight).testTag("personal_filter_count_track")) {
                        // Reveal part of the full texture, rather than squeezing it into each step.
                        Image(
                            painter = painterResource(R.drawable.hud_bar_cast),
                            contentDescription = null,
                            modifier = Modifier.fillMaxSize().drawWithContent {
                                clipRect(right = size.width * previewCount / MaxCustomGroups) {
                                    this@drawWithContent.drawContent()
                                }
                            }
                        )
                        Image(
                            painter = painterResource(R.drawable.hud_bar_castchannel_bg),
                            contentDescription = null,
                            modifier = Modifier.fillMaxSize()
                        )
                    }
                },
                modifier = Modifier.fillMaxWidth().testTag("personal_filter_count_slider")
                    .semantics { contentDescription = sliderDescription }
            )
            OutlinedText(
                text = stringResource(R.string.settings_personal_filter_count, previewCount, MaxCustomGroups),
                style = TextStyle(
                    color = Color.White,
                    fontFamily = MenuFontFamily,
                    fontWeight = FontWeight.Bold,
                    fontSize = 22.sp,
                    lineHeight = 28.sp,
                    letterSpacing = 0.sp,
                    textAlign = TextAlign.Center
                ),
                modifier = Modifier.fillMaxWidth().testTag("personal_filter_count_label")
            )
            MainMenuItem(
                imageResId = R.drawable.storm_standardbutton_blue_normal,
                label = stringResource(R.string.settings_data_transfer),
                trailingImageResId = R.drawable.ui_editoricon_general_save,
                modifier = Modifier.padding(top = 16.dp)
                    .size(menuButtonWidth, menuButtonWidth * (108f / 292f))
                    .testTag("settings_data_transfer")
                    .clickable(enabled = dataTransferEnabled,
                        interactionSource = null, indication = null, role = Role.Button,
                        onClick = onDataTransfer
                    )
            )
            MainMenuItem(
                imageResId = R.drawable.storm_standardbutton_blue_normal,
                label = stringResource(R.string.settings_about),
                modifier = Modifier
                    .size(menuButtonWidth, menuButtonWidth * (108f / 292f))
                    .testTag("settings_about")
                    .clickable(
                        interactionSource = null, indication = null, role = Role.Button,
                        onClick = onAbout
                    )
            )
            MainMenuItem(
                imageResId = R.drawable.storm_standardbutton_blue_normal,
                label = stringResource(R.string.settings_legal_notices),
                modifier = Modifier
                    .size(menuButtonWidth, menuButtonWidth * (108f / 292f))
                    .testTag("settings_legal_notices")
                    .clickable(
                        interactionSource = null, indication = null, role = Role.Button,
                        onClick = onLegalNotices
                    )
            )
        }
        Image(
            painter = painterResource(R.drawable.storm_ui_glues_button_close_normal),
            contentDescription = stringResource(R.string.talent_calculator_home),
            contentScale = ContentScale.Fit,
            modifier = Modifier.align(AbsoluteAlignment.TopLeft)
                .absoluteOffset(x = 24.dp, y = backOffset.y)
                .size(CalculatorBackIconSize)
                .clickable(
                    interactionSource = null, indication = null, role = Role.Button, onClick = onBack
                )
        )
    }
}
