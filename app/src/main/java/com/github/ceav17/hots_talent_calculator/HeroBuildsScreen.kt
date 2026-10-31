package com.github.ceav17.hots_talent_calculator

import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.absoluteOffset
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.DialogProperties
import androidx.compose.ui.window.Popup
import androidx.compose.ui.window.PopupProperties
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.repeatOnLifecycle
import java.io.IOException
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONException

private data class HeroBuildPreviewAssets(
    val tiers: List<HeroTalentTier>,
    val images: Map<String, ImageBitmap>
)

@Composable
internal fun HeroBuildsButton(onClick: () -> Unit, modifier: Modifier = Modifier) {
    BoxWithConstraints(
        contentAlignment = Alignment.Center,
        modifier = modifier.clickable(interactionSource = null, indication = null, role = Role.Button, onClick = onClick)
            .testTag("hero_builds_button")
    ) {
        MainMenuItem(
            imageResId = R.drawable.storm_standardbutton_blue_normal,
            label = stringResource(R.string.hero_builds_button),
            fontSize = 22.sp,
            textPadding = 16.dp,
            modifier = Modifier.fillMaxWidth().height(minOf(112.dp, maxWidth * (108f / 292f)))
        )
    }
}

@Composable
internal fun HeroBuildsScreen(hero: HeroSearchEntry?, onBack: () -> Unit, onBuildSelected: (String) -> Unit) {
    val context = LocalContext.current
    val store = remember(context) { HeroBuildsStore(context) }
    val scope = rememberCoroutineScope()
    val copyBuildCode = rememberBuildCodeCopyAction()
    val heroId = hero?.id
    val assets = context.assets
    val lifecycle = LocalLifecycleOwner.current.lifecycle
    val portrait = remember(heroId) { heroId?.let(::heroSelectionPortrait) }
    val density = LocalDensity.current
    val textMeasurer = rememberTextMeasurer()
    var builds by remember(heroId) { mutableStateOf<List<HeroBuild>?>(null) }
    var loadFailed by remember(heroId) { mutableStateOf(false) }
    var previewAssets by remember(heroId) { mutableStateOf<HeroBuildPreviewAssets?>(null) }
    var previewFailed by remember(heroId) { mutableStateOf(false) }
    var showNewBuild by rememberSaveable(heroId) { mutableStateOf(false) }
    var saving by remember { mutableStateOf(false) }
    var saveFailed by remember { mutableStateOf(false) }
    var duplicateName by remember { mutableStateOf<String?>(null) }
    var pendingDeleteId by rememberSaveable(heroId) { mutableStateOf<String?>(null) }
    var deleteFailed by remember { mutableStateOf(false) }
    var menuBuildId by remember(heroId) { mutableStateOf<String?>(null) }
    var pendingRenameId by rememberSaveable(heroId) { mutableStateOf<String?>(null) }
    LaunchedEffect(heroId, store, lifecycle) {
        if (heroId != null) {
            // Returning from the editor must refresh previews even if this screen stayed composed.
            lifecycle.repeatOnLifecycle(Lifecycle.State.RESUMED) {
                try {
                    builds = store.load(heroId)
                    loadFailed = false
                } catch (_: IOException) {
                    loadFailed = true
                }
            }
        }
    }
    val hasBuilds = builds?.isNotEmpty() == true
    LaunchedEffect(heroId, assets, hasBuilds) {
        if (heroId != null && hasBuilds && previewAssets == null) {
            withContext(Dispatchers.Main.immediate) {
                try {
                    previewAssets = withContext(Dispatchers.IO) {
                        val tiers = loadHeroAbilityCatalog(assets, heroId).talentTiers
                        val paths = tiers.flatMap { tier -> tier.talents.map { it.originalIconPath } }.distinct()
                        // One small set of bitmaps is shared by all builds of this hero.
                        val images = paths.associateWith { path -> loadAssetImage(assets, path) }
                        HeroBuildPreviewAssets(tiers, images)
                    }
                    previewFailed = false
                } catch (_: IOException) {
                    previewFailed = true
                } catch (_: JSONException) {
                    previewFailed = true
                }
            }
        }
    }
    val titleStyle = TextStyle(color = Color.White, fontFamily = MenuFontFamily,
        fontWeight = FontWeight.Bold, fontSize = 22.sp, lineHeight = 28.sp, textAlign = TextAlign.Center)

    ResponsiveScreen(
        backgroundResId = R.drawable.storm_ui_draft_background,
        modifier = Modifier.testTag("hero_builds_screen")
    ) {
        val headerTop = calculatorBackIconOffset(maxWidth, maxHeight).y
        Image(
            painterResource(R.drawable.storm_ui_glues_button_close_normal),
            contentDescription = stringResource(R.string.hero_builds_back),
            modifier = Modifier.absoluteOffset(x = 24.dp, y = headerTop).size(CalculatorBackIconSize)
                .clickable(interactionSource = null, indication = null, role = Role.Button, onClick = onBack)
        )
        val headerLeft = 24.dp + CalculatorBackIconSize + 8.dp
        val headerWidth = (maxWidth - headerLeft - 24.dp).coerceAtLeast(1.dp)
        val nameWidth = (headerWidth - CalculatorBackIconSize - 8.dp).coerceAtLeast(1.dp)
        val heroName = hero?.name.orEmpty()
        val measuredNameWidth = remember(heroName, titleStyle, textMeasurer) {
            textMeasurer.measure(heroName, titleStyle, softWrap = false, maxLines = 1)
                .size.width.coerceAtLeast(1)
        }
        // Keep long names on one line beside the portrait on narrower screens.
        val nameScale = minOf(1f, with(density) { nameWidth.toPx() } / measuredNameWidth)
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.absoluteOffset(x = headerLeft, y = headerTop)
                .size(headerWidth, CalculatorBackIconSize)
        ) {
            OutlinedText(heroName, titleStyle.copy(
                fontSize = titleStyle.fontSize * nameScale,
                lineHeight = titleStyle.lineHeight * nameScale
            ), maxLines = 1, softWrap = false, modifier = Modifier.weight(1f))
            if (portrait != null) {
                Image(
                    painter = painterResource(portrait),
                    contentDescription = stringResource(R.string.hero_page_portrait, heroName),
                    contentScale = ContentScale.Fit,
                    modifier = Modifier.size(CalculatorBackIconSize)
                )
            }
        }
        val canCreate = heroId != null && builds != null && builds.orEmpty().size < MaxHeroBuilds && !saving
        val buttonWidth = minOf(if (maxHeight < 480.dp) 220.dp else 260.dp, maxWidth - 40.dp)
        val listHeight = (maxHeight - headerTop - CalculatorBackIconSize - 12.dp -
            20.dp - 24.dp - 16.dp - buttonWidth * (108f / 292f)).coerceAtLeast(180.dp)
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier.fillMaxSize()
                .padding(top = headerTop + CalculatorBackIconSize + 12.dp, start = 20.dp, end = 20.dp)
                .verticalScroll(rememberScrollState())
                .padding(bottom = 16.dp)
        ) {
            OutlinedText(stringResource(R.string.hero_build_count, builds.orEmpty().size, MaxHeroBuilds),
                titleStyle.copy(fontSize = 16.sp, lineHeight = 20.sp), modifier = Modifier.testTag("hero_build_count"))
            Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxWidth().height(listHeight)
                .background(Color(0xB3170E31)).border(1.dp, Color(0xFF826CD2)).testTag("hero_build_list")) {
                val loaded = builds
                if (loaded.isNullOrEmpty()) {
                    val message = when {
                        loadFailed || heroId == null -> R.string.hero_build_load_failed
                        loaded == null -> R.string.hero_build_loading
                        else -> R.string.hero_build_empty
                    }
                    OutlinedText(stringResource(message), titleStyle.copy(fontSize = 18.sp, lineHeight = 25.sp),
                        modifier = Modifier.padding(24.dp).testTag("hero_build_empty"))
                } else {
                    LazyColumn(modifier = Modifier.fillMaxSize()) {
                        items(loaded, key = { it.id }) { build ->
                            val preview = previewAssets
                            Column(
                                verticalArrangement = Arrangement.spacedBy(4.dp),
                                modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 8.dp)
                                    .testTag("hero_build_${build.id}")
                            ) {
                                FlowRow(
                                    verticalArrangement = Arrangement.spacedBy(4.dp),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Box(
                                        contentAlignment = Alignment.Center,
                                        modifier = Modifier.weight(1f).widthIn(min = 140.dp).heightIn(min = 48.dp)
                                            .testTag("hero_build_name_${build.id}")
                                            .combinedClickable(
                                                enabled = !saving, interactionSource = null, indication = null,
                                                role = Role.Button,
                                                onLongClickLabel = stringResource(R.string.hero_build_rename),
                                                onLongClick = { menuBuildId = build.id },
                                                onClick = { onBuildSelected(build.id) }
                                            )
                                    ) {
                                        OutlinedText(build.name, titleStyle.copy(fontSize = 18.sp, lineHeight = 25.sp))
                                        if (menuBuildId == build.id) {
                                            BuildRenameMenu(
                                                onDismiss = { menuBuildId = null },
                                                onRename = {
                                                    menuBuildId = null
                                                    saveFailed = false
                                                    duplicateName = null
                                                    pendingRenameId = build.id
                                                }
                                            )
                                        }
                                    }
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        HeroBuildCopyButtons(enabled = !saving && heroId != null && preview != null) { format ->
                                            if (heroId != null && preview != null) {
                                                menuBuildId = null
                                                copyBuildCode(formatHeroBuildCode(heroId, preview.tiers, build.talents, format), format)
                                            }
                                        }
                                        Image(
                                            painterResource(R.drawable.storm_ui_profiledialog_portraitchange),
                                            contentDescription = stringResource(R.string.hero_build_rename_action, build.name),
                                            modifier = Modifier.size(48.dp)
                                                .testTag("hero_build_rename_${build.id}")
                                                .clickable(enabled = !saving, interactionSource = null, indication = null,
                                                    role = Role.Button) {
                                                    menuBuildId = null
                                                    saveFailed = false
                                                    duplicateName = null
                                                    pendingRenameId = build.id
                                                }.padding(8.dp)
                                        )
                                        Image(
                                            painterResource(R.drawable.hud_topbar_death_cross_red),
                                            contentDescription = stringResource(R.string.hero_build_delete_action, build.name),
                                            modifier = Modifier.size(48.dp)
                                                .testTag("hero_build_delete_${build.id}")
                                                .clickable(enabled = !saving, interactionSource = null, indication = null, role = Role.Button) {
                                                    deleteFailed = false
                                                    pendingDeleteId = build.id
                                                }.padding(8.dp)
                                        )
                                    }
                                }
                                when {
                                    preview != null -> HeroBuildTalentPreview(
                                        build = build, assets = preview,
                                        modifier = Modifier.fillMaxWidth()
                                            .clickable(enabled = !saving, interactionSource = null, indication = null,
                                                role = Role.Button) { onBuildSelected(build.id) }
                                    )
                                    previewFailed -> OutlinedText(
                                        stringResource(R.string.hero_build_preview_failed),
                                        titleStyle.copy(fontSize = 12.sp, lineHeight = 16.sp)
                                    )
                                    else -> Spacer(Modifier.height(53.dp))
                                }
                            }
                            HorizontalDivider(color = Color(0x667D64B8), modifier = Modifier.padding(horizontal = 16.dp))
                        }
                    }
                }
            }
            MainMenuItem(
                imageResId = R.drawable.storm_standardbutton_blue_normal,
                label = stringResource(R.string.hero_build_new),
                modifier = Modifier.size(buttonWidth, buttonWidth * (108f / 292f))
                    .alpha(if (canCreate) 1f else 0.45f)
                    .testTag("hero_build_new")
                    .clickable(enabled = canCreate, interactionSource = null, indication = null, role = Role.Button) {
                        saveFailed = false
                        duplicateName = null
                        showNewBuild = true
                    }
            )
        }
    }
    if (showNewBuild && heroId != null && builds != null) {
        HeroBuildNameDialog(
            saving = saving,
            saveFailed = saveFailed,
            duplicateName = duplicateName,
            onDismiss = { showNewBuild = false },
            onConfirm = { name ->
                if (!saving && builds.orEmpty().size < MaxHeroBuilds) {
                    saving = true
                    saveFailed = false
                    duplicateName = null
                    scope.launch {
                        try {
                            val updated = store.create(heroId, name)
                            builds = updated
                            showNewBuild = false
                            onBuildSelected(updated.last().id)
                        } catch (_: DuplicateHeroBuildNameException) {
                            duplicateName = name.trim()
                        } catch (_: IOException) {
                            saveFailed = true
                        } finally {
                            saving = false
                        }
                    }
                }
            }
        )
    }
    val buildToRename = builds?.firstOrNull { it.id == pendingRenameId }
    if (buildToRename != null && heroId != null) {
        key(buildToRename.id) {
            HeroBuildNameDialog(
                initialName = buildToRename.name,
                renaming = true,
                saving = saving,
                saveFailed = saveFailed,
                duplicateName = duplicateName,
                onDismiss = { pendingRenameId = null },
                onConfirm = { name ->
                    if (!saving) {
                        saving = true
                        saveFailed = false
                        duplicateName = null
                        scope.launch(start = CoroutineStart.UNDISPATCHED) {
                            // Once confirmed, finish saving even if navigation closes this screen.
                            withContext(NonCancellable) {
                                try {
                                    builds = store.rename(heroId, buildToRename.id, name)
                                    pendingRenameId = null
                                } catch (_: DuplicateHeroBuildNameException) {
                                    duplicateName = name.trim()
                                } catch (_: IOException) {
                                    saveFailed = true
                                } finally {
                                    saving = false
                                }
                            }
                        }
                    }
                }
            )
        }
    }
    val buildToDelete = builds?.firstOrNull { it.id == pendingDeleteId }
    if (buildToDelete != null && heroId != null) {
        DeleteHeroBuildDialog(
            name = buildToDelete.name,
            saving = saving,
            deleteFailed = deleteFailed,
            onDismiss = { pendingDeleteId = null },
            onConfirm = {
                if (!saving) {
                    saving = true
                    deleteFailed = false
                    scope.launch {
                        try {
                            builds = store.delete(heroId, buildToDelete.id)
                            pendingDeleteId = null
                        } catch (_: IOException) {
                            deleteFailed = true
                        } finally {
                            saving = false
                        }
                    }
                }
            }
        )
    }
}

