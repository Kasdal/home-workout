package com.example.workoutapp.ui.workouts

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.filled.Accessibility
import androidx.compose.material.icons.filled.BackHand
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.Category
import androidx.compose.material.icons.filled.DirectionsBike
import androidx.compose.material.icons.filled.DirectionsRun
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.filled.Pool
import androidx.compose.material.icons.filled.SelfImprovement
import androidx.compose.material.icons.filled.SportsKabaddi
import androidx.compose.material.icons.filled.Stars
import androidx.compose.material.icons.filled.TouchApp
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import com.example.workoutapp.model.Category

object CategoryIcons {
    val all: List<Pair<String, ImageVector>> = listOf(
        "FitnessCenter" to Icons.Default.FitnessCenter,
        "DirectionsRun" to Icons.Default.DirectionsRun,
        "SelfImprovement" to Icons.Default.SelfImprovement,
        "FavoriteBorder" to Icons.Default.FavoriteBorder,
        "LocalFireDepartment" to Icons.Default.LocalFireDepartment,
        "Bolt" to Icons.Default.Bolt,
        "BackHand" to Icons.Default.BackHand,
        "AccessTime" to Icons.Default.AccessTime,
        "Stars" to Icons.Default.Stars,
        "SportsKabaddi" to Icons.Default.SportsKabaddi,
        "DirectionsBike" to Icons.Default.DirectionsBike,
        "Pool" to Icons.Default.Pool,
        "Accessibility" to Icons.Default.Accessibility,
        "TouchApp" to Icons.Default.TouchApp,
        "History" to Icons.Default.History,
        "Category" to Icons.Default.Category
    )

    fun iconForName(name: String): ImageVector =
        all.firstOrNull { it.first == name }?.second ?: Icons.Default.Category
}

@Composable
fun CategoryIconPicker(
    selected: String,
    onSelect: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    LazyVerticalGrid(
        columns = GridCells.Fixed(4),
        modifier = modifier,
        contentPadding = PaddingValues(8.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        items(CategoryIcons.all, key = { it.first }) { (name, icon) ->
            val isSelected = name == selected
            Box(
                modifier = Modifier
                    .size(56.dp)
                    .background(
                        color = if (isSelected) MaterialTheme.colorScheme.primaryContainer
                        else MaterialTheme.colorScheme.surfaceVariant,
                        shape = CircleShape
                    )
                    .border(
                        width = if (isSelected) 2.dp else 0.dp,
                        color = MaterialTheme.colorScheme.primary,
                        shape = CircleShape
                    )
                    .clickable { onSelect(name) },
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = name,
                    tint = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer
                    else MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}
