"""Cash/deposit item detection utility for ETF constituents.

Mirrors logic from Android CashItemUtil.kt for consistency.

Cash items in KIS API ETF constituent responses may have null or empty stock codes.
This utility provides consistent detection logic and synthetic code generation.
"""

from typing import Optional, Tuple

from .logger import get_logger

_logger = get_logger(__name__)

# Stock codes known to represent cash/deposit items
# 010010: Common short code for KRW cash/deposit in ETF constituent data
# KRD010010001: ISIN-format code for 원화현금/원화예금 (full ISIN with KR prefix)
CASH_STOCK_CODES = frozenset({
    "010010",
    "KRD010010001",
})

# Keywords that identify cash/deposit items by name (lowercase for matching)
CASH_NAME_KEYWORDS = (
    "원화현금",
    "원화예금",
    "현금",
    "cash",
    "예금",
    "krw",
    "원화",
    "예치금",
    "mmf",
    "머니마켓",
    "money market",
)


def is_cash_code(stock_code: Optional[str]) -> bool:
    """Check if stock code is a known cash/deposit code.

    Args:
        stock_code: The stock code to check

    Returns:
        True if the code is a known cash code
    """
    if not stock_code:
        return False
    return stock_code in CASH_STOCK_CODES


def is_cash_name(stock_name: Optional[str]) -> Tuple[bool, Optional[str]]:
    """Check if stock name indicates cash/deposit.

    Args:
        stock_name: The name of the constituent item

    Returns:
        Tuple of (is_cash, matched_keyword)
    """
    if not stock_name:
        return False, None

    lower_name = stock_name.lower()
    for keyword in CASH_NAME_KEYWORDS:
        if keyword.lower() in lower_name:
            return True, keyword
    return False, None


def is_cash_item(stock_code: Optional[str], stock_name: Optional[str]) -> bool:
    """Check if item is cash/deposit by code or name.

    Args:
        stock_code: The stock code
        stock_name: The stock name

    Returns:
        True if the item is identified as cash/deposit
    """
    if is_cash_code(stock_code):
        return True
    is_cash, _ = is_cash_name(stock_name)
    return is_cash


def generate_cash_code(etf_code: str, stock_name: str) -> str:
    """Generate synthetic stock code for cash items.

    Format: CASH_{etfCode}_{hash}
    This ensures uniqueness per ETF and per cash name type.

    Args:
        etf_code: The ETF code (e.g., "069500")
        stock_name: The cash item name (e.g., "원화예금")

    Returns:
        A synthetic stock code (e.g., "CASH_069500_1A2B")
    """
    # Use hash & 0xFFFF to get 4-digit hex, matching Android implementation
    name_hash = hash(stock_name) & 0xFFFF
    return f"CASH_{etf_code}_{name_hash:04X}"


def is_synthetic_cash_code(stock_code: str) -> bool:
    """Check if a stock code is a synthetic cash code.

    Args:
        stock_code: The stock code to check

    Returns:
        True if the code is a synthetic cash code
    """
    return stock_code.startswith("CASH_")


def log_cash_detection(
    etf_code: str,
    stock_code: Optional[str],
    stock_name: Optional[str],
    evaluation_amount: int,
) -> None:
    """Log cash item detection with full details.

    Use this for tracking which keywords/codes are matched during ETF collection.

    Args:
        etf_code: The ETF code containing this cash item
        stock_code: The stock code (may be None/empty for cash items)
        stock_name: The stock name
        evaluation_amount: The evaluation amount in won
    """
    matched_by_code = is_cash_code(stock_code)
    _, matched_keyword = is_cash_name(stock_name)
    amount_in_eok = evaluation_amount / 100_000_000

    _logger.info(
        "Cash detected: ETF=%s, code=%s, name=%s, amount=%.2f억, "
        "matchedByCode=%s, matchedKeyword=%s",
        etf_code,
        stock_code,
        stock_name,
        amount_in_eok,
        matched_by_code,
        matched_keyword,
    )
