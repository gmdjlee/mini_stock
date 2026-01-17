#!/usr/bin/env python
"""
기술적 지표 테스트 스크립트 - 레퍼런스 코드와 동일한 차트 생성.

레퍼런스 코드 분석 결과:
- Fear/Greed, Elder Impulse: Weekly 데이터 사용
- DeMark TD Setup: Daily, Weekly, Monthly 데이터 지원

사용법:
    # Mock 데이터로 테스트 (API 키 불필요)
    python scripts/test_indicators.py --mock

    # 실제 API로 테스트 (API 키 필요)
    python scripts/test_indicators.py --ticker 005930

    # 특정 지표만 테스트
    python scripts/test_indicators.py --mock --indicator trend
    python scripts/test_indicators.py --mock --indicator elder
    python scripts/test_indicators.py --mock --indicator demark

    # 차트 저장
    python scripts/test_indicators.py --mock --save

    # 차트 표시 (GUI 환경)
    python scripts/test_indicators.py --mock --show

차트 종류 (레퍼런스 코드 스타일):
    1. Trend Signal + Fear/Greed Index (Weekly, 듀얼 축)
    2. Elder Impulse System (Weekly, 색상 점)
    3. DeMark TD Setup Counts (Daily/Weekly/Monthly, 듀얼 축: Close + TD 카운트)
"""

import argparse
import io
import math
import os
import random
import sys
from pathlib import Path

import matplotlib.pyplot as plt

# Add src to path
sys.path.insert(0, str(Path(__file__).parent.parent / "src"))


def generate_mock_ohlcv(days: int = 500):
    """Mock OHLCV 데이터 생성 (레퍼런스 코드와 동일한 기간: 약 2년).

    레퍼런스 코드는 2022-01-01부터 데이터를 사용하므로
    52주 고가/저가 계산을 위해 충분한 데이터가 필요합니다.

    자기상관(Autocorrelation) 적용: 실제 시장 데이터와 유사하게
    이전 가격에 기반한 연속적인 가격 움직임 생성.
    """
    from datetime import datetime, timedelta
    random.seed(42)

    base_price = 55000
    dates = []
    opens = []
    highs = []
    lows = []
    closes = []
    volumes = []

    # Generate dates starting from today going backwards
    start_date = datetime(2025, 1, 14)

    # 자기상관을 위한 모멘텀 기반 가격 생성 (역순으로 생성 후 뒤집기)
    prices_chrono = []
    price = base_price
    momentum = 0.0  # 가격 모멘텀

    for i in range(days):
        # 장기 추세 (sine wave)
        cycle = i / days * 6 * math.pi
        trend_target = base_price + math.sin(cycle) * 8000

        # 모멘텀 기반 자기상관 적용 (0.85 = 높은 자기상관)
        # 가격이 추세 방향으로 천천히 움직임
        momentum = 0.85 * momentum + 0.15 * (trend_target - price) / price
        momentum += random.uniform(-0.005, 0.005)  # 작은 랜덤 노이즈
        momentum = max(-0.03, min(0.03, momentum))  # 클리핑

        price = price * (1 + momentum)
        prices_chrono.append(price)

    # 역순으로 변환 (newest first)
    prices_chrono.reverse()

    # OHLCV 생성
    for i in range(days):
        current_date = start_date - timedelta(days=i)
        dates.append(current_date.strftime("%Y%m%d"))

        price = prices_chrono[i]

        # OHLC 생성 (자기상관 유지하면서 작은 변동)
        open_price = int(price * random.uniform(0.995, 1.005))
        close_price = int(price * random.uniform(0.995, 1.005))
        high_price = int(max(open_price, close_price) * random.uniform(1.0, 1.015))
        low_price = int(min(open_price, close_price) * random.uniform(0.985, 1.0))
        volume = random.randint(10_000_000, 30_000_000)

        opens.append(open_price)
        highs.append(high_price)
        lows.append(low_price)
        closes.append(close_price)
        volumes.append(volume)

    return {
        "ticker": "005930",
        "name": "삼성전자 (Mock)",
        "dates": dates,
        "open": opens,
        "high": highs,
        "low": lows,
        "close": closes,
        "volume": volumes,
    }


# 레퍼런스 코드와 동일한 기간 상수
DAYS_1_YEAR = 252  # 1년 (거래일 기준)
DAYS_2_YEARS = 500  # 2년 (52주 고가/저가 계산용)
WEEKS_1_YEAR = 52  # 1년 (주간 기준)
WEEKS_2_YEARS = 104  # 2년 (52주 고가/저가 계산용)
MONTHS_FULL = 60  # 5년 (월간 전체 기간)


def generate_mock_weekly_ohlcv(weeks: int = WEEKS_2_YEARS):
    """Mock 주간 OHLCV 데이터 생성 (레퍼런스 방식: 일간 데이터 리샘플링).

    레퍼런스 코드처럼 일간 데이터를 생성한 후 주간으로 리샘플링합니다.
    - Open: 주간 첫 거래일 시가
    - High: 주간 최고가
    - Low: 주간 최저가
    - Close: 주간 마지막 거래일 종가
    - Volume: 주간 거래량 합계
    """
    from stock_analyzer.stock.ohlcv import resample_to_weekly

    # 일간 데이터 생성 (주간 수 * 7일 + 여유분)
    daily_days = weeks * 7 + 30
    daily = generate_mock_ohlcv(daily_days)

    # 주간으로 리샘플링
    weekly = resample_to_weekly(
        daily["dates"],
        daily["open"],
        daily["high"],
        daily["low"],
        daily["close"],
        daily["volume"],
    )

    # 요청한 주 수만큼 자르기
    trim_len = min(weeks, len(weekly["dates"]))

    return {
        "ticker": "005930",
        "name": "삼성전자 (Mock Weekly - Resampled)",
        "dates": weekly["dates"][:trim_len],
        "open": weekly["open"][:trim_len],
        "high": weekly["high"][:trim_len],
        "low": weekly["low"][:trim_len],
        "close": weekly["close"][:trim_len],
        "volume": weekly["volume"][:trim_len],
    }


