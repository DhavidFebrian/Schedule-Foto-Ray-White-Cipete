package com.example.ui.screens

import android.app.DatePickerDialog
import android.content.Intent
import android.net.Uri
import com.example.ui.AgentInfo
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.animation.*
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.zIndex
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import coil.compose.AsyncImage
import com.example.data.Schedule
import com.example.ui.ScheduleViewModel
import com.example.ui.getAgentPhoneByName
import androidx.compose.foundation.Canvas
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.unit.Velocity
import com.example.ui.SyncState
import java.text.SimpleDateFormat
import java.util.*

import com.example.ui.ScheduleCategory

fun formatIndonesianDate(rawDate: String): String {
    return try {
        val parser = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        val output = SimpleDateFormat("EEEE, d MMMM yyyy", Locale("id", "ID"))
        parser.parse(rawDate.trim())?.let { output.format(it) } ?: rawDate
    } catch (e: Exception) {
        rawDate
    }
}

fun formatTwelveHourTime(rawTime: String): String {
    return try {
        val parser = SimpleDateFormat("HH:mm", Locale.getDefault())
        val output = SimpleDateFormat("hh:mm a", Locale.US)
        parser.parse(rawTime.trim())?.let { output.format(it).uppercase() } ?: rawTime
    } catch (e: Exception) {
        try {
            val parser = SimpleDateFormat("HH:mm:ss", Locale.getDefault())
            val output = SimpleDateFormat("hh:mm a", Locale.US)
            parser.parse(rawTime.trim())?.let { output.format(it).uppercase() } ?: rawTime
        } catch (e2: Exception) {
            rawTime
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun DashboardScreen(
    viewModel: ScheduleViewModel,
    onNavigateToForm: () -> Unit,
    onOpenDrawer: () -> Unit,
    modifier: Modifier = Modifier
) {
    val schedules by viewModel.filteredSchedules.collectAsStateWithLifecycle()
    val allSchedules by viewModel.allSchedules.collectAsStateWithLifecycle()
    val staffOptions by viewModel.staffList.collectAsStateWithLifecycle()
    
    val searchQuery by viewModel.searchQuery.collectAsStateWithLifecycle()
    val filterStaff by viewModel.filterStaff.collectAsStateWithLifecycle()
    val filterStartDate by viewModel.filterStartDate.collectAsStateWithLifecycle()
    val filterEndDate by viewModel.filterEndDate.collectAsStateWithLifecycle()
    val syncStatus by viewModel.syncStatus.collectAsStateWithLifecycle()
    val listingImagesMap by viewModel.listingImagesMap.collectAsStateWithLifecycle()
    val listingImagesGalleryMap by viewModel.listingImagesGalleryMap.collectAsStateWithLifecycle()
    val agentInfoMap by viewModel.agentInfoMap.collectAsStateWithLifecycle()

    val selectedCategory by viewModel.selectedCategory.collectAsStateWithLifecycle()
    var selectedScheduleForDetail by remember { mutableStateOf<Schedule?>(null) }
    var selectedScheduleForFollowUp by remember { mutableStateOf<Schedule?>(null) }
    var showCalendarView by remember { mutableStateOf(false) }
    var selectedCalendarDate by remember { mutableStateOf<String?>(
        SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
    ) }

    val categorizedSchedules = remember(schedules, selectedCategory) {
        schedules.filter { item ->
            val statusLower = item.status.lowercase().trim()
            val typeLower = item.type.lowercase().trim()
            val isSelesai = typeLower.startsWith("done")
            val isAktif = !typeLower.startsWith("done") && typeLower.isNotBlank() && item.tanggal.trim().isNotBlank()
            val isTask = typeLower.startsWith("done") && statusLower != "done"

            when (selectedCategory) {
                ScheduleCategory.SEMUA -> true
                ScheduleCategory.AKTIF -> isAktif
                ScheduleCategory.SELESAI -> isSelesai
                ScheduleCategory.TASK -> isTask
            }
        }
    }

    val context = LocalContext.current
    var showFilterSheet by remember { mutableStateOf(false) }

    // Date picker helpers
    val calendar = Calendar.getInstance()
    val dateFormatter = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())

    val startDatePicker = DatePickerDialog(
        context,
        { _, year, month, dayOfMonth ->
            val cal = Calendar.getInstance().apply { set(year, month, dayOfMonth) }
            viewModel.filterStartDate.value = dateFormatter.format(cal.time)
        },
        calendar.get(Calendar.YEAR),
        calendar.get(Calendar.MONTH),
        calendar.get(Calendar.DAY_OF_MONTH)
    )

    val endDatePicker = DatePickerDialog(
        context,
        { _, year, month, dayOfMonth ->
            val cal = Calendar.getInstance().apply { set(year, month, dayOfMonth) }
            viewModel.filterEndDate.value = dateFormatter.format(cal.time)
        },
        calendar.get(Calendar.YEAR),
        calendar.get(Calendar.MONTH),
        calendar.get(Calendar.DAY_OF_MONTH)
    )

    Scaffold(
        modifier = modifier,
        topBar = {
            CenterAlignedTopAppBar(
                navigationIcon = {
                    IconButton(onClick = onOpenDrawer) {
                        Icon(imageVector = Icons.Default.Menu, contentDescription = "Menu")
                    }
                },
                title = {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = "SCHEDULE FOTO RWC",
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 2.sp
                            ),
                            color = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            text = "Raffa - David",
                            style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                },
                actions = {
                    IconButton(
                        onClick = { viewModel.retryUnsyncedSchedules() },
                        modifier = Modifier.testTag("retry_sync_button")
                    ) {
                        val hasPending = allSchedules.any { !it.synced }
                        Icon(
                            imageVector = if (hasPending) Icons.Default.CloudUpload else Icons.Default.CloudDone,
                            contentDescription = "Sync Queue",
                            tint = if (hasPending) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.outline
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = {
                    viewModel.resetForm()
                    onNavigateToForm()
                },
                icon = { Icon(Icons.Default.Add, contentDescription = "Tambah") },
                text = { Text("Jadwal Baru") },
                containerColor = MaterialTheme.colorScheme.primaryContainer,
                contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                modifier = Modifier
                    .navigationBarsPadding()
                    .testTag("add_schedule_fab")
            )
        }
    ) { paddingValues ->
        val listState = androidx.compose.foundation.lazy.rememberLazyListState()
        var swipeOffset by remember { mutableStateOf(0f) }
        val nestedScrollConnection = remember(listState) {
            object : NestedScrollConnection {
                override fun onPreScroll(available: Offset, source: NestedScrollSource): Offset {
                    // Pulling up reduces swipeOffset first if it has visual drag offset
                    if (available.y < 0 && swipeOffset > 0f) {
                        val consumed = available.y.coerceAtLeast(-swipeOffset)
                        swipeOffset += consumed
                        return Offset(0f, consumed)
                    }
                    // Pulling down while already scrolled to the top of list: intercept immediately
                    if (available.y > 0 && swipeOffset > 0f && listState.firstVisibleItemIndex == 0 && listState.firstVisibleItemScrollOffset == 0) {
                        if (swipeOffset < 300f) {
                            swipeOffset = (swipeOffset + available.y * 0.4f).coerceAtMost(300f)
                            return Offset(0f, available.y)
                        }
                    }
                    return Offset.Zero
                }

                override fun onPostScroll(
                    consumed: Offset,
                    available: Offset,
                    source: NestedScrollSource
                ): Offset {
                    // Pulling down more when LazyColumn is fully at the top but couldn't consume the drag
                    if (available.y > 0 && listState.firstVisibleItemIndex == 0 && listState.firstVisibleItemScrollOffset == 0) {
                        swipeOffset = (swipeOffset + available.y * 0.4f).coerceAtMost(300f)
                        return Offset(0f, available.y)
                    }
                    return Offset.Zero
                }

                override suspend fun onPreFling(available: Velocity): Velocity {
                    if (swipeOffset > 150f) {
                        viewModel.syncData()
                    }
                    swipeOffset = 0f
                    return Velocity.Zero
                }
            }
        }

        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .background(MaterialTheme.colorScheme.surface)
                .nestedScroll(nestedScrollConnection)
        ) {
            // Modern Pull-to-refresh Indicator Banner overlayed at the top
            AnimatedVisibility(
                visible = swipeOffset > 20f || syncStatus is SyncState.Loading,
                enter = expandVertically() + fadeIn(),
                exit = shrinkVertically() + fadeOut(),
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.TopCenter)
                    .zIndex(10f)
            ) {
                Surface(
                    color = MaterialTheme.colorScheme.primaryContainer,
                    contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                    shape = RoundedCornerShape(bottomStart = 16.dp, bottomEnd = 16.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 24.dp),
                    shadowElevation = 4.dp
                ) {
                    Row(
                        modifier = Modifier
                            .padding(12.dp)
                            .fillMaxWidth(),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        if (syncStatus is SyncState.Loading) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(16.dp),
                                strokeWidth = 2.dp,
                                color = MaterialTheme.colorScheme.primary
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                "Menghubungkan ke Google Sheets...",
                                style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold)
                            )
                        } else {
                            Icon(
                                imageVector = if (swipeOffset > 150f) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = if (swipeOffset > 150f) "Lepaskan untuk sinkronisasi" else "Tarik ke bawah untuk menyegarkan",
                                style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold)
                            )
                        }
                    }
                }
            }

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 16.dp)
            ) {
                // Sync notification overlay banner
                AnimatedVisibility(
                    visible = syncStatus !is SyncState.Idle,
                    enter = fadeIn() + slideInVertically(),
                    exit = fadeOut() + slideOutVertically()
                ) {
                    Surface(
                        color = when (syncStatus) {
                            is SyncState.Loading -> MaterialTheme.colorScheme.secondaryContainer
                            is SyncState.Success -> MaterialTheme.colorScheme.primaryContainer
                            is SyncState.Error -> MaterialTheme.colorScheme.errorContainer
                            else -> MaterialTheme.colorScheme.surfaceVariant
                        },
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 8.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)
                        ) {
                            when (syncStatus) {
                                is SyncState.Loading -> {
                                    CircularProgressIndicator(
                                        modifier = Modifier.size(20.dp),
                                        strokeWidth = 2.dp,
                                        color = MaterialTheme.colorScheme.secondary
                                    )
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Text(
                                        text = "Menghubungkan ke Google Sheets...",
                                        style = MaterialTheme.typography.bodyMedium
                                    )
                                }
                                is SyncState.Success -> {
                                    Icon(
                                        imageVector = Icons.Default.CheckCircle,
                                        contentDescription = "Success",
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(20.dp)
                                    )
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Text(
                                        text = (syncStatus as SyncState.Success).message,
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = FontWeight.Bold,
                                        modifier = Modifier.weight(1f)
                                    )
                                    IconButton(
                                        onClick = { viewModel.dismissSyncStatus() },
                                        modifier = Modifier.size(24.dp)
                                    ) {
                                        Icon(Icons.Default.Close, contentDescription = "Close", modifier = Modifier.size(16.dp))
                                    }
                                }
                                is SyncState.Error -> {
                                    Icon(
                                        imageVector = Icons.Default.Error,
                                        contentDescription = "Error",
                                        tint = MaterialTheme.colorScheme.error,
                                        modifier = Modifier.size(20.dp)
                                    )
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Text(
                                        text = (syncStatus as SyncState.Error).message,
                                        color = MaterialTheme.colorScheme.onErrorContainer,
                                        style = MaterialTheme.typography.bodyMedium,
                                        modifier = Modifier.weight(1f),
                                        maxLines = 2,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    IconButton(
                                        onClick = { viewModel.dismissSyncStatus() },
                                        modifier = Modifier.size(24.dp)
                                    ) {
                                        Icon(Icons.Default.Close, contentDescription = "Close", modifier = Modifier.size(16.dp))
                                    }
                                }
                                else -> {}
                            }
                        }
                    }
                }

                // 1. Mode Tampilan Switcher (remains fixed/static at the top so user can toggle anytime)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Mode Tampilan:",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Row(
                        modifier = Modifier
                            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f), RoundedCornerShape(100.dp))
                            .padding(2.dp),
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        val selectedBgColor = MaterialTheme.colorScheme.primaryContainer
                        val unselectedBgColor = Color.Transparent
                        val selectedContentColor = MaterialTheme.colorScheme.onPrimaryContainer
                        val unselectedContentColor = MaterialTheme.colorScheme.onSurfaceVariant
                        
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(100.dp))
                                .background(if (!showCalendarView) selectedBgColor else unselectedBgColor)
                                .clickable { showCalendarView = false }
                                .padding(horizontal = 14.dp, vertical = 6.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Menu,
                                    contentDescription = null,
                                    modifier = Modifier.size(14.dp),
                                    tint = if (!showCalendarView) selectedContentColor else unselectedContentColor
                                )
                                Text(
                                    text = "Daftar",
                                    style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold),
                                    color = if (!showCalendarView) selectedContentColor else unselectedContentColor
                                )
                            }
                        }
                        
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(100.dp))
                                .background(if (showCalendarView) selectedBgColor else unselectedBgColor)
                                .clickable { showCalendarView = true }
                                .padding(horizontal = 14.dp, vertical = 6.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.DateRange,
                                    contentDescription = null,
                                    modifier = Modifier.size(14.dp),
                                    tint = if (showCalendarView) selectedContentColor else unselectedContentColor
                                )
                                Text(
                                    text = "Kalender",
                                    style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold),
                                    color = if (showCalendarView) selectedContentColor else unselectedContentColor
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                if (!showCalendarView) {
                    LazyColumn(
                        state = listState,
                        modifier = Modifier
                            .fillMaxSize()
                            .weight(1f)
                            .testTag("schedule_list"),
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                        contentPadding = PaddingValues(bottom = 80.dp)
                    ) {
                        // A. Header Stats Cards (Redesigned, Symmetric, and Ultra-Modern) - Item 1 of LazyColumn
                        item {
                            Spacer(modifier = Modifier.height(4.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                // Card 1: Total Jadwal
                                Card(
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(105.dp)
                                        .clickable { viewModel.selectedCategory.value = ScheduleCategory.SEMUA },
                                    colors = CardDefaults.cardColors(
                                        containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.25f)
                                    ),
                                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.3f)),
                                    shape = RoundedCornerShape(18.dp)
                                ) {
                                    Column(
                                        modifier = Modifier
                                            .fillMaxSize()
                                            .padding(12.dp),
                                        verticalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.CalendarToday,
                                                contentDescription = null,
                                                tint = MaterialTheme.colorScheme.primary,
                                                modifier = Modifier.size(20.dp)
                                            )
                                            Box(
                                                modifier = Modifier
                                                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.15f), CircleShape)
                                                    .size(8.dp)
                                            )
                                        }
                                        Column {
                                            Text(
                                                text = "Total Jadwal",
                                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Medium),
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                            Text(
                                                text = "${allSchedules.size}",
                                                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.ExtraBold),
                                                color = MaterialTheme.colorScheme.primary
                                            )
                                        }
                                    }
                                }

                                // Card 2: Jadwal Aktif
                                val activeCount = allSchedules.count { 
                                    val typeLower = it.type.lowercase().trim()
                                    !typeLower.startsWith("done") && typeLower.isNotBlank() && it.tanggal.trim().isNotBlank()
                                }
                                Card(
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(105.dp)
                                        .clickable { viewModel.selectedCategory.value = ScheduleCategory.AKTIF },
                                    colors = CardDefaults.cardColors(
                                        containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.25f)
                                    ),
                                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.secondary.copy(alpha = 0.3f)),
                                    shape = RoundedCornerShape(18.dp)
                                ) {
                                    Column(
                                        modifier = Modifier
                                            .fillMaxSize()
                                            .padding(12.dp),
                                        verticalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.FlashOn,
                                                contentDescription = null,
                                                tint = MaterialTheme.colorScheme.secondary,
                                                modifier = Modifier.size(20.dp)
                                            )
                                            Box(
                                                modifier = Modifier
                                                    .background(MaterialTheme.colorScheme.secondary.copy(alpha = 0.15f), CircleShape)
                                                    .size(8.dp)
                                            )
                                        }
                                        Column {
                                            Text(
                                                text = "Jadwal Aktif",
                                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Medium),
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                            Text(
                                                text = "$activeCount",
                                                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.ExtraBold),
                                                color = MaterialTheme.colorScheme.secondary
                                            )
                                        }
                                    }
                                }

                                // Card 3: Task
                                val taskCount = allSchedules.count { 
                                    val typeLower = it.type.lowercase().trim()
                                    val statusLower = it.status.lowercase().trim()
                                    typeLower.startsWith("done") && statusLower != "done"
                                }
                                Card(
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(105.dp)
                                        .clickable { viewModel.selectedCategory.value = ScheduleCategory.TASK },
                                    colors = CardDefaults.cardColors(
                                        containerColor = MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.25f)
                                    ),
                                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.tertiary.copy(alpha = 0.3f)),
                                    shape = RoundedCornerShape(18.dp)
                                ) {
                                    Column(
                                        modifier = Modifier
                                            .fillMaxSize()
                                            .padding(12.dp),
                                        verticalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.SpaceDashboard,
                                                contentDescription = null,
                                                tint = MaterialTheme.colorScheme.tertiary,
                                                modifier = Modifier.size(20.dp)
                                            )
                                            Box(
                                                modifier = Modifier
                                                    .background(MaterialTheme.colorScheme.tertiary.copy(alpha = 0.15f), CircleShape)
                                                    .size(8.dp)
                                            )
                                        }
                                        Column {
                                            Text(
                                                text = "Dashboard Task",
                                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Medium),
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                            Text(
                                                text = "$taskCount",
                                                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.ExtraBold),
                                                color = MaterialTheme.colorScheme.tertiary
                                            )
                                        }
                                    }
                                }
                            }
                        }



                        // B. Search & Filter Row - Item 2 of LazyColumn
                        item {
                            val isFilterActive = filterStaff != "Semua" || filterStartDate != null || filterEndDate != null
                            Spacer(modifier = Modifier.height(8.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                OutlinedTextField(
                                    value = searchQuery,
                                    onValueChange = { viewModel.searchQuery.value = it },
                                    modifier = Modifier
                                        .weight(1f)
                                        .testTag("search_field"),
                                    placeholder = { Text("Cari Lokasi, ME, atau ID...") },
                                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = "Search Icon") },
                                    trailingIcon = {
                                        if (searchQuery.isNotEmpty()) {
                                            IconButton(onClick = { viewModel.searchQuery.value = "" }) {
                                                Icon(Icons.Default.Clear, contentDescription = "Clear search")
                                            }
                                        }
                                    },
                                    singleLine = true,
                                    shape = RoundedCornerShape(12.dp)
                                )

                                Button(
                                    onClick = { showFilterSheet = !showFilterSheet },
                                    shape = RoundedCornerShape(12.dp),
                                    modifier = Modifier
                                        .height(56.dp)
                                        .testTag("filter_button"),
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = if (isFilterActive) MaterialTheme.colorScheme.tertiary 
                                                         else MaterialTheme.colorScheme.secondaryContainer,
                                        contentColor = if (isFilterActive) MaterialTheme.colorScheme.onTertiary 
                                                       else MaterialTheme.colorScheme.onSecondaryContainer
                                    )
                                ) {
                                    Badge(containerColor = if (isFilterActive) MaterialTheme.colorScheme.onTertiary else MaterialTheme.colorScheme.tertiary) {
                                        Icon(
                                            imageVector = if (isFilterActive) Icons.Default.FilterListOff else Icons.Default.FilterList,
                                            contentDescription = "Toggle Filter"
                                        )
                                    }
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(text = "Saring", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
                                }
                            }
                        }

                        // C. Animated Filters panel - Item 3 of LazyColumn
                        item {
                            val isFilterActive = filterStaff != "Semua" || filterStartDate != null || filterEndDate != null
                            AnimatedVisibility(
                                visible = showFilterSheet,
                                enter = expandVertically() + fadeIn(),
                                exit = shrinkVertically() + fadeOut()
                            ) {
                                Card(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 4.dp)
                                        .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(12.dp)),
                                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f))
                                ) {
                                    Column(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(16.dp)
                                    ) {
                                        Text(
                                            text = "Saring Berdasarkan",
                                            style = MaterialTheme.typography.titleSmall,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                        Spacer(modifier = Modifier.height(10.dp))

                                        // Staff filter
                                        Text(text = "Pilih Staff:", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.outline)
                                        Spacer(modifier = Modifier.height(4.dp))
                                        ScrollableTabRow(
                                            selectedTabIndex = staffOptions.indexOf(filterStaff).coerceAtLeast(0),
                                            edgePadding = 0.dp,
                                            containerColor = Color.Transparent,
                                            indicator = {},
                                            divider = {}
                                        ) {
                                            staffOptions.forEach { staff ->
                                                val isSelected = filterStaff == staff
                                                FilterChip(
                                                    selected = isSelected,
                                                    onClick = { viewModel.filterStaff.value = staff },
                                                    label = { Text(staff) },
                                                    modifier = Modifier.padding(horizontal = 4.dp),
                                                    shape = RoundedCornerShape(18.dp)
                                                )
                                            }
                                        }

                                        Spacer(modifier = Modifier.height(12.dp))

                                        // Date Range filter
                                        Text(text = "Rentang Tanggal:", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.outline)
                                        Spacer(modifier = Modifier.height(6.dp))
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            // Start Date
                                            OutlinedCard(
                                                modifier = Modifier
                                                    .weight(1f)
                                                    .clickable { startDatePicker.show() },
                                                shape = RoundedCornerShape(8.dp)
                                            ) {
                                                Row(
                                                    modifier = Modifier
                                                        .fillMaxWidth()
                                                        .padding(horizontal = 12.dp, vertical = 10.dp),
                                                    verticalAlignment = Alignment.CenterVertically,
                                                    horizontalArrangement = Arrangement.SpaceBetween
                                                ) {
                                                    Text(
                                                        text = filterStartDate ?: "Mulai",
                                                        style = MaterialTheme.typography.bodyMedium,
                                                        color = if (filterStartDate != null) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.outline
                                                    )
                                                    Icon(Icons.Default.CalendarToday, contentDescription = null, modifier = Modifier.size(16.dp))
                                                }
                                            }

                                            Text("-", fontWeight = FontWeight.Bold)

                                            // End Date
                                            OutlinedCard(
                                                modifier = Modifier
                                                    .weight(1f)
                                                    .clickable { endDatePicker.show() },
                                                shape = RoundedCornerShape(8.dp)
                                            ) {
                                                Row(
                                                    modifier = Modifier
                                                        .fillMaxWidth()
                                                        .padding(horizontal = 12.dp, vertical = 10.dp),
                                                    verticalAlignment = Alignment.CenterVertically,
                                                    horizontalArrangement = Arrangement.SpaceBetween
                                                ) {
                                                    Text(
                                                        text = filterEndDate ?: "Selesai",
                                                        style = MaterialTheme.typography.bodyMedium,
                                                        color = if (filterEndDate != null) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.outline
                                                    )
                                                    Icon(Icons.Default.CalendarToday, contentDescription = null, modifier = Modifier.size(16.dp))
                                                }
                                            }

                                            // Clear range
                                            if (filterStartDate != null || filterEndDate != null) {
                                                IconButton(
                                                    onClick = {
                                                        viewModel.filterStartDate.value = null
                                                        viewModel.filterEndDate.value = null
                                                    },
                                                    modifier = Modifier.size(36.dp)
                                                ) {
                                                    Icon(Icons.Default.History, contentDescription = "Reset Date Range", tint = MaterialTheme.colorScheme.error)
                                                }
                                            }
                                        }

                                        Spacer(modifier = Modifier.height(14.dp))

                                        if (isFilterActive) {
                                            TextButton(
                                                onClick = {
                                                    viewModel.filterStaff.value = "Semua"
                                                    viewModel.filterStartDate.value = null
                                                    viewModel.filterEndDate.value = null
                                                    viewModel.searchQuery.value = ""
                                                },
                                                modifier = Modifier.align(Alignment.End)
                                            ) {
                                                Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(16.dp))
                                                Spacer(modifier = Modifier.width(4.dp))
                                                Text("Reset Semua Saringan")
                                            }
                                        }
                                    }
                                }
                            }
                        }

                        // D. Sticky Header for category tabs
                        stickyHeader {
                            Surface(
                                modifier = Modifier.fillMaxWidth(),
                                color = MaterialTheme.colorScheme.surface,
                                shadowElevation = 1.dp
                            ) {
                                Column(modifier = Modifier.fillMaxWidth()) {
                                    Spacer(modifier = Modifier.height(10.dp))
                                    ScrollableTabRow(
                                        selectedTabIndex = selectedCategory.ordinal,
                                        edgePadding = 0.dp,
                                        containerColor = Color.Transparent,
                                        indicator = { tabPositions ->
                                            TabRowDefaults.SecondaryIndicator(
                                                Modifier.tabIndicatorOffset(tabPositions[selectedCategory.ordinal]),
                                                color = MaterialTheme.colorScheme.primary
                                            )
                                        },
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        ScheduleCategory.values().forEach { category ->
                                            val isSelected = selectedCategory == category
                                            val categoryCount = remember(schedules, category) {
                                                schedules.count { item ->
                                                    val statusLower = item.status.lowercase().trim()
                                                    val typeLower = item.type.lowercase().trim()
                                                    val isSelesai = typeLower.startsWith("done")
                                                    val isAktif = !typeLower.startsWith("done") && typeLower.isNotBlank() && item.tanggal.trim().isNotBlank()
                                                    val isTask = typeLower.startsWith("done") && statusLower != "done"

                                                    when (category) {
                                                        ScheduleCategory.SEMUA -> true
                                                        ScheduleCategory.AKTIF -> isAktif
                                                        ScheduleCategory.SELESAI -> isSelesai
                                                        ScheduleCategory.TASK -> isTask
                                                    }
                                                }
                                            }
                                            Tab(
                                                selected = isSelected,
                                                onClick = { viewModel.selectedCategory.value = category },
                                                text = {
                                                    Text(
                                                        text = "${category.displayName} ($categoryCount)",
                                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                                        color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                                                        style = MaterialTheme.typography.bodyMedium
                                                    )
                                                }
                                            )
                                        }
                                    }
                                    Spacer(modifier = Modifier.height(12.dp))
                                }
                            }
                        }

                        // E. Section Header label - Item 4 of LazyColumn
                        item {
                            Spacer(modifier = Modifier.height(2.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "Agenda: ${selectedCategory.displayName} (${categorizedSchedules.size})",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                if (schedules.size != allSchedules.size) {
                                    Text(
                                        text = "Disaring",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.tertiary,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.height(6.dp))
                        }

                        // F. Inner list of entries - Items of LazyColumn
                        if (categorizedSchedules.isEmpty()) {
                            item {
                                Card(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 16.dp),
                                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.2f)),
                                    shape = RoundedCornerShape(24.dp)
                                ) {
                                    Column(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(24.dp),
                                        horizontalAlignment = Alignment.CenterHorizontally,
                                        verticalArrangement = Arrangement.Center
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .size(72.dp)
                                                .background(
                                                    MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.5f),
                                                    CircleShape
                                                ),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.EventNote,
                                                contentDescription = "No Events icon",
                                                tint = MaterialTheme.colorScheme.secondary,
                                                modifier = Modifier.size(36.dp)
                                            )
                                        }
                                        Spacer(modifier = Modifier.height(16.dp))
                                        Text(
                                            text = "Tidak Ada Jadwal (" + selectedCategory.displayName + ")",
                                            style = MaterialTheme.typography.titleMedium,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                        Spacer(modifier = Modifier.height(6.dp))
                                        Text(
                                            text = "Tidak ditemukan jadwal pemotretan untuk kategori ini.",
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = MaterialTheme.colorScheme.outline,
                                            textAlign = TextAlign.Center
                                        )
                                    }
                                }
                            }
                        } else {
                            items(categorizedSchedules, key = { it.id }) { schedule ->
                                ScheduleRowItem(
                                    schedule = schedule,
                                    listingImagesMap = listingImagesMap,
                                    onFetchImage = { id -> viewModel.fetchListingImageIfNeeded(id, schedule.namaMe) },
                                    onEditClick = {
                                        viewModel.startEditing(schedule)
                                        onNavigateToForm()
                                    },
                                    onFollowUpClick = {
                                        selectedScheduleForFollowUp = schedule
                                    },
                                    onClick = { selectedScheduleForDetail = schedule }
                                )
                            }
                        }
                    }
                } else {
                    // CALENDAR VIEW (Adaptive layout, takes up full height, hides the stats cards)
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .weight(1f)
                            .verticalScroll(rememberScrollState())
                    ) {
                        Spacer(modifier = Modifier.height(10.dp))

                        DashboardCalendarView(
                            schedules = allSchedules,
                            selectedDate = selectedCalendarDate,
                            onDateSelected = { selectedCalendarDate = it }
                        )

                        Spacer(modifier = Modifier.height(14.dp))

                        // Header for selected date schedules
                        val dateObj = remember(selectedCalendarDate) {
                            try {
                                selectedCalendarDate?.let {
                                    SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).parse(it)
                                }
                            } catch (e: Exception) { null }
                        }
                        val displayDateText = remember(dateObj, selectedCalendarDate) {
                            if (dateObj != null) {
                                SimpleDateFormat("d MMMM yyyy", Locale("id", "ID")).format(dateObj)
                            } else {
                                selectedCalendarDate ?: "Pilih Tanggal"
                            }
                        }

                        val dateSchedules = remember(allSchedules, selectedCalendarDate) {
                            val targetDate = selectedCalendarDate?.let { com.example.data.normalizeDate(it) } ?: ""
                            allSchedules.filter { com.example.data.normalizeDate(it.tanggal) == targetDate }
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Jadwal: $displayDateText (${dateSchedules.size})",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        if (dateSchedules.isEmpty()) {
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 12.dp),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.15f)),
                                shape = RoundedCornerShape(16.dp)
                            ) {
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(20.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Event,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.outline.copy(alpha = 0.7f),
                                        modifier = Modifier.size(40.dp)
                                    )
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Text(
                                        text = "Tidak Ada Jadwal",
                                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                                        color = MaterialTheme.colorScheme.outline
                                    )
                                    Text(
                                        text = "Silakan pilih tanggal lain yang memiliki tanda titik indikator.",
                                        style = MaterialTheme.typography.labelMedium,
                                        color = MaterialTheme.colorScheme.outline.copy(alpha = 0.8f),
                                        textAlign = TextAlign.Center
                                    )
                                }
                            }
                        } else {
                            // Enclosed preview lists for the calendar day selection, in compact, vertically scrollable layout
                            Column(
                                modifier = Modifier.fillMaxWidth(),
                                verticalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                dateSchedules.forEach { schedule ->
                                    ScheduleRowItem(
                                        schedule = schedule,
                                        listingImagesMap = listingImagesMap,
                                        onFetchImage = { id -> viewModel.fetchListingImageIfNeeded(id, schedule.namaMe) },
                                        onEditClick = {
                                            viewModel.startEditing(schedule)
                                            onNavigateToForm()
                                        },
                                        onFollowUpClick = {
                                            selectedScheduleForFollowUp = schedule
                                        },
                                        onClick = { selectedScheduleForDetail = schedule }
                                    )
                                }
                            }
                        }
                        
                        Spacer(modifier = Modifier.height(80.dp))
                    }
                }
            }
        }
        
        selectedScheduleForDetail?.let { schedule ->
            ScheduleDetailDialog(
                schedule = schedule,
                listingImagesMap = listingImagesMap,
                listingImagesGalleryMap = listingImagesGalleryMap,
                agentInfoMap = agentInfoMap,
                onDismiss = { selectedScheduleForDetail = null },
                onDelete = {
                    viewModel.deleteSchedule(schedule)
                    selectedScheduleForDetail = null
                },
                onEdit = {
                    viewModel.startEditing(schedule)
                    selectedScheduleForDetail = null
                    onNavigateToForm()
                }
            )
        }

        selectedScheduleForFollowUp?.let { schedule ->
            WhatsAppChooserDialog(
                schedule = schedule,
                agentInfoMap = agentInfoMap,
                onDismiss = { selectedScheduleForFollowUp = null }
            )
        }
    }
}

