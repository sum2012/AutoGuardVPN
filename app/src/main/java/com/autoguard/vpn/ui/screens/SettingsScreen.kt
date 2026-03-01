package com.autoguard.vpn.ui.screens

import android.content.Intent
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
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
    val appLanguage by viewModel.appLanguage.collectAsState()
    val context = LocalContext.current
    
    val showLanguageDialog = remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.settings_title)) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.action_cancel)
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

            // Language Selection
            SettingsInfoItem(
                title = stringResource(R.string.settings_language),
                value = when (appLanguage) {
                    "zh-rTW" -> stringResource(R.string.language_tchinese)
                    "zh-rCN" -> stringResource(R.string.language_schinese)
                    else -> stringResource(R.string.language_english)
                },
                onClick = { showLanguageDialog.value = true }
            )

            HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))

            // Data Source info
            SettingsInfoItem(
                title = stringResource(R.string.settings_server),
                value = dataSourceType.name
            )

            HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

            // App Category
            CategoryHeader(text = stringResource(R.string.settings_about))

            SettingsInfoItem(
                title = stringResource(R.string.settings_version),
                value = "1.0.2"
            )

            HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))

            SettingsInfoItem(
                title = "Source",
                value = "GitHub",
                onClick = {
                    val intent = Intent(Intent.ACTION_VIEW, "https://github.com/sum2012/AutoGuardVPN".toUri())
                    context.startActivity(intent)
                }
            )
        }
    }

    if (showLanguageDialog.value) {
        LanguageDialog(
            currentLanguage = appLanguage,
            onDismiss = { showLanguageDialog.value = false },
            onLanguageSelected = { 
                viewModel.setLanguage(it)
                showLanguageDialog.value = false
            }
        )
    }
}

@Composable
fun LanguageDialog(
    currentLanguage: String,
    onDismiss: () -> Unit,
    onLanguageSelected: (String) -> Unit
) {
    val languages = listOf(
        LanguageOption("en", stringResource(R.string.language_english)),
        LanguageOption("zh-rTW", stringResource(R.string.language_tchinese)),
        LanguageOption("zh-rCN", stringResource(R.string.language_schinese))
    )

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.settings_language)) },
        text = {
            Column(Modifier.selectableGroup()) {
                languages.forEach { language ->
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .height(56.dp)
                            .selectable(
                                selected = (language.code == currentLanguage),
                                onClick = { onLanguageSelected(language.code) },
                                role = Role.RadioButton
                            )
                            .padding(horizontal = 16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(
                            selected = (language.code == currentLanguage),
                            onClick = null
                        )
                        Text(
                            text = language.name,
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
