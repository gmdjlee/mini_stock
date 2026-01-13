"""Unit tests for chart module."""

import pytest

from stock_analyzer.chart import bar, candle, line


# =============================================================================
# Test Data Fixtures
# =============================================================================


@pytest.fixture
def sample_dates():
    """Sample date list."""
    return [
        "20250110",
        "20250109",
        "20250108",
        "20250107",
        "20250106",
        "20250103",
        "20250102",
    ]


@pytest.fixture
def sample_ohlcv(sample_dates):
    """Sample OHLCV data."""
    return {
        "ticker": "005930",
        "dates": sample_dates,
        "open": [55000, 54500, 54000, 54200, 53800, 53500, 53000],
        "high": [55500, 55000, 54500, 54500, 54200, 54000, 53500],
        "low": [54500, 54000, 53800, 53800, 53500, 53200, 52800],
        "close": [55200, 54800, 54200, 54000, 53900, 53800, 53200],
        "volume": [15000000, 12000000, 10000000, 11000000, 9000000, 8000000, 7500000],
    }


@pytest.fixture
def sample_trend_data(sample_dates):
    """Sample trend indicator data."""
    return {
        "ticker": "005930",
        "dates": sample_dates,
        "ma_signal": [1, 1, 0, 0, -1, -1, 0],
        "cmf": [0.15, 0.12, 0.08, 0.02, -0.05, -0.08, -0.03],
        "fear_greed": [68, 65, 58, 52, 45, 42, 48],
        "trend": ["bullish", "bullish", "neutral", "neutral", "bearish", "bearish", "neutral"],
        "ma5": [54640, 54280, 53980, 53800, 53650, None, None],
        "ma20": [54000, 53900, 53800, 53700, 53600, None, None],
        "ma60": [53500, 53450, 53400, 53350, 53300, None, None],
    }


@pytest.fixture
def sample_elder_data(sample_dates):
    """Sample Elder Impulse data."""
    return {
        "ticker": "005930",
        "dates": sample_dates,
        "color": ["green", "green", "blue", "blue", "red", "red", "blue"],
        "ema13": [54800, 54500, 54200, 54000, 53800, 53600, 53400],
        "macd_hist": [120, 80, 40, 10, -30, -60, -20],
    }


@pytest.fixture
def sample_demark_data(sample_dates):
    """Sample DeMark TD data."""
    return {
        "ticker": "005930",
        "dates": sample_dates,
        "setup_count": [9, 8, 7, 6, 5, 4, 3],
        "setup_type": ["buy", "buy", "buy", "buy", "buy", "buy", "buy"],
        "setup_complete": [True, False, False, False, False, False, False],
        "perfected": [True, False, False, False, False, False, False],
    }


@pytest.fixture
def sample_analysis_data(sample_dates):
    """Sample supply/demand analysis data."""
    return {
        "ticker": "005930",
        "name": "삼성전자",
        "dates": sample_dates,
        "mcap": [
            328000000000000,
            326000000000000,
            324000000000000,
            322000000000000,
            320000000000000,
            318000000000000,
            316000000000000,
        ],
        "for_5d": [
            1500000000000,
            1200000000000,
            800000000000,
            500000000000,
            -200000000000,
            -500000000000,
            -300000000000,
        ],
        "ins_5d": [
            -500000000000,
            -300000000000,
            200000000000,
            400000000000,
            600000000000,
            300000000000,
            100000000000,
        ],
    }


# =============================================================================
# Candle Chart Tests
# =============================================================================


