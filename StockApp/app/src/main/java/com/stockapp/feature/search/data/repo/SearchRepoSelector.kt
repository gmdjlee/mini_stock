package com.stockapp.feature.search.data.repo

import android.util.Log
import com.stockapp.core.config.FeatureFlagRepo
import com.stockapp.core.config.FeatureFlags
import com.stockapp.feature.search.domain.model.Stock
import com.stockapp.feature.search.domain.repo.SearchRepo
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

private const val TAG = "SearchRepoSelector"

/**
 * Repository selector that delegates to either Python or Native Kotlin implementation
 * based on feature flag configuration.
 *
 * This enables gradual migration from Python (Chaquopy) to native Kotlin.
 */
@Singleton
class SearchRepoSelector @Inject constructor(
    private val nativeRepo: NativeSearchRepoImpl,
    private val pyRepo: SearchRepoImpl,
    private val featureFlagRepo: FeatureFlagRepo
) : SearchRepo {

    /**
     * Select the appropriate repository based on feature flag.
     */
    private suspend fun selectRepo(): SearchRepo {
        val useNative = featureFlagRepo.isEnabled(FeatureFlags.USE_NATIVE_SEARCH)
        Log.d(TAG, "selectRepo() useNative=$useNative")
        return if (useNative) nativeRepo else pyRepo
    }

    override suspend fun search(query: String): Result<List<Stock>> {
        return selectRepo().search(query)
    }

    override suspend fun getAll(): Result<List<Stock>> {
        return selectRepo().getAll()
    }

    override fun getHistory(): Flow<List<Stock>> {
        // History is shared between implementations - use native by default
        // since it doesn't depend on Python
        return nativeRepo.getHistory()
    }

    override suspend fun saveHistory(stock: Stock) {
        // History is shared between implementations
        nativeRepo.saveHistory(stock)
    }

    override suspend fun clearHistory() {
        // History is shared between implementations
        nativeRepo.clearHistory()
    }

    override suspend fun searchForSuggestions(query: String): List<Stock> {
        return selectRepo().searchForSuggestions(query)
    }

    override suspend fun isCacheAvailable(): Boolean {
        return selectRepo().isCacheAvailable()
    }

    override suspend fun getCacheCount(): Int {
        return selectRepo().getCacheCount()
    }
}
