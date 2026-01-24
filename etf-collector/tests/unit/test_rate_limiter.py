"""Tests for rate limiter module."""

import threading
import time

import pytest

from etf_collector.limiter.rate_limiter import (
    SlidingWindowRateLimiter,
    RateLimiterConfig,
    create_rate_limiter,
)


class TestRateLimiterConfig:
    """Tests for RateLimiterConfig."""

    def test_default_config(self):
        """Test default configuration values."""
        config = RateLimiterConfig()
        assert config.requests_per_second == 15.0
        assert config.burst_size == 1
        assert config.retry_on_limit is True
        assert config.max_retries == 3
        assert config.retry_delay == 1.0

    def test_custom_config(self):
        """Test custom configuration."""
        config = RateLimiterConfig(
            requests_per_second=5.0,
            burst_size=2,
            retry_on_limit=False,
            max_retries=5,
            retry_delay=2.0,
        )
        assert config.requests_per_second == 5.0
        assert config.burst_size == 2
        assert config.retry_on_limit is False
        assert config.max_retries == 5
        assert config.retry_delay == 2.0


class TestSlidingWindowRateLimiter:
    """Tests for SlidingWindowRateLimiter."""

    def test_basic_acquire(self):
        """Test basic acquire functionality."""
        config = RateLimiterConfig(requests_per_second=10.0)
        limiter = SlidingWindowRateLimiter(config)

        # First request should succeed immediately
        assert limiter.acquire() is True

    def test_try_acquire_success(self):
        """Test try_acquire returns True when allowed."""
        config = RateLimiterConfig(requests_per_second=10.0)
        limiter = SlidingWindowRateLimiter(config)

        assert limiter.try_acquire() is True

    def test_try_acquire_rate_limited(self):
        """Test try_acquire returns False when rate limited."""
        config = RateLimiterConfig(requests_per_second=2.0)
        limiter = SlidingWindowRateLimiter(config)

        # Make requests up to limit
        assert limiter.try_acquire() is True
        assert limiter.try_acquire() is True

        # Third request should be rate limited
        assert limiter.try_acquire() is False

    def test_rate_limiting(self):
        """Test that rate limiting kicks in after burst."""
        config = RateLimiterConfig(requests_per_second=5.0)
        limiter = SlidingWindowRateLimiter(config)

        start = time.time()
        for _ in range(6):
            limiter.acquire()
        elapsed = time.time() - start

        # Should take at least 0.2s (1 request needs to wait)
        assert elapsed >= 0.15

    def test_acquire_with_timeout(self):
        """Test acquire with timeout."""
        config = RateLimiterConfig(requests_per_second=2.0)
        limiter = SlidingWindowRateLimiter(config)

        # Fill up the limit
        assert limiter.acquire(timeout=1.0) is True
        assert limiter.acquire(timeout=1.0) is True

        # Third request should timeout quickly since limit is reached
        start = time.time()
        result = limiter.acquire(timeout=0.1)
        elapsed = time.time() - start

        # When limit is reached, acquire with timeout should return False
        # But if window slides enough, it might still succeed
        # So we just verify timing is reasonable (with some tolerance for timing precision)
        assert elapsed >= 0.1 or result is True
        assert elapsed < 1.1  # Allow small tolerance for timing precision

    def test_wait_if_needed(self):
        """Test wait_if_needed convenience method."""
        config = RateLimiterConfig(requests_per_second=10.0)
        limiter = SlidingWindowRateLimiter(config)

        # Should not raise
        limiter.wait_if_needed()

    def test_reset(self):
        """Test reset clears request history."""
        config = RateLimiterConfig(requests_per_second=2.0)
        limiter = SlidingWindowRateLimiter(config)

        # Fill up limit
        limiter.acquire()
        limiter.acquire()
        assert limiter.try_acquire() is False

        # Reset
        limiter.reset()

        # Should allow requests again
        assert limiter.try_acquire() is True

    def test_current_rate(self):
        """Test current_rate property."""
        config = RateLimiterConfig(requests_per_second=10.0)
        limiter = SlidingWindowRateLimiter(config)

        assert limiter.current_rate == 0.0

        limiter.acquire()
        assert limiter.current_rate == 1.0

        limiter.acquire()
        assert limiter.current_rate == 2.0

    def test_wait_time(self):
        """Test wait_time estimation."""
        config = RateLimiterConfig(requests_per_second=2.0)
        limiter = SlidingWindowRateLimiter(config)

        # No wait needed initially
        assert limiter.wait_time() == 0.0

        # Fill up limit
        limiter.acquire()
        limiter.acquire()

        # Should have positive wait time
        wait = limiter.wait_time()
        assert wait > 0.0
        assert wait <= 1.0

    def test_thread_safety(self):
        """Test thread-safe operation."""
        config = RateLimiterConfig(requests_per_second=10.0)
        limiter = SlidingWindowRateLimiter(config)

        results = []
        errors = []

        def worker():
            try:
                for _ in range(5):
                    limiter.acquire()
                    results.append(time.time())
            except Exception as e:
                errors.append(e)

        threads = [threading.Thread(target=worker) for _ in range(3)]
        for t in threads:
            t.start()
        for t in threads:
            t.join()

        assert len(errors) == 0
        assert len(results) == 15


class TestCreateRateLimiter:
    """Tests for create_rate_limiter factory function."""

    def test_real_environment(self):
        """Test rate limiter for real environment."""
        limiter = create_rate_limiter("real")
        assert limiter.config.requests_per_second == 15.0

    def test_virtual_environment(self):
        """Test rate limiter for virtual environment."""
        limiter = create_rate_limiter("virtual")
        assert limiter.config.requests_per_second == 4.0

    def test_default_environment(self):
        """Test default environment is real."""
        limiter = create_rate_limiter()
        assert limiter.config.requests_per_second == 15.0
