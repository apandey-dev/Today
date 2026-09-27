package com.minimal.today.ui.screens

import android.app.TimePickerDialog
import android.widget.Toast
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.minimal.today.ui.theme.MaliFontFamily
import kotlinx.coroutines.launch
import java.util.Calendar
import java.util.Locale
import kotlin.math.roundToInt

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun AddTodoScreen(
    onAddTodo: (text: String, notes: String?, startTime: String?, endTime: String?) -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    var taskText by remember { mutableStateOf("") }
    var notesText by remember { mutableStateOf("") }
    var startTime by remember { mutableStateOf<String?>(null) }
    var endTime by remember { mutableStateOf<String?>(null) }
    val focusRequester = remember { FocusRequester() }
    val context = LocalContext.current
    val scrollState = rememberScrollState()

    LaunchedEffect(Unit) {
        focusRequester.requestFocus()
    }

    val submitTask = {
        val trimmed = taskText.trim()
        if (trimmed.isNotEmpty()) {
            val trimmedNotes = notesText.trim().takeIf { it.isNotEmpty() }
            onAddTodo(trimmed, trimmedNotes, startTime, endTime)
        }
    }

    val openTimePicker = { isStart: Boolean ->
        val calendar = Calendar.getInstance()
        val hour = calendar.get(Calendar.HOUR_OF_DAY)
        val minute = calendar.get(Calendar.MINUTE)

        TimePickerDialog(
            context,
            { _, pickedHour, pickedMinute ->
                val isPm = pickedHour >= 12
                val hour12 = when {
                    pickedHour == 0 -> 12
                    pickedHour > 12 -> pickedHour - 12
                    else -> pickedHour
                }
                val amPm = if (isPm) "PM" else "AM"
                val formatted = String.format(Locale.US, "%02d:%02d %s", hour12, pickedMinute, amPm)
                if (isStart) {
                    startTime = formatted
                } else {
                    endTime = formatted
                }
            },
            hour,
            minute,
            false
        ).show()
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        // Minimal Top Header
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 4.dp, end = 12.dp, top = 4.dp, bottom = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(
                onClick = onBack,
                modifier = Modifier.size(38.dp)
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Back",
                    tint = MaterialTheme.colorScheme.onBackground,
                    modifier = Modifier.size(20.dp)
                )
            }

            Text(
                text = "new task",
                fontFamily = MaliFontFamily,
                fontWeight = FontWeight.Medium,
                fontSize = 18.sp,
                color = MaterialTheme.colorScheme.onBackground,
                modifier = Modifier.padding(start = 2.dp)
            )
        }

        HorizontalDivider(
            thickness = 0.5.dp,
            color = MaterialTheme.colorScheme.outline
        )

        // Scrollable Form Container so nothing gets cut off
        Column(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .verticalScroll(scrollState)
                .padding(horizontal = 14.dp, vertical = 14.dp)
        ) {
            Text(
                text = "task",
                fontFamily = MaliFontFamily,
                fontSize = 12.sp,
                fontStyle = FontStyle.Italic,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(4.dp))

            // Unboxed Clean Input Field
            BasicTextField(
                value = taskText,
                onValueChange = { taskText = it },
                textStyle = TextStyle(
                    fontFamily = MaliFontFamily,
                    fontSize = 17.sp,
                    color = MaterialTheme.colorScheme.onBackground
                ),
                cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                modifier = Modifier
                    .fillMaxWidth()
                    .focusRequester(focusRequester),
                decorationBox = { innerTextField ->
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 6.dp)
                    ) {
                        if (taskText.isEmpty()) {
                            Text(
                                text = "what needs to be done?",
                                fontFamily = MaliFontFamily,
                                fontSize = 17.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.45f)
                            )
                        }
                        innerTextField()
                    }
                }
            )

            // Sleek underline
            HorizontalDivider(
                thickness = 1.dp,
                color = if (taskText.isNotEmpty()) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline
            )

            Spacer(modifier = Modifier.height(20.dp))

            // Stacked Start and End Time Rows (Row 1: start + time, Row 2: end + time)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "schedule (optional)",
                    fontFamily = MaliFontFamily,
                    fontSize = 12.sp,
                    fontStyle = FontStyle.Italic,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                if (startTime != null || endTime != null) {
                    Text(
                        text = "long press to clear",
                        fontFamily = MaliFontFamily,
                        fontSize = 10.sp,
                        fontStyle = FontStyle.Italic,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Line 1: start + time in same row
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .border(BorderStroke(0.8.dp, MaterialTheme.colorScheme.outline), shape = RoundedCornerShape(4.dp))
                    .background(MaterialTheme.colorScheme.surface, shape = RoundedCornerShape(4.dp))
                    .combinedClickable(
                        onClick = { openTimePicker(true) },
                        onLongClick = {
                            if (startTime != null) {
                                startTime = null
                                Toast.makeText(context, "Start time cleared", Toast.LENGTH_SHORT).show()
                            }
                        }
                    )
                    .padding(horizontal = 12.dp, vertical = 10.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "start",
                    fontFamily = MaliFontFamily,
                    fontWeight = FontWeight.Medium,
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = startTime ?: "set time",
                    fontFamily = MaliFontFamily,
                    fontWeight = if (startTime != null) FontWeight.Medium else FontWeight.Normal,
                    fontSize = 13.sp,
                    color = if (startTime != null) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.55f)
                )
            }

            Spacer(modifier = Modifier.height(6.dp))

            // Line 2: end + time in next row
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .border(BorderStroke(0.8.dp, MaterialTheme.colorScheme.outline), shape = RoundedCornerShape(4.dp))
                    .background(MaterialTheme.colorScheme.surface, shape = RoundedCornerShape(4.dp))
                    .combinedClickable(
                        onClick = { openTimePicker(false) },
                        onLongClick = {
                            if (endTime != null) {
                                endTime = null
                                Toast.makeText(context, "End time cleared", Toast.LENGTH_SHORT).show()
                            }
                        }
                    )
                    .padding(horizontal = 12.dp, vertical = 10.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "end",
                    fontFamily = MaliFontFamily,
                    fontWeight = FontWeight.Medium,
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = endTime ?: "set time",
                    fontFamily = MaliFontFamily,
                    fontWeight = if (endTime != null) FontWeight.Medium else FontWeight.Normal,
                    fontSize = 13.sp,
                    color = if (endTime != null) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.55f)
                )
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Notes Section at the bottom of the form
            Text(
                text = "notes (optional)",
                fontFamily = MaliFontFamily,
                fontSize = 12.sp,
                fontStyle = FontStyle.Italic,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(4.dp))

            BasicTextField(
                value = notesText,
                onValueChange = { notesText = it },
                textStyle = TextStyle(
                    fontFamily = MaliFontFamily,
                    fontSize = 14.sp,
                    lineHeight = 20.sp,
                    color = MaterialTheme.colorScheme.onBackground
                ),
                cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                minLines = 3,
                maxLines = 6,
                modifier = Modifier
                    .fillMaxWidth()
                    .border(
                        BorderStroke(0.8.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.6f)),
                        shape = RoundedCornerShape(4.dp)
                    )
                    .background(MaterialTheme.colorScheme.surface, shape = RoundedCornerShape(4.dp))
                    .padding(10.dp),
                decorationBox = { innerTextField ->
                    Box(modifier = Modifier.fillMaxWidth()) {
                        if (notesText.isEmpty()) {
                            Text(
                                text = "add notes...",
                                fontFamily = MaliFontFamily,
                                fontSize = 13.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.45f)
                            )
                        }
                        innerTextField()
                    }
                }
            )

            Spacer(modifier = Modifier.height(16.dp))
        }

        // Attached Bottom iOS-style Slide to Add Button
        Surface(
            modifier = Modifier.fillMaxWidth(),
            color = MaterialTheme.colorScheme.background,
            tonalElevation = 2.dp
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 14.dp, vertical = 10.dp)
            ) {
                SlideToAddButton(
                    enabled = taskText.trim().isNotEmpty(),
                    onSlideComplete = {
                        submitTask()
                    }
                )
            }
        }
    }
}

