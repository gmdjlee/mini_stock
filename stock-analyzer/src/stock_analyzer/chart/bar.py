"""Bar chart for volume and supply/demand data."""

import io
from datetime import datetime
from typing import Any, Dict, List, Optional, Union

import matplotlib.pyplot as plt

from ..core.log import log_info
from .utils import format_xaxis, parse_date, sanitize_text

# Configure matplotlib for non-GUI backend
plt.switch_backend("Agg")


def plot(
    dates: List[str],
    values: List[Union[int, float]],
    title: str = "",
    ylabel: str = "Value",
    color: Optional[str] = None,
    color_by_sign: bool = False,
    positive_color: str = "#26A69A",
    negative_color: str = "#EF5350",
    figsize: tuple = (12, 6),
    hlines: Optional[List[Dict[str, Any]]] = None,
    save_path: Optional[str] = None,
) -> Dict:
    """
    Create bar chart.

    Args:
        dates: Date list (YYYYMMDD format)
        values: Value list
        title: Chart title
        ylabel: Y-axis label
        color: Bar color (ignored if color_by_sign is True)
        color_by_sign: Color bars by positive/negative value
        positive_color: Color for positive values
        negative_color: Color for negative values
        figsize: Figure size (width, height)
        hlines: Horizontal lines [{"y": 0, "color": "gray"}]
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
    if not dates or not values:
        return {
            "ok": False,
            "error": {"code": "INVALID_ARG", "msg": "날짜와 데이터가 필요합니다"},
        }

    if len(dates) < 2:
        return {
            "ok": False,
            "error": {"code": "NO_DATA", "msg": "최소 2일 이상의 데이터가 필요합니다"},
        }

    try:
        fig, ax = plt.subplots(figsize=figsize)

        # Reverse data for chart display (oldest first on left, newest on right)
        dates_display = list(reversed(dates))
        values_display = list(reversed(values))

        # Parse dates
        date_objs = [parse_date(d) for d in dates_display]

        # Determine colors
        if color_by_sign:
            colors = [
                positive_color if v >= 0 else negative_color for v in values_display
            ]
        else:
            colors = color or "#2196F3"

        # Draw bars
        ax.bar(range(len(values_display)), values_display, color=colors, alpha=0.8, width=0.8)

        # Draw horizontal lines
        if hlines:
            for hline in hlines:
                y = hline.get("y", 0)
                line_color = hline.get("color", "gray")
                linestyle = hline.get("linestyle", "-")
                ax.axhline(y=y, color=line_color, linestyle=linestyle, alpha=0.5)

        # Formatting
        ax.set_title(sanitize_text(title), fontsize=14, fontweight="bold")
        ax.set_ylabel(sanitize_text(ylabel), fontsize=10)
        ax.grid(True, alpha=0.3, axis="y")

        # Format x-axis
        format_xaxis(ax, date_objs)

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

        log_info("chart.bar", "plot complete", {"title": title, "points": len(values_display)})

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


def plot_multi(
    dates: List[str],
    series: Dict[str, List[Union[int, float]]],
    title: str = "",
    ylabel: str = "Value",
    colors: Optional[Dict[str, str]] = None,
    stacked: bool = False,
    figsize: tuple = (12, 6),
    save_path: Optional[str] = None,
) -> Dict:
    """
    Create grouped or stacked bar chart.

    Args:
        dates: Date list (YYYYMMDD format)
        series: Multiple data series {"Foreign": [...], "Institution": [...]}
        title: Chart title
        ylabel: Y-axis label
        colors: Custom colors {"Foreign": "#4CAF50"}
        stacked: Stack bars if True, group if False
        figsize: Figure size
        save_path: Path to save image

    Returns:
        Same format as plot()
    """
    if not dates or not series:
        return {
            "ok": False,
            "error": {"code": "INVALID_ARG", "msg": "날짜와 데이터 시리즈가 필요합니다"},
        }

    try:
        fig, ax = plt.subplots(figsize=figsize)

        # Reverse data for chart display (oldest first on left, newest on right)
        dates_display = list(reversed(dates))
        series_display = {name: list(reversed(values)) for name, values in series.items()}

        date_objs = [parse_date(d) for d in dates_display]

        # Default colors
        default_colors = {
            "foreign": "#4CAF50",
            "institution": "#2196F3",
            "personal": "#FF9800",
            "for_5d": "#4CAF50",
            "ins_5d": "#2196F3",
        }

        if colors:
            default_colors.update(colors)

        n_series = len(series_display)
        n_dates = len(dates_display)
        width = 0.8 / n_series if not stacked else 0.8

        if stacked:
            # Stacked bar chart
            bottom_pos = [0.0] * n_dates
            bottom_neg = [0.0] * n_dates

            for name, values in series_display.items():
                color = default_colors.get(name.lower(), "#607D8B")

                # Separate positive and negative for proper stacking
                pos_vals = [v if v >= 0 else 0 for v in values]
                neg_vals = [v if v < 0 else 0 for v in values]

                ax.bar(
                    range(n_dates),
                    pos_vals,
                    width,
                    bottom=bottom_pos,
                    color=color,
                    alpha=0.8,
                    label=name,
                )
                ax.bar(
                    range(n_dates),
                    neg_vals,
                    width,
                    bottom=bottom_neg,
                    color=color,
                    alpha=0.8,
                )

                bottom_pos = [b + v for b, v in zip(bottom_pos, pos_vals)]
                bottom_neg = [b + v for b, v in zip(bottom_neg, neg_vals)]
        else:
            # Grouped bar chart
            for i, (name, values) in enumerate(series_display.items()):
                color = default_colors.get(name.lower(), "#607D8B")
                offset = (i - n_series / 2 + 0.5) * width
                x = [j + offset for j in range(n_dates)]

                ax.bar(x, values, width, color=color, alpha=0.8, label=name)

        # Zero line
        ax.axhline(y=0, color="gray", linestyle="-", alpha=0.5)

        # Formatting
        ax.set_title(sanitize_text(title), fontsize=14, fontweight="bold")
        ax.set_ylabel(sanitize_text(ylabel), fontsize=10)
        ax.grid(True, alpha=0.3, axis="y")
        ax.legend(loc="upper left")

        # Format x-axis
        format_xaxis(ax, date_objs)

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

        log_info("chart.bar", "plot_multi complete", {"title": title, "series": len(series_display)})

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


def plot_supply_demand(
    analysis_data: Dict,
    title: str = "",
    figsize: tuple = (12, 8),
    save_path: Optional[str] = None,
) -> Dict:
    """
    Create supply/demand analysis chart.

    Args:
        analysis_data: Analysis data from stock.analysis.analyze()
        title: Chart title
        figsize: Figure size
        save_path: Path to save image

    Returns:
        Same format as plot()
    """
    if not analysis_data or "ticker" not in analysis_data:
        return {
            "ok": False,
            "error": {"code": "INVALID_ARG", "msg": "유효한 수급 분석 데이터가 필요합니다"},
        }

    dates = analysis_data.get("dates", [])
    if len(dates) < 2:
        return {
            "ok": False,
            "error": {"code": "NO_DATA", "msg": "충분한 데이터가 없습니다"},
        }

    try:
        fig, axes = plt.subplots(
            nrows=2,
            ncols=1,
            figsize=figsize,
            sharex=True,
        )

        # Reverse data for chart display (oldest first on left, newest on right)
        dates_display = list(reversed(dates))
        date_objs = [parse_date(d) for d in dates_display]

        # Panel 1: Market Cap
        ax_mcap = axes[0]
        mcap = list(reversed(analysis_data.get("mcap", [])))
        mcap_trillion = [m / 1e12 for m in mcap]  # Convert to trillion

        ax_mcap.fill_between(
            range(len(mcap_trillion)),
            mcap_trillion,
            color="#1976D2",
            alpha=0.3,
        )
        ax_mcap.plot(
            range(len(mcap_trillion)),
            mcap_trillion,
            color="#1976D2",
            linewidth=1.5,
        )

        chart_title = title or f"{analysis_data['ticker']} {analysis_data.get('name', '')} Supply/Demand"
        ax_mcap.set_title(
            sanitize_text(chart_title),
            fontsize=14,
            fontweight="bold",
        )
        ax_mcap.set_ylabel("Market Cap (Trillion KRW)", fontsize=10)
        ax_mcap.grid(True, alpha=0.3)

        # Panel 2: Foreign/Institution 5D Net
        ax_flow = axes[1]
        for_5d = list(reversed(analysis_data.get("for_5d", [])))
        ins_5d = list(reversed(analysis_data.get("ins_5d", [])))

        # Convert to billion
        for_5d_b = [v / 1e9 for v in for_5d]
        ins_5d_b = [v / 1e9 for v in ins_5d]

        width = 0.35
        x = range(len(dates_display))
        x_for = [i - width / 2 for i in x]
        x_ins = [i + width / 2 for i in x]

        colors_for = ["#4CAF50" if v >= 0 else "#C8E6C9" for v in for_5d_b]
        colors_ins = ["#2196F3" if v >= 0 else "#BBDEFB" for v in ins_5d_b]

        ax_flow.bar(x_for, for_5d_b, width, color=colors_for, alpha=0.8, label="Foreign 5D")
        ax_flow.bar(x_ins, ins_5d_b, width, color=colors_ins, alpha=0.8, label="Institution 5D")

        ax_flow.axhline(y=0, color="gray", linestyle="-", alpha=0.5)
        ax_flow.set_ylabel("Net Buy (Billion KRW)", fontsize=10)
        ax_flow.grid(True, alpha=0.3, axis="y")
        ax_flow.legend(loc="upper left")

        # Format x-axis
        format_xaxis(ax_flow, date_objs)

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

        log_info("chart.bar", "plot_supply_demand complete", {"ticker": analysis_data["ticker"]})

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


def plot_demark(
    demark_data: Dict,
    title: str = "",
    figsize: tuple = (12, 6),
    save_path: Optional[str] = None,
) -> Dict:
    """
    Create DeMark TD Setup chart (EtfMonitor reference).

    Shows independent Sell Setup and Buy Setup counts as line charts.
    - Sell Setup: Red line (4일 전 비교, 상승 피로)
    - Buy Setup: Blue line (4일 전 비교, 하락 피로)

    Args:
        demark_data: DeMark data from indicator.demark.calc()
        title: Chart title
        figsize: Figure size
        save_path: Path to save image

    Returns:
        Same format as plot()
    """
    if not demark_data or "ticker" not in demark_data:
        return {
            "ok": False,
            "error": {"code": "INVALID_ARG", "msg": "유효한 DeMark 데이터가 필요합니다"},
        }

    dates = demark_data.get("dates", [])
    if len(dates) < 2:
        return {
            "ok": False,
            "error": {"code": "NO_DATA", "msg": "충분한 데이터가 없습니다"},
        }

    try:
        fig, ax = plt.subplots(figsize=figsize)

        # Reverse data for chart display (oldest first on left, newest on right)
        dates_display = list(reversed(dates))
        date_objs = [parse_date(d) for d in dates_display]

        sell_setup = list(reversed(demark_data.get("sell_setup", [])))
        buy_setup = list(reversed(demark_data.get("buy_setup", [])))

        # Plot as line charts (reference style)
        ax.plot(
            range(len(sell_setup)),
            sell_setup,
            color="red",
            linewidth=1.5,
            label="TD Sell Setup (4일 기준)",
        )
        ax.plot(
            range(len(buy_setup)),
            buy_setup,
            color="blue",
            linewidth=1.5,
            label="TD Buy Setup (4일 기준)",
        )

        # Dynamic y-axis limit
        max_count = max(max(sell_setup) if sell_setup else 0, max(buy_setup) if buy_setup else 0)
        ax.set_ylim(0, max_count + 2)

        # Formatting
        demark_title = title or f"{demark_data['ticker']} DeMark TD Setup Counts"
        ax.set_title(
            sanitize_text(demark_title),
            fontsize=14,
            fontweight="bold",
        )
        ax.set_ylabel("TD Setup Count", fontsize=10)
        ax.grid(True, alpha=0.3)
        ax.legend(loc="upper right")

        # Format x-axis
        format_xaxis(ax, date_objs)

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

        log_info("chart.bar", "plot_demark complete", {"ticker": demark_data["ticker"]})

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
