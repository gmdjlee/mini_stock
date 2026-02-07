package com.stockapp.feature.etf.data.repo

import com.stockapp.core.db.entity.EtfEntity
import com.stockapp.feature.etf.domain.model.EtfFilterConfig
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Tests for ETF filtering logic (shouldIncludeEtf).
 *
 * Since shouldIncludeEtf is private in EtfCollectorRepoImpl,
 * we replicate the exact filter logic here for unit testing.
 * This approach tests the algorithm in isolation without needing
 * to mock all of EtfCollectorRepoImpl's dependencies.
 */
class EtfFilterTest {

    /**
     * Replicates the exact shouldIncludeEtf logic from EtfCollectorRepoImpl.
     * This must be kept in sync with the source implementation.
     */
    private fun shouldIncludeEtf(etf: EtfEntity, config: EtfFilterConfig): Boolean {
        val name = etf.etfName

        // Check Active only filter
        if (config.activeOnly && etf.etfType != "Active") {
            return false
        }

        // Check exclude keywords
        if (config.excludeKeywords.any { name.contains(it, ignoreCase = true) }) {
            return false
        }

        // Check include keywords (if specified, at least one must match)
        if (config.includeKeywords.isNotEmpty()) {
            return config.includeKeywords.any { name.contains(it, ignoreCase = true) }
        }

        return true
    }

    // ==================== Helper ====================

    private fun createEtf(
        code: String = "069500",
        name: String = "KODEX 200",
        type: String = "Passive"
    ) = EtfEntity(
        etfCode = code,
        etfName = name,
        etfType = type,
        managementCompany = "Samsung",
        trackingIndex = "KOSPI 200",
        assetClass = "Equity",
        totalAssets = 1000.0,
        isFiltered = false,
        updatedAt = System.currentTimeMillis()
    )

    // ==================== activeOnly Tests ====================

    @Test
    fun `activeOnly true includes only Active type ETFs`() {
        val config = EtfFilterConfig(
            activeOnly = true,
            includeKeywords = emptyList(),
            excludeKeywords = emptyList()
        )

        val activeEtf = createEtf(name = "KODEX 반도체 액티브", type = "Active")
        val passiveEtf = createEtf(name = "KODEX 200", type = "Passive")

        assertTrue(shouldIncludeEtf(activeEtf, config))
        assertFalse(shouldIncludeEtf(passiveEtf, config))
    }

    @Test
    fun `activeOnly false includes all types`() {
        val config = EtfFilterConfig(
            activeOnly = false,
            includeKeywords = emptyList(),
            excludeKeywords = emptyList()
        )

        val activeEtf = createEtf(name = "KODEX 반도체 액티브", type = "Active")
        val passiveEtf = createEtf(name = "KODEX 200", type = "Passive")

        assertTrue(shouldIncludeEtf(activeEtf, config))
        assertTrue(shouldIncludeEtf(passiveEtf, config))
    }

    // ==================== Include Keywords Tests ====================

    @Test
    fun `include keywords - ETF name containing keyword is included`() {
        val config = EtfFilterConfig(
            activeOnly = false,
            includeKeywords = listOf("반도체", "AI"),
            excludeKeywords = emptyList()
        )

        val semEtf = createEtf(name = "KODEX 반도체 ETF")
        val aiEtf = createEtf(name = "TIGER AI 소프트웨어")
        val otherEtf = createEtf(name = "KODEX 200")

        assertTrue(shouldIncludeEtf(semEtf, config))
        assertTrue(shouldIncludeEtf(aiEtf, config))
        assertFalse(shouldIncludeEtf(otherEtf, config))
    }

    @Test
    fun `include keywords are case insensitive`() {
        val config = EtfFilterConfig(
            activeOnly = false,
            includeKeywords = listOf("ai"),
            excludeKeywords = emptyList()
        )

        val upperEtf = createEtf(name = "KODEX AI ETF")
        val lowerEtf = createEtf(name = "Tiger ai tech")

        assertTrue(shouldIncludeEtf(upperEtf, config))
        assertTrue(shouldIncludeEtf(lowerEtf, config))
    }

    @Test
    fun `empty include keywords means all non-excluded pass`() {
        val config = EtfFilterConfig(
            activeOnly = false,
            includeKeywords = emptyList(),
            excludeKeywords = emptyList()
        )

        val anyEtf = createEtf(name = "ANY ETF NAME")
        assertTrue(shouldIncludeEtf(anyEtf, config))
    }

    // ==================== Exclude Keywords Tests ====================

    @Test
    fun `exclude keywords - ETF name containing exclude keyword is excluded`() {
        val config = EtfFilterConfig(
            activeOnly = false,
            includeKeywords = emptyList(),
            excludeKeywords = listOf("인버스", "레버리지")
        )

        val inverseEtf = createEtf(name = "KODEX 200 인버스")
        val leveragedEtf = createEtf(name = "TIGER 2차전지 레버리지")
        val normalEtf = createEtf(name = "KODEX 반도체")

        assertFalse(shouldIncludeEtf(inverseEtf, config))
        assertFalse(shouldIncludeEtf(leveragedEtf, config))
        assertTrue(shouldIncludeEtf(normalEtf, config))
    }

