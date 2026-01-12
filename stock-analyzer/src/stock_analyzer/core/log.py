"""Logging utilities."""

import logging
from typing import Any, Dict, Optional

# Configure root logger
logging.basicConfig(
    level=logging.INFO,
    format="[%(name)s] %(levelname)s: %(message)s",
)


def get_logger(name: str) -> logging.Logger:
    """Get a logger with the given name."""
    return logging.getLogger(name)


def log_info(module: str, msg: str, ctx: Optional[Dict[str, Any]] = None) -> None:
    """Log info message with context."""
    logger = get_logger(module)
    if ctx:
        logger.info(f"{msg} {ctx}")
    else:
        logger.info(msg)


def log_err(module: str, error: Exception, ctx: Optional[Dict[str, Any]] = None) -> None:
    """Log error message with context."""
    logger = get_logger(module)
    if ctx:
        logger.error(f"{str(error)} {ctx}")
    else:
        logger.error(str(error))
