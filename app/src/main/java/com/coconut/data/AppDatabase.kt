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
        SecurityAuditEventEntity::class,
        DomainRuleEntity::class,
        BlocklistSourceEntity::class,
        DnsConfigurationEntity::class,
        StrictModeConfigEntity::class,
        DomainChangeRequestEntity::class,
        DnsScheduleEntity::class,
        AppCategoryOverrideEntity::class,
        QuarantineEntity::class
    ],
    version = 7,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun snappedAppDao(): SnappedAppDao
    abstract fun protectionConfigDao(): ProtectionConfigDao
    abstract fun devicePolicyStateDao(): DevicePolicyStateDao
    abstract fun securityAuditDao(): SecurityAuditDao
    abstract fun domainRuleDao(): DomainRuleDao
    abstract fun blocklistSourceDao(): BlocklistSourceDao
    abstract fun dnsConfigurationDao(): DnsConfigurationDao
    abstract fun strictModeConfigDao(): StrictModeConfigDao
    abstract fun domainChangeRequestDao(): DomainChangeRequestDao
    abstract fun dnsScheduleDao(): DnsScheduleDao
    abstract fun appCategoryOverrideDao(): AppCategoryOverrideDao
    abstract fun quarantineDao(): QuarantineDao

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
                db.execSQL("ALTER TABLE snapped_apps ADD COLUMN uninstallBlocked INTEGER NOT NULL DEFAULT 0")
                db.execSQL("ALTER TABLE snapped_apps ADD COLUMN lastVerifiedAt INTEGER NOT NULL DEFAULT 0")
                db.execSQL("UPDATE snapped_apps SET actualState = 'SNAP_ACTIVE' WHERE actualState = 'SNAPPED — PROTECTED'")
                db.execSQL("UPDATE snapped_apps SET actualState = 'AWAITING_INSTALLATION' WHERE actualState = 'SNAPPED — AWAITING INSTALLATION'")
                db.execSQL("UPDATE protection_config SET disallowConfigPrivateDns = 0 WHERE contentShieldEnabled = 0")
                db.execSQL("UPDATE protection_config SET selfProtectionEnabled = 0")
            }
        }

        val MIGRATION_4_5 = object : Migration(4, 5) {
            override fun migrate(db: SupportSQLiteDatabase) {
                // 1. Create domain_rules table and indices
                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS domain_rules (
                        domain TEXT PRIMARY KEY NOT NULL,
                        ruleType TEXT NOT NULL,
                        isEnabled INTEGER NOT NULL DEFAULT 1,
                        source TEXT NOT NULL DEFAULT 'USER',
                        createdAt INTEGER NOT NULL,
                        updatedAt INTEGER NOT NULL
                    )
                """.trimIndent())
                db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS index_domain_rules_domain ON domain_rules(domain)")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_domain_rules_ruleType_isEnabled ON domain_rules(ruleType, isEnabled)")

                // 2. Create blocklist_sources table
                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS blocklist_sources (
                        id TEXT PRIMARY KEY NOT NULL,
                        name TEXT NOT NULL,
                        url TEXT NOT NULL,
                        version TEXT NOT NULL,
                        lastUpdated INTEGER NOT NULL,
                        ruleCount INTEGER NOT NULL,
                        isEnabled INTEGER NOT NULL,
                        category TEXT NOT NULL
                    )
                """.trimIndent())

                // Seed built-in blocklist sources
                val now = System.currentTimeMillis()
                db.execSQL("""
                    INSERT OR IGNORE INTO blocklist_sources (id, name, url, version, lastUpdated, ruleCount, isEnabled, category)
                    VALUES ('hagezi_threat_mini', 'HaGeZi Threat Intelligence Feeds (TIF Mini)', 'https://raw.githubusercontent.com/hagezi/dns-blocklists/main/adblock/tif.mini.txt', '1.0', $now, 189700, 1, 'THREATS')
                """.trimIndent())
                db.execSQL("""
                    INSERT OR IGNORE INTO blocklist_sources (id, name, url, version, lastUpdated, ruleCount, isEnabled, category)
                    VALUES ('hagezi_nsfw', 'HaGeZi NSFW (Adult Content)', 'https://raw.githubusercontent.com/hagezi/dns-blocklists/main/adblock/nsfw.txt', '1.0', $now, 77921, 0, 'ADULT_CONTENT')
                """.trimIndent())
                db.execSQL("""
                    INSERT OR IGNORE INTO blocklist_sources (id, name, url, version, lastUpdated, ruleCount, isEnabled, category)
                    VALUES ('hagezi_pro_mini', 'HaGeZi Multi PRO Mini', 'https://raw.githubusercontent.com/hagezi/dns-blocklists/main/adblock/pro.mini.txt', '1.0', $now, 164028, 0, 'ADS_TRACKING')
                """.trimIndent())

                // 3. Create dns_configuration table
                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS dns_configuration (
                        id INTEGER PRIMARY KEY NOT NULL,
                        dnsEnabled INTEGER NOT NULL,
                        blockThreats INTEGER NOT NULL,
                        blockAdultContent INTEGER NOT NULL DEFAULT 0,
                        blockAdsTrackers INTEGER NOT NULL,
                        autoUpdate INTEGER NOT NULL,
                        wifiOnly INTEGER NOT NULL,
                        dnsCacheEnabled INTEGER NOT NULL,
                        lastUpdateTimestamp INTEGER NOT NULL
                    )
                """.trimIndent())
                try {
                    db.execSQL("ALTER TABLE dns_configuration ADD COLUMN blockAdultContent INTEGER NOT NULL DEFAULT 0")
                } catch (_: Exception) {}
                db.execSQL("""
                    INSERT OR IGNORE INTO dns_configuration (id, dnsEnabled, blockThreats, blockAdultContent, blockAdsTrackers, autoUpdate, wifiOnly, dnsCacheEnabled, lastUpdateTimestamp)
                    VALUES (1, 0, 1, 0, 0, 1, 1, 1, 0)
                """.trimIndent())

                // 4. Create strict_mode_config table
                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS strict_mode_config (
                        id INTEGER PRIMARY KEY NOT NULL,
                        isEnabled INTEGER NOT NULL,
                        passcodeHash TEXT,
                        passcodeSalt TEXT,
                        unlockDelayMinutes INTEGER NOT NULL,
                        unlockRequestTimestamp INTEGER NOT NULL,
                        temporaryUnlockUntil INTEGER NOT NULL
                    )
                """.trimIndent())
                db.execSQL("""
                    INSERT OR IGNORE INTO strict_mode_config (id, isEnabled, passcodeHash, passcodeSalt, unlockDelayMinutes, unlockRequestTimestamp, temporaryUnlockUntil)
                    VALUES (1, 0, NULL, NULL, 15, 0, 0)
                """.trimIndent())

                // 5. Map snapped_apps actualState to SNAPPED
                db.execSQL("UPDATE snapped_apps SET actualState = 'SNAPPED' WHERE actualState = 'SNAP_ACTIVE' OR actualState = 'PROTECTED'")
            }
        }

        val MIGRATION_5_6 = object : Migration(5, 6) {
            override fun migrate(db: SupportSQLiteDatabase) {
                // 1. Create domain_change_requests table and indices
                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS domain_change_requests (
                        id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        requestId TEXT NOT NULL,
                        normalizedDomain TEXT NOT NULL,
                        operation TEXT NOT NULL,
                        createdAtEpochMillis INTEGER NOT NULL,
                        effectiveAtEpochMillis INTEGER NOT NULL,
                        status TEXT NOT NULL,
                        createdTimeZoneId TEXT NOT NULL,
                        lastEvaluatedAtEpochMillis INTEGER NOT NULL
                    )
                """.trimIndent())
                db.execSQL("CREATE INDEX IF NOT EXISTS index_domain_change_requests_normalizedDomain_status ON domain_change_requests(normalizedDomain, status)")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_domain_change_requests_status_effectiveAtEpochMillis ON domain_change_requests(status, effectiveAtEpochMillis)")

                // 2. Create dns_schedule table
                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS dns_schedule (
                        id INTEGER PRIMARY KEY NOT NULL,
                        enabled INTEGER NOT NULL,
                        startHour INTEGER NOT NULL,
                        startMinute INTEGER NOT NULL,
                        endHour INTEGER NOT NULL,
                        endMinute INTEGER NOT NULL,
                        lastEvaluatedAtEpochMillis INTEGER NOT NULL
                    )
                """.trimIndent())
                db.execSQL("""
                    INSERT OR IGNORE INTO dns_schedule (id, enabled, startHour, startMinute, endHour, endMinute, lastEvaluatedAtEpochMillis)
                    VALUES (1, 1, 23, 0, 6, 0, 0)
                """.trimIndent())

                // 3. Create app_category_overrides table
                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS app_category_overrides (
                        packageName TEXT PRIMARY KEY NOT NULL,
                        category TEXT NOT NULL,
                        updatedAt INTEGER NOT NULL
                    )
                """.trimIndent())
            }
        }

        val MIGRATION_6_7 = object : Migration(6, 7) {
            override fun migrate(db: SupportSQLiteDatabase) {
                // 1. Add manualDnsEnabled, manualDnsConfirmed, and upstream configurations to dns_configuration
                db.execSQL("ALTER TABLE dns_configuration ADD COLUMN manualDnsEnabled INTEGER NOT NULL DEFAULT 0")
                db.execSQL("ALTER TABLE dns_configuration ADD COLUMN manualDnsConfirmed INTEGER NOT NULL DEFAULT 0")
                db.execSQL("ALTER TABLE dns_configuration ADD COLUMN primaryUpstream TEXT NOT NULL DEFAULT 'SYSTEM'")
                db.execSQL("ALTER TABLE dns_configuration ADD COLUMN secondaryUpstream TEXT NOT NULL DEFAULT ''")
                db.execSQL("ALTER TABLE dns_configuration ADD COLUMN customUpstreamEndpoint TEXT NOT NULL DEFAULT ''")
                db.execSQL("ALTER TABLE dns_configuration ADD COLUMN upstreamProtocol TEXT NOT NULL DEFAULT 'SYSTEM'")

                // Conservative migration: do not blindly infer intent from ambiguous runtime dnsEnabled.
                // If legacy dnsEnabled was 1, maintain protection conservatively (manualDnsEnabled = 1),
                // but mark manualDnsConfirmed = 0 so the user is given a transparent one-time prompt.
                // If legacy dnsEnabled was 0, user already had DNS off, so manualDnsConfirmed = 1.
                db.execSQL("UPDATE dns_configuration SET manualDnsEnabled = CASE WHEN dnsEnabled = 1 THEN 1 ELSE 0 END")
                db.execSQL("UPDATE dns_configuration SET manualDnsConfirmed = CASE WHEN dnsEnabled = 1 THEN 0 ELSE 1 END")

                // 2. Create quarantine_state table
                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS quarantine_state (
                        id INTEGER PRIMARY KEY NOT NULL,
                        isActive INTEGER NOT NULL DEFAULT 0,
                        startTimeEpochMillis INTEGER NOT NULL DEFAULT 0,
                        durationMinutes INTEGER NOT NULL DEFAULT 30,
                        endTimeEpochMillis INTEGER NOT NULL DEFAULT 0,
                        bootCountAtStart INTEGER NOT NULL DEFAULT 0,
                        elapsedRealtimeAtStart INTEGER NOT NULL DEFAULT 0,
                        selectedPackages TEXT NOT NULL DEFAULT '',
                        lastEvaluatedEpochMillis INTEGER NOT NULL DEFAULT 0
                    )
                """.trimIndent())
                db.execSQL("""
                    INSERT OR IGNORE INTO quarantine_state (id, isActive, startTimeEpochMillis, durationMinutes, endTimeEpochMillis, bootCountAtStart, elapsedRealtimeAtStart, selectedPackages, lastEvaluatedEpochMillis)
                    VALUES (1, 0, 0, 30, 0, 0, 0, '', 0)
                """.trimIndent())
            }
        }

        fun get(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: run {
                    val appContext = context.applicationContext
                    // Create non-destructive pre-migration backup before opening or migrating database
                    DatabaseBackupManager.createPreMigrationBackupIfNeeded(appContext)

                    Room.databaseBuilder(
                        appContext,
                        AppDatabase::class.java,
                        "coconut_snap.db"
                    )
                        .addMigrations(MIGRATION_1_2, MIGRATION_2_3, MIGRATION_3_4, MIGRATION_4_5, MIGRATION_5_6, MIGRATION_6_7)
                        .build()
                        .also { INSTANCE = it }
                }
            }
        }
    }
}
