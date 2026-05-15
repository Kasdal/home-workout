package com.example.workoutapp.ui.onboarding

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.example.workoutapp.ui.navigation.Screen

@Composable
fun OnboardingScreen(
    navController: NavController,
    viewModel: OnboardingViewModel = hiltViewModel()
) {
    val hasProfiles by viewModel.hasProfiles.collectAsState()

    if (hasProfiles) {
        LaunchedEffect(Unit) {
            navController.navigate(Screen.Profile.route) {
                popUpTo(Screen.Onboarding.route) { inclusive = true }
            }
        }
        return
    }

    var weight by remember { mutableStateOf("") }
    var height by remember { mutableStateOf("") }
    var age by remember { mutableStateOf("") }
    var gender by remember { mutableStateOf("Male") }
    val parsedWeight = weight.toFloatOrNull()
    val parsedHeight = height.toFloatOrNull()
    val parsedAge = age.toIntOrNull()
    val weightError = weight.isNotBlank() && (parsedWeight == null || parsedWeight !in 20f..350f)
    val heightError = height.isNotBlank() && (parsedHeight == null || parsedHeight !in 80f..250f)
    val ageError = age.isNotBlank() && (parsedAge == null || parsedAge !in 10..100)
    val formValid = parsedWeight != null && parsedWeight in 20f..350f &&
        parsedHeight != null && parsedHeight in 80f..250f &&
        parsedAge != null && parsedAge in 10..100

    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(20.dp),
        contentAlignment = Alignment.Center
    ) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
        ) {
            Column(
                modifier = Modifier.padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(
                        text = "Welcome to Home Workout",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "These basics help estimate workout calories and personalize progress stats.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                OutlinedTextField(
                    value = weight,
                    onValueChange = { weight = it },
                    label = { Text("Weight (kg)") },
                    placeholder = { Text("Example: 75") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    isError = weightError,
                    supportingText = { if (weightError) Text("Enter a weight from 20 to 350 kg.") },
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = height,
                    onValueChange = { height = it },
                    label = { Text("Height (cm)") },
                    placeholder = { Text("Example: 178") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    isError = heightError,
                    supportingText = { if (heightError) Text("Enter a height from 80 to 250 cm.") },
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = age,
                    onValueChange = { age = it },
                    label = { Text("Age") },
                    placeholder = { Text("Example: 32") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    isError = ageError,
                    supportingText = { if (ageError) Text("Enter an age from 10 to 100.") },
                    modifier = Modifier.fillMaxWidth()
                )

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Gender", modifier = Modifier.weight(1f))
                    RadioButton(selected = gender == "Male", onClick = { gender = "Male" })
                    Text("Male")
                    Spacer(modifier = Modifier.width(16.dp))
                    RadioButton(selected = gender == "Female", onClick = { gender = "Female" })
                    Text("Female")
                }

                Button(
                    onClick = {
                        viewModel.saveMetrics(parsedWeight!!, parsedHeight!!, parsedAge!!, gender) {
                        navController.navigate(Screen.Workout.route) {
                            popUpTo(Screen.Onboarding.route) { inclusive = true }
                        }
                    }
                    },
                    enabled = formValid,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Start Workout")
                }
            }
        }
    }
}
