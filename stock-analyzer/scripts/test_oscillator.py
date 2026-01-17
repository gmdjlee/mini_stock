#!/usr/bin/env python
"""
오실레이터 기능 테스트 스크립트.

사용법:
    # Mock 데이터로 테스트 (API 키 불필요)
    python scripts/test_oscillator.py --mock

    # 실제 API로 테스트 (API 키 필요)
    python scripts/test_oscillator.py --ticker 005930

    # 차트 저장
    python scripts/test_oscillator.py --mock --save

    # 차트 표시 (GUI 환경)
    python scripts/test_oscillator.py --mock --show
"""

import argparse
import os
import sys
from pathlib import Path

# Add src to path
sys.path.insert(0, str(Path(__file__).parent.parent / "src"))


def generate_mock_data(days: int = 60):
    """Mock 수급 데이터 생성."""
    import random
    random.seed(42)  # 재현 가능한 결과

    base_mcap = 380_000_000_000_000  # 380조

    dates = []
    mcap = []
    for_5d = []
    ins_5d = []

    for i in range(days):
        # 날짜 생성 (역순)
        day = days - i
        dates.append(f"2025-01-{day:02d}" if day <= 31 else f"2024-12-{day-31:02d}")

        # 시가총액 (약간의 변동)
        mcap.append(base_mcap + random.randint(-5, 5) * 1_000_000_000_000)

        # 외국인/기관 순매수 (변동성 있는 패턴)
        cycle = (i % 20) / 20 * 3.14159 * 2
        import math
        for_base = int(5_000_000_000 * math.sin(cycle))  # 사인파
        ins_base = int(3_000_000_000 * math.cos(cycle + 1))  # 위상 차이

        for_5d.append(for_base + random.randint(-1_000_000_000, 1_000_000_000))
        ins_5d.append(ins_base + random.randint(-500_000_000, 500_000_000))

    return {
        "ticker": "005930",
        "name": "삼성전자",
        "dates": dates,
        "mcap": mcap,
        "for_5d": for_5d,
        "ins_5d": ins_5d,
    }


def test_with_mock(show: bool = False, save: bool = False):
    """Mock 데이터로 오실레이터 테스트."""
    from stock_analyzer.indicator import oscillator
    from stock_analyzer.chart import oscillator as osc_chart

    print("=" * 60)
    print("오실레이터 기능 테스트 (Mock 데이터)")
    print("=" * 60)

    # 1. Mock 데이터 생성
    print("\n[1] Mock 데이터 생성...")
    mock_data = generate_mock_data(60)
    print(f"    - 종목: {mock_data['ticker']} ({mock_data['name']})")
    print(f"    - 기간: {mock_data['dates'][-1]} ~ {mock_data['dates'][0]}")
    print(f"    - 데이터 수: {len(mock_data['dates'])}일")

    # 2. 오실레이터 계산
    print("\n[2] 오실레이터 계산...")
    osc_result = oscillator.calc_from_analysis(
        ticker=mock_data["ticker"],
        name=mock_data["name"],
        dates=mock_data["dates"],
        mcap=mock_data["mcap"],
        foreign_daily=mock_data["for_5d"],
        institution_daily=mock_data["ins_5d"],
    )

    if not osc_result["ok"]:
        print(f"    오류: {osc_result['error']}")
        return

    data = osc_result["data"]
    print(f"    - MACD 최신값: {data['macd'][-1]:.2e}")
    print(f"    - Signal 최신값: {data['signal'][-1]:.2e}")
    print(f"    - Oscillator 최신값: {data['oscillator'][-1]:.2e}")

    # 3. 신호 분석
    print("\n[3] 매매 신호 분석...")
    signal_result = oscillator.analyze_signal(osc_result)

    if signal_result["ok"]:
        sig = signal_result["data"]
        print(f"    - 총 점수: {sig['total_score']}")
        print(f"    - 신호 유형: {sig['signal_type']}")
        print(f"    - 오실레이터 점수: {sig['oscillator_score']}")
        print(f"    - 크로스 점수: {sig['cross_score']}")
        print(f"    - 추세 점수: {sig['trend_score']}")
        print(f"    - 설명: {sig['description']}")

    # 4. 차트 생성
    print("\n[4] 차트 생성...")
    save_path = None
    if save:
        save_path = str(Path(__file__).parent.parent / "output" / "oscillator_chart.png")
        os.makedirs(os.path.dirname(save_path), exist_ok=True)

    chart_result = osc_chart.plot_with_signal(
        data,
        signal_result,
        save_path=save_path,
    )

    if chart_result["ok"]:
        print(f"    - 이미지 크기: {len(chart_result['data']['image_bytes']):,} bytes")
        if save_path:
            print(f"    - 저장 경로: {save_path}")

        if show:
            # GUI에서 차트 표시
            try:
                import matplotlib
                matplotlib.use("TkAgg")  # 인터랙티브 백엔드로 전환
                import matplotlib.pyplot as plt
                from io import BytesIO
                from PIL import Image

                img = Image.open(BytesIO(chart_result["data"]["image_bytes"]))
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
    else:
        print(f"    차트 생성 오류: {chart_result['error']}")

    print("\n" + "=" * 60)
    print("테스트 완료!")
    print("=" * 60)


