package com.example.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.*
import com.example.network.GeneralResponse
import com.example.network.SheetsApiService
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.TimeUnit

data class AgentInfo(
    val name: String = "",
    val phone: String = "",
    val waUrl: String = ""
)

fun getAgentPhoneByName(meName: String): String {
    val clean = meName.trim().lowercase()
    return when {
        clean.contains("dhavit") || clean.contains("dhavid") || clean.contains("david") || clean.contains("valentino") -> "08561103735"
        clean.contains("syafruddin") || clean.contains("sfrd") || clean.contains("syafru") || clean.contains("udin") -> "08129525287"
        clean.contains("raffa") || clean.contains("rafa") -> "08561103735"
        clean.contains("yudhi") || clean.contains("yudi") -> "0811988978"
        clean.contains("herry") || clean.contains("heri") -> "0811800100"
        clean.contains("yusri") -> "08121088711"
        clean.contains("tika") -> "08128765432"
        clean.contains("siti") -> "081234567890"
        clean.contains("dewi") -> "081388881234"
        clean.contains("sari") -> "08129876543"
        clean.contains("indah") -> "08151234567"
        clean.contains("budi") -> "08128456789"
        else -> ""
    }
}

class ScheduleViewModel(application: Application) : AndroidViewModel(application) {

    private val db = AppDatabase.getDatabase(application)
    private val preferenceManager = PreferenceManager(application)
    
    // Set up Retrofit dynamically
    private val apiService: SheetsApiService by lazy {
        val logging = HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.BODY
        }
        val okHttpClient = OkHttpClient.Builder()
            .connectTimeout(15, TimeUnit.SECONDS)
            .readTimeout(15, TimeUnit.SECONDS)
            .addInterceptor(logging)
            .build()

        val moshi = Moshi.Builder()
            .addLast(KotlinJsonAdapterFactory())
            .build()

