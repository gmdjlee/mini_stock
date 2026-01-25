# StockApp

키움증권 REST API를 활용한 주식 분석 Android 앱

## 기술 스택

| 기술 | 용도 | 버전 |
|------|------|------|
| Kotlin | 앱 개발 언어 | 2.1.0 |
| Jetpack Compose | UI 프레임워크 | BOM 2024.12 |
| Chaquopy | Python 통합 | 15.0.1 |
| Hilt | 의존성 주입 | 2.54 |
| Room | 로컬 DB | 2.8.3 |
| Vico | 차트 라이브러리 | 2.0.0 |
| WorkManager | 백그라운드 스케줄링 | Latest |
| OkHttp | REST API 클라이언트 | 4.12.0 |

## 개발 Phase

| Phase | 상태 | 설명 |
|-------|------|------|
| App Phase 0 | ✅ 완료 | 프로젝트 설정, Chaquopy 통합 |
| App Phase 1 | ✅ 완료 | 종목 검색, 수급 분석 화면 |
| App Phase 2 | ✅ 완료 | 기술적 지표 화면 (Vico Charts) |
| App Phase 3 | ⛔ 제거됨 | ~~시장 지표, 조건검색 화면~~ |
| App Phase 4 | ✅ 완료 | 설정 화면 (API 키, 투자 모드) |
| App Phase 5 | ✅ 완료 | 자동 스케줄링 (WorkManager) |
| App Phase 6 | ✅ 완료 | 순위정보 (Kotlin REST API 직접 호출) |

## 앱 네비게이션 (Bottom Nav)

| 탭 | 화면 | 기능 |
|----|------|------|
| 🔍 Search | SearchScreen | 종목 검색, 검색 히스토리 |
| 📊 Analysis | AnalysisScreen | 수급 분석, 매매 신호 |
| 📈 Indicator | IndicatorScreen | 기술적 지표 (Trend, Elder, DeMark) |
| 🏆 Ranking | RankingScreen | 순위정보 (호가잔량, 거래량, 신용비율 등) |
| ⚙️ Settings | SettingsScreen | API 키 설정, 스케줄링 설정 |

## 프로젝트 구조

```
app/src/main/
├── java/com/stockapp/
│   ├── App.kt                 # Hilt Application
│   ├── MainActivity.kt        # Main Activity
│   ├── core/
│   │   ├── db/               # Room 데이터베이스 (8 entities, 8 DAOs)
│   │   │   ├── AppDb.kt
│   │   │   ├── dao/          # DAO 인터페이스
│   │   │   └── entity/       # Entity 클래스
│   │   ├── di/               # Hilt DI 모듈
│   │   ├── py/               # Python Bridge
│   │   │   ├── PyClient.kt
│   │   │   └── PyResponse.kt
│   │   ├── api/              # Kiwoom REST API (Kotlin 직접 호출)
│   │   │   ├── ApiModels.kt
│   │   │   ├── TokenManager.kt
│   │   │   └── KiwoomApiClient.kt
│   │   ├── cache/            # 캐시 관리
│   │   ├── state/            # 공유 상태
│   │   └── ui/
│   │       ├── theme/        # Material 3 테마
│   │       └── component/    # 공통 컴포넌트
│   │           ├── chart/    # 차트 컴포넌트
│   │           └── stockinput/  # 종목 입력 컴포넌트
│   ├── feature/
│   │   ├── search/           # 종목 검색 (Phase 1)
│   │   ├── analysis/         # 수급 분석 (Phase 1)
│   │   ├── indicator/        # 기술 지표 (Phase 2)
│   │   ├── settings/         # 설정 (Phase 4)
│   │   ├── scheduling/       # 자동 스케줄링 (Phase 5)
│   │   └── ranking/          # 순위정보 (Phase 6)
│   └── nav/                  # 네비게이션
├── python/
│   └── stock_analyzer/       # Python 패키지 (chart/ 제외)
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

또는 앱 내 Settings 화면에서 API 키 입력 (암호화 저장)

### 빌드 및 실행

```bash
# 빌드
./gradlew build

# 디버그 APK 설치
./gradlew installDebug

# 테스트
./gradlew test

# Lint 검사
./gradlew lint

# Kotlin 코드 포맷팅
./gradlew ktlintFormat
```

## Python 패키지

`stock-analyzer` Python 패키지를 Chaquopy를 통해 Android에서 실행합니다.
차트 모듈(`chart/`)은 Vico Charts로 대체하여 네이티브 렌더링을 사용합니다.

### 포함 모듈
- `client/` - 키움 API 클라이언트
- `stock/` - 종목 검색, 수급 분석, OHLCV
- `indicator/` - 기술적 지표 (Trend, Elder, DeMark, Oscillator)

### 제외 모듈
- `chart/` - Vico Charts로 대체 (matplotlib Android 미지원)
- `market/` - 사용하지 않음
- `search/` - 사용하지 않음 (조건검색)

## 순위정보 (Kotlin REST API)

Python 패키지가 FROZEN 상태이므로, 순위정보 기능은 Kotlin에서 직접 Kiwoom REST API를 호출합니다.

| 순위 유형 | API ID | 설명 |
|----------|--------|------|
| 호가잔량급증 (매수/매도) | ka10021 | 호가잔량 급증 종목 |
| 거래량급증 | ka10023 | 거래량 급증 종목 |
| 당일거래량상위 | ka10030 | 당일 거래량 상위 |
| 신용비율상위 | ka10033 | 신용비율 상위 종목 |
| 외국인기관상위 | ka90009 | 외국인/기관 순매수/순매도 상위 |

## 참고 문서

- [CLAUDE.md](../CLAUDE.md) - 프로젝트 가이드
- [ANDROID_PREPARATION.md](../docs/ANDROID_PREPARATION.md) - Android 개발 준비
- [STOCK_APP_SPEC.md](../docs/STOCK_APP_SPEC.md) - 상세 명세서
- [CODE_REVIEW_REPORT.md](../docs/CODE_REVIEW_REPORT.md) - 코드 리뷰 보고서
