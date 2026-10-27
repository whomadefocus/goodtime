package com.apps.adrcotfas.goodtime.applimits

import android.content.Intent
import android.provider.Settings
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import goodtime_productivity.shared.generated.resources.Res
import goodtime_productivity.shared.generated.resources.app_limits_add_limit
import goodtime_productivity.shared.generated.resources.app_limits_android_only
import goodtime_productivity.shared.generated.resources.app_limits_app_already_limited
import goodtime_productivity.shared.generated.resources.app_limits_apps_today
import goodtime_productivity.shared.generated.resources.app_limits_cancel
import goodtime_productivity.shared.generated.resources.app_limits_choose_app
import goodtime_productivity.shared.generated.resources.app_limits_daily_limit_minutes
import goodtime_productivity.shared.generated.resources.app_limits_daily_limits
import goodtime_productivity.shared.generated.resources.app_limits_delete
import goodtime_productivity.shared.generated.resources.app_limits_delete_confirmation
import goodtime_productivity.shared.generated.resources.app_limits_enable_blocking_description
import goodtime_productivity.shared.generated.resources.app_limits_enable_blocking_title
import goodtime_productivity.shared.generated.resources.app_limits_go_home
import goodtime_productivity.shared.generated.resources.app_limits_invalid_limit
import goodtime_productivity.shared.generated.resources.app_limits_limit_name
import goodtime_productivity.shared.generated.resources.app_limits_limit_reached
import goodtime_productivity.shared.generated.resources.app_limits_name_default
import goodtime_productivity.shared.generated.resources.app_limits_no_apps_found
import goodtime_productivity.shared.generated.resources.app_limits_no_limits
import goodtime_productivity.shared.generated.resources.app_limits_save
import goodtime_productivity.shared.generated.resources.app_limits_search_apps
import goodtime_productivity.shared.generated.resources.app_limits_select_at_least_one
import goodtime_productivity.shared.generated.resources.app_limits_select_apps
import goodtime_productivity.shared.generated.resources.app_limits_today
import goodtime_productivity.shared.generated.resources.app_limits_usage_access_description
import goodtime_productivity.shared.generated.resources.app_limits_usage_access_title
import goodtime_productivity.shared.generated.resources.app_limits_invalid_minutes
import goodtime_productivity.shared.generated.resources.app_limits_used_of
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.jetbrains.compose.resources.stringResource
import java.util.UUID

