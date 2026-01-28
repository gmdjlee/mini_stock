# 동기화 스케줄링 기능 개선 계획

> 작성일: 2026-01-28
> 브랜치: `claude/fix-sync-scheduling-s5xyu`

## 개요

Settings 화면의 Scheduling 탭 동기화 기능에 대한 개선 작업입니다.

## 요구사항

1. 오류 발생 시 데이터 동기화 및 수집 오류 메시지를 동기화 기록에 표시하고 강제 정지
2. 오류로 정지된 job은 다시 실행하지 않도록 설정
3. 동기화 기록의 에러 메시지 삭제 기능 추가
4. key, secret이 설정되어 있음에도 key 설정이 되지 않았다고 표시되는 문제 수정
5. 스케줄링에 지정된 시간에만 데이터 동기화가 작동하는지 확인

---

## 분석 결과

### 현재 구현 상태

| # | 요구사항 | 현재 상태 | 조치 필요 |
|---|---------|----------|----------|
| 1 | 오류 메시지 표시 | ✅ `SyncHistoryEntity.errorMessage`에 저장 및 UI 표시됨 | 없음 |
| 2 | 오류 시 강제 정지 | ❌ `MAX_RETRY_COUNT=3`으로 3회 재시도 | 재시도 로직 제거 |
| 3 | 오류 후 재실행 방지 | ❌ 실패해도 다음 스케줄에 재실행됨 | `isErrorStopped` 플래그 추가 |
| 4 | 동기화 기록 삭제 | ❌ 삭제 기능 없음 | 스와이프 삭제 UI 추가 |
| 5 | API 키 문제 | ❌ 네트워크 필요한 `testApiKey()` 호출 | 네트워크 없이 초기화 |
| 6 | 스케줄링 시간 | ✅ WorkManager `PeriodicWorkRequest` 정상 작동 | 없음 |

### API 키 "설정되지 않음" 문제 원인

**문제**: `App.kt`의 `initializeWithSavedKeys()`가 `testApiKey()`를 호출하는데, 이 함수는 실제 Kiwoom OAuth API를 호출하여 키를 검증합니다.

```kotlin
// SettingsRepoImpl.kt (현재)
override suspend fun initializeWithSavedKeys(): Result<Boolean> {
    val config = getApiKeyConfig().first()
    if (config.isValid()) {
        testApiKey(config)  // <- 네트워크 필요!
    }
}
```

앱 시작 시 네트워크가 불안정하면:
1. 키가 EncryptedSharedPreferences에 저장되어 있어도
2. `testApiKey()`가 실패하여 `PyClient`가 초기화되지 않음
3. `PyClient.isReady()` = `false`
4. "API 키가 설정되지 않았습니다" 오류 발생

**해결**: 앱 시작 시 네트워크 테스트 없이 `PyClient.initialize()` 직접 호출

---

## 구현 계획

### 1단계: 데이터베이스 변경

#### 1.1 Entity 수정

**파일**: `StockApp/app/src/main/java/com/stockapp/core/db/entity/SchedulingEntity.kt`

`SchedulingConfigEntity`에 필드 추가:
```kotlin
@Entity(tableName = "scheduling_config")
data class SchedulingConfigEntity(
    @PrimaryKey
    val id: Int = 1,
    val isEnabled: Boolean = true,
    val syncHour: Int = 1,
    val syncMinute: Int = 0,
    val lastSyncAt: Long = 0L,
    val lastSyncStatus: String = "NEVER",
    val lastSyncMessage: String? = null,
    val isErrorStopped: Boolean = false,  // 추가: 오류로 중지됨 플래그
    val updatedAt: Long = System.currentTimeMillis()
)
```

#### 1.2 DAO 수정

**파일**: `StockApp/app/src/main/java/com/stockapp/core/db/dao/SchedulingDao.kt`

```kotlin
// SchedulingConfigDao에 추가
@Query("UPDATE scheduling_config SET isErrorStopped = :stopped, updatedAt = :updatedAt WHERE id = 1")
suspend fun setErrorStopped(stopped: Boolean, updatedAt: Long = System.currentTimeMillis())

// SyncHistoryDao에 추가
@Query("DELETE FROM sync_history WHERE id = :id")
suspend fun deleteById(id: Long)
```

#### 1.3 데이터베이스 마이그레이션

**파일**: `StockApp/app/src/main/java/com/stockapp/core/db/AppDb.kt`

```kotlin
val MIGRATION_X_Y = object : Migration(X, Y) {
    override fun migrate(database: SupportSQLiteDatabase) {
        database.execSQL(
            "ALTER TABLE scheduling_config ADD COLUMN isErrorStopped INTEGER NOT NULL DEFAULT 0"
        )
    }
}
```

---

### 2단계: Domain Layer 수정

#### 2.1 Model 수정

**파일**: `StockApp/app/src/main/java/com/stockapp/feature/scheduling/domain/model/SchedulingModels.kt`

