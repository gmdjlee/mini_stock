"""Candlestick chart for OHLCV data."""

import io
from datetime import datetime
from typing import Dict, List, Optional

import matplotlib.pyplot as plt
import matplotlib.font_manager as fm
import pandas as pd

from ..core.log import log_info

# Configure matplotlib for non-GUI backend
plt.switch_backend("Agg")


def _configure_korean_font():
    """Configure matplotlib to use a font that supports Korean characters."""
    # List of fonts that typically support Korean (excluding DejaVu Sans which doesn't)
    korean_fonts = [
        "Malgun Gothic",  # Windows
        "맑은 고딕",  # Windows (Korean name)
        "NanumGothic",  # Linux/Mac (commonly installed)
        "NanumBarunGothic",
        "AppleGothic",  # Mac
        "Apple SD Gothic Neo",  # Mac
        "Noto Sans CJK KR",  # Cross-platform
    ]

    # Get available system fonts
    available_fonts = {f.name for f in fm.fontManager.ttflist}

    # Find the first available Korean font
    for font in korean_fonts:
        if font in available_fonts:
            plt.rcParams["font.family"] = font
            plt.rcParams["axes.unicode_minus"] = False  # Fix minus sign display
            return font

    return None


# Try to configure Korean font at module load
_configured_font = _configure_korean_font()


def _sanitize_text(text: str) -> str:
    """Remove Korean characters if no Korean font is available."""
    if _configured_font is not None:
        return text
    # Remove Korean characters (Hangul range: U+AC00 to U+D7A3)
    return "".join(c for c in text if not ("\uac00" <= c <= "\ud7a3")).strip()


def plot(
    dates: List[str],
    opens: List[int],
    highs: List[int],
    lows: List[int],
    closes: List[int],
    volumes: Optional[List[int]] = None,
    title: str = "",
    ma_lines: Optional[Dict[str, List[Optional[int]]]] = None,
    elder_colors: Optional[List[str]] = None,
    figsize: tuple = (12, 8),
    save_path: Optional[str] = None,
) -> Dict:
    """
    Create candlestick chart with optional indicators.

    Args:
        dates: Date list (YYYYMMDD format)
        opens: Open prices
        highs: High prices
        lows: Low prices
        closes: Close prices
        volumes: Volume list (optional, for volume subplot)
        title: Chart title
        ma_lines: Moving average lines {"MA5": [...], "MA20": [...]}
        elder_colors: Elder Impulse colors per candle ("green", "red", "blue")
        figsize: Figure size (width, height)
        save_path: Path to save image (optional)

    Returns:
        {
            "ok": True,
            "data": {
                "image_bytes": bytes (PNG image data),
                "save_path": str or None
            }
        }

    Errors:
        - INVALID_ARG: Invalid argument
        - NO_DATA: Insufficient data
        - CHART_ERROR: Chart generation failed
    """
    if not dates or not opens or not highs or not lows or not closes:
        return {
            "ok": False,
            "error": {"code": "INVALID_ARG", "msg": "OHLCV 데이터가 필요합니다"},
        }

    if len(dates) < 5:
        return {
            "ok": False,
            "error": {"code": "NO_DATA", "msg": "최소 5일 이상의 데이터가 필요합니다"},
        }

    try:
        # Create figure with subplots
        has_volume = volumes is not None and len(volumes) == len(dates)
        nrows = 2 if has_volume else 1
        height_ratios = [3, 1] if has_volume else [1]

        fig, axes = plt.subplots(
            nrows=nrows,
            ncols=1,
            figsize=figsize,
            gridspec_kw={"height_ratios": height_ratios},
            sharex=True,
        )

        ax_main = axes[0] if has_volume else axes
        ax_vol = axes[1] if has_volume else None

        # Parse dates
        date_objs = [_parse_date(d) for d in dates]

        # Draw candlesticks
        _draw_candlesticks(
            ax_main, date_objs, opens, highs, lows, closes, elder_colors
        )

        # Draw MA lines if provided
        if ma_lines:
            _draw_ma_lines(ax_main, date_objs, ma_lines)

        # Draw volume bars if provided
        if ax_vol and volumes:
            _draw_volume_bars(ax_vol, date_objs, volumes, closes)

        # Formatting
        ax_main.set_title(_sanitize_text(title), fontsize=14, fontweight="bold")
        ax_main.set_ylabel("Price", fontsize=10)
        ax_main.grid(True, alpha=0.3)
        if ma_lines:
            ax_main.legend(loc="upper left")

        if ax_vol:
            ax_vol.set_ylabel("Volume", fontsize=10)
            ax_vol.grid(True, alpha=0.3)

        # Format x-axis
        _format_xaxis(ax_vol or ax_main, date_objs)

        plt.tight_layout()

        # Save to bytes
        buf = io.BytesIO()
        plt.savefig(buf, format="png", dpi=100, bbox_inches="tight")
        buf.seek(0)
        image_bytes = buf.getvalue()
        buf.close()

        # Save to file if path provided
        saved_path = None
        if save_path:
            plt.savefig(save_path, format="png", dpi=100, bbox_inches="tight")
            saved_path = save_path

        plt.close(fig)

        log_info("chart.candle", "plot complete", {"title": title, "points": len(dates)})

        return {
            "ok": True,
            "data": {
                "image_bytes": image_bytes,
                "save_path": saved_path,
            },
        }

    except Exception as e:
        plt.close("all")
        return {
            "ok": False,
            "error": {"code": "CHART_ERROR", "msg": f"차트 생성 실패: {str(e)}"},
        }


