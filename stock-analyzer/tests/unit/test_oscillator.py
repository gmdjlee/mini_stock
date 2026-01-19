"""Tests for oscillator indicator and chart modules."""

import pytest

from stock_analyzer.client.kiwoom import ApiResponse
from stock_analyzer.indicator import oscillator
from stock_analyzer.chart import oscillator as osc_chart


# ============================================================
# Test Fixtures
# ============================================================


@pytest.fixture
def mock_investor_trend_extended():
    """Generate extended investor trend data (50 days)."""
    trend_list = []
    # API returns market cap in 백만원 (million won), e.g., 380_000_000 = 380조원
    base_mcap = 380_000_000  # 380 trillion in 백만원 unit

    for i in range(50):
        # Simulate varying supply/demand
        foreign = 5_000_000_000 * (1 if i % 3 != 0 else -1)  # 5 billion
        institution = 3_000_000_000 * (1 if i % 4 != 0 else -1)  # 3 billion

        trend_list.append({
            "dt": f"202501{50-i:02d}",
            "mrkt_tot_amt": base_mcap + (i * 100),  # ~100억원 increments in 백만원
            "frgnr_invsr": foreign * (50 - i) // 50,
            "orgn": institution * (50 - i) // 50,
        })

    return {
        "stk_invsr_orgn": trend_list,
        "return_code": 0,
        "return_msg": "정상적으로 처리되었습니다",
    }


@pytest.fixture
def mock_stock_info_response():
    """Mock stock info response."""
    return {
        "stk_cd": "005930",
        "stk_nm": "삼성전자",
        "cur_prc": 55000,
        # ka10001 API returns market cap in 억원 (100 million won)
        "mac": 3_800_000,  # 380조원 in 억원 unit (380조 = 3,800,000억원)
        "flo_stk": 6900000,  # 유통주식수 (천주 단위) - 69억주
        "return_code": 0,
        "return_msg": "정상적으로 처리되었습니다",
    }


@pytest.fixture
def mock_daily_chart_response():
    """Mock daily chart response (50 days)."""
    chart_list = []
    base_price = 55000

    for i in range(50):
        price = base_price + (i * 100)  # Simulate price variation
        chart_list.append({
            "dt": f"202501{50-i:02d}",
            "open_pric": price - 500,
            "high_pric": price + 500,
            "low_pric": price - 1000,
            "cur_prc": price,  # close price
            "trde_qty": 10000000,
        })

    return {
        "stk_dt_pole_chart_qry": chart_list,
        "return_code": 0,
        "return_msg": "정상적으로 처리되었습니다",
    }


@pytest.fixture
def mock_kiwoom_client_oscillator(mock_stock_info_response, mock_investor_trend_extended, mock_daily_chart_response):
    """Create mock client with extended investor trend data."""
    from unittest.mock import Mock
    from stock_analyzer.client.kiwoom import KiwoomClient

    client = Mock(spec=KiwoomClient)
    client.get_stock_info.return_value = ApiResponse(
        ok=True,
        data=mock_stock_info_response,
    )
    client.get_investor_trend.return_value = ApiResponse(
        ok=True,
        data=mock_investor_trend_extended,
    )
    client.get_daily_chart.return_value = ApiResponse(
        ok=True,
        data=mock_daily_chart_response,
    )
    return client