```kotlin
data class SchedulingConfig(
    val isEnabled: Boolean = true,
    val syncHour: Int = 1,
    val syncMinute: Int = 0,
    val lastSyncAt: Long = 0L,
    val lastSyncStatus: SyncStatus = SyncStatus.NEVER,
    val lastSyncMessage: String? = null,
    val isErrorStopped: Boolean = false  // 추가
)
```

#### 2.2 Repository Interface 수정

**파일**: `StockApp/app/src/main/java/com/stockapp/feature/scheduling/domain/repo/SchedulingRepo.kt`

```kotlin
interface SchedulingRepo {
    // 기존 메서드들...

    /**
     * 동기화 기록 삭제
     */
    suspend fun deleteSyncHistory(id: Long)

    /**
     * 오류 중지 플래그 설정
     */
    suspend fun setErrorStopped(stopped: Boolean)

    /**
     * 오류 해제 및 스케줄링 재개
     */
    suspend fun clearErrorAndResume()
}
```

---

### 3단계: Data Layer 수정

#### 3.1 SchedulingRepoImpl 수정

**파일**: `StockApp/app/src/main/java/com/stockapp/feature/scheduling/data/repo/SchedulingRepoImpl.kt`

```kotlin
override suspend fun deleteSyncHistory(id: Long) {
    syncHistoryDao.deleteById(id)
}

override suspend fun setErrorStopped(stopped: Boolean) {
    ensureConfigExists()
    configDao.setErrorStopped(stopped)
}

override suspend fun clearErrorAndResume() {
    ensureConfigExists()
    configDao.setErrorStopped(false)
}

// toDomain() 매핑 업데이트
private fun SchedulingConfigEntity.toDomain() = SchedulingConfig(
    isEnabled = isEnabled,
    syncHour = syncHour,
    syncMinute = syncMinute,
    lastSyncAt = lastSyncAt,
    lastSyncStatus = SyncStatus.fromString(lastSyncStatus),
    lastSyncMessage = lastSyncMessage,
    isErrorStopped = isErrorStopped  // 추가
)
```

#### 3.2 SettingsRepoImpl 수정 (API 키 문제 해결)

**파일**: `StockApp/app/src/main/java/com/stockapp/feature/settings/data/repo/SettingsRepoImpl.kt`

```kotlin
override suspend fun initializeWithSavedKeys(): Result<Boolean> {
    return try {
        val config = getApiKeyConfig().first()
        if (config.isValid()) {
            // 변경: 네트워크 테스트 없이 직접 초기화
            val baseUrl = when (config.investmentMode) {
                InvestmentMode.MOCK -> MOCK_URL
                InvestmentMode.PRODUCTION -> PROD_URL
            }

            pyClient.initialize(
                appKey = config.appKey,
                secretKey = config.secretKey,
                baseUrl = baseUrl
            ).fold(
                onSuccess = { Result.success(true) },
                onFailure = { e -> Result.failure(e) }
            )
        } else {
            Result.success(false)
        }
    } catch (e: Exception) {
        Result.failure(e)
    }
}
```

---

### 4단계: Worker 수정

**파일**: `StockApp/app/src/main/java/com/stockapp/feature/scheduling/worker/StockSyncWorker.kt`

```kotlin
@HiltWorker
class StockSyncWorker @AssistedInject constructor(
    @Assisted appContext: Context,
    @Assisted workerParams: WorkerParameters,
    private val schedulingRepo: SchedulingRepo
) : CoroutineWorker(appContext, workerParams) {

    override suspend fun doWork(): Result {
        Log.d(TAG, "doWork() started")

        val config = schedulingRepo.getConfig()

        // 변경: 오류 중지 상태 체크 추가
        if (!config.isEnabled || config.isErrorStopped) {
            Log.d(TAG, "Sync is disabled or error-stopped, skipping")
            return Result.success()
        }

        return try {
            val syncTypeStr = inputData.getString(KEY_SYNC_TYPE) ?: SyncType.SCHEDULED.name
            val syncType = try {
                SyncType.valueOf(syncTypeStr)
            } catch (e: Exception) {
                SyncType.SCHEDULED
            }

            Log.d(TAG, "Executing sync, type=$syncType")

            val result = schedulingRepo.syncAllData(syncType)

            if (result.success) {
                Log.d(TAG, "Sync completed successfully")
                Result.success()
            } else {
                Log.w(TAG, "Sync failed: ${result.errorMessage}")
                // 변경: 재시도 없이 오류 플래그 설정 후 즉시 실패
                schedulingRepo.setErrorStopped(true)
                Result.failure()
            }
        } catch (e: Exception) {
            Log.e(TAG, "Sync exception: ${e.message}", e)
            // 변경: 재시도 없이 오류 플래그 설정 후 즉시 실패
            schedulingRepo.setErrorStopped(true)
            Result.failure()
        }
    }

    companion object {
        const val WORK_NAME_PERIODIC = "stock_sync_periodic"
        const val WORK_NAME_ONCE = "stock_sync_once"
        const val KEY_SYNC_TYPE = "sync_type"
        // 삭제: private const val MAX_RETRY_COUNT = 3
    }
}
```

---

### 5단계: ViewModel 수정

