#!/usr/bin/env python
"""
기술적 지표 테스트 스크립트.

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
"""

import argparse
import math
import os
import random
import sys
from pathlib import Path

# Add src to path
sys.path.insert(0, str(Path(__file__).parent.parent / "src"))


def generate_mock_ohlcv(days: int = 180):
    """Mock OHLCV 데이터 생성."""
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
    price = base_price
    for i in range(days):
        # Dates are "newest first"
        current_date = start_date - timedelta(days=i)
        dates.append(current_date.strftime("%Y-%m-%d"))

        # 가격 변동 시뮬레이션
        change = random.uniform(-0.03, 0.03)
        price = price * (1 + change)

        open_price = int(price * random.uniform(0.99, 1.01))
        close_price = int(price * random.uniform(0.99, 1.01))
        high_price = int(max(open_price, close_price) * random.uniform(1.0, 1.02))
        low_price = int(min(open_price, close_price) * random.uniform(0.98, 1.0))
        volume = random.randint(10_000_000, 30_000_000)

        opens.append(open_price)
        highs.append(high_price)
        lows.append(low_price)
        closes.append(close_price)
        volumes.append(volume)

    return {
        "ticker": "005930",
        "name": "삼성전자",
        "dates": dates,
        "open": opens,
        "high": highs,
        "low": lows,
        "close": closes,
        "volume": volumes,
    }


def test_trend_with_mock(show: bool = False, save: bool = False):
    """Mock 데이터로 Trend Signal 테스트."""
    from stock_analyzer.indicator import trend
    from stock_analyzer.chart import line

    print("=" * 60)
    print("Trend Signal 테스트 (Mock 데이터)")
    print("=" * 60)

    # 1. Mock OHLCV 데이터 생성
    print("\n[1] Mock OHLCV 데이터 생성...")
    mock_ohlcv = generate_mock_ohlcv(180)
    print(f"    - 종목: {mock_ohlcv['ticker']} ({mock_ohlcv['name']})")
    print(f"    - 기간: {mock_ohlcv['dates'][-1]} ~ {mock_ohlcv['dates'][0]}")
    print(f"    - 데이터 수: {len(mock_ohlcv['dates'])}일")

    # 2. Trend Signal 계산
    print("\n[2] Trend Signal 계산...")
    trend_result = trend.calc_from_ohlcv(
        ticker=mock_ohlcv["ticker"],
        dates=mock_ohlcv["dates"],
        closes=mock_ohlcv["close"],
        highs=mock_ohlcv["high"],
        lows=mock_ohlcv["low"],
        volumes=mock_ohlcv["volume"],
    )

    if not trend_result["ok"]:
        print(f"    오류: {trend_result['error']}")
        return

    data = trend_result["data"]
    # Data is "newest first", so index 0 is the most recent
    print(f"    - MA Signal 최신값: {data['ma_signal'][0]}")
    print(f"    - CMF 최신값: {data['cmf'][0]:.4f}")
    print(f"    - Fear/Greed 최신값: {data['fear_greed'][0]}")
    print(f"    - Trend 최신값: {data['trend'][0]}")

    # 3. 차트 생성
    print("\n[3] 차트 생성...")
    save_path = None
    if save:
        save_path = str(Path(__file__).parent.parent / "output" / "trend_chart.png")
        os.makedirs(os.path.dirname(save_path), exist_ok=True)

    # chart functions expect data dict directly, not wrapped result
    chart_result = line.plot_trend(data, save_path=save_path)

    if chart_result["ok"]:
        print(f"    - 이미지 크기: {len(chart_result['data']['image_bytes']):,} bytes")
        if save_path:
            print(f"    - 저장 경로: {save_path}")

        if show:
            _show_chart(chart_result["data"]["image_bytes"])
    else:
        print(f"    차트 생성 오류: {chart_result['error']}")

    print("\n" + "=" * 60)
    print("Trend Signal 테스트 완료!")
    print("=" * 60)


