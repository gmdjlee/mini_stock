"""Configuration management for ETF Collector.

This module supports two API providers:
- KIS API (Korea Investment & Securities): For ETF constituent data
- Kiwoom API: For fetching complete ETF list dynamically

KIS API Portal: https://apiportal.koreainvestment.com
Kiwoom API Portal: https://openapi.kiwoom.com
"""

import os
from dataclasses import dataclass, field
from enum import Enum
from typing import Optional

from dotenv import load_dotenv


# ============================================================================
# KIS API Configuration
# ============================================================================

# KIS API URLs
KIS_REAL_URL = "https://openapi.koreainvestment.com:9443"
KIS_VIRTUAL_URL = "https://openapivts.koreainvestment.com:29443"

# KIS Rate limits (requests per second)
KIS_RATE_LIMIT_REAL = 15
KIS_RATE_LIMIT_VIRTUAL = 4

# Legacy aliases for backward compatibility
RATE_LIMIT_REAL = KIS_RATE_LIMIT_REAL
RATE_LIMIT_VIRTUAL = KIS_RATE_LIMIT_VIRTUAL

# KIS API endpoints
ENDPOINTS = {
    "token": "/oauth2/tokenP",
    "etf_component": "/uapi/etfetn/v1/quotations/inquire-component-stock-price",
    "stock_search": "/uapi/domestic-stock/v1/quotations/search-info",
}

# ============================================================================
# Kiwoom API Configuration
# ============================================================================

# Kiwoom API URLs
KIWOOM_REAL_URL = "https://api.kiwoom.com"
KIWOOM_MOCK_URL = "https://mockapi.kiwoom.com"

# Kiwoom Rate limits (requests per second)
KIWOOM_RATE_LIMIT_REAL = 15
KIWOOM_RATE_LIMIT_MOCK = 4

# Kiwoom API endpoints
KIWOOM_ENDPOINTS = {
    "token": "/oauth2/token",  # au10001
    "etf_list": "/api/dostk/etf",  # ka40004 (ETF전체시세요청)
    "etf_info": "/api/dostk/etf",  # ka40002 (ETF종목정보요청)
}

# ============================================================================
# Common Configuration
# ============================================================================

# Timeouts (seconds)
DEFAULT_TIMEOUT = 30

# Error codes (both APIs use similar patterns)
ERROR_CODES = {
    "EGW00201": "API rate limit exceeded",
    "EGW00123": "Token expired",
    "OPSW0009": "System error",
    "MCA00000": "Invalid request",
}


class EtfListSource(Enum):
    """Source for ETF list data."""

    KIWOOM = "kiwoom"  # Use Kiwoom API (ka40004)
    PREDEFINED = "predefined"  # Use predefined active ETF codes


class ConfigError(Exception):
    """Configuration error."""

    pass


@dataclass
class Config:
    """Application configuration for dual-API support (KIS + Kiwoom)."""

    # KIS API settings (required for constituent data)
    app_key: str  # KIS APP KEY
    app_secret: str  # KIS APP SECRET
    account_no: str = ""
    environment: str = "real"  # "real" or "virtual"

    # Kiwoom API settings (optional, for dynamic ETF list)
    kiwoom_app_key: Optional[str] = None
    kiwoom_secret_key: Optional[str] = None
    kiwoom_environment: str = "real"  # "real" or "mock"

    # ETF list source selection
    etf_list_source: EtfListSource = field(default=EtfListSource.KIWOOM)

    @property
    def base_url(self) -> str:
        """Get KIS base URL based on environment."""
        if self.environment == "virtual":
            return KIS_VIRTUAL_URL
        return KIS_REAL_URL

    @property
    def rate_limit(self) -> int:
        """Get KIS rate limit based on environment."""
        if self.environment == "virtual":
            return KIS_RATE_LIMIT_VIRTUAL
        return KIS_RATE_LIMIT_REAL

    @property
    def kiwoom_base_url(self) -> str:
        """Get Kiwoom base URL based on environment."""
        if self.kiwoom_environment == "mock":
            return KIWOOM_MOCK_URL
        return KIWOOM_REAL_URL

    @property
    def kiwoom_rate_limit(self) -> int:
        """Get Kiwoom rate limit based on environment."""
        if self.kiwoom_environment == "mock":
            return KIWOOM_RATE_LIMIT_MOCK
        return KIWOOM_RATE_LIMIT_REAL

    @property
    def has_kiwoom_credentials(self) -> bool:
        """Check if Kiwoom credentials are configured."""
        return bool(self.kiwoom_app_key and self.kiwoom_secret_key)

    @property
    def use_kiwoom_for_etf_list(self) -> bool:
        """Whether to use Kiwoom API for ETF list."""
        return (
            self.etf_list_source == EtfListSource.KIWOOM
            and self.has_kiwoom_credentials
        )

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

        # KIS API settings (required)
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

        # Kiwoom API settings (optional)
        kiwoom_app_key = os.getenv("KIWOOM_APP_KEY")
        kiwoom_secret_key = os.getenv("KIWOOM_SECRET_KEY")
        kiwoom_environment = os.getenv("KIWOOM_ENVIRONMENT", "real")

        if kiwoom_environment not in ("real", "mock"):
            kiwoom_environment = "real"

        # ETF list source selection
        etf_source_str = os.getenv("ETF_LIST_SOURCE", "kiwoom").lower()
        if etf_source_str == "predefined":
            etf_list_source = EtfListSource.PREDEFINED
        else:
            etf_list_source = EtfListSource.KIWOOM

        return cls(
            app_key=app_key,
            app_secret=app_secret,
            account_no=account_no,
            environment=environment,
            kiwoom_app_key=kiwoom_app_key,
            kiwoom_secret_key=kiwoom_secret_key,
            kiwoom_environment=kiwoom_environment,
            etf_list_source=etf_list_source,
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

    def validate_kiwoom(self) -> bool:
        """Validate Kiwoom configuration.

        Returns:
            True if Kiwoom configuration is valid

        Raises:
            ConfigError: If Kiwoom configuration is invalid
        """
        if not self.kiwoom_app_key:
            raise ConfigError("KIWOOM_APP_KEY is required for Kiwoom API")
        if not self.kiwoom_secret_key:
            raise ConfigError("KIWOOM_SECRET_KEY is required for Kiwoom API")
        if self.kiwoom_environment not in ("real", "mock"):
            raise ConfigError("KIWOOM_ENVIRONMENT must be 'real' or 'mock'")
        return True
