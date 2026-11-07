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
        StrictModeConfigEntity::class
    ],
    version = 5,
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
                    VALUES ('hagezi_threat_mini', 'HaGeZi Threat Intelligence Feeds (TIF Mini)', 'https://raw.githubusercontent.com/hagezi/dns-blocklists/main/adblock/tif.mini.txt', '1.0', $now, 10000, 1, 'THREATS')
                """.trimIndent())
                db.execSQL("""
                    INSERT OR IGNORE INTO blocklist_sources (id, name, url, version, lastUpdated, ruleCount, isEnabled, category)
                    VALUES ('hagezi_nsfw', 'HaGeZi NSFW (Adult Content)', 'https://raw.githubusercontent.com/hagezi/dns-blocklists/main/adblock/nsfw.txt', '1.0', $now, 10000, 0, 'ADULT_CONTENT')
                """.trimIndent())
                db.execSQL("""
                    INSERT OR IGNORE INTO blocklist_sources (id, name, url, version, lastUpdated, ruleCount, isEnabled, category)
                    VALUES ('hagezi_pro_mini', 'HaGeZi Multi PRO Mini', 'https://raw.githubusercontent.com/hagezi/dns-blocklists/main/adblock/pro.mini.txt', '1.0', $now, 10000, 0, 'ADS_TRACKING')
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

        fun get(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "coconut_snap.db"
                )
                    .addMigrations(MIGRATION_1_2, MIGRATION_2_3, MIGRATION_3_4, MIGRATION_4_5)
                    .build()
                    .also { INSTANCE = it }
            }
        }
    }
}
