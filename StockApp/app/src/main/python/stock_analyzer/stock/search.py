"""Stock search functionality."""

from dataclasses import dataclass
from typing import Dict, List, Optional

from ..client.kiwoom import KiwoomClient
from ..core.log import log_err, log_info


@dataclass
class StockInfo:
    """Stock information."""

    ticker: str
    name: str
    market: str  # KOSPI/KOSDAQ


def search(client: KiwoomClient, query: str) -> Dict:
    """
    Search stocks by name or code.

    Args:
        client: Kiwoom API client
        query: Search query (name or code)

    Returns:
        {
            "ok": True,
            "data": [
                {"ticker": "005930", "name": "삼성전자", "market": "KOSPI"},
                ...
            ]
        }

    Errors:
        - INVALID_ARG: Invalid argument
        - API_ERROR: API call failed
    """
    if not query or not query.strip():
        return {
            "ok": False,
            "error": {"code": "INVALID_ARG", "msg": "검색어가 필요합니다"},
        }

    # Get full stock list
    resp = client.get_stock_list()
    if not resp.ok:
        return {"ok": False, "error": resp.error}

    # Filter by query
    query = query.strip().upper()
    results = []

    stk_list = resp.data.get("stk_list", [])
    for item in stk_list:
        ticker = item.get("stk_cd", "")
        name = item.get("stk_nm", "")

        if query in ticker or query in name.upper():
            results.append({
                "ticker": ticker,
                "name": name,
                "market": _get_market_name(item.get("mrkt_tp", "")),
            })

    log_info("stock.search", "search complete", {"query": query, "count": len(results)})

    return {"ok": True, "data": results[:50]}  # Max 50 results


def get_all(client: KiwoomClient, market: str = "0") -> Dict:
    """
    Get all stocks.

    Args:
        client: Kiwoom API client
        market: Market type (0: All, 1: KOSPI, 2: KOSDAQ)

    Returns:
        {
            "ok": True,
            "data": [
                {"ticker": "005930", "name": "삼성전자", "market": "KOSPI"},
                ...
            ]
        }
    """
    resp = client.get_stock_list(market)
    if not resp.ok:
        return {"ok": False, "error": resp.error}

    results = []
    for item in resp.data.get("stk_list", []):
        results.append({
            "ticker": item.get("stk_cd", ""),
            "name": item.get("stk_nm", ""),
            "market": _get_market_name(item.get("mrkt_tp", "")),
        })

    log_info("stock.search", "get_all complete", {"market": market, "count": len(results)})

    return {"ok": True, "data": results}


def get_name(client: KiwoomClient, ticker: str) -> Optional[str]:
    """
    Get stock name by ticker.

    Args:
        client: Kiwoom API client
        ticker: Stock code

    Returns:
        Stock name or None
    """
    resp = client.get_stock_info(ticker)
    if resp.ok:
        return resp.data.get("stk_nm")
    return None


def get_info(client: KiwoomClient, ticker: str) -> Dict:
    """
    Get stock basic info.

    Args:
        client: Kiwoom API client
        ticker: Stock code

    Returns:
        {
            "ok": True,
            "data": {
                "ticker": "005930",
                "name": "삼성전자",
                "price": 55000,
                "mcap": 328000000000000,
                "per": 8.5,
                "pbr": 1.2
            }
        }
    """
    if not ticker or not ticker.strip():
        return {
            "ok": False,
            "error": {"code": "INVALID_ARG", "msg": "종목코드가 필요합니다"},
        }

    resp = client.get_stock_info(ticker.strip())
    if not resp.ok:
        return {"ok": False, "error": resp.error}

    data = resp.data
    return {
        "ok": True,
        "data": {
            "ticker": data.get("stk_cd", ticker),
            "name": data.get("stk_nm", ""),
            "price": _to_int(data.get("cur_prc", 0)),
            "mcap": _to_int(data.get("mrkt_tot_amt", 0)),
            "per": _to_float(data.get("per", 0)),
            "pbr": _to_float(data.get("pbr", 0)),
        },
    }


def _to_int(value) -> int:
    """Convert value to int safely."""
    try:
        return int(value)
    except (ValueError, TypeError):
        return 0


def _to_float(value) -> float:
    """Convert value to float safely."""
    try:
        return float(value)
    except (ValueError, TypeError):
        return 0.0


def _get_market_name(market_tp: str) -> str:
    """Convert market type code to name."""
    return {"1": "KOSPI", "2": "KOSDAQ"}.get(market_tp, "기타")