def test_elder_with_mock(show: bool = False, save: bool = False):
    """Mock 데이터로 Elder Impulse 테스트."""
    from stock_analyzer.indicator import elder
    from stock_analyzer.chart import line

    print("=" * 60)
    print("Elder Impulse 테스트 (Mock 데이터)")
    print("=" * 60)

    # 1. Mock OHLCV 데이터 생성
    print("\n[1] Mock OHLCV 데이터 생성...")
    mock_ohlcv = generate_mock_ohlcv(180)
    print(f"    - 종목: {mock_ohlcv['ticker']} ({mock_ohlcv['name']})")
    print(f"    - 기간: {mock_ohlcv['dates'][-1]} ~ {mock_ohlcv['dates'][0]}")
    print(f"    - 데이터 수: {len(mock_ohlcv['dates'])}일")

    # 2. Elder Impulse 계산
    print("\n[2] Elder Impulse 계산...")
    elder_result = elder.calc_from_ohlcv(
        ticker=mock_ohlcv["ticker"],
        dates=mock_ohlcv["dates"],
        closes=mock_ohlcv["close"],
    )

    if not elder_result["ok"]:
        print(f"    오류: {elder_result['error']}")
        return

    data = elder_result["data"]
    # Data is "newest first", so index 0 is the most recent
    print(f"    - Color 최신값: {data['color'][0]}")
    ema13_val = data['ema13'][0]
    macd_hist_val = data['macd_hist'][0]
    print(f"    - EMA13 최신값: {ema13_val:.2f}" if ema13_val is not None else "    - EMA13 최신값: N/A")
    print(f"    - MACD Hist 최신값: {macd_hist_val:.4f}" if macd_hist_val is not None else "    - MACD Hist 최신값: N/A")
    print(f"    - EMA13 Dir 최신값: {data['ema13_dir'][0]}")

    # 색상 통계
    colors = data["color"]
    green_count = colors.count("green")
    red_count = colors.count("red")
    blue_count = colors.count("blue")
    print(f"    - 색상 분포: Green={green_count}, Red={red_count}, Blue={blue_count}")

    # 3. 차트 생성
    print("\n[3] 차트 생성...")
    save_path = None
    if save:
        save_path = str(Path(__file__).parent.parent / "output" / "elder_chart.png")
        os.makedirs(os.path.dirname(save_path), exist_ok=True)

    # chart functions expect data dict directly, not wrapped result
    chart_result = line.plot_elder(data, save_path=save_path)

    if chart_result["ok"]:
        print(f"    - 이미지 크기: {len(chart_result['data']['image_bytes']):,} bytes")
        if save_path:
            print(f"    - 저장 경로: {save_path}")

        if show:
            _show_chart(chart_result["data"]["image_bytes"])
    else:
        print(f"    차트 생성 오류: {chart_result['error']}")

    print("\n" + "=" * 60)
    print("Elder Impulse 테스트 완료!")
    print("=" * 60)


