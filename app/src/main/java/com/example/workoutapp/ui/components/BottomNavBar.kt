package com.example.workoutapp.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import com.example.workoutapp.ui.theme.NeonGreen

@Composable
fun BottomNavBar(
    currentRoute: String,
    onNavigate: (String) -> Unit
) {
    Surface(
        shadowElevation = 8.dp,
        tonalElevation = 3.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp, horizontal = 4.dp),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            BottomNavItem(
                icon = Icons.Default.Person,
                label = "Profile",
                route = "profiles",
                currentRoute = currentRoute,
                onClick = { onNavigate("profiles") }
            )
            
            BottomNavItem(
                icon = Icons.Default.CalendarToday,
                label = "Calendar",
                route = "history",
                currentRoute = currentRoute,
                onClick = { onNavigate("history") }
            )
            
            BottomNavItem(
                icon = Icons.Default.Lightbulb,
                label = "Insights",
                route = "insights",
                currentRoute = currentRoute,
                onClick = { onNavigate("insights") }
            )
            
            BottomNavItem(
                icon = Icons.Default.FitnessCenter,
                label = "Workouts",
                route = "workouts",
                currentRoute = currentRoute,
                onClick = { onNavigate("workouts") }
            )
            
            BottomNavItem(
                icon = Icons.Default.Settings,
                label = "Settings",
                route = "settings",
                currentRoute = currentRoute,
                onClick = { onNavigate("settings") }
            )
        }
    }
}

@Composable
fun BottomNavItem(
    icon: ImageVector,
    label: String,
    route: String,
    currentRoute: String,
    onClick: () -> Unit
) {
    val isSelected = currentRoute == route
    
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .width(70.dp)
    ) {
        IconButton(onClick = onClick) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                tint = if (isSelected) NeonGreen else MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = if (isSelected) NeonGreen else MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1
        )
    }
}
