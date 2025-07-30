package com.ingredientguard.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.ingredientguard.data.repository.ScanHistoryItem
import com.ingredientguard.ui.components.HistoryItemCard
import com.ingredientguard.viewmodel.HistoryViewModel
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HistoryScreen(
    onBack: () -> Unit,
    onItemClick: (ScanHistoryItem) -> Unit,
    viewModel: HistoryViewModel = viewModel()
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Istoric scanări") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, "Înapoi")
                    }
                },
                actions = {
                    IconButton(
                        onClick = { viewModel.toggleAllergenFilter() }
                    ) {
                        Icon(
                            if (viewModel.showOnlyWithAllergens) Icons.Default.FilterAlt 
                            else Icons.Default.FilterAltOff,
                            "Filtrează alergeni"
                        )
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            // Search bar
            OutlinedTextField(
                value = viewModel.searchQuery,
                onValueChange = { viewModel.searchHistory(it) },
                label = { Text("Caută în istoric") },
                leadingIcon = {
                    Icon(Icons.Default.Search, "Caută")
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
            )

            if (viewModel.isLoading) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            } else if (viewModel.historyItems.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            Icons.Default.History,
                            contentDescription = null,
                            modifier = Modifier.size(64.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = if (viewModel.searchQuery.isNotBlank()) 
                                "Nu s-au găsit rezultate" 
                                else "Nicio scanare salvată",
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(viewModel.historyItems) { item ->
                        HistoryItemCard(
                            item = item,
                            onClick = { onItemClick(item) },
                            onDelete = { viewModel.deleteScan(item) }
                        )
                    }
                }
            }
        }
    }
}
