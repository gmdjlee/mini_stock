"""CLI entry point for ETF Collector.

Supports two API providers:
- KIS API: For ETF constituent data
- Kiwoom API: For fetching complete ETF list dynamically
"""

import argparse
import sys
import time

from .auth.kis_auth import KisAuthClient, AuthError
from .auth.kiwoom_auth import KiwoomAuthClient, KiwoomAuthError
from .collector.constituent import ConstituentCollector
from .collector.etf_list import EtfListCollector
from .collector.kiwoom_etf_list import KiwoomEtfListCollector, MarketType
from .config import Config, ConfigError, EtfListSource
from .filter.keyword import (
    ACTIVE_ETF_FILTER,
    EXCLUDE_LEVERAGE_FILTER,
    create_filter_from_args,
)
from .limiter.rate_limiter import SlidingWindowRateLimiter, RateLimiterConfig
from .storage.data_storage import DataStorage, OutputFormat
from .utils.logger import log_info, log_err, log_warn, set_level


def main():
    """Main entry point."""
    parser = argparse.ArgumentParser(
        description="ETF Collector - Collect ETF data using Kiwoom and KIS APIs",
        formatter_class=argparse.RawDescriptionHelpFormatter,
        epilog="""
Examples:
  # Collect all ETFs from Kiwoom API and their constituents from KIS API
  python -m etf_collector collect --output ./data/all_etfs.csv

  # Collect only active ETFs
  python -m etf_collector collect --active-only --format json

  # Use Kiwoom API for ETF list (default if KIWOOM_* env vars are set)
  python -m etf_collector collect --etf-source kiwoom --active-only

  # Use predefined ETF list (fallback)
  python -m etf_collector collect --etf-source predefined --active-only

  # Collect with keyword filter
  python -m etf_collector collect --include "반도체,AI" --format json

  # Exclude leverage/inverse ETFs
  python -m etf_collector collect --exclude "레버리지,인버스,2X"

  # Collect only ETF list (no constituents)
  python -m etf_collector collect --etf-list-only --format json

  # Specify market type (KOSPI, KOSDAQ, ALL)
  python -m etf_collector collect --market all --active-only

  # Test rate limiter
  python -m etf_collector test-rate-limit --env real --duration 10

  # Show configuration
  python -m etf_collector config --show
""",
    )

    parser.add_argument(
        "-v", "--verbose",
        action="store_true",
        help="Enable verbose output (DEBUG level)",
    )

    subparsers = parser.add_subparsers(dest="command", required=True)

    # collect command
    collect_parser = subparsers.add_parser(
        "collect",
        help="Collect ETF data",
        description="Collect ETF list and constituent stocks",
    )
    collect_parser.add_argument(
        "-o", "--output",
        default="./data/etf_data",
        help="Output path (without extension, default: ./data/etf_data)",
    )
    collect_parser.add_argument(
        "--etf-source",
        choices=["kiwoom", "predefined"],
        default=None,
        help="Source for ETF list: 'kiwoom' (API) or 'predefined' (hardcoded list)",
    )
    collect_parser.add_argument(
        "--market",
        choices=["all", "kospi", "kosdaq", "krx"],
        default="all",
        help="Market filter for Kiwoom API (default: all)",
    )
    collect_parser.add_argument(
        "--kiwoom-env",
        choices=["real", "mock"],
        default=None,
        help="Kiwoom API environment (overrides KIWOOM_ENVIRONMENT)",
    )
    collect_parser.add_argument(
        "--include",
        help="Comma-separated keywords to include (ETF name must contain at least one)",
    )
    collect_parser.add_argument(
        "--exclude",
        help="Comma-separated keywords to exclude (ETF name must not contain any)",
    )
    collect_parser.add_argument(
        "--active-only",
        action="store_true",
        help="Filter only active ETFs (contains '액티브' or 'Active')",
    )
    collect_parser.add_argument(
        "--no-leverage",
        action="store_true",
        help="Exclude leverage/inverse ETFs",
    )
    collect_parser.add_argument(
        "--format",
        choices=["csv", "json"],
        default="csv",
        help="Output format (default: csv)",
    )
    collect_parser.add_argument(
        "--etf-list-only",
        action="store_true",
        help="Only collect ETF list without constituents",
    )
    collect_parser.add_argument(
        "--etf-code",
        help="Collect constituents for a specific ETF code only",
    )
    collect_parser.add_argument(
        "--env-file",
        help="Path to .env file",
    )

    # test-rate-limit command
    rate_limit_parser = subparsers.add_parser(
        "test-rate-limit",
        help="Test rate limiter",
    )
    rate_limit_parser.add_argument(
        "--env",
        choices=["real", "virtual"],
        default="real",
        help="Environment (real: 15/sec, virtual: 4/sec)",
    )
    rate_limit_parser.add_argument(
        "--duration",
        type=int,
        default=10,
        help="Test duration in seconds (default: 10)",
    )

    # config command
    config_parser = subparsers.add_parser(
        "config",
        help="Show configuration",
    )
    config_parser.add_argument(
        "--show",
        action="store_true",
        help="Show current configuration",
    )
    config_parser.add_argument(
        "--env-file",
        help="Path to .env file",
    )

    args = parser.parse_args()

    # Set logging level
    if args.verbose:
        set_level("DEBUG")

    # Execute command
    if args.command == "collect":
        return run_collect(args)
    elif args.command == "test-rate-limit":
        return run_test_rate_limit(args)
    elif args.command == "config":
        return run_config(args)

    return 0


