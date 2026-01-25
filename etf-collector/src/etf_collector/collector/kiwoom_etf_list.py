"""ETF list collector using Kiwoom API (ka40004).

This module fetches the complete ETF list from Kiwoom's ETF전체시세요청 API.
Unlike the KIS API which returns 500 error for ETF search, Kiwoom API provides
a proper endpoint for fetching all ETF data.
"""

import time
from dataclasses import dataclass
from enum import Enum
from typing import Any, Callable, Dict, List, Optional

import requests

from ..auth.kiwoom_auth import KiwoomAuthClient, KiwoomAuthError
from ..config import DEFAULT_TIMEOUT
from ..limiter.rate_limiter import SlidingWindowRateLimiter
from ..utils.helpers import now_iso
from ..utils.logger import log_info, log_err, log_debug

MODULE = "kiwoom_etf_list"


class KiwoomEtfError(Exception):
    """Kiwoom ETF API error."""


class MarketType(Enum):
    """Stock exchange type for Kiwoom API."""

    ALL = "0"  # All markets
    KOSPI = "1"  # KOSPI only
    KOSDAQ = "2"  # KOSDAQ only
    KRX = "3"  # KRX only (for mock trading)


class ManagementCompany(Enum):
    """ETF management company filter."""

    ALL = "0000"  # All companies
    SAMSUNG = "0001"  # Samsung (KODEX)
    MIRAE = "0002"  # Mirae Asset (TIGER)
    KB = "0003"  # KB Asset (RISE)
    # Add more as needed


@dataclass
class KiwoomEtfInfo:
    """ETF information from Kiwoom API."""

    etf_code: str  # ETF ticker (e.g., "069500")
    etf_name: str  # ETF name (e.g., "KODEX 200")
    etf_type: str  # "Active" or "Passive"
    close_price: int = 0  # Current/close price
    price_change: int = 0  # Price change from previous day
    price_change_sign: str = ""  # Price change sign (1-5)
    price_change_rate: float = 0.0  # Price change rate (%)
    volume: int = 0  # Trading volume
    nav: float = 0.0  # NAV (Net Asset Value)
    tracking_index: str = ""  # Tracking index name
    tracking_index_code: str = ""  # Tracking index code
    tracking_error_rate: float = 0.0  # Tracking error rate (%)
    management_company: str = ""  # Management company code
    multiplier: str = ""  # Multiplier (1X, 2X, etc.)
    listing_date: Optional[str] = None
    asset_class: str = ""
    total_assets: float = 0.0  # In 억원
    collected_at: str = ""

    def __post_init__(self):
        if not self.collected_at:
            self.collected_at = now_iso()

    def is_active(self) -> bool:
        """Check if this is an active ETF."""
        return self.etf_type.lower() == "active"

    def to_etf_info(self):
        """Convert to standard EtfInfo for compatibility."""
        from .etf_list import EtfInfo

        return EtfInfo(
            etf_code=self.etf_code,
            etf_name=self.etf_name,
            etf_type=self.etf_type,
            listing_date=self.listing_date,
            tracking_index=self.tracking_index,
            asset_class=self.asset_class,
            management_company=self.management_company,
            total_assets=self.total_assets,
            collected_at=self.collected_at,
        )