@Composable
fun ScheduleRowItem(
    schedule: Schedule,
    listingImagesMap: Map<String, String>,
    onFetchImage: (String) -> Unit,
    onEditClick: () -> Unit,
    onFollowUpClick: () -> Unit,
    onClick: () -> Unit
) {
    val typeLower = schedule.type.lowercase().trim()
    val isSelesai = typeLower.startsWith("done")
    val isAktif = !isSelesai && typeLower.isNotBlank() && schedule.tanggal.trim().isNotBlank()
    val indicatorColor = when {
        isSelesai -> MaterialTheme.colorScheme.primary
        isAktif -> MaterialTheme.colorScheme.secondary
        else -> MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)
    }

    LaunchedEffect(schedule.idListing) {
        val cleanId = schedule.idListing.trim()
        if (cleanId.isNotBlank()) {
            onFetchImage(cleanId)
        }
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("schedule_item_${schedule.id}")
            .clickable { onClick() },
        colors = CardDefaults.cardColors(
            containerColor = indicatorColor.copy(alpha = 0.05f)
        ),
        border = BorderStroke(1.dp, indicatorColor.copy(alpha = 0.15f)),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(IntrinsicSize.Min),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Distinctive visual color indicator bar
                Box(
                    modifier = Modifier
                        .width(6.dp)
                        .fillMaxHeight()
                        .background(indicatorColor)
                )

                // Large property image display on the left (matches height perfectly!)
                val cleanId = schedule.idListing.trim()
                if (cleanId.isNotBlank()) {
                    val imageUrl = listingImagesMap[cleanId]
                    Box(
                        modifier = Modifier
                            .width(115.dp)
                            .fillMaxHeight()
                            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)),
                        contentAlignment = Alignment.Center
                    ) {
                        if (imageUrl != null) {
                            AsyncImage(
                                model = imageUrl,
                                contentDescription = "Foto Listing $cleanId",
                                modifier = Modifier.fillMaxSize(),
                                contentScale = androidx.compose.ui.layout.ContentScale.Crop
                            )
                        } else {
                            CircularProgressIndicator(
                                modifier = Modifier.size(24.dp),
                                strokeWidth = 2.dp,
                                color = indicatorColor.copy(alpha = 0.5f)
                            )
                        }
                    }
                } else {
                    // Manual Input / No Listing ID image placeholder
                    Box(
                        modifier = Modifier
                            .width(115.dp)
                            .fillMaxHeight()
                            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.2f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Image,
                            contentDescription = "No Image",
                            tint = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f),
                            modifier = Modifier.size(32.dp)
                        )
                    }
                }

                // Text Content on the Right side (shifted slightly to the right with start spacing)
                Column(
                    modifier = Modifier
                        .weight(1.0f)
                        .padding(start = 16.dp, top = 12.dp, end = 12.dp, bottom = 12.dp)
                ) {
                    // Listing ID label & Sync Tag row
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        if (schedule.idListing.isNotBlank()) {
                            Box(
                                modifier = Modifier
                                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.1f), RoundedCornerShape(8.dp))
                                    .padding(horizontal = 8.dp, vertical = 4.dp)
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                    Icon(Icons.Default.QrCode, contentDescription = null, modifier = Modifier.size(12.dp), tint = MaterialTheme.colorScheme.primary)
                                    Text(
                                        text = schedule.idListing,
                                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                }
                            }
                        } else {
                            Box(
                                modifier = Modifier
                                    .background(MaterialTheme.colorScheme.secondary.copy(alpha = 0.1f), RoundedCornerShape(8.dp))
                                    .padding(horizontal = 8.dp, vertical = 4.dp)
                            ) {
                                Text(
                                    text = "Manual Input",
                                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                    color = MaterialTheme.colorScheme.secondary
                                )
                            }
                        }

                        // Sync indicators
                        val isSynced = schedule.synced
                        Box(
                            modifier = Modifier
                                .background(
                                    if (isSynced) MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)
                                    else MaterialTheme.colorScheme.error.copy(alpha = 0.1f),
                                    RoundedCornerShape(100.dp)
                                )
                                .padding(horizontal = 8.dp, vertical = 2.dp)
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                                Icon(
                                    imageVector = if (isSynced) Icons.Default.CloudDone else Icons.Default.CloudOff,
                                    contentDescription = null,
                                    tint = if (isSynced) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error,
                                    modifier = Modifier.size(10.dp)
                                )
                                Text(
                                    text = if (isSynced) "Synced" else "Pending",
                                    style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp),
                                    color = if (isSynced) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(6.dp))

                    // Location / Jalan details
                    val displayLocation = schedule.lokasi.ifBlank { "Lokasi tidak tersedia" }
                    Text(
                        text = displayLocation,
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold, fontSize = 15.sp),
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )

                    Spacer(modifier = Modifier.height(4.dp))

                    // ME row
                    val displayName = schedule.namaMe.ifBlank { "ME tidak diketahui" }
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Person,
                            contentDescription = "ME Name Icon",
                            tint = MaterialTheme.colorScheme.outline,
                            modifier = Modifier.size(14.dp)
                        )
                        Text(
                            text = "ME: $displayName",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    Spacer(modifier = Modifier.height(4.dp))

                    // Date & Time row (formatted with Day of week and 12H AM/PM)
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            Icon(Icons.Default.CalendarToday, contentDescription = null, tint = MaterialTheme.colorScheme.outline, modifier = Modifier.size(14.dp))
                            Text(formatIndonesianDate(schedule.tanggal), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            Icon(Icons.Default.AccessTime, contentDescription = null, tint = MaterialTheme.colorScheme.outline, modifier = Modifier.size(14.dp))
                            Text(formatTwelveHourTime(schedule.jam), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }

                    Spacer(modifier = Modifier.height(6.dp))

                    // Type Badge & Status Badge row beside staff chip
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            val isDoneType = schedule.type.lowercase().contains("done")
                            Box(
                                modifier = Modifier
                                    .background(
                                        if (isDoneType) MaterialTheme.colorScheme.secondaryContainer 
                                        else MaterialTheme.colorScheme.tertiaryContainer, 
                                        RoundedCornerShape(6.dp)
                                    )
                                    .padding(horizontal = 6.dp, vertical = 2.dp)
                            ) {
                                Text(
                                    text = "Type: ${schedule.type}",
                                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold, fontSize = 9.sp),
                                    color = if (isDoneType) MaterialTheme.colorScheme.onSecondaryContainer else MaterialTheme.colorScheme.onTertiaryContainer
                                )
                            }

                            val isDoneStatus = schedule.status.uppercase().trim() == "DONE"
                            Box(
                                modifier = Modifier
                                    .background(
                                        if (isDoneStatus) MaterialTheme.colorScheme.primaryContainer
                                        else MaterialTheme.colorScheme.surfaceVariant, 
                                        RoundedCornerShape(6.dp)
                                    )
                                    .padding(horizontal = 6.dp, vertical = 2.dp)
                            ) {
                                Text(
                                    text = "Status: ${schedule.status}",
                                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold, fontSize = 9.sp),
                                    color = if (isDoneStatus) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }

                        // Staff Label
                        Box(
                            modifier = Modifier
                                .background(MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.5f), RoundedCornerShape(6.dp))
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Text(
                                text = schedule.staff,
                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold, fontSize = 9.sp),
                                color = MaterialTheme.colorScheme.onSecondaryContainer
                            )
                        }
                    }
                }
            }

            // Divider separating details and direct columns actions (Edit & Follow Up)
            HorizontalDivider(
                color = indicatorColor.copy(alpha = 0.12f),
                modifier = Modifier.padding(horizontal = 12.dp)
            )

            // Split Action Columns row: Edit vs Follow Up! (Highly requested feature)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(44.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Column 1: Edit button
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .clickable { onEditClick() },
                    contentAlignment = Alignment.Center
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Edit,
                            contentDescription = "Edit",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(16.dp)
                        )
                        Text(
                            text = "Edit Jadwal",
                            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }

                // Vertical Divider
                Box(
                    modifier = Modifier
                        .width(1.dp)
                        .fillMaxHeight(0.6f)
                        .background(indicatorColor.copy(alpha = 0.15f))
                )

                // Column 2: Follow Up button
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .clickable { onFollowUpClick() },
                    contentAlignment = Alignment.Center
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Message,
                            contentDescription = "WhatsApp",
                            tint = Color(0xFF25D366),
                            modifier = Modifier.size(16.dp)
                        )
                        Text(
                            text = "Follow Up ME",
                            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                            color = Color(0xFF1E7E34)
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun ScheduleDetailDialog(
    schedule: Schedule,
    listingImagesMap: Map<String, String>,
    listingImagesGalleryMap: Map<String, List<String>>,
    agentInfoMap: Map<String, AgentInfo>,
    onDismiss: () -> Unit,
    onDelete: () -> Unit,
    onEdit: () -> Unit
) {
    var showDeleteConfirm by remember { mutableStateOf(false) }

    if (showDeleteConfirm) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirm = false },
            title = {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Warning,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.error
                    )
                    Text("Konfirmasi Hapus")
                }
            },
            text = {
                Text("Apakah Anda yakin ingin menghapus jadwal untuk lokasi '${schedule.lokasi}' secara lokal?")
            },
            confirmButton = {
                Button(
                    onClick = {
                        showDeleteConfirm = false
                        onDelete()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("Hapus")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirm = false }) {
                    Text("Batal")
                }
            }
        )
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Detail Jadwal",
                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold)
                )
                IconButton(onClick = onDismiss) {
                    Icon(Icons.Default.Close, contentDescription = "Close dialog")
                }
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Horizontal scroll gallery of all available property images
                val cleanId = schedule.idListing.trim()
                if (cleanId.isNotBlank()) {
                    val galleryList = listingImagesGalleryMap[cleanId] ?: emptyList()
                    val fallbackImg = listingImagesMap[cleanId]
                    val imagesToDisplay = if (galleryList.isNotEmpty()) galleryList else listOfNotNull(fallbackImg)
                    
                    if (imagesToDisplay.isNotEmpty()) {
                        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Text(
                                text = "Foto Listing (Total ${imagesToDisplay.size} foto - slide ke kanan/kiri)",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.primary,
                                fontWeight = FontWeight.Bold
                            )
                            LazyRow(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                items(imagesToDisplay) { img ->
                                    Card(
                                        modifier = Modifier
                                            .width(220.dp)
                                            .height(150.dp),
                                        shape = RoundedCornerShape(12.dp),
                                        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                                    ) {
                                        AsyncImage(
                                            model = img,
                                            contentDescription = "Foto Listing $cleanId",
                                            modifier = Modifier.fillMaxSize(),
                                            contentScale = androidx.compose.ui.layout.ContentScale.Crop
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                // ID Listing & Status badges
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    if (schedule.idListing.isNotBlank()) {
                        Box(
                            modifier = Modifier
                                .background(
                                    MaterialTheme.colorScheme.primary.copy(alpha = 0.1f),
                                    RoundedCornerShape(8.dp)
                                )
                                .padding(horizontal = 8.dp, vertical = 4.dp)
                        ) {
                            Text(
                                text = "ID: ${schedule.idListing}",
                                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    } else {
                        Box(
                            modifier = Modifier
                                .background(
                                    MaterialTheme.colorScheme.secondary.copy(alpha = 0.1f),
                                    RoundedCornerShape(8.dp)
                                )
                                .padding(horizontal = 8.dp, vertical = 4.dp)
                        ) {
                            Text(
                                text = "Manual Input",
                                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.secondary
                            )
                        }
                    }

                    Box(
                        modifier = Modifier
                            .background(
                                if (schedule.synced) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.errorContainer,
                                RoundedCornerShape(8.dp)
                            )
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = if (schedule.synced) "Synced" else "Pending Sync",
                            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                            color = if (schedule.synced) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onErrorContainer
                        )
                    }
                }

                // Follow Up WhatsApp Container (PROACTIVE FOLLOW-UP DASHBOARD AS REQUESTED)
                val agentInfo = agentInfoMap[cleanId]
                val testMeName = agentInfo?.name?.ifBlank { schedule.namaMe } ?: schedule.namaMe
                val rawPhone = agentInfo?.phone?.ifBlank { "" } ?: ""
                
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.2f)
                    ),
                    shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.3f))
                ) {
                    Column(
                        modifier = Modifier.padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Phone,
                                contentDescription = "Follow Up",
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(18.dp)
                            )
                            Text(
                                text = "Follow Up ME (Marketing Executive)",
                                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                        
                        Text(
                            text = "ME Name: $testMeName",
                            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        
                        if (rawPhone.isNotBlank()) {
                            Text(
                                text = "WhatsApp: $rawPhone",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        } else {
                            Text(
                                text = "No phone parsed automatically.",
                                style = MaterialTheme.typography.bodySmall.copy(fontStyle = androidx.compose.ui.text.font.FontStyle.Italic),
                                color = MaterialTheme.colorScheme.outline
                            )
                        }

                        val context = LocalContext.current
                        Button(
                            onClick = {
                                val targetName = testMeName.ifBlank { "ME" }
                                val formattedPhone = if (rawPhone.isNotBlank()) {
                                    var clean = rawPhone.replace("[^\\d]".toRegex(), "")
                                    if (clean.startsWith("0")) {
                                        clean = "62" + clean.substring(1)
                                    }
                                    clean
                                } else "628561103735" // Robust safe default if no phone is scraped

                                val javaCalendar = Calendar.getInstance()
                                val hour = javaCalendar.get(Calendar.HOUR_OF_DAY)
                                val greeting = when (hour) {
                                    in 4..10 -> "Selamat Pagi"
                                    in 11..14 -> "Selamat Siang"
                                    in 15..18 -> "Selamat Sore"
                                    else -> "Selamat Malam"
                                }
                                
                                val lowerName = targetName.lowercase()
                                val femaleKeywords = listOf("sri", "siti", "dewi", "putri", "fitri", "indah", "ibu", "bu ", "maria", "ani ", "diana", "rani", "lia", "eka", "linda", "kartika", "nur", "sarah")
                                val honorific = if (femaleKeywords.any { lowerName.contains(it) }) "Bu" else "Pak"
                                
                                val listingUrl = "https://raywhitecipete.net/ListingView/Detail/${schedule.idListing.trim()}"
                                val testMsg = "$listingUrl\n\n$greeting $honorific $targetName,\nUntuk ID Listing ${schedule.idListing.trim()} berikut bisa kita ambil foto ulang kapan ya? agar kita bisa input ke dalam jadwal.\n\nTerima kasih."
                                
                                val intentUri = "https://api.whatsapp.com/send?phone=$formattedPhone&text=${Uri.encode(testMsg)}"
                                val intent = Intent(Intent.ACTION_VIEW, Uri.parse(intentUri))
                                context.startActivity(intent)
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF25D366)),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("Hubungi via WhatsApp", color = Color.White)
                        }
                    }
                }

                // Lokasi
                Column {
                    Text("Lokasi", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.outline)
                    Text(
                        text = schedule.lokasi.ifBlank { "Lokasi belum ditentukan / otomatis autofill oleh Maps" },
                        style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.SemiBold),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }

                HorizontalDivider()

                // Nama ME
                Column {
                    Text("Nama ME", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.outline)
                    Text(
                        text = schedule.namaMe.ifBlank { "Akan diautofill..." },
                        style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.SemiBold),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }

                HorizontalDivider()

                // Date and Time
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Tanggal", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.outline)
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.CalendarToday,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp),
                                tint = MaterialTheme.colorScheme.primary
                            )
                            Text(
                                text = formatIndonesianDate(schedule.tanggal),
                                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium)
                            )
                        }
                    }

                    Column(modifier = Modifier.weight(1f)) {
                        Text("Jam", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.outline)
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.AccessTime,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp),
                                tint = MaterialTheme.colorScheme.primary
                            )
                            Text(
                                text = formatTwelveHourTime(schedule.jam),
                                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium)
                            )
                        }
                    }
                }

                HorizontalDivider()

                // Type & Status
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Type", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.outline)
                        Text(
                            text = schedule.type,
                            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.primary
                        )
                    }

                    Column(modifier = Modifier.weight(1f)) {
                        Text("Status", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.outline)
                        Text(
                            text = schedule.status,
                            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                            color = if (schedule.status.uppercase() == "DONE") MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.tertiary
                        )
                    }
                }

                HorizontalDivider()

                // Staff
                Column {
                    Text("Staff", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.outline)
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Person,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp),
                            tint = MaterialTheme.colorScheme.secondary
                        )
                        Text(
                            text = schedule.staff,
                            style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.SemiBold)
                        )
                    }
                }
            }
        },
        confirmButton = {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedButton(
                    onClick = onEdit,
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(Icons.Default.Edit, contentDescription = null)
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Ubah")
                }
                Button(
                    onClick = { showDeleteConfirm = true },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(Icons.Default.Delete, contentDescription = null)
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Hapus")
                }
            }
        }
    )
}

