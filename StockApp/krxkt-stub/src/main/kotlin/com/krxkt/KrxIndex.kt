package com.krxkt

import com.krxkt.api.KrxClient
import com.krxkt.error.KrxError
import com.krxkt.model.*

class KrxIndex(client: KrxClient) {
    private fun stub(): Nothing = throw KrxError("krxkt stub: KRX data source not available")

    fun getOhlcvByTicker(startDate: String, endDate: String, ticker: String): List<IndexOhlcv> = stub()
    fun getIndexList(date: String, market: IndexMarket): List<IndexInfo> = stub()
}
