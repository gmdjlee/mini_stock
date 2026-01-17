"""OHLCV price data functionality."""

from dataclasses import dataclass
from typing import Dict, List

from ..client.kiwoom import KiwoomClient
from ..core import safe_int
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
        opens.append(safe_int(_get_field(item, "open_pric", "opn_prc")))
        highs.append(safe_int(_get_field(item, "high_pric", "high_prc")))
        lows.append(safe_int(_get_field(item, "low_pric", "low_prc")))
        closes.append(safe_int(_get_field(item, "cur_prc", "cls_prc")))
        volumes.append(safe_int(_get_field(item, "trde_qty", "trd_qty")))

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


def resample_to_weekly(
    dates: List[str],
    opens: List[int],
    highs: List[int],
    lows: List[int],
    closes: List[int],
    volumes: List[int],
) -> Dict:
    """
    Resample daily OHLCV data to weekly (Friday close).

    Reference logic from 추세판별.txt:
    - Open: first day's open
    - High: max of the week
    - Low: min of the week
    - Close: last day's close
    - Volume: sum of the week

    Args:
        dates: Daily dates (YYYYMMDD format, newest first)
        opens, highs, lows, closes, volumes: Daily OHLCV data

    Returns:
        Weekly OHLCV data dict with same structure
    """
    from datetime import datetime

    if not dates:
        return {
            "dates": [],
            "open": [],
            "high": [],
            "low": [],
            "close": [],
            "volume": [],
        }

    # Convert to chronological order for processing
    dates_chrono = list(reversed(dates))
    opens_chrono = list(reversed(opens))
    highs_chrono = list(reversed(highs))
    lows_chrono = list(reversed(lows))
    closes_chrono = list(reversed(closes))
    volumes_chrono = list(reversed(volumes))

    # Group by ISO week (year, week_number)
    weekly_data = {}

    for i, date_str in enumerate(dates_chrono):
        dt = datetime.strptime(date_str, "%Y%m%d")
        # ISO week: (year, week_number)
        iso_cal = dt.isocalendar()
        week_key = (iso_cal[0], iso_cal[1])

        if week_key not in weekly_data:
            weekly_data[week_key] = {
                "dates": [],
                "open": [],
                "high": [],
                "low": [],
                "close": [],
                "volume": [],
            }

        weekly_data[week_key]["dates"].append(date_str)
        weekly_data[week_key]["open"].append(opens_chrono[i])
        weekly_data[week_key]["high"].append(highs_chrono[i])
        weekly_data[week_key]["low"].append(lows_chrono[i])
        weekly_data[week_key]["close"].append(closes_chrono[i])
        weekly_data[week_key]["volume"].append(volumes_chrono[i])

    # Aggregate each week
    result_dates = []
    result_opens = []
    result_highs = []
    result_lows = []
    result_closes = []
    result_volumes = []

    for week_key in sorted(weekly_data.keys()):
        week = weekly_data[week_key]
        # Use last day of the week as the date (Friday or last trading day)
        result_dates.append(week["dates"][-1])
        result_opens.append(week["open"][0])  # First day's open
        result_highs.append(max(week["high"]))  # Week's high
        result_lows.append(min(week["low"]))  # Week's low
        result_closes.append(week["close"][-1])  # Last day's close
        result_volumes.append(sum(week["volume"]))  # Total volume

    # Reverse back to newest-first order
    return {
        "dates": list(reversed(result_dates)),
        "open": list(reversed(result_opens)),
        "high": list(reversed(result_highs)),
        "low": list(reversed(result_lows)),
        "close": list(reversed(result_closes)),
        "volume": list(reversed(result_volumes)),
    }


def resample_to_monthly(
    dates: List[str],
    opens: List[int],
    highs: List[int],
    lows: List[int],
    closes: List[int],
    volumes: List[int],
) -> Dict:
    """
    Resample daily OHLCV data to monthly.

    Args:
        dates: Daily dates (YYYYMMDD format, newest first)
        opens, highs, lows, closes, volumes: Daily OHLCV data

    Returns:
        Monthly OHLCV data dict with same structure
    """
    if not dates:
        return {
            "dates": [],
            "open": [],
            "high": [],
            "low": [],
            "close": [],
            "volume": [],
        }

    # Convert to chronological order for processing
    dates_chrono = list(reversed(dates))
    opens_chrono = list(reversed(opens))
    highs_chrono = list(reversed(highs))
    lows_chrono = list(reversed(lows))
    closes_chrono = list(reversed(closes))
    volumes_chrono = list(reversed(volumes))

    # Group by month (year, month)
    monthly_data = {}

    for i, date_str in enumerate(dates_chrono):
        month_key = (date_str[:4], date_str[4:6])  # (YYYY, MM)

        if month_key not in monthly_data:
            monthly_data[month_key] = {
                "dates": [],
                "open": [],
                "high": [],
                "low": [],
                "close": [],
                "volume": [],
            }

        monthly_data[month_key]["dates"].append(date_str)
        monthly_data[month_key]["open"].append(opens_chrono[i])
        monthly_data[month_key]["high"].append(highs_chrono[i])
        monthly_data[month_key]["low"].append(lows_chrono[i])
        monthly_data[month_key]["close"].append(closes_chrono[i])
        monthly_data[month_key]["volume"].append(volumes_chrono[i])

    # Aggregate each month
    result_dates = []
    result_opens = []
    result_highs = []
    result_lows = []
    result_closes = []
    result_volumes = []

    for month_key in sorted(monthly_data.keys()):
        month = monthly_data[month_key]
        result_dates.append(month["dates"][-1])  # Last day of month
        result_opens.append(month["open"][0])  # First day's open
        result_highs.append(max(month["high"]))  # Month's high
        result_lows.append(min(month["low"]))  # Month's low
        result_closes.append(month["close"][-1])  # Last day's close
        result_volumes.append(sum(month["volume"]))  # Total volume

    # Reverse back to newest-first order
    return {
        "dates": list(reversed(result_dates)),
        "open": list(reversed(result_opens)),
        "high": list(reversed(result_highs)),
        "low": list(reversed(result_lows)),
        "close": list(reversed(result_closes)),
        "volume": list(reversed(result_volumes)),
    }


def get_daily_resampled_to_weekly(
    client: KiwoomClient,
    ticker: str,
    days: int = 500,
    adj_price: bool = True,
) -> Dict:
    """
    Get daily data and resample to weekly (like reference code).

    This fetches daily data and resamples it to weekly intervals,
    matching the reference code's behavior using pandas resample('W-FRI').

    Args:
        client: Kiwoom API client
        ticker: Stock code
        days: Number of days of daily data to fetch
        adj_price: Use adjusted price

    Returns:
        Weekly OHLCV data (same format as get_weekly)
    """
    # Fetch daily data
    daily_result = get_daily(client, ticker, days=days, adj_price=adj_price)

    if not daily_result["ok"]:
        return daily_result

    daily = daily_result["data"]

    # Resample to weekly
    weekly = resample_to_weekly(
        daily["dates"],
        daily["open"],
        daily["high"],
        daily["low"],
        daily["close"],
        daily["volume"],
    )

    weekly["ticker"] = ticker

    log_info("stock.ohlcv", "get_daily_resampled_to_weekly complete", {
        "ticker": ticker,
        "daily_count": len(daily["dates"]),
        "weekly_count": len(weekly["dates"]),
    })

    return {"ok": True, "data": weekly}