def run_collect(args) -> int:
    """Run collect command.

    Args:
        args: Parsed arguments

    Returns:
        Exit code
    """
    try:
        # Load configuration
        config = Config.from_env(args.env_file)
        log_info("cli", "Configuration loaded", {"env": config.environment})

        # Override Kiwoom environment if specified
        if args.kiwoom_env:
            config.kiwoom_environment = args.kiwoom_env

        # Determine ETF list source
        etf_source = config.etf_list_source
        if args.etf_source:
            etf_source = (
                EtfListSource.KIWOOM
                if args.etf_source == "kiwoom"
                else EtfListSource.PREDEFINED
            )

        # Initialize KIS components (for constituent data)
        kis_auth_client = KisAuthClient(
            config.app_key, config.app_secret, config.base_url
        )
        min_interval = 0.5  # Prevent server-side rate limiting
        kis_rate_limiter = SlidingWindowRateLimiter(
            RateLimiterConfig(
                requests_per_second=float(config.rate_limit),
                min_interval=min_interval,
            )
        )
        storage = DataStorage(output_dir="./data")

        # Build filter
        include_keywords = args.include.split(",") if args.include else None
        exclude_keywords = args.exclude.split(",") if args.exclude else None

        combined_filter = create_filter_from_args(
            include=include_keywords,
            exclude=exclude_keywords,
            active_only=args.active_only,
        )

        # Add leverage filter if requested
        if args.no_leverage:
            combined_filter.filters.append(EXCLUDE_LEVERAGE_FILTER)

        output_format = OutputFormat.JSON if args.format == "json" else OutputFormat.CSV

        # Specific ETF code - use KIS constituent collector directly
        if args.etf_code:
            return collect_single_etf(
                args.etf_code,
                kis_auth_client,
                kis_rate_limiter,
                storage,
                output_format,
                args.output,
            )

        # Collect ETF list based on source
        if etf_source == EtfListSource.KIWOOM and config.has_kiwoom_credentials:
            # Use Kiwoom API for ETF list
            result = collect_etf_list_from_kiwoom(config, args)
            if not result.get("ok"):
                log_err("cli", f"Kiwoom API failed: {result.get('error', {})}")
                print(f"Kiwoom API failed: {result.get('error', {}).get('msg')}")
                print("Falling back to predefined ETF list...")
                result = collect_etf_list_from_predefined(
                    kis_auth_client, kis_rate_limiter, config
                )
            elif not result.get("data"):
                # Kiwoom API returned empty list (common in mock environment)
                log_warn("cli", "Kiwoom API returned empty ETF list")
                print("Kiwoom API returned 0 ETFs (mock API may not have data)")
                print("Falling back to predefined ETF list...")
                result = collect_etf_list_from_predefined(
                    kis_auth_client, kis_rate_limiter, config
                )
        else:
            # Use predefined ETF list
            if etf_source == EtfListSource.KIWOOM and not config.has_kiwoom_credentials:
                print("Warning: Kiwoom credentials not configured. Using predefined list.")
            result = collect_etf_list_from_predefined(
                kis_auth_client, kis_rate_limiter, config
            )

        if not result.get("ok"):
            log_err("cli", f"Failed to fetch ETF list: {result.get('error', {})}")
            return 1

        etfs = result["data"]
        etf_list_errors = result.get("errors")

        print(f"Fetched {len(etfs)} ETFs")
        if etf_list_errors:
            print(f"Warning: {len(etf_list_errors)} ETFs failed to fetch")

        # Apply filters
        if combined_filter.filters:
            filtered_etfs = combined_filter.filter_etfs(etfs)
            print(f"After filtering: {len(filtered_etfs)} ETFs")
            etfs = filtered_etfs

        if not etfs:
            print("No ETFs match the filter criteria")
            return 0

        # ETF list only - save and exit
        if args.etf_list_only:
            filepath = storage.save_etf_list(etfs, "etf_list", output_format)
            print(f"ETF list saved to: {filepath}")
            return 0

        # Collect constituents using KIS API
        constituent_collector = ConstituentCollector(
            kis_auth_client, kis_rate_limiter, config.base_url
        )

        def progress_callback(current: int, total: int, name: str):
            print(f"[{current}/{total}] Collecting constituents: {name}")

        result = constituent_collector.get_all_constituents(etfs, progress_callback)

        if not result.get("ok"):
            log_err("cli", f"Failed to collect constituents: {result.get('error', {})}")
            return 1

        summaries = result["data"]
        errors = result.get("errors")

        # Prepare filter info for report
        filter_info = None
        if combined_filter.filters:
            filter_info = {
                "etf_source": etf_source.value,
                "active_only": args.active_only,
                "include_keywords": include_keywords,
                "exclude_keywords": exclude_keywords,
                "no_leverage": args.no_leverage,
            }

        # Save report
        filepath = storage.save_full_report(
            summaries,
            filename="etf_report",
            output_format=output_format,
            filter_info=filter_info,
        )
        print(f"Report saved to: {filepath}")

        total_constituents = sum(len(s.constituents) for s in summaries)
        print(f"Total: {len(summaries)} ETFs, {total_constituents} constituents")

        if errors:
            print(f"Warning: {len(errors)} ETFs failed to collect")

        return 0

    except ConfigError as e:
        log_err("cli", f"Configuration error: {e}")
        print(f"Error: {e}")
        print("Please set KIS_APP_KEY and KIS_APP_SECRET environment variables")
        return 1
    except (AuthError, KiwoomAuthError) as e:
        log_err("cli", f"Authentication error: {e}")
        print(f"Authentication failed: {e}")
        return 1
    except Exception as e:
        log_err("cli", f"Unexpected error: {e}")
        print(f"Error: {e}")
        return 1


