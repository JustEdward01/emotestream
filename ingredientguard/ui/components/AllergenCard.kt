package com.ingredientguard.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.ingredientguard.data.models.DetectedAllergen
import com.ingredientguard.data.models.AllergenSeverity

@Composable
fun AllergenCard(allergens: List<DetectedAllergen>) {
    Card(
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.errorContainer
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    Icons.Default.Warning,
                    contentDescription = "Atenție",
                    tint = MaterialTheme.colorScheme.error
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    "⚠️ ALERGENI DETECTAȚI",
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.error
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            allergens.forEach { allergen ->
                AllergenChip(allergen = allergen)
                Spacer(modifier = Modifier.height(4.dp))
            }
        }
    }
}

@Composable
fun AllergenChip(allergen: DetectedAllergen) {
    val color = when (allergen.severity) {
        AllergenSeverity.HIGH -> MaterialTheme.colorScheme.error
        AllergenSeverity.MEDIUM -> Color(0xFFFF9800)
        AllergenSeverity.LOW -> Color(0xFF4CAF50)
    }

    AssistChip(
        onClick = { },
        label = { Text(allergen.name.uppercase()) },
        colors = AssistChipDefaults.assistChipColors(
            containerColor = color.copy(alpha = 0.1f),
            labelColor = color
        )
    )
}
