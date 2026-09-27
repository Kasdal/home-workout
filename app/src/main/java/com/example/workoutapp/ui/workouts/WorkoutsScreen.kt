package com.example.workoutapp.ui.workouts

import android.content.Intent
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.draggable
import androidx.compose.foundation.gestures.rememberDraggableState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Label
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.toMutableStateList
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.ColorMatrix
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import coil.compose.rememberAsyncImagePainter
import com.example.workoutapp.R
import com.example.workoutapp.data.storage.PhotoUploadResult
import com.example.workoutapp.model.Category
import com.example.workoutapp.model.Exercise
import com.example.workoutapp.model.ExerciseType
import com.example.workoutapp.model.SessionExercise
import com.example.workoutapp.model.WorkoutTemplate
import com.example.workoutapp.ui.components.BottomNavBar
import com.example.workoutapp.ui.workout.ExerciseEditDialog
import com.example.workoutapp.util.formatKg
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WorkoutsScreen(
    navController: NavController,
    viewModel: com.example.workoutapp.ui.workout.WorkoutViewModel = hiltViewModel()
) {
    val exercises by viewModel.exercises.collectAsStateWithLifecycle(initialValue = emptyList())
    val categories by viewModel.categories.collectAsStateWithLifecycle(initialValue = emptyList())
    val templates by viewModel.templates.collectAsStateWithLifecycle(initialValue = emptyList())
    val sessionDates by viewModel.sessions.collectAsStateWithLifecycle(initialValue = emptyList())
    val snackbarHostState = remember { SnackbarHostState() }
    val snackbarScope = rememberCoroutineScope()
    val successMessage = stringResource(R.string.photo_upload_success)
    val sourceUnreadableMessage = stringResource(R.string.photo_upload_source_unreadable)
    val uploadFailedMessage = stringResource(R.string.photo_upload_failed)

    LaunchedEffect(viewModel) {
        viewModel.photoUploadEvents.collect { result ->
            val message = when (result) {
                is PhotoUploadResult.Success -> successMessage
                PhotoUploadResult.SourceUnreadable -> sourceUnreadableMessage
                is PhotoUploadResult.UploadFailed -> uploadFailedMessage
            }
            snackbarScope.launch {
                snackbarHostState.showSnackbar(message)
            }
        }
    }

    LaunchedEffect(viewModel) {
        viewModel.syncErrorEvents.collect { event ->
            snackbarScope.launch {
                val result = snackbarHostState.showSnackbar(
                    message = "Sync failed. Changes were not saved.",
                    actionLabel = if (event.canRetry) "Retry" else null,
                    duration = SnackbarDuration.Long
                )
                if (result == SnackbarResult.ActionPerformed && event.canRetry) {
                    viewModel.retryPendingWrites()
                }
            }
        }
    }

    WorkoutsScreenContent(
        exercises = exercises,
        categories = categories,
        templates = templates,
        sessionDates = sessionDates.associate { it.id to it.date },
        snackbarHostState = snackbarHostState,
        onNavigateToRoute = navController::navigate,
        onAddExercise = viewModel::addExercise,
        onUpdateExercise = viewModel::updateExercise,
        onDeleteExercise = viewModel::deleteExercise,
        onUpdateExercisePhoto = viewModel::updateExercisePhoto,
        getExerciseHistory = viewModel::getExerciseHistory,
        onReorderExercises = viewModel::updateExerciseOrder,
        onSetExerciseCategory = viewModel::setExerciseCategory,
        onSaveTemplate = viewModel::saveTemplate,
        onDeleteTemplate = viewModel::deleteTemplate
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WorkoutsScreenContent(
    exercises: List<Exercise>,
    categories: List<com.example.workoutapp.model.Category>,
    templates: List<WorkoutTemplate> = emptyList(),
    sessionDates: Map<Int, Long> = emptyMap(),
    snackbarHostState: SnackbarHostState,
    onNavigateToRoute: (String) -> Unit,
    onAddExercise: (Exercise) -> Unit,
    onUpdateExercise: (Exercise) -> Unit,
    onDeleteExercise: (Int) -> Unit,
    onUpdateExercisePhoto: (Int, Uri) -> Unit,
    getExerciseHistory: (String) -> Flow<List<SessionExercise>>,
    onReorderExercises: (List<Exercise>) -> Unit,
    onSetExerciseCategory: (Exercise, String?) -> Unit = { _, _ -> },
    onSaveTemplate: (WorkoutTemplate) -> Unit = {},
    onDeleteTemplate: (String) -> Unit = {}
) {
    val context = LocalContext.current
    var selectedExerciseId by remember { mutableStateOf<Int?>(null) }
    var showExerciseWizard by remember { mutableStateOf(false) }
    var reorderMode by remember { mutableStateOf(false) }
    var orderedExercises by remember { mutableStateOf(exercises) }
    var selectedCategoryId by remember { mutableStateOf<String?>(null) }
    var showTemplatesDialog by remember { mutableStateOf(false) }
    var trendExerciseName by remember { mutableStateOf<String?>(null) }

    val trendPoints = produceState<List<com.example.workoutapp.domain.trend.ExerciseTrendPoint>>(
        initialValue = emptyList(),
        trendExerciseName,
        sessionDates
    ) {
        val name = trendExerciseName ?: return@produceState
        getExerciseHistory(name).collect { entries ->
            value = com.example.workoutapp.domain.trend.ExerciseTrendCalculator.build(entries, sessionDates)
        }
    }

    val categoriesById = categories.associateBy { it.id }
    val displayExercises = if (selectedCategoryId == null) {
        orderedExercises
    } else {
        orderedExercises.filter { it.categoryId == selectedCategoryId }
    }

    LaunchedEffect(exercises, reorderMode) {
        if (!reorderMode) {
            orderedExercises = exercises
        }
    }

    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        uri?.let {
            selectedExerciseId?.let { exerciseId ->
                try {
                    context.contentResolver.takePersistableUriPermission(
                        uri,
                        Intent.FLAG_GRANT_READ_URI_PERMISSION
                    )
                } catch (_: Exception) {
                }
                onUpdateExercisePhoto(exerciseId, uri)
            }
        }
    }

    if (showExerciseWizard) {
        ExerciseWizardDialog(
            onDismiss = { showExerciseWizard = false },
            onCreate = {
                onAddExercise(it)
                showExerciseWizard = false
            }
        )
    }

    if (showTemplatesDialog) {
        TemplatesDialog(
            templates = templates,
            exercises = exercises,
            onSaveTemplate = onSaveTemplate,
            onDeleteTemplate = onDeleteTemplate,
            onDismiss = { showTemplatesDialog = false }
        )
    }

    val trendExercise = trendExerciseName
    if (trendExercise != null) {
        com.example.workoutapp.ui.components.ExerciseTrendDialog(
            exerciseName = trendExercise,
            points = trendPoints.value,
            onDismiss = { trendExerciseName = null }
        )
    }

    fun enterReorderMode() {
        orderedExercises = exercises
        reorderMode = true
    }

    fun moveExerciseInOrder(exerciseId: Int, direction: Int) {
        val fromIndex = orderedExercises.indexOfFirst { it.id == exerciseId }
        val toIndex = fromIndex + direction
        if (fromIndex !in orderedExercises.indices || toIndex !in orderedExercises.indices) return

        orderedExercises = orderedExercises.toMutableList().apply {
            add(toIndex, removeAt(fromIndex))
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = { Text("Workout Library") },
                actions = {
                    if (exercises.isNotEmpty() && !reorderMode) {
                        TextButton(onClick = ::enterReorderMode) {
                            Text("Reorder")
                        }
                        TextButton(onClick = { showTemplatesDialog = true }) {
                            Text("Templates")
                        }
                    }
                    Text(
                        text = "${exercises.size} total",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(end = 12.dp)
                    )
                }
            )
        },
        bottomBar = {
            BottomNavBar(
                currentRoute = "workouts",
                onNavigate = onNavigateToRoute
            )
        },
        floatingActionButton = {
            if (!reorderMode) {
                FloatingActionButton(onClick = { showExerciseWizard = true }) {
                    Icon(Icons.Default.Add, contentDescription = "Add Exercise")
                }
            }
        }
    ) { padding ->
        if (exercises.isEmpty()) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Text(
                    text = "No exercises in your library yet.",
                    style = MaterialTheme.typography.titleMedium,
                    textAlign = TextAlign.Center
                )
                Spacer(modifier = Modifier.height(10.dp))
                Text(
                    text = "Tap + to add your first exercise.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center
                )
            }
        } else {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
            ) {
                if (reorderMode) {
                    ReorderModeBanner(
                        onDone = {
                            onReorderExercises(orderedExercises)
                            reorderMode = false
                        },
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                    )
                }

                if (!reorderMode && categories.isNotEmpty()) {
                    CategoryFilterRow(
                        categories = categories,
                        selectedCategoryId = selectedCategoryId,
                        onSelectCategory = { selectedCategoryId = it }
                    )
                }

                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 16.dp),
                    contentPadding = PaddingValues(bottom = 96.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    itemsIndexed(displayExercises, key = { _, exercise -> exercise.id }) { index, exercise ->
                        // Remember the flow per item. Calling getExerciseHistory
                        // inline built a brand new cold flow on every
                        // recomposition, and collectAsState uses the flow
                        // instance as its effect key, so each recomposition
                        // cancelled and re-attached a Firestore listener for
                        // every visible card. ExerciseCard already used this
                        // pattern for its own history.
                        val exerciseHistoryFlow =
                            remember(exercise.name) { getExerciseHistory(exercise.name) }
                        WorkoutLibraryItem(
                            exercise = exercise,
                            exerciseHistory = exerciseHistoryFlow,
                            canMoveUp = index > 0,
                            canMoveDown = index < displayExercises.lastIndex,
                            reorderMode = reorderMode,
                            onEnterReorderMode = ::enterReorderMode,
                            onUpdate = onUpdateExercise,
                            onDelete = { onDeleteExercise(exercise.id) },
                            onMoveUp = { moveExerciseInOrder(exercise.id, -1) },
                            onMoveDown = { moveExerciseInOrder(exercise.id, 1) },
                            onUploadPhoto = {
                                selectedExerciseId = exercise.id
                                photoPickerLauncher.launch(
                                    PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                                )
                            },
                            onActiveToggle = {
                                onUpdateExercise(exercise.copy(activeInSession = !exercise.activeInSession))
                            },
                            category = exercise.categoryId?.let { categoriesById[it] },
                            allCategories = categories,
                            onSetCategory = { categoryId ->
                                onSetExerciseCategory(exercise, categoryId)
                            },
                            onViewTrends = { trendExerciseName = exercise.name }
                        )
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun WorkoutLibraryItem(
    exercise: Exercise,
    exerciseHistory: Flow<List<SessionExercise>>,
    canMoveUp: Boolean,
    canMoveDown: Boolean,
    reorderMode: Boolean,
    onEnterReorderMode: () -> Unit,
    onUpdate: (Exercise) -> Unit,
    onDelete: () -> Unit,
    onMoveUp: () -> Unit,
    onMoveDown: () -> Unit,
    onUploadPhoto: () -> Unit,
    onActiveToggle: (Boolean) -> Unit = {},
    onCategoryClick: () -> Unit = {},
    category: Category? = null,
    allCategories: List<Category> = emptyList(),
    onSetCategory: ((String?) -> Unit)? = null,
    onViewTrends: (() -> Unit)? = null
) {
    var showEditDialog by remember { mutableStateOf(false) }
    var showDeleteDialog by remember { mutableStateOf(false) }
    var showDetailsDialog by remember { mutableStateOf(false) }
    var showOverflowMenu by remember { mutableStateOf(false) }
    var showCategoryDialog by remember { mutableStateOf(false) }
    var dragDistance by remember { mutableStateOf(0f) }
    var cardHeightPx by remember { mutableStateOf(0f) }
    val history by exerciseHistory.collectAsStateWithLifecycle(initialValue = emptyList())
    val recentHistory = history.sortedByDescending { it.sessionId }
    val lastEntry = recentHistory.firstOrNull()
    val bestVolume = history.maxOfOrNull { it.volume } ?: 0f

    LaunchedEffect(reorderMode) {
        dragDistance = 0f
    }

    if (showEditDialog) {
        ExerciseEditDialog(
            exercise = exercise,
            onDismiss = { showEditDialog = false },
            onSave = { name, weight, reps, sets ->
                onUpdate(exercise.copy(name = name, weight = weight, reps = reps, sets = sets))
                showEditDialog = false
            }
        )
    }

    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text("Delete Exercise?") },
            text = { Text("Remove ${exercise.name} from your workout library?") },
            confirmButton = {
                Button(
                    onClick = {
                        showDeleteDialog = false
                        onDelete()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("Delete")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    if (showDetailsDialog) {
        ExerciseDetailsDialog(
            exercise = exercise,
            history = history,
            onViewTrends = onViewTrends,
            onDismiss = { showDetailsDialog = false }
        )
    }

    if (showCategoryDialog && onSetCategory != null) {
        CategoryAssignDialog(
            categories = allCategories,
            currentCategoryId = exercise.categoryId,
            onSelect = { categoryId ->
                onSetCategory(categoryId)
                showCategoryDialog = false
            },
            onDismiss = { showCategoryDialog = false }
        )
    }

    val dragState = rememberDraggableState { delta ->
        dragDistance += delta
        val reorderThreshold = (cardHeightPx * 0.5f).coerceAtLeast(120f)
        when {
            dragDistance <= -reorderThreshold && canMoveUp -> {
                dragDistance = 0f
                onMoveUp()
            }
            dragDistance >= reorderThreshold && canMoveDown -> {
                dragDistance = 0f
                onMoveDown()
            }
        }
    }

    val interactionModifier = if (reorderMode) {
        Modifier.draggable(
            state = dragState,
            orientation = Orientation.Vertical,
            enabled = true,
            onDragStopped = { dragDistance = 0f }
        )
    } else {
        Modifier
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .alpha(if (exercise.activeInSession) 1f else 0.55f)
            .then(interactionModifier)
            .onGloballyPositioned { cardHeightPx = it.size.height.toFloat() }
            .offset { IntOffset(x = 0, y = if (reorderMode) dragDistance.roundToInt() else 0) }
            .zIndex(if (reorderMode && dragDistance != 0f) 1f else 0f)
            .border(
                width = if (reorderMode) 1.dp else 0.dp,
                color = if (reorderMode) MaterialTheme.colorScheme.primary else Color.Transparent,
                shape = RoundedCornerShape(12.dp)
            ),
        colors = CardDefaults.cardColors(
            containerColor = if (reorderMode) {
                MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.35f)
            } else {
                MaterialTheme.colorScheme.surfaceVariant
            }
        )
    ) {
        Column {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(180.dp)
                    .background(MaterialTheme.colorScheme.surfaceVariant)
                    .testTag("library_card_hero"),
                contentAlignment = Alignment.Center
            ) {
                val photoModel = exercise.photoUri
                if (photoModel.isNullOrBlank()) {
                    Icon(
                        imageVector = CategoryIcons.iconForName(category?.iconName ?: "Category"),
                        contentDescription = null,
                        modifier = Modifier.size(64.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                } else {
                    val saturation = if (exercise.activeInSession) 1f else 0f
                    val matrix = ColorMatrix().apply { setToSaturation(saturation) }
                    Image(
                        painter = rememberAsyncImagePainter(photoModel),
                        contentDescription = exercise.name,
                        modifier = Modifier
                            .fillMaxSize()
                            .testTag("library_card_photo"),
                        contentScale = ContentScale.Crop,
                        colorFilter = ColorFilter.colorMatrix(matrix)
                    )
                }
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 14.dp, top = 8.dp, end = 6.dp, bottom = 14.dp),
                verticalAlignment = Alignment.Top
            ) {
                if (!reorderMode) {
                    Checkbox(
                        checked = exercise.activeInSession,
                        onCheckedChange = onActiveToggle,
                        modifier = Modifier.padding(top = 4.dp),
                        colors = CheckboxDefaults.colors(
                            checkedColor = MaterialTheme.colorScheme.primary
                        )
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                }
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .combinedClickable(
                            onClick = { showDetailsDialog = true },
                            onLongClick = onEnterReorderMode
                        ),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.Top
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = exercise.name,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = exerciseSummary(exercise),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        if (reorderMode) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.Menu, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = "Drag",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.primary,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }

                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        LibraryBadge(
                            text = when (exercise.exerciseType) {
                                ExerciseType.HOLD.name -> "Hold"
                                ExerciseType.BODYWEIGHT.name -> "Bodyweight"
                                else -> "Standard"
                            }
                        )
                        if (exercise.usesSensor) {
                            LibraryBadge("Sensor")
                        }
                        if (exercise.photoUri != null) {
                            LibraryBadge("Photo")
                        }
                    }

                    if (!reorderMode && category != null) {
                        AssistChip(
                            onClick = {
                                if (onSetCategory != null) {
                                    showCategoryDialog = true
                                } else {
                                    onCategoryClick()
                                }
                            },
                            label = { Text(category.name, style = MaterialTheme.typography.labelSmall) },
                            leadingIcon = {
                                Icon(
                                    imageVector = CategoryIcons.iconForName(category.iconName),
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        )
                    }

                    if (reorderMode) {
                        Text(
                            text = "Drag this card up or down to move it.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    } else if (history.isNotEmpty()) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = "Last: ${lastEntry?.sets ?: 0} x ${lastEntry?.reps ?: 0} @ ${formatKg(lastEntry?.weight ?: 0f)}",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = "Best: ${formatKg(bestVolume)}",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.primary,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    } else {
                        Text(
                            text = "Tap for setup and history",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                if (!reorderMode) {
                    Box {
                        IconButton(onClick = { showOverflowMenu = true }) {
                            Icon(Icons.Default.MoreVert, contentDescription = "Exercise actions")
                        }
                        ExerciseActionsMenu(
                            expanded = showOverflowMenu,
                            onDismiss = { showOverflowMenu = false },
                            onDetails = { showDetailsDialog = true },
                            onPhoto = onUploadPhoto,
                            onEdit = { showEditDialog = true },
                            onDelete = { showDeleteDialog = true },
                            onReorder = onEnterReorderMode,
                            onCategory = if (onSetCategory != null) {
                                { showCategoryDialog = true }
                            } else {
                                null
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun LibraryBadge(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.labelSmall,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier
            .background(
                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f),
                shape = RoundedCornerShape(50)
            )
            .padding(horizontal = 8.dp, vertical = 3.dp)
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CategoryFilterRow(
    categories: List<Category>,
    selectedCategoryId: String?,
    onSelectCategory: (String?) -> Unit
) {
    LazyRow(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 4.dp)
    ) {
        item(key = "all") {
            FilterChip(
                selected = selectedCategoryId == null,
                onClick = { onSelectCategory(null) },
                label = { Text("All") }
            )
        }
        items(categories, key = { it.id }) { category ->
            FilterChip(
                selected = selectedCategoryId == category.id,
                onClick = {
                    onSelectCategory(if (selectedCategoryId == category.id) null else category.id)
                },
                label = { Text(category.name) },
                leadingIcon = {
                    Icon(
                        imageVector = CategoryIcons.iconForName(category.iconName),
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                }
            )
        }
    }
}

@Composable
private fun CategoryAssignDialog(
    categories: List<Category>,
    currentCategoryId: String?,
    onSelect: (String?) -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Assign Category") },
        text = {
            Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                CategoryOptionRow(
                    label = "No category",
                    selected = currentCategoryId == null,
                    onClick = { onSelect(null) }
                )
                categories.forEach { category ->
                    CategoryOptionRow(
                        label = category.name,
                        leadingIcon = CategoryIcons.iconForName(category.iconName),
                        selected = currentCategoryId == category.id,
                        onClick = { onSelect(category.id) }
                    )
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Close")
            }
        }
    )
}

@Composable
private fun CategoryOptionRow(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    leadingIcon: androidx.compose.ui.graphics.vector.ImageVector? = null
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 10.dp, horizontal = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        RadioButton(selected = selected, onClick = null)
        if (leadingIcon != null) {
            Icon(
                imageVector = leadingIcon,
                contentDescription = null,
                modifier = Modifier.size(18.dp)
            )
        }
        Spacer(modifier = Modifier.width(8.dp))
        Text(text = label, style = MaterialTheme.typography.bodyMedium)
    }
}

@Composable
private fun TemplatesDialog(
    templates: List<WorkoutTemplate>,
    exercises: List<Exercise>,
    onSaveTemplate: (WorkoutTemplate) -> Unit,
    onDeleteTemplate: (String) -> Unit,
    onDismiss: () -> Unit
) {
    var editingTemplate by remember { mutableStateOf<WorkoutTemplate?>(null) }
    var creating by remember { mutableStateOf(false) }

    if (creating || editingTemplate != null) {
        TemplateEditorDialog(
            initial = editingTemplate,
            exercises = exercises,
            onSave = { template ->
                onSaveTemplate(template)
                creating = false
                editingTemplate = null
            },
            onDismiss = {
                creating = false
                editingTemplate = null
            }
        )
        return
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Workout Templates") },
        text = {
            Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                if (templates.isEmpty()) {
                    Text(
                        text = "No templates yet. Create one to reuse an exercise selection and order.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                } else {
                    templates.forEach { template ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = template.name,
                                    style = MaterialTheme.typography.bodyLarge,
                                    fontWeight = FontWeight.Medium
                                )
                                Text(
                                    text = "${template.exerciseIds.size} exercises",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            IconButton(onClick = { editingTemplate = template }) {
                                Icon(Icons.Default.Edit, contentDescription = "Edit ${template.name}")
                            }
                            IconButton(onClick = { onDeleteTemplate(template.id) }) {
                                Icon(
                                    Icons.Default.Delete,
                                    contentDescription = "Delete ${template.name}",
                                    tint = MaterialTheme.colorScheme.error
                                )
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(onClick = { creating = true }) {
                Text("New Template")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Close")
            }
        }
    )
}

@Composable
private fun TemplateEditorDialog(
    initial: WorkoutTemplate?,
    exercises: List<Exercise>,
    onSave: (WorkoutTemplate) -> Unit,
    onDismiss: () -> Unit
) {
    var name by remember { mutableStateOf(initial?.name ?: "") }
    val selectedIds = remember(exercises) {
        initial?.exerciseIds?.filter { id -> exercises.any { it.id == id } }?.toMutableStateList()
            ?: mutableStateListOf()
    }

    fun move(id: Int, delta: Int) {
        val index = selectedIds.indexOf(id)
        val target = index + delta
        if (index < 0 || target !in selectedIds.indices) return
        selectedIds.removeAt(index)
        selectedIds.add(target, id)
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (initial == null) "New Template" else "Edit Template") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Template name") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Text(
                    text = "Select exercises in order. Use arrows to reorder.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Column(
                    modifier = Modifier
                        .verticalScroll(rememberScrollState())
                        .heightIn(max = 320.dp)
                ) {
                    exercises.forEach { exercise ->
                        val isSelected = exercise.id in selectedIds
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Checkbox(
                                checked = isSelected,
                                onCheckedChange = { checked ->
                                    if (checked) selectedIds.add(exercise.id) else selectedIds.remove(exercise.id)
                                }
                            )
                            Text(
                                text = exercise.name,
                                modifier = Modifier.weight(1f),
                                maxLines = 1
                            )
                            if (isSelected) {
                                IconButton(
                                    onClick = { move(exercise.id, -1) },
                                    enabled = exercise.id != selectedIds.first()
                                ) {
                                    Icon(Icons.Default.KeyboardArrowUp, contentDescription = "Move up")
                                }
                                IconButton(
                                    onClick = { move(exercise.id, 1) },
                                    enabled = exercise.id != selectedIds.last()
                                ) {
                                    Icon(Icons.Default.KeyboardArrowDown, contentDescription = "Move down")
                                }
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    onSave(
                        WorkoutTemplate(
                            id = initial?.id ?: "",
                            name = name.trim(),
                            exerciseIds = selectedIds.toList(),
                            sortOrder = initial?.sortOrder ?: 0
                        )
                    )
                },
                enabled = name.isNotBlank() && selectedIds.isNotEmpty()
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

@Composable
private fun ExerciseActionsMenu(
    expanded: Boolean,
    onDismiss: () -> Unit,
    onDetails: () -> Unit,
    onPhoto: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    onReorder: () -> Unit,
    onCategory: (() -> Unit)? = null
) {
    DropdownMenu(expanded = expanded, onDismissRequest = onDismiss) {
        DropdownMenuItem(
            text = { Text("Details") },
            onClick = {
                onDismiss()
                onDetails()
            },
            leadingIcon = { Icon(Icons.Default.Info, contentDescription = null) }
        )
        if (onCategory != null) {
            DropdownMenuItem(
                text = { Text("Category") },
                onClick = {
                    onDismiss()
                    onCategory()
                },
                leadingIcon = { Icon(Icons.Default.Label, contentDescription = null) }
            )
        }
        DropdownMenuItem(
            text = { Text("Photo") },
            onClick = {
                onDismiss()
                onPhoto()
            },
            leadingIcon = { Icon(Icons.Default.Image, contentDescription = null) }
        )
        DropdownMenuItem(
            text = { Text("Edit") },
            onClick = {
                onDismiss()
                onEdit()
            },
            leadingIcon = { Icon(Icons.Default.Edit, contentDescription = null) }
        )
        DropdownMenuItem(
            text = { Text("Reorder") },
            onClick = {
                onDismiss()
                onReorder()
            },
            leadingIcon = { Icon(Icons.Default.Menu, contentDescription = null) }
        )
        DropdownMenuItem(
            text = { Text("Delete") },
            onClick = {
                onDismiss()
                onDelete()
            },
            leadingIcon = { Icon(Icons.Default.Delete, contentDescription = null) }
        )
    }
}

@Composable
private fun ReorderModeBanner(
    onDone: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.primaryContainer, RoundedCornerShape(16.dp))
            .padding(14.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = "Reorder mode",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = "Drag exercises up or down. Tap Done when finished.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        TextButton(onClick = onDone) {
            Text("Done")
        }
    }
}

@Composable
private fun ExerciseDetailsDialog(
    exercise: Exercise,
    history: List<SessionExercise>,
    onDismiss: () -> Unit,
    onViewTrends: (() -> Unit)? = null
) {
    val recentHistory = history.sortedByDescending { it.sessionId }
    val lastEntry = recentHistory.firstOrNull()
    val bestWeight = history.maxOfOrNull { it.weight } ?: 0f
    val bestVolume = history.maxOfOrNull { it.volume } ?: 0f

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(exercise.name) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                DetailSection(title = "Current Setup") {
                    DetailText(exerciseSummary(exercise))
                    DetailText(
                        when (exercise.exerciseType) {
                            ExerciseType.HOLD.name -> "Hold exercise"
                            ExerciseType.BODYWEIGHT.name -> "Bodyweight exercise"
                            else -> "Weighted exercise"
                        }
                    )
                    DetailText(if (exercise.usesSensor) "Sensor enabled" else "Manual tracking")
                }

                DetailSection(title = "History") {
                    if (history.isEmpty()) {
                        DetailText("No completed sessions yet.")
                    } else {
                        DetailText("Completed ${history.size} time${if (history.size == 1) "" else "s"}")
                        DetailText("Last: ${lastEntry?.sets ?: 0} x ${lastEntry?.reps ?: 0} @ ${formatKg(lastEntry?.weight ?: 0f)}")
                        DetailText("Best weight: ${formatKg(bestWeight)}")
                        DetailText("Best volume: ${formatKg(bestVolume)}")
                    }
                }

                if (recentHistory.isNotEmpty()) {
                    DetailSection(title = "Recent Performances") {
                        recentHistory.take(5).forEach { entry ->
                            DetailText("${entry.sets} x ${entry.reps} @ ${formatKg(entry.weight)} - ${formatKg(entry.volume)} volume")
                        }
                    }
                }
            }
        },
        confirmButton = {
            Row {
                if (onViewTrends != null && history.isNotEmpty()) {
                    TextButton(onClick = onViewTrends) {
                        Text("View Trends")
                    }
                }
                TextButton(onClick = onDismiss) {
                    Text("Close")
                }
            }
        }
    )
}

@Composable
private fun DetailSection(
    title: String,
    content: @Composable ColumnScope.() -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(text = title, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
        content()
    }
}

@Composable
private fun DetailText(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant
    )
}

private fun exerciseSummary(exercise: Exercise): String {
    return when (exercise.exerciseType) {
        ExerciseType.HOLD.name -> "${exercise.sets} sets x ${exercise.holdDurationSeconds}s hold"
        ExerciseType.BODYWEIGHT.name -> "${exercise.sets} sets x ${exercise.reps} reps (bodyweight)"
        else -> "${exercise.sets} sets x ${exercise.reps} reps @ ${formatKg(exercise.weight)}"
    }
}
