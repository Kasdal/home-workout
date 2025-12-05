package com.example.workoutapp.ui.profile

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.HelpOutline
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.example.workoutapp.data.local.entity.UserMetrics
import com.example.workoutapp.ui.components.BottomNavBar

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreen(
    navController: NavController,
    viewModel: ProfileViewModel = hiltViewModel()
) {
    val profiles by viewModel.allProfiles.collectAsState(initial = emptyList())
    val currentProfile by viewModel.currentProfile.collectAsState()
    var showAddDialog by remember { mutableStateOf(false) }
    var editingProfile by remember { mutableStateOf<UserMetrics?>(null) }

    if (showAddDialog || editingProfile != null) {
        ProfileEditDialog(
            profile = editingProfile,
            onDismiss = {
                showAddDialog = false
                editingProfile = null
            },
            onSave = { name, weight, height, age, gender ->
                if (editingProfile != null) {
                    viewModel.updateProfile(editingProfile!!.copy(
                        name = name,
                        weightKg = weight,
                        heightCm = height,
                        age = age,
                        gender = gender
                    ))
                } else {
                    viewModel.addProfile(name, weight, height, age, gender)
                }
                showAddDialog = false
                editingProfile = null
            }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Profiles") }
            )
        },
        bottomBar = {
            BottomNavBar(
                currentRoute = "profiles",
                onNavigate = { route -> navController.navigate(route) }
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = { showAddDialog = true }) {
                Icon(Icons.Default.Add, contentDescription = "Add Profile")
            }
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Onboarding Button
            item {
                Button(
                    onClick = { navController.navigate("tutorial") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 8.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer
                    )
                ) {
                    Icon(
                        imageVector = Icons.Default.HelpOutline,
                        contentDescription = "Onboarding",
                        modifier = Modifier.padding(end = 8.dp)
                    )
                    Text("Onboarding Tutorial")
                }
            }
            
            items(profiles) { profile ->
                ProfileCard(
                    profile = profile,
                    isActive = profile.id == currentProfile?.id,
                    onSelect = { viewModel.setActiveProfile(profile.id) },
                    onEdit = { editingProfile = profile },
                    onDelete = { viewModel.deleteProfile(profile.id) }
                )
            }
        }
    }
}

@Composable
fun ProfileCard(
    profile: UserMetrics,
    isActive: Boolean,
    onSelect: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = if (isActive) MaterialTheme.colorScheme.primaryContainer
            else MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = profile.name ?: "Profile ${profile.id}",
                    style = MaterialTheme.typography.titleMedium
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "${profile.weightKg}kg • ${profile.heightCm}cm • ${profile.age}y • ${profile.gender}",
                    style = MaterialTheme.typography.bodyMedium
                )
            }
            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                if (!isActive) {
                    IconButton(onClick = onSelect) {
                        Icon(Icons.Default.Check, contentDescription = "Select")
                    }
                }
                TextButton(onClick = onEdit) {
                    Text("Edit")
                }
                if (!isActive) {
                    TextButton(onClick = onDelete) {
                        Text("Delete")
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileEditDialog(
    profile: UserMetrics?,
    onDismiss: () -> Unit,
    onSave: (String, Float, Float, Int, String) -> Unit
) {
    var name by remember { mutableStateOf(profile?.name ?: "") }
    var weight by remember { mutableStateOf(profile?.weightKg?.toString() ?: "") }
    var height by remember { mutableStateOf(profile?.heightCm?.toString() ?: "") }
    var age by remember { mutableStateOf(profile?.age?.toString() ?: "") }
    var gender by remember { mutableStateOf(profile?.gender ?: "Male") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (profile == null) "Add Profile" else "Edit Profile") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Name") },
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = weight,
                    onValueChange = { weight = it },
                    label = { Text("Weight (kg)") },
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = height,
                    onValueChange = { height = it },
                    label = { Text("Height (cm)") },
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = age,
                    onValueChange = { age = it },
                    label = { Text("Age") },
                    modifier = Modifier.fillMaxWidth()
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilterChip(
                        selected = gender == "Male",
                        onClick = { gender = "Male" },
                        label = { Text("Male") }
                    )
                    FilterChip(
                        selected = gender == "Female",
                        onClick = { gender = "Female" },
                        label = { Text("Female") }
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val w = weight.toFloatOrNull() ?: 70f
                    val h = height.toFloatOrNull() ?: 170f
                    val a = age.toIntOrNull() ?: 25
                    onSave(name.ifBlank { "Profile" }, w, h, a, gender)
                }
            ) {
                Text("Save")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}
