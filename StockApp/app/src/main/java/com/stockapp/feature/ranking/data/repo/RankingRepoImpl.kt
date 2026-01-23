package com.stockapp.feature.ranking.data.repo

import com.stockapp.core.api.ApiError
import com.stockapp.core.api.KiwoomApiClient
import com.stockapp.feature.ranking.data.dto.CreditRatioTopResponse
import com.stockapp.feature.ranking.data.dto.DailyVolumeTopResponse
import com.stockapp.feature.ranking.data.dto.ForeignInstitutionTopResponse
import com.stockapp.feature.ranking.data.dto.OrderBookSurgeResponse
import com.stockapp.feature.ranking.data.dto.VolumeSurgeResponse
import com.stockapp.feature.ranking.domain.model.CreditRatioTopParams
import com.stockapp.feature.ranking.domain.model.DailyVolumeTopParams
import com.stockapp.feature.ranking.domain.model.ForeignInstitutionTopParams
import com.stockapp.feature.ranking.domain.model.OrderBookSurgeParams
import com.stockapp.feature.ranking.domain.model.RankingItem
import com.stockapp.feature.ranking.domain.model.RankingResult
import com.stockapp.feature.ranking.domain.model.RankingType
import com.stockapp.feature.ranking.domain.model.VolumeSurgeParams
import com.stockapp.feature.ranking.domain.repo.RankingRepo
import com.stockapp.feature.settings.domain.model.InvestmentMode
import com.stockapp.feature.settings.domain.repo.SettingsRepo
import kotlinx.coroutines.flow.first
import kotlinx.serialization.json.Json
import java.time.LocalDateTime
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class RankingRepoImpl @Inject constructor(
    private val apiClient: KiwoomApiClient,
    private val settingsRepo: SettingsRepo,
    private val json: Json
) : RankingRepo {

    private suspend fun getApiConfig(): ApiConfig {
        val config = settingsRepo.getApiKeyConfig().first()
        if (!config.isValid()) {
            throw ApiError.NoApiKeyError()
        }
        val baseUrl = when (config.investmentMode) {
            InvestmentMode.MOCK -> "https://mockapi.kiwoom.com"
            InvestmentMode.PRODUCTION -> "https://api.kiwoom.com"
        }
        return ApiConfig(config.appKey, config.secretKey, baseUrl)
    }

    override suspend fun getOrderBookSurge(params: OrderBookSurgeParams): Result<RankingResult> {
        return try {
            val config = getApiConfig()
            val rankingType = if (params.tradeType == "1") {
                RankingType.ORDER_BOOK_SURGE_BUY
            } else {
                RankingType.ORDER_BOOK_SURGE_SELL
            }

            apiClient.call(
                apiId = "ka10021",
                url = "/api/dostk/rkinfo",
                body = params.toRequestBody(),
                appKey = config.appKey,
                secretKey = config.secretKey,
                baseUrl = config.baseUrl
            ) { responseJson ->
                val response = json.decodeFromString<OrderBookSurgeResponse>(responseJson)
                parseOrderBookSurgeResponse(response, params, rankingType)
            }
        } catch (e: ApiError) {
            Result.failure(e)
        }
    }

    override suspend fun getVolumeSurge(params: VolumeSurgeParams): Result<RankingResult> {
        return try {
            val config = getApiConfig()

            apiClient.call(
                apiId = "ka10023",
                url = "/api/dostk/rkinfo",
                body = params.toRequestBody(),
                appKey = config.appKey,
                secretKey = config.secretKey,
                baseUrl = config.baseUrl
            ) { responseJson ->
                val response = json.decodeFromString<VolumeSurgeResponse>(responseJson)
                parseVolumeSurgeResponse(response, params)
            }
        } catch (e: ApiError) {
            Result.failure(e)
        }
    }

    override suspend fun getDailyVolumeTop(params: DailyVolumeTopParams): Result<RankingResult> {
        return try {
            val config = getApiConfig()

            apiClient.call(
                apiId = "ka10030",
                url = "/api/dostk/rkinfo",
                body = params.toRequestBody(),
                appKey = config.appKey,
                secretKey = config.secretKey,
                baseUrl = config.baseUrl
            ) { responseJson ->
                val response = json.decodeFromString<DailyVolumeTopResponse>(responseJson)
                parseDailyVolumeTopResponse(response, params)
            }
        } catch (e: ApiError) {
            Result.failure(e)
        }
    }

    override suspend fun getCreditRatioTop(params: CreditRatioTopParams): Result<RankingResult> {
        return try {
            val config = getApiConfig()

            apiClient.call(
                apiId = "ka10033",
                url = "/api/dostk/rkinfo",
                body = params.toRequestBody(),
                appKey = config.appKey,
                secretKey = config.secretKey,
                baseUrl = config.baseUrl
            ) { responseJson ->
                val response = json.decodeFromString<CreditRatioTopResponse>(responseJson)
                parseCreditRatioTopResponse(response, params)
            }
        } catch (e: ApiError) {
            Result.failure(e)
        }
    }

    override suspend fun getForeignInstitutionTop(params: ForeignInstitutionTopParams): Result<RankingResult> {
        return try {
            val config = getApiConfig()

            apiClient.call(
                apiId = "ka90009",
                url = "/api/dostk/rkinfo",
                body = params.toRequestBody(),
                appKey = config.appKey,
                secretKey = config.secretKey,
                baseUrl = config.baseUrl
            ) { responseJson ->
                val response = json.decodeFromString<ForeignInstitutionTopResponse>(responseJson)
                parseForeignInstitutionTopResponse(response, params)
            }
        } catch (e: ApiError) {
            Result.failure(e)
        }
    }

    // Parsing helpers

    private fun parseOrderBookSurgeResponse(
        response: OrderBookSurgeResponse,
        params: OrderBookSurgeParams,
        rankingType: RankingType
    ): RankingResult {
        val items = mutableListOf<RankingItem>()
        val dtoItems = response.items ?: emptyList()

        for ((index, dto) in dtoItems.withIndex()) {
            items.add(
                RankingItem(
                    rank = index + 1,
                    ticker = cleanTicker(dto.stkCd),
                    name = dto.stkNm ?: "",
                    currentPrice = parseLong(dto.curPrc),
                    priceChange = parseLong(dto.predPre),
                    priceChangeSign = parseSign(dto.predPreSig),
                    changeRate = 0.0, // Not provided in this API
                    surgeQuantity = parseLong(dto.sdninQty),
                    surgeRate = parseDouble(dto.sdninRt),
                    totalBuyQuantity = parseLong(dto.totBuyQty)
                )
            )
        }

        return RankingResult(
            rankingType = rankingType,
            marketType = params.marketType,
            exchangeType = params.exchangeType,
            items = items,
            fetchedAt = LocalDateTime.now()
        )
    }

    private fun parseVolumeSurgeResponse(
        response: VolumeSurgeResponse,
        params: VolumeSurgeParams
    ): RankingResult {
        val items = mutableListOf<RankingItem>()
        val dtoItems = response.items ?: emptyList()

        for ((index, dto) in dtoItems.withIndex()) {
            items.add(
                RankingItem(
                    rank = index + 1,
                    ticker = cleanTicker(dto.stkCd),
                    name = dto.stkNm ?: "",
                    currentPrice = parseLong(dto.curPrc),
                    priceChange = parseLong(dto.predPre),
                    priceChangeSign = parseSign(dto.predPreSig),
                    changeRate = parseDouble(dto.fluRt),
                    volume = parseLong(dto.nowTrdeQty),
                    surgeQuantity = parseLong(dto.sdninQty),
                    surgeRate = parseDouble(dto.sdninRt)
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

    private fun parseDailyVolumeTopResponse(
        response: DailyVolumeTopResponse,
        params: DailyVolumeTopParams
    ): RankingResult {
        val items = mutableListOf<RankingItem>()
        val dtoItems = response.items ?: emptyList()

        for ((index, dto) in dtoItems.withIndex()) {
            items.add(
                RankingItem(
                    rank = index + 1,
                    ticker = cleanTicker(dto.stkCd),
                    name = dto.stkNm ?: "",
                    currentPrice = parseLong(dto.curPrc),
                    priceChange = parseLong(dto.predPre),
                    priceChangeSign = parseSign(dto.predPreSig),
                    changeRate = parseDouble(dto.fluRt),
                    volume = parseLong(dto.trdeQty)
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

    private fun parseCreditRatioTopResponse(
        response: CreditRatioTopResponse,
        params: CreditRatioTopParams
    ): RankingResult {
        val items = mutableListOf<RankingItem>()
        val dtoItems = response.items ?: emptyList()

        for ((index, dto) in dtoItems.withIndex()) {
            items.add(
                RankingItem(
                    rank = index + 1,
                    ticker = cleanTicker(dto.stkCd),
                    name = dto.stkNm ?: "",
                    currentPrice = parseLong(dto.curPrc),
                    priceChange = parseLong(dto.predPre),
                    priceChangeSign = parseSign(dto.predPreSig),
                    changeRate = parseDouble(dto.fluRt),
                    creditRatio = parseDouble(dto.crdRt),
                    volume = parseLong(dto.nowTrdeQty)
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

    private fun parseForeignInstitutionTopResponse(
        response: ForeignInstitutionTopResponse,
        params: ForeignInstitutionTopParams
    ): RankingResult {
        val items = mutableListOf<RankingItem>()

        // Combine foreign and institution net buy data
        val forTickers = response.forNetprpsStkCdList ?: emptyList()
        val forNames = response.forNetprpsStkNmList ?: emptyList()
        val forAmts = response.forNetprpsAmtList ?: emptyList()

        val orgnTickers = response.orgnNetprpsStkCdList ?: emptyList()
        val orgnNames = response.orgnNetprpsStkNmList ?: emptyList()
        val orgnAmts = response.orgnNetprpsAmtList ?: emptyList()

        // Create a map for institution data keyed by ticker
        val orgnDataMap = mutableMapOf<String, Long>()
        for (i in orgnTickers.indices) {
            val ticker = orgnTickers.getOrNull(i) ?: continue
            orgnDataMap[ticker] = parseLong(orgnAmts.getOrNull(i))
        }

        // Primary list is foreign net buy, add institution data if available
        for (i in forTickers.indices) {
            val ticker = forTickers.getOrNull(i) ?: ""
            items.add(
                RankingItem(
                    rank = i + 1,
                    ticker = ticker,
                    name = forNames.getOrNull(i) ?: "",
                    currentPrice = 0, // Not provided in this API
                    priceChange = 0,
                    priceChangeSign = "",
                    changeRate = 0.0,
                    foreignNetBuy = parseLong(forAmts.getOrNull(i)),
                    institutionNetBuy = orgnDataMap[ticker]
                )
            )
        }

        return RankingResult(
            rankingType = RankingType.FOREIGN_INSTITUTION_TOP,
            marketType = params.marketType,
            exchangeType = params.exchangeType,
            items = items,
            fetchedAt = LocalDateTime.now()
        )
    }

    // Utility functions

    private fun cleanTicker(value: String?): String {
        if (value == null) return ""
        // Remove "_AL" suffix and any other common suffixes from ticker codes
        return value.replace("_AL", "").replace("_KS", "").replace("_KQ", "").trim()
    }

    private fun parseLong(value: String?): Long {
        if (value == null) return 0
        return value.replace(",", "").replace("+", "").trim().toLongOrNull() ?: 0
    }

    private fun parseDouble(value: String?): Double {
        if (value == null) return 0.0
        return value.replace(",", "").replace("+", "").replace("%", "").trim().toDoubleOrNull() ?: 0.0
    }

    private fun parseSign(value: String?): String {
        return when (value?.trim()) {
            "1", "2", "+" -> "+"
            "4", "5", "-" -> "-"
            else -> ""
        }
    }

    private data class ApiConfig(
        val appKey: String,
        val secretKey: String,
        val baseUrl: String
    )
}
