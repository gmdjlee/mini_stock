"""Elder Impulse System indicator (EMA13, MACD)."""

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
    Calculate Elder Impulse System.

    Elder Impulse System uses:
    - EMA13 (13-day Exponential Moving Average)
    - MACD Histogram (MACD Line - Signal Line)

    Color determination:
    - Green: EMA13 rising AND MACD Histogram rising
    - Red: EMA13 falling AND MACD Histogram falling
    - Blue: Otherwise (neutral/mixed signals)

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
                "color": ["green", "blue", "red", ...],  # impulse color
                "ema13": [55000, 54800, ...],
                "macd_line": [500, 480, ...],
                "signal_line": [450, 460, ...],
                "macd_hist": [50, 20, ...],
                "ema13_dir": [1, 0, -1, ...],  # 1: up, 0: flat, -1: down
                "hist_dir": [1, -1, ...],  # 1: up, -1: down
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

    # Fetch OHLCV data with extra days for EMA/MACD warmup
    fetch_days = days + 50  # Extra days for indicator warmup
    ohlcv_result = ohlcv.get_daily(client, ticker, days=fetch_days)

    if not ohlcv_result["ok"]:
        return ohlcv_result

    data = ohlcv_result["data"]
    closes = data["close"]
    dates = data["dates"]

    if len(closes) < 35:
        return {
            "ok": False,
            "error": {"code": "NO_DATA", "msg": "데이터가 충분하지 않습니다 (최소 35일 필요)"},
        }

    # Calculate indicators
    ema13 = _calc_ema(closes, 13)
    macd_line, signal_line, macd_hist = _calc_macd(closes)

    # Calculate directions
    ema13_dir = _calc_direction(ema13)
    hist_dir = _calc_direction(macd_hist)

    # Determine impulse colors
    colors = _calc_impulse_color(ema13_dir, hist_dir)

    # Trim to requested days
    trim_len = min(days, len(dates) - 34)
    result = {
        "ticker": ticker,
        "dates": dates[:trim_len],
        "color": colors[:trim_len],
        "ema13": ema13[:trim_len],
        "macd_line": macd_line[:trim_len],
        "signal_line": signal_line[:trim_len],
        "macd_hist": macd_hist[:trim_len],
        "ema13_dir": ema13_dir[:trim_len],
        "hist_dir": hist_dir[:trim_len],
    }

    log_info("indicator.elder", "calc complete", {"ticker": ticker, "days": trim_len})

    return {"ok": True, "data": result}


def calc_from_ohlcv(
    ticker: str,
    dates: List[str],
    closes: List[int],
) -> Dict:
    """
    Calculate Elder Impulse from OHLCV data directly.

    Args:
        ticker: Stock code
        dates: Date list
        closes: Close prices

    Returns:
        Same format as calc()
    """
    if len(closes) < 35:
        return {
            "ok": False,
            "error": {"code": "NO_DATA", "msg": "데이터가 충분하지 않습니다 (최소 35일 필요)"},
        }

    ema13 = _calc_ema(closes, 13)
    macd_line, signal_line, macd_hist = _calc_macd(closes)
    ema13_dir = _calc_direction(ema13)
    hist_dir = _calc_direction(macd_hist)
    colors = _calc_impulse_color(ema13_dir, hist_dir)

    result = {
        "ticker": ticker,
        "dates": dates,
        "color": colors,
        "ema13": ema13,
        "macd_line": macd_line,
        "signal_line": signal_line,
        "macd_hist": macd_hist,
        "ema13_dir": ema13_dir,
        "hist_dir": hist_dir,
    }

    return {"ok": True, "data": result}


