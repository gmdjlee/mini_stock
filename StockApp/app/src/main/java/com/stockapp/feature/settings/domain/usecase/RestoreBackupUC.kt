package com.stockapp.feature.settings.domain.usecase

import android.net.Uri
import com.stockapp.core.backup.RestoreMode
import com.stockapp.core.backup.RestoreProgress
import com.stockapp.core.backup.RestoreResult
import com.stockapp.feature.settings.domain.repo.BackupRepo
import javax.inject.Inject

/**
 * Use case for restoring a database backup.
 */
class RestoreBackupUC @Inject constructor(
    private val backupRepo: BackupRepo
) {
    /**
     * Restore data from a backup file.
     *
     * @param inputUri URI of the backup file
     * @param mode Restore mode (MERGE or REPLACE)
     * @param onProgress Progress callback
     * @return Result containing RestoreResult
     */
    suspend operator fun invoke(
        inputUri: Uri,
        mode: RestoreMode,
        onProgress: (RestoreProgress) -> Unit = {}
    ): Result<RestoreResult> {
        // Load backup file
        onProgress(RestoreProgress.Loading)
        val loadResult = backupRepo.loadBackupFile(inputUri)

        return loadResult.fold(
            onSuccess = { backup ->
                // Validate
                onProgress(RestoreProgress.Validating)

                // Restore
                backupRepo.restoreBackup(backup, mode, onProgress)
            },
            onFailure = { e ->
                Result.failure(e)
            }
        )
    }
}
