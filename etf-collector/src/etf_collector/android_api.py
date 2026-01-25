"""Android/Chaquopy integration API for ETF Collector.

This module provides a simplified, JSON-based API for calling ETF Collector
functions from Android/Kotlin via Chaquopy.

All functions return JSON strings to avoid Python object serialization issues
in the Java/Kotlin bridge.

Example usage from Kotlin:
    val py = Python.getInstance()
    val module = py.getModule("etf_collector.android_api")
    val result = module.callAttr("get_constituents", configJson, "069500").toString()
    val parsed = Json.decodeFromString<ApiResponse>(result)
"""

import json
from typing import Any, Dict, Optional

from .auth.kis_auth import KisAuthClient
from .auth.kiwoom_auth import KiwoomAuthClient
from .collector.constituent import ConstituentCollector, EtfConstituentSummary
from .collector.etf_list import EtfListCollector
from .collector.kiwoom_etf_list import KiwoomEtfListCollector
from .config import Config, ConfigError, EtfListSource
from .limiter.rate_limiter import SlidingWindowRateLimiter
from .storage.data_storage import DataStorage, OutputFormat
from .utils.logger import log_info, log_err

MODULE = "android_api"


def _serialize_result(result: Dict[str, Any]) -> str:
    """Serialize result dictionary to JSON string.

    Args:
        result: Result dictionary with ok, data/error keys

    Returns:
        JSON string
    """
    try:
        return json.dumps(result, ensure_ascii=False, default=str)
    except Exception as e:
        return json.dumps({
            "ok": False,
            "error": {"code": "SERIALIZATION_ERROR", "msg": str(e)}
        })


def _parse_config(config_json: str) -> Dict[str, Any]:
    """Parse config JSON string and create Config instance.

    Args:
        config_json: JSON string with configuration

    Returns:
        Dictionary with Config instance or error
    """
    try:
        data = json.loads(config_json)
        config = Config.from_dict(data)
        return {"ok": True, "config": config}
    except json.JSONDecodeError as e:
        return {"ok": False, "error": {"code": "JSON_ERROR", "msg": f"Invalid JSON: {e}"}}
    except ConfigError as e:
        return {"ok": False, "error": {"code": "CONFIG_ERROR", "msg": str(e)}}
    except Exception as e:
        return {"ok": False, "error": {"code": "UNKNOWN_ERROR", "msg": str(e)}}


def get_constituents(config_json: str, etf_code: str, etf_name: str = "") -> str:
    """Get constituent stocks for an ETF.

    Args:
        config_json: JSON string with KIS API configuration
        etf_code: ETF ticker code (6 digits, e.g., "069500")
        etf_name: ETF name (optional)

    Returns:
        JSON string with result:
        - Success: {"ok": true, "data": {...}}
        - Error: {"ok": false, "error": {"code": "...", "msg": "..."}}
    """
    log_info(MODULE, f"get_constituents called", {"etf_code": etf_code})

    # Parse config
    config_result = _parse_config(config_json)
    if not config_result.get("ok"):
        return _serialize_result(config_result)

    config = config_result["config"]

    try:
        # Initialize clients
        auth_client = KisAuthClient(
            app_key=config.app_key,
            app_secret=config.app_secret,
            base_url=config.base_url,
        )
        rate_limiter = SlidingWindowRateLimiter(
            max_requests=config.rate_limit,
            window_seconds=1.0,
            min_interval=0.5,
        )
        collector = ConstituentCollector(
            auth_client=auth_client,
            rate_limiter=rate_limiter,
            base_url=config.base_url,
        )

        # Fetch constituents
        result = collector.get_constituents(etf_code, etf_name)

        # Convert dataclass to dict for JSON serialization
        if result.get("ok") and result.get("data"):
            summary = result["data"]
            result["data"] = _summary_to_dict(summary)

        return _serialize_result(result)

    except Exception as e:
        log_err(MODULE, f"get_constituents failed: {e}")
        return _serialize_result({
            "ok": False,
            "error": {"code": "UNKNOWN_ERROR", "msg": str(e)}
        })


def get_etf_list(config_json: str, use_predefined: bool = False) -> str:
    """Get list of active ETFs.

    Args:
        config_json: JSON string with API configuration
        use_predefined: If True, use predefined ETF codes instead of Kiwoom API

    Returns:
        JSON string with result:
        - Success: {"ok": true, "data": [{"etf_code": "...", "etf_name": "...", ...}, ...]}
        - Error: {"ok": false, "error": {"code": "...", "msg": "..."}}
    """
    log_info(MODULE, "get_etf_list called", {"use_predefined": use_predefined})

    # Parse config
    config_result = _parse_config(config_json)
    if not config_result.get("ok"):
        return _serialize_result(config_result)

    config = config_result["config"]

    try:
        if use_predefined or not config.use_kiwoom_for_etf_list:
            # Use predefined active ETF codes
            from .data.active_etf_codes import ACTIVE_ETF_CODES
            etf_list = [
                {"etf_code": code, "etf_name": "", "etf_type": "active"}
                for code in ACTIVE_ETF_CODES
            ]
            return _serialize_result({"ok": True, "data": etf_list})

        # Use Kiwoom API
        kiwoom_auth = KiwoomAuthClient(
            app_key=config.kiwoom_app_key,
            secret_key=config.kiwoom_secret_key,
            base_url=config.kiwoom_base_url,
        )
        kiwoom_limiter = SlidingWindowRateLimiter(
            max_requests=config.kiwoom_rate_limit,
            window_seconds=1.0,
            min_interval=0.5,
        )
        kiwoom_collector = KiwoomEtfListCollector(
            auth_client=kiwoom_auth,
            rate_limiter=kiwoom_limiter,
            base_url=config.kiwoom_base_url,
        )

        result = kiwoom_collector.get_etf_list()

        # Convert dataclass to dict for JSON serialization
        if result.get("ok") and result.get("data"):
            etf_list = result["data"]
            result["data"] = [_etf_info_to_dict(etf) for etf in etf_list]

        return _serialize_result(result)

    except Exception as e:
        log_err(MODULE, f"get_etf_list failed: {e}")
        return _serialize_result({
            "ok": False,
            "error": {"code": "UNKNOWN_ERROR", "msg": str(e)}
        })


