package com.stockapp.core.backup

import javax.inject.Inject
import javax.inject.Singleton

/**
 * Migrator for backup file format versions.
 * Handles migration from older backup formats to the current format.
 */
@Singleton
class BackupMigrator @Inject constructor() {

    /**
     * Migrate a backup file to the current format version if needed.
     *
     * @param backup The backup file to migrate
     * @return The migrated backup file
     */
    fun migrate(backup: BackupFile): BackupFile {
        var current = backup
        val fromVersion = backup.metadata.formatVersion

        // Apply migrations sequentially
        if (fromVersion < 1) {
            current = migrateV0toV1(current)
        }

        // Future migrations would be added here:
        // if (fromVersion < 2 && BACKUP_FORMAT_VERSION >= 2) {
        //     current = migrateV1toV2(current)
        // }

        // Update metadata to current version
        return current.copy(
            metadata = current.metadata.copy(
                formatVersion = BACKUP_FORMAT_VERSION
            )
        )
    }

    /**
     * Check if migration is needed.
     */
    fun needsMigration(backup: BackupFile): Boolean {
        return backup.metadata.formatVersion < BACKUP_FORMAT_VERSION
    }

    /**
     * Check if backup format is supported.
     */
    fun isSupported(backup: BackupFile): Boolean {
        // We support all versions up to and including current version
        return backup.metadata.formatVersion <= BACKUP_FORMAT_VERSION
    }

    // ============================================================
    // Migration functions
    // ============================================================

    /**
     * Migration from version 0 (hypothetical pre-release) to version 1.
     * This is a placeholder for the initial version.
     */
    private fun migrateV0toV1(backup: BackupFile): BackupFile {
        // Version 1 is the initial format, so no actual migration needed
        // This is here as an example for future migrations
        return backup
    }

    // Example of future migration:
    // private fun migrateV1toV2(backup: BackupFile): BackupFile {
    //     // Example: Add new field with default value
    //     val updatedTables = backup.tables.copy(
    //         // Transform data as needed
    //     )
    //     return backup.copy(tables = updatedTables)
    // }
}
