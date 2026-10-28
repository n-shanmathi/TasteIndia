package com.example.tasteindia.ui.recipes

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.tasteindia.domain.model.MealSummary
import com.example.tasteindia.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RecipesScreen(
    viewModel: RecipesViewModel,
    onRecipeClick: (String) -> Unit,
    onSignOut: () -> Unit = {}
) {
    val uiState by viewModel.uiState.collectAsState()
    var showFilterBottomSheet by remember { mutableStateOf(false) }
    var showSignOutDialog by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            HeaderSection(
                onFilterClick = { showFilterBottomSheet = true },
                activeFilterCount = uiState.appliedFilterCount,
                onSignOutClick = { showSignOutDialog = true }
            )
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            // Search Bar & Filter Button
            SearchBarSection(
                query = uiState.searchQuery,
                onQueryChange = viewModel::onSearchQueryChange,
                onFilterClick = { showFilterBottomSheet = true },
                hasActiveFilters = uiState.hasActiveFilters
            )

            // Category Horizontal Scrollable Chips
            CategoryChipsSection(
                categories = uiState.categories,
                selectedCategory = uiState.selectedCategory,
                onCategorySelect = viewModel::onCategorySelect,
                isLoading = uiState.isCategoriesLoading
            )

            // Active Filters Row
            if (uiState.hasActiveFilters) {
                ActiveFiltersRow(
                    selectedCategory = uiState.selectedCategory,
                    selectedIngredient = uiState.selectedIngredient,
                    showFavoritesOnly = uiState.showFavoritesOnly,
                    searchQuery = uiState.searchQuery,
                    sortOrder = uiState.sortOrder,
                    onClearCategory = { viewModel.onCategorySelect(null) },
                    onClearIngredient = { viewModel.onIngredientSelect(null) },
                    onClearFavoritesOnly = { viewModel.toggleFavoritesOnlyFilter() },
                    onClearSearch = { viewModel.onSearchQueryChange("") },
                    onClearSort = { viewModel.onSortOrderChange(SortOrder.NONE) },
                    onClearAll = viewModel::clearAllFilters
                )
            }

            // Results Count Indicator
            if (!uiState.isLoading && uiState.errorMessage == null) {
                Text(
                    text = "${uiState.meals.size} Indian Recipes Found",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                )
            }

            // Main Content Area (Grid / Skeletons / Error / Empty)
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .weight(1f)
            ) {
                when {
                    uiState.isLoading -> {
                        RecipeSkeletonGrid()
                    }
                    uiState.errorMessage != null -> {
                        ErrorStateView(
                            message = uiState.errorMessage!!,
                            onRetry = viewModel::loadInitialData
                        )
                    }
                    uiState.meals.isEmpty() -> {
                        EmptyStateView(
                            searchQuery = uiState.searchQuery,
                            onClearFilters = viewModel::clearAllFilters
                        )
                    }
                    else -> {
                        LazyVerticalGrid(
                            columns = GridCells.Fixed(2),
                            contentPadding = PaddingValues(16.dp),
                            horizontalArrangement = Arrangement.spacedBy(14.dp),
                            verticalArrangement = Arrangement.spacedBy(14.dp),
                            modifier = Modifier.fillMaxSize()
                        ) {
                            // Optional Featured Recipe Hero section
                            uiState.featuredMeal?.let { featured ->
                                if (!uiState.hasActiveFilters) {
                                    item(span = { GridItemSpan(2) }) {
                                        FeaturedRecipeCard(
                                            meal = featured,
                                            onRecipeClick = onRecipeClick,
                                            onFavoriteClick = { viewModel.toggleFavorite(featured) }
                                        )
                                    }
                                }
                            }

                            items(
                                items = uiState.meals,
                                key = { it.idMeal }
                            ) { meal ->
                                RecipeGridCard(
                                    meal = meal,
                                    onRecipeClick = onRecipeClick,
                                    onFavoriteClick = { viewModel.toggleFavorite(meal) }
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    // Filter & Sort Bottom Sheet
    if (showFilterBottomSheet) {
        FilterBottomSheet(
            categories = uiState.categories,
            ingredients = uiState.ingredients,
            selectedCategory = uiState.selectedCategory,
            selectedIngredient = uiState.selectedIngredient,
            showFavoritesOnly = uiState.showFavoritesOnly,
            sortOrder = uiState.sortOrder,
            onCategorySelect = viewModel::onCategorySelect,
            onIngredientSelect = viewModel::onIngredientSelect,
            onToggleFavoritesOnly = viewModel::toggleFavoritesOnlyFilter,
            onSortOrderSelect = viewModel::onSortOrderChange,
            onClearAll = viewModel::clearAllFilters,
            onDismiss = { showFilterBottomSheet = false }
        )
    }

    if (showSignOutDialog) {
        AlertDialog(
            onDismissRequest = { showSignOutDialog = false },
            title = { Text("Sign out?") },
            text = { Text("You can sign in again to access your saved recipes.") },
            confirmButton = {
                TextButton(onClick = {
                    showSignOutDialog = false
                    onSignOut()
                }) { Text("Sign out") }
            },
            dismissButton = {
                TextButton(onClick = { showSignOutDialog = false }) { Text("Cancel") }
            }
        )
    }
}

@Composable
fun HeaderSection(
    onFilterClick: () -> Unit,
    activeFilterCount: Int,
    onSignOutClick: () -> Unit = {}
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column {
            Text(
                text = "TasteIndia",
                style = MaterialTheme.typography.headlineLarge,
                color = MaterialTheme.colorScheme.primary
            )
            Text(
                text = "Discover authentic flavors of India",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Row {
            BadgedBox(
                badge = {
                    if (activeFilterCount > 0) {
                        Badge(
                            containerColor = SaffronAccent,
                            contentColor = DarkCharcoal
                        ) {
                            Text("$activeFilterCount")
                        }
                    }
                }
            ) {
                IconButton(
                    onClick = onFilterClick,
                    colors = IconButtonDefaults.iconButtonColors(
                        containerColor = MaterialTheme.colorScheme.secondaryContainer
                    )
                ) {
                    Icon(
                        imageVector = Icons.Default.FilterList,
                        contentDescription = "Filter Recipes",
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
            }
            IconButton(onClick = onSignOutClick) {
                Icon(
                    imageVector = Icons.Default.AccountCircle,
                    contentDescription = "Sign out",
                    tint = MaterialTheme.colorScheme.primary
                )
            }
        }
    }
}

@Composable
fun SearchBarSection(
    query: String,
    onQueryChange: (String) -> Unit,
    onFilterClick: () -> Unit,
    hasActiveFilters: Boolean
) {
    OutlinedTextField(
        value = query,
        onValueChange = onQueryChange,
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp),
        placeholder = { Text("Search Indian dishes (e.g. Butter Chicken)...") },
        leadingIcon = {
            Icon(
                imageVector = Icons.Default.Search,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        },
        trailingIcon = {
            if (query.isNotEmpty()) {
                IconButton(onClick = { onQueryChange("") }) {
                    Icon(
                        imageVector = Icons.Default.Clear,
                        contentDescription = "Clear Search"
                    )
                }
            }
        },
        singleLine = true,
        shape = RoundedCornerShape(24.dp),
        colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = MaterialTheme.colorScheme.primary,
            unfocusedBorderColor = MaterialTheme.colorScheme.outline,
            focusedContainerColor = MaterialTheme.colorScheme.surface,
            unfocusedContainerColor = MaterialTheme.colorScheme.surface
        )
    )
}

@Composable
fun CategoryChipsSection(
    categories: List<String>,
    selectedCategory: String?,
    onCategorySelect: (String?) -> Unit,
    isLoading: Boolean
) {
    if (isLoading && categories.isEmpty()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            repeat(4) {
                Box(
                    modifier = Modifier
                        .width(90.dp)
                        .height(36.dp)
                        .clip(RoundedCornerShape(18.dp))
                        .background(CardBorder)
                )
            }
        }
    } else {
        LazyRow(
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            item {
                FilterChip(
                    selected = selectedCategory == null,
                    onClick = { onCategorySelect(null) },
                    label = { Text("All Dishes") },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = MaterialTheme.colorScheme.primary,
                        selectedLabelColor = MaterialTheme.colorScheme.onPrimary
                    ),
                    border = FilterChipDefaults.filterChipBorder(
                        enabled = true,
                        selected = selectedCategory == null,
                        borderColor = CardBorder
                    )
                )
            }
            items(categories) { category ->
                FilterChip(
                    selected = selectedCategory == category,
                    onClick = { onCategorySelect(category) },
                    label = { Text(category) },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = MaterialTheme.colorScheme.primary,
                        selectedLabelColor = MaterialTheme.colorScheme.onPrimary
                    ),
                    border = FilterChipDefaults.filterChipBorder(
                        enabled = true,
                        selected = selectedCategory == category,
                        borderColor = CardBorder
                    )
                )
            }
        }
    }
}

@Composable
fun ActiveFiltersRow(
    selectedCategory: String?,
    selectedIngredient: String?,
    showFavoritesOnly: Boolean,
    searchQuery: String,
    sortOrder: SortOrder,
    onClearCategory: () -> Unit,
    onClearIngredient: () -> Unit,
    onClearFavoritesOnly: () -> Unit,
    onClearSearch: () -> Unit,
    onClearSort: () -> Unit,
    onClearAll: () -> Unit
) {
    LazyRow(
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (showFavoritesOnly) {
            item {
                InputChip(
                    selected = true,
                    onClick = onClearFavoritesOnly,
                    label = { Text("Favorites Only") },
                    trailingIcon = { Icon(Icons.Default.Close, contentDescription = null, Modifier.size(16.dp)) }
                )
            }
        }
        if (!selectedCategory.isNullOrBlank()) {
            item {
                InputChip(
                    selected = true,
                    onClick = onClearCategory,
                    label = { Text("Category: $selectedCategory") },
                    trailingIcon = { Icon(Icons.Default.Close, contentDescription = null, Modifier.size(16.dp)) }
                )
            }
        }
        if (!selectedIngredient.isNullOrBlank()) {
            item {
                InputChip(
                    selected = true,
                    onClick = onClearIngredient,
                    label = { Text("Ingredient: $selectedIngredient") },
                    trailingIcon = { Icon(Icons.Default.Close, contentDescription = null, Modifier.size(16.dp)) }
                )
            }
        }
        if (searchQuery.isNotBlank()) {
            item {
                InputChip(
                    selected = true,
                    onClick = onClearSearch,
                    label = { Text("Search: $searchQuery") },
                    trailingIcon = { Icon(Icons.Default.Close, contentDescription = null, Modifier.size(16.dp)) }
                )
            }
        }
        if (sortOrder != SortOrder.NONE) {
            item {
                InputChip(
                    selected = true,
                    onClick = onClearSort,
                    label = { Text(if (sortOrder == SortOrder.A_TO_Z) "Sort: A-Z" else "Sort: Z-A") },
                    trailingIcon = { Icon(Icons.Default.Close, contentDescription = null, Modifier.size(16.dp)) }
                )
            }
        }
        item {
            TextButton(onClick = onClearAll) {
                Text("Clear All", color = SoftTerracotta, style = MaterialTheme.typography.labelMedium)
            }
        }
    }
}

@Composable
fun FeaturedRecipeCard(
    meal: MealSummary,
    onRecipeClick: (String) -> Unit,
    onFavoriteClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .height(200.dp)
            .clickable { onRecipeClick(meal.idMeal) },
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            AsyncImage(
                model = meal.strMealThumb,
                contentDescription = meal.strMeal,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(Color.Transparent, Color.Black.copy(alpha = 0.8f)),
                            startY = 100f
                        )
                    )
            )
            Column(
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .padding(16.dp)
            ) {
                Surface(
                    color = SaffronAccent,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.padding(bottom = 6.dp)
                ) {
                    Text(
                        text = "⭐ Featured Recipe",
                        style = MaterialTheme.typography.labelSmall,
                        color = DarkCharcoal,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
                Text(
                    text = meal.strMeal,
                    style = MaterialTheme.typography.titleLarge,
                    color = Color.White,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }
            IconButton(
                onClick = onFavoriteClick,
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(8.dp)
            ) {
                val heartScale by animateFloatAsState(
                    targetValue = if (meal.isFavorite) 1.12f else 1f,
                    label = "featured heart scale"
                )
                Icon(
                    imageVector = if (meal.isFavorite) Icons.Default.Favorite else Icons.Outlined.FavoriteBorder,
                    contentDescription = "Favorite",
                    tint = if (meal.isFavorite) FavoriteRed else Color.White,
                    modifier = Modifier.graphicsLayer {
                        scaleX = heartScale
                        scaleY = heartScale
                    }
                )
            }
        }
    }
}

@Composable
fun RecipeGridCard(
    meal: MealSummary,
    onRecipeClick: (String) -> Unit,
    onFavoriteClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onRecipeClick(meal.idMeal) },
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(130.dp)
            ) {
                AsyncImage(
                    model = meal.strMealThumb,
                    contentDescription = meal.strMeal,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
                IconButton(
                    onClick = onFavoriteClick,
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(6.dp)
                        .size(32.dp)
                ) {
                    val heartScale by animateFloatAsState(
                        targetValue = if (meal.isFavorite) 1.15f else 1f,
                        label = "recipe heart scale"
                    )
                    Icon(
                        imageVector = if (meal.isFavorite) Icons.Default.Favorite else Icons.Outlined.FavoriteBorder,
                        contentDescription = "Favorite",
                        tint = if (meal.isFavorite) FavoriteRed else Color.White,
                        modifier = Modifier
                            .size(18.dp)
                            .graphicsLayer {
                                scaleX = heartScale
                                scaleY = heartScale
                            }
                    )
                }
            }
            Column(
                modifier = Modifier.padding(12.dp)
            ) {
                Text(
                    text = meal.strMeal,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

@Composable
fun RecipeSkeletonGrid() {
    LazyVerticalGrid(
        columns = GridCells.Fixed(2),
        contentPadding = PaddingValues(16.dp),
        horizontalArrangement = Arrangement.spacedBy(14.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
        modifier = Modifier.fillMaxSize()
    ) {
        items(6) {
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = CardBorder),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(180.dp)
            ) {}
        }
    }
}

@Composable
fun ErrorStateView(
    message: String,
    onRetry: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = Icons.Default.CloudOff,
            contentDescription = null,
            modifier = Modifier.size(64.dp),
            tint = SoftTerracotta
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = "Unable to load recipes",
            style = MaterialTheme.typography.titleLarge,
            color = MaterialTheme.colorScheme.onSurface
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = message,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.height(20.dp))
        Button(
            onClick = onRetry,
            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
        ) {
            Icon(Icons.Default.Refresh, contentDescription = null)
            Spacer(modifier = Modifier.width(8.dp))
            Text("Retry")
        }
    }
}

@Composable
fun EmptyStateView(
    searchQuery: String,
    onClearFilters: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = Icons.Default.SearchOff,
            contentDescription = null,
            modifier = Modifier.size(64.dp),
            tint = SaffronAccent
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = "No Indian Recipes Found",
            style = MaterialTheme.typography.titleLarge,
            color = MaterialTheme.colorScheme.onSurface
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = if (searchQuery.isNotEmpty()) "No dishes matched '$searchQuery'" else "Try clearing filters to see more dishes.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.height(20.dp))
        OutlinedButton(onClick = onClearFilters) {
            Text("Clear All Filters")
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FilterBottomSheet(
    categories: List<String>,
    ingredients: List<String>,
    selectedCategory: String?,
    selectedIngredient: String?,
    showFavoritesOnly: Boolean,
    sortOrder: SortOrder,
    onCategorySelect: (String?) -> Unit,
    onIngredientSelect: (String?) -> Unit,
    onToggleFavoritesOnly: () -> Unit,
    onSortOrderSelect: (SortOrder) -> Unit,
    onClearAll: () -> Unit,
    onDismiss: () -> Unit
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Filter & Sort", style = MaterialTheme.typography.headlineMedium, color = MaterialTheme.colorScheme.primary)
                TextButton(onClick = { onClearAll() }) {
                    Text("Clear All", color = SoftTerracotta)
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Quick Filters (Favorites Only)
            Text("Quick Filters", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(8.dp))
            FilterChip(
                selected = showFavoritesOnly,
                onClick = onToggleFavoritesOnly,
                label = { Text("❤️ Favourites Only") }
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Sort Order
            Text("Sort Order", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FilterChip(
                    selected = sortOrder == SortOrder.A_TO_Z,
                    onClick = { onSortOrderSelect(if (sortOrder == SortOrder.A_TO_Z) SortOrder.NONE else SortOrder.A_TO_Z) },
                    label = { Text("A to Z") }
                )
                FilterChip(
                    selected = sortOrder == SortOrder.Z_TO_A,
                    onClick = { onSortOrderSelect(if (sortOrder == SortOrder.Z_TO_A) SortOrder.NONE else SortOrder.Z_TO_A) },
                    label = { Text("Z to A") }
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Categories
            Text("Category", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(8.dp))
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(categories) { category ->
                    FilterChip(
                        selected = selectedCategory == category,
                        onClick = { onCategorySelect(category) },
                        label = { Text(category) }
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Ingredients
            Text("Main Ingredient", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(8.dp))
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(ingredients.take(20)) { ingredient ->
                    FilterChip(
                        selected = selectedIngredient == ingredient,
                        onClick = { onIngredientSelect(ingredient) },
                        label = { Text(ingredient) }
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            Button(
                onClick = onDismiss,
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
            ) {
                Text("Apply & Close")
            }
        }
    }
}
