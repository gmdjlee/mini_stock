package com.stockapp.core.ui.component.stockinput.model

import com.stockapp.core.db.entity.SearchHistoryEntity
import com.stockapp.feature.search.domain.model.Market
import com.stockapp.feature.search.domain.model.Stock

/**
 * SearchHistoryEntity를 Stock으로 변환
 */
fun SearchHistoryEntity.toStock(): Stock = Stock(
    ticker = ticker,
    name = name,
    market = Market.OTHER  // 히스토리에는 market 정보 없음
)

/**
 * Stock을 SearchHistoryEntity로 변환
 */
fun Stock.toHistoryEntity(): SearchHistoryEntity = SearchHistoryEntity(
    ticker = ticker,
    name = name
)
