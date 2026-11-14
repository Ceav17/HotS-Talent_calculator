package com.github.ceav17.hots_talent_calculator

import java.io.File
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL
import java.nio.file.Files
import java.nio.file.StandardCopyOption
import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject

internal data class CachedLeaderboard(
    val snapshot: LeaderboardSnapshot,
    val checkedAt: Long,
    val etag: String? = null,
    val lastModified: String? = null,
    val seasonId: Int = LEADERBOARD_INITIAL_SEASON_ID
)

internal data class LeaderboardResponse(
    val snapshot: LeaderboardSnapshot?,
    val etag: String?,
    val lastModified: String?
)

internal fun interface LeaderboardSource {
    fun fetch(region: LeaderboardRegion, seasonId: Int, cached: CachedLeaderboard?): LeaderboardResponse
}

internal class BlizzardLeaderboardSource(
    private val connection: (URL) -> HttpURLConnection = { it.openConnection() as HttpURLConnection }
) : LeaderboardSource {
    override fun fetch(region: LeaderboardRegion, seasonId: Int, cached: CachedLeaderboard?): LeaderboardResponse {
        require(cached == null || cached.seasonId == seasonId)
        val request = connection(URL(region.url(seasonId)))
        try {
            request.connectTimeout = 10_000
            request.readTimeout = 10_000
            request.instanceFollowRedirects = false
            request.setRequestProperty("Accept", "text/html")
            request.setRequestProperty("User-Agent", "HotS-Talent-calculator/leaderboards")
            cached?.etag?.let { request.setRequestProperty("If-None-Match", it) }
            cached?.lastModified?.let { request.setRequestProperty("If-Modified-Since", it) }
            val status = request.responseCode
            val etag = request.getHeaderField("ETag")
            val modified = request.getHeaderField("Last-Modified")
            if (status == HttpURLConnection.HTTP_NOT_MODIFIED && cached != null) {
                return LeaderboardResponse(null, etag ?: cached.etag, modified ?: cached.lastModified)
            }
            if (status != HttpURLConnection.HTTP_OK) throw IOException("Leaderboard HTTP status: $status")
            val snapshot = request.inputStream.bufferedReader(Charsets.UTF_8).use {
                LeaderboardParser.read(it, cached?.snapshot, region.path(seasonId))
            }
            return LeaderboardResponse(snapshot, etag, modified)
        } finally {
            request.disconnect()
        }
    }
}

internal class LeaderboardCache(private val directory: File) {
    private fun file(region: LeaderboardRegion, seasonId: Int) = File(directory, "storm-$seasonId-${region.slug}.json")

    fun load(region: LeaderboardRegion, seasonId: Int = LEADERBOARD_INITIAL_SEASON_ID): CachedLeaderboard? = try {
        val file = file(region, seasonId)
        if (!file.isFile || file.length() > LEADERBOARD_MAX_PAGE_CHARS) null else {
            val root = JSONObject(file.readText())
            require(root.getInt("schema") == 1 && root.getInt("seasonId") == seasonId)
            require(root.getString("region") == region.slug)
            val players = root.getJSONArray("players")
            require(players.length() <= 1000)
            val snapshot = LeaderboardSnapshot(
                season = root.getString("season"),
                updatedAt = root.getLong("updatedAt"),
                players = List(players.length()) { index ->
                    val player = players.getJSONObject(index)
                    val roles = player.getJSONArray("roles")
                    require(roles.length() == LeaderboardRole.entries.size)
                    LeaderboardPlayer(
                        player.getInt("rank"), player.getString("battleTag"),
                        if (player.isNull("highestRank")) null else player.getInt("highestRank"),
                        player.getInt("points"), player.getInt("games"), player.getInt("wins"),
                        List(roles.length()) { roles.getInt(it) }
                    )
                }
            )
            CachedLeaderboard(snapshot, root.getLong("checkedAt"), root.optionalString("etag"), root.optionalString("lastModified"), seasonId)
        }
    } catch (_: IOException) {
        null
    } catch (_: JSONException) {
        null
    } catch (_: IllegalArgumentException) {
        null
    }

    fun save(region: LeaderboardRegion, cached: CachedLeaderboard) {
        val root = JSONObject().put("schema", 1).put("seasonId", cached.seasonId)
            .put("region", region.slug).put("season", cached.snapshot.season)
            .put("updatedAt", cached.snapshot.updatedAt).put("checkedAt", cached.checkedAt)
            .put("etag", cached.etag ?: JSONObject.NULL).put("lastModified", cached.lastModified ?: JSONObject.NULL)
            .put("players", JSONArray().apply {
                cached.snapshot.players.forEach { player ->
                    put(JSONObject().put("rank", player.rank).put("battleTag", player.battleTag)
                        .put("highestRank", player.highestRank ?: JSONObject.NULL).put("points", player.points)
                        .put("games", player.games).put("wins", player.wins).put("roles", JSONArray(player.roles)))
                }
            })
        writeLeaderboardFile(file(region, cached.seasonId), root.toString())
    }

