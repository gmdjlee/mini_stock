"""Kiwoom OAuth token management."""

from dataclasses import dataclass
from datetime import datetime
from typing import Optional

import requests

from ..core.log import log_err, log_info


@dataclass
class TokenInfo:
    """OAuth token information."""

    token: str
    expires_dt: datetime
    token_type: str = "bearer"

    @property
    def is_expired(self) -> bool:
        """Check if token is expired."""
        return datetime.now() >= self.expires_dt

    @property
    def bearer(self) -> str:
        """Get bearer token string."""
        return f"Bearer {self.token}"


class AuthError(Exception):
    """Authentication error."""

    pass


class AuthClient:
    """OAuth token issuer and manager."""

    API_ID = "au10001"
    TOKEN_PATH = "/oauth2/token"

    def __init__(self, app_key: str, secret_key: str, base_url: str):
        """
        Initialize auth client.

        Args:
            app_key: Kiwoom API app key
            secret_key: Kiwoom API secret key
            base_url: API base URL
        """
        self.app_key = app_key
        self.secret_key = secret_key
        self.base_url = base_url
        self._token: Optional[TokenInfo] = None

    def get_token(self, force_refresh: bool = False) -> TokenInfo:
        """
        Get token (auto-refresh if expired).

        Args:
            force_refresh: Force token refresh

        Returns:
            TokenInfo object

        Raises:
            AuthError: If token fetch fails
        """
        if force_refresh or self._token is None or self._token.is_expired:
            self._token = self._fetch_token()
        return self._token

    def _fetch_token(self) -> TokenInfo:
        """
        Fetch new token from API (au10001).

        Returns:
            TokenInfo object

        Raises:
            AuthError: If token fetch fails
        """
        url = f"{self.base_url}{self.TOKEN_PATH}"

        try:
            resp = requests.post(
                url,
                headers={
                    "api-id": self.API_ID,
                    "Content-Type": "application/json;charset=UTF-8",
                },
                json={
                    "grant_type": "client_credentials",
                    "appkey": self.app_key,
                    "secretkey": self.secret_key,
                },
                timeout=30,
            )
            resp.raise_for_status()
            data = resp.json()

            if data.get("return_code") != 0:
                raise AuthError(data.get("return_msg", "Token fetch failed"))

            expires_dt = datetime.strptime(data["expires_dt"], "%Y%m%d%H%M%S")

            log_info("client.auth", "Token issued", {"expires": data["expires_dt"]})

            return TokenInfo(
                token=data["token"],
                expires_dt=expires_dt,
                token_type=data.get("token_type", "bearer"),
            )

        except requests.RequestException as e:
            log_err("client.auth", e, {"url": url})
            raise AuthError(f"Network error: {str(e)}") from e

    def revoke_token(self) -> bool:
        """
        Revoke current token (au10002).

        Returns:
            True if successful
        """
        if self._token is None:
            return True

        url = f"{self.base_url}/oauth2/revoke"

        try:
            resp = requests.post(
                url,
                headers={
                    "api-id": "au10002",
                    "Content-Type": "application/json;charset=UTF-8",
                },
                json={
                    "token": self._token.token,
                },
                timeout=30,
            )
            resp.raise_for_status()
            data = resp.json()

            if data.get("return_code") == 0:
                self._token = None
                log_info("client.auth", "Token revoked")
                return True

            return False

        except requests.RequestException as e:
            log_err("client.auth", e, {"action": "revoke"})
            return False

    def clear_token(self) -> None:
        """Clear cached token."""
        self._token = None
