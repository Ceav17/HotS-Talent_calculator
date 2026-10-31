package com.github.ceav17.hots_talent_calculator

import androidx.annotation.DrawableRes
import androidx.annotation.PluralsRes
import androidx.annotation.StringRes

internal enum class MapCampType(@PluralsRes val labelResId: Int) {
    BOSS(R.plurals.map_boss_camps),
    BRUISER(R.plurals.map_bruiser_camps),
    SIEGE(R.plurals.map_siege_camps),
    DOUBLOON(R.plurals.map_doubloon_camps),
    FORTIFICATION(R.plurals.map_fortification_camps),
    RECON(R.plurals.map_recon_camps),
    SAMURAI(R.plurals.map_samurai_camps),
    SAPPER(R.plurals.map_sapper_camps),
    SUPPORT_BOSS(R.plurals.map_support_boss_camps)
}

internal data class MapCamp(
    val type: MapCampType,
    val count: Int,
    @DrawableRes val imageResId: Int,
    val respawnTime: String,
    val spawnTime: String = "30s"
)

// Quick Match and Ranked share these entries and their image resources.
internal enum class BattlegroundMap(
    val id: String,
    @DrawableRes val imageResId: Int,
    @StringRes val nameResId: Int,
    @DrawableRes val fountainResId: Int,
    @DrawableRes val minionResId: Int,
    @DrawableRes val objectiveResId: Int,
    val objectiveSpawnTime: String,
    val camps: List<MapCamp>,
    val objectiveChannelTime: String? = null,
    val hasGlobes: Boolean = false,
    val inRanked: Boolean = true
) {
    ALTERAC_PASS(
        id = "alterac_pass",
        imageResId = R.drawable.storm_ui_homescreenbackground_wcav,
        nameResId = R.string.map_alterac_pass,
        fountainResId = R.drawable.storm_ui_ingame_targetinfopanel_unit_wcav_building_moonwell_horde,
        minionResId = R.drawable.storm_ui_ingame_targetinfopanel_unit_wcav_minion_melee_horde,
        objectiveResId = R.drawable.ui_ingame_mapmechanic_loadscreen_wcav_icon1_horde,
        objectiveSpawnTime = "3m",
        camps = listOf(
            MapCamp(MapCampType.BOSS, 2,
                R.drawable.storm_ui_ingame_targetinfopanel_unit_wcav_boss_giant,
                respawnTime = "5m", spawnTime = "5m"),
            MapCamp(MapCampType.SIEGE, 2,
                R.drawable.storm_ui_ingame_targetinfopanel_unit_wcav_merc_gnoll,
                respawnTime = "1m 30s")
        ),
        objectiveChannelTime = "3s"
    ),
    BATTLEFIELD_OF_ETERNITY(
        id = "battlefield_of_eternity",
        imageResId = R.drawable.ui_ingame_mapmechanic_loadscreen_battlefieldofeternity,
        nameResId = R.string.map_battlefield_of_eternity,
        fountainResId = R.drawable.storm_ui_ingame_targetinfopanel_unit_boe_building_moonwell_hell,
        minionResId = R.drawable.storm_ui_ingame_targetinfopanel_unit_boe_minion_melee_hell,
        objectiveResId = R.drawable.ui_ingame_mapmechanic_loadscreen_eternity_icon1,
        objectiveSpawnTime = "3m",
        camps = listOf(
            MapCamp(MapCampType.BRUISER, 2,
                R.drawable.storm_ui_ingame_targetinfopanel_unit_boe_merc_bruiser_summoner,
                respawnTime = "4m"),
            MapCamp(MapCampType.SIEGE, 2,
                R.drawable.storm_ui_ingame_targetinfopanel_unit_boe_merc_siege,
                respawnTime = "3m")
        )
    ),
    BLACKHEARTS_BAY(
        id = "blackhearts_bay",
        imageResId = R.drawable.ui_ingame_mapmechanic_loadscreen_blackheartsbay,
        nameResId = R.string.map_blackhearts_bay,
        fountainResId = R.drawable.storm_ui_ingame_targetinfopanel_unit_bb_building_moonwell,
        minionResId = R.drawable.storm_ui_ingame_targetinfopanel_unit_bb_minion_melee,
        objectiveResId = R.drawable.ui_ingame_mapmechanic_loadscreen_blackheartsbay_icon2,
        objectiveSpawnTime = "1m 30s",
        camps = listOf(
            MapCamp(MapCampType.BOSS, 1,
                R.drawable.storm_ui_ingame_targetinfopanel_unit_bb_boss_gravegolem,
                respawnTime = "5m", spawnTime = "5m"),
            MapCamp(MapCampType.BRUISER, 3,
                R.drawable.storm_ui_ingame_targetinfopanel_unit_bb_merc_bruiser_wizard,
                respawnTime = "4m"),
            MapCamp(MapCampType.DOUBLOON, 4,
                R.drawable.storm_ui_ingame_targetinfopanel_unit_bb_merc_doublooncamp,
                respawnTime = "2m 30s"),
            MapCamp(MapCampType.SIEGE, 2,
                R.drawable.storm_ui_ingame_targetinfopanel_unit_bb_merc_siege_pirate,
                respawnTime = "3m")
        ),
        objectiveChannelTime = "5s",
        inRanked = false
    ),
    BRAXIS_HOLDOUT(
        id = "braxis_holdout",
        imageResId = R.drawable.storm_ui_homescreenbackground_braxisholdout,
        nameResId = R.string.map_braxis_holdout,
        fountainResId = R.drawable.storm_ui_ingame_targetinfopanel_unit_braxis_building_moonwell,
        minionResId = R.drawable.storm_ui_ingame_targetinfopanel_unit_braxis_minion_melee,
        objectiveResId = R.drawable.ui_ingame_mapmechanic_loadscreen_braxisholdout_icon2,
        objectiveSpawnTime = "1m 30s",
        camps = listOf(
            MapCamp(MapCampType.BOSS, 1,
                R.drawable.storm_ui_ingame_targetinfopanel_unit_braxis_boss,
                respawnTime = "5m", spawnTime = "5m"),
            MapCamp(MapCampType.BRUISER, 2,
                R.drawable.storm_ui_ingame_targetinfopanel_unit_braxis_merc_bruiser_raven,
                respawnTime = "4m"),
            MapCamp(MapCampType.SIEGE, 2,
                R.drawable.storm_ui_ingame_targetinfopanel_unit_braxis_merc_siege,
                respawnTime = "3m")
        ),
        hasGlobes = true
    ),
    CURSED_HOLLOW(
        id = "cursed_hollow",
        imageResId = R.drawable.ui_ingame_mapmechanic_loadscreen_cursedhollow,
        nameResId = R.string.map_cursed_hollow,
        fountainResId = R.drawable.storm_ui_ingame_targetinfopanel_unit_bb_building_moonwell,
        minionResId = R.drawable.storm_ui_ingame_targetinfopanel_unit_bb_minion_melee,
        objectiveResId = R.drawable.ui_ingame_mapmechanic_loadscreen_cursedhollow_icon1,
        objectiveSpawnTime = "3m",
        camps = listOf(
            MapCamp(MapCampType.BOSS, 2,
                R.drawable.storm_ui_ingame_targetinfopanel_unit_cursedhollow_boss_gravegolem,
                respawnTime = "5m", spawnTime = "5m"),
            MapCamp(MapCampType.BRUISER, 2,
                R.drawable.storm_ui_ingame_targetinfopanel_unit_cursedhollow_merc_bruiser_wizard,
                respawnTime = "4m"),
            MapCamp(MapCampType.SIEGE, 2,
                R.drawable.storm_ui_ingame_targetinfopanel_unit_cursedhollow_merc_siege,
                respawnTime = "3m")
        ),
        objectiveChannelTime = "6s"
    ),
    DRAGON_SHIRE(
        id = "dragon_shire",
        imageResId = R.drawable.ui_ingame_mapmechanic_loadscreen_dragonshire,
        nameResId = R.string.map_dragon_shire,
        fountainResId = R.drawable.storm_ui_ingame_targetinfopanel_unit_bb_building_moonwell,
        minionResId = R.drawable.storm_ui_ingame_targetinfopanel_unit_bb_minion_melee,
        objectiveResId = R.drawable.ui_ingame_mapmechanic_loadscreen_dragonshire_icon3,
        objectiveSpawnTime = "1m 30s",
        camps = listOf(
            MapCamp(MapCampType.BRUISER, 3,
                R.drawable.storm_ui_ingame_targetinfopanel_unit_dragonshire_merc_bruiser_wizard,
                respawnTime = "4m"),
            MapCamp(MapCampType.SIEGE, 2,
                R.drawable.storm_ui_ingame_targetinfopanel_unit_dragonshire_merc_siege,
                respawnTime = "3m")
        ),
        objectiveChannelTime = "3s"
    ),
    GARDEN_OF_TERROR(
        id = "garden_of_terror",
        imageResId = R.drawable.ui_ingame_mapmechanic_loadscreen_gardenofterror,
        nameResId = R.string.map_garden_of_terror,
        fountainResId = R.drawable.storm_ui_ingame_targetinfopanel_unit_bb_building_moonwell,
        minionResId = R.drawable.storm_ui_ingame_targetinfopanel_unit_bb_minion_melee,
        objectiveResId = R.drawable.ui_ingame_mapmechanic_loadscreen_gardenofterror_icon1,
        objectiveSpawnTime = "2m 30s",
        camps = listOf(
            MapCamp(MapCampType.BRUISER, 2,
                R.drawable.storm_ui_ingame_targetinfopanel_unit_dragonshire_merc_bruiser_wizard,
                respawnTime = "4m"),
            MapCamp(MapCampType.SIEGE, 4,
                R.drawable.storm_ui_ingame_targetinfopanel_unit_dragonshire_merc_siege,
                respawnTime = "3m")
        ),
        objectiveChannelTime = "6s"
    ),
    HANAMURA_TEMPLE(
        id = "hanamura_temple",
        imageResId = R.drawable.ui_ingame_mapmechanic_loadscreen_hanamura_rework,
        nameResId = R.string.map_hanamura_temple,
        fountainResId = R.drawable.storm_ui_ingame_targetinfopanel_unit_hanamura_building_moonwell,
        minionResId = R.drawable.storm_ui_ingame_targetinfopanel_unit_hanamura_minion_melee,
        objectiveResId = R.drawable.ui_ingame_mapmechanic_loadscreen_hanamura_icon1,
        objectiveSpawnTime = "3m",
        camps = listOf(
            MapCamp(MapCampType.FORTIFICATION, 2,
                R.drawable.storm_ui_ingame_targetinfopanel_unit_hanamura_merc_turret,
                respawnTime = "2m 30s"),
            MapCamp(MapCampType.RECON, 2,
                R.drawable.storm_ui_ingame_targetinfopanel_unit_hanamura_merc_supportcamp,
                respawnTime = "Instant"),
            MapCamp(MapCampType.SAMURAI, 2,
                R.drawable.storm_ui_ingame_targetinfopanel_unit_hanamura_merc_yakuza,
                respawnTime = "2m 30s")
        ),
        inRanked = false
    ),
    HAUNTED_MINES(
        id = "haunted_mines",
        imageResId = R.drawable.ui_ingame_mapmechanic_loadscreen_hauntedmines,
        nameResId = R.string.map_haunted_mines,
        fountainResId = R.drawable.storm_ui_ingame_targetinfopanel_unit_bb_building_moonwell,
        minionResId = R.drawable.storm_ui_ingame_targetinfopanel_unit_bb_minion_melee,
        objectiveResId = R.drawable.ui_ingame_mapmechanic_loadscreen_hauntedmines_icon2,
        objectiveSpawnTime = "3m",
        camps = listOf(
            MapCamp(MapCampType.SAPPER, 2,
                R.drawable.storm_ui_ingame_targetinfopanel_unit_towersofdoom_merc_siege,
                respawnTime = "2m 30s"),
            MapCamp(MapCampType.SIEGE, 2,
                R.drawable.storm_ui_ingame_targetinfopanel_unit_cursedhollow_merc_siege,
                respawnTime = "3m")
        )
    ),
    INFERNAL_SHRINES(
        id = "infernal_shrines",
        imageResId = R.drawable.ui_ingame_mapmechanic_loadscreen_shrines,
        nameResId = R.string.map_infernal_shrines,
        fountainResId = R.drawable.storm_ui_ingame_targetinfopanel_unit_boe_building_moonwell_heaven,
        minionResId = R.drawable.storm_ui_ingame_targetinfopanel_unit_boe_minion_melee_heaven,
        objectiveResId = R.drawable.ui_ingame_mapmechanic_loadscreen_shrines_icon1,
        objectiveSpawnTime = "3m",
        camps = listOf(
            MapCamp(MapCampType.BRUISER, 2,
                R.drawable.storm_ui_ingame_targetinfopanel_unit_boe_merc_bruiser_summoner,
                respawnTime = "4m"),
            MapCamp(MapCampType.SIEGE, 3,
                R.drawable.storm_ui_ingame_targetinfopanel_unit_boe_merc_siege,
                respawnTime = "3m")
        )
    ),
    SKY_TEMPLE(
        id = "sky_temple",
        imageResId = R.drawable.ui_ingame_mapmechanic_loadscreen_skytemple,
        nameResId = R.string.map_sky_temple,
        fountainResId = R.drawable.storm_ui_ingame_targetinfopanel_unit_skytemple_building_moonwell,
        minionResId = R.drawable.storm_ui_ingame_targetinfopanel_unit_skytemple_minion_melee,
        objectiveResId = R.drawable.ui_ingame_mapmechanic_loadscreen_skytemple_icon2,
        objectiveSpawnTime = "3m",
        camps = listOf(
            MapCamp(MapCampType.BOSS, 1,
                R.drawable.storm_ui_ingame_targetinfopanel_unit_skytemple_boss,
                respawnTime = "5m", spawnTime = "5m"),
            MapCamp(MapCampType.BRUISER, 2,
                R.drawable.storm_ui_ingame_targetinfopanel_unit_skytemple_merc_bruiser_caster,
                respawnTime = "4m"),
            MapCamp(MapCampType.SIEGE, 2,
                R.drawable.storm_ui_ingame_targetinfopanel_unit_skytemple_merc_siege,
                respawnTime = "3m")
        )
    ),
    TOMB_OF_THE_SPIDER_QUEEN(
        id = "tomb_of_the_spider_queen",
        imageResId = R.drawable.ui_ingame_mapmechanic_loadscreen_tombofthespiderqueen,
        nameResId = R.string.map_tomb_of_the_spider_queen,
        fountainResId = R.drawable.storm_ui_ingame_targetinfopanel_unit_tombofthespiderqueen_building_moonwell,
        minionResId = R.drawable.storm_ui_ingame_targetinfopanel_unit_skytemple_minion_melee,
        objectiveResId = R.drawable.ui_ingame_mapmechanic_loadscreen_tombofthespiderqueen_icon2,
        objectiveSpawnTime = "30s",
        camps = listOf(
            MapCamp(MapCampType.BOSS, 1,
                R.drawable.storm_ui_ingame_targetinfopanel_unit_tombofthespiderqueen_boss,
                respawnTime = "5m", spawnTime = "5m"),
            MapCamp(MapCampType.BRUISER, 2,
                R.drawable.storm_ui_ingame_targetinfopanel_unit_tombofthespiderqueen_merc_bruiser_caster,
                respawnTime = "4m"),
            MapCamp(MapCampType.SIEGE, 1,
                R.drawable.storm_ui_ingame_targetinfopanel_unit_skytemple_merc_siege,
                respawnTime = "3m")
        ),
        objectiveChannelTime = "6s"
    ),
    TOWERS_OF_DOOM(
        id = "towers_of_doom",
        imageResId = R.drawable.ui_ingame_mapmechanic_loadscreen_towersofdoom,
        nameResId = R.string.map_towers_of_doom,
        fountainResId = R.drawable.storm_ui_ingame_targetinfopanel_unit_bb_building_moonwell,
        minionResId = R.drawable.storm_ui_ingame_targetinfopanel_unit_bb_minion_melee,
        objectiveResId = R.drawable.ui_ingame_mapmechanic_loadscreen_towersofdoom_icon2,
        objectiveSpawnTime = "3m",
        camps = listOf(
            MapCamp(MapCampType.BOSS, 1,
                R.drawable.storm_ui_ingame_targetinfopanel_unit_towersofdoom_boss,
                respawnTime = "5m", spawnTime = "5m"),
            MapCamp(MapCampType.SAPPER, 3,
                R.drawable.storm_ui_ingame_targetinfopanel_unit_towersofdoom_merc_siege,
                respawnTime = "2m 30s")
        ),
        objectiveChannelTime = "6s"
    ),
    VOLSKAYA_FOUNDRY(
        id = "volskaya_foundry",
        imageResId = R.drawable.storm_ui_homescreenbackground_volskaya,
        nameResId = R.string.map_volskaya_foundry,
        fountainResId = R.drawable.storm_ui_ingame_targetinfopanel_unit_volskaya_building_moonwell,
        minionResId = R.drawable.storm_ui_ingame_targetinfopanel_unit_volskaya_minion_melee,
        objectiveResId = R.drawable.ui_ingame_mapmechanic_loadscreen_volskaya_icon2,
        objectiveSpawnTime = "3m",
        camps = listOf(
            MapCamp(MapCampType.FORTIFICATION, 2,
                R.drawable.storm_ui_ingame_targetinfopanel_unit_volskaya_merc_turret,
                respawnTime = "2m 30s"),
            MapCamp(MapCampType.SIEGE, 2,
                R.drawable.storm_ui_ingame_targetinfopanel_unit_volskaya_merc_siege,
                respawnTime = "3m"),
            MapCamp(MapCampType.SUPPORT_BOSS, 1,
                R.drawable.storm_ui_ingame_targetinfopanel_unit_volskaya_merc_support_robot,
                respawnTime = "3m")
        ),
        objectiveChannelTime = "1s"
    ),
    WARHEAD_JUNCTION(
        id = "warhead_junction",
        imageResId = R.drawable.storm_ui_homescreenbackground_warhead,
        nameResId = R.string.map_warhead_junction,
        fountainResId = R.drawable.storm_ui_ingame_targetinfopanel_unit_braxis_building_moonwell,
        minionResId = R.drawable.storm_ui_ingame_targetinfopanel_unit_braxis_minion_melee,
        objectiveResId = R.drawable.ui_ingame_mapmechanic_loadscreen_warhead_icon2,
        objectiveSpawnTime = "3m",
        camps = listOf(
            MapCamp(MapCampType.BRUISER, 2,
                R.drawable.storm_ui_ingame_targetinfopanel_unit_warheadjunction_merc_bruiser_raven,
                respawnTime = "4m"),
            MapCamp(MapCampType.SIEGE, 2,
                R.drawable.storm_ui_ingame_targetinfopanel_unit_warheadjunction_merc_siege,
                respawnTime = "3m"),
            MapCamp(MapCampType.BOSS, 1,
                R.drawable.storm_ui_ingame_targetinfopanel_unit_warheadjunction_boss,
                respawnTime = "5m", spawnTime = "5m")
        ),
        objectiveChannelTime = "5s to collect\n3s to use",
        inRanked = false
    );

    val route: String get() = "maps/battleground/$id"
}
