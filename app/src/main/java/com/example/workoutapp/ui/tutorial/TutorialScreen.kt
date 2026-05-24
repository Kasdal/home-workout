package com.example.workoutapp.ui.tutorial

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.example.workoutapp.ui.theme.NeonGreen
import com.example.workoutapp.ui.navigation.Screen
import kotlinx.coroutines.launch
import androidx.compose.animation.core.*
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.runtime.getValue

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun TutorialScreen(
    navController: NavController,
    onComplete: () -> Unit = {},
    fromOnboarding: Boolean = false
) {
    val pages = listOf(
        TutorialPage(
            title = "Workout Library",
            description = "Create exercises and manage your workout library with custom sets, reps, and weights.",
            imageRes = com.example.workoutapp.R.drawable.tutorial_page_1
        ),
        TutorialPage(
            title = "Exercise Wizard",
            description = "Add exercises with sets, reps, weights, hold timers, or ESP sensor support.",
            imageRes = com.example.workoutapp.R.drawable.tutorial_page_2
        ),
        TutorialPage(
            title = "Active Session",
            description = "Start a session and work through your exercises one by one.",
            imageRes = com.example.workoutapp.R.drawable.tutorial_page_3
        ),
        TutorialPage(
            title = "Hold to Complete",
            description = "Hold the exercise card to mark a set as complete during your session.",
            imageRes = com.example.workoutapp.R.drawable.tutorial_page_4
        ),
        TutorialPage(
            title = "Rest Timer",
            description = "Auto-countdown between sets with configurable rest periods.",
            imageRes = com.example.workoutapp.R.drawable.tutorial_page_5
        ),
        TutorialPage(
            title = "Weight Adjustment",
            description = "Adjust weight in 5kg increments during your session for quick changes.",
            imageRes = com.example.workoutapp.R.drawable.tutorial_page_6
        ),
        TutorialPage(
            title = "History Dashboard",
            description = "Track your progress with volume charts, personal records, and workout streaks.",
            imageRes = com.example.workoutapp.R.drawable.tutorial_page_7
        ),
        TutorialPage(
            title = "Rest Day Calendar",
            description = "Mark rest days and see your weekly and monthly consistency stats.",
            imageRes = com.example.workoutapp.R.drawable.tutorial_page_8
        ),
        TutorialPage(
            title = "Settings & Customization",
            description = "Customize sounds, timers, theme mode, and ESP sensor support.",
            imageRes = com.example.workoutapp.R.drawable.tutorial_page_9
        )
    )

    val pagerState = rememberPagerState(pageCount = { pages.size })
    val scope = rememberCoroutineScope()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("How to Use") },
                navigationIcon = {
                    IconButton(onClick = { navController.navigateUp() }) {
                        Icon(Icons.Default.ArrowBack, "Back")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            // Pager
            HorizontalPager(
                state = pagerState,
                modifier = Modifier.weight(1f)
            ) { page ->
                TutorialPageContent(pages[page])
            }

            // Page indicator
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalArrangement = Arrangement.Center
            ) {
                repeat(pages.size) { index ->
                    Box(
                        modifier = Modifier
                            .size(if (index == pagerState.currentPage) 12.dp else 8.dp)
                            .padding(4.dp)
                            .background(
                                color = if (index == pagerState.currentPage) NeonGreen else Color.Gray,
                                shape = MaterialTheme.shapes.small
                            )
                    )
                }
            }

            // Navigation buttons
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                // Previous button
                if (pagerState.currentPage > 0) {
                    OutlinedButton(
                        onClick = {
                            scope.launch {
                                pagerState.animateScrollToPage(pagerState.currentPage - 1)
                            }
                        }
                    ) {
                        Icon(Icons.Default.ArrowBack, null)
                        Spacer(Modifier.width(4.dp))
                        Text("Previous")
                    }
                } else {
                    Spacer(Modifier.width(1.dp))
                }

                // Next/Done button
                Button(
                    onClick = {
                        if (pagerState.currentPage < pages.size - 1) {
                            scope.launch {
                                pagerState.animateScrollToPage(pagerState.currentPage + 1)
                            }
                        } else {
                            onComplete()
                            if (fromOnboarding) {
                                navController.navigate(Screen.Workout.route) {
                                    popUpTo(0) { inclusive = true }
                                }
                            } else {
                                navController.navigateUp()
                            }
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = NeonGreen)
                ) {
                    Text(
                        text = if (pagerState.currentPage < pages.size - 1) "Next" else "Get Started",
                        color = Color.Black,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(Modifier.width(4.dp))
                    Icon(
                        imageVector = if (pagerState.currentPage < pages.size - 1) 
                            Icons.Default.ArrowForward else Icons.Default.Check,
                        contentDescription = null,
                        tint = Color.Black
                    )
                }
            }
        }
    }
}

@Composable
private fun TutorialPageContent(page: TutorialPage) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        if (page.imageRes != null) {
            androidx.compose.foundation.Image(
                painter = androidx.compose.ui.res.painterResource(id = page.imageRes),
                contentDescription = page.title,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(320.dp),
                contentScale = androidx.compose.ui.layout.ContentScale.Fit
            )
        } else {
            AnimatedIllustration(page.emoji)
        }
        
        Spacer(modifier = Modifier.height(24.dp))
        Text(
            text = page.title,
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(12.dp))
        Text(
            text = page.description,
            style = MaterialTheme.typography.bodyLarge,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun AnimatedIllustration(emoji: String) {
    val infiniteTransition = rememberInfiniteTransition(label = "illustration")
    
    // Smooth, subtle pulsing animation for all emojis
    val scale by infiniteTransition.animateFloat(
        initialValue = 0.98f,
        targetValue = 1.02f,
        animationSpec = infiniteRepeatable(
            animation = tween(2000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse"
    )
    
    Box(
        modifier = Modifier
            .size(220.dp)
            .padding(vertical = 40.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = emoji,
            style = MaterialTheme.typography.displayLarge,
            fontSize = MaterialTheme.typography.displayLarge.fontSize * 1.8f,
            modifier = Modifier.graphicsLayer { 
                scaleX = scale
                scaleY = scale
            }
        )
    }
}

data class TutorialPage(
    val title: String,
    val description: String,
    val emoji: String = "",
    val imageRes: Int? = null
)
