# 재무정보(Financial) API 리뷰 보고서

**Review Date**: 2026-01-27
**Reviewer**: Claude Code
**Feature**: Financial Info Menu (App Phase 7)

---

## 1. 요약

재무정보 메뉴의 구현 상태를 검토한 결과, 전반적으로 잘 구조화된 코드이나 **TR ID 오류 1건**과 **코드 중복 이슈**가 발견되었습니다.

### 검토 항목 요약

| 항목 | 상태 | 비고 |
|------|------|------|
| TR ID 검증 | ❌ 오류 발견 | 성장성비율 TR ID 불일치 |
| KIS API Client 통합 | ⚠️ 개선 필요 | 기존 클라이언트 미활용 |
| 코드 패턴/품질 | ✅ 양호 | Clean Architecture 준수 |
| UI 구현 | ✅ 양호 | Compose 표준 패턴 준수 |
| 캐싱 | ✅ 양호 | Room + 24시간 TTL |
| 에러 처리 | ✅ 양호 | Result 패턴 적용 |

---

## 2. TR ID 검증 결과

### 2.1 사용자 제공 TR ID (정확한 값)

| API | TR ID (정확) |
|-----|--------------|
| 국내주식 대차대조표 | FHKST66430100 |
| 국내주식 손익계산서 | FHKST66430200 |
| 국내주식 재무비율 | FHKST66430300 |
| 국내주식 수익성비율 | FHKST66430400 |
| 국내주식 기타주요비율 | FHKST66430500 |
| 국내주식 안정성비율 | FHKST66430600 |
| 국내주식 성장성비율 | **FHKST66430800** |

### 2.2 구현된 TR ID

**파일**: `StockApp/app/src/main/java/com/stockapp/feature/financial/data/repo/FinancialRepoImpl.kt` (라인 404-410)

```kotlin
private const val TR_ID_BALANCE_SHEET = "FHKST66430100"      // ✅ 정확
private const val TR_ID_INCOME_STATEMENT = "FHKST66430200"   // ✅ 정확
private const val TR_ID_FINANCIAL_RATIO = "FHKST66430300"    // ✅ 정확
private const val TR_ID_PROFIT_RATIO = "FHKST66430400"       // ✅ 정확
private const val TR_ID_OTHER_MAJOR_RATIO = "FHKST66430500"  // ✅ 정확
private const val TR_ID_STABILITY_RATIO = "FHKST66430600"    // ✅ 정확
private const val TR_ID_GROWTH_RATIO = "FHKST66430700"       // ❌ 오류: FHKST66430800이어야 함
```

### 2.3 수정 필요 사항

| 파일 | 라인 | 현재 값 | 수정 값 |
|------|------|---------|---------|
| `FinancialRepoImpl.kt` | 410 | `FHKST66430700` | `FHKST66430800` |
| `docs/KIS_FINANCIAL_API.md` | 22, 250 | `FHKST66430700` | `FHKST66430800` |

---

## 3. KIS API Client 통합 분석

### 3.1 현재 상태

`FinancialRepoImpl`은 기존 `KisApiClient`를 활용하지 않고 **자체 HTTP 클라이언트와 토큰 관리 로직**을 구현했습니다.

**FinancialRepoImpl.kt의 중복 구현:**

```kotlin
// 자체 OkHttpClient 생성 (라인 65-69)
private val httpClient = OkHttpClient.Builder()
    .connectTimeout(30, TimeUnit.SECONDS)
    .readTimeout(30, TimeUnit.SECONDS)
    .writeTimeout(30, TimeUnit.SECONDS)
    .build()

// 자체 토큰 캐싱 (라인 71-72)
private var cachedToken: String? = null
private var tokenExpiresAt: Long = 0
```

### 3.2 기존 공통 클라이언트

| 클래스 | 위치 | 기능 |
|--------|------|------|
| `KisApiClient` | `core/api/KisApiClient.kt` | KIS REST API 호출, 토큰 관리, Rate Limiting |
| `KiwoomApiClient` | `core/api/KiwoomApiClient.kt` | Kiwoom REST API 호출, 토큰 관리 |
| `TokenManager` | `core/api/TokenManager.kt` | Kiwoom 토큰 관리 |

