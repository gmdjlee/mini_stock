"""ETF constituent stock collector using KIS API (FHKST121600C0)."""

from dataclasses import dataclass
from datetime import datetime
from typing import Any, Callable, Dict, List, Optional

import requests

from ..auth.kis_auth import KisAuthClient
from ..config import ENDPOINTS, DEFAULT_TIMEOUT, ERROR_CODES
from ..limiter.rate_limiter import SlidingWindowRateLimiter
from ..utils.helpers import to_int, to_float, now_iso
from ..utils.logger import log_info, log_err, log_debug, log_warn
from .etf_list import EtfInfo

MODULE = "constituent"


@dataclass
class ConstituentStock:
    """ETF constituent stock information."""

    etf_code: str  # ETF ticker
    etf_name: str  # ETF name
    stock_code: str  # Constituent stock code (stck_shrn_iscd)
    stock_name: str  # Constituent stock name (hts_kor_isnm)
    current_price: int  # Current price (stck_prpr)
    price_change: int  # Price change (prdy_vrss)
    price_change_sign: str  # Change sign (1:상한, 2:상승, 3:보합, 4:하한, 5:하락)
    price_change_rate: float  # Change rate % (prdy_ctrt)
    volume: int  # Trading volume (acml_vol)
    trading_value: int  # Trading value (acml_tr_pbmn)
    market_cap: int  # Market cap (hts_avls)
    weight: float  # Weight in ETF % (etf_cnfg_issu_rlim)
    evaluation_amount: int  # Evaluation amount (etf_vltn_amt)
    collected_at: str = ""

    def __post_init__(self):
        if not self.collected_at:
            self.collected_at = now_iso()


@dataclass
class EtfConstituentSummary:
    """ETF constituent stock summary."""

    etf_code: str
    etf_name: str
    current_price: int  # ETF current price (output1.stck_prpr)
    price_change: int  # ETF price change (output1.prdy_vrss)
    price_change_rate: float  # ETF change rate (output1.prdy_ctrt)
    nav: float  # NAV (output1.nav)
    total_assets: int  # Total assets (output1.etf_ntas_ttam)
    cu_unit_count: int  # CU unit securities count (output1.etf_cu_unit_scrt_cnt)
    constituent_count: int  # Total constituent count (output1.etf_cnfg_issu_cnt)
    constituents: List[ConstituentStock]
    collected_at: str = ""

    def __post_init__(self):
        if not self.collected_at:
            self.collected_at = now_iso()


