package com.github.ceav17.hots_talent_calculator

import androidx.compose.foundation.layout.widthIn
import android.app.ActivityManager
import android.content.ComponentCallbacks2
import android.graphics.Color as AndroidColor
import android.os.Build
import android.os.Bundle
import android.os.Process
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.AbsoluteAlignment
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.NavType
import androidx.navigation.navArgument
import com.github.ceav17.hots_talent_calculator.ui.theme.TalentCalculatorHotSTheme
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import java.io.IOException

private const val MAIN_MENU_ROUTE = "main_menu"
private const val MAPS_ROUTE = "maps"
private const val SETTINGS_ROUTE = "settings"
private const val DATA_TRANSFER_ROUTE = "settings/data_transfer"
private const val ABOUT_ROUTE = "settings/about"
private const val LEGAL_NOTICES_ROUTE = "settings/legal_notices"
private const val TALENT_CALCULATOR_ROUTE = "talent_calculator"
private const val HERO_ROUTE = "talent_calculator/hero/{heroId}"
private const val HERO_BUILDS_ROUTE = "talent_calculator/hero/{heroId}/builds"
private const val HERO_BUILD_ROUTE = "talent_calculator/hero/{heroId}/builds/{buildId}"

internal val MenuFontFamily = FontFamily(
    Font(R.font.metronicforblizzard_bold, FontWeight.Bold)
)

class MainActivity : ComponentActivity() {
    private var exitRequested = false

    override fun onCreate(savedInstanceState: Bundle?) {
        // The system draws the launch theme until our first frame; keep it out of the app window.
        setTheme(R.style.Theme_TalentCalculatorHotS)
        super.onCreate(savedInstanceState)
        updateRecentAppsIcon()
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.dark(AndroidColor.TRANSPARENT),
            navigationBarStyle = SystemBarStyle.dark(AndroidColor.TRANSPARENT)
        )
        window.isNavigationBarContrastEnforced = false
        setContent {
            TalentCalculatorHotSTheme {
                TalentCalculatorApp(onExit = ::exitApplication)
            }
        }
    }

    @Suppress("DEPRECATION") // Android 10–12 require the constructor instead of Builder.
    private fun updateRecentAppsIcon() {
        val label = getString(R.string.app_name)
        val description = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ActivityManager.TaskDescription.Builder()
                .setLabel(label)
                .setIcon(R.mipmap.ic_launcher)
                .build()
        } else {
            ActivityManager.TaskDescription(label, R.mipmap.ic_launcher)
        }
        setTaskDescription(description)
    }

    private fun exitApplication() {
        exitRequested = true
        finishAndRemoveTask()
    }

    @Suppress("DEPRECATION") // Older Android versions still report running-memory pressure levels.
    override fun onTrimMemory(level: Int) {
        super.onTrimMemory(level)
        if (level >= ComponentCallbacks2.TRIM_MEMORY_RUNNING_LOW) clearAssetImageCache()
    }

    override fun onDestroy() {
        super.onDestroy()
        if (exitRequested) {
            // Complete activity cleanup before terminating the otherwise cached process.
            Process.killProcess(Process.myPid())
        }
    }
}

