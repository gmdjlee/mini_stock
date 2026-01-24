"""Pytest fixtures for ETF Collector tests."""

import pytest
from datetime import datetime, timedelta


@pytest.fixture
def mock_kis_token_response():
    """Mock KIS token response."""
    return {
        "access_token": "test_access_token_12345",
        "token_type": "Bearer",
        "expires_in": 86400,
    }


@pytest.fixture
def mock_etf_list_response():
    """Mock ETF list response (CTPF1604R)."""
    return {
        "output": [
            {
                "pdno": "069500",
                "prdt_abrv_name": "KODEX 200",
                "lstg_dt": "20021014",
                "idx_bztp_scls_cd_name": "시장대표",
                "idx_bztp_lcls_cd_name": "국내주식",
                "etf_cmpn_cd_name": "삼성자산운용",
                "etf_assr_ttam": "5823450000000000",
            },
            {
                "pdno": "278530",
                "prdt_abrv_name": "KODEX 200 액티브",
                "lstg_dt": "20170901",
                "idx_bztp_scls_cd_name": "시장대표",
                "idx_bztp_lcls_cd_name": "국내주식",
                "etf_cmpn_cd_name": "삼성자산운용",
                "etf_assr_ttam": "123450000000000",
            },
            {
                "pdno": "379800",
                "prdt_abrv_name": "KODEX 미국S&P500TR",
                "lstg_dt": "20210101",
                "idx_bztp_scls_cd_name": "해외지수",
                "idx_bztp_lcls_cd_name": "해외주식",
                "etf_cmpn_cd_name": "삼성자산운용",
                "etf_assr_ttam": "234560000000000",
            },
            {
                "pdno": "123456",
                "prdt_abrv_name": "TIGER Active AI",
                "lstg_dt": "20220615",
                "idx_bztp_scls_cd_name": "섹터",
                "idx_bztp_lcls_cd_name": "국내주식",
                "etf_cmpn_cd_name": "미래에셋자산운용",
                "etf_assr_ttam": "56780000000000",
            },
            {
                "pdno": "234567",
                "prdt_abrv_name": "KODEX 200 레버리지",
                "lstg_dt": "20100101",
                "idx_bztp_scls_cd_name": "레버리지",
                "idx_bztp_lcls_cd_name": "국내주식",
                "etf_cmpn_cd_name": "삼성자산운용",
                "etf_assr_ttam": "45670000000000",
            },
        ],
        "rt_cd": "0",
        "msg_cd": "0000",
        "msg1": "정상처리",
    }


@pytest.fixture
def mock_constituent_response():
    """Mock constituent response (FHKST121600C0)."""
    return {
        "output1": {
            "stck_prpr": "35250",
            "prdy_vrss": "500",
            "prdy_ctrt": "1.44",
            "nav": "35248.50",
            "etf_ntas_ttam": "58234500000000",
            "etf_cu_unit_scrt_cnt": "50000",
            "etf_cnfg_issu_cnt": "200",
        },
        "output2": [
            {
                "stck_shrn_iscd": "005930",
                "hts_kor_isnm": "삼성전자",
                "stck_prpr": "71500",
                "prdy_vrss": "500",
                "prdy_vrss_sign": "2",
                "prdy_ctrt": "0.70",
                "acml_vol": "15000000",
                "acml_tr_pbmn": "1072500000000",
                "hts_avls": "427000000000000",
                "etf_vltn_amt": "15625000000",
                "etf_cnfg_issu_rlim": "31.25",
            },
            {
                "stck_shrn_iscd": "000660",
                "hts_kor_isnm": "SK하이닉스",
                "stck_prpr": "135000",
                "prdy_vrss": "2000",
                "prdy_vrss_sign": "2",
                "prdy_ctrt": "1.50",
                "acml_vol": "5000000",
                "acml_tr_pbmn": "675000000000",
                "hts_avls": "98000000000000",
                "etf_vltn_amt": "4210000000",
                "etf_cnfg_issu_rlim": "8.42",
            },
            {
                "stck_shrn_iscd": "035420",
                "hts_kor_isnm": "NAVER",
                "stck_prpr": "195000",
                "prdy_vrss": "-3000",
                "prdy_vrss_sign": "5",
                "prdy_ctrt": "-1.52",
                "acml_vol": "2000000",
                "acml_tr_pbmn": "390000000000",
                "hts_avls": "32000000000000",
                "etf_vltn_amt": "2100000000",
                "etf_cnfg_issu_rlim": "4.20",
            },
        ],
        "rt_cd": "0",
        "msg_cd": "0000",
        "msg1": "정상처리",
    }


@pytest.fixture
def mock_error_response():
    """Mock API error response."""
    return {
        "rt_cd": "1",
        "msg_cd": "EGW00201",
        "msg1": "초당 거래건수를 초과하였습니다.",
    }


@pytest.fixture
def sample_etf_infos():
    """Sample EtfInfo objects for testing."""
    from etf_collector.collector.etf_list import EtfInfo

    return [
        EtfInfo(
            etf_code="069500",
            etf_name="KODEX 200",
            etf_type="Passive",
            listing_date="20021014",
            tracking_index="시장대표",
            asset_class="국내주식",
            management_company="삼성자산운용",
            total_assets=58234.5,
        ),
        EtfInfo(
            etf_code="278530",
            etf_name="KODEX 200 액티브",
            etf_type="Active",
            listing_date="20170901",
            tracking_index="시장대표",
            asset_class="국내주식",
            management_company="삼성자산운용",
            total_assets=1234.5,
        ),
        EtfInfo(
            etf_code="123456",
            etf_name="TIGER Active AI",
            etf_type="Active",
            listing_date="20220615",
            tracking_index="섹터",
            asset_class="국내주식",
            management_company="미래에셋자산운용",
            total_assets=567.8,
        ),
        EtfInfo(
            etf_code="234567",
            etf_name="KODEX 200 레버리지",
            etf_type="Passive",
            listing_date="20100101",
            tracking_index="레버리지",
            asset_class="국내주식",
            management_company="삼성자산운용",
            total_assets=456.7,
        ),
    ]
