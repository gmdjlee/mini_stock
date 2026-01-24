"""ETF list collector using predefined Active ETF codes.

Note: This module uses KIS API (Korea Investment & Securities),
NOT Kiwoom API. The two are different API providers.

Important: KIS API's stock search endpoint (CTPF1604R) does not work for ETFs
and returns HTTP 500 error. This module creates EtfInfo objects directly from
the predefined ACTIVE_ETF_CODES list. Detailed ETF info (NAV, total assets, etc.)
is fetched by ConstituentCollector via the ETF component API (FHKST121600C0).
"""

from dataclasses import dataclass
from typing import Any, Callable, Dict, List, Optional

from ..auth.kis_auth import KisAuthClient
from ..data.active_etf_codes import ACTIVE_ETF_CODES, get_active_etf_codes
from ..limiter.rate_limiter import SlidingWindowRateLimiter
from ..utils.helpers import now_iso
from ..utils.logger import log_info

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
    """Collector for ETF list from predefined codes.

    Note: This collector does not make API calls for ETF basic info because
    KIS API's stock search endpoint (CTPF1604R) returns HTTP 500 error for ETFs.
    Instead, it creates EtfInfo objects from the predefined ACTIVE_ETF_CODES list.
    """

    def __init__(
        self,
        auth_client: KisAuthClient,
        rate_limiter: SlidingWindowRateLimiter,
        base_url: str,
    ):
        """Initialize ETF list collector.

        Args:
            auth_client: KIS authentication client (not used, kept for interface compatibility)
            rate_limiter: Rate limiter instance (not used, kept for interface compatibility)
            base_url: KIS API base URL (not used, kept for interface compatibility)
        """
        self.auth = auth_client
        self.limiter = rate_limiter
        self.base_url = base_url

    def get_all_etfs(
        self,
        etf_codes: Optional[List[str]] = None,
        progress_callback: Optional[Callable[[int, int, str], None]] = None,
    ) -> Dict[str, Any]:
        """Get ETFs from predefined list.

        Note: KIS API's stock search endpoint (CTPF1604R) does not work for ETFs
        and returns 500 error. Instead, we create EtfInfo objects directly from
        the predefined ACTIVE_ETF_CODES list. Detailed ETF info (NAV, assets, etc.)
        will be fetched by ConstituentCollector via the ETF component API.

        Args:
            etf_codes: Optional list of ETF codes to use. If None,
                      uses the predefined ACTIVE_ETF_CODES list.
            progress_callback: Optional callback (current, total, etf_name)

        Returns:
            {"ok": True, "data": List[EtfInfo]} on success
        """
        code_name_map = {code: name for code, name in ACTIVE_ETF_CODES}
        codes_to_fetch = etf_codes if etf_codes else get_active_etf_codes()

        log_info(MODULE, f"Fetching {len(codes_to_fetch)} ETFs from predefined list")

        etfs: List[EtfInfo] = []

        for idx, etf_code in enumerate(codes_to_fetch, 1):
            etf_name = code_name_map.get(etf_code, f"ETF-{etf_code}")
            if progress_callback:
                progress_callback(idx, len(codes_to_fetch), etf_name)

            # Determine ETF type from name
            etf_type = self._determine_etf_type(etf_name)

            etf = EtfInfo(
                etf_code=etf_code,
                etf_name=etf_name,
                etf_type=etf_type,
            )
            etfs.append(etf)

        log_info(
            MODULE,
            f"Loaded {len(etfs)} ETFs from predefined list",
        )

        return {
            "ok": True,
            "data": etfs,
        }

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
