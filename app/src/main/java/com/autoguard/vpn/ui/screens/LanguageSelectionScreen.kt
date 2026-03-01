package com.autoguard.vpn.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.autoguard.vpn.R
import com.autoguard.vpn.ui.viewmodel.MainViewModel

data class LanguageOption(val code: String, val name: String)

@Composable
fun LanguageSelectionScreen(
    viewModel: MainViewModel,
    onLanguageSelected: () -> Unit
) {
    val currentLanguage by viewModel.appLanguage.collectAsState()
    
    val languages = listOf(
        LanguageOption("en", stringResource(R.string.language_english)),
        LanguageOption("zh-rTW", stringResource(R.string.language_tchinese)),
        LanguageOption("zh-rCN", stringResource(R.string.language_schinese))
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = stringResource(R.string.first_run_language_title),
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold
        )
        
        Spacer(modifier = Modifier.height(8.dp))
        
        Text(
            text = stringResource(R.string.first_run_language_subtitle),
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        
        Spacer(modifier = Modifier.height(32.dp))
        
        Column(Modifier.selectableGroup()) {
            languages.forEach { language ->
                Row(
                    Modifier
                        .fillMaxWidth()
                        .height(56.dp)
                        .selectable(
                            selected = (language.code == currentLanguage),
                            onClick = { viewModel.setLanguage(language.code) },
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
        
        Spacer(modifier = Modifier.height(48.dp))
        
        Button(
            onClick = onLanguageSelected,
            modifier = Modifier.fillMaxWidth(),
            shape = MaterialTheme.shapes.medium
        ) {
            Text(
                text = stringResource(R.string.action_confirm),
                modifier = Modifier.padding(vertical = 8.dp)
            )
        }
    }
}
