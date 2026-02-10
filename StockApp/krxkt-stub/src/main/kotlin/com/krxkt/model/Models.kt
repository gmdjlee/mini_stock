package com.krxkt.model

data class TickerInfo(
    val ticker: String = "",
    val name: String = "",
    val marketName: String = ""
)

data class MarketOhlcv(
    val ticker: String = "",
    val name: String = "",
    val close: Long = 0,
    val open: Long = 0,
    val high: Long = 0,
    val low: Long = 0,
    val volume: Long = 0,
    val changeRate: Double = 0.0
)

data class StockOhlcvHistory(
    val date: String = "",
    val open: Long = 0,
    val high: Long = 0,
    val low: Long = 0,
    val close: Long = 0,
    val volume: Long = 0
)

data class MarketCap(
    val ticker: String = "",
    val name: String = "",
    val marketCap: Long = 0,
    val volume: Long = 0
)

data class StockFundamental(
    val ticker: String = "",
    val name: String = "",
    val per: Double = 0.0,
    val pbr: Double = 0.0,
    val dividendYield: Double = 0.0
)

data class InvestorTrading(
    val date: String = "",
    val foreigner: Long = 0,
    val institutionalTotal: Long = 0,
    val individual: Long = 0,
    val total: Long = 0
)

data class EtfInfo(
    val ticker: String = "",
    val name: String = "",
    val indexProvider: String? = null,
    val targetIndexName: String? = null
)

data class EtfPrice(
    val ticker: String = "",
    val name: String = "",
    val close: Long = 0,
    val nav: Double = 0.0,
    val volume: Long = 0
)

data class EtfPortfolio(
    val ticker: String = "",
    val name: String = "",
    val amount: Long = 0,
    val weight: Double? = null,
    val valuationAmount: Long = 0
)

data class EtfOhlcvHistory(
    val date: String = "",
    val open: Long = 0,
    val high: Long = 0,
    val low: Long = 0,
    val close: Long = 0,
    val volume: Long = 0
)

data class ShortSelling(
    val ticker: String = "",
    val name: String = "",
    val totalVolume: Long = 0,
    val shortVolume: Long = 0
)

data class ShortSellingHistory(
    val date: String = "",
    val totalVolume: Long = 0,
    val shortVolume: Long = 0
)

data class IndexOhlcv(
    val date: String = "",
    val open: Long = 0,
    val high: Long = 0,
    val low: Long = 0,
    val close: Long = 0,
    val volume: Long = 0
)

data class IndexInfo(
    val ticker: String = "",
    val name: String = ""
)
