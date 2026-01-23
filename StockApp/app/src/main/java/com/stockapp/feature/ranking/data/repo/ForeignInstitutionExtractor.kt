package com.stockapp.feature.ranking.data.repo

import com.stockapp.feature.ranking.data.dto.ForeignInstitutionItemDto
import com.stockapp.feature.ranking.domain.model.RankingItem
import com.stockapp.feature.ranking.domain.model.TradeDirection

/**
 * Extractor functions for Foreign/Institution ranking data (ka90009 API).
 */
internal object ForeignInstitutionExtractor {

    /**
     * Extract foreign investor data (순매수 or 순매도).
     */
    fun extractForeignData(
        dto: ForeignInstitutionItemDto,
        index: Int,
        isAmount: Boolean,
        direction: TradeDirection
    ): RankingItem? {
        return if (direction == TradeDirection.NET_BUY) {
            val ticker = dto.forNetprpsStkCd ?: return null
            val value = RankingParseUtils.parseLong(if (isAmount) dto.forNetprpsAmt else dto.forNetprpsQty)
            RankingItem(
                rank = index + 1,
                ticker = RankingParseUtils.cleanTicker(ticker),
                name = dto.forNetprpsStkNm ?: "",
                currentPrice = 0,
                priceChange = 0,
                priceChangeSign = "",
                changeRate = 0.0,
                foreignNetBuy = value,
                netValue = value
            )
        } else {
            val ticker = dto.forNetslmtStkCd ?: return null
            val value = RankingParseUtils.parseLong(if (isAmount) dto.forNetslmtAmt else dto.forNetslmtQty)
            RankingItem(
                rank = index + 1,
                ticker = RankingParseUtils.cleanTicker(ticker),
                name = dto.forNetslmtStkNm ?: "",
                currentPrice = 0,
                priceChange = 0,
                priceChangeSign = "",
                changeRate = 0.0,
                foreignNetSell = value,
                netValue = value
            )
        }
    }

    /**
     * Extract institution investor data (순매수 or 순매도).
     * Uses institution-specific ticker and name fields from the API response.
     * Falls back to foreign ticker/name if institution fields are empty (mock API compatibility).
     */
    fun extractInstitutionData(
        dto: ForeignInstitutionItemDto,
        index: Int,
        isAmount: Boolean,
        direction: TradeDirection
    ): RankingItem? {
        return if (direction == TradeDirection.NET_BUY) {
            // Use institution ticker/name, fallback to foreign if empty (mock API returns empty institution data)
            val ticker = dto.orgnNetprpsStkCd?.takeIf { it.isNotEmpty() }
                ?: dto.forNetprpsStkCd
                ?: return null
            val name = dto.orgnNetprpsStkNm?.takeIf { it.isNotEmpty() }
                ?: dto.forNetprpsStkNm
                ?: ""
            val value = RankingParseUtils.parseLong(if (isAmount) dto.orgnNetprpsAmt else dto.orgnNetprpsQty)
            RankingItem(
                rank = index + 1,
                ticker = RankingParseUtils.cleanTicker(ticker),
                name = name,
                currentPrice = 0,
                priceChange = 0,
                priceChangeSign = "",
                changeRate = 0.0,
                institutionNetBuy = value,
                netValue = value
            )
        } else {
            // Use institution ticker/name, fallback to foreign if empty (mock API returns empty institution data)
            val ticker = dto.orgnNetslmtStkCd?.takeIf { it.isNotEmpty() }
                ?: dto.forNetslmtStkCd
                ?: return null
            val name = dto.orgnNetslmtStkNm?.takeIf { it.isNotEmpty() }
                ?: dto.forNetslmtStkNm
                ?: ""
            val value = RankingParseUtils.parseLong(if (isAmount) dto.orgnNetslmtAmt else dto.orgnNetslmtQty)
            RankingItem(
                rank = index + 1,
                ticker = RankingParseUtils.cleanTicker(ticker),
                name = name,
                currentPrice = 0,
                priceChange = 0,
                priceChangeSign = "",
                changeRate = 0.0,
                institutionNetSell = value,
                netValue = value
            )
        }
    }

    /**
     * Extract all investors data (foreign + institution combined).
     * Uses foreign ticker/name as reference and combines both investor values.
     * netValue shows the combined total (foreign + institution).
     */
    fun extractAllInvestorsData(
        dto: ForeignInstitutionItemDto,
        index: Int,
        isAmount: Boolean,
        direction: TradeDirection
    ): RankingItem? {
        return if (direction == TradeDirection.NET_BUY) {
            val ticker = dto.forNetprpsStkCd ?: return null
            val foreignValue = RankingParseUtils.parseLong(if (isAmount) dto.forNetprpsAmt else dto.forNetprpsQty)
            val institutionValue = RankingParseUtils.parseLong(if (isAmount) dto.orgnNetprpsAmt else dto.orgnNetprpsQty)
            RankingItem(
                rank = index + 1,
                ticker = RankingParseUtils.cleanTicker(ticker),
                name = dto.forNetprpsStkNm ?: "",
                currentPrice = 0,
                priceChange = 0,
                priceChangeSign = "",
                changeRate = 0.0,
                foreignNetBuy = foreignValue,
                institutionNetBuy = institutionValue,
                netValue = foreignValue + institutionValue  // Combined total
            )
        } else {
            val ticker = dto.forNetslmtStkCd ?: return null
            val foreignValue = RankingParseUtils.parseLong(if (isAmount) dto.forNetslmtAmt else dto.forNetslmtQty)
            val institutionValue = RankingParseUtils.parseLong(if (isAmount) dto.orgnNetslmtAmt else dto.orgnNetslmtQty)
            RankingItem(
                rank = index + 1,
                ticker = RankingParseUtils.cleanTicker(ticker),
                name = dto.forNetslmtStkNm ?: "",
                currentPrice = 0,
                priceChange = 0,
                priceChangeSign = "",
                changeRate = 0.0,
                foreignNetSell = foreignValue,
                institutionNetSell = institutionValue,
                netValue = foreignValue + institutionValue  // Combined total
            )
        }
    }
}
