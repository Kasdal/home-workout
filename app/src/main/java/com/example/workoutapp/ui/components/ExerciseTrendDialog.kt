package com.example.workoutapp.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.workoutapp.domain.trend.ExerciseTrendCalculator
import com.example.workoutapp.domain.trend.ExerciseTrendPoint
import com.example.workoutapp.ui.theme.NeonGreen
import com.example.workoutapp.util.formatKg

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExerciseTrendDialog(
    exerciseName: String,
    points: List<ExerciseTrendPoint>,
    onDismiss: () -> Unit
) {
    var rangeDays by remember { mutableStateOf<Int?>(90) }
    val filtered = remember(points, rangeDays) {
        ExerciseTrendCalculator.filterByRange(points, System.currentTimeMillis(), rangeDays)
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(exerciseName) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    RangeChip(label = "30D", value = 30, selected = rangeDays == 30, onSelect = { rangeDays = 30 })
                    RangeChip(label = "90D", value = 90, selected = rangeDays == 90, onSelect = { rangeDays = 90 })
                    RangeChip(label = "All", value = null, selected = rangeDays == null, onSelect = { rangeDays = null })
                }

                if (filtered.isEmpty()) {
                    Text(
                        text = "No sessions in this range yet.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 16.dp)
                    )
                } else {
                    TrendLineChart(title = "Top weight per session", values = filtered.map { it.weight })
                    TrendLineChart(title = "Volume per session", values = filtered.map { it.volume })
                    Text(
                        text = frequencySummary(filtered),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Close")
            }
        }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun RangeChip(label: String, value: Int?, selected: Boolean, onSelect: () -> Unit) {
    FilterChip(
        selected = selected,
        onClick = onSelect,
        label = { Text(label) }
    )
}

private fun frequencySummary(points: List<ExerciseTrendPoint>): String {
    if (points.isEmpty()) return ""
    val spanDays = ((points.last().dateMillis - points.first().dateMillis) / 86_400_000L).coerceAtLeast(1)
    val perWeek = points.size * 7f / spanDays
    return "${points.size} session${if (points.size == 1) "" else "s"} in range · ~${String.format("%,.1f", perWeek)} per week"
}

@Composable
private fun TrendLineChart(title: String, values: List<Float>) {
    Column {
        Text(
            text = title,
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Bold
        )
        Canvas(
            modifier = Modifier
                .fillMaxWidth()
                .height(110.dp)
                .padding(vertical = 4.dp)
        ) {
            val maxValue = values.maxOrNull()?.takeIf { it > 0f } ?: return@Canvas
            val stepX = size.width / (values.size - 1).coerceAtLeast(1)

            fun yFor(value: Float): Float {
                return size.height - (value / maxValue * (size.height * 0.9f)) - size.height * 0.05f
            }

            if (values.size == 1) {
                drawCircle(
                    color = NeonGreen,
                    radius = 6.dp.toPx(),
                    center = Offset(size.width / 2f, yFor(values.first()))
                )
                return@Canvas
            }

            val path = Path()
            values.forEachIndexed { index, value ->
                val x = index * stepX
                val y = yFor(value)
                if (index == 0) path.moveTo(x, y) else path.lineTo(x, y)
            }

            values.forEachIndexed { index, value ->
                drawCircle(
                    color = NeonGreen,
                    radius = 4.dp.toPx(),
                    center = Offset(index * stepX, yFor(value))
                )
            }

            drawPath(path = path, color = NeonGreen, style = Stroke(width = 2.dp.toPx()))
        }
        Row(modifier = Modifier.fillMaxWidth()) {
            Text(
                text = formatKg(values.firstOrNull() ?: 0f),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.weight(1f))
            Text(
                text = "now ${formatKg(values.lastOrNull() ?: 0f)}",
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Bold,
                color = NeonGreen
            )
        }
    }
}
