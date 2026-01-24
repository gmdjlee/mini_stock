# ETF Collector

Active ETF constituent stock collector using **KIS API** (Korea Investment & Securities).

## Important Note

> **This project uses KIS API (한국투자증권 Open API), NOT Kiwoom API (키움증권 API).**
>
> The two are different API providers with different authentication mechanisms and endpoints.
> - KIS API Portal: https://apiportal.koreainvestment.com
> - Official GitHub: https://github.com/koreainvestment/open-trading-api

## Features

- Collect Active ETF list from predefined codes
- Fetch constituent stocks for each ETF
- Support keyword-based filtering (include/exclude)
- Rate limiting to comply with API restrictions
- Export to CSV or JSON format

## Installation

```bash
cd etf-collector
uv sync --all-extras
```

## Configuration

Create a `.env` file in the project root:

```bash
cp .env.example .env
```

Edit `.env` with your KIS API credentials:

```env
KIS_APP_KEY=your_app_key
KIS_APP_SECRET=your_app_secret
KIS_ACCOUNT_NO=your_account_number  # Optional
KIS_ENVIRONMENT=real  # or "virtual" for paper trading
```

## Usage

### Collect Active ETFs and their constituents

```bash
# Collect all active ETFs with JSON output
uv run python -m etf_collector collect --active-only --format json

# Collect with specific keyword filter
uv run python -m etf_collector collect --include "반도체,AI" --format json

# Exclude leverage/inverse ETFs
uv run python -m etf_collector collect --exclude "레버리지,인버스,2X"

# Collect ETF list only (without constituents)
uv run python -m etf_collector collect --active-only --etf-list-only

# Collect specific ETF constituents
uv run python -m etf_collector collect --etf-code 069500
```

### Show configuration

```bash
uv run python -m etf_collector config --show
```

### Test rate limiter

```bash
uv run python -m etf_collector test-rate-limit --env real --duration 10
```

## API Limitation

KIS API's product search endpoint (`CTPF1604R`) does not support bulk ETF listing
with an empty product code (PDNO). Therefore, this collector uses a predefined
list of Active ETF codes maintained in `src/etf_collector/data/active_etf_codes.py`.

To add new Active ETFs, update the `ACTIVE_ETF_CODES` list in that file.

## Project Structure

```
etf-collector/
├── src/etf_collector/
│   ├── auth/           # KIS OAuth authentication
│   ├── collector/      # ETF list and constituent collectors
│   ├── config.py       # Configuration management
│   ├── data/           # Predefined ETF codes
│   ├── filter/         # Keyword-based filtering
│   ├── limiter/        # Rate limiting
│   ├── storage/        # Data storage (CSV/JSON)
│   └── utils/          # Logging and helpers
├── tests/              # Unit and integration tests
└── pyproject.toml
```

## Running Tests

```bash
uv run pytest tests/unit/ -v
```

## License

MIT
