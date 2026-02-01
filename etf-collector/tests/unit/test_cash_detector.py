"""Tests for cash detection utility module."""

import pytest

from etf_collector.utils.cash_detector import (
    is_cash_code,
    is_cash_name,
    is_cash_item,
    generate_cash_code,
    is_synthetic_cash_code,
    CASH_STOCK_CODES,
    CASH_NAME_KEYWORDS,
)


class TestIsCashCode:
    """Tests for is_cash_code function."""

    def test_krd_format(self):
        """Test KRD format code detection."""
        assert is_cash_code("KRD010010001") is True

    def test_short_format(self):
        """Test short format code detection."""
        assert is_cash_code("010010") is True

    def test_regular_stock_code(self):
        """Test regular stock code is not cash."""
        assert is_cash_code("005930") is False
        assert is_cash_code("069500") is False
        assert is_cash_code("373220") is False

    def test_empty_code(self):
        """Test empty code returns False."""
        assert is_cash_code("") is False

    def test_none_code(self):
        """Test None code returns False."""
        assert is_cash_code(None) is False

    def test_whitespace_code(self):
        """Test whitespace-only code returns False."""
        assert is_cash_code("   ") is False


class TestIsCashName:
    """Tests for is_cash_name function."""

    @pytest.mark.parametrize("name,expected_keyword", [
        ("원화현금", "원화현금"),
        ("원화예금", "원화예금"),
        ("현금및예치금", "현금"),
        ("KRW현금", "현금"),
        ("예치금", "예치금"),
    ])
    def test_korean_cash_names(self, name, expected_keyword):
        """Test various Korean cash name patterns."""
        is_cash, keyword = is_cash_name(name)
        assert is_cash is True
        assert keyword is not None

    @pytest.mark.parametrize("name", [
        "CASH",
        "cash",
        "Cash Account",
        "MMF",
        "mmf",
        "MMF예치금",
        "머니마켓",
        "money market",
        "Money Market Fund",
    ])
    def test_english_and_mixed_cash_names(self, name):
        """Test English and mixed language cash name patterns."""
        is_cash, keyword = is_cash_name(name)
        assert is_cash is True
        assert keyword is not None

    def test_regular_stock_name(self):
        """Test regular stock name is not cash."""
        is_cash, keyword = is_cash_name("삼성전자")
        assert is_cash is False
        assert keyword is None

    def test_regular_stock_names_various(self):
        """Test various regular stock names are not cash."""
        regular_names = [
            "SK하이닉스",
            "LG에너지솔루션",
            "현대자동차",
            "NAVER",
            "카카오",
        ]
        for name in regular_names:
            is_cash, keyword = is_cash_name(name)
            assert is_cash is False, f"'{name}' should not be detected as cash"

    def test_case_insensitive(self):
        """Test case-insensitive matching for English keywords."""
        is_cash1, _ = is_cash_name("MONEY MARKET")
        is_cash2, _ = is_cash_name("money market")
        is_cash3, _ = is_cash_name("Money Market")
        assert is_cash1 is True
        assert is_cash2 is True
        assert is_cash3 is True

    def test_empty_name(self):
        """Test empty name returns False."""
        is_cash, keyword = is_cash_name("")
        assert is_cash is False
        assert keyword is None

    def test_none_name(self):
        """Test None name returns False."""
        is_cash, keyword = is_cash_name(None)
        assert is_cash is False
        assert keyword is None


