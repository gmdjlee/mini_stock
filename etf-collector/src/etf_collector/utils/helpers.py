"""Helper utilities for ETF Collector."""

from datetime import datetime, timedelta
from typing import Any, Optional, Union


def today_str(fmt: str = "%Y%m%d") -> str:
    """Get today's date as string.

    Args:
        fmt: Date format string

    Returns:
        Formatted date string
    """
    return datetime.now().strftime(fmt)


def now_iso() -> str:
    """Get current datetime in ISO format.

    Returns:
        ISO formatted datetime string
    """
    return datetime.now().isoformat()


def now_str(fmt: str = "%Y-%m-%d %H:%M:%S") -> str:
    """Get current datetime as formatted string.

    Args:
        fmt: Datetime format string

    Returns:
        Formatted datetime string
    """
    return datetime.now().strftime(fmt)


def parse_date(date_str: str, fmt: str = "%Y%m%d") -> datetime:
    """Parse date string to datetime.

    Args:
        date_str: Date string to parse
        fmt: Expected date format

    Returns:
        Parsed datetime object

    Raises:
        ValueError: If parsing fails
    """
    return datetime.strptime(date_str, fmt)


def format_date(dt: datetime, fmt: str = "%Y%m%d") -> str:
    """Format datetime to string.

    Args:
        dt: Datetime object
        fmt: Output format string

    Returns:
        Formatted date string
    """
    return dt.strftime(fmt)


def to_int(value: Any, default: int = 0) -> int:
    """Safe conversion to int.

    Args:
        value: Value to convert
        default: Default value if conversion fails

    Returns:
        Integer value
    """
    if value is None:
        return default
    try:
        # Handle string with commas
        if isinstance(value, str):
            value = value.replace(",", "").strip()
        return int(float(value))
    except (ValueError, TypeError):
        return default


def to_float(value: Any, default: float = 0.0) -> float:
    """Safe conversion to float.

    Args:
        value: Value to convert
        default: Default value if conversion fails

    Returns:
        Float value
    """
    if value is None:
        return default
    try:
        # Handle string with commas
        if isinstance(value, str):
            value = value.replace(",", "").strip()
        return float(value)
    except (ValueError, TypeError):
        return default


def format_number(value: Union[int, float], decimals: int = 0) -> str:
    """Format number with thousand separators.

    Args:
        value: Number to format
        decimals: Number of decimal places

    Returns:
        Formatted number string
    """
    if decimals > 0:
        return f"{value:,.{decimals}f}"
    return f"{int(value):,}"


def truncate_string(s: str, max_len: int = 50, suffix: str = "...") -> str:
    """Truncate string to max length.

    Args:
        s: String to truncate
        max_len: Maximum length
        suffix: Suffix to add if truncated

    Returns:
        Truncated string
    """
    if len(s) <= max_len:
        return s
    return s[: max_len - len(suffix)] + suffix
