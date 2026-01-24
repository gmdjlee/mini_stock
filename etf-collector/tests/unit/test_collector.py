"""Tests for ETF collector modules."""

from unittest.mock import Mock, patch
import pytest

from etf_collector.collector.etf_list import EtfListCollector, EtfInfo
from etf_collector.collector.constituent import (
    ConstituentCollector,
    ConstituentStock,
    EtfConstituentSummary,
)


class TestEtfInfo:
    """Tests for EtfInfo dataclass."""

    def test_create_etf_info(self):
        """Test creating EtfInfo."""
        etf = EtfInfo(
            etf_code="069500",
            etf_name="KODEX 200",
            etf_type="Passive",
        )
        assert etf.etf_code == "069500"
        assert etf.etf_name == "KODEX 200"
        assert etf.is_active() is False

    def test_is_active_true(self):
        """Test is_active for active ETF."""
        etf = EtfInfo(
            etf_code="278530",
            etf_name="KODEX 200 액티브",
            etf_type="Active",
        )
        assert etf.is_active() is True

    def test_collected_at_auto_set(self):
        """Test collected_at is auto-set."""
        etf = EtfInfo(
            etf_code="069500",
            etf_name="KODEX 200",
            etf_type="Passive",
        )
        assert etf.collected_at != ""


class TestEtfListCollector:
    """Tests for EtfListCollector."""

    def setup_method(self):
        """Set up test fixtures."""
        self.mock_auth = Mock()
        self.mock_auth.app_key = "test_app_key"
        self.mock_auth.app_secret = "test_app_secret"
        self.mock_auth.get_token.return_value = Mock(authorization="Bearer test")

        self.mock_limiter = Mock()
        self.mock_limiter.wait_if_needed = Mock()

        # Test ETF codes matching the mock fixture
        self.test_etf_codes = ["069500", "278530", "379800", "123456", "234567"]

    @patch("etf_collector.collector.etf_list.requests.get")
    def test_get_all_etfs_success(self, mock_get, mock_etf_list_response):
        """Test successful ETF list fetch with specific codes."""
        mock_resp = Mock()
        mock_resp.json.return_value = mock_etf_list_response
        mock_resp.raise_for_status = Mock()
        mock_get.return_value = mock_resp

        collector = EtfListCollector(
            self.mock_auth, self.mock_limiter, "https://api.test.com"
        )
        # Pass specific ETF codes to test
        result = collector.get_all_etfs(etf_codes=self.test_etf_codes)

        assert result["ok"] is True
        assert len(result["data"]) == 5

        # Check first ETF
        etf = result["data"][0]
        assert etf.etf_code == "069500"
        assert etf.etf_name == "KODEX 200"

    @patch("etf_collector.collector.etf_list.requests.get")
    def test_get_all_etfs_determines_active_type(self, mock_get):
        """Test ETF type is correctly determined based on name."""
        # Create individual responses for each ETF code
        responses = [
            {"output": [{"pdno": "069500", "prdt_abrv_name": "KODEX 200", "lstg_dt": ""}],
             "rt_cd": "0", "msg_cd": "0000", "msg1": "정상처리"},
            {"output": [{"pdno": "278530", "prdt_abrv_name": "KODEX 200 액티브", "lstg_dt": ""}],
             "rt_cd": "0", "msg_cd": "0000", "msg1": "정상처리"},
            {"output": [{"pdno": "379800", "prdt_abrv_name": "KODEX 미국S&P500TR", "lstg_dt": ""}],
             "rt_cd": "0", "msg_cd": "0000", "msg1": "정상처리"},
            {"output": [{"pdno": "123456", "prdt_abrv_name": "TIGER Active AI", "lstg_dt": ""}],
             "rt_cd": "0", "msg_cd": "0000", "msg1": "정상처리"},
            {"output": [{"pdno": "234567", "prdt_abrv_name": "KODEX 200 레버리지", "lstg_dt": ""}],
             "rt_cd": "0", "msg_cd": "0000", "msg1": "정상처리"},
        ]

        def create_response(idx):
            resp = Mock()
            resp.json.return_value = responses[idx]
            resp.raise_for_status = Mock()
            return resp

        mock_get.side_effect = [create_response(i) for i in range(5)]

        collector = EtfListCollector(
            self.mock_auth, self.mock_limiter, "https://api.test.com"
        )
        # Pass specific ETF codes to test
        result = collector.get_all_etfs(etf_codes=self.test_etf_codes)

        etfs = result["data"]
        active_etfs = [e for e in etfs if e.etf_type == "Active"]
        passive_etfs = [e for e in etfs if e.etf_type == "Passive"]

        assert len(active_etfs) == 2  # "액티브" and "Active AI"
        assert len(passive_etfs) == 3

    @patch("etf_collector.collector.etf_list.requests.get")
    def test_get_all_etfs_api_error(self, mock_get, mock_error_response):
        """Test API error handling returns ALL_FAILED when all codes fail."""
        mock_resp = Mock()
        mock_resp.json.return_value = mock_error_response
        mock_resp.raise_for_status = Mock()
        mock_get.return_value = mock_resp

        collector = EtfListCollector(
            self.mock_auth, self.mock_limiter, "https://api.test.com"
        )
        # Pass specific ETF codes to test
        result = collector.get_all_etfs(etf_codes=self.test_etf_codes)

        assert result["ok"] is False
        assert "error" in result
        assert result["error"]["code"] == "ALL_FAILED"

    @patch("etf_collector.collector.etf_list.requests.get")
    def test_get_all_etfs_timeout(self, mock_get):
        """Test timeout handling returns ALL_FAILED when all codes timeout."""
        import requests

        mock_get.side_effect = requests.exceptions.Timeout()

        collector = EtfListCollector(
            self.mock_auth, self.mock_limiter, "https://api.test.com"
        )
        # Pass specific ETF codes to test
        result = collector.get_all_etfs(etf_codes=self.test_etf_codes)

        assert result["ok"] is False
        assert result["error"]["code"] == "ALL_FAILED"

    @patch("etf_collector.collector.etf_list.requests.get")
    def test_get_etf_by_code_timeout(self, mock_get):
        """Test single ETF fetch timeout returns TIMEOUT error."""
        import requests

        mock_get.side_effect = requests.exceptions.Timeout()

        collector = EtfListCollector(
            self.mock_auth, self.mock_limiter, "https://api.test.com"
        )
        result = collector.get_etf_by_code("069500")

        assert result["ok"] is False
        assert result["error"]["code"] == "TIMEOUT"