@Composable
private fun BuildRenameMenu(onDismiss: () -> Unit, onRename: () -> Unit) {
    val margin = with(LocalDensity.current) { 8.dp.roundToPx() }
    val positionProvider = remember(margin) { FavoriteMenuPositionProvider(margin) }
    Popup(
        popupPositionProvider = positionProvider,
        onDismissRequest = onDismiss,
        properties = PopupProperties(focusable = true)
    ) {
        HeroMenuAction(
            label = stringResource(R.string.hero_build_rename),
            enabled = true,
            tag = "hero_build_rename_menu",
            onClick = onRename
        )
    }
}

@Composable
private fun HeroBuildTalentPreview(build: HeroBuild, assets: HeroBuildPreviewAssets, modifier: Modifier = Modifier) {
    BoxWithConstraints(modifier.testTag("hero_build_preview_${build.id}")) {
        val spacing = 6.dp
        val slotWidth = ((maxWidth - spacing * 6) / 7).coerceAtLeast(1.dp)
        val iconSize = minOf(36.dp, slotWidth)
        val levelStyle = TextStyle(
            color = Color.White, fontFamily = MenuFontFamily, fontWeight = FontWeight.Bold,
            fontSize = 11.sp, lineHeight = 14.sp, textAlign = TextAlign.Center
        )
        Row(horizontalArrangement = Arrangement.spacedBy(spacing), modifier = Modifier.fillMaxWidth()) {
            assets.tiers.forEach { tier ->
                val preferredId = build.talents[tier.level]?.preferred
                val talent = tier.talents.firstOrNull { it.id == preferredId }
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(3.dp),
                    modifier = Modifier.width(slotWidth).testTag("hero_build_preview_${build.id}_slot_${tier.level}")
                ) {
                    Box(Modifier.size(iconSize)) {
                        if (talent != null) {
                            Image(
                                bitmap = assets.images.getValue(talent.originalIconPath),
                                contentDescription = talent.name, contentScale = ContentScale.Fit,
                                modifier = Modifier.fillMaxSize()
                                    .testTag("hero_build_preview_${build.id}_talent_${tier.level}_${talent.id}")
                            )
                        }
                    }
                    OutlinedText(
                        tier.level.toString(), levelStyle, maxLines = 1, softWrap = false,
                        modifier = Modifier.testTag("hero_build_preview_${build.id}_level_${tier.level}")
                    )
                }
            }
        }
    }
}

