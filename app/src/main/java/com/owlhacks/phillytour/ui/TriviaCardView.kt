package com.owlhacks.phillytour.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Help
import androidx.compose.material.icons.filled.HighlightOff
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.owlhacks.phillytour.model.TriviaQuestion

@Composable
fun TriviaCardView(
    trivia: TriviaQuestion,
    modifier: Modifier = Modifier
) {
    var selectedIndex by remember(trivia) { mutableStateOf<Int?>(null) }
    var hasSubmitted by remember(trivia) { mutableStateOf(false) }

    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Header Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Help,
                        contentDescription = "Trivia",
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = "River Trivia",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                }

                if (hasSubmitted) {
                    val isCorrect = selectedIndex == trivia.correctIndex
                    Text(
                        text = if (isCorrect) "✓ Correct!" else "✕ Try Again",
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Bold,
                        color = if (isCorrect) Color(0xFF16A34A) else Color(0xFFDC2626)
                    )
                }
            }

            // Question
            Text(
                text = trivia.question,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface
            )

            // Options
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                trivia.options.forEachIndexed { index, option ->
                    val isCorrectChoice = index == trivia.correctIndex
                    val isSelected = index == selectedIndex

                    val containerColor = when {
                        !hasSubmitted -> MaterialTheme.colorScheme.surface
                        isCorrectChoice -> Color(0xFFDCFCE7)
                        isSelected -> Color(0xFFFEE2E2)
                        else -> MaterialTheme.colorScheme.surface
                    }

                    val borderColor = when {
                        !hasSubmitted -> MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)
                        isCorrectChoice -> Color(0xFF22C55E)
                        isSelected -> Color(0xFFEF4444)
                        else -> MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)
                    }

                    val contentColor = when {
                        !hasSubmitted -> MaterialTheme.colorScheme.onSurface
                        isCorrectChoice -> Color(0xFF15803D)
                        isSelected -> Color(0xFFB91C1C)
                        else -> MaterialTheme.colorScheme.onSurface
                    }

                    OutlinedButton(
                        onClick = {
                            selectedIndex = index
                            hasSubmitted = true
                        },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(10.dp),
                        colors = ButtonDefaults.outlinedButtonColors(
                            containerColor = containerColor,
                            contentColor = contentColor
                        ),
                        border = BorderStroke(1.5.dp, borderColor),
                        contentPadding = PaddingValues(horizontal = 14.dp, vertical = 12.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = option,
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = if (hasSubmitted && isCorrectChoice) FontWeight.Bold else FontWeight.Normal
                            )

                            if (hasSubmitted) {
                                if (isCorrectChoice) {
                                    Icon(
                                        imageVector = Icons.Default.CheckCircle,
                                        contentDescription = "Correct",
                                        tint = Color(0xFF22C55E),
                                        modifier = Modifier.size(20.dp)
                                    )
                                } else if (isSelected) {
                                    Icon(
                                        imageVector = Icons.Default.HighlightOff,
                                        contentDescription = "Wrong",
                                        tint = Color(0xFFEF4444),
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
