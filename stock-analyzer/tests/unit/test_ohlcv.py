"""Tests for ohlcv module."""

import pytest

from stock_analyzer.client.kiwoom import ApiResponse
from stock_analyzer.stock import ohlcv


class TestGetDaily:
    """Tests for get_daily function."""

    def test_empty_ticker(self, mock_kiwoom_client):
        """Test with empty ticker."""
        result = ohlcv.get_daily(mock_kiwoom_client, "")
        assert result["ok"] is False
        assert result["error"]["code"] == "INVALID_ARG"

    def test_success(self, mock_kiwoom_client):
        """Test successful call."""
        result = ohlcv.get_daily(mock_kiwoom_client, "005930")
        assert result["ok"] is True
        assert result["data"]["ticker"] == "005930"
        assert len(result["data"]["dates"]) == 2
        assert len(result["data"]["open"]) == 2
        assert len(result["data"]["high"]) == 2
        assert len(result["data"]["low"]) == 2
        assert len(result["data"]["close"]) == 2
        assert len(result["data"]["volume"]) == 2

    def test_values(self, mock_kiwoom_client):
        """Test OHLCV values."""
        result = ohlcv.get_daily(mock_kiwoom_client, "005930")
        assert result["data"]["open"][0] == 54000
        assert result["data"]["high"][0] == 55500
        assert result["data"]["low"][0] == 53800
        assert result["data"]["close"][0] == 55000
        assert result["data"]["volume"][0] == 15000000

    def test_no_data(self, mock_kiwoom_client):
        """Test with no chart data."""
        mock_kiwoom_client.get_daily_chart.return_value = ApiResponse(
            ok=True,
            data={"chart_list": [], "return_code": 0},
        )
        result = ohlcv.get_daily(mock_kiwoom_client, "005930")
        assert result["ok"] is False
        assert result["error"]["code"] == "NO_DATA"


class TestGetWeekly:
    """Tests for get_weekly function."""

    def test_empty_ticker(self, mock_kiwoom_client):
        """Test with empty ticker."""
        result = ohlcv.get_weekly(mock_kiwoom_client, "")
        assert result["ok"] is False
        assert result["error"]["code"] == "INVALID_ARG"

    def test_success(self, mock_kiwoom_client):
        """Test successful call."""
        mock_kiwoom_client.get_weekly_chart.return_value = ApiResponse(
            ok=True,
            data={
                "stk_stk_pole_chart_qry": [
                    {
                        "dt": "20250110",
                        "open_pric": 53000,
                        "high_pric": 56000,
                        "low_pric": 52500,
                        "cur_prc": 55000,
                        "trde_qty": 75000000,
                    },
                ],
                "return_code": 0,
            },
        )
        result = ohlcv.get_weekly(mock_kiwoom_client, "005930")
        assert result["ok"] is True
        assert len(result["data"]["dates"]) == 1


class TestGetMonthly:
    """Tests for get_monthly function."""

    def test_empty_ticker(self, mock_kiwoom_client):
        """Test with empty ticker."""
        result = ohlcv.get_monthly(mock_kiwoom_client, "")
        assert result["ok"] is False
        assert result["error"]["code"] == "INVALID_ARG"

    def test_success(self, mock_kiwoom_client):
        """Test successful call."""
        mock_kiwoom_client.get_monthly_chart.return_value = ApiResponse(
            ok=True,
            data={
                "stk_mth_pole_chart_qry": [
                    {
                        "dt": "202501",
                        "open_pric": 52000,
                        "high_pric": 58000,
                        "low_pric": 51000,
                        "cur_prc": 55000,
                        "trde_qty": 300000000,
                    },
                ],
                "return_code": 0,
            },
        )
        result = ohlcv.get_monthly(mock_kiwoom_client, "005930")
        assert result["ok"] is True
        assert len(result["data"]["dates"]) == 1
