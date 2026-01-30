package com.stockapp.feature.settings.domain.usecase

import android.net.Uri
import com.stockapp.core.backup.BackupConfig
import com.stockapp.core.backup.BackupProgress
import com.stockapp.feature.settings.domain.repo.BackupRepo
import javax.inject.Inject

/**
 * Use case for creating a database backup.
 */
class CreateBackupUC @Inject constructor(
    private val backupRepo: BackupRepo
) {
    /**
     * Create a backup and save it to the specified URI.
     *
     * @param config Backup configuration (type, date range)
     * @param outputUri URI to save the backup file
     * @param onProgress Progress callback
     * @return Result indicating success or failure
     */
    suspend operator fun invoke(
        config: BackupConfig,
        outputUri: Uri,
        onProgress: (BackupProgress) -> Unit = {}
    ): Result<Unit> {
        // Create backup
        val backupResult = backupRepo.createBackup(config, onProgress)

        return backupResult.fold(
            onSuccess = { backup ->
                // Save to file
                onProgress(BackupProgress.Saving)
                val saveResult = backupRepo.saveBackupFile(backup, outputUri)
                saveResult.fold(
                    onSuccess = {
                        onProgress(BackupProgress.Complete)
                        Result.success(Unit)
                    },
                    onFailure = { e ->
                        Result.failure(e)
                    }
                )
            },
            onFailure = { e ->
                Result.failure(e)
            }
        )
    }
}
