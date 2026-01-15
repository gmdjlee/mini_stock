"""Oscillator chart for supply/demand MACD visualization."""

import io
from datetime import datetime
from typing import Any, Dict, List, Optional

import matplotlib.pyplot as plt
import matplotlib.font_manager as fm

from ..core.log import log_info

# Configure matplotlib for non-GUI backend
plt.switch_backend("Agg")


def _configure_korean_font():
    """Configure matplotlib to use a font that supports Korean characters."""
    korean_fonts = [
        "Malgun Gothic",
        "맑은 고딕",
        "NanumGothic",
        "NanumBarunGothic",
        "AppleGothic",
        "Apple SD Gothic Neo",
        "Noto Sans CJK KR",
    ]

    available_fonts = {f.name for f in fm.fontManager.ttflist}

    for font in korean_fonts:
        if font in available_fonts:
            plt.rcParams["font.family"] = font
            plt.rcParams["axes.unicode_minus"] = False
            return font

    return None


_configured_font = _configure_korean_font()


def _sanitize_text(text: str) -> str:
    """Remove Korean characters if no Korean font is available."""
    if _configured_font is not None:
        return text
    return "".join(c for c in text if not ("\uac00" <= c <= "\ud7a3")).strip()


def plot(
    osc_data: Dict,
    title: str = "",
    figsize: tuple = (14, 10),
    save_path: Optional[str] = None,
) -> Dict:
    """
    Create oscillator chart with 3 panels.

    Panel 1: Market Cap trend
    Panel 2: MACD and Signal line
    Panel 3: Oscillator histogram

    Args:
        osc_data: Oscillator data from indicator.oscillator.calc()
        title: Chart title
        figsize: Figure size
        save_path: Path to save image

    Returns:
        {
            "ok": True,
            "data": {
                "image_bytes": bytes,
                "save_path": str or None
            }
        }

    Errors:
        - INVALID_ARG: Invalid argument
        - NO_DATA: Insufficient data
        - CHART_ERROR: Chart generation failed
    """
    if not osc_data or "ticker" not in osc_data:
        return {
            "ok": False,
            "error": {"code": "INVALID_ARG", "msg": "유효한 오실레이터 데이터가 필요합니다"},
        }

    dates = osc_data.get("dates", [])
    if len(dates) < 2:
        return {
            "ok": False,
            "error": {"code": "NO_DATA", "msg": "충분한 데이터가 없습니다"},
        }

    try:
        fig, axes = plt.subplots(
            nrows=3,
            ncols=1,
            figsize=figsize,
            sharex=True,
            gridspec_kw={"height_ratios": [2, 2, 1.5]},
        )

        date_objs = [_parse_date(d) for d in dates]
        x = range(len(dates))

        # Panel 1: Market Cap
        ax_mcap = axes[0]
        market_cap = osc_data.get("market_cap", [])

        ax_mcap.fill_between(x, market_cap, color="#1976D2", alpha=0.3)
        ax_mcap.plot(x, market_cap, color="#1976D2", linewidth=1.5)

        chart_title = title or f"{osc_data['ticker']} {osc_data.get('name', '')} Supply Oscillator"
        ax_mcap.set_title(_sanitize_text(chart_title), fontsize=14, fontweight="bold")
        ax_mcap.set_ylabel("Market Cap (Trillion KRW)", fontsize=10)
        ax_mcap.grid(True, alpha=0.3)

        # Panel 2: MACD and Signal
        ax_macd = axes[1]
        macd = osc_data.get("macd", [])
        signal = osc_data.get("signal", [])

        ax_macd.plot(x, macd, color="#2196F3", linewidth=1.5, label="MACD")
        ax_macd.plot(x, signal, color="#FF9800", linewidth=1.5, label="Signal")
        ax_macd.axhline(y=0, color="gray", linestyle="-", alpha=0.5)
        ax_macd.set_ylabel("MACD", fontsize=10)
        ax_macd.grid(True, alpha=0.3)
        ax_macd.legend(loc="upper left")

        # Highlight cross points
        for i in range(1, len(macd)):
            if macd[i] > signal[i] and macd[i - 1] <= signal[i - 1]:
                # Golden Cross
                ax_macd.scatter([i], [macd[i]], color="#4CAF50", s=50, zorder=5, marker="^")
            elif macd[i] < signal[i] and macd[i - 1] >= signal[i - 1]:
                # Dead Cross
                ax_macd.scatter([i], [macd[i]], color="#F44336", s=50, zorder=5, marker="v")

        # Panel 3: Oscillator Histogram
        ax_osc = axes[2]
        oscillator = osc_data.get("oscillator", [])

        colors = ["#26A69A" if v >= 0 else "#EF5350" for v in oscillator]
        ax_osc.bar(x, oscillator, color=colors, alpha=0.8, width=0.8)
        ax_osc.axhline(y=0, color="gray", linestyle="-", alpha=0.5)
        ax_osc.set_ylabel("Histogram", fontsize=10)
        ax_osc.grid(True, alpha=0.3, axis="y")

        # Format x-axis
        _format_xaxis(ax_osc, date_objs)

        plt.tight_layout()

        # Save to bytes
        buf = io.BytesIO()
        plt.savefig(buf, format="png", dpi=100, bbox_inches="tight")
        buf.seek(0)
        image_bytes = buf.getvalue()
        buf.close()

        saved_path = None
        if save_path:
            plt.savefig(save_path, format="png", dpi=100, bbox_inches="tight")
            saved_path = save_path

        plt.close(fig)

        log_info("chart.oscillator", "plot complete", {"ticker": osc_data["ticker"]})

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