private data class AppLimitsUiState(
    val isLoading: Boolean = true,
    val hasUsageAccess: Boolean = false,
    val hasAccessibilityAccess: Boolean = false,
    val apps: List<AppUsageEntry> = emptyList(),
    val rules: List<AppLimitRule> = emptyList(),
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
actual fun AppLimitsScreen(onNavigateBack: () -> Unit) {
    val context = LocalContext.current.applicationContext
    val lifecycleOwner = LocalLifecycleOwner.current
    val scope = rememberCoroutineScope()
    var uiState by remember { mutableStateOf(AppLimitsUiState()) }
    var editingRule by remember { mutableStateOf<AppLimitRule?>(null) }
    var showEditor by remember { mutableStateOf(false) }
    var pendingDelete by remember { mutableStateOf<AppLimitRule?>(null) }

    val refresh: () -> Unit = remember(context, scope) {
        {
            scope.launch {
                val state = withContext(Dispatchers.IO) {
                    AppLimitsUiState(
                        isLoading = false,
                        hasUsageAccess = AppLimitsRepository.hasUsageAccess(context),
                        hasAccessibilityAccess = AppLimitsRepository.hasAccessibilityAccess(context),
                        apps = AppLimitsRepository.queryLaunchableApps(context),
                        rules = AppLimitsRepository.readRules(context),
                    )
                }
                uiState = state
            }
            Unit
        }
    }

    LaunchedEffect(refresh) { refresh() }
    DisposableEffect(lifecycleOwner, refresh) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) refresh()
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(Res.string.app_limits_title)) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Outlined.ArrowBack, contentDescription = null)
                    }
                },
                actions = {
                    IconButton(onClick = refresh) {
                        Icon(Icons.Outlined.Refresh, contentDescription = null)
                    }
                },
            )
        },
    ) { padding ->
        if (uiState.isLoading) {
            Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize().padding(padding),
                contentPadding = androidx.compose.foundation.layout.PaddingValues(bottom = 24.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                item {
                    Column(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp)) {
                        Text(stringResource(Res.string.app_limits_today), style = MaterialTheme.typography.titleMedium)
                        Spacer(Modifier.height(8.dp))
                        if (!uiState.hasUsageAccess) {
                            PermissionRow(
                                title = stringResource(Res.string.app_limits_usage_access_title),
                                description = stringResource(Res.string.app_limits_usage_access_description),
                                action = stringResource(Res.string.app_limits_usage_access_title),
                                onClick = {
                                    context.startActivity(Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
                                },
                            )
                            Spacer(Modifier.height(8.dp))
                        }
                        if (!uiState.hasAccessibilityAccess) {
                            PermissionRow(
                                title = stringResource(Res.string.app_limits_enable_blocking_title),
                                description = stringResource(Res.string.app_limits_enable_blocking_description),
                                action = stringResource(Res.string.app_limits_enable_blocking_title),
                                onClick = {
                                    context.startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
                                },
                            )
                        }
                    }
                }
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(start = 16.dp, end = 16.dp, top = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            stringResource(Res.string.app_limits_daily_limits),
                            modifier = Modifier.weight(1f),
                            style = MaterialTheme.typography.titleMedium,
                        )
                        Button(onClick = {
                            editingRule = null
                            showEditor = true
                        }) {
                            Icon(Icons.Outlined.Add, contentDescription = null)
                            Text(stringResource(Res.string.app_limits_add_limit))
                        }
                    }
                }
                if (uiState.rules.isEmpty()) {
                    item {
                        Text(
                            stringResource(Res.string.app_limits_no_limits),
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
                items(uiState.rules, key = { it.id }) { rule ->
                    LimitRuleRow(
                        rule = rule,
                        apps = uiState.apps,
                        usage = uiState.apps.associate { it.packageName to it.usageMillis },
                        onEdit = {
                            editingRule = rule
                            showEditor = true
                        },
                        onToggle = { enabled ->
                            val updated = uiState.rules.map { if (it.id == rule.id) it.copy(isEnabled = enabled) else it }
                            AppLimitsRepository.writeRules(context, updated)
                            uiState = uiState.copy(rules = updated)
                        },
                        onDelete = { pendingDelete = rule },
                    )
                }
                item {
                    Text(
                        stringResource(Res.string.app_limits_apps_today),
                        modifier = Modifier.fillMaxWidth().padding(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 4.dp),
                        style = MaterialTheme.typography.titleMedium,
                    )
                }
                val appsUsedToday = uiState.apps.filter { it.usageMillis > 0L }
                if (appsUsedToday.isEmpty()) {
                    item {
                        Text(
                            stringResource(Res.string.app_limits_no_apps_found),
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
                items(appsUsedToday, key = { it.packageName }) { app ->
                    AppUsageRow(app)
                }
            }
        }
    }

    if (showEditor) {
        AppLimitEditor(
            rule = editingRule,
            apps = uiState.apps,
            existingRules = uiState.rules,
            onDismiss = { showEditor = false },
            onSave = { rule ->
                val updated = uiState.rules.filterNot { it.id == rule.id } + rule
                AppLimitsRepository.writeRules(context, updated)
                uiState = uiState.copy(rules = updated)
                showEditor = false
            },
        )
    }

    pendingDelete?.let { rule ->
        AlertDialog(
            onDismissRequest = { pendingDelete = null },
            title = { Text(stringResource(Res.string.app_limits_delete_confirmation)) },
            confirmButton = {
                TextButton(onClick = {
                    val updated = uiState.rules.filterNot { it.id == rule.id }
                    AppLimitsRepository.writeRules(context, updated)
                    uiState = uiState.copy(rules = updated)
                    pendingDelete = null
                }) { Text(stringResource(Res.string.app_limits_delete)) }
            },
            dismissButton = {
                TextButton(onClick = { pendingDelete = null }) { Text(stringResource(Res.string.app_limits_cancel)) }
            },
        )
    }
}

@Composable
private fun PermissionRow(
    title: String,
    description: String,
    action: String,
    onClick: () -> Unit,
) {
    Card(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(title, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
            Text(description, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Button(onClick = onClick) { Text(action) }
        }
    }
}

@Composable
private fun LimitRuleRow(
    rule: AppLimitRule,
    apps: List<AppUsageEntry>,
    usage: Map<String, Long>,
    onEdit: () -> Unit,
    onToggle: (Boolean) -> Unit,
    onDelete: () -> Unit,
) {
    val totalUsage = AppLimitsRepository.ruleUsageMillis(rule, usage)
    val fraction = (totalUsage.toFloat() / (rule.dailyLimitMinutes * 60_000L)).coerceIn(0f, 1f)
    val appNames = rule.packageNames.mapNotNull { name -> apps.firstOrNull { it.packageName == name }?.label }
    Card(
        onClick = onEdit,
        modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp),
        shape = RoundedCornerShape(8.dp),
    ) {
        Column(Modifier.padding(horizontal = 16.dp, vertical = 12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text(rule.name, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Medium)
                    Text(
                        appNames.joinToString().ifBlank { rule.packageNames.joinToString() },
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                Switch(checked = rule.isEnabled, onCheckedChange = onToggle)
                IconButton(onClick = onDelete) {
                    Icon(Icons.Outlined.Delete, contentDescription = stringResource(Res.string.app_limits_delete))
                }
            }
            LinearProgressIndicator(progress = { fraction }, modifier = Modifier.fillMaxWidth())
            Text(
                stringResource(Res.string.app_limits_used_of, formatUsage(totalUsage), formatLimit(rule.dailyLimitMinutes)),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun AppUsageRow(app: AppUsageEntry) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Image(
            bitmap = app.icon.asImageBitmap(),
            contentDescription = null,
            modifier = Modifier.size(36.dp),
        )
        Text(app.label, modifier = Modifier.weight(1f), style = MaterialTheme.typography.bodyMedium)
        Text(formatUsage(app.usageMillis), style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AppLimitEditor(
    rule: AppLimitRule?,
    apps: List<AppUsageEntry>,
    existingRules: List<AppLimitRule>,
    onDismiss: () -> Unit,
    onSave: (AppLimitRule) -> Unit,
) {
    val selectAppError = stringResource(Res.string.app_limits_select_at_least_one)
    val invalidLimitError = stringResource(Res.string.app_limits_invalid_minutes)
    var name by remember(rule?.id) { mutableStateOf(rule?.name.orEmpty()) }
    var limit by remember(rule?.id) { mutableStateOf(rule?.dailyLimitMinutes?.toString() ?: "60") }
    var selectedPackages by remember(rule?.id) { mutableStateOf(rule?.packageNames?.toSet().orEmpty()) }
    var query by remember { mutableStateOf("") }
    var error by remember { mutableStateOf<String?>(null) }
    val blockedPackages = existingRules.filter { it.id != rule?.id }.flatMap { it.packageNames }.toSet()
    val filteredApps = apps.filter { it.label.contains(query, ignoreCase = true) }
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = sheetState) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp).padding(bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(stringResource(Res.string.app_limits_select_apps), style = MaterialTheme.typography.titleLarge)
            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                label = { Text(stringResource(Res.string.app_limits_limit_name)) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )
            OutlinedTextField(
                value = limit,
                onValueChange = { value -> limit = value.filter(Char::isDigit) },
                label = { Text(stringResource(Res.string.app_limits_daily_limit_minutes)) },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )
            OutlinedTextField(
                value = query,
                onValueChange = { query = it },
                label = { Text(stringResource(Res.string.app_limits_search_apps)) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )
            Column(Modifier.height(240.dp)) {
                LazyColumn(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    items(filteredApps, key = { it.packageName }) { app ->
                        val alreadyLimited = app.packageName in blockedPackages
                        Row(
                            modifier = Modifier.fillMaxWidth().clickable(enabled = !alreadyLimited) {
                                selectedPackages = if (app.packageName in selectedPackages) selectedPackages - app.packageName else selectedPackages + app.packageName
                            }.padding(vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                        ) {
                            Image(app.icon.asImageBitmap(), contentDescription = null, modifier = Modifier.size(32.dp))
                            Column(Modifier.weight(1f)) {
                                Text(app.label, style = MaterialTheme.typography.bodyMedium)
                                if (alreadyLimited) {
                                    Text(
                                        stringResource(Res.string.app_limits_app_already_limited),
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    )
                                }
                            }
                            androidx.compose.material3.Checkbox(
                                checked = app.packageName in selectedPackages,
                                onCheckedChange = { checked ->
                                    selectedPackages = if (checked) selectedPackages + app.packageName else selectedPackages - app.packageName
                                },
                                enabled = !alreadyLimited,
                            )
                        }
                    }
                }
            }
            error?.let { Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall) }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                TextButton(onClick = onDismiss) { Text(stringResource(Res.string.app_limits_cancel)) }
                Spacer(Modifier.weight(1f))
                Button(onClick = {
                    val minutes = limit.toIntOrNull()
                    if (selectedPackages.isEmpty()) {
                        error = selectAppError
                    } else if (minutes == null || minutes !in 1..1440) {
                        error = invalidLimitError
                    } else {
                        onSave(
                            AppLimitRule(
                                id = rule?.id ?: UUID.randomUUID().toString(),
                                name = name.trim().ifBlank { selectedPackages.firstOrNull()?.let { pkg -> apps.firstOrNull { it.packageName == pkg }?.label } ?: "Daily limit" },
                                packageNames = selectedPackages.toList(),
                                dailyLimitMinutes = minutes,
                                isEnabled = rule?.isEnabled ?: true,
                            ),
                        )
                    }
                }) { Text(stringResource(Res.string.app_limits_save)) }
            }
        }
    }
}

private fun formatUsage(milliseconds: Long): String {
    val totalMinutes = milliseconds.coerceAtLeast(0L) / 60_000L
    val hours = totalMinutes / 60
    val minutes = totalMinutes % 60
    return if (hours > 0) "${hours}h ${minutes}m" else "${minutes}m"
}

private fun formatLimit(minutes: Int): String {
    val hours = minutes / 60
    val remainingMinutes = minutes % 60
    return if (hours > 0 && remainingMinutes > 0) "${hours}h ${remainingMinutes}m" else if (hours > 0) "${hours}h" else "${minutes}m"
}
