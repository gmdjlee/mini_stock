"""Configuration management."""

import os
from dataclasses import dataclass
from typing import Optional

from dotenv import load_dotenv


@dataclass
class Config:
    """Application configuration."""

    app_key: str
    secret_key: str
    base_url: str

    @classmethod
    def from_env(cls, env_path: Optional[str] = None) -> "Config":
        """Load configuration from environment variables."""
        if env_path:
            load_dotenv(env_path)
        else:
            load_dotenv()

        app_key = os.getenv("KIWOOM_APP_KEY", "")
        secret_key = os.getenv("KIWOOM_SECRET_KEY", "")
        base_url = os.getenv("KIWOOM_BASE_URL", "https://api.kiwoom.com")

        if not app_key or not secret_key:
            raise ConfigError("KIWOOM_APP_KEY and KIWOOM_SECRET_KEY are required")

        return cls(
            app_key=app_key,
            secret_key=secret_key,
            base_url=base_url,
        )


class ConfigError(Exception):
    """Configuration error."""

    pass


# Default URLs
PROD_URL = "https://api.kiwoom.com"
MOCK_URL = "https://mockapi.kiwoom.com"

# API endpoints
ENDPOINTS = {
    "token": "/oauth2/token",
    "stock_info": "/api/dostk/stkinfo",
    "foreign_trend": "/api/dostk/frgnistt",
    "chart": "/api/dostk/chart",
    "etf": "/api/dostk/etf",
    "condition": "/api/dostk/websocket",
}

# Timeouts (seconds)
DEFAULT_TIMEOUT = 30
ANALYSIS_TIMEOUT = 60

# Limits
MAX_SEARCH_RESULTS = 50
MAX_HISTORY_DAYS = 365
