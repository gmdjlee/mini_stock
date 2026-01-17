"""DeMark TD Setup indicator (Custom version).

Custom TD Setup rules (based on reference):
- Sell Setup: Close(t) > Close(t-4) 연속이면 +1, 아니면 0으로 리셋
- Buy Setup: Close(t) < Close(t-2) 연속이면 +1, 아니면 0으로 리셋
- Sell과 Buy는 독립적으로 카운트 (동시에 값이 있을 수 있음)
- 카운트 한도 없음 (무한 증가)

Reference: Shows Daily, Weekly, and Monthly TD Setup charts.
"""

from typing import Dict, List, Literal

from ..client.kiwoom import KiwoomClient
from ..core.log import log_info
from ..stock import ohlcv


def calc(
    client: KiwoomClient,
    ticker: str,
    days: int = 180,
    timeframe: Literal["daily", "weekly", "monthly"] = "daily",
) -> Dict:
    """
    Calculate Custom DeMark TD Setup.

    Custom TD Setup Rules:
    - Sell Setup: Close > Close[4] 연속이면 +1, 아니면 0으로 리셋 (상승 피로)
    - Buy Setup: Close < Close[2] 연속이면 +1, 아니면 0으로 리셋 (하락 피로)
    - Sell과 Buy는 독립적으로 카운트 (동시에 값이 있을 수 있음)

    Args:
        client: Kiwoom API client
        ticker: Stock code
        days: Number of periods for result
        timeframe: "daily", "weekly", or "monthly"

    Returns:
        {
            "ok": True,
            "data": {
                "ticker": "005930",
                "timeframe": "daily",
                "dates": ["20250110", ...],
                "close": [55000, ...],  # Close prices for charting
                "sell_setup": [0, 1, 2, 3, ...],  # Sell 카운트 (무한)
                "buy_setup": [0, 0, 1, 2, ...],   # Buy 카운트 (무한)
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

    # Fetch OHLCV data based on timeframe
    fetch_extra = 10
    if timeframe == "weekly":
        # Fetch daily data and resample to weekly (like reference code)
        fetch_days = (days + fetch_extra) * 7
        ohlcv_result = ohlcv.get_daily_resampled_to_weekly(client, ticker, days=fetch_days)
    elif timeframe == "monthly":
        # Fetch daily data and resample to monthly (like reference code)
        fetch_days = (days + fetch_extra) * 22  # ~22 trading days per month
        daily_result = ohlcv.get_daily(client, ticker, days=fetch_days)
        if not daily_result["ok"]:
            ohlcv_result = daily_result
        else:
            daily = daily_result["data"]
            monthly = ohlcv.resample_to_monthly(
                daily["dates"], daily["open"], daily["high"],
                daily["low"], daily["close"], daily["volume"],
            )
            monthly["ticker"] = ticker
            ohlcv_result = {"ok": True, "data": monthly}
    else:
        ohlcv_result = ohlcv.get_daily(client, ticker, days=days + fetch_extra)

    if not ohlcv_result["ok"]:
        return ohlcv_result

    data = ohlcv_result["data"]
    closes = data["close"]
    dates = data["dates"]

    if len(closes) < 5:
        return {
            "ok": False,
            "error": {"code": "NO_DATA", "msg": "데이터가 충분하지 않습니다 (최소 5 필요)"},
        }

    # Calculate TD Setup
    sell_setup, buy_setup = _calc_td_setup(closes)

    # Trim to requested periods
    trim_len = min(days, len(dates) - 4)
    result = {
        "ticker": ticker,
        "timeframe": timeframe,
        "dates": dates[:trim_len],
        "close": closes[:trim_len],
        "sell_setup": sell_setup[:trim_len],
        "buy_setup": buy_setup[:trim_len],
    }

    log_info("indicator.demark", "calc complete", {"ticker": ticker, "timeframe": timeframe, "periods": trim_len})

    return {"ok": True, "data": result}


def calc_from_ohlcv(
    ticker: str,
    dates: List[str],
    closes: List[int],
    timeframe: str = "daily",
) -> Dict:
    """
    Calculate Custom DeMark TD Setup from OHLCV data directly.

    Args:
        ticker: Stock code
        dates: Date list
        closes: Close prices
        timeframe: "daily", "weekly", or "monthly" (for labeling)

    Returns:
        Same format as calc()
    """
    if len(closes) < 5:
        return {
            "ok": False,
            "error": {"code": "NO_DATA", "msg": "데이터가 충분하지 않습니다 (최소 5 필요)"},
        }

    sell_setup, buy_setup = _calc_td_setup(closes)

    result = {
        "ticker": ticker,
        "timeframe": timeframe,
        "dates": dates,
        "close": closes,
        "sell_setup": sell_setup,
        "buy_setup": buy_setup,
    }

    return {"ok": True, "data": result}


def _calc_td_setup(closes: List[int]) -> tuple:
    """
    Calculate Custom TD Setup counts.

    Custom TD Setup Rules:
    - Sell Setup: Close > Close[4] 연속이면 +1, 아니면 0으로 리셋
    - Buy Setup: Close < Close[2] 연속이면 +1, 아니면 0으로 리셋
    - 둘은 독립적으로 계산 (동시에 값이 있을 수 있음)

    Note: Data is in reverse order (newest first)

    Args:
        closes: Close prices (newest first)

    Returns:
        Tuple of (sell_setup, buy_setup)
    """
    n = len(closes)
    sell_setup = [0] * n
    buy_setup = [0] * n

    # Process in chronological order (reverse the list)
    closes_chrono = list(reversed(closes))
    sell_chrono = [0] * n
    buy_chrono = [0] * n

    for i in range(n):
        # Sell Setup: 4일 전보다 위에 있으면 카운트 증가
        if i >= 4 and closes_chrono[i] > closes_chrono[i - 4]:
            sell_chrono[i] = sell_chrono[i - 1] + 1
        else:
            sell_chrono[i] = 0

        # Buy Setup: 2일 전보다 아래 있으면 카운트 증가
        if i >= 2 and closes_chrono[i] < closes_chrono[i - 2]:
            buy_chrono[i] = buy_chrono[i - 1] + 1
        else:
            buy_chrono[i] = 0

    # Reverse back to newest-first order
    sell_setup = list(reversed(sell_chrono))
    buy_setup = list(reversed(buy_chrono))

    return sell_setup, buy_setup


def get_active_setups(
    sell_setup: List[int],
    buy_setup: List[int],
    dates: List[str],
) -> Dict:
    """
    Get summary of current TD Setup status.

    Args:
        sell_setup: Sell setup count list
        buy_setup: Buy setup count list
        dates: Date list

    Returns:
        {
            "current_sell": 5,
            "current_buy": 3,
            "max_sell": 12,
            "max_buy": 8,
            "recent_setups": [
                {"date": "20250110", "sell": 5, "buy": 3},
                ...
            ]
        }
    """
    result = {
        "current_sell": 0,
        "current_buy": 0,
        "max_sell": 0,
        "max_buy": 0,
        "recent_setups": [],
    }

    if not sell_setup or not buy_setup:
        return result

    # Current status (newest first)
    result["current_sell"] = sell_setup[0]
    result["current_buy"] = buy_setup[0]

    # Max values
    result["max_sell"] = max(sell_setup) if sell_setup else 0
    result["max_buy"] = max(buy_setup) if buy_setup else 0

    # List recent setups (last 20 bars)
    for i in range(min(20, len(sell_setup))):
        if sell_setup[i] > 0 or buy_setup[i] > 0:
            result["recent_setups"].append({
                "date": dates[i] if i < len(dates) else "",
                "sell": sell_setup[i],
                "buy": buy_setup[i],
            })

    return result