class ConstituentCollector:
    """Collector for ETF constituent stocks using FHKST121600C0 API."""

    def __init__(
        self,
        auth_client: KisAuthClient,
        rate_limiter: SlidingWindowRateLimiter,
        base_url: str,
    ):
        """Initialize constituent collector.

        Args:
            auth_client: KIS authentication client
            rate_limiter: Rate limiter instance
            base_url: KIS API base URL
        """
        self.auth = auth_client
        self.limiter = rate_limiter
        self.base_url = base_url
        self.max_retries = 3
        self.retry_delay = 1.0

    def get_constituents(self, etf_code: str, etf_name: str = "") -> Dict[str, Any]:
        """Fetch constituent stocks for a single ETF.

        Args:
            etf_code: ETF ticker code (e.g., "069500")
            etf_name: ETF name (optional, for reference)

        Returns:
            {"ok": True, "data": EtfConstituentSummary} on success
            {"ok": False, "error": {"code": str, "msg": str}} on failure
        """
        log_info(MODULE, f"Fetching constituents for {etf_code} ({etf_name})")

        # API parameters for FHKST121600C0
        params = {
            "FID_COND_MRKT_DIV_CODE": "J",  # 주식/ETF/ETN
            "FID_INPUT_ISCD": etf_code,
            "FID_COND_SCR_DIV_CODE": "11216",
        }

        result = self._call_api_with_retry(params)
        if not result.get("ok"):
            return result

        try:
            summary = self._parse_response(etf_code, etf_name, result["data"])
            log_info(
                MODULE,
                f"Fetched {len(summary.constituents)} constituents for {etf_code}",
            )
            return {"ok": True, "data": summary}
        except Exception as e:
            log_err(MODULE, f"Failed to parse constituents: {e}")
            return {"ok": False, "error": {"code": "PARSE_ERROR", "msg": str(e)}}

    def get_all_constituents(
        self,
        etf_list: List[EtfInfo],
        progress_callback: Optional[Callable[[int, int, str], None]] = None,
    ) -> Dict[str, Any]:
        """Fetch constituents for multiple ETFs.

        Args:
            etf_list: List of EtfInfo objects
            progress_callback: Optional callback (current, total, etf_name)

        Returns:
            {"ok": True, "data": List[EtfConstituentSummary]} on success
            {"ok": False, "error": {...}} on partial/full failure
        """
        log_info(MODULE, f"Fetching constituents for {len(etf_list)} ETFs")

        results: List[EtfConstituentSummary] = []
        errors: List[Dict[str, Any]] = []

        for idx, etf in enumerate(etf_list, 1):
            if progress_callback:
                progress_callback(idx, len(etf_list), etf.etf_name)

            result = self.get_constituents(etf.etf_code, etf.etf_name)

            if result.get("ok"):
                results.append(result["data"])
            else:
                errors.append(
                    {
                        "etf_code": etf.etf_code,
                        "etf_name": etf.etf_name,
                        "error": result.get("error", {}),
                    }
                )
                log_warn(
                    MODULE,
                    f"Failed to fetch constituents for {etf.etf_code}",
                    {"error": result.get("error", {})},
                )

        log_info(
            MODULE,
            f"Collection complete",
            {"success": len(results), "failed": len(errors)},
        )

        if not results and errors:
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
            "data": results,
            "errors": errors if errors else None,
        }

    def _call_api_with_retry(self, params: Dict[str, str]) -> Dict[str, Any]:
        """Make API call with retry on rate limit errors.

        Args:
            params: Query parameters

        Returns:
            API response or error
        """
        import time

        for attempt in range(self.max_retries + 1):
            result = self._call_api(params)

            if result.get("ok"):
                return result

            error_code = result.get("error", {}).get("code", "")

            # Retry on rate limit error
            if error_code == "EGW00201":
                if attempt < self.max_retries:
                    delay = self.retry_delay * (2**attempt)  # Exponential backoff
                    log_warn(
                        MODULE,
                        f"Rate limit hit, retrying in {delay}s",
                        {"attempt": attempt + 1},
                    )
                    time.sleep(delay)
                    continue

            # Retry on token expired
            if error_code == "EGW00123":
                log_info(MODULE, "Token expired, refreshing")
                self.auth.get_token(force_refresh=True)
                if attempt < self.max_retries:
                    continue

            # No retry for other errors
            return result

        return result

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

        url = f"{self.base_url}{ENDPOINTS['etf_component']}"
        token = self.auth.get_token()

        headers = {
            "Content-Type": "application/json; charset=utf-8",
            "authorization": token.authorization,
            "appkey": self.auth.app_key,
            "appsecret": self.auth.app_secret,
            "tr_id": "FHKST121600C0",
            "custtype": "P",
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
                msg = data.get("msg1", ERROR_CODES.get(msg_cd, "Unknown error"))
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

    def _parse_response(
        self,
        etf_code: str,
        etf_name: str,
        data: Dict[str, Any],
    ) -> EtfConstituentSummary:
        """Parse constituent response.

        Args:
            etf_code: ETF code
            etf_name: ETF name
            data: Raw API response

        Returns:
            EtfConstituentSummary object
        """
        output1 = data.get("output1", {})
        output2 = data.get("output2", [])

        # Parse ETF summary from output1
        constituents = []
        for item in output2:
            stock = ConstituentStock(
                etf_code=etf_code,
                etf_name=etf_name,
                stock_code=item.get("stck_shrn_iscd", ""),
                stock_name=item.get("hts_kor_isnm", ""),
                current_price=to_int(item.get("stck_prpr", 0)),
                price_change=to_int(item.get("prdy_vrss", 0)),
                price_change_sign=item.get("prdy_vrss_sign", "3"),
                price_change_rate=to_float(item.get("prdy_ctrt", 0)),
                volume=to_int(item.get("acml_vol", 0)),
                trading_value=to_int(item.get("acml_tr_pbmn", 0)),
                market_cap=to_int(item.get("hts_avls", 0)),
                weight=to_float(item.get("etf_cnfg_issu_rlim", 0)),
                evaluation_amount=to_int(item.get("etf_vltn_amt", 0)),
            )
            if stock.stock_code:  # Skip empty entries
                constituents.append(stock)

        return EtfConstituentSummary(
            etf_code=etf_code,
            etf_name=etf_name,
            current_price=to_int(output1.get("stck_prpr", 0)),
            price_change=to_int(output1.get("prdy_vrss", 0)),
            price_change_rate=to_float(output1.get("prdy_ctrt", 0)),
            nav=to_float(output1.get("nav", 0)),
            total_assets=to_int(output1.get("etf_ntas_ttam", 0)),
            cu_unit_count=to_int(output1.get("etf_cu_unit_scrt_cnt", 0)),
            constituent_count=to_int(output1.get("etf_cnfg_issu_cnt", 0)),
            constituents=constituents,
        )
