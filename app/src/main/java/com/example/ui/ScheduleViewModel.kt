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

    fun fetchListingImageIfNeeded(idListing: String) {
        val cleanId = idListing.trim()
        if (cleanId.isBlank()) return
        if (_listingImagesMap.value.containsKey(cleanId)) return

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
                        val imgRegex = """<img[^>]+src=["']([^"']+)["']""".toRegex(RegexOption.IGNORE_CASE)
                        val matches = imgRegex.findAll(html)
                        
                        // Gather all valid image sources
                        val candidateUrls = matches.map { it.groupValues[1] }.toList()
                        var foundUrl: String? = null
                        
                        // Strategy 1: Prioritize explicit property gallery images (usually located under uploads, property, or listings categories)
                        for (src in candidateUrls) {
                            val lower = src.lowercase()
                            if ((lower.contains("upload") || lower.contains("listing") || lower.contains("property") || lower.contains("media") || lower.contains("images")) &&
                                (lower.endsWith(".jpg") || lower.endsWith(".jpeg") || lower.endsWith(".png")) &&
                                !lower.contains("logo") && !lower.contains("icon") && !lower.contains("avatar") && !lower.contains("marker") && !lower.contains("raywhite")) {
                                foundUrl = if (src.startsWith("/")) {
                                    "https://raywhitecipete.net" + src
                                } else {
                                    src
                                }
                                break
                            }
                        }
                        
                        // Strategy 2: Graceful fallback filtering out all typical UI assets/logos/vectors
                        if (foundUrl == null) {
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
                                    
                                    foundUrl = if (src.startsWith("/")) {
                                        "https://raywhitecipete.net" + src
                                    } else {
                                        src
                                    }
                                    break
                                }
                            }
                        }
                        
                        if (foundUrl != null) {
                            _listingImagesMap.update { current ->
                                current + (cleanId to foundUrl)
                            }
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
        resetForm()
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
