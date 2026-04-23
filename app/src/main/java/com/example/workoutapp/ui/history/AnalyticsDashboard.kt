package com.example.workoutapp.ui.history

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.TrendingDown
import androidx.compose.material.icons.automirrored.filled.TrendingUp
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.SportsGymnastics
import androidx.compose.material.icons.filled.Timeline
import androidx.compose.material.icons.automirrored.filled.TrendingFlat
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.workoutapp.model.WorkoutSession
import com.example.workoutapp.ui.theme.NeonGreen
import com.example.workoutapp.ui.theme.SurfaceGrey
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun AnalyticsDashboard(
    sessions: List<WorkoutSession>,
    personalRecords: PersonalRecords,
    weeklyOverview: WeeklyOverview,
    exercisePrs: List<ExercisePr>,
    volumeTrend: List<WeeklyVolumePoint>,
    weeklyFrequency: List<Int>,
    insights: List<WorkoutInsight>,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.padding(bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp)
        ) {
            Text(
                text = "Your Analytics",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                color = NeonGreen
            )
            Text(
                text = SimpleDateFormat("EEEE, MMMM d", Locale.getDefault()).format(Date()),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
            )
        }

        WeeklyOverviewCard(weeklyOverview)

        VolumeTrendChart(volumeTrend)

        FrequencyHeatmap(weeklyFrequency, personalRecords)

        ExercisePrTable(exercisePrs)

        AllTimeStats(personalRecords)

        InsightsSection(insights)
    }
}

@Composable
private fun WeeklyOverviewCard(overview: WeeklyOverview) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp),
        colors = CardDefaults.cardColors(containerColor = SurfaceGrey),
        shape = RoundedCornerShape(20.dp)
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "This Week",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = NeonGreen
                )
                if (overview.workoutsThisWeek > overview.workoutsLastWeek) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(
                            Icons.AutoMirrored.Filled.TrendingUp,
                            contentDescription = null,
                            tint = NeonGreen,
                            modifier = Modifier.size(16.dp)
                        )
                        Text(
                            text = "+${overview.workoutsDelta} vs last week",
                            style = MaterialTheme.typography.labelSmall,
                            color = NeonGreen
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                StatPill(
                    icon = Icons.Default.SportsGymnastics,
                    value = "${overview.workoutsThisWeek}",
                    label = "Workouts",
                    color = NeonGreen
                )
                StatPill(
                    icon = Icons.Default.Timeline,
                    value = "${(overview.volumeThisWeek / 1000).let { if (it >= 1) String.format("%.1fk", it) else overview.volumeThisWeek.toInt() }}kg",
                    label = "Volume",
                    color = Color(0xFF64B5F6)
                )
                StatPill(
                    icon = Icons.Default.Schedule,
                    value = "${overview.avgDurationMin}m",
                    label = "Avg Duration",
                    color = Color(0xFFBA68C8)
                )
            }
        }
    }
}

@Composable
private fun StatPill(icon: ImageVector, value: String, label: String, color: Color) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Box(
            modifier = Modifier
                .size(48.dp)
                .clip(CircleShape)
                .background(color.copy(alpha = 0.15f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(icon, contentDescription = null, tint = color, modifier = Modifier.size(24.dp))
        }
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = value,
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            color = color
        )
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
        )
    }
}

