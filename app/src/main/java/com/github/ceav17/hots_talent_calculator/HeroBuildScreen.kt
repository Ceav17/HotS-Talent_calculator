package com.github.ceav17.hots_talent_calculator

import android.widget.Toast
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.absoluteOffset
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.selection.toggleable
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.ColorMatrix
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.imageResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.sp
import java.io.IOException
import kotlin.math.roundToInt
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONException

// Share each original PNG across heroes and tiers, without changing their exported JSON.
internal val HeroTalent.originalIconPath: String
    get() = "talent_icons/${iconPath.substringAfterLast('/')}"

private data class HeroBuildContent(
    val build: HeroBuild,
    val catalog: HeroAbilityCatalog,
    val images: Map<String, ImageBitmap>
)

private val UnselectedBuildTalentFilter =
    ColorFilter.colorMatrix(ColorMatrix().apply { setToSaturation(0f) })
private val PreferredBuildTalentBorderFilter =
    ColorFilter.tint(Color(0xFF39F739), BlendMode.Modulate)
private val AlternativeBuildTalentBorderFilter =
    ColorFilter.tint(Color(0xFFFF8A24), BlendMode.Modulate)

@Composable
internal fun HeroBuildScreen(heroId: String, buildId: String, onBack: () -> Unit) {
    val context = LocalContext.current
    val assets = context.assets
    val store = remember(context) { HeroBuildsStore(context) }
    val scope = rememberCoroutineScope()
    val copyBuildCode = rememberBuildCodeCopyAction()
    var editedBuild by remember(heroId, buildId) { mutableStateOf<HeroBuild?>(null) }
    var loadFailed by remember(heroId, buildId) { mutableStateOf(false) }
    val focusManager = LocalFocusManager.current
    val keyboardController = LocalSoftwareKeyboardController.current
    fun saveBuildChange(change: suspend () -> HeroBuild) {
        // Queue imports and taps together; finish their writes even when navigating back.
        scope.launch(start = CoroutineStart.UNDISPATCHED) {
            withContext(NonCancellable) {
                try {
                    editedBuild = change()
                } catch (_: IOException) {
                    Toast.makeText(context.applicationContext,
                        R.string.hero_build_save_failed, Toast.LENGTH_LONG).show()
                }
            }
        }
    }
    val content by produceState<HeroBuildContent?>(null, heroId, buildId, assets, store) {
        // Publish on the UI thread even when the effect resumes during activity recreation.
        withContext(Dispatchers.Main.immediate) {
            value = try {
                withContext(Dispatchers.IO) {
                    val build = store.load(heroId).firstOrNull { it.id == buildId }
                        ?: throw IOException("Build not found: $buildId")
                    val catalog = loadHeroAbilityCatalog(assets, heroId)
                    val paths = catalog.talentTiers.flatMap { tier -> tier.talents.map { it.originalIconPath } }.distinct()
                    val images = paths.associateWith { path -> loadAssetImage(assets, path) }
                    HeroBuildContent(build, catalog, images)
                }
            } catch (_: IOException) {
                loadFailed = true
                null
            } catch (_: JSONException) {
                loadFailed = true
                null
            }
        }
    }
    var selectedLevel by rememberSaveable(heroId, buildId) { mutableIntStateOf(1) }
    val titleStyle = TextStyle(
        color = Color.White, fontFamily = MenuFontFamily, fontWeight = FontWeight.Bold,
        fontSize = 20.sp, lineHeight = 24.sp, textAlign = TextAlign.Center
    )

    ResponsiveScreen(
        backgroundResId = R.drawable.storm_ui_draft_background,
        modifier = Modifier.testTag("hero_build_screen")
            .pointerInput(focusManager, keyboardController) {
                detectTapGestures {
                    focusManager.clearFocus()
                    keyboardController?.hide()
                }
            }
    ) {
        val headerTop = calculatorBackIconOffset(maxWidth, maxHeight).y
        val headerLeft = 24.dp + CalculatorBackIconSize + 8.dp
        Image(
            painterResource(R.drawable.storm_ui_glues_button_close_normal),
            contentDescription = stringResource(R.string.hero_build_back),
            modifier = Modifier.absoluteOffset(x = 24.dp, y = headerTop).size(CalculatorBackIconSize)
                .clickable(interactionSource = null, indication = null, role = Role.Button, onClick = onBack)
        )
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier.absoluteOffset(x = headerLeft, y = headerTop)
                .size((maxWidth - headerLeft - 24.dp).coerceAtLeast(1.dp), CalculatorBackIconSize)
        ) {
            OutlinedText(content?.build?.name.orEmpty(), titleStyle, maxLines = 2)
        }
        val contentTop = headerTop + CalculatorBackIconSize + 16.dp
        val loaded = content
        if (loaded == null) {
            Box(contentAlignment = Alignment.Center,
                modifier = Modifier.fillMaxSize().padding(top = contentTop, start = 24.dp, end = 24.dp)) {
                OutlinedText(stringResource(
                    if (loadFailed) R.string.hero_build_detail_failed else R.string.hero_build_detail_loading
                ), titleStyle)
            }
        } else {
            val tier = loaded.catalog.talentTiers.firstOrNull { it.level == selectedLevel }
                ?: loaded.catalog.talentTiers.first()
            LazyColumn(
                contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 8.dp, bottom = 24.dp),
                verticalArrangement = Arrangement.spacedBy(20.dp),
                modifier = Modifier.align(Alignment.TopCenter).widthIn(max = 640.dp).fillMaxSize()
                    .padding(top = contentTop)
                        .testTag("build_content")
            ) {
                item(key = "code") {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        // Two 48dp actions beside the existing field, which is at most 300dp wide.
                        modifier = Modifier.widthIn(max = 396.dp).fillMaxWidth()
                    ) {
                        HeroBuildCopyButtons { format ->
                            copyBuildCode(formatHeroBuildCode(
                                heroId, loaded.catalog.talentTiers,
                                (editedBuild ?: loaded.build).talents, format
                            ), format)
                        }
                        HeroBuildCodeField(
                            catalog = loaded.catalog,
                            selections = (editedBuild ?: loaded.build).talents,
                            onImport = { selections ->
                                saveBuildChange { store.replaceTalents(heroId, buildId, selections) }
                            },
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
                item(key = "tiers") {
                    HeroBuildTalentRows(
                        content = loaded, build = editedBuild ?: loaded.build,
                        selectedLevel = tier.level,
                        onSelect = { selectedLevel = it },
                        onToggleTalent = { level, talentId ->
                            saveBuildChange { store.toggleTalent(heroId, buildId, level, talentId) }
                        }
                    )
                }
                items(tier.talents, key = { "${tier.level}:${it.id}" }) { talent ->
                    HeroDescriptionRow(
                        heroId = heroId, entryId = talent.id,
                        name = talent.name, description = talent.description,
                        image = loaded.images.getValue(talent.originalIconPath),
                        modifier = Modifier.testTag("build_talent_description_${talent.id}")
                    )
                }
            }
        }
    }
}