**파일**: `StockApp/app/src/main/java/com/stockapp/feature/scheduling/ui/SchedulingVm.kt`

```kotlin
/**
 * 동기화 기록 삭제
 */
fun deleteSyncHistory(historyId: Long) {
    viewModelScope.launch {
        try {
            schedulingRepo.deleteSyncHistory(historyId)
        } catch (e: Exception) {
            _error.value = "삭제 실패: ${e.message}"
        }
    }
}

/**
 * 오류 해제 및 스케줄링 재개
 */
fun clearErrorAndResume() {
    viewModelScope.launch {
        try {
            schedulingRepo.clearErrorAndResume()
            // 활성화된 경우 스케줄 재등록
            val config = schedulingRepo.getConfig()
            if (config.isEnabled) {
                schedulingManager.scheduleDailySync(config.syncHour, config.syncMinute)
            }
        } catch (e: Exception) {
            _error.value = e.message
        }
    }
}
```

---

### 6단계: UI 수정

**파일**: `StockApp/app/src/main/java/com/stockapp/feature/scheduling/ui/SchedulingTab.kt`

#### 6.1 오류 중지 배너 추가

자동 동기화 토글 카드 다음에 추가:

```kotlin
// 오류로 중지됨 배너
item {
    AnimatedVisibility(visible = uiState.config.isErrorStopped) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.errorContainer
            )
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "동기화 일시 중지됨",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onErrorContainer
                    )
                    Text(
                        text = "이전 동기화가 실패하여 자동 동기화가 중지되었습니다.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onErrorContainer
                    )
                }
                TextButton(onClick = { viewModel.clearErrorAndResume() }) {
                    Text("재개", color = MaterialTheme.colorScheme.error)
                }
            }
        }
    }
}
```

#### 6.2 스와이프 삭제 기능 추가

```kotlin
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SwipeableSyncHistoryItem(
    history: SyncHistory,
    onDelete: (Long) -> Unit
) {
    val dismissState = rememberSwipeToDismissBoxState(
        confirmValueChange = { value ->
            if (value == SwipeToDismissBoxValue.EndToStart) {
                onDelete(history.id)
                true
            } else {
                false
            }
        }
    )

    SwipeToDismissBox(
        state = dismissState,
        backgroundContent = {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(MaterialTheme.colorScheme.error)
                    .padding(horizontal = 16.dp),
                contentAlignment = Alignment.CenterEnd
            ) {
                Icon(
                    imageVector = Icons.Default.Delete,
                    contentDescription = "삭제",
                    tint = MaterialTheme.colorScheme.onError
                )
            }
        },
        content = {
            SyncHistoryItem(history = history)
        },
        enableDismissFromStartToEnd = false,
        enableDismissFromEndToStart = true
    )
}
```

동기화 기록 목록 부분 수정:

```kotlin
items(uiState.syncHistory, key = { it.id }) { history ->
    SwipeableSyncHistoryItem(
        history = history,
        onDelete = { viewModel.deleteSyncHistory(it) }
    )
}
```

---

## 수정 파일 요약

| # | 파일 경로 | 변경 내용 |
|---|----------|----------|
| 1 | `core/db/entity/SchedulingEntity.kt` | `isErrorStopped` 필드 추가 |
| 2 | `core/db/dao/SchedulingDao.kt` | `setErrorStopped()`, `deleteById()` 추가 |
| 3 | `core/db/AppDb.kt` | 데이터베이스 마이그레이션 추가 |
| 4 | `feature/scheduling/domain/model/SchedulingModels.kt` | `isErrorStopped` 필드 추가 |
| 5 | `feature/scheduling/domain/repo/SchedulingRepo.kt` | 인터페이스 메서드 추가 |
| 6 | `feature/scheduling/data/repo/SchedulingRepoImpl.kt` | 구현체 메서드 추가 |
| 7 | `feature/scheduling/worker/StockSyncWorker.kt` | 재시도 로직 제거, 오류 플래그 설정 |
| 8 | `feature/scheduling/ui/SchedulingVm.kt` | `deleteSyncHistory()`, `clearErrorAndResume()` 추가 |
| 9 | `feature/scheduling/ui/SchedulingTab.kt` | 오류 배너, 스와이프 삭제 UI 추가 |
| 10 | `feature/settings/data/repo/SettingsRepoImpl.kt` | 네트워크 없이 초기화하도록 수정 |

---

## 테스트 체크리스트

- [ ] 동기화 실패 시 즉시 중지되고 재시도하지 않음
- [ ] 오류 발생 시 "동기화 일시 중지됨" 배너 표시
- [ ] "재개" 버튼 클릭 시 오류 플래그 해제 및 스케줄 재등록
- [ ] 동기화 기록 스와이프 삭제 동작
- [ ] 네트워크 없이 앱 시작 시 저장된 API 키로 PyClient 초기화 성공
- [ ] 스케줄링된 시간에만 자동 동기화 실행 확인

---

## 참고 사항

- 기존 코드 패턴 및 설계 원칙 준수
- Room 데이터베이스 마이그레이션 필수
- Material3 SwipeToDismissBox 컴포넌트 사용
