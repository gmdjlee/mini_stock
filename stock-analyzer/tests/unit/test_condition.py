"""Tests for condition search module."""

import pytest

from stock_analyzer.client.kiwoom import ApiResponse
from stock_analyzer.search import condition


class TestGetList:
    """Tests for get_list function."""

    def test_success(self, mock_kiwoom_client):
        """Test successful get_list."""
        result = condition.get_list(mock_kiwoom_client)
        assert result["ok"] is True
        assert len(result["data"]) == 3
        assert result["data"][0]["idx"] == "000"
        assert result["data"][0]["name"] == "골든크로스"

    def test_all_conditions(self, mock_kiwoom_client):
        """Test all conditions are returned."""
        result = condition.get_list(mock_kiwoom_client)
        assert result["ok"] is True
        conditions = result["data"]
        assert conditions[1]["idx"] == "001"
        assert conditions[1]["name"] == "급등주"
        assert conditions[2]["idx"] == "002"
        assert conditions[2]["name"] == "거래량 폭발"

    def test_no_data(self, mock_kiwoom_client):
        """Test with no conditions."""
        mock_kiwoom_client.get_condition_list.return_value = ApiResponse(
            ok=True,
            data={"cond_list": [], "return_code": 0},
        )
        result = condition.get_list(mock_kiwoom_client)
        assert result["ok"] is False
        assert result["error"]["code"] == "NO_DATA"

    def test_api_error(self, mock_kiwoom_client):
        """Test API error handling."""
        mock_kiwoom_client.get_condition_list.return_value = ApiResponse(
            ok=False,
            error={"code": "API_ERROR", "msg": "API 오류"},
        )
        result = condition.get_list(mock_kiwoom_client)
        assert result["ok"] is False
        assert result["error"]["code"] == "API_ERROR"


class TestSearch:
    """Tests for search function."""

    def test_empty_cond_idx(self, mock_kiwoom_client):
        """Test with empty condition index."""
        result = condition.search(mock_kiwoom_client, "", "골든크로스")
        assert result["ok"] is False
        assert result["error"]["code"] == "INVALID_ARG"

    def test_empty_cond_name(self, mock_kiwoom_client):
        """Test with empty condition name."""
        result = condition.search(mock_kiwoom_client, "000", "")
        assert result["ok"] is False
        assert result["error"]["code"] == "INVALID_ARG"

    def test_success(self, mock_kiwoom_client):
        """Test successful search."""
        result = condition.search(mock_kiwoom_client, "000", "골든크로스")
        assert result["ok"] is True
        data = result["data"]
        assert data["condition"]["idx"] == "000"
        assert data["condition"]["name"] == "골든크로스"
        assert len(data["stocks"]) == 2

    def test_stock_data(self, mock_kiwoom_client):
        """Test stock data in search result."""
        result = condition.search(mock_kiwoom_client, "000", "골든크로스")
        assert result["ok"] is True
        stocks = result["data"]["stocks"]
        assert stocks[0]["ticker"] == "005930"
        assert stocks[0]["name"] == "삼성전자"
        assert stocks[0]["price"] == 55000
        assert stocks[0]["change"] == 1.5

    def test_empty_result(self, mock_kiwoom_client):
        """Test search with no matching stocks."""
        mock_kiwoom_client.search_condition.return_value = ApiResponse(
            ok=True,
            data={"stk_list": [], "return_code": 0},
        )
        result = condition.search(mock_kiwoom_client, "000", "골든크로스")
        assert result["ok"] is True
        assert len(result["data"]["stocks"]) == 0

    def test_api_error(self, mock_kiwoom_client):
        """Test API error handling."""
        mock_kiwoom_client.search_condition.return_value = ApiResponse(
            ok=False,
            error={"code": "API_ERROR", "msg": "API 오류"},
        )
        result = condition.search(mock_kiwoom_client, "000", "골든크로스")
        assert result["ok"] is False
        assert result["error"]["code"] == "API_ERROR"


class TestSearchByIdx:
    """Tests for search_by_idx function."""

    def test_empty_cond_idx(self, mock_kiwoom_client):
        """Test with empty condition index."""
        result = condition.search_by_idx(mock_kiwoom_client, "")
        assert result["ok"] is False
        assert result["error"]["code"] == "INVALID_ARG"

    def test_success(self, mock_kiwoom_client):
        """Test successful search by index."""
        result = condition.search_by_idx(mock_kiwoom_client, "000")
        assert result["ok"] is True
        data = result["data"]
        assert data["condition"]["idx"] == "000"
        assert data["condition"]["name"] == "골든크로스"
        assert len(data["stocks"]) == 2

    def test_condition_not_found(self, mock_kiwoom_client):
        """Test with non-existent condition index."""
        result = condition.search_by_idx(mock_kiwoom_client, "999")
        assert result["ok"] is False
        assert result["error"]["code"] == "CONDITION_NOT_FOUND"

    def test_get_list_error(self, mock_kiwoom_client):
        """Test when get_list fails."""
        mock_kiwoom_client.get_condition_list.return_value = ApiResponse(
            ok=False,
            error={"code": "API_ERROR", "msg": "API 오류"},
        )
        result = condition.search_by_idx(mock_kiwoom_client, "000")
        assert result["ok"] is False
        assert result["error"]["code"] == "API_ERROR"
