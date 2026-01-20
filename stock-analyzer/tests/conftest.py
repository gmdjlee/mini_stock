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
    """Mock stock list response data (matches ka10099 API response)."""
    # Default response for backward compatibility (used in search tests)
    return {
        "list": [
            {"code": "005930", "name": "삼성전자", "marketName": "코스피"},
            {"code": "000660", "name": "SK하이닉스", "marketName": "코스피"},
            {"code": "035720", "name": "카카오", "marketName": "코스피"},
            {"code": "035420", "name": "NAVER", "marketName": "코스피"},
            {"code": "373220", "name": "LG에너지솔루션", "marketName": "코스피"},
            {"code": "267250", "name": "HD현대", "marketName": "코스피"},
        ],
        "return_code": 0,
        "return_msg": "정상적으로 처리되었습니다",
    }


@pytest.fixture
def mock_stock_list_kospi_response():
    """Mock KOSPI stock list response (mrkt_tp="1")."""
    return {
        "list": [
            {"code": "005930", "name": "삼성전자", "marketName": "거래소"},
            {"code": "000660", "name": "SK하이닉스", "marketName": "거래소"},
            {"code": "035420", "name": "NAVER", "marketName": "거래소"},
        ],
        "return_code": 0,
        "return_msg": "정상적으로 처리되었습니다",
    }


@pytest.fixture
def mock_stock_list_kosdaq_response():
    """Mock KOSDAQ stock list response (mrkt_tp="2")."""
    return {
        "list": [
            {"code": "035720", "name": "카카오", "marketName": "코스닥"},
            {"code": "373220", "name": "LG에너지솔루션", "marketName": "코스닥"},
            {"code": "267250", "name": "HD현대", "marketName": "코스닥"},
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
    """Mock investor trend response data (matches ka10059 API response)."""
    return {
        "stk_invsr_orgn": [
            {
                "dt": "20250110",
                "mrkt_tot_amt": 328000000000000,
                "frgnr_invsr": 7500000000,
                "orgn": -2500000000,
            },
            {
                "dt": "20250109",
                "mrkt_tot_amt": 327000000000000,
                "frgnr_invsr": 6500000000,
                "orgn": -2000000000,
            },
        ],
        "return_code": 0,
        "return_msg": "정상적으로 처리되었습니다",
    }


@pytest.fixture
def mock_chart_response():
    """Mock chart response data (matches ka10081 API response)."""
    return {
        "stk_dt_pole_chart_qry": [
            {
                "dt": "20250110",
                "open_pric": 54000,
                "high_pric": 55500,
                "low_pric": 53800,
                "cur_prc": 55000,
                "trde_qty": 15000000,
            },
            {
                "dt": "20250109",
                "open_pric": 53500,
                "high_pric": 54500,
                "low_pric": 53000,
                "cur_prc": 54000,
                "trde_qty": 12000000,
            },
        ],
        "return_code": 0,
        "return_msg": "정상적으로 처리되었습니다",
    }


@pytest.fixture
def mock_deposit_trend_response():
    """Mock deposit trend response data."""
    return {
        "deposit_list": [
            {
                "dt": "20250110",
                "cust_deposit": 50000000000000,
                "credit_loan": 15000000000000,
            },
            {
                "dt": "20250109",
                "cust_deposit": 49500000000000,
                "credit_loan": 14800000000000,
            },
        ],
        "return_code": 0,
        "return_msg": "정상적으로 처리되었습니다",
    }


@pytest.fixture
def mock_credit_trend_response():
    """Mock credit trend response data."""
    return {
        "credit_list": [
            {
                "dt": "20250110",
                "credit_bal": 18000000000000,
                "credit_rt": 5.2,
            },
            {
                "dt": "20250109",
                "credit_bal": 17800000000000,
                "credit_rt": 5.1,
            },
        ],
        "return_code": 0,
        "return_msg": "정상적으로 처리되었습니다",
    }


@pytest.fixture
def mock_condition_list_response():
    """Mock condition list response data."""
    return {
        "cond_list": [
            {"cond_idx": "000", "cond_nm": "골든크로스"},
            {"cond_idx": "001", "cond_nm": "급등주"},
            {"cond_idx": "002", "cond_nm": "거래량 폭발"},
        ],
        "return_code": 0,
        "return_msg": "정상적으로 처리되었습니다",
    }


@pytest.fixture
def mock_condition_search_response():
    """Mock condition search response data."""
    return {
        "stk_list": [
            {"stk_cd": "005930", "stk_nm": "삼성전자", "cur_prc": 55000, "chg_rt": 1.5},
            {"stk_cd": "000660", "stk_nm": "SK하이닉스", "cur_prc": 125000, "chg_rt": 2.3},
        ],
        "return_code": 0,
        "return_msg": "정상적으로 처리되었습니다",
    }


@pytest.fixture
def mock_kiwoom_client(
    mock_token_response,
    mock_stock_list_response,
    mock_stock_list_kospi_response,
    mock_stock_list_kosdaq_response,
    mock_stock_info_response,
    mock_investor_trend_response,
    mock_chart_response,
    mock_deposit_trend_response,
    mock_credit_trend_response,
    mock_condition_list_response,
    mock_condition_search_response,
):
    """Create a mock KiwoomClient."""
    from stock_analyzer.client.kiwoom import ApiResponse, KiwoomClient

    client = Mock(spec=KiwoomClient)

    # Mock get_stock_list - return different data based on market code
    def get_stock_list_side_effect(market="0", cont_yn="", next_key=""):
        if market == "1":  # KOSPI
            return ApiResponse(ok=True, data=mock_stock_list_kospi_response)
        elif market == "2":  # KOSDAQ
            return ApiResponse(ok=True, data=mock_stock_list_kosdaq_response)
        else:  # "0" or default - return all (used in search function)
            return ApiResponse(ok=True, data=mock_stock_list_response)

    client.get_stock_list.side_effect = get_stock_list_side_effect

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

    # Mock get_deposit_trend
    client.get_deposit_trend.return_value = ApiResponse(
        ok=True,
        data=mock_deposit_trend_response,
    )

    # Mock get_credit_trend
    client.get_credit_trend.return_value = ApiResponse(
        ok=True,
        data=mock_credit_trend_response,
    )

    # Mock get_condition_list
    client.get_condition_list.return_value = ApiResponse(
        ok=True,
        data=mock_condition_list_response,
    )

    # Mock search_condition
    client.search_condition.return_value = ApiResponse(
        ok=True,
        data=mock_condition_search_response,
    )

    return client
