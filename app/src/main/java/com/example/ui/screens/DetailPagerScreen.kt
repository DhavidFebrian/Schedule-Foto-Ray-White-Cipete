package com.example.ui.screens

import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.animation.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.data.Schedule
import com.example.ui.AgentInfo
import com.example.ui.ScheduleViewModel
import com.example.ui.getAgentPhoneByName
import com.example.ui.getAgentEmailByName
import com.example.ui.getAgentInstagramByName
import com.example.ui.cleanListingDescription
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalFoundationApi::class, ExperimentalMaterial3Api::class)
@Composable
fun DetailPagerScreen(
    schedule: Schedule,
    listingImagesMap: Map<String, String>,
    listingImagesGalleryMap: Map<String, List<String>>,
    agentInfoMap: Map<String, AgentInfo>,
    viewModel: ScheduleViewModel,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    
    // Automatically trigger scraping when entering screen to load price/description/images
    LaunchedEffect(schedule.idListing) {
        viewModel.fetchListingImageIfNeeded(schedule.idListing, schedule.namaMe)
    }

    val listingPriceMap by viewModel.listingPriceMap.collectAsState()
    val listingDescMap by viewModel.listingDescMap.collectAsState()
    
    // Pager state starting at Index 1 (Center: Detail Listing)
    val pagerState = rememberPagerState(initialPage = 1) { 3 }
    
    val currentTab = when (pagerState.currentPage) {
        0 -> "EDIT"
        1 -> "DETAIL"
        else -> "FOLLOW UP"
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = "Jadwal ${schedule.idListing.ifBlank { "Detail" }}",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                        )
                        Text(
                            text = when (pagerState.currentPage) {
                                0 -> "Halaman Edit Jadwal"
                                1 -> "Halaman Detail"
                                else -> "Halaman Follow Up WhatsApp"
                            },
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.outline
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onDismiss) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Kembali ke Dashboard"
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surfaceColorAtElevation(3.dp)
                )
            )
        },
        bottomBar = {
            NavigationBar(
                containerColor = Color.Transparent,
                tonalElevation = 0.dp,
                modifier = Modifier.height(72.dp)
            ) {
                NavigationBarItem(
                    selected = pagerState.currentPage == 0,
                    onClick = { coroutineScope.launch { pagerState.animateScrollToPage(0) } },
                    icon = { Icon(Icons.Default.Edit, contentDescription = "Edit Foto") },
                    label = { Text("Edit Foto", style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold)) }
                )
                NavigationBarItem(
                    selected = pagerState.currentPage == 1,
                    onClick = { coroutineScope.launch { pagerState.animateScrollToPage(1) } },
                    icon = { Icon(Icons.Default.Info, contentDescription = "Detail Listing") },
                    label = { Text("Detail Listing", style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold)) }
                )
                NavigationBarItem(
                    selected = pagerState.currentPage == 2,
                    onClick = { coroutineScope.launch { pagerState.animateScrollToPage(2) } },
                    icon = { Icon(Icons.Default.Chat, contentDescription = "Follow Up") },
                    label = { Text("Follow Up", style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold)) }
                )
            }
        },
        modifier = modifier.fillMaxSize().testTag("detail_pager_screen")
    ) { innerPadding ->
        HorizontalPager(
            state = pagerState,
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) { pageIndex ->
            when (pageIndex) {
                0 -> PageEdit(
                    schedule = schedule,
                    viewModel = viewModel,
                    onSaveSuccess = {
                        coroutineScope.launch {
                            Toast.makeText(context, "Perubahan berhasil disimpan lokal & Sheets!", Toast.LENGTH_SHORT).show()
                            pagerState.animateScrollToPage(1)
                        }
                    },
                    onBack = {
                        coroutineScope.launch {
                            pagerState.animateScrollToPage(1)
                        }
                    }
                )
                1 -> PageDetailHome(
                    schedule = schedule,
                    listingImagesMap = listingImagesMap,
                    listingImagesGalleryMap = listingImagesGalleryMap,
                    agentInfoMap = agentInfoMap,
                    listingPriceMap = listingPriceMap,
                    listingDescMap = listingDescMap,
                    onNavigateToEdit = { coroutineScope.launch { pagerState.animateScrollToPage(0) } },
                    onNavigateToFollowUp = { coroutineScope.launch { pagerState.animateScrollToPage(2) } },
                    onBack = onDismiss
                )
                2 -> PageFollowUp(
                    schedule = schedule,
                    agentInfoMap = agentInfoMap,
                    onBack = {
                        coroutineScope.launch {
                            pagerState.animateScrollToPage(1)
                        }
                    }
                )
            }
        }
    }
}

