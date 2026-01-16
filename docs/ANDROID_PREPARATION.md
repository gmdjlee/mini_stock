# Android App 개발 사전 준비 가이드

**Version**: 1.0
**Created**: 2026-01-16
**Based on**: STOCK_APP_SPEC.md

---

## 1. 사전 준비 체크리스트

### 1.1 Python 패키지 상태 (완료)

| Phase | 상태 | 모듈 | 테스트 |
|-------|------|------|--------|
| Phase 0 | ✅ 완료 | config, core/*, client/* | 통과 |
| Phase 1 | ✅ 완료 | stock/search, analysis, ohlcv | 통과 |
| Phase 2 | ✅ 완료 | indicator/trend, elder, demark | 통과 |
| Phase 3 | ✅ 완료 | chart/candle, line, bar | 통과 |
| Phase 4 | ✅ 완료 | market/deposit, search/condition | 통과 |
| Phase 5 | ✅ 완료 | indicator/oscillator, chart/oscillator | 통과 |

**총 테스트**: 160개 (11 테스트 파일)
**코드 라인**: ~5,437 lines (28 Python 파일)

### 1.2 현재 Python 의존성

```toml
# pyproject.toml 의존성
[dependencies]
pandas = ">=2.0.0"
numpy = ">=1.24.0"
requests = ">=2.31.0"
python-dotenv = ">=1.0.0"
matplotlib = ">=3.7.0"
mplfinance = ">=0.12.10"
```

---

## 2. Chaquopy 호환성 분석

### 2.1 의존성 호환성 매트릭스

| 패키지 | Chaquopy 지원 | 비고 |
|--------|--------------|------|
| `requests` | ✅ 완전 지원 | Pure Python |
| `python-dotenv` | ✅ 완전 지원 | Pure Python |
| `numpy` | ⚠️ 조건부 지원 | Pre-built wheel 필요 |
| `pandas` | ⚠️ 조건부 지원 | numpy 의존, 바이너리 컴포넌트 |
| `matplotlib` | ❌ 제한적 | 백엔드 이슈, GUI 의존성 |
| `mplfinance` | ❌ 제한적 | matplotlib 의존 |

### 2.2 해결 방안

#### 방안 A: 차트 기능 분리 (권장)
```
Python (Android)          Kotlin (Android)
├── client/*       →     PyClient 호출
├── stock/*        →     데이터 계산
├── indicator/*    →     지표 계산
├── market/*       →     시장 지표
├── search/*       →     조건검색
│
└── chart/*        ✗     Vico Charts로 대체
```

**장점**:
- 가장 안정적인 방법
- Android 네이티브 차트 라이브러리 활용 (Vico)
- 성능 우수 (네이티브 렌더링)

**단점**:
- Kotlin 차트 컴포넌트 추가 개발 필요

#### 방안 B: 서버 기반 차트 생성
```
Python (Android)          Remote Server
├── client/*        →     API 호출
├── stock/*         →     데이터 계산
├── indicator/*     →     지표 계산
│
└── 차트 요청       →     서버에서 PNG 생성 → 이미지 표시
```

**장점**:
- Python 차트 코드 재사용 가능

**단점**:
- 서버 인프라 필요
- 네트워크 의존성

#### 방안 C: 차트 없는 데이터 전용 앱
```
Python (Android)
├── client/*        →     API 호출
├── stock/*         →     데이터 계산
├── indicator/*     →     지표 계산 (수치만)
│
└── 차트 ✗               데이터 테이블로 표시
```

### 2.3 권장 방안: A (차트 기능 분리)

Python 패키지를 Android용으로 수정:
1. `matplotlib`, `mplfinance` 의존성 제거
2. `chart/` 모듈을 Kotlin Vico Charts로 구현
3. 데이터 계산 로직만 Python에서 처리

---

## 3. Android 프로젝트 설정

### 3.1 요구 환경

| 항목 | 버전 | 비고 |
|------|------|------|
| Android Studio | Ladybug (2024.2.1) 이상 | 최신 권장 |
| Kotlin | 2.1.0+ | Compose 호환 |
| Gradle | 8.5+ | AGP 8.3+ |
| JDK | 17+ | Android 필수 |
| minSdk | 26 | Python 3.11 요구 |
| targetSdk | 35 | 최신 |
| Chaquopy | 15.0.1+ | Python 3.8-3.12 지원 |

### 3.2 Gradle 설정 (build.gradle.kts)

```kotlin
// 프로젝트 레벨 build.gradle.kts
plugins {
    id("com.android.application") version "8.3.0" apply false
    id("org.jetbrains.kotlin.android") version "2.1.0" apply false
    id("com.google.dagger.hilt.android") version "2.54" apply false
    id("com.chaquo.python") version "15.0.1" apply false
}
```

```kotlin
// app/build.gradle.kts
plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("com.google.dagger.hilt.android")
    id("com.google.devtools.ksp")
    id("com.chaquo.python")
}

android {
    namespace = "com.stockapp"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.stockapp"
        minSdk = 26
        targetSdk = 35
        versionCode = 1
        versionName = "1.0.0"

        // Chaquopy 설정
        ndk {
            abiFilters += listOf("arm64-v8a", "x86_64")
        }

        python {
            version = "3.11"

            pip {
                install("requests")
                install("python-dotenv")
                install("numpy")
                install("pandas")
                // matplotlib, mplfinance 제외 (Android에서 미지원)
            }
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }

    buildFeatures {
        compose = true
    }

    composeOptions {
        kotlinCompilerExtensionVersion = "1.5.8"
    }
}

dependencies {
    // Compose
    implementation(platform("androidx.compose:compose-bom:2024.12.01"))
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.ui:ui-tooling-preview")

    // Hilt
    implementation("com.google.dagger:hilt-android:2.54")
    ksp("com.google.dagger:hilt-compiler:2.54")
    implementation("androidx.hilt:hilt-navigation-compose:1.2.0")

    // Room
    implementation("androidx.room:room-runtime:2.8.3")
    implementation("androidx.room:room-ktx:2.8.3")
    ksp("androidx.room:room-compiler:2.8.3")

    // Vico Charts (네이티브 차트 라이브러리)
    implementation("com.patrykandpatrick.vico:compose:2.0.0")
    implementation("com.patrykandpatrick.vico:compose-m3:2.0.0")

    // Serialization
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.6.3")
}
```

### 3.3 Python 패키지 배치

```
StockApp/
├── app/
│   └── src/main/
│       ├── java/com/stockapp/     # Kotlin 코드
│       │
│       └── python/                 # Python 패키지 위치
│           └── stock_analyzer/     # ← stock-analyzer/src/stock_analyzer/ 복사
│               ├── __init__.py
│               ├── config.py
│               ├── core/
│               │   ├── __init__.py
│               │   ├── log.py
│               │   ├── http.py
│               │   ├── date.py
│               │   └── json_helper.py
│               ├── client/
│               │   ├── __init__.py
│               │   ├── auth.py
│               │   └── kiwoom.py
│               ├── stock/
│               │   ├── __init__.py
│               │   ├── search.py
│               │   ├── analysis.py
│               │   └── ohlcv.py
│               ├── indicator/
│               │   ├── __init__.py
│               │   ├── trend.py
│               │   ├── elder.py
│               │   ├── demark.py
│               │   └── oscillator.py
│               ├── market/
│               │   ├── __init__.py
│               │   └── deposit.py
│               └── search/
│                   ├── __init__.py
│                   └── condition.py
│               # chart/ 폴더 제외 (Kotlin Vico로 대체)
```

---

## 4. Python 패키지 수정 사항

### 4.1 Android용 패키지 생성

```bash
# Android용 Python 패키지 복사 (차트 모듈 제외)
mkdir -p StockApp/app/src/main/python/stock_analyzer

# 필요한 모듈만 복사
cp -r stock-analyzer/src/stock_analyzer/__init__.py StockApp/app/src/main/python/stock_analyzer/
cp -r stock-analyzer/src/stock_analyzer/config.py StockApp/app/src/main/python/stock_analyzer/
cp -r stock-analyzer/src/stock_analyzer/core StockApp/app/src/main/python/stock_analyzer/
cp -r stock-analyzer/src/stock_analyzer/client StockApp/app/src/main/python/stock_analyzer/
cp -r stock-analyzer/src/stock_analyzer/stock StockApp/app/src/main/python/stock_analyzer/
cp -r stock-analyzer/src/stock_analyzer/indicator StockApp/app/src/main/python/stock_analyzer/
cp -r stock-analyzer/src/stock_analyzer/market StockApp/app/src/main/python/stock_analyzer/
cp -r stock-analyzer/src/stock_analyzer/search StockApp/app/src/main/python/stock_analyzer/

# chart/ 폴더는 복사하지 않음 (Kotlin으로 구현)
```

### 4.2 __init__.py 수정 (Android용)

```python
# stock_analyzer/__init__.py (Android 버전)
"""Stock Analyzer - Android Version (without chart modules)."""

__version__ = "0.2.0-android"

from .config import Config
from .client.kiwoom import KiwoomClient
from .client.auth import AuthClient

# chart 모듈 import 제외
__all__ = [
    "Config",
    "KiwoomClient",
    "AuthClient",
]
```

### 4.3 환경 변수 처리

Android에서는 `.env` 파일 대신 `SharedPreferences` 또는 `BuildConfig` 사용:

```kotlin
// app/build.gradle.kts
android {
    buildFeatures {
        buildConfig = true
    }

    defaultConfig {
        // API 키는 local.properties에서 읽어옴
        buildConfigField("String", "KIWOOM_APP_KEY",
            "\"${project.findProperty("KIWOOM_APP_KEY") ?: ""}\"")
        buildConfigField("String", "KIWOOM_SECRET_KEY",
            "\"${project.findProperty("KIWOOM_SECRET_KEY") ?: ""}\"")
        buildConfigField("String", "KIWOOM_BASE_URL",
            "\"https://api.kiwoom.com\"")
    }
}
```

```properties
# local.properties (git에 포함하지 않음)
KIWOOM_APP_KEY=your_app_key_here
KIWOOM_SECRET_KEY=your_secret_key_here
```

---

## 5. Android 앱 개발 순서

### Step 1: Android Studio 프로젝트 생성

1. Android Studio 실행
2. New Project → Empty Activity (Compose)
3. 프로젝트 설정:
   - Name: `StockApp`
   - Package: `com.stockapp`
   - Minimum SDK: API 26
   - Build configuration: Kotlin DSL

### Step 2: Gradle 설정

1. 프로젝트 레벨 `build.gradle.kts`에 Chaquopy 플러그인 추가
2. 앱 레벨 `build.gradle.kts`에 의존성 추가
3. `gradle/libs.versions.toml` 버전 카탈로그 설정
4. Gradle Sync 실행

### Step 3: Python 패키지 통합

1. `app/src/main/python/` 디렉토리 생성
2. `stock_analyzer` 패키지 복사 (차트 모듈 제외)
3. Gradle Sync로 Python 패키지 인식 확인

### Step 4: 핵심 인프라 구현

1. **PyClient.kt** - Python 호출 브릿지
2. **AppDb.kt** - Room 데이터베이스
3. **DI Modules** - Hilt 의존성 주입
4. **Theme** - Material 3 테마

### Step 5: Feature별 구현

| 순서 | Feature | 화면 | Python 모듈 |
|------|---------|------|-------------|
| 1 | Search | SearchScreen | stock/search |
| 2 | Analysis | AnalysisScreen | stock/analysis |
| 3 | OHLCV Chart | ChartScreen | stock/ohlcv + Vico |
| 4 | Indicators | IndicatorScreen | indicator/* |
| 5 | Market | MarketScreen | market/deposit |
| 6 | Condition | ConditionScreen | search/condition |

### Step 6: 차트 구현 (Vico)

```kotlin
// Vico Charts로 캔들스틱 차트 구현 예시
@Composable
fun CandlestickChart(
    ohlcv: OhlcvData,
    modifier: Modifier = Modifier
) {
    val modelProducer = remember { CartesianChartModelProducer() }

    LaunchedEffect(ohlcv) {
        modelProducer.runTransaction {
            candlestickSeries(
                opening = ohlcv.open,
                closing = ohlcv.close,
                low = ohlcv.low,
                high = ohlcv.high
            )
        }
    }

    CartesianChartHost(
        chart = rememberCartesianChart(
            rememberCandlestickCartesianLayer()
        ),
        modelProducer = modelProducer,
        modifier = modifier
    )
}
```

---

## 6. 테스트 전략

### 6.1 단위 테스트

```kotlin
// Python 호출 테스트
@Test
fun `search returns stocks when query is valid`() = runTest {
    val result = pyClient.call(
        module = "stock_analyzer.stock.search",
        func = "search",
        args = listOf(client, "삼성")
    )
    assertTrue(result.isSuccess)
}
```

### 6.2 통합 테스트

```kotlin
// Repository 테스트
@Test
fun `repository returns cached data when available`() = runTest {
    // Given: 캐시된 데이터
    stockDao.insert(cachedStock)

    // When: 검색 실행
    val result = repository.search("삼성")

    // Then: 캐시 데이터 반환
    assertTrue(result.isSuccess)
}
```

### 6.3 UI 테스트

```kotlin
// Compose UI 테스트
@Test
fun searchScreen_showsResults_whenSearchSucceeds() {
    composeTestRule.setContent {
        SearchScreen(viewModel = mockViewModel)
    }

    composeTestRule
        .onNodeWithText("삼성전자")
        .assertIsDisplayed()
}
```

---

## 7. 주의사항 및 팁

### 7.1 Chaquopy 주의사항

1. **Python 버전**: 3.8 ~ 3.12 지원, 3.11 권장
2. **ABI 필터**: arm64-v8a, x86_64만 지정 (앱 크기 감소)
3. **첫 실행 시간**: Python 초기화로 2-3초 소요
4. **메모리**: Python 인터프리터 ~50MB 추가

### 7.2 성능 최적화

1. **Python 호출 최소화**: 배치 처리, 결과 캐싱
2. **백그라운드 처리**: Coroutine에서 Python 호출
3. **LazyLoading**: Python 모듈 필요시 로드

### 7.3 디버깅

```kotlin
// Logcat에서 Python 로그 확인
python {
    console.logLevel = "verbose"
}
```

---

## 8. 체크리스트 요약

### 개발 시작 전
- [ ] Android Studio 최신 버전 설치
- [ ] JDK 17+ 설치
- [ ] 키움 API 키 발급 확인

### 프로젝트 설정
- [ ] Android 프로젝트 생성 (Compose)
- [ ] Chaquopy 플러그인 설정
- [ ] 의존성 추가 (Hilt, Room, Vico)
- [ ] Python 패키지 복사 (차트 제외)

### 핵심 구현
- [ ] PyClient 브릿지 구현
- [ ] Room DB 설정
- [ ] Hilt DI 모듈 설정
- [ ] Material 3 테마 적용

### Feature 구현
- [ ] 종목 검색 화면
- [ ] 수급 분석 화면
- [ ] OHLCV 차트 (Vico)
- [ ] 기술적 지표 화면
- [ ] 시장 지표 화면
- [ ] 조건검색 화면

### 테스트 및 배포
- [ ] 단위 테스트 작성
- [ ] UI 테스트 작성
- [ ] ProGuard 규칙 설정
- [ ] 릴리스 빌드 테스트

---

**다음 단계**: Android Studio에서 프로젝트 생성 후 Step-by-Step으로 진행
