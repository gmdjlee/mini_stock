"""Condition search functionality using HTS conditions."""

from dataclasses import dataclass
from typing import Dict, List

from ..client.kiwoom import KiwoomClient
from ..core import safe_float, safe_int
from ..core.log import log_info


@dataclass
class Condition:
    """Condition search item."""

    idx: str      # Condition index
    name: str     # Condition name


@dataclass
class ConditionResult:
    """Condition search result."""

    condition: Condition
    stocks: List[Dict]  # List of matching stocks


def get_list(client: KiwoomClient) -> Dict:
    """
    Get condition search list.

    Args:
        client: Kiwoom API client

    Returns:
        {
            "ok": True,
            "data": [
                {"idx": "000", "name": "골든크로스"},
                {"idx": "001", "name": "급등주"},
                ...
            ]
        }

    Errors:
        - NO_DATA: No conditions available
        - API_ERROR: API call failed
    """
    resp = client.get_condition_list()
    if not resp.ok:
        return {"ok": False, "error": resp.error}

    cond_list = resp.data.get("cond_list", [])
    if not cond_list:
        return {
            "ok": False,
            "error": {"code": "NO_DATA", "msg": "조건검색 목록이 없습니다"},
        }

    results = []
    for item in cond_list:
        results.append({
            "idx": item.get("cond_idx", ""),
            "name": item.get("cond_nm", ""),
        })

    log_info("search.condition", "get_list complete", {"count": len(results)})

    return {"ok": True, "data": results}


def search(client: KiwoomClient, cond_idx: str, cond_name: str) -> Dict:
    """
    Execute condition search.

    Args:
        client: Kiwoom API client
        cond_idx: Condition index
        cond_name: Condition name

    Returns:
        {
            "ok": True,
            "data": {
                "condition": {"idx": "000", "name": "골든크로스"},
                "stocks": [
                    {"ticker": "005930", "name": "삼성전자", "price": 55000, "change": 1.5},
                    ...
                ]
            }
        }

    Errors:
        - INVALID_ARG: Invalid argument
        - NO_DATA: No matching stocks
        - API_ERROR: API call failed
    """
    if not cond_idx or not cond_idx.strip():
        return {
            "ok": False,
            "error": {"code": "INVALID_ARG", "msg": "조건검색 인덱스가 필요합니다"},
        }

    if not cond_name or not cond_name.strip():
        return {
            "ok": False,
            "error": {"code": "INVALID_ARG", "msg": "조건검색 이름이 필요합니다"},
        }

    cond_idx = cond_idx.strip()
    cond_name = cond_name.strip()

    resp = client.search_condition(cond_idx, cond_name)
    if not resp.ok:
        return {"ok": False, "error": resp.error}

    stock_list = resp.data.get("stk_list", [])

    stocks = []
    for item in stock_list:
        stocks.append({
            "ticker": item.get("stk_cd", ""),
            "name": item.get("stk_nm", ""),
            "price": safe_int(item.get("cur_prc", 0)),
            "change": safe_float(item.get("chg_rt", 0)),
        })

    log_info("search.condition", "search complete", {
        "cond_idx": cond_idx,
        "cond_name": cond_name,
        "count": len(stocks),
    })

    return {
        "ok": True,
        "data": {
            "condition": {"idx": cond_idx, "name": cond_name},
            "stocks": stocks,
        },
    }


def search_by_idx(client: KiwoomClient, cond_idx: str) -> Dict:
    """
    Execute condition search by index (auto-fetch condition name).

    Args:
        client: Kiwoom API client
        cond_idx: Condition index

    Returns:
        Same as search()

    Errors:
        - INVALID_ARG: Invalid argument
        - CONDITION_NOT_FOUND: Condition not found
        - NO_DATA: No matching stocks
        - API_ERROR: API call failed
    """
    if not cond_idx or not cond_idx.strip():
        return {
            "ok": False,
            "error": {"code": "INVALID_ARG", "msg": "조건검색 인덱스가 필요합니다"},
        }

    cond_idx = cond_idx.strip()

    # Get condition list to find the name
    list_result = get_list(client)
    if not list_result["ok"]:
        return list_result

    # Find condition by index
    cond_name = None
    for cond in list_result["data"]:
        if cond["idx"] == cond_idx:
            cond_name = cond["name"]
            break

    if cond_name is None:
        return {
            "ok": False,
            "error": {"code": "CONDITION_NOT_FOUND", "msg": f"조건검색 인덱스 {cond_idx}를 찾을 수 없습니다"},
        }

    return search(client, cond_idx, cond_name)
