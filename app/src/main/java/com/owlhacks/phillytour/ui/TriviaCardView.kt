package com.owlhacks.phillytour.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.owlhacks.phillytour.model.TriviaQuestion

@Composable
fun TriviaCard(
    trivia: TriviaQuestion,
    selected: Int?,
    onSelect: (Int) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Text(
            text = "Trivia",
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary
        )
        Text(text = trivia.question, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Medium)

        trivia.options.forEachIndexed { index, option ->
            val backgroundColor = when {
                selected == null -> MaterialTheme.colorScheme.surfaceVariant
                index == trivia.correctIndex -> Color(0x3322C55E)
                index == selected -> Color(0x33EF4444)
                else -> MaterialTheme.colorScheme.surfaceVariant
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(backgroundColor)
                    .clickable(enabled = selected == null) { onSelect(index) }
                    .padding(16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(text = option, modifier = Modifier.weight(1f))
                if (selected != null) {
                    if (index == trivia.correctIndex) {
                        Icon(Icons.Filled.Check, contentDescription = null, tint = Color(0xFF22C55E))
                    } else if (index == selected) {
                        Icon(Icons.Filled.Close, contentDescription = null, tint = Color(0xFFEF4444))
                    }
                }
            }
        }

        if (selected != null) {
            val correct = selected == trivia.correctIndex
            Text(
                text = if (correct) "Correct!" else "Not quite — it's ${trivia.options[trivia.correctIndex]}.",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
                color = if (correct) Color(0xFF16A34A) else Color(0xFFEA580C)
            )
        }
    }
}
