package com.example.ui.screens

import android.app.DatePickerDialog
import android.app.TimePickerDialog
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.ui.ScheduleViewModel
import com.example.ui.SubmitState
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalLayoutApi::class, ExperimentalMaterial3Api::class)
@Composable
fun InputFormScreen(
    viewModel: ScheduleViewModel,
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val scrollState = rememberScrollState()

    // ViewModel Flows
    val listingId by viewModel.formListingId.collectAsStateWithLifecycle()
    val namaMe by viewModel.formNamaMe.collectAsStateWithLifecycle()
    val lokasi by viewModel.formLokasi.collectAsStateWithLifecycle()
    val tanggal by viewModel.formTanggal.collectAsStateWithLifecycle()
    val jam by viewModel.formJam.collectAsStateWithLifecycle()
    val staff by viewModel.formStaff.collectAsStateWithLifecycle()
    val formType by viewModel.formType.collectAsStateWithLifecycle()
    val formStatus by viewModel.formStatus.collectAsStateWithLifecycle()
    val submitStatus by viewModel.submitStatus.collectAsStateWithLifecycle()
    val editingSchedule by viewModel.editingSchedule.collectAsStateWithLifecycle()
    val isEditMode = editingSchedule != null

    // DateTime picker initialization helpers
    val calendar = Calendar.getInstance()
    
    // Parse existing date or use today
    val dateFormatter = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
    val parsedDate = try {
        dateFormatter.parse(tanggal) ?: Date()
    } catch (e: Exception) {
        Date()
    }
    val dateCalendar = Calendar.getInstance().apply { time = parsedDate }

    val datePickerDialog = DatePickerDialog(
        context,
        { _, year, month, dayOfMonth ->
            val cal = Calendar.getInstance().apply { set(year, month, dayOfMonth) }
            viewModel.formTanggal.value = dateFormatter.format(cal.time)
        },
        dateCalendar.get(Calendar.YEAR),
        dateCalendar.get(Calendar.MONTH),
        dateCalendar.get(Calendar.DAY_OF_MONTH)
    )

    // Parse existing time or use now
    val timeFormatter = SimpleDateFormat("HH:mm", Locale.getDefault())
    val parsedTime = try {
        timeFormatter.parse(jam) ?: Date()
    } catch (e: Exception) {
        Date()
    }
    val timeCalendar = Calendar.getInstance().apply { time = parsedTime }

    val timePickerDialog = TimePickerDialog(
        context,
        { _, hourOfDay, minute ->
            viewModel.formJam.value = String.format(Locale.getDefault(), "%02d:%02d", hourOfDay, minute)
        },
        timeCalendar.get(Calendar.HOUR_OF_DAY),
        timeCalendar.get(Calendar.MINUTE),
        true
    )

    // Success navigation trigger
    LaunchedEffect(submitStatus) {
        if (submitStatus is SubmitState.Success) {
            onNavigateBack()
        }
    }

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
                            text = if (isEditMode) "Edit Jadwal" else "Tambah Jadwal",
                            style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack, modifier = Modifier.testTag("back_button")) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.surface)
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
                    .verticalScroll(scrollState)
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Submit Status Overlay Box
                AnimatedVisibility(
                    visible = submitStatus is SubmitState.Error,
                    enter = slideInVertically() + fadeIn(),
                    exit = slideOutVertically() + fadeOut()
                ) {
                    if (submitStatus is SubmitState.Error) {
                        Surface(
                            color = MaterialTheme.colorScheme.errorContainer,
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(16.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(Icons.Default.ErrorOutline, contentDescription = "Error", tint = MaterialTheme.colorScheme.error)
                                Spacer(modifier = Modifier.width(12.dp))
                                Text(
                                    text = (submitStatus as SubmitState.Error).message,
                                    color = MaterialTheme.colorScheme.onErrorContainer,
                                    style = MaterialTheme.typography.bodyMedium,
                                    modifier = Modifier.weight(1f)
                                )
                                IconButton(onClick = { viewModel.dismissSubmitStatus() }) {
                                    Icon(Icons.Default.Close, contentDescription = "Dismiss", tint = MaterialTheme.colorScheme.onErrorContainer)
                                }
                            }
                        }
                    }
                }

                // Callout Tip Box explaining conditional logic
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.4f)),
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.Top
                    ) {
                        Icon(
                            imageVector = Icons.Default.Info,
                            contentDescription = "Info",
                            tint = MaterialTheme.colorScheme.secondary,
                            modifier = Modifier.padding(top = 2.dp)
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                text = "Panduan Penginputan:",
                                style = MaterialTheme.typography.labelLarge,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSecondaryContainer
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = "1. Jika menginput ID Listing, info ME dan Lokasi akan di-autofill otomatis oleh skrip Google Sheets.\n" +
                                       "2. Jika ID Listing dikosongkan, Anda WAJIB mengisi Nama ME dan Lokasi secara manual.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSecondaryContainer
                            )
                        }
                    }
                }

                // Section 1: Listing ID (Optional/Autocomplete Trigger)
                Text(
                    text = "ID Listing & Informasi Utama",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )

                OutlinedTextField(
                    value = listingId,
                    onValueChange = { viewModel.formListingId.value = it },
                    label = { Text("ID Listing (Opsional)") },
                    placeholder = { Text("Contoh: L-2384") },
                    leadingIcon = { Icon(Icons.Default.QrCode, contentDescription = null) },
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("input_id_listing")
                )

                // Conditional validation flags for ME and Location
                val isListingIdEmpty = listingId.isBlank()

                OutlinedTextField(
                    value = namaMe,
                    onValueChange = { viewModel.formNamaMe.value = it },
                    label = { Text(if (isListingIdEmpty) "Nama ME / Agen *" else "Nama ME / Agen (Opsional)") },
                    placeholder = { Text("Contoh: Budi Susanto") },
                    leadingIcon = { Icon(Icons.Default.Person, contentDescription = null) },
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("input_nama_me"),
                    isError = isListingIdEmpty && namaMe.isBlank()
                )

                OutlinedTextField(
                    value = lokasi,
                    onValueChange = { viewModel.formLokasi.value = it },
                    label = { Text(if (isListingIdEmpty) "Lokasi Properti *" else "Lokasi Properti (Opsional)") },
                    placeholder = { Text("Contoh: Kavling Hijau Cluster B, Serpong") },
                    leadingIcon = { Icon(Icons.Default.LocationOn, contentDescription = null) },
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("input_lokasi"),
                    isError = isListingIdEmpty && lokasi.isBlank()
                )

                Spacer(modifier = Modifier.height(4.dp))

                // Section 2: Date & Time Picker
                Text(
                    text = "Waktu Pemotretan (Wajib)",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // Date picker component field
                    OutlinedCard(
                        modifier = Modifier
                            .weight(1f)
                            .testTag("pick_date_card")
                            .clickable { datePickerDialog.show() },
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 16.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column {
                                Text("Tanggal", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.outline)
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = tanggal,
                                    style = MaterialTheme.typography.bodyLarge,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                            Icon(Icons.Default.CalendarToday, contentDescription = "Pick Date", tint = MaterialTheme.colorScheme.primary)
                        }
                    }

                    // Time picker component field
                    OutlinedCard(
                        modifier = Modifier
                            .weight(1f)
                            .testTag("pick_time_card")
                            .clickable { timePickerDialog.show() },
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 16.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column {
                                Text("Jam", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.outline)
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = jam,
                                    style = MaterialTheme.typography.bodyLarge,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                            Icon(Icons.Default.AccessTime, contentDescription = "Pick Time", tint = MaterialTheme.colorScheme.primary)
                        }
                    }
                }

                Spacer(modifier = Modifier.height(4.dp))

                // Section 3: Staff Picker
                Text(
                    text = "Staff Fotografer (Wajib)",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )

                // Custom Multi-Select Dropdown for Staff Selection
                var dropdownExpanded by remember { mutableStateOf(false) }
                val currentStaffLower = staff.lowercase()
                val isDavidSelected = currentStaffLower.contains("david")
                val isRaffaSelected = currentStaffLower.contains("raffa")

                Box(modifier = Modifier.fillMaxWidth()) {
                    OutlinedTextField(
                        value = staff,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Staff / Fotografer *") },
                        placeholder = { Text("Pilih Staff (David / Raffa)") },
                        leadingIcon = { Icon(Icons.Default.DirectionsRun, contentDescription = null) },
                        trailingIcon = {
                            Icon(
                                imageVector = if (dropdownExpanded) Icons.Default.ArrowDropUp else Icons.Default.ArrowDropDown,
                                contentDescription = "Toggle Dropdown"
                            )
                        },
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("input_staff"),
                        isError = staff.isBlank()
                    )

                    // Overlay transparent click interceptor
                    Box(
                        modifier = Modifier
                            .matchParentSize()
                            .clickable { dropdownExpanded = !dropdownExpanded }
                    )

                    DropdownMenu(
                        expanded = dropdownExpanded,
                        onDismissRequest = { dropdownExpanded = false },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        DropdownMenuItem(
                            text = {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Checkbox(
                                        checked = isDavidSelected,
                                        onCheckedChange = null
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text("David")
                                }
                            },
                            onClick = {
                                val isChecked = !isDavidSelected
                                val newList = mutableListOf<String>()
                                if (isChecked) newList.add("David")
                                if (isRaffaSelected) newList.add("Raffa")
                                viewModel.formStaff.value = newList.joinToString(", ")
                            }
                        )

                        DropdownMenuItem(
                            text = {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Checkbox(
                                        checked = isRaffaSelected,
                                        onCheckedChange = null
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text("Raffa")
                                }
                            },
                            onClick = {
                                val isChecked = !isRaffaSelected
                                val newList = mutableListOf<String>()
                                if (isDavidSelected) newList.add("David")
                                if (isChecked) newList.add("Raffa")
                                viewModel.formStaff.value = newList.joinToString(", ")
                            }
                        )
                    }
                }

                Spacer(modifier = Modifier.height(4.dp))

                // Section 4: Type & Status fields
                Text(
                    text = "Kategori & Status Jadwal",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )

                // Type field
                OutlinedTextField(
                    value = formType,
                    onValueChange = { viewModel.formType.value = it },
                    label = { Text("Tipe Jadwal / Kolom Type") },
                    placeholder = { Text("Ketik tipe jadwal (Contoh: Foto, Video, Done)") },
                    leadingIcon = { Icon(Icons.Default.Category, contentDescription = null) },
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("input_type")
                )

                Text(
                    text = "Pilih Cepat Tipe:",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.outline
                )

                FlowRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    listOf("Foto", "Video", "Done", "Drone").forEach { preset ->
                        val isSelected = formType.equals(preset, ignoreCase = true)
                        InputChip(
                            selected = isSelected,
                            onClick = { viewModel.formType.value = preset },
                            label = { Text(preset) },
                            modifier = Modifier.padding(vertical = 4.dp),
                            shape = RoundedCornerShape(12.dp),
                            leadingIcon = {
                                if (isSelected) {
                                    Icon(
                                        imageVector = Icons.Default.Check,
                                        contentDescription = "Selected",
                                        modifier = Modifier.size(14.dp)
                                    )
                                }
                            }
                        )
                    }
                }

                Spacer(modifier = Modifier.height(4.dp))

                // Status field
                OutlinedTextField(
                    value = formStatus,
                    onValueChange = { viewModel.formStatus.value = it },
                    label = { Text("Status Jadwal / Kolom Status") },
                    placeholder = { Text("Ketik status (Contoh: Pending, Garis Tanah, Up Foto, Done)") },
                    leadingIcon = { Icon(Icons.Default.Info, contentDescription = null) },
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("input_status")
                )

                Text(
                    text = "Pilih Cepat Status / Pekerjaan:",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.outline
                )

                FlowRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    listOf("Pending", "In Progress", "Done", "Garis Tanah", "Up Foto", "Edit Video").forEach { preset ->
                        val isSelected = formStatus.equals(preset, ignoreCase = true)
                        InputChip(
                            selected = isSelected,
                            onClick = { viewModel.formStatus.value = preset },
                            label = { Text(preset) },
                            modifier = Modifier.padding(vertical = 4.dp),
                            shape = RoundedCornerShape(12.dp),
                            leadingIcon = {
                                if (isSelected) {
                                    Icon(
                                        imageVector = Icons.Default.Check,
                                        contentDescription = "Selected",
                                        modifier = Modifier.size(14.dp)
                                    )
                                }
                            }
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Save button
                val canSubmit = (listingId.isNotBlank() || (namaMe.isNotBlank() && lokasi.isNotBlank())) &&
                        tanggal.isNotBlank() && jam.isNotBlank() && staff.isNotBlank()

                Button(
                    onClick = { viewModel.submitSchedule() },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp)
                        .testTag("submit_schedule_button"),
                    enabled = canSubmit && submitStatus !is SubmitState.Loading,
                    shape = RoundedCornerShape(14.dp)
                ) {
                    if (submitStatus is SubmitState.Loading) {
                        CircularProgressIndicator(
                            color = MaterialTheme.colorScheme.onPrimary,
                            modifier = Modifier.size(24.dp),
                            strokeWidth = 2.dp
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Text("Menyimpan & Mensinkronisasi...", fontWeight = FontWeight.Bold)
                    } else {
                        Icon(Icons.Default.Save, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = if (isEditMode) "Simpan Perubahan" else "Simpan Jadwal Foto",
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                Spacer(modifier = Modifier.height(48.dp))
            }
        }
    }
}
