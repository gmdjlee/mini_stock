"""Tests for analysis module."""

import pytest

from stock_analyzer.client.kiwoom import ApiResponse
from stock_analyzer.stock import analysis


class TestAnalyze:
    """Tests for analyze function."""

    def test_analyze_empty_ticker(self, mock_kiwoom_client):
        """Test analyze with empty ticker."""
        result = analysis.analyze(mock_kiwoom_client, "")
        assert result["ok"] is False
        assert result["error"]["code"] == "INVALID_ARG"

    def test_analyze_success(self, mock_kiwoom_client):
        """Test analyze success."""
        result = analysis.analyze(mock_kiwoom_client, "005930")
        assert result["ok"] is True
        assert result["data"]["ticker"] == "005930"
        assert result["data"]["name"] == "삼성전자"
        assert len(result["data"]["dates"]) == 2
        assert len(result["data"]["mcap"]) == 2
        assert len(result["data"]["for_5d"]) == 2
        assert len(result["data"]["ins_5d"]) == 2

    def test_analyze_date_format(self, mock_kiwoom_client):
        """Test analyze date format conversion."""
        result = analysis.analyze(mock_kiwoom_client, "005930")
        assert result["ok"] is True
        # Date should be converted from YYYYMMDD to YYYY-MM-DD
        assert result["data"]["dates"][0] == "2025-01-10"

    def test_analyze_no_trend_data(self, mock_kiwoom_client):
        """Test analyze with no trend data."""
        mock_kiwoom_client.get_investor_trend.return_value = ApiResponse(
            ok=True,
            data={"trend_list": [], "return_code": 0},
        )
        result = analysis.analyze(mock_kiwoom_client, "005930")
        assert result["ok"] is False
        assert result["error"]["code"] == "NO_DATA"


class TestGetForeignTrend:
    """Tests for get_foreign_trend function."""

    def test_empty_ticker(self, mock_kiwoom_client):
        """Test with empty ticker."""
        result = analysis.get_foreign_trend(mock_kiwoom_client, "")
        assert result["ok"] is False
        assert result["error"]["code"] == "INVALID_ARG"

    def test_success(self, mock_kiwoom_client):
        """Test successful call."""
        mock_kiwoom_client.get_foreign_trend.return_value = ApiResponse(
            ok=True,
            data={
                "frgn_net": 1500000000,
                "frgn_hold_qty": 3000000000,
                "frgn_hold_rt": 52.5,
                "return_code": 0,
            },
        )
        result = analysis.get_foreign_trend(mock_kiwoom_client, "005930")
        assert result["ok"] is True
        assert result["data"]["net_buy"] == 1500000000
        assert result["data"]["holding_ratio"] == 52.5


class TestGetInstitutionTrend:
    """Tests for get_institution_trend function."""

    def test_empty_ticker(self, mock_kiwoom_client):
        """Test with empty ticker."""
        result = analysis.get_institution_trend(mock_kiwoom_client, "")
        assert result["ok"] is False
        assert result["error"]["code"] == "INVALID_ARG"

    def test_success(self, mock_kiwoom_client):
        """Test successful call."""
        mock_kiwoom_client.get_institution_trend.return_value = ApiResponse(
            ok=True,
            data={
                "istt_net": -500000000,
                "fin_inv_net": 100000000,
                "insur_net": -50000000,
                "inv_trust_net": -100000000,
                "return_code": 0,
            },
        )
        result = analysis.get_institution_trend(mock_kiwoom_client, "005930")
        assert result["ok"] is True
        assert result["data"]["net_buy"] == -500000000
        assert result["data"]["finance"] == 100000000


class TestGetInvestorSummary:
    """Tests for get_investor_summary function."""

    def test_empty_ticker(self, mock_kiwoom_client):
        """Test with empty ticker."""
        result = analysis.get_investor_summary(mock_kiwoom_client, "")
        assert result["ok"] is False
        assert result["error"]["code"] == "INVALID_ARG"

    def test_success(self, mock_kiwoom_client):
        """Test successful call."""
        mock_kiwoom_client.get_investor_summary.return_value = ApiResponse(
            ok=True,
            data={
                "frgn_net_sum": 7500000000,
                "istt_net_sum": -2500000000,
                "prsn_net_sum": -5000000000,
                "return_code": 0,
            },
        )
        result = analysis.get_investor_summary(mock_kiwoom_client, "005930")
        assert result["ok"] is True
        assert result["data"]["foreign_net"] == 7500000000
        assert result["data"]["institution_net"] == -2500000000
        assert result["data"]["individual_net"] == -5000000000
