# 액티브 ETF 기능 명세서

## 1. 개요

### 1.1 목적
본 문서는 StockApp Android 앱에 액티브 ETF 정보 조회 기능을 추가하기 위한 명세서입니다. **KIS (한국투자증권) API**를 활용하여 ETF의 기본 정보, 구성종목 데이터를 수집하고 관리합니다.

### 1.2 주요 기능
1. 액티브 ETF 목록 조회 및 키워드 기반 필터링 (포함/제외)
2. ETF 구성종목 상세 정보 수집
3. API 요청 제한 관리 (Rate Limiter)
4. 데이터 캐싱 및 저장

### 1.3 개발 전략

> **중요**: 2단계 개발 프로세스를 따릅니다.

```
┌─────────────────────────────────────────────────────────────────────┐
│                        개발 프로세스                                 │
├─────────────────────────────────────────────────────────────────────┤
│                                                                     │
│  Phase 1: Python 프로토타입                                         │
│  ┌─────────────────────────────────────────────────────────────┐   │
│  │  • KIS API 연동 및 기능 구현                                 │   │
│  │  • 단위 테스트 및 기능 검증                                  │   │
│  │  • 데이터 수집 로직 완성                                     │   │
│  │  • CLI 인터페이스로 동작 확인                                │   │
│  └─────────────────────────────────────────────────────────────┘   │
│                              ↓                                      │
│  Phase 2: Android 앱 이식                                           │
│  ┌─────────────────────────────────────────────────────────────┐   │
│  │  • Python 로직을 Kotlin으로 변환                             │   │
│  │  • StockApp 아키텍처에 맞게 최적화                           │   │
│  │  • Clean Architecture 패턴 적용                              │   │
│  │  • Room DB 캐싱, Hilt DI 통합                                │   │
│  └─────────────────────────────────────────────────────────────┘   │
│                                                                     │
└─────────────────────────────────────────────────────────────────────┘
```

### 1.4 사용 API
- **KIS API만 사용** (키움 API 사용하지 않음)
- KIS Developers: https://apiportal.koreainvestment.com/
- KIS API GitHub: https://github.com/koreainvestment/open-trading-api

### 1.5 참조 문서
- 원본 명세서: 액티브 ETF 정보 수집 프로그램 명세서
- StockApp 아키텍처: `CLAUDE.md`
- Ranking 기능 참조: `StockApp/app/src/main/java/com/stockapp/feature/ranking/`

---

## 2. Phase 1: Python 프로토타입

### 2.1 프로젝트 구조

```
etf-collector/                    # 독립 Python 프로젝트
├── pyproject.toml               # 프로젝트 설정 (uv 사용)
├── .env.example                 # 환경 변수 예시
├── src/
│   └── etf_collector/
│       ├── __init__.py
│       ├── config.py            # 설정 관리
│       ├── auth/
│       │   └── kis_auth.py      # KIS API 인증
│       ├── collector/
│       │   ├── etf_list.py      # ETF 목록 수집
│       │   └── constituent.py   # 구성종목 수집
│       ├── filter/
│       │   └── keyword.py       # 키워드 필터링
│       ├── limiter/
│       │   └── rate_limiter.py  # Rate Limiter
│       ├── storage/
│       │   └── data_storage.py  # 데이터 저장
│       └── utils/
│           ├── logger.py        # 로깅
│           └── helpers.py       # 유틸리티
├── tests/
│   ├── unit/                    # 단위 테스트
│   └── integration/             # 통합 테스트
├── scripts/
│   └── run_collector.py         # 실행 스크립트
└── data/                        # 수집 데이터 저장
```

### 2.2 의존성

```toml
# pyproject.toml
[project]
name = "etf-collector"
version = "0.1.0"
requires-python = ">=3.10"
dependencies = [
    "requests>=2.28.0",
    "pandas>=1.5.0",
    "pyyaml>=6.0",
    "python-dotenv>=1.0.0",
]

[project.optional-dependencies]
dev = [
    "pytest>=7.0.0",
    "pytest-asyncio>=0.21.0",
]
```

### 2.3 환경 변수

```bash
# .env
KIS_APP_KEY=your_app_key_here
KIS_APP_SECRET=your_app_secret_here
KIS_ACCOUNT_NO=your_account_number
KIS_ENVIRONMENT=real  # real 또는 virtual
```

---

