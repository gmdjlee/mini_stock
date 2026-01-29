package com.stockapp.feature.settings.domain.usecase

import android.net.Uri
import com.stockapp.core.backup.BackupMetadata
import com.stockapp.core.backup.ValidationResult
import com.stockapp.feature.settings.domain.repo.BackupRepo
import javax.inject.Inject

/**
 * Use case for validating a backup file.
 */
class ValidateBackupUC @Inject constructor(
    private val backupRepo: BackupRepo
) {
    /**
     * Validate a backup file and return metadata if valid.
     *
     * @param inputUri URI of the backup file
     * @return Result containing ValidationResult
     */
    suspend operator fun invoke(inputUri: Uri): Result<ValidationResult> {
        return backupRepo.validateBackup(inputUri)
    }
}

/**
 * Use case for loading backup file metadata.
 */
class LoadBackupMetadataUC @Inject constructor(
    private val backupRepo: BackupRepo
) {
    /**
     * Load backup file and return its metadata.
     *
     * @param inputUri URI of the backup file
     * @return Result containing BackupMetadata
     */
    suspend operator fun invoke(inputUri: Uri): Result<BackupMetadata> {
        return backupRepo.loadBackupFile(inputUri).map { it.metadata }
    }
}