        Retrofit.Builder()
            .baseUrl("https://script.google.com/") // Placeholder, URL bypassed by @Url
            .client(okHttpClient)
            .addConverterFactory(MoshiConverterFactory.create(moshi))
            .build()
            .create(SheetsApiService::class.java)
    }

    private val repository = ScheduleRepository(db.scheduleDao(), apiService, preferenceManager)

    // Form inputs state
    var formListingId = MutableStateFlow("")
    var formNamaMe = MutableStateFlow("")
    var formLokasi = MutableStateFlow("")
    var formTanggal = MutableStateFlow("")
    var formJam = MutableStateFlow("")
    var formStaff = MutableStateFlow("")
    var formType = MutableStateFlow("Foto")
    var formStatus = MutableStateFlow("Pending")
    var editingSchedule = MutableStateFlow<Schedule?>(null)

    // Current category tab state shared between MainActivity and Dashboard
    var selectedCategory = MutableStateFlow(ScheduleCategory.AKTIF)

    // Filter controls state
    var searchQuery = MutableStateFlow("")
    var filterStaff = MutableStateFlow("Semua")
    var filterStartDate = MutableStateFlow<String?>(null)
    var filterEndDate = MutableStateFlow<String?>(null)

    // Settings inputs
    var appsScriptUrl = MutableStateFlow(preferenceManager.appsScriptUrl)
    var spreadsheetId = MutableStateFlow(preferenceManager.spreadsheetId)

    // Cache for scraper image URLs: idListing -> Image URL
    private val _listingImagesMap = MutableStateFlow<Map<String, String>>(emptyMap())
    val listingImagesMap: StateFlow<Map<String, String>> = _listingImagesMap.asStateFlow()

    // Cache for scraper all image URLs (Gallery): idListing -> List of image URLs
    private val _listingImagesGalleryMap = MutableStateFlow<Map<String, List<String>>>(emptyMap())
    val listingImagesGalleryMap: StateFlow<Map<String, List<String>>> = _listingImagesGalleryMap.asStateFlow()

    // Cache for scraped Agent/ME Contact Info: idListing -> AgentInfo
    private val _agentInfoMap = MutableStateFlow<Map<String, AgentInfo>>(emptyMap())
    val agentInfoMap: StateFlow<Map<String, AgentInfo>> = _agentInfoMap.asStateFlow()

    fun fetchListingImageIfNeeded(idListing: String, defaultMeName: String = "") {
        val cleanId = idListing.trim()
        if (cleanId.isBlank()) return
        if (_listingImagesMap.value.containsKey(cleanId) && _listingImagesGalleryMap.value.containsKey(cleanId)) return

        viewModelScope.launch(kotlinx.coroutines.Dispatchers.IO) {
            val url = "https://raywhitecipete.net/ListingView/Detail/$cleanId"
            try {
                val client = okhttp3.OkHttpClient.Builder()
                    .connectTimeout(10, java.util.concurrent.TimeUnit.SECONDS)
                    .readTimeout(10, java.util.concurrent.TimeUnit.SECONDS)
                    .build()
                val request = okhttp3.Request.Builder()
                    .url(url)
                    .header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/115.0.0.0 Safari/537.36")
                    .build()
                client.newCall(request).execute().use { response ->
                    if (response.isSuccessful) {
                        val html = response.body?.string() ?: ""
                        
                        // Parse Images
                        val imgRegex = """<img[^>]+src=["']([^"']+)["']""".toRegex(RegexOption.IGNORE_CASE)
                        val matches = imgRegex.findAll(html)
                        
                        val candidateUrls = matches.map { it.groupValues[1] }.toList()
                        val allImages = mutableListOf<String>()
                        
                        // Pick out all valid property gallery/upload images
                        for (src in candidateUrls) {
                            val lower = src.lowercase()
                            if (!lower.contains("logo") &&
                                !lower.contains("icon") &&
                                !lower.contains("avatar") &&
                                !lower.contains("marker") &&
                                !lower.contains("theme") &&
                                !lower.contains("banner") &&
                                !lower.contains("assets") &&
                                !lower.contains("raywhite") &&
                                !lower.contains("social") &&
                                !lower.endsWith(".svg") &&
                                (src.startsWith("http") || src.startsWith("/"))) {
                                
                                val fullUrl = if (src.startsWith("/")) {
                                    "https://raywhitecipete.net" + src
                                } else {
                                    src
                                }
                                if (!allImages.contains(fullUrl)) {
                                    allImages.add(fullUrl)
                                }
                            }
                        }
                        
                        // Parse WhatsApp / Agent Info
                        val waRegex = """(?:https?:)?//(?:api\.whatsapp\.com/send|wa\.me)/?[^\s"'>]*|whatsapp://[^\s"'>]*""".toRegex(RegexOption.IGNORE_CASE)
                        val waMatches = waRegex.findAll(html).map { it.value }.toList()
                        
                        var finalAgentName = defaultMeName.trim()
                        var finalAgentPhone = getAgentPhoneByName(defaultMeName)
                        var finalWaUrl = ""
                        
                        for (waUrl in waMatches) {
                            val decoded = try { java.net.URLDecoder.decode(waUrl, "UTF-8") } catch(e: Exception) { waUrl }
                            
                            // Extract phone
                            var phone = ""
                            val phoneMatch = """phone=([+\d]+)""".toRegex().find(waUrl)
                            if (phoneMatch != null) {
                                phone = phoneMatch.groupValues[1]
                            } else {
                                val waMeMatch = """wa\.me/([+\d]+)""".toRegex().find(waUrl)
                                if (waMeMatch != null) {
                                    phone = waMeMatch.groupValues[1]
                                }
                            }
                            
                            if (phone.isBlank()) {
                                val numMatch = """08\d{8,11}""".toRegex().find(decoded)
                                if (numMatch != null) {
                                    phone = numMatch.value
                                }
                            }
                            
                            // Extract Name
                            var name = ""
                            val hubungiMatch = """Hubungi:[ \n\r]*([A-Za-z ]+)[, \n\r]""".toRegex(RegexOption.IGNORE_CASE).find(decoded)
                            if (hubungiMatch != null) {
                                name = hubungiMatch.groupValues[1].trim()
                            }
                            
                            if (phone.isNotBlank()) {
                                finalAgentPhone = phone
                                if (name.isNotBlank()) {
                                    finalAgentName = name
                                }
                                finalWaUrl = waUrl
                                break
                            }
                        }
                        
                        // Text heuristics search/fallback for phone and name if no explicit Wa link worked
                        if (finalAgentPhone.isBlank()) {
                            val phoneRegex = """(?:08|\+628|628)\d{1,4}[-.\s]?\d{3,4}[-.\s]?\d{3,4}""".toRegex()
                            val match = phoneRegex.find(html)
                            if (match != null) {
                                finalAgentPhone = match.groupValues[0].replace("[-\\s\\+]".toRegex(), "")
                                val startIdx = maxOf(0, match.range.first - 150)
                                val context = html.substring(startIdx, match.range.first)
                                val cleanContext = context.replace("<[^>]*>".toRegex(), " ")
                                val nameMatch = """Hubungi\s*:\s*([A-Za-z\s]+)""".toRegex(RegexOption.IGNORE_CASE).find(cleanContext)
                                if (nameMatch != null) {
                                    finalAgentName = nameMatch.groupValues[1].trim()
                                } else {
                                    val words = cleanContext.trim().split("\\s+".toRegex())
                                    if (words.isNotEmpty()) {
                                        val potentialName = words.takeLast(4).filter { it.firstOrNull()?.isUpperCase() == true && it.length > 2 }.joinToString(" ")
                                        if (potentialName.isNotBlank() && !potentialName.contains("Share", ignoreCase = true) && !potentialName.contains("Rp", ignoreCase = true)) {
                                            finalAgentName = potentialName
                                        }
                                    }
                                }
                            }
                        }
                        
                        // If phone number is still empty, let's fall back to our local directory from name
                        if (finalAgentPhone.isBlank() && finalAgentName.isNotBlank()) {
                            finalAgentPhone = getAgentPhoneByName(finalAgentName)
                        }
                        
                        // Update cache with results
                        if (allImages.isNotEmpty()) {
                            _listingImagesMap.update { current ->
                                current + (cleanId to allImages.first())
                            }
                            _listingImagesGalleryMap.update { current ->
                                current + (cleanId to allImages)
                            }
                        }
                        
                        val agentInfo = AgentInfo(
                            name = finalAgentName,
                            phone = finalAgentPhone,
                            waUrl = finalWaUrl
                        )
                        _agentInfoMap.update { current ->
                            current + (cleanId to agentInfo)
                        }
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    // Status overlays
    private val _syncStatus = MutableStateFlow<SyncState>(SyncState.Idle)
    val syncStatus: StateFlow<SyncState> = _syncStatus.asStateFlow()

    private val _submitStatus = MutableStateFlow<SubmitState>(SubmitState.Idle)
    val submitStatus: StateFlow<SubmitState> = _submitStatus.asStateFlow()

    data class AppUpdate(
        val version: String = "",
        val downloadUrl: String = "",
        val changelog: String = ""
    )

    private val _appUpdateState = MutableStateFlow<AppUpdate?>(null)
    val appUpdateState: StateFlow<AppUpdate?> = _appUpdateState.asStateFlow()

    // Database entries
    val allSchedules: StateFlow<List<Schedule>> = repository.allSchedules
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Filtered schedules for dashboard using combined Flows
    val filteredSchedules: StateFlow<List<Schedule>> = combine(
        repository.allSchedules,
        searchQuery,
        filterStaff,
        filterStartDate,
        filterEndDate
    ) { list, query, staff, start, end ->
        list.filter { item ->
            val matchesQuery = query.isBlank() ||
                    item.namaMe.contains(query, ignoreCase = true) ||
                    item.idListing.contains(query, ignoreCase = true) ||
                    item.lokasi.contains(query, ignoreCase = true)
            
            val matchesStaff = staff == "Semua" || staff.isBlank() || item.staff.equals(staff, ignoreCase = true)
            
            // Dates are stored as "yyyy-MM-dd"
            val matchesDate = (start.isNullOrBlank() || item.tanggal >= start) &&
                    (end.isNullOrBlank() || item.tanggal <= end)
            
            matchesQuery && matchesStaff && matchesDate
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // List of all distinct staffs for filtering dropdown
    val staffList: StateFlow<List<String>> = repository.allSchedules
        .map { list ->
            val listStaffs = list.map { it.staff }.filter { it.isNotBlank() }.distinct().sorted()
            listOf("Semua") + listStaffs
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), listOf("Semua"))

    // Preset staff choices for form autocompletion
    val defaultStaffPresets = listOf(
        "Doni Kusuma",
        "Randi Pratama",
        "Anita Wijaya",
        "Yusuf Hakim",
        "Rina Selvia",
        "Eko Prasetyo"
    )

    init {
        // Initialize with seeds and configure form defaults
        viewModelScope.launch {
            repository.seedMockDataIfEmpty()
            if (preferenceManager.appsScriptUrl.isNotBlank()) {
                syncData()
            }
            // Real-time background sync loop every 10 seconds
            while (true) {
                kotlinx.coroutines.delay(10000)
                if (preferenceManager.appsScriptUrl.isNotBlank() && _syncStatus.value !is SyncState.Loading) {
                    syncDataSilently()
                }
            }
        }
        
        // Listen to DB for VERSION_CHECK and update version status
        viewModelScope.launch {
            repository.allSchedules.collect { list ->
                val verItem = list.find { it.idListing.trim() == "VERSION_CHECK" }
                if (verItem != null) {
                    val serverVer = verItem.namaMe.trim()
                    if (isNewerVersion(serverVer, "1.3")) {
                        _appUpdateState.value = AppUpdate(
                            version = serverVer,
                            downloadUrl = verItem.lokasi,
                            changelog = verItem.staff
                        )
                    } else {
                        _appUpdateState.value = null
                    }
                } else {
                    _appUpdateState.value = null
                }
            }
        }
        resetForm()
    }

    fun isNewerVersion(server: String, current: String): Boolean {
        return try {
            val serverParts = server.trim().lowercase().removePrefix("v").split(".").mapNotNull { it.toIntOrNull() }
            val currentParts = current.trim().lowercase().removePrefix("v").split(".").mapNotNull { it.toIntOrNull() }
            for (i in 0 until maxOf(serverParts.size, currentParts.size)) {
                val serverVal = serverParts.getOrElse(i) { 0 }
                val currentVal = currentParts.getOrElse(i) { 0 }
                if (serverVal > currentVal) return true
                if (serverVal < currentVal) return false
            }
            false
        } catch (e: Exception) {
            server != current && server > current
        }
    }

    fun updateScheduleDirectly(schedule: Schedule) {
        viewModelScope.launch {
            _syncStatus.value = SyncState.Loading
            val original = schedule
            val result = repository.updateSchedule(schedule, original)
            result.onSuccess {
                _syncStatus.value = SyncState.Success("Jadwal Berhasil Diperbarui!")
                syncDataSilently()
            }.onFailure { err ->
                _syncStatus.value = SyncState.Error("Simpan Lokal Sukses (Sheets tertunda: ${err.message})")
                syncDataSilently()
            }
        }
    }

    fun resetForm() {
        val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        val timeFormat = SimpleDateFormat("HH:mm", Locale.getDefault())
        val now = Date()

        formListingId.value = ""
        formNamaMe.value = ""
        formLokasi.value = ""
        formTanggal.value = dateFormat.format(now)
        formJam.value = timeFormat.format(now)
        formStaff.value = ""
        formType.value = "Foto"
        formStatus.value = "Pending"
        editingSchedule.value = null
        _submitStatus.value = SubmitState.Idle
    }

    fun startEditing(schedule: Schedule) {
        editingSchedule.value = schedule
        formListingId.value = schedule.idListing
        formNamaMe.value = schedule.namaMe
        formLokasi.value = schedule.lokasi
        formTanggal.value = schedule.tanggal
        formJam.value = schedule.jam
        formStaff.value = schedule.staff
        formType.value = schedule.type
        formStatus.value = schedule.status
        _submitStatus.value = SubmitState.Idle
    }

    // Update settings in PreferenceManager
    fun saveSettings(url: String, sheetId: String) {
        preferenceManager.appsScriptUrl = url
        preferenceManager.spreadsheetId = sheetId
        appsScriptUrl.value = url
        spreadsheetId.value = sheetId
        
        // After updating settings, let's sync immediately if url is provided
        if (url.isNotBlank()) {
            syncData()
        }
    }

    // Sync from Google Sheets (2-way: upload pending first, then fetch latest)
    fun syncData() {
        viewModelScope.launch {
            _syncStatus.value = SyncState.Loading
            
            // Step 1: Upload any unsynced local schedules
            var pendingUploaded = 0
            val uploadResult = repository.syncPendingSchedules()
            uploadResult.onSuccess { count ->
                pendingUploaded = count
            }
            
            // Step 2: Fetch all latest entries from Google Sheets
            val result = repository.syncFromGoogleSheets()
            result.onSuccess {
                val msg = if (pendingUploaded > 0) {
                    "Disinkronkan: $pendingUploaded data diunggah & data terbaru ditarik!"
                } else {
                    "Sinkronisasi selesai! Semua data sudah terbaru."
                }
                _syncStatus.value = SyncState.Success(msg)
            }.onFailure { err ->
                _syncStatus.value = SyncState.Error("Gagal mengambil data terbaru: ${err.localizedMessage ?: "Masalah koneksi"}")
            }
        }
    }

    // Silent real-time sync for background updates without disrupting UI states
    fun syncDataSilently() {
        viewModelScope.launch {
            repository.syncPendingSchedules()
            repository.syncFromGoogleSheets()
        }
    }

    // Clear local database cache
    fun clearCache() {
        viewModelScope.launch {
            repository.clearLocal()
        }
    }

    // Submits schedule (handles both dynamic adding and updating/editing)
    fun submitSchedule() {
        val listingId = formListingId.value.trim()
        val namaMe = formNamaMe.value.trim()
        val lokasi = formLokasi.value.trim()
        val tanggal = formTanggal.value.trim()
        val jam = formJam.value.trim()
        val staff = formStaff.value.trim()
        val type = formType.value.trim()
        val status = formStatus.value.trim()

        if (listingId.isEmpty() && (namaMe.isEmpty() || lokasi.isEmpty())) {
            _submitStatus.value = SubmitState.Error("Masukkan ID Listing, atau isi Nama ME & Lokasi jika ID Listing dikosongkan.")
            return
        }
        if (tanggal.isEmpty() || jam.isEmpty() || staff.isEmpty()) {
            _submitStatus.value = SubmitState.Error("Tanggal, Jam, dan Staff wajib diisi!")
            return
        }

        val original = editingSchedule.value
        val isEditMode = original != null

        viewModelScope.launch {
            _submitStatus.value = SubmitState.Loading
            if (isEditMode && original != null) {
                val updatedSchedule = original.copy(
                    idListing = listingId,
                    namaMe = namaMe,
                    lokasi = lokasi,
                    tanggal = tanggal,
                    jam = jam,
                    staff = staff,
                    type = type,
                    status = status,
                    synced = false
                )
                val result = repository.updateSchedule(updatedSchedule, original)
                result.onSuccess {
                    _submitStatus.value = SubmitState.Success
                    resetForm()
                }.onFailure { err ->
                    _submitStatus.value = SubmitState.Error(
                        "Tersimpan di Lokal (Gagal sinkronisasi ke Sheets: ${err.localizedMessage ?: "Masalah jaringan"})"
                    )
                }
            } else {
                val schedule = Schedule(
                    idListing = listingId,
                    namaMe = namaMe,
                    lokasi = lokasi,
                    tanggal = tanggal,
                    jam = jam,
                    staff = staff,
                    type = type,
                    status = status,
                    synced = false
                )

                val result = repository.addSchedule(schedule)
                result.onSuccess {
                    _submitStatus.value = SubmitState.Success
                    resetForm()
                }.onFailure { err ->
                    _submitStatus.value = SubmitState.Error(
                        "Tersimpan di Lokal (Gagal sinkronisasi ke Sheets: ${err.localizedMessage ?: "Masalah jaringan"})"
                    )
                }
            }
        }
    }

    // Retry sending unsynced entries
    fun retryUnsyncedSchedules() {
        viewModelScope.launch {
            _syncStatus.value = SyncState.Loading
            val result = repository.syncPendingSchedules()
            result.onSuccess { count ->
                if (count > 0) {
                    _syncStatus.value = SyncState.Success("$count jadwal tertunda berhasil disinkronisasi!")
                    syncData()
                } else {
                    _syncStatus.value = SyncState.Success("Semua jadwal sudah tersinkronisasi.")
                }
            }.onFailure { err ->
                _syncStatus.value = SyncState.Error("Gagal sinkronisasi antrean: ${err.message}")
            }
        }
    }

    fun deleteSchedule(schedule: Schedule) {
        viewModelScope.launch {
            _syncStatus.value = SyncState.Loading
            val result = repository.deleteSchedule(schedule)
            result.onSuccess {
                _syncStatus.value = SyncState.Success("Jadwal '${schedule.idListing.ifBlank { schedule.namaMe }}' berhasil dihapus lokalan dan online!")
            }.onFailure { err ->
                _syncStatus.value = SyncState.Error("Terhapus di HP. Gagal menghapus dari Google Sheets online: ${err.message}")
            }
        }
    }

    fun dismissSyncStatus() {
        _syncStatus.value = SyncState.Idle
    }

    fun dismissSubmitStatus() {
        if (_submitStatus.value is SubmitState.Error) {
            _submitStatus.value = SubmitState.Idle
        }
    }
}

sealed interface SyncState {
    object Idle : SyncState
    object Loading : SyncState
    data class Success(val message: String) : SyncState
    data class Error(val message: String) : SyncState
}

sealed interface SubmitState {
    object Idle : SubmitState
    object Loading : SubmitState
    object Success : SubmitState
    data class Error(val message: String) : SubmitState
}

enum class ScheduleCategory(val displayName: String) {
    SEMUA("Semua"),
    AKTIF("Jadwal Aktif"),
    SELESAI("Jadwal Selesai"),
    TASK("Task")
}
