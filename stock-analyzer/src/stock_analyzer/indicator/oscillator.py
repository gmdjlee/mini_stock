"""Market Cap & Supply/Demand Oscillator (MACD Style)."""

from typing import Dict, List

from ..client.kiwoom import KiwoomClient
from ..core.log import log_info
from ..stock import analysis


def calc(client: KiwoomClient, ticker: str, days: int = 180) -> Dict:
    """
    Calculate supply oscillator.

    Args:
        client: Kiwoom API client
        ticker: Stock code
        days: Number of days to calculate

    Returns:
        {
            "ok": True,
            "data": {
                "ticker": "005930",
                "name": "삼성전자",
                "dates": ["2025-01-02", ...],
                "market_cap": [380.0, ...],          # Trillion KRW
                "supply_ratio": [0.0015, ...],       # Supply ratio
                "ema12": [0.0012, ...],
                "ema26": [0.0010, ...],
                "macd": [0.0002, ...],
                "signal": [0.00015, ...],
                "oscillator": [0.00005, ...]         # Histogram
            }
        }

    Errors:
        - INVALID_ARG: Invalid argument
        - INSUFFICIENT_DATA: Minimum 26 days required
        - API_ERROR: API call failed
    """
    if not ticker or not ticker.strip():
        return {
            "ok": False,
            "error": {"code": "INVALID_ARG", "msg": "종목코드가 필요합니다"},
        }

    # 1. Get supply/demand data
    analysis_result = analysis.analyze(client, ticker, days)
    if not analysis_result["ok"]:
        return analysis_result

    data = analysis_result["data"]
    n = len(data["dates"])

    if n < 26:
        return {
            "ok": False,
            "error": {"code": "INSUFFICIENT_DATA", "msg": "최소 26일 데이터 필요"},
        }

    # 2. Calculate Supply Ratio
    supply_ratio = []
    for i in range(n):
        mcap = data["mcap"][i]
        if mcap == 0:
            supply_ratio.append(0.0)
        else:
            supply = data["for_5d"][i] + data["ins_5d"][i]
            supply_ratio.append(supply / mcap)

    # 3. Calculate EMA
    ema12 = _calc_ema(supply_ratio, 12)
    ema26 = _calc_ema(supply_ratio, 26)

    # 4. Calculate MACD
    macd = [ema12[i] - ema26[i] for i in range(n)]

    # 5. Calculate Signal Line
    signal = _calc_ema(macd, 9)

    # 6. Calculate Oscillator (Histogram)
    oscillator = [macd[i] - signal[i] for i in range(n)]

    # 7. Normalize market cap (Trillion KRW)
    market_cap_trillion = [m / 1_000_000_000_000 for m in data["mcap"]]

    log_info("indicator.oscillator", "calc complete", {"ticker": ticker, "days": n})

    return {
        "ok": True,
        "data": {
            "ticker": ticker,
            "name": data["name"],
            "dates": data["dates"],
            "market_cap": market_cap_trillion,
            "supply_ratio": supply_ratio,
            "ema12": ema12,
            "ema26": ema26,
            "macd": macd,
            "signal": signal,
            "oscillator": oscillator,
        },
    }


def calc_from_analysis(
    ticker: str,
    name: str,
    dates: List[str],
    mcap: List[int],
    for_5d: List[int],
    ins_5d: List[int],
) -> Dict:
    """
    Calculate oscillator from pre-fetched analysis data.

    Args:
        ticker: Stock code
        name: Stock name
        dates: Date list
        mcap: Market cap list
        for_5d: Foreign 5-day net buy list
        ins_5d: Institution 5-day net buy list

    Returns:
        Same format as calc()
    """
    n = len(dates)

    if n < 26:
        return {
            "ok": False,
            "error": {"code": "INSUFFICIENT_DATA", "msg": "최소 26일 데이터 필요"},
        }

    # Calculate Supply Ratio
    supply_ratio = []
    for i in range(n):
        if mcap[i] == 0:
            supply_ratio.append(0.0)
        else:
            supply = for_5d[i] + ins_5d[i]
            supply_ratio.append(supply / mcap[i])

    # Calculate EMA
    ema12 = _calc_ema(supply_ratio, 12)
    ema26 = _calc_ema(supply_ratio, 26)

    # Calculate MACD
    macd_line = [ema12[i] - ema26[i] for i in range(n)]

    # Calculate Signal Line
    signal = _calc_ema(macd_line, 9)

    # Calculate Oscillator (Histogram)
    oscillator = [macd_line[i] - signal[i] for i in range(n)]

    # Normalize market cap (Trillion KRW)
    market_cap_trillion = [m / 1_000_000_000_000 for m in mcap]

    return {
        "ok": True,
        "data": {
            "ticker": ticker,
            "name": name,
            "dates": dates,
            "market_cap": market_cap_trillion,
            "supply_ratio": supply_ratio,
            "ema12": ema12,
            "ema26": ema26,
            "macd": macd_line,
            "signal": signal,
            "oscillator": oscillator,
        },
    }


