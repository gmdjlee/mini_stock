package com.stockapp.feature.ranking.data.repo

import android.util.Log
import com.stockapp.core.api.ApiError
import com.stockapp.core.api.KiwoomApiClient
import com.stockapp.feature.ranking.data.dto.ForeignInstitutionItemDto
import com.stockapp.feature.ranking.data.dto.ForeignInstitutionTopResponse
import com.stockapp.feature.ranking.data.dto.OrderBookSurgeResponse
import com.stockapp.feature.ranking.data.dto.RankingItemDto
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
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.decodeFromJsonElement
import kotlinx.serialization.json.jsonObject
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
                // Use dynamic parsing to find the data array field
                val items = findAndParseItemsArray(responseJson)
                parseDailyVolumeTopItems(items, params)
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
                // Use dynamic parsing to find the data array field
                val items = findAndParseItemsArray(responseJson)
                parseCreditRatioTopItems(items, params)
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

    /**
     * Dynamically finds and parses the items array from API response.
     * Kiwoom API wraps ranking data in a named field (e.g., "bid_req_sdnin", "trde_qty_sdnin")
     * which varies by API. This method finds the array field automatically.
     */
    private fun findAndParseItemsArray(responseJson: String): List<RankingItemDto> {
        try {
            val rootObject = json.parseToJsonElement(responseJson).jsonObject

            // Skip known metadata fields
            val skipFields = setOf("return_code", "return_msg", "msg_cd", "msg1")

            // Find the first field that contains a JsonArray of objects
            for ((key, value) in rootObject.entries) {
                if (key in skipFields) continue

                if (value is JsonArray && value.isNotEmpty()) {
                    // Check if it's an array of objects (not just strings)
                    val firstElement = value.firstOrNull()
                    if (firstElement is JsonObject) {
                        Log.d(TAG, "Found items array in field: $key with ${value.size} items")
                        return json.decodeFromJsonElement<List<RankingItemDto>>(value)
                    }
                }
            }

            Log.w(TAG, "No items array found in response. Available fields: ${rootObject.keys}")
            return emptyList()
        } catch (e: Exception) {
            Log.e(TAG, "Error parsing items array: ${e.message}", e)
            return emptyList()
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

    private fun parseDailyVolumeTopItems(
        dtoItems: List<RankingItemDto>,
        params: DailyVolumeTopParams
    ): RankingResult {
        val items = mutableListOf<RankingItem>()

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

    private fun parseCreditRatioTopItems(
        dtoItems: List<RankingItemDto>,
        params: CreditRatioTopParams
    ): RankingResult {
        val items = mutableListOf<RankingItem>()

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
        val dtoItems = response.items ?: emptyList()

        // Each row contains 4 different stock entries (for_netslmt, for_netprps, orgn_netslmt, orgn_netprps)
        // We use foreign net buy (for_netprps) as the primary list
        for ((index, dto) in dtoItems.withIndex()) {
            // Skip if no foreign net buy data
            val ticker = dto.forNetprpsStkCd
            if (ticker.isNullOrEmpty()) continue

            items.add(
                RankingItem(
                    rank = index + 1,
                    ticker = cleanTicker(ticker),
                    name = dto.forNetprpsStkNm ?: "",
                    currentPrice = 0, // Not provided in this API
                    priceChange = 0,
                    priceChangeSign = "",
                    changeRate = 0.0,
                    foreignNetBuy = parseLong(dto.forNetprpsAmt),
                    institutionNetBuy = findInstitutionNetBuy(dto, ticker)
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

    /**
     * Find institution net buy amount for the same ticker from the same row.
     * Each row has separate foreign and institution data, so we check if the
     * institution net buy ticker matches the foreign ticker.
     */
    private fun findInstitutionNetBuy(dto: ForeignInstitutionItemDto, forTicker: String): Long? {
        val orgnTicker = cleanTicker(dto.orgnNetprpsStkCd)
        return if (orgnTicker == cleanTicker(forTicker)) {
            parseLong(dto.orgnNetprpsAmt)
        } else {
            null
        }
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

    companion object {
        private const val TAG = "RankingRepoImpl"
    }
}