### 3.3 중복 코드 분석

| 기능 | KisApiClient | FinancialRepoImpl | 중복 여부 |
|------|--------------|-------------------|----------|
| OkHttpClient 생성 | ✅ | ✅ | 중복 |
| 토큰 캐싱 | ✅ | ✅ | 중복 |
| 토큰 만료 체크 | ✅ | ✅ | 중복 |
| API 호출 헤더 구성 | ✅ | ✅ | 중복 |
| Rate Limiting | ✅ (500ms) | ❌ 없음 | **누락** |

### 3.4 권장 개선 사항

`FinancialRepoImpl`이 기존 `KisApiClient.get()` 메서드를 활용하도록 리팩토링을 권장합니다.

**현재 코드:**
```kotlin
class FinancialRepoImpl @Inject constructor(
    private val financialCacheDao: FinancialCacheDao,
    private val settingsRepo: SettingsRepo,
    private val json: Json
) : FinancialRepo {
    private val httpClient = OkHttpClient.Builder()...  // 자체 클라이언트
```

**권장 코드:**
```kotlin
class FinancialRepoImpl @Inject constructor(
    private val financialCacheDao: FinancialCacheDao,
    private val settingsRepo: SettingsRepo,
    private val kisApiClient: KisApiClient,  // 기존 클라이언트 주입
    private val json: Json
) : FinancialRepo {
    // httpClient 제거, kisApiClient.get() 사용
```

---

## 4. 코드 패턴 및 품질 분석

### 4.1 아키텍처 구조 (✅ 양호)

```
feature/financial/
├── domain/
│   ├── model/FinancialModels.kt    # 460 lines - 도메인 모델
│   ├── repo/FinancialRepo.kt       # 리포지토리 인터페이스
│   └── usecase/GetFinancialSummaryUC.kt
├── data/
│   ├── dto/FinancialDto.kt         # 204 lines - API 응답 DTO
│   └── repo/FinancialRepoImpl.kt   # 423 lines - 리포지토리 구현
├── ui/
│   ├── FinancialScreen.kt          # 199 lines - UI
│   ├── FinancialVm.kt              # 130 lines - ViewModel
│   ├── ProfitabilityContent.kt     # 수익성 탭
│   └── StabilityContent.kt         # 안정성 탭
└── di/
    └── FinancialModule.kt          # Hilt DI 모듈
```

### 4.2 강점

| 항목 | 설명 |
|------|------|
| Clean Architecture | 명확한 계층 분리 (domain/data/ui/di) |
| 병렬 API 호출 | `coroutineScope` + `async`로 5개 API 동시 호출 |
| 데이터 병합 | `stac_yymm` 기준 결산년월별 데이터 병합 |
| 캐싱 전략 | Room + 24시간 TTL |
| UI 상태 관리 | `sealed class FinancialState` 패턴 |
| Pull-to-refresh | `PullToRefreshBox` 지원 |
| 테마 지원 | `ThemeToggleButton` 통합 |

### 4.3 DTO 변환 패턴 (✅ 양호)

```kotlin
// FinancialDto.kt - 깔끔한 toDomain() 변환
@Serializable
data class BalanceSheetDto(...) {
    fun toDomain(): BalanceSheet? {
        val ym = stacYymm ?: return null
        return BalanceSheet(
            period = FinancialPeriod.fromYearMonth(ym),
            currentAssets = cras?.toLongOrNull(),
            ...
        )
    }
}
```

### 4.4 개선 권장 사항

| 우선순위 | 항목 | 설명 |
|----------|------|------|
| 🔴 High | TR ID 수정 | 성장성비율 `FHKST66430700` → `FHKST66430800` |
| 🟡 Medium | KisApiClient 통합 | 중복 HTTP 클라이언트/토큰 로직 제거 |
| 🟡 Medium | Rate Limiting 추가 | API 호출 간 최소 간격 적용 |
| 🟢 Low | 로깅 개선 | BuildConfig.DEBUG 조건부 로깅 추가 |

