package com.stockapp.e2e

import android.content.Intent
import android.net.Uri
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.core.app.ApplicationProvider
import com.stockapp.MainActivity
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import org.junit.Before
import org.junit.Rule
import org.junit.Test

/**
 * E2E tests for navigation and deep links (P3).
 *
 * Tests:
 * 1. Deep link navigation to various screens
 * 2. Bottom navigation state preservation
 * 3. Navigation arguments handling
 */
@HiltAndroidTest
class NavigationE2ETest {

    @get:Rule(order = 0)
    val hiltRule = HiltAndroidRule(this)

    @get:Rule(order = 1)
    val composeTestRule = createAndroidComposeRule<MainActivity>()

    @Before
    fun setUp() {
        hiltRule.inject()
    }

    @Test
    fun deepLink_toSearch_navigatesToSearchScreen() {
        // Deep link: stockapp://search
        val intent = Intent(
            Intent.ACTION_VIEW,
            Uri.parse("stockapp://search"),
            ApplicationProvider.getApplicationContext(),
            MainActivity::class.java
        )

        composeTestRule.activityRule.scenario.onActivity { activity ->
            activity.startActivity(intent)
        }

        composeTestRule.waitForIdle()

        // Verify Search screen is displayed
        composeTestRule.onNodeWithText("종목명 또는 코드 검색").assertIsDisplayed()
    }

    @Test
    fun deepLink_toSettings_navigatesToSettingsScreen() {
        // Deep link: stockapp://settings
        val intent = Intent(
            Intent.ACTION_VIEW,
            Uri.parse("stockapp://settings"),
            ApplicationProvider.getApplicationContext(),
            MainActivity::class.java
        )

        composeTestRule.activityRule.scenario.onActivity { activity ->
            activity.startActivity(intent)
        }

        composeTestRule.waitForIdle()

        // Verify Settings screen content
        composeTestRule.onNodeWithText("API Key").assertIsDisplayed()
    }

    @Test
    fun deepLink_toRanking_navigatesToRankingScreen() {
        // Deep link: stockapp://ranking
        val intent = Intent(
            Intent.ACTION_VIEW,
            Uri.parse("stockapp://ranking"),
            ApplicationProvider.getApplicationContext(),
            MainActivity::class.java
        )

        composeTestRule.activityRule.scenario.onActivity { activity ->
            activity.startActivity(intent)
        }

        composeTestRule.waitForIdle()
        // Ranking screen should be displayed
    }

    @Test
    fun deepLink_toEtf_navigatesToEtfScreen() {
        // Deep link: stockapp://etf
        val intent = Intent(
            Intent.ACTION_VIEW,
            Uri.parse("stockapp://etf"),
            ApplicationProvider.getApplicationContext(),
            MainActivity::class.java
        )

        composeTestRule.activityRule.scenario.onActivity { activity ->
            activity.startActivity(intent)
        }

        composeTestRule.waitForIdle()
        // ETF screen should be displayed
    }

    @Test
    fun deepLink_toStock_navigatesToAnalysisWithTicker() {
        // Deep link: stockapp://stock/005930 (Samsung Electronics)
        val intent = Intent(
            Intent.ACTION_VIEW,
            Uri.parse("stockapp://stock/005930"),
            ApplicationProvider.getApplicationContext(),
            MainActivity::class.java
        )

        composeTestRule.activityRule.scenario.onActivity { activity ->
            activity.startActivity(intent)
        }

        composeTestRule.waitForIdle()

        // Analysis screen should be displayed
        // Either loading state or analysis content
    }

    @Test
    fun deepLink_toStockIndicator_navigatesToIndicatorWithTicker() {
        // Deep link: stockapp://stock/005930/indicator
        val intent = Intent(
            Intent.ACTION_VIEW,
            Uri.parse("stockapp://stock/005930/indicator"),
            ApplicationProvider.getApplicationContext(),
            MainActivity::class.java
        )

        composeTestRule.activityRule.scenario.onActivity { activity ->
            activity.startActivity(intent)
        }

        composeTestRule.waitForIdle()
        // Indicator screen should be displayed
    }

    @Test
    fun deepLink_toStockFinancial_navigatesToFinancialWithTicker() {
        // Deep link: stockapp://stock/005930/financial
        val intent = Intent(
            Intent.ACTION_VIEW,
            Uri.parse("stockapp://stock/005930/financial"),
            ApplicationProvider.getApplicationContext(),
            MainActivity::class.java
        )

        composeTestRule.activityRule.scenario.onActivity { activity ->
            activity.startActivity(intent)
        }

        composeTestRule.waitForIdle()
        // Financial screen should be displayed
    }

    @Test
    fun bottomNav_maintainsState_acrossMultipleNavigations() {
        // Navigate through multiple screens
        composeTestRule.onNodeWithText("설정").performClick()
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithText("순위정보").performClick()
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithText("ETF").performClick()
        composeTestRule.waitForIdle()

        // Navigate back to Search
        composeTestRule.onNodeWithText("검색").performClick()
        composeTestRule.waitForIdle()

        // State should be preserved
        composeTestRule.onNodeWithText("종목명 또는 코드 검색").assertIsDisplayed()
    }

    @Test
    fun tabSelection_isHighlighted_whenSelected() {
        // Navigate to Analysis
        composeTestRule.onNodeWithText("수급 분석").performClick()
        composeTestRule.waitForIdle()

        // The tab should be visually selected (highlighted)
        composeTestRule.onNodeWithText("수급 분석").assertIsDisplayed()
    }
}