def collect_etf_list_from_kiwoom(config: Config, args) -> dict:
    """Collect ETF list from Kiwoom API.

    Args:
        config: Application configuration
        args: CLI arguments

    Returns:
        Result dictionary with ETF list
    """
    print("Fetching ETF list from Kiwoom API...")

    # Initialize Kiwoom components
    kiwoom_auth = KiwoomAuthClient(
        config.kiwoom_app_key,
        config.kiwoom_secret_key,
        config.kiwoom_base_url,
    )
    kiwoom_rate_limiter = SlidingWindowRateLimiter(
        RateLimiterConfig(
            requests_per_second=float(config.kiwoom_rate_limit),
            min_interval=0.5,
        )
    )

    # Determine market type
    market_map = {
        "all": MarketType.ALL,
        "kospi": MarketType.KOSPI,
        "kosdaq": MarketType.KOSDAQ,
        "krx": MarketType.KRX,
    }
    market_type = market_map.get(args.market, MarketType.ALL)

    kiwoom_etf_collector = KiwoomEtfListCollector(
        kiwoom_auth, kiwoom_rate_limiter, config.kiwoom_base_url
    )

    def etf_list_progress(current: int, total: int, name: str):
        print(f"[{current}/{total}] Processing ETF: {name}")

    return kiwoom_etf_collector.get_all_etfs(
        market_type=market_type,
        progress_callback=etf_list_progress,
    )


def collect_etf_list_from_predefined(
    auth_client: KisAuthClient,
    rate_limiter: SlidingWindowRateLimiter,
    config: Config,
) -> dict:
    """Collect ETF list from predefined codes.

    Args:
        auth_client: KIS auth client
        rate_limiter: Rate limiter
        config: Application configuration

    Returns:
        Result dictionary with ETF list
    """
    print("Using predefined Active ETF list...")

    etf_collector = EtfListCollector(auth_client, rate_limiter, config.base_url)

    def etf_list_progress(current: int, total: int, name: str):
        print(f"[{current}/{total}] Loading ETF: {name}")

    return etf_collector.get_all_etfs(progress_callback=etf_list_progress)


