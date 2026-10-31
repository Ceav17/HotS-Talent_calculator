package com.github.ceav17.hots_talent_calculator

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.res.AssetManager
import android.media.AudioManager
import android.widget.Toast
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.Paint
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.imageResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.IOException
import kotlin.math.roundToInt

internal data class HeroVoiceLine(val id: String, val title: String, val variant: String?, val audioPath: String) {
    val label: String get() = if (variant == null) title else "$title ($variant)"
}

internal data class HeroVoiceCatalog(val portraitPath: String, val lines: List<HeroVoiceLine>)

internal fun loadHeroVoiceCatalog(assets: AssetManager, heroId: String): HeroVoiceCatalog {
    val data = assets.open("data/voice_lines/$heroId.json").bufferedReader().use { JSONObject(it.readText()) }
    val lines = data.getJSONArray("lines")
    return HeroVoiceCatalog(data.getString("portrait"), List(lines.length()) { index ->
        val line = lines.getJSONObject(index)
        HeroVoiceLine(line.getString("id"), line.getString("title"),
            if (line.isNull("variant")) null else line.getString("variant"), line.getString("audio"))
    })
}

private data class HeroVoiceContent(val catalog: HeroVoiceCatalog, val portrait: ImageBitmap)

@Composable
internal fun HeroVoiceLines(heroId: String) {
    val context = LocalContext.current
    val assets = context.assets
    val lifecycle = LocalLifecycleOwner.current.lifecycle
    val errorMessage = stringResource(R.string.hero_voice_error)
    val player = remember(heroId, context) {
        HeroVoicePlayer(context) { Toast.makeText(context, errorMessage, Toast.LENGTH_SHORT).show() }
    }
    DisposableEffect(player, lifecycle, context) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_STOP) player.stop()
        }
        val receiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context?, intent: Intent?) {
                if (intent?.action == AudioManager.ACTION_AUDIO_BECOMING_NOISY) player.stop()
            }
        }
        lifecycle.addObserver(observer)
        ContextCompat.registerReceiver(context, receiver, IntentFilter(AudioManager.ACTION_AUDIO_BECOMING_NOISY),
            ContextCompat.RECEIVER_NOT_EXPORTED)
        onDispose {
            lifecycle.removeObserver(observer)
            context.unregisterReceiver(receiver)
            player.stop()
        }
    }
    var failed by remember(heroId) { mutableStateOf(false) }
    val content by produceState<HeroVoiceContent?>(null, heroId, assets) {
        try {
            value = withContext(Dispatchers.IO) {
                val catalog = loadHeroVoiceCatalog(assets, heroId)
                val portrait = loadAssetImage(assets, catalog.portraitPath)
                HeroVoiceContent(catalog, portrait)
            }
        } catch (_: IOException) {
            failed = true
        }
    }
    val loaded = content
    if (failed) {
        Text(errorMessage, color = Color.White, fontFamily = MenuFontFamily)
    } else if (loaded != null) {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth().testTag("hero_voice_lines")) {
            loaded.catalog.lines.forEach { line ->
                HeroVoiceLineCard(line, loaded.portrait,
                    active = player.activeId == line.id,
                    playing = player.activeId == line.id && player.isPlaying,
                    onClick = { player.toggle(line) })
            }
        }
    }
}

@Composable
private fun HeroVoiceLineCard(line: HeroVoiceLine, portrait: ImageBitmap, active: Boolean, playing: Boolean, onClick: () -> Unit) {
    val playDescription = stringResource(if (active) R.string.hero_voice_stop else R.string.hero_voice_play, line.label)
    val playbackState = stringResource(when {
        playing -> R.string.hero_voice_playing
        active -> R.string.hero_voice_loading
        else -> R.string.hero_voice_stopped
    })
    BoxWithConstraints(Modifier.fillMaxWidth()) {
        val cardWidth = maxWidth
        Box(
            modifier = Modifier.fillMaxWidth().height(maxOf(cardWidth * (204f / 720f), 112.dp))
                .clickable(interactionSource = null, indication = null, role = Role.Button, onClick = onClick)
                .semantics {
                    contentDescription = playDescription
                    stateDescription = playbackState
                }
                .testTag("hero_voice_${line.id}")
        ) {
            HeroVoiceCardBackground(portrait, Modifier.fillMaxSize())
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.fillMaxSize().padding(start = cardWidth * 0.055f, end = cardWidth * 0.24f)
            ) {
                Box(contentAlignment = Alignment.Center, modifier = Modifier.size(48.dp)) {
                    Image(painterResource(R.drawable.storm_ui_store_previewbtn_hover), null, Modifier.fillMaxSize())
                    if (playing) {
                        HeroVoiceWave()
                    } else {
                        Image(painterResource(R.drawable.storm_ui_store_previewbtn_play_normal), null,
                            Modifier.size(25.5.dp))
                    }
                }
                Column(verticalArrangement = Arrangement.spacedBy(3.dp), modifier = Modifier.weight(1f)) {
                    Text(line.title, style = TextStyle(color = Color(0xFFD8DCF6), fontFamily = MenuFontFamily,
                        fontWeight = FontWeight.Bold, fontSize = 16.sp, lineHeight = 19.sp))
                    if (line.variant != null) {
                        Text(line.variant, color = Color(0xFFB7AEDC), fontFamily = MenuFontFamily,
                            fontWeight = FontWeight.Bold, fontSize = 12.sp, lineHeight = 14.sp)
                    }
                }
            }
        }
    }
}

