"""ETF list collector using KIS API.

Note: This module uses KIS API (Korea Investment & Securities),
NOT Kiwoom API. The two are different API providers.

The KIS API search-info endpoint (CTPF1604R) does not support
bulk ETF listing with empty PDNO parameter. Therefore, this module
uses a predefined list of Active ETF codes and fetches each one
individually.
"""

from dataclasses import dataclass
from datetime import datetime
from typing import Any, Callable, Dict, List, Optional

import requests

from ..auth.kis_auth import KisAuthClient
from ..config import ENDPOINTS, DEFAULT_TIMEOUT
from ..data.active_etf_codes import ACTIVE_ETF_CODES, get_active_etf_codes
from ..limiter.rate_limiter import SlidingWindowRateLimiter
from ..utils.helpers import to_float, now_iso
from ..utils.logger import log_info, log_err, log_debug, log_warn

MODULE = "etf_list"


@dataclass
class EtfInfo:
    """ETF basic information."""

    etf_code: str  # ETF ticker (e.g., "069500")
    etf_name: str  # ETF name (e.g., "KODEX 200")
    etf_type: str  # "Active" or "Passive"
    listing_date: Optional[str] = None
    tracking_index: str = ""
    asset_class: str = ""
    management_company: str = ""
    total_assets: float = 0.0  # In 억원
    collected_at: str = ""

    def __post_init__(self):
        if not self.collected_at:
            self.collected_at = now_iso()

    def is_active(self) -> bool:
        """Check if this is an active ETF."""
        return self.etf_type.lower() == "active"