@Composable
fun TalentCalculatorApp(onExit: () -> Unit, modifier: Modifier = Modifier) {
    val navController = rememberNavController()
    val context = LocalContext.current
    val assets = context.assets
    val favoritesStore = remember(context) { HeroFavoritesStore(context) }
    var favoriteIds by remember(favoritesStore) { mutableStateOf<Set<String>?>(null) }
    var savingFavorite by remember { mutableStateOf(false) }
    val favoritesScope = rememberCoroutineScope()
    LaunchedEffect(favoritesStore) {
        favoriteIds = favoritesStore.load()
    }
    val customGroupStore = remember(context) { HeroCustomGroupStore(context) }
    var customGroups by remember(customGroupStore) { mutableStateOf<HeroCustomGroups?>(null) }
    var savingCustomGroup by remember { mutableStateOf(false) }
    var pendingCountSaves by remember { mutableIntStateOf(0) }
    val customGroupScope = rememberCoroutineScope()
    val customGroupWrites = remember { Mutex() }
    LaunchedEffect(customGroupStore) {
        customGroups = customGroupStore.load()
    }
    val heroes by produceState(emptyMap<String, HeroSearchEntry>(), assets) {
        value = withContext(Dispatchers.IO) { loadHeroSearchEntries(assets) }
    }
    val heroNames = remember(heroes) { heroes.values.map { it.name } }

    NavHost(
        navController = navController,
        startDestination = MAIN_MENU_ROUTE,
        modifier = modifier.fillMaxSize()
    ) {
        composable(MAIN_MENU_ROUTE) {
            MainMenu(
                onTalentCalculator = {
                    navController.navigate(TALENT_CALCULATOR_ROUTE) {
                        launchSingleTop = true
                    }
                },
                onMaps = {
                    navController.navigate(MAPS_ROUTE) {
                        launchSingleTop = true
                    }
                },
                onSettings = {
                    navController.navigate(SETTINGS_ROUTE) {
                        launchSingleTop = true
                    }
                },
                onExit = onExit,
                onTwitch = { openHeroesOnTwitch(context) }
            )
        }
        composable(SETTINGS_ROUTE) { entry ->
            SettingsScreen(
                visibleCount = customGroups?.visibleCount ?: 1,
                enabled = customGroups != null,
                savingCount = pendingCountSaves > 0,
                onCountChange = { count ->
                    if (customGroups != null && (count != customGroups?.visibleCount || pendingCountSaves > 0)) {
                        pendingCountSaves++
                        customGroupScope.launch {
                            try {
                                // Queue rapid adjustments instead of dropping changes during a disk write.
                                customGroupWrites.withLock {
                                    customGroups = customGroupStore.setVisibleCount(count)
                                }
                            } catch (_: IOException) {
                                Toast.makeText(context, R.string.custom_group_save_failed, Toast.LENGTH_SHORT).show()
                            } finally {
                                pendingCountSaves--
                            }
                        }
                    }
                },
                dataTransferEnabled = favoriteIds != null && customGroups != null &&
                    !savingFavorite && !savingCustomGroup && pendingCountSaves == 0,
                onDataTransfer = {
                    if (navController.currentBackStackEntry == entry &&
                        entry.lifecycle.currentState == Lifecycle.State.RESUMED
                    ) {
                        navController.navigate(DATA_TRANSFER_ROUTE) { launchSingleTop = true }
                    }
                },
                onAbout = {
                    if (navController.currentBackStackEntry == entry &&
                        entry.lifecycle.currentState == Lifecycle.State.RESUMED
                    ) {
                        navController.navigate(ABOUT_ROUTE) { launchSingleTop = true }
                    }
                },
                onLegalNotices = {
                    if (navController.currentBackStackEntry == entry &&
                        entry.lifecycle.currentState == Lifecycle.State.RESUMED
                    ) {
                        navController.navigate(LEGAL_NOTICES_ROUTE) { launchSingleTop = true }
                    }
                },
                onBack = { navController.popBackStack() }
            )
        }
        composable(DATA_TRANSFER_ROUTE) {
            UserDataTransferScreen(
                onImported = { data ->
                    favoriteIds = data.favoriteIds
                    customGroups = data.customGroups
                },
                onBack = { navController.popBackStack() }
            )
        }
        composable(ABOUT_ROUTE) {
            AboutScreen(onBack = { navController.popBackStack() })
        }
        composable(LEGAL_NOTICES_ROUTE) {
            LegalNoticesScreen(onBack = { navController.popBackStack() })
        }
        composable(MAPS_ROUTE) {
            MapsMenuScreen(
                onModeSelected = { mode ->
                    navController.navigate(mode.route) {
                        launchSingleTop = true
                    }
                },
                onBack = { navController.popBackStack() }
            )
        }
        MapMode.entries.forEach { mode ->
            composable(mode.route) {
                MapListScreen(
                    mode = mode,
                    onBack = { navController.popBackStack() },
                    onMapSelected = { route ->
                        navController.navigate(route) {
                            launchSingleTop = true
                        }
                    }
                )
            }
        }
        AramMap.entries.forEach { map ->
            composable(map.route) {
                AramMapScreen(map = map, onBack = { navController.popBackStack() })
            }
        }
        BattlegroundMap.entries.forEach { map ->
            composable(map.route) {
                BattlegroundMapScreen(map = map, onBack = { navController.popBackStack() })
            }
        }
        composable(TALENT_CALCULATOR_ROUTE) { entry ->
            TalentCalculatorScreen(
                heroes = heroes,
                favoriteIds = favoriteIds.orEmpty(),
                favoriteActionsEnabled = favoriteIds != null && !savingFavorite,
                customGroups = customGroups ?: HeroCustomGroups(),
                customGroupActionsEnabled = customGroups != null && !savingCustomGroup && pendingCountSaves == 0,
                onCustomGroupChange = { index, updatedGroup ->
                    if (!savingCustomGroup && customGroups != null) {
                        savingCustomGroup = true
                        customGroupScope.launch {
                            try {
                                customGroupWrites.withLock {
                                    customGroups = customGroupStore.saveGroup(index, updatedGroup)
                                }
                            } catch (_: IOException) {
                                Toast.makeText(context, R.string.custom_group_save_failed, Toast.LENGTH_SHORT).show()
                            } finally {
                                savingCustomGroup = false
                            }
                        }
                    }
                },
                onFavoriteChange = { heroId, favorite ->
                    if (!savingFavorite && favoriteIds != null) {
                        savingFavorite = true
                        favoritesScope.launch {
                            try {
                                favoriteIds = favoritesStore.setFavorite(heroId, favorite)
                            } catch (_: IOException) {
                                Toast.makeText(context, R.string.favorites_save_failed, Toast.LENGTH_SHORT).show()
                            } finally {
                                savingFavorite = false
                            }
                        }
                    }
                },
                onHome = { navController.popBackStack(MAIN_MENU_ROUTE, inclusive = false) },
                onHeroSelected = { heroId ->
                    // Ignore queued taps once navigation leaves the selection screen.
                    if (navController.currentBackStackEntry == entry &&
                        entry.lifecycle.currentState == Lifecycle.State.RESUMED
                    ) {
                        navController.navigate("talent_calculator/hero/$heroId") {
                            launchSingleTop = true
                        }
                    }
                }
            )
        }
        composable(
            route = HERO_ROUTE,
            arguments = listOf(navArgument("heroId") { type = NavType.StringType })
        ) { entry ->
            HeroScreen(
                hero = heroes[entry.arguments?.getString("heroId")],
                heroNames = heroNames,
                onBack = { navController.popBackStack() },
                onBuilds = {
                    entry.arguments?.getString("heroId")?.let { heroId ->
                        navController.navigate("talent_calculator/hero/$heroId/builds") { launchSingleTop = true }
                    }
                }
            )
        }
        composable(
            route = HERO_BUILDS_ROUTE,
            arguments = listOf(navArgument("heroId") { type = NavType.StringType })
        ) { entry ->
            val heroId = entry.arguments?.getString("heroId")
            HeroBuildsScreen(
                hero = heroes[heroId],
                onBack = { navController.popBackStack() },
                onBuildSelected = { buildId ->
                    if (heroId != null) {
                        navController.navigate("talent_calculator/hero/$heroId/builds/$buildId") {
                            launchSingleTop = true
                        }
                    }
                }
            )
        }
        composable(
            route = HERO_BUILD_ROUTE,
            arguments = listOf(
                navArgument("heroId") { type = NavType.StringType },
                navArgument("buildId") { type = NavType.StringType }
            )
        ) { entry ->
            HeroBuildScreen(
                heroId = requireNotNull(entry.arguments?.getString("heroId")),
                buildId = requireNotNull(entry.arguments?.getString("buildId")),
                onBack = { navController.popBackStack() }
            )
        }
    }
}