data class CalendarDay(
    val dayOfMonth: Int,
    val dateString: String, // "yyyy-MM-dd"
    val isCurrentMonth: Boolean,
    val isToday: Boolean
)

fun getCalendarDays(year: Int, month: Int): List<CalendarDay> {
    val days = mutableListOf<CalendarDay>()
    
    // Calendar instance set to the first day of target year and month
    val cal = Calendar.getInstance().apply {
        clear()
        set(Calendar.YEAR, year)
        set(Calendar.MONTH, month)
        set(Calendar.DAY_OF_MONTH, 1)
    }
    
    // Day of week of the 1st day (1 = Sunday, 2 = Monday, ...)
    val firstDayOfWeek = cal.get(Calendar.DAY_OF_WEEK)
    
    // Shift so that empty slots represent previous month days
    val emptyBefore = firstDayOfWeek - 1
    
    // Previous month filler days
    val prevMonthCal = (cal.clone() as Calendar).apply {
        add(Calendar.MONTH, -1)
    }
    val maxDaysPrev = prevMonthCal.getActualMaximum(Calendar.DAY_OF_MONTH)
    
    for (i in (maxDaysPrev - emptyBefore + 1)..maxDaysPrev) {
        val y = prevMonthCal.get(Calendar.YEAR)
        val m = prevMonthCal.get(Calendar.MONTH)
        val dateStr = String.format("%04d-%02d-%02d", y, m + 1, i)
        days.add(CalendarDay(i, dateStr, false, false))
    }
    
    // Current month days
    val maxDaysCurrent = cal.getActualMaximum(Calendar.DAY_OF_MONTH)
    val todayCal = Calendar.getInstance()
    
    for (i in 1..maxDaysCurrent) {
        val dateStr = String.format("%04d-%02d-%02d", year, month + 1, i)
        val isToday = todayCal.get(Calendar.YEAR) == year && 
                      todayCal.get(Calendar.MONTH) == month && 
                      todayCal.get(Calendar.DAY_OF_MONTH) == i
        days.add(CalendarDay(i, dateStr, true, isToday))
    }
    
    // Next month filler days to complete standard 42-grid cells (6 weeks)
    val nextMonthCal = (cal.clone() as Calendar).apply {
        add(Calendar.MONTH, 1)
    }
    val remaining = 42 - days.size
    for (i in 1..remaining) {
        val y = nextMonthCal.get(Calendar.YEAR)
        val m = nextMonthCal.get(Calendar.MONTH)
        val dateStr = String.format("%04d-%02d-%02d", y, m + 1, i)
        days.add(CalendarDay(i, dateStr, false, false))
    }
    
    return days
}

