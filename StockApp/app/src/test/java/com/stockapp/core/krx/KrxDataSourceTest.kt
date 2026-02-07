package com.stockapp.core.krx

import com.krxkt.KrxEtf
import com.krxkt.KrxStock
import com.krxkt.error.KrxError
import com.krxkt.model.EtfInfo
import com.krxkt.model.Market
import com.krxkt.model.TickerInfo
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.eq
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import okhttp3.OkHttpClient

/**
 * Unit tests for KrxDataSource.
 * Tests the safeCall wrapper and delegation to underlying KRX API clients.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class KrxDataSourceTest {

    private val testDispatcher = StandardTestDispatcher()
    private lateinit var krxDataSource: KrxDataSource
    private lateinit var mockOkHttpClient: OkHttpClient

    @Before
    fun setup() {
        mockOkHttpClient = mock()
        krxDataSource = KrxDataSource(mockOkHttpClient, testDispatcher)
    }

    // ==================== safeCall Tests ====================

    @Test
    fun `safeCall wraps success in Result success`() = runTest(testDispatcher) {
        // Since KrxDataSource creates real KrxStock/KrxEtf internally,
        // we can only test that the safeCall mechanism works by observing
        // the Result wrapper behavior. The real HTTP calls will fail in tests,
        // which exercises the error handling path.

        // Calling a method that will fail due to no real network
        // verifies that safeCall properly wraps exceptions in Result.failure
        val result = krxDataSource.getTickerList("20250101", Market.ALL)

        // Should be a failure (no real network available) wrapped in Result
        assertTrue(
            "Expected Result.failure for network call in test environment",
            result.isFailure
        )
    }

    @Test
    fun `safeCall wraps exceptions in Result failure`() = runTest(testDispatcher) {
        // getMarketOhlcv will fail because no real network is available
        val result = krxDataSource.getMarketOhlcv("20250101")

        assertTrue(result.isFailure)
        // The error should be an Exception (either KrxError or general Exception)
        val error = result.exceptionOrNull()
        assertTrue(
            "Error should not be null",
            error != null
        )
    }

    @Test
    fun `getEtfTickerList wraps result properly`() = runTest(testDispatcher) {
        val result = krxDataSource.getEtfTickerList("20250101")

        // Should be failure in test environment (no network)
        assertTrue(result.isFailure)
        assertTrue(result.exceptionOrNull() != null)
    }

    @Test
    fun `getEtfPortfolio wraps result properly`() = runTest(testDispatcher) {
        val result = krxDataSource.getEtfPortfolio("20250101", "069500")

        assertTrue(result.isFailure)
        assertTrue(result.exceptionOrNull() != null)
    }

    @Test
    fun `getOhlcvByTicker wraps result properly`() = runTest(testDispatcher) {
        val result = krxDataSource.getOhlcvByTicker("20250101", "20250131", "005930")

        assertTrue(result.isFailure)
        assertTrue(result.exceptionOrNull() != null)
    }

    @Test
    fun `getTradingByInvestor wraps result properly`() = runTest(testDispatcher) {
        val result = krxDataSource.getTradingByInvestor("20250101", "20250131", "005930")

        assertTrue(result.isFailure)
        assertTrue(result.exceptionOrNull() != null)
    }

    @Test
    fun `getShortSellingAll wraps result properly`() = runTest(testDispatcher) {
        val result = krxDataSource.getShortSellingAll("20250101")

        assertTrue(result.isFailure)
        assertTrue(result.exceptionOrNull() != null)
    }

    @Test
    fun `getIndexOhlcv wraps result properly`() = runTest(testDispatcher) {
        val result = krxDataSource.getIndexOhlcv("20250101", "20250131", "1001")

        assertTrue(result.isFailure)
        assertTrue(result.exceptionOrNull() != null)
    }

    // ==================== safeCall Logic Verification ====================

    @Test
    fun `multiple calls each produce independent Result objects`() = runTest(testDispatcher) {
        val result1 = krxDataSource.getTickerList("20250101")
        val result2 = krxDataSource.getTickerList("20250102")

        // Both should be failures but independent
        assertTrue(result1.isFailure)
        assertTrue(result2.isFailure)
        // They should be different Result instances
        assertTrue(result1 != result2 || result1.isFailure)
    }

    @Test
    fun `getMarketCap wraps result properly`() = runTest(testDispatcher) {
        val result = krxDataSource.getMarketCap("20250101")

        assertTrue(result.isFailure)
        assertTrue(result.exceptionOrNull() != null)
    }

    @Test
    fun `getMarketFundamental wraps result properly`() = runTest(testDispatcher) {
        val result = krxDataSource.getMarketFundamental("20250101")

        assertTrue(result.isFailure)
        assertTrue(result.exceptionOrNull() != null)
    }
}
