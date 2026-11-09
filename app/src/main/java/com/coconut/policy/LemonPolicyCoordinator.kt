package com.coconut.policy

import android.content.Context
import android.net.VpnService
import com.coconut.data.AppDatabase
import com.coconut.data.DnsConfigurationEntity
import com.coconut.data.SecurityAuditEventEntity
import com.coconut.dns.CoconutVpnService
import com.coconut.dns.DnsScheduleManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext

enum class VpnRuntimeState {
    DISABLED,
    STARTING,
    RUNNING,
    PROTECTED,
    DEGRADED,
    REVOKED,
    RECOVERING,
    FAILED
}

data class PolicyDecision(
    val effectiveDnsDesired: Boolean,
    val canUserDisableManual: Boolean,
    val isMandatory: Boolean,
    val triggerReasons: List<String>
)

class LemonPolicyCoordinator(private val context: Context) {

    private val appContext = context.applicationContext
    private val db = AppDatabase.get(appContext)
    private val configDao = db.dnsConfigurationDao()
    private val auditDao = db.securityAuditDao()
    private val scheduleManager = DnsScheduleManager(appContext)
    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private val reconcileMutex = Mutex()

    private val _vpnRuntimeState = MutableStateFlow(VpnRuntimeState.DISABLED)
    val vpnRuntimeState: StateFlow<VpnRuntimeState> = _vpnRuntimeState.asStateFlow()

    private val _effectiveDnsDesired = MutableStateFlow(false)
    val effectiveDnsDesired: StateFlow<Boolean> = _effectiveDnsDesired.asStateFlow()

    private val _manualDnsPreference = MutableStateFlow(false)
    val manualDnsPreference: StateFlow<Boolean> = _manualDnsPreference.asStateFlow()

    private val _manualDnsConfirmed = MutableStateFlow(true)
    val manualDnsConfirmed: StateFlow<Boolean> = _manualDnsConfirmed.asStateFlow()

    private val _lastVerifiedEpochMillis = MutableStateFlow(0L)
    val lastVerifiedEpochMillis: StateFlow<Long> = _lastVerifiedEpochMillis.asStateFlow()

    private var recoveryAttempts = 0
    private val maxRecoveryAttempts = 3

    companion object {
        @Volatile
        private var INSTANCE: LemonPolicyCoordinator? = null

        fun getInstance(context: Context): LemonPolicyCoordinator {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: LemonPolicyCoordinator(context).also { INSTANCE = it }
            }
        }