@Composable
fun MainMenu(
    onTalentCalculator: () -> Unit,
    onMaps: () -> Unit,
    onSettings: () -> Unit,
    onExit: () -> Unit,
    onTwitch: () -> Unit,
    modifier: Modifier = Modifier
) {
    ResponsiveScreen(
        backgroundResId = R.drawable.storm_ui_homescreenbackground_nexus,
        backgroundAlignment = AbsoluteAlignment.BottomLeft,
        modifier = modifier
    ) {
        val compact = maxHeight < 600.dp
        val buttonWidth = minOf(if (compact) 248.dp else 292.dp, maxWidth)
        val buttonHeight = buttonWidth * (108f / 292f)
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween,
            modifier = Modifier.fillMaxWidth().verticalScroll(rememberScrollState())
                .heightIn(min = maxHeight).padding(vertical = 8.dp)
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
                Box(
                    modifier = Modifier.fillMaxWidth().height(if (compact) 96.dp else 208.dp)
                ) {
                    Image(
                        painter = painterResource(R.drawable.custom_hots_logo),
                        contentDescription = stringResource(R.string.main_menu_logo),
                        contentScale = ContentScale.Fit,
                        modifier = Modifier.align(Alignment.TopCenter).widthIn(max = 520.dp)
                            .fillMaxWidth().height(if (compact) 72.dp else 160.dp)
                            .padding(horizontal = 16.dp)
                    )
                    Image(
                        painter = painterResource(R.drawable.storm_ui_taskbar_buttonicon_menu),
                        contentDescription = stringResource(R.string.main_menu_settings),
                        contentScale = ContentScale.Fit,
                        modifier = Modifier.align(Alignment.BottomEnd).padding(end = 24.dp).size(48.dp)
                            .testTag("main_menu_settings")
                            .clickable(interactionSource = null, indication = null, role = Role.Button, onClick = onSettings)
                    )
                }
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf(
                        R.string.main_menu_talent_calculator to onTalentCalculator,
                        R.string.main_menu_maps to onMaps
                    ).forEach { (label, onClick) ->
                        MainMenuItem(
                            imageResId = R.drawable.storm_standardbutton_blue_hover,
                            label = stringResource(label),
                            fontSize = if (compact) 20.sp else 22.sp,
                            modifier = Modifier.size(buttonWidth, buttonHeight)
                                .clickable(interactionSource = null, indication = null, role = Role.Button, onClick = onClick)
                        )
                    }
                }
            }
            Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
                Image(
                    painter = painterResource(R.drawable.glitch_flat_purple),
                    contentDescription = stringResource(R.string.main_menu_twitch),
                    contentScale = ContentScale.Fit,
                    modifier = Modifier.align(Alignment.End).padding(end = 24.dp).size(56.dp, 64.dp)
                        .testTag("main_menu_twitch")
                        .clickable(interactionSource = null, indication = null, role = Role.Button, onClick = onTwitch)
                        .padding(4.dp)
                )
                MainMenuItem(
                    imageResId = R.drawable.storm_standardbutton_blue_normal,
                    label = stringResource(R.string.main_menu_exit),
                    modifier = Modifier.size(buttonWidth, buttonHeight)
                        .clickable(interactionSource = null, indication = null, role = Role.Button, onClick = onExit)
                )
            }
        }
    }
}

