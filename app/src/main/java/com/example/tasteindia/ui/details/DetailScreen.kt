package com.example.tasteindia.ui.details

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.tasteindia.domain.model.MealDetail
import com.example.tasteindia.ui.recipes.ErrorStateView
import com.example.tasteindia.ui.theme.*
import com.example.tasteindia.util.IngredientParser

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DetailScreen(
    viewModel: DetailViewModel,
    onBackClick: () -> Unit,
    onStartCookingClick: (String) -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current

    Scaffold(
        bottomBar = {
            if (uiState.mealDetail != null) {
                Surface(
                    shadowElevation = 8.dp,
                    color = MaterialTheme.colorScheme.surface,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Box(modifier = Modifier.padding(16.dp)) {
                        Button(
                            onClick = { onStartCookingClick(uiState.mealDetail!!.idMeal) },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(54.dp),
                            shape = RoundedCornerShape(27.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                        ) {
                            Icon(Icons.Default.RestaurantMenu, contentDescription = null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Start Cooking",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
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
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                    }
                }
                uiState.errorMessage != null -> {
                    ErrorStateView(
                        message = uiState.errorMessage!!,
                        onRetry = viewModel::loadDetail
                    )
                }
                uiState.mealDetail != null -> {
                    val detail = uiState.mealDetail!!
                    val scrollState = rememberScrollState()

                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .verticalScroll(scrollState)
                    ) {
                        // Hero Image with Overlaid Top Actions
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(280.dp)
                        ) {
                            AsyncImage(
                                model = detail.strMealThumb,
                                contentDescription = detail.strMeal,
                                contentScale = ContentScale.Crop,
                                modifier = Modifier.fillMaxSize()
                            )
                            // Top Bar Buttons
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                IconButton(
                                    onClick = onBackClick,
                                    modifier = Modifier.background(Color.Black.copy(alpha = 0.5f), CircleShape)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.ArrowBack,
                                        contentDescription = "Back",
                                        tint = Color.White
                                    )
                                }
                                IconButton(
                                    onClick = viewModel::toggleFavorite,
                                ) {
                                    val heartScale by animateFloatAsState(
                                        targetValue = if (detail.isFavorite) 1.12f else 1f,
                                        label = "detail heart scale"
                                    )
                                    Icon(
                                        imageVector = if (detail.isFavorite) Icons.Default.Favorite else Icons.Outlined.FavoriteBorder,
                                        contentDescription = "Favorite",
                                        tint = if (detail.isFavorite) FavoriteRed else Color.White,
                                        modifier = Modifier.graphicsLayer {
                                            scaleX = heartScale
                                            scaleY = heartScale
                                        }
                                    )
                                }
                            }
                        }

                        // Content Card Body
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(20.dp)
                        ) {
                            // Recipe Title
                            Text(
                                text = detail.strMeal,
                                style = MaterialTheme.typography.headlineLarge,
                                color = MaterialTheme.colorScheme.primary
                            )

                            Spacer(modifier = Modifier.height(10.dp))

                            // Badges (Category & Area)
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                detail.strCategory?.let { category ->
                                    AssistChip(
                                        onClick = { },
                                        label = { Text(category) },
                                        leadingIcon = { Icon(Icons.Default.Category, contentDescription = null, Modifier.size(16.dp)) },
                                        colors = AssistChipDefaults.assistChipColors(containerColor = SoftSaffronBackground)
                                    )
                                }
                                detail.strArea?.let { area ->
                                    AssistChip(
                                        onClick = { },
                                        label = { Text(area) },
                                        leadingIcon = { Icon(Icons.Default.Place, contentDescription = null, Modifier.size(16.dp)) }
                                    )
                                }
                            }

                            // Tags if available
                            if (detail.strTags.isNotEmpty()) {
                                Row(
                                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                                    modifier = Modifier.padding(top = 4.dp)
                                ) {
                                    detail.strTags.forEach { tag ->
                                        SuggestionChip(
                                            onClick = { },
                                            label = { Text("#$tag", style = MaterialTheme.typography.labelSmall) }
                                        )
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(20.dp))

                            // Ingredients Section
                            Text(
                                text = "Ingredients (${detail.ingredients.size})",
                                style = MaterialTheme.typography.titleLarge,
                                color = MaterialTheme.colorScheme.onSurface,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.height(10.dp))

                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(16.dp),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                                border = CardDefaults.outlinedCardBorder()
                            ) {
                                Column(modifier = Modifier.padding(16.dp)) {
                                    detail.ingredients.forEachIndexed { index, item ->
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(vertical = 6.dp),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                Icon(
                                                    imageVector = Icons.Default.CheckCircle,
                                                    contentDescription = null,
                                                    tint = SaffronAccent,
                                                    modifier = Modifier.size(18.dp)
                                                )
                                                Spacer(modifier = Modifier.width(10.dp))
                                                Text(
                                                    text = item.name,
                                                    style = MaterialTheme.typography.bodyLarge,
                                                    color = MaterialTheme.colorScheme.onSurface
                                                )
                                            }
                                            Surface(
                                                color = SoftSaffronBackground,
                                                shape = RoundedCornerShape(8.dp)
                                            ) {
                                                Text(
                                                    text = item.measure,
                                                    style = MaterialTheme.typography.labelMedium,
                                                    color = DarkCharcoal,
                                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                                )
                                            }
                                        }
                                        if (index < detail.ingredients.size - 1) {
                                            HorizontalDivider(
                                                color = CardBorder,
                                                thickness = 0.5.dp,
                                                modifier = Modifier.padding(vertical = 2.dp)
                                            )
                                        }
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(24.dp))

                            // Instructions Section
                            Text(
                                text = "Cooking Instructions",
                                style = MaterialTheme.typography.titleLarge,
                                color = MaterialTheme.colorScheme.onSurface,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.height(10.dp))

                            val parsedSteps = IngredientParser.parseInstructionsToSteps(detail.strInstructions)
                            parsedSteps.forEachIndexed { stepIdx, step ->
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 8.dp)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(28.dp)
                                            .clip(CircleShape)
                                            .background(MaterialTheme.colorScheme.primary),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = "${stepIdx + 1}",
                                            color = Color.White,
                                            style = MaterialTheme.typography.labelMedium
                                        )
                                    }
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Text(
                                        text = step,
                                        style = MaterialTheme.typography.bodyLarge,
                                        color = MaterialTheme.colorScheme.onSurface,
                                        lineHeight = 22.sp,
                                        modifier = Modifier.weight(1f)
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(24.dp))

                            // Links Section (YouTube / Source)
                            if (!detail.strYoutube.isNullOrBlank() || !detail.strSource.isNullOrBlank()) {
                                Text(
                                    text = "External Links",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                    detail.strYoutube?.let { url ->
                                        OutlinedButton(
                                            onClick = {
                                                try {
                                                    val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
                                                    context.startActivity(intent)
                                                } catch (e: Exception) { }
                                            }
                                        ) {
                                            Icon(Icons.Default.PlayArrow, contentDescription = null, tint = SoftTerracotta)
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Text("Watch Video", color = SoftTerracotta)
                                        }
                                    }
                                    detail.strSource?.let { url ->
                                        OutlinedButton(
                                            onClick = {
                                                try {
                                                    val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
                                                    context.startActivity(intent)
                                                } catch (e: Exception) { }
                                            }
                                        ) {
                                            Icon(Icons.Default.OpenInNew, contentDescription = null)
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Text("Original Source")
                                        }
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(80.dp)) // Padding for bottom bar
                        }
                    }
                }
            }
        }
    }
}
