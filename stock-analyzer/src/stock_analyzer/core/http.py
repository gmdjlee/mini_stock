"""HTTP client utilities."""

from dataclasses import dataclass
from typing import Any, Dict, Optional

import requests

from .log import log_err, log_info


@dataclass
class HttpResponse:
    """HTTP response wrapper."""

    ok: bool
    status_code: int
    data: Optional[Dict[str, Any]] = None
    error: Optional[str] = None
    headers: Optional[Dict[str, str]] = None


class HttpClient:
    """HTTP client with retry support."""

    def __init__(self, timeout: int = 30, max_retries: int = 3):
        self.timeout = timeout
        self.max_retries = max_retries
        self.session = requests.Session()

    def post(
        self,
        url: str,
        headers: Dict[str, str],
        json_data: Dict[str, Any],
        timeout: Optional[int] = None,
    ) -> HttpResponse:
        """POST request with JSON body."""
        timeout = timeout or self.timeout

        try:
            resp = self.session.post(
                url,
                headers=headers,
                json=json_data,
                timeout=timeout,
            )

            return HttpResponse(
                ok=resp.ok,
                status_code=resp.status_code,
                data=resp.json() if resp.content else None,
                headers=dict(resp.headers),
            )

        except requests.Timeout:
            return HttpResponse(
                ok=False,
                status_code=0,
                error="Request timeout",
            )
        except requests.RequestException as e:
            log_err("http", e, {"url": url})
            return HttpResponse(
                ok=False,
                status_code=0,
                error=str(e),
            )

    def close(self) -> None:
        """Close the session."""
        self.session.close()