@Composable
private fun HeroBuildTalentRows(
    content: HeroBuildContent,
    build: HeroBuild,
    selectedLevel: Int,
    onSelect: (Int) -> Unit,
    onToggleTalent: (Int, String) -> Unit
) {
    val preferredBorder = ImageBitmap.imageResource(R.drawable.storm_temp_btn_border_transportslot_filled)
    val alternativeBorder = ImageBitmap.imageResource(R.drawable.storm_temp_btn_border_research)
    BoxWithConstraints(Modifier.fillMaxWidth()) {
        val levelWidth = 44.dp
        val slots = maxOf(5, content.catalog.talentTiers.maxOf { it.talents.size })
        val iconSize = minOf(52.dp, (maxWidth - levelWidth - 12.dp - 6.dp * (slots - 1)) / slots)
            .coerceAtLeast(1.dp)
        Column(
            verticalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.selectableGroup().testTag("build_talent_rows")
        ) {
            content.catalog.talentTiers.forEach { tier ->
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.fillMaxWidth().testTag("build_talent_row_${tier.level}")
                ) {
                    HeroTalentLevelButton(
                        level = tier.level, selected = tier.level == selectedLevel,
                        onSelect = { onSelect(tier.level) },
                        modifier = Modifier.width(levelWidth).aspectRatio(84f / 104f)
                            .testTag("build_talent_level_${tier.level}")
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        val selection = build.talents[tier.level]
                        tier.talents.forEach { talent ->
                            HeroBuildTalentIcon(
                                talent = talent,
                                image = content.images.getValue(talent.originalIconPath),
                                iconSize = iconSize,
                                selection = selection,
                                preferredBorder = preferredBorder,
                                alternativeBorder = alternativeBorder,
                                onClick = { onToggleTalent(tier.level, talent.id) }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun HeroBuildTalentIcon(
    talent: HeroTalent,
    image: ImageBitmap,
    iconSize: Dp,
    selection: HeroBuildTalentSelection?,
    preferredBorder: ImageBitmap,
    alternativeBorder: ImageBitmap,
    onClick: () -> Unit
) {
    val preferred = selection?.preferred == talent.id
    val alternative = selection?.alternatives?.contains(talent.id) == true
    val stateLabel = stringResource(when {
        preferred -> R.string.hero_build_talent_preferred
        alternative -> R.string.hero_build_talent_alternative
        else -> R.string.hero_build_talent_unselected
    })
    Box(
        modifier = Modifier.size(iconSize)
            .testTag("build_talent_icon_${talent.id}")
            .toggleable(
                value = preferred || alternative, role = Role.Checkbox,
                interactionSource = null, indication = null, onValueChange = { onClick() }
            )
            .semantics {
                contentDescription = talent.name
                stateDescription = stateLabel
            }
    ) {
        if (preferred || alternative) {
            Canvas(Modifier.matchParentSize()) {
                val inset = 4.dp.toPx()
                // Both 64px frames have a 48px opening; keep the entire illustration visible.
                val side = ((size.width - inset * 2) * (64f / 48f)).roundToInt()
                drawImage(
                    image = if (preferred) preferredBorder else alternativeBorder,
                    dstOffset = IntOffset(((size.width - side) / 2).roundToInt(), ((size.height - side) / 2).roundToInt()),
                    dstSize = IntSize(side, side),
                    colorFilter = if (preferred) PreferredBuildTalentBorderFilter else AlternativeBuildTalentBorderFilter,
                    filterQuality = FilterQuality.High
                )
            }
        }
        Image(
            bitmap = image, contentDescription = null, contentScale = ContentScale.Fit,
            colorFilter = if (selection != null && !preferred && !alternative) UnselectedBuildTalentFilter else null,
            modifier = Modifier.matchParentSize().padding(4.dp)
        )
    }
}
