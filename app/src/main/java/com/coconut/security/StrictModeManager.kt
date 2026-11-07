package com.coconut.security

import android.content.Context
import com.coconut.data.AppDatabase
import com.coconut.data.StrictModeConfigEntity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.withContext
import java.security.SecureRandom
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.PBEKeySpec

/**
 * StrictModeManager manages internal self-control friction within Coconut:
 * - Locks in-app policy changes behind a PBKDF2-HMAC-SHA256 passcode.
 * - Enforces an optional configurable delay countdown (e.g. 15 minutes) before settings can be edited.
 * - Grants a 10-minute temporary unlock window upon authorized passcode verification.
 *
 * Policy Compliance Note:
 * Strict Mode operates strictly within Coconut's application boundaries.
 * It does NOT inspect Android OS Settings, does NOT monitor or traverse screen node hierarchies,
 * and does NOT interfere with the user's ability to disable or uninstall Coconut through Android Settings.
 */
class StrictModeManager(context: Context) {
    private val appContext = context.applicationContext
    private val db = AppDatabase.get(appContext)
    private val dao = db.strictModeConfigDao()
    private val scope = CoroutineScope(Dispatchers.IO)

    val configFlow: StateFlow<StrictModeConfigEntity?> = dao.getConfigFlow()
        .stateIn(
            scope = scope,
            started = SharingStarted.Eagerly,
            initialValue = null
        )

    fun isStrictProtectionActive(): Boolean {
        val config = configFlow.value ?: return false
        if (!config.isEnabled) return false
        val now = System.currentTimeMillis()
        // If temporary unlock window is active, protection is temporarily lifted
        if (now < config.temporaryUnlockUntil) return false
        return true
    }

    suspend fun setStrictModeEnabled(enabled: Boolean, passcode: String? = null): Boolean = withContext(Dispatchers.IO) {
        val current = dao.getConfig() ?: StrictModeConfigEntity()
        if (enabled) {
            val (hash, salt) = if (!passcode.isNullOrBlank()) {
                hashPasscode(passcode)
            } else {
                Pair(current.passcodeHash, current.passcodeSalt)
            }
            dao.insertOrUpdate(
                current.copy(
                    isEnabled = true,
                    passcodeHash = hash,
                    passcodeSalt = salt,
                    temporaryUnlockUntil = 0L,
                    unlockRequestTimestamp = 0L
                )
            )
        } else {
            dao.insertOrUpdate(
                current.copy(
                    isEnabled = false,
                    temporaryUnlockUntil = 0L,
                    unlockRequestTimestamp = 0L
                )
            )
        }
        true
    }

    suspend fun verifyPasscode(input: String): Boolean = withContext(Dispatchers.IO) {
        val current = dao.getConfig() ?: return@withContext false
        val salt = current.passcodeSalt ?: return@withContext false
        val expectedHash = current.passcodeHash ?: return@withContext false
        val computedHash = hashWithSalt(input, salt)
        computedHash == expectedHash
    }

    suspend fun startUnlockCountdown(): Long = withContext(Dispatchers.IO) {
        val current = dao.getConfig() ?: return@withContext 0L
        val now = System.currentTimeMillis()
        dao.insertOrUpdate(
            current.copy(
                unlockRequestTimestamp = now
            )
        )
        now
    }

    suspend fun completeUnlockAfterCountdown(): Boolean = withContext(Dispatchers.IO) {
        val current = dao.getConfig() ?: return@withContext false
        val now = System.currentTimeMillis()
        val delayMillis = current.unlockDelayMinutes * 60 * 1000L
        if (current.unlockRequestTimestamp > 0 && now >= current.unlockRequestTimestamp + delayMillis) {
            val temporaryWindow = now + (10 * 60 * 1000L) // 10-minute temporary access window
            dao.insertOrUpdate(
                current.copy(
                    temporaryUnlockUntil = temporaryWindow,
                    unlockRequestTimestamp = 0L
                )
            )
            true
        } else {
            false
        }
    }

    suspend fun cancelUnlockCountdown() = withContext(Dispatchers.IO) {
        val current = dao.getConfig() ?: return@withContext
        dao.insertOrUpdate(current.copy(unlockRequestTimestamp = 0L))
    }

    private fun hashPasscode(passcode: String): Pair<String, String> {
        val random = SecureRandom()
        val saltBytes = ByteArray(16)
        random.nextBytes(saltBytes)
        val saltHex = saltBytes.joinToString("") { "%02x".format(it) }
        val hash = hashWithPbkdf2(passcode, saltBytes)
        return Pair(hash, saltHex)
    }

    private fun hashWithSalt(input: String, saltHex: String): String {
        val saltBytes = try {
            hexToByteArray(saltHex)
        } catch (_: Exception) {
            saltHex.toByteArray(Charsets.UTF_8)
        }
        return hashWithPbkdf2(input, saltBytes)
    }

    private fun hashWithPbkdf2(passcode: String, salt: ByteArray): String {
        val spec = PBEKeySpec(passcode.toCharArray(), salt, PBKDF2_ITERATIONS, KEY_LENGTH_BITS)
        val factory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256")
        val hashBytes = factory.generateSecret(spec).encoded
        return hashBytes.joinToString("") { "%02x".format(it) }
    }

    private fun hexToByteArray(hex: String): ByteArray {
        val len = hex.length
        val data = ByteArray(len / 2)
        var i = 0
        while (i < len) {
            data[i / 2] = ((Character.digit(hex[i], 16) shl 4) + Character.digit(hex[i + 1], 16)).toByte()
            i += 2
        }
        return data
    }

    companion object {
        // OWASP & NIST SP 800-63B standard calibration for PBKDF2-HMAC-SHA256
        private const val PBKDF2_ITERATIONS = 100_000
        private const val KEY_LENGTH_BITS = 256
    }
}
