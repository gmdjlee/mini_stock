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
    max_pages = 50  # Safety limit per market

    # Map market names to API mrkt_tp values
    # ka10099 API: 0=코스피(KOSPI), 10=코스닥(KOSDAQ)
    market_to_mrkt_tp = {
        "KOSPI": "0",
        "KOSDAQ": "10",
    }

    log_info("stock.search", "starting search loop", {"query": query, "markets": markets})

    # Fetch each market separately to ensure we search all stocks
    for market in markets:
        mrkt_tp = market_to_mrkt_tp.get(market)
        if not mrkt_tp:
            log_info("stock.search", f"Skipping unknown market: {market}", {})
            continue

        cont_yn = ""
        next_key = ""

        log_info("stock.search", f"Searching {market} stocks", {"mrkt_tp": mrkt_tp})

        for page_num in range(max_pages):
            log_info("stock.search", f"fetching {market} page {page_num + 1}", {
                "cont_yn": cont_yn,
                "next_key": next_key[:20] if next_key else ""
            })

            resp = client.get_stock_list(mrkt_tp, cont_yn=cont_yn, next_key=next_key)

            log_info("stock.search", "API response received", {
                "ok": resp.ok,
                "has_next": resp.has_next if resp.ok else None,
                "error": resp.error if not resp.ok else None
            })

            if not resp.ok:
                log_err("stock.search", "API error", {"error": resp.error})
                return {"ok": False, "error": resp.error}

            # API returns 'list' with 'code', 'name' fields
            stk_list = resp.data.get("stk_list") or resp.data.get("list") or []

            log_info("stock.search", f"{market} stock list info", {
                "list_length": len(stk_list),
                "first_items": stk_list[:3] if stk_list else []
            })

            for item in stk_list:
                ticker = item.get("code", "")
                name = item.get("name", "")

                if query in ticker or query in name.upper():
                    results.append({
                        "ticker": ticker,
                        "name": name,
                        "market": market,
                    })

                    # Early exit if we have enough results
                    if len(results) >= 50:
                        break

            log_info("stock.search", f"{market} page {page_num + 1} processed", {
                "results_so_far": len(results),
                "has_next": resp.has_next
            })

            # Stop if we have enough results or no more pages
            if len(results) >= 50 or not resp.has_next:
                break

            # Prepare for next page
            cont_yn = "Y"
            next_key = resp.next_key or ""

        # Stop if we have enough results
        if len(results) >= 50:
            break

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
    max_pages = 100  # Safety limit per market

    log_info("stock.search", "get_all fetching all stocks", {"markets": markets})

    # Map market names to API mrkt_tp values
    # ka10099 API: 0=코스피(KOSPI), 10=코스닥(KOSDAQ)
    market_to_mrkt_tp = {
        "KOSPI": "0",
        "KOSDAQ": "10",
    }

    # Fetch each market separately to ensure we get all stocks
    for market in markets:
        mrkt_tp = market_to_mrkt_tp.get(market)
        if not mrkt_tp:
            log_info("stock.search", f"Skipping unknown market: {market}", {})
            continue

        cont_yn = ""
        next_key = ""
        market_count = 0

        log_info("stock.search", f"Fetching {market} stocks", {"mrkt_tp": mrkt_tp})

        for page_num in range(max_pages):
            resp = client.get_stock_list(mrkt_tp, cont_yn=cont_yn, next_key=next_key)
            if not resp.ok:
                log_err("stock.search", f"get_all API error for {market}", {"error": resp.error})
                return {"ok": False, "error": resp.error}

            # Debug: Log response structure
            log_info("stock.search", f"get_all {market} page {page_num + 1} response", {
                "keys": list(resp.data.keys()) if resp.data else [],
                "data_sample": str(resp.data)[:500] if resp.data else "None",
                "has_next": resp.has_next,
            })

            # API returns 'stk_list' with stock data (or 'list' for backward compatibility)
            # Note: API may return None for list, so use `or []` to handle that case
            stk_list = resp.data.get("stk_list") or resp.data.get("list") or []

            log_info("stock.search", f"get_all {market} page {page_num + 1} items", {
                "list_length": len(stk_list) if stk_list else 0,
                "first_items": stk_list[:3] if stk_list else [],
            })

            for item in stk_list:
                ticker = item.get("code", "")
                name = item.get("name", "")

                results.append({
                    "ticker": ticker,
                    "name": name,
                    "market": market,
                })
                market_count += 1

            log_info("stock.search", f"get_all {market} page {page_num + 1} processed", {
                "market_count": market_count,
                "has_next": resp.has_next,
            })

            # Stop if no more pages
            if not resp.has_next:
                break

            # Prepare for next page
            cont_yn = "Y"
            next_key = resp.next_key or ""

        log_info("stock.search", f"get_all {market} complete", {"count": market_count})

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
    # '거래소' = KOSPI (유가증권시장)
    if "거래소" in market_name or "코스피" in market_name or "KOSPI" in name_upper:
        return "KOSPI"
    if "코스닥" in market_name or "KOSDAQ" in name_upper:
        return "KOSDAQ"
    return market_name or "기타"
