package com.minimal.today.ui.screens

import android.content.Context
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.draggable
import androidx.compose.foundation.gestures.rememberDraggableState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.minimal.today.model.TodoItem
import com.minimal.today.ui.theme.MaliFontFamily
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

fun triggerHapticFeedback(context: Context) {
    try {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val vibratorManager = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager
            vibratorManager?.defaultVibrator?.vibrate(VibrationEffect.createPredefined(VibrationEffect.EFFECT_CLICK))
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val vibrator = context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
            vibrator?.vibrate(VibrationEffect.createPredefined(VibrationEffect.EFFECT_CLICK))
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val vibrator = context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
            vibrator?.vibrate(VibrationEffect.createOneShot(20, VibrationEffect.DEFAULT_AMPLITUDE))
        } else {
            @Suppress("DEPRECATION")
            val vibrator = context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
            @Suppress("DEPRECATION")
            vibrator?.vibrate(20)
        }
    } catch (_: Exception) {}
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TodayScreen(
    todos: List<TodoItem>,
    isDarkTheme: Boolean,
    onSetDoneStatus: (item: TodoItem, isDone: Boolean) -> Unit,
    onDeleteTodo: (item: TodoItem) -> Unit,
    onNavigateToAdd: () -> Unit,
    onNavigateToLog: () -> Unit,
    onToggleTheme: (Boolean) -> Unit,
    onClearCompleted: () -> Unit,
    modifier: Modifier = Modifier
) {
    var showMenuSheet by remember { mutableStateOf(false) }
    var selectedTodoForDetails by remember { mutableStateOf<TodoItem?>(null) }
    var todoToDelete by remember { mutableStateOf<TodoItem?>(null) }

    val menuSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val detailsSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val scope = rememberCoroutineScope()
    val context = LocalContext.current

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        // Minimalist Top Bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 12.dp, end = 4.dp, top = 4.dp, bottom = 4.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "today",
                fontFamily = MaliFontFamily,
                fontWeight = FontWeight.Medium,
                fontSize = 21.sp,
                letterSpacing = (-0.5).sp,
                color = MaterialTheme.colorScheme.onBackground
            )

            // The single button for many actions
            IconButton(
                onClick = { showMenuSheet = true },
                modifier = Modifier.size(38.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.MoreVert,
                    contentDescription = "Actions Menu",
                    tint = MaterialTheme.colorScheme.onBackground,
                    modifier = Modifier.size(20.dp)
                )
            }
        }

        // Hairline separator
        HorizontalDivider(
            thickness = 0.5.dp,
            color = MaterialTheme.colorScheme.outline
        )

        // Compact Todo List or Empty state
        if (todos.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(top = 80.dp),
                contentAlignment = Alignment.TopCenter
            ) {
                Text(
                    text = "no tasks",
                    fontFamily = MaliFontFamily,
                    fontSize = 14.sp,
                    fontStyle = FontStyle.Italic,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 6.dp, vertical = 6.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                items(todos, key = { it.id }) { item ->
                    SwipeableTodoTile(
                        item = item,
                        isDark = isDarkTheme,
                        onSwipeDone = {
                            triggerHapticFeedback(context)
                            onSetDoneStatus(item, true)
                        },
                        onSwipeUndone = {
                            triggerHapticFeedback(context)
                            onSetDoneStatus(item, false)
                        },
                        onOpenDetails = {
                            triggerHapticFeedback(context)
                            selectedTodoForDetails = item
                        }
                    )
                }
            }
        }
    }

    // 1. Actions Menu Bottom Sheet with upgraded segmented theme toggle
    if (showMenuSheet) {
        ModalBottomSheet(
            onDismissRequest = { showMenuSheet = false },
            sheetState = menuSheetState,
            shape = RoundedCornerShape(topStart = 12.dp, topEnd = 12.dp),
            containerColor = MaterialTheme.colorScheme.surface,
            scrimColor = Color.Black.copy(alpha = 0.45f),
            dragHandle = {
                Box(
                    modifier = Modifier
                        .padding(top = 10.dp, bottom = 6.dp)
                        .size(width = 28.dp, height = 2.5.dp)
                        .background(
                            color = MaterialTheme.colorScheme.outline.copy(alpha = 0.8f),
                            shape = RoundedCornerShape(1.dp)
                        )
                )
            }
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 14.dp, vertical = 6.dp)
            ) {
                // Header with subtle typography
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 10.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "actions",
                        fontFamily = MaliFontFamily,
                        fontWeight = FontWeight.Medium,
                        fontSize = 13.sp,
                        letterSpacing = 1.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                    )

                    val doneCount = todos.count { it.isDone }
                    val totalCount = todos.size
                    if (totalCount > 0) {
                        Text(
                            text = "$doneCount of $totalCount done",
                            fontFamily = MaliFontFamily,
                            fontSize = 11.sp,
                            fontStyle = FontStyle.Italic,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                        )
                    }
                }

                HorizontalDivider(
                    thickness = 0.5.dp,
                    color = MaterialTheme.colorScheme.outline.copy(alpha = 0.6f)
                )

                // 1. Add Todo Option
                CompactSheetRow(
                    number = "1",
                    title = "add todo",
                    subtitle = "new task for today",
                    onClick = {
                        scope.launch {
                            menuSheetState.hide()
                            showMenuSheet = false
                            onNavigateToAdd()
                        }
                    }
                )

                HorizontalDivider(
                    thickness = 0.5.dp,
                    color = MaterialTheme.colorScheme.outline.copy(alpha = 0.35f)
                )

                // 2. Theme Option with Enhanced Segmented Toggle Control (with Icons + Wider layout)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 10.dp, horizontal = 2.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = "2.",
                            fontFamily = MaliFontFamily,
                            fontWeight = FontWeight.Medium,
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                            modifier = Modifier.width(22.dp)
                        )
                        Column {
                            Text(
                                text = "theme",
                                fontFamily = MaliFontFamily,
                                fontWeight = FontWeight.Medium,
                                fontSize = 15.sp,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = "appearance mode",
                                fontFamily = MaliFontFamily,
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                            )
                        }
                    }

                    // Upgraded Wide Segmented Toggle with Icons
                    EnhancedThemeToggle(
                        isDark = isDarkTheme,
                        onSelectTheme = { dark ->
                            onToggleTheme(dark)
                        }
                    )
                }

                HorizontalDivider(
                    thickness = 0.5.dp,
                    color = MaterialTheme.colorScheme.outline.copy(alpha = 0.35f)
                )

                // 3. View TXT Log Option
                CompactSheetRow(
                    number = "3",
                    title = "todos.txt log",
                    subtitle = "view all saved days",
                    onClick = {
                        scope.launch {
                            menuSheetState.hide()
                            showMenuSheet = false
                            onNavigateToLog()
                        }
                    }
                )

                // 4. Clear Completed Option (if any)
                if (todos.any { it.isDone }) {
                    HorizontalDivider(
                        thickness = 0.5.dp,
                        color = MaterialTheme.colorScheme.outline.copy(alpha = 0.35f)
                    )
                    CompactSheetRow(
                        number = "4",
                        title = "clear completed",
                        subtitle = "remove finished items",
                        titleColor = MaterialTheme.colorScheme.onSurfaceVariant,
                        onClick = {
                            scope.launch {
                                menuSheetState.hide()
                                showMenuSheet = false
                                onClearCompleted()
                            }
                        }
                    )
                }

                Spacer(modifier = Modifier.height(14.dp))
            }
        }
    }

    // 2. Beautifully Redesigned Task Details Bottom Sheet
    selectedTodoForDetails?.let { item ->
        ModalBottomSheet(
            onDismissRequest = { selectedTodoForDetails = null },
            sheetState = detailsSheetState,
            shape = RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp),
            containerColor = MaterialTheme.colorScheme.surface,
            scrimColor = Color.Black.copy(alpha = 0.5f),
            dragHandle = {
                Box(
                    modifier = Modifier
                        .padding(top = 10.dp, bottom = 6.dp)
                        .size(width = 32.dp, height = 3.dp)
                        .background(
                            color = MaterialTheme.colorScheme.outline.copy(alpha = 0.7f),
                            shape = RoundedCornerShape(1.5.dp)
                        )
                )
            }
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 18.dp, vertical = 8.dp)
            ) {
                // Header Bar: Title + Delete Button (Text only, no icon)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "TASK DETAILS",
                        fontFamily = MaliFontFamily,
                        fontWeight = FontWeight.Medium,
                        fontSize = 11.sp,
                        letterSpacing = 1.2.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                    )

                    // Minimal Text-only Delete Button
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f))
                            .border(
                                BorderStroke(0.6.dp, Color(0xFFEF4444).copy(alpha = 0.4f)),
                                shape = RoundedCornerShape(6.dp)
                            )
                            .clickable {
                                triggerHapticFeedback(context)
                                todoToDelete = item
                            }
                            .padding(horizontal = 14.dp, vertical = 5.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "delete",
                            fontFamily = MaliFontFamily,
                            fontWeight = FontWeight.Medium,
                            fontSize = 12.sp,
                            color = Color(0xFFEF4444)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Main Task Title
                Text(
                    text = item.text,
                    fontFamily = MaliFontFamily,
                    fontWeight = FontWeight.Medium,
                    fontSize = 18.sp,
                    lineHeight = 24.sp,
                    color = MaterialTheme.colorScheme.onSurface
                )

                // Schedule Card (Start & End Time)
                if (!item.startTime.isNullOrBlank() || !item.endTime.isNullOrBlank()) {
                    Spacer(modifier = Modifier.height(12.dp))
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .border(
                                BorderStroke(0.8.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.6f)),
                                shape = RoundedCornerShape(6.dp)
                            )
                            .background(MaterialTheme.colorScheme.background, shape = RoundedCornerShape(6.dp))
                            .padding(horizontal = 12.dp, vertical = 8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = "start",
                                fontFamily = MaliFontFamily,
                                fontSize = 10.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                            )
                            Text(
                                text = item.startTime ?: "—",
                                fontFamily = MaliFontFamily,
                                fontWeight = FontWeight.Medium,
                                fontSize = 13.sp,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }

                        Text(
                            text = "→",
                            fontFamily = MaliFontFamily,
                            fontSize = 13.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f)
                        )

                        Column(horizontalAlignment = Alignment.End) {
                            Text(
                                text = "end",
                                fontFamily = MaliFontFamily,
                                fontSize = 10.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                            )
                            Text(
                                text = item.endTime ?: "—",
                                fontFamily = MaliFontFamily,
                                fontWeight = FontWeight.Medium,
                                fontSize = 13.sp,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Notes Card
                Text(
                    text = "notes",
                    fontFamily = MaliFontFamily,
                    fontSize = 11.sp,
                    fontStyle = FontStyle.Italic,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                )

                Spacer(modifier = Modifier.height(4.dp))

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(
                            BorderStroke(0.8.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.6f)),
                            shape = RoundedCornerShape(6.dp)
                        )
                        .background(MaterialTheme.colorScheme.background, shape = RoundedCornerShape(6.dp))
                        .padding(12.dp)
                ) {
                    Text(
                        text = if (!item.notes.isNullOrBlank()) item.notes else "no additional notes attached.",
                        fontFamily = MaliFontFamily,
                        fontSize = 13.5.sp,
                        lineHeight = 19.sp,
                        color = if (!item.notes.isNullOrBlank()) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.45f)
                    )
                }

                Spacer(modifier = Modifier.height(20.dp))

                // Slide to Complete Button (Neutral greyish default, turns green upon reaching end)
                SlideToStatusButton(
                    isDone = item.isDone,
                    onSlideComplete = {
                        val nextDone = !item.isDone
                        triggerHapticFeedback(context)
                        onSetDoneStatus(item, nextDone)
                        scope.launch {
                            detailsSheetState.hide()
                            selectedTodoForDetails = null
                        }
                    }
                )

                Spacer(modifier = Modifier.height(14.dp))
            }
        }
    }

    // 3. Custom Made Confirmation Dialog for Deleting Task (Generous Width & Elevated Polish)
    todoToDelete?.let { item ->
        CustomDeleteDialog(
            taskText = item.text,
            onConfirm = {
                triggerHapticFeedback(context)
                onDeleteTodo(item)
                todoToDelete = null
                scope.launch {
                    detailsSheetState.hide()
                    selectedTodoForDetails = null
                }
            },
            onDismiss = {
                todoToDelete = null
            }
        )
    }
}

