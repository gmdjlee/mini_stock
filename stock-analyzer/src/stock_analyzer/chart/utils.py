"""Common utilities for chart modules."""

from datetime import datetime
from typing import List

import matplotlib.pyplot as plt
import matplotlib.font_manager as fm


# Color constants
COLORS = {
    # Elder Impulse colors
    "elder_green": "#26A69A",  # Teal green
    "elder_red": "#EF5350",  # Material red
    "elder_blue": "#42A5F5",  # Material blue
    # MA line colors
    "ma5": "#FF9800",  # Orange
    "ma20": "#2196F3",  # Blue
    "ma60": "#9C27B0",  # Purple
    "ma120": "#795548",  # Brown
    "ma_default": "#607D8B",  # Gray
    # Chart colors
    "up": "#26A69A",  # Green for price increase
    "down": "#EF5350",  # Red for price decrease
    "neutral": "#42A5F5",  # Blue for neutral
    # Bar chart colors
    "positive": "#26A69A",
    "negative": "#EF5350",
}

# Korean fonts in priority order
KOREAN_FONTS = [
    "Malgun Gothic",  # Windows
    "맑은 고딕",  # Windows (Korean name)
    "NanumGothic",  # Linux/Mac (commonly installed)
    "NanumBarunGothic",
    "AppleGothic",  # Mac
    "Apple SD Gothic Neo",  # Mac
    "Noto Sans CJK KR",  # Cross-platform
]


def configure_korean_font() -> str | None:
    """Configure matplotlib to use a font that supports Korean characters.

    Returns:
        Font name if configured, None otherwise.
    """
    available_fonts = {f.name for f in fm.fontManager.ttflist}

    for font in KOREAN_FONTS:
        if font in available_fonts:
            plt.rcParams["font.family"] = font
            plt.rcParams["axes.unicode_minus"] = False
            return font

    return None


# Try to configure Korean font at module load
_configured_font = configure_korean_font()


def sanitize_text(text: str) -> str:
    """Remove Korean characters if no Korean font is available.

    Args:
        text: Text to sanitize.

    Returns:
        Sanitized text (Korean removed if no font available).
    """
    if _configured_font is not None:
        return text
    # Remove Korean characters (Hangul range: U+AC00 to U+D7A3)
    return "".join(c for c in text if not ("\uac00" <= c <= "\ud7a3")).strip()


def parse_date(date_str: str) -> datetime:
    """Parse date string to datetime.

    Supports formats: YYYYMMDD, YYYY-MM-DD

    Args:
        date_str: Date string.

    Returns:
        Parsed datetime object.
    """
    for fmt in ("%Y%m%d", "%Y-%m-%d"):
        try:
            return datetime.strptime(date_str, fmt)
        except ValueError:
            continue
    return datetime.now()


def format_xaxis(ax, dates: List[datetime]) -> None:
    """Format x-axis with date labels.

    Args:
        ax: Matplotlib axis.
        dates: List of datetime objects.
    """
    n = len(dates)
    if n <= 30:
        step = 5
    elif n <= 90:
        step = 10
    else:
        step = 20

    tick_positions = list(range(0, n, step))
    tick_labels = [dates[i].strftime("%m/%d") for i in tick_positions if i < n]

    ax.set_xticks(tick_positions[: len(tick_labels)])
    ax.set_xticklabels(tick_labels, rotation=45, ha="right")
    ax.set_xlim(-1, n)


def elder_to_color(elder: str) -> str:
    """Convert Elder Impulse color name to matplotlib color.

    Args:
        elder: Elder color name ("green", "red", "blue").

    Returns:
        Matplotlib color code.
    """
    colors = {
        "green": COLORS["elder_green"],
        "red": COLORS["elder_red"],
        "blue": COLORS["elder_blue"],
    }
    return colors.get(elder, COLORS["elder_blue"])


def get_bar_color(value: float) -> str:
    """Get bar color based on value sign.

    Args:
        value: Numeric value.

    Returns:
        Color code for positive or negative.
    """
    return COLORS["positive"] if value >= 0 else COLORS["negative"]


def save_figure(fig, save_path: str | None = None) -> dict:
    """Save figure to bytes and optionally to file.

    Args:
        fig: Matplotlib figure.
        save_path: Optional path to save image file.

    Returns:
        Dictionary with image_bytes and save_path.
    """
    import io

    buf = io.BytesIO()
    fig.savefig(buf, format="png", dpi=100, bbox_inches="tight")
    buf.seek(0)
    image_bytes = buf.read()
    buf.close()

    if save_path:
        with open(save_path, "wb") as f:
            f.write(image_bytes)

    return {
        "image_bytes": image_bytes,
        "save_path": save_path,
    }
