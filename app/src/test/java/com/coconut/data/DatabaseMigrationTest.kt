package com.coconut.data

import androidx.sqlite.db.SupportSQLiteDatabase
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File
import java.sql.Connection
import java.sql.DriverManager

class DatabaseMigrationTest {

    @Test
    fun testMigration6To7PreservesUserDataAndSetsConservativeConfirmation() {
        val connection = DriverManager.getConnection("jdbc:sqlite::memory:")

        try {
            // 1. Setup exact v6 schema
            setupV6Schema(connection)

            // 2. Insert existing user records (Case A: User had dnsEnabled = 1; Case B: Another user had dnsEnabled = 0)
            insertV6Data(connection)

            // 3. Execute MIGRATION_6_7 using dynamic proxy for SupportSQLiteDatabase
            val proxy = java.lang.reflect.Proxy.newProxyInstance(
                SupportSQLiteDatabase::class.java.classLoader,
                arrayOf(SupportSQLiteDatabase::class.java)
            ) { _, method, args ->
                when (method.name) {
                    "execSQL" -> {
                        val sql = args[0] as String
                        if (args.size > 1 && args[1] != null) {
                            val bindArgs = args[1] as Array<*>
                            connection.prepareStatement(sql).use { ps ->
                                bindArgs.forEachIndexed { i, arg -> ps.setObject(i + 1, arg) }
                                ps.execute()
                            }
                        } else {
                            connection.createStatement().use { it.execute(sql) }
                        }
                        null
                    }
                    else -> null
                }
            } as SupportSQLiteDatabase

            AppDatabase.MIGRATION_6_7.migrate(proxy)

            // 4. Verify dns_configuration changes:
            // When legacy dnsEnabled was 1, manualDnsEnabled is conservatively set to 1,
            // but manualDnsConfirmed is 0 so the user is prompted for one-time daytime confirmation.
            connection.createStatement().use { stmt ->
                val rs = stmt.executeQuery("SELECT * FROM dns_configuration WHERE id = 1")
                assertTrue("dns_configuration row 1 must exist", rs.next())
                assertEquals(1, rs.getInt("dnsEnabled"))
                assertEquals(1, rs.getInt("manualDnsEnabled"))
                assertEquals("Legacy dnsEnabled=1 must require daytime confirmation", 0, rs.getInt("manualDnsConfirmed"))
                assertEquals("SYSTEM", rs.getString("primaryUpstream"))
                assertEquals("", rs.getString("secondaryUpstream"))
                assertEquals("", rs.getString("customUpstreamEndpoint"))
                assertEquals("SYSTEM", rs.getString("upstreamProtocol"))
                rs.close()
            }

            // 5. Verify quarantine_state table exists and is initialized with default row
            connection.createStatement().use { stmt ->
                val rs = stmt.executeQuery("SELECT * FROM quarantine_state WHERE id = 1")
                assertTrue("quarantine_state table must exist and contain default row", rs.next())
                assertEquals(1, rs.getInt("id"))
                assertEquals(0, rs.getInt("isActive"))
                assertEquals(30, rs.getInt("durationMinutes"))
                assertEquals("", rs.getString("selectedPackages"))
                rs.close()
            }

            // 6. Verify existing snapped_apps data was NOT modified or dropped
            connection.createStatement().use { stmt ->
                val rs = stmt.executeQuery("SELECT * FROM snapped_apps WHERE packageName = 'com.instagram.android'")
                assertTrue("Existing snapped app record must be preserved", rs.next())
                assertEquals("Instagram", rs.getString("appName"))
                assertEquals("SNAPPED", rs.getString("actualState"))
                assertEquals(1, rs.getInt("uninstallBlocked"))
                rs.close()
            }

            // 7. Verify existing dns_schedule data was NOT modified or dropped
            connection.createStatement().use { stmt ->
                val rs = stmt.executeQuery("SELECT * FROM dns_schedule WHERE id = 1")
                assertTrue("Existing dns_schedule row must be preserved", rs.next())
                assertEquals(1, rs.getInt("enabled"))
                assertEquals(23, rs.getInt("startHour"))
                assertEquals(6, rs.getInt("endHour"))
                rs.close()
            }
        } finally {
            connection.close()
        }
    }