def collect_single_etf(
    etf_code: str,
    auth_client: KisAuthClient,
    rate_limiter: SlidingWindowRateLimiter,
    storage: DataStorage,
    output_format: OutputFormat,
    output_path: str,
) -> int:
    """Collect constituents for a single ETF.

    Args:
        etf_code: ETF ticker code
        auth_client: KIS auth client
        rate_limiter: Rate limiter
        storage: Data storage
        output_format: Output format
        output_path: Output path

    Returns:
        Exit code
    """
    config = Config.from_env()
    collector = ConstituentCollector(auth_client, rate_limiter, config.base_url)

    print(f"Fetching constituents for ETF: {etf_code}")
    result = collector.get_constituents(etf_code)

    if not result.get("ok"):
        log_err("cli", f"Failed: {result.get('error', {})}")
        print(f"Error: {result.get('error', {}).get('msg', 'Unknown error')}")
        return 1

    summary = result["data"]
    print(f"Fetched {len(summary.constituents)} constituents")

    # Save
    filepath = storage.save_full_report(
        [summary],
        filename=f"etf_{etf_code}",
        output_format=output_format,
    )
    print(f"Saved to: {filepath}")

    return 0


def run_test_rate_limit(args) -> int:
    """Run rate limit test.

    Args:
        args: Parsed arguments

    Returns:
        Exit code
    """
    env = args.env
    duration = args.duration

    rate = 15.0 if env == "real" else 4.0
    min_interval = 0.5  # Higher interval needed to avoid 500 errors
    print(f"Testing rate limiter: {rate} requests/second, min_interval={min_interval}s for {duration} seconds")

    limiter = SlidingWindowRateLimiter(RateLimiterConfig(
        requests_per_second=rate,
        min_interval=min_interval,
    ))

    request_count = 0
    start_time = time.time()

    while time.time() - start_time < duration:
        limiter.wait_if_needed()
        request_count += 1
        elapsed = time.time() - start_time
        current_rate = request_count / elapsed if elapsed > 0 else 0
        print(f"Request {request_count}: elapsed={elapsed:.2f}s, rate={current_rate:.2f}/s")

    total_time = time.time() - start_time
    actual_rate = request_count / total_time

    print(f"\nResults:")
    print(f"  Total requests: {request_count}")
    print(f"  Total time: {total_time:.2f}s")
    print(f"  Actual rate: {actual_rate:.2f} requests/second")
    print(f"  Target rate: {rate} requests/second")

    return 0


def run_config(args) -> int:
    """Run config command.

    Args:
        args: Parsed arguments

    Returns:
        Exit code
    """
    if not args.show:
        print("Use --show to display configuration")
        return 0

    try:
        config = Config.from_env(args.env_file)

        print("=" * 50)
        print("ETF Collector Configuration")
        print("=" * 50)

        print("\n[KIS API - Constituent Data]")
        print(f"  Environment: {config.environment}")
        print(f"  Base URL: {config.base_url}")
        print(f"  Rate Limit: {config.rate_limit} requests/second")
        print(f"  App Key: {'*' * 8}...{config.app_key[-4:] if len(config.app_key) > 4 else '****'}")
        print(f"  Account No: {config.account_no or '(not set)'}")

        print("\n[Kiwoom API - ETF List]")
        if config.has_kiwoom_credentials:
            print(f"  Environment: {config.kiwoom_environment}")
            print(f"  Base URL: {config.kiwoom_base_url}")
            print(f"  Rate Limit: {config.kiwoom_rate_limit} requests/second")
            print(f"  App Key: {'*' * 8}...{config.kiwoom_app_key[-4:] if len(config.kiwoom_app_key) > 4 else '****'}")
        else:
            print("  Status: Not configured")
            print("  (Set KIWOOM_APP_KEY and KIWOOM_SECRET_KEY to enable)")

        print("\n[ETF List Source]")
        print(f"  Configured: {config.etf_list_source.value}")
        print(f"  Effective: {'kiwoom' if config.use_kiwoom_for_etf_list else 'predefined'}")

        print("=" * 50)

        return 0

    except ConfigError as e:
        print(f"Configuration error: {e}")
        print("\nRequired environment variables:")
        print("  KIS_APP_KEY=your_kis_app_key")
        print("  KIS_APP_SECRET=your_kis_app_secret")
        print("  KIS_ENVIRONMENT=real|virtual (optional, default: real)")
        print("\nOptional (for Kiwoom ETF list):")
        print("  KIWOOM_APP_KEY=your_kiwoom_app_key")
        print("  KIWOOM_SECRET_KEY=your_kiwoom_secret_key")
        print("  KIWOOM_ENVIRONMENT=real|mock (optional, default: real)")
        print("  ETF_LIST_SOURCE=kiwoom|predefined (optional, default: kiwoom)")
        return 1


if __name__ == "__main__":
    sys.exit(main())
