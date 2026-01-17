"""Trend Signal indicator (MA, CMF, Fear/Greed)."""

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
    Calculate Trend Signal.

    Trend Signal combines:
    - MA (Moving Average) Signal: Short/Mid/Long MA crossover
    - CMF (Chaikin Money Flow): Money flow indicator
    - Fear/Greed Index: Combined market sentiment

    Args:
        client: Kiwoom API client
        ticker: Stock code
        days: Number of days for calculation

    Returns:
        {
            "ok": True,
            "data": {
                "ticker": "005930",
                "dates": ["20250110", ...],
                "ma_signal": [1, 0, -1, ...],  # 1: bullish, 0: neutral, -1: bearish
                "cmf": [0.15, 0.08, ...],  # -1 to 1
                "fear_greed": [65, 58, ...],  # 0-100
                "trend": ["bullish", "neutral", ...],  # combined signal
                "ma5": [55000, ...],
                "ma20": [54000, ...],
                "ma60": [53000, ...]
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

    # Fetch OHLCV data with extra days for MA calculation
    fetch_days = days + 60  # Extra days for MA60 calculation
    ohlcv_result = ohlcv.get_daily(client, ticker, days=fetch_days)

    if not ohlcv_result["ok"]:
        return ohlcv_result

    data = ohlcv_result["data"]
    closes = data["close"]
    highs = data["high"]
    lows = data["low"]
    volumes = data["volume"]
    dates = data["dates"]

    if len(closes) < 60:
        return {
            "ok": False,
            "error": {"code": "NO_DATA", "msg": "데이터가 충분하지 않습니다 (최소 60일 필요)"},
        }

    # Calculate indicators
    ma5 = _calc_ma(closes, 5)
    ma20 = _calc_ma(closes, 20)
    ma60 = _calc_ma(closes, 60)
    ma_signal = _calc_ma_signal(ma5, ma20, ma60)

    cmf = _calc_cmf(highs, lows, closes, volumes, period=20)
    fear_greed = _calc_fear_greed(closes, volumes)
    trend = _calc_trend(ma_signal, cmf, fear_greed)

    # Trim to requested days
    trim_len = min(days, len(dates) - 59)
    result = {
        "ticker": ticker,
        "dates": dates[:trim_len],
        "ma_signal": ma_signal[:trim_len],
        "cmf": cmf[:trim_len],
        "fear_greed": fear_greed[:trim_len],
        "trend": trend[:trim_len],
        "ma5": ma5[:trim_len],
        "ma20": ma20[:trim_len],
        "ma60": ma60[:trim_len],
    }

    log_info("indicator.trend", "calc complete", {"ticker": ticker, "days": trim_len})

    return {"ok": True, "data": result}


def calc_from_ohlcv(
    ticker: str,
    dates: List[str],
    closes: List[int],
    highs: List[int],
    lows: List[int],
    volumes: List[int],
) -> Dict:
    """
    Calculate Trend Signal from OHLCV data directly.

    Use this when OHLCV data is already available.

    Args:
        ticker: Stock code
        dates: Date list
        closes: Close prices
        highs: High prices
        lows: Low prices
        volumes: Volume list

    Returns:
        Same format as calc()
    """
    if len(closes) < 60:
        return {
            "ok": False,
            "error": {"code": "NO_DATA", "msg": "데이터가 충분하지 않습니다 (최소 60일 필요)"},
        }

    ma5 = _calc_ma(closes, 5)
    ma20 = _calc_ma(closes, 20)
    ma60 = _calc_ma(closes, 60)
    ma_signal = _calc_ma_signal(ma5, ma20, ma60)

    cmf = _calc_cmf(highs, lows, closes, volumes, period=20)
    fear_greed = _calc_fear_greed(closes, volumes)
    trend = _calc_trend(ma_signal, cmf, fear_greed)

    result = {
        "ticker": ticker,
        "dates": dates,
        "ma_signal": ma_signal,
        "cmf": cmf,
        "fear_greed": fear_greed,
        "trend": trend,
        "ma5": ma5,
        "ma20": ma20,
        "ma60": ma60,
    }

    return {"ok": True, "data": result}


def _calc_ma(prices: List[int], period: int) -> List[Optional[int]]:
    """
    Calculate Simple Moving Average.

    Args:
        prices: Price list (newest first)
        period: MA period

    Returns:
        MA values (None for insufficient data)
    """
    result = []
    for i in range(len(prices)):
        if i + period > len(prices):
            result.append(None)
        else:
            window = prices[i : i + period]
            result.append(int(sum(window) / period))
    return result


def _calc_ma_signal(
    ma5: List[Optional[int]],
    ma20: List[Optional[int]],
    ma60: List[Optional[int]],
) -> List[int]:
    """
    Calculate MA-based signal.

    Signal Logic:
    - 1 (Bullish): MA5 > MA20 > MA60 (uptrend alignment)
    - -1 (Bearish): MA5 < MA20 < MA60 (downtrend alignment)
    - 0 (Neutral): Otherwise

    Returns:
        Signal list (1, 0, -1)
    """
    result = []
    for i in range(len(ma5)):
        if ma5[i] is None or ma20[i] is None or ma60[i] is None:
            result.append(0)
        elif ma5[i] > ma20[i] > ma60[i]:
            result.append(1)
        elif ma5[i] < ma20[i] < ma60[i]:
            result.append(-1)
        else:
            result.append(0)
    return result


def _calc_cmf(
    highs: List[int],
    lows: List[int],
    closes: List[int],
    volumes: List[int],
    period: int = 20,
) -> List[float]:
    """
    Calculate Chaikin Money Flow (CMF).

    CMF = Sum(Money Flow Volume) / Sum(Volume) over period

    Money Flow Multiplier = ((Close - Low) - (High - Close)) / (High - Low)
    Money Flow Volume = MFM * Volume

    Returns:
        CMF values (-1 to 1)
    """
    result = []

    # Calculate Money Flow Multiplier and Volume
    mfv = []
    for i in range(len(closes)):
        hl_range = highs[i] - lows[i]
        if hl_range == 0:
            mfv.append(0.0)
        else:
            mfm = ((closes[i] - lows[i]) - (highs[i] - closes[i])) / hl_range
            mfv.append(mfm * volumes[i])

    # Calculate CMF for each period
    for i in range(len(closes)):
        if i + period > len(closes):
            result.append(0.0)
        else:
            sum_mfv = sum(mfv[i : i + period])
            sum_vol = sum(volumes[i : i + period])
            if sum_vol == 0:
                result.append(0.0)
            else:
                cmf_value = sum_mfv / sum_vol
                result.append(round(cmf_value, 4))

    return result


def _calc_fear_greed(
    closes: List[int],
    volumes: List[int],
) -> List[float]:
    """
    Calculate Fear/Greed Index (Reference formula).

    Components:
    - Momentum5 (45%): 5-day log return (상승 피로도)
    - Pos52 (45%): Position within 52-day high/low range
    - VolSurge (5%): Recent volume surge vs past volume
    - VolSpike (5%): Recent volatility vs past volatility (negative)

    Formula:
    FG = 0.45*m + 0.45*p + 0.05*v + 0.05*vs

    Returns:
        Fear/Greed index (approximately -1 to 1.5)
    """
    import math

    n = len(closes)
    result = [0.0] * n

    if n < 52:
        return result

    # Process in chronological order
    closes_chrono = list(reversed(closes))
    volumes_chrono = list(reversed(volumes))

    # Calculate components
    momentum5 = [0.0] * n
    pos52 = [0.0] * n
    vol_surge = [1.0] * n
    vol_spike = [1.0] * n
    returns = [0.0] * n

    for i in range(n):
        # Momentum5: log return over 5 periods * 100
        if i >= 5 and closes_chrono[i] > 0 and closes_chrono[i - 5] > 0:
            momentum5[i] = (math.log(closes_chrono[i]) - math.log(closes_chrono[i - 5])) * 100

        # Pos52: Position within 52-day range
        if i >= 51:
            window = closes_chrono[max(0, i - 51) : i + 1]
            low52 = min(window)
            high52 = max(window)
            if high52 > low52:
                pos52[i] = (closes_chrono[i] - low52) / (high52 - low52)
            else:
                pos52[i] = 0.5
        else:
            # Use available data
            window = closes_chrono[: i + 1]
            if window:
                low_val = min(window)
                high_val = max(window)
                if high_val > low_val:
                    pos52[i] = (closes_chrono[i] - low_val) / (high_val - low_val)
                else:
                    pos52[i] = 0.5

        # Returns for volatility calculation
        if i >= 1 and closes_chrono[i - 1] > 0:
            returns[i] = (closes_chrono[i] - closes_chrono[i - 1]) / closes_chrono[i - 1]

    # VolSurge: recent 5-day avg volume / past 20-day avg volume
    for i in range(n):
        if i >= 20:
            recent_vol = sum(volumes_chrono[i - 4 : i + 1]) / 5
            past_vol = sum(volumes_chrono[i - 19 : i + 1]) / 20
            if past_vol > 0:
                vol_surge[i] = max(0, min(3, recent_vol / past_vol))
        elif i >= 5:
            recent_vol = sum(volumes_chrono[: i + 1]) / (i + 1)
            if recent_vol > 0:
                vol_surge[i] = 1.0

    # VolSpike: recent 5-day volatility / past 20-day volatility
    for i in range(n):
        if i >= 20:
            recent_returns = returns[i - 4 : i + 1]
            past_returns = returns[i - 19 : i + 1]

            recent_std = _calc_std(recent_returns)
            past_std = _calc_std(past_returns)

            if past_std > 0:
                vol_spike[i] = max(0, min(3, recent_std / past_std))
        elif i >= 5:
            vol_spike[i] = 1.0

    # Calculate FG with smoothing
    fg_chrono = [0.0] * n

    for i in range(n):
        if i < 10:
            fg_chrono[i] = 0.0
            continue

        # Smoothed momentum (7-period mean, then /10)
        m_window = momentum5[max(0, i - 6) : i + 1]
        m = (sum(m_window) / len(m_window) / 10) if m_window else 0
        m = max(-1, min(1.5, m))

        # Smoothed position (7-period mean, then *2 - 1)
        p_window = pos52[max(0, i - 6) : i + 1]
        p = (2 * sum(p_window) / len(p_window) - 1) if p_window else 0
        p = max(-1, min(1.5, p))

        # Smoothed volume surge (10-period mean, then -1)
        v_window = vol_surge[max(0, i - 9) : i + 1]
        v = (sum(v_window) / len(v_window) - 1) if v_window else 0
        v = max(-0.5, min(1.2, v))

        # Smoothed volatility spike (10-period mean, then -1, negative)
        vs_window = vol_spike[max(0, i - 9) : i + 1]
        vs = -((sum(vs_window) / len(vs_window) - 1)) if vs_window else 0
        vs = max(-0.5, min(1.2, vs))

        # Final FG
        fg_chrono[i] = 0.45 * m + 0.45 * p + 0.05 * v + 0.05 * vs

    # Reverse back to newest-first order
    result = list(reversed(fg_chrono))
    return result


def _calc_std(values: List[float]) -> float:
    """Calculate standard deviation."""
    if len(values) < 2:
        return 0.0
    mean = sum(values) / len(values)
    variance = sum((x - mean) ** 2 for x in values) / len(values)
    return variance ** 0.5


def _calc_trend(
    ma_signal: List[int],
    cmf: List[float],
    fear_greed: List[float],
) -> List[str]:
    """
    Determine overall trend.

    Logic:
    - "bullish": Strong uptrend signals
    - "bearish": Strong downtrend signals
    - "neutral": Mixed signals

    Fear/Greed thresholds (reference):
    - > 0.5: Greed (탐욕, 상승 과열)
    - < -0.5: Fear (공포, 하락 과열)

    Returns:
        Trend labels
    """
    result = []

    for i in range(len(ma_signal)):
        bull_count = 0
        bear_count = 0

        # MA Signal
        if ma_signal[i] == 1:
            bull_count += 1
        elif ma_signal[i] == -1:
            bear_count += 1

        # CMF
        if cmf[i] > 0.05:
            bull_count += 1
        elif cmf[i] < -0.05:
            bear_count += 1

        # Fear/Greed (new range: -1 ~ 1.5)
        # > 0.5: Greed (bullish momentum)
        # < -0.5: Fear (bearish momentum)
        if fear_greed[i] > 0.5:
            bull_count += 1
        elif fear_greed[i] < -0.5:
            bear_count += 1

        if bull_count >= 2:
            result.append("bullish")
        elif bear_count >= 2:
            result.append("bearish")
        else:
            result.append("neutral")

    return result
