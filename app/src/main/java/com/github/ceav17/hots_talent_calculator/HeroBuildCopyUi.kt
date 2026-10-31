package com.github.ceav17.hots_talent_calculator

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp

@Composable
internal fun rememberBuildCodeCopyAction(): (String, HeroBuildCodeFormat) -> Unit {
    val context = LocalContext.current
    val clipboardLabel = stringResource(R.string.hero_build_code_label)
    val clipboard = remember(context) {
        context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
    }
    var feedback by remember(context) { mutableStateOf<Toast?>(null) }
    DisposableEffect(context) {
        onDispose { feedback?.cancel() }
    }
    return remember(context, clipboard, clipboardLabel) {
        { code: String, format: HeroBuildCodeFormat ->
            clipboard.setPrimaryClip(ClipData.newPlainText(clipboardLabel, code))
            val message = when (format) {
                HeroBuildCodeFormat.Game -> R.string.hero_build_copied_game
                HeroBuildCodeFormat.App -> R.string.hero_build_copied_app
            }
            // Replace the previous confirmation instead of queueing messages on repeated taps.
            feedback?.cancel()
            feedback = Toast.makeText(context, message, Toast.LENGTH_SHORT).also { it.show() }
        }
    }
}

@Composable
internal fun HeroBuildCopyButtons(
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    onCopy: (HeroBuildCodeFormat) -> Unit
) {
    Row(modifier.alpha(if (enabled) 1f else 0.45f)) {
        HeroBuildCodeFormat.entries.forEach { format ->
            val gameFormat = format == HeroBuildCodeFormat.Game
            Image(
                painter = painterResource(if (gameFormat) R.drawable.storm_heroquest_icon_incomplete else R.drawable.app_launcher_artwork),
                contentDescription = stringResource(if (gameFormat) R.string.hero_build_copy_game else R.string.hero_build_copy_app),
                contentScale = ContentScale.Fit,
                modifier = Modifier.size(48.dp)
                    .testTag(if (gameFormat) "hero_build_copy_game" else "hero_build_copy_app")
                    .clickable(enabled = enabled, interactionSource = null, indication = null, role = Role.Button) {
                        onCopy(format)
                    }.padding(8.dp)
            )
        }
    }
}
