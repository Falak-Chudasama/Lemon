package com.coconut.policy

import android.content.Context
import android.os.SystemClock
import android.provider.Settings
import com.coconut.data.AppDatabase
import com.coconut.data.QuarantineEntity
import com.coconut.data.SecurityAuditEventEntity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class QuarantineManager(private val context: Context) {

    private val appContext = context.applicationContext
    private val db = AppDatabase.get(appContext)
    private val quarantineDao = db.quarantineDao()
    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    val quarantineFlow: Flow<QuarantineEntity?> = quarantineDao.getQuarantineFlow()

    private val _isQuarantineActive = MutableStateFlow(false)
    val isQuarantineActive: StateFlow<Boolean> = _isQuarantineActive.asStateFlow()

    private val _quarantinedPackages = MutableStateFlow<Set<String>>(emptySet())
    val quarantinedPackages: StateFlow<Set<String>> = _quarantinedPackages.asStateFlow()

    private val _remainingSeconds = MutableStateFlow(0L)
    val remainingSeconds: StateFlow<Long> = _remainingSeconds.asStateFlow()

    companion object {
        const val MIN_DURATION_MINUTES = 30
        const val MAX_DURATION_MINUTES = 300 // 5 hours

        fun clampDuration(minutes: Int): Int = minutes.coerceIn(MIN_DURATION_MINUTES, MAX_DURATION_MINUTES)

        @Volatile
        private var INSTANCE: QuarantineManager? = null

        fun getInstance(context: Context): QuarantineManager {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: QuarantineManager(context).also { INSTANCE = it }
            }
        }

        fun calculateRemainingSecondsPure(
            isActive: Boolean,
            durationMinutes: Int,
            startTimeEpochMillis: Long,
            endTimeEpochMillis: Long,
            bootCountAtStart: Int,
            elapsedRealtimeAtStart: Long,
            currentWallClock: Long,
            currentBootCount: Int,
            currentElapsedRealtime: Long
        ): Long {
            if (!isActive) return 0L

            val rebootOccurred = if (currentBootCount > 0 && bootCountAtStart > 0) {
                currentBootCount > bootCountAtStart
            } else {
                currentElapsedRealtime < elapsedRealtimeAtStart
            }

            val remainingFromWallClock = ((endTimeEpochMillis - currentWallClock) / 1000L).coerceAtLeast(0L)

            return if (!rebootOccurred && elapsedRealtimeAtStart > 0) {
                val totalDurationSeconds = durationMinutes * 60L
                val elapsedSeconds = (currentElapsedRealtime - elapsedRealtimeAtStart) / 1000L
                val remainingMonotonic = (totalDurationSeconds - elapsedSeconds).coerceAtLeast(0L)
                maxOf(remainingMonotonic, remainingFromWallClock)
            } else {
                remainingFromWallClock
            }
        }
    }

    init {
        scope.launch {
            quarantineFlow.collectLatest { entity ->
                if (entity != null && entity.isActive) {
                    val remaining = calculateRemainingSeconds(entity)
                    if (remaining > 0) {
                        _isQuarantineActive.value = true
                        _quarantinedPackages.value = entity.getPackageList()
                        _remainingSeconds.value = remaining
                    } else {
                        // Matured/expired
                        expireQuarantine(entity)
                    }
                } else {
                    _isQuarantineActive.value = false
                    _quarantinedPackages.value = emptySet()
                    _remainingSeconds.value = 0L
                }
            }
        }
    }

    private fun getDeviceBootCount(): Int {
        return try {
            Settings.Global.getInt(appContext.contentResolver, Settings.Global.BOOT_COUNT, 0)
        } catch (_: Exception) {
            0
        }
    }

    /**
     * Calculates remaining time in seconds using monotonic clock when within the same boot,
     * and wall-clock time with conservative checkpoint after reboot.
     */
    fun calculateRemainingSeconds(entity: QuarantineEntity): Long {
        return calculateRemainingSecondsPure(
            isActive = entity.isActive,
            durationMinutes = entity.durationMinutes,
            startTimeEpochMillis = entity.startTimeEpochMillis,
            endTimeEpochMillis = entity.endTimeEpochMillis,
            bootCountAtStart = entity.bootCountAtStart,
            elapsedRealtimeAtStart = entity.elapsedRealtimeAtStart,
            currentWallClock = System.currentTimeMillis(),
            currentBootCount = getDeviceBootCount(),
            currentElapsedRealtime = SystemClock.elapsedRealtime()
        )
    }

    /**
     * Starts a new Quarantine session.
     * Enforces bounds (30m to 300m) and records start checkpoints.
     */
    suspend fun startQuarantine(durationMinutes: Int, packages: Set<String>): Result<Unit> = withContext(Dispatchers.IO) {
        val clampedDuration = durationMinutes.coerceIn(MIN_DURATION_MINUTES, MAX_DURATION_MINUTES)
        val now = System.currentTimeMillis()
        val durationMillis = clampedDuration * 60 * 1000L
        val endTime = now + durationMillis
        val bootCount = getDeviceBootCount()
        val elapsed = SystemClock.elapsedRealtime()

        val entity = QuarantineEntity.fromPackageList(
            packages = packages,
            durationMinutes = clampedDuration,
            startTime = now,
            endTime = endTime,
            bootCount = bootCount,
            elapsedRealtime = elapsed
        )

        quarantineDao.insertOrUpdate(entity)
        _isQuarantineActive.value = true
        _quarantinedPackages.value = packages
        _remainingSeconds.value = clampedDuration * 60L

        // Audit log
        db.securityAuditDao().insert(
            SecurityAuditEventEntity(
                eventType = "QUARANTINE_STARTED",
                target = "Lemon",
                details = "Quarantine started: $clampedDuration minutes, ${packages.size} apps restricted.",
                severity = "INFO"
            )
        )

        LemonNotificationManager.notifyQuarantineStarted(appContext, clampedDuration, packages.size)

        // Reconcile DNS to force DNS protection ON
        LemonPolicyCoordinator.getInstance(appContext).reconcile("quarantine_started")

        Result.success(Unit)
    }

    /**
     * Reconciles Quarantine state (e.g. on boot, tick, or foreground).
     * Automatically completes session if expired.
     */
    suspend fun reconcile(): Boolean = withContext(Dispatchers.IO) {
        val entity = quarantineDao.getQuarantine() ?: return@withContext false
        if (!entity.isActive) {
            _isQuarantineActive.value = false
            return@withContext false
        }

        val remaining = calculateRemainingSeconds(entity)
        if (remaining <= 0) {
            expireQuarantine(entity)
            false
        } else {
            _isQuarantineActive.value = true
            _quarantinedPackages.value = entity.getPackageList()
            _remainingSeconds.value = remaining

            // Warn if approximately 10 minutes remaining
            if (remaining in 540..660) {
                LemonNotificationManager.notifyQuarantineExpiringSoon(appContext, 10)
            }
            true
        }
    }

    private suspend fun expireQuarantine(entity: QuarantineEntity) {
        val now = System.currentTimeMillis()
        quarantineDao.deactivate(now)
        _isQuarantineActive.value = false
        _quarantinedPackages.value = emptySet()
        _remainingSeconds.value = 0L

        // Audit log
        db.securityAuditDao().insert(
            SecurityAuditEventEntity(
                eventType = "QUARANTINE_EXPIRED",
                target = "Lemon",
                details = "Quarantine period completed. Temporary app restrictions released.",
                severity = "INFO"
            )
        )

        LemonNotificationManager.notifyQuarantineExpired(appContext)

        // Reconcile DNS (restoring manual preference unless night schedule is active)
        LemonPolicyCoordinator.getInstance(appContext).reconcile("quarantine_expired")
    }

    fun isAppQuarantined(packageName: String): Boolean {
        return _isQuarantineActive.value && _quarantinedPackages.value.contains(packageName)
    }
}
