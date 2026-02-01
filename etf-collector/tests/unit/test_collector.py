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


class TestConstituentStockCash:
    """Tests for ConstituentStock cash item detection."""

    def test_is_cash_with_krd_code(self):
        """Test is_cash property with KRD code."""
        stock = ConstituentStock(
            etf_code="069500",
            etf_name="KODEX 200",
            stock_code="KRD010010001",
            stock_name="원화현금",
            current_price=0,
            price_change=0,
            price_change_sign="3",
            price_change_rate=0.0,
            volume=0,
            trading_value=0,
            market_cap=0,
            weight=1.0,
            evaluation_amount=500000000,
        )
        assert stock.is_cash is True

    def test_is_cash_with_short_code(self):
        """Test is_cash property with short code."""
        stock = ConstituentStock(
            etf_code="069500",
            etf_name="KODEX 200",
            stock_code="010010",
            stock_name="원화예금",
            current_price=0,
            price_change=0,
            price_change_sign="3",
            price_change_rate=0.0,
            volume=0,
            trading_value=0,
            market_cap=0,
            weight=0.6,
            evaluation_amount=300000000,
        )
        assert stock.is_cash is True

    def test_is_cash_with_name_only(self):
        """Test is_cash property with cash name but no cash code."""
        stock = ConstituentStock(
            etf_code="069500",
            etf_name="KODEX 200",
            stock_code="CASH_069500_1234",
            stock_name="현금및예치금",
            current_price=0,
            price_change=0,
            price_change_sign="3",
            price_change_rate=0.0,
            volume=0,
            trading_value=0,
            market_cap=0,
            weight=0.4,
            evaluation_amount=200000000,
        )
        assert stock.is_cash is True

    def test_is_cash_regular_stock(self):
        """Test is_cash property returns False for regular stock."""
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
        assert stock.is_cash is False


