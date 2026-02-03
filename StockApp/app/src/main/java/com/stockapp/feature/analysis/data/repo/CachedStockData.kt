package com.stockapp.feature.analysis.data.repo

import com.stockapp.feature.analysis.domain.model.StockData
import kotlinx.serialization.Serializable

/**
 * Cache serialization wrapper for StockData.
 * Shared between AnalysisRepoImpl and NativeAnalysisRepoImpl.
 */
@Serializable
internal data class CachedStockData(
    val ticker: String,
    val name: String,
    val dates: List<String>,
    val mcap: List<Long>,
    val for5d: List<Long>,
    val ins5d: List<Long>
) {
    fun toDomain(): StockData = StockData(
        ticker = ticker,
        name = name,
        dates = dates,
        mcap = mcap,
        for5d = for5d,
        ins5d = ins5d
    )

    companion object {
        fun fromDomain(data: StockData): CachedStockData = CachedStockData(
            ticker = data.ticker,
            name = data.name,
            dates = data.dates,
            mcap = data.mcap,
            for5d = data.for5d,
            ins5d = data.ins5d
        )
    }
}
