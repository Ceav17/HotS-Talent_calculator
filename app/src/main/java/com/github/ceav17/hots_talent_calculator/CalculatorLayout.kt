package com.github.ceav17.hots_talent_calculator

import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.dp

internal val HeroFiltersLeftInset = 24.dp
internal val CalculatorBackIconSize = 56.dp

internal fun screenTopSpacing(height: Dp): Dp = (height * 0.05f).coerceIn(8.dp, 32.dp)

// Align the calculator Home button with the search row and filter columns.
internal fun calculatorBackIconOffset(width: Dp, height: Dp): DpOffset {
    val panelHeight = minOf(253.dp, width) * (89f / 253f)
    return DpOffset(
        x = HeroFiltersLeftInset + (HeroFiltersWidth - CalculatorBackIconSize) / 2,
        y = screenTopSpacing(height) + (panelHeight - CalculatorBackIconSize) / 2
    )
}