def test_demark_with_mock(show: bool = False, save: bool = False):
    """Mock 데이터로 DeMark TD 테스트."""
    from stock_analyzer.indicator import demark
    from stock_analyzer.chart import bar

    print("=" * 60)
    print("DeMark TD 테스트 (Mock 데이터)")
    print("=" * 60)

    # 1. Mock OHLCV 데이터 생성
    print("\n[1] Mock OHLCV 데이터 생성...")
    mock_ohlcv = generate_mock_ohlcv(180)
    print(f"    - 종목: {mock_ohlcv['ticker']} ({mock_ohlcv['name']})")
    print(f"    - 기간: {mock_ohlcv['dates'][-1]} ~ {mock_ohlcv['dates'][0]}")
    print(f"    - 데이터 수: {len(mock_ohlcv['dates'])}일")

    # 2. DeMark TD 계산
    print("\n[2] DeMark TD 계산...")
    demark_result = demark.calc_from_ohlcv(
        ticker=mock_ohlcv["ticker"],
        dates=mock_ohlcv["dates"],
        closes=mock_ohlcv["close"],
        highs=mock_ohlcv["high"],
        lows=mock_ohlcv["low"],
    )

    if not demark_result["ok"]:
        print(f"    오류: {demark_result['error']}")
        return

    data = demark_result["data"]
    # Data is "newest first", so index 0 is the most recent
    print(f"    - Setup Count 최신값: {data['setup_count'][0]}")
    print(f"    - Setup Type 최신값: {data['setup_type'][0]}")
    print(f"    - Setup Complete 최신값: {data['setup_complete'][0]}")
    print(f"    - Perfected 최신값: {data['perfected'][0]}")

    # Setup 통계
    setup_types = data["setup_type"]
    buy_count = setup_types.count("buy")
    sell_count = setup_types.count("sell")
    none_count = setup_types.count("none")
    complete_count = sum(1 for c in data["setup_complete"] if c)
    print(f"    - Setup 분포: Buy={buy_count}, Sell={sell_count}, None={none_count}")
    print(f"    - 완성된 Setup: {complete_count}개")

    # 3. 차트 생성
    print("\n[3] 차트 생성...")
    save_path = None
    if save:
        save_path = str(Path(__file__).parent.parent / "output" / "demark_chart.png")
        os.makedirs(os.path.dirname(save_path), exist_ok=True)

    # chart functions expect data dict directly, not wrapped result
    chart_result = bar.plot_demark(data, save_path=save_path)

    if chart_result["ok"]:
        print(f"    - 이미지 크기: {len(chart_result['data']['image_bytes']):,} bytes")
        if save_path:
            print(f"    - 저장 경로: {save_path}")

        if show:
            _show_chart(chart_result["data"]["image_bytes"])
    else:
        print(f"    차트 생성 오류: {chart_result['error']}")

    print("\n" + "=" * 60)
    print("DeMark TD 테스트 완료!")
    print("=" * 60)