class KiwoomEtfListCollector:
    """Collector for ETF list from Kiwoom API (ka40004).

    This collector fetches the complete ETF list from Kiwoom's ETF전체시세요청 API.
    It provides more comprehensive ETF data compared to the predefined list approach.
    """

    API_ID = "ka40004"
    API_PATH = "/api/dostk/etf"

    # Retry settings
    MAX_RETRIES = 3
    RETRY_DELAY = 1.0  # seconds

    def __init__(
        self,
        auth_client: KiwoomAuthClient,
        rate_limiter: SlidingWindowRateLimiter,
        base_url: str,
    ):
        """Initialize Kiwoom ETF list collector.

        Args:
            auth_client: Kiwoom authentication client
            rate_limiter: Rate limiter instance
            base_url: Kiwoom API base URL
        """
        self.auth = auth_client
        self.limiter = rate_limiter
        self.base_url = base_url

    def get_all_etfs(
        self,
        market_type: MarketType = MarketType.ALL,
        management_company: ManagementCompany = ManagementCompany.ALL,
        progress_callback: Optional[Callable[[int, int, str], None]] = None,
    ) -> Dict[str, Any]:
        """Fetch all ETFs from Kiwoom API (ka40004).

        Args:
            market_type: Stock exchange filter (KOSPI, KOSDAQ, ALL)
            management_company: Management company filter
            progress_callback: Optional callback (current, total, etf_name)

        Returns:
            {"ok": True, "data": List[EtfInfo]} on success
            {"ok": False, "error": {...}} on failure
        """
        log_info(
            MODULE,
            "Fetching ETF list from Kiwoom API",
            {"market": market_type.name, "company": management_company.name},
        )

        try:
            # Prepare request parameters for ka40004
            params = {
                "txon_type": "0",  # Tax type: 0=All
                "navpre": "0",  # NAV comparison: 0=All
                "mngmcomp": management_company.value,  # Management company
                "txon_yn": "0",  # Tax: 0=All
                "trace_idex": "0",  # Tracking index: 0=All
                "stex_tp": market_type.value,  # Exchange type
            }

            result = self._call_api_with_retry(params)

            if not result.get("ok"):
                return result

            raw_data = result["data"]

            # Parse response to EtfInfo objects
            etfs = self._parse_response(raw_data)

            if progress_callback:
                for idx, etf in enumerate(etfs, 1):
                    progress_callback(idx, len(etfs), etf.etf_name)

            log_info(MODULE, f"Fetched {len(etfs)} ETFs from Kiwoom API")

            # Convert to standard EtfInfo for compatibility
            etf_infos = [etf.to_etf_info() for etf in etfs]

            return {
                "ok": True,
                "data": etf_infos,
            }

        except KiwoomAuthError as e:
            log_err(MODULE, f"Authentication error: {e}")
            return {
                "ok": False,
                "error": {"code": "AUTH_ERROR", "msg": str(e)},
            }
        except KiwoomEtfError as e:
            log_err(MODULE, f"ETF API error: {e}")
            return {
                "ok": False,
                "error": {"code": "API_ERROR", "msg": str(e)},
            }
        except Exception as e:
            log_err(MODULE, f"Unexpected error: {e}")
            return {
                "ok": False,
                "error": {"code": "UNKNOWN_ERROR", "msg": str(e)},
            }

    def _call_api(self, params: Dict[str, str]) -> Dict[str, Any]:
        """Make rate-limited API call to Kiwoom.

        Args:
            params: Request parameters

        Returns:
            API response dictionary

        Raises:
            KiwoomEtfError: If API call fails
        """
        # Wait for rate limiter
        self.limiter.wait_if_needed()

        # Get token
        token = self.auth.get_token()

        url = f"{self.base_url}{self.API_PATH}"
        headers = {
            "api-id": self.API_ID,
            "authorization": token.bearer,
            "Content-Type": "application/json;charset=UTF-8",
        }

        log_debug(MODULE, "Calling Kiwoom ETF API", {"url": url, "params": params})

        try:
            resp = requests.post(
                url,
                json=params,
                headers=headers,
                timeout=DEFAULT_TIMEOUT,
            )
            resp.raise_for_status()
            data = resp.json()

            # Check Kiwoom API return code
            return_code = data.get("return_code")
            if return_code != 0:
                return_msg = data.get("return_msg", "Unknown error")
                return {
                    "ok": False,
                    "error": {"code": f"KIWOOM_{return_code}", "msg": return_msg},
                }

            return {"ok": True, "data": data}

        except requests.exceptions.Timeout:
            raise KiwoomEtfError("Request timed out")
        except requests.exceptions.ConnectionError as e:
            raise KiwoomEtfError(f"Connection error: {e}")
        except requests.exceptions.HTTPError as e:
            raise KiwoomEtfError(f"HTTP error: {resp.status_code} - {e}")

    def _call_api_with_retry(self, params: Dict[str, str]) -> Dict[str, Any]:
        """Make API call with retry logic.

        Args:
            params: Request parameters

        Returns:
            API response dictionary
        """
        last_error = None

        for attempt in range(self.MAX_RETRIES):
            try:
                result = self._call_api(params)

                # Check for rate limit error
                if not result.get("ok"):
                    error_code = result.get("error", {}).get("code", "")
                    if "EGW00201" in error_code:  # Rate limit
                        log_info(
                            MODULE,
                            f"Rate limited, retrying (attempt {attempt + 1}/{self.MAX_RETRIES})",
                        )
                        time.sleep(self.RETRY_DELAY * (2**attempt))
                        continue

                return result

            except KiwoomEtfError as e:
                last_error = e
                if attempt < self.MAX_RETRIES - 1:
                    log_info(
                        MODULE,
                        f"Retrying (attempt {attempt + 1}/{self.MAX_RETRIES}): {e}",
                    )
                    time.sleep(self.RETRY_DELAY * (2**attempt))
                continue

        if last_error:
            raise last_error

        return {
            "ok": False,
            "error": {"code": "RETRY_FAILED", "msg": "Max retries exceeded"},
        }

    def _parse_response(self, data: Dict[str, Any]) -> List[KiwoomEtfInfo]:
        """Parse Kiwoom API response to EtfInfo objects.

        Args:
            data: Raw API response

        Returns:
            List of KiwoomEtfInfo objects
        """
        etfs: List[KiwoomEtfInfo] = []

        # The response should contain a list of ETFs
        # Response field name may vary - try common patterns
        etf_list = data.get("list") or data.get("etf_list") or data.get("output") or []

        if not isinstance(etf_list, list):
            log_err(MODULE, "Unexpected response format", {"data_keys": list(data.keys())})
            return etfs

        for item in etf_list:
            try:
                etf_code = item.get("stk_cd", "")
                etf_name = item.get("stk_nm", "")

                if not etf_code:
                    continue

                # Determine ETF type from name
                etf_type = self._determine_etf_type(etf_name)

                # Parse numeric fields safely
                close_price = self._parse_int(item.get("close_pric", 0))
                price_change = self._parse_int(item.get("pred_pre", 0))
                price_change_rate = self._parse_float(item.get("pre_rt", 0.0))
                volume = self._parse_int(item.get("trde_qty", 0))
                nav = self._parse_float(item.get("nav", 0.0))
                tracking_error = self._parse_float(item.get("trace_eor_rt", 0.0))

                etf = KiwoomEtfInfo(
                    etf_code=etf_code,
                    etf_name=etf_name,
                    etf_type=etf_type,
                    close_price=close_price,
                    price_change=price_change,
                    price_change_sign=item.get("pre_sig", ""),
                    price_change_rate=price_change_rate,
                    volume=volume,
                    nav=nav,
                    tracking_index=item.get("trace_idex_nm", ""),
                    tracking_index_code=item.get("trace_idex_cd", ""),
                    tracking_error_rate=tracking_error,
                    management_company=item.get("mngmcomp", ""),
                    multiplier=item.get("drng", ""),
                )
                etfs.append(etf)

            except Exception as e:
                log_err(MODULE, f"Error parsing ETF: {e}", {"item": item})
                continue

        log_info(MODULE, f"Parsed {len(etfs)} ETFs from response")
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

    def _parse_int(self, value) -> int:
        """Safely parse integer value."""
        if isinstance(value, int):
            return value
        if isinstance(value, str):
            try:
                return int(value.replace(",", ""))
            except ValueError:
                return 0
        return 0

    def _parse_float(self, value) -> float:
        """Safely parse float value."""
        if isinstance(value, (int, float)):
            return float(value)
        if isinstance(value, str):
            try:
                return float(value.replace(",", ""))
            except ValueError:
                return 0.0
        return 0.0