@Composable
private fun VolumeTrendChart(trend: List<WeeklyVolumePoint>) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp),
        colors = CardDefaults.cardColors(containerColor = SurfaceGrey),
        shape = RoundedCornerShape(20.dp)
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Text(
                text = "Volume Trend",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = NeonGreen
            )
            Text(
                text = "Last 8 weeks",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
            )

            Spacer(modifier = Modifier.height(16.dp))

            val nonZeroPoints = trend.filter { it.totalVolume > 0 }
            val maxVolume = (nonZeroPoints.maxOfOrNull { it.totalVolume } ?: 1f).coerceAtLeast(1f)

            if (nonZeroPoints.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(120.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "No data yet",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f)
                    )
                }
            } else {
                Canvas(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(120.dp)
                ) {
                    val w = size.width
                    val h = size.height
                    val stepX = w / (trend.size - 1).coerceAtLeast(1)

                    val fillPath = Path().apply {
                        trend.forEachIndexed { index, point ->
                            val x = index * stepX
                            val y = h - (point.totalVolume / maxVolume * h * 0.85f)
                            if (index == 0) moveTo(x, h) else lineTo(x, y)
                        }
                        lineTo(w, h)
                        close()
                    }

                    drawPath(
                        path = fillPath,
                        brush = Brush.verticalGradient(
                            colors = listOf(NeonGreen.copy(alpha = 0.3f), NeonGreen.copy(alpha = 0.0f)),
                            startY = 0f,
                            endY = h
                        )
                    )

                    val linePath = Path().apply {
                        trend.forEachIndexed { index, point ->
                            val x = index * stepX
                            val y = h - (point.totalVolume / maxVolume * h * 0.85f)
                            if (index == 0) moveTo(x, y) else lineTo(x, y)
                        }
                    }

                    drawPath(
                        path = linePath,
                        color = NeonGreen,
                        style = Stroke(width = 3.dp.toPx(), cap = StrokeCap.Round)
                    )

                    trend.forEachIndexed { index, point ->
                        val x = index * stepX
                        val y = h - (point.totalVolume / maxVolume * h * 0.85f)
                        if (point.totalVolume > 0) {
                            drawCircle(
                                color = NeonGreen,
                                radius = 4.dp.toPx(),
                                center = Offset(x, y)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    trend.forEach { point ->
                        Text(
                            text = point.label,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f),
                            textAlign = TextAlign.Center,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun FrequencyHeatmap(frequency: List<Int>, personalRecords: PersonalRecords) {
    val maxFreq = (frequency.maxOrNull() ?: 1).coerceAtLeast(1)
    val weekLabels = listOf("W1", "W2", "W3", "W4")

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp),
        colors = CardDefaults.cardColors(containerColor = SurfaceGrey),
        shape = RoundedCornerShape(20.dp)
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Text(
                text = "Weekly Frequency",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = NeonGreen
            )
            Text(
                text = "Last 4 weeks",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
            )

            Spacer(modifier = Modifier.height(16.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.Bottom
            ) {
                frequency.forEachIndexed { index, count ->
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        val intensity = if (maxFreq > 0) count.toFloat() / maxFreq else 0f
                        val cellColor = when {
                            count == 0 -> MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
                            intensity < 0.33f -> NeonGreen.copy(alpha = 0.3f)
                            intensity < 0.66f -> NeonGreen.copy(alpha = 0.6f)
                            else -> NeonGreen
                        }

                        Box(
                            modifier = Modifier
                                .width(48.dp)
                                .height(48.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .background(cellColor),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = if (count > 0) "$count" else "-",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = if (count > 0) Color.Black else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f)
                            )
                        }

                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = weekLabels.getOrElse(index) { "" },
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                        )
                    }
                }
            }

            if (personalRecords.currentStreak > 0) {
                Spacer(modifier = Modifier.height(12.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        Icons.Default.LocalFireDepartment,
                        contentDescription = null,
                        tint = Color(0xFFFF6B35),
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "${personalRecords.currentStreak} day streak",
                        style = MaterialTheme.typography.labelMedium,
                        color = Color(0xFFFF6B35),
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}

@Composable
private fun ExercisePrTable(prs: List<ExercisePr>) {
    if (prs.isEmpty()) return

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp),
        colors = CardDefaults.cardColors(containerColor = SurfaceGrey),
        shape = RoundedCornerShape(20.dp)
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Text(
                text = "Exercise Records",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = NeonGreen
            )
            Text(
                text = "Best weights by exercise",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
            )

            Spacer(modifier = Modifier.height(12.dp))

            prs.take(8).forEach { pr ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 6.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = pr.name,
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Medium,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = "${pr.sessionCount} sessions",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f)
                        )
                    }

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        val trendIcon = when (pr.trend) {
                            ExerciseTrend.UP -> Icons.AutoMirrored.Filled.TrendingUp
                            ExerciseTrend.DOWN -> Icons.AutoMirrored.Filled.TrendingDown
                            ExerciseTrend.FLAT -> Icons.AutoMirrored.Filled.TrendingFlat
                        }
                        val trendColor = when (pr.trend) {
                            ExerciseTrend.UP -> NeonGreen
                            ExerciseTrend.DOWN -> Color(0xFFEF5350)
                            ExerciseTrend.FLAT -> MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f)
                        }

                        Icon(
                            trendIcon,
                            contentDescription = null,
                            tint = trendColor,
                            modifier = Modifier.size(18.dp)
                        )

                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "${pr.bestWeight.toInt()}kg",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = NeonGreen
                        )
                    }
                }

                if (pr != prs.take(8).last()) {
                    HorizontalDivider(
                        modifier = Modifier.padding(vertical = 2.dp),
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.05f)
                    )
                }
            }
        }
    }
}

