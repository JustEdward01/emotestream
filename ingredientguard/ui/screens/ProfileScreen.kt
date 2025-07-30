package com.ingredientguard.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ingredientguard.viewmodel.UserProfileViewModel
import com.ingredientguard.data.models.UserProfile
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreen(
    viewModel: UserProfileViewModel,
    onNavigateBack: () -> Unit,
    onNavigateToSettings: () -> Unit
) {
    val uiState = viewModel.uiState
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Profilul meu") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, "Înapoi")
                    }
                },
                actions = {
                    IconButton(onClick = onNavigateToSettings) {
                        Icon(Icons.Default.Settings, "Setări")
                    }
                }
            )
        }
    ) { paddingValues ->
        if (uiState.isLoading) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }
        } else {
            uiState.profile?.let { profile ->
                ProfileContent(
                    profile = profile,
                    isEditing = uiState.isEditing,
                    onEdit = viewModel::startEditing,
                    onSave = viewModel::updateProfile,
                    onCancel = viewModel::cancelEditing,
                    onUpdateAllergens = viewModel::updatePersonalAllergens,
                    modifier = Modifier.padding(paddingValues)
                )
            }
        }
    }
    
    // Error handling
    uiState.error?.let { error ->
        LaunchedEffect(error) {
            // Show snackbar or toast
            viewModel.clearError()
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileContent(
    profile: UserProfile,
    isEditing: Boolean,
    onEdit: () -> Unit,
    onSave: (UserProfile) -> Unit,
    onCancel: () -> Unit,
    onUpdateAllergens: (List<String>) -> Unit,
    modifier: Modifier = Modifier
) {
    var editedFirstName by remember(profile) { mutableStateOf(profile.firstName) }
    var editedLastName by remember(profile) { mutableStateOf(profile.lastName) }
    var editedEmail by remember(profile) { mutableStateOf(profile.email) }
    var selectedAllergens by remember(profile) { mutableStateOf(profile.personalAllergens.toSet()) }
    
    val availableAllergens = listOf(
        "Gluten", "Lactoza", "Ouă", "Nuci", "Arahide", "Soia", 
        "Pește", "Crustacee", "Susan", "Muștar", "Telină", "Lupin"
    )
    
    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Profile Header
        item {
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer
                )
            ) {
                Column(
                    modifier = Modifier.padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // Avatar placeholder
                    Surface(
                        modifier = Modifier.size(120.dp),
                        shape = CircleShape,
                        color = MaterialTheme.colorScheme.primary
                    ) {
                        Box(
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "${profile.firstName.firstOrNull() ?: ""}${profile.lastName.firstOrNull() ?: ""}",
                                fontSize = 36.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onPrimary
                            )
                        }
                    }
                    
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    Text(
                        text = "${profile.firstName} ${profile.lastName}",
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Bold
                    )
                    
                    if (profile.email.isNotBlank()) {
                        Text(
                            text = profile.email,
                            fontSize = 14.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    if (!isEditing) {
                        Button(
                            onClick = onEdit,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(Icons.Default.Edit, "Editează")
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Editează profilul")
                        }
                    }
                }
            }
        }
        
        // Personal Information Section
        item {
            Card {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Default.Person,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            "Informații personale",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                    
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    if (isEditing) {
                        OutlinedTextField(
                            value = editedFirstName,
                            onValueChange = { editedFirstName = it },
                            label = { Text("Prenume") },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true
                        )
                        
                        Spacer(modifier = Modifier.height(8.dp))
                        
                        OutlinedTextField(
                            value = editedLastName,
                            onValueChange = { editedLastName = it },
                            label = { Text("Nume de familie") },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true
                        )
                        
                        Spacer(modifier = Modifier.height(8.dp))
                        
                        OutlinedTextField(
                            value = editedEmail,
                            onValueChange = { editedEmail = it },
                            label = { Text("Email") },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(
                                keyboardType = KeyboardType.Email
                            )
                        )
                    } else {
                        ProfileInfoRow("Prenume", profile.firstName)
                        ProfileInfoRow("Nume", profile.lastName)
                        if (profile.email.isNotBlank()) {
                            ProfileInfoRow("Email", profile.email)
                        }
                        ProfileInfoRow("Membru din", SimpleDateFormat("dd MMMM yyyy", Locale("ro")).format(profile.dateCreated))
                    }
                }
            }
        }
        
        // Allergens Section
        item {
            Card {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                Icons.Default.Warning,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.error
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                "Alergiile mele",
                                fontSize = 18.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                        
                        if (profile.personalAllergens.isNotEmpty()) {
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = MaterialTheme.colorScheme.error.copy(alpha = 0.1f)
                            ) {
                                Text(
                                    text = "${profile.personalAllergens.size}",
                                    color = MaterialTheme.colorScheme.error,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                )
                            }
                        }
                    }
                    
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    if (isEditing) {
                        Text(
                            "Selectează alergiile tale:",
                            fontSize = 14.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        
                        Spacer(modifier = Modifier.height(8.dp))
                        
                        availableAllergens.forEach { allergen ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Checkbox(
                                    checked = selectedAllergens.contains(allergen),
                                    onCheckedChange = { checked ->
                                        selectedAllergens = if (checked) {
                                            selectedAllergens + allergen
                                        } else {
                                            selectedAllergens - allergen
                                        }
                                    }
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(allergen)
                            }
                        }
                    } else {
                        if (profile.personalAllergens.isEmpty()) {
                            Text(
                                "Nu ai specificat alergii",
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                fontSize = 14.sp
                            )
                        } else {
                            LazyRow(
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                items(profile.personalAllergens) { allergen ->
                                    AssistChip(
                                        onClick = { },
                                        label = { Text(allergen) },
                                        leadingIcon = {
                                            Icon(
                                                Icons.Default.Warning,
                                                contentDescription = null,
                                                modifier = Modifier.size(16.dp)
                                            )
                                        },
                                        colors = AssistChipDefaults.assistChipColors(
                                            containerColor = MaterialTheme.colorScheme.errorContainer,
                                            labelColor = MaterialTheme.colorScheme.onErrorContainer,
                                            leadingIconContentColor = MaterialTheme.colorScheme.error
                                        )
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
        
        // Action buttons when editing
        if (isEditing) {
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    OutlinedButton(
                        onClick = onCancel,
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Anulează")
                    }
                    
                    Button(
                        onClick = {
                            val updatedProfile = profile.copy(
                                firstName = editedFirstName,
                                lastName = editedLastName,
                                email = editedEmail,
                                personalAllergens = selectedAllergens.toList()
                            )
                            onSave(updatedProfile)
                        },
                        modifier = Modifier.weight(1f),
                        enabled = editedFirstName.isNotBlank() && editedLastName.isNotBlank()
                    ) {
                        Text("Salvează")
                    }
                }
            }
        }
    }
}

@Composable
fun ProfileInfoRow(
    label: String,
    value: String
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = label,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            fontSize = 14.sp
        )
        Text(
            text = value,
            fontSize = 14.sp,
            fontWeight = FontWeight.Medium
        )
    }
}