def analyze_signal(osc_result: Dict) -> Dict:
    """
    Analyze trading signal from oscillator result.

    Args:
        osc_result: Result from calc() function

    Returns:
        {
            "ok": True,
            "data": {
                "total_score": 67,
                "signal_type": "STRONG_BUY",
                "oscillator_score": 40,
                "cross_score": 15,
                "trend_score": 12,
                "description": "수급 강세, MACD 시그널 상향"
            }
        }
    """
    if not osc_result.get("ok"):
        return osc_result

    data = osc_result["data"]
    osc = data["oscillator"]
    macd = data["macd"]
    signal = data["signal"]

    n = len(osc)
    if n < 3:
        return {
            "ok": False,
            "error": {"code": "INSUFFICIENT_DATA", "msg": "최소 3일 데이터 필요"},
        }

    score = 0

    # 1. Oscillator Value (±40)
    latest_osc = osc[-1]
    if latest_osc > 0.005:
        osc_score = 40
    elif latest_osc > 0.002:
        osc_score = 20
    elif latest_osc < -0.005:
        osc_score = -40
    elif latest_osc < -0.002:
        osc_score = -20
    else:
        osc_score = 0
    score += osc_score

    # 2. MACD Cross (±30)
    if macd[-1] > signal[-1] and macd[-2] <= signal[-2]:
        cross_score = 30  # Golden Cross
    elif macd[-1] < signal[-1] and macd[-2] >= signal[-2]:
        cross_score = -30  # Dead Cross
    elif macd[-1] > signal[-1]:
        cross_score = 15  # Above Signal
    else:
        cross_score = -15  # Below Signal
    score += cross_score

    # 3. Histogram Trend (±30)
    recent_hist = osc[-3:]
    if all(h > 0 for h in recent_hist) and _is_increasing(recent_hist):
        trend_score = 30
    elif all(h < 0 for h in recent_hist) and _is_decreasing(recent_hist):
        trend_score = -30
    else:
        trend_score = 0
    score += trend_score

    # Determine Signal Type
    score = max(-100, min(100, score))
    if score >= 60:
        signal_type = "STRONG_BUY"
    elif score >= 20:
        signal_type = "BUY"
    elif score <= -60:
        signal_type = "STRONG_SELL"
    elif score <= -20:
        signal_type = "SELL"
    else:
        signal_type = "NEUTRAL"

    return {
        "ok": True,
        "data": {
            "total_score": score,
            "signal_type": signal_type,
            "oscillator_score": osc_score,
            "cross_score": cross_score,
            "trend_score": trend_score,
            "description": _generate_description(signal_type, osc_score, cross_score),
        },
    }


def _calc_ema(values: List[float], period: int) -> List[float]:
    """Calculate EMA."""
    if not values:
        return []
    alpha = 2 / (period + 1)
    ema = [values[0]]
    for i in range(1, len(values)):
        ema.append(alpha * values[i] + (1 - alpha) * ema[i - 1])
    return ema


def _is_increasing(values: List[float]) -> bool:
    """Check if values are increasing."""
    return all(values[i] > values[i - 1] for i in range(1, len(values)))


def _is_decreasing(values: List[float]) -> bool:
    """Check if values are decreasing."""
    return all(values[i] < values[i - 1] for i in range(1, len(values)))


def _generate_description(signal_type: str, osc_score: int, cross_score: int) -> str:
    """Generate description from scores."""
    parts = []
    if osc_score > 0:
        parts.append("수급 강세")
    elif osc_score < 0:
        parts.append("수급 약세")

    if cross_score == 30:
        parts.append("골든크로스 발생")
    elif cross_score == -30:
        parts.append("데드크로스 발생")
    elif cross_score > 0:
        parts.append("MACD 시그널 상향")
    else:
        parts.append("MACD 시그널 하향")

    return ", ".join(parts) if parts else "중립"
