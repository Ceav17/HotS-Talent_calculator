package com.github.ceav17.hots_talent_calculator

import androidx.annotation.DrawableRes
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.BoxWithConstraintsScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.runtime.Composable
import androidx.compose.ui.AbsoluteAlignment
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp

@Composable
internal fun ResponsiveScreen(
    @DrawableRes backgroundResId: Int,
    modifier: Modifier = Modifier,
    backgroundAlignment: Alignment = AbsoluteAlignment.TopLeft,
    content: @Composable BoxWithConstraintsScope.() -> Unit
) {
    Box(modifier = modifier.fillMaxSize(), contentAlignment = Alignment.TopCenter) {
        Image(
            painter = painterResource(backgroundResId),
            contentDescription = null,
            contentScale = ContentScale.Crop,
            alignment = backgroundAlignment,
            modifier = Modifier.fillMaxSize()
        )
        // Keep controls clear of system UI, and limit line lengths without scaling text or touch targets.
        BoxWithConstraints(
            modifier = Modifier.safeDrawingPadding().widthIn(max = 840.dp).fillMaxSize(),
            content = content
        )
    }
}
