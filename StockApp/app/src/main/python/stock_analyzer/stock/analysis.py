"""Supply/demand analysis functionality."""

from dataclasses import dataclass
from typing import Dict, List

from ..client.kiwoom import KiwoomClient
from ..core import safe_float, safe_int
from ..core.log import log_err, log_info
from . import ohlcv


@dataclass
class StockData:
    """Stock analysis data."""

    ticker: str
    name: str
    dates: List[str]
    mcap: List[int]      # Market cap
    for_5d: List[int]    # Foreign 5-day net buy (rolling sum)
    ins_5d: List[int]    # Institution 5-day net buy (rolling sum)


def _rolling_sum(values: List[int], window: int) -> List[int]:
    """
    Calculate rolling sum with min_periods=1 (matching pandas rolling behavior).

    Args:
        values: List of values
        window: Rolling window size

    Returns:
        List of rolling sums
    """
    result = []
    for i in range(len(values)):
        # Use available data up to window size (min_periods=1)
        start = max(0, i - window + 1)
        result.append(sum(values[start:i + 1]))
    return result


def analyze(client: KiwoomClient, ticker: str, days: int = 180) -> Dict:
    """
    Analyze stock supply/demand.

    Args:
        client: Kiwoom API client
        ticker: Stock code
        days: Number of days to analyze

    Returns:
        {
            "ok": True,
            "data": {
                "ticker": "005930",
                "name": "삼성전자",
                "dates": ["2025-01-02", ...],
                "mcap": [380000000000000, ...],
                "for_5d": [1500000000, ...],
                "ins_5d": [-500000000, ...]
            }
        }

    Errors:
        - INVALID_ARG: Invalid argument
        - TICKER_NOT_FOUND: Stock not found
        - NO_DATA: No data available
        - API_ERROR: API call failed
    """
    if not ticker or not ticker.strip():
        return {
            "ok": False,
            "error": {"code": "INVALID_ARG", "msg": "종목코드가 필요합니다"},
        }

    ticker = ticker.strip()

    # 1. Get basic info
    info_resp = client.get_stock_info(ticker)
    if not info_resp.ok:
        if info_resp.error and info_resp.error.get("code") == "TICKER_NOT_FOUND":
            return {"ok": False, "error": info_resp.error}
        return {"ok": False, "error": info_resp.error}

    name = info_resp.data.get("stk_nm", ticker)
    # API field name per official docs: mac (시가총액)
    # ka10001 API returns market cap in 억원 (100 million won), convert to raw won
    mcap = safe_int(info_resp.data.get("mac", 0)) * 100_000_000
    # Get floating shares for daily market cap calculation (in 천주 units)
    flo_stk = safe_int(info_resp.data.get("flo_stk", 0))
    shares = flo_stk * 1000 if flo_stk > 0 else 0

    # 2. Get investor trend
    trend_resp = client.get_investor_trend(ticker)
    if not trend_resp.ok:
        return {"ok": False, "error": trend_resp.error}

    # API returns data in 'stk_invsr_orgn' field
    trend_data = trend_resp.data.get("stk_invsr_orgn", []) or trend_resp.data.get("list", [])
    if not trend_data:
        return {
            "ok": False,
            "error": {"code": "NO_DATA", "msg": "수급 데이터가 없습니다"},
        }

    # 2.5. Get OHLCV data for daily market cap calculation
    # This ensures market cap varies with stock price, avoiding flat lines
    ohlcv_result = ohlcv.get_daily(client, ticker, days=days)
    date_to_close = {}
    if ohlcv_result["ok"]:
        ohlcv_data = ohlcv_result["data"]
        for i, dt in enumerate(ohlcv_data["dates"]):
            # Normalize date format to YYYY-MM-DD
            if len(dt) == 8:
                dt = f"{dt[:4]}-{dt[4:6]}-{dt[6:8]}"
            date_to_close[dt] = ohlcv_data["close"][i]

    # 3. Parse data - collect raw daily values first
    dates = []
    mcaps = []
    for_daily = []  # Daily foreign net buy (not 5-day sum yet)
    ins_daily = []  # Daily institution net buy (not 5-day sum yet)

    for item in trend_data[:days]:
        dt = item.get("dt", "")
        # Convert date format if needed (YYYYMMDD -> YYYY-MM-DD)
        if len(dt) == 8:
            dt = f"{dt[:4]}-{dt[4:6]}-{dt[6:8]}"
        dates.append(dt)

        # Calculate daily market cap from OHLCV close price × shares
        # This matches oscillator.py logic to avoid flat market cap lines
        if shares > 0 and dt in date_to_close:
            daily_mcap = shares * date_to_close[dt]
        else:
            # Fallback: use mrkt_tot_amt from API or basic info mcap
            mrkt_tot_amt = safe_int(item.get("mrkt_tot_amt", 0))
            if mrkt_tot_amt > 0:
                daily_mcap = mrkt_tot_amt * 1_000_000
            else:
                daily_mcap = mcap
        mcaps.append(daily_mcap)

        # API field names per official docs: frgnr_invsr (외국인투자자), orgn (기관계)
        # These are daily values in 백만원 (million won)
        for_daily.append(safe_int(item.get("frgnr_invsr", 0)))
        ins_daily.append(safe_int(item.get("orgn", 0)))

    # 4. Calculate 5-day rolling sum (matching EtfMonitor reference)
    # This smooths the data and matches the reference oscillator calculation
    for_5d = _rolling_sum(for_daily, 5)
    ins_5d = _rolling_sum(ins_daily, 5)

    log_info("stock.analysis", "analyze complete", {"ticker": ticker, "days": len(dates)})

    return {
        "ok": True,
        "data": {
            "ticker": ticker,
            "name": name,
            "dates": dates,
            "mcap": mcaps,
            "for_5d": for_5d,
            "ins_5d": ins_5d,
        },
    }