    @Test
    fun `exclude keywords are case insensitive`() {
        val config = EtfFilterConfig(
            activeOnly = false,
            includeKeywords = emptyList(),
            excludeKeywords = listOf("china")
        )

        val chinaUpper = createEtf(name = "KODEX China ETF")
        val chinaLower = createEtf(name = "TIGER china tech")

        assertFalse(shouldIncludeEtf(chinaUpper, config))
        assertFalse(shouldIncludeEtf(chinaLower, config))
    }

    // ==================== Exclude Priority Over Include ====================

    @Test
    fun `exclude takes priority over include`() {
        val config = EtfFilterConfig(
            activeOnly = false,
            includeKeywords = listOf("반도체"),
            excludeKeywords = listOf("인버스")
        )

        // This ETF matches include ("반도체") but also matches exclude ("인버스")
        val etf = createEtf(name = "KODEX 반도체 인버스")
        assertFalse(shouldIncludeEtf(etf, config))
    }

    @Test
    fun `exclude takes priority over include - leveraged semiconductor`() {
        val config = EtfFilterConfig(
            activeOnly = false,
            includeKeywords = listOf("2차전지"),
            excludeKeywords = listOf("2X", "레버리지")
        )

        val leveragedEtf = createEtf(name = "KODEX 2차전지 2X 레버리지")
        assertFalse(shouldIncludeEtf(leveragedEtf, config))
    }

    // ==================== Combined Filtering ====================

    @Test
    fun `combined filtering - activeOnly plus include plus exclude`() {
        val config = EtfFilterConfig(
            activeOnly = true,
            includeKeywords = listOf("반도체", "AI"),
            excludeKeywords = listOf("인버스", "레버리지")
        )

        // Active + include match + no exclude -> included
        val goodEtf = createEtf(name = "KODEX 반도체 액티브", type = "Active")
        assertTrue(shouldIncludeEtf(goodEtf, config))

        // Passive -> excluded by activeOnly
        val passiveEtf = createEtf(name = "KODEX 반도체", type = "Passive")
        assertFalse(shouldIncludeEtf(passiveEtf, config))

        // Active + no include match -> excluded
        val noMatchEtf = createEtf(name = "KODEX 200 액티브", type = "Active")
        assertFalse(shouldIncludeEtf(noMatchEtf, config))

        // Active + include match + exclude match -> excluded (exclude wins)
        val excludedEtf = createEtf(name = "KODEX 반도체 인버스 액티브", type = "Active")
        assertFalse(shouldIncludeEtf(excludedEtf, config))
    }

    @Test
    fun `default filter config keywords work correctly`() {
        val config = EtfFilterConfig() // uses default keywords

        // Should be included: contains default include keyword "반도체"
        val semEtf = createEtf(name = "KODEX 반도체 액티브", type = "Active")
        assertTrue(shouldIncludeEtf(semEtf, config))

        // Should be excluded: contains default exclude keyword "인버스"
        val inverseEtf = createEtf(name = "KODEX 반도체 인버스 액티브", type = "Active")
        assertFalse(shouldIncludeEtf(inverseEtf, config))

        // Should be excluded: Passive type (activeOnly=true by default)
        val passiveEtf = createEtf(name = "KODEX 반도체", type = "Passive")
        assertFalse(shouldIncludeEtf(passiveEtf, config))
    }

    @Test
    fun `multiple include keywords - any match is sufficient`() {
        val config = EtfFilterConfig(
            activeOnly = false,
            includeKeywords = listOf("반도체", "바이오", "AI"),
            excludeKeywords = emptyList()
        )

        assertTrue(shouldIncludeEtf(createEtf(name = "KODEX 반도체"), config))
        assertTrue(shouldIncludeEtf(createEtf(name = "TIGER 바이오"), config))
        assertTrue(shouldIncludeEtf(createEtf(name = "ARIRANG AI"), config))
        assertFalse(shouldIncludeEtf(createEtf(name = "KODEX 200"), config))
    }

    @Test
    fun `multiple exclude keywords - any match excludes`() {
        val config = EtfFilterConfig(
            activeOnly = false,
            includeKeywords = emptyList(),
            excludeKeywords = listOf("인버스", "레버리지", "차이나")
        )

        assertFalse(shouldIncludeEtf(createEtf(name = "KODEX 200 인버스"), config))
        assertFalse(shouldIncludeEtf(createEtf(name = "TIGER 레버리지"), config))
        assertFalse(shouldIncludeEtf(createEtf(name = "KODEX 차이나"), config))
        assertTrue(shouldIncludeEtf(createEtf(name = "KODEX 반도체"), config))
    }

    @Test
    fun `partial keyword match works`() {
        val config = EtfFilterConfig(
            activeOnly = false,
            includeKeywords = listOf("테크"),
            excludeKeywords = emptyList()
        )

        // "테크" is a substring of "바이오테크놀로지"
        assertTrue(shouldIncludeEtf(createEtf(name = "TIGER 바이오테크놀로지"), config))
    }
}
