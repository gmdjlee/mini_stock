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
    """

    requests_per_second: float = 15.0
    burst_size: int = 1
    retry_on_limit: bool = True
    max_retries: int = 3
    retry_delay: float = 1.0


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
        self.min_interval = 1.0 / self.config.requests_per_second

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

            # Wait before retry
            time.sleep(self.min_interval)

    def try_acquire(self) -> bool:
        """Non-blocking acquire - returns immediately.

        Returns:
            True if acquired, False if rate limit exceeded
        """
        with self.lock:
            current_time = time.time()

            # Remove timestamps outside the window
            while self.request_times and current_time - self.request_times[0] > self.window_size:
                self.request_times.popleft()

            # Check if we can make a request
            if len(self.request_times) < self.config.requests_per_second:
                self.request_times.append(current_time)
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
            if len(self.request_times) < self.config.requests_per_second:
                return 0.0

            current_time = time.time()
            oldest_time = self.request_times[0] if self.request_times else current_time

            # Time until oldest request falls out of window
            wait = (oldest_time + self.window_size) - current_time
            return max(0.0, wait)


def create_rate_limiter(environment: str = "real") -> SlidingWindowRateLimiter:
    """Create a rate limiter configured for the given environment.

    Args:
        environment: "real" or "virtual"

    Returns:
        Configured SlidingWindowRateLimiter
    """
    if environment == "virtual":
        config = RateLimiterConfig(requests_per_second=4.0)
    else:
        config = RateLimiterConfig(requests_per_second=15.0)

    return SlidingWindowRateLimiter(config)
