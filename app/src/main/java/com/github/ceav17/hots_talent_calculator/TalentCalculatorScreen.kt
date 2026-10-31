package com.github.ceav17.hots_talent_calculator

import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.absoluteOffset
import androidx.compose.foundation.layout.absolutePadding
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.text.input.InputTransformation
import androidx.compose.foundation.text.input.TextFieldLineLimits
import androidx.compose.foundation.text.input.maxLength
import androidx.compose.foundation.text.input.placeCursorAtEnd
import androidx.compose.foundation.text.input.rememberTextFieldState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.listSaver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.AbsoluteAlignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.github.ceav17.hots_talent_calculator.ui.theme.TalentCalculatorHotSTheme

private val HeroFilterSelectionSaver = listSaver<Set<String>, String>(
    save = { it.toList() },
    restore = { it.toSet() }
)

private val CustomFilterSelectionSaver = listSaver<Set<Int>, Int>(
    save = { it.toList() },
    restore = { it.toSet() }
)

@Composable
internal fun TalentCalculatorScreen(
    heroes: Map<String, HeroSearchEntry>,
    onHome: () -> Unit,
    onHeroSelected: (String) -> Unit,
    modifier: Modifier = Modifier,
    favoriteIds: Set<String> = emptySet(),
    favoriteActionsEnabled: Boolean = false,
    onFavoriteChange: (String, Boolean) -> Unit = { _, _ -> },
    customGroups: HeroCustomGroups = HeroCustomGroups(),
    customGroupActionsEnabled: Boolean = false,
    onCustomGroupChange: (Int, HeroCustomGroup) -> Unit = { _, _ -> }
) {
    val searchState = rememberTextFieldState()
    var selectedRoles by rememberSaveable(stateSaver = HeroFilterSelectionSaver) {
        mutableStateOf(emptySet<String>())
    }
    var selectedUniverses by rememberSaveable(stateSaver = HeroFilterSelectionSaver) {
        mutableStateOf(emptySet<String>())
    }
    var favoritesOnly by rememberSaveable { mutableStateOf(false) }
    var selectedCustomGroups by rememberSaveable(stateSaver = CustomFilterSelectionSaver) {
        mutableStateOf(emptySet<Int>())
    }
    var editingCustomGroup by rememberSaveable { mutableStateOf<Int?>(null) }
    var searchFocused by remember { mutableStateOf(false) }
    val searchLabel = stringResource(R.string.talent_calculator_search)
    val focusManager = LocalFocusManager.current
    val keyboardController = LocalSoftwareKeyboardController.current
    val searchTextStyle = TextStyle(
        color = Color.White,
        fontFamily = MenuFontFamily,
        fontWeight = FontWeight.Bold,
        fontSize = 22.sp,
        lineHeight = 28.sp,
        letterSpacing = 0.sp,
        textAlign = TextAlign.Right
    )

    LaunchedEffect(searchFocused) {
        if (searchFocused) {
            // Run after the opening tap places the cursor at the touched position.
            searchState.edit { placeCursorAtEnd() }
        }
    }

    ResponsiveScreen(
        backgroundResId = R.drawable.storm_loading_generic,
        modifier = modifier
            .fillMaxSize()
            .pointerInput(focusManager, keyboardController) {
                // The text field consumes its own taps; background taps dismiss editing.
                detectTapGestures {
                    focusManager.clearFocus()
                    keyboardController?.hide()
                }
            }
    ) {

        val panelWidth = minOf(253.dp, maxWidth)
        val panelHeight = panelWidth * (89f / 253f)
        // Exclude the PNG's 25 px outer glow on each side from the visible frame's margins.
        val frameInset = panelWidth * (25f / 253f)
        val frameWidth = panelWidth - frameInset * 2
        val frameHeight = panelHeight - frameInset * 2
        val panelLeft = (maxWidth - frameWidth) * (4f / 5f) - frameInset
        val panelTop = screenTopSpacing(maxHeight)
        val searchIconSize = 28.dp

        Box(
            modifier = Modifier
                .align(AbsoluteAlignment.TopLeft)
                .absoluteOffset(x = panelLeft, y = panelTop)
                .size(panelWidth, panelHeight)
        ) {
            Image(
                painter = painterResource(R.drawable.storm_contextmenu_background),
                contentDescription = null,
                contentScale = ContentScale.Fit,
                modifier = Modifier.fillMaxSize()
            )
            if (searchFocused) {
                // Map the highlight's visible bounds (6, 6)-(206, 43) onto the frame.
                Image(
                    painter = painterResource(R.drawable.storm_contextmenu_item_highlight),
                    contentDescription = null,
                    contentScale = ContentScale.FillBounds,
                    modifier = Modifier
                        .align(AbsoluteAlignment.TopLeft)
                        .absoluteOffset(
                            x = frameInset - frameWidth * (6f / 200f),
                            y = frameInset - frameHeight * (6f / 37f) + 1.dp
                        )
                        .size(frameWidth * (206f / 200f), frameHeight * (47f / 37f))
                )
            }
            BasicTextField(
                state = searchState,
                inputTransformation = InputTransformation.maxLength(20),
                outputTransformation = {
                    // Hide the composing underline without interrupting the keyboard's composition.
                    addStyle(SpanStyle(textDecoration = TextDecoration.None), 0, length)
                },
                textStyle = searchTextStyle,
                cursorBrush = SolidColor(Color.White),
                lineLimits = TextFieldLineLimits.SingleLine,
                keyboardOptions = KeyboardOptions(
                    autoCorrectEnabled = false,
                    imeAction = ImeAction.Done
                ),
                onKeyboardAction = {
                    focusManager.clearFocus()
                    keyboardController?.hide()
                },
                modifier = Modifier
                    .align(AbsoluteAlignment.TopLeft)
                    .absoluteOffset(x = frameInset, y = frameInset)
                    .size(frameWidth, frameHeight)
                    .onFocusChanged { searchFocused = it.isFocused }
                    .semantics { contentDescription = searchLabel },
                decorator = { innerTextField ->
                    Box(
                        modifier = Modifier.fillMaxSize().padding(horizontal = 6.dp),
                        contentAlignment = AbsoluteAlignment.CenterRight
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .absolutePadding(right = searchIconSize + 6.dp),
                            propagateMinConstraints = true
                        ) {
                            if (!searchFocused && searchState.text.isEmpty()) {
                                Text(
                                    text = searchLabel,
                                    style = searchTextStyle.copy(
                                        color = Color(0xFFA0A0A0),
                                        textAlign = TextAlign.Left
                                    ),
                                    maxLines = 1
                                )
                            }
                            innerTextField()
                        }
                        Image(
                            painter = painterResource(R.drawable.storm_ui_icon_searchglass),
                            contentDescription = null,
                            contentScale = ContentScale.Fit,
                            modifier = Modifier
                                .align(AbsoluteAlignment.CenterRight)
                                .size(searchIconSize)
                        )
                    }
                }
            )
        }

        val heroGridTop = panelTop + frameInset + frameHeight
        val gridHeaderHeight = if (maxHeight < 600.dp) 36.dp else HeroGridHeaderHeight
        val filtersTop = heroGridTop + gridHeaderHeight
        val filtersLeft = HeroFiltersLeftInset
        HeroFilters(
            selectedRoles = selectedRoles,
            selectedUniverses = selectedUniverses,
            favoritesOnly = favoritesOnly,
            customGroups = customGroups.visibleGroups,
            selectedCustomGroups = selectedCustomGroups,
            customGroupActionsEnabled = customGroupActionsEnabled,
            onCustomGroupChange = { index, selected ->
                selectedCustomGroups = if (selected) selectedCustomGroups + index else selectedCustomGroups - index
            },
            onEditCustomGroup = { editingCustomGroup = it },
            onFavoritesChange = { favoritesOnly = it },
            onRoleChange = { role, selected ->
                selectedRoles = if (selected) selectedRoles + role else selectedRoles - role
            },
            onUniverseChange = { universe, selected ->
                selectedUniverses =
                    if (selected) selectedUniverses + universe else selectedUniverses - universe
            },
            modifier = Modifier
                .align(AbsoluteAlignment.TopLeft)
                .absoluteOffset(x = filtersLeft, y = filtersTop)
                .heightIn(max = (maxHeight - filtersTop).coerceAtLeast(0.dp))
                .verticalScroll(rememberScrollState()),
            onFilterClick = {
                focusManager.clearFocus()
                keyboardController?.hide()
            }
        )

        val heroGridLeft = filtersLeft + HeroFiltersWidth + 12.dp
        HeroGrid(
            searchEntries = heroes,
            headerHeight = gridHeaderHeight,
            searchQuery = searchState.text.toString(),
            selectedRoles = selectedRoles,
            selectedUniverses = selectedUniverses,
            favoriteIds = favoriteIds,
            favoritesOnly = favoritesOnly,
            favoriteActionsEnabled = favoriteActionsEnabled,
            customGroups = customGroups,
            selectedCustomGroups = selectedCustomGroups,
            customGroupActionsEnabled = customGroupActionsEnabled,
            onCustomGroupChange = onCustomGroupChange,
            onFavoriteChange = onFavoriteChange,
            onFavoriteMenuOpen = {
                focusManager.clearFocus()
                keyboardController?.hide()
            },
            onHeroSelected = { heroId ->
                focusManager.clearFocus()
                keyboardController?.hide()
                onHeroSelected(heroId)
            },
            modifier = Modifier
                .align(AbsoluteAlignment.TopLeft)
                .absoluteOffset(x = heroGridLeft, y = heroGridTop)
                .size(
                    width = (maxWidth - heroGridLeft - 12.dp).coerceAtLeast(0.dp),
                    height = (maxHeight - heroGridTop).coerceAtLeast(0.dp)
                )
        )

        val homeIconOffset = calculatorBackIconOffset(maxWidth, maxHeight)
        Image(
            painter = painterResource(R.drawable.storm_ui_glues_button_close_normal),
            contentDescription = stringResource(R.string.talent_calculator_home),
            contentScale = ContentScale.Fit,
            modifier = Modifier
                .align(AbsoluteAlignment.TopLeft)
                .absoluteOffset(
                    x = 24.dp,
                    y = homeIconOffset.y
                )
                .size(CalculatorBackIconSize)
                .clickable(
                    interactionSource = null,
                    indication = null,
                    role = Role.Button
                ) {
                    focusManager.clearFocus()
                    keyboardController?.hide()
                    onHome()
                }
        )
    }
    val editingIndex = editingCustomGroup?.takeIf { it in 0 until customGroups.visibleCount }
    if (editingIndex != null) {
        CustomGroupLabelDialog(
            label = customGroups.groups[editingIndex].label,
            onDismiss = { editingCustomGroup = null },
            onSave = { label ->
                editingCustomGroup = null
                onCustomGroupChange(editingIndex, customGroups.groups[editingIndex].copy(label = label))
            }
        )
    }
}

@Preview(name = "Talent calculator - Phone", widthDp = 387, heightDp = 864)
@Preview(name = "Talent calculator - Tablet", widthDp = 1280, heightDp = 800)
@Composable
fun TalentCalculatorPreview() {
    TalentCalculatorHotSTheme {
        TalentCalculatorScreen(heroes = emptyMap(), onHome = {}, onHeroSelected = {})
    }
}