class TestCandleChart:
    """Tests for candle.py."""

    def test_plot_basic(self, sample_ohlcv):
        """Test basic candlestick chart generation."""
        result = candle.plot(
            dates=sample_ohlcv["dates"],
            opens=sample_ohlcv["open"],
            highs=sample_ohlcv["high"],
            lows=sample_ohlcv["low"],
            closes=sample_ohlcv["close"],
            title="Test Candle Chart",
        )

        assert result["ok"] is True
        assert "image_bytes" in result["data"]
        assert isinstance(result["data"]["image_bytes"], bytes)
        assert len(result["data"]["image_bytes"]) > 0

    def test_plot_with_volume(self, sample_ohlcv):
        """Test candlestick chart with volume subplot."""
        result = candle.plot(
            dates=sample_ohlcv["dates"],
            opens=sample_ohlcv["open"],
            highs=sample_ohlcv["high"],
            lows=sample_ohlcv["low"],
            closes=sample_ohlcv["close"],
            volumes=sample_ohlcv["volume"],
            title="Test Candle with Volume",
        )

        assert result["ok"] is True
        assert "image_bytes" in result["data"]

    def test_plot_with_ma_lines(self, sample_ohlcv, sample_trend_data):
        """Test candlestick chart with MA overlay."""
        ma_lines = {
            "MA5": sample_trend_data["ma5"],
            "MA20": sample_trend_data["ma20"],
        }

        result = candle.plot(
            dates=sample_ohlcv["dates"],
            opens=sample_ohlcv["open"],
            highs=sample_ohlcv["high"],
            lows=sample_ohlcv["low"],
            closes=sample_ohlcv["close"],
            ma_lines=ma_lines,
        )

        assert result["ok"] is True

    def test_plot_with_elder_colors(self, sample_ohlcv, sample_elder_data):
        """Test candlestick chart with Elder Impulse colors."""
        result = candle.plot(
            dates=sample_ohlcv["dates"],
            opens=sample_ohlcv["open"],
            highs=sample_ohlcv["high"],
            lows=sample_ohlcv["low"],
            closes=sample_ohlcv["close"],
            elder_colors=sample_elder_data["color"],
        )

        assert result["ok"] is True

    def test_plot_from_ohlcv(self, sample_ohlcv):
        """Test plot_from_ohlcv helper function."""
        result = candle.plot_from_ohlcv(sample_ohlcv)

        assert result["ok"] is True
        assert "image_bytes" in result["data"]

    def test_plot_empty_data(self):
        """Test error handling for empty data."""
        result = candle.plot(
            dates=[],
            opens=[],
            highs=[],
            lows=[],
            closes=[],
        )

        assert result["ok"] is False
        assert result["error"]["code"] == "INVALID_ARG"

    def test_plot_insufficient_data(self):
        """Test error handling for insufficient data."""
        result = candle.plot(
            dates=["20250110", "20250109"],
            opens=[55000, 54000],
            highs=[55500, 54500],
            lows=[54500, 53500],
            closes=[55200, 54200],
        )

        assert result["ok"] is False
        assert result["error"]["code"] == "NO_DATA"

    def test_plot_from_ohlcv_invalid(self):
        """Test error handling for invalid OHLCV data."""
        result = candle.plot_from_ohlcv({})

        assert result["ok"] is False
        assert result["error"]["code"] == "INVALID_ARG"


# =============================================================================
# Line Chart Tests
# =============================================================================


class TestLineChart:
    """Tests for line.py."""

    def test_plot_basic(self, sample_dates):
        """Test basic line chart generation."""
        series = {
            "Price": [55000, 54500, 54000, 53500, 53000, 52500, 52000],
        }

        result = line.plot(
            dates=sample_dates,
            series=series,
            title="Test Line Chart",
        )

        assert result["ok"] is True
        assert "image_bytes" in result["data"]

    def test_plot_multiple_series(self, sample_dates):
        """Test line chart with multiple series."""
        series = {
            "MA5": [55000, 54800, 54600, 54400, 54200, None, None],
            "MA20": [54500, 54400, 54300, 54200, 54100, None, None],
        }

        result = line.plot(
            dates=sample_dates,
            series=series,
            title="Test Multi-line Chart",
        )

        assert result["ok"] is True

    def test_plot_with_hlines(self, sample_dates):
        """Test line chart with horizontal reference lines."""
        series = {"CMF": [0.15, 0.10, 0.05, 0.0, -0.05, -0.10, -0.05]}

        result = line.plot(
            dates=sample_dates,
            series=series,
            hlines=[
                {"y": 0.05, "color": "green", "linestyle": "--"},
                {"y": -0.05, "color": "red", "linestyle": "--"},
            ],
        )

        assert result["ok"] is True

    def test_plot_trend(self, sample_trend_data):
        """Test trend signal multi-panel chart."""
        result = line.plot_trend(sample_trend_data)

        assert result["ok"] is True
        assert "image_bytes" in result["data"]

    def test_plot_elder(self, sample_elder_data):
        """Test Elder Impulse chart."""
        result = line.plot_elder(sample_elder_data)

        assert result["ok"] is True
        assert "image_bytes" in result["data"]

    def test_plot_empty_series(self, sample_dates):
        """Test error handling for empty series."""
        result = line.plot(dates=sample_dates, series={})

        assert result["ok"] is False
        assert result["error"]["code"] == "INVALID_ARG"

    def test_plot_trend_invalid(self):
        """Test error handling for invalid trend data."""
        result = line.plot_trend({})

        assert result["ok"] is False
        assert result["error"]["code"] == "INVALID_ARG"

    def test_plot_elder_invalid(self):
        """Test error handling for invalid Elder data."""
        result = line.plot_elder({})

        assert result["ok"] is False
        assert result["error"]["code"] == "INVALID_ARG"


