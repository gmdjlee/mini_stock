# ETF Collector

Active ETF constituent stock collector using **KIS API** (Korea Investment & Securities) and **Kiwoom API**.

## Important Note

> **This project uses KIS API (한국투자증권 Open API) for constituent data and Kiwoom API (키움증권 API) for ETF list.**
>
> - KIS API Portal: https://apiportal.koreainvestment.com
> - Kiwoom API Portal: https://openapi.kiwoom.com

## Features

- Collect Active ETF list from Kiwoom API or predefined codes
- Fetch constituent stocks for each ETF via KIS API
- Support keyword-based filtering (include/exclude)
- Rate limiting to comply with API restrictions
- Export to CSV or JSON format
- **Android/Chaquopy integration API** (for StockApp)
- **Security features**: Path traversal protection, credential masking

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

Edit `.env` with your API credentials:

```env
# KIS API (Required for constituent data)
KIS_APP_KEY=your_kis_app_key
KIS_APP_SECRET=your_kis_app_secret
KIS_ACCOUNT_NO=your_account_number  # Optional
KIS_ENVIRONMENT=real  # or "virtual" for paper trading

# Kiwoom API (Optional for ETF list)
KIWOOM_APP_KEY=your_kiwoom_app_key
KIWOOM_SECRET_KEY=your_kiwoom_secret_key
KIWOOM_ENVIRONMENT=mock  # or "real"

# ETF List Source
ETF_LIST_SOURCE=kiwoom  # or "predefined"
```

## Usage

### CLI Commands

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

# Show configuration
uv run python -m etf_collector config --show

# Test rate limiter
uv run python -m etf_collector test-rate-limit --env real --duration 10
```

### Android/Chaquopy Integration

For Android app integration via Chaquopy:

```python
# All functions return JSON strings
from etf_collector.android_api import (
    get_constituents,           # Single ETF constituents
    get_etf_list,              # ETF list
    collect_all_constituents,   # All ETF constituents
)
```

**Kotlin Example**:
```kotlin
val py = Python.getInstance()
val module = py.getModule("etf_collector.android_api")

// Create config JSON
val configJson = """
{
    "app_key": "your_kis_app_key",
    "app_secret": "your_kis_secret_key",
    "environment": "real"
}
""".trimIndent()

// Get single ETF constituents
val result = module.callAttr("get_constituents", configJson, "069500").toString()
val parsed = Json.decodeFromString<ApiResponse>(result)
```

## Project Structure

```
etf-collector/
├── src/etf_collector/
│   ├── android_api.py     # Android/Chaquopy integration API
│   ├── config.py          # Configuration management
│   ├── auth/              # API authentication
│   │   ├── kis_auth.py    # KIS OAuth
│   │   └── kiwoom_auth.py # Kiwoom OAuth
│   ├── collector/         # Data collectors
│   │   ├── constituent.py # ETF constituent collector
│   │   ├── etf_list.py    # ETF list management
│   │   └── kiwoom_etf_list.py  # Kiwoom ETF list
│   ├── filter/            # Keyword-based filtering
│   ├── limiter/           # Rate limiting
│   ├── storage/           # Data storage (CSV/JSON)
│   ├── data/              # Predefined ETF codes
│   └── utils/             # Utilities
│       ├── helpers.py     # Helper functions
│       ├── logger.py      # Logging
│       └── validators.py  # Input validation
├── tests/                 # Unit tests (91 tests)
└── pyproject.toml
```

## Security Features

- **Path Traversal Protection**: All file paths are validated
- **Credential Masking**: API keys are masked in logs
- **Input Validation**: ETF codes and API responses are validated
- **Secure Storage**: StorageError for invalid path operations

## Running Tests

```bash
# Run all unit tests
uv run pytest tests/unit/ -v

# Run with coverage
uv run pytest tests/unit/ --cov=etf_collector
```

## API Response Format

All functions return a consistent result pattern:

```python
# Success
{"ok": True, "data": {...}}

# Error
{"ok": False, "error": {"code": "ERROR_CODE", "msg": "Error message"}}
```

## Error Codes

| Code | Description |
|------|-------------|
| `INVALID_ARG` | Invalid argument (e.g., ETF code format) |
| `NO_DATA` | No data available |
| `API_ERROR` | External API error |
| `AUTH_ERROR` | Authentication failed |
| `NETWORK_ERROR` | Network error |
| `PATH_TRAVERSAL` | Path traversal attempt detected |
| `INVALID_PATH` | Invalid file path |
| `STORAGE_ERROR` | File storage error |

## Code Quality

| Metric | Value |
|--------|-------|
| Tests | 91 (100% pass) |
| Security Score | 8.5/10 |
| Android Ready | 8.5/10 |

See [CODE_REVIEW.md](CODE_REVIEW.md) for detailed analysis.

## License

MIT
