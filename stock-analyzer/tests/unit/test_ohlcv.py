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


class TestResampleToWeekly:
    """Tests for resample_to_weekly function."""

    def test_empty_data(self):
        """Test with empty data."""
        result = ohlcv.resample_to_weekly([], [], [], [], [], [])
        assert result["dates"] == []
        assert result["open"] == []

    def test_single_week(self):
        """Test with data from a single week."""
        # Monday to Friday of same week (newest first)
        dates = ["20250110", "20250109", "20250108", "20250107", "20250106"]
        opens = [100, 101, 102, 103, 104]
        highs = [110, 111, 112, 113, 114]
        lows = [90, 91, 92, 93, 94]
        closes = [105, 106, 107, 108, 109]
        volumes = [1000, 1100, 1200, 1300, 1400]

        result = ohlcv.resample_to_weekly(dates, opens, highs, lows, closes, volumes)

        assert len(result["dates"]) == 1
        assert result["dates"][0] == "20250110"  # Last day of week
        assert result["open"][0] == 104  # First day's open (Monday)
        assert result["high"][0] == 114  # Max high
        assert result["low"][0] == 90  # Min low
        assert result["close"][0] == 105  # Last day's close (Friday)
        assert result["volume"][0] == 6000  # Sum of volumes

    def test_multiple_weeks(self):
        """Test with data from multiple weeks."""
        # Two weeks of data (newest first)
        dates = [
            "20250117", "20250116", "20250115",  # Week 3
            "20250110", "20250109", "20250108",  # Week 2
        ]
        opens = [200, 201, 202, 100, 101, 102]
        highs = [220, 221, 222, 120, 121, 122]
        lows = [180, 181, 182, 80, 81, 82]
        closes = [210, 211, 212, 110, 111, 112]
        volumes = [2000, 2100, 2200, 1000, 1100, 1200]

        result = ohlcv.resample_to_weekly(dates, opens, highs, lows, closes, volumes)

        assert len(result["dates"]) == 2
        # Newest week first
        assert result["dates"][0] == "20250117"
        assert result["dates"][1] == "20250110"


class TestResampleToMonthly:
    """Tests for resample_to_monthly function."""

    def test_empty_data(self):
        """Test with empty data."""
        result = ohlcv.resample_to_monthly([], [], [], [], [], [])
        assert result["dates"] == []

    def test_single_month(self):
        """Test with data from a single month."""
        dates = ["20250115", "20250110", "20250105"]
        opens = [100, 101, 102]
        highs = [110, 115, 112]
        lows = [90, 91, 88]
        closes = [105, 106, 107]
        volumes = [1000, 1100, 1200]

        result = ohlcv.resample_to_monthly(dates, opens, highs, lows, closes, volumes)

        assert len(result["dates"]) == 1
        assert result["dates"][0] == "20250115"  # Last day of month
        assert result["open"][0] == 102  # First day's open
        assert result["high"][0] == 115  # Max high
        assert result["low"][0] == 88  # Min low
        assert result["close"][0] == 105  # Last day's close
        assert result["volume"][0] == 3300  # Sum of volumes

    def test_multiple_months(self):
        """Test with data from multiple months."""
        dates = [
            "20250115", "20250110",  # January 2025
            "20241220", "20241210",  # December 2024
        ]
        opens = [200, 201, 100, 101]
        highs = [220, 221, 120, 121]
        lows = [180, 181, 80, 81]
        closes = [210, 211, 110, 111]
        volumes = [2000, 2100, 1000, 1100]

        result = ohlcv.resample_to_monthly(dates, opens, highs, lows, closes, volumes)

        assert len(result["dates"]) == 2
        # Newest month first
        assert result["dates"][0] == "20250115"
        assert result["dates"][1] == "20241220"
