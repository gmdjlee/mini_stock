# StockApp

키움증권 REST API를 활용한 주식 분석 Android 앱

## 기술 스택

| 기술 | 용도 | 버전 |
|------|------|------|
| Kotlin | 앱 개발 언어 | 2.1.0 |
| Jetpack Compose | UI 프레임워크 | BOM 2024.12 |
| Chaquopy | Python 통합 | 15.0.1 |
| Hilt | 의존성 주입 | 2.54 |
| Room | 로컬 DB | 2.6.1 |
| Vico | 차트 라이브러리 | 2.0.0-beta.3 |

## 프로젝트 구조

```
app/src/main/
├── java/com/stockapp/
│   ├── App.kt                 # Hilt Application
│   ├── MainActivity.kt        # Main Activity
│   ├── core/
│   │   ├── db/               # Room 데이터베이스
│   │   │   ├── AppDb.kt
│   │   │   ├── dao/          # DAO 인터페이스
│   │   │   └── entity/       # Entity 클래스
│   │   ├── di/               # Hilt DI 모듈
│   │   ├── py/               # Python Bridge
│   │   │   ├── PyClient.kt
│   │   │   └── PyResponse.kt
│   │   └── ui/
│   │       ├── theme/        # Material 3 테마
│   │       └── component/    # 공통 컴포넌트
│   ├── feature/
│   │   ├── search/           # 종목 검색
│   │   ├── analysis/         # 수급 분석
│   │   ├── indicator/        # 기술 지표
│   │   ├── market/           # 시장 지표
│   │   └── condition/        # 조건검색
│   └── nav/                  # 네비게이션
├── python/
│   └── stock_analyzer/       # Python 패키지
└── res/
```

## 설정

### API 키 설정

1. `local.properties.example`을 `local.properties`로 복사
2. 키움증권 API 키 입력:
```properties
KIWOOM_APP_KEY=your_app_key_here
KIWOOM_SECRET_KEY=your_secret_key_here
```

### 빌드 및 실행

```bash
# 빌드
./gradlew build

# 디버그 APK 설치
./gradlew installDebug

# 테스트
./gradlew test
```

## 개발 Phase

| Phase | 상태 | 설명 |
|-------|------|------|
| App Phase 0 | ✅ 완료 | 프로젝트 설정, Chaquopy 통합 |
| App Phase 1 | 📋 대기 | 종목 검색, 수급 분석 화면 |
| App Phase 2 | 📋 대기 | 기술적 지표 화면 (Vico Charts) |
| App Phase 3 | 📋 대기 | 시장 지표, 조건검색 화면 |

## Python 패키지

`stock-analyzer` Python 패키지를 Chaquopy를 통해 Android에서 실행합니다.
차트 모듈(`chart/`)은 Vico Charts로 대체하여 네이티브 렌더링을 사용합니다.

### 포함 모듈
- `client/` - 키움 API 클라이언트
- `stock/` - 종목 검색, 수급 분석, OHLCV
- `indicator/` - 기술적 지표 (Trend, Elder, DeMark, Oscillator)
- `market/` - 시장 지표 (예탁금, 신용잔고)
- `search/` - 조건검색

### 제외 모듈
- `chart/` - Vico Charts로 대체 (matplotlib Android 미지원)

## 참고 문서

- [CLAUDE.md](../CLAUDE.md) - 프로젝트 가이드
- [ANDROID_PREPARATION.md](../docs/ANDROID_PREPARATION.md) - Android 개발 준비
- [STOCK_APP_SPEC.md](../docs/STOCK_APP_SPEC.md) - 상세 명세서
