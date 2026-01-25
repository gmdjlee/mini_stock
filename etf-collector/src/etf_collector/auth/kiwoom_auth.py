"""Kiwoom API OAuth authentication module."""

from dataclasses import dataclass
from datetime import datetime, timedelta
from typing import Optional

import requests

from ..config import DEFAULT_TIMEOUT
from ..utils.logger import log_info, log_err, log_debug
from ..utils.validators import mask_credentials

MODULE = "kiwoom_auth"

# Token expiration buffer (refresh 1 minute before actual expiry)
TOKEN_EXPIRY_BUFFER_SECONDS = 60


class KiwoomAuthError(Exception):
    """Kiwoom authentication error."""


@dataclass
class KiwoomTokenInfo:
    """Kiwoom OAuth token information."""

    token: str
    expires_dt: datetime
    token_type: str = "bearer"

    @property
    def is_expired(self) -> bool:
        """Check if token is expired (with buffer for safety).

        Returns:
            True if token is expired or about to expire
        """
        buffer = timedelta(seconds=TOKEN_EXPIRY_BUFFER_SECONDS)
        return datetime.now() >= (self.expires_dt - buffer)

    @property
    def bearer(self) -> str:
        """Get bearer token string.

        Returns:
            Bearer token string (e.g., "Bearer abc123")
        """
        return f"Bearer {self.token}"

    @property
    def authorization(self) -> str:
        """Get authorization header value (alias for bearer).

        Returns:
            Authorization header string
        """
        return self.bearer


class KiwoomAuthClient:
    """Kiwoom API OAuth authentication client."""

    API_ID = "au10001"
    TOKEN_PATH = "/oauth2/token"

    def __init__(self, app_key: str, secret_key: str, base_url: str):
        """Initialize Kiwoom authentication client.

        Args:
            app_key: Kiwoom API app key
            secret_key: Kiwoom API secret key
            base_url: Kiwoom API base URL
        """
        self.app_key = app_key
        self.secret_key = secret_key
        self.base_url = base_url
        self._token: Optional[KiwoomTokenInfo] = None

    def get_token(self, force_refresh: bool = False) -> KiwoomTokenInfo:
        """Get valid token (auto-refresh if expired).

        Args:
            force_refresh: Force token refresh even if not expired

        Returns:
            Valid KiwoomTokenInfo instance

        Raises:
            KiwoomAuthError: If token fetch fails
        """
        if force_refresh or self._token is None or self._token.is_expired:
            log_info(MODULE, "Fetching new Kiwoom token", {"force": force_refresh})
            self._token = self._fetch_token()
        else:
            log_debug(MODULE, "Using cached Kiwoom token")
        return self._token

    def _fetch_token(self) -> KiwoomTokenInfo:
        """Fetch new token from Kiwoom API (au10001).

        Returns:
            KiwoomTokenInfo instance with new token

        Raises:
            KiwoomAuthError: If token fetch fails
        """
        url = f"{self.base_url}{self.TOKEN_PATH}"
        headers = {
            "api-id": self.API_ID,
            "Content-Type": "application/json;charset=UTF-8",
        }
        body = {
            "grant_type": "client_credentials",
            "appkey": self.app_key,
            "secretkey": self.secret_key,
        }

        try:
            # Log with masked credentials for security
            log_debug(MODULE, "Requesting Kiwoom token", {"url": url, "body": mask_credentials(body)})
            resp = requests.post(url, json=body, headers=headers, timeout=DEFAULT_TIMEOUT)
            resp.raise_for_status()
            data = resp.json()

            # Check Kiwoom API return code
            if data.get("return_code") != 0:
                error_msg = data.get("return_msg", "Token fetch failed")
                raise KiwoomAuthError(f"Token fetch failed: {error_msg}")

            # Parse token and expiration
            token = data.get("token")
            if not token:
                raise KiwoomAuthError("No token in response")

            # Parse expiration datetime (format: YYYYMMDDHHMMSS)
            expires_dt_str = data.get("expires_dt")
            if expires_dt_str:
                expires_dt = datetime.strptime(expires_dt_str, "%Y%m%d%H%M%S")
            else:
                # Default to 24 hours if not provided
                expires_dt = datetime.now() + timedelta(hours=24)

            token_type = data.get("token_type", "bearer")

            log_info(
                MODULE,
                "Kiwoom token fetched successfully",
                {"expires_dt": expires_dt.isoformat()},
            )

            return KiwoomTokenInfo(
                token=token,
                expires_dt=expires_dt,
                token_type=token_type,
            )

        except requests.exceptions.Timeout:
            log_err(MODULE, "Kiwoom token request timed out", {"url": url})
            raise KiwoomAuthError("Token request timed out")
        except requests.exceptions.ConnectionError as e:
            log_err(MODULE, "Connection error", {"url": url, "error": str(e)})
            raise KiwoomAuthError(f"Connection error: {e}")
        except requests.exceptions.HTTPError as e:
            log_err(MODULE, "HTTP error", {"status": resp.status_code, "error": str(e)})
            raise KiwoomAuthError(f"HTTP error: {resp.status_code} - {e}")
        except Exception as e:
            if isinstance(e, KiwoomAuthError):
                raise
            log_err(MODULE, "Unexpected error", {"error": str(e)})
            raise KiwoomAuthError(f"Unexpected error: {e}")

    def clear_token(self) -> None:
        """Clear cached token."""
        self._token = None
        log_info(MODULE, "Kiwoom token cache cleared")