## 3. KIS API 명세 (Python 프로토타입용)

### 3.1 사용 API 목록

| API ID | API 명 | 용도 | URL |
|--------|--------|------|-----|
| - | 접근토큰발급 | OAuth 인증 | `/oauth2/tokenP` |
| CTPF1604R | 상품기본조회 | ETF 목록 조회 | `/uapi/domestic-stock/v1/quotations/search-info` |
| FHKST121600C0 | ETF구성종목시세 | 구성종목 정보 | `/uapi/etfetn/v1/quotations/inquire-component-stock-price` |

### 3.2 ETF 구성종목시세 API (FHKST121600C0)

#### 3.2.1 요청 파라미터

| 파라미터 | 필수 | 설명 | 예시 |
|---------|------|------|------|
| `FID_COND_MRKT_DIV_CODE` | Y | 조건시장분류코드 | "J" (주식/ETF/ETN) |
| `FID_INPUT_ISCD` | Y | ETF 종목코드 | "069500" |
| `FID_COND_SCR_DIV_CODE` | Y | 조건화면분류코드 | "11216" |

#### 3.2.2 응답 데이터

**Output1 (ETF 기본 정보)**

| 필드명 | 설명 | 타입 |
|--------|------|------|
| `stck_prpr` | 주식 현재가 | int |
| `prdy_vrss` | 전일 대비 | int |
| `prdy_vrss_sign` | 전일 대비 부호 | string |
| `prdy_ctrt` | 전일 대비율 | float |
| `nav` | NAV (순자산가치) | float |
| `etf_ntas_ttam` | ETF 순자산총액 | int |
| `etf_cu_unit_scrt_cnt` | ETF CU 단위 유가증권 수 | int |
| `etf_cnfg_issu_cnt` | ETF 구성종목 수 | int |

**Output2 (구성종목 상세정보)**

| 필드명 | 한글명 | 설명 | 타입 |
|--------|--------|------|------|
| `stck_shrn_iscd` | 주식 단축 종목코드 | 구성종목 코드 | string |
| `hts_kor_isnm` | HTS 한글 종목명 | 구성종목명 | string |
| `stck_prpr` | 주식 현재가 | 현재가 | int |
| `prdy_vrss` | 전일 대비 | 전일 대비 | int |
| `prdy_vrss_sign` | 전일 대비 부호 | 1:상한, 2:상승, 3:보합, 4:하한, 5:하락 | string |
| `prdy_ctrt` | 전일 대비율 | 등락률 (%) | float |
| `acml_vol` | 누적 거래량 | 거래량 | int |
| `acml_tr_pbmn` | 누적 거래대금 | 거래대금 | int |
| `hts_avls` | HTS 시가총액 | 시가총액 | int |
| `etf_vltn_amt` | ETF 구성종목 평가금액 | 평가금액 | int |
| `etf_cnfg_issu_rlim` | ETF 구성종목 비중 | 비중 (%) | float |

### 3.3 Python 데이터 모델

```python
from dataclasses import dataclass
from datetime import datetime
from typing import List, Optional

@dataclass
class EtfInfo:
    """ETF 기본 정보"""
    etf_code: str              # ETF 종목코드
    etf_name: str              # ETF 종목명
    etf_type: str              # ETF 유형 (액티브/패시브)
    listing_date: Optional[str]  # 상장일
    tracking_index: str        # 추적 지수
    asset_class: str           # 자산분류
    management_company: str    # 운용사
    total_assets: float        # 순자산총액 (억원)

@dataclass
class ConstituentStock:
    """ETF 구성종목"""
    etf_code: str              # ETF 종목코드
    etf_name: str              # ETF 종목명
    stock_code: str            # 구성종목 코드
    stock_name: str            # 구성종목명
    current_price: int         # 현재가
    price_change: int          # 전일 대비
    price_change_rate: float   # 전일 대비율 (%)
    weight: float              # 구성 비중 (%)
    evaluation_amount: int     # 평가금액
    collected_at: datetime     # 수집 시각

@dataclass
class EtfConstituentSummary:
    """ETF 구성종목 요약"""
    etf_code: str
    etf_name: str
    total_constituent_count: int  # 총 구성종목 수
    nav: float                    # NAV
    total_assets: int             # 순자산총액
    constituents: List[ConstituentStock]
    collected_at: datetime
```

---

## 4. 키워드 필터링 (Python)