class TestConstituentCollectorCash:
    """Tests for ConstituentCollector cash item handling."""

    def setup_method(self):
        """Set up test fixtures."""
        self.mock_auth = Mock()
        self.mock_auth.app_key = "test_app_key"
        self.mock_auth.app_secret = "test_app_secret"
        self.mock_auth.get_token.return_value = Mock(authorization="Bearer test")

        self.mock_limiter = Mock()
        self.mock_limiter.wait_if_needed = Mock()

    @patch("etf_collector.collector.constituent.requests.get")
    def test_get_constituents_includes_cash_by_krd_code(
        self, mock_get, mock_constituent_response_with_cash
    ):
        """Test cash items with KRD code are collected."""
        mock_resp = Mock()
        mock_resp.json.return_value = mock_constituent_response_with_cash
        mock_resp.raise_for_status = Mock()
        mock_get.return_value = mock_resp

        collector = ConstituentCollector(
            self.mock_auth, self.mock_limiter, "https://api.test.com"
        )
        result = collector.get_constituents("069500", "KODEX 200")

        assert result["ok"] is True
        constituents = result["data"].constituents

        # Find KRD cash item
        krd_cash = [c for c in constituents if c.stock_code == "KRD010010001"]
        assert len(krd_cash) == 1
        assert krd_cash[0].stock_name == "원화현금"
        assert krd_cash[0].is_cash is True

    @patch("etf_collector.collector.constituent.requests.get")
    def test_get_constituents_includes_cash_by_short_code(
        self, mock_get, mock_constituent_response_with_cash
    ):
        """Test cash items with short code (010010) are collected."""
        mock_resp = Mock()
        mock_resp.json.return_value = mock_constituent_response_with_cash
        mock_resp.raise_for_status = Mock()
        mock_get.return_value = mock_resp

        collector = ConstituentCollector(
            self.mock_auth, self.mock_limiter, "https://api.test.com"
        )
        result = collector.get_constituents("069500", "KODEX 200")

        constituents = result["data"].constituents

        # Find short code cash item
        short_cash = [c for c in constituents if c.stock_code == "010010"]
        assert len(short_cash) == 1
        assert short_cash[0].stock_name == "원화예금"
        assert short_cash[0].is_cash is True

    @patch("etf_collector.collector.constituent.requests.get")
    def test_get_constituents_cash_with_empty_code_gets_synthetic(
        self, mock_get, mock_constituent_response_with_cash
    ):
        """Test cash items with empty codes get synthetic codes."""
        mock_resp = Mock()
        mock_resp.json.return_value = mock_constituent_response_with_cash
        mock_resp.raise_for_status = Mock()
        mock_get.return_value = mock_resp

        collector = ConstituentCollector(
            self.mock_auth, self.mock_limiter, "https://api.test.com"
        )
        result = collector.get_constituents("069500", "KODEX 200")

        constituents = result["data"].constituents

        # Find item with synthetic code (originally had empty code)
        synthetic_cash = [c for c in constituents if c.stock_code.startswith("CASH_")]
        assert len(synthetic_cash) == 1
        assert synthetic_cash[0].stock_name == "현금및예치금"
        assert synthetic_cash[0].is_cash is True

    @patch("etf_collector.collector.constituent.requests.get")
    def test_get_constituents_cash_evaluation_amount(
        self, mock_get, mock_constituent_response_with_cash
    ):
        """Test cash item evaluation_amount is parsed correctly."""
        mock_resp = Mock()
        mock_resp.json.return_value = mock_constituent_response_with_cash
        mock_resp.raise_for_status = Mock()
        mock_get.return_value = mock_resp

        collector = ConstituentCollector(
            self.mock_auth, self.mock_limiter, "https://api.test.com"
        )
        result = collector.get_constituents("069500")

        constituents = result["data"].constituents
        krd_cash = next(c for c in constituents if c.stock_code == "KRD010010001")

        # 500000000 = 5억원
        assert krd_cash.evaluation_amount == 500000000

    @patch("etf_collector.collector.constituent.requests.get")
    def test_get_constituents_mixed_stocks_and_cash(
        self, mock_get, mock_constituent_response_with_cash
    ):
        """Test both regular stocks and cash items are collected."""
        mock_resp = Mock()
        mock_resp.json.return_value = mock_constituent_response_with_cash
        mock_resp.raise_for_status = Mock()
        mock_get.return_value = mock_resp

        collector = ConstituentCollector(
            self.mock_auth, self.mock_limiter, "https://api.test.com"
        )
        result = collector.get_constituents("069500", "KODEX 200")

        constituents = result["data"].constituents

        # Count regular stocks vs cash items
        cash_items = [c for c in constituents if c.is_cash]
        regular_stocks = [c for c in constituents if not c.is_cash]

        # 2 regular stocks (삼성전자, SK하이닉스)
        assert len(regular_stocks) == 2
        # 3 cash items (KRD, short code, synthetic)
        assert len(cash_items) == 3
        # Total 5 items
        assert len(constituents) == 5

    @patch("etf_collector.collector.constituent.requests.get")
    def test_get_constituents_cash_only_response(
        self, mock_get, mock_constituent_response_cash_only
    ):
        """Test response with only cash items (edge case)."""
        mock_resp = Mock()
        mock_resp.json.return_value = mock_constituent_response_cash_only
        mock_resp.raise_for_status = Mock()
        mock_get.return_value = mock_resp

        collector = ConstituentCollector(
            self.mock_auth, self.mock_limiter, "https://api.test.com"
        )
        result = collector.get_constituents("069500", "KODEX 200")

        assert result["ok"] is True
        constituents = result["data"].constituents

        # All items should be cash
        assert len(constituents) == 2
        assert all(c.is_cash for c in constituents)

        # Check total cash amount
        total_cash = sum(c.evaluation_amount for c in constituents)
        assert total_cash == 1000000000  # 10억원

    @patch("etf_collector.collector.constituent.requests.get")
    def test_get_constituents_regular_stock_not_detected_as_cash(
        self, mock_get, mock_constituent_response_with_cash
    ):
        """Test regular stocks are not falsely detected as cash."""
        mock_resp = Mock()
        mock_resp.json.return_value = mock_constituent_response_with_cash
        mock_resp.raise_for_status = Mock()
        mock_get.return_value = mock_resp

        collector = ConstituentCollector(
            self.mock_auth, self.mock_limiter, "https://api.test.com"
        )
        result = collector.get_constituents("069500", "KODEX 200")

        constituents = result["data"].constituents

        # Samsung should not be cash
        samsung = next(c for c in constituents if c.stock_code == "005930")
        assert samsung.is_cash is False
        assert samsung.stock_name == "삼성전자"

        # SK Hynix should not be cash
        sk = next(c for c in constituents if c.stock_code == "000660")
        assert sk.is_cash is False
        assert sk.stock_name == "SK하이닉스"