def test_with_api(ticker: str, show: bool = False, save: bool = False):
    """실제 API로 오실레이터 테스트."""
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
    from stock_analyzer.indicator import oscillator
    from stock_analyzer.chart import oscillator as osc_chart

    print("=" * 60)
    print(f"오실레이터 기능 테스트 (실제 API) - {ticker}")
    print("=" * 60)

    # 1. 클라이언트 생성
    print("\n[1] API 클라이언트 생성...")
    client = KiwoomClient(app_key, secret_key, base_url)

    # 2. 오실레이터 계산
    print("\n[2] 오실레이터 계산...")
    osc_result = oscillator.calc(client, ticker, days=180)

    if not osc_result["ok"]:
        print(f"    오류: {osc_result['error']}")
        return

    data = osc_result["data"]
    print(f"    - 종목: {data['ticker']} ({data['name']})")
    print(f"    - 기간: {data['dates'][-1]} ~ {data['dates'][0]}")
    print(f"    - 데이터 수: {len(data['dates'])}일")
    print(f"    - MACD 최신값: {data['macd'][-1]:.2e}")
    print(f"    - Signal 최신값: {data['signal'][-1]:.2e}")
    print(f"    - Oscillator 최신값: {data['oscillator'][-1]:.2e}")

    # 3. 신호 분석
    print("\n[3] 매매 신호 분석...")
    signal_result = oscillator.analyze_signal(osc_result)

    if signal_result["ok"]:
        sig = signal_result["data"]
        print(f"    - 총 점수: {sig['total_score']}")
        print(f"    - 신호 유형: {sig['signal_type']}")
        print(f"    - 설명: {sig['description']}")

    # 4. 차트 생성
    print("\n[4] 차트 생성...")
    save_path = None
    if save:
        save_path = str(Path(__file__).parent.parent / "output" / f"oscillator_{ticker}.png")
        os.makedirs(os.path.dirname(save_path), exist_ok=True)

    chart_result = osc_chart.plot_with_signal(
        data,
        signal_result,
        save_path=save_path,
    )

    if chart_result["ok"]:
        print(f"    - 이미지 크기: {len(chart_result['data']['image_bytes']):,} bytes")
        if save_path:
            print(f"    - 저장 경로: {save_path}")

        if show:
            try:
                import matplotlib
                matplotlib.use("TkAgg")  # 인터랙티브 백엔드로 전환
                import matplotlib.pyplot as plt
                from io import BytesIO
                from PIL import Image

                img = Image.open(BytesIO(chart_result["data"]["image_bytes"]))
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

    print("\n" + "=" * 60)
    print("테스트 완료!")
    print("=" * 60)


def main():
    parser = argparse.ArgumentParser(description="오실레이터 기능 테스트")
    parser.add_argument("--mock", action="store_true", help="Mock 데이터로 테스트")
    parser.add_argument("--ticker", type=str, help="종목코드 (실제 API 테스트)")
    parser.add_argument("--show", action="store_true", help="차트 화면에 표시")
    parser.add_argument("--save", action="store_true", help="차트 파일로 저장")

    args = parser.parse_args()

    if args.mock:
        test_with_mock(show=args.show, save=args.save)
    elif args.ticker:
        test_with_api(args.ticker, show=args.show, save=args.save)
    else:
        print("사용법:")
        print("  python scripts/test_oscillator.py --mock          # Mock 테스트")
        print("  python scripts/test_oscillator.py --mock --show   # 차트 표시")
        print("  python scripts/test_oscillator.py --mock --save   # 차트 저장")
        print("  python scripts/test_oscillator.py --ticker 005930 # 실제 API")


if __name__ == "__main__":
    main()