def plot_from_ohlcv(
    ohlcv_data: Dict,
    title: str = "",
    ma_lines: Optional[Dict[str, List[Optional[int]]]] = None,
    elder_colors: Optional[List[str]] = None,
    figsize: tuple = (12, 8),
    save_path: Optional[str] = None,
) -> Dict:
    """
    Create candlestick chart from OHLCV result dictionary.

    Args:
        ohlcv_data: OHLCV data from stock.ohlcv.get_daily()
        title: Chart title (auto-generated if not provided)
        ma_lines: Moving average lines
        elder_colors: Elder Impulse colors
        figsize: Figure size
        save_path: Path to save image

    Returns:
        Same format as plot()
    """
    if not ohlcv_data or "ticker" not in ohlcv_data:
        return {
            "ok": False,
            "error": {"code": "INVALID_ARG", "msg": "유효한 OHLCV 데이터가 필요합니다"},
        }

    if not title:
        title = f"{ohlcv_data['ticker']} Candlestick Chart"

    return plot(
        dates=ohlcv_data.get("dates", []),
        opens=ohlcv_data.get("open", []),
        highs=ohlcv_data.get("high", []),
        lows=ohlcv_data.get("low", []),
        closes=ohlcv_data.get("close", []),
        volumes=ohlcv_data.get("volume"),
        title=title,
        ma_lines=ma_lines,
        elder_colors=elder_colors,
        figsize=figsize,
        save_path=save_path,
    )


def _parse_date(date_str: str) -> datetime:
    """Parse date string (YYYYMMDD) to datetime."""
    try:
        return datetime.strptime(date_str, "%Y%m%d")
    except ValueError:
        return datetime.now()


def _draw_candlesticks(
    ax,
    dates: List[datetime],
    opens: List[int],
    highs: List[int],
    lows: List[int],
    closes: List[int],
    elder_colors: Optional[List[str]] = None,
):
    """Draw candlestick bars."""
    width = 0.6
    width2 = 0.1

    for i, (dt, o, h, l, c) in enumerate(zip(dates, opens, highs, lows, closes)):
        # Determine candle color
        if elder_colors and i < len(elder_colors):
            color = _elder_to_color(elder_colors[i])
        else:
            color = "green" if c >= o else "red"

        edge_color = color

        # Draw the body
        bottom = min(o, c)
        height = abs(c - o) or 1  # Avoid zero height

        ax.bar(
            i,
            height,
            width,
            bottom=bottom,
            color=color,
            edgecolor=edge_color,
            linewidth=1,
        )

        # Draw the wicks
        ax.vlines(i, l, h, color=edge_color, linewidth=1)


def _elder_to_color(elder: str) -> str:
    """Convert Elder Impulse color name to matplotlib color."""
    colors = {
        "green": "#26A69A",  # Teal green
        "red": "#EF5350",  # Material red
        "blue": "#42A5F5",  # Material blue
    }
    return colors.get(elder, "#42A5F5")


def _draw_ma_lines(
    ax, dates: List[datetime], ma_lines: Dict[str, List[Optional[int]]]
):
    """Draw moving average lines."""
    colors = {
        "MA5": "#FF9800",  # Orange
        "MA20": "#2196F3",  # Blue
        "MA60": "#9C27B0",  # Purple
        "MA120": "#795548",  # Brown
    }

    for name, values in ma_lines.items():
        if not values:
            continue

        x_vals = []
        y_vals = []
        for i, v in enumerate(values):
            if v is not None:
                x_vals.append(i)
                y_vals.append(v)

        color = colors.get(name, "#607D8B")
        ax.plot(x_vals, y_vals, label=name, color=color, linewidth=1.5, alpha=0.8)


def _draw_volume_bars(ax, dates: List[datetime], volumes: List[int], closes: List[int]):
    """Draw volume bars."""
    colors = []
    for i in range(len(closes)):
        if i == len(closes) - 1:
            colors.append("#26A69A")  # First/last bar green
        elif closes[i] >= closes[i + 1]:
            colors.append("#26A69A")  # Green for up
        else:
            colors.append("#EF5350")  # Red for down

    ax.bar(range(len(volumes)), volumes, color=colors, alpha=0.7, width=0.8)


def _format_xaxis(ax, dates: List[datetime]):
    """Format x-axis with date labels."""
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