def generate_mock_monthly_ohlcv(months: int = MONTHS_FULL):
    """Mock 월간 OHLCV 데이터 생성 (레퍼런스 방식: 일간 데이터 리샘플링).

    레퍼런스 코드처럼 일간 데이터를 생성한 후 월간으로 리샘플링합니다.
    - Open: 월간 첫 거래일 시가
    - High: 월간 최고가
    - Low: 월간 최저가
    - Close: 월간 마지막 거래일 종가
    - Volume: 월간 거래량 합계
    """
    from stock_analyzer.stock.ohlcv import resample_to_monthly

    # 일간 데이터 생성 (월 수 * 22거래일 + 여유분)
    daily_days = months * 22 + 60
    daily = generate_mock_ohlcv(daily_days)

    # 월간으로 리샘플링
    monthly = resample_to_monthly(
        daily["dates"],
        daily["open"],
        daily["high"],
        daily["low"],
        daily["close"],
        daily["volume"],
    )

    # 요청한 월 수만큼 자르기
    trim_len = min(months, len(monthly["dates"]))

    return {
        "ticker": "005930",
        "name": "삼성전자 (Mock Monthly - Resampled)",
        "dates": monthly["dates"][:trim_len],
        "open": monthly["open"][:trim_len],
        "high": monthly["high"][:trim_len],
        "low": monthly["low"][:trim_len],
        "close": monthly["close"][:trim_len],
        "volume": monthly["volume"][:trim_len],
    }