@Composable
internal fun MainMenuItem(
    imageResId: Int,
    label: String,
    modifier: Modifier = Modifier,
    leadingImageResId: Int? = null,
    fontSize: TextUnit = 22.sp,
    textPadding: Dp = 28.dp,
    trailingImageResId: Int? = null
) {
    val labelStyle = TextStyle(
        color = Color.White,
        fontFamily = MenuFontFamily,
        fontWeight = FontWeight.Bold,
        fontSize = fontSize,
        lineHeight = fontSize * (28f / 22f),
        letterSpacing = 0.sp,
        textAlign = TextAlign.Center
    )
    Box(modifier = modifier, contentAlignment = Alignment.Center) {
        Image(
            painter = painterResource(imageResId),
            contentDescription = null,
            contentScale = ContentScale.Fit,
            modifier = Modifier.fillMaxSize()
        )
        leadingImageResId?.let { iconResId ->
            Image(
                painter = painterResource(iconResId),
                contentDescription = null,
                contentScale = ContentScale.Fit,
                modifier = Modifier
                    .align(AbsoluteAlignment.CenterLeft)
                    .padding(start = 32.dp)
                    .fillMaxHeight(0.46f)
                    .aspectRatio(1f)
            )
        }
        if (trailingImageResId == null) {
            OutlinedText(
                text = label,
                style = labelStyle,
                maxLines = 1,
                modifier = Modifier.padding(horizontal = textPadding)
            )
        } else {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.padding(horizontal = textPadding)
            ) {
                OutlinedText(
                    text = label,
                    style = labelStyle,
                    maxLines = 1,
                    modifier = Modifier.weight(1f, fill = false)
                )
                Image(
                    painter = painterResource(trailingImageResId),
                    contentDescription = null,
                    contentScale = ContentScale.Fit,
                    modifier = Modifier.size(32.dp)
                )
            }
        }
    }
}

@Composable
fun NexusBackground(modifier: Modifier = Modifier) {
    Image(
        painter = painterResource(R.drawable.storm_ui_homescreenbackground_nexus),
        contentDescription = null,
        contentScale = ContentScale.Crop,
        alignment = AbsoluteAlignment.BottomLeft,
        modifier = modifier.fillMaxSize()
    )
}

@Preview(name = "Small Phone", widthDp = 360, heightDp = 400)
@Preview(name = "Phone", widthDp = 387, heightDp = 864)
@Preview(name = "Tablet", widthDp = 1280, heightDp = 800)
@Composable
fun MainMenuPreview() {
    TalentCalculatorHotSTheme {
        MainMenu(onTalentCalculator = {}, onMaps = {}, onSettings = {}, onExit = {}, onTwitch = {})
    }
}
