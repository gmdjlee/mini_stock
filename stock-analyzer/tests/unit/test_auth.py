"""Tests for auth module."""

from datetime import datetime, timedelta
from unittest.mock import Mock, patch

import pytest

from stock_analyzer.client.auth import AuthClient, AuthError, TokenInfo


class TestTokenInfo:
    """Tests for TokenInfo dataclass."""

    def test_is_expired_false(self):
        """Test token is not expired."""
        token = TokenInfo(
            token="test_token",
            expires_dt=datetime.now() + timedelta(hours=1),
        )
        assert token.is_expired is False

    def test_is_expired_true(self):
        """Test token is expired."""
        token = TokenInfo(
            token="test_token",
            expires_dt=datetime.now() - timedelta(hours=1),
        )
        assert token.is_expired is True

    def test_bearer_format(self):
        """Test bearer token format."""
        token = TokenInfo(
            token="test_token_12345",
            expires_dt=datetime.now() + timedelta(hours=1),
        )
        assert token.bearer == "Bearer test_token_12345"


class TestAuthClient:
    """Tests for AuthClient."""

    def test_init(self):
        """Test client initialization."""
        client = AuthClient(
            app_key="test_app_key",
            secret_key="test_secret_key",
            base_url="https://api.kiwoom.com",
        )
        assert client.app_key == "test_app_key"
        assert client.secret_key == "test_secret_key"
        assert client.base_url == "https://api.kiwoom.com"
        assert client._token is None

    @patch("stock_analyzer.client.auth.requests.post")
    def test_fetch_token_success(self, mock_post, mock_token_response):
        """Test successful token fetch."""
        mock_response = Mock()
        mock_response.json.return_value = mock_token_response
        mock_response.raise_for_status = Mock()
        mock_post.return_value = mock_response

        client = AuthClient(
            app_key="test_app_key",
            secret_key="test_secret_key",
            base_url="https://api.kiwoom.com",
        )

        token = client.get_token()

        assert token.token == "test_token_12345"
        assert token.token_type == "bearer"
        mock_post.assert_called_once()

    @patch("stock_analyzer.client.auth.requests.post")
    def test_fetch_token_error(self, mock_post):
        """Test token fetch error."""
        mock_response = Mock()
        mock_response.json.return_value = {
            "return_code": -1,
            "return_msg": "Invalid credentials",
        }
        mock_response.raise_for_status = Mock()
        mock_post.return_value = mock_response

        client = AuthClient(
            app_key="test_app_key",
            secret_key="test_secret_key",
            base_url="https://api.kiwoom.com",
        )

        with pytest.raises(AuthError):
            client.get_token()

    @patch("stock_analyzer.client.auth.requests.post")
    def test_get_token_cached(self, mock_post, mock_token_response):
        """Test token caching."""
        mock_response = Mock()
        mock_response.json.return_value = mock_token_response
        mock_response.raise_for_status = Mock()
        mock_post.return_value = mock_response

        client = AuthClient(
            app_key="test_app_key",
            secret_key="test_secret_key",
            base_url="https://api.kiwoom.com",
        )

        # First call fetches token
        token1 = client.get_token()
        # Second call uses cached token
        token2 = client.get_token()

        assert token1.token == token2.token
        assert mock_post.call_count == 1  # Only one API call

    @patch("stock_analyzer.client.auth.requests.post")
    def test_get_token_force_refresh(self, mock_post, mock_token_response):
        """Test force token refresh."""
        mock_response = Mock()
        mock_response.json.return_value = mock_token_response
        mock_response.raise_for_status = Mock()
        mock_post.return_value = mock_response

        client = AuthClient(
            app_key="test_app_key",
            secret_key="test_secret_key",
            base_url="https://api.kiwoom.com",
        )

        # First call
        client.get_token()
        # Force refresh
        client.get_token(force_refresh=True)

        assert mock_post.call_count == 2  # Two API calls

    def test_clear_token(self):
        """Test clear token."""
        client = AuthClient(
            app_key="test_app_key",
            secret_key="test_secret_key",
            base_url="https://api.kiwoom.com",
        )
        client._token = TokenInfo(
            token="test_token",
            expires_dt=datetime.now() + timedelta(hours=1),
        )

        client.clear_token()

        assert client._token is None
