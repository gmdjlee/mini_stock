"""KIS API OAuth authentication module."""

from dataclasses import dataclass, field
from datetime import datetime, timedelta
from typing import Optional

import requests

from ..config import ENDPOINTS, DEFAULT_TIMEOUT
from ..utils.logger import log_info, log_err, log_debug

MODULE = "kis_auth"


class AuthError(Exception):
    """Authentication error."""


@dataclass
class TokenInfo:
    """OAuth token information."""

    access_token: str
    token_type: str = "Bearer"
    expires_at: datetime = field(default_factory=lambda: datetime.now() + timedelta(hours=24))

    @property
    def is_expired(self) -> bool:
        """Check if token is expired (with 60-second buffer).

        Returns:
            True if token is expired or about to expire
        """
        return datetime.now() >= self.expires_at - timedelta(seconds=60)

    @property
    def authorization(self) -> str:
        """Get authorization header value.

        Returns:
            Authorization header string (e.g., "Bearer abc123")
        """
        return f"{self.token_type} {self.access_token}"


class KisAuthClient:
    """KIS API OAuth authentication client."""

    def __init__(self, app_key: str, app_secret: str, base_url: str):
        """Initialize authentication client.

        Args:
            app_key: KIS API app key
            app_secret: KIS API secret
            base_url: KIS API base URL
        """
        self.app_key = app_key
        self.app_secret = app_secret
        self.base_url = base_url
        self._token: Optional[TokenInfo] = None

    def get_token(self, force_refresh: bool = False) -> TokenInfo:
        """Get valid token (auto-refresh if expired).

        Args:
            force_refresh: Force token refresh even if not expired

        Returns:
            Valid TokenInfo instance

        Raises:
            AuthError: If token fetch fails
        """
        if force_refresh or self._token is None or self._token.is_expired:
            log_info(MODULE, "Fetching new token", {"force": force_refresh})
            self._token = self._fetch_token()
        else:
            log_debug(MODULE, "Using cached token")
        return self._token

    def _fetch_token(self) -> TokenInfo:
        """Fetch new token from KIS API.

        Returns:
            TokenInfo instance with new token

        Raises:
            AuthError: If token fetch fails
        """
        url = f"{self.base_url}{ENDPOINTS['token']}"
        headers = {"Content-Type": "application/json"}
        body = {
            "grant_type": "client_credentials",
            "appkey": self.app_key,
            "appsecret": self.app_secret,
        }

        try:
            log_debug(MODULE, "Requesting token", {"url": url})
            resp = requests.post(url, json=body, headers=headers, timeout=DEFAULT_TIMEOUT)
            resp.raise_for_status()
            data = resp.json()

            access_token = data.get("access_token")
            if not access_token:
                error_msg = data.get("error_description", "No access_token in response")
                raise AuthError(f"Token fetch failed: {error_msg}")

            # Calculate expiration time
            expires_in = int(data.get("expires_in", 86400))  # Default 24 hours
            expires_at = datetime.now() + timedelta(seconds=expires_in)

            token_type = data.get("token_type", "Bearer")

            log_info(
                MODULE,
                "Token fetched successfully",
                {"expires_in": expires_in, "expires_at": expires_at.isoformat()},
            )

            return TokenInfo(
                access_token=access_token,
                token_type=token_type,
                expires_at=expires_at,
            )

        except requests.exceptions.Timeout:
            log_err(MODULE, "Token request timed out", {"url": url})
            raise AuthError("Token request timed out")
        except requests.exceptions.ConnectionError as e:
            log_err(MODULE, "Connection error", {"url": url, "error": str(e)})
            raise AuthError(f"Connection error: {e}")
        except requests.exceptions.HTTPError as e:
            log_err(MODULE, "HTTP error", {"status": resp.status_code, "error": str(e)})
            raise AuthError(f"HTTP error: {resp.status_code} - {e}")
        except Exception as e:
            log_err(MODULE, "Unexpected error", {"error": str(e)})
            raise AuthError(f"Unexpected error: {e}")

    def clear_token(self) -> None:
        """Clear cached token."""
        self._token = None
        log_info(MODULE, "Token cache cleared")
