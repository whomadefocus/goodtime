package com.apps.adrcotfas.goodtime.applimits

import android.app.AppOpsManager
import android.app.usage.UsageStatsManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.os.Process
import android.provider.Settings
import androidx.core.graphics.drawable.toBitmap
import kotlinx.serialization.Serializable
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.util.Calendar

@Serializable
data class AppLimitRule(
    val id: String,
    val name: String,
    val packageNames: List<String>,
    val dailyLimitMinutes: Int,
    val isEnabled: Boolean = true,
)

data class AppUsageEntry(
    val packageName: String,
    val label: String,
    val usageMillis: Long,
    val icon: Bitmap,
)

object AppLimitsRepository {
    private const val PREFERENCES_NAME = "app_limits"
    private const val RULES_KEY = "rules"

    fun hasUsageAccess(context: Context): Boolean {
        val appOpsManager = context.getSystemService(Context.APP_OPS_SERVICE) as AppOpsManager
        return appOpsManager.checkOpNoThrow(
            AppOpsManager.OPSTR_GET_USAGE_STATS,
            Process.myUid(),
            context.packageName,
        ) == AppOpsManager.MODE_ALLOWED
    }

    fun hasAccessibilityAccess(context: Context): Boolean {
        val expectedService = ComponentName(context, AppLimitAccessibilityService::class.java).flattenToString()
        val enabledServices =
            Settings.Secure.getString(
                context.contentResolver,
                Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES,
            )
        return enabledServices
            ?.split(':')
            ?.any { it.equals(expectedService, ignoreCase = true) } == true
    }

    fun readRules(context: Context): List<AppLimitRule> {
        val storedRules = preferences(context).getString(RULES_KEY, null) ?: return emptyList()
        return runCatching { Json.decodeFromString<List<AppLimitRule>>(storedRules) }.getOrDefault(emptyList())
    }

    fun writeRules(
        context: Context,
        rules: List<AppLimitRule>,
    ) {
        preferences(context).edit().putString(RULES_KEY, Json.encodeToString(rules)).apply()
    }

    fun queryDailyUsage(context: Context): Map<String, Long> {
        val startOfDay =
            Calendar.getInstance().apply {
                set(Calendar.HOUR_OF_DAY, 0)
                set(Calendar.MINUTE, 0)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
            }.timeInMillis
        val usageStatsManager = context.getSystemService(Context.USAGE_STATS_SERVICE) as UsageStatsManager
        return usageStatsManager
            .queryUsageStats(UsageStatsManager.INTERVAL_DAILY, startOfDay, System.currentTimeMillis())
            .orEmpty()
            .groupBy { it.packageName }
            .mapValues { (_, stats) -> stats.sumOf { it.totalTimeInForeground } }
    }

    fun queryLaunchableApps(context: Context): List<AppUsageEntry> {
        val packageManager = context.packageManager
        val launcherIntent = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER)
        val usage = queryDailyUsage(context)
        return packageManager
            .queryIntentActivities(launcherIntent, PackageManager.MATCH_ALL)
            .asSequence()
            .mapNotNull { resolveInfo ->
                val activityInfo = resolveInfo.activityInfo ?: return@mapNotNull null
                val packageName = activityInfo.packageName
                if (packageName == context.packageName) return@mapNotNull null
                AppUsageEntry(
                    packageName = packageName,
                    label = resolveInfo.loadLabel(packageManager).toString(),
                    usageMillis = usage[packageName] ?: 0L,
                    icon = resolveInfo.loadIcon(packageManager).toBitmap(width = 96, height = 96),
                )
            }.distinctBy { it.packageName }
            .sortedWith(compareByDescending<AppUsageEntry> { it.usageMillis }.thenBy { it.label.lowercase() })
            .toList()
    }

    fun ruleUsageMillis(
        rule: AppLimitRule,
        usage: Map<String, Long>,
    ): Long = rule.packageNames.sumOf { usage[it] ?: 0L }

    private fun preferences(context: Context) =
        context.getSharedPreferences(PREFERENCES_NAME, Context.MODE_PRIVATE)
}