def test_with_api(ticker: str, indicator: str = "all", show: bool = False, save: bool = False):
    """실제 API로 지표 테스트."""
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
    from stock_analyzer.chart import line, bar

    print("=" * 60)
    print(f"기술적 지표 테스트 (실제 API) - {ticker}")
    print("=" * 60)

    # 1. 클라이언트 생성
    print("\n[1] API 클라이언트 생성...")
    client = KiwoomClient(app_key, secret_key, base_url)

    # 2. Trend Signal
    if indicator in ("all", "trend"):
        print("\n" + "-" * 40)
        print("[Trend Signal]")
        print("-" * 40)
        trend_result = trend.calc(client, ticker, days=180)

        if trend_result["ok"]:
            data = trend_result["data"]
            print(f"    종목: {data['ticker']} ({data.get('name', '')})")
            print(f"    기간: {data['dates'][-1]} ~ {data['dates'][0]}")
            # Data is "newest first", so index 0 is the most recent
            print(f"    MA Signal: {data['ma_signal'][0]}")
            print(f"    CMF: {data['cmf'][0]:.4f}")
            print(f"    Fear/Greed: {data['fear_greed'][0]}")
            print(f"    Trend: {data['trend'][0]}")

            save_path = None
            if save:
                save_path = str(Path(__file__).parent.parent / "output" / f"trend_{ticker}.png")
                os.makedirs(os.path.dirname(save_path), exist_ok=True)

            # chart functions expect data dict directly, not wrapped result
            chart_result = line.plot_trend(data, save_path=save_path)
            if chart_result["ok"]:
                print(f"    차트 크기: {len(chart_result['data']['image_bytes']):,} bytes")
                if save_path:
                    print(f"    저장: {save_path}")
                if show:
                    _show_chart(chart_result["data"]["image_bytes"])
        else:
            print(f"    오류: {trend_result['error']}")

    # 3. Elder Impulse
    if indicator in ("all", "elder"):
        print("\n" + "-" * 40)
        print("[Elder Impulse]")
        print("-" * 40)
        elder_result = elder.calc(client, ticker, days=180)

        if elder_result["ok"]:
            data = elder_result["data"]
            print(f"    종목: {data['ticker']} ({data.get('name', '')})")
            print(f"    기간: {data['dates'][-1]} ~ {data['dates'][0]}")
            # Data is "newest first", so index 0 is the most recent
            print(f"    Color: {data['color'][0]}")
            ema13_val = data['ema13'][0]
            macd_hist_val = data['macd_hist'][0]
            print(f"    EMA13: {ema13_val:.2f}" if ema13_val is not None else "    EMA13: N/A")
            print(f"    MACD Hist: {macd_hist_val:.4f}" if macd_hist_val is not None else "    MACD Hist: N/A")
            print(f"    EMA13 Dir: {data['ema13_dir'][0]}")

            save_path = None
            if save:
                save_path = str(Path(__file__).parent.parent / "output" / f"elder_{ticker}.png")
                os.makedirs(os.path.dirname(save_path), exist_ok=True)

            # chart functions expect data dict directly, not wrapped result
            chart_result = line.plot_elder(data, save_path=save_path)
            if chart_result["ok"]:
                print(f"    차트 크기: {len(chart_result['data']['image_bytes']):,} bytes")
                if save_path:
                    print(f"    저장: {save_path}")
                if show:
                    _show_chart(chart_result["data"]["image_bytes"])
        else:
            print(f"    오류: {elder_result['error']}")

    # 4. DeMark TD
    if indicator in ("all", "demark"):
        print("\n" + "-" * 40)
        print("[DeMark TD]")
        print("-" * 40)
        demark_result = demark.calc(client, ticker, days=180)

        if demark_result["ok"]:
            data = demark_result["data"]
            print(f"    종목: {data['ticker']} ({data.get('name', '')})")
            print(f"    기간: {data['dates'][-1]} ~ {data['dates'][0]}")
            # Data is "newest first", so index 0 is the most recent
            print(f"    Setup Count: {data['setup_count'][0]}")
            print(f"    Setup Type: {data['setup_type'][0]}")
            print(f"    Setup Complete: {data['setup_complete'][0]}")
            print(f"    Perfected: {data['perfected'][0]}")

            # Active Setups
            active_setups = demark.get_active_setups(
                setup_count=data["setup_count"],
                setup_type=data["setup_type"],
                setup_complete=data["setup_complete"],
                dates=data["dates"],
            )
            if active_setups.get("active_setups"):
                print(f"    Active Setups:")
                for setup in active_setups["active_setups"][:3]:
                    print(f"      - {setup['date']}: {setup['type']} ({setup['count']})")

            save_path = None
            if save:
                save_path = str(Path(__file__).parent.parent / "output" / f"demark_{ticker}.png")
                os.makedirs(os.path.dirname(save_path), exist_ok=True)

            # chart functions expect data dict directly, not wrapped result
            chart_result = bar.plot_demark(data, save_path=save_path)
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
    parser = argparse.ArgumentParser(description="기술적 지표 테스트")
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
        print("사용법:")
        print("  python scripts/test_indicators.py --mock                    # 전체 Mock 테스트")
        print("  python scripts/test_indicators.py --mock --indicator trend  # Trend Signal만")
        print("  python scripts/test_indicators.py --mock --indicator elder  # Elder Impulse만")
        print("  python scripts/test_indicators.py --mock --indicator demark # DeMark TD만")
        print("  python scripts/test_indicators.py --mock --show             # 차트 표시")
        print("  python scripts/test_indicators.py --mock --save             # 차트 저장")
        print("  python scripts/test_indicators.py --ticker 005930           # 실제 API")
        print("  python scripts/test_indicators.py --ticker 005930 --show    # 실제 API + 차트 표시")


if __name__ == "__main__":
    main()