@Composable
private fun DeleteHeroBuildDialog(name: String, saving: Boolean, deleteFailed: Boolean, onDismiss: () -> Unit, onConfirm: () -> Unit) {
    AlertDialog(
        onDismissRequest = { if (!saving) onDismiss() },
        properties = DialogProperties(dismissOnBackPress = !saving, dismissOnClickOutside = !saving),
        title = { Text(stringResource(R.string.hero_build_delete_title), fontFamily = MenuFontFamily) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(stringResource(R.string.hero_build_delete_question, name), fontFamily = MenuFontFamily)
                if (deleteFailed) Text(stringResource(R.string.hero_build_delete_failed), color = MaterialTheme.colorScheme.error)
            }
        },
        confirmButton = {
            Text(stringResource(R.string.hero_build_delete_yes), fontFamily = MenuFontFamily,
                color = MaterialTheme.colorScheme.primary.copy(alpha = if (saving) 0.38f else 1f),
                modifier = Modifier.testTag("hero_build_delete_yes")
                    .clickable(enabled = !saving, interactionSource = null, indication = null,
                        role = Role.Button, onClick = onConfirm).padding(16.dp))
        },
        dismissButton = {
            Text(stringResource(R.string.hero_build_delete_no), fontFamily = MenuFontFamily,
                modifier = Modifier.testTag("hero_build_delete_no")
                    .clickable(enabled = !saving, interactionSource = null, indication = null,
                        role = Role.Button, onClick = onDismiss).padding(16.dp))
        }
    )
}

