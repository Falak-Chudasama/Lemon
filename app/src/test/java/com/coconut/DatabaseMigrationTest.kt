package com.coconut

import androidx.sqlite.db.SupportSQLiteDatabase
import com.coconut.data.AppDatabase
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.lang.reflect.Proxy

class DatabaseMigrationTest {

    @Test
    fun testMigration3To4ExecutesNonDestructiveSchemaUpdates() {
        val executedSqlList = mutableListOf<String>()

        // Create a mock SupportSQLiteDatabase proxy that records execSQL invocations
        val dbProxy = Proxy.newProxyInstance(
            SupportSQLiteDatabase::class.java.classLoader,
            arrayOf(SupportSQLiteDatabase::class.java)
        ) { _, method, args ->
            if (method.name == "execSQL" && args != null && args.isNotEmpty()) {
                val sql = args[0] as String
                executedSqlList.add(sql)
            }
            null
        } as SupportSQLiteDatabase

        // Execute MIGRATION_3_4
        AppDatabase.MIGRATION_3_4.migrate(dbProxy)

        // Verify that MIGRATION_3_4 executed all expected migration steps:
        assertEquals(6, executedSqlList.size)

        // 1. Column additions to snapped_apps
        assertTrue(
            "Must add uninstallBlocked column with default 0",
            executedSqlList.any { it.contains("ALTER TABLE snapped_apps ADD COLUMN uninstallBlocked INTEGER NOT NULL DEFAULT 0") }
        )
        assertTrue(
            "Must add lastVerifiedAt column with default 0",
            executedSqlList.any { it.contains("ALTER TABLE snapped_apps ADD COLUMN lastVerifiedAt INTEGER NOT NULL DEFAULT 0") }
        )

        // 2. Migration of legacy actualState strings
        assertTrue(
            "Must migrate legacy 'SNAPPED — PROTECTED' to 'SNAP_ACTIVE'",
            executedSqlList.any { it.contains("UPDATE snapped_apps SET actualState = 'SNAP_ACTIVE' WHERE actualState = 'SNAPPED — PROTECTED'") }
        )
        assertTrue(
            "Must migrate legacy 'SNAPPED — AWAITING INSTALLATION' to 'AWAITING_INSTALLATION'",
            executedSqlList.any { it.contains("UPDATE snapped_apps SET actualState = 'AWAITING_INSTALLATION' WHERE actualState = 'SNAPPED — AWAITING INSTALLATION'") }
        )

        // 3. Correction of problematic defaults in existing records
        assertTrue(
            "Must clear disallowConfigPrivateDns when Content Shield is OFF",
            executedSqlList.any { it.contains("UPDATE protection_config SET disallowConfigPrivateDns = 0 WHERE contentShieldEnabled = 0") }
        )
        assertTrue(
            "Must ensure selfProtectionEnabled is not forced on existing users",
            executedSqlList.any { it.contains("UPDATE protection_config SET selfProtectionEnabled = 0") }
        )
    }
}