def plot_with_signal(
    osc_data: Dict,
    signal_data: Dict,
    title: str = "",
    figsize: tuple = (14, 12),
    save_path: Optional[str] = None,
) -> Dict:
    """
    Create oscillator chart with signal analysis panel.

    Panel 1: Market Cap trend
    Panel 2: MACD and Signal line
    Panel 3: Oscillator histogram
    Panel 4: Signal analysis summary

    Args:
        osc_data: Oscillator data from indicator.oscillator.calc()
        signal_data: Signal data from indicator.oscillator.analyze_signal()
        title: Chart title
        figsize: Figure size
        save_path: Path to save image

    Returns:
        Same format as plot()
    """
    if not osc_data or "ticker" not in osc_data:
        return {
            "ok": False,
            "error": {"code": "INVALID_ARG", "msg": "유효한 오실레이터 데이터가 필요합니다"},
        }

    if not signal_data or not signal_data.get("ok"):
        # Fall back to basic plot if signal data is invalid
        return plot(osc_data, title, figsize, save_path)

    dates = osc_data.get("dates", [])
    if len(dates) < 2:
        return {
            "ok": False,
            "error": {"code": "NO_DATA", "msg": "충분한 데이터가 없습니다"},
        }

    try:
        fig, axes = plt.subplots(
            nrows=4,
            ncols=1,
            figsize=figsize,
            gridspec_kw={"height_ratios": [2, 2, 1.5, 0.8]},
        )

        date_objs = [_parse_date(d) for d in dates]
        x = range(len(dates))

        # Panel 1: Market Cap
        ax_mcap = axes[0]
        market_cap = osc_data.get("market_cap", [])

        ax_mcap.fill_between(x, market_cap, color="#1976D2", alpha=0.3)
        ax_mcap.plot(x, market_cap, color="#1976D2", linewidth=1.5)

        chart_title = title or f"{osc_data['ticker']} {osc_data.get('name', '')} Supply Oscillator"
        ax_mcap.set_title(_sanitize_text(chart_title), fontsize=14, fontweight="bold")
        ax_mcap.set_ylabel("Market Cap (Trillion KRW)", fontsize=10)
        ax_mcap.grid(True, alpha=0.3)

        # Panel 2: MACD and Signal
        ax_macd = axes[1]
        macd = osc_data.get("macd", [])
        signal = osc_data.get("signal", [])

        ax_macd.plot(x, macd, color="#2196F3", linewidth=1.5, label="MACD")
        ax_macd.plot(x, signal, color="#FF9800", linewidth=1.5, label="Signal")
        ax_macd.axhline(y=0, color="gray", linestyle="-", alpha=0.5)
        ax_macd.set_ylabel("MACD", fontsize=10)
        ax_macd.grid(True, alpha=0.3)
        ax_macd.legend(loc="upper left")

        # Highlight cross points
        for i in range(1, len(macd)):
            if macd[i] > signal[i] and macd[i - 1] <= signal[i - 1]:
                ax_macd.scatter([i], [macd[i]], color="#4CAF50", s=50, zorder=5, marker="^")
            elif macd[i] < signal[i] and macd[i - 1] >= signal[i - 1]:
                ax_macd.scatter([i], [macd[i]], color="#F44336", s=50, zorder=5, marker="v")

        # Panel 3: Oscillator Histogram
        ax_osc = axes[2]
        oscillator = osc_data.get("oscillator", [])

        colors = ["#26A69A" if v >= 0 else "#EF5350" for v in oscillator]
        ax_osc.bar(x, oscillator, color=colors, alpha=0.8, width=0.8)
        ax_osc.axhline(y=0, color="gray", linestyle="-", alpha=0.5)
        ax_osc.set_ylabel("Histogram", fontsize=10)
        ax_osc.grid(True, alpha=0.3, axis="y")
        _format_xaxis(ax_osc, date_objs)

        # Panel 4: Signal Summary
        ax_signal = axes[3]
        ax_signal.axis("off")

        sig = signal_data["data"]
        signal_type = sig["signal_type"]
        total_score = sig["total_score"]
        description = sig["description"]

        # Determine color based on signal type
        if signal_type in ["STRONG_BUY", "BUY"]:
            bg_color = "#E8F5E9"
            text_color = "#2E7D32"
        elif signal_type in ["STRONG_SELL", "SELL"]:
            bg_color = "#FFEBEE"
            text_color = "#C62828"
        else:
            bg_color = "#ECEFF1"
            text_color = "#546E7A"

        # Draw signal box
        ax_signal.add_patch(plt.Rectangle((0.05, 0.1), 0.9, 0.8, facecolor=bg_color, edgecolor="none"))
        ax_signal.text(
            0.5, 0.6,
            f"{_sanitize_text(signal_type)} (Score: {total_score})",
            ha="center", va="center",
            fontsize=14, fontweight="bold", color=text_color,
            transform=ax_signal.transAxes
        )
        ax_signal.text(
            0.5, 0.25,
            _sanitize_text(description),
            ha="center", va="center",
            fontsize=11, color=text_color,
            transform=ax_signal.transAxes
        )
        ax_signal.set_xlim(0, 1)
        ax_signal.set_ylim(0, 1)

        plt.tight_layout()

        # Save to bytes
        buf = io.BytesIO()
        plt.savefig(buf, format="png", dpi=100, bbox_inches="tight")
        buf.seek(0)
        image_bytes = buf.getvalue()
        buf.close()

        saved_path = None
        if save_path:
            plt.savefig(save_path, format="png", dpi=100, bbox_inches="tight")
            saved_path = save_path

        plt.close(fig)

        log_info("chart.oscillator", "plot_with_signal complete", {"ticker": osc_data["ticker"]})

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


def _parse_date(date_str: str) -> datetime:
    """Parse date string to datetime."""
    try:
        # Handle YYYY-MM-DD format
        if "-" in date_str:
            return datetime.strptime(date_str, "%Y-%m-%d")
        # Handle YYYYMMDD format
        return datetime.strptime(date_str, "%Y%m%d")
    except ValueError:
        return datetime.now()


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
