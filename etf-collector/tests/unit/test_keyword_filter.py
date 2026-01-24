"""Tests for keyword filter module."""

import pytest

from etf_collector.filter.keyword import (
    KeywordFilter,
    FilterMode,
    CombinedFilter,
    ACTIVE_ETF_FILTER,
    EXCLUDE_LEVERAGE_FILTER,
    combine_filters,
    create_filter_from_args,
)


class TestKeywordFilter:
    """Tests for KeywordFilter."""

    def test_include_mode_matches(self):
        """Test INCLUDE mode matches any keyword."""
        f = KeywordFilter(
            keywords=["액티브", "Active"],
            mode=FilterMode.INCLUDE,
        )

        assert f.matches("KODEX 200 액티브") is True
        assert f.matches("Active ETF") is True
        assert f.matches("KODEX 200") is False

    def test_include_or_mode(self):
        """Test INCLUDE_OR mode (alias for INCLUDE)."""
        f = KeywordFilter(
            keywords=["반도체", "AI"],
            mode=FilterMode.INCLUDE_OR,
        )

        assert f.matches("반도체 ETF") is True
        assert f.matches("AI Tech ETF") is True
        assert f.matches("금융 ETF") is False

    def test_include_and_mode(self):
        """Test INCLUDE_AND mode requires all keywords."""
        f = KeywordFilter(
            keywords=["KODEX", "200"],
            mode=FilterMode.INCLUDE_AND,
        )

        assert f.matches("KODEX 200") is True
        assert f.matches("KODEX 200 액티브") is True
        assert f.matches("KODEX 100") is False
        assert f.matches("TIGER 200") is False

    def test_exclude_mode(self):
        """Test EXCLUDE mode excludes matching items."""
        f = KeywordFilter(
            keywords=["레버리지", "인버스"],
            mode=FilterMode.EXCLUDE,
        )

        assert f.matches("KODEX 200") is True
        assert f.matches("KODEX 레버리지") is False
        assert f.matches("KODEX 인버스") is False

    def test_case_insensitive(self):
        """Test case-insensitive matching (default)."""
        f = KeywordFilter(
            keywords=["active"],
            mode=FilterMode.INCLUDE,
            case_sensitive=False,
        )

        assert f.matches("ACTIVE ETF") is True
        assert f.matches("Active etf") is True
        assert f.matches("active etf") is True

    def test_case_sensitive(self):
        """Test case-sensitive matching."""
        f = KeywordFilter(
            keywords=["Active"],
            mode=FilterMode.INCLUDE,
            case_sensitive=True,
        )

        assert f.matches("Active ETF") is True
        assert f.matches("ACTIVE ETF") is False
        assert f.matches("active etf") is False

    def test_empty_keywords_matches_all(self):
        """Test empty keywords list matches everything."""
        f = KeywordFilter(keywords=[], mode=FilterMode.INCLUDE)

        assert f.matches("anything") is True
        assert f.matches("") is True

    def test_filter_etfs(self, sample_etf_infos):
        """Test filtering list of ETFs."""
        f = KeywordFilter(
            keywords=["액티브", "Active"],
            mode=FilterMode.INCLUDE_OR,
        )

        filtered = f.filter_etfs(sample_etf_infos)

        assert len(filtered) == 2
        assert all("액티브" in e.etf_name or "Active" in e.etf_name for e in filtered)

    def test_str_representation(self):
        """Test string representation."""
        f = KeywordFilter(
            keywords=["test"],
            mode=FilterMode.INCLUDE,
        )

        result = str(f)
        assert "test" in result
        assert "include" in result


class TestPresetFilters:
    """Tests for preset filters."""

    def test_active_etf_filter(self):
        """Test ACTIVE_ETF_FILTER preset."""
        assert ACTIVE_ETF_FILTER.matches("KODEX 200 액티브") is True
        assert ACTIVE_ETF_FILTER.matches("TIGER Active") is True
        assert ACTIVE_ETF_FILTER.matches("KODEX 200") is False

    def test_exclude_leverage_filter(self):
        """Test EXCLUDE_LEVERAGE_FILTER preset."""
        assert EXCLUDE_LEVERAGE_FILTER.matches("KODEX 200") is True
        assert EXCLUDE_LEVERAGE_FILTER.matches("KODEX 레버리지") is False
        assert EXCLUDE_LEVERAGE_FILTER.matches("KODEX 인버스") is False
        assert EXCLUDE_LEVERAGE_FILTER.matches("KODEX 2X") is False


class TestCombinedFilter:
    """Tests for CombinedFilter."""

    def test_empty_combined_filter(self):
        """Test empty combined filter matches all."""
        cf = CombinedFilter([])

        assert cf.matches("anything") is True

    def test_combined_filter_all_must_pass(self):
        """Test all filters must pass in combined filter."""
        f1 = KeywordFilter(keywords=["액티브"], mode=FilterMode.INCLUDE)
        f2 = KeywordFilter(keywords=["레버리지"], mode=FilterMode.EXCLUDE)

        cf = CombinedFilter([f1, f2])

        # Both conditions must be met
        assert cf.matches("KODEX 액티브") is True
        assert cf.matches("KODEX 200") is False  # Not active
        assert cf.matches("KODEX 액티브 레버리지") is False  # Has leverage

    def test_filter_etfs(self, sample_etf_infos):
        """Test filtering ETFs through combined filter."""
        cf = CombinedFilter([ACTIVE_ETF_FILTER, EXCLUDE_LEVERAGE_FILTER])

        filtered = cf.filter_etfs(sample_etf_infos)

        # Should get active ETFs without leverage
        assert len(filtered) == 2
        assert all("레버리지" not in e.etf_name for e in filtered)


class TestCombineFilters:
    """Tests for combine_filters function."""

    def test_combine_multiple_filters(self):
        """Test combining multiple filters."""
        f1 = KeywordFilter(keywords=["KODEX"], mode=FilterMode.INCLUDE)
        f2 = KeywordFilter(keywords=["레버리지"], mode=FilterMode.EXCLUDE)

        cf = combine_filters(f1, f2)

        assert isinstance(cf, CombinedFilter)
        assert len(cf.filters) == 2


class TestCreateFilterFromArgs:
    """Tests for create_filter_from_args function."""

    def test_active_only(self):
        """Test active_only argument."""
        cf = create_filter_from_args(active_only=True)

        assert len(cf.filters) == 1
        assert cf.matches("KODEX 액티브") is True
        assert cf.matches("KODEX 200") is False

    def test_include_keywords(self):
        """Test include keywords argument."""
        cf = create_filter_from_args(include=["반도체", "AI"])

        assert cf.matches("반도체 ETF") is True
        assert cf.matches("AI ETF") is True
        assert cf.matches("금융 ETF") is False

    def test_exclude_keywords(self):
        """Test exclude keywords argument."""
        cf = create_filter_from_args(exclude=["레버리지"])

        assert cf.matches("KODEX 200") is True
        assert cf.matches("KODEX 레버리지") is False

    def test_combined_args(self):
        """Test combining multiple arguments."""
        cf = create_filter_from_args(
            include=["반도체"],
            exclude=["레버리지"],
            active_only=False,
        )

        assert len(cf.filters) == 2
        assert cf.matches("반도체 ETF") is True
        assert cf.matches("반도체 레버리지") is False
        assert cf.matches("금융 ETF") is False

    def test_no_args(self):
        """Test with no arguments returns empty filter."""
        cf = create_filter_from_args()

        assert len(cf.filters) == 0
        assert cf.matches("anything") is True
