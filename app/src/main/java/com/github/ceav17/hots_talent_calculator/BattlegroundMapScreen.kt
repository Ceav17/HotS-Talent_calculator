package com.github.ceav17.hots_talent_calculator

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp

@Composable
internal fun BattlegroundMapScreen(
    map: BattlegroundMap,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    MapDetailScreen(
        imageResId = map.imageResId,
        nameResId = map.nameResId,
        backLabelResId = R.string.map_detail_back,
        pageTag = "battleground_map_${map.id}",
        onBack = onBack,
        modifier = modifier
    ) {
        item(key = "fountain_and_minions") {
            Row(horizontalArrangement = Arrangement.spacedBy(20.dp), modifier = Modifier.fillMaxWidth()) {
                MapInfo(
                    imageResId = map.fountainResId,
                    text = stringResource(R.string.aram_healing_fountain),
                    modifier = Modifier.weight(1f)
                )
                MapInfo(
                    imageResId = map.minionResId,
                    text = stringResource(R.string.aram_minions),
                    modifier = Modifier.weight(1f)
                )
            }
        }
        item(key = "objective_and_first_info") {
            var objectiveText = stringResource(R.string.map_objective_spawn, map.objectiveSpawnTime)
            map.objectiveChannelTime?.let {
                objectiveText += "\n" + stringResource(R.string.map_objective_channel, it)
            }
            Row(horizontalArrangement = Arrangement.spacedBy(20.dp), modifier = Modifier.fillMaxWidth()) {
                MapInfo(
                    imageResId = map.objectiveResId,
                    text = objectiveText,
                    modifier = Modifier.weight(1f)
                )
                if (map.hasGlobes) {
                    MapInfo(
                        imageResId = R.drawable.ui_ingame_mapmechanic_loadscreen_silvercity_icon2,
                        text = stringResource(R.string.aram_globes),
                        modifier = Modifier.weight(1f)
                    )
                } else {
                    MapCampInfo(camp = map.camps.first(), modifier = Modifier.weight(1f))
                }
            }
        }
        items(
            items = if (map.hasGlobes) map.camps else map.camps.drop(1),
            key = { "camp_${it.type.name}" }
        ) { camp ->
            Row(horizontalArrangement = Arrangement.spacedBy(20.dp), modifier = Modifier.fillMaxWidth()) {
                Spacer(modifier = Modifier.weight(1f))
                MapCampInfo(camp = camp, modifier = Modifier.weight(1f))
            }
        }
    }
}

@Composable
private fun MapCampInfo(camp: MapCamp, modifier: Modifier = Modifier) {
    val title = pluralStringResource(camp.type.labelResId, camp.count, camp.count)
    val timings = if (camp.spawnTime == camp.respawnTime) {
        stringResource(R.string.map_camp_shared_times, camp.spawnTime)
    } else {
        stringResource(R.string.map_camp_times, camp.spawnTime, camp.respawnTime)
    }
    MapInfo(imageResId = camp.imageResId, text = "$title\n$timings", modifier = modifier)
}