# =============================================================================
# Bar Chart Tests
# =============================================================================


class TestBarChart:
    """Tests for bar.py."""

    def test_plot_basic(self, sample_dates):
        """Test basic bar chart generation."""
        values = [100, 80, 60, 40, 20, 0, -20]

        result = bar.plot(
            dates=sample_dates,
            values=values,
            title="Test Bar Chart",
        )

        assert result["ok"] is True
        assert "image_bytes" in result["data"]

    def test_plot_color_by_sign(self, sample_dates):
        """Test bar chart with color by sign."""
        values = [100, 50, -30, 80, -60, 20, -10]

        result = bar.plot(
            dates=sample_dates,
            values=values,
            color_by_sign=True,
        )

        assert result["ok"] is True

    def test_plot_multi_grouped(self, sample_dates):
        """Test grouped bar chart."""
        series = {
            "Foreign": [100, 50, -30, 80, -60, 20, -10],
            "Institution": [-50, 30, 60, -20, 80, -40, 30],
        }

        result = bar.plot_multi(
            dates=sample_dates,
            series=series,
            stacked=False,
        )

        assert result["ok"] is True

    def test_plot_multi_stacked(self, sample_dates):
        """Test stacked bar chart."""
        series = {
            "Foreign": [100, 50, -30, 80, -60, 20, -10],
            "Institution": [-50, 30, 60, -20, 80, -40, 30],
        }

        result = bar.plot_multi(
            dates=sample_dates,
            series=series,
            stacked=True,
        )

        assert result["ok"] is True

    def test_plot_supply_demand(self, sample_analysis_data):
        """Test supply/demand analysis chart."""
        result = bar.plot_supply_demand(sample_analysis_data)

        assert result["ok"] is True
        assert "image_bytes" in result["data"]

    def test_plot_demark(self, sample_demark_data):
        """Test DeMark TD setup chart."""
        result = bar.plot_demark(sample_demark_data)

        assert result["ok"] is True
        assert "image_bytes" in result["data"]

    def test_plot_empty_data(self):
        """Test error handling for empty data."""
        result = bar.plot(dates=[], values=[])

        assert result["ok"] is False
        assert result["error"]["code"] == "INVALID_ARG"

    def test_plot_supply_demand_invalid(self):
        """Test error handling for invalid supply/demand data."""
        result = bar.plot_supply_demand({})

        assert result["ok"] is False
        assert result["error"]["code"] == "INVALID_ARG"

    def test_plot_demark_invalid(self):
        """Test error handling for invalid DeMark data."""
        result = bar.plot_demark({})

        assert result["ok"] is False
        assert result["error"]["code"] == "INVALID_ARG"


# =============================================================================
# Integration Tests
# =============================================================================


class TestChartIntegration:
    """Integration tests for chart module."""

    def test_candle_with_all_options(self, sample_ohlcv, sample_trend_data, sample_elder_data):
        """Test candlestick chart with all optional features."""
        ma_lines = {
            "MA5": sample_trend_data["ma5"],
            "MA20": sample_trend_data["ma20"],
            "MA60": sample_trend_data["ma60"],
        }

        result = candle.plot(
            dates=sample_ohlcv["dates"],
            opens=sample_ohlcv["open"],
            highs=sample_ohlcv["high"],
            lows=sample_ohlcv["low"],
            closes=sample_ohlcv["close"],
            volumes=sample_ohlcv["volume"],
            title="Full Featured Candle Chart",
            ma_lines=ma_lines,
            elder_colors=sample_elder_data["color"],
            figsize=(14, 10),
        )

        assert result["ok"] is True

    def test_all_indicator_charts(
        self, sample_trend_data, sample_elder_data, sample_demark_data
    ):
        """Test all indicator visualization charts."""
        # Trend chart
        trend_result = line.plot_trend(sample_trend_data)
        assert trend_result["ok"] is True

        # Elder chart
        elder_result = line.plot_elder(sample_elder_data)
        assert elder_result["ok"] is True

        # DeMark chart
        demark_result = bar.plot_demark(sample_demark_data)
        assert demark_result["ok"] is True

    def test_image_output_format(self, sample_ohlcv):
        """Test that output is valid PNG image."""
        result = candle.plot_from_ohlcv(sample_ohlcv)

        assert result["ok"] is True
        image_bytes = result["data"]["image_bytes"]

        # Check PNG magic bytes
        assert image_bytes[:8] == b"\x89PNG\r\n\x1a\n"
