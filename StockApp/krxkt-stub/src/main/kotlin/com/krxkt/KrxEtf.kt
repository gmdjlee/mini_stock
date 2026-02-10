package com.krxkt

import com.krxkt.api.KrxClient
import com.krxkt.cache.TickerCache
import com.krxkt.error.KrxError
import com.krxkt.model.*

class KrxEtf(client: KrxClient, cache: TickerCache) {
    private fun stub(): Nothing = throw KrxError("krxkt stub: KRX data source not available")

    fun getEtfPrice(date: String): List<EtfPrice> = stub()
    fun getPortfolio(date: String, ticker: String): List<EtfPortfolio> = stub()
    fun getEtfTickerList(date: String): List<EtfInfo> = stub()
    fun getOhlcvByTicker(startDate: String, endDate: String, ticker: String): List<EtfOhlcvHistory> = stub()
}
