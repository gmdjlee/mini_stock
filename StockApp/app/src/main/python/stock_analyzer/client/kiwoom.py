"""Kiwoom REST API client."""

import time
from dataclasses import dataclass
from typing import Any, Dict, Optional

import requests

from ..core.log import log_err, log_info, log_warn
from .auth import AuthClient


# Rate limiting settings
DEFAULT_MIN_INTERVAL = 0.5  # Minimum seconds between API calls
DEFAULT_MAX_RETRIES = 3  # Maximum retry attempts for 429 errors
DEFAULT_RETRY_BASE_DELAY = 1.0  # Base delay for exponential backoff


@dataclass
class ApiResponse:
    """API response wrapper."""

    ok: bool
    data: Optional[Any] = None
    error: Optional[Dict[str, str]] = None
    has_next: bool = False
    next_key: Optional[str] = None


class KiwoomClient:
    """Kiwoom REST API wrapper."""

    def __init__(
        self,
        app_key: str,
        secret_key: str,
        base_url: str = "https://api.kiwoom.com",
        min_interval: float = DEFAULT_MIN_INTERVAL,
        max_retries: int = DEFAULT_MAX_RETRIES,
        retry_base_delay: float = DEFAULT_RETRY_BASE_DELAY,
    ):
        """
        Initialize Kiwoom client.

        Args:
            app_key: Kiwoom API app key
            secret_key: Kiwoom API secret key
            base_url: API base URL
            min_interval: Minimum seconds between API calls
            max_retries: Maximum retry attempts for 429 errors
            retry_base_delay: Base delay for exponential backoff
        """
        self.base_url = base_url
        self.auth = AuthClient(app_key, secret_key, base_url)

        # Rate limiting
        self._min_interval = min_interval
        self._max_retries = max_retries
        self._retry_base_delay = retry_base_delay
        self._last_call_time: float = 0

    def _wait_for_rate_limit(self) -> None:
        """Wait if needed to respect rate limit."""
        now = time.time()
        elapsed = now - self._last_call_time
        if elapsed < self._min_interval:
            sleep_time = self._min_interval - elapsed
            time.sleep(sleep_time)
        self._last_call_time = time.time()

    def _call(
        self,
        api_id: str,
        url: str,
        body: Dict[str, Any],
        cont_yn: str = "",
        next_key: str = "",
        timeout: int = 30,
    ) -> ApiResponse:
        """
        Call API endpoint with rate limiting and retry on 429.

        Args:
            api_id: API identifier
            url: API endpoint path
            body: Request body
            cont_yn: Continuation flag (Y/N)
            next_key: Next key for pagination
            timeout: Request timeout in seconds

        Returns:
            ApiResponse object
        """
        token = self.auth.get_token()

        headers = {
            "api-id": api_id,
            "authorization": token.bearer,
            "Content-Type": "application/json;charset=UTF-8",
        }
        if cont_yn:
            headers["cont-yn"] = cont_yn
        if next_key:
            headers["next-key"] = next_key

        full_url = f"{self.base_url}{url}"

        for attempt in range(self._max_retries + 1):
            # Apply rate limiting
            self._wait_for_rate_limit()

            try:
                resp = requests.post(
                    full_url,
                    headers=headers,
                    json=body,
                    timeout=timeout,
                )

                # Handle 429 rate limit with retry
                if resp.status_code == 429:
                    if attempt < self._max_retries:
                        delay = self._retry_base_delay * (2 ** attempt)
                        log_warn(
                            "client.kiwoom",
                            f"Rate limited, retrying in {delay:.1f}s",
                            {"api_id": api_id, "attempt": attempt + 1},
                        )
                        time.sleep(delay)
                        continue
                    else:
                        log_err("client.kiwoom", "Rate limit exceeded", {"api_id": api_id})
                        return ApiResponse(
                            ok=False,
                            error={"code": "RATE_LIMIT", "msg": "API 호출 한도 초과"},
                        )

                resp.raise_for_status()
                data = resp.json()

                # Extract continuation info from headers
                has_next = resp.headers.get("cont-yn", "N") == "Y"
                resp_next_key = resp.headers.get("next-key", "")

                if data.get("return_code", 0) != 0:
                    return ApiResponse(
                        ok=False,
                        error={
                            "code": str(data.get("return_code")),
                            "msg": data.get("return_msg", "Unknown error"),
                        },
                    )

                log_info("client.kiwoom", "API call", {"api_id": api_id})

                return ApiResponse(
                    ok=True,
                    data=data,
                    has_next=has_next,
                    next_key=resp_next_key,
                )

            except requests.Timeout:
                return ApiResponse(
                    ok=False,
                    error={"code": "TIMEOUT", "msg": "Request timeout"},
                )
            except requests.RequestException as e:
                log_err("client.kiwoom", e, {"api_id": api_id, "url": url})
                return ApiResponse(
                    ok=False,
                    error={"code": "NETWORK_ERROR", "msg": str(e)},
                )

        # Should not reach here, but just in case
        return ApiResponse(
            ok=False,
            error={"code": "UNKNOWN_ERROR", "msg": "Unexpected error"},
        )

    # ========== Stock Search ==========

    def get_stock_list(
        self,
        market: str = "0",
        cont_yn: str = "",
        next_key: str = "",
    ) -> ApiResponse:
        """
        Get stock list (ka10099).

        Args:
            market: Market type (0: All, 1: KOSPI, 2: KOSDAQ)
            cont_yn: Continuation flag (Y/N) for pagination
            next_key: Next key for pagination

        Returns:
            ApiResponse with stock list
        """
        return self._call(
            "ka10099",
            "/api/dostk/stkinfo",
            {"mrkt_tp": market},
            cont_yn=cont_yn,
            next_key=next_key,
        )

    def get_stock_info(self, ticker: str) -> ApiResponse:
        """
        Get stock basic info (ka10001).

        Args:
            ticker: Stock code

        Returns:
            ApiResponse with stock info (name, price, market cap, PER, PBR, etc.)
        """
        return self._call(
            "ka10001",
            "/api/dostk/stkinfo",
            {"stk_cd": ticker},
        )

    # ========== Supply/Demand Analysis ==========

    def get_foreign_trend(self, ticker: str) -> ApiResponse:
        """
        Get foreign investor trading trend (ka10008).

        Args:
            ticker: Stock code

        Returns:
            ApiResponse with foreign net buy, holding quantity, holding ratio, etc.
        """
        return self._call(
            "ka10008",
            "/api/dostk/frgnistt",
            {"stk_cd": ticker},
        )

    def get_institution_trend(self, ticker: str) -> ApiResponse:
        """
        Get institutional trading trend (ka10045).

        Args:
            ticker: Stock code

        Returns:
            ApiResponse with institutional net buy details
        """
        return self._call(
            "ka10045",
            "/api/dostk/stkinfo",
            {"stk_cd": ticker},
        )

    def get_investor_trend(
        self,
        ticker: str,
        date: str = None,
        amt_qty_tp: str = "1",
        trde_tp: str = "0",
        unit_tp: str = "1000",
    ) -> ApiResponse:
        """
        Get investor/institution trend by stock (ka10059).

        Args:
            ticker: Stock code
            date: Base date (YYYYMMDD), defaults to yesterday
            amt_qty_tp: Amount/quantity type (1: Amount, 2: Quantity)
            trde_tp: Trade type (0: Net buy, 1: Buy, 2: Sell)
            unit_tp: Unit type (1000, etc.)

        Returns:
            ApiResponse with investor trading trend
        """
        from ..core.date import days_ago

        # Use yesterday's date by default to ensure data availability
        # (today's data may not be available if market is closed or after hours)
        dt = date or days_ago(1)
        return self._call(
            "ka10059",
            "/api/dostk/stkinfo",
            {
                "dt": dt,
                "stk_cd": ticker,
                "amt_qty_tp": amt_qty_tp,
                "trde_tp": trde_tp,
                "unit_tp": unit_tp,
            },
        )

    def get_investor_summary(
        self,
        ticker: str,
        start_date: str = None,
        end_date: str = None,
        days: int = 30,
        amt_qty_tp: str = "1",
        trde_tp: str = "0",
        unit_tp: str = "1000",
    ) -> ApiResponse:
        """
        Get investor/institution summary by stock (ka10061).

        Args:
            ticker: Stock code
            start_date: Start date (YYYYMMDD), defaults to `days` ago
            end_date: End date (YYYYMMDD), defaults to today
            days: Number of days (used if start_date not provided)
            amt_qty_tp: Amount/quantity type (1: Amount, 2: Quantity)
            trde_tp: Trade type (0: Net buy, 1: Buy, 2: Sell)
            unit_tp: Unit type (1000, etc.)

        Returns:
            ApiResponse with investor summary
        """
        from ..core.date import days_ago, today_str

        end_dt = end_date or today_str()
        strt_dt = start_date or days_ago(days)
        return self._call(
            "ka10061",
            "/api/dostk/stkinfo",
            {
                "stk_cd": ticker,
                "strt_dt": strt_dt,
                "end_dt": end_dt,
                "amt_qty_tp": amt_qty_tp,
                "trde_tp": trde_tp,
                "unit_tp": unit_tp,
            },
        )

    # ========== Chart Data ==========

    def get_daily_chart(
        self,
        ticker: str,
        start_date: str,
        end_date: str,
        adj_price: str = "1",
    ) -> ApiResponse:
        """
        Get daily chart data (ka10081).

        Args:
            ticker: Stock code
            start_date: Start date (YYYYMMDD) - not used by API, kept for interface
            end_date: End date (YYYYMMDD) - used as base_dt
            adj_price: Adjusted price flag (0: No, 1: Yes) - maps to upd_stkpc_tp

        Returns:
            ApiResponse with OHLCV data
        """
        return self._call(
            "ka10081",
            "/api/dostk/chart",
            {
                "stk_cd": ticker,
                "base_dt": end_date,
                "upd_stkpc_tp": adj_price,
            },
        )

    def get_weekly_chart(
        self,
        ticker: str,
        start_date: str,
        end_date: str,
        adj_price: str = "1",
    ) -> ApiResponse:
        """
        Get weekly chart data (ka10082).

        Args:
            ticker: Stock code
            start_date: Start date (YYYYMMDD) - not used by API, kept for interface
            end_date: End date (YYYYMMDD) - used as base_dt
            adj_price: Adjusted price flag (0: No, 1: Yes) - maps to upd_stkpc_tp

        Returns:
            ApiResponse with OHLCV data
        """
        return self._call(
            "ka10082",
            "/api/dostk/chart",
            {
                "stk_cd": ticker,
                "base_dt": end_date,
                "upd_stkpc_tp": adj_price,
            },
        )

    def get_monthly_chart(
        self,
        ticker: str,
        start_date: str,
        end_date: str,
        adj_price: str = "1",
    ) -> ApiResponse:
        """
        Get monthly chart data (ka10083).

        Args:
            ticker: Stock code
            start_date: Start date (YYYYMMDD) - not used by API, kept for interface
            end_date: End date (YYYYMMDD) - used as base_dt
            adj_price: Adjusted price flag (0: No, 1: Yes) - maps to upd_stkpc_tp

        Returns:
            ApiResponse with OHLCV data
        """
        return self._call(
            "ka10083",
            "/api/dostk/chart",
            {
                "stk_cd": ticker,
                "base_dt": end_date,
                "upd_stkpc_tp": adj_price,
            },
        )

    # ========== ETF ==========

    def get_etf_list(self) -> ApiResponse:
        """
        Get ETF full quote (ka40004).

        Returns:
            ApiResponse with ETF list
        """
        return self._call("ka40004", "/api/dostk/etf", {})

    def get_etf_daily(self, ticker: str) -> ApiResponse:
        """
        Get ETF daily trend (ka40003).

        Args:
            ticker: ETF code

        Returns:
            ApiResponse with ETF daily data
        """
        return self._call(
            "ka40003",
            "/api/dostk/etf",
            {"stk_cd": ticker},
        )

    # ========== Condition Search ==========

    def get_condition_list(self) -> ApiResponse:
        """
        Get condition search list (ka10171).

        Returns:
            ApiResponse with condition list
        """
        return self._call("ka10171", "/api/dostk/cond", {})

    def search_condition(self, cond_idx: str, cond_name: str) -> ApiResponse:
        """
        Execute condition search (ka10172).

        Args:
            cond_idx: Condition index
            cond_name: Condition name

        Returns:
            ApiResponse with matching stocks
        """
        return self._call(
            "ka10172",
            "/api/dostk/cond",
            {"cond_idx": cond_idx, "cond_nm": cond_name},
        )

    # ========== Market Indicators ==========

    def get_deposit_trend(self, days: int = 30) -> ApiResponse:
        """
        Get daily estimated deposit asset trend (kt00002).

        Args:
            days: Number of days to retrieve

        Returns:
            ApiResponse with deposit trend data
        """
        from ..core.date import days_ago, today_str

        end_dt = today_str()
        start_dt = days_ago(days)
        return self._call(
            "kt00002",
            "/api/dostk/acnt",
            {"start_dt": start_dt, "end_dt": end_dt},
        )

    def get_credit_trend(self, days: int = 30) -> ApiResponse:
        """
        Get credit trend from daily estimated deposit asset (kt00002).

        Note: ka10013 requires stock code, so we use kt00002 which includes
        crd_loan (신용융자금) data at the account level.

        Args:
            days: Number of days to retrieve

        Returns:
            ApiResponse with credit trend data
        """
        from ..core.date import days_ago, today_str

        end_dt = today_str()
        start_dt = days_ago(days)
        return self._call(
            "kt00002",
            "/api/dostk/acnt",
            {"start_dt": start_dt, "end_dt": end_dt},
        )