@Composable
fun DashboardCalendarView(
    schedules: List<Schedule>,
    selectedDate: String?,
    onDateSelected: (String) -> Unit
) {
    var calendarYear by remember { mutableStateOf(Calendar.getInstance().get(Calendar.YEAR)) }
    var calendarMonth by remember { mutableStateOf(Calendar.getInstance().get(Calendar.MONTH)) } // 0-indexed
    
    val days = remember(calendarYear, calendarMonth) {
        getCalendarDays(calendarYear, calendarMonth)
    }
    
    val monthYearFormatter = remember { SimpleDateFormat("MMMM yyyy", Locale("id", "ID")) }
    val displayMonthYear = remember(calendarYear, calendarMonth) {
        val cal = Calendar.getInstance().apply {
            set(Calendar.YEAR, calendarYear)
            set(Calendar.MONTH, calendarMonth)
        }
        monthYearFormatter.format(cal.time)
    }
    
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 12.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
        ),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        shape = RoundedCornerShape(20.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            // Calendar Switch Month row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = {
                    if (calendarMonth == 0) {
                        calendarMonth = 11
                        calendarYear -= 1
                    } else {
                        calendarMonth -= 1
                    }
                }) {
                    Icon(
                        imageVector = Icons.Default.KeyboardArrowLeft,
                        contentDescription = "Bulan Sebelumnya",
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
                
                Text(
                    text = displayMonthYear.uppercase(Locale.getDefault()),
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.primary
                )
                
                IconButton(onClick = {
                    if (calendarMonth == 11) {
                        calendarMonth = 0
                        calendarYear += 1
                    } else {
                        calendarMonth += 1
                    }
                }) {
                    Icon(
                        imageVector = Icons.Default.KeyboardArrowRight,
                        contentDescription = "Bulan Selanjutnya",
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(12.dp))
            
            // Days of the week row
            val daysOfWeek = listOf("Min", "Sen", "Sel", "Rab", "Kam", "Jum", "Sab")
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                daysOfWeek.forEach { dayName ->
                    Text(
                        text = dayName,
                        modifier = Modifier.width(36.dp),
                        textAlign = TextAlign.Center,
                        style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold),
                        color = if (dayName == "Min") MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.outline
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(8.dp))
            
            // Days grid (6 weeks x 7 days)
            val rowsCount = days.size / 7
            for (rowIdx in 0 until rowsCount) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    for (colIdx in 0 until 7) {
                        val day = days[rowIdx * 7 + colIdx]
                        
                        // Count schedules on this specific date using robust date normalization
                        val daySchedules = remember(schedules, day.dateString) {
                            val normTarget = com.example.data.normalizeDate(day.dateString)
                            schedules.filter { com.example.data.normalizeDate(it.tanggal) == normTarget }
                        }
                        
                        val isSelected = selectedDate == day.dateString
                        
                        Box(
                            modifier = Modifier
                                .size(42.dp)
                                .clip(CircleShape)
                                .background(
                                    when {
                                        isSelected -> MaterialTheme.colorScheme.primary
                                        day.isToday -> MaterialTheme.colorScheme.secondaryContainer
                                        else -> Color.Transparent
                                    }
                                )
                                .clickable {
                                    onDateSelected(day.dateString)
                                }
                                .padding(4.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Center
                            ) {
                                Text(
                                    text = day.dayOfMonth.toString(),
                                    style = MaterialTheme.typography.bodyMedium.copy(
                                        fontWeight = if (isSelected || day.isToday) FontWeight.Bold else FontWeight.Normal
                                    ),
                                    color = when {
                                        isSelected -> MaterialTheme.colorScheme.onPrimary
                                        !day.isCurrentMonth -> MaterialTheme.colorScheme.outline.copy(alpha = 0.4f)
                                        day.isToday -> MaterialTheme.colorScheme.onSecondaryContainer
                                        else -> MaterialTheme.colorScheme.onSurface
                                    }
                                )
                                
                                // Integrated schedules indicators below day number
                                if (daySchedules.isNotEmpty()) {
                                    Row(
                                        horizontalArrangement = Arrangement.spacedBy(2.5.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        // Draw a dot for each schedule representing active vs completed events
                                        daySchedules.forEach { schedule ->
                                            val tLower = schedule.type.lowercase()
                                            val isDone = tLower.startsWith("done")
                                            val dotColor = when {
                                                isSelected -> MaterialTheme.colorScheme.onPrimary
                                                isDone -> MaterialTheme.colorScheme.primary // Gold
                                                else -> MaterialTheme.colorScheme.secondary // Cosmic Indigo/Accent
                                            }
                                            Box(
                                                modifier = Modifier
                                                    .size(4.dp)
                                                    .clip(CircleShape)
                                                    .background(dotColor)
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun MonthlyAnalysisCard(schedules: List<Schedule>) {
    val currentMonthName = SimpleDateFormat("MMMM yyyy", Locale("id", "ID")).format(Date())

    val fotoCount = schedules.count { 
        val t = it.type.lowercase()
        t.contains("foto") || t.isBlank()
    }
    val videoCount = schedules.count { 
        it.type.lowercase().contains("video")
    }
    val totalCount = fotoCount + videoCount

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.25f)
        ),
        shape = RoundedCornerShape(20.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(18.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Ringkasan Analisis",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.ExtraBold),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "Statistik Listing ($currentMonthName)",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Box(
                    modifier = Modifier
                        .background(MaterialTheme.colorScheme.primaryContainer, RoundedCornerShape(100.dp))
                        .padding(horizontal = 10.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = "Total: $totalCount",
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Box(
                    modifier = Modifier.size(110.dp),
                    contentAlignment = Alignment.Center
                ) {
                    val colorFoto = MaterialTheme.colorScheme.primary
                    val colorVideo = MaterialTheme.colorScheme.secondary
                    val colorEmpty = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)

                    Canvas(modifier = Modifier.fillMaxSize()) {
                        val strokeWidthPx = 14.dp.toPx()
                        val diameter = size.minDimension - strokeWidthPx
                        val topLeftOffset = Offset(
                            (size.width - diameter) / 2f,
                            (size.height - diameter) / 2f
                        )
                        val arcSize = androidx.compose.ui.geometry.Size(diameter, diameter)

                        if (totalCount == 0) {
                            drawArc(
                                color = colorEmpty,
                                startAngle = 0f,
                                sweepAngle = 360f,
                                useCenter = false,
                                topLeft = topLeftOffset,
                                size = arcSize,
                                style = androidx.compose.ui.graphics.drawscope.Stroke(
                                    width = strokeWidthPx,
                                    cap = androidx.compose.ui.graphics.StrokeCap.Round
                                )
                            )
                        } else {
                            val fotoAngle = (fotoCount.toFloat() / totalCount.toFloat()) * 360f
                            val videoAngle = (videoCount.toFloat() / totalCount.toFloat()) * 360f

                            drawArc(
                                color = colorFoto,
                                startAngle = -90f,
                                sweepAngle = fotoAngle,
                                useCenter = false,
                                topLeft = topLeftOffset,
                                size = arcSize,
                                style = androidx.compose.ui.graphics.drawscope.Stroke(
                                    width = strokeWidthPx,
                                    cap = androidx.compose.ui.graphics.StrokeCap.Round
                                )
                            )

                            drawArc(
                                color = colorVideo,
                                startAngle = -90f + fotoAngle,
                                sweepAngle = videoAngle,
                                useCenter = false,
                                topLeft = topLeftOffset,
                                size = arcSize,
                                style = androidx.compose.ui.graphics.drawscope.Stroke(
                                    width = strokeWidthPx,
                                    cap = androidx.compose.ui.graphics.StrokeCap.Round
                                )
                            )
                        }
                    }

                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = if (totalCount > 0) "${((fotoCount.toFloat() / totalCount.toFloat()) * 100).toInt()}%" else "0%",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Black),
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "Foto",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    ChartLegendRow(
                        color = MaterialTheme.colorScheme.primary,
                        label = "Foto Listing",
                        value = "$fotoCount Media",
                        percentage = if (totalCount > 0) "${((fotoCount.toFloat() / totalCount.toFloat()) * 100).toInt()}%" else "0%"
                    )
                    ChartLegendRow(
                        color = MaterialTheme.colorScheme.secondary,
                        label = "Video Listing",
                        value = "$videoCount Media",
                        percentage = if (totalCount > 0) "${((videoCount.toFloat() / totalCount.toFloat()) * 100).toInt()}%" else "0%"
                    )
                }
            }
        }
    }
}

@Composable
fun ChartLegendRow(color: Color, label: String, value: String, percentage: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(10.dp)
                .clip(CircleShape)
                .background(color)
        )
        Column {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = label,
                    style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = percentage,
                    style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Black),
                    color = color
                )
            }
            Text(
                text = value,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
fun WhatsAppChooserDialog(
    schedule: Schedule,
    agentInfoMap: Map<String, AgentInfo>,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val agentInfo = agentInfoMap[schedule.idListing.trim()]
    
    var finalPhone = agentInfo?.phone?.trim() ?: ""
    if (finalPhone.isBlank()) {
        finalPhone = getAgentPhoneByName(schedule.namaMe)
    }
    
    var cleanPhone = finalPhone.replace("[^\\d]".toRegex(), "")
    if (cleanPhone.startsWith("0")) {
        cleanPhone = "62" + cleanPhone.substring(1)
    }
    if (cleanPhone.isBlank()) {
        cleanPhone = "085169671344"
    }

    val formattedDate = formatIndonesianDate(schedule.tanggal)
    val formattedTime = formatTwelveHourTime(schedule.jam)

    val message = """
Halo ${schedule.namaMe.ifBlank { "ME Ray White" }}, saya dari Tim RWC Foto.

Berikut info lengkap jadwal kegiatan untuk property Anda:
📌 *ID Listing*: ${schedule.idListing.ifBlank { "(Manual Input)" }}
🎬 *Tipe*: ${schedule.type}
📅 *Jadwal*: $formattedDate pada $formattedTime
📍 *Lokasi*: ${schedule.lokasi.ifBlank { "-" }}
🏃 *Staff*: ${schedule.staff}
⚡ *Status*: Pemotretan ${schedule.status}

Detail lengkap properti website Ray White Cipete:
🔗 https://raywhitecipete.net/ListingView/Detail/${schedule.idListing.trim()}

Mohon ketersediaannya untuk follow up. Terima kasih.
    """.trimIndent()

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Icon(
                    imageVector = Icons.Default.Message,
                    contentDescription = null,
                    tint = Color(0xFF25D366),
                    modifier = Modifier.size(28.dp)
                )
                Text(
                    text = "Follow Up WhatsApp",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                )
            }
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(
                    text = "Kirim detail jadwal follow-up ke ME ${schedule.namaMe.ifBlank { "" }} via WhatsApp.",
                    style = MaterialTheme.typography.bodyMedium
                )
                
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            Icon(Icons.Default.Phone, contentDescription = null, modifier = Modifier.size(14.dp), tint = MaterialTheme.colorScheme.secondary)
                            Text(
                                text = "Penerima: ${schedule.namaMe} (${finalPhone.ifBlank { "085169671344 (Default)" }})",
                                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.secondary
                            )
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = message,
                            maxLines = 6,
                            overflow = TextOverflow.Ellipsis,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Text(
                    text = "Pilih jenis aplikasi WhatsApp yang ingin digunakan:",
                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Medium),
                    color = MaterialTheme.colorScheme.outline
                )
            }
        },
        confirmButton = {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Button(
                    onClick = {
                        val uri = Uri.parse("https://api.whatsapp.com/send?phone=$cleanPhone&text=${Uri.encode(message)}")
                        val intent = Intent(Intent.ACTION_VIEW, uri).apply {
                            setPackage("com.whatsapp")
                        }
                        try {
                            context.startActivity(intent)
                        } catch (e: Exception) {
                            val fallbackIntent = Intent(Intent.ACTION_VIEW, uri)
                            context.startActivity(fallbackIntent)
                        }
                        onDismiss()
                    },
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF25D366), contentColor = Color.White)
                ) {
                    Text("WhatsApp", style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold))
                }

                Button(
                    onClick = {
                        val uri = Uri.parse("https://api.whatsapp.com/send?phone=$cleanPhone&text=${Uri.encode(message)}")
                        val intent = Intent(Intent.ACTION_VIEW, uri).apply {
                            setPackage("com.whatsapp.w4b")
                        }
                        try {
                            context.startActivity(intent)
                        } catch (e: Exception) {
                            val fallbackIntent = Intent(Intent.ACTION_VIEW, uri)
                            context.startActivity(fallbackIntent)
                        }
                        onDismiss()
                    },
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF075E54), contentColor = Color.White)
                ) {
                    Text("WA Business", style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold))
                }
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Batal")
            }
        }
    )
}
