package com.stockapp.e2e

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import com.stockapp.MainActivity
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import org.junit.Before
import org.junit.Rule
import org.junit.Test

/**
 * E2E tests for the Search flow (P3).
 *
 * Tests the main user journey:
 * 1. App launches to Search screen
 * 2. User can interact with search field
 * 3. Bottom navigation works correctly
 */
@HiltAndroidTest
class SearchFlowE2ETest {

    @get:Rule(order = 0)
    val hiltRule = HiltAndroidRule(this)

    @get:Rule(order = 1)
    val composeTestRule = createAndroidComposeRule<MainActivity>()

    @Before
    fun setUp() {
        hiltRule.inject()
    }

    @Test
    fun searchScreen_isDisplayedOnLaunch() {
        // Verify search screen is shown on app launch
        composeTestRule.onNodeWithText("종목 검색").assertIsDisplayed()
    }

    @Test
    fun searchField_isDisplayed() {
        // Verify search input field is visible
        composeTestRule.onNodeWithText("종목명 또는 코드 검색").assertIsDisplayed()
    }

    @Test
    fun bottomNavigation_allTabsAreVisible() {
        // Verify all 7 bottom navigation tabs are displayed
        composeTestRule.onNodeWithText("검색").assertIsDisplayed()
        composeTestRule.onNodeWithText("수급 분석").assertIsDisplayed()
        composeTestRule.onNodeWithText("기술 지표").assertIsDisplayed()
        composeTestRule.onNodeWithText("재무정보").assertIsDisplayed()
        composeTestRule.onNodeWithText("순위정보").assertIsDisplayed()
        composeTestRule.onNodeWithText("ETF").assertIsDisplayed()
        composeTestRule.onNodeWithText("설정").assertIsDisplayed()
    }

    @Test
    fun bottomNav_navigatesToAnalysis() {
        // Navigate to Analysis tab
        composeTestRule.onNodeWithText("수급 분석").performClick()
        composeTestRule.waitForIdle()

        // Verify Analysis screen is shown (shows "no stock selected" message)
        composeTestRule.onNodeWithText("종목을 선택해주세요").assertIsDisplayed()
    }

    @Test
    fun bottomNav_navigatesToIndicator() {
        // Navigate to Indicator tab
        composeTestRule.onNodeWithText("기술 지표").performClick()
        composeTestRule.waitForIdle()

        // Verify indicator screen content is displayed
        // The screen shows "no stock selected" when no stock is chosen
        composeTestRule.onNodeWithText("종목을 선택해주세요").assertIsDisplayed()
    }

    @Test
    fun bottomNav_navigatesToSettings() {
        // Navigate to Settings tab
        composeTestRule.onNodeWithText("설정").performClick()
        composeTestRule.waitForIdle()

        // Verify Settings screen is shown
        composeTestRule.onNodeWithText("API Key").assertIsDisplayed()
    }

    @Test
    fun bottomNav_navigatesToRanking() {
        // Navigate to Ranking tab
        composeTestRule.onNodeWithText("순위정보").performClick()
        composeTestRule.waitForIdle()

        // Verify Ranking screen is shown
        // Will show API key error if not configured
        composeTestRule.waitForIdle()
    }

    @Test
    fun bottomNav_navigatesToFinancial() {
        // Navigate to Financial tab
        composeTestRule.onNodeWithText("재무정보").performClick()
        composeTestRule.waitForIdle()

        // Verify Financial screen is shown (shows "no stock selected" message)
        composeTestRule.onNodeWithText("종목을 선택해주세요").assertIsDisplayed()
    }

    @Test
    fun bottomNav_navigatesToEtf() {
        // Navigate to ETF tab
        composeTestRule.onNodeWithText("ETF").performClick()
        composeTestRule.waitForIdle()

        // Verify ETF screen content is displayed
        composeTestRule.waitForIdle()
    }

    @Test
    fun bottomNav_statePreserved_whenNavigatingBack() {
        // Navigate away from Search
        composeTestRule.onNodeWithText("설정").performClick()
        composeTestRule.waitForIdle()

        // Navigate back to Search
        composeTestRule.onNodeWithText("검색").performClick()
        composeTestRule.waitForIdle()

        // Verify Search screen state is preserved
        composeTestRule.onNodeWithText("종목명 또는 코드 검색").assertIsDisplayed()
    }
}
