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

    def test_get_all_filters_by_market_name(self, mock_kiwoom_client):
        """Test that get_all filters stocks by marketName field.

        When fetching with mrkt_tp=0 (all markets), stocks are filtered by their
        marketName field. Only stocks with marketName containing '코스피', '거래소',
        or '코스닥' are included (by default).
        """
        from stock_analyzer.client.kiwoom import ApiResponse

        # Mock response with mixed market names (including sector names)
        # This simulates real API behavior where some stocks have sector names
        def custom_side_effect(market="0", cont_yn="", next_key=""):
            return ApiResponse(
                ok=True,
                data={
                    "list": [
                        {"code": "005930", "name": "삼성전자", "marketName": "코스피"},
                        {"code": "000660", "name": "SK하이닉스", "marketName": "거래소"},
                        {"code": "035720", "name": "카카오", "marketName": "코스닥"},
                        {"code": "373220", "name": "LG에너지솔루션", "marketName": "코스닥"},
                        {"code": "088980", "name": "맥쿼리인프라", "marketName": "인프라투자금융"},
                        {"code": "415640", "name": "KB발해인프라", "marketName": "인프라투자금융"},
                    ],
                    "return_code": 0,
                    "return_msg": "정상적으로 처리되었습니다",
                },
            )

        mock_kiwoom_client.get_stock_list.side_effect = custom_side_effect

        result = get_all(mock_kiwoom_client)
        assert result["ok"] is True
        # Only 4 stocks with KOSPI/KOSDAQ market names should be returned
        # (인프라투자금융 stocks are filtered out)
        assert len(result["data"]) == 4

        # Verify market assignment based on marketName field
        kospi_stocks = [s for s in result["data"] if s["market"] == "KOSPI"]
        kosdaq_stocks = [s for s in result["data"] if s["market"] == "KOSDAQ"]

        assert len(kospi_stocks) == 2
        assert len(kosdaq_stocks) == 2

        # Check specific tickers
        kospi_tickers = [s["ticker"] for s in kospi_stocks]
        kosdaq_tickers = [s["ticker"] for s in kosdaq_stocks]

        assert "005930" in kospi_tickers  # 삼성전자 (코스피)
        assert "000660" in kospi_tickers  # SK하이닉스 (거래소)
        assert "035720" in kosdaq_tickers  # 카카오 (코스닥)
        assert "373220" in kosdaq_tickers  # LG에너지솔루션 (코스닥)

        # Verify 인프라투자금융 stocks are not included
        all_tickers = [s["ticker"] for s in result["data"]]
        assert "088980" not in all_tickers
        assert "415640" not in all_tickers


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