### 4.1 필터링 모델

```python
from dataclasses import dataclass
from typing import List
from enum import Enum

class FilterMode(Enum):
    INCLUDE = "include"        # 키워드 포함 종목만 선택
    EXCLUDE = "exclude"        # 키워드 포함 종목 제외
    INCLUDE_AND = "include_and"  # 모든 키워드 포함
    INCLUDE_OR = "include_or"    # 하나 이상 키워드 포함

@dataclass
class KeywordFilter:
    keywords: List[str]
    mode: FilterMode
    case_sensitive: bool = False

    def apply(self, etf_name: str) -> bool:
        """키워드 필터링 적용"""
        target = etf_name if self.case_sensitive else etf_name.lower()
        keywords = self.keywords if self.case_sensitive else [k.lower() for k in self.keywords]

        if self.mode == FilterMode.INCLUDE:
            return any(kw in target for kw in keywords)
        elif self.mode == FilterMode.EXCLUDE:
            return not any(kw in target for kw in keywords)
        elif self.mode == FilterMode.INCLUDE_AND:
            return all(kw in target for kw in keywords)
        elif self.mode == FilterMode.INCLUDE_OR:
            return any(kw in target for kw in keywords)
        return False
```

### 4.2 액티브 ETF 필터링 조건

```python
# 액티브 ETF 판별 조건
ACTIVE_ETF_FILTER = KeywordFilter(
    keywords=["액티브", "Active"],
    mode=FilterMode.INCLUDE_OR,
    case_sensitive=False
)

# 레버리지/인버스 제외 필터
EXCLUDE_LEVERAGE_FILTER = KeywordFilter(
    keywords=["레버리지", "인버스", "2X", "3X", "inverse"],
    mode=FilterMode.EXCLUDE,
    case_sensitive=False
)
```

---

## 5. Rate Limiter (Python)

### 5.1 KIS API Rate Limit 사양

| 환경 | 초당 요청 제한 | 권장 설정 |
|------|---------------|-----------|
| 실전투자 | 20건/초 | 15건/초 (안전 마진) |
| 모의투자 | 5건/초 | 4건/초 (안전 마진) |

### 5.2 구현

```python
import time
import threading
from collections import deque
from dataclasses import dataclass
from typing import Optional

@dataclass
class RateLimiterConfig:
    requests_per_second: float = 15.0  # 초당 요청 수
    burst_size: int = 1                # 버스트 허용량
    retry_on_limit: bool = True        # 제한 초과 시 재시도
    max_retries: int = 3               # 최대 재시도 횟수
    retry_delay: float = 1.0           # 재시도 대기 시간 (초)

class SlidingWindowRateLimiter:
    """슬라이딩 윈도우 방식 Rate Limiter"""

    def __init__(self, config: RateLimiterConfig):
        self.config = config
        self.window_size = 1.0  # 1초 윈도우
        self.request_times: deque = deque()
        self.lock = threading.Lock()
        self.min_interval = 1.0 / config.requests_per_second

    def acquire(self, timeout: Optional[float] = None) -> bool:
        """요청 허가 획득 (블로킹)"""
        start_time = time.time()

        while True:
            with self.lock:
                current_time = time.time()

                # 윈도우 외부 요청 제거
                while self.request_times and \
                      current_time - self.request_times[0] > self.window_size:
                    self.request_times.popleft()

                # 요청 가능 여부 확인
                if len(self.request_times) < self.config.requests_per_second:
                    self.request_times.append(current_time)
                    return True

            # 타임아웃 체크
            if timeout is not None and time.time() - start_time > timeout:
                return False

            time.sleep(self.min_interval)

    def wait_if_needed(self):
        """필요 시 대기"""
        self.acquire()
```

### 5.3 에러 코드 처리

| 에러 코드 | 설명 | 처리 방법 |
|----------|------|----------|
| `EGW00201` | API 호출 유량 초과 | 1초 대기 후 재시도 |
| `EGW00123` | 토큰 만료 | 토큰 재발급 후 재시도 |
| `OPSW0009` | 시스템 오류 | 5초 대기 후 재시도 |

---

## 6. Python CLI 인터페이스

### 6.1 명령어

