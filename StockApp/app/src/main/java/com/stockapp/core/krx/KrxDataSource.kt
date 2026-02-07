package com.stockapp.core.krx

import android.util.Log
import com.krxkt.KrxEtf
import com.krxkt.KrxIndex
import com.krxkt.KrxStock
import com.krxkt.api.KrxClient
import com.krxkt.cache.TickerCache
import com.krxkt.error.KrxError
import com.krxkt.model.*
import com.stockapp.core.di.IoDispatcher
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import kotlin.coroutines.cancellation.CancellationException
import javax.inject.Inject
import javax.inject.Singleton

/**
 * KRX 직접 데이터 소스 (kotlin_krx 라이브러리 래퍼)
 *
 * 한국거래소(KRX)에서 직접 데이터를 가져오는 primary 데이터 소스.
 * Kiwoom/KIS API 대비 장점:
 * - API 키 불필요 (공개 API)
 * - Rate limit 없음 (자체 retry 로직)
 * - 더 풍부한 데이터 (투자자별 거래, 공매도, 지수 등)
 *
 * 제한사항:
 * - 실시간 데이터 미지원 → Kiwoom API로 fallback
 * - 한국 네트워크/VPN 필요
 */
@Singleton
class KrxDataSource @Inject constructor(
    okHttpClient: OkHttpClient,
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher
) {
    private val krxClient = KrxClient(okHttpClient)
    private val tickerCache = TickerCache()

    val stock: KrxStock = KrxStock(krxClient, tickerCache)
    val etf: KrxEtf = KrxEtf(krxClient, tickerCache)
    val index: KrxIndex = KrxIndex(krxClient)

    /**
     * KRX에서 종목 리스트 조회 (검색용)
     */
    suspend fun getTickerList(date: String, market: Market = Market.ALL): Result<List<TickerInfo>> =
        safeCall { stock.getTickerList(date, market) }

    /**
     * KRX에서 전종목 OHLCV 조회
     */
    suspend fun getMarketOhlcv(date: String, market: Market = Market.ALL): Result<List<MarketOhlcv>> =
        safeCall { stock.getMarketOhlcv(date, market) }

    /**
     * KRX에서 개별종목 OHLCV 기간 조회
     */
    suspend fun getOhlcvByTicker(
        startDate: String,
        endDate: String,
        ticker: String
    ): Result<List<StockOhlcvHistory>> =
        safeCall { stock.getOhlcvByTicker(startDate, endDate, ticker) }

    /**
     * KRX에서 전종목 시가총액 조회
     */
    suspend fun getMarketCap(date: String, market: Market = Market.ALL): Result<List<MarketCap>> =
        safeCall { stock.getMarketCap(date, market) }

    /**
     * KRX에서 전종목 펀더멘탈 조회 (PER, PBR 등)
     */
    suspend fun getMarketFundamental(
        date: String,
        market: Market = Market.ALL
    ): Result<List<StockFundamental>> =
        safeCall { stock.getMarketFundamental(date, market) }

    /**
     * KRX에서 개별종목 투자자별 거래실적 조회
     */
    suspend fun getTradingByInvestor(
        startDate: String,
        endDate: String,
        ticker: String,
        valueType: TradingValueType = TradingValueType.VALUE,
        askBidType: AskBidType = AskBidType.NET_BUY
    ): Result<List<InvestorTrading>> =
        safeCall { stock.getTradingByInvestor(startDate, endDate, ticker, valueType, askBidType) }

    /**
     * KRX에서 전체시장 투자자별 거래실적 조회
     */
    suspend fun getMarketTradingByInvestor(
        startDate: String,
        endDate: String,
        market: Market = Market.ALL,
        valueType: TradingValueType = TradingValueType.VALUE,
        askBidType: AskBidType = AskBidType.NET_BUY
    ): Result<List<InvestorTrading>> =
        safeCall { stock.getMarketTradingByInvestor(startDate, endDate, market, valueType, askBidType) }

    /**
     * KRX에서 전종목 ETF 시세 조회
     */
    suspend fun getEtfPrice(date: String): Result<List<EtfPrice>> =
        safeCall { etf.getEtfPrice(date) }

    /**
     * KRX에서 ETF 구성종목 조회
     */
    suspend fun getEtfPortfolio(date: String, ticker: String): Result<List<EtfPortfolio>> =
        safeCall { etf.getPortfolio(date, ticker) }

    /**
     * KRX에서 ETF 티커 리스트 조회
     */
    suspend fun getEtfTickerList(date: String): Result<List<EtfInfo>> =
        safeCall { etf.getEtfTickerList(date) }

    /**
     * KRX에서 개별 ETF OHLCV 기간 조회
     */
    suspend fun getEtfOhlcvByTicker(
        startDate: String,
        endDate: String,
        ticker: String
    ): Result<List<EtfOhlcvHistory>> =
        safeCall { etf.getOhlcvByTicker(startDate, endDate, ticker) }

    /**
     * KRX에서 공매도 거래 현황 조회 (전종목)
     */
    suspend fun getShortSellingAll(
        date: String,
        market: Market = Market.KOSPI
    ): Result<List<ShortSelling>> =
        safeCall { stock.getShortSellingAll(date, market) }

    /**
     * KRX에서 개별종목 공매도 거래 일별 추이
     */
    suspend fun getShortSellingByTicker(
        startDate: String,
        endDate: String,
        ticker: String
    ): Result<List<ShortSellingHistory>> =
        safeCall { stock.getShortSellingByTicker(startDate, endDate, ticker) }

    /**
     * KRX에서 지수 OHLCV 기간 조회
     */
    suspend fun getIndexOhlcv(
        startDate: String,
        endDate: String,
        ticker: String
    ): Result<List<IndexOhlcv>> =
        safeCall { index.getOhlcvByTicker(startDate, endDate, ticker) }

    /**
     * KRX에서 지수 목록 조회
     */
    suspend fun getIndexList(
        date: String,
        market: IndexMarket = IndexMarket.ALL
    ): Result<List<IndexInfo>> =
        safeCall { index.getIndexList(date, market) }

    /**
     * 안전한 KRX API 호출 래퍼.
     * KrxError를 Result.failure로 변환.
     */
    private suspend fun <T> safeCall(block: suspend () -> T): Result<T> =
        withContext(ioDispatcher) {
            try {
                Result.success(block())
            } catch (e: KrxError) {
                Log.w(TAG, "KRX API error: ${e.message}")
                Result.failure(e)
            } catch (e: Exception) {
                if (e is CancellationException) throw e
                Log.w(TAG, "KRX unexpected error: ${e.message}")
                Result.failure(e)
            }
        }

    fun close() {
        stock.close()
    }

    companion object {
        private const val TAG = "KrxDataSource"
    }
}