def collect_all_constituents(
    config_json: str,
    etf_codes: Optional[str] = None,
    output_dir: str = "./data",
) -> str:
    """Collect constituents for multiple ETFs and save to file.

    Args:
        config_json: JSON string with API configuration
        etf_codes: Optional JSON array of ETF codes to collect.
                   If None, collects all active ETFs.
        output_dir: Directory to save output files

    Returns:
        JSON string with result:
        - Success: {"ok": true, "data": {"file_path": "...", "etf_count": N, "constituent_count": N}}
        - Error: {"ok": false, "error": {"code": "...", "msg": "..."}}
    """
    log_info(MODULE, "collect_all_constituents called")

    # Parse config
    config_result = _parse_config(config_json)
    if not config_result.get("ok"):
        return _serialize_result(config_result)

    config = config_result["config"]

    try:
        # Parse etf_codes if provided
        if etf_codes:
            codes = json.loads(etf_codes)
            from .collector.etf_list import EtfInfo
            etf_list = [EtfInfo(etf_code=code, etf_name="") for code in codes]
        else:
            # Get ETF list
            etf_list_result = json.loads(get_etf_list(config_json))
            if not etf_list_result.get("ok"):
                return _serialize_result(etf_list_result)
            from .collector.etf_list import EtfInfo
            etf_list = [
                EtfInfo(etf_code=e["etf_code"], etf_name=e.get("etf_name", ""))
                for e in etf_list_result["data"]
            ]

        if not etf_list:
            return _serialize_result({
                "ok": False,
                "error": {"code": "NO_DATA", "msg": "No ETFs to collect"}
            })

        # Initialize collectors
        auth_client = KisAuthClient(
            app_key=config.app_key,
            app_secret=config.app_secret,
            base_url=config.base_url,
        )
        rate_limiter = SlidingWindowRateLimiter(
            max_requests=config.rate_limit,
            window_seconds=1.0,
            min_interval=0.5,
        )
        collector = ConstituentCollector(
            auth_client=auth_client,
            rate_limiter=rate_limiter,
            base_url=config.base_url,
        )

        # Collect all constituents
        result = collector.get_all_constituents(etf_list)

        if not result.get("ok"):
            return _serialize_result(result)

        # Save to file
        storage = DataStorage(output_dir)
        summaries = result["data"]
        file_path = storage.save_full_report(summaries)

        total_constituents = sum(len(s.constituents) for s in summaries)

        return _serialize_result({
            "ok": True,
            "data": {
                "file_path": file_path,
                "etf_count": len(summaries),
                "constituent_count": total_constituents,
            }
        })

    except Exception as e:
        log_err(MODULE, f"collect_all_constituents failed: {e}")
        return _serialize_result({
            "ok": False,
            "error": {"code": "UNKNOWN_ERROR", "msg": str(e)}
        })


def _summary_to_dict(summary: EtfConstituentSummary) -> Dict[str, Any]:
    """Convert EtfConstituentSummary to dictionary.

    Args:
        summary: EtfConstituentSummary instance

    Returns:
        Dictionary representation
    """
    return {
        "etf_code": summary.etf_code,
        "etf_name": summary.etf_name,
        "current_price": summary.current_price,
        "price_change": summary.price_change,
        "price_change_rate": summary.price_change_rate,
        "nav": summary.nav,
        "total_assets": summary.total_assets,
        "cu_unit_count": summary.cu_unit_count,
        "constituent_count": summary.constituent_count,
        "collected_at": summary.collected_at,
        "constituents": [
            {
                "etf_code": c.etf_code,
                "etf_name": c.etf_name,
                "stock_code": c.stock_code,
                "stock_name": c.stock_name,
                "current_price": c.current_price,
                "price_change": c.price_change,
                "price_change_sign": c.price_change_sign,
                "price_change_rate": c.price_change_rate,
                "volume": c.volume,
                "trading_value": c.trading_value,
                "market_cap": c.market_cap,
                "weight": c.weight,
                "evaluation_amount": c.evaluation_amount,
                "collected_at": c.collected_at,
            }
            for c in summary.constituents
        ],
    }


def _etf_info_to_dict(etf) -> Dict[str, Any]:
    """Convert EtfInfo to dictionary.

    Args:
        etf: EtfInfo instance

    Returns:
        Dictionary representation
    """
    return {
        "etf_code": etf.etf_code,
        "etf_name": etf.etf_name,
        "etf_type": getattr(etf, "etf_type", ""),
        "listing_date": getattr(etf, "listing_date", ""),
        "tracking_index": getattr(etf, "tracking_index", ""),
        "asset_class": getattr(etf, "asset_class", ""),
        "management_company": getattr(etf, "management_company", ""),
        "total_assets": getattr(etf, "total_assets", 0),
        "collected_at": getattr(etf, "collected_at", ""),
    }
