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

    def test_search_filters_kospi_kosdaq_only(self, mock_kiwoom_client):
        """Test search filters KOSPI/KOSDAQ stocks only by default."""
        from stock_analyzer.client.kiwoom import ApiResponse

        # Mock response with mixed markets
        mock_kiwoom_client.get_stock_list.return_value = ApiResponse(
            ok=True,
            data={
                "list": [
                    {"code": "005930", "name": "삼성전자", "marketName": "코스피"},
                    {"code": "035720", "name": "카카오", "marketName": "코스닥"},
                    {"code": "900110", "name": "이스트아시아홀딩스", "marketName": "코넥스"},
                ],
                "return_code": 0,
                "return_msg": "정상적으로 처리되었습니다",
            },
        )

        # Search should only return KOSPI/KOSDAQ stocks
        result = search(mock_kiwoom_client, "")  # Will fail with INVALID_ARG
        assert result["ok"] is False

        # Search with valid query
        result = search(mock_kiwoom_client, "삼성")
        assert result["ok"] is True
        assert len(result["data"]) == 1
        assert result["data"][0]["market"] == "KOSPI"

    def test_search_with_custom_markets(self, mock_kiwoom_client):
        """Test search with custom markets filter."""
        from stock_analyzer.client.kiwoom import ApiResponse

        # Mock response with mixed markets - override side_effect
        mock_kiwoom_client.get_stock_list.side_effect = None
        mock_kiwoom_client.get_stock_list.return_value = ApiResponse(
            ok=True,
            data={
                "list": [
                    {"code": "005930", "name": "삼성전자", "marketName": "코스피"},
                    {"code": "035720", "name": "카카오", "marketName": "코스닥"},
                    {"code": "900110", "name": "이스트아시아홀딩스", "marketName": "코넥스"},
                ],
                "return_code": 0,
                "return_msg": "정상적으로 처리되었습니다",
            },
        )

        # Search with all markets including KONEX
        result = search(mock_kiwoom_client, "이스트", markets=["KOSPI", "KOSDAQ", "코넥스"])
        assert result["ok"] is True
        assert len(result["data"]) == 1
        assert result["data"][0]["ticker"] == "900110"


class TestGetAll:
    """Tests for get_all function."""

    def test_get_all_success(self, mock_kiwoom_client):
        """Test get all stocks (default: KOSPI/KOSDAQ only)."""
        result = get_all(mock_kiwoom_client)
        assert result["ok"] is True
        # All mock data is KOSPI, so all 6 stocks should be returned
        assert len(result["data"]) == 6
        # Verify all returned stocks are KOSPI or KOSDAQ
        for stock in result["data"]:
            assert stock["market"] in ["KOSPI", "KOSDAQ"]

    def test_get_all_with_markets_filter(self, mock_kiwoom_client):
        """Test get all stocks with custom markets filter."""
        result = get_all(mock_kiwoom_client, markets=["KOSPI"])
        assert result["ok"] is True
        # KOSPI mock has 3 stocks
        assert len(result["data"]) == 3
        for stock in result["data"]:
            assert stock["market"] == "KOSPI"

    def test_get_all_filters_out_other_markets(self, mock_kiwoom_client):
        """Test that non-KOSPI/KOSDAQ stocks are filtered out."""
        from stock_analyzer.client.kiwoom import ApiResponse

        # Mock response with mixed markets (KOSPI, KOSDAQ, and others)
        # Override side_effect to provide custom mock for this test
        def custom_side_effect(market="0", cont_yn="", next_key=""):
            if market == "1":  # KOSPI
                return ApiResponse(
                    ok=True,
                    data={
                        "list": [
                            {"code": "005930", "name": "삼성전자", "marketName": "거래소"},
                            {"code": "900110", "name": "이스트아시아홀딩스", "marketName": "코넥스"},
                        ],
                        "return_code": 0,
                        "return_msg": "정상적으로 처리되었습니다",
                    },
                )
            elif market == "2":  # KOSDAQ
                return ApiResponse(
                    ok=True,
                    data={
                        "list": [
                            {"code": "035720", "name": "카카오", "marketName": "코스닥"},
                            {"code": "950130", "name": "외국주식ETN", "marketName": "ETN"},
                        ],
                        "return_code": 0,
                        "return_msg": "정상적으로 처리되었습니다",
                    },
                )
            return ApiResponse(ok=True, data={"list": []})

        mock_kiwoom_client.get_stock_list.side_effect = custom_side_effect

        result = get_all(mock_kiwoom_client)
        assert result["ok"] is True
        # KOSPI API returns: 1 valid (거래소→KOSPI), 1 filtered out (코넥스)
        # KOSDAQ API returns: 1 valid (코스닥→KOSDAQ), 1 filtered out (ETN)
        # Only 2 stocks should be returned (one KOSPI, one KOSDAQ)
        assert len(result["data"]) == 2
        tickers = [stock["ticker"] for stock in result["data"]]
        assert "005930" in tickers  # KOSPI (거래소)
        assert "035720" in tickers  # KOSDAQ (코스닥)
        assert "900110" not in tickers  # KONEX filtered out
        assert "950130" not in tickers  # ETN filtered out


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
