"""Oscillator chart for supply/demand MACD visualization."""

import io
from datetime import datetime
from typing import Any, Dict, List, Optional

import matplotlib.pyplot as plt

from ..core.log import log_info
from .utils import format_xaxis, parse_date, sanitize_text

# Configure matplotlib for non-GUI backend
plt.switch_backend("Agg")


def _format_mcap_label(mcap_value: float) -> str:
    """Format market cap Y-axis label with appropriate unit."""
    if mcap_value >= 1000:
        return "Market Cap (1000조)"
    elif mcap_value >= 100:
        return "Market Cap (조)"
    elif mcap_value >= 1:
        return "Market Cap (조)"
    else:
        return "Market Cap (억원)"


def plot(
    osc_data: Dict,
    title: str = "",
    figsize: tuple = (14, 12),
    save_path: Optional[str] = None,
) -> Dict:
    """
    Create oscillator chart with 4 panels (spec-compliant).

    Panel 1: Market Cap + Oscillator (Dual-Axis)
    Panel 2: Foreign/Institution 5D Net Buy
    Panel 3: MACD and Signal line
    Panel 4: Oscillator histogram

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
            nrows=4,
            ncols=1,
            figsize=figsize,
            sharex=False,  # Don't share x-axis to allow individual date labels
            gridspec_kw={"height_ratios": [2.5, 1.5, 2, 1.5]},
        )

        # Reverse data for chart display (oldest first on left, newest on right)
        dates_display = list(reversed(dates))
        date_objs = [parse_date(d) for d in dates_display]
        x = range(len(dates_display))

        # Panel 1: Market Cap + Oscillator (Dual-Axis per spec)
        ax_mcap = axes[0]
        market_cap = list(reversed(osc_data.get("market_cap", [])))
        oscillator = list(reversed(osc_data.get("oscillator", [])))

        # Calculate market cap range for proper scaling
        mcap_min, mcap_max = (min(market_cap), max(market_cap)) if market_cap else (0, 0)
        mcap_range = mcap_max - mcap_min
        if mcap_range > 0:
            padding = mcap_range * 0.1
            y_min = mcap_min - padding
            y_max = mcap_max + padding
        else:
            # If all values are the same, add 1% padding
            padding = mcap_max * 0.01 if mcap_max > 0 else 1
            y_min = mcap_min - padding
            y_max = mcap_max + padding

        # Left axis: Market Cap - fill from y_min instead of 0 to show variation
        ax_mcap.fill_between(x, y_min, market_cap, color="#1976D2", alpha=0.2)
        line1 = ax_mcap.plot(x, market_cap, color="#1976D2", linewidth=2, label="Market Cap")

        chart_title = title or f"{osc_data['ticker']} {osc_data.get('name', '')} Supply Oscillator"
        ax_mcap.set_title(sanitize_text(chart_title), fontsize=14, fontweight="bold")

        # Format Y-axis label with appropriate unit
        mcap_label = _format_mcap_label(mcap_max)
        ax_mcap.set_ylabel(mcap_label, fontsize=10, color="#1976D2")
        ax_mcap.tick_params(axis="y", labelcolor="#1976D2")
        ax_mcap.grid(True, alpha=0.3)

        # Set y-axis limits and disable scientific notation
        ax_mcap.set_ylim(y_min, y_max)
        ax_mcap.ticklabel_format(useOffset=False, style='plain', axis='y')

        # Right axis: Oscillator (%)
        ax_osc_right = ax_mcap.twinx()
        osc_pct = [v * 100 for v in oscillator]  # Convert to percentage

        # Ensure oscillator line is visible with appropriate scale
        line2 = ax_osc_right.plot(x, osc_pct, color="#FF5722", linewidth=2, label="Oscillator (%)")
        ax_osc_right.axhline(y=0, color="gray", linestyle="--", alpha=0.7)
        ax_osc_right.set_ylabel("Oscillator (%)", fontsize=10, color="#FF5722")
        ax_osc_right.tick_params(axis="y", labelcolor="#FF5722")

        # Auto-scale right axis based on data range
        if osc_pct:
            osc_min, osc_max = min(osc_pct), max(osc_pct)
            osc_range = osc_max - osc_min
            if osc_range > 0:
                padding = osc_range * 0.1
                ax_osc_right.set_ylim(osc_min - padding, osc_max + padding)

        # Combined legend
        lines = line1 + line2
        labels = [l.get_label() for l in lines]
        ax_mcap.legend(lines, labels, loc="upper left")

        # Format x-axis for Panel 1
        format_xaxis(ax_mcap, date_objs)

        # Panel 2: Foreign/Institution 5D Net Buy
        ax_supply = axes[1]
        foreign_5d = list(reversed(osc_data.get("foreign_5d", [])))
        institution_5d = list(reversed(osc_data.get("institution_5d", [])))

        if foreign_5d and institution_5d:
            width = 0.35
            x_for = [i - width / 2 for i in x]
            x_ins = [i + width / 2 for i in x]

            colors_for = ["#4CAF50" if v >= 0 else "#A5D6A7" for v in foreign_5d]
            colors_ins = ["#2196F3" if v >= 0 else "#90CAF9" for v in institution_5d]

            ax_supply.bar(x_for, foreign_5d, width, color=colors_for, alpha=0.8, label="Foreign 5D")
            ax_supply.bar(x_ins, institution_5d, width, color=colors_ins, alpha=0.8, label="Inst. 5D")
            ax_supply.axhline(y=0, color="gray", linestyle="-", alpha=0.5)
            ax_supply.set_ylabel("Net Buy (Billion KRW)", fontsize=10)
            ax_supply.grid(True, alpha=0.3, axis="y")
            ax_supply.legend(loc="upper left", fontsize=8)
            format_xaxis(ax_supply, date_objs)

        # Panel 3: MACD and Signal
        ax_macd = axes[2]
        macd = list(reversed(osc_data.get("macd", [])))
        signal = list(reversed(osc_data.get("signal", [])))

        # Convert to percentage for display
        macd_pct = [v * 100 for v in macd]
        signal_pct = [v * 100 for v in signal]

        ax_macd.plot(x, macd_pct, color="#2196F3", linewidth=1.5, label="MACD")
        ax_macd.plot(x, signal_pct, color="#FF9800", linewidth=1.5, linestyle="--", label="Signal")
        ax_macd.axhline(y=0, color="gray", linestyle="-", alpha=0.5)
        ax_macd.set_ylabel("MACD (%)", fontsize=10)
        ax_macd.grid(True, alpha=0.3)
        ax_macd.legend(loc="upper left", fontsize=8)

        # Highlight cross points
        for i in range(1, len(macd)):
            if macd[i] > signal[i] and macd[i - 1] <= signal[i - 1]:
                # Golden Cross
                ax_macd.scatter([i], [macd_pct[i]], color="#4CAF50", s=80, zorder=5, marker="^")
                ax_macd.annotate("GC", (i, macd_pct[i]), textcoords="offset points",
                               xytext=(0, 10), ha="center", fontsize=8, color="#4CAF50")
            elif macd[i] < signal[i] and macd[i - 1] >= signal[i - 1]:
                # Dead Cross
                ax_macd.scatter([i], [macd_pct[i]], color="#F44336", s=80, zorder=5, marker="v")
                ax_macd.annotate("DC", (i, macd_pct[i]), textcoords="offset points",
                               xytext=(0, -15), ha="center", fontsize=8, color="#F44336")

        format_xaxis(ax_macd, date_objs)

        # Panel 4: Oscillator Histogram
        ax_hist = axes[3]

        colors = ["#26A69A" if v >= 0 else "#EF5350" for v in osc_pct]
        ax_hist.bar(x, osc_pct, color=colors, alpha=0.8, width=0.8)
        ax_hist.axhline(y=0, color="gray", linestyle="-", alpha=0.5)
        ax_hist.set_ylabel("Histogram (%)", fontsize=10)
        ax_hist.grid(True, alpha=0.3, axis="y")

        # Format x-axis
        format_xaxis(ax_hist, date_objs)

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

    Panel 1: Market Cap + Oscillator (Dual-Axis)
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
            sharex=False,  # Don't share x-axis to allow individual date labels
            gridspec_kw={"height_ratios": [2, 2, 1.5, 0.8]},
        )

        # Reverse data for chart display (oldest first on left, newest on right)
        dates_display = list(reversed(dates))
        date_objs = [parse_date(d) for d in dates_display]
        x = range(len(dates_display))

        # Panel 1: Market Cap + Oscillator (Dual-Axis per spec)
        ax_mcap = axes[0]
        market_cap = list(reversed(osc_data.get("market_cap", [])))
        oscillator = list(reversed(osc_data.get("oscillator", [])))

        # Calculate market cap range for proper scaling
        mcap_min, mcap_max = (min(market_cap), max(market_cap)) if market_cap else (0, 0)
        mcap_range = mcap_max - mcap_min
        if mcap_range > 0:
            padding = mcap_range * 0.1
            y_min = mcap_min - padding
            y_max = mcap_max + padding
        else:
            # If all values are the same, add 1% padding
            padding = mcap_max * 0.01 if mcap_max > 0 else 1
            y_min = mcap_min - padding
            y_max = mcap_max + padding

        # Left axis: Market Cap - fill from y_min instead of 0 to show variation
        ax_mcap.fill_between(x, y_min, market_cap, color="#1976D2", alpha=0.2)
        line1 = ax_mcap.plot(x, market_cap, color="#1976D2", linewidth=2, label="Market Cap")

        chart_title = title or f"{osc_data['ticker']} {osc_data.get('name', '')} Supply Oscillator"
        ax_mcap.set_title(sanitize_text(chart_title), fontsize=14, fontweight="bold")

        # Format Y-axis label with appropriate unit
        mcap_label = _format_mcap_label(mcap_max)
        ax_mcap.set_ylabel(mcap_label, fontsize=10, color="#1976D2")
        ax_mcap.tick_params(axis="y", labelcolor="#1976D2")
        ax_mcap.grid(True, alpha=0.3)

        # Set y-axis limits and disable scientific notation
        ax_mcap.set_ylim(y_min, y_max)
        ax_mcap.ticklabel_format(useOffset=False, style='plain', axis='y')

        # Right axis: Oscillator (%)
        ax_osc_right = ax_mcap.twinx()
        osc_pct = [v * 100 for v in oscillator]  # Convert to percentage

        line2 = ax_osc_right.plot(x, osc_pct, color="#FF5722", linewidth=2, label="Oscillator (%)")
        ax_osc_right.axhline(y=0, color="gray", linestyle="--", alpha=0.7)
        ax_osc_right.set_ylabel("Oscillator (%)", fontsize=10, color="#FF5722")
        ax_osc_right.tick_params(axis="y", labelcolor="#FF5722")

        # Auto-scale right axis based on data range
        if osc_pct:
            osc_min, osc_max = min(osc_pct), max(osc_pct)
            osc_range = osc_max - osc_min
            if osc_range > 0:
                padding = osc_range * 0.1
                ax_osc_right.set_ylim(osc_min - padding, osc_max + padding)

        # Combined legend
        lines = line1 + line2
        labels = [l.get_label() for l in lines]
        ax_mcap.legend(lines, labels, loc="upper left")

        format_xaxis(ax_mcap, date_objs)

        # Panel 2: MACD and Signal
        ax_macd = axes[1]
        macd = list(reversed(osc_data.get("macd", [])))
        signal_line = list(reversed(osc_data.get("signal", [])))

        # Convert to percentage for display
        macd_pct = [v * 100 for v in macd]
        signal_pct = [v * 100 for v in signal_line]

        ax_macd.plot(x, macd_pct, color="#2196F3", linewidth=1.5, label="MACD")
        ax_macd.plot(x, signal_pct, color="#FF9800", linewidth=1.5, linestyle="--", label="Signal")
        ax_macd.axhline(y=0, color="gray", linestyle="-", alpha=0.5)
        ax_macd.set_ylabel("MACD (%)", fontsize=10)
        ax_macd.grid(True, alpha=0.3)
        ax_macd.legend(loc="upper left", fontsize=8)

        # Highlight cross points
        for i in range(1, len(macd)):
            if macd[i] > signal_line[i] and macd[i - 1] <= signal_line[i - 1]:
                # Golden Cross
                ax_macd.scatter([i], [macd_pct[i]], color="#4CAF50", s=80, zorder=5, marker="^")
                ax_macd.annotate("GC", (i, macd_pct[i]), textcoords="offset points",
                               xytext=(0, 10), ha="center", fontsize=8, color="#4CAF50")
            elif macd[i] < signal_line[i] and macd[i - 1] >= signal_line[i - 1]:
                # Dead Cross
                ax_macd.scatter([i], [macd_pct[i]], color="#F44336", s=80, zorder=5, marker="v")
                ax_macd.annotate("DC", (i, macd_pct[i]), textcoords="offset points",
                               xytext=(0, -15), ha="center", fontsize=8, color="#F44336")

        format_xaxis(ax_macd, date_objs)

        # Panel 3: Oscillator Histogram
        ax_osc = axes[2]

        colors = ["#26A69A" if v >= 0 else "#EF5350" for v in osc_pct]
        ax_osc.bar(x, osc_pct, color=colors, alpha=0.8, width=0.8)
        ax_osc.axhline(y=0, color="gray", linestyle="-", alpha=0.5)
        ax_osc.set_ylabel("Histogram (%)", fontsize=10)
        ax_osc.grid(True, alpha=0.3, axis="y")
        format_xaxis(ax_osc, date_objs)

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
            f"{sanitize_text(signal_type)} (Score: {total_score})",
            ha="center", va="center",
            fontsize=14, fontweight="bold", color=text_color,
            transform=ax_signal.transAxes
        )
        ax_signal.text(
            0.5, 0.25,
            sanitize_text(description),
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