@pytest.fixture
def sample_oscillator_data():
    """Sample oscillator calculation data."""
    n = 50
    dates = [f"202501{50-i:02d}" for i in range(n)]
    mcap = [380_000_000_000_000 + i * 100_000_000_000 for i in range(n)]
    for_5d = [5_000_000_000 * (1 if i % 3 != 0 else -1) * (n - i) // n for i in range(n)]
    ins_5d = [3_000_000_000 * (1 if i % 4 != 0 else -1) * (n - i) // n for i in range(n)]

    return {
        "ticker": "005930",
        "name": "삼성전자",
        "dates": dates,
        "mcap": mcap,
        "for_5d": for_5d,
        "ins_5d": ins_5d,
    }


# ============================================================
# Oscillator Calc Tests
# ============================================================


class TestOscillatorCalc:
    """Tests for oscillator.calc function."""

    def test_empty_ticker(self, mock_kiwoom_client_oscillator):
        """Test with empty ticker."""
        result = oscillator.calc(mock_kiwoom_client_oscillator, "")
        assert result["ok"] is False
        assert result["error"]["code"] == "INVALID_ARG"

    def test_success(self, mock_kiwoom_client_oscillator):
        """Test successful calculation."""
        result = oscillator.calc(mock_kiwoom_client_oscillator, "005930", days=50)
        assert result["ok"] is True
        assert "data" in result

        data = result["data"]
        assert data["ticker"] == "005930"
        assert "dates" in data
        assert "market_cap" in data
        assert "foreign_5d" in data  # New: 5-day cumulative foreign (억원)
        assert "institution_5d" in data  # New: 5-day cumulative institution (억원)
        assert "supply_ratio" in data
        assert "ema12" in data
        assert "ema26" in data
        assert "macd" in data
        assert "signal" in data
        assert "oscillator" in data

    def test_output_lengths_match(self, mock_kiwoom_client_oscillator):
        """Test that all output arrays have same length."""
        result = oscillator.calc(mock_kiwoom_client_oscillator, "005930", days=50)
        assert result["ok"] is True

        data = result["data"]
        n = len(data["dates"])
        assert len(data["market_cap"]) == n
        assert len(data["foreign_5d"]) == n  # New field
        assert len(data["institution_5d"]) == n  # New field
        assert len(data["supply_ratio"]) == n
        assert len(data["ema12"]) == n
        assert len(data["ema26"]) == n
        assert len(data["macd"]) == n
        assert len(data["signal"]) == n
        assert len(data["oscillator"]) == n

    def test_market_cap_in_trillion(self, mock_kiwoom_client_oscillator):
        """Test market cap is normalized to trillion."""
        result = oscillator.calc(mock_kiwoom_client_oscillator, "005930", days=50)
        assert result["ok"] is True

        # Market cap should be in hundreds (380 trillion -> 380)
        for mcap in result["data"]["market_cap"]:
            assert 100 < mcap < 1000  # Reasonable range for Korean large caps

    def test_insufficient_data(self, mock_kiwoom_client_oscillator):
        """Test with insufficient data (less than 30 days - 26 for EMA + 5 for rolling)."""
        from unittest.mock import Mock
        from stock_analyzer.client.kiwoom import KiwoomClient

        client = Mock(spec=KiwoomClient)
        client.get_stock_info.return_value = ApiResponse(
            ok=True,
            # ka10001 API returns market cap in 억원 (100 million won)
            data={"stk_nm": "삼성전자", "mac": 3_800_000, "flo_stk": 6900000},  # 380조원 in 억원
        )
        client.get_investor_trend.return_value = ApiResponse(
            ok=True,
            data={
                "stk_invsr_orgn": [
                    # ka10059 API returns market cap in 백만원 (million won)
                    {"dt": f"202501{25-i:02d}", "mrkt_tot_amt": 380_000_000, "frgnr_invsr": 0, "orgn": 0}
                    for i in range(25)  # 25 days is less than required 30
                ]
            },
        )
        # Mock daily chart for OHLCV data (analysis.py now fetches this)
        client.get_daily_chart.return_value = ApiResponse(
            ok=True,
            data={
                "stk_dt_pole_chart_qry": [
                    {"dt": f"202501{25-i:02d}", "cur_prc": 55000 + i * 100, "open_pric": 54500, "high_pric": 55500, "low_pric": 54000, "trde_qty": 10000000}
                    for i in range(25)
                ]
            },
        )

        result = oscillator.calc(client, "005930", days=25)
        assert result["ok"] is False
        assert result["error"]["code"] == "INSUFFICIENT_DATA"


class TestOscillatorCalcFromAnalysis:
    """Tests for oscillator.calc_from_analysis function."""

    def test_insufficient_data(self):
        """Test with insufficient data (less than 30 days)."""
        result = oscillator.calc_from_analysis(
            ticker="005930",
            name="삼성전자",
            dates=["20250101"] * 25,  # 25 days is less than required 30
            mcap=[380_000_000_000_000] * 25,
            foreign_daily=[5_000_000_000] * 25,
            institution_daily=[3_000_000_000] * 25,
        )
        assert result["ok"] is False
        assert result["error"]["code"] == "INSUFFICIENT_DATA"

    def test_success(self, sample_oscillator_data):
        """Test successful calculation."""
        result = oscillator.calc_from_analysis(
            ticker=sample_oscillator_data["ticker"],
            name=sample_oscillator_data["name"],
            dates=sample_oscillator_data["dates"],
            mcap=sample_oscillator_data["mcap"],
            foreign_daily=sample_oscillator_data["for_5d"],
            institution_daily=sample_oscillator_data["ins_5d"],
        )
        assert result["ok"] is True
        assert result["data"]["ticker"] == "005930"
        # Check new 5-day cumulative fields exist
        assert "foreign_5d" in result["data"]
        assert "institution_5d" in result["data"]

    def test_apply_rolling_false(self, sample_oscillator_data):
        """Test with apply_rolling=False (data already has 5-day cumulative)."""
        result = oscillator.calc_from_analysis(
            ticker=sample_oscillator_data["ticker"],
            name=sample_oscillator_data["name"],
            dates=sample_oscillator_data["dates"],
            mcap=sample_oscillator_data["mcap"],
            foreign_daily=sample_oscillator_data["for_5d"],
            institution_daily=sample_oscillator_data["ins_5d"],
            apply_rolling=False,  # Skip rolling sum calculation
        )
        assert result["ok"] is True
        # Values should be directly converted without rolling sum
        assert "foreign_5d" in result["data"]
        assert "institution_5d" in result["data"]


# ============================================================
# Analyze Signal Tests
# ============================================================


class TestAnalyzeSignal:
    """Tests for oscillator.analyze_signal function."""

    def test_invalid_input(self):
        """Test with invalid input."""
        result = oscillator.analyze_signal({"ok": False, "error": {"code": "TEST"}})
        assert result["ok"] is False

    def test_insufficient_data(self):
        """Test with insufficient oscillator data."""
        result = oscillator.analyze_signal({
            "ok": True,
            "data": {
                "oscillator": [0.001],
                "macd": [0.001],
                "signal": [0.0005],
            }
        })
        assert result["ok"] is False
        assert result["error"]["code"] == "INSUFFICIENT_DATA"

    def test_success(self, sample_oscillator_data):
        """Test successful signal analysis."""
        osc_result = oscillator.calc_from_analysis(
            ticker=sample_oscillator_data["ticker"],
            name=sample_oscillator_data["name"],
            dates=sample_oscillator_data["dates"],
            mcap=sample_oscillator_data["mcap"],
            foreign_daily=sample_oscillator_data["for_5d"],
            institution_daily=sample_oscillator_data["ins_5d"],
        )

        result = oscillator.analyze_signal(osc_result)
        assert result["ok"] is True

        data = result["data"]
        assert "total_score" in data
        assert "signal_type" in data
        assert "oscillator_score" in data
        assert "cross_score" in data
        assert "trend_score" in data
        assert "description" in data

    def test_score_range(self, sample_oscillator_data):
        """Test score is in valid range."""
        osc_result = oscillator.calc_from_analysis(
            ticker=sample_oscillator_data["ticker"],
            name=sample_oscillator_data["name"],
            dates=sample_oscillator_data["dates"],
            mcap=sample_oscillator_data["mcap"],
            foreign_daily=sample_oscillator_data["for_5d"],
            institution_daily=sample_oscillator_data["ins_5d"],
        )

        result = oscillator.analyze_signal(osc_result)
        assert result["ok"] is True
        assert -100 <= result["data"]["total_score"] <= 100

    def test_signal_type_values(self, sample_oscillator_data):
        """Test signal type is valid."""
        osc_result = oscillator.calc_from_analysis(
            ticker=sample_oscillator_data["ticker"],
            name=sample_oscillator_data["name"],
            dates=sample_oscillator_data["dates"],
            mcap=sample_oscillator_data["mcap"],
            foreign_daily=sample_oscillator_data["for_5d"],
            institution_daily=sample_oscillator_data["ins_5d"],
        )

        result = oscillator.analyze_signal(osc_result)
        assert result["ok"] is True
        assert result["data"]["signal_type"] in [
            "STRONG_BUY", "BUY", "NEUTRAL", "SELL", "STRONG_SELL"
        ]


# ============================================================
# Helper Function Tests
# ============================================================


class TestRollingSumCalculation:
    """Tests for rolling sum calculation helper."""

    def test_empty_values(self):
        """Test with empty values."""
        result = oscillator._calc_rolling_sum([], 5)
        assert result == []

    def test_single_value(self):
        """Test with single value."""
        result = oscillator._calc_rolling_sum([100.0], 5)
        assert result == [100.0]

    def test_rolling_sum_calculation(self):
        """Test rolling sum calculation accuracy."""
        values = [10.0, 20.0, 30.0, 40.0, 50.0, 60.0, 70.0]
        result = oscillator._calc_rolling_sum(values, 5)

        # Result should be list of same length
        assert len(result) == len(values)

        # Check rolling sums
        assert result[0] == 10.0  # Only 1 value
        assert result[1] == 30.0  # 10 + 20
        assert result[2] == 60.0  # 10 + 20 + 30
        assert result[3] == 100.0  # 10 + 20 + 30 + 40
        assert result[4] == 150.0  # 10 + 20 + 30 + 40 + 50 (first full window)
        assert result[5] == 200.0  # 20 + 30 + 40 + 50 + 60 (window slides)
        assert result[6] == 250.0  # 30 + 40 + 50 + 60 + 70

    def test_window_larger_than_data(self):
        """Test with window larger than data."""
        values = [10.0, 20.0, 30.0]
        result = oscillator._calc_rolling_sum(values, 5)

        # Should sum all available values
        assert len(result) == 3
        assert result[0] == 10.0
        assert result[1] == 30.0
        assert result[2] == 60.0


class TestEmaCalculation:
    """Tests for EMA calculation helper."""

    def test_empty_values(self):
        """Test with empty values."""
        result = oscillator._calc_ema([], 12)
        assert result == []

    def test_single_value(self):
        """Test with single value."""
        result = oscillator._calc_ema([1.0], 12)
        assert result == [1.0]

    def test_ema_calculation(self):
        """Test EMA calculation accuracy."""
        values = [1.0, 2.0, 3.0, 4.0, 5.0]
        result = oscillator._calc_ema(values, 3)

        # EMA should be list of same length
        assert len(result) == len(values)

        # First value should be same as input
        assert result[0] == values[0]

        # Subsequent values should be smoothed
        alpha = 2 / (3 + 1)
        expected_1 = alpha * 2.0 + (1 - alpha) * 1.0
        assert abs(result[1] - expected_1) < 0.0001


class TestTrendHelpers:
    """Tests for trend helper functions."""

    def test_is_increasing(self):
        """Test increasing detection."""
        assert oscillator._is_increasing([1, 2, 3]) is True
        assert oscillator._is_increasing([1, 2, 2]) is False
        assert oscillator._is_increasing([3, 2, 1]) is False

    def test_is_decreasing(self):
        """Test decreasing detection."""
        assert oscillator._is_decreasing([3, 2, 1]) is True
        assert oscillator._is_decreasing([1, 2, 3]) is False
        assert oscillator._is_decreasing([2, 2, 1]) is False


# ============================================================
# Oscillator Chart Tests
# ============================================================


class TestOscillatorChartPlot:
    """Tests for oscillator chart plot function."""

    def test_invalid_data(self):
        """Test with invalid data."""
        result = osc_chart.plot({})
        assert result["ok"] is False
        assert result["error"]["code"] == "INVALID_ARG"

    def test_insufficient_data(self):
        """Test with insufficient data."""
        result = osc_chart.plot({"ticker": "005930", "dates": ["20250101"]})
        assert result["ok"] is False
        assert result["error"]["code"] == "NO_DATA"

    def test_success(self, sample_oscillator_data):
        """Test successful chart generation."""
        osc_result = oscillator.calc_from_analysis(
            ticker=sample_oscillator_data["ticker"],
            name=sample_oscillator_data["name"],
            dates=sample_oscillator_data["dates"],
            mcap=sample_oscillator_data["mcap"],
            foreign_daily=sample_oscillator_data["for_5d"],
            institution_daily=sample_oscillator_data["ins_5d"],
        )

        result = osc_chart.plot(osc_result["data"])
        assert result["ok"] is True
        assert "image_bytes" in result["data"]
        assert result["data"]["image_bytes"] is not None
        assert len(result["data"]["image_bytes"]) > 0

    def test_with_title(self, sample_oscillator_data):
        """Test chart with custom title."""
        osc_result = oscillator.calc_from_analysis(
            ticker=sample_oscillator_data["ticker"],
            name=sample_oscillator_data["name"],
            dates=sample_oscillator_data["dates"],
            mcap=sample_oscillator_data["mcap"],
            foreign_daily=sample_oscillator_data["for_5d"],
            institution_daily=sample_oscillator_data["ins_5d"],
        )

        result = osc_chart.plot(osc_result["data"], title="Custom Title")
        assert result["ok"] is True


class TestOscillatorChartPlotWithSignal:
    """Tests for oscillator chart with signal panel."""

    def test_invalid_osc_data(self):
        """Test with invalid oscillator data."""
        result = osc_chart.plot_with_signal({}, {})
        assert result["ok"] is False
        assert result["error"]["code"] == "INVALID_ARG"

    def test_fallback_without_signal(self, sample_oscillator_data):
        """Test fallback to basic plot when signal is invalid."""
        osc_result = oscillator.calc_from_analysis(
            ticker=sample_oscillator_data["ticker"],
            name=sample_oscillator_data["name"],
            dates=sample_oscillator_data["dates"],
            mcap=sample_oscillator_data["mcap"],
            foreign_daily=sample_oscillator_data["for_5d"],
            institution_daily=sample_oscillator_data["ins_5d"],
        )

        # Invalid signal data
        result = osc_chart.plot_with_signal(osc_result["data"], {"ok": False})
        assert result["ok"] is True

    def test_success(self, sample_oscillator_data):
        """Test successful chart with signal."""
        osc_result = oscillator.calc_from_analysis(
            ticker=sample_oscillator_data["ticker"],
            name=sample_oscillator_data["name"],
            dates=sample_oscillator_data["dates"],
            mcap=sample_oscillator_data["mcap"],
            foreign_daily=sample_oscillator_data["for_5d"],
            institution_daily=sample_oscillator_data["ins_5d"],
        )

        signal_result = oscillator.analyze_signal(osc_result)

        result = osc_chart.plot_with_signal(osc_result["data"], signal_result)
        assert result["ok"] is True
        assert "image_bytes" in result["data"]
        assert len(result["data"]["image_bytes"]) > 0


# ============================================================
# API Error Tests
# ============================================================


class TestOscillatorApiErrors:
    """Tests for API error handling."""

    def test_stock_info_error(self):
        """Test with stock info API error."""
        from unittest.mock import Mock
        from stock_analyzer.client.kiwoom import KiwoomClient

        client = Mock(spec=KiwoomClient)
        client.get_stock_info.return_value = ApiResponse(
            ok=False,
            error={"code": "TICKER_NOT_FOUND", "msg": "종목을 찾을 수 없습니다"},
        )

        result = oscillator.calc(client, "999999")
        assert result["ok"] is False
        assert result["error"]["code"] == "TICKER_NOT_FOUND"

    def test_investor_trend_error(self):
        """Test with investor trend API error."""
        from unittest.mock import Mock
        from stock_analyzer.client.kiwoom import KiwoomClient

        client = Mock(spec=KiwoomClient)
        client.get_stock_info.return_value = ApiResponse(
            ok=True,
            # ka10001 API returns market cap in 억원 (100 million won)
            data={"stk_nm": "삼성전자", "mac": 3_800_000},  # 380조원 in 억원
        )
        client.get_investor_trend.return_value = ApiResponse(
            ok=False,
            error={"code": "API_ERROR", "msg": "Server error"},
        )

        result = oscillator.calc(client, "005930")
        assert result["ok"] is False
        assert result["error"]["code"] == "API_ERROR"


# ============================================================
# Integration Tests
# ============================================================


class TestOscillatorIntegration:
    """Integration tests for oscillator module."""

    def test_import_from_package(self):
        """Test importing oscillator from package."""
        from stock_analyzer.indicator import oscillator
        from stock_analyzer.chart import oscillator as osc_chart

        assert hasattr(oscillator, "calc")
        assert hasattr(oscillator, "calc_from_analysis")
        assert hasattr(oscillator, "analyze_signal")
        assert hasattr(osc_chart, "plot")
        assert hasattr(osc_chart, "plot_with_signal")

    def test_full_workflow(self, sample_oscillator_data):
        """Test complete oscillator workflow."""
        # 1. Calculate oscillator
        osc_result = oscillator.calc_from_analysis(
            ticker=sample_oscillator_data["ticker"],
            name=sample_oscillator_data["name"],
            dates=sample_oscillator_data["dates"],
            mcap=sample_oscillator_data["mcap"],
            foreign_daily=sample_oscillator_data["for_5d"],
            institution_daily=sample_oscillator_data["ins_5d"],
        )
        assert osc_result["ok"] is True
        # Verify 5-day rolling sum fields
        assert "foreign_5d" in osc_result["data"]
        assert "institution_5d" in osc_result["data"]

        # 2. Analyze signal
        signal_result = oscillator.analyze_signal(osc_result)
        assert signal_result["ok"] is True

        # 3. Generate chart
        chart_result = osc_chart.plot_with_signal(osc_result["data"], signal_result)
        assert chart_result["ok"] is True
        assert len(chart_result["data"]["image_bytes"]) > 0
