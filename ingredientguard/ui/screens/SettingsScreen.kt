package com.ingredientguard.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ingredientguard.viewmodel.SettingsViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    viewModel: SettingsViewModel,
    onNavigateBack: () -> Unit
) {
    val settings = viewModel.settings
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Setări") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, "Înapoi")
                    }
                }
            )
        }
    ) { paddingValues ->
        if (settings == null) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Notifications Section
                item {
                    SettingsSection(
                        title = "Notificări",
                        icon = Icons.Default.Notifications
                    ) {
                        SettingsToggle(
                            title = "Activează notificările",
                            subtitle = "Primește alertări pentru alergeni",
                            checked = settings.notificationsEnabled,
                            onCheckedChange = viewModel::updateNotifications
                        )
                        
                        SettingsToggle(
                            title = "Sunet",
                            subtitle = "Redă sunet la detectarea alergenilor",
                            checked = settings.soundEnabled,
                            onCheckedChange = viewModel::updateSound,
                            enabled = settings.notificationsEnabled
                        )
                        
                        SettingsToggle(
                            title = "Vibrație",
                            subtitle = "Vibrează la alertări importante",
                            checked = settings.vibrationEnabled,
                            onCheckedChange = viewModel::updateVibration,
                            enabled = settings.notificationsEnabled
                        )
                    }
                }
                
                // Scanning Section
                item {
                    SettingsSection(
                        title = "Scanare",
                        icon = Icons.Default.Scanner
                    ) {
                        SettingsToggle(
                            title = "Salvare automată",
                            subtitle = "Salvează automat rezultatele scanărilor",
                            checked = settings.autoSaveScans,
                            onCheckedChange = viewModel::updateAutoSave
                        )
                        
                        SettingsDropdown(
                            title = "Prag de alertă",
                            subtitle = "Nivelul minim pentru alertări",
                            selectedValue = settings.warningThreshold,
                            options = mapOf(
                                "LOW" to "Scăzut",
                                "MEDIUM" to "Mediu", 
                                "HIGH" to "Ridicat"
                            ),
                            onValueChange = viewModel::updateWarningThreshold
                        )
                    }
                }
                
                // Appearance Section
                item {
                    SettingsSection(
                        title = "Aspect",
                        icon = Icons.Default.Palette
                    ) {
                        SettingsDropdown(
                            title = "Temă",
                            subtitle = "Aspectul aplicației",
                            selectedValue = settings.theme,
                            options = mapOf(
                                "LIGHT" to "Luminos",
                                "DARK" to "Întunecat",
                                "SYSTEM" to "Sistem"
                            ),
                            onValueChange = viewModel::updateTheme
                        )
                        
                        SettingsDropdown(
                            title = "Limbă",
                            subtitle = "Limba aplicației",
                            selectedValue = settings.language,
                            options = mapOf(
                                "ro" to "Română",
                                "en" to "English"
                            ),
                            onValueChange = { /* Handle language change */ }
                        )
                    }
                }
                
                // About Section
                item {
                    SettingsSection(
                        title = "Despre",
                        icon = Icons.Default.Info
                    ) {
                        SettingsItem(
                            title = "Versiunea aplicației",
                            subtitle = "1.0.0",
                            onClick = { }
                        )
                        
                        SettingsItem(
                            title = "Termeni și condiții",
                            subtitle = "Citește termenii de utilizare",
                            onClick = { },
                            showArrow = true
                        )
                        
                        SettingsItem(
                            title = "Politica de confidențialitate",
                            subtitle = "Cum îți protejăm datele",
                            onClick = { },
                            showArrow = true
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun SettingsSection(
    title: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    content: @Composable ColumnScope.() -> Unit
) {
    Card {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    icon,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = title,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }
            
            Spacer(modifier = Modifier.height(12.dp))
            
            content()
        }
    }
}

@Composable
fun SettingsToggle(
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    enabled: Boolean = true
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .alpha(if (enabled) 1f else 0.6f),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                fontSize = 16.sp,
                fontWeight = FontWeight.Medium
            )
            Text(
                text = subtitle,
                fontSize = 14.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            enabled = enabled
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsDropdown(
    title: String,
    subtitle: String,
    selectedValue: String,
    options: Map<String, String>,
    onValueChange: (String) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    
    Column {
        ExposedDropdownMenuBox(
            expanded = expanded,
            onExpandedChange = { expanded = !expanded }
        ) {
            OutlinedTextField(
                value = options[selectedValue] ?: selectedValue,
                onValueChange = { },
                readOnly = true,
                label = { Text(title) },
                trailingIcon = {
                    ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded)
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .menuAnchor()
            )
            
            ExposedDropdownMenu(
                expanded = expanded,
                onDismissRequest = { expanded = false }
            ) {
                options.forEach { (key, value) ->
                    DropdownMenuItem(
                        text = { Text(value) },
                        onClick = {
                            onValueChange(key)
                            expanded = false
                        }
                    )
                }
            }
        }
        
        Text(
            text = subtitle,
            fontSize = 12.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(start = 16.dp, top = 4.dp)
        )
    }
}

@Composable
fun SettingsItem(
    title: String,
    subtitle: String,
    onClick: () -> Unit,
    showArrow: Boolean = false
) {
    Surface(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Medium
                )
                Text(
                    text = subtitle,
                    fontSize = 14.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            
            if (showArrow) {
                Icon(
                    Icons.Default.ChevronRight,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}