/**
 * Custom Minimal Confirmation Dialog for Delete Action with Generous Width
 */
@Composable
private fun CustomDeleteDialog(
    taskText: String,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.92f) // Generous comfortable width
                .clip(RoundedCornerShape(12.dp)),
            shape = RoundedCornerShape(12.dp),
            color = MaterialTheme.colorScheme.surface,
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.6f)),
            tonalElevation = 8.dp
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 20.dp)
            ) {
                // Header with Delete Title
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = "delete task",
                        fontFamily = MaliFontFamily,
                        fontWeight = FontWeight.Medium,
                        fontSize = 18.sp,
                        color = MaterialTheme.colorScheme.onSurface
                    )

                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(4.dp))
                            .background(Color(0xFFEF4444).copy(alpha = 0.12f))
                            .padding(horizontal = 8.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = "warning",
                            fontFamily = MaliFontFamily,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Medium,
                            color = Color(0xFFEF4444)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                Text(
                    text = "are you sure you want to delete this task?",
                    fontFamily = MaliFontFamily,
                    fontSize = 14.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Spacer(modifier = Modifier.height(8.dp))

                // Quoted Task Box
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(
                            BorderStroke(0.6.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)),
                            shape = RoundedCornerShape(6.dp)
                        )
                        .background(MaterialTheme.colorScheme.background, shape = RoundedCornerShape(6.dp))
                        .padding(10.dp)
                ) {
                    Text(
                        text = "\"$taskText\"",
                        fontFamily = MaliFontFamily,
                        fontSize = 13.sp,
                        fontStyle = FontStyle.Italic,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.85f),
                        maxLines = 3,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                Spacer(modifier = Modifier.height(20.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Cancel Button
                    Surface(
                        onClick = onDismiss,
                        shape = RoundedCornerShape(6.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
                        modifier = Modifier
                            .weight(1f)
                            .height(40.dp)
                    ) {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "cancel",
                                fontFamily = MaliFontFamily,
                                fontWeight = FontWeight.Normal,
                                fontSize = 13.5.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    // Delete Confirm Button
                    Surface(
                        onClick = onConfirm,
                        shape = RoundedCornerShape(6.dp),
                        color = Color(0xFFEF4444),
                        modifier = Modifier
                            .weight(1f)
                            .height(40.dp)
                    ) {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "delete",
                                fontFamily = MaliFontFamily,
                                fontWeight = FontWeight.Medium,
                                fontSize = 13.5.sp,
                                color = Color.White
                            )
                        }
                    }
                }
            }
        }
    }
}

