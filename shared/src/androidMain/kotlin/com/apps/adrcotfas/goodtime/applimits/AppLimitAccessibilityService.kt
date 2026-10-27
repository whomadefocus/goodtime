package com.apps.adrcotfas.goodtime.applimits

import android.accessibilityservice.AccessibilityService
import android.content.Intent
import android.os.Handler
import android.os.Looper
import android.view.accessibility.AccessibilityEvent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class AppLimitAccessibilityService : AccessibilityService() {
    private val handler = Handler(Looper.getMainLooper())
    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    @Volatile
    private var foregroundPackage: String? = null
    private var isChecking = false

    private val limitCheck =
        object : Runnable {
            override fun run() {
                checkCurrentApp()
                handler.postDelayed(this, CHECK_INTERVAL_MILLIS)
            }
        }

    override fun onServiceConnected() {
        handler.post(limitCheck)
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        foregroundPackage = event?.packageName?.toString()
    }

    override fun onInterrupt() = Unit

    override fun onDestroy() {
        handler.removeCallbacksAndMessages(null)
        serviceScope.cancel()
        super.onDestroy()
    }

    private fun checkCurrentApp() {
        val packageName = foregroundPackage ?: return
        if (packageName == this.packageName || isChecking) return
        isChecking = true
        serviceScope.launch {
            val blockedRule =
                runCatching {
                    val matchingRule =
                        if (AppLimitsRepository.hasUsageAccess(this@AppLimitAccessibilityService)) {
                            AppLimitsRepository
                                .readRules(this@AppLimitAccessibilityService)
                                .firstOrNull { it.isEnabled && packageName in it.packageNames }
                        } else {
                            null
                        }
                    matchingRule?.takeIf {
                        AppLimitsRepository.ruleUsageMillis(
                            it,
                            AppLimitsRepository.queryDailyUsage(this@AppLimitAccessibilityService),
                        ) >= it.dailyLimitMinutes * MILLIS_PER_MINUTE
                    }
                }.getOrNull()

            withContext(Dispatchers.Main) {
                isChecking = false
                if (blockedRule != null && foregroundPackage == packageName) {
                    foregroundPackage = null
                    startActivity(
                        Intent(this@AppLimitAccessibilityService, AppLimitBlockActivity::class.java)
                            .putExtra(AppLimitBlockActivity.EXTRA_RULE_NAME, blockedRule.name)
                            .putExtra(AppLimitBlockActivity.EXTRA_LIMIT_MINUTES, blockedRule.dailyLimitMinutes)
                            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP),
                    )
                }
            }
        }
    }

    private companion object {
        const val CHECK_INTERVAL_MILLIS = 15_000L
        const val MILLIS_PER_MINUTE = 60_000L
    }
}