// ============================================
// PAGE 0 : EDIT PANEL
// ============================================
@Composable
fun PageEdit(
    schedule: Schedule,
    viewModel: ScheduleViewModel,
    onSaveSuccess: () -> Unit,
    onBack: () -> Unit = {}
) {
    var idListing by remember { mutableStateOf(schedule.idListing) }
    var namaMe by remember { mutableStateOf(schedule.namaMe) }
    var lokasi by remember { mutableStateOf(schedule.lokasi) }
    var tanggal by remember { mutableStateOf(schedule.tanggal) }
    var jam by remember { mutableStateOf(schedule.jam) }
    var staff by remember { mutableStateOf(schedule.staff) }
    var type by remember { mutableStateOf(schedule.type) }
    var status by remember { mutableStateOf(schedule.status) }
    
    var isSaving by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f))
        ) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(14.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Icon(Icons.Default.EditCalendar, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                Text(
                    text = "Edit Parameter Jadwal Listing",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                )
            }
        }

        OutlinedTextField(
            value = idListing,
            onValueChange = { idListing = it },
            label = { Text("ID Listing (Contoh: L-2241)") },
            leadingIcon = { Icon(Icons.Default.Bookmark, contentDescription = null) },
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp)
        )

        OutlinedTextField(
            value = namaMe,
            onValueChange = { namaMe = it },
            label = { Text("Nama Marketing Executive (ME)") },
            leadingIcon = { Icon(Icons.Default.Person, contentDescription = null) },
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp)
        )

        OutlinedTextField(
            value = lokasi,
            onValueChange = { lokasi = it },
            label = { Text("Lokasi (Alamat properti / Cluster)") },
            leadingIcon = { Icon(Icons.Default.LocationOn, contentDescription = null) },
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp)
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            OutlinedTextField(
                value = tanggal,
                onValueChange = { tanggal = it },
                label = { Text("Tanggal (YYYY-MM-DD)") },
                leadingIcon = { Icon(Icons.Default.CalendarToday, contentDescription = null) },
                modifier = Modifier.weight(1.5f),
                shape = RoundedCornerShape(12.dp)
            )

            OutlinedTextField(
                value = jam,
                onValueChange = { jam = it },
                label = { Text("Jam (HH:MM)") },
                leadingIcon = { Icon(Icons.Default.AccessTime, contentDescription = null) },
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(12.dp)
            )
        }

        OutlinedTextField(
            value = staff,
            onValueChange = { staff = it },
            label = { Text("Runner / Staff ditugaskan") },
            leadingIcon = { Icon(Icons.Default.DirectionsRun, contentDescription = null) },
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp)
        )

        // Type Select Presets Row
        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text("Tipe Kegiatan:", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                listOf("Foto", "Video", "Done", "Drone").forEach { preset ->
                    FilterChip(
                        selected = type == preset,
                        onClick = { type = preset },
                        label = { Text(preset) },
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }

        // Status Select Presets Grid
        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text("Status Pemotretan:", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
            val rows = listOf(
                listOf("Pending", "In Progress", "Done"),
                listOf("Garis Tanah", "Up Foto", "Edit Video")
            )
            rows.forEach { rowPresets ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    rowPresets.forEach { preset ->
                        FilterChip(
                            selected = status == preset,
                            onClick = { status = preset },
                            label = { Text(preset) },
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        Button(
            onClick = {
                isSaving = true
                viewModel.updateScheduleDirectly(
                    schedule = schedule.copy(
                        idListing = idListing.trim(),
                        namaMe = namaMe.trim(),
                        lokasi = lokasi.trim(),
                        tanggal = tanggal.trim(),
                        jam = jam.trim(),
                        staff = staff.trim(),
                        type = type.trim(),
                        status = status.trim(),
                        synced = false
                    ),
                    original = schedule
                )
                isSaving = false
                onSaveSuccess()
            },
            modifier = Modifier.fillMaxWidth().height(56.dp).testTag("save_edit_button"),
            shape = RoundedCornerShape(12.dp),
            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(Icons.Default.Save, contentDescription = null)
                Text(
                    text = if (isSaving) "Menyimpan..." else "Simpan Perubahan",
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp
                )
            }
        }

        OutlinedButton(
            onClick = onBack,
            modifier = Modifier.fillMaxWidth().height(54.dp),
            shape = RoundedCornerShape(12.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Kembali")
                Text("Kembali ke Detail Properti", fontWeight = FontWeight.Bold)
            }
        }
        
        Spacer(modifier = Modifier.height(40.dp))
    }
}

// ============================================
// PAGE 1 : DETAIL HOME
// ============================================
@Composable
fun PageDetailHome(
    schedule: Schedule,
    listingImagesMap: Map<String, String>,
    listingImagesGalleryMap: Map<String, List<String>>,
    agentInfoMap: Map<String, AgentInfo>,
    listingPriceMap: Map<String, String>,
    listingDescMap: Map<String, String>,
    onNavigateToEdit: () -> Unit,
    onNavigateToFollowUp: () -> Unit,
    onBack: () -> Unit = {}
) {
    val cleanId = schedule.idListing.trim()
    val galleryList = if (cleanId.isNotBlank()) listingImagesGalleryMap[cleanId] ?: emptyList() else emptyList()
    val fallbackImg = if (cleanId.isNotBlank()) listingImagesMap[cleanId] else null
    val imagesToDisplay = if (galleryList.isNotEmpty()) galleryList else listOfNotNull(fallbackImg)
    
    val agentInfo = if (cleanId.isNotBlank()) agentInfoMap[cleanId] else null
    
    val finalAgentName = agentInfo?.name?.ifBlank { schedule.namaMe } ?: schedule.namaMe
    var finalAgentPhone = getAgentPhoneByName(finalAgentName).ifBlank { getAgentPhoneByName(schedule.namaMe) }
    if (finalAgentPhone.isBlank()) {
        finalAgentPhone = "085169671344" // Default Office fallback
    }

    // Retrieve scraped price & description
    val price = listingPriceMap[cleanId] ?: "Rp. Hubungi Agent"
    val rawDesc = listingDescMap[cleanId] ?: "Deskripsi tidak tersedia di websiteListing. Silakan hubungi agent terkait."
    val cleanedDesc = cleanListingDescription(rawDesc)
    val description = cleanedDesc
        .replace("\r", "")
        .replace("(?m)^[ \t]*\r?\n".toRegex(), "\n")
        .replace(" {2,}".toRegex(), " ")
        .trim()

    val isRent = price.lowercase().contains("sewa") || 
                 price.lowercase().contains("/th") || 
                 price.lowercase().contains("/bln") || 
                 description.lowercase().contains("disewakan") || 
                 description.lowercase().contains("sewa ") || 
                 description.lowercase().contains("rental") ||
                 schedule.type.lowercase().contains("sewa") ||
                 schedule.type.lowercase().contains("rent")
    
    val priceHeaderLabel = if (isRent) "For Rent" else "For Sale"

    // Marketing photo is the last image of the gallery
    val marketingImg = if (imagesToDisplay.size > 1) imagesToDisplay.last() else null

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        
        // 1. Sliding property photo gallery
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(
                text = "Galeri Foto Properti",
                style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.primary
            )
            
            LazyRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // If we have multiple images, filter out the last one if it's the marketing photo
                val carouselImages = if (marketingImg != null && imagesToDisplay.size > 1) {
                    imagesToDisplay.dropLast(1)
                } else {
                    imagesToDisplay
                }

                itemsIndexed(carouselImages) { index, img ->
                    Card(
                        modifier = Modifier
                            .width(260.dp)
                            .height(180.dp),
                        shape = RoundedCornerShape(16.dp),
                        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
                    ) {
                        Box(modifier = Modifier.fillMaxSize()) {
                            AsyncImage(
                                model = img,
                                contentDescription = "Foto Listing $cleanId - Slide ${index+1}",
                                modifier = Modifier.fillMaxSize(),
                                contentScale = ContentScale.Crop
                            )
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .align(Alignment.BottomCenter)
                                    .background(Color.Black.copy(alpha = 0.4f))
                                    .padding(vertical = 4.dp)
                            ) {
                                Text(
                                    text = "Slide ${index + 1} dari ${carouselImages.size}",
                                    color = Color.White,
                                    fontSize = 11.sp,
                                    modifier = Modifier.fillMaxWidth(),
                                    textAlign = TextAlign.Center
                                )
                            }
                        }
                    }
                }
            }
        }
        
        // 2. Marketing Executive (ME) Full Width Card
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceColorAtElevation(2.dp)),
            elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
        ) {
            Row(
                modifier = Modifier.padding(16.dp),
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Agent Avatar / Marketing Photo (Rounded Circle/Box)
                Card(
                    modifier = Modifier.size(80.dp),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Box(modifier = Modifier.fillMaxSize()) {
                        if (marketingImg != null) {
                            AsyncImage(
                                model = marketingImg,
                                contentDescription = "Foto Marketing ME",
                                modifier = Modifier.fillMaxSize(),
                                contentScale = ContentScale.Crop
                            )
                        } else if (agentInfo?.avatarUrl?.isNotBlank() == true) {
                            AsyncImage(
                                model = agentInfo.avatarUrl,
                                contentDescription = "Foto Agent",
                                modifier = Modifier.fillMaxSize(),
                                contentScale = ContentScale.Crop
                            )
                        } else {
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .background(MaterialTheme.colorScheme.primaryContainer),
                                contentAlignment = Alignment.Center
                            ) {
                                val initials = finalAgentName.split(" ")
                                    .filter { it.isNotBlank() }
                                    .take(2)
                                    .map { it.first().uppercaseChar() }
                                    .joinToString("")
                                Text(
                                    text = initials.ifEmpty { "ME" },
                                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.ExtraBold),
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                        }
                    }
                }

                // Agent Contact Info
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(
                        text = "Marketing Executive (ME)",
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.primary
                    )
                    
                    Text(
                        text = finalAgentName.ifBlank { "ME Ray White" },
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.ExtraBold),
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )

                    val finalEmail = agentInfo?.email?.ifBlank { getAgentEmailByName(finalAgentName) } ?: getAgentEmailByName(finalAgentName)
                    val finalIg = agentInfo?.instagram?.ifBlank { getAgentInstagramByName(finalAgentName) } ?: getAgentInstagramByName(finalAgentName)

                    // WhatsApp Row
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Call,
                            contentDescription = "WhatsApp",
                            tint = Color(0xFF25D366),
                            modifier = Modifier.size(14.dp)
                        )
                        Text(
                            text = finalAgentPhone,
                            style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }

                    // Email Row
                    if (finalEmail.isNotBlank()) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Email,
                                contentDescription = "Email",
                                tint = Color(0xFFEA4335),
                                modifier = Modifier.size(14.dp)
                            )
                            Text(
                                text = finalEmail,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }

                    // Instagram Row
                    if (finalIg.isNotBlank()) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.AlternateEmail,
                                contentDescription = "Instagram",
                                tint = Color(0xFFE1306C),
                                modifier = Modifier.size(14.dp)
                            )
                            Text(
                                text = finalIg,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }
            }
        }

        // 3. Pricing Card (Full Width)
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.35f)),
            shape = RoundedCornerShape(16.dp)
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    text = priceHeaderLabel,
                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.primary
                )
                Text(
                    text = price,
                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Black),
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
            }
        }

        // 4. Deskripsi Card (Full Width - Text displays perfectly with wrap)
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f))
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = "Deskripsi Properti",
                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.primary
                )
                Text(
                    text = description,
                    style = MaterialTheme.typography.bodyMedium.copy(lineHeight = 20.sp),
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        
        // Removed guidance slide card indicator as requested by user - bottom navigation is already fixed below.

        // 4. Details parameters info card
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            Column(
                modifier = Modifier.padding(18.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Informasi Kegiatan",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.ExtraBold),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    
                    val isDone = schedule.status.lowercase().contains("done") || schedule.type.lowercase().contains("done")
                    Box(
                        modifier = Modifier
                            .background(
                                color = if (isDone) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.secondaryContainer,
                                shape = RoundedCornerShape(100.dp)
                            )
                            .padding(horizontal = 10.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = schedule.status,
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                            color = if (isDone) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSecondaryContainer
                        )
                    }
                }
                
                HorizontalDivider()
                
                // Fields
                InfoRow(label = "ID Listing", value = cleanId.ifBlank { "Unassigned" })
                InfoRow(label = "Kategori / Tipe", value = schedule.type)
                InfoRow(label = "Lokasi", value = schedule.lokasi)
                InfoRow(label = "Tanggal Sesi", value = formatIndonesianDate(schedule.tanggal))
                InfoRow(label = "Waktu Sesi", value = formatTwelveHourTime(schedule.jam))
                InfoRow(label = "Runner (Staff)", value = schedule.staff)
                InfoRow(label = "Sinkronisasi Sheets", value = if (schedule.synced) "Sudah Sinkron" else "Tertunda (Sinyal/Offline)")
            }
        }

        OutlinedButton(
            onClick = onBack,
            modifier = Modifier.fillMaxWidth().height(54.dp),
            shape = RoundedCornerShape(12.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Kembali")
                Text("Kembali ke Dashboard", fontWeight = FontWeight.Bold)
            }
        }
        
        Spacer(modifier = Modifier.height(40.dp))
    }
}