/**
 * Slide to Complete Button in Bottom Sheet:
 * - Default: Normal clean greyish / neutral style
 * - Reaching end threshold: Smoothly illuminates to Green
 */
@Composable
private fun SlideToStatusButton(
    isDone: Boolean,
    onSlideComplete: () -> Unit,
    modifier: Modifier = Modifier
) {
    val density = LocalDensity.current
    val scope = rememberCoroutineScope()
    val dragOffsetX = remember { Animatable(0f) }

    BoxWithConstraints(
        modifier = modifier
            .fillMaxWidth()
            .height(50.dp)
    ) {
        val maxDragWidth = with(density) { (maxWidth - 50.dp).toPx() }
        val progress = if (maxDragWidth > 0) (dragOffsetX.value / maxDragWidth).coerceIn(0f, 1f) else 0f
        val isNearEnd = progress >= 0.70f

        val activeGreen = Color(0xFF22C55E)
        val neutralTrack = MaterialTheme.colorScheme.surfaceVariant
        val neutralThumb = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f)

        val trackColor by animateColorAsState(
            targetValue = if (isNearEnd) activeGreen.copy(alpha = 0.18f) else neutralTrack,
            label = "trackColor"
        )

        val thumbColor by animateColorAsState(
            targetValue = if (isNearEnd) activeGreen else neutralThumb,
            label = "thumbColor"
        )

        val labelText = if (isDone) "slide to mark pending" else "slide to complete"

        Box(
            modifier = Modifier
                .fillMaxSize()
                .clip(RoundedCornerShape(25.dp))
                .background(trackColor)
                .border(
                    BorderStroke(
                        1.dp,
                        if (isNearEnd) activeGreen.copy(alpha = 0.4f) else MaterialTheme.colorScheme.outline.copy(alpha = 0.35f)
                    ),
                    shape = RoundedCornerShape(25.dp)
                ),
            contentAlignment = Alignment.CenterStart
        ) {
            // Track Label
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                val textAlpha = (1f - progress).coerceIn(0.15f, 1f)
                Text(
                    text = labelText,
                    fontFamily = MaliFontFamily,
                    fontWeight = FontWeight.Medium,
                    fontSize = 13.5.sp,
                    color = if (isNearEnd) activeGreen.copy(alpha = textAlpha) else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = textAlpha)
                )
            }

            // Draggable Thumb
            Box(
                modifier = Modifier
                    .offset { IntOffset(dragOffsetX.value.roundToInt(), 0) }
                    .size(50.dp)
                    .padding(3.dp)
                    .clip(CircleShape)
                    .background(thumbColor)
                    .draggable(
                        orientation = Orientation.Horizontal,
                        state = rememberDraggableState { delta ->
                            scope.launch {
                                val next = (dragOffsetX.value + delta).coerceIn(0f, maxDragWidth)
                                dragOffsetX.snapTo(next)
                            }
                        },
                        onDragStopped = {
                            scope.launch {
                                if (dragOffsetX.value > maxDragWidth * 0.70f) {
                                    dragOffsetX.animateTo(
                                        maxDragWidth,
                                        spring(dampingRatio = Spring.DampingRatioNoBouncy, stiffness = Spring.StiffnessMedium)
                                    )
                                    onSlideComplete()
                                } else {
                                    dragOffsetX.animateTo(
                                        0f,
                                        spring(dampingRatio = Spring.DampingRatioNoBouncy, stiffness = Spring.StiffnessMedium)
                                    )
                                }
                            }
                        }
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                    contentDescription = "Slide",
                    tint = Color.White,
                    modifier = Modifier.size(18.dp)
                )
            }
        }
    }
}