/**
 * iOS-Style Full Width "Slide to Add" Button
 */
@Composable
private fun SlideToAddButton(
    enabled: Boolean,
    onSlideComplete: () -> Unit,
    modifier: Modifier = Modifier
) {
    val density = LocalDensity.current
    val scope = rememberCoroutineScope()
    val dragOffsetX = remember { Animatable(0f) }

    BoxWithConstraints(
        modifier = modifier
            .fillMaxWidth()
            .height(52.dp)
            .clip(RoundedCornerShape(26.dp))
            .background(
                if (enabled) MaterialTheme.colorScheme.surfaceVariant else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
            )
            .border(
                BorderStroke(
                    1.dp,
                    if (enabled) MaterialTheme.colorScheme.primary.copy(alpha = 0.4f) else MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)
                ),
                shape = RoundedCornerShape(26.dp)
            ),
        contentAlignment = Alignment.CenterStart
    ) {
        val maxDragWidth = with(density) { (maxWidth - 52.dp).toPx() }

        // Track Shimmering Text
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            val textAlpha = if (!enabled) 0.3f else (1f - (dragOffsetX.value / maxDragWidth).coerceIn(0f, 1f))
            Text(
                text = "slide to add to today",
                fontFamily = MaliFontFamily,
                fontWeight = FontWeight.Medium,
                fontSize = 14.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = textAlpha)
            )
        }

        // Draggable Thumb Button
        Box(
            modifier = Modifier
                .offset { IntOffset(dragOffsetX.value.roundToInt(), 0) }
                .size(52.dp)
                .padding(3.dp)
                .clip(CircleShape)
                .background(
                    if (enabled) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.3f)
                )
                .draggable(
                    enabled = enabled,
                    orientation = Orientation.Horizontal,
                    state = rememberDraggableState { delta ->
                        scope.launch {
                            val next = (dragOffsetX.value + delta).coerceIn(0f, maxDragWidth)
                            dragOffsetX.snapTo(next)
                        }
                    },
                    onDragStopped = {
                        scope.launch {
                            if (dragOffsetX.value > maxDragWidth * 0.72f) {
                                // Complete drag
                                dragOffsetX.animateTo(
                                    maxDragWidth,
                                    spring(dampingRatio = Spring.DampingRatioNoBouncy, stiffness = Spring.StiffnessMedium)
                                )
                                onSlideComplete()
                            } else {
                                // Snap back with iOS spring
                                dragOffsetX.animateTo(
                                    0f,
                                    spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessLow)
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
                tint = if (enabled) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.surface,
                modifier = Modifier.size(20.dp)
            )
        }
    }
}
