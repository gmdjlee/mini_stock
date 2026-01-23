package com.stockapp.feature.ranking.domain.repo

import com.stockapp.feature.ranking.domain.model.CreditRatioTopParams
import com.stockapp.feature.ranking.domain.model.DailyVolumeTopParams
import com.stockapp.feature.ranking.domain.model.ForeignInstitutionTopParams
import com.stockapp.feature.ranking.domain.model.OrderBookSurgeParams
import com.stockapp.feature.ranking.domain.model.RankingResult
import com.stockapp.feature.ranking.domain.model.VolumeSurgeParams

/**
 * Repository interface for ranking data.
 */
interface RankingRepo {

    /**
     * Get order book surge ranking (ka10021).
     * 호가잔량급증요청 - 매수/매도 호가잔량이 급증한 종목
     */
    suspend fun getOrderBookSurge(params: OrderBookSurgeParams): Result<RankingResult>

    /**
     * Get volume surge ranking (ka10023).
     * 거래량급증요청 - 거래량이 급증한 종목
     */
    suspend fun getVolumeSurge(params: VolumeSurgeParams): Result<RankingResult>

    /**
     * Get daily volume top ranking (ka10030).
     * 당일거래량상위요청 - 당일 거래량 상위 종목
     */
    suspend fun getDailyVolumeTop(params: DailyVolumeTopParams): Result<RankingResult>

    /**
     * Get credit ratio top ranking (ka10033).
     * 신용비율상위요청 - 신용비율 상위 종목
     */
    suspend fun getCreditRatioTop(params: CreditRatioTopParams): Result<RankingResult>

    /**
     * Get foreign/institution top ranking (ka90009).
     * 외국인기관매매상위요청 - 외국인/기관 순매수 상위 종목
     */
    suspend fun getForeignInstitutionTop(params: ForeignInstitutionTopParams): Result<RankingResult>
}
