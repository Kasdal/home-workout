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
import kotlinx.coroutines.launch
import androidx.compose.animation.core.*
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.runtime.getValue

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun TutorialScreen(
    navController: NavController,
    onComplete: () -> Unit = {}
) {
    val pages = listOf(
        TutorialPage(
            title = "Welcome! 💪",
            description = "Track your workouts with ease and watch your progress grow!",
            emoji = "🏋️"
        ),
        TutorialPage(
            title = "Bottom Navigation",
            description = "Use the bottom navigation bar to quickly access Profile, Calendar, Insights, Workouts, and Settings.",
            emoji = "🧭"
        ),
        TutorialPage(
            title = "Flexible Tracking",
            description = "Customize reps and sets for each exercise. Use +5/-5kg buttons for quick weight adjustments.",
            emoji = "⚡"
        ),
        TutorialPage(
            title = "Session Management",
            description = "Start your workout session to enter Focus Mode. Complete sets and track your progress in real-time.",
            emoji = "🎯"
        ),
        TutorialPage(
            title = "Tap to Undo",
            description = "Made a mistake? Just tap any completed checkmark to undo that set.",
            emoji = "↩️"
        ),
        TutorialPage(
            title = "Calendar & Records",
            description = "View your Personal Records and Progress Report in the Calendar screen. Track your best lifts!",
            emoji = "📅"
        ),
        TutorialPage(
            title = "Insights & Trends",
            description = "Get motivational insights and visualize your progress with detailed charts showing volume and frequency trends.",
            emoji = "📊"
        ),
        TutorialPage(
            title = "Settings & Export",
            description = "Customize sounds, themes, and export your data anytime via the Settings screen.",
            emoji = "⚙️"
        ),
        TutorialPage(
            title = "You're Ready!",
            description = "Start your fitness journey now. Push your limits!",
            emoji = "🚀"
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
                            navController.navigateUp()
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
        // Animated illustration based on page
        AnimatedIllustration(page.emoji)
        
        Spacer(modifier = Modifier.height(32.dp))
        Text(
            text = page.title,
            style = MaterialTheme.typography.headlineLarge,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(16.dp))
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
    val emoji: String
)
