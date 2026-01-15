"""Tests for indicator module."""

import pytest

from stock_analyzer.client.kiwoom import ApiResponse
from stock_analyzer.indicator import trend, elder, demark


# ============================================================
# Test Fixtures
# ============================================================


@pytest.fixture
def sample_ohlcv_data():
    """Generate sample OHLCV data for testing (100 days)."""
    # Simulate uptrend then downtrend
    base_price = 50000
    dates = [f"202501{100-i:02d}" for i in range(100)]

    # Create realistic price movement
    closes = []
    highs = []
    lows = []
    opens = []
    volumes = []

    for i in range(100):
        # Price oscillates with overall trend
        if i < 50:
            # Uptrend
            price = base_price + (50 - i) * 100 + (i % 5) * 50
        else:
            # Downtrend
            price = base_price + (100 - i) * 100 - (i % 5) * 50

        closes.append(price)
        highs.append(price + 500)
        lows.append(price - 500)
        opens.append(price + 100)
        volumes.append(1000000 + i * 10000)

    return {
        "dates": dates,
        "close": closes,
        "high": highs,
        "low": lows,
        "open": opens,
        "volume": volumes,
    }


@pytest.fixture
def mock_chart_response_extended():
    """Extended chart data for indicator testing (matches ka10081 API response)."""
    # Generate 100 days of data
    chart_list = []
    base_price = 50000

    for i in range(100):
        if i < 50:
            price = base_price + (50 - i) * 100
        else:
            price = base_price + (100 - i) * 100

        chart_list.append({
            "dt": f"202501{100-i:02d}",
            "open_pric": price + 100,
            "high_pric": price + 500,
            "low_pric": price - 500,
            "cur_prc": price,
            "trde_qty": 1000000 + i * 10000,
        })

    return {
        "stk_dt_pole_chart_qry": chart_list,
        "return_code": 0,
        "return_msg": "정상적으로 처리되었습니다",
    }


@pytest.fixture
def mock_kiwoom_client_extended(mock_chart_response_extended):
    """Create mock client with extended chart data."""
    from unittest.mock import Mock
    from stock_analyzer.client.kiwoom import KiwoomClient

    client = Mock(spec=KiwoomClient)
    client.get_daily_chart.return_value = ApiResponse(
        ok=True,
        data=mock_chart_response_extended,
    )
    return client


# ============================================================
# Trend Signal Tests
# ============================================================


class TestTrendCalc:
    """Tests for trend.calc function."""

    def test_empty_ticker(self, mock_kiwoom_client_extended):
        """Test with empty ticker."""
        result = trend.calc(mock_kiwoom_client_extended, "")
        assert result["ok"] is False
        assert result["error"]["code"] == "INVALID_ARG"

    def test_success(self, mock_kiwoom_client_extended):
        """Test successful calculation."""
        result = trend.calc(mock_kiwoom_client_extended, "005930", days=30)
        assert result["ok"] is True
        assert "data" in result

        data = result["data"]
        assert data["ticker"] == "005930"
        assert "dates" in data
        assert "ma_signal" in data
        assert "cmf" in data
        assert "fear_greed" in data
        assert "trend" in data
        assert "ma5" in data
        assert "ma20" in data
        assert "ma60" in data

    def test_output_lengths_match(self, mock_kiwoom_client_extended):
        """Test that all output arrays have same length."""
        result = trend.calc(mock_kiwoom_client_extended, "005930", days=30)
        assert result["ok"] is True

        data = result["data"]
        n = len(data["dates"])
        assert len(data["ma_signal"]) == n
        assert len(data["cmf"]) == n
        assert len(data["fear_greed"]) == n
        assert len(data["trend"]) == n
        assert len(data["ma5"]) == n
        assert len(data["ma20"]) == n
        assert len(data["ma60"]) == n

    def test_ma_signal_values(self, mock_kiwoom_client_extended):
        """Test MA signal values are valid."""
        result = trend.calc(mock_kiwoom_client_extended, "005930", days=30)
        assert result["ok"] is True

        for signal in result["data"]["ma_signal"]:
            assert signal in [-1, 0, 1]

    def test_cmf_range(self, mock_kiwoom_client_extended):
        """Test CMF values are in valid range."""
        result = trend.calc(mock_kiwoom_client_extended, "005930", days=30)
        assert result["ok"] is True

        for cmf_val in result["data"]["cmf"]:
            assert -1 <= cmf_val <= 1

    def test_fear_greed_range(self, mock_kiwoom_client_extended):
        """Test Fear/Greed values are in valid range."""
        result = trend.calc(mock_kiwoom_client_extended, "005930", days=30)
        assert result["ok"] is True

        for fg_val in result["data"]["fear_greed"]:
            assert 0 <= fg_val <= 100

    def test_trend_values(self, mock_kiwoom_client_extended):
        """Test trend values are valid."""
        result = trend.calc(mock_kiwoom_client_extended, "005930", days=30)
        assert result["ok"] is True

        for t in result["data"]["trend"]:
            assert t in ["bullish", "bearish", "neutral"]


