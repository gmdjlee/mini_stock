"""Tests for market module."""

import pytest

from stock_analyzer.client.kiwoom import ApiResponse
from stock_analyzer.market import deposit


class TestGetDeposit:
    """Tests for get_deposit function."""

    def test_invalid_days(self, mock_kiwoom_client):
        """Test with invalid days parameter."""
        result = deposit.get_deposit(mock_kiwoom_client, 0)
        assert result["ok"] is False
        assert result["error"]["code"] == "INVALID_ARG"

        result = deposit.get_deposit(mock_kiwoom_client, -1)
        assert result["ok"] is False
        assert result["error"]["code"] == "INVALID_ARG"

    def test_success(self, mock_kiwoom_client):
        """Test successful get_deposit."""
        result = deposit.get_deposit(mock_kiwoom_client, 30)
        assert result["ok"] is True
        assert len(result["data"]["dates"]) == 2
        assert len(result["data"]["deposit"]) == 2
        assert len(result["data"]["credit_loan"]) == 2

    def test_date_format(self, mock_kiwoom_client):
        """Test date format conversion."""
        result = deposit.get_deposit(mock_kiwoom_client, 30)
        assert result["ok"] is True
        assert result["data"]["dates"][0] == "2025-01-10"

    def test_deposit_values(self, mock_kiwoom_client):
        """Test deposit values."""
        result = deposit.get_deposit(mock_kiwoom_client, 30)
        assert result["ok"] is True
        assert result["data"]["deposit"][0] == 50000000000000
        assert result["data"]["credit_loan"][0] == 15000000000000

    def test_no_data(self, mock_kiwoom_client):
        """Test with no data."""
        mock_kiwoom_client.get_deposit_trend.return_value = ApiResponse(
            ok=True,
            data={"deposit_list": [], "return_code": 0},
        )
        result = deposit.get_deposit(mock_kiwoom_client, 30)
        assert result["ok"] is False
        assert result["error"]["code"] == "NO_DATA"

    def test_api_error(self, mock_kiwoom_client):
        """Test API error handling."""
        mock_kiwoom_client.get_deposit_trend.return_value = ApiResponse(
            ok=False,
            error={"code": "API_ERROR", "msg": "API 오류"},
        )
        result = deposit.get_deposit(mock_kiwoom_client, 30)
        assert result["ok"] is False
        assert result["error"]["code"] == "API_ERROR"


class TestGetCredit:
    """Tests for get_credit function."""

    def test_invalid_days(self, mock_kiwoom_client):
        """Test with invalid days parameter."""
        result = deposit.get_credit(mock_kiwoom_client, 0)
        assert result["ok"] is False
        assert result["error"]["code"] == "INVALID_ARG"

    def test_success(self, mock_kiwoom_client):
        """Test successful get_credit."""
        result = deposit.get_credit(mock_kiwoom_client, 30)
        assert result["ok"] is True
        assert len(result["data"]["dates"]) == 2
        assert len(result["data"]["credit_balance"]) == 2
        assert len(result["data"]["credit_ratio"]) == 2

    def test_credit_values(self, mock_kiwoom_client):
        """Test credit values."""
        result = deposit.get_credit(mock_kiwoom_client, 30)
        assert result["ok"] is True
        assert result["data"]["credit_balance"][0] == 18000000000000
        assert result["data"]["credit_ratio"][0] == 5.2

    def test_no_data(self, mock_kiwoom_client):
        """Test with no data."""
        mock_kiwoom_client.get_credit_trend.return_value = ApiResponse(
            ok=True,
            data={"credit_list": [], "return_code": 0},
        )
        result = deposit.get_credit(mock_kiwoom_client, 30)
        assert result["ok"] is False
        assert result["error"]["code"] == "NO_DATA"


class TestGetMarketIndicators:
    """Tests for get_market_indicators function."""

    def test_invalid_days(self, mock_kiwoom_client):
        """Test with invalid days parameter."""
        result = deposit.get_market_indicators(mock_kiwoom_client, 0)
        assert result["ok"] is False
        assert result["error"]["code"] == "INVALID_ARG"

    def test_success(self, mock_kiwoom_client):
        """Test successful get_market_indicators."""
        result = deposit.get_market_indicators(mock_kiwoom_client, 30)
        assert result["ok"] is True
        data = result["data"]
        assert "dates" in data
        assert "deposit" in data
        assert "credit_loan" in data
        assert "credit_balance" in data
        assert "credit_ratio" in data

    def test_combined_values(self, mock_kiwoom_client):
        """Test combined market indicator values."""
        result = deposit.get_market_indicators(mock_kiwoom_client, 30)
        assert result["ok"] is True
        data = result["data"]
        # From deposit
        assert data["deposit"][0] == 50000000000000
        assert data["credit_loan"][0] == 15000000000000
        # From credit
        assert data["credit_balance"][0] == 18000000000000
        assert data["credit_ratio"][0] == 5.2

    def test_deposit_api_error(self, mock_kiwoom_client):
        """Test when deposit API fails."""
        mock_kiwoom_client.get_deposit_trend.return_value = ApiResponse(
            ok=False,
            error={"code": "API_ERROR", "msg": "API 오류"},
        )
        result = deposit.get_market_indicators(mock_kiwoom_client, 30)
        assert result["ok"] is False
        assert result["error"]["code"] == "API_ERROR"

    def test_credit_api_error(self, mock_kiwoom_client):
        """Test when credit API fails."""
        mock_kiwoom_client.get_credit_trend.return_value = ApiResponse(
            ok=False,
            error={"code": "API_ERROR", "msg": "API 오류"},
        )
        result = deposit.get_market_indicators(mock_kiwoom_client, 30)
        assert result["ok"] is False
        assert result["error"]["code"] == "API_ERROR"