    private fun JSONObject.optionalString(name: String) = if (isNull(name)) null else getString(name).takeIf { it.isNotBlank() }
}

// Store discovered seasons outside cacheDir so clearing cached tables does not reset discovery.
internal class LeaderboardSeasonStore(private val directory: File) {
    private fun file(region: LeaderboardRegion) = File(directory, "${region.slug}.txt")

    fun load(region: LeaderboardRegion): Int = try {
        val file = file(region)
        val value = if (file.isFile && file.length() <= 16) file.readText().trim().toIntOrNull() else null
        value?.takeIf { it in LEADERBOARD_INITIAL_SEASON_ID until Int.MAX_VALUE }
            ?: LEADERBOARD_INITIAL_SEASON_ID
    } catch (_: IOException) {
        LEADERBOARD_INITIAL_SEASON_ID
    }

    fun save(region: LeaderboardRegion, seasonId: Int) {
        require(seasonId in LEADERBOARD_INITIAL_SEASON_ID until Int.MAX_VALUE)
        writeLeaderboardFile(file(region), seasonId.toString())
    }
}

private fun writeLeaderboardFile(file: File, contents: String) {
    val directory = requireNotNull(file.parentFile)
    if (!directory.isDirectory && !directory.mkdirs() && !directory.isDirectory) {
        throw IOException("Cannot create leaderboard storage")
    }
    val temporary = File.createTempFile("leaderboard-", ".tmp", directory)
    try {
        temporary.writeText(contents)
        Files.move(temporary.toPath(), file.toPath(), StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE)
    } finally {
        temporary.delete()
    }
}

internal enum class LeaderboardProblem { OFFLINE, NETWORK, FORMAT, CACHE_WRITE }

internal data class LeaderboardResult(
    val snapshot: LeaderboardSnapshot? = null,
    val cached: Boolean = false,
    val expired: Boolean = false,
    val problem: LeaderboardProblem? = null
)

internal class LeaderboardRepository(
    private val cache: LeaderboardCache,
    private val seasons: LeaderboardSeasonStore,
    private val source: LeaderboardSource = BlizzardLeaderboardSource(),
    private val now: () -> Long = System::currentTimeMillis
) {
    fun load(region: LeaderboardRegion, online: Boolean): LeaderboardResult {
        val seasonId = seasons.load(region)
        val saved = cache.load(region, seasonId)
        fun fallback(problem: LeaderboardProblem): LeaderboardResult {
            // Revalidating an unchanged page never extends the age of Blizzard's data.
            val usable = saved?.snapshot?.usableOffline(now()) == true
            return LeaderboardResult(
                snapshot = saved?.snapshot?.takeIf { usable },
                cached = usable, expired = saved != null && !usable, problem = problem
            )
        }
        if (!online) return fallback(LeaderboardProblem.OFFLINE)

        // A reachable but empty season is normal during placements. Keep using the previous one.
        val nextSeason = seasonId + 1
        val next = try {
            source.fetch(region, nextSeason, null)
        } catch (_: IOException) {
            null
        }
        next?.snapshot?.takeIf { it.players.isNotEmpty() }?.let { snapshot ->
            return saveResult(region, nextSeason, snapshot, next, advanceSeason = true)
        }

        val response = try {
            source.fetch(region, seasonId, saved)
        } catch (_: InvalidLeaderboardException) {
            return fallback(LeaderboardProblem.FORMAT)
        } catch (_: IOException) {
            return fallback(LeaderboardProblem.NETWORK)
        }
        val snapshot = response.snapshot ?: saved?.snapshot ?: return fallback(LeaderboardProblem.FORMAT)
        if (saved != null && snapshot.updatedAt < saved.snapshot.updatedAt) return fallback(LeaderboardProblem.FORMAT)
        return saveResult(region, seasonId, snapshot, response)
    }

    private fun saveResult(
        region: LeaderboardRegion,
        seasonId: Int,
        snapshot: LeaderboardSnapshot,
        response: LeaderboardResponse,
        advanceSeason: Boolean = false
    ): LeaderboardResult {
        val fresh = CachedLeaderboard(snapshot, now(), response.etag, response.lastModified, seasonId)
        val problem = try {
            cache.save(region, fresh)
            if (advanceSeason) seasons.save(region, seasonId)
            null
        } catch (_: IOException) {
            LeaderboardProblem.CACHE_WRITE
        }
        return LeaderboardResult(snapshot = snapshot, problem = problem)
    }
}
