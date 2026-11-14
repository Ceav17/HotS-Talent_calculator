package com.github.ceav17.hots_talent_calculator

import java.io.IOException
import java.io.Reader
import java.time.LocalDateTime
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import java.util.Locale
import org.jsoup.Jsoup
import org.jsoup.nodes.Document

internal const val LEADERBOARD_INITIAL_SEASON_ID = 34
internal const val LEADERBOARD_CACHE_MAX_AGE_MS = 24 * 60 * 60 * 1000L
internal const val LEADERBOARD_MAX_PAGE_CHARS = 2 * 1024 * 1024

internal enum class LeaderboardRegion(val slug: String) {
    AMERICAS("americas"), ASIA("asia"), EUROPE("europe");

    fun path(seasonId: Int) = "/leaderboards/storm/$seasonId/$slug"

    fun url(seasonId: Int) = "https://heroesofthestorm.blizzard.com/en-us${path(seasonId)}/"
}

// This order matches the six SVG headers in Blizzard's table.
internal enum class LeaderboardRole {
    TANK, BRUISER, SUPPORT, HEALER, MELEE_ASSASSIN, RANGED_ASSASSIN
}

internal data class LeaderboardPlayer(
    val rank: Int,
    val battleTag: String,
    val highestRank: Int?,
    val points: Int,
    val games: Int,
    val wins: Int,
    val roles: List<Int>
) {
    init {
        require(rank in 1..1000 && battleTag.isNotBlank() && battleTag.length <= 128)
        require(highestRank == null || highestRank in 1..1000)
        require(games >= 0 && wins in 0..games)
        require(roles.size == LeaderboardRole.entries.size && roles.all { it in 0..100 })
    }

    fun rolePercentage(role: LeaderboardRole): Int {
        // Keep the source order in cache; correct Blizzard's swapped columns when displaying them.
        val sourceRole = when (role) {
            LeaderboardRole.HEALER -> LeaderboardRole.MELEE_ASSASSIN
            LeaderboardRole.MELEE_ASSASSIN -> LeaderboardRole.HEALER
            else -> role
        }
        return roles[sourceRole.ordinal]
    }
}

internal data class LeaderboardSnapshot(
    val season: String,
    val updatedAt: Long,
    val players: List<LeaderboardPlayer>
) {
    init {
        require(season.isNotBlank() && season.length <= 100 && updatedAt > 0)
        require(players.size <= 1000 && players.map { it.rank }.distinct().size == players.size)
    }

    fun usableOffline(now: Long): Boolean = now - updatedAt in 0 until LEADERBOARD_CACHE_MAX_AGE_MS
}

internal class InvalidLeaderboardException(cause: Throwable? = null) :
    IOException("Blizzard leaderboard format was not recognized", cause)

internal object LeaderboardParser {
    private val dateFormat = DateTimeFormatter.ofPattern("EEEE, MMMM d, uuuu h:mm a", Locale.ENGLISH)

    private fun metadata(document: Document): Pair<String, Long> {
        val season = requireNotNull(document.selectFirst(".Leaderboards-controlsTitle")).text()
        val date = requireNotNull(document.selectFirst(".Leaderboards-updatedDate")).text()
        val instant = LocalDateTime.parse(date, dateFormat).toInstant(ZoneOffset.UTC)
        require(season.isNotBlank())
        return season to instant.toEpochMilli()
    }

    private fun verifySeason(document: Document, expectedPath: String?) {
        if (expectedPath == null) return
        val selected = requireNotNull(document.selectFirst("#leaderboards-region-select option[selected]"))
        require(selected.attr("data-url").trimEnd('/') == expectedPath)
    }

    fun parse(html: String, expectedPath: String? = null): LeaderboardSnapshot = checked {
        require(html.length <= LEADERBOARD_MAX_PAGE_CHARS)
        val document = Jsoup.parse(html)
        verifySeason(document, expectedPath)
        val (season, updatedAt) = metadata(document)
        val table = requireNotNull(document.selectFirst("table.RankingTable"))
        require(table.select("th.RankingTable-header--role").size == LeaderboardRole.entries.size)
        val body = requireNotNull(table.selectFirst("tbody.RankingTable-body"))
        val rows = body.select("tr")
        require(rows.size <= 1000)
        val players = rows.map { row ->
            fun cell(name: String): String = requireNotNull(row.selectFirst("td.RankingTable-cell--$name")).text()
            val roles = row.children().filter {
                it.tagName() == "td" && it.hasClass("RankingTable-cell") &&
                    it.classNames().none { name -> name.startsWith("RankingTable-cell--") }
            }
                .map { cell ->
                    val text = cell.text()
                    require(text.endsWith('%'))
                    number(text.removeSuffix("%"))
                }
            val highestRank = cell("highestRank")
            LeaderboardPlayer(
                rank = number(cell("rank")),
                battleTag = cell("battletag"),
                highestRank = if (highestRank in setOf("", "-", "–", "—")) null else number(highestRank),
                points = number(cell("points")),
                games = number(cell("games")),
                wins = number(cell("wins")),
                roles = roles
            )
        }.sortedBy { it.rank }
        LeaderboardSnapshot(season, updatedAt, players)
    }

    // Last Updated precedes the table. Stop reading early if that timestamp is unchanged.
    // A null result means the caller can keep the cached rows; HTTP 304 avoids the body entirely.
    fun read(reader: Reader, cached: LeaderboardSnapshot?, expectedPath: String? = null): LeaderboardSnapshot? = checked {
        val text = StringBuilder()
        val buffer = CharArray(2048)
        var headerChecked = false
        while (true) {
            val count = reader.read(buffer)
            if (count < 0) break
            require(text.length + count <= LEADERBOARD_MAX_PAGE_CHARS)
            text.append(buffer, 0, count)
            if (!headerChecked) {
                val marker = text.indexOf("Leaderboards-updatedDate")
                if (marker >= 0 && text.indexOf("</span>", marker) >= 0) {
                    val document = Jsoup.parse(text.toString())
                    verifySeason(document, expectedPath)
                    val (season, updatedAt) = metadata(document)
                    if (cached?.updatedAt == updatedAt && cached.season == season) return@checked null
                    headerChecked = true
                } else {
                    require(text.length <= 64 * 1024)
                }
            }
        }
        parse(text.toString(), expectedPath)
    }

    private fun number(value: String): Int {
        val normalized = value.replace(",", "").replace("\u00a0", "").trim()
        require(normalized.matches(Regex("-?[0-9]+")))
        return normalized.toInt()
    }

    private inline fun <T> checked(block: () -> T): T = try {
        block()
    } catch (error: IllegalArgumentException) {
        throw InvalidLeaderboardException(error)
    } catch (error: java.time.DateTimeException) {
        throw InvalidLeaderboardException(error)
    }
}
