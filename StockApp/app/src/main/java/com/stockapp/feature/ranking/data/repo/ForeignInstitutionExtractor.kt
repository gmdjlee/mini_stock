package com.stockapp.feature.ranking.data.repo

import com.stockapp.feature.ranking.data.dto.ForeignInstitutionItemDto
import com.stockapp.feature.ranking.domain.model.RankingItem
import com.stockapp.feature.ranking.domain.model.TradeDirection

/**
 * Extractor functions for Foreign/Institution ranking data (ka90009 API).
 */
internal object ForeignInstitutionExtractor {

    /**
     * Extract foreign investor data.
     */
    fun extractForeignData(
        dto: ForeignInstitutionItemDto,
        index: Int,
        isAmount: Boolean,
        direction: TradeDirection
    ): RankingItem? {
        val fields = dto.getFieldsForDirection(direction)
        val ticker = fields.forTicker ?: return null
        val value = RankingParseUtils.parseLong(if (isAmount) fields.forAmt else fields.forQty)

        return createBaseItem(index, ticker, fields.forName).let {
            if (direction == TradeDirection.NET_BUY) {
                it.copy(foreignNetBuy = value, netValue = value)
            } else {
                it.copy(foreignNetSell = value, netValue = value)
            }
        }
    }

    /**
     * Extract institution investor data.
     * Falls back to foreign ticker/name if institution fields are empty (mock API compatibility).
     */
    fun extractInstitutionData(
        dto: ForeignInstitutionItemDto,
        index: Int,
        isAmount: Boolean,
        direction: TradeDirection
    ): RankingItem? {
        val fields = dto.getFieldsForDirection(direction)
        val ticker = fields.orgnTicker?.takeIf { it.isNotEmpty() }
            ?: fields.forTicker
            ?: return null
        val name = fields.orgnName?.takeIf { it.isNotEmpty() }
            ?: fields.forName
            ?: ""
        val value = RankingParseUtils.parseLong(if (isAmount) fields.orgnAmt else fields.orgnQty)

        return createBaseItem(index, ticker, name).let {
            if (direction == TradeDirection.NET_BUY) {
                it.copy(institutionNetBuy = value, netValue = value)
            } else {
                it.copy(institutionNetSell = value, netValue = value)
            }
        }
    }

    /**
     * Extract all investors data (foreign + institution combined).
     */
    fun extractAllInvestorsData(
        dto: ForeignInstitutionItemDto,
        index: Int,
        isAmount: Boolean,
        direction: TradeDirection
    ): RankingItem? {
        val fields = dto.getFieldsForDirection(direction)
        val ticker = fields.forTicker ?: return null
        val foreignValue = RankingParseUtils.parseLong(if (isAmount) fields.forAmt else fields.forQty)
        val institutionValue = RankingParseUtils.parseLong(if (isAmount) fields.orgnAmt else fields.orgnQty)

        return createBaseItem(index, ticker, fields.forName).let {
            if (direction == TradeDirection.NET_BUY) {
                it.copy(
                    foreignNetBuy = foreignValue,
                    institutionNetBuy = institutionValue,
                    netValue = foreignValue + institutionValue
                )
            } else {
                it.copy(
                    foreignNetSell = foreignValue,
                    institutionNetSell = institutionValue,
                    netValue = foreignValue + institutionValue
                )
            }
        }
    }

    private fun createBaseItem(index: Int, ticker: String, name: String?) = RankingItem(
        rank = index + 1,
        ticker = RankingParseUtils.cleanTicker(ticker),
        name = name ?: "",
        currentPrice = 0,
        priceChange = 0,
        priceChangeSign = "",
        changeRate = 0.0
    )

    /**
     * Helper class to hold direction-specific field values.
     */
    private data class DirectionFields(
        val forTicker: String?,
        val forName: String?,
        val forAmt: String?,
        val forQty: String?,
        val orgnTicker: String?,
        val orgnName: String?,
        val orgnAmt: String?,
        val orgnQty: String?
    )

    private fun ForeignInstitutionItemDto.getFieldsForDirection(direction: TradeDirection) =
        if (direction == TradeDirection.NET_BUY) {
            DirectionFields(
                forTicker = forNetprpsStkCd,
                forName = forNetprpsStkNm,
                forAmt = forNetprpsAmt,
                forQty = forNetprpsQty,
                orgnTicker = orgnNetprpsStkCd,
                orgnName = orgnNetprpsStkNm,
                orgnAmt = orgnNetprpsAmt,
                orgnQty = orgnNetprpsQty
            )
        } else {
            DirectionFields(
                forTicker = forNetslmtStkCd,
                forName = forNetslmtStkNm,
                forAmt = forNetslmtAmt,
                forQty = forNetslmtQty,
                orgnTicker = orgnNetslmtStkCd,
                orgnName = orgnNetslmtStkNm,
                orgnAmt = orgnNetslmtAmt,
                orgnQty = orgnNetslmtQty
            )
        }
}
