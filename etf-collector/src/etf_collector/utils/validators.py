"""Centralized input validation module for ETF Collector.

This module provides validation utilities to prevent security vulnerabilities
and ensure data integrity across the application.
"""

import re
from pathlib import Path
from typing import Any, Dict, List, Optional, Tuple


# Validation patterns
ETF_CODE_PATTERN = re.compile(r"^[0-9]{6}$")
DATE_PATTERN = re.compile(r"^\d{4}-\d{2}-\d{2}$")
DATE_COMPACT_PATTERN = re.compile(r"^\d{8}$")

# Dangerous path patterns to block
DANGEROUS_PATH_PATTERNS = ["..", "~", "/etc", "/var", "/tmp", "C:\\Windows", "C:\\System"]

# Allowed file extensions for storage
ALLOWED_EXTENSIONS = {".json", ".csv"}


class ValidationError(Exception):
    """Validation error with error code."""

    def __init__(self, message: str, code: str = "VALIDATION_ERROR"):
        super().__init__(message)
        self.code = code
        self.message = message


def validate_etf_code(code: Any) -> Tuple[bool, Optional[str]]:
    """Validate ETF code format (6 digits).

    Args:
        code: ETF code to validate

    Returns:
        Tuple of (is_valid, error_message)
    """
    if not code:
        return False, "ETF code is required"
    if not isinstance(code, str):
        return False, f"ETF code must be string, got {type(code).__name__}"
    if not ETF_CODE_PATTERN.match(code):
        return False, f"Invalid ETF code format: {code} (expected 6 digits)"
    return True, None


def validate_date(date_str: Any, allow_compact: bool = True) -> Tuple[bool, Optional[str]]:
    """Validate date string format.

    Args:
        date_str: Date string to validate
        allow_compact: Allow YYYYMMDD format in addition to YYYY-MM-DD

    Returns:
        Tuple of (is_valid, error_message)
    """
    if not date_str:
        return False, "Date is required"
    if not isinstance(date_str, str):
        return False, f"Date must be string, got {type(date_str).__name__}"

    if DATE_PATTERN.match(date_str):
        return True, None
    if allow_compact and DATE_COMPACT_PATTERN.match(date_str):
        return True, None

    return False, f"Invalid date format: {date_str} (expected YYYY-MM-DD or YYYYMMDD)"


def validate_path(
    filepath: str,
    base_dir: Optional[str] = None,
    allowed_extensions: Optional[set] = None,
) -> Tuple[bool, Optional[str], Optional[Path]]:
    """Validate file path for security (path traversal prevention).

    Args:
        filepath: Path to validate
        base_dir: If provided, ensure path is within this directory
        allowed_extensions: If provided, only allow these file extensions

    Returns:
        Tuple of (is_valid, error_message, resolved_path)
    """
    if not filepath:
        return False, "File path is required", None
    if not isinstance(filepath, str):
        return False, f"File path must be string, got {type(filepath).__name__}", None

    # Check for dangerous patterns
    for pattern in DANGEROUS_PATH_PATTERNS:
        if pattern in filepath:
            return False, f"Dangerous path pattern detected: {pattern}", None

    try:
        path = Path(filepath).resolve()
    except Exception as e:
        return False, f"Invalid path: {e}", None

    # Check if within base directory
    if base_dir:
        try:
            base = Path(base_dir).resolve()
            if not str(path).startswith(str(base)):
                return False, "Path traversal attempt detected", None
        except Exception as e:
            return False, f"Invalid base directory: {e}", None

    # Check file extension
    if allowed_extensions:
        ext = path.suffix.lower()
        if ext not in allowed_extensions:
            return False, f"Unsupported file extension: {ext}", None

    return True, None, path


def validate_filename(
    filename: str,
    allowed_extensions: Optional[set] = None,
) -> Tuple[bool, Optional[str]]:
    """Validate filename (no path separators allowed).

    Args:
        filename: Filename to validate
        allowed_extensions: If provided, only allow these file extensions

    Returns:
        Tuple of (is_valid, error_message)
    """
    if not filename:
        return False, "Filename is required"
    if not isinstance(filename, str):
        return False, f"Filename must be string, got {type(filename).__name__}"

    # Disallow path separators
    if "/" in filename or "\\" in filename:
        return False, "Filename cannot contain path separators"

    # Disallow dangerous patterns
    if ".." in filename:
        return False, "Filename cannot contain '..'"

    # Check file extension
    if allowed_extensions:
        ext = Path(filename).suffix.lower()
        if ext not in allowed_extensions:
            return False, f"Unsupported file extension: {ext}"

    return True, None


def validate_api_response(
    data: Any,
    required_fields: Optional[List[str]] = None,
) -> Tuple[bool, Optional[str]]:
    """Validate API response structure.

    Args:
        data: API response data to validate
        required_fields: List of required field names

    Returns:
        Tuple of (is_valid, error_message)
    """
    if data is None:
        return False, "Response data is None"
    if not isinstance(data, dict):
        return False, f"Response must be dict, got {type(data).__name__}"

    if required_fields:
        missing = [f for f in required_fields if f not in data]
        if missing:
            return False, f"Missing required fields: {', '.join(missing)}"

    return True, None


def validate_list_response(
    data: Any,
    item_type: type = dict,
) -> Tuple[bool, Optional[str]]:
    """Validate that data is a list of expected type.

    Args:
        data: Data to validate
        item_type: Expected type of list items

    Returns:
        Tuple of (is_valid, error_message)
    """
    if data is None:
        return False, "Data is None"
    if not isinstance(data, list):
        return False, f"Expected list, got {type(data).__name__}"

    for i, item in enumerate(data):
        if not isinstance(item, item_type):
            return False, f"Item {i} has invalid type: expected {item_type.__name__}, got {type(item).__name__}"

    return True, None


def mask_credentials(data: Dict[str, Any]) -> Dict[str, Any]:
    """Mask sensitive credentials in a dictionary for safe logging.

    Args:
        data: Dictionary potentially containing credentials

    Returns:
        Copy of dictionary with masked credentials
    """
    if not data:
        return data

    sensitive_keys = {
        "appkey",
        "appsecret",
        "secretkey",
        "secret_key",
        "app_secret",
        "password",
        "token",
        "access_token",
        "authorization",
    }

    masked = {}
    for key, value in data.items():
        key_lower = key.lower()
        if key_lower in sensitive_keys or any(s in key_lower for s in ["secret", "key", "token", "password"]):
            if isinstance(value, str) and len(value) > 4:
                masked[key] = value[:4] + "****"
            else:
                masked[key] = "****"
        elif isinstance(value, dict):
            masked[key] = mask_credentials(value)
        else:
            masked[key] = value

    return masked


def safe_int(val: Any, default: int = 0) -> int:
    """Safely convert value to integer.

    Args:
        val: Value to convert
        default: Default value if conversion fails

    Returns:
        Converted integer or default
    """
    if val is None:
        return default
    if isinstance(val, bool):
        return 1 if val else 0
    try:
        if isinstance(val, str):
            val = val.strip()
            if not val:
                return default
        return int(float(val))
    except (TypeError, ValueError):
        return default


def safe_float(val: Any, default: float = 0.0) -> float:
    """Safely convert value to float.

    Args:
        val: Value to convert
        default: Default value if conversion fails

    Returns:
        Converted float or default
    """
    if val is None:
        return default
    if isinstance(val, bool):
        return 1.0 if val else 0.0
    try:
        if isinstance(val, str):
            val = val.strip()
            if not val:
                return default
        return float(val)
    except (TypeError, ValueError):
        return default
