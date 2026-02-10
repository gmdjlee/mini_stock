package com.krxkt

import com.krxkt.api.KrxClient
import com.krxkt.cache.TickerCache
import com.krxkt.error.KrxError
import com.krxkt.model.*

class KrxStock(client: KrxClient, cache: TickerCache) {
    private fun stub(): Nothing = throw KrxError("krxkt stub: KRX data source not available")

    fun getTickerList(date: String, market: Market): List<TickerInfo> = stub()
    fun getMarketOhlcv(date: String, market: Market): List<MarketOhlcv> = stub()
    fun getOhlcvByTicker(startDate: String, endDate: String, ticker: String): List<StockOhlcvHistory> = stub()
    fun getMarketCap(date: String, market: Market): List<MarketCap> = stub()
    fun getMarketFundamental(date: String, market: Market): List<StockFundamental> = stub()
    fun getTradingByInvestor(startDate: String, endDate: String, ticker: String, valueType: TradingValueType, askBidType: AskBidType): List<InvestorTrading> = stub()
    fun getMarketTradingByInvestor(startDate: String, endDate: String, market: Market, valueType: TradingValueType, askBidType: AskBidType): List<InvestorTrading> = stub()
    fun getShortSellingAll(date: String, market: Market): List<ShortSelling> = stub()
    fun getShortSellingByTicker(startDate: String, endDate: String, ticker: String): List<ShortSellingHistory> = stub()
    fun close() {}
}
