"""Tests for configuration module."""

import os
import tempfile
from pathlib import Path

import pytest

from etf_collector.config import (
    Config,
    ConfigError,
    KIS_REAL_URL,
    KIS_VIRTUAL_URL,
    RATE_LIMIT_REAL,
    RATE_LIMIT_VIRTUAL,
)


class TestConstants:
    """Tests for module constants."""

    def test_real_url(self):
        """Test real environment URL."""
        assert "openapi.koreainvestment.com" in KIS_REAL_URL
        assert "9443" in KIS_REAL_URL

    def test_virtual_url(self):
        """Test virtual environment URL."""
        assert "openapivts.koreainvestment.com" in KIS_VIRTUAL_URL
        assert "29443" in KIS_VIRTUAL_URL

    def test_rate_limits(self):
        """Test rate limit constants."""
        assert RATE_LIMIT_REAL == 15
        assert RATE_LIMIT_VIRTUAL == 4


class TestConfig:
    """Tests for Config dataclass."""

    def test_create_config(self):
        """Test creating Config instance."""
        config = Config(
            app_key="test_key",
            app_secret="test_secret",
            account_no="12345678",
            environment="real",
        )

        assert config.app_key == "test_key"
        assert config.app_secret == "test_secret"
        assert config.account_no == "12345678"
        assert config.environment == "real"

    def test_base_url_real(self):
        """Test base_url for real environment."""
        config = Config(
            app_key="key",
            app_secret="secret",
            account_no="",
            environment="real",
        )

        assert config.base_url == KIS_REAL_URL

    def test_base_url_virtual(self):
        """Test base_url for virtual environment."""
        config = Config(
            app_key="key",
            app_secret="secret",
            account_no="",
            environment="virtual",
        )

        assert config.base_url == KIS_VIRTUAL_URL

    def test_rate_limit_real(self):
        """Test rate_limit for real environment."""
        config = Config(
            app_key="key",
            app_secret="secret",
            account_no="",
            environment="real",
        )

        assert config.rate_limit == RATE_LIMIT_REAL

    def test_rate_limit_virtual(self):
        """Test rate_limit for virtual environment."""
        config = Config(
            app_key="key",
            app_secret="secret",
            account_no="",
            environment="virtual",
        )

        assert config.rate_limit == RATE_LIMIT_VIRTUAL

    def test_validate_success(self):
        """Test successful validation."""
        config = Config(
            app_key="key",
            app_secret="secret",
            account_no="",
            environment="real",
        )

        assert config.validate() is True

    def test_validate_missing_app_key(self):
        """Test validation fails without app_key."""
        config = Config(
            app_key="",
            app_secret="secret",
            account_no="",
            environment="real",
        )

        with pytest.raises(ConfigError) as exc_info:
            config.validate()

        assert "app_key" in str(exc_info.value)

    def test_validate_missing_app_secret(self):
        """Test validation fails without app_secret."""
        config = Config(
            app_key="key",
            app_secret="",
            account_no="",
            environment="real",
        )

        with pytest.raises(ConfigError) as exc_info:
            config.validate()

        assert "app_secret" in str(exc_info.value)

    def test_validate_invalid_environment(self):
        """Test validation fails with invalid environment."""
        config = Config(
            app_key="key",
            app_secret="secret",
            account_no="",
            environment="invalid",
        )

        with pytest.raises(ConfigError) as exc_info:
            config.validate()

        assert "environment" in str(exc_info.value)


class TestConfigFromEnv:
    """Tests for Config.from_env()."""

    def setup_method(self):
        """Clear environment variables before each test."""
        for key in ["KIS_APP_KEY", "KIS_APP_SECRET", "KIS_ACCOUNT_NO", "KIS_ENVIRONMENT"]:
            if key in os.environ:
                del os.environ[key]

    def teardown_method(self):
        """Clean up environment variables after each test."""
        for key in ["KIS_APP_KEY", "KIS_APP_SECRET", "KIS_ACCOUNT_NO", "KIS_ENVIRONMENT"]:
            if key in os.environ:
                del os.environ[key]

    def test_from_env_success(self):
        """Test loading from environment variables."""
        os.environ["KIS_APP_KEY"] = "test_key"
        os.environ["KIS_APP_SECRET"] = "test_secret"
        os.environ["KIS_ACCOUNT_NO"] = "12345678"
        os.environ["KIS_ENVIRONMENT"] = "real"

        config = Config.from_env()

        assert config.app_key == "test_key"
        assert config.app_secret == "test_secret"
        assert config.account_no == "12345678"
        assert config.environment == "real"

    def test_from_env_missing_app_key(self, monkeypatch):
        """Test error when app_key is missing."""
        # Prevent load_dotenv from loading .env file
        monkeypatch.setattr("etf_collector.config.load_dotenv", lambda *args, **kwargs: None)
        os.environ["KIS_APP_SECRET"] = "test_secret"

        with pytest.raises(ConfigError) as exc_info:
            Config.from_env()

        assert "KIS_APP_KEY" in str(exc_info.value)

    def test_from_env_missing_app_secret(self, monkeypatch):
        """Test error when app_secret is missing."""
        # Prevent load_dotenv from loading .env file
        monkeypatch.setattr("etf_collector.config.load_dotenv", lambda *args, **kwargs: None)
        os.environ["KIS_APP_KEY"] = "test_key"

        with pytest.raises(ConfigError) as exc_info:
            Config.from_env()

        assert "KIS_APP_SECRET" in str(exc_info.value)

    def test_from_env_default_environment(self):
        """Test default environment is 'real'."""
        os.environ["KIS_APP_KEY"] = "test_key"
        os.environ["KIS_APP_SECRET"] = "test_secret"

        config = Config.from_env()

        assert config.environment == "real"

    def test_from_env_invalid_environment(self):
        """Test error with invalid environment value."""
        os.environ["KIS_APP_KEY"] = "test_key"
        os.environ["KIS_APP_SECRET"] = "test_secret"
        os.environ["KIS_ENVIRONMENT"] = "invalid"

        with pytest.raises(ConfigError) as exc_info:
            Config.from_env()

        assert "real" in str(exc_info.value) or "virtual" in str(exc_info.value)

    def test_from_env_with_dotenv_file(self):
        """Test loading from .env file."""
        # Create temp .env file
        with tempfile.NamedTemporaryFile(mode="w", suffix=".env", delete=False) as f:
            f.write("KIS_APP_KEY=file_key\n")
            f.write("KIS_APP_SECRET=file_secret\n")
            f.write("KIS_ENVIRONMENT=virtual\n")
            env_path = f.name

        try:
            config = Config.from_env(env_path)

            assert config.app_key == "file_key"
            assert config.app_secret == "file_secret"
            assert config.environment == "virtual"
        finally:
            os.unlink(env_path)
