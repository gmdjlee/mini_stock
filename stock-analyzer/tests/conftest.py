"""Pytest configuration and fixtures."""

from unittest.mock import Mock, patch

import pytest


@pytest.fixture
def mock_token_response():
    """Mock token response data."""
    return {
        "expires_dt": "20261231235959",
        "token_type": "bearer",
        "token": "test_token_12345",
        "return_code": 0,
        "return_msg": "정상적으로 처리되었습니다",
    }


@pytest.fixture
def mock_stock_list_response():
    """Mock stock list response data."""
    return {
        "stk_list": [
            {"stk_cd": "005930", "stk_nm": "삼성전자", "mrkt_tp": "1"},
            {"stk_cd": "000660", "stk_nm": "SK하이닉스", "mrkt_tp": "1"},
            {"stk_cd": "035720", "stk_nm": "카카오", "mrkt_tp": "1"},
            {"stk_cd": "035420", "stk_nm": "NAVER", "mrkt_tp": "1"},
            {"stk_cd": "373220", "stk_nm": "LG에너지솔루션", "mrkt_tp": "1"},
        ],
        "return_code": 0,
        "return_msg": "정상적으로 처리되었습니다",
    }


@pytest.fixture
def mock_stock_info_response():
    """Mock stock info response data."""
    return {
        "stk_cd": "005930",
        "stk_nm": "삼성전자",
        "cur_prc": 55000,
        "mrkt_tot_amt": 328000000000000,
        "per": 8.5,
        "pbr": 1.2,
        "return_code": 0,
        "return_msg": "정상적으로 처리되었습니다",
    }


@pytest.fixture
def mock_investor_trend_response():
    """Mock investor trend response data."""
    return {
        "trend_list": [
            {
                "dt": "20250110",
                "mrkt_tot_amt": 328000000000000,
                "frgn_5d_net": 7500000000,
                "istt_5d_net": -2500000000,
            },
            {
                "dt": "20250109",
                "mrkt_tot_amt": 327000000000000,
                "frgn_5d_net": 6500000000,
                "istt_5d_net": -2000000000,
            },
        ],
        "return_code": 0,
        "return_msg": "정상적으로 처리되었습니다",
    }


@pytest.fixture
def mock_chart_response():
    """Mock chart response data."""
    return {
        "chart_list": [
            {
                "dt": "20250110",
                "opn_prc": 54000,
                "high_prc": 55500,
                "low_prc": 53800,
                "cls_prc": 55000,
                "trd_qty": 15000000,
            },
            {
                "dt": "20250109",
                "opn_prc": 53500,
                "high_prc": 54500,
                "low_prc": 53000,
                "cls_prc": 54000,
                "trd_qty": 12000000,
            },
        ],
        "return_code": 0,
        "return_msg": "정상적으로 처리되었습니다",
    }


@pytest.fixture
def mock_kiwoom_client(
    mock_token_response,
    mock_stock_list_response,
    mock_stock_info_response,
    mock_investor_trend_response,
    mock_chart_response,
):
    """Create a mock KiwoomClient."""
    from stock_analyzer.client.kiwoom import ApiResponse, KiwoomClient

    client = Mock(spec=KiwoomClient)

    # Mock get_stock_list
    client.get_stock_list.return_value = ApiResponse(
        ok=True,
        data=mock_stock_list_response,
    )

    # Mock get_stock_info
    client.get_stock_info.return_value = ApiResponse(
        ok=True,
        data=mock_stock_info_response,
    )

    # Mock get_investor_trend
    client.get_investor_trend.return_value = ApiResponse(
        ok=True,
        data=mock_investor_trend_response,
    )

    # Mock get_daily_chart
    client.get_daily_chart.return_value = ApiResponse(
        ok=True,
        data=mock_chart_response,
    )

    return client