def get_foreign_trend(client: KiwoomClient, ticker: str) -> Dict:
    """
    Get foreign investor trend.

    Args:
        client: Kiwoom API client
        ticker: Stock code

    Returns:
        {
            "ok": True,
            "data": {
                "ticker": "005930",
                "net_buy": 1500000000,
                "holding_qty": 3000000000,
                "holding_ratio": 52.5
            }
        }
    """
    if not ticker or not ticker.strip():
        return {
            "ok": False,
            "error": {"code": "INVALID_ARG", "msg": "종목코드가 필요합니다"},
        }

    resp = client.get_foreign_trend(ticker.strip())
    if not resp.ok:
        return {"ok": False, "error": resp.error}

    data = resp.data
    return {
        "ok": True,
        "data": {
            "ticker": ticker,
            "net_buy": safe_int(data.get("frgn_net", 0)),
            "holding_qty": safe_int(data.get("frgn_hold_qty", 0)),
            "holding_ratio": safe_float(data.get("frgn_hold_rt", 0)),
        },
    }


def get_institution_trend(client: KiwoomClient, ticker: str) -> Dict:
    """
    Get institutional investor trend.

    Args:
        client: Kiwoom API client
        ticker: Stock code

    Returns:
        {
            "ok": True,
            "data": {
                "ticker": "005930",
                "net_buy": -500000000,
                "finance": 100000000,
                "insurance": -50000000,
                "invest_trust": -100000000
            }
        }
    """
    if not ticker or not ticker.strip():
        return {
            "ok": False,
            "error": {"code": "INVALID_ARG", "msg": "종목코드가 필요합니다"},
        }

    resp = client.get_institution_trend(ticker.strip())
    if not resp.ok:
        return {"ok": False, "error": resp.error}

    data = resp.data
    return {
        "ok": True,
        "data": {
            "ticker": ticker,
            "net_buy": safe_int(data.get("istt_net", 0)),
            "finance": safe_int(data.get("fin_inv_net", 0)),
            "insurance": safe_int(data.get("insur_net", 0)),
            "invest_trust": safe_int(data.get("inv_trust_net", 0)),
        },
    }


def get_investor_summary(client: KiwoomClient, ticker: str, period: str = "1") -> Dict:
    """
    Get investor summary.

    Args:
        client: Kiwoom API client
        ticker: Stock code
        period: Period (1: Daily, 2: Weekly, 3: Monthly)

    Returns:
        {
            "ok": True,
            "data": {
                "ticker": "005930",
                "foreign_net": 7500000000,
                "institution_net": -2500000000,
                "individual_net": -5000000000
            }
        }
    """
    if not ticker or not ticker.strip():
        return {
            "ok": False,
            "error": {"code": "INVALID_ARG", "msg": "종목코드가 필요합니다"},
        }

    resp = client.get_investor_summary(ticker.strip(), period)
    if not resp.ok:
        return {"ok": False, "error": resp.error}

    data = resp.data
    return {
        "ok": True,
        "data": {
            "ticker": ticker,
            "foreign_net": safe_int(data.get("frgn_net_sum", 0)),
            "institution_net": safe_int(data.get("istt_net_sum", 0)),
            "individual_net": safe_int(data.get("prsn_net_sum", 0)),
        },
    }
