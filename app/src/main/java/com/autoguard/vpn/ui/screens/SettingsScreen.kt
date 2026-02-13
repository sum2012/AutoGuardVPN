package com.autoguard.vpn.ui.screens

import android.content.Intent
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.net.toUri
import com.autoguard.vpn.R
import com.autoguard.vpn.ui.viewmodel.MainViewModel

/**
 * Settings Screen
 * Allows users to configure VPN behavior and app preferences
 *
 * @param viewModel Main ViewModel
 * @param onNavigateBack Callback to go back
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    viewModel: MainViewModel,
    onNavigateBack: () -> Unit
) {
    val killSwitchEnabled by viewModel.killSwitchEnabled.collectAsState()
    val dataSourceType by viewModel.dataSourceType.collectAsState()
    val currentLanguage by viewModel.language.collectAsState(initial = "")
    val context = LocalContext.current
    
    var showLanguageDialog by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.settings_title)) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back"
                        )
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(rememberScrollState())
        ) {
            // VPN Settings Category
            CategoryHeader(text = stringResource(R.string.settings_connection))

            // Kill Switch
            SettingsSwitchItem(
                title = stringResource(R.string.settings_kill_switch),
                subtitle = stringResource(R.string.settings_kill_switch_desc),
                checked = killSwitchEnabled,
                onCheckedChange = { viewModel.setKillSwitchEnabled(it) }
            )

            HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))

            // Data Source info
            SettingsInfoItem(
                title = stringResource(R.string.settings_data_source),
                value = dataSourceType.name
            )

            HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

            // Appearance Category
            CategoryHeader(text = stringResource(R.string.settings_appearance))
            
            // Language Selection
            SettingsInfoItem(
                title = stringResource(R.string.settings_language),
                value = when (currentLanguage) {
                    "en" -> stringResource(R.string.language_english)
                    "zh-CN" -> stringResource(R.string.language_chinese_simplified)
                    "zh-TW" -> stringResource(R.string.language_chinese_traditional)
                    else -> stringResource(R.string.language_english)
                },
                onClick = { showLanguageDialog = true }
            )

            HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

            // App Category
            CategoryHeader(text = stringResource(R.string.settings_about))

            SettingsInfoItem(
                title = stringResource(R.string.settings_version),
                value = "1.0.1"
            )

            HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))

            SettingsInfoItem(
                title = stringResource(R.string.settings_source),
                value = "GitHub",
                onClick = {
                    val intent = Intent(Intent.ACTION_VIEW, "https://github.com/sum2012/AutoGuardVPN".toUri())
                    context.startActivity(intent)
                }
            )
        }
    }

    if (showLanguageDialog) {
        LanguageSelectionDialog(
            currentLanguage = currentLanguage,
            onLanguageSelected = { 
                viewModel.setLanguage(it)
                showLanguageDialog = false
            },
            onDismiss = { showLanguageDialog = false }
        )
    }
}

@Composable
fun LanguageSelectionDialog(
    currentLanguage: String,
    onLanguageSelected: (String) -> Unit,
    onDismiss: () -> Unit
) {
    val languages = listOf(
        "en" to stringResource(R.string.language_english),
        "zh-CN" to stringResource(R.string.language_chinese_simplified),
        "zh-TW" to stringResource(R.string.language_chinese_traditional)
    )

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.language_select_title)) },
        text = {
            Column {
                languages.forEach { (code, name) ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onLanguageSelected(code) }
                            .padding(vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(
                            selected = code == currentLanguage || (currentLanguage == "" && code == "en"),
                            onClick = { onLanguageSelected(code) }
                        )
                        Text(
                            text = name,
                            style = MaterialTheme.typography.bodyLarge,
                            modifier = Modifier.padding(start = 16.dp)
                        )
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.action_cancel))
            }
        }
    )
}

@Composable
private fun CategoryHeader(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.labelLarge,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(start = 16.dp, top = 16.dp, end = 16.dp, bottom = 8.dp),
        fontWeight = FontWeight.Bold
    )
}

@Composable
private fun SettingsSwitchItem(
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Medium
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange
        )
    }
}

@Composable
private fun SettingsInfoItem(
    title: String,
    value: String,
    onClick: (() -> Unit)? = null
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier)
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Medium
            )
        }
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            color = if (onClick != null) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}
