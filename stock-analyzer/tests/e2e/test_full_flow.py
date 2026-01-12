"""End-to-end tests for full workflow.

These tests verify the complete flow from search to analysis.
Requires valid API credentials.
"""

import os

import pytest

# Skip all tests in this module if credentials are not set
pytestmark = pytest.mark.skipif(
    not os.getenv("KIWOOM_APP_KEY") or not os.getenv("KIWOOM_SECRET_KEY"),
    reason="KIWOOM_APP_KEY and KIWOOM_SECRET_KEY required",
)


@pytest.fixture
def client():
    """Create a KiwoomClient."""
    from stock_analyzer.client.kiwoom import KiwoomClient
    from stock_analyzer.config import Config

    config = Config.from_env()
    return KiwoomClient(
        app_key=config.app_key,
        secret_key=config.secret_key,
        base_url=config.base_url,
    )


class TestFullFlow:
    """Full workflow tests."""

    def test_search_and_analyze(self, client):
        """Test search → analyze flow."""
        from stock_analyzer.stock import analysis, search

        # 1. Search for stock
        search_result = search.search(client, "삼성전자")
        assert search_result["ok"] is True
        assert len(search_result["data"]) > 0

        # 2. Get first result ticker
        ticker = search_result["data"][0]["ticker"]

        # 3. Analyze stock
        analysis_result = analysis.analyze(client, ticker)
        assert analysis_result["ok"] is True
        assert analysis_result["data"]["ticker"] == ticker

    def test_search_and_ohlcv(self, client):
        """Test search → OHLCV flow."""
        from stock_analyzer.stock import ohlcv, search

        # 1. Search for stock
        search_result = search.search(client, "005930")
        assert search_result["ok"] is True

        # 2. Get OHLCV data
        ohlcv_result = ohlcv.get_daily(client, "005930", days=30)
        assert ohlcv_result["ok"] is True
        assert len(ohlcv_result["data"]["dates"]) > 0