```bash
# 기본 실행 - 모든 액티브 ETF 구성종목 수집
python -m etf_collector collect --output ./data/etf_data.csv

# 키워드 필터링 (포함)
python -m etf_collector collect --include "반도체,AI" --output ./data/filtered.csv

# 키워드 필터링 (제외)
python -m etf_collector collect --exclude "레버리지,인버스" --output ./data/filtered.csv

# 기간 지정 (상장일 기준)
python -m etf_collector collect --start-date 2020-01-01 --end-date 2024-12-31

# Rate Limit 테스트
python -m etf_collector test-rate-limit --env real --duration 30

# 설정 확인
python -m etf_collector config --show
```

### 6.2 설정 파일

```yaml
# config/settings.yaml
kis_api:
  base_url: "https://openapi.koreainvestment.com:9443"
  app_key: "${KIS_APP_KEY}"
  app_secret: "${KIS_APP_SECRET}"
  account_no: "${KIS_ACCOUNT_NO}"
  environment: "real"  # real / virtual

rate_limit:
  requests_per_second: 15
  burst_size: 1
  retry_on_limit: true
  max_retries: 3

collection:
  default_output_dir: "./data"
  output_format: "csv"  # csv / json

filter:
  default_mode: "include"
  case_sensitive: false

logging:
  level: "INFO"
  file: "./logs/etf_collector.log"
```

---

## 7. 데이터 저장 형식

### 7.1 ETF 목록 CSV

```csv
etf_code,etf_name,etf_type,listing_date,management_company,total_assets,collected_at
069500,KODEX 200,패시브,2002-10-14,삼성자산운용,58234.5,2026-01-24T10:30:00
278530,KODEX 200TR,액티브,2017-09-01,삼성자산운용,1234.5,2026-01-24T10:30:00
```

### 7.2 구성종목 CSV

```csv
etf_code,etf_name,stock_code,stock_name,current_price,price_change,price_change_rate,weight,evaluation_amount,collected_at
069500,KODEX 200,005930,삼성전자,71500,500,0.70,31.25,15625000000,2026-01-24T10:30:00
069500,KODEX 200,000660,SK하이닉스,135000,2000,1.50,8.42,4210000000,2026-01-24T10:30:00
```

### 7.3 JSON 형식

```json
{
  "collection_info": {
    "collected_at": "2026-01-24T10:30:00",
    "filter_applied": {
      "keywords": ["반도체", "AI"],
      "mode": "include_or"
    },
    "total_etfs": 25,
    "total_constituents": 542
  },
  "etfs": [
    {
      "etf_code": "069500",
      "etf_name": "KODEX 200",
      "nav": 35250.5,
      "total_assets": 58234500000000,
      "constituent_count": 200,
      "constituents": [
        {
          "stock_code": "005930",
          "stock_name": "삼성전자",
          "weight": 31.25,
          "evaluation_amount": 15625000000
        }
      ]
    }
  ]
}
```

---

## 8. Python 프로토타입 테스트 체크리스트

### 8.1 단위 테스트

- [ ] KIS API 인증 토큰 발급/갱신
- [ ] Rate Limiter 동작 (점진적 증가, 버스트, 지속 부하)
- [ ] 키워드 필터링 (각 모드별)
- [ ] 데이터 파싱 및 변환
- [ ] 에러 처리 (네트워크, API 오류)

### 8.2 통합 테스트

- [ ] ETF 목록 수집 (실제 API 호출)
- [ ] 구성종목 수집 (단일/다수 ETF)
- [ ] 데이터 저장 (CSV, JSON)
- [ ] 대용량 데이터 처리

### 8.3 완료 기준

- [ ] 모든 단위 테스트 통과
- [ ] 실제 API로 액티브 ETF 목록 수집 성공
- [ ] 구성종목 데이터 정상 수집
- [ ] CLI 명령어 정상 동작
- [ ] 에러 처리 및 복구 검증

---

## 9. Phase 2: Android 앱 이식

### 9.1 이식 전략

```
Python 모듈                      Kotlin 모듈 (StockApp)
────────────────────────────────────────────────────────────
auth/kis_auth.py          →     core/api/KisApiClient.kt (신규)
                                core/api/KisTokenManager.kt (신규)
collector/etf_list.py     →     feature/etf/data/repo/EtfRepoImpl.kt
collector/constituent.py  →     feature/etf/data/repo/EtfRepoImpl.kt
filter/keyword.py         →     feature/etf/domain/model/EtfKeywordFilter.kt
limiter/rate_limiter.py   →     core/api/KisApiClient.kt (Rate Limit 내장)
storage/data_storage.py   →     core/db/entity/EtfCacheEntity.kt
                                core/db/dao/EtfCacheDao.kt
```

