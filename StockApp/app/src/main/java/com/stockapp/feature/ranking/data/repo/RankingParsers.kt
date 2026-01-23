package com.stockapp.feature.ranking.data.repo

import com.stockapp.feature.ranking.data.dto.ForeignInstitutionItemDto
import com.stockapp.feature.ranking.data.dto.ForeignInstitutionTopResponse
import com.stockapp.feature.ranking.data.dto.RankingItemDto
import com.stockapp.feature.ranking.domain.model.CreditRatioTopParams
import com.stockapp.feature.ranking.domain.model.DailyVolumeTopParams
import com.stockapp.feature.ranking.domain.model.ForeignInstitutionTopParams
import com.stockapp.feature.ranking.domain.model.InvestorType
import com.stockapp.feature.ranking.domain.model.OrderBookDirection
import com.stockapp.feature.ranking.domain.model.OrderBookSurgeParams
import com.stockapp.feature.ranking.domain.model.RankingItem
import com.stockapp.feature.ranking.domain.model.RankingResult
import com.stockapp.feature.ranking.domain.model.RankingType
import com.stockapp.feature.ranking.domain.model.ValueType
import com.stockapp.feature.ranking.domain.model.VolumeSurgeParams
import java.time.LocalDateTime

/**
 * Parser functions for ranking API responses.
 */
internal object RankingParsers {

    /**
     * Parse OrderBook Surge (ka10021) response items.
     */
    fun parseOrderBookSurgeItems(
        dtoItems: List<RankingItemDto>,
        params: OrderBookSurgeParams,
        orderBookDirection: OrderBookDirection
    ): RankingResult {
        val items = mutableListOf<RankingItem>()

        for ((index, dto) in dtoItems.withIndex()) {
            items.add(
                RankingItem(
                    rank = index + 1,
                    ticker = RankingParseUtils.cleanTicker(dto.stkCd),
                    name = dto.stkNm ?: "",
                    currentPrice = RankingParseUtils.parseLong(dto.curPrc),
                    priceChange = RankingParseUtils.parseLong(dto.predPre),
                    priceChangeSign = RankingParseUtils.parseSign(dto.predPreSig),
                    changeRate = 0.0, // Not provided in this API
                    surgeQuantity = RankingParseUtils.parseLong(dto.sdninQty),
                    surgeRate = RankingParseUtils.parseDouble(dto.sdninRt),
                    totalBuyQuantity = RankingParseUtils.parseLong(dto.totBuyQty)
                )
            )
        }

        return RankingResult(
            rankingType = RankingType.ORDER_BOOK_SURGE,
            marketType = params.marketType,
            exchangeType = params.exchangeType,
            items = items,
            fetchedAt = LocalDateTime.now(),
            orderBookDirection = orderBookDirection
        )
    }

    /**
     * Parse Volume Surge (ka10023) response items.
     */
    fun parseVolumeSurgeItems(
        dtoItems: List<RankingItemDto>,
        params: VolumeSurgeParams
    ): RankingResult {
        val items = mutableListOf<RankingItem>()

        for ((index, dto) in dtoItems.withIndex()) {
            items.add(
                RankingItem(
                    rank = index + 1,
                    ticker = RankingParseUtils.cleanTicker(dto.stkCd),
                    name = dto.stkNm ?: "",
                    currentPrice = RankingParseUtils.parseLong(dto.curPrc),
                    priceChange = RankingParseUtils.parseLong(dto.predPre),
                    priceChangeSign = RankingParseUtils.parseSign(dto.predPreSig),
                    changeRate = RankingParseUtils.parseDouble(dto.fluRt),
                    volume = RankingParseUtils.parseLong(dto.nowTrdeQty),
                    surgeQuantity = RankingParseUtils.parseLong(dto.sdninQty),
                    surgeRate = RankingParseUtils.parseDouble(dto.sdninRt)
                )
            )
        }

        return RankingResult(
            rankingType = RankingType.VOLUME_SURGE,
            marketType = params.marketType,
            exchangeType = params.exchangeType,
            items = items,
            fetchedAt = LocalDateTime.now()
        )
    }