    @Test
    fun testDatabaseBackupConsistencyAndRestoration() {
        val tempDir = File(System.getProperty("java.io.tmpdir"), "lemon_backup_integrity_test_${System.currentTimeMillis()}")
        tempDir.mkdirs()

        try {
            val liveDbFile = File(tempDir, "coconut_snap.db")
            val backupDbFile = File(tempDir, "coconut_snap_pre_v7.db")
            val restoredDbFile = File(tempDir, "coconut_snap_restored.db")

            // Create live database with schema and data
            val conn = DriverManager.getConnection("jdbc:sqlite:${liveDbFile.absolutePath}")
            setupV6Schema(conn)
            insertV6Data(conn)
            conn.close()

            // Perform file snapshot copy (simulating safe snapshot)
            liveDbFile.copyTo(backupDbFile, overwrite = true)

            // Independently open backup and verify PRAGMA integrity_check
            val backupConn = DriverManager.getConnection("jdbc:sqlite:${backupDbFile.absolutePath}")
            backupConn.createStatement().use { stmt ->
                val rs = stmt.executeQuery("PRAGMA integrity_check")
                assertTrue(rs.next())
                val result = rs.getString(1)
                assertEquals("PRAGMA integrity_check must return 'ok'", "ok", result)
                rs.close()
            }

            // Validate schema: check that core tables exist in the backup
            backupConn.createStatement().use { stmt ->
                val rs = stmt.executeQuery("SELECT count(*) FROM sqlite_master WHERE type='table' AND name IN ('dns_configuration', 'snapped_apps', 'dns_schedule')")
                assertTrue(rs.next())
                assertEquals("All 3 core tables must exist in backup", 3, rs.getInt(1))
                rs.close()
            }

            // Validate representative record in the backup
            backupConn.createStatement().use { stmt ->
                val rs = stmt.executeQuery("SELECT count(*) FROM dns_configuration WHERE id = 1")
                assertTrue(rs.next())
                assertEquals("dns_configuration record 1 must exist in backup", 1, rs.getInt(1))
                rs.close()
            }
            backupConn.close()

            // Test non-destructive restoration with database closed
            backupDbFile.copyTo(restoredDbFile, overwrite = true)
            val restoredConn = DriverManager.getConnection("jdbc:sqlite:${restoredDbFile.absolutePath}")
            restoredConn.createStatement().use { stmt ->
                val rs = stmt.executeQuery("SELECT appName FROM snapped_apps WHERE packageName = 'com.instagram.android'")
                assertTrue("Restored database must contain original data", rs.next())
                assertEquals("Instagram", rs.getString("appName"))
                rs.close()
            }
            restoredConn.close()

            // Verify live original database was preserved throughout
            assertTrue("Live database must remain intact", liveDbFile.exists())
            assertTrue("Live database must not be empty", liveDbFile.length() > 0L)
        } finally {
            tempDir.deleteRecursively()
        }
    }

    private fun setupV6Schema(conn: Connection) {
        conn.createStatement().use { stmt ->
            // snapped_apps
            stmt.execute("""
                CREATE TABLE IF NOT EXISTS snapped_apps (
                    packageName TEXT PRIMARY KEY NOT NULL,
                    appName TEXT NOT NULL,
                    actualState TEXT NOT NULL,
                    isTemporarilyAllowed INTEGER NOT NULL,
                    uninstalledWhileActive INTEGER NOT NULL,
                    isSystemApp INTEGER NOT NULL,
                    category TEXT NOT NULL,
                    blockedAtTimestamp INTEGER NOT NULL,
                    unlockGraceExpiresAt INTEGER NOT NULL,
                    uninstallBlocked INTEGER NOT NULL DEFAULT 0,
                    lastVerifiedAt INTEGER NOT NULL DEFAULT 0
                )
            """.trimIndent())

            // dns_configuration (v6 version)
            stmt.execute("""
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

            // dns_schedule
            stmt.execute("""
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

            // security_audit_events
            stmt.execute("""
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

    private fun insertV6Data(conn: Connection) {
        conn.createStatement().use { stmt ->
            stmt.execute("""
                INSERT INTO dns_configuration (id, dnsEnabled, blockThreats, blockAdultContent, blockAdsTrackers, autoUpdate, wifiOnly, dnsCacheEnabled, lastUpdateTimestamp)
                VALUES (1, 1, 1, 0, 1, 1, 1, 1, 1700000000000)
            """.trimIndent())

            stmt.execute("""
                INSERT INTO snapped_apps (packageName, appName, actualState, isTemporarilyAllowed, uninstalledWhileActive, isSystemApp, category, blockedAtTimestamp, unlockGraceExpiresAt, uninstallBlocked, lastVerifiedAt)
                VALUES ('com.instagram.android', 'Instagram', 'SNAPPED', 0, 0, 0, 'SOCIAL', 1700000000000, 0, 1, 1700000000000)
            """.trimIndent())

            stmt.execute("""
                INSERT INTO dns_schedule (id, enabled, startHour, startMinute, endHour, endMinute, lastEvaluatedAtEpochMillis)
                VALUES (1, 1, 23, 0, 6, 0, 1700000000000)
            """.trimIndent())
        }
    }
}