### 9.2 아키텍처 최적화

#### 9.2.1 KIS API 클라이언트 추가

```kotlin
// core/api/KisApiClient.kt
@Singleton
class KisApiClient @Inject constructor(
    private val tokenManager: KisTokenManager
) {
    private val rateLimitMutex = Mutex()
    private var lastRequestTime: Long = 0
    private val minRequestInterval = 67L  // 15 req/sec = 66.67ms

    suspend fun <T> call(
        apiId: String,
        url: String,
        params: Map<String, String>,
        appKey: String,
        secretKey: String,
        parser: (String) -> T
    ): Result<T>
}
```

#### 9.2.2 Feature 모듈 구조

```
feature/etf/
├── domain/
│   ├── model/
│   │   ├── EtfModels.kt           # EtfInfo, ConstituentStock
│   │   ├── EtfKeywordFilter.kt    # 키워드 필터링
│   │   └── EtfParams.kt           # API 파라미터
│   ├── repo/
│   │   └── EtfRepo.kt             # Repository 인터페이스
│   └── usecase/
│       ├── GetActiveEtfListUC.kt
│       └── GetEtfConstituentsUC.kt
├── data/
│   ├── dto/
│   │   └── KisEtfDto.kt           # KIS API 응답 DTO
│   └── repo/
│       └── EtfRepoImpl.kt
├── ui/
│   ├── EtfScreen.kt
│   ├── EtfVm.kt
│   └── EtfConstituentSheet.kt     # 구성종목 바텀시트
└── di/
    └── EtfModule.kt
```

### 9.3 Domain 모델 (Kotlin)

```kotlin
// feature/etf/domain/model/EtfModels.kt

/**
 * ETF 기본 정보
 */
data class EtfInfo(
    val etfCode: String,
    val etfName: String,
    val etfType: EtfType,
    val listingDate: String?,
    val trackingIndex: String,
    val assetClass: String,
    val managementCompany: String,
    val totalAssets: Double  // 억원
)

enum class EtfType {
    ACTIVE, PASSIVE
}

/**
 * ETF 구성종목
 */
data class ConstituentStock(
    val etfCode: String,
    val stockCode: String,
    val stockName: String,
    val currentPrice: Long,
    val priceChange: Long,
    val priceChangeRate: Double,
    val weight: Double,        // 비중 (%)
    val evaluationAmount: Long
)

/**
 * ETF 구성종목 요약
 */
data class EtfConstituentSummary(
    val etfCode: String,
    val etfName: String,
    val totalConstituentCount: Int,
    val nav: Double,
    val totalAssets: Long,
    val constituents: List<ConstituentStock>,
    val collectedAt: LocalDateTime
)
```

### 9.4 키워드 필터 (Kotlin)

```kotlin
// feature/etf/domain/model/EtfKeywordFilter.kt

enum class FilterMode {
    INCLUDE,      // 키워드 포함 종목만 선택
    EXCLUDE,      // 키워드 포함 종목 제외
    INCLUDE_AND,  // 모든 키워드 포함
    INCLUDE_OR    // 하나 이상 키워드 포함
}

data class EtfKeywordFilter(
    val keywords: List<String> = listOf("액티브", "Active"),
    val mode: FilterMode = FilterMode.INCLUDE_OR,
    val caseSensitive: Boolean = false
) {
    fun matches(etfName: String): Boolean {
        val target = if (caseSensitive) etfName else etfName.lowercase()
        val searchKeywords = if (caseSensitive) keywords else keywords.map { it.lowercase() }

        return when (mode) {
            FilterMode.INCLUDE -> searchKeywords.any { target.contains(it) }
            FilterMode.EXCLUDE -> searchKeywords.none { target.contains(it) }
            FilterMode.INCLUDE_AND -> searchKeywords.all { target.contains(it) }
            FilterMode.INCLUDE_OR -> searchKeywords.any { target.contains(it) }
        }
    }
}
```

### 9.5 Room 캐싱

