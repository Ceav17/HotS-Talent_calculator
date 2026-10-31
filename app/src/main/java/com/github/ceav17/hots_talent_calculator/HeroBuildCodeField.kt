package com.github.ceav17.hots_talent_calculator

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.absoluteOffset
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.text.input.InputTransformation
import androidx.compose.foundation.text.input.TextFieldLineLimits
import androidx.compose.foundation.text.input.maxLength
import androidx.compose.foundation.text.input.rememberTextFieldState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.AbsoluteAlignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.error
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.drop

@Composable
internal fun HeroBuildCodeField(
    catalog: HeroAbilityCatalog,
    selections: Map<Int, HeroBuildTalentSelection>,
    onImport: (Map<Int, HeroBuildTalentSelection>) -> Unit,
    modifier: Modifier = Modifier
) {
    val state = rememberTextFieldState()
    var focused by remember { mutableStateOf(false) }
    var showError by rememberSaveable { mutableStateOf(false) }
    val importCode by rememberUpdatedState(onImport)
    val heroName = heroBuildCodeName(catalog.heroId)
    val placeholder = formatHeroBuildCode(catalog.heroId, catalog.talentTiers, selections)
    val fieldLabel = stringResource(R.string.hero_build_code_label)
    val errorMessage = stringResource(R.string.hero_build_code_invalid, heroName)
    val focusManager = LocalFocusManager.current
    val keyboardController = LocalSoftwareKeyboardController.current
    val textMeasurer = rememberTextMeasurer()
    val baseStyle = TextStyle(
        color = Color.White, fontFamily = MenuFontFamily, fontWeight = FontWeight.Bold,
        fontSize = 22.sp, lineHeight = 28.sp, letterSpacing = 0.sp, textAlign = TextAlign.Left
    )

    LaunchedEffect(state, catalog) {
        // Restoring the field after scrolling or rotation must not import its old code again.
        snapshotFlow { state.text.toString() }.drop(1).collect { input ->
            val selection = parseHeroBuildCode(input, catalog)
            showError = selection == null && input.trim().endsWith("]")
            if (selection != null) importCode(selection)
        }
    }

    BoxWithConstraints(modifier.widthIn(max = 300.dp).fillMaxWidth()) {
        val panelWidth = maxWidth
        val panelHeight = panelWidth * (89f / 253f)
        val frameInset = panelWidth * (25f / 253f)
        val frameWidth = panelWidth - frameInset * 2
        val frameHeight = panelHeight - frameInset * 2
        val availableWidth = with(LocalDensity.current) { (frameWidth - 12.dp).toPx().coerceAtLeast(1f) }
        val labelWidth = textMeasurer.measure(text = placeholder, style = baseStyle, softWrap = false).size.width
        val textStyle = baseStyle.copy(
            fontSize = baseStyle.fontSize * minOf(1f, availableWidth / labelWidth.coerceAtLeast(1))
        )
        Column {
            Box(Modifier.size(panelWidth, panelHeight)) {
                Image(
                    painterResource(R.drawable.storm_contextmenu_background), null,
                    contentScale = ContentScale.Fit, modifier = Modifier.fillMaxSize()
                )
                if (focused) {
                    Image(
                        painterResource(R.drawable.storm_contextmenu_item_highlight), null,
                        contentScale = ContentScale.FillBounds,
                        modifier = Modifier.absoluteOffset(
                            x = frameInset - frameWidth * (6f / 200f),
                            y = frameInset - frameHeight * (6f / 37f) + 1.dp
                        ).size(frameWidth * (206f / 200f), frameHeight * (47f / 37f))
                    )
                }
                BasicTextField(
                    state = state,
                    inputTransformation = InputTransformation.maxLength(64),
                    outputTransformation = {
                        addStyle(SpanStyle(textDecoration = TextDecoration.None), 0, length)
                    },
                    textStyle = textStyle,
                    cursorBrush = SolidColor(Color.White),
                    lineLimits = TextFieldLineLimits.SingleLine,
                    keyboardOptions = KeyboardOptions(
                        autoCorrectEnabled = false, keyboardType = KeyboardType.Ascii,
                        imeAction = ImeAction.Done
                    ),
                    onKeyboardAction = {
                        showError = state.text.isNotBlank() && parseHeroBuildCode(state.text.toString(), catalog) == null
                        focusManager.clearFocus()
                        keyboardController?.hide()
                    },
                    modifier = Modifier.absoluteOffset(x = frameInset, y = frameInset)
                        .size(frameWidth, frameHeight)
                        .onFocusChanged { focused = it.isFocused }
                        .testTag("hero_build_code")
                        .semantics {
                            contentDescription = fieldLabel
                            if (showError) error(errorMessage)
                        },
                    decorator = { innerTextField ->
                        Box(
                            contentAlignment = AbsoluteAlignment.CenterLeft,
                            modifier = Modifier.fillMaxSize().padding(horizontal = 6.dp)
                        ) {
                            if (!focused && state.text.isEmpty()) {
                                Text(placeholder, style = textStyle.copy(color = Color(0xFFA0A0A0)), maxLines = 1)
                            }
                            innerTextField()
                        }
                    }
                )
            }
            if (showError) {
                Text(
                    errorMessage,
                    style = baseStyle.copy(color = Color(0xFFFF9B9B), fontSize = 14.sp, lineHeight = 18.sp),
                    modifier = Modifier.padding(start = frameInset + 6.dp, end = 6.dp)
                )
            }
        }
    }
}
