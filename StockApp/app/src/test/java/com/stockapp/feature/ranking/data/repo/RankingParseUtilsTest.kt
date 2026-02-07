package com.stockapp.feature.ranking.data.repo

import org.junit.Assert.assertEquals
import org.junit.Test

class RankingParseUtilsTest {

    // ===== cleanTicker =====

    @Test
    fun `cleanTicker removes _AL suffix`() {
        assertEquals("005930", RankingParseUtils.cleanTicker("005930_AL"))
    }

    @Test
    fun `cleanTicker removes _KS suffix`() {
        assertEquals("005930", RankingParseUtils.cleanTicker("005930_KS"))
    }

    @Test
    fun `cleanTicker removes _KQ suffix`() {
        assertEquals("035720", RankingParseUtils.cleanTicker("035720_KQ"))
    }

    @Test
    fun `cleanTicker removes multiple suffixes`() {
        assertEquals("005930", RankingParseUtils.cleanTicker("005930_AL_KS"))
    }

    @Test
    fun `cleanTicker trims whitespace`() {
        assertEquals("005930", RankingParseUtils.cleanTicker("  005930  "))
    }

    @Test
    fun `cleanTicker returns empty for null`() {
        assertEquals("", RankingParseUtils.cleanTicker(null))
    }

    @Test
    fun `cleanTicker returns plain ticker unchanged`() {
        assertEquals("005930", RankingParseUtils.cleanTicker("005930"))
    }

    // ===== parseLong =====

    @Test
    fun `parseLong parses simple number`() {
        assertEquals(12345L, RankingParseUtils.parseLong("12345"))
    }

    @Test
    fun `parseLong removes commas`() {
        assertEquals(1234567L, RankingParseUtils.parseLong("1,234,567"))
    }

    @Test
    fun `parseLong removes plus sign`() {
        assertEquals(12345L, RankingParseUtils.parseLong("+12345"))
    }

    @Test
    fun `parseLong removes comma and plus sign`() {
        assertEquals(1234567L, RankingParseUtils.parseLong("+1,234,567"))
    }

    @Test
    fun `parseLong handles negative numbers`() {
        assertEquals(-12345L, RankingParseUtils.parseLong("-12345"))
    }

    @Test
    fun `parseLong trims whitespace`() {
        assertEquals(12345L, RankingParseUtils.parseLong("  12345  "))
    }

    @Test
    fun `parseLong returns 0 for null`() {
        assertEquals(0L, RankingParseUtils.parseLong(null))
    }

    @Test
    fun `parseLong returns 0 for empty string`() {
        assertEquals(0L, RankingParseUtils.parseLong(""))
    }

    @Test
    fun `parseLong returns 0 for non-numeric string`() {
        assertEquals(0L, RankingParseUtils.parseLong("abc"))
    }

    @Test
    fun `parseLong handles zero`() {
        assertEquals(0L, RankingParseUtils.parseLong("0"))
    }

    // ===== parseDouble =====

    @Test
    fun `parseDouble parses simple decimal`() {
        assertEquals(12.34, RankingParseUtils.parseDouble("12.34"), 0.001)
    }

    @Test
    fun `parseDouble removes commas`() {
        assertEquals(1234.56, RankingParseUtils.parseDouble("1,234.56"), 0.001)
    }

    @Test
    fun `parseDouble removes plus sign`() {
        assertEquals(12.34, RankingParseUtils.parseDouble("+12.34"), 0.001)
    }

    @Test
    fun `parseDouble removes percent sign`() {
        assertEquals(12.34, RankingParseUtils.parseDouble("12.34%"), 0.001)
    }

    @Test
    fun `parseDouble removes plus comma and percent`() {
        assertEquals(1234.56, RankingParseUtils.parseDouble("+1,234.56%"), 0.001)
    }

    @Test
    fun `parseDouble handles negative`() {
        assertEquals(-12.34, RankingParseUtils.parseDouble("-12.34"), 0.001)
    }

    @Test
    fun `parseDouble returns 0 for null`() {
        assertEquals(0.0, RankingParseUtils.parseDouble(null), 0.001)
    }

    @Test
    fun `parseDouble returns 0 for empty string`() {
        assertEquals(0.0, RankingParseUtils.parseDouble(""), 0.001)
    }

    @Test
    fun `parseDouble returns 0 for non-numeric string`() {
        assertEquals(0.0, RankingParseUtils.parseDouble("abc"), 0.001)
    }

    @Test
    fun `parseDouble handles integer value`() {
        assertEquals(12345.0, RankingParseUtils.parseDouble("12345"), 0.001)
    }

    // ===== parseSign =====

    @Test
    fun `parseSign returns plus for 1`() {
        assertEquals("+", RankingParseUtils.parseSign("1"))
    }

    @Test
    fun `parseSign returns plus for 2`() {
        assertEquals("+", RankingParseUtils.parseSign("2"))
    }

    @Test
    fun `parseSign returns plus for plus`() {
        assertEquals("+", RankingParseUtils.parseSign("+"))
    }

    @Test
    fun `parseSign returns minus for 4`() {
        assertEquals("-", RankingParseUtils.parseSign("4"))
    }

    @Test
    fun `parseSign returns minus for 5`() {
        assertEquals("-", RankingParseUtils.parseSign("5"))
    }

    @Test
    fun `parseSign returns minus for minus`() {
        assertEquals("-", RankingParseUtils.parseSign("-"))
    }

    @Test
    fun `parseSign returns empty for 3`() {
        assertEquals("", RankingParseUtils.parseSign("3"))
    }

    @Test
    fun `parseSign returns empty for null`() {
        assertEquals("", RankingParseUtils.parseSign(null))
    }

    @Test
    fun `parseSign returns empty for empty string`() {
        assertEquals("", RankingParseUtils.parseSign(""))
    }

    @Test
    fun `parseSign trims whitespace`() {
        assertEquals("+", RankingParseUtils.parseSign("  1  "))
    }
}
