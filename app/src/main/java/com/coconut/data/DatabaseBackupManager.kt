package com.coconut.data

import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.util.Log
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream

data class BackupVerificationResult(
    val isValid: Boolean,
    val message: String,
    val tableCount: Int = 0,
    val recordCount: Int = 0
)

object DatabaseBackupManager {

    private const val TAG = "DatabaseBackupManager"
    private const val DB_NAME = "coconut_snap.db"
    private const val BACKUP_DIR_NAME = "database_backups"
    private const val PRE_MIGRATION_BACKUP_NAME = "coconut_snap_pre_v7.db"

    private val SQLITE_HEADER = byteArrayOf(
        0x53, 0x51, 0x4C, 0x69, 0x74, 0x65, 0x20, 0x66,
        0x6F, 0x72, 0x6D, 0x61, 0x74, 0x20, 0x33, 0x00
    )

    /**
     * Creates an immutable pre-migration backup of the existing database before any schema migration runs.
     * Opens the backup independently, executes PRAGMA integrity_check, validates schema tables,
     * and verifies representative records before confirming success.
     */
    fun createPreMigrationBackupIfNeeded(context: Context): File? {
        val dbFile = context.getDatabasePath(DB_NAME)
        if (!dbFile.exists() || dbFile.length() == 0L) {
            Log.i(TAG, "No existing database file found at ${dbFile.absolutePath}; skipping pre-migration backup.")
            return null
        }

        val backupDir = File(context.filesDir, BACKUP_DIR_NAME)
        if (!backupDir.exists()) {
            backupDir.mkdirs()
        }

        val backupFile = File(backupDir, PRE_MIGRATION_BACKUP_NAME)
        if (backupFile.exists() && backupFile.length() > 0L) {
            val verification = verifyBackupConsistency(backupFile)
            if (verification.isValid) {
                Log.i(TAG, "Verified pre-migration backup already exists at ${backupFile.absolutePath}.")
                return backupFile
            }
        }

        try {
            // Copy main database file
            copyFile(dbFile, backupFile)

            // Copy WAL and SHM transaction logs if present to capture uncommitted checkpoints
            val walFile = File(dbFile.parentFile, "$DB_NAME-wal")
            if (walFile.exists()) {
                val backupWal = File(backupDir, "$PRE_MIGRATION_BACKUP_NAME-wal")
                copyFile(walFile, backupWal)
            }

            val shmFile = File(dbFile.parentFile, "$DB_NAME-shm")
            if (shmFile.exists()) {
                val backupShm = File(backupDir, "$PRE_MIGRATION_BACKUP_NAME-shm")
                copyFile(shmFile, backupShm)
            }

            // Thorough consistency verification
            val verification = verifyBackupConsistency(backupFile)
            if (!verification.isValid) {
                Log.e(TAG, "Backup integrity verification FAILED: ${verification.message}")
                return null
            }

            Log.i(TAG, "Pre-migration backup successfully created and verified (${backupFile.length()} bytes, ${verification.message})")
            return backupFile
        } catch (e: Exception) {
            Log.e(TAG, "Failed to create pre-migration backup", e)
            return null
        }
    }

