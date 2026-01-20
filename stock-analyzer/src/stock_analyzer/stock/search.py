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


# Default markets to include (KOSPI and KOSDAQ only)
DEFAULT_MARKETS = ["KOSPI", "KOSDAQ"]


def search(
    client: KiwoomClient,
    query: str,
    markets: Optional[List[str]] = None,
) -> Dict:
    """
    Search stocks by name or code.

    Args:
        client: Kiwoom API client
        query: Search query (name or code)
        markets: List of markets to include (default: ["KOSPI", "KOSDAQ"])

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
    if markets is None:
        markets = DEFAULT_MARKETS

    log_info("stock.search", "search started", {"query": query, "markets": markets})

    if not query or not query.strip():
        log_err("stock.search", "empty query", {"query": query})
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

    log_info("stock.search", "starting pagination loop", {"query": query, "max_pages": max_pages})

    for page_num in range(max_pages):
        log_info("stock.search", f"fetching page {page_num + 1}", {
            "cont_yn": cont_yn,
            "next_key": next_key[:20] if next_key else ""
        })

        resp = client.get_stock_list(cont_yn=cont_yn, next_key=next_key)

        log_info("stock.search", "API response received", {
            "ok": resp.ok,
            "has_next": resp.has_next if resp.ok else None,
            "error": resp.error if not resp.ok else None
        })

        if not resp.ok:
            log_err("stock.search", "API error", {"error": resp.error})
            return {"ok": False, "error": resp.error}

        # Log raw response data keys for debugging
        log_info("stock.search", "response data keys", {
            "keys": list(resp.data.keys()) if resp.data else [],
            "data_sample": str(resp.data)[:500] if resp.data else "None"
        })

        # API returns 'list' with 'code', 'name', 'marketName' fields
        stk_list = resp.data.get("list", [])

        log_info("stock.search", "stock list info", {
            "list_length": len(stk_list),
            "first_items": stk_list[:3] if stk_list else []
        })

        for item in stk_list:
            ticker = item.get("code", "")
            name = item.get("name", "")
            market = _get_market_name(item.get("marketName", ""))

            # Filter by market (KOSPI/KOSDAQ only by default)
            if market not in markets:
                continue

            if query in ticker or query in name.upper():
                results.append({
                    "ticker": ticker,
                    "name": name,
                    "market": market,
                })

                # Early exit if we have enough results
                if len(results) >= 50:
                    break

        log_info("stock.search", f"page {page_num + 1} processed", {
            "results_so_far": len(results),
            "has_next": resp.has_next
        })

        # Stop if we have enough results or no more pages
        if len(results) >= 50 or not resp.has_next:
            break

        # Prepare for next page
        cont_yn = "Y"
        next_key = resp.next_key or ""

    log_info("stock.search", "search complete", {"query": query, "count": len(results)})

    return {"ok": True, "data": results[:50]}  # Max 50 results


def get_all(
    client: KiwoomClient,
    markets: Optional[List[str]] = None,
) -> Dict:
    """
    Get all stocks.

    Args:
        client: Kiwoom API client
        markets: List of markets to include (default: ["KOSPI", "KOSDAQ"])

    Returns:
        {
            "ok": True,
            "data": [
                {"ticker": "005930", "name": "삼성전자", "market": "KOSPI"},
                ...
            ]
        }
    """
    if markets is None:
        markets = DEFAULT_MARKETS

    results = []
    cont_yn = ""
    next_key = ""
    max_pages = 100  # Safety limit

    for _ in range(max_pages):
        # Fetch all markets from API, filter locally
        resp = client.get_stock_list("0", cont_yn=cont_yn, next_key=next_key)
        if not resp.ok:
            return {"ok": False, "error": resp.error}

        # API returns 'list' with 'code', 'name', 'marketName' fields
        for item in resp.data.get("list", []):
            market = _get_market_name(item.get("marketName", ""))

            # Filter by market (KOSPI/KOSDAQ only by default)
            if market not in markets:
                continue

            results.append({
                "ticker": item.get("code", ""),
                "name": item.get("name", ""),
                "market": market,
            })

        # Stop if no more pages
        if not resp.has_next:
            break

        # Prepare for next page
        cont_yn = "Y"
        next_key = resp.next_key or ""

    log_info("stock.search", "get_all complete", {"markets": markets, "count": len(results)})

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
