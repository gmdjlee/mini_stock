"""Line chart for indicators and price data."""

import io
from datetime import datetime
from typing import Any, Dict, List, Optional, Union

import matplotlib.pyplot as plt

from ..core.log import log_info

# Configure matplotlib for non-GUI backend
plt.switch_backend("Agg")


def plot(
    dates: List[str],
    series: Dict[str, List[Union[int, float, None]]],
    title: str = "",
    ylabel: str = "Value",
    colors: Optional[Dict[str, str]] = None,
    figsize: tuple = (12, 6),
    fill_between: Optional[Dict[str, tuple]] = None,
    hlines: Optional[List[Dict[str, Any]]] = None,
    save_path: Optional[str] = None,
) -> Dict:
    """
    Create line chart with multiple series.

    Args:
        dates: Date list (YYYYMMDD format)
        series: Data series {"MA5": [...], "MA20": [...]}
        title: Chart title
        ylabel: Y-axis label
        colors: Custom colors for series {"MA5": "#FF9800"}
        figsize: Figure size (width, height)
        fill_between: Fill area {"series_name": (lower_bound, upper_bound, color, alpha)}
        hlines: Horizontal lines [{"y": 50, "color": "gray", "linestyle": "--"}]
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
    if not dates or not series:
        return {
            "ok": False,
            "error": {"code": "INVALID_ARG", "msg": "날짜와 데이터 시리즈가 필요합니다"},
        }

    if len(dates) < 2:
        return {
            "ok": False,
            "error": {"code": "NO_DATA", "msg": "최소 2일 이상의 데이터가 필요합니다"},
        }

    try:
        fig, ax = plt.subplots(figsize=figsize)

        # Parse dates
        date_objs = [_parse_date(d) for d in dates]

        # Default colors
        default_colors = {
            "MA5": "#FF9800",
            "MA20": "#2196F3",
            "MA60": "#9C27B0",
            "MA120": "#795548",
            "close": "#1976D2",
            "price": "#1976D2",
            "cmf": "#00BCD4",
            "fear_greed": "#FF5722",
            "ema13": "#4CAF50",
            "macd_hist": "#9C27B0",
        }

        if colors:
            default_colors.update(colors)

        # Draw lines
        for name, values in series.items():
            if not values:
                continue

            x_vals = []
            y_vals = []
            for i, v in enumerate(values):
                if v is not None:
                    x_vals.append(i)
                    y_vals.append(v)

            color = default_colors.get(name.lower(), "#607D8B")
            ax.plot(x_vals, y_vals, label=name, color=color, linewidth=1.5)

        # Fill between if specified
        if fill_between:
            for name, params in fill_between.items():
                if len(params) >= 4:
                    lower, upper, color, alpha = params[:4]
                    ax.fill_between(
                        range(len(dates)),
                        [lower] * len(dates),
                        [upper] * len(dates),
                        color=color,
                        alpha=alpha,
                    )

        # Draw horizontal lines
        if hlines:
            for hline in hlines:
                y = hline.get("y", 0)
                color = hline.get("color", "gray")
                linestyle = hline.get("linestyle", "--")
                label = hline.get("label")
                ax.axhline(
                    y=y,
                    color=color,
                    linestyle=linestyle,
                    alpha=0.7,
                    label=label,
                )

        # Formatting
        ax.set_title(title, fontsize=14, fontweight="bold")
        ax.set_ylabel(ylabel, fontsize=10)
        ax.grid(True, alpha=0.3)
        ax.legend(loc="upper left")

        # Format x-axis
        _format_xaxis(ax, date_objs)

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

        log_info("chart.line", "plot complete", {"title": title, "series": len(series)})

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


def plot_trend(
    trend_data: Dict,
    title: str = "",
    figsize: tuple = (12, 10),
    save_path: Optional[str] = None,
) -> Dict:
    """
    Create multi-panel trend signal chart.

    Args:
        trend_data: Trend data from indicator.trend.calc()
        title: Chart title
        figsize: Figure size
        save_path: Path to save image

    Returns:
        Same format as plot()
    """
    if not trend_data or "ticker" not in trend_data:
        return {
            "ok": False,
            "error": {"code": "INVALID_ARG", "msg": "유효한 트렌드 데이터가 필요합니다"},
        }

    dates = trend_data.get("dates", [])
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
            gridspec_kw={"height_ratios": [2, 1, 1]},
            sharex=True,
        )

        date_objs = [_parse_date(d) for d in dates]

        # Panel 1: MA lines
        ax_ma = axes[0]
        ma_series = {
            "MA5": trend_data.get("ma5", []),
            "MA20": trend_data.get("ma20", []),
            "MA60": trend_data.get("ma60", []),
        }
        for name, values in ma_series.items():
            x_vals, y_vals = _filter_none(values)
            if x_vals:
                color = {"MA5": "#FF9800", "MA20": "#2196F3", "MA60": "#9C27B0"}[name]
                ax_ma.plot(x_vals, y_vals, label=name, color=color, linewidth=1.5)

        ax_ma.set_title(title or f"{trend_data['ticker']} Trend Signal", fontsize=14, fontweight="bold")
        ax_ma.set_ylabel("Price", fontsize=10)
        ax_ma.grid(True, alpha=0.3)
        ax_ma.legend(loc="upper left")

        # Panel 2: CMF
        ax_cmf = axes[1]
        cmf_values = trend_data.get("cmf", [])
        x_vals = list(range(len(cmf_values)))
        colors = ["#26A69A" if v >= 0 else "#EF5350" for v in cmf_values]
        ax_cmf.bar(x_vals, cmf_values, color=colors, alpha=0.7)
        ax_cmf.axhline(y=0, color="gray", linestyle="-", alpha=0.5)
        ax_cmf.axhline(y=0.05, color="#26A69A", linestyle="--", alpha=0.5)
        ax_cmf.axhline(y=-0.05, color="#EF5350", linestyle="--", alpha=0.5)
        ax_cmf.set_ylabel("CMF", fontsize=10)
        ax_cmf.set_ylim(-0.5, 0.5)
        ax_cmf.grid(True, alpha=0.3)

        # Panel 3: Fear/Greed Index
        ax_fg = axes[2]
        fg_values = trend_data.get("fear_greed", [])
        ax_fg.fill_between(range(len(fg_values)), 0, 40, color="#EF5350", alpha=0.2)
        ax_fg.fill_between(range(len(fg_values)), 40, 60, color="#9E9E9E", alpha=0.2)
        ax_fg.fill_between(range(len(fg_values)), 60, 100, color="#26A69A", alpha=0.2)
        ax_fg.plot(range(len(fg_values)), fg_values, color="#FF5722", linewidth=1.5)
        ax_fg.axhline(y=50, color="gray", linestyle="-", alpha=0.5)
        ax_fg.set_ylabel("Fear/Greed", fontsize=10)
        ax_fg.set_ylim(0, 100)
        ax_fg.grid(True, alpha=0.3)

        # Format x-axis
        _format_xaxis(ax_fg, date_objs)

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

        log_info("chart.line", "plot_trend complete", {"ticker": trend_data["ticker"]})

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


def plot_elder(
    elder_data: Dict,
    title: str = "",
    figsize: tuple = (12, 8),
    save_path: Optional[str] = None,
) -> Dict:
    """
    Create Elder Impulse chart.

    Args:
        elder_data: Elder data from indicator.elder.calc()
        title: Chart title
        figsize: Figure size
        save_path: Path to save image

    Returns:
        Same format as plot()
    """
    if not elder_data or "ticker" not in elder_data:
        return {
            "ok": False,
            "error": {"code": "INVALID_ARG", "msg": "유효한 Elder 데이터가 필요합니다"},
        }

    dates = elder_data.get("dates", [])
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
            gridspec_kw={"height_ratios": [2, 1]},
            sharex=True,
        )

        date_objs = [_parse_date(d) for d in dates]

        # Panel 1: EMA13 with color markers
        ax_ema = axes[0]
        ema13 = elder_data.get("ema13", [])
        colors = elder_data.get("color", [])

        x_vals, y_vals = _filter_none(ema13)
        if x_vals:
            ax_ema.plot(x_vals, y_vals, color="#607D8B", linewidth=1.5, label="EMA13")

        # Color markers based on Elder colors
        for i, (x, y) in enumerate(zip(x_vals, y_vals)):
            idx = x if x < len(colors) else 0
            color = _elder_to_color(colors[idx] if idx < len(colors) else "blue")
            ax_ema.scatter(x, y, color=color, s=20, alpha=0.8)

        ax_ema.set_title(title or f"{elder_data['ticker']} Elder Impulse", fontsize=14, fontweight="bold")
        ax_ema.set_ylabel("EMA13", fontsize=10)
        ax_ema.grid(True, alpha=0.3)
        ax_ema.legend(loc="upper left")

        # Panel 2: MACD Histogram
        ax_macd = axes[1]
        macd_hist = elder_data.get("macd_hist", [])

        bar_colors = []
        for i, val in enumerate(macd_hist):
            if val is None:
                bar_colors.append("#9E9E9E")
            elif val >= 0:
                # Green if rising, lighter if falling
                if i > 0 and macd_hist[i - 1] is not None and val > macd_hist[i - 1]:
                    bar_colors.append("#26A69A")
                else:
                    bar_colors.append("#80CBC4")
            else:
                # Red if falling, lighter if rising
                if i > 0 and macd_hist[i - 1] is not None and val < macd_hist[i - 1]:
                    bar_colors.append("#EF5350")
                else:
                    bar_colors.append("#FFCDD2")

        vals = [v if v is not None else 0 for v in macd_hist]
        ax_macd.bar(range(len(vals)), vals, color=bar_colors, alpha=0.8)
        ax_macd.axhline(y=0, color="gray", linestyle="-", alpha=0.5)
        ax_macd.set_ylabel("MACD Hist", fontsize=10)
        ax_macd.grid(True, alpha=0.3)

        # Format x-axis
        _format_xaxis(ax_macd, date_objs)

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

        log_info("chart.line", "plot_elder complete", {"ticker": elder_data["ticker"]})

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
    """Parse date string (YYYYMMDD) to datetime."""
    try:
        return datetime.strptime(date_str, "%Y%m%d")
    except ValueError:
        return datetime.now()


def _filter_none(values: List) -> tuple:
    """Filter None values and return x, y lists."""
    x_vals = []
    y_vals = []
    for i, v in enumerate(values):
        if v is not None:
            x_vals.append(i)
            y_vals.append(v)
    return x_vals, y_vals


def _elder_to_color(elder: str) -> str:
    """Convert Elder Impulse color name to matplotlib color."""
    colors = {
        "green": "#26A69A",
        "red": "#EF5350",
        "blue": "#42A5F5",
    }
    return colors.get(elder, "#42A5F5")


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
