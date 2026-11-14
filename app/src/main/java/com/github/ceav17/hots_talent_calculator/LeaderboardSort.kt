package com.github.ceav17.hots_talent_calculator

import java.text.Collator
import java.util.Locale

internal enum class LeaderboardSortColumn(
    val defaultDescending: Boolean = false,
    val role: LeaderboardRole? = null
) {
    RANK, BATTLE_TAG, HIGHEST_RANK, POINTS(true), GAMES(true), WINS(true),
    TANK(true, LeaderboardRole.TANK),
    BRUISER(true, LeaderboardRole.BRUISER),
    SUPPORT(true, LeaderboardRole.SUPPORT),
    HEALER(true, LeaderboardRole.HEALER),
    MELEE_ASSASSIN(true, LeaderboardRole.MELEE_ASSASSIN),
    RANGED_ASSASSIN(true, LeaderboardRole.RANGED_ASSASSIN)
}

internal fun sortLeaderboardPlayers(
    players: List<LeaderboardPlayer>,
    column: LeaderboardSortColumn,
    descending: Boolean = column.defaultDescending
): List<LeaderboardPlayer> {
    val collator = if (column == LeaderboardSortColumn.BATTLE_TAG) {
        Collator.getInstance(Locale.ROOT).apply { strength = Collator.PRIMARY }
    } else null
    return players.sortedWith { left, right ->
        // Unknown highest ranks stay at the bottom in either direction.
        if (column == LeaderboardSortColumn.HIGHEST_RANK &&
            (left.highestRank == null || right.highestRank == null)
        ) {
            when {
                left.highestRank == null && right.highestRank == null -> left.rank.compareTo(right.rank)
                left.highestRank == null -> 1
                else -> -1
            }
        } else {
            val comparison = when (column) {
                LeaderboardSortColumn.RANK -> left.rank.compareTo(right.rank)
                LeaderboardSortColumn.BATTLE_TAG -> requireNotNull(collator).compare(left.battleTag, right.battleTag)
                LeaderboardSortColumn.HIGHEST_RANK -> requireNotNull(left.highestRank).compareTo(requireNotNull(right.highestRank))
                // Rank is authoritative, including ties in displayed points.
                LeaderboardSortColumn.POINTS -> right.rank.compareTo(left.rank)
                LeaderboardSortColumn.GAMES -> left.games.compareTo(right.games)
                LeaderboardSortColumn.WINS -> left.wins.compareTo(right.wins)
                else -> {
                    val role = requireNotNull(column.role)
                    left.rolePercentage(role).compareTo(right.rolePercentage(role))
                }
            }
            val directed = if (descending) -comparison else comparison
            if (directed != 0) directed else left.rank.compareTo(right.rank)
        }
    }
}
