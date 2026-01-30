package com.stockapp.feature.settings.domain.repo

import android.net.Uri
import com.stockapp.core.backup.BackupConfig
import com.stockapp.core.backup.BackupFile
import com.stockapp.core.backup.BackupProgress
import com.stockapp.core.backup.RestoreMode
import com.stockapp.core.backup.RestoreProgress
import com.stockapp.core.backup.RestoreResult
import com.stockapp.core.backup.ValidationResult

/**
 * Repository interface for backup operations.
 */
interface BackupRepo {
    /**
     * Create a backup based on the given configuration.
     */
    suspend fun createBackup(
        config: BackupConfig,
        onProgress: (BackupProgress) -> Unit = {}
    ): Result<BackupFile>

    /**
     * Restore data from a backup file.
     */
    suspend fun restoreBackup(
        backup: BackupFile,
        mode: RestoreMode,
        onProgress: (RestoreProgress) -> Unit = {}
    ): Result<RestoreResult>

    /**
     * Load and validate a backup file from URI.
     */
    suspend fun loadBackupFile(uri: Uri): Result<BackupFile>

    /**
     * Save backup file to URI.
     */
    suspend fun saveBackupFile(backup: BackupFile, uri: Uri): Result<Unit>

    /**
     * Validate a backup file from URI.
     */
    suspend fun validateBackup(uri: Uri): Result<ValidationResult>

    /**
     * Get estimated backup size in bytes.
     */
    suspend fun getEstimatedBackupSize(): Long
}
