"""Logging utilities for ETF Collector."""

import logging
import sys
from typing import Any, Dict, Optional

# Configure root logger
_log_format = "%(asctime)s [%(levelname)s] %(name)s: %(message)s"
_date_format = "%Y-%m-%d %H:%M:%S"

logging.basicConfig(
    level=logging.INFO,
    format=_log_format,
    datefmt=_date_format,
    handlers=[logging.StreamHandler(sys.stdout)],
)


def get_logger(name: str) -> logging.Logger:
    """Get a logger with the given name.

    Args:
        name: Logger name (usually module name)

    Returns:
        Configured logger instance
    """
    return logging.getLogger(name)


def _format_context(ctx: Optional[Dict[str, Any]]) -> str:
    """Format context dictionary for logging.

    Args:
        ctx: Context dictionary

    Returns:
        Formatted string
    """
    if not ctx:
        return ""
    parts = [f"{k}={v}" for k, v in ctx.items()]
    return " | " + ", ".join(parts)


def log_info(module: str, msg: str, ctx: Optional[Dict[str, Any]] = None) -> None:
    """Log info message with context.

    Args:
        module: Module name
        msg: Log message
        ctx: Optional context dictionary
    """
    logger = get_logger(module)
    logger.info(f"{msg}{_format_context(ctx)}")


def log_warn(module: str, msg: str, ctx: Optional[Dict[str, Any]] = None) -> None:
    """Log warning message with context.

    Args:
        module: Module name
        msg: Log message
        ctx: Optional context dictionary
    """
    logger = get_logger(module)
    logger.warning(f"{msg}{_format_context(ctx)}")


def log_err(module: str, error: Any, ctx: Optional[Dict[str, Any]] = None) -> None:
    """Log error message with context.

    Args:
        module: Module name
        error: Error message or exception
        ctx: Optional context dictionary
    """
    logger = get_logger(module)
    error_msg = str(error) if not isinstance(error, str) else error
    logger.error(f"{error_msg}{_format_context(ctx)}")


def log_debug(module: str, msg: str, ctx: Optional[Dict[str, Any]] = None) -> None:
    """Log debug message with context.

    Args:
        module: Module name
        msg: Log message
        ctx: Optional context dictionary
    """
    logger = get_logger(module)
    logger.debug(f"{msg}{_format_context(ctx)}")


def set_level(level: str) -> None:
    """Set logging level.

    Args:
        level: Logging level (DEBUG, INFO, WARNING, ERROR)
    """
    level_map = {
        "DEBUG": logging.DEBUG,
        "INFO": logging.INFO,
        "WARNING": logging.WARNING,
        "ERROR": logging.ERROR,
    }
    logging.getLogger().setLevel(level_map.get(level.upper(), logging.INFO))
