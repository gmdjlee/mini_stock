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
    fear_greed = _calc_fear_greed(closes, volumes, ma20, cmf)
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
    fear_greed = _calc_fear_greed(closes, volumes, ma20, cmf)
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
    ma20: List[Optional[int]],
    cmf: List[float],
) -> List[int]:
    """
    Calculate Fear/Greed Index.

    Components (simplified version):
    - Price vs MA20 (40%): Above = greed, Below = fear
    - Momentum (30%): Recent price change
    - CMF (30%): Money flow direction

    Returns:
        Fear/Greed index (0-100, 50 = neutral)
    """
    result = []

    for i in range(len(closes)):
        score = 50  # Start at neutral

        # Price vs MA20 (40 points max)
        if ma20[i] is not None and ma20[i] > 0:
            deviation = (closes[i] - ma20[i]) / ma20[i]
            # Cap at +/- 20%
            deviation = max(-0.2, min(0.2, deviation))
            score += int(deviation * 200)  # -40 to +40

        # Momentum: 5-day change (30 points max)
        if i + 5 < len(closes) and closes[i + 5] > 0:
            momentum = (closes[i] - closes[i + 5]) / closes[i + 5]
            momentum = max(-0.15, min(0.15, momentum))
            score += int(momentum * 200)  # -30 to +30

        # CMF (30 points max)
        cmf_score = int(cmf[i] * 30)
        score += cmf_score

        # Clamp to 0-100
        result.append(max(0, min(100, score)))

    return result


def _calc_trend(
    ma_signal: List[int],
    cmf: List[float],
    fear_greed: List[int],
) -> List[str]:
    """
    Determine overall trend.

    Logic:
    - "bullish": Strong uptrend signals
    - "bearish": Strong downtrend signals
    - "neutral": Mixed signals

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

        # Fear/Greed
        if fear_greed[i] >= 60:
            bull_count += 1
        elif fear_greed[i] <= 40:
            bear_count += 1

        if bull_count >= 2:
            result.append("bullish")
        elif bear_count >= 2:
            result.append("bearish")
        else:
            result.append("neutral")

    return result
