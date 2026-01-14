"""Kiwoom REST API client."""

from dataclasses import dataclass
from typing import Any, Dict, Optional

import requests

from ..core.log import log_err, log_info
from .auth import AuthClient


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
    ):
        """
        Initialize Kiwoom client.

        Args:
            app_key: Kiwoom API app key
            secret_key: Kiwoom API secret key
            base_url: API base URL
        """
        self.base_url = base_url
        self.auth = AuthClient(app_key, secret_key, base_url)

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
        Call API endpoint.

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

        try:
            resp = requests.post(
                full_url,
                headers=headers,
                json=body,
                timeout=timeout,
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

    # ========== Stock Search ==========

    def get_stock_list(self, market: str = "0") -> ApiResponse:
        """
        Get stock list (ka10099).

        Args:
            market: Market type (0: All, 1: KOSPI, 2: KOSDAQ)

        Returns:
            ApiResponse with stock list
        """
        return self._call(
            "ka10099",
            "/api/dostk/stkinfo",
            {"mrkt_tp": market},
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

    def get_investor_trend(self, ticker: str, period: str = "1") -> ApiResponse:
        """
        Get investor/institution trend by stock (ka10059).

        Args:
            ticker: Stock code
            period: Period (1: Daily, 2: Weekly, 3: Monthly)

        Returns:
            ApiResponse with investor trading trend
        """
        return self._call(
            "ka10059",
            "/api/dostk/stkinfo",
            {"stk_cd": ticker, "inq_cnd": period},
        )

    def get_investor_summary(self, ticker: str, period: str = "1") -> ApiResponse:
        """
        Get investor/institution summary by stock (ka10061).

        Args:
            ticker: Stock code
            period: Period (1: Daily, 2: Weekly, 3: Monthly)

        Returns:
            ApiResponse with investor summary
        """
        return self._call(
            "ka10061",
            "/api/dostk/stkinfo",
            {"stk_cd": ticker, "inq_cnd": period},
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
            adj_price: Adjusted price flag (0: No, 1: Yes)

        Returns:
            ApiResponse with OHLCV data
        """
        return self._call(
            "ka10081",
            "/api/dostk/chart",
            {
                "stk_cd": ticker,
                "base_dt": end_date,
                "adj_prc_tp": adj_price,
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
            adj_price: Adjusted price flag (0: No, 1: Yes)

        Returns:
            ApiResponse with OHLCV data
        """
        return self._call(
            "ka10082",
            "/api/dostk/chart",
            {
                "stk_cd": ticker,
                "base_dt": end_date,
                "adj_prc_tp": adj_price,
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
            adj_price: Adjusted price flag (0: No, 1: Yes)

        Returns:
            ApiResponse with OHLCV data
        """
        return self._call(
            "ka10083",
            "/api/dostk/chart",
            {
                "stk_cd": ticker,
                "base_dt": end_date,
                "adj_prc_tp": adj_price,
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
        Get customer deposit trend (kt00001).

        Args:
            days: Number of days to retrieve

        Returns:
            ApiResponse with deposit trend data
        """
        return self._call(
            "kt00001",
            "/api/dostk/mrktdata",
            {"inq_cnt": str(days)},
        )

    def get_credit_trend(self, days: int = 30) -> ApiResponse:
        """
        Get credit trading trend (ka10013).

        Args:
            days: Number of days to retrieve

        Returns:
            ApiResponse with credit balance trend data
        """
        return self._call(
            "ka10013",
            "/api/dostk/stkinfo",
            {"inq_cnt": str(days)},
        )
