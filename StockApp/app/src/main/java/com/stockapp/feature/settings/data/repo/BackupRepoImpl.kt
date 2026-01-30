package com.stockapp.feature.settings.data.repo

import android.content.Context
import android.net.Uri
import com.stockapp.core.backup.BackupConfig
import com.stockapp.core.backup.BackupFile
import com.stockapp.core.backup.BackupManager
import com.stockapp.core.backup.BackupMigrator
import com.stockapp.core.backup.BackupProgress
import com.stockapp.core.backup.BackupSerializer
import com.stockapp.core.backup.RestoreMode
import com.stockapp.core.backup.RestoreProgress
import com.stockapp.core.backup.RestoreResult
import com.stockapp.core.backup.ValidationResult
import com.stockapp.core.di.IoDispatcher
import com.stockapp.feature.settings.domain.repo.BackupRepo
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Implementation of BackupRepo.
 */
@Singleton
class BackupRepoImpl @Inject constructor(
    @ApplicationContext private val context: Context,
    private val backupManager: BackupManager,
    private val serializer: BackupSerializer,
    private val migrator: BackupMigrator,
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher
) : BackupRepo {

    override suspend fun createBackup(
        config: BackupConfig,
        onProgress: (BackupProgress) -> Unit
    ): Result<BackupFile> = withContext(ioDispatcher) {
        runCatching {
            backupManager.createBackup(
                backupType = config.backupType,
                startDate = config.startDate,
                endDate = config.endDate
            ) { progress, message ->
                onProgress(BackupProgress.Creating(progress, message))
            }
        }
    }

    override suspend fun restoreBackup(
        backup: BackupFile,
        mode: RestoreMode,
        onProgress: (RestoreProgress) -> Unit
    ): Result<RestoreResult> = withContext(ioDispatcher) {
        runCatching {
            // Migrate if needed
            val migratedBackup = if (migrator.needsMigration(backup)) {
                onProgress(RestoreProgress.Migrating)
                migrator.migrate(backup)
            } else {
                backup
            }

            // Restore
            backupManager.restoreBackup(migratedBackup, mode) { progress, message ->
                onProgress(RestoreProgress.Restoring(progress, message))
            }.also {
                onProgress(RestoreProgress.Complete)
            }
        }
    }

    override suspend fun loadBackupFile(uri: Uri): Result<BackupFile> = withContext(ioDispatcher) {
        runCatching {
            val content = context.contentResolver.openInputStream(uri)?.use { stream ->
                stream.bufferedReader().readText()
            } ?: throw IOException("파일을 열 수 없습니다")

            val backup = serializer.deserialize(content)

            // Check if format is supported
            if (!migrator.isSupported(backup)) {
                throw IllegalStateException(
                    "지원하지 않는 백업 형식입니다 (버전: ${backup.metadata.formatVersion})"
                )
            }

            backup
        }
    }

    override suspend fun saveBackupFile(backup: BackupFile, uri: Uri): Result<Unit> = withContext(ioDispatcher) {
        runCatching {
            val content = serializer.serialize(backup)
            context.contentResolver.openOutputStream(uri)?.use { stream ->
                stream.write(content.toByteArray(Charsets.UTF_8))
            } ?: throw IOException("파일을 저장할 수 없습니다")
        }
    }

    override suspend fun validateBackup(uri: Uri): Result<ValidationResult> = withContext(ioDispatcher) {
        runCatching {
            val content = context.contentResolver.openInputStream(uri)?.use { stream ->
                stream.bufferedReader().readText()
            } ?: throw IOException("파일을 열 수 없습니다")

            serializer.validate(content)
        }
    }

    override suspend fun getEstimatedBackupSize(): Long = withContext(ioDispatcher) {
        // Simple estimation based on typical data sizes
        // This could be improved by actually counting rows
        val dbFile = context.getDatabasePath("stock_app.db")
        if (dbFile.exists()) {
            // JSON is typically 2-3x the size of SQLite data
            (dbFile.length() * 2.5).toLong()
        } else {
            0L
        }
    }
}