class TestIsCashItem:
    """Tests for combined is_cash_item function."""

    def test_by_code_only(self):
        """Test detection by code when name is regular stock."""
        assert is_cash_item("KRD010010001", "삼성전자") is True
        assert is_cash_item("010010", "SK하이닉스") is True

    def test_by_name_only(self):
        """Test detection by name when code is regular."""
        assert is_cash_item("999999", "원화예금") is True
        assert is_cash_item("123456", "현금및예치금") is True

    def test_by_both(self):
        """Test detection when both code and name match."""
        assert is_cash_item("KRD010010001", "원화현금") is True
        assert is_cash_item("010010", "원화예금") is True

    def test_neither(self):
        """Test when neither code nor name matches."""
        assert is_cash_item("005930", "삼성전자") is False
        assert is_cash_item("069500", "KODEX 200") is False

    def test_empty_code_with_cash_name(self):
        """Test detection with empty code but cash name."""
        assert is_cash_item("", "원화예금") is True
        assert is_cash_item(None, "현금") is True

    def test_cash_code_with_empty_name(self):
        """Test detection with cash code but empty name."""
        assert is_cash_item("KRD010010001", "") is True
        assert is_cash_item("010010", None) is True


class TestGenerateCashCode:
    """Tests for generate_cash_code function."""

    def test_format(self):
        """Test generated code format."""
        code = generate_cash_code("069500", "원화예금")
        assert code.startswith("CASH_069500_")
        assert len(code) > len("CASH_069500_")

    def test_deterministic(self):
        """Test same inputs produce same output."""
        code1 = generate_cash_code("069500", "원화예금")
        code2 = generate_cash_code("069500", "원화예금")
        assert code1 == code2

    def test_different_etf_different_codes(self):
        """Test different ETF codes produce different outputs."""
        code1 = generate_cash_code("069500", "원화예금")
        code2 = generate_cash_code("373220", "원화예금")
        assert code1 != code2

    def test_different_names_different_codes(self):
        """Test different names produce different codes."""
        code1 = generate_cash_code("069500", "원화예금")
        code2 = generate_cash_code("069500", "원화현금")
        assert code1 != code2

    def test_hash_format(self):
        """Test hash portion is 4-digit hex."""
        code = generate_cash_code("069500", "원화예금")
        parts = code.split("_")
        assert len(parts) == 3
        assert parts[0] == "CASH"
        assert parts[1] == "069500"
        # Hash should be 4 hex digits
        assert len(parts[2]) == 4
        int(parts[2], 16)  # Should not raise


class TestIsSyntheticCashCode:
    """Tests for is_synthetic_cash_code function."""

    def test_synthetic_code(self):
        """Test synthetic code detection."""
        assert is_synthetic_cash_code("CASH_069500_1A2B") is True
        assert is_synthetic_cash_code("CASH_373220_FFFF") is True

    def test_generated_code(self):
        """Test generated code is recognized as synthetic."""
        code = generate_cash_code("069500", "원화예금")
        assert is_synthetic_cash_code(code) is True

    def test_regular_code(self):
        """Test regular code is not synthetic."""
        assert is_synthetic_cash_code("005930") is False
        assert is_synthetic_cash_code("069500") is False

    def test_krd_code_not_synthetic(self):
        """Test KRD cash code is not synthetic."""
        assert is_synthetic_cash_code("KRD010010001") is False

    def test_short_cash_code_not_synthetic(self):
        """Test short cash code is not synthetic."""
        assert is_synthetic_cash_code("010010") is False


class TestConstants:
    """Tests for module constants."""

    def test_cash_stock_codes_contains_expected(self):
        """Test CASH_STOCK_CODES contains expected codes."""
        assert "KRD010010001" in CASH_STOCK_CODES
        assert "010010" in CASH_STOCK_CODES

    def test_cash_stock_codes_is_frozenset(self):
        """Test CASH_STOCK_CODES is immutable."""
        assert isinstance(CASH_STOCK_CODES, frozenset)

    def test_cash_name_keywords_contains_expected(self):
        """Test CASH_NAME_KEYWORDS contains expected keywords."""
        expected = ["원화현금", "원화예금", "현금", "cash", "mmf"]
        for keyword in expected:
            assert keyword in CASH_NAME_KEYWORDS, f"'{keyword}' should be in CASH_NAME_KEYWORDS"

    def test_cash_name_keywords_is_tuple(self):
        """Test CASH_NAME_KEYWORDS is immutable tuple."""
        assert isinstance(CASH_NAME_KEYWORDS, tuple)
