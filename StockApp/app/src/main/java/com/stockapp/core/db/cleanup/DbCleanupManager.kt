package com.stockapp.core.db.cleanup

import android.util.Log
import com.stockapp.core.config.AppConfig
import com.stockapp.core.db.dao.AnalysisCacheDao
import com.stockapp.core.db.dao.IndicatorCacheDao
import com.stockapp.core.db.dao.InvestorTradingCacheDao
import com.stockapp.core.db.dao.OhlcvCacheDao
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import javax.inject.Inject
import javax.inject.Singleton

private const val TAG = "DbCleanupManager"

/**
 * Manages DB size by cleaning up expired/old cached data.
 * Called during background sync and optionally on app startup.
 */
@Singleton
class DbCleanupManager @Inject constructor(
    private val ohlcvCacheDao: OhlcvCacheDao,
    private val investorTradingCacheDao: InvestorTradingCacheDao,
    private val analysisCacheDao: AnalysisCacheDao,
    private val indicatorCacheDao: IndicatorCacheDao
) {
    private val dateFormat = DateTimeFormatter.ofPattern("yyyyMMdd")

    /**
     * Run all cleanup operations.
     */
    suspend fun runCleanup() {
        Log.d(TAG, "runCleanup() started")
        try {
            cleanupOhlcvCache()
            cleanupInvestorTradingCache()
            cleanupFeatureCaches()
            Log.d(TAG, "runCleanup() completed")
        } catch (e: Exception) {
            Log.e(TAG, "runCleanup() error: ${e.message}", e)
        }
    }

    private suspend fun cleanupOhlcvCache() {
        val cutoffDate = LocalDate.now()
            .minusDays(AppConfig.OHLCV_MAX_RETENTION_DAYS.toLong())
            .format(dateFormat)

        val tickers = ohlcvCacheDao.getDistinctTickers()
        var deleted = 0
        tickers.forEach { ticker ->
            ohlcvCacheDao.deleteOldData(ticker, cutoffDate)
            deleted++
        }
        val totalRemaining = ohlcvCacheDao.countAll()
        Log.d(TAG, "OHLCV cleanup: processed $deleted tickers, $totalRemaining bars remaining")
    }

    private suspend fun cleanupInvestorTradingCache() {
        val threshold = System.currentTimeMillis() -
            (AppConfig.INVESTOR_TRADING_MAX_RETENTION_DAYS.toLong() * 24 * 60 * 60 * 1000)
        investorTradingCacheDao.deleteExpired(threshold)
        val remaining = investorTradingCacheDao.countAll()
        Log.d(TAG, "Investor trading cleanup: $remaining records remaining")
    }

    private suspend fun cleanupFeatureCaches() {
        val now = System.currentTimeMillis()
        analysisCacheDao.deleteExpired(now - AppConfig.ANALYSIS_CACHE_TTL_MS)
        indicatorCacheDao.deleteExpired(now - AppConfig.INDICATOR_CACHE_TTL_MS)
    }
}
