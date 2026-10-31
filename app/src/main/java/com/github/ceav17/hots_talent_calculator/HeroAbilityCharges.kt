package com.github.ceav17.hots_talent_calculator

import androidx.compose.foundation.Image
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.imageResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import kotlin.math.roundToInt

// HDP ability IDs distinguish the base spell from forms and follow-up commands sharing its key.
private val BaseAbilityChargeCounts = mapOf(
    "AbathurToxicNest" to 3,
    "AzmodanSummonDemonWarrior" to 2,
    "FirebatOilSpill" to 2,
    "AmazonLightningFury" to 2,
    "GenjiShuriken" to 3,
    "JunkratFragLauncher" to 4,
    "KerriganRavage" to 2,
    "MonkRadiantDash" to 2,
    "WitchDoctorPlagueofToads" to 2,
    "ProbiusWarpRift" to 2,
    "TracerBlink" to 3,
    "ZagaraBanelingBarrage" to 2,
    "ZagaraSummonCreepTumor" to 3,
    "ZaryaParticleGrenade" to 4
)

internal fun heroAbilityCharges(ability: HeroAbility): Int? = BaseAbilityChargeCounts[ability.abilityId]

@Composable
internal fun HeroAbilityArtwork(
    image: ImageBitmap,
    modifier: Modifier = Modifier,
    charges: Int? = null
) {
    if (charges == null) {
        Image(bitmap = image, contentDescription = null, contentScale = ContentScale.Fit, modifier = modifier)
        return
    }
    val background = ImageBitmap.imageResource(R.drawable.storm_ui_ingame_chargebackground)
    val textMeasurer = rememberTextMeasurer()
    Image(
        bitmap = image,
        contentDescription = pluralStringResource(R.plurals.hero_ability_charges, charges, charges),
        contentScale = ContentScale.Fit,
        modifier = modifier.drawWithCache {
            // Native hexagon frame: 126x126; badge center on the lower-right slanted edge.
            val scale = size.minDimension / 126f
            val center = Offset(size.width / 2f + 22f * scale, size.height / 2f + 39f * scale)
            val badgeSize = (32f * scale).roundToInt().coerceAtLeast(1)
            val label = textMeasurer.measure(
                text = charges.toString(),
                style = TextStyle(
                    fontFamily = MenuFontFamily,
                    fontWeight = FontWeight.Bold,
                    fontSize = (badgeSize * 0.5f).toSp()
                ),
                maxLines = 1,
                softWrap = false
            )
            // The visible circle sits slightly above the center of the PNG.
            val textPosition = Offset(
                center.x - label.size.width / 2f,
                center.y - 2f * scale - label.size.height / 2f
            )
            val outline = Stroke(width = 2f * scale)
            onDrawWithContent {
                drawContent()
                drawImage(
                    image = background,
                    dstOffset = IntOffset((center.x - badgeSize / 2f).roundToInt(), (center.y - badgeSize / 2f).roundToInt()),
                    dstSize = IntSize(badgeSize, badgeSize),
                    filterQuality = FilterQuality.High
                )
                drawText(label, color = Color.Black, topLeft = textPosition, drawStyle = outline)
                drawText(label, color = Color.White, topLeft = textPosition, drawStyle = Fill)
            }
        }
    )
}
