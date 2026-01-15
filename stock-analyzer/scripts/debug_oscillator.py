#!/usr/bin/env python
"""
오실레이터 데이터 플로우 디버깅 스크립트.

API에서 받아온 데이터가 올바르게 처리되는지 확인합니다.
"""

import os
import sys
from pathlib import Path

sys.path.insert(0, str(Path(__file__).parent.parent / "src"))

from dotenv import load_dotenv
load_dotenv()


def debug_api_data(ticker: str = "005930"):
    """API 데이터 디버깅."""
    app_key = os.getenv("KIWOOM_APP_KEY")
    secret_key = os.getenv("KIWOOM_SECRET_KEY")
    base_url = os.getenv("KIWOOM_BASE_URL", "https://api.kiwoom.com")

    if not app_key or not secret_key:
        print("Error: API keys not set in .env")
        return

    from stock_analyzer.client.kiwoom import KiwoomClient
    from stock_analyzer.stock import analysis
    from stock_analyzer.indicator import oscillator

    print("=" * 70)
    print(f"데이터 플로우 디버깅 - {ticker}")
    print("=" * 70)

    client = KiwoomClient(app_key, secret_key, base_url)

    # 1. Raw API Response (stock info)
    print("\n[1] Stock Info API 응답:")
    info_resp = client.get_stock_info(ticker)
    if info_resp.ok:
        print(f"    종목명: {info_resp.data.get('stk_nm')}")
        print(f"    시가총액(mac): {info_resp.data.get('mac'):,}")
    else:
        print(f"    Error: {info_resp.error}")

    # 2. Raw API Response (investor trend)
    print("\n[2] Investor Trend API 응답 (최근 5일):")
    trend_resp = client.get_investor_trend(ticker)
    if trend_resp.ok:
        trend_data = trend_resp.data.get("stk_invsr_orgn", [])
        print(f"    총 데이터 수: {len(trend_data)}일")
        print("\n    최근 5일 데이터:")
        print("    " + "-" * 60)
        print(f"    {'날짜':^12} | {'시가총액(조)':^15} | {'외국인':^15} | {'기관':^15}")
        print("    " + "-" * 60)
        for item in trend_data[:5]:
            dt = item.get("dt", "")
            mcap = item.get("mrkt_tot_amt", 0)
            foreign = item.get("frgnr_invsr", 0)
            inst = item.get("orgn", 0)
            print(f"    {dt:^12} | {mcap/1e12:>13.2f}조 | {foreign/1e8:>13.1f}억 | {inst/1e8:>13.1f}억")
    else:
        print(f"    Error: {trend_resp.error}")

    # 3. Analyzed Data
    print("\n[3] analysis.analyze() 결과 (최근 5일):")
    analysis_result = analysis.analyze(client, ticker, days=60)
    if analysis_result["ok"]:
        data = analysis_result["data"]
        print(f"    종목: {data['ticker']} ({data['name']})")
        print(f"    데이터 수: {len(data['dates'])}일")
        print("\n    최근 5일:")
        print("    " + "-" * 75)
        print(f"    {'날짜':^12} | {'시가총액(조)':^12} | {'외인5D(억)':^12} | {'기관5D(억)':^12} | {'수급비율':^12}")
        print("    " + "-" * 75)
        for i in range(min(5, len(data['dates']))):
            dt = data['dates'][i]
            mcap = data['mcap'][i]
            for_5d = data['for_5d'][i]
            ins_5d = data['ins_5d'][i]
            supply = for_5d + ins_5d
            ratio = supply / mcap if mcap > 0 else 0
            print(f"    {dt:^12} | {mcap/1e12:>10.2f}조 | {for_5d/1e8:>10.1f}억 | {ins_5d/1e8:>10.1f}억 | {ratio*100:>10.4f}%")
    else:
        print(f"    Error: {analysis_result['error']}")

    # 4. Oscillator Calculation
    print("\n[4] oscillator.calc() 결과:")
    osc_result = oscillator.calc(client, ticker, days=60)
    if osc_result["ok"]:
        osc_data = osc_result["data"]
        print(f"    데이터 수: {len(osc_data['dates'])}일")
        print("\n    최근 10일 오실레이터 값:")
        print("    " + "-" * 90)
        print(f"    {'날짜':^12} | {'시총(조)':^10} | {'수급비율(%)':^12} | {'MACD':^12} | {'Signal':^12} | {'Osc':^12}")
        print("    " + "-" * 90)
        for i in range(min(10, len(osc_data['dates']))):
            dt = osc_data['dates'][i]
            mcap = osc_data['market_cap'][i]
            sr = osc_data['supply_ratio'][i]
            macd = osc_data['macd'][i]
            signal = osc_data['signal'][i]
            osc = osc_data['oscillator'][i]
            print(f"    {dt:^12} | {mcap:>8.1f}조 | {sr*100:>10.4f}% | {macd*100:>10.4f}% | {signal*100:>10.4f}% | {osc*100:>10.4f}%")

        # Signal Analysis
        print("\n[5] 신호 분석:")
        signal_result = oscillator.analyze_signal(osc_result)
        if signal_result["ok"]:
            sig = signal_result["data"]
            print(f"    총 점수: {sig['total_score']}")
            print(f"    신호 유형: {sig['signal_type']}")
            print(f"    - 오실레이터 점수: {sig['oscillator_score']}")
            print(f"    - 크로스 점수: {sig['cross_score']}")
            print(f"    - 추세 점수: {sig['trend_score']}")
            print(f"    설명: {sig['description']}")

            # Check thresholds
            latest_osc = osc_data['oscillator'][-1]
            print(f"\n    [임계값 체크]")
            print(f"    최신 오실레이터 값: {latest_osc*100:.4f}% ({latest_osc:.6f})")
            print(f"    - > 0.5% (+40점): {latest_osc > 0.005}")
            print(f"    - > 0.2% (+20점): {latest_osc > 0.002}")
            print(f"    - < -0.2% (-20점): {latest_osc < -0.002}")
            print(f"    - < -0.5% (-40점): {latest_osc < -0.005}")
    else:
        print(f"    Error: {osc_result['error']}")

    print("\n" + "=" * 70)


if __name__ == "__main__":
    import argparse
    parser = argparse.ArgumentParser()
    parser.add_argument("--ticker", default="005930", help="종목코드")
    args = parser.parse_args()

    debug_api_data(args.ticker)
