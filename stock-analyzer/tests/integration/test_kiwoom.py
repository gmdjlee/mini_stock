"""Integration tests for Kiwoom API.

These tests require valid API credentials and should be run against
the mock trading server for safety.

To run:
    KIWOOM_BASE_URL=https://mockapi.kiwoom.com pytest tests/integration/ -v
"""

import os

import pytest

# Skip all tests in this module if credentials are not set
pytestmark = pytest.mark.skipif(
    not os.getenv("KIWOOM_APP_KEY") or not os.getenv("KIWOOM_SECRET_KEY"),
    reason="KIWOOM_APP_KEY and KIWOOM_SECRET_KEY required",
)


@pytest.fixture
def live_client():
    """Create a live KiwoomClient."""
    from stock_analyzer.client.kiwoom import KiwoomClient
    from stock_analyzer.config import Config

    config = Config.from_env()
    return KiwoomClient(
        app_key=config.app_key,
        secret_key=config.secret_key,
        base_url=config.base_url,
    )


class TestLiveAuth:
    """Live authentication tests."""

    def test_token_fetch(self, live_client):
        """Test actual token fetch."""
        token = live_client.auth.get_token()
        assert token.token is not None
        assert not token.is_expired


class TestLiveStockInfo:
    """Live stock info tests."""

    def test_get_stock_list(self, live_client):
        """Test get stock list."""
        resp = live_client.get_stock_list()
        assert resp.ok is True
        assert "stk_list" in resp.data

    def test_get_stock_info(self, live_client):
        """Test get stock info for Samsung."""
        resp = live_client.get_stock_info("005930")
        assert resp.ok is True
        assert resp.data.get("stk_nm") is not None