        /**
         * Pure policy evaluation function for testability and deterministic decision making.
         */
        fun evaluatePolicy(
            manualDnsEnabled: Boolean,
            nightScheduleActive: Boolean,
            quarantineActive: Boolean
        ): PolicyDecision {
            val mandatory = nightScheduleActive || quarantineActive
            val desired = manualDnsEnabled || mandatory
            val canDisable = !mandatory
            val reasons = mutableListOf<String>()
            if (manualDnsEnabled) reasons.add("MANUAL")
            if (nightScheduleActive) reasons.add("NIGHT_SCHEDULE")
            if (quarantineActive) reasons.add("QUARANTINE")
            return PolicyDecision(
                effectiveDnsDesired = desired,
                canUserDisableManual = canDisable,
                isMandatory = mandatory,
                triggerReasons = reasons
            )
        }
    }

    init {
        // Observe database configuration to keep manual preference reactive
        scope.launch {
            configDao.getConfigurationFlow().collect { config ->
                if (config != null) {
                    _manualDnsPreference.value = config.manualDnsEnabled
                    _manualDnsConfirmed.value = config.manualDnsConfirmed
                }
            }
        }
    }

    /**
     * Resolves the one-time daytime DNS confirmation prompted after upgrading from legacy Coconut v2.0.0.
     */
    suspend fun confirmManualDnsPreference(keepAlwaysOn: Boolean) = withContext(Dispatchers.IO) {
        configDao.confirmManualDnsPreference(keepAlwaysOn)
        _manualDnsPreference.value = keepAlwaysOn
        _manualDnsConfirmed.value = true
        reconcile("manual_preference_confirmed")
    }

    /**
     * Determines whether the nightly schedule is currently in effect.
     */
    fun isNightScheduleActive(): Boolean {
        return scheduleManager.isInsideScheduleWindow(23, 0, 6, 0)
    }

    /**
     * The single authoritative DNS policy reconciliation function.
     * Evaluates:
     * effectiveDnsEnabled = manualDnsEnabled || nightScheduleActive || quarantineActive
     * Idempotent and thread-safe.
     */
    suspend fun reconcile(reason: String): VpnRuntimeState = withContext(Dispatchers.IO) {
        reconcileMutex.withLock {
            val config = configDao.getConfiguration() ?: DnsConfigurationEntity().also {
                configDao.insertOrUpdate(it)
            }
            val manualEnabled = config.manualDnsEnabled
            _manualDnsPreference.value = manualEnabled

            val nightActive = isNightScheduleActive()
            val quarantineActive = QuarantineManager.getInstance(appContext).isQuarantineActive.value

            val decision = evaluatePolicy(manualEnabled, nightActive, quarantineActive)
            val desired = decision.effectiveDnsDesired
            _effectiveDnsDesired.value = desired

            if (desired) {
                // Must be ON
                val prepareIntent = VpnService.prepare(appContext)
                if (prepareIntent != null) {
                    // Consent not granted or revoked
                    _vpnRuntimeState.value = VpnRuntimeState.REVOKED
                    LemonNotificationManager.notifyVpnRevoked(appContext)
                    return@withLock VpnRuntimeState.REVOKED
                }

                if (_vpnRuntimeState.value != VpnRuntimeState.PROTECTED &&
                    _vpnRuntimeState.value != VpnRuntimeState.RUNNING &&
                    _vpnRuntimeState.value != VpnRuntimeState.STARTING
                ) {
                    _vpnRuntimeState.value = VpnRuntimeState.STARTING
                    CoconutVpnService.start(appContext)
                }
            } else {
                // Must be OFF
                if (_vpnRuntimeState.value != VpnRuntimeState.DISABLED) {
                    CoconutVpnService.stop(appContext)
                    _vpnRuntimeState.value = VpnRuntimeState.DISABLED
                    recoveryAttempts = 0
                }
            }

            _vpnRuntimeState.value
        }
    }

    /**
     * Called exclusively by user toggle to set the manual DNS preference.
     * Never called by service lifecycle events.
     */
    suspend fun setManualDnsPreference(enabled: Boolean): Boolean = withContext(Dispatchers.IO) {
        val nightActive = isNightScheduleActive()
        val quarantineActive = QuarantineManager.getInstance(appContext).isQuarantineActive.value

        // If trying to turn OFF while mandatory policy is active, reject
        val decision = evaluatePolicy(enabled, nightActive, quarantineActive)
        if (!enabled && !decision.canUserDisableManual) {
            return@withContext false
        }

        configDao.setManualDnsEnabled(enabled)
        _manualDnsPreference.value = enabled
        reconcile("manual_user_toggle")
        true
    }

    // --- Service Lifecycle Callbacks ---

    fun onServiceStarted() {
        if (_vpnRuntimeState.value != VpnRuntimeState.PROTECTED) {
            _vpnRuntimeState.value = VpnRuntimeState.RUNNING
        }
    }

    fun onTunnelEstablished() {
        _vpnRuntimeState.value = VpnRuntimeState.PROTECTED
        _lastVerifiedEpochMillis.value = System.currentTimeMillis()
        recoveryAttempts = 0
        LemonNotificationManager.notifyVpnRecovered(appContext)
    }

    fun onServiceRevoked() {
        _vpnRuntimeState.value = VpnRuntimeState.REVOKED
        recoveryAttempts = 0

        scope.launch {
            auditDao.insert(
                SecurityAuditEventEntity(
                    eventType = "VPN_REVOKED",
                    target = "Lemon",
                    details = "Android VpnService.onRevoke() triggered. System VPN consent was deleted or revoked.",
                    severity = "WARNING"
                )
            )
            LemonNotificationManager.notifyVpnRevoked(appContext)
        }
    }

    fun onServiceStoppedUnexpectedly() {
        if (!_effectiveDnsDesired.value) {
            _vpnRuntimeState.value = VpnRuntimeState.DISABLED
            return
        }

        if (_vpnRuntimeState.value == VpnRuntimeState.REVOKED) {
            return
        }

        // Policy requires protection, but service stopped!
        if (recoveryAttempts < maxRecoveryAttempts) {
            recoveryAttempts++
            _vpnRuntimeState.value = VpnRuntimeState.RECOVERING
            scope.launch {
                val backoffMs = (1000L * (1 shl (recoveryAttempts - 1)))
                delay(backoffMs)
                reconcile("recovery_attempt_$recoveryAttempts")
            }
        } else {
            _vpnRuntimeState.value = VpnRuntimeState.FAILED
            scope.launch {
                auditDao.insert(
                    SecurityAuditEventEntity(
                        eventType = "VPN_RECOVERY_FAILED",
                        target = "Lemon",
                        details = "VPN restart failed after $maxRecoveryAttempts attempts.",
                        severity = "ERROR"
                    )
                )
            }
        }
    }

    fun onServiceStoppedNormally() {
        _vpnRuntimeState.value = VpnRuntimeState.DISABLED
        recoveryAttempts = 0
    }
}