@Composable
private fun HeroBuildNameDialog(
    saving: Boolean,
    saveFailed: Boolean,
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit,
    initialName: String = "",
    renaming: Boolean = false,
    duplicateName: String? = null
) {
    var input by rememberSaveable(initialName, stateSaver = TextFieldValue.Saver) {
        mutableStateOf(TextFieldValue(initialName, selection = TextRange(0, initialName.length)))
    }
    val count = remember(input.text) { buildNameCharacterCount(input.text) }
    val valid = remember(input.text) { isValidBuildName(input.text) }
    val nameTaken = duplicateName != null && input.text.trim().equals(duplicateName, ignoreCase = true)
    val focusRequester = remember { FocusRequester() }
    AlertDialog(
        onDismissRequest = { if (!saving) onDismiss() },
        properties = DialogProperties(dismissOnBackPress = !saving, dismissOnClickOutside = !saving),
        title = {
            Text(stringResource(if (renaming) R.string.hero_build_rename_title else R.string.hero_build_new),
                fontFamily = MenuFontFamily)
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = input,
                    onValueChange = {
                        if (it.text.length <= 2048 && buildNameCharacterCount(it.text) <= MaxBuildNameCharacters) input = it
                    },
                    singleLine = true,
                    enabled = !saving,
                    isError = nameTaken || (input.text.isNotEmpty() && !valid),
                    label = { Text(stringResource(R.string.hero_build_name)) },
                    supportingText = { Text(stringResource(R.string.hero_build_name_hint, count, MaxBuildNameCharacters)) },
                    textStyle = TextStyle(fontFamily = MenuFontFamily, fontSize = 18.sp),
                    keyboardOptions = KeyboardOptions(autoCorrectEnabled = false, imeAction = ImeAction.Done),
                    keyboardActions = KeyboardActions(onDone = { if (valid && !saving) onConfirm(input.text) }),
                    modifier = Modifier.focusRequester(focusRequester).testTag("hero_build_name_input")
                )
                if (nameTaken) {
                    Text(
                        stringResource(R.string.hero_build_duplicate_name),
                        color = MaterialTheme.colorScheme.error,
                        fontFamily = MenuFontFamily,
                        modifier = Modifier.testTag("hero_build_duplicate_name")
                    )
                } else if (saveFailed) {
                    Text(stringResource(R.string.hero_build_save_failed), color = MaterialTheme.colorScheme.error)
                }
            }
        },
        confirmButton = {
            Text(stringResource(if (renaming) R.string.hero_build_rename_save else R.string.hero_build_create),
                fontFamily = MenuFontFamily,
                color = MaterialTheme.colorScheme.primary.copy(alpha = if (valid && !saving) 1f else 0.38f),
                modifier = Modifier.testTag(if (renaming) "hero_build_rename_save" else "hero_build_create")
                    .clickable(enabled = valid && !saving,
                        interactionSource = null, indication = null, role = Role.Button) { onConfirm(input.text) }.padding(16.dp))
        },
        dismissButton = {
            Text(stringResource(R.string.custom_group_cancel), fontFamily = MenuFontFamily,
                modifier = Modifier.clickable(enabled = !saving, interactionSource = null, indication = null,
                    role = Role.Button, onClick = onDismiss).padding(16.dp))
        }
    )
    LaunchedEffect(Unit) { focusRequester.requestFocus() }
}
