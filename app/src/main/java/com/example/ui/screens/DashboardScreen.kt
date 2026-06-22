package com.example.ui.screens

import android.app.DatePickerDialog
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
import com.example.ui.SyncState
import java.text.SimpleDateFormat
import java.util.*

enum class ScheduleCategory(val displayName: String) {
    SEMUA("Semua"),
    AKTIF("Jadwal Aktif"),
    SELESAI("Jadwal Selesai"),
    TASK("Task")
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun DashboardScreen(
    viewModel: ScheduleViewModel,
    onNavigateToForm: () -> Unit,
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

    var selectedCategory by remember { mutableStateOf(ScheduleCategory.AKTIF) }
    var selectedScheduleForDetail by remember { mutableStateOf<Schedule?>(null) }
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
            TopAppBar(
                title = {
                    Column {
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
                        onClick = { viewModel.syncData() },
                        modifier = Modifier.testTag("refresh_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = "Sync Data",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
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
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .background(MaterialTheme.colorScheme.surface)
        ) {
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
                                        .clickable { selectedCategory = ScheduleCategory.SEMUA },
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
                                        .clickable { selectedCategory = ScheduleCategory.AKTIF },
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
                                        .clickable { selectedCategory = ScheduleCategory.TASK },
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
                                                onClick = { selectedCategory = category },
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
                                    onFetchImage = { viewModel.fetchListingImageIfNeeded(it) },
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
                                        onFetchImage = { viewModel.fetchListingImageIfNeeded(it) },
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
    }
}

@Composable
fun ScheduleRowItem(
    schedule: Schedule,
    listingImagesMap: Map<String, String>,
    onFetchImage: (String) -> Unit,
    onClick: () -> Unit
) {
    val typeLower = schedule.type.lowercase().trim()
    val isSelesai = typeLower.startsWith("done")
    val isAktif = !isSelesai && typeLower.isNotBlank() && schedule.tanggal.trim().isNotBlank()
    val indicatorColor = when {
        isSelesai -> MaterialTheme.colorScheme.primary // Amber Gold / Secondary
        isAktif -> MaterialTheme.colorScheme.secondary // Cosmic Indigo Accent
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
            .clickable { onClick() }
            .testTag("schedule_item_${schedule.id}"),
        colors = CardDefaults.cardColors(
            containerColor = indicatorColor.copy(alpha = 0.06f)
        ),
        border = BorderStroke(1.dp, indicatorColor.copy(alpha = 0.15f)),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        shape = RoundedCornerShape(16.dp)
    ) {
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

            // Real-time property image display (Thumbnail di kiri)
            val cleanId = schedule.idListing.trim()
            if (cleanId.isNotBlank()) {
                val imageUrl = listingImagesMap[cleanId]
                Box(
                    modifier = Modifier
                        .padding(start = 12.dp, top = 12.dp, bottom = 12.dp)
                        .size(76.dp)
                        .clip(RoundedCornerShape(10.dp))
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
                            modifier = Modifier.size(20.dp),
                            strokeWidth = 2.dp,
                            color = indicatorColor.copy(alpha = 0.5f)
                        )
                    }
                }
            }

            Column(
                modifier = Modifier
                    .weight(1.0f)
                    .padding(16.dp)
            ) {
                // Row components: metadata and sync tags
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.Top
                ) {
                // Listing ID label
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

                // Sync Tag
                if (schedule.synced) {
                    Box(
                        modifier = Modifier
                            .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.15f), RoundedCornerShape(12.dp))
                            .padding(horizontal = 10.dp, vertical = 2.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            Icon(
                                imageVector = Icons.Default.CloudDone,
                                contentDescription = "Synced",
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(12.dp)
                            )
                            Text(
                                text = "Synced",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.primary,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                } else {
                    Box(
                        modifier = Modifier
                            .background(MaterialTheme.colorScheme.error.copy(alpha = 0.15f), RoundedCornerShape(12.dp))
                            .padding(horizontal = 10.dp, vertical = 2.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            Icon(
                                imageVector = Icons.Default.CloudOff,
                                contentDescription = "Pending Sync",
                                tint = MaterialTheme.colorScheme.error,
                                modifier = Modifier.size(12.dp)
                            )
                            Text(
                                text = "Pending",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.error,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // ME details / Location details
            val displayName = schedule.namaMe.ifBlank { "Autofilling..." }
            val displayLocation = schedule.lokasi.ifBlank { "Lokasi akan diautofill oleh Maps/Sheets..." }

            Text(
                text = displayLocation,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )

            Spacer(modifier = Modifier.height(4.dp))

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Person,
                    contentDescription = "ME Name Icon",
                    tint = MaterialTheme.colorScheme.outline,
                    modifier = Modifier.size(16.dp)
                )
                Text(
                    text = "ME: $displayName",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Type & Status indicators row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Type label/badge
                val isDoneType = schedule.type.lowercase().contains("done")
                Box(
                    modifier = Modifier
                        .background(
                            if (isDoneType) MaterialTheme.colorScheme.secondaryContainer 
                            else MaterialTheme.colorScheme.tertiaryContainer, 
                            RoundedCornerShape(8.dp)
                        )
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        Icon(
                            imageVector = Icons.Default.Category,
                            contentDescription = null,
                            modifier = Modifier.size(12.dp),
                            tint = if (isDoneType) MaterialTheme.colorScheme.onSecondaryContainer else MaterialTheme.colorScheme.onTertiaryContainer
                        )
                        Text(
                            text = "Type: ${schedule.type.ifBlank { "Foto" }}",
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                            color = if (isDoneType) MaterialTheme.colorScheme.onSecondaryContainer else MaterialTheme.colorScheme.onTertiaryContainer
                        )
                    }
                }

                // Status label/badge
                val isDoneStatus = schedule.status.uppercase().trim() == "DONE"
                Box(
                    modifier = Modifier
                        .background(
                            if (isDoneStatus) MaterialTheme.colorScheme.primaryContainer
                            else MaterialTheme.colorScheme.surfaceVariant, 
                            RoundedCornerShape(8.dp)
                        )
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        Icon(
                            imageVector = if (isDoneStatus) Icons.Default.CheckCircle else Icons.Default.Info,
                            contentDescription = null,
                            modifier = Modifier.size(12.dp),
                            tint = if (isDoneStatus) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = "Status: ${schedule.status.ifBlank { "Pending" }}",
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                            color = if (isDoneStatus) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))
            Divider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
            Spacer(modifier = Modifier.height(10.dp))

            // Date, time and staff
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Target Date Time
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.CalendarToday,
                            contentDescription = "Date icon",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(14.dp)
                        )
                        // Make date look friendlier
                        val formattedDate = try {
                            val parser = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
                            val output = SimpleDateFormat("dd MMM yyyy", Locale.getDefault())
                            parser.parse(schedule.tanggal)?.let { output.format(it) } ?: schedule.tanggal
                        } catch (e: Exception) {
                            schedule.tanggal
                        }
                        Text(
                            text = formattedDate,
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.AccessTime,
                            contentDescription = "Time icon",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(14.dp)
                        )
                        Text(
                            text = schedule.jam,
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }

                // Staff chip
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    modifier = Modifier
                        .background(MaterialTheme.colorScheme.secondaryContainer, RoundedCornerShape(12.dp))
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.DirectionsRun,
                        contentDescription = "Staff",
                        tint = MaterialTheme.colorScheme.onSecondaryContainer,
                        modifier = Modifier.size(12.dp)
                    )
                    Text(
                        text = schedule.staff,
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onSecondaryContainer
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
                // Real-time photo banner
                val cleanId = schedule.idListing.trim()
                if (cleanId.isNotBlank()) {
                    val imageUrl = listingImagesMap[cleanId]
                    if (imageUrl != null) {
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(160.dp),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            AsyncImage(
                                model = imageUrl,
                                contentDescription = "Foto Listing $cleanId",
                                modifier = Modifier.fillMaxSize(),
                                contentScale = androidx.compose.ui.layout.ContentScale.Crop
                            )
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
                                text = schedule.tanggal,
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
                                text = schedule.jam,
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
                                        // Draw a dot for each schedule (up to 3) representing active vs completed events
                                        daySchedules.take(3).forEach { schedule ->
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
