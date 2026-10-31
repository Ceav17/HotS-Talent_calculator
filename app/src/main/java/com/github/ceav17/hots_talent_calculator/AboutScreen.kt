package com.github.ceav17.hots_talent_calculator

import androidx.compose.foundation.layout.widthIn
import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.widget.Toast
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.absoluteOffset
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.AbsoluteAlignment
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.LinkAnnotation
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextLinkStyles
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.fromHtml
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.net.toUri

private const val GITHUB_REPOSITORY_URL = "https://github.com/ceav17/HotS-Talent_calculator"

@Composable
internal fun AboutScreen(onBack: () -> Unit, modifier: Modifier = Modifier) {
    val context = LocalContext.current
    SettingsTextPage(
        title = stringResource(R.string.settings_about),
        onBack = onBack,
        modifier = modifier,
        onGitHub = { openAboutLink(context, GITHUB_REPOSITORY_URL) }
    ) { textStyle ->
        OutlinedText(
            text = stringResource(R.string.about_development),
            style = textStyle,
            modifier = Modifier.fillMaxWidth()
        )
        OutlinedText(
            text = stringResource(R.string.about_updates),
            style = textStyle,
            modifier = Modifier.fillMaxWidth()
        )
        val feedbackHtml = stringResource(R.string.about_feedback)
        val feedback = remember(feedbackHtml, context) {
            AnnotatedString.fromHtml(
                htmlString = feedbackHtml,
                linkStyles = TextLinkStyles(
                    style = SpanStyle(
                        color = Color(0xFF4DA3FF),
                        textDecoration = TextDecoration.None
                    )
                ),
                linkInteractionListener = { link ->
                    if (link is LinkAnnotation.Url) openAboutLink(context, link.url)
                }
            )
        }
        Box(modifier = Modifier.fillMaxWidth()) {
            // Keep the outline behind native Text so links retain their colors and accessibility.
            OutlinedText(
                text = feedback.text,
                style = textStyle.copy(color = Color.Black),
                modifier = Modifier.fillMaxWidth().clearAndSetSemantics {}
            )
            Text(text = feedback, style = textStyle, modifier = Modifier.fillMaxWidth())
        }
    }
}

@Composable
internal fun LegalNoticesScreen(onBack: () -> Unit, modifier: Modifier = Modifier) {
    SettingsTextPage(
        title = stringResource(R.string.settings_legal_notices),
        onBack = onBack,
        modifier = modifier
    ) { textStyle ->
        OutlinedText(
            text = stringResource(R.string.about_unofficial_notice),
            style = textStyle,
            modifier = Modifier.fillMaxWidth()
        )
        OutlinedText(
            text = stringResource(R.string.about_blizzard_notice),
            style = textStyle,
            modifier = Modifier.fillMaxWidth()
        )
        OutlinedText(
            text = stringResource(R.string.about_services_notice),
            style = textStyle,
            modifier = Modifier.fillMaxWidth()
        )
    }
}

@Composable
internal fun SettingsTextPage(
    title: String,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    onGitHub: (() -> Unit)? = null,
    content: @Composable (TextStyle) -> Unit
) {
    ResponsiveScreen(
        backgroundResId = R.drawable.storm_ui_homescreenbackground_nexus,
        backgroundAlignment = AbsoluteAlignment.BottomLeft,
        modifier = modifier
    ) {
        val backTop = calculatorBackIconOffset(maxWidth, maxHeight).y
        val titleLeft = 24.dp + CalculatorBackIconSize + 12.dp
        val titleRight = if (onGitHub != null) titleLeft else 24.dp
        val textStyle = TextStyle(
            color = Color.White,
            fontFamily = MenuFontFamily,
            fontWeight = FontWeight.Bold,
            fontSize = 18.sp,
            lineHeight = 26.sp,
            letterSpacing = 0.sp
        )

        Box(
            modifier = Modifier.align(AbsoluteAlignment.TopLeft)
                .absoluteOffset(x = titleLeft, y = backTop)
                .size(
                    width = (maxWidth - titleLeft - titleRight).coerceAtLeast(0.dp),
                    height = CalculatorBackIconSize
                ),
            contentAlignment = Alignment.Center
        ) {
            OutlinedText(
                text = title,
                style = textStyle.copy(fontSize = 28.sp, lineHeight = 34.sp, textAlign = TextAlign.Center),
                maxLines = 1
            )
        }
        Column(
            verticalArrangement = Arrangement.spacedBy(20.dp),
            modifier = Modifier.align(Alignment.TopCenter).widthIn(max = 688.dp).fillMaxSize()
                .padding(start = 24.dp, end = 24.dp, top = backTop + CalculatorBackIconSize + 24.dp)
                .verticalScroll(rememberScrollState())
                .padding(bottom = 24.dp)
        ) {
            content(textStyle)
        }
        if (onGitHub != null) {
            Image(
                painter = painterResource(R.drawable.github_invertocat_black),
                contentDescription = stringResource(R.string.about_github),
                contentScale = ContentScale.Fit,
                modifier = Modifier.align(AbsoluteAlignment.TopRight)
                    .absoluteOffset(x = (-24).dp, y = backTop)
                    .size(CalculatorBackIconSize)
                    .clip(CircleShape)
                    .background(Color.White)
                    .clickable(
                        interactionSource = null, indication = null, role = Role.Button,
                        onClick = onGitHub
                    )
                    .padding(8.dp)
            )
        }
        Image(
            painter = painterResource(R.drawable.storm_ui_glues_button_close_normal),
            contentDescription = stringResource(R.string.about_back),
            contentScale = ContentScale.Fit,
            modifier = Modifier.align(AbsoluteAlignment.TopLeft)
                .absoluteOffset(x = 24.dp, y = backTop)
                .size(CalculatorBackIconSize)
                .clickable(
                    interactionSource = null, indication = null, role = Role.Button, onClick = onBack
                )
        )
    }
}

private fun openAboutLink(context: Context, url: String) {
    val email = url.startsWith("mailto:")
    val intent = Intent(if (email) Intent.ACTION_SENDTO else Intent.ACTION_VIEW, url.toUri())
        .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    if (!email) intent.addCategory(Intent.CATEGORY_BROWSABLE)
    val errorMessage = if (email) R.string.about_no_mail_app else R.string.about_no_link_app
    try {
        context.startActivity(intent)
    } catch (_: ActivityNotFoundException) {
        Toast.makeText(context, errorMessage, Toast.LENGTH_SHORT).show()
    } catch (_: SecurityException) {
        Toast.makeText(context, errorMessage, Toast.LENGTH_SHORT).show()
    }
}
