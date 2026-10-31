package com.github.ceav17.hots_talent_calculator

import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.annotation.StringRes
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.absoluteOffset
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.AbsoluteAlignment
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp

internal enum class MapMode(val route: String, @StringRes val labelResId: Int) {
    ARAM("maps/aram", R.string.maps_aram),
    QUICKMATCH("maps/quickmatch", R.string.maps_quickmatch),
    RANKED("maps/ranked", R.string.maps_ranked)
}

@Composable
internal fun MapsMenuScreen(
    onModeSelected: (MapMode) -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    ResponsiveScreen(
        backgroundResId = R.drawable.storm_ui_homescreenbackground_nexus,
        backgroundAlignment = AbsoluteAlignment.BottomLeft,
        modifier = modifier
    ) {
        val buttonWidth = minOf(292.dp, maxWidth)
        val buttonHeight = buttonWidth * (108f / 292f)
        val backIconOffset = calculatorBackIconOffset(maxWidth, maxHeight)

        val firstButtonTop = maxOf(
            maxHeight * 0.25f - buttonHeight / 2,
            backIconOffset.y + CalculatorBackIconSize + 8.dp
        )

        Column(
            verticalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier
                .align(Alignment.TopCenter)
                .offset(y = firstButtonTop)
                .heightIn(max = (maxHeight - firstButtonTop).coerceAtLeast(0.dp))
                .verticalScroll(rememberScrollState())
        ) {
            MapMode.entries.forEach { mode ->
                MainMenuItem(
                    imageResId = R.drawable.storm_standardbutton_blue_hover,
                    label = stringResource(mode.labelResId),
                    leadingImageResId = if (mode == MapMode.RANKED) {
                        R.drawable.storm_ui_ranked_button_battlegrounds_selected
                    } else {
                        null
                    },
                    modifier = Modifier
                        .size(buttonWidth, buttonHeight)
                        .clickable(
                            interactionSource = null,
                            indication = null,
                            role = Role.Button,
                            onClick = { onModeSelected(mode) }
                        )
                )
            }
        }
        Image(
            painter = painterResource(R.drawable.storm_ui_glues_button_close_normal),
            contentDescription = stringResource(R.string.talent_calculator_home),
            contentScale = ContentScale.Fit,
            modifier = Modifier
                .align(AbsoluteAlignment.TopLeft)
                .absoluteOffset(x = 24.dp, y = backIconOffset.y)
                .size(CalculatorBackIconSize)
                .clickable(
                    interactionSource = null,
                    indication = null,
                    role = Role.Button,
                    onClick = onBack
                )
        )
    }
}

@Composable
internal fun MapModeScreen(modifier: Modifier = Modifier) {
    Image(
        painter = painterResource(R.drawable.storm_loading_generic),
        contentDescription = null,
        contentScale = ContentScale.Crop,
        alignment = AbsoluteAlignment.TopLeft,
        modifier = modifier.fillMaxSize()
    )
}
