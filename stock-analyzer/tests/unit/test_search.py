"""Tests for search module."""

import pytest

from stock_analyzer.stock.search import search, get_all, get_name, get_info


class TestSearch:
    """Tests for search function."""

    def test_search_empty_query(self, mock_kiwoom_client):
        """Test search with empty query."""
        result = search(mock_kiwoom_client, "")
        assert result["ok"] is False
        assert result["error"]["code"] == "INVALID_ARG"

    def test_search_whitespace_query(self, mock_kiwoom_client):
        """Test search with whitespace query."""
        result = search(mock_kiwoom_client, "   ")
        assert result["ok"] is False
        assert result["error"]["code"] == "INVALID_ARG"

    def test_search_by_name(self, mock_kiwoom_client):
        """Test search by stock name."""
        result = search(mock_kiwoom_client, "삼성")
        assert result["ok"] is True
        assert len(result["data"]) >= 1
        assert any(item["name"] == "삼성전자" for item in result["data"])

    def test_search_by_code(self, mock_kiwoom_client):
        """Test search by stock code."""
        result = search(mock_kiwoom_client, "005930")
        assert result["ok"] is True
        assert len(result["data"]) >= 1
        assert result["data"][0]["ticker"] == "005930"

    def test_search_case_insensitive(self, mock_kiwoom_client):
        """Test search is case insensitive."""
        result = search(mock_kiwoom_client, "naver")
        assert result["ok"] is True
        assert any(item["name"] == "NAVER" for item in result["data"])

    def test_search_no_results(self, mock_kiwoom_client):
        """Test search with no results."""
        result = search(mock_kiwoom_client, "존재하지않는종목")
        assert result["ok"] is True
        assert len(result["data"]) == 0


class TestGetAll:
    """Tests for get_all function."""

    def test_get_all_success(self, mock_kiwoom_client):
        """Test get all stocks."""
        result = get_all(mock_kiwoom_client)
        assert result["ok"] is True
        assert len(result["data"]) == 5

    def test_get_all_with_market(self, mock_kiwoom_client):
        """Test get all stocks with market filter."""
        result = get_all(mock_kiwoom_client, market="1")
        assert result["ok"] is True
        mock_kiwoom_client.get_stock_list.assert_called_with("1")


class TestGetInfo:
    """Tests for get_info function."""

    def test_get_info_success(self, mock_kiwoom_client):
        """Test get stock info."""
        result = get_info(mock_kiwoom_client, "005930")
        assert result["ok"] is True
        assert result["data"]["ticker"] == "005930"
        assert result["data"]["name"] == "삼성전자"
        assert result["data"]["price"] == 55000
        assert result["data"]["mcap"] == 328000000000000

    def test_get_info_empty_ticker(self, mock_kiwoom_client):
        """Test get info with empty ticker."""
        result = get_info(mock_kiwoom_client, "")
        assert result["ok"] is False
        assert result["error"]["code"] == "INVALID_ARG"


class TestGetName:
    """Tests for get_name function."""

    def test_get_name_success(self, mock_kiwoom_client):
        """Test get stock name."""
        name = get_name(mock_kiwoom_client, "005930")
        assert name == "삼성전자"
