package com.stockapp.feature.ranking.data.repo

import android.util.Log
import com.stockapp.core.api.ApiError
import com.stockapp.core.api.KiwoomApiClient
import com.stockapp.feature.ranking.data.dto.ForeignInstitutionTopResponse
import com.stockapp.feature.ranking.data.dto.RankingItemDto
import com.stockapp.feature.ranking.domain.model.CreditRatioTopParams
import com.stockapp.feature.ranking.domain.model.DailyVolumeTopParams
import com.stockapp.feature.ranking.domain.model.ForeignInstitutionTopParams
import com.stockapp.feature.ranking.domain.model.OrderBookDirection
import com.stockapp.feature.ranking.domain.model.OrderBookSurgeParams
import com.stockapp.feature.ranking.domain.model.RankingResult
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
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Repository implementation for ranking data.
 * Uses Kotlin REST API client to fetch ranking data from Kiwoom API.
 */
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
            val orderBookDirection = if (params.tradeType == "1") {
                OrderBookDirection.BUY
            } else {
                OrderBookDirection.SELL
            }

            apiClient.call(
                apiId = "ka10021",
                url = "/api/dostk/rkinfo",
                body = params.toRequestBody(),
                appKey = config.appKey,
                secretKey = config.secretKey,
                baseUrl = config.baseUrl
            ) { responseJson ->
                val items = findAndParseItemsArray(responseJson)
                RankingParsers.parseOrderBookSurgeItems(items, params, orderBookDirection)
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
                val items = findAndParseItemsArray(responseJson)
                RankingParsers.parseVolumeSurgeItems(items, params)
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
                val items = findAndParseItemsArray(responseJson)
                RankingParsers.parseDailyVolumeTopItems(items, params)
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
                val items = findAndParseItemsArray(responseJson)
                RankingParsers.parseCreditRatioTopItems(items, params)
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
                RankingParsers.parseForeignInstitutionTopResponse(response, params)
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

            // Find the first field that contains a JsonArray
            for ((key, value) in rootObject.entries) {
                if (key in skipFields) continue

                if (value is JsonArray) {
                    // Found the data array field
                    if (value.isEmpty()) {
                        Log.d(TAG, "Found data array in field: $key but it is empty (no data available)")
                        return emptyList()
                    }

                    // Check if it's an array of objects (not just strings)
                    val firstElement = value.firstOrNull()
                    if (firstElement is JsonObject) {
                        Log.d(TAG, "Found items array in field: $key with ${value.size} items")
                        return json.decodeFromJsonElement<List<RankingItemDto>>(value)
                    } else {
                        Log.w(TAG, "Found array in field: $key but elements are not objects")
                    }
                }
            }

            Log.w(TAG, "No data array field found in response. Available fields: ${rootObject.keys}")
            return emptyList()
        } catch (e: Exception) {
            Log.e(TAG, "Error parsing items array: ${e.message}", e)
            return emptyList()
        }
    }

    companion object {
        private const val TAG = "RankingRepoImpl"
    }
}