def plot_trend_reference_style(trend_data: dict, closes: list = None, save_path: str = None):
    """
    레퍼런스 코드 스타일의 Trend + Fear/Greed 차트.

    레퍼런스 스타일:
    - Close 라인 (파란색)
    - MA10 라인 (주황색 점선)
    - Primary/Additional Buy/Sell 마커
    - Fear/Greed Index (우측 축)
    - ±0.5 기준선
    """
    dates = trend_data["dates"]
    n_dates = len(dates)
    # closes 길이가 dates보다 길면 최근 n_dates개만 사용
    if closes and len(closes) > n_dates:
        closes_data = closes[:n_dates]
    else:
        closes_data = closes if closes else trend_data.get("ma5", [])
    ma10 = trend_data.get("ma10", trend_data.get("ma20", []))  # MA10 또는 MA20
    fear_greed = trend_data.get("fear_greed", [])
    ma_signal = trend_data.get("ma_signal", [])
    trend_values = trend_data.get("trend", [])

    # 데이터 역순 (차트 표시용 - 오래된 것이 왼쪽)
    dates_display = list(reversed(dates))
    closes_display = list(reversed(closes_data)) if closes_data else []
    ma10_display = list(reversed(ma10)) if ma10 else []
    fg_display = list(reversed(fear_greed))
    signal_display = list(reversed(ma_signal))
    trend_display = list(reversed(trend_values))

    n = len(dates_display)
    x = list(range(n))

    fig, ax1 = plt.subplots(figsize=(16, 5))

    # Close 라인 (레퍼런스: 파란색)
    if closes_display:
        ax1.plot(x, closes_display, label="Close", color="tab:blue", linewidth=1.0)

    # MA10 라인 (레퍼런스: 주황색 점선)
    if ma10_display:
        ax1.plot(x, ma10_display, label="MA10", linestyle="--", color="tab:orange", linewidth=1.0)

    # Buy/Sell 신호 표시 (레퍼런스 스타일: Primary/Additional)
    primary_buy_indices = []
    primary_sell_indices = []
    additional_buy_indices = []
    additional_sell_indices = []

    for i, (sig, tr) in enumerate(zip(signal_display, trend_display)):
        if tr == "bullish" and sig == 1:
            primary_buy_indices.append(i)
        elif tr == "bearish" and sig == -1:
            primary_sell_indices.append(i)
        elif sig == 1:  # Additional buy (non-bullish trend)
            additional_buy_indices.append(i)
        elif sig == -1:  # Additional sell (non-bearish trend)
            additional_sell_indices.append(i)

    # 차트에 표시할 y값 (Close 또는 MA10)
    y_for_markers = closes_display if closes_display else ma10_display

    # Additional Buy (연한 빨강, 작은 마커)
    if additional_buy_indices and y_for_markers:
        ax1.scatter([i for i in additional_buy_indices],
                   [y_for_markers[i] for i in additional_buy_indices],
                   marker='^', s=60, alpha=0.4, color='red', label="Additional Buy", zorder=4)

    # Primary Buy (진한 빨강, 큰 마커)
    if primary_buy_indices and y_for_markers:
        ax1.scatter([i for i in primary_buy_indices],
                   [y_for_markers[i] for i in primary_buy_indices],
                   marker='^', s=100, alpha=1.0, color='darkred', label="Primary Buy", zorder=5)

    # Additional Sell (연한 파랑, 작은 마커)
    if additional_sell_indices and y_for_markers:
        ax1.scatter([i for i in additional_sell_indices],
                   [y_for_markers[i] for i in additional_sell_indices],
                   marker='v', s=60, alpha=0.4, color='blue', label="Additional Sell", zorder=4)

    # Primary Sell (진한 파랑, 큰 마커)
    if primary_sell_indices and y_for_markers:
        ax1.scatter([i for i in primary_sell_indices],
                   [y_for_markers[i] for i in primary_sell_indices],
                   marker='v', s=100, alpha=1.0, color='darkblue', label="Primary Sell", zorder=5)

    ax1.set_title(f"{trend_data['ticker']}.KS Weekly Strategy + Fear & Greed", fontsize=14, fontweight='bold')
    ax1.set_xlabel("Date")
    ax1.set_ylabel("Price")
    ax1.grid(True, alpha=0.3)
    ax1.legend(loc="upper left")

    # X축 레이블 (레퍼런스: YYYY-MM 형식)
    step = max(1, n // 10)
    tick_positions = list(range(0, n, step))
    tick_labels = [dates_display[i][:4] + "-" + dates_display[i][4:6] for i in tick_positions if i < n]
    ax1.set_xticks(tick_positions[:len(tick_labels)])
    ax1.set_xticklabels(tick_labels, rotation=45, ha='right')

    # Fear/Greed on secondary axis (레퍼런스 스타일)
    ax2 = ax1.twinx()
    ax2.plot(x, fg_display, color='orange', linewidth=1.5, label='FG Index', alpha=0.8)
    ax2.axhline(0.5, linestyle='--', color='red', alpha=0.5)
    ax2.axhline(-0.5, linestyle='--', color='green', alpha=0.5)
    ax2.set_ylabel("FG")
    ax2.set_ylim(-1.5, 1.5)
    ax2.legend(loc="upper right")

    plt.tight_layout()

    # Save to bytes
    buf = io.BytesIO()
    plt.savefig(buf, format="png", dpi=100, bbox_inches="tight")
    buf.seek(0)
    image_bytes = buf.getvalue()
    buf.close()

    if save_path:
        plt.savefig(save_path, format="png", dpi=100, bbox_inches="tight")

    plt.close(fig)

    return {"ok": True, "data": {"image_bytes": image_bytes, "save_path": save_path}}


def plot_elder_reference_style(elder_data: dict, closes: list = None, save_path: str = None, last_n_days: int = DAYS_1_YEAR):
    """
    레퍼런스 코드 스타일의 Elder Impulse System 차트.

    레퍼런스 스타일:
    - Close 라인 (파란색)
    - EMA13 라인 (주황색 점선)
    - Bull/Bear/Neutral 색상 점 (Close 위에 표시)
    - **최근 1년 데이터만 표시** (레퍼런스: last 1 year)
    """
    # 최근 1년만 사용 (레퍼런스: last 1 year)
    dates = elder_data["dates"][:last_n_days]
    ema13 = elder_data.get("ema13", [])[:last_n_days]
    colors = elder_data.get("color", [])[:last_n_days]
    closes_data = closes[:last_n_days] if closes else []

    # 데이터 역순
    dates_display = list(reversed(dates))
    ema13_display = list(reversed(ema13))
    colors_display = list(reversed(colors))
    closes_display = list(reversed(closes_data)) if closes_data else []

    n = len(dates_display)
    x = list(range(n))

    fig, ax = plt.subplots(figsize=(16, 5))

    # Close 라인 (레퍼런스: 파란색 실선)
    if closes_display:
        ax.plot(x, closes_display, linewidth=1.0, label="Close", color="tab:blue")

    # EMA13 라인 (레퍼런스: 주황색 점선)
    valid_x = [i for i, v in enumerate(ema13_display) if v is not None]
    valid_y = [v for v in ema13_display if v is not None]
    ax.plot(valid_x, valid_y, linewidth=1.0, linestyle="--", label="EMA13 (Weekly)", color="tab:orange")

    # 색상별 점 그리기 - Close 가격 위에 표시 (레퍼런스 스타일)
    y_for_scatter = closes_display if closes_display else ema13_display
    for i, (val, color) in enumerate(zip(y_for_scatter, colors_display)):
        if val is None:
            continue
        if color == "green":
            ax.scatter(i, val, s=40, alpha=0.9, color="green", zorder=5)
        elif color == "red":
            ax.scatter(i, val, s=40, alpha=0.9, color="red", zorder=5)
        else:  # blue/neutral
            ax.scatter(i, val, s=30, alpha=0.7, color="gray", zorder=5)

    # 범례용 더미 플롯 (레퍼런스 순서)
    ax.scatter([], [], s=30, color="gray", label="Neutral")
    ax.scatter([], [], s=40, color="green", label="Bullish Impulse")
    ax.scatter([], [], s=40, color="red", label="Bearish Impulse")

    ax.set_title(f"{elder_data['ticker']}.KS Elder Impulse System (Weekly, last 1 year)", fontsize=14, fontweight='bold')
    ax.set_xlabel("Date")
    ax.set_ylabel("Price")
    ax.grid(True, alpha=0.3)
    ax.legend(loc="upper left")

    # X축 레이블 (레퍼런스: YYYY-MM 형식)
    step = max(1, n // 10)
    tick_positions = list(range(0, n, step))
    tick_labels = [dates_display[i][:4] + "-" + dates_display[i][4:6] for i in tick_positions if i < n]
    ax.set_xticks(tick_positions[:len(tick_labels)])
    ax.set_xticklabels(tick_labels, rotation=45, ha='right')

    plt.tight_layout()

    # Save to bytes
    buf = io.BytesIO()
    plt.savefig(buf, format="png", dpi=100, bbox_inches="tight")
    buf.seek(0)
    image_bytes = buf.getvalue()
    buf.close()

    if save_path:
        plt.savefig(save_path, format="png", dpi=100, bbox_inches="tight")

    plt.close(fig)

    return {"ok": True, "data": {"image_bytes": image_bytes, "save_path": save_path}}


def plot_demark_reference_style(demark_data: dict, closes: list, save_path: str = None, last_n_days: int = DAYS_1_YEAR, chart_type: str = "Daily"):
    """
    레퍼런스 코드 스타일의 DeMark TD Setup 차트.

    레퍼런스 스타일:
    - Close 라인 (좌측 축, 검정색)
    - TD Sell Setup (우측 축, 빨간색)
    - TD Buy Setup (우측 축, 파란색)
    - **최근 1년 데이터만 표시** (레퍼런스: last 1 year)

    Args:
        chart_type: "Daily", "Weekly", or "Monthly"
    """
    # 최근 1년만 사용 (레퍼런스: last 1 year)
    dates = demark_data["dates"][:last_n_days]
    sell_setup = demark_data.get("sell_setup", [])[:last_n_days]
    buy_setup = demark_data.get("buy_setup", [])[:last_n_days]
    closes = closes[:last_n_days] if closes else []

    # 데이터 역순
    dates_display = list(reversed(dates))
    closes_display = list(reversed(closes)) if closes else []
    sell_display = list(reversed(sell_setup))
    buy_display = list(reversed(buy_setup))

    n = len(dates_display)
    x = list(range(n))

    fig, ax1 = plt.subplots(figsize=(16, 5))

    # Close 라인 (좌측 축, 레퍼런스: 검정색)
    if closes_display:
        ax1.plot(x, closes_display, color='black', linewidth=1.0, label='Close')
        ax1.set_ylabel("Price")

    # 타이틀 (레퍼런스 형식: "005930.KS Daily DeMark TD Setup Counts (last 1 year)")
    period_text = "last 1 year" if last_n_days == DAYS_1_YEAR else "Full Period"
    ax1.set_title(f"{demark_data['ticker']}.KS {chart_type} DeMark TD Setup Counts ({period_text})", fontsize=14, fontweight='bold')
    ax1.set_xlabel("Date")
    ax1.grid(True, alpha=0.3)
    ax1.legend(loc="upper left")

    # X축 레이블 (레퍼런스: YYYY-MM 형식)
    step = max(1, n // 10)
    tick_positions = list(range(0, n, step))
    tick_labels = [dates_display[i][:4] + "-" + dates_display[i][4:6] for i in tick_positions if i < n]
    ax1.set_xticks(tick_positions[:len(tick_labels)])
    ax1.set_xticklabels(tick_labels, rotation=45, ha='right')

    # TD 카운트 (우측 축) - 레퍼런스 스타일
    ax2 = ax1.twinx()
    ax2.plot(x, sell_display, color='red', linewidth=1.5, label='TD Sell Setup')
    ax2.plot(x, buy_display, color='blue', linewidth=1.5, label='TD Buy Setup')
    ax2.set_ylabel("TD Setup Count", color='gray')
    ax2.tick_params(axis='y', labelcolor='gray')

    # Y축 범위 설정
    max_td = max(max(sell_display) if sell_display else 0, max(buy_display) if buy_display else 0)
    ax2.set_ylim(0, max_td + 2)
    ax2.legend(loc="upper right")

    plt.tight_layout()

    # Save to bytes
    buf = io.BytesIO()
    plt.savefig(buf, format="png", dpi=100, bbox_inches="tight")
    buf.seek(0)
    image_bytes = buf.getvalue()
    buf.close()

    if save_path:
        plt.savefig(save_path, format="png", dpi=100, bbox_inches="tight")

    plt.close(fig)

    return {"ok": True, "data": {"image_bytes": image_bytes, "save_path": save_path}}


def test_trend_with_mock(show: bool = False, save: bool = False):
    """Mock 데이터로 Trend Signal 테스트 (레퍼런스: Weekly 데이터)."""
    from stock_analyzer.indicator import trend

    print("=" * 60)
    print("Trend Signal 테스트 (Mock Weekly 데이터) - 레퍼런스 스타일")
    print("=" * 60)

    # 1. Mock Weekly OHLCV 데이터 생성 (레퍼런스: 주간 데이터 사용)
    print("\n[1] Mock Weekly OHLCV 데이터 생성 (2년, 52주 계산용)...")
    mock_ohlcv = generate_mock_weekly_ohlcv(WEEKS_2_YEARS)
    print(f"    - 종목: {mock_ohlcv['ticker']} ({mock_ohlcv['name']})")
    print(f"    - 기간: {mock_ohlcv['dates'][-1]} ~ {mock_ohlcv['dates'][0]}")
    print(f"    - 데이터 수: {len(mock_ohlcv['dates'])}주")

    # 2. Trend Signal 계산 (weekly timeframe)
    print("\n[2] Trend Signal 계산 (Weekly timeframe)...")
    trend_result = trend.calc_from_ohlcv(
        ticker=mock_ohlcv["ticker"],
        dates=mock_ohlcv["dates"],
        closes=mock_ohlcv["close"],
        highs=mock_ohlcv["high"],
        lows=mock_ohlcv["low"],
        volumes=mock_ohlcv["volume"],
        timeframe="weekly",
    )

    if not trend_result["ok"]:
        print(f"    오류: {trend_result['error']}")
        return

    data = trend_result["data"]
    # Data is "newest first", so index 0 is the most recent
    print(f"    - Timeframe: {data.get('timeframe', 'daily')}")
    print(f"    - MA Signal 최신값: {data['ma_signal'][0]}")
    print(f"    - CMF 최신값: {data['cmf'][0]:.4f}")
    print(f"    - Fear/Greed 최신값: {data['fear_greed'][0]:.4f}")
    print(f"    - Trend 최신값: {data['trend'][0]}")

    # 3. 레퍼런스 스타일 차트 생성
    print("\n[3] 레퍼런스 스타일 차트 생성...")
    save_path = None
    if save:
        save_path = str(Path(__file__).parent.parent / "output" / "trend_fear_greed_weekly.png")
        os.makedirs(os.path.dirname(save_path), exist_ok=True)

    chart_result = plot_trend_reference_style(data, closes=mock_ohlcv["close"], save_path=save_path)

    if chart_result["ok"]:
        print(f"    - 이미지 크기: {len(chart_result['data']['image_bytes']):,} bytes")
        if save_path:
            print(f"    - 저장 경로: {save_path}")

        if show:
            _show_chart(chart_result["data"]["image_bytes"])
    else:
        print(f"    차트 생성 오류: {chart_result.get('error', 'Unknown')}")

    print("\n" + "=" * 60)
    print("Trend Signal 테스트 완료!")
    print("=" * 60)


def test_elder_with_mock(show: bool = False, save: bool = False):
    """Mock 데이터로 Elder Impulse 테스트 (레퍼런스: Weekly 데이터)."""
    from stock_analyzer.indicator import elder

    print("=" * 60)
    print("Elder Impulse 테스트 (Mock Weekly 데이터) - 레퍼런스 스타일")
    print("=" * 60)

    # 1. Mock Weekly OHLCV 데이터 생성 (레퍼런스: 주간 데이터 사용, 최근 1년 차트)
    print("\n[1] Mock Weekly OHLCV 데이터 생성 (2년, 차트는 최근 1년)...")
    mock_ohlcv = generate_mock_weekly_ohlcv(WEEKS_2_YEARS)
    print(f"    - 종목: {mock_ohlcv['ticker']} ({mock_ohlcv['name']})")
    print(f"    - 기간: {mock_ohlcv['dates'][-1]} ~ {mock_ohlcv['dates'][0]}")
    print(f"    - 데이터 수: {len(mock_ohlcv['dates'])}주 (차트: 최근 {WEEKS_1_YEAR}주)")

    # 2. Elder Impulse 계산 (weekly timeframe)
    print("\n[2] Elder Impulse 계산 (Weekly timeframe)...")
    elder_result = elder.calc_from_ohlcv(
        ticker=mock_ohlcv["ticker"],
        dates=mock_ohlcv["dates"],
        closes=mock_ohlcv["close"],
        timeframe="weekly",
    )

    if not elder_result["ok"]:
        print(f"    오류: {elder_result['error']}")
        return

    data = elder_result["data"]
    # Data is "newest first", so index 0 is the most recent
    print(f"    - Timeframe: {data.get('timeframe', 'daily')}")
    print(f"    - Color 최신값: {data['color'][0]}")
    ema13_val = data['ema13'][0]
    macd_hist_val = data['macd_hist'][0]
    print(f"    - EMA13 최신값: {ema13_val:.2f}" if ema13_val is not None else "    - EMA13 최신값: N/A")
    print(f"    - MACD Hist 최신값: {macd_hist_val:.4f}" if macd_hist_val is not None else "    - MACD Hist 최신값: N/A")

    # 색상 통계
    colors = data["color"]
    green_count = colors.count("green")
    red_count = colors.count("red")
    blue_count = colors.count("blue")
    print(f"    - 색상 분포: Green={green_count}, Red={red_count}, Blue={blue_count}")

    # 3. 레퍼런스 스타일 차트 생성
    print("\n[3] 레퍼런스 스타일 차트 생성...")
    save_path = None
    if save:
        save_path = str(Path(__file__).parent.parent / "output" / "elder_impulse_weekly.png")
        os.makedirs(os.path.dirname(save_path), exist_ok=True)

    chart_result = plot_elder_reference_style(data, closes=mock_ohlcv["close"], save_path=save_path, last_n_days=WEEKS_1_YEAR)

    if chart_result["ok"]:
        print(f"    - 이미지 크기: {len(chart_result['data']['image_bytes']):,} bytes")
        if save_path:
            print(f"    - 저장 경로: {save_path}")

        if show:
            _show_chart(chart_result["data"]["image_bytes"])
    else:
        print(f"    차트 생성 오류: {chart_result.get('error', 'Unknown')}")

    print("\n" + "=" * 60)
    print("Elder Impulse 테스트 완료!")
    print("=" * 60)


def test_demark_with_mock(show: bool = False, save: bool = False):
    """Mock 데이터로 DeMark TD 테스트 (레퍼런스: Daily/Weekly/Monthly 3종 차트)."""
    from stock_analyzer.indicator import demark

    print("=" * 60)
    print("DeMark TD 테스트 (Mock 데이터) - 레퍼런스 스타일 (3종 차트)")
    print("=" * 60)

    output_dir = Path(__file__).parent.parent / "output"
    if save:
        os.makedirs(output_dir, exist_ok=True)

    # ============================================
    # 1. Daily DeMark TD (레퍼런스: 다운로드4.png)
    # ============================================
    print("\n" + "-" * 40)
    print("[1] Daily DeMark TD Setup (last 1 year)")
    print("-" * 40)

    mock_daily = generate_mock_ohlcv(DAYS_2_YEARS)
    print(f"    - 종목: {mock_daily['ticker']}")
    print(f"    - 기간: {mock_daily['dates'][-1]} ~ {mock_daily['dates'][0]}")
    print(f"    - 데이터 수: {len(mock_daily['dates'])}일")

    daily_result = demark.calc_from_ohlcv(
        ticker=mock_daily["ticker"],
        dates=mock_daily["dates"],
        closes=mock_daily["close"],
        timeframe="daily",
    )

    if daily_result["ok"]:
        data = daily_result["data"]
        print(f"    - Timeframe: {data.get('timeframe', 'daily')}")
        print(f"    - Sell Setup 최신값: {data['sell_setup'][0]}, Max: {max(data['sell_setup'])}")
        print(f"    - Buy Setup 최신값: {data['buy_setup'][0]}, Max: {max(data['buy_setup'])}")

        save_path = str(output_dir / "demark_td_daily.png") if save else None
        chart_result = plot_demark_reference_style(
            data, mock_daily["close"], save_path=save_path,
            last_n_days=DAYS_1_YEAR, chart_type="Daily"
        )
        if chart_result["ok"]:
            print(f"    - 차트 크기: {len(chart_result['data']['image_bytes']):,} bytes")
            if save_path:
                print(f"    - 저장: {save_path}")
            if show:
                _show_chart(chart_result["data"]["image_bytes"])

    # ============================================
    # 2. Weekly DeMark TD (레퍼런스: 다운로드3.png)
    # ============================================
    print("\n" + "-" * 40)
    print("[2] Weekly DeMark TD Setup (last 1 year)")
    print("-" * 40)

    mock_weekly = generate_mock_weekly_ohlcv(WEEKS_2_YEARS)
    print(f"    - 종목: {mock_weekly['ticker']}")
    print(f"    - 기간: {mock_weekly['dates'][-1]} ~ {mock_weekly['dates'][0]}")
    print(f"    - 데이터 수: {len(mock_weekly['dates'])}주")

    weekly_result = demark.calc_from_ohlcv(
        ticker=mock_weekly["ticker"],
        dates=mock_weekly["dates"],
        closes=mock_weekly["close"],
        timeframe="weekly",
    )

    if weekly_result["ok"]:
        data = weekly_result["data"]
        print(f"    - Timeframe: {data.get('timeframe', 'weekly')}")
        print(f"    - Sell Setup 최신값: {data['sell_setup'][0]}, Max: {max(data['sell_setup'])}")
        print(f"    - Buy Setup 최신값: {data['buy_setup'][0]}, Max: {max(data['buy_setup'])}")

        save_path = str(output_dir / "demark_td_weekly.png") if save else None
        chart_result = plot_demark_reference_style(
            data, mock_weekly["close"], save_path=save_path,
            last_n_days=WEEKS_1_YEAR, chart_type="Weekly"
        )
        if chart_result["ok"]:
            print(f"    - 차트 크기: {len(chart_result['data']['image_bytes']):,} bytes")
            if save_path:
                print(f"    - 저장: {save_path}")
            if show:
                _show_chart(chart_result["data"]["image_bytes"])

    # ============================================
    # 3. Monthly DeMark TD (레퍼런스: 다운로드5.png - Full Period)
    # ============================================
    print("\n" + "-" * 40)
    print("[3] Monthly DeMark TD Setup (Full Period)")
    print("-" * 40)

    try:
        mock_monthly = generate_mock_monthly_ohlcv(MONTHS_FULL)
        print(f"    - 종목: {mock_monthly['ticker']}")
        print(f"    - 기간: {mock_monthly['dates'][-1]} ~ {mock_monthly['dates'][0]}")
        print(f"    - 데이터 수: {len(mock_monthly['dates'])}개월")

        monthly_result = demark.calc_from_ohlcv(
            ticker=mock_monthly["ticker"],
            dates=mock_monthly["dates"],
            closes=mock_monthly["close"],
            timeframe="monthly",
        )

        if monthly_result["ok"]:
            data = monthly_result["data"]
            print(f"    - Timeframe: {data.get('timeframe', 'monthly')}")
            print(f"    - Sell Setup 최신값: {data['sell_setup'][0]}, Max: {max(data['sell_setup'])}")
            print(f"    - Buy Setup 최신값: {data['buy_setup'][0]}, Max: {max(data['buy_setup'])}")

            save_path = str(output_dir / "demark_td_monthly.png") if save else None
            # Full period for monthly (레퍼런스: Full Period)
            chart_result = plot_demark_reference_style(
                data, mock_monthly["close"], save_path=save_path,
                last_n_days=len(data["dates"]), chart_type="Monthly"
            )
            if chart_result["ok"]:
                print(f"    - 차트 크기: {len(chart_result['data']['image_bytes']):,} bytes")
                if save_path:
                    print(f"    - 저장: {save_path}")
                if show:
                    _show_chart(chart_result["data"]["image_bytes"])
    except ImportError as e:
        print(f"    - 월간 데이터 생성 건너뜀 (python-dateutil 필요): {e}")

    print("\n" + "=" * 60)
    print("DeMark TD 테스트 완료! (Daily/Weekly/Monthly 3종)")
    print("=" * 60)


def test_with_api(ticker: str, indicator: str = "all", show: bool = False, save: bool = False):
    """실제 API로 지표 테스트 (레퍼런스와 동일하게 Weekly 리샘플링 사용)."""
    from dotenv import load_dotenv
    load_dotenv()

    app_key = os.getenv("KIWOOM_APP_KEY")
    secret_key = os.getenv("KIWOOM_SECRET_KEY")
    base_url = os.getenv("KIWOOM_BASE_URL", "https://api.kiwoom.com")

    if not app_key or not secret_key:
        print("오류: .env 파일에 API 키를 설정해 주세요.")
        print("  KIWOOM_APP_KEY=your_app_key")
        print("  KIWOOM_SECRET_KEY=your_secret_key")
        return

    from stock_analyzer.client.kiwoom import KiwoomClient
    from stock_analyzer.indicator import trend, elder, demark
    from stock_analyzer.stock import ohlcv

    print("=" * 60)
    print(f"기술적 지표 테스트 (실제 API) - {ticker}")
    print("=" * 60)

    # 1. 클라이언트 생성
    print("\n[1] API 클라이언트 생성...")
    client = KiwoomClient(app_key, secret_key, base_url)

    # 2. OHLCV 데이터 조회 (일간 → 주간 리샘플링)
    print("\n[2] OHLCV 데이터 조회 (일간 데이터 → 주간 리샘플링)...")
    daily_result = ohlcv.get_daily(client, ticker, days=DAYS_2_YEARS)

    if not daily_result["ok"]:
        print(f"    오류: {daily_result['error']}")
        return

    daily_data = daily_result["data"]
    print(f"    - 일간 기간: {daily_data['dates'][-1]} ~ {daily_data['dates'][0]}")
    print(f"    - 일간 데이터 수: {len(daily_data['dates'])}일")

    # 주간으로 리샘플링 (레퍼런스와 동일)
    weekly_data = ohlcv.resample_to_weekly(
        daily_data["dates"], daily_data["open"], daily_data["high"],
        daily_data["low"], daily_data["close"], daily_data["volume"],
    )
    weekly_data["ticker"] = ticker
    print(f"    - 주간 데이터 수: {len(weekly_data['dates'])}주 (리샘플링)")

    # 출력 디렉토리 생성
    output_dir = Path(__file__).parent.parent / "output"
    if save:
        os.makedirs(output_dir, exist_ok=True)

    # 3. Trend Signal (Weekly timeframe - 레퍼런스와 동일)
    if indicator in ("all", "trend"):
        print("\n" + "-" * 40)
        print("[Trend Signal + Fear/Greed] (Weekly - 리샘플링)")
        print("-" * 40)
        # calc() 함수 내부에서 리샘플링을 수행하므로 timeframe="weekly" 지정
        trend_result = trend.calc(client, ticker, days=WEEKS_2_YEARS, timeframe="weekly")

        if trend_result["ok"]:
            data = trend_result["data"]
            print(f"    Timeframe: {data.get('timeframe', 'daily')}")
            print(f"    MA Signal: {data['ma_signal'][0]}")
            print(f"    CMF: {data['cmf'][0]:.4f}")
            print(f"    Fear/Greed: {data['fear_greed'][0]:.4f}")
            print(f"    Trend: {data['trend'][0]}")

            save_path = str(output_dir / f"trend_fear_greed_{ticker}_weekly.png") if save else None
            chart_result = plot_trend_reference_style(data, closes=weekly_data["close"], save_path=save_path)
            if chart_result["ok"]:
                print(f"    차트 크기: {len(chart_result['data']['image_bytes']):,} bytes")
                if save_path:
                    print(f"    저장: {save_path}")
                if show:
                    _show_chart(chart_result["data"]["image_bytes"])
        else:
            print(f"    오류: {trend_result['error']}")

    # 4. Elder Impulse (Weekly timeframe - 레퍼런스와 동일)
    if indicator in ("all", "elder"):
        print("\n" + "-" * 40)
        print("[Elder Impulse System] (Weekly - 리샘플링, 차트: 최근 1년)")
        print("-" * 40)
        elder_result = elder.calc(client, ticker, days=WEEKS_2_YEARS, timeframe="weekly")

        if elder_result["ok"]:
            data = elder_result["data"]
            print(f"    Timeframe: {data.get('timeframe', 'daily')}")
            print(f"    Color: {data['color'][0]}")
            ema13_val = data['ema13'][0]
            print(f"    EMA13: {ema13_val:.2f}" if ema13_val is not None else "    EMA13: N/A")

            # 색상 통계
            colors = data["color"]
            green = colors.count("green")
            red = colors.count("red")
            blue = colors.count("blue")
            print(f"    색상 분포: Green={green}, Red={red}, Blue={blue}")

            save_path = str(output_dir / f"elder_impulse_{ticker}_weekly.png") if save else None
            chart_result = plot_elder_reference_style(data, closes=weekly_data["close"], save_path=save_path, last_n_days=WEEKS_1_YEAR)
            if chart_result["ok"]:
                print(f"    차트 크기: {len(chart_result['data']['image_bytes']):,} bytes")
                if save_path:
                    print(f"    저장: {save_path}")
                if show:
                    _show_chart(chart_result["data"]["image_bytes"])
        else:
            print(f"    오류: {elder_result['error']}")

    # 5. DeMark TD (Weekly timeframe - 레퍼런스와 동일)
    if indicator in ("all", "demark"):
        print("\n" + "-" * 40)
        print("[DeMark TD Setup] (Weekly - 리샘플링, 차트: 최근 1년)")
        print("-" * 40)
        demark_result = demark.calc(client, ticker, days=WEEKS_2_YEARS, timeframe="weekly")

        if demark_result["ok"]:
            data = demark_result["data"]
            print(f"    Timeframe: {data.get('timeframe', 'daily')}")
            print(f"    Sell Setup 최신값: {data['sell_setup'][0]}")
            print(f"    Buy Setup 최신값: {data['buy_setup'][0]}")
            print(f"    Max Sell: {max(data['sell_setup'])}, Max Buy: {max(data['buy_setup'])}")

            # Active Setups
            active = demark.get_active_setups(
                data["sell_setup"],
                data["buy_setup"],
                data["dates"],
            )
            print(f"    Current: Sell={active['current_sell']}, Buy={active['current_buy']}")

            save_path = str(output_dir / f"demark_td_{ticker}_weekly.png") if save else None
            chart_result = plot_demark_reference_style(data, weekly_data["close"], save_path=save_path, last_n_days=WEEKS_1_YEAR, chart_type="Weekly")
            if chart_result["ok"]:
                print(f"    차트 크기: {len(chart_result['data']['image_bytes']):,} bytes")
                if save_path:
                    print(f"    저장: {save_path}")
                if show:
                    _show_chart(chart_result["data"]["image_bytes"])
        else:
            print(f"    오류: {demark_result['error']}")

    print("\n" + "=" * 60)
    print("테스트 완료!")
    print("=" * 60)


def _show_chart(image_bytes: bytes):
    """차트를 GUI에 표시."""
    try:
        import matplotlib
        matplotlib.use("TkAgg")
        import matplotlib.pyplot as plt
        from io import BytesIO
        from PIL import Image

        img = Image.open(BytesIO(image_bytes))
        plt.figure(figsize=(14, 10))
        plt.imshow(img)
        plt.axis("off")
        plt.tight_layout()
        plt.show()
    except ImportError as e:
        print(f"    차트 표시를 위해 추가 패키지 필요: {e}")
        print("    pip install Pillow")
    except Exception as e:
        print(f"    차트 표시 오류: {e}")


def main():
    parser = argparse.ArgumentParser(description="기술적 지표 테스트 (레퍼런스 스타일)")
    parser.add_argument("--mock", action="store_true", help="Mock 데이터로 테스트")
    parser.add_argument("--ticker", type=str, help="종목코드 (실제 API 테스트)")
    parser.add_argument(
        "--indicator",
        type=str,
        choices=["all", "trend", "elder", "demark"],
        default="all",
        help="테스트할 지표 (default: all)",
    )
    parser.add_argument("--show", action="store_true", help="차트 화면에 표시")
    parser.add_argument("--save", action="store_true", help="차트 파일로 저장")

    args = parser.parse_args()

    if args.mock:
        if args.indicator in ("all", "trend"):
            test_trend_with_mock(show=args.show, save=args.save)
            print("\n")
        if args.indicator in ("all", "elder"):
            test_elder_with_mock(show=args.show, save=args.save)
            print("\n")
        if args.indicator in ("all", "demark"):
            test_demark_with_mock(show=args.show, save=args.save)
    elif args.ticker:
        test_with_api(args.ticker, args.indicator, show=args.show, save=args.save)
    else:
        print("기술적 지표 테스트 스크립트 (레퍼런스 코드 스타일)")
        print("")
        print("차트 종류:")
        print("  1. Trend Signal + Fear/Greed Index (듀얼 축)")
        print("  2. Elder Impulse System (색상 점)")
        print("  3. DeMark TD Setup Counts (듀얼 축: Close + TD 카운트)")
        print("")
        print("사용법:")
        print("  python scripts/test_indicators.py --mock                    # 전체 Mock 테스트")
        print("  python scripts/test_indicators.py --mock --indicator trend  # Trend Signal만")
        print("  python scripts/test_indicators.py --mock --indicator elder  # Elder Impulse만")
        print("  python scripts/test_indicators.py --mock --indicator demark # DeMark TD만")
        print("  python scripts/test_indicators.py --mock --show             # 차트 표시")
        print("  python scripts/test_indicators.py --mock --save             # 차트 저장")
        print("  python scripts/test_indicators.py --ticker 005930           # 실제 API")
        print("  python scripts/test_indicators.py --ticker 005930 --save    # 실제 API + 저장")


if __name__ == "__main__":
    main()
