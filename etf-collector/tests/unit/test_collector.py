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
    """Tests for EtfListCollector.

    Note: EtfListCollector no longer makes API calls. It loads ETF info
    directly from the predefined ACTIVE_ETF_CODES list because KIS API's
    stock search endpoint (CTPF1604R) does not work for ETFs.
    """

    def setup_method(self):
        """Set up test fixtures."""
        self.mock_auth = Mock()
        self.mock_auth.app_key = "test_app_key"
        self.mock_auth.app_secret = "test_app_secret"
        self.mock_auth.get_token.return_value = Mock(authorization="Bearer test")

        self.mock_limiter = Mock()
        self.mock_limiter.wait_if_needed = Mock()

    @patch("etf_collector.collector.etf_list.ACTIVE_ETF_CODES", [
        ("069500", "KODEX 200"),
        ("278530", "KODEX 200 액티브"),
        ("379800", "KODEX 미국S&P500TR"),
        ("123456", "TIGER Active AI"),
        ("234567", "KODEX 200 레버리지"),
    ])
    @patch("etf_collector.collector.etf_list.get_active_etf_codes")
    def test_get_all_etfs_success(self, mock_get_codes):
        """Test successful ETF list fetch from predefined codes."""
        mock_get_codes.return_value = ["069500", "278530", "379800", "123456", "234567"]

        collector = EtfListCollector(
            self.mock_auth, self.mock_limiter, "https://api.test.com"
        )
        result = collector.get_all_etfs()

        assert result["ok"] is True
        assert len(result["data"]) == 5

        # Check first ETF
        etf = result["data"][0]
        assert etf.etf_code == "069500"
        assert etf.etf_name == "KODEX 200"

    @patch("etf_collector.collector.etf_list.ACTIVE_ETF_CODES", [
        ("069500", "KODEX 200"),
        ("278530", "KODEX 200 액티브"),
        ("379800", "KODEX 미국S&P500TR"),
        ("123456", "TIGER Active AI"),
        ("234567", "KODEX 200 레버리지"),
    ])
    @patch("etf_collector.collector.etf_list.get_active_etf_codes")
    def test_get_all_etfs_determines_active_type(self, mock_get_codes):
        """Test ETF type is correctly determined based on name."""
        mock_get_codes.return_value = ["069500", "278530", "379800", "123456", "234567"]

        collector = EtfListCollector(
            self.mock_auth, self.mock_limiter, "https://api.test.com"
        )
        result = collector.get_all_etfs()

        etfs = result["data"]
        active_etfs = [e for e in etfs if e.etf_type == "Active"]
        passive_etfs = [e for e in etfs if e.etf_type == "Passive"]

        # "KODEX 200 액티브" and "TIGER Active AI" should be Active
        assert len(active_etfs) == 2
        assert len(passive_etfs) == 3

    def test_get_all_etfs_with_specific_codes(self):
        """Test fetching ETFs with specific codes."""
        collector = EtfListCollector(
            self.mock_auth, self.mock_limiter, "https://api.test.com"
        )
        # Use specific codes that may not be in ACTIVE_ETF_CODES
        result = collector.get_all_etfs(etf_codes=["TEST001", "TEST002"])

        assert result["ok"] is True
        assert len(result["data"]) == 2
        # Unknown codes get default names
        assert result["data"][0].etf_code == "TEST001"
        assert result["data"][0].etf_name == "ETF-TEST001"

    def test_get_all_etfs_with_callback(self):
        """Test progress callback is called."""
        collector = EtfListCollector(
            self.mock_auth, self.mock_limiter, "https://api.test.com"
        )

        callback_calls = []

        def callback(current, total, name):
            callback_calls.append((current, total, name))

        result = collector.get_all_etfs(etf_codes=["TEST001", "TEST002"], progress_callback=callback)

        assert result["ok"] is True
        assert len(callback_calls) == 2
        assert callback_calls[0] == (1, 2, "ETF-TEST001")
        assert callback_calls[1] == (2, 2, "ETF-TEST002")


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