@Composable
private fun HeroVoiceWave() {
    val waveA = ImageBitmap.imageResource(R.drawable.storm_ui_store_previewbtn_wavea)
    val waveB = ImageBitmap.imageResource(R.drawable.storm_ui_store_previewbtn_waveb)
    val blend by rememberInfiniteTransition(label = "Voice waveform").animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 240, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "Wave blend"
    )
    Canvas(Modifier.size(37.5.dp)) {
        val destination = IntSize(size.width.roundToInt(), size.height.roundToInt())
        // The central bars are at (53.5, 48.5) and (44.5, 49) in the 100px sprites.
        // Add the weighted sprites in isolation to keep the shared bar equally bright.
        drawContext.canvas.saveLayer(Rect(Offset.Zero, size), Paint())
        translate(left = size.width * -0.035f, top = size.height * 0.015f) {
            drawImage(waveA, dstSize = destination, alpha = 1f - blend)
        }
        translate(left = size.width * 0.055f, top = size.height * 0.01f) {
            drawImage(waveB, dstSize = destination, alpha = blend, blendMode = BlendMode.Plus)
        }
        drawContext.canvas.restore()
    }
}

@Composable
private fun HeroVoiceCardBackground(portrait: ImageBitmap, modifier: Modifier) {
    val frame = ImageBitmap.imageResource(R.drawable.storm_ui_store_voiceframe_off)
    val shadow = ImageBitmap.imageResource(R.drawable.storm_ui_store_voiceframe_top)
    val highlight = ImageBitmap.imageResource(R.drawable.storm_ui_store_voiceframeshinemask)
    val pattern = ImageBitmap.imageResource(R.drawable.storm_ui_store_gemdialog_trianglepattern)
    Canvas(modifier) {
        val destination = IntSize(size.width.roundToInt(), size.height.roundToInt())
        drawImage(frame, dstSize = destination)
        val left = size.width * (23f / 720f)
        val top = size.height * (22f / 204f)
        val right = size.width * (697f / 720f)
        val bottom = size.height * (182f / 204f)
        clipRect(left, top, right, bottom) {
            val background = Color(0xFF241044)
            drawRect(Brush.verticalGradient(listOf(Color(0xFF32145C), background, Color(0xFF160B30))))
            val tileWidth = (size.width * 0.18f).roundToInt().coerceAtLeast(1)
            val tileHeight = (tileWidth * pattern.height.toFloat() / pattern.width).roundToInt().coerceAtLeast(1)
            for (y in 0..size.height.toInt() step tileHeight) {
                for (x in 0..size.width.toInt() step tileWidth) {
                    drawImage(pattern, dstOffset = IntOffset(x, y), dstSize = IntSize(tileWidth, tileHeight),
                        colorFilter = ColorFilter.tint(Color(0xFF9370D7)), alpha = 0.65f)
                }
            }
            val width = size.width * 0.40f
            val height = bottom - top
            val portraitRight = right + size.width * (12f / 720f)
            val portraitLeft = portraitRight - width
            // Match the game's lower crop, with the portrait continuing under the right border.
            val sourceTop = (portrait.height * (136f / 1008f)).roundToInt()
            val sourceHeight = (height * portrait.width / width).roundToInt()
                .coerceIn(1, portrait.height - sourceTop)
            // Fade only the portrait so the triangle texture remains visible underneath.
            drawContext.canvas.saveLayer(Rect(portraitLeft, top, portraitRight, bottom), Paint())
            drawImage(portrait, srcOffset = IntOffset(0, sourceTop), srcSize = IntSize(portrait.width, sourceHeight),
                dstOffset = IntOffset(portraitLeft.roundToInt(), top.roundToInt()),
                dstSize = IntSize(width.roundToInt(), height.roundToInt()))
            drawRect(Brush.horizontalGradient(listOf(Color.Transparent, Color.Black),
                startX = portraitLeft, endX = portraitLeft + width * 0.50f),
                topLeft = Offset(portraitLeft, top), size = Size(width, height), blendMode = BlendMode.DstIn)
            drawContext.canvas.restore()
        }
        drawImage(shadow, dstSize = destination)
        drawImage(highlight, dstSize = destination, colorFilter = ColorFilter.tint(Color(0xFFBEB0FF)))
    }
}