// Helper info row component
@Composable
fun InfoRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(16.dp),
        verticalAlignment = Alignment.Top
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.outline,
            modifier = Modifier.width(100.dp)
        )
        val isEmptyValue = value.isBlank() || value.trim() == "-"
        Text(
            text = if (isEmptyValue) "(belum di input)" else value,
            style = MaterialTheme.typography.bodyMedium.copy(
                fontWeight = if (isEmptyValue) FontWeight.Normal else FontWeight.SemiBold
            ),
            color = if (isEmptyValue) MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f) else MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.weight(1f)
        )
    }
}

// ============================================
// PAGE 2 : FOLLOW UP PANEL
// ============================================
@Composable
fun PageFollowUp(
    schedule: Schedule,
    agentInfoMap: Map<String, AgentInfo>,
    onBack: () -> Unit = {}
) {
    val context = LocalContext.current
    val cleanId = schedule.idListing.trim()
    val agentInfo = if (cleanId.isNotBlank()) agentInfoMap[cleanId] else null
    
    val finalAgentName = agentInfo?.name?.ifBlank { schedule.namaMe } ?: schedule.namaMe
    var finalAgentPhone = getAgentPhoneByName(finalAgentName).ifBlank { getAgentPhoneByName(schedule.namaMe) }
    
    var cleanPhone = finalAgentPhone.replace("[^\\d]".toRegex(), "")
    if (cleanPhone.startsWith("0")) {
        cleanPhone = "62" + cleanPhone.substring(1)
    }
    if (cleanPhone.isBlank()) {
        cleanPhone = "6285169671344" // Default Office line
    }

    val formattedDate = formatIndonesianDate(schedule.tanggal)
    val formattedTime = formatTwelveHourTime(schedule.jam)

    val templateMessage = """
Halo ${finalAgentName.ifBlank { "ME Ray White" }}, saya dari Tim RWC Foto.

Berikut info lengkap jadwal kegiatan untuk properti Anda:
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
    
    var messageText by remember { mutableStateOf(templateMessage) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f))
        ) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Icon(
                        imageVector = Icons.Default.ContactPhone,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        "Kontak Penerima",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                    )
                }
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Penerima: $finalAgentName",
                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Black)
                )
                Text(
                    text = "No. WhatsApp: $finalAgentPhone",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.outline
                )
            }
        }

        // Message text editor
        OutlinedTextField(
            value = messageText,
            onValueChange = { messageText = it },
            label = { Text("Pesan Follow Up (Siap Dikirim)") },
            modifier = Modifier.fillMaxWidth().height(220.dp),
            shape = RoundedCornerShape(16.dp),
            textStyle = MaterialTheme.typography.bodySmall
        )
        
        Text(
            text = "Pilih jenis aplikasi WhatsApp untuk membuka chat personal:",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.outline,
            modifier = Modifier.fillMaxWidth()
        )

        // WhatsApp trigger handles
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Button(
                onClick = {
                    val uri = Uri.parse("https://api.whatsapp.com/send?phone=$cleanPhone&text=${Uri.encode(messageText)}")
                    val intent = Intent(Intent.ACTION_VIEW, uri).apply {
                        setPackage("com.whatsapp")
                    }
                    try {
                        context.startActivity(intent)
                    } catch (e: Exception) {
                        try {
                            val fallbackIntent = Intent(Intent.ACTION_VIEW, uri)
                            context.startActivity(fallbackIntent)
                        } catch (e2: Exception) {
                            Toast.makeText(context, "Gagal meluncurkan aplikasi WhatsApp", Toast.LENGTH_SHORT).show()
                        }
                    }
                },
                modifier = Modifier.weight(1f).height(54.dp),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF25D366))
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Icon(Icons.AutoMirrored.Filled.Send, contentDescription = null, tint = Color.White)
                    Text("WhatsApp", color = Color.White, fontWeight = FontWeight.Bold)
                }
            }

            Button(
                onClick = {
                    val uri = Uri.parse("https://api.whatsapp.com/send?phone=$cleanPhone&text=${Uri.encode(messageText)}")
                    val intent = Intent(Intent.ACTION_VIEW, uri).apply {
                        setPackage("com.whatsapp.w4b")
                    }
                    try {
                        context.startActivity(intent)
                    } catch (e: Exception) {
                        try {
                            val fallbackIntent = Intent(Intent.ACTION_VIEW, uri)
                            context.startActivity(fallbackIntent)
                        } catch (e2: Exception) {
                            Toast.makeText(context, "Gagal meluncurkan aplikasi WhatsApp Business", Toast.LENGTH_SHORT).show()
                        }
                    }
                },
                modifier = Modifier.weight(1f).height(54.dp),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF075E54))
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Icon(Icons.Default.SendToMobile, contentDescription = null, tint = Color.White)
                    Text("WA Business", color = Color.White, fontWeight = FontWeight.Bold)
                }
            }
        }

        OutlinedButton(
            onClick = onBack,
            modifier = Modifier.fillMaxWidth().height(54.dp),
            shape = RoundedCornerShape(12.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Kembali")
                Text("Kembali ke Detail Properti", fontWeight = FontWeight.Bold)
            }
        }
    }
}

// Helper conversions are loaded from DashboardScreen package namespace
