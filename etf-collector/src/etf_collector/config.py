"""Configuration management for ETF Collector.

Note: This module uses KIS API (Korea Investment & Securities),
NOT Kiwoom API. The two are different API providers.

KIS API Portal: https://apiportal.koreainvestment.com
"""

import os
from dataclasses import dataclass
from typing import Optional

from dotenv import load_dotenv


# API URLs
KIS_REAL_URL = "https://openapi.koreainvestment.com:9443"
KIS_VIRTUAL_URL = "https://openapivts.koreainvestment.com:29443"

# Rate limits (requests per second)
RATE_LIMIT_REAL = 15
RATE_LIMIT_VIRTUAL = 4

# API endpoints
ENDPOINTS = {
    "token": "/oauth2/tokenP",
    "etf_component": "/uapi/etfetn/v1/quotations/inquire-component-stock-price",
    "stock_search": "/uapi/domestic-stock/v1/quotations/search-info",
}

# Timeouts (seconds)
DEFAULT_TIMEOUT = 30

# Error codes
ERROR_CODES = {
    "EGW00201": "API rate limit exceeded",
    "EGW00123": "Token expired",
    "OPSW0009": "System error",
    "MCA00000": "Invalid request",
}


class ConfigError(Exception):
    """Configuration error."""

    pass


@dataclass
class Config:
    """Application configuration."""

    app_key: str
    app_secret: str
    account_no: str
    environment: str  # "real" or "virtual"

    @property
    def base_url(self) -> str:
        """Get base URL based on environment."""
        if self.environment == "virtual":
            return KIS_VIRTUAL_URL
        return KIS_REAL_URL

    @property
    def rate_limit(self) -> int:
        """Get rate limit based on environment."""
        if self.environment == "virtual":
            return RATE_LIMIT_VIRTUAL
        return RATE_LIMIT_REAL

    @classmethod
    def from_env(cls, dotenv_path: Optional[str] = None) -> "Config":
        """Load configuration from environment variables.

        Args:
            dotenv_path: Optional path to .env file

        Returns:
            Config instance

        Raises:
            ConfigError: If required environment variables are missing
        """
        if dotenv_path:
            load_dotenv(dotenv_path)
        else:
            load_dotenv()

        app_key = os.getenv("KIS_APP_KEY")
        app_secret = os.getenv("KIS_APP_SECRET")
        account_no = os.getenv("KIS_ACCOUNT_NO", "")
        environment = os.getenv("KIS_ENVIRONMENT", "real")

        if not app_key:
            raise ConfigError("KIS_APP_KEY is required")
        if not app_secret:
            raise ConfigError("KIS_APP_SECRET is required")

        if environment not in ("real", "virtual"):
            raise ConfigError("KIS_ENVIRONMENT must be 'real' or 'virtual'")

        return cls(
            app_key=app_key,
            app_secret=app_secret,
            account_no=account_no,
            environment=environment,
        )

    def validate(self) -> bool:
        """Validate configuration.

        Returns:
            True if configuration is valid

        Raises:
            ConfigError: If configuration is invalid
        """
        if not self.app_key:
            raise ConfigError("app_key is required")
        if not self.app_secret:
            raise ConfigError("app_secret is required")
        if self.environment not in ("real", "virtual"):
            raise ConfigError("environment must be 'real' or 'virtual'")
        return True
