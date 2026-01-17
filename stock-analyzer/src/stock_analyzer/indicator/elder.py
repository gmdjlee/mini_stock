"""Elder Impulse System indicator (EMA13, MACD).

Reference: Uses weekly data with EMA13 and MACD(12,26,9).
Impulse color is determined by the direction (slope) of EMA13 and MACD histogram.
"""

from typing import Dict, List, Literal, Optional

from ..client.kiwoom import KiwoomClient
from ..core.log import log_info
from ..stock import ohlcv


def calc(
    client: KiwoomClient,
    ticker: str,
    days: int = 180,
    timeframe: Literal["daily", "weekly"] = "daily",
) -> Dict:
    """
    Calculate Elder Impulse System.

    Elder Impulse System uses:
    - EMA13 (13-period Exponential Moving Average)
    - MACD Histogram (MACD Line - Signal Line)

    Color determination (based on slope/direction):
    - Green (bull): EMA13 slope > 0 AND MACD Histogram slope > 0
    - Red (bear): EMA13 slope < 0 AND MACD Histogram slope < 0
    - Blue (neutral): Otherwise (mixed signals)

    Args:
        client: Kiwoom API client
        ticker: Stock code
        days: Number of days/weeks for result
        timeframe: "daily" or "weekly" (reference uses weekly)

    Returns:
        {
            "ok": True,
            "data": {
                "ticker": "005930",
                "timeframe": "weekly",
                "dates": ["20250110", ...],
                "color": ["green", "blue", "red", ...],  # impulse color
                "ema13": [55000, 54800, ...],
                "macd_line": [500, 480, ...],
                "signal_line": [450, 460, ...],
                "macd_hist": [50, 20, ...],
                "ema13_slope": [100, -50, ...],  # EMA13 difference from previous
                "hist_slope": [10, -5, ...],  # Histogram difference from previous
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
    if timeframe == "weekly":
        # Fetch extra weeks for EMA/MACD warmup
        fetch_weeks = days + 50
        ohlcv_result = ohlcv.get_weekly(client, ticker, weeks=fetch_weeks)
    else:
        # Fetch extra days for indicator warmup
        fetch_days = days + 50
        ohlcv_result = ohlcv.get_daily(client, ticker, days=fetch_days)

    if not ohlcv_result["ok"]:
        return ohlcv_result

    data = ohlcv_result["data"]
    closes = data["close"]
    dates = data["dates"]

    min_periods = 35
    if len(closes) < min_periods:
        return {
            "ok": False,
            "error": {"code": "NO_DATA", "msg": f"데이터가 충분하지 않습니다 (최소 {min_periods} 필요)"},
        }

    # Calculate indicators using reference-style EMA (adjust=False, no SMA init)
    ema13 = _calc_ema_no_sma(closes, 13)
    macd_line, signal_line, macd_hist = _calc_macd_no_sma(closes)

    # Calculate slopes (diff from previous value) - this is what reference uses
    ema13_slope = _calc_slope(ema13)
    hist_slope = _calc_slope(macd_hist)

    # Determine impulse colors based on slopes
    colors = _calc_impulse_color_by_slope(ema13_slope, hist_slope)

    # Trim to requested days/weeks
    trim_len = min(days, len(dates) - 34)
    result = {
        "ticker": ticker,
        "timeframe": timeframe,
        "dates": dates[:trim_len],
        "color": colors[:trim_len],
        "ema13": ema13[:trim_len],
        "macd_line": macd_line[:trim_len],
        "signal_line": signal_line[:trim_len],
        "macd_hist": macd_hist[:trim_len],
        "ema13_slope": ema13_slope[:trim_len],
        "hist_slope": hist_slope[:trim_len],
    }

    log_info("indicator.elder", "calc complete", {"ticker": ticker, "timeframe": timeframe, "periods": trim_len})

    return {"ok": True, "data": result}


def calc_from_ohlcv(
    ticker: str,
    dates: List[str],
    closes: List[int],
    timeframe: str = "daily",
) -> Dict:
    """
    Calculate Elder Impulse from OHLCV data directly.

    Args:
        ticker: Stock code
        dates: Date list
        closes: Close prices
        timeframe: "daily" or "weekly" (for labeling)

    Returns:
        Same format as calc()
    """
    if len(closes) < 35:
        return {
            "ok": False,
            "error": {"code": "NO_DATA", "msg": "데이터가 충분하지 않습니다 (최소 35 필요)"},
        }

    # Use reference-style EMA (no SMA initialization)
    ema13 = _calc_ema_no_sma(closes, 13)
    macd_line, signal_line, macd_hist = _calc_macd_no_sma(closes)

    # Calculate slopes (reference uses .diff())
    ema13_slope = _calc_slope(ema13)
    hist_slope = _calc_slope(macd_hist)

    # Determine colors based on slopes
    colors = _calc_impulse_color_by_slope(ema13_slope, hist_slope)

    result = {
        "ticker": ticker,
        "timeframe": timeframe,
        "dates": dates,
        "color": colors,
        "ema13": ema13,
        "macd_line": macd_line,
        "signal_line": signal_line,
        "macd_hist": macd_hist,
        "ema13_slope": ema13_slope,
        "hist_slope": hist_slope,
    }

    return {"ok": True, "data": result}


def _calc_ema_no_sma(prices: List[int], period: int) -> List[Optional[float]]:
    """
    Calculate EMA using reference formula (ewm with adjust=False).

    This matches pandas ewm(alpha=2/(period+1), adjust=False).mean()
    - Starts from first value (no SMA initialization)
    - Formula: ema[0] = price[0], ema[t] = alpha * price[t] + (1-alpha) * ema[t-1]

    Note: prices are in reverse order (newest first)

    Args:
        prices: Price list (newest first)
        period: EMA period

    Returns:
        EMA values as floats
    """
    if not prices:
        return []

    # Reverse to chronological order for calculation
    prices_chrono = list(reversed(prices))
    alpha = 2 / (period + 1)

    # Start from first value (no SMA initialization)
    ema_chrono = [float(prices_chrono[0])]

    for i in range(1, len(prices_chrono)):
        ema_value = alpha * prices_chrono[i] + (1 - alpha) * ema_chrono[-1]
        ema_chrono.append(ema_value)

    # Reverse back to newest-first order
    return list(reversed(ema_chrono))


def _calc_macd_no_sma(
    prices: List[int],
    fast_period: int = 12,
    slow_period: int = 26,
    signal_period: int = 9,
) -> tuple:
    """
    Calculate MACD using reference formula (no SMA initialization).

    MACD Line = EMA12 - EMA26
    Signal Line = EMA9 of MACD Line
    Histogram = MACD Line - Signal Line

    Args:
        prices: Price list (newest first)
        fast_period: Fast EMA period (default 12)
        slow_period: Slow EMA period (default 26)
        signal_period: Signal line period (default 9)

    Returns:
        Tuple of (macd_line, signal_line, histogram)
    """
    if not prices:
        return [], [], []

    # Calculate EMAs
    ema_fast = _calc_ema_no_sma(prices, fast_period)
    ema_slow = _calc_ema_no_sma(prices, slow_period)

    # Calculate MACD Line
    macd_line = [ema_fast[i] - ema_slow[i] for i in range(len(prices))]

    # Calculate Signal Line (EMA of MACD Line)
    # Reverse MACD to chronological order
    macd_chrono = list(reversed(macd_line))
    alpha = 2 / (signal_period + 1)

    signal_chrono = [macd_chrono[0]]
    for i in range(1, len(macd_chrono)):
        signal_value = alpha * macd_chrono[i] + (1 - alpha) * signal_chrono[-1]
        signal_chrono.append(signal_value)

    signal_line = list(reversed(signal_chrono))

    # Calculate Histogram
    histogram = [macd_line[i] - signal_line[i] for i in range(len(prices))]

    return macd_line, signal_line, histogram


def _calc_slope(values: List[Optional[float]]) -> List[float]:
    """
    Calculate slope (diff from previous value) of a series.

    This matches pandas .diff() method.
    Note: Data is in reverse order (newest first), so slope = current - next (older).

    Args:
        values: Value list (newest first)

    Returns:
        Slope list (positive = rising, negative = falling)
    """
    result = []
    for i in range(len(values)):
        if i + 1 >= len(values):
            result.append(0.0)
        elif values[i] is None or values[i + 1] is None:
            result.append(0.0)
        else:
            # Since data is newest-first, slope = current - previous (which is next in list)
            result.append(values[i] - values[i + 1])
    return result


def _calc_impulse_color_by_slope(
    ema13_slope: List[float],
    hist_slope: List[float],
) -> List[str]:
    """
    Determine Elder Impulse color based on slopes.

    Reference logic:
    - "bull" (green): ema_slope > 0 AND hist_slope > 0
    - "bear" (red): ema_slope < 0 AND hist_slope < 0
    - "neutral" (blue): Otherwise

    Args:
        ema13_slope: EMA13 slope list
        hist_slope: MACD Histogram slope list

    Returns:
        Color list ("green", "red", "blue")
    """
    result = []
    for i in range(len(ema13_slope)):
        if ema13_slope[i] > 0 and hist_slope[i] > 0:
            result.append("green")
        elif ema13_slope[i] < 0 and hist_slope[i] < 0:
            result.append("red")
        else:
            result.append("blue")
    return result


# Keep legacy functions for backward compatibility
def _calc_ema(prices: List[int], period: int) -> List[Optional[int]]:
    """Legacy EMA calculation with SMA initialization (deprecated)."""
    result = _calc_ema_no_sma(prices, period)
    return [int(v) if v is not None else None for v in result]


def _calc_macd(
    prices: List[int],
    fast_period: int = 12,
    slow_period: int = 26,
    signal_period: int = 9,
) -> tuple:
    """Legacy MACD calculation (deprecated, use _calc_macd_no_sma)."""
    macd_line, signal_line, histogram = _calc_macd_no_sma(prices, fast_period, slow_period, signal_period)
    return (
        [int(m) for m in macd_line],
        [int(s) for s in signal_line],
        [int(h) for h in histogram],
    )


def _calc_direction(values: List[Optional[int]]) -> List[int]:
    """Legacy direction calculation (deprecated, use _calc_slope)."""
    slopes = _calc_slope([float(v) if v is not None else None for v in values])
    return [1 if s > 0 else (-1 if s < 0 else 0) for s in slopes]


def _calc_impulse_color(
    ema13_dir: List[int],
    hist_dir: List[int],
) -> List[str]:
    """Legacy impulse color calculation (deprecated)."""
    result = []
    for i in range(len(ema13_dir)):
        if ema13_dir[i] == 1 and hist_dir[i] == 1:
            result.append("green")
        elif ema13_dir[i] == -1 and hist_dir[i] == -1:
            result.append("red")
        else:
            result.append("blue")
    return result
