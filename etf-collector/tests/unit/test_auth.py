"""Tests for KIS authentication module."""

from datetime import datetime, timedelta
from unittest.mock import Mock, patch

import pytest

from etf_collector.auth.kis_auth import KisAuthClient, TokenInfo, AuthError


class TestTokenInfo:
    """Tests for TokenInfo dataclass."""

    def test_is_expired_false(self):
        """Test token is not expired."""
        token = TokenInfo(
            access_token="test",
            expires_at=datetime.now() + timedelta(hours=1),
        )
        assert token.is_expired is False

    def test_is_expired_true_past(self):
        """Test token is expired when in past."""
        token = TokenInfo(
            access_token="test",
            expires_at=datetime.now() - timedelta(hours=1),
        )
        assert token.is_expired is True

    def test_is_expired_within_buffer(self):
        """Test token is expired within 60-second buffer."""
        token = TokenInfo(
            access_token="test",
            expires_at=datetime.now() + timedelta(seconds=30),
        )
        assert token.is_expired is True

    def test_is_expired_outside_buffer(self):
        """Test token is not expired outside 60-second buffer."""
        token = TokenInfo(
            access_token="test",
            expires_at=datetime.now() + timedelta(seconds=120),
        )
        assert token.is_expired is False

    def test_authorization_header(self):
        """Test authorization header format."""
        token = TokenInfo(
            access_token="abc123",
            token_type="Bearer",
            expires_at=datetime.now() + timedelta(hours=1),
        )
        assert token.authorization == "Bearer abc123"

    def test_custom_token_type(self):
        """Test custom token type."""
        token = TokenInfo(
            access_token="abc123",
            token_type="Basic",
            expires_at=datetime.now() + timedelta(hours=1),
        )
        assert token.authorization == "Basic abc123"


class TestKisAuthClient:
    """Tests for KisAuthClient."""

    @patch("etf_collector.auth.kis_auth.requests.post")
    def test_fetch_token_success(self, mock_post, mock_kis_token_response):
        """Test successful token fetch."""
        mock_resp = Mock()
        mock_resp.json.return_value = mock_kis_token_response
        mock_resp.raise_for_status = Mock()
        mock_post.return_value = mock_resp

        client = KisAuthClient("app_key", "app_secret", "https://api.test.com")
        token = client.get_token()

        assert token.access_token == "test_access_token_12345"
        assert token.token_type == "Bearer"
        assert token.is_expired is False

    @patch("etf_collector.auth.kis_auth.requests.post")
    def test_fetch_token_cached(self, mock_post, mock_kis_token_response):
        """Test token is cached and not re-fetched."""
        mock_resp = Mock()
        mock_resp.json.return_value = mock_kis_token_response
        mock_resp.raise_for_status = Mock()
        mock_post.return_value = mock_resp

        client = KisAuthClient("app_key", "app_secret", "https://api.test.com")

        # First call
        token1 = client.get_token()
        # Second call should use cache
        token2 = client.get_token()

        assert token1.access_token == token2.access_token
        assert mock_post.call_count == 1

    @patch("etf_collector.auth.kis_auth.requests.post")
    def test_force_refresh(self, mock_post, mock_kis_token_response):
        """Test force refresh fetches new token."""
        mock_resp = Mock()
        mock_resp.json.return_value = mock_kis_token_response
        mock_resp.raise_for_status = Mock()
        mock_post.return_value = mock_resp

        client = KisAuthClient("app_key", "app_secret", "https://api.test.com")

        # First call
        client.get_token()
        # Force refresh
        client.get_token(force_refresh=True)

        assert mock_post.call_count == 2

    @patch("etf_collector.auth.kis_auth.requests.post")
    def test_fetch_token_timeout(self, mock_post):
        """Test token fetch timeout."""
        import requests

        mock_post.side_effect = requests.exceptions.Timeout()

        client = KisAuthClient("app_key", "app_secret", "https://api.test.com")

        with pytest.raises(AuthError) as exc_info:
            client.get_token()

        assert "timed out" in str(exc_info.value).lower()

    @patch("etf_collector.auth.kis_auth.requests.post")
    def test_fetch_token_connection_error(self, mock_post):
        """Test token fetch connection error."""
        import requests

        mock_post.side_effect = requests.exceptions.ConnectionError("Connection failed")

        client = KisAuthClient("app_key", "app_secret", "https://api.test.com")

        with pytest.raises(AuthError) as exc_info:
            client.get_token()

        assert "connection" in str(exc_info.value).lower()

    @patch("etf_collector.auth.kis_auth.requests.post")
    def test_fetch_token_no_access_token(self, mock_post):
        """Test token fetch with missing access_token."""
        mock_resp = Mock()
        mock_resp.json.return_value = {"error_description": "Invalid credentials"}
        mock_resp.raise_for_status = Mock()
        mock_post.return_value = mock_resp

        client = KisAuthClient("app_key", "app_secret", "https://api.test.com")

        with pytest.raises(AuthError) as exc_info:
            client.get_token()

        assert "Invalid credentials" in str(exc_info.value)

    def test_clear_token(self, mock_kis_token_response):
        """Test clearing token cache."""
        with patch("etf_collector.auth.kis_auth.requests.post") as mock_post:
            mock_resp = Mock()
            mock_resp.json.return_value = mock_kis_token_response
            mock_resp.raise_for_status = Mock()
            mock_post.return_value = mock_resp

            client = KisAuthClient("app_key", "app_secret", "https://api.test.com")

            # Get token
            client.get_token()
            assert client._token is not None

            # Clear cache
            client.clear_token()
            assert client._token is None

            # Get token again should fetch new one
            client.get_token()
            assert mock_post.call_count == 2
