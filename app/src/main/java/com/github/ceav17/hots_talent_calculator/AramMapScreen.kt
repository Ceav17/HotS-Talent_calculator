package com.github.ceav17.hots_talent_calculator

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp

internal enum class AramMap(
    val id: String,
    @DrawableRes val imageResId: Int,
    @StringRes val nameResId: Int,
    @DrawableRes val fountainResId: Int,
    @DrawableRes val minionResId: Int,
    @DrawableRes val campResId: Int?,
    @StringRes val campDetailsResId: Int?
) {
    BRAXIS_OUTPOST(
        "braxis_outpost",
        R.drawable.storm_ui_aram_braxisoutpost,
        R.string.aram_braxis_outpost,
        R.drawable.storm_ui_ingame_targetinfopanel_unit_braxis_building_moonwell,
        R.drawable.storm_ui_ingame_targetinfopanel_unit_braxis_minion_melee,
        R.drawable.storm_ui_ingame_targetinfopanel_unit_braxis_merc_bruiser_raven,
        R.string.aram_camps_braxis_outpost
    ),
    INDUSTRIAL_DISTRICT(
        "industrial_district",
        R.drawable.storm_ui_homescreenbackground_industrialdistrict,
        R.string.aram_industrial_district,
        R.drawable.storm_ui_ingame_targetinfopanel_unit_volskaya_building_moonwell,
        R.drawable.storm_ui_ingame_targetinfopanel_unit_volskaya_minion_melee,
        R.drawable.storm_ui_ingame_targetinfopanel_unit_volskaya_merc_turret,
        R.string.aram_camps_industrial_district
    ),
    LOST_CAVERN(
        "lost_cavern",
        R.drawable.storm_ui_homescreenbackground_lostcavern,
        R.string.aram_lost_cavern,
        R.drawable.storm_ui_ingame_targetinfopanel_unit_tombofthespiderqueen_building_moonwell,
        R.drawable.storm_ui_ingame_targetinfopanel_unit_skytemple_minion_melee,
        null,
        null
    ),
    SILVER_CITY(
        "silver_city",
        R.drawable.storm_ui_homescreenbackground_silvercity,
        R.string.aram_silver_city,
        R.drawable.storm_ui_ingame_targetinfopanel_unit_boe_building_moonwell_heaven,
        R.drawable.storm_ui_ingame_targetinfopanel_unit_boe_minion_melee_heaven,
        R.drawable.storm_ui_ingame_targetinfopanel_unit_boe_merc_siege,
        R.string.aram_camps_silver_city
    );

    val route: String get() = "${MapMode.ARAM.route}/$id"
}


@Composable
internal fun AramMapScreen(map: AramMap, onBack: () -> Unit, modifier: Modifier = Modifier) {
    MapDetailScreen(
        imageResId = map.imageResId,
        nameResId = map.nameResId,
        backLabelResId = R.string.aram_map_back,
        pageTag = "aram_map_${map.id}",
        onBack = onBack,
        modifier = modifier
    ) {
        item(key = "fountain_and_globes") {
            Row(horizontalArrangement = Arrangement.spacedBy(20.dp), modifier = Modifier.fillMaxWidth()) {
                MapInfo(
                    imageResId = map.fountainResId,
                    text = stringResource(R.string.aram_healing_fountain),
                    modifier = Modifier.weight(1f)
                )
                MapInfo(
                    imageResId = R.drawable.ui_ingame_mapmechanic_loadscreen_silvercity_icon2,
                    text = stringResource(R.string.aram_globes),
                    modifier = Modifier.weight(1f)
                )
            }
        }
        item(key = "minions_and_camps") {
            val campResId = map.campResId
            val campDetailsResId = map.campDetailsResId
            Row(horizontalArrangement = Arrangement.spacedBy(20.dp), modifier = Modifier.fillMaxWidth()) {
                MapInfo(
                    imageResId = map.minionResId,
                    text = stringResource(R.string.aram_minions),
                    modifier = Modifier.weight(1f)
                )
                if (campResId != null && campDetailsResId != null) {
                    MapInfo(
                        imageResId = campResId,
                        text = stringResource(R.string.aram_camps_start) + "\n" + stringResource(campDetailsResId),
                        modifier = Modifier.weight(1f)
                    )
                } else {
                    Spacer(Modifier.weight(1f))
                }
            }
        }
    }
}
