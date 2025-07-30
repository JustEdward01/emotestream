package com.ingredientguard.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.ingredientguard.data.models.AnalysisResult
import com.ingredientguard.ui.components.AllergenCard
import com.ingredientguard.ui.components.IngredientsCard
import com.ingredientguard.ui.components.FullTextCard

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ResultsScreen(
    result: AnalysisResult,
    onBack: () -> Unit
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Rezultate analiză") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, "Înapoi")
                    }
                }
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            if (result.allergens.isNotEmpty()) {
                item {
                    AllergenCard(allergens = result.allergens)
                }
            }

            item {
                IngredientsCard(ingredients = result.ingredients)
            }

            item {
                FullTextCard(text = result.fullText)
            }
        }
    }
}