def _calc_ema(prices: List[int], period: int) -> List[Optional[int]]:
    """
    Calculate Exponential Moving Average.

    EMA = Price(t) * k + EMA(t-1) * (1-k)
    where k = 2 / (period + 1)

    Note: prices are in reverse order (newest first)

    Args:
        prices: Price list (newest first)
        period: EMA period

    Returns:
        EMA values (None for insufficient data)
    """
    if len(prices) < period:
        return [None] * len(prices)

    # Reverse to chronological order for calculation
    prices_chrono = list(reversed(prices))
    k = 2 / (period + 1)

    # Initialize with SMA
    sma = sum(prices_chrono[:period]) / period
    ema_chrono = [None] * (period - 1) + [sma]

    # Calculate EMA
    for i in range(period, len(prices_chrono)):
        ema_value = prices_chrono[i] * k + ema_chrono[-1] * (1 - k)
        ema_chrono.append(ema_value)

    # Convert to int and reverse back
    ema_result = [int(v) if v is not None else None for v in reversed(ema_chrono)]
    return ema_result


def _calc_macd(
    prices: List[int],
    fast_period: int = 12,
    slow_period: int = 26,
    signal_period: int = 9,
) -> tuple:
    """
    Calculate MACD (Moving Average Convergence Divergence).

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
    # Calculate EMAs (these will have None values at the end)
    ema_fast = _calc_ema(prices, fast_period)
    ema_slow = _calc_ema(prices, slow_period)

    # Calculate MACD Line
    macd_line = []
    for i in range(len(prices)):
        if ema_fast[i] is not None and ema_slow[i] is not None:
            macd_line.append(ema_fast[i] - ema_slow[i])
        else:
            macd_line.append(None)

    # Calculate Signal Line (EMA of MACD Line)
    # Filter out None values for EMA calculation
    valid_macd = [m for m in macd_line if m is not None]
    if len(valid_macd) < signal_period:
        return (
            [None] * len(prices),
            [None] * len(prices),
            [None] * len(prices),
        )

    # Reverse valid MACD values (oldest first)
    valid_macd_chrono = list(reversed(valid_macd))
    k = 2 / (signal_period + 1)

    # Initialize signal with SMA
    sma = sum(valid_macd_chrono[:signal_period]) / signal_period
    signal_chrono = [None] * (signal_period - 1) + [sma]

    for i in range(signal_period, len(valid_macd_chrono)):
        signal_value = valid_macd_chrono[i] * k + signal_chrono[-1] * (1 - k)
        signal_chrono.append(signal_value)

    # Reverse back and pad with None
    signal_valid = list(reversed(signal_chrono))
    none_count = len(prices) - len(valid_macd)
    signal_line = signal_valid + [None] * none_count

    # Calculate Histogram
    histogram = []
    for i in range(len(prices)):
        if macd_line[i] is not None and signal_line[i] is not None:
            histogram.append(int(macd_line[i] - signal_line[i]))
        else:
            histogram.append(None)

    # Convert to int
    macd_line = [int(m) if m is not None else None for m in macd_line]
    signal_line = [int(s) if s is not None else None for s in signal_line]

    return macd_line, signal_line, histogram


def _calc_direction(values: List[Optional[int]]) -> List[int]:
    """
    Calculate direction (rising/falling) of a series.

    Args:
        values: Value list (newest first)

    Returns:
        Direction list (1: rising, -1: falling, 0: flat or None)
    """
    result = []
    for i in range(len(values)):
        if i + 1 >= len(values):
            result.append(0)
        elif values[i] is None or values[i + 1] is None:
            result.append(0)
        elif values[i] > values[i + 1]:
            result.append(1)  # Rising (current > previous)
        elif values[i] < values[i + 1]:
            result.append(-1)  # Falling
        else:
            result.append(0)  # Flat
    return result


def _calc_impulse_color(
    ema13_dir: List[int],
    hist_dir: List[int],
) -> List[str]:
    """
    Determine Elder Impulse color.

    - Green: Both EMA13 and Histogram rising
    - Red: Both EMA13 and Histogram falling
    - Blue: Mixed signals

    Args:
        ema13_dir: EMA13 direction list
        hist_dir: MACD Histogram direction list

    Returns:
        Color list ("green", "red", "blue")
    """
    result = []
    for i in range(len(ema13_dir)):
        if ema13_dir[i] == 1 and hist_dir[i] == 1:
            result.append("green")
        elif ema13_dir[i] == -1 and hist_dir[i] == -1:
            result.append("red")
        else:
            result.append("blue")
    return result