/**
 * Enhanced Wide Segmented Toggle with Icons & Clean Spacing
 */
@Composable
private fun EnhancedThemeToggle(
    isDark: Boolean,
    onSelectTheme: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .border(
                BorderStroke(0.8.dp, MaterialTheme.colorScheme.outline),
                shape = RoundedCornerShape(16.dp)
            )
            .background(MaterialTheme.colorScheme.background, shape = RoundedCornerShape(16.dp))
            .padding(3.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Light Option
        Row(
            modifier = Modifier
                .clip(RoundedCornerShape(14.dp))
                .background(if (!isDark) MaterialTheme.colorScheme.primary else Color.Transparent)
                .clickable { onSelectTheme(false) }
                .padding(horizontal = 10.dp, vertical = 5.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = Icons.Default.LightMode,
                contentDescription = null,
                tint = if (!isDark) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(13.dp)
            )
            Spacer(modifier = Modifier.width(4.dp))
            Text(
                text = "light",
                fontFamily = MaliFontFamily,
                fontSize = 11.5.sp,
                fontWeight = if (!isDark) FontWeight.Medium else FontWeight.Normal,
                color = if (!isDark) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        // Dark Option
        Row(
            modifier = Modifier
                .clip(RoundedCornerShape(14.dp))
                .background(if (isDark) MaterialTheme.colorScheme.primary else Color.Transparent)
                .clickable { onSelectTheme(true) }
                .padding(horizontal = 10.dp, vertical = 5.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = Icons.Default.DarkMode,
                contentDescription = null,
                tint = if (isDark) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(13.dp)
            )
            Spacer(modifier = Modifier.width(4.dp))
            Text(
                text = "dark",
                fontFamily = MaliFontFamily,
                fontSize = 11.5.sp,
                fontWeight = if (isDark) FontWeight.Medium else FontWeight.Normal,
                color = if (isDark) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun CompactSheetRow(
    number: String,
    title: String,
    subtitle: String? = null,
    titleColor: Color = MaterialTheme.colorScheme.onSurface,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 11.dp, horizontal = 2.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = "$number.",
            fontFamily = MaliFontFamily,
            fontWeight = FontWeight.Medium,
            fontSize = 12.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
            modifier = Modifier.width(22.dp)
        )

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                fontFamily = MaliFontFamily,
                fontWeight = FontWeight.Medium,
                fontSize = 15.sp,
                color = titleColor
            )
            if (!subtitle.isNullOrBlank()) {
                Text(
                    text = subtitle,
                    fontFamily = MaliFontFamily,
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                )
            }
        }
    }
}

/**
 * iOS-Style Swipeable Todo Tile:
 * - Single click does NOTHING
 * - ONLY long press opens details bottom sheet
 * - Swipe right -> done, swipe left -> undone
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun SwipeableTodoTile(
    item: TodoItem,
    isDark: Boolean,
    onSwipeDone: () -> Unit,
    onSwipeUndone: () -> Unit,
    onOpenDetails: () -> Unit
) {
    val density = LocalDensity.current
    val scope = rememberCoroutineScope()
    val dragOffset = remember { Animatable(0f) }
    var isActivelyDragging by remember { mutableStateOf(false) }

    val textColor = if (item.isDone) {
        MaterialTheme.colorScheme.onBackground.copy(alpha = 0.38f)
    } else {
        MaterialTheme.colorScheme.onBackground
    }

    val timeColor = if (item.isDone) {
        MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.30f)
    } else {
        MaterialTheme.colorScheme.primary.copy(alpha = 0.85f)
    }

    val tileBg = MaterialTheme.colorScheme.surface

    val tileBorderColor = if (item.isDone) {
        MaterialTheme.colorScheme.outline.copy(alpha = 0.30f)
    } else {
        MaterialTheme.colorScheme.outline.copy(alpha = 0.65f)
    }

    val doneBgColor = if (isDark) Color(0xFF14301D) else Color(0xFFE8F5E9)
    val doneTextColor = if (isDark) Color(0xFF4ADE80) else Color(0xFF2E7D32)

    val undoneBgColor = if (isDark) Color(0xFF242424) else Color(0xFFEEEEEE)
    val undoneTextColor = if (isDark) Color(0xFFAAAAAA) else Color(0xFF666666)

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
    ) {
        val maxSlidePx = with(density) { 100.dp.toPx() }

        // Background Action Indicators: ONLY visible while user is actively dragging
        if (isActivelyDragging && dragOffset.value != 0f) {
            Box(
                modifier = Modifier
                    .matchParentSize()
                    .background(
                        if (dragOffset.value > 0) doneBgColor else undoneBgColor,
                        shape = RoundedCornerShape(8.dp)
                    )
                    .padding(horizontal = 16.dp),
                contentAlignment = if (dragOffset.value > 0) Alignment.CenterStart else Alignment.CenterEnd
            ) {
                if (dragOffset.value > 15f) {
                    Text(
                        text = "done",
                        fontFamily = MaliFontFamily,
                        fontWeight = FontWeight.Medium,
                        fontSize = 13.sp,
                        color = doneTextColor
                    )
                } else if (dragOffset.value < -15f) {
                    Text(
                        text = "undone",
                        fontFamily = MaliFontFamily,
                        fontWeight = FontWeight.Medium,
                        fontSize = 13.sp,
                        color = undoneTextColor
                    )
                }
            }
        }

        // Foreground Tile with clean snap
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .offset { IntOffset(dragOffset.value.roundToInt(), 0) }
                .border(BorderStroke(0.8.dp, tileBorderColor), shape = RoundedCornerShape(8.dp))
                .background(tileBg, shape = RoundedCornerShape(8.dp))
                .combinedClickable(
                    onClick = { /* Single click does NOTHING as requested */ },
                    onLongClick = onOpenDetails // ONLY long press opens details sheet
                )
                .draggable(
                    orientation = Orientation.Horizontal,
                    state = rememberDraggableState { delta ->
                        isActivelyDragging = true
                        scope.launch {
                            val next = (dragOffset.value + delta).coerceIn(-maxSlidePx, maxSlidePx)
                            dragOffset.snapTo(next)
                        }
                    },
                    onDragStopped = {
                        val shouldMarkDone = dragOffset.value > maxSlidePx * 0.55f
                        val shouldMarkUndone = dragOffset.value < -maxSlidePx * 0.55f
                        isActivelyDragging = false

                        scope.launch {
                            if (shouldMarkDone) {
                                onSwipeDone()
                            } else if (shouldMarkUndone) {
                                onSwipeUndone()
                            }
                            dragOffset.animateTo(
                                0f,
                                spring(dampingRatio = Spring.DampingRatioNoBouncy, stiffness = Spring.StiffnessMedium)
                            )
                        }
                    }
                )
                .padding(horizontal = 12.dp, vertical = 11.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                // Task Text
                Text(
                    text = item.text,
                    fontFamily = MaliFontFamily,
                    fontWeight = if (item.isDone) FontWeight.Normal else FontWeight.Medium,
                    fontSize = 15.sp,
                    color = textColor,
                    lineHeight = 19.sp
                )

                // Optional Batch Time
                val timeText = item.formattedTime
                if (!timeText.isNullOrBlank()) {
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = timeText,
                        fontFamily = MaliFontFamily,
                        fontSize = 11.5.sp,
                        color = timeColor
                    )
                }

                // Optional Notes Preview
                if (!item.notes.isNullOrBlank()) {
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = item.notes,
                        fontFamily = MaliFontFamily,
                        fontSize = 11.sp,
                        fontStyle = FontStyle.Italic,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = if (item.isDone) 0.3f else 0.55f),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }
    }
}
