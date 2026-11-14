package com.github.ceav17.hots_talent_calculator

import android.app.Application
import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import androidx.core.content.edit
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import java.io.File
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

internal data class LeaderboardUiState(
    val region: LeaderboardRegion = LeaderboardRegion.EUROPE,
    val loading: Boolean = false,
    val result: LeaderboardResult = LeaderboardResult()
)

internal class LeaderboardViewModel(application: Application) : AndroidViewModel(application) {
    private val preferences = application.getSharedPreferences("leaderboards", Context.MODE_PRIVATE)
    private val repository = LeaderboardRepository(
        LeaderboardCache(File(application.cacheDir, "leaderboards")),
        LeaderboardSeasonStore(File(application.filesDir, "leaderboard-seasons"))
    )
    private val connectivity = application.getSystemService(ConnectivityManager::class.java)
    private val results = mutableMapOf<LeaderboardRegion, LeaderboardResult>()
    private val requests = mutableMapOf<LeaderboardRegion, Job>()
    private val mutableState = MutableStateFlow(
        LeaderboardUiState(region = preferences.getString("last_region", null).let { slug ->
            LeaderboardRegion.entries.firstOrNull { it.slug == slug } ?: LeaderboardRegion.EUROPE
        })
    )
    val state = mutableState.asStateFlow()

    private val networkCallback = object : ConnectivityManager.NetworkCallback() {
        override fun onLost(network: Network) {
            viewModelScope.launch {
                if (!online()) refresh()
            }
        }

        override fun onCapabilitiesChanged(network: Network, networkCapabilities: NetworkCapabilities) {
            if (networkCapabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)) {
                viewModelScope.launch {
                    if (mutableState.value.result.problem == LeaderboardProblem.OFFLINE) refresh()
                }
            }
        }
    }

    init {
        connectivity.registerDefaultNetworkCallback(networkCallback)
    }

    override fun onCleared() {
        connectivity.unregisterNetworkCallback(networkCallback)
    }

    private fun online(): Boolean = connectivity.getNetworkCapabilities(connectivity.activeNetwork)
        ?.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) == true

    fun selectRegion(region: LeaderboardRegion) {
        if (region != mutableState.value.region) {
            preferences.edit { putString("last_region", region.slug) }
            val previous = results[region] ?: LeaderboardResult()
            mutableState.value = LeaderboardUiState(
                region = region,
                loading = requests[region]?.isActive == true,
                result = previous.takeIf { it.snapshot?.usableOffline(System.currentTimeMillis()) == true }
                    ?: LeaderboardResult()
            )
        }
        refresh()
    }

    fun refresh() {
        val region = mutableState.value.region
        // One request per region; quick tab changes cannot display another region's response.
        if (requests[region]?.isActive == true) return
        val current = mutableState.value.result
        val visible = current.takeIf { it.snapshot?.usableOffline(System.currentTimeMillis()) == true }
            ?: LeaderboardResult()
        mutableState.value = mutableState.value.copy(loading = true, result = visible)
        requests[region] = viewModelScope.launch {
            try {
                var result = withContext(Dispatchers.IO) { repository.load(region, online()) }
                if (result.problem == LeaderboardProblem.NETWORK && !online()) {
                    result = result.copy(problem = LeaderboardProblem.OFFLINE)
                }
                results[region] = result
                if (mutableState.value.region == region) {
                    mutableState.value = LeaderboardUiState(region, result = result)
                }
            } finally {
                requests.remove(region)
            }
        }
    }
}