```kotlin
// core/db/entity/EtfEntities.kt

@Entity(
    tableName = "etf_info_cache",
    indices = [Index(value = ["etfName"])]
)
data class EtfInfoCacheEntity(
    @PrimaryKey val etfCode: String,
    val etfName: String,
    val etfType: String,
    val listingDate: String?,
    val trackingIndex: String,
    val assetClass: String,
    val managementCompany: String,
    val totalAssets: Double,
    val cachedAt: Long = System.currentTimeMillis()
)

@Entity(
    tableName = "etf_constituent_cache",
    primaryKeys = ["etfCode", "stockCode"]
)
data class EtfConstituentCacheEntity(
    val etfCode: String,
    val stockCode: String,
    val stockName: String,
    val currentPrice: Long,
    val priceChange: Long,
    val priceChangeRate: Double,
    val weight: Double,
    val evaluationAmount: Long,
    val cachedAt: Long = System.currentTimeMillis()
)
```

### 9.6 UI 화면 구성

```
┌────────────────────────────────────────┐
│  액티브 ETF                   🔄 🌙    │
├────────────────────────────────────────┤
│  ☑ 액티브 ETF만 보기                   │
├────────────────────────────────────────┤
│  검색결과: 45개                        │
├────────────────────────────────────────┤
│  ┌──────────────────────────────────┐ │
│  │ KODEX 200 액티브                  │ │
│  │ 069500 | 삼성자산운용             │ │
│  │ NAV 35,250  |  구성종목 200개     │ │
│  │ [ 구성종목 보기 ]                 │ │
│  └──────────────────────────────────┘ │
└────────────────────────────────────────┘

구성종목 바텀시트:
┌────────────────────────────────────────┐
│  KODEX 200 액티브 구성종목             │
├────────────────────────────────────────┤
│  총 200개 종목                         │
├────────────────────────────────────────┤
│  ┌────────────────────────────────┐   │
│  │ 1. 삼성전자 (005930)            │   │
│  │    71,500원 (+0.70%)           │   │
│  │    비중: 31.25%                │   │
│  └────────────────────────────────┘   │
│  ┌────────────────────────────────┐   │
│  │ 2. SK하이닉스 (000660)          │   │
│  │    135,000원 (+1.50%)          │   │
│  │    비중: 8.42%                 │   │
│  └────────────────────────────────┘   │
└────────────────────────────────────────┘
```

### 9.7 네비게이션 통합 옵션

| 옵션 | 설명 | 권장 |
|------|------|------|
| A | Ranking 화면에 탭 추가 | **권장** |
| B | 독립 ETF 탭 (6탭) | 탭 과다 |
| C | Settings에서 접근 | 접근성 저하 |

---

## 10. 구현 일정

### Phase 1: Python 프로토타입 (5일)

| 일차 | 작업 내용 |
|------|----------|
| 1 | 프로젝트 설정, KIS API 인증 모듈 |
| 2 | Rate Limiter 구현 및 테스트 |
| 3 | ETF 목록 수집, 키워드 필터링 |
| 4 | 구성종목 수집, 데이터 저장 |
| 5 | CLI 완성, 통합 테스트 |

### Phase 2: Android 앱 이식 (5-6일)

| 일차 | 작업 내용 |
|------|----------|
| 1 | KIS API 클라이언트 추가 (KisApiClient, KisTokenManager) |
| 2 | Domain 모델, Repository 인터페이스 |
| 3 | DTO, Repository 구현 |
| 4 | Room Entity, DAO, DB 마이그레이션 |
| 5 | ViewModel, UI 화면 |
| 6 | 네비게이션 통합, 테스트 |

**총 예상: 10-11일**

---

## 11. 체크리스트

### Phase 1 완료 체크리스트

- [ ] Python 프로젝트 설정 완료
- [ ] KIS API 인증 동작
- [ ] Rate Limiter 구현 및 테스트 통과
- [ ] ETF 목록 수집 기능 동작
- [ ] 키워드 필터링 동작
- [ ] 구성종목 수집 기능 동작
- [ ] CLI 명령어 정상 동작
- [ ] 데이터 저장 (CSV/JSON) 동작
- [ ] 모든 단위 테스트 통과

### Phase 2 완료 체크리스트

- [ ] KisApiClient 구현
- [ ] KisTokenManager 구현
- [ ] Domain 모델 정의
- [ ] Repository 인터페이스 정의
- [ ] DTO 정의
- [ ] Repository 구현
- [ ] Room Entity/DAO 정의
- [ ] AppDb 버전 업데이트
- [ ] Hilt Module 정의
- [ ] ViewModel 구현
- [ ] Screen 구현
- [ ] 네비게이션 통합
- [ ] API 테스트 통과
- [ ] UI 테스트 통과

