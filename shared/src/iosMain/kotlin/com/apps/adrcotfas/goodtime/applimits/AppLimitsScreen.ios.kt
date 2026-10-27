package com.apps.adrcotfas.goodtime.applimits

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import goodtime_productivity.shared.generated.resources.Res
import goodtime_productivity.shared.generated.resources.app_limits_android_only
import org.jetbrains.compose.resources.stringResource

@Composable
actual fun AppLimitsScreen(onNavigateBack: () -> Unit) {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Text(
            text = stringResource(Res.string.app_limits_android_only),
            color = MaterialTheme.colorScheme.onSurface,
        )
    }
}
