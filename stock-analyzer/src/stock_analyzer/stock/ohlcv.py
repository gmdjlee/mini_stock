"""OHLCV price data functionality."""

from dataclasses import dataclass
from typing import Dict, List

from ..client.kiwoom import KiwoomClient
from ..core.date import days_ago, today_str
from ..core.log import log_info


@dataclass
class OhlcvData:
    """OHLCV price data."""

    ticker: str
    dates: List[str]
    open: List[int]
    high: List[int]
    low: List[int]
    close: List[int]
    volume: List[int]


def get_daily(
    client: KiwoomClient,
    ticker: str,
    start_date: str = None,
    end_date: str = None,
    days: int = 180,
    adj_price: bool = True,
) -> Dict:
    """
    Get daily OHLCV data.

    Args:
        client: Kiwoom API client
        ticker: Stock code
        start_date: Start date (YYYYMMDD), defaults to `days` ago
        end_date: End date (YYYYMMDD), defaults to today
        days: Number of days (used if start_date not provided)
        adj_price: Use adjusted price

    Returns:
        {
            "ok": True,
            "data": {
                "ticker": "005930",
                "dates": ["20250102", ...],
                "open": [54000, ...],
                "high": [55500, ...],
                "low": [53800, ...],
                "close": [55000, ...],
                "volume": [15000000, ...]
            }
        }

    Errors:
        - INVALID_ARG: Invalid argument
        - NO_DATA: No data available
        - API_ERROR: API call failed
    """
    if not ticker or not ticker.strip():
        return {
            "ok": False,
            "error": {"code": "INVALID_ARG", "msg": "종목코드가 필요합니다"},
        }

    ticker = ticker.strip()
    end_date = end_date or today_str()
    start_date = start_date or days_ago(days)

    resp = client.get_daily_chart(
        ticker,
        start_date,
        end_date,
        adj_price="1" if adj_price else "0",
    )

    if not resp.ok:
        return {"ok": False, "error": resp.error}

    # API returns data in 'stk_dt_pole_chart_qry' field for daily chart
    chart_data = resp.data.get("stk_dt_pole_chart_qry", []) or resp.data.get("list", [])
    if not chart_data:
        return {
            "ok": False,
            "error": {"code": "NO_DATA", "msg": "차트 데이터가 없습니다"},
        }

    result = _parse_chart_data(ticker, chart_data)

    log_info("stock.ohlcv", "get_daily complete", {"ticker": ticker, "count": len(result["dates"])})

    return {"ok": True, "data": result}


def get_weekly(
    client: KiwoomClient,
    ticker: str,
    start_date: str = None,
    end_date: str = None,
    weeks: int = 52,
    adj_price: bool = True,
) -> Dict:
    """
    Get weekly OHLCV data.

    Args:
        client: Kiwoom API client
        ticker: Stock code
        start_date: Start date (YYYYMMDD), defaults to `weeks` weeks ago
        end_date: End date (YYYYMMDD), defaults to today
        weeks: Number of weeks (used if start_date not provided)
        adj_price: Use adjusted price

    Returns:
        Same format as get_daily
    """
    if not ticker or not ticker.strip():
        return {
            "ok": False,
            "error": {"code": "INVALID_ARG", "msg": "종목코드가 필요합니다"},
        }

    ticker = ticker.strip()
    end_date = end_date or today_str()
    start_date = start_date or days_ago(weeks * 7)

    resp = client.get_weekly_chart(
        ticker,
        start_date,
        end_date,
        adj_price="1" if adj_price else "0",
    )

    if not resp.ok:
        return {"ok": False, "error": resp.error}

    # API returns data in 'stk_stk_pole_chart_qry' field for weekly chart
    chart_data = resp.data.get("stk_stk_pole_chart_qry", []) or resp.data.get("list", [])
    if not chart_data:
        return {
            "ok": False,
            "error": {"code": "NO_DATA", "msg": "차트 데이터가 없습니다"},
        }

    result = _parse_chart_data(ticker, chart_data)

    log_info("stock.ohlcv", "get_weekly complete", {"ticker": ticker, "count": len(result["dates"])})

    return {"ok": True, "data": result}


def get_monthly(
    client: KiwoomClient,
    ticker: str,
    start_date: str = None,
    end_date: str = None,
    months: int = 24,
    adj_price: bool = True,
) -> Dict:
    """
    Get monthly OHLCV data.

    Args:
        client: Kiwoom API client
        ticker: Stock code
        start_date: Start date (YYYYMMDD), defaults to `months` months ago
        end_date: End date (YYYYMMDD), defaults to today
        months: Number of months (used if start_date not provided)
        adj_price: Use adjusted price

    Returns:
        Same format as get_daily
    """
    if not ticker or not ticker.strip():
        return {
            "ok": False,
            "error": {"code": "INVALID_ARG", "msg": "종목코드가 필요합니다"},
        }

    ticker = ticker.strip()
    end_date = end_date or today_str()
    start_date = start_date or days_ago(months * 30)

    resp = client.get_monthly_chart(
        ticker,
        start_date,
        end_date,
        adj_price="1" if adj_price else "0",
    )

    if not resp.ok:
        return {"ok": False, "error": resp.error}

    # API returns data in 'stk_mth_pole_chart_qry' field for monthly chart
    chart_data = resp.data.get("stk_mth_pole_chart_qry", []) or resp.data.get("list", [])
    if not chart_data:
        return {
            "ok": False,
            "error": {"code": "NO_DATA", "msg": "차트 데이터가 없습니다"},
        }

    result = _parse_chart_data(ticker, chart_data)

    log_info("stock.ohlcv", "get_monthly complete", {"ticker": ticker, "count": len(result["dates"])})

    return {"ok": True, "data": result}


def _parse_chart_data(ticker: str, chart_data: List[Dict]) -> Dict:
    """Parse chart data from API response."""
    dates = []
    opens = []
    highs = []
    lows = []
    closes = []
    volumes = []

    for item in chart_data:
        dates.append(item.get("dt", ""))
        # Support both official API field names and legacy field names
        opens.append(_safe_int(_get_field(item, "open_pric", "opn_prc")))
        highs.append(_safe_int(_get_field(item, "high_pric", "high_prc")))
        lows.append(_safe_int(_get_field(item, "low_pric", "low_prc")))
        closes.append(_safe_int(_get_field(item, "cur_prc", "cls_prc")))
        volumes.append(_safe_int(_get_field(item, "trde_qty", "trd_qty")))

    return {
        "ticker": ticker,
        "dates": dates,
        "open": opens,
        "high": highs,
        "low": lows,
        "close": closes,
        "volume": volumes,
    }


def _get_field(item: Dict, *field_names):
    """Get value from item using multiple possible field names."""
    for name in field_names:
        if name in item and item[name] is not None:
            return item[name]
    return 0


def _safe_int(value) -> int:
    """Safely convert value to int."""
    try:
        return int(value) if value is not None else 0
    except (ValueError, TypeError):
        return 0