    /**
     * Genuine backup consistency verification:
     * 1. Magic header validation
     * 2. Independent opening via SQLite
     * 3. PRAGMA integrity_check execution
     * 4. Schema validation (essential tables exist)
     * 5. Representative record validation
     */
    fun verifyBackupConsistency(file: File): BackupVerificationResult {
        if (!file.exists() || file.length() < 16L) {
            return BackupVerificationResult(false, "File does not exist or is too small")
        }

        // 1. Verify SQLite 3 magic header
        val header = ByteArray(16)
        try {
            FileInputStream(file).use { input ->
                val bytesRead = input.read(header)
                if (bytesRead < 16 || !header.contentEquals(SQLITE_HEADER)) {
                    return BackupVerificationResult(false, "Invalid SQLite 3 file header")
                }
            }
        } catch (e: Exception) {
            return BackupVerificationResult(false, "Failed to read header: ${e.message}")
        }

        // 2. Open independently with SQLiteDatabase in read-only mode
        return try {
            val db = SQLiteDatabase.openDatabase(
                file.absolutePath,
                null,
                SQLiteDatabase.OPEN_READONLY
            )
            try {
                // 3. PRAGMA integrity_check
                val integrityCursor = db.rawQuery("PRAGMA integrity_check", null)
                var integrityOk = false
                var integrityMessage = "unknown"
                if (integrityCursor.moveToFirst()) {
                    integrityMessage = integrityCursor.getString(0)
                    integrityOk = integrityMessage.equals("ok", ignoreCase = true)
                }
                integrityCursor.close()

                if (!integrityOk) {
                    return BackupVerificationResult(false, "PRAGMA integrity_check failed: $integrityMessage")
                }

                // 4. Validate schema: check that core tables exist
                val tableCursor = db.rawQuery(
                    "SELECT count(*) FROM sqlite_master WHERE type='table' AND name IN ('dns_configuration', 'snapped_apps', 'dns_schedule')",
                    null
                )
                var tableCount = 0
                if (tableCursor.moveToFirst()) {
                    tableCount = tableCursor.getInt(0)
                }
                tableCursor.close()

                if (tableCount < 3) {
                    return BackupVerificationResult(
                        false,
                        "Schema validation incomplete: found $tableCount of 3 expected core tables",
                        tableCount = tableCount
                    )
                }

                // 5. Validate representative record
                val recordCursor = db.rawQuery("SELECT count(*) FROM dns_configuration WHERE id = 1", null)
                var recordCount = 0
                if (recordCursor.moveToFirst()) {
                    recordCount = recordCursor.getInt(0)
                }
                recordCursor.close()

                if (recordCount < 1) {
                    return BackupVerificationResult(
                        false,
                        "Record validation failed: dns_configuration record missing",
                        tableCount = tableCount,
                        recordCount = 0
                    )
                }

                BackupVerificationResult(
                    isValid = true,
                    message = "PRAGMA integrity_check OK, $tableCount core tables verified, $recordCount config record verified",
                    tableCount = tableCount,
                    recordCount = recordCount
                )
            } finally {
                db.close()
            }
        } catch (e: Exception) {
            BackupVerificationResult(false, "SQLite independent open verification failed: ${e.message}")
        }
    }

    /**
     * Restores the database from a verified backup file.
     * Ensures backup file consistency is validated before replacing the live database.
     */
    fun restoreFromBackup(context: Context, backupFile: File): Boolean {
        val verification = verifyBackupConsistency(backupFile)
        if (!verification.isValid) {
            Log.e(TAG, "Cannot restore: backup file is invalid (${verification.message}).")
            return false
        }

        val targetDb = context.getDatabasePath(DB_NAME)
        return try {
            copyFile(backupFile, targetDb)

            // Also restore WAL and SHM if present
            val backupWal = File(backupFile.parentFile, "${backupFile.name}-wal")
            if (backupWal.exists()) {
                val targetWal = File(targetDb.parentFile, "$DB_NAME-wal")
                copyFile(backupWal, targetWal)
            }

            val backupShm = File(backupFile.parentFile, "${backupFile.name}-shm")
            if (backupShm.exists()) {
                val targetShm = File(targetDb.parentFile, "$DB_NAME-shm")
                copyFile(backupShm, targetShm)
            }

            Log.i(TAG, "Database successfully restored from ${backupFile.absolutePath}")
            true
        } catch (e: Exception) {
            Log.e(TAG, "Failed to restore database from backup", e)
            false
        }
    }

    private fun copyFile(source: File, destination: File) {
        FileInputStream(source).use { input ->
            FileOutputStream(destination).use { output ->
                input.channel.transferTo(0, source.length(), output.channel)
            }
        }
    }
}
