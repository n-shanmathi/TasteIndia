package com.example.tasteindia.ui.cookmode

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.tasteindia.ui.recipes.ErrorStateView
import com.example.tasteindia.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CookModeScreen(
    viewModel: CookModeViewModel,
    onCloseClick: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()

    Scaffold(
        topBar = {
            Column {
                TopAppBar(
                    title = {
                        Text(
                            text = "COOK MODE: ${uiState.mealName}",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary,
                            maxLines = 1
                        )
                    },
                    navigationIcon = {
                        IconButton(onClick = onCloseClick) {
                            Icon(Icons.Default.Close, contentDescription = "Close Cook Mode")
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background)
                )
                LinearProgressIndicator(
                    progress = { uiState.progress },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(6.dp),
                    color = SaffronAccent,
                    trackColor = CardBorder
                )
            }
        },
        bottomBar = {
            if (!uiState.isLoading && uiState.errorMessage == null) {
                Surface(
                    shadowElevation = 12.dp,
                    color = MaterialTheme.colorScheme.surface,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        OutlinedButton(
                            onClick = viewModel::previousStep,
                            enabled = uiState.currentStepIndex > 0 || uiState.isCompleted,
                            modifier = Modifier.weight(1f).height(50.dp)
                        ) {
                            Icon(Icons.Default.ArrowBack, contentDescription = null)
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Previous")
                        }

                        Spacer(modifier = Modifier.width(16.dp))

                        Button(
                            onClick = {
                                if (uiState.isCompleted) {
                                    onCloseClick()
                                } else {
                                    viewModel.nextStep()
                                }
                            },
                            modifier = Modifier.weight(1.2f).height(50.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (uiState.isCompleted) SaffronAccent else MaterialTheme.colorScheme.primary
                            )
                        ) {
                            Text(
                                text = when {
                                    uiState.isCompleted -> "Finish"
                                    uiState.currentStepIndex == uiState.steps.size - 1 -> "Complete"
                                    else -> "Next Step"
                                },
                                fontWeight = FontWeight.Bold,
                                color = if (uiState.isCompleted) DarkCharcoal else Color.White
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Icon(
                                imageVector = if (uiState.isCompleted) Icons.Default.Check else Icons.Default.ArrowForward,
                                contentDescription = null,
                                tint = if (uiState.isCompleted) DarkCharcoal else Color.White
                            )
                        }
                    }
                }
            }
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            when {
                uiState.isLoading -> {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                    }
                }
                uiState.errorMessage != null -> {
                    ErrorStateView(
                        message = uiState.errorMessage!!,
                        onRetry = viewModel::loadCookSteps
                    )
                }
                uiState.isCompleted -> {
                    // Celebration Completion Screen
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Box(
                            modifier = Modifier
                                .size(100.dp)
                                .clip(CircleShape)
                                .background(SoftSaffronBackground),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.EmojiEvents,
                                contentDescription = null,
                                modifier = Modifier.size(60.dp),
                                tint = SaffronAccent
                            )
                        }
                        Spacer(modifier = Modifier.height(24.dp))
                        Text(
                            text = "Bon Appétit!",
                            style = MaterialTheme.typography.headlineLarge,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "You've successfully completed cooking ${uiState.mealName}.",
                            style = MaterialTheme.typography.bodyLarge,
                            textAlign = TextAlign.Center,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(32.dp))
                        OutlinedButton(onClick = viewModel::restart) {
                            Icon(Icons.Default.Replay, contentDescription = null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Restart Cooking")
                        }
                    }
                }
                else -> {
                    // Single Step Display
                    val step = uiState.currentStep
                    if (step != null) {
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(24.dp),
                            verticalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column {
                                // Step Counter Badge
                                Surface(
                                    color = SoftSaffronBackground,
                                    shape = RoundedCornerShape(16.dp),
                                    modifier = Modifier.padding(bottom = 16.dp)
                                ) {
                                    Text(
                                        text = "STEP ${step.stepNumber} OF ${step.totalSteps}",
                                        style = MaterialTheme.typography.labelLarge,
                                        color = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp)
                                    )
                                }

                                // Recipe Thumbnail (if available)
                                uiState.thumbnailUrl?.let { thumb ->
                                    Card(
                                        shape = RoundedCornerShape(16.dp),
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(160.dp)
                                            .padding(bottom = 20.dp)
                                    ) {
                                        AsyncImage(
                                            model = thumb,
                                            contentDescription = null,
                                            contentScale = ContentScale.Crop,
                                            modifier = Modifier.fillMaxSize()
                                        )
                                    }
                                }

                                // Large Step Instruction Text (Optimized for 1-hand reading)
                                Text(
                                    text = step.instructionText,
                                    style = MaterialTheme.typography.headlineMedium.copy(
                                        fontSize = 22.sp,
                                        lineHeight = 32.sp
                                    ),
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