class TestTrendCalcFromOhlcv:
    """Tests for trend.calc_from_ohlcv function."""

    def test_insufficient_data(self):
        """Test with insufficient data."""
        result = trend.calc_from_ohlcv(
            ticker="005930",
            dates=["20250101"] * 30,
            closes=[50000] * 30,
            highs=[51000] * 30,
            lows=[49000] * 30,
            volumes=[1000000] * 30,
        )
        assert result["ok"] is False
        assert result["error"]["code"] == "NO_DATA"

    def test_success(self, sample_ohlcv_data):
        """Test successful calculation."""
        result = trend.calc_from_ohlcv(
            ticker="005930",
            dates=sample_ohlcv_data["dates"],
            closes=sample_ohlcv_data["close"],
            highs=sample_ohlcv_data["high"],
            lows=sample_ohlcv_data["low"],
            volumes=sample_ohlcv_data["volume"],
        )
        assert result["ok"] is True


# ============================================================
# Elder Impulse Tests
# ============================================================


class TestElderCalc:
    """Tests for elder.calc function."""

    def test_empty_ticker(self, mock_kiwoom_client_extended):
        """Test with empty ticker."""
        result = elder.calc(mock_kiwoom_client_extended, "")
        assert result["ok"] is False
        assert result["error"]["code"] == "INVALID_ARG"

    def test_success(self, mock_kiwoom_client_extended):
        """Test successful calculation."""
        result = elder.calc(mock_kiwoom_client_extended, "005930", days=30)
        assert result["ok"] is True

        data = result["data"]
        assert data["ticker"] == "005930"
        assert "dates" in data
        assert "color" in data
        assert "ema13" in data
        assert "macd_line" in data
        assert "signal_line" in data
        assert "macd_hist" in data
        assert "ema13_dir" in data
        assert "hist_dir" in data

    def test_color_values(self, mock_kiwoom_client_extended):
        """Test color values are valid."""
        result = elder.calc(mock_kiwoom_client_extended, "005930", days=30)
        assert result["ok"] is True

        for color in result["data"]["color"]:
            assert color in ["green", "red", "blue"]

    def test_direction_values(self, mock_kiwoom_client_extended):
        """Test direction values are valid."""
        result = elder.calc(mock_kiwoom_client_extended, "005930", days=30)
        assert result["ok"] is True

        for dir_val in result["data"]["ema13_dir"]:
            assert dir_val in [-1, 0, 1]

        for dir_val in result["data"]["hist_dir"]:
            assert dir_val in [-1, 0, 1]

    def test_output_lengths_match(self, mock_kiwoom_client_extended):
        """Test that all output arrays have same length."""
        result = elder.calc(mock_kiwoom_client_extended, "005930", days=30)
        assert result["ok"] is True

        data = result["data"]
        n = len(data["dates"])
        assert len(data["color"]) == n
        assert len(data["ema13"]) == n
        assert len(data["macd_line"]) == n
        assert len(data["signal_line"]) == n
        assert len(data["macd_hist"]) == n
        assert len(data["ema13_dir"]) == n
        assert len(data["hist_dir"]) == n


