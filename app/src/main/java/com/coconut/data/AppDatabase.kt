package com.coconut.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(
    entities = [
        SnappedAppEntity::class,
        ProtectionConfigurationEntity::class,
        DevicePolicyStateEntity::class,
        SecurityAuditEventEntity::class
    ],
    version = 4,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun snappedAppDao(): SnappedAppDao
    abstract fun protectionConfigDao(): ProtectionConfigDao
    abstract fun devicePolicyStateDao(): DevicePolicyStateDao
    abstract fun securityAuditDao(): SecurityAuditDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE snapped_apps ADD COLUMN firstSnappedAt INTEGER NOT NULL DEFAULT 0")
                db.execSQL("ALTER TABLE snapped_apps ADD COLUMN lastAppliedAt INTEGER NOT NULL DEFAULT 0")
                db.execSQL("ALTER TABLE snapped_apps ADD COLUMN desiredState TEXT NOT NULL DEFAULT 'SUSPENDED'")
                db.execSQL("ALTER TABLE snapped_apps ADD COLUMN actualState TEXT NOT NULL DEFAULT 'SNAPPED — PROTECTED'")
                db.execSQL("ALTER TABLE snapped_apps ADD COLUMN installationState TEXT NOT NULL DEFAULT 'INSTALLED'")
                db.execSQL("ALTER TABLE snapped_apps ADD COLUMN lastError TEXT DEFAULT NULL")
                db.execSQL("ALTER TABLE snapped_apps ADD COLUMN createdAt INTEGER NOT NULL DEFAULT 0")
                db.execSQL("ALTER TABLE snapped_apps ADD COLUMN updatedAt INTEGER NOT NULL DEFAULT 0")

                db.execSQL("UPDATE snapped_apps SET firstSnappedAt = snappedAt, lastAppliedAt = snappedAt, createdAt = snappedAt, updatedAt = snappedAt WHERE firstSnappedAt = 0")

                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS protection_config (
                        id INTEGER PRIMARY KEY NOT NULL,
                        contentShieldEnabled INTEGER NOT NULL,
                        dnsProviderName TEXT NOT NULL,
                        dnsHostname TEXT NOT NULL,
                        selfProtectionEnabled INTEGER NOT NULL,
                        disallowConfigVpn INTEGER NOT NULL,
                        disallowDebugging INTEGER NOT NULL,
                        disallowUnknownSources INTEGER NOT NULL,
                        disallowSafeBoot INTEGER NOT NULL,
                        disallowFactoryReset INTEGER NOT NULL,
                        disallowAppsControl INTEGER NOT NULL,
                        updatedAt INTEGER NOT NULL
                    )
                """.trimIndent())

                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS device_policy_state (
                        id INTEGER PRIMARY KEY NOT NULL,
                        isDeviceOwner INTEGER NOT NULL,
                        isUninstallBlocked INTEGER NOT NULL,
                        activeDnsMode TEXT NOT NULL,
                        activeDnsHost TEXT,
                        isDnsProtected INTEGER NOT NULL,
                        totalSnappedCount INTEGER NOT NULL,
                        activeSnappedCount INTEGER NOT NULL,
                        lastReconciledAt INTEGER NOT NULL,
                        reconciliationStatus TEXT NOT NULL
                    )
                """.trimIndent())

                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS security_audit_events (
                        id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        timestamp INTEGER NOT NULL,
                        eventType TEXT NOT NULL,
                        target TEXT,
                        details TEXT NOT NULL,
                        severity TEXT NOT NULL
                    )
                """.trimIndent())
            }
        }

        val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE protection_config ADD COLUMN disallowConfigPrivateDns INTEGER NOT NULL DEFAULT 1")
            }
        }

        val MIGRATION_3_4 = object : Migration(3, 4) {
            override fun migrate(db: SupportSQLiteDatabase) {
                // Non-destructive migration to Version 4:
                // 1. Add uninstallBlocked column to snapped_apps
                db.execSQL("ALTER TABLE snapped_apps ADD COLUMN uninstallBlocked INTEGER NOT NULL DEFAULT 0")

                // 2. Add lastVerifiedAt column to snapped_apps
                db.execSQL("ALTER TABLE snapped_apps ADD COLUMN lastVerifiedAt INTEGER NOT NULL DEFAULT 0")

                // 3. Migrate legacy actualState strings to typed status values
                db.execSQL("UPDATE snapped_apps SET actualState = 'SNAP_ACTIVE' WHERE actualState = 'SNAPPED — PROTECTED'")
                db.execSQL("UPDATE snapped_apps SET actualState = 'AWAITING_INSTALLATION' WHERE actualState = 'SNAPPED — AWAITING INSTALLATION'")

                // 4. Correct flawed defaults in existing records:
                // Content Shield owns Private DNS restriction: if Content Shield is OFF, clear disallowConfigPrivateDns
                db.execSQL("UPDATE protection_config SET disallowConfigPrivateDns = 0 WHERE contentShieldEnabled = 0")

                // Self-protection must not be automatically forced: default to 0 (false)
                db.execSQL("UPDATE protection_config SET selfProtectionEnabled = 0")
            }
        }

        fun get(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "coconut_snap.db"
                )
                    .addMigrations(MIGRATION_1_2, MIGRATION_2_3, MIGRATION_3_4)
                    .build()
                    .also { INSTANCE = it }
            }
        }
    }
}
