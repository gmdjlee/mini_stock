"""DeMark TD Sequential indicator (TD Setup)."""

from typing import Dict, List, Optional

from ..client.kiwoom import KiwoomClient
from ..core.log import log_info
from ..stock import ohlcv


def calc(
    client: KiwoomClient,
    ticker: str,
    days: int = 180,
) -> Dict:
    """
    Calculate DeMark TD Sequential Setup.

    TD Sequential consists of:
    1. TD Setup: 9 consecutive closes compared to close 4 bars earlier
       - Buy Setup: 9 consecutive closes < close[4]
       - Sell Setup: 9 consecutive closes > close[4]

    2. TD Countdown: Follows completed Setup (not implemented here)

    Args:
        client: Kiwoom API client
        ticker: Stock code
        days: Number of days for result

    Returns:
        {
            "ok": True,
            "data": {
                "ticker": "005930",
                "dates": ["20250110", ...],
                "setup_count": [0, 1, 2, ..., 9, 0, ...],  # 1-9 or 0
                "setup_type": ["none", "buy", "sell", ...],
                "setup_complete": [False, False, ..., True, False, ...],
                "perfected": [False, ..., True, ...],  # TD Perfected Setup
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

    # Fetch OHLCV data with extra days for lookback
    fetch_days = days + 20
    ohlcv_result = ohlcv.get_daily(client, ticker, days=fetch_days)

    if not ohlcv_result["ok"]:
        return ohlcv_result

    data = ohlcv_result["data"]
    closes = data["close"]
    highs = data["high"]
    lows = data["low"]
    dates = data["dates"]

    if len(closes) < 14:
        return {
            "ok": False,
            "error": {"code": "NO_DATA", "msg": "데이터가 충분하지 않습니다 (최소 14일 필요)"},
        }

    # Calculate TD Setup
    setup_count, setup_type, setup_complete = _calc_td_setup(closes)

    # Calculate Perfected Setup
    perfected = _calc_perfected(closes, highs, lows, setup_count, setup_type)

    # Trim to requested days
    trim_len = min(days, len(dates) - 4)
    result = {
        "ticker": ticker,
        "dates": dates[:trim_len],
        "setup_count": setup_count[:trim_len],
        "setup_type": setup_type[:trim_len],
        "setup_complete": setup_complete[:trim_len],
        "perfected": perfected[:trim_len],
    }

    log_info("indicator.demark", "calc complete", {"ticker": ticker, "days": trim_len})

    return {"ok": True, "data": result}


def calc_from_ohlcv(
    ticker: str,
    dates: List[str],
    closes: List[int],
    highs: List[int],
    lows: List[int],
) -> Dict:
    """
    Calculate DeMark TD Setup from OHLCV data directly.

    Args:
        ticker: Stock code
        dates: Date list
        closes: Close prices
        highs: High prices
        lows: Low prices

    Returns:
        Same format as calc()
    """
    if len(closes) < 14:
        return {
            "ok": False,
            "error": {"code": "NO_DATA", "msg": "데이터가 충분하지 않습니다 (최소 14일 필요)"},
        }

    setup_count, setup_type, setup_complete = _calc_td_setup(closes)
    perfected = _calc_perfected(closes, highs, lows, setup_count, setup_type)

    result = {
        "ticker": ticker,
        "dates": dates,
        "setup_count": setup_count,
        "setup_type": setup_type,
        "setup_complete": setup_complete,
        "perfected": perfected,
    }

    return {"ok": True, "data": result}


def _calc_td_setup(closes: List[int]) -> tuple:
    """
    Calculate TD Setup counts.

    TD Setup Rules:
    - Compare current close to close 4 bars earlier
    - Buy Setup: Close < Close[4] for 9 consecutive bars
    - Sell Setup: Close > Close[4] for 9 consecutive bars
    - Setup resets when condition breaks or reaches 9

    Note: Data is in reverse order (newest first)

    Args:
        closes: Close prices (newest first)

    Returns:
        Tuple of (setup_count, setup_type, setup_complete)
    """
    n = len(closes)
    setup_count = [0] * n
    setup_type = ["none"] * n
    setup_complete = [False] * n

    # Need at least 4 bars of lookback
    if n < 5:
        return setup_count, setup_type, setup_complete

    # Process in chronological order (reverse the list)
    closes_chrono = list(reversed(closes))
    counts_chrono = [0] * n
    types_chrono = ["none"] * n
    complete_chrono = [False] * n

    current_count = 0
    current_type = "none"

    for i in range(4, n):
        prev_close = closes_chrono[i - 4]
        curr_close = closes_chrono[i]

        if curr_close < prev_close:
            # Buy setup condition
            if current_type == "buy":
                current_count += 1
            else:
                current_type = "buy"
                current_count = 1
        elif curr_close > prev_close:
            # Sell setup condition
            if current_type == "sell":
                current_count += 1
            else:
                current_type = "sell"
                current_count = 1
        else:
            # Equal - continue current setup
            if current_type != "none":
                current_count += 1

        # Cap at 9 and mark complete
        if current_count >= 9:
            counts_chrono[i] = 9
            types_chrono[i] = current_type
            complete_chrono[i] = True
            # Reset after completion
            current_count = 0
            current_type = "none"
        else:
            counts_chrono[i] = current_count
            types_chrono[i] = current_type if current_count > 0 else "none"

    # Reverse back to newest-first order
    setup_count = list(reversed(counts_chrono))
    setup_type = list(reversed(types_chrono))
    setup_complete = list(reversed(complete_chrono))

    return setup_count, setup_type, setup_complete


def _calc_perfected(
    closes: List[int],
    highs: List[int],
    lows: List[int],
    setup_count: List[int],
    setup_type: List[str],
) -> List[bool]:
    """
    Check for TD Perfected Setup.

    Perfected Buy Setup:
    - Setup 9 is complete
    - Low of bar 8 or 9 is less than lows of bars 6 and 7

    Perfected Sell Setup:
    - Setup 9 is complete
    - High of bar 8 or 9 is greater than highs of bars 6 and 7

    Args:
        closes: Close prices
        highs: High prices
        lows: Low prices
        setup_count: Setup count for each bar
        setup_type: Setup type for each bar

    Returns:
        Perfected flags for each bar
    """
    n = len(closes)
    perfected = [False] * n

    # Process in chronological order
    closes_chrono = list(reversed(closes))
    highs_chrono = list(reversed(highs))
    lows_chrono = list(reversed(lows))
    counts_chrono = list(reversed(setup_count))
    types_chrono = list(reversed(setup_type))
    perfected_chrono = [False] * n

    for i in range(8, n):
        if counts_chrono[i] != 9:
            continue

        if types_chrono[i] == "buy":
            # Check if low of bar 8 or 9 < lows of bars 6 and 7
            low_8 = lows_chrono[i - 1] if i >= 1 else float("inf")
            low_9 = lows_chrono[i]
            low_6 = lows_chrono[i - 3] if i >= 3 else 0
            low_7 = lows_chrono[i - 2] if i >= 2 else 0

            min_low_8_9 = min(low_8, low_9)
            if min_low_8_9 < low_6 and min_low_8_9 < low_7:
                perfected_chrono[i] = True

        elif types_chrono[i] == "sell":
            # Check if high of bar 8 or 9 > highs of bars 6 and 7
            high_8 = highs_chrono[i - 1] if i >= 1 else 0
            high_9 = highs_chrono[i]
            high_6 = highs_chrono[i - 3] if i >= 3 else float("inf")
            high_7 = highs_chrono[i - 2] if i >= 2 else float("inf")

            max_high_8_9 = max(high_8, high_9)
            if max_high_8_9 > high_6 and max_high_8_9 > high_7:
                perfected_chrono[i] = True

    perfected = list(reversed(perfected_chrono))
    return perfected


def get_active_setups(
    setup_count: List[int],
    setup_type: List[str],
    setup_complete: List[bool],
    dates: List[str],
) -> Dict:
    """
    Get summary of active and completed setups.

    Args:
        setup_count: Setup count list
        setup_type: Setup type list
        setup_complete: Setup complete flags
        dates: Date list

    Returns:
        {
            "current_count": 5,
            "current_type": "buy",
            "last_complete": {
                "type": "sell",
                "date": "20250105",
            },
            "active_setups": [
                {"date": "20250110", "count": 5, "type": "buy"},
                ...
            ]
        }
    """
    result = {
        "current_count": 0,
        "current_type": "none",
        "last_complete": None,
        "active_setups": [],
    }

    if not setup_count:
        return result

    # Current status
    result["current_count"] = setup_count[0]
    result["current_type"] = setup_type[0]

    # Find last completed setup
    for i in range(len(setup_complete)):
        if setup_complete[i]:
            result["last_complete"] = {
                "type": setup_type[i],
                "date": dates[i] if i < len(dates) else "",
            }
            break

    # List active setups (count > 0)
    for i in range(min(20, len(setup_count))):  # Last 20 bars
        if setup_count[i] > 0:
            result["active_setups"].append({
                "date": dates[i] if i < len(dates) else "",
                "count": setup_count[i],
                "type": setup_type[i],
            })

    return result
