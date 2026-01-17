"""JSON utilities."""

import json
from dataclasses import asdict, is_dataclass
from typing import Any, Dict, Type, TypeVar

T = TypeVar("T")


def to_json(obj: Any, indent: int = None) -> str:
    """Convert object to JSON string."""
    if is_dataclass(obj):
        return json.dumps(asdict(obj), indent=indent, ensure_ascii=False)
    return json.dumps(obj, indent=indent, ensure_ascii=False)


def from_json(json_str: str) -> Dict[str, Any]:
    """Parse JSON string to dictionary."""
    return json.loads(json_str)


def safe_get(data: Dict[str, Any], key: str, default: Any = None) -> Any:
    """Safely get value from dictionary."""
    return data.get(key, default) if data else default


def safe_int(value: Any, default: int = 0) -> int:
    """Safely convert value to int."""
    try:
        return int(value) if value is not None else default
    except (ValueError, TypeError):
        return default


def safe_float(value: Any, default: float = 0.0) -> float:
    """Safely convert value to float."""
    try:
        return float(value) if value is not None else default
    except (ValueError, TypeError):
        return default
