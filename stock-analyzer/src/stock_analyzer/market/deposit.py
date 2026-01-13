"""Market deposit and credit balance indicators."""

from dataclasses import dataclass
from typing import Dict, List

from ..client.kiwoom import KiwoomClient
from ..core.log import log_info


@dataclass
class DepositData:
    """Customer deposit trend data."""

    dates: List[str]
    deposit: List[int]      # 고객예탁금
    credit_loan: List[int]  # 신용융자


@dataclass
class CreditData:
    """Credit trading trend data."""

    dates: List[str]
    credit_balance: List[int]  # 신용잔고
    credit_ratio: List[float]  # 신용비율


def get_deposit(client: KiwoomClient, days: int = 30) -> Dict:
    """
    Get customer deposit trend.

    Args:
        client: Kiwoom API client
        days: Number of days to retrieve (default: 30)

    Returns:
        {
            "ok": True,
            "data": {
                "dates": ["2025-01-10", ...],
                "deposit": [50000000000000, ...],
                "credit_loan": [15000000000000, ...]
            }
        }

    Errors:
        - INVALID_ARG: Invalid argument
        - NO_DATA: No data available
        - API_ERROR: API call failed
    """
    if days <= 0:
        return {
            "ok": False,
            "error": {"code": "INVALID_ARG", "msg": "조회 기간은 1일 이상이어야 합니다"},
        }

    resp = client.get_deposit_trend(days)
    if not resp.ok:
        return {"ok": False, "error": resp.error}

    trend_data = resp.data.get("deposit_list", [])
    if not trend_data:
        return {
            "ok": False,
            "error": {"code": "NO_DATA", "msg": "예탁금 데이터가 없습니다"},
        }

    dates = []
    deposit = []
    credit_loan = []

    for item in trend_data[:days]:
        dt = item.get("dt", "")
        if len(dt) == 8:
            dt = f"{dt[:4]}-{dt[4:6]}-{dt[6:8]}"
        dates.append(dt)
        deposit.append(_safe_int(item.get("cust_deposit", 0)))
        credit_loan.append(_safe_int(item.get("credit_loan", 0)))

    log_info("market.deposit", "get_deposit complete", {"days": len(dates)})

    return {
        "ok": True,
        "data": {
            "dates": dates,
            "deposit": deposit,
            "credit_loan": credit_loan,
        },
    }


def get_credit(client: KiwoomClient, days: int = 30) -> Dict:
    """
    Get credit trading trend.

    Args:
        client: Kiwoom API client
        days: Number of days to retrieve (default: 30)

    Returns:
        {
            "ok": True,
            "data": {
                "dates": ["2025-01-10", ...],
                "credit_balance": [18000000000000, ...],
                "credit_ratio": [5.2, ...]
            }
        }

    Errors:
        - INVALID_ARG: Invalid argument
        - NO_DATA: No data available
        - API_ERROR: API call failed
    """
    if days <= 0:
        return {
            "ok": False,
            "error": {"code": "INVALID_ARG", "msg": "조회 기간은 1일 이상이어야 합니다"},
        }

    resp = client.get_credit_trend(days)
    if not resp.ok:
        return {"ok": False, "error": resp.error}

    trend_data = resp.data.get("credit_list", [])
    if not trend_data:
        return {
            "ok": False,
            "error": {"code": "NO_DATA", "msg": "신용잔고 데이터가 없습니다"},
        }

    dates = []
    credit_balance = []
    credit_ratio = []

    for item in trend_data[:days]:
        dt = item.get("dt", "")
        if len(dt) == 8:
            dt = f"{dt[:4]}-{dt[4:6]}-{dt[6:8]}"
        dates.append(dt)
        credit_balance.append(_safe_int(item.get("credit_bal", 0)))
        credit_ratio.append(_safe_float(item.get("credit_rt", 0)))

    log_info("market.deposit", "get_credit complete", {"days": len(dates)})

    return {
        "ok": True,
        "data": {
            "dates": dates,
            "credit_balance": credit_balance,
            "credit_ratio": credit_ratio,
        },
    }


def get_market_indicators(client: KiwoomClient, days: int = 30) -> Dict:
    """
    Get combined market indicators (deposit + credit).

    Args:
        client: Kiwoom API client
        days: Number of days to retrieve (default: 30)

    Returns:
        {
            "ok": True,
            "data": {
                "dates": ["2025-01-10", ...],
                "deposit": [50000000000000, ...],
                "credit_loan": [15000000000000, ...],
                "credit_balance": [18000000000000, ...],
                "credit_ratio": [5.2, ...]
            }
        }

    Errors:
        - INVALID_ARG: Invalid argument
        - NO_DATA: No data available
        - API_ERROR: API call failed
    """
    if days <= 0:
        return {
            "ok": False,
            "error": {"code": "INVALID_ARG", "msg": "조회 기간은 1일 이상이어야 합니다"},
        }

    deposit_result = get_deposit(client, days)
    if not deposit_result["ok"]:
        return deposit_result

    credit_result = get_credit(client, days)
    if not credit_result["ok"]:
        return credit_result

    # Combine data (use deposit dates as base)
    deposit_data = deposit_result["data"]
    credit_data = credit_result["data"]

    log_info("market.deposit", "get_market_indicators complete", {"days": len(deposit_data["dates"])})

    return {
        "ok": True,
        "data": {
            "dates": deposit_data["dates"],
            "deposit": deposit_data["deposit"],
            "credit_loan": deposit_data["credit_loan"],
            "credit_balance": credit_data["credit_balance"],
            "credit_ratio": credit_data["credit_ratio"],
        },
    }


def _safe_int(value) -> int:
    """Safely convert value to int."""
    try:
        return int(value) if value is not None else 0
    except (ValueError, TypeError):
        return 0


def _safe_float(value) -> float:
    """Safely convert value to float."""
    try:
        return float(value) if value is not None else 0.0
    except (ValueError, TypeError):
        return 0.0
