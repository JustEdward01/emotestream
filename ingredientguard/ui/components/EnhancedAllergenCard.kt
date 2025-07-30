package com.ingredientguard.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.ingredientguard.data.models.DetectedAllergen
import com.ingredientguard.data.models.AllergenSeverity
import androidx.compose.ui.unit.sp

@Composable
fun EnhancedAllergenCard(
    allergens: List<DetectedAllergen>,
    personalAllergens: List<String> = emptyList()
) {
    Card(
        colors = CardDefaults.cardColors(
            containerColor = if (personalAllergens.isNotEmpty()) {
                MaterialTheme.colorScheme.error.copy(alpha = 0.15f)
            } else {
                MaterialTheme.colorScheme.errorContainer
            }
        ),
        border = if (personalAllergens.isNotEmpty()) {
            BorderStroke(2.dp, MaterialTheme.colorScheme.error)
        } else null
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
                    text = if (personalAllergens.isNotEmpty()) {
                        "ALERTĂ ALERGII PERSONALE!"
                    } else {
                        "ALERGENI DETECTAȚI"
                    },
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.error
                )
            }
            
            if (personalAllergens.isNotEmpty()) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Acest produs conține alergeni din lista ta personală!",
                    fontSize = 14.sp,
                    color = MaterialTheme.colorScheme.error,
                    fontWeight = FontWeight.Medium
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            allergens.forEach { allergen ->
                EnhancedAllergenChip(
                    allergen = allergen,
                    isPersonal = personalAllergens.contains(allergen.name.lowercase())
                )
                Spacer(modifier = Modifier.height(4.dp))
            }
        }
    }
}

@Composable
fun EnhancedAllergenChip(
    allergen: DetectedAllergen,
    isPersonal: Boolean = false
) {
    val color = if (isPersonal) {
        MaterialTheme.colorScheme.error
    } else {
        when (allergen.severity) {
            AllergenSeverity.HIGH -> MaterialTheme.colorScheme.error
            AllergenSeverity.MEDIUM -> Color(0xFFFF9800)
            AllergenSeverity.LOW -> Color(0xFF4CAF50)
        }
    }

    AssistChip(
        onClick = { },
        label = { 
            Text(
                text = if (isPersonal) {
                    "${allergen.name.uppercase()} (PERSONAL)"
                } else {
                    allergen.name.uppercase()
                },
                fontWeight = if (isPersonal) FontWeight.Bold else FontWeight.Normal
            )
        },
        leadingIcon = if (isPersonal) {
            {
                Icon(
                    Icons.Default.PriorityHigh,
                    contentDescription = "Personal allergen",
                    modifier = Modifier.size(16.dp)
                )
            }
        } else null,
        colors = AssistChipDefaults.assistChipColors(
            containerColor = color.copy(alpha = if (isPersonal) 0.2f else 0.1f),
            labelColor = color,
            leadingIconContentColor = color
        ),
        border = if (isPersonal) {
            BorderStroke(1.dp, color)
        } else null
    )
}