    /**
     * Parse Daily Volume Top (ka10030) response items.
     */
    fun parseDailyVolumeTopItems(
        dtoItems: List<RankingItemDto>,
        params: DailyVolumeTopParams
    ): RankingResult {
        val items = mutableListOf<RankingItem>()

        for ((index, dto) in dtoItems.withIndex()) {
            items.add(
                RankingItem(
                    rank = index + 1,
                    ticker = RankingParseUtils.cleanTicker(dto.stkCd),
                    name = dto.stkNm ?: "",
                    currentPrice = RankingParseUtils.parseLong(dto.curPrc),
                    priceChange = RankingParseUtils.parseLong(dto.predPre),
                    priceChangeSign = RankingParseUtils.parseSign(dto.predPreSig),
                    changeRate = RankingParseUtils.parseDouble(dto.fluRt),
                    volume = RankingParseUtils.parseLong(dto.trdeQty)
                )
            )
        }

        return RankingResult(
            rankingType = RankingType.DAILY_VOLUME_TOP,
            marketType = params.marketType,
            exchangeType = params.exchangeType,
            items = items,
            fetchedAt = LocalDateTime.now()
        )
    }

    /**
     * Parse Credit Ratio Top (ka10033) response items.
     */
    fun parseCreditRatioTopItems(
        dtoItems: List<RankingItemDto>,
        params: CreditRatioTopParams
    ): RankingResult {
        val items = mutableListOf<RankingItem>()

        for ((index, dto) in dtoItems.withIndex()) {
            items.add(
                RankingItem(
                    rank = index + 1,
                    ticker = RankingParseUtils.cleanTicker(dto.stkCd),
                    name = dto.stkNm ?: "",
                    currentPrice = RankingParseUtils.parseLong(dto.curPrc),
                    priceChange = RankingParseUtils.parseLong(dto.predPre),
                    priceChangeSign = RankingParseUtils.parseSign(dto.predPreSig),
                    changeRate = RankingParseUtils.parseDouble(dto.fluRt),
                    creditRatio = RankingParseUtils.parseDouble(dto.crdRt),
                    volume = RankingParseUtils.parseLong(dto.nowTrdeQty)
                )
            )
        }

        return RankingResult(
            rankingType = RankingType.CREDIT_RATIO_TOP,
            marketType = params.marketType,
            exchangeType = params.exchangeType,
            items = items,
            fetchedAt = LocalDateTime.now()
        )
    }

    /**
     * Parse Foreign/Institution Top (ka90009) response.
     */
    fun parseForeignInstitutionTopResponse(
        response: ForeignInstitutionTopResponse,
        params: ForeignInstitutionTopParams
    ): RankingResult {
        val items = mutableListOf<RankingItem>()
        val dtoItems = response.items ?: emptyList()
        val isAmount = params.amountQtyType == "1"

        for ((index, dto) in dtoItems.withIndex()) {
            val item = when (params.investorType) {
                InvestorType.FOREIGN -> ForeignInstitutionExtractor.extractForeignData(
                    dto, index, isAmount, params.tradeDirection
                )
                InvestorType.INSTITUTION -> ForeignInstitutionExtractor.extractInstitutionData(
                    dto, index, isAmount, params.tradeDirection
                )
                InvestorType.ALL -> ForeignInstitutionExtractor.extractAllInvestorsData(
                    dto, index, isAmount, params.tradeDirection
                )
            }
            item?.let { items.add(it) }
        }

        return RankingResult(
            rankingType = RankingType.FOREIGN_INSTITUTION_TOP,
            marketType = params.marketType,
            exchangeType = params.exchangeType,
            items = items.filter { it.ticker.isNotEmpty() },
            fetchedAt = LocalDateTime.now(),
            investorType = params.investorType,
            tradeDirection = params.tradeDirection,
            valueType = if (isAmount) ValueType.AMOUNT else ValueType.QUANTITY
        )
    }
}
