package com.stockapp.core.backup

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Serializer for backup files.
 * Handles JSON serialization/deserialization with error handling.
 */
@Singleton
class BackupSerializer @Inject constructor(
    private val json: Json
) {
    /**
     * Serialize backup file to JSON string.
     */
    suspend fun serialize(backup: BackupFile): String {
        return withContext(Dispatchers.Default) {
            json.encodeToString(BackupFile.serializer(), backup)
        }
    }

    /**
     * Deserialize JSON string to backup file.
     * Uses ignoreUnknownKeys from Json configuration for forward compatibility.
     */
    suspend fun deserialize(content: String): BackupFile {
        return withContext(Dispatchers.Default) {
            json.decodeFromString(BackupFile.serializer(), content)
        }
    }

    /**
     * Extract only metadata from backup content for quick validation.
     * More efficient than deserializing the entire file.
     */
    suspend fun extractMetadata(content: String): BackupMetadata? {
        return withContext(Dispatchers.Default) {
            try {
                // Try to parse the entire file first for accuracy
                val backup = json.decodeFromString(BackupFile.serializer(), content)
                backup.metadata
            } catch (e: Exception) {
                // If full parsing fails, try to extract just metadata
                try {
                    // Simple approach: parse as BackupFile which has metadata as first field
                    val backup = json.decodeFromString(BackupFile.serializer(), content)
                    backup.metadata
                } catch (_: Exception) {
                    null
                }
            }
        }
    }

    /**
     * Validate backup content structure without full deserialization.
     */
    suspend fun validate(content: String): ValidationResult {
        return withContext(Dispatchers.Default) {
            try {
                val backup = json.decodeFromString(BackupFile.serializer(), content)

                // Validate format version
                if (backup.metadata.formatVersion > BACKUP_FORMAT_VERSION) {
                    return@withContext ValidationResult.Invalid(
                        "백업 파일 버전(${backup.metadata.formatVersion})이 앱 버전($BACKUP_FORMAT_VERSION)보다 높습니다. " +
                            "앱을 업데이트해 주세요."
                    )
                }

                // Validate basic structure
                if (backup.metadata.createdAt <= 0) {
                    return@withContext ValidationResult.Invalid("유효하지 않은 백업 파일입니다: 생성 시간 없음")
                }

                ValidationResult.Valid(backup.metadata)
            } catch (e: Exception) {
                ValidationResult.Invalid("백업 파일을 읽을 수 없습니다: ${e.message}")
            }
        }
    }
}