class TestElderCalcFromOhlcv:
    """Tests for elder.calc_from_ohlcv function."""

    def test_insufficient_data(self):
        """Test with insufficient data."""
        result = elder.calc_from_ohlcv(
            ticker="005930",
            dates=["20250101"] * 20,
            closes=[50000] * 20,
        )
        assert result["ok"] is False
        assert result["error"]["code"] == "NO_DATA"

    def test_success(self, sample_ohlcv_data):
        """Test successful calculation."""
        result = elder.calc_from_ohlcv(
            ticker="005930",
            dates=sample_ohlcv_data["dates"],
            closes=sample_ohlcv_data["close"],
        )
        assert result["ok"] is True


# ============================================================
# DeMark TD Tests
# ============================================================


class TestDemarkCalc:
    """Tests for demark.calc function."""

    def test_empty_ticker(self, mock_kiwoom_client_extended):
        """Test with empty ticker."""
        result = demark.calc(mock_kiwoom_client_extended, "")
        assert result["ok"] is False
        assert result["error"]["code"] == "INVALID_ARG"

    def test_success(self, mock_kiwoom_client_extended):
        """Test successful calculation."""
        result = demark.calc(mock_kiwoom_client_extended, "005930", days=30)
        assert result["ok"] is True

        data = result["data"]
        assert data["ticker"] == "005930"
        assert "dates" in data
        assert "setup_count" in data
        assert "setup_type" in data
        assert "setup_complete" in data
        assert "perfected" in data

    def test_setup_count_range(self, mock_kiwoom_client_extended):
        """Test setup count values are in valid range."""
        result = demark.calc(mock_kiwoom_client_extended, "005930", days=30)
        assert result["ok"] is True

        for count in result["data"]["setup_count"]:
            assert 0 <= count <= 9

    def test_setup_type_values(self, mock_kiwoom_client_extended):
        """Test setup type values are valid."""
        result = demark.calc(mock_kiwoom_client_extended, "005930", days=30)
        assert result["ok"] is True

        for setup_type in result["data"]["setup_type"]:
            assert setup_type in ["none", "buy", "sell"]

    def test_boolean_flags(self, mock_kiwoom_client_extended):
        """Test boolean flag types."""
        result = demark.calc(mock_kiwoom_client_extended, "005930", days=30)
        assert result["ok"] is True

        for complete in result["data"]["setup_complete"]:
            assert isinstance(complete, bool)

        for perfect in result["data"]["perfected"]:
            assert isinstance(perfect, bool)

    def test_output_lengths_match(self, mock_kiwoom_client_extended):
        """Test that all output arrays have same length."""
        result = demark.calc(mock_kiwoom_client_extended, "005930", days=30)
        assert result["ok"] is True

        data = result["data"]
        n = len(data["dates"])
        assert len(data["setup_count"]) == n
        assert len(data["setup_type"]) == n
        assert len(data["setup_complete"]) == n
        assert len(data["perfected"]) == n


class TestDemarkCalcFromOhlcv:
    """Tests for demark.calc_from_ohlcv function."""

    def test_insufficient_data(self):
        """Test with insufficient data."""
        result = demark.calc_from_ohlcv(
            ticker="005930",
            dates=["20250101"] * 10,
            closes=[50000] * 10,
            highs=[51000] * 10,
            lows=[49000] * 10,
        )
        assert result["ok"] is False
        assert result["error"]["code"] == "NO_DATA"

    def test_success(self, sample_ohlcv_data):
        """Test successful calculation."""
        result = demark.calc_from_ohlcv(
            ticker="005930",
            dates=sample_ohlcv_data["dates"],
            closes=sample_ohlcv_data["close"],
            highs=sample_ohlcv_data["high"],
            lows=sample_ohlcv_data["low"],
        )
        assert result["ok"] is True