---

## 부록 A: KIS API 참조 코드

### A.1 ETF 구성종목시세 호출 예시

**GitHub 소스 코드 위치:**
- https://github.com/koreainvestment/open-trading-api/tree/main/examples_llm/etfetn/inquire_component_stock_price/

```python
"""
ETF 구성종목시세 API 호출 예시
- API: /uapi/etfetn/v1/quotations/inquire-component-stock-price
- TR ID: FHKST121600C0
"""

import sys
from typing import Tuple
import pandas as pd

sys.path.extend(['../..', '.'])
import kis_auth as ka

API_URL = "/uapi/etfetn/v1/quotations/inquire-component-stock-price"

def inquire_component_stock_price(
    fid_cond_mrkt_div_code: str,
    fid_input_iscd: str,
    fid_cond_scr_div_code: str
) -> Tuple[pd.DataFrame, pd.DataFrame]:
    """
    ETF 구성종목시세 조회

    Args:
        fid_cond_mrkt_div_code: 조건시장분류코드 (J: 주식/ETF/ETN)
        fid_input_iscd: ETF 종목코드 (예: 069500)
        fid_cond_scr_div_code: 조건화면분류코드 (11216)

    Returns:
        Tuple[output1 DataFrame, output2 DataFrame]
    """
    tr_id = "FHKST121600C0"

    params = {
        "FID_COND_MRKT_DIV_CODE": fid_cond_mrkt_div_code,
        "FID_INPUT_ISCD": fid_input_iscd,
        "FID_COND_SCR_DIV_CODE": fid_cond_scr_div_code
    }

    res = ka._url_fetch(API_URL, tr_id, "", params)

    if res.isOK():
        output1_data = res.getBody().output1
        df1 = pd.DataFrame([output1_data]) if output1_data else pd.DataFrame()

        output2_data = res.getBody().output2
        df2 = pd.DataFrame(output2_data) if output2_data else pd.DataFrame()

        return df1, df2
    else:
        res.printError(url=API_URL)
        return pd.DataFrame(), pd.DataFrame()
```

### A.2 응답 필드 매핑

```python
# Output2 (구성종목) 컬럼 매핑
CONSTITUENT_COLUMN_MAPPING = {
    'stck_shrn_iscd': '주식_단축_종목코드',
    'hts_kor_isnm': 'HTS_한글_종목명',
    'stck_prpr': '주식_현재가',
    'prdy_vrss': '전일_대비',
    'prdy_vrss_sign': '전일_대비_부호',
    'prdy_ctrt': '전일_대비율',
    'acml_vol': '누적_거래량',
    'acml_tr_pbmn': '누적_거래대금',
    'hts_avls': 'HTS_시가총액',
    'etf_vltn_amt': 'ETF_구성종목_평가금액',
    'etf_cnfg_issu_rlim': 'ETF_구성종목_비중'
}
```

---

## 부록 B: 기존 앱과의 차이점

| 항목 | 기존 앱 (키움 API) | ETF 기능 (KIS API) |
|------|-------------------|-------------------|
| API 제공사 | 키움증권 | 한국투자증권 |
| 인증 방식 | OAuth (키움) | OAuth (KIS) |
| Rate Limit | 500ms/req | 67ms/req (15 req/sec) |
| 구성종목 | **미지원** | **지원** |
| API 클라이언트 | KiwoomApiClient | KisApiClient (신규) |
| 토큰 관리 | TokenManager | KisTokenManager (신규) |

### 공존 전략

```kotlin
// Settings에서 API 키 관리
data class ApiKeyConfig(
    // 기존 키움 API
    val kiwoomAppKey: String,
    val kiwoomSecretKey: String,
    val kiwoomInvestmentMode: InvestmentMode,

    // 신규 KIS API
    val kisAppKey: String,
    val kisSecretKey: String,
    val kisAccountNo: String
)
```

---

**문서 버전**: 2.0
**작성일**: 2026-01-24
**수정일**: 2026-01-24
**작성자**: Claude Code Agent
**변경 사항**:
- KIS API만 사용하도록 변경 (키움 API 제외)
- Python 프로토타입 → Android 이식 2단계 개발 프로세스 추가
- 구성종목 API 지원 추가
