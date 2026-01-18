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

    # Filter by query
    query = query.strip().upper()
    results = []

    # Get stock list with pagination
    cont_yn = ""
    next_key = ""
    max_pages = 50  # Safety limit

    for _ in range(max_pages):
        resp = client.get_stock_list(cont_yn=cont_yn, next_key=next_key)
        if not resp.ok:
            return {"ok": False, "error": resp.error}

        # API returns 'list' with 'code', 'name', 'marketName' fields
        stk_list = resp.data.get("list", [])
        for item in stk_list:
            ticker = item.get("code", "")
            name = item.get("name", "")

            if query in ticker or query in name.upper():
                results.append({
                    "ticker": ticker,
                    "name": name,
                    "market": _get_market_name(item.get("marketName", "")),
                })

                # Early exit if we have enough results
                if len(results) >= 50:
                    break

        # Stop if we have enough results or no more pages
        if len(results) >= 50 or not resp.has_next:
            break

        # Prepare for next page
        cont_yn = "Y"
        next_key = resp.next_key or ""

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
    results = []
    cont_yn = ""
    next_key = ""
    max_pages = 100  # Safety limit

    for _ in range(max_pages):
        resp = client.get_stock_list(market, cont_yn=cont_yn, next_key=next_key)
        if not resp.ok:
            return {"ok": False, "error": resp.error}

        # API returns 'list' with 'code', 'name', 'marketName' fields
        for item in resp.data.get("list", []):
            results.append({
                "ticker": item.get("code", ""),
                "name": item.get("name", ""),
                "market": _get_market_name(item.get("marketName", "")),
            })

        # Stop if no more pages
        if not resp.has_next:
            break

        # Prepare for next page
        cont_yn = "Y"
        next_key = resp.next_key or ""

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


def _get_market_name(market_name: str) -> str:
    """Convert market name to standardized format."""
    if not market_name:
        return "기타"
    name_upper = market_name.upper()
    if "코스피" in market_name or "KOSPI" in name_upper:
        return "KOSPI"
    if "코스닥" in market_name or "KOSDAQ" in name_upper:
        return "KOSDAQ"
    return market_name or "기타"
