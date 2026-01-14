"""Supply/demand analysis functionality."""

from dataclasses import dataclass
from typing import Dict, List

from ..client.kiwoom import KiwoomClient
from ..core.log import log_err, log_info


@dataclass
class StockData:
    """Stock analysis data."""

    ticker: str
    name: str
    dates: List[str]
    mcap: List[int]      # Market cap
    for_5d: List[int]    # Foreign 5-day net buy
    ins_5d: List[int]    # Institution 5-day net buy


def analyze(client: KiwoomClient, ticker: str, days: int = 180) -> Dict:
    """
    Analyze stock supply/demand.

    Args:
        client: Kiwoom API client
        ticker: Stock code
        days: Number of days to analyze

    Returns:
        {
            "ok": True,
            "data": {
                "ticker": "005930",
                "name": "삼성전자",
                "dates": ["2025-01-02", ...],
                "mcap": [380000000000000, ...],
                "for_5d": [1500000000, ...],
                "ins_5d": [-500000000, ...]
            }
        }

    Errors:
        - INVALID_ARG: Invalid argument
        - TICKER_NOT_FOUND: Stock not found
        - NO_DATA: No data available
        - API_ERROR: API call failed
    """
    if not ticker or not ticker.strip():
        return {
            "ok": False,
            "error": {"code": "INVALID_ARG", "msg": "종목코드가 필요합니다"},
        }

    ticker = ticker.strip()

    # 1. Get basic info
    info_resp = client.get_stock_info(ticker)
    if not info_resp.ok:
        if info_resp.error and info_resp.error.get("code") == "TICKER_NOT_FOUND":
            return {"ok": False, "error": info_resp.error}
        return {"ok": False, "error": info_resp.error}

    name = info_resp.data.get("stk_nm", ticker)
    # API field name per official docs: mac (시가총액)
    mcap = _safe_int(info_resp.data.get("mac", 0))

    # 2. Get investor trend
    trend_resp = client.get_investor_trend(ticker)
    if not trend_resp.ok:
        return {"ok": False, "error": trend_resp.error}

    # API returns data in 'stk_invsr_orgn' field
    trend_data = trend_resp.data.get("stk_invsr_orgn", []) or trend_resp.data.get("list", [])
    if not trend_data:
        return {
            "ok": False,
            "error": {"code": "NO_DATA", "msg": "수급 데이터가 없습니다"},
        }

    # 3. Parse data
    dates = []
    mcaps = []
    for_5d = []
    ins_5d = []

    for item in trend_data[:days]:
        dt = item.get("dt", "")
        # Convert date format if needed (YYYYMMDD -> YYYY-MM-DD)
        if len(dt) == 8:
            dt = f"{dt[:4]}-{dt[4:6]}-{dt[6:8]}"
        dates.append(dt)

        mcaps.append(_safe_int(item.get("mrkt_tot_amt", mcap)))
        # API field names per official docs: frgnr_invsr (외국인투자자), orgn (기관계)
        for_5d.append(_safe_int(item.get("frgnr_invsr", 0)))
        ins_5d.append(_safe_int(item.get("orgn", 0)))

    log_info("stock.analysis", "analyze complete", {"ticker": ticker, "days": len(dates)})

    return {
        "ok": True,
        "data": {
            "ticker": ticker,
            "name": name,
            "dates": dates,
            "mcap": mcaps,
            "for_5d": for_5d,
            "ins_5d": ins_5d,
        },
    }


def get_foreign_trend(client: KiwoomClient, ticker: str) -> Dict:
    """
    Get foreign investor trend.

    Args:
        client: Kiwoom API client
        ticker: Stock code

    Returns:
        {
            "ok": True,
            "data": {
                "ticker": "005930",
                "net_buy": 1500000000,
                "holding_qty": 3000000000,
                "holding_ratio": 52.5
            }
        }
    """
    if not ticker or not ticker.strip():
        return {
            "ok": False,
            "error": {"code": "INVALID_ARG", "msg": "종목코드가 필요합니다"},
        }

    resp = client.get_foreign_trend(ticker.strip())
    if not resp.ok:
        return {"ok": False, "error": resp.error}

    data = resp.data
    return {
        "ok": True,
        "data": {
            "ticker": ticker,
            "net_buy": _safe_int(data.get("frgn_net", 0)),
            "holding_qty": _safe_int(data.get("frgn_hold_qty", 0)),
            "holding_ratio": _safe_float(data.get("frgn_hold_rt", 0)),
        },
    }


def get_institution_trend(client: KiwoomClient, ticker: str) -> Dict:
    """
    Get institutional investor trend.

    Args:
        client: Kiwoom API client
        ticker: Stock code

    Returns:
        {
            "ok": True,
            "data": {
                "ticker": "005930",
                "net_buy": -500000000,
                "finance": 100000000,
                "insurance": -50000000,
                "invest_trust": -100000000
            }
        }
    """
    if not ticker or not ticker.strip():
        return {
            "ok": False,
            "error": {"code": "INVALID_ARG", "msg": "종목코드가 필요합니다"},
        }

    resp = client.get_institution_trend(ticker.strip())
    if not resp.ok:
        return {"ok": False, "error": resp.error}

    data = resp.data
    return {
        "ok": True,
        "data": {
            "ticker": ticker,
            "net_buy": _safe_int(data.get("istt_net", 0)),
            "finance": _safe_int(data.get("fin_inv_net", 0)),
            "insurance": _safe_int(data.get("insur_net", 0)),
            "invest_trust": _safe_int(data.get("inv_trust_net", 0)),
        },
    }


def get_investor_summary(client: KiwoomClient, ticker: str, period: str = "1") -> Dict:
    """
    Get investor summary.

    Args:
        client: Kiwoom API client
        ticker: Stock code
        period: Period (1: Daily, 2: Weekly, 3: Monthly)

    Returns:
        {
            "ok": True,
            "data": {
                "ticker": "005930",
                "foreign_net": 7500000000,
                "institution_net": -2500000000,
                "individual_net": -5000000000
            }
        }
    """
    if not ticker or not ticker.strip():
        return {
            "ok": False,
            "error": {"code": "INVALID_ARG", "msg": "종목코드가 필요합니다"},
        }

    resp = client.get_investor_summary(ticker.strip(), period)
    if not resp.ok:
        return {"ok": False, "error": resp.error}

    data = resp.data
    return {
        "ok": True,
        "data": {
            "ticker": ticker,
            "foreign_net": _safe_int(data.get("frgn_net_sum", 0)),
            "institution_net": _safe_int(data.get("istt_net_sum", 0)),
            "individual_net": _safe_int(data.get("prsn_net_sum", 0)),
        },
    }


def _safe_int(value) -> int:
    """Safely convert value to int."""
    try:
        return int(value) if value is not None else 0
    except (ValueError, TypeError):
        return 0


def _safe_float(value) -> float:
    """Safely convert value to float."""
    try:
        return float(value) if value is not None else 0.0
    except (ValueError, TypeError):
        return 0.0