---

## 5. 파일별 상세 분석

### 5.1 FinancialRepoImpl.kt (423 lines)

**잘된 점:**
- 병렬 API 호출 (`coroutineScope` + 5개 `async`)
- 캐시 TTL 체크 (`isCacheExpired`)
- 에러 시 빈 리스트 반환으로 부분 실패 허용

**개선점:**
- Rate Limiting 미적용 (KisApiClient는 500ms 간격 적용)
- 자체 OkHttpClient/토큰 관리 (중복)

### 5.2 FinancialDto.kt (204 lines)

**잘된 점:**
- `@SerialName` 정확한 API 필드 매핑
- Null-safe 변환 (`toLongOrNull()`, `toDoubleOrNull()`)
- 각 DTO별 `toDomain()` 메서드

### 5.3 FinancialModels.kt (460 lines)

**잘된 점:**
- 풍부한 도메인 모델 정의
- Cache 버전 별도 분리 (`@Serializable`)
- `toSummary()` 확장 함수로 UI용 변환
- 억원 단위 변환 (`/ 100_000_000`)

### 5.4 FinancialVm.kt (130 lines)

**잘된 점:**
- `SelectedStockManager` 관찰로 종목 변경 자동 감지
- 명확한 상태 관리 (`FinancialState` sealed class)
- 에러 메시지 한글화

### 5.5 FinancialScreen.kt (199 lines)

**잘된 점:**
- 상태별 UI 분기 처리
- `PullToRefreshBox` 지원
- `ThemeToggleButton` 통합
- 접근성 고려 (`contentDescription`)

---

## 6. 결론

### 6.1 종합 평가

| 항목 | 점수 | 비고 |
|------|------|------|
| 기능 완성도 | 9/10 | TR ID 오류 제외 시 완성도 높음 |
| 코드 품질 | 8/10 | 중복 코드 존재 |
| 아키텍처 | 9/10 | Clean Architecture 준수 |
| UI/UX | 9/10 | Material3 + 테마 지원 |
| 캐싱 | 10/10 | Room + TTL 적절히 적용 |
| **종합** | **8.5/10** | 양호 |

### 6.2 즉시 조치 필요 사항

1. **TR ID 수정** (Critical)
   - `FinancialRepoImpl.kt:410`: `FHKST66430700` → `FHKST66430800`
   - `docs/KIS_FINANCIAL_API.md`: 동일 수정

### 6.3 권장 개선 사항

1. **KisApiClient 통합** (Medium)
   - 중복 HTTP 클라이언트 제거
   - 기존 토큰 관리 활용
   - Rate Limiting 자동 적용

2. **Rate Limiting 추가** (Medium)
   - 현재 병렬 호출 시 Rate Limit 위험
   - KIS API 제한: 20건/초

---

## 7. 관련 파일 목록

```
StockApp/app/src/main/java/com/stockapp/feature/financial/
├── data/
│   ├── dto/FinancialDto.kt
│   └── repo/FinancialRepoImpl.kt
├── domain/
│   ├── model/FinancialModels.kt
│   ├── repo/FinancialRepo.kt
│   └── usecase/GetFinancialSummaryUC.kt
├── ui/
│   ├── FinancialScreen.kt
│   ├── FinancialVm.kt
│   ├── ProfitabilityContent.kt
│   └── StabilityContent.kt
└── di/
    └── FinancialModule.kt

StockApp/app/src/main/java/com/stockapp/core/
├── api/
│   └── KisApiClient.kt          # 기존 KIS 클라이언트 (활용 권장)
└── db/
    ├── entity/StockEntity.kt    # FinancialCacheEntity 포함
    └── dao/FinancialCacheDao.kt

docs/
├── KIS_FINANCIAL_API.md         # API 명세서 (TR ID 수정 필요)
└── FINANCIAL_API_REVIEW.md      # 본 리뷰 문서
```

---

**End of Review**