class EtfListCollector:
    """Collector for ETF list using CTPF1604R API."""

    def __init__(
        self,
        auth_client: KisAuthClient,
        rate_limiter: SlidingWindowRateLimiter,
        base_url: str,
    ):
        """Initialize ETF list collector.

        Args:
            auth_client: KIS authentication client
            rate_limiter: Rate limiter instance
            base_url: KIS API base URL
        """
        self.auth = auth_client
        self.limiter = rate_limiter
        self.base_url = base_url

    def get_all_etfs(
        self,
        etf_codes: Optional[List[str]] = None,
        progress_callback: Optional[Callable[[int, int, str], None]] = None,
    ) -> Dict[str, Any]:
        """Fetch ETFs from predefined list using KIS API.

        Note: KIS API (CTPF1604R) does not support bulk ETF listing with
        empty PDNO parameter. This method uses a predefined list of Active
        ETF codes and fetches each one individually.

        Args:
            etf_codes: Optional list of ETF codes to fetch. If None,
                      uses the predefined ACTIVE_ETF_CODES list.
            progress_callback: Optional callback (current, total, etf_name)

        Returns:
            {"ok": True, "data": List[EtfInfo]} on success
            {"ok": False, "error": {"code": str, "msg": str}} on failure
        """
        codes_to_fetch = etf_codes if etf_codes else get_active_etf_codes()
        code_name_map = {code: name for code, name in ACTIVE_ETF_CODES}

        log_info(MODULE, f"Fetching {len(codes_to_fetch)} ETFs from predefined list")

        etfs: List[EtfInfo] = []
        errors: List[Dict[str, Any]] = []

        for idx, etf_code in enumerate(codes_to_fetch, 1):
            etf_name = code_name_map.get(etf_code, "")
            if progress_callback:
                progress_callback(idx, len(codes_to_fetch), etf_name or etf_code)

            result = self.get_etf_by_code(etf_code)

            if result.get("ok"):
                etf = result["data"]
                # Update name from predefined list if API doesn't return it
                if not etf.etf_name and etf_name:
                    etf.etf_name = etf_name
                etfs.append(etf)
            else:
                error_info = {
                    "etf_code": etf_code,
                    "etf_name": etf_name,
                    "error": result.get("error", {}),
                }
                errors.append(error_info)
                log_warn(
                    MODULE,
                    f"Failed to fetch ETF {etf_code}",
                    {"error": result.get("error", {})},
                )

        log_info(
            MODULE,
            f"Fetched {len(etfs)} ETFs",
            {"success": len(etfs), "failed": len(errors)},
        )

        if not etfs and errors:
            return {
                "ok": False,
                "error": {
                    "code": "ALL_FAILED",
                    "msg": f"All {len(errors)} ETFs failed to fetch",
                    "details": errors,
                },
            }

        return {
            "ok": True,
            "data": etfs,
            "errors": errors if errors else None,
        }

    def get_etf_by_code(self, etf_code: str) -> Dict[str, Any]:
        """Fetch a specific ETF by code.

        Args:
            etf_code: ETF ticker code

        Returns:
            {"ok": True, "data": EtfInfo} on success
            {"ok": False, "error": {"code": str, "msg": str}} on failure
        """
        log_info(MODULE, f"Fetching ETF: {etf_code}")

        params = {
            "PDNO": etf_code,
            "PRDT_TYPE_CD": "300",
        }

        result = self._call_api(params)
        if not result.get("ok"):
            return result

        try:
            etfs = self._parse_etf_list(result["data"])
            if not etfs:
                return {
                    "ok": False,
                    "error": {"code": "NOT_FOUND", "msg": f"ETF {etf_code} not found"},
                }
            return {"ok": True, "data": etfs[0]}
        except Exception as e:
            log_err(MODULE, f"Failed to parse ETF: {e}")
            return {"ok": False, "error": {"code": "PARSE_ERROR", "msg": str(e)}}

    def _call_api(self, params: Dict[str, str]) -> Dict[str, Any]:
        """Make rate-limited API call.

        Args:
            params: Query parameters

        Returns:
            {"ok": True, "data": response_data} or
            {"ok": False, "error": {...}}
        """
        # Acquire rate limit
        self.limiter.wait_if_needed()

        url = f"{self.base_url}{ENDPOINTS['stock_search']}"
        token = self.auth.get_token()

        headers = {
            "Content-Type": "application/json; charset=utf-8",
            "authorization": token.authorization,
            "appkey": self.auth.app_key,
            "appsecret": self.auth.app_secret,
            "tr_id": "CTPF1604R",
            "custtype": "P",  # Personal
        }

        try:
            log_debug(MODULE, f"Calling API", {"url": url, "params": params})
            resp = requests.get(url, params=params, headers=headers, timeout=DEFAULT_TIMEOUT)
            resp.raise_for_status()
            data = resp.json()

            # Check API response code
            rt_cd = data.get("rt_cd", "")
            if rt_cd != "0":
                msg_cd = data.get("msg_cd", "")
                msg = data.get("msg1", "Unknown error")
                log_err(MODULE, f"API error: {msg_cd} - {msg}")
                return {"ok": False, "error": {"code": msg_cd, "msg": msg}}

            return {"ok": True, "data": data}

        except requests.exceptions.Timeout:
            log_err(MODULE, "API request timed out")
            return {"ok": False, "error": {"code": "TIMEOUT", "msg": "Request timed out"}}
        except requests.exceptions.ConnectionError as e:
            log_err(MODULE, f"Connection error: {e}")
            return {"ok": False, "error": {"code": "CONNECTION_ERROR", "msg": str(e)}}
        except requests.exceptions.HTTPError as e:
            log_err(MODULE, f"HTTP error: {e}")
            return {"ok": False, "error": {"code": "HTTP_ERROR", "msg": str(e)}}
        except Exception as e:
            log_err(MODULE, f"Unexpected error: {e}")
            return {"ok": False, "error": {"code": "UNKNOWN_ERROR", "msg": str(e)}}

    def _parse_etf_list(self, data: Dict[str, Any]) -> List[EtfInfo]:
        """Parse ETF list from API response.

        Args:
            data: Raw API response

        Returns:
            List of EtfInfo objects
        """
        output = data.get("output", [])
        if not isinstance(output, list):
            output = [output] if output else []

        etfs = []
        for item in output:
            etf_name = item.get("prdt_abrv_name", "") or item.get("prdt_name", "")
            etf_code = item.get("pdno", "") or item.get("shtn_pdno", "")

            if not etf_code:
                continue

            # Determine if Active ETF based on name
            etf_type = self._determine_etf_type(etf_name)

            etf = EtfInfo(
                etf_code=etf_code,
                etf_name=etf_name,
                etf_type=etf_type,
                listing_date=item.get("lstg_dt", ""),
                tracking_index=item.get("idx_bztp_scls_cd_name", ""),
                asset_class=item.get("idx_bztp_lcls_cd_name", ""),
                management_company=item.get("etf_cmpn_cd_name", ""),
                total_assets=to_float(item.get("etf_assr_ttam", 0)) / 100_000_000,  # 원 -> 억원
            )
            etfs.append(etf)

        return etfs

    def _determine_etf_type(self, etf_name: str) -> str:
        """Determine ETF type based on name.

        Args:
            etf_name: ETF name

        Returns:
            "Active" or "Passive"
        """
        name_lower = etf_name.lower()
        if "액티브" in etf_name or "active" in name_lower:
            return "Active"
        return "Passive"