class TestDemarkGetActiveSetups:
    """Tests for demark.get_active_setups function."""

    def test_empty_data(self):
        """Test with empty data."""
        result = demark.get_active_setups([], [], [], [])
        assert result["current_count"] == 0
        assert result["current_type"] == "none"
        assert result["last_complete"] is None
        assert result["active_setups"] == []

    def test_with_data(self, sample_ohlcv_data):
        """Test with sample data."""
        calc_result = demark.calc_from_ohlcv(
            ticker="005930",
            dates=sample_ohlcv_data["dates"],
            closes=sample_ohlcv_data["close"],
            highs=sample_ohlcv_data["high"],
            lows=sample_ohlcv_data["low"],
        )

        if calc_result["ok"]:
            data = calc_result["data"]
            result = demark.get_active_setups(
                data["setup_count"],
                data["setup_type"],
                data["setup_complete"],
                data["dates"],
            )
            assert "current_count" in result
            assert "current_type" in result
            assert "last_complete" in result
            assert "active_setups" in result


# ============================================================
# API Error Tests
# ============================================================


class TestIndicatorApiErrors:
    """Tests for API error handling."""

    def test_trend_api_error(self, mock_kiwoom_client_extended):
        """Test trend with API error."""
        mock_kiwoom_client_extended.get_daily_chart.return_value = ApiResponse(
            ok=False,
            error={"code": "API_ERROR", "msg": "Server error"},
        )
        result = trend.calc(mock_kiwoom_client_extended, "005930")
        assert result["ok"] is False
        assert result["error"]["code"] == "API_ERROR"

    def test_elder_api_error(self, mock_kiwoom_client_extended):
        """Test elder with API error."""
        mock_kiwoom_client_extended.get_daily_chart.return_value = ApiResponse(
            ok=False,
            error={"code": "API_ERROR", "msg": "Server error"},
        )
        result = elder.calc(mock_kiwoom_client_extended, "005930")
        assert result["ok"] is False
        assert result["error"]["code"] == "API_ERROR"

    def test_demark_api_error(self, mock_kiwoom_client_extended):
        """Test demark with API error."""
        mock_kiwoom_client_extended.get_daily_chart.return_value = ApiResponse(
            ok=False,
            error={"code": "API_ERROR", "msg": "Server error"},
        )
        result = demark.calc(mock_kiwoom_client_extended, "005930")
        assert result["ok"] is False
        assert result["error"]["code"] == "API_ERROR"

    def test_trend_no_data(self, mock_kiwoom_client_extended):
        """Test trend with no chart data."""
        mock_kiwoom_client_extended.get_daily_chart.return_value = ApiResponse(
            ok=True,
            data={"chart_list": [], "return_code": 0},
        )
        result = trend.calc(mock_kiwoom_client_extended, "005930")
        assert result["ok"] is False
        assert result["error"]["code"] == "NO_DATA"


# ============================================================
# Integration Tests
# ============================================================


class TestIndicatorIntegration:
    """Integration tests for indicator module."""

    def test_import_from_package(self):
        """Test importing indicators from package."""
        from stock_analyzer.indicator import trend, elder, demark

        assert hasattr(trend, "calc")
        assert hasattr(trend, "calc_from_ohlcv")
        assert hasattr(elder, "calc")
        assert hasattr(elder, "calc_from_ohlcv")
        assert hasattr(demark, "calc")
        assert hasattr(demark, "calc_from_ohlcv")

    def test_all_indicators_same_ticker(self, mock_kiwoom_client_extended):
        """Test all indicators with same ticker."""
        ticker = "005930"

        trend_result = trend.calc(mock_kiwoom_client_extended, ticker, days=30)
        elder_result = elder.calc(mock_kiwoom_client_extended, ticker, days=30)
        demark_result = demark.calc(mock_kiwoom_client_extended, ticker, days=30)

        assert trend_result["ok"] is True
        assert elder_result["ok"] is True
        assert demark_result["ok"] is True

        # All should have same ticker
        assert trend_result["data"]["ticker"] == ticker
        assert elder_result["data"]["ticker"] == ticker
        assert demark_result["data"]["ticker"] == ticker