@Composable
private fun AllTimeStats(records: PersonalRecords) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp),
        colors = CardDefaults.cardColors(containerColor = SurfaceGrey),
        shape = RoundedCornerShape(20.dp)
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Text(
                text = "All Time",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = NeonGreen
            )

            Spacer(modifier = Modifier.height(16.dp))

            Row(modifier = Modifier.fillMaxWidth()) {
                AllTimeStatItem(
                    modifier = Modifier.weight(1f),
                    value = "${records.totalWorkouts}",
                    label = "Workouts",
                    color = NeonGreen
                )
                AllTimeStatItem(
                    modifier = Modifier.weight(1f),
                    value = "${(records.totalVolume / 1000).let { if (it >= 1) String.format("%.1fk", it) else records.totalVolume.toInt() }}kg",
                    label = "Total Volume",
                    color = Color(0xFF64B5F6)
                )
                AllTimeStatItem(
                    modifier = Modifier.weight(1f),
                    value = "${records.totalDurationMin / 60}h",
                    label = "Time Trained",
                    color = Color(0xFFBA68C8)
                )
                AllTimeStatItem(
                    modifier = Modifier.weight(1f),
                    value = "${records.longestSession}m",
                    label = "Longest",
                    color = Color(0xFFFF6B35)
                )
            }
        }
    }
}

@Composable
private fun AllTimeStatItem(
    modifier: Modifier = Modifier,
    value: String,
    label: String,
    color: Color
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = value,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = color
        )
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
            textAlign = TextAlign.Center
        )
    }
}

@Composable
private fun InsightsSection(insights: List<WorkoutInsight>) {
    if (insights.isEmpty()) return

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp),
        colors = CardDefaults.cardColors(containerColor = SurfaceGrey),
        shape = RoundedCornerShape(20.dp)
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Text(
                text = "Insights",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = NeonGreen
            )

            Spacer(modifier = Modifier.height(12.dp))

            insights.forEach { insight ->
                val typeColor = when (insight.type) {
                    InsightType.ACHIEVEMENT -> Color(0xFFFFD700)
                    InsightType.PROGRESS -> NeonGreen
                    InsightType.STREAK -> Color(0xFFFF6B35)
                    InsightType.ENCOURAGEMENT -> Color(0xFF64B5F6)
                }

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = insight.emoji,
                        style = MaterialTheme.typography.headlineSmall
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = insight.title,
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = typeColor
                        )
                        Text(
                            text = insight.message,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                        )
                    }
                }

                if (insight != insights.last()) {
                    HorizontalDivider(
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.05f)
                    )
                }
            }
        }
    }
}

@Composable
private fun HorizontalDivider(
    modifier: Modifier = Modifier,
    color: Color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f)
) {
    androidx.compose.material3.HorizontalDivider(
        modifier = modifier,
        color = color,
        thickness = 1.dp
    )
}
