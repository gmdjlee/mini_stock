#!/usr/bin/env python3
"""Sample script to run stock analysis."""

import sys
from pathlib import Path

# Add src to path for development
sys.path.insert(0, str(Path(__file__).parent.parent / "src"))

from stock_analyzer.client.kiwoom import KiwoomClient
from stock_analyzer.config import Config
from stock_analyzer.stock import search as search_stock, get_info
from stock_analyzer.stock import analyze
from stock_analyzer.stock import get_daily


def main():
    """Run sample analysis."""
    # Load config from .env
    try:
        config = Config.from_env()
    except Exception as e:
        print(f"Error loading config: {e}")
        print("Please create .env file with KIWOOM_APP_KEY and KIWOOM_SECRET_KEY")
        return

    # Create client
    client = KiwoomClient(
        app_key=config.app_key,
        secret_key=config.secret_key,
        base_url=config.base_url,
    )

    # Example: Search for Samsung
    print("=" * 50)
    print("Searching for '삼성전자'...")
    result = search_stock(client, "삼성전자")
    if result["ok"]:
        for stock in result["data"][:5]:
            print(f"  {stock['ticker']} - {stock['name']} ({stock['market']})")
    else:
        print(f"Error: {result['error']}")

    # Example: Get stock info
    print("\n" + "=" * 50)
    print("Getting stock info for 005930...")
    result = get_info(client, "005930")
    if result["ok"]:
        data = result["data"]
        print(f"  Name: {data['name']}")
        print(f"  Price: {data['price']:,}")
        print(f"  Market Cap: {data['mcap']:,}")
        print(f"  PER: {data['per']}")
        print(f"  PBR: {data['pbr']}")
    else:
        print(f"Error: {result['error']}")

    # Example: Analyze supply/demand
    print("\n" + "=" * 50)
    print("Analyzing supply/demand for 005930...")
    result = analyze(client, "005930", days=10)
    if result["ok"]:
        data = result["data"]
        print(f"  Ticker: {data['ticker']}")
        print(f"  Name: {data['name']}")
        print(f"  Latest dates: {data['dates'][:3]}")
        print(f"  Foreign 5d net: {data['for_5d'][:3]}")
        print(f"  Institution 5d net: {data['ins_5d'][:3]}")
    else:
        print(f"Error: {result['error']}")

    # Example: Get OHLCV data
    print("\n" + "=" * 50)
    print("Getting daily OHLCV for 005930 (last 10 days)...")
    result = get_daily(client, "005930", days=10)
    if result["ok"]:
        data = result["data"]
        print(f"  Ticker: {data['ticker']}")
        print(f"  Dates: {data['dates'][:3]}")
        print(f"  Close prices: {data['close'][:3]}")
        print(f"  Volumes: {data['volume'][:3]}")
    else:
        print(f"Error: {result['error']}")

    print("\n" + "=" * 50)
    print("Done!")


if __name__ == "__main__":
    main()