class TestConstituentStock:
    """Tests for ConstituentStock dataclass."""

    def test_create_constituent_stock(self):
        """Test creating ConstituentStock."""
        stock = ConstituentStock(
            etf_code="069500",
            etf_name="KODEX 200",
            stock_code="005930",
            stock_name="삼성전자",
            current_price=71500,
            price_change=500,
            price_change_sign="2",
            price_change_rate=0.70,
            volume=15000000,
            trading_value=1072500000000,
            market_cap=427000000000000,
            weight=31.25,
            evaluation_amount=15625000000,
        )

        assert stock.stock_code == "005930"
        assert stock.stock_name == "삼성전자"
        assert stock.weight == 31.25


class TestConstituentCollector:
    """Tests for ConstituentCollector."""

    def setup_method(self):
        """Set up test fixtures."""
        self.mock_auth = Mock()
        self.mock_auth.app_key = "test_app_key"
        self.mock_auth.app_secret = "test_app_secret"
        self.mock_auth.get_token.return_value = Mock(authorization="Bearer test")

        self.mock_limiter = Mock()
        self.mock_limiter.wait_if_needed = Mock()

    @patch("etf_collector.collector.constituent.requests.get")
    def test_get_constituents_success(self, mock_get, mock_constituent_response):
        """Test successful constituent fetch."""
        mock_resp = Mock()
        mock_resp.json.return_value = mock_constituent_response
        mock_resp.raise_for_status = Mock()
        mock_get.return_value = mock_resp

        collector = ConstituentCollector(
            self.mock_auth, self.mock_limiter, "https://api.test.com"
        )
        result = collector.get_constituents("069500", "KODEX 200")

        assert result["ok"] is True

        summary = result["data"]
        assert isinstance(summary, EtfConstituentSummary)
        assert summary.etf_code == "069500"
        assert len(summary.constituents) == 3
        assert summary.nav == 35248.50

    @patch("etf_collector.collector.constituent.requests.get")
    def test_get_constituents_parses_output1(self, mock_get, mock_constituent_response):
        """Test output1 parsing."""
        mock_resp = Mock()
        mock_resp.json.return_value = mock_constituent_response
        mock_resp.raise_for_status = Mock()
        mock_get.return_value = mock_resp

        collector = ConstituentCollector(
            self.mock_auth, self.mock_limiter, "https://api.test.com"
        )
        result = collector.get_constituents("069500")
        summary = result["data"]

        assert summary.current_price == 35250
        assert summary.price_change == 500
        assert summary.total_assets == 58234500000000
        assert summary.constituent_count == 200

    @patch("etf_collector.collector.constituent.requests.get")
    def test_get_constituents_parses_output2(self, mock_get, mock_constituent_response):
        """Test output2 parsing."""
        mock_resp = Mock()
        mock_resp.json.return_value = mock_constituent_response
        mock_resp.raise_for_status = Mock()
        mock_get.return_value = mock_resp

        collector = ConstituentCollector(
            self.mock_auth, self.mock_limiter, "https://api.test.com"
        )
        result = collector.get_constituents("069500")
        constituents = result["data"].constituents

        # Check first constituent
        samsung = constituents[0]
        assert samsung.stock_code == "005930"
        assert samsung.stock_name == "삼성전자"
        assert samsung.current_price == 71500
        assert samsung.weight == 31.25

    @patch("etf_collector.collector.constituent.requests.get")
    def test_get_all_constituents(self, mock_get, mock_constituent_response, sample_etf_infos):
        """Test fetching constituents for multiple ETFs."""
        mock_resp = Mock()
        mock_resp.json.return_value = mock_constituent_response
        mock_resp.raise_for_status = Mock()
        mock_get.return_value = mock_resp

        collector = ConstituentCollector(
            self.mock_auth, self.mock_limiter, "https://api.test.com"
        )

        # Test with first two ETFs
        etfs = sample_etf_infos[:2]
        result = collector.get_all_constituents(etfs)

        assert result["ok"] is True
        assert len(result["data"]) == 2

    @patch("etf_collector.collector.constituent.requests.get")
    def test_get_all_constituents_with_callback(self, mock_get, mock_constituent_response, sample_etf_infos):
        """Test progress callback is called."""
        mock_resp = Mock()
        mock_resp.json.return_value = mock_constituent_response
        mock_resp.raise_for_status = Mock()
        mock_get.return_value = mock_resp

        collector = ConstituentCollector(
            self.mock_auth, self.mock_limiter, "https://api.test.com"
        )

        callback_calls = []

        def callback(current, total, name):
            callback_calls.append((current, total, name))

        etfs = sample_etf_infos[:2]
        collector.get_all_constituents(etfs, progress_callback=callback)

        assert len(callback_calls) == 2
        assert callback_calls[0] == (1, 2, "KODEX 200")
        assert callback_calls[1] == (2, 2, "KODEX 200 액티브")
