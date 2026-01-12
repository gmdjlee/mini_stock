"""Date utilities."""

from datetime import datetime, timedelta
from typing import Optional


def today_str(fmt: str = "%Y%m%d") -> str:
    """Get today's date as string."""
    return datetime.now().strftime(fmt)


def fmt_date(dt: datetime, fmt: str = "%Y%m%d") -> str:
    """Format datetime to string."""
    return dt.strftime(fmt)


def parse_date(date_str: str, fmt: str = "%Y%m%d") -> datetime:
    """Parse date string to datetime."""
    return datetime.strptime(date_str, fmt)


def days_ago(days: int, fmt: str = "%Y%m%d") -> str:
    """Get date N days ago as string."""
    dt = datetime.now() - timedelta(days=days)
    return dt.strftime(fmt)


def date_range(start_date: str, end_date: str, fmt: str = "%Y%m%d") -> int:
    """Calculate days between two dates."""
    start = parse_date(start_date, fmt)
    end = parse_date(end_date, fmt)
    return (end - start).days


def to_display_date(date_str: str, from_fmt: str = "%Y%m%d", to_fmt: str = "%Y-%m-%d") -> str:
    """Convert date format for display."""
    dt = parse_date(date_str, from_fmt)
    return dt.strftime(to_fmt)
