package com.stockapp.feature.ranking.domain.usecase

import com.stockapp.feature.ranking.domain.model.CreditRatioTopParams
import com.stockapp.feature.ranking.domain.model.DailyVolumeTopParams
import com.stockapp.feature.ranking.domain.model.ExchangeType
import com.stockapp.feature.ranking.domain.model.ForeignInstitutionTopParams
import com.stockapp.feature.ranking.domain.model.InvestorType
import com.stockapp.feature.ranking.domain.model.ItemCount
import com.stockapp.feature.ranking.domain.model.MarketType
import com.stockapp.feature.ranking.domain.model.OrderBookDirection
import com.stockapp.feature.ranking.domain.model.TradeDirection
import com.stockapp.feature.ranking.domain.model.ValueType
import com.stockapp.feature.ranking.domain.model.OrderBookSurgeParams
import com.stockapp.feature.ranking.domain.model.RankingResult
import com.stockapp.feature.ranking.domain.model.RankingType
import com.stockapp.feature.ranking.domain.model.VolumeSurgeParams
import com.stockapp.feature.ranking.domain.repo.RankingRepo
import javax.inject.Inject

/**
 * Use case for fetching ranking data.
 */
class GetRankingUC @Inject constructor(
    private val repo: RankingRepo
) {
    /**
     * Get ranking data based on type, market, exchange, and item count.
     * For ORDER_BOOK_SURGE, orderBookDirection filter is supported.
     * For FOREIGN_INSTITUTION_TOP, additional filters (investorType, tradeDirection, valueType) are supported.
     */
    suspend operator fun invoke(
        rankingType: RankingType,
        marketType: MarketType,
        exchangeType: ExchangeType,
        itemCount: ItemCount = ItemCount.TEN,
        // ka10021 specific filter
        orderBookDirection: OrderBookDirection = OrderBookDirection.BUY,
        // ka90009 specific filters
        investorType: InvestorType = InvestorType.FOREIGN,
        tradeDirection: TradeDirection = TradeDirection.NET_BUY,
        valueType: ValueType = ValueType.AMOUNT
    ): Result<RankingResult> {
        val result = when (rankingType) {
            RankingType.ORDER_BOOK_SURGE -> repo.getOrderBookSurge(
                OrderBookSurgeParams(
                    marketType = marketType,
                    exchangeType = exchangeType,
                    tradeType = orderBookDirection.code
                )
            )
            RankingType.VOLUME_SURGE -> repo.getVolumeSurge(
                VolumeSurgeParams(
                    marketType = marketType,
                    exchangeType = exchangeType
                )
            )
            RankingType.DAILY_VOLUME_TOP -> repo.getDailyVolumeTop(
                DailyVolumeTopParams(
                    marketType = marketType,
                    exchangeType = exchangeType
                )
            )
            RankingType.CREDIT_RATIO_TOP -> repo.getCreditRatioTop(
                CreditRatioTopParams(
                    marketType = marketType,
                    exchangeType = exchangeType
                )
            )
            RankingType.FOREIGN_INSTITUTION_TOP -> repo.getForeignInstitutionTop(
                ForeignInstitutionTopParams(
                    marketType = marketType,
                    exchangeType = exchangeType,
                    amountQtyType = valueType.code,
                    investorType = investorType,
                    tradeDirection = tradeDirection
                )
            )
        }

        // Limit items to requested count
        return result.map { rankingResult ->
            rankingResult.copy(
                items = rankingResult.items.take(itemCount.value)
            )
        }
    }
}
