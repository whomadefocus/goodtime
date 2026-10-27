package com.apps.adrcotfas.goodtime.applimits

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.apps.adrcotfas.goodtime.ui.ApplicationTheme
import goodtime_productivity.shared.generated.resources.Res
import goodtime_productivity.shared.generated.resources.app_limits_go_home
import goodtime_productivity.shared.generated.resources.app_limits_limit_description
import goodtime_productivity.shared.generated.resources.app_limits_limit_reached
import org.jetbrains.compose.resources.stringResource

class AppLimitBlockActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val ruleName = intent.getStringExtra(EXTRA_RULE_NAME).orEmpty()
        val limitMinutes = intent.getIntExtra(EXTRA_LIMIT_MINUTES, 0)
        setContent {
            ApplicationTheme {
                BlockScreen(ruleName, limitMinutes) { goHome() }
            }
        }
    }

    private fun goHome() {
        val homeIntent = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_HOME)
        startActivity(homeIntent)
        finish()
    }

    companion object {
        const val EXTRA_RULE_NAME = "rule_name"
        const val EXTRA_LIMIT_MINUTES = "limit_minutes"
    }
}

@Composable
private fun BlockScreen(
    ruleName: String,
    limitMinutes: Int,
    onGoHome: () -> Unit,
) {
    Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
        Column(
            modifier = Modifier.fillMaxSize().padding(32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            Text(ruleName, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.SemiBold)
            Text(
                stringResource(Res.string.app_limits_limit_reached, ruleName),
                modifier = Modifier.padding(top = 12.dp, bottom = 8.dp),
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(
                stringResource(Res.string.app_limits_limit_description, limitMinutes),
                modifier = Modifier.padding(bottom = 24.dp),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Button(onClick = onGoHome) {
                Text(stringResource(Res.string.app_limits_go_home))
            }
        }
    }
}
