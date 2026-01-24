"""Sliding window rate limiter for API requests."""

import threading
import time
from collections import deque
from dataclasses import dataclass
from typing import Optional

from ..utils.logger import log_debug, log_warn


@dataclass
class RateLimiterConfig:
    """Rate limiter configuration.

    Attributes:
        requests_per_second: Maximum requests per second (Real: 15, Virtual: 4)
        burst_size: Allowed burst size
        retry_on_limit: Whether to retry on rate limit
        max_retries: Maximum retry attempts
        retry_delay: Base delay between retries (seconds)
        min_interval: Minimum interval between requests (seconds)
            This helps prevent server-side rate limiting even when
            requests_per_second would allow faster calls.
    """

    requests_per_second: float = 15.0
    burst_size: int = 1
    retry_on_limit: bool = True
    max_retries: int = 3
    retry_delay: float = 1.0
    min_interval: float = 0.0  # Minimum interval between requests


class SlidingWindowRateLimiter:
    """Thread-safe sliding window rate limiter.

    Uses a sliding window algorithm to track request timestamps and
    enforce rate limits.
    """

    def __init__(self, config: Optional[RateLimiterConfig] = None):
        """Initialize rate limiter.

        Args:
            config: Rate limiter configuration (uses defaults if None)
        """
        self.config = config or RateLimiterConfig()
        self.window_size = 1.0  # 1 second window
        self.request_times: deque = deque()
        self.lock = threading.Lock()
        # min_interval for acquire() sleep, based on requests_per_second
        self.min_interval = 1.0 / self.config.requests_per_second
        # Configured minimum interval between requests (only if > 0)
        self._enforced_min_interval = self.config.min_interval
        self._last_request_time: float = 0.0

    def acquire(self, timeout: Optional[float] = None) -> bool:
        """Blocking acquire - waits until request is allowed.

        Args:
            timeout: Maximum time to wait (None for unlimited)

        Returns:
            True if acquired, False if timeout
        """
        start_time = time.time()

        while True:
            if self.try_acquire():
                return True

            # Check timeout
            if timeout is not None and time.time() - start_time >= timeout:
                log_warn(
                    "rate_limiter",
                    "Acquire timed out",
                    {"timeout": timeout, "elapsed": time.time() - start_time},
                )
                return False

            # Wait for the appropriate amount of time before retry
            # Use wait_time() which accounts for both enforced min interval
            # and sliding window rate limiting
            wait = self.wait_time()
            if wait > 0:
                time.sleep(wait)
            else:
                # Minimum sleep to avoid busy waiting
                time.sleep(self.min_interval)

    def try_acquire(self) -> bool:
        """Non-blocking acquire - returns immediately.

        Returns:
            True if acquired, False if rate limit exceeded
        """
        with self.lock:
            current_time = time.time()

            # Check enforced minimum interval since last request (only if > 0)
            if self._enforced_min_interval > 0:
                time_since_last = current_time - self._last_request_time
                if time_since_last < self._enforced_min_interval:
                    log_debug(
                        "rate_limiter",
                        "Min interval not reached",
                        {"time_since_last": time_since_last, "min_interval": self._enforced_min_interval},
                    )
                    return False

            # Remove timestamps outside the window
            while self.request_times and current_time - self.request_times[0] > self.window_size:
                self.request_times.popleft()

            # Check if we can make a request
            if len(self.request_times) < self.config.requests_per_second:
                self.request_times.append(current_time)
                self._last_request_time = current_time
                log_debug(
                    "rate_limiter",
                    "Request acquired",
                    {"requests_in_window": len(self.request_times)},
                )
                return True

            log_debug(
                "rate_limiter",
                "Rate limit reached",
                {"requests_in_window": len(self.request_times)},
            )
            return False

    def wait_if_needed(self) -> None:
        """Convenience method - blocks until request is allowed."""
        self.acquire()

    def reset(self) -> None:
        """Reset the rate limiter state."""
        with self.lock:
            self.request_times.clear()
            self._last_request_time = 0.0
        log_debug("rate_limiter", "Rate limiter reset")

    @property
    def current_rate(self) -> float:
        """Get current request rate (requests in last second).

        Returns:
            Number of requests in the current window
        """
        with self.lock:
            current_time = time.time()
            # Count requests within window
            count = sum(1 for t in self.request_times if current_time - t <= self.window_size)
            return float(count)

    def wait_time(self) -> float:
        """Get estimated wait time until next request is allowed.

        Returns:
            Estimated wait time in seconds (0 if no wait needed)
        """
        with self.lock:
            current_time = time.time()

            # Check enforced minimum interval wait time (only if > 0)
            min_interval_wait = 0.0
            if self._enforced_min_interval > 0:
                min_interval_wait = self._enforced_min_interval - (current_time - self._last_request_time)

            # Check sliding window wait time
            window_wait = 0.0
            if len(self.request_times) >= self.config.requests_per_second:
                oldest_time = self.request_times[0] if self.request_times else current_time
                window_wait = (oldest_time + self.window_size) - current_time

            # Return the maximum of both wait times
            return max(0.0, min_interval_wait, window_wait)


def create_rate_limiter(environment: str = "real") -> SlidingWindowRateLimiter:
    """Create a rate limiter configured for the given environment.

    Args:
        environment: "real" or "virtual"

    Returns:
        Configured SlidingWindowRateLimiter
    """
    if environment == "virtual":
        # Virtual environment: 4 req/s, min 0.5s interval
        config = RateLimiterConfig(requests_per_second=4.0, min_interval=0.5)
    else:
        # Real environment: 15 req/s, but KIS API may have stricter limits
        # on certain endpoints (like ETF constituents), so we add a minimum
        # interval of 0.5s to prevent server-side rate limiting (500 errors)
        config = RateLimiterConfig(requests_per_second=15.0, min_interval=0.5)

    return SlidingWindowRateLimiter(config)
