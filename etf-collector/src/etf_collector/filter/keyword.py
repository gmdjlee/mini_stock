"""Keyword-based ETF filtering."""

from dataclasses import dataclass, field
from enum import Enum
from typing import List

from ..collector.etf_list import EtfInfo


class FilterMode(Enum):
    """Keyword filter mode."""

    INCLUDE = "include"  # Any keyword matches (select)
    EXCLUDE = "exclude"  # No keyword matches (reject if any match)
    INCLUDE_AND = "include_and"  # All keywords must match
    INCLUDE_OR = "include_or"  # Any keyword matches (alias for INCLUDE)


@dataclass
class KeywordFilter:
    """Keyword-based filter for ETF names.

    Attributes:
        keywords: List of keywords to match
        mode: Filter mode (include, exclude, etc.)
        case_sensitive: Whether matching is case-sensitive
    """

    keywords: List[str] = field(default_factory=list)
    mode: FilterMode = FilterMode.INCLUDE_OR
    case_sensitive: bool = False

    def matches(self, text: str) -> bool:
        """Check if text matches filter criteria.

        Args:
            text: Text to check (usually ETF name)

        Returns:
            True if text matches filter criteria
        """
        if not self.keywords:
            return True  # No filter = match all

        target = text if self.case_sensitive else text.lower()
        search_keywords = (
            self.keywords
            if self.case_sensitive
            else [kw.lower() for kw in self.keywords]
        )

        if self.mode in (FilterMode.INCLUDE, FilterMode.INCLUDE_OR):
            return any(kw in target for kw in search_keywords)

        if self.mode == FilterMode.EXCLUDE:
            return not any(kw in target for kw in search_keywords)

        if self.mode == FilterMode.INCLUDE_AND:
            return all(kw in target for kw in search_keywords)

        return False

    def filter_etfs(self, etfs: List[EtfInfo]) -> List[EtfInfo]:
        """Filter list of ETFs by this filter.

        Args:
            etfs: List of EtfInfo objects

        Returns:
            Filtered list of EtfInfo objects
        """
        return [etf for etf in etfs if self.matches(etf.etf_name)]

    def __str__(self) -> str:
        """String representation."""
        return f"KeywordFilter(keywords={self.keywords}, mode={self.mode.value})"


# Preset filters
ACTIVE_ETF_FILTER = KeywordFilter(
    keywords=["액티브", "Active"],
    mode=FilterMode.INCLUDE_OR,
    case_sensitive=False,
)
"""Filter for active ETFs (includes names with '액티브' or 'Active')."""

EXCLUDE_LEVERAGE_FILTER = KeywordFilter(
    keywords=["레버리지", "인버스", "2X", "3X", "inverse", "Leverage"],
    mode=FilterMode.EXCLUDE,
    case_sensitive=False,
)
"""Filter to exclude leverage/inverse ETFs."""


def combine_filters(*filters: KeywordFilter) -> "CombinedFilter":
    """Combine multiple filters (all must pass).

    Args:
        *filters: KeywordFilter instances to combine

    Returns:
        CombinedFilter that requires all filters to pass
    """
    return CombinedFilter(list(filters))


@dataclass
class CombinedFilter:
    """Combined filter that requires all sub-filters to pass."""

    filters: List[KeywordFilter] = field(default_factory=list)

    def matches(self, text: str) -> bool:
        """Check if text matches all filters.

        Args:
            text: Text to check

        Returns:
            True if all filters match
        """
        return all(f.matches(text) for f in self.filters)

    def filter_etfs(self, etfs: List[EtfInfo]) -> List[EtfInfo]:
        """Filter ETFs through all sub-filters.

        Args:
            etfs: List of EtfInfo objects

        Returns:
            Filtered list (passes all filters)
        """
        result = etfs
        for f in self.filters:
            result = f.filter_etfs(result)
        return result


def create_filter_from_args(
    include: List[str] = None,
    exclude: List[str] = None,
    active_only: bool = False,
) -> CombinedFilter:
    """Create filter from CLI arguments.

    Args:
        include: Keywords to include (comma-separated in CLI)
        exclude: Keywords to exclude (comma-separated in CLI)
        active_only: Whether to filter only active ETFs

    Returns:
        CombinedFilter based on arguments
    """
    filters = []

    if active_only:
        filters.append(ACTIVE_ETF_FILTER)

    if include:
        filters.append(
            KeywordFilter(
                keywords=include,
                mode=FilterMode.INCLUDE_OR,
            )
        )

    if exclude:
        filters.append(
            KeywordFilter(
                keywords=exclude,
                mode=FilterMode.EXCLUDE,
            )
        )

    return CombinedFilter(filters) if filters else CombinedFilter()
