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
    val waUrl: String = "",
    val avatarUrl: String = "",
    val email: String = "",
    val instagram: String = ""
)

data class AgentContact(
    val nameKey: String,
    val phone: String,
    val email: String,
    val instagram: String
)

val AGENT_CONTACT_LIST = listOf(
    AgentContact("donny", "087877777677", "donny.raywhite@yahoo.com", "donny.raywhite"),
    AgentContact("agung", "081808801688", "agung.rwcipete@gmail.com", "agungrwcipete"),
    AgentContact("bayu", "081287222799", "bayu.raywhite@gmail.com", "bayu.raywhite"),
    AgentContact("dian", "08161338093", "dians.raywhite@gmail.com", "south.jakarta.home"),
    AgentContact("dini", "081282331997", "diniwiryandoko@gmail.com", "diniraywhitecipete"),
    AgentContact("dutta", "081381564918", "duta.raywhite@gmail.com", "duttaraywhite"),
    AgentContact("duta", "081381564918", "duta.raywhite@gmail.com", "duttaraywhite"),
    AgentContact("ifa", "087885588897", "ifadebrianti.raywhite@gmail.com", "ifa.raywhite"),
    AgentContact("ike", "081808361616", "ikejuliastuti@gmail.com", "raywhite_ike"),
    AgentContact("ilham", "08561103735", "ilham.rwcipete@gmail.com", "ilhamsraywhite"),
    AgentContact("imelda", "082177888816", "imelda.djuarta@yahoo.com", "raywhite_imelda_djuarta"),
    AgentContact("iskandar", "08111823456", "iskandar.raywhite@gmail.com", "iskandar.raywhite"),
    AgentContact("remmy", "081286960275", "michael.rwcipete@gmail.com", ""),
    AgentContact("resmi", "081380620625", "resmi.raywhite@gmail.com", ""),
    AgentContact("sam", "081298070006", "sam.rwcipete@gmail.com", "samsuperagent"),
    AgentContact("santiaji", "085219698553", "ajisantiaji88@gmail.com", ""),
    AgentContact("vincent", "081212892189", "vincent@raywhitecipete.net", "vincentbrata"),
    AgentContact("yayan", "082114005670", "yayanhb2005@gmail.com", "yayanrahadiansyah"),
    AgentContact("haryadi", "08111373777", "haryadi.raywhite@gmail.com", "Haryadi.raywhite"),
    AgentContact("rony", "0811190046", "rony.raywhitecipete@gmail.com", ""),
    AgentContact("indah", "082125120021", "indah.raywhitecipete@gmail.com", "indah.raywhite"),
    AgentContact("dasep", "0818348046", "dasep.raywhite@gmail.com", "dasepraywhite"),
    AgentContact("ruby", "085779153217", "ruby.raywhite@gmail.com", "ruby.raywhite"),
    AgentContact("aii dyana", "081295951179", "aiidyana.raywhite@gmail.com", ""),
    AgentContact("dyana", "081295951179", "aiidyana.raywhite@gmail.com", ""),
    AgentContact("dedie", "082198909493", "dedie.rwc@gmail.com", "dedie.raywhite"),
    AgentContact("ayu", "", "ayu.raywhite@gmail.com", ""),
    AgentContact("dhenis", "08567773081", "dhenisemanuelraywhite@gmail.com", "dhenis.raywhite"),
    AgentContact("zulkifli", "081230016702", "zulkifli.raywhite@gmail.com", "zulkifli_rwc"),
    AgentContact("muljadi", "0811108308", "muljadi.raywhite@yahoo.com", ""),
    AgentContact("andika", "081932899091", "tan.andika@gmail.com", "andika.rwc"),
    AgentContact("mari", "08118087908", "marihariadi.raywhite@gmail.com", "mari.raywhite"),
    AgentContact("amelia", "087784882233", "amelia.raywhitecipete@gmail.com", "aimeeworks.property"),
    AgentContact("hilda", "0817216161", "hilda.raywhite@gmail.com", "hilda.raywhite"),
    AgentContact("desy", "081284001033", "desy.raywhite@gmail.com", ""),
    AgentContact("briand", "08176676276", "briandrwc@gmail.com", "briandproperty"),
    AgentContact("rika", "081218280096", "rikaraywhite@yahoo.com", "rikaraywhite1"),
    AgentContact("meisi", "0817855005", "meisiraywhite@gmail.com", "meisi.raywhite"),
    AgentContact("yuma", "08118585137", "yumaray.raywhite@gmail.com", ""),
    AgentContact("dhavit", "085169671344", "dhavitvalentino@gmail.com", "@dhavitvalentino"),
    AgentContact("valentino", "085169671344", "dhavitvalentino@gmail.com", "@dhavitvalentino")
)

fun findContact(meName: String): AgentContact? {
    val cleanName = meName.trim().lowercase()
    if (cleanName.isBlank()) return null
    return AGENT_CONTACT_LIST.find { contact ->
        cleanName.contains(contact.nameKey) || contact.nameKey.contains(cleanName)
    }
}

fun getAgentPhoneByName(meName: String): String {
    val contact = findContact(meName)
    return contact?.phone ?: ""
}

fun getAgentEmailByName(meName: String): String {
    val contact = findContact(meName)
    return contact?.email ?: ""
}

fun getAgentInstagramByName(meName: String): String {
    val contact = findContact(meName)
    if (contact != null && contact.instagram.isNotBlank()) {
        val ig = contact.instagram
        return if (ig.startsWith("@")) ig else "@$ig"
    }
    return ""
}

fun cleanListingDescription(rawDesc: String): String {
    val lines = rawDesc.split("\n")
    var numLineIndex = -1
    // Look at the top 8 lines for a line containing only digits
    for (i in 0 until minOf(lines.size, 8)) {
        val trimmedLine = lines[i].trim()
        if (trimmedLine.isNotEmpty() && trimmedLine.matches("""\d+""".toRegex())) {
            numLineIndex = i
            break
        }
    }
    if (numLineIndex != -1 && numLineIndex < lines.size - 1) {
        // Drop the number line and everything above it, and only return lines below it!
        return lines.drop(numLineIndex + 1).joinToString("\n").trim()
    }
    return rawDesc.trim()
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

    // Cache for scraped listing price: idListing -> Price
    private val _listingPriceMap = MutableStateFlow<Map<String, String>>(emptyMap())
    val listingPriceMap: StateFlow<Map<String, String>> = _listingPriceMap.asStateFlow()

    // Cache for scraped listing descriptions: idListing -> Description
    private val _listingDescMap = MutableStateFlow<Map<String, String>>(emptyMap())
    val listingDescMap: StateFlow<Map<String, String>> = _listingDescMap.asStateFlow()

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
                        
                        // Extract Agent Personal Photo from HTML (heuristic tags / paths containing agent, avatar, etc)
                        var agentAvatarUrl = ""
                        for (src in candidateUrls) {
                            val lower = src.lowercase()
                            if (lower.contains("agent") || lower.contains("avatar") || lower.contains("profile") || lower.contains("team") || lower.contains("member") || lower.contains("staff") || lower.contains("/me/")) {
                                val fullUrl = if (src.startsWith("/")) {
                                    "https://raywhitecipete.net" + src
                                } else {
                                    src
                                }
                                agentAvatarUrl = fullUrl
                                break
                            }
                        }

                        // Parse WhatsApp / Agent Info
                        val waRegex = """(?:https?:)?//(?:api\.whatsapp\.com/send|wa\.me)/?[^\s"'>]*|whatsapp://[^\s"'>]*""".toRegex(RegexOption.IGNORE_CASE)
                        val waMatches = waRegex.findAll(html).map { it.value }.toList()
                        
                        var finalAgentName = defaultMeName.trim()
                        var finalAgentPhone = getAgentPhoneByName(defaultMeName)
                        var finalWaUrl = ""
                        
                        var bestAgentName = defaultMeName.trim()
                        var bestAgentPhone = ""
                        var bestWaUrl = ""
                        
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
                            
                            // Check if phone matches the default office admin digits
                            val digitsOnly = phone.replace("+", "").removePrefix("62").removePrefix("0")
                            val isOfficeAdminLine = digitsOnly == "85169671344"
                            
                            // Extract Name
                            var name = ""
                            val hubungiMatch = """Hubungi:[ \n\r]*([A-Za-z ]+)[, \n\r]""".toRegex(RegexOption.IGNORE_CASE).find(decoded)
                            if (hubungiMatch != null) {
                                name = hubungiMatch.groupValues[1].trim()
                            } else {
                                val hubungiNoColon = """Hubungi\s+([A-Za-z ]+)""".toRegex(RegexOption.IGNORE_CASE).find(decoded)
                                if (hubungiNoColon != null) {
                                    name = hubungiNoColon.groupValues[1].trim()
                                }
                            }
                            
                            if (phone.isNotBlank()) {
                                // Prioritize the personal agent number over the floating office support number
                                if (bestAgentPhone.isBlank() || (!isOfficeAdminLine && bestAgentPhone.replace("+", "").removePrefix("62").removePrefix("0") == "85169671344")) {
                                    bestAgentPhone = phone
                                    if (name.isNotBlank()) {
                                        bestAgentName = name
                                    }
                                    bestWaUrl = waUrl
                                    // If we found a personal number (non-office), we can stop
                                    if (!isOfficeAdminLine) {
                                        break
                                    }
                                }
                            }
                        }
                        
                        if (bestAgentPhone.isNotBlank()) {
                            finalAgentPhone = bestAgentPhone
                            finalAgentName = bestAgentName
                            finalWaUrl = bestWaUrl
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
                        
                        // Parse Price & Description
                        var parsedPrice = ""
                        val markupPriceRegex = """<h\d[^>]*>(Rp[^<]+)</h\d>""".toRegex(RegexOption.IGNORE_CASE)
                        val markupMatch = markupPriceRegex.find(html)
                        if (markupMatch != null) {
                            parsedPrice = markupMatch.groupValues[1].trim()
                        }
                        if (parsedPrice.isBlank()) {
                            val priceRegex = """Rp\.?\s*([0-9\.,]+(?:\s*(?:Milyar|M|Juta|J))?)""".toRegex(RegexOption.IGNORE_CASE)
                            val priceMatches = priceRegex.findAll(html)
                            for (m in priceMatches) {
                                val candidate = m.value.trim()
                                if (candidate.length > 5 && candidate.length < 30) {
                                    parsedPrice = candidate
                                    break
                                }
                            }
                        }
                        if (parsedPrice.isBlank()) {
                            parsedPrice = "Hubungi Agent"
                        }

                        var parsedDesc = ""
                        val descDivRegex = """<div[^>]+(?:class|id)=["']([^"']*(?:desc|content|detail)[^"']*)["'][^>]*>([\s\S]*?)</div>""".toRegex(RegexOption.IGNORE_CASE)
                        val descMatches = descDivRegex.findAll(html)
                        var longestText = ""
                        for (match in descMatches) {
                            val classOrId = match.groupValues[1].lowercase()
                            if (classOrId.contains("header") || classOrId.contains("nav") || classOrId.contains("footer") || classOrId.contains("menu")) continue
                            val innerHtml = match.groupValues[2]
                            val cleanTxt = innerHtml.replace("<[^>]*>".toRegex(), " ")
                                                    .replace("&nbsp;".toRegex(), " ")
                                                    .replace("""\s+""".toRegex(), " ")
                                                    .trim()
                            if (cleanTxt.length > longestText.length && cleanTxt.length < 5000) {
                                if (cleanTxt.lowercase().contains("luas") || cleanTxt.lowercase().contains("kamar") || cleanTxt.lowercase().contains("tanah") || cleanTxt.lowercase().contains("mandi") || cleanTxt.lowercase().contains("house")) {
                                    longestText = cleanTxt
                                } else if (longestText.isEmpty() || !longestText.lowercase().contains("luas")) {
                                    longestText = cleanTxt
                                }
                            }
                        }
                        if (longestText.length > 30) {
                            parsedDesc = longestText
                        } else {
                            val pRegex = """<p[^>]*>([\s\S]*?)</p>""".toRegex(RegexOption.IGNORE_CASE)
                            val pMatches = pRegex.findAll(html)
                            val pList = mutableListOf<String>()
                            for (pm in pMatches) {
                                val txt = pm.groupValues[1].replace("<[^>]*>".toRegex(), " ").replace("&nbsp;".toRegex(), " ").trim()
                                if (txt.length > 20 && !txt.contains("Copyright", ignoreCase = true) && !txt.contains("Ray White", ignoreCase = true)) {
                                    pList.add(txt)
                                }
                            }
                            if (pList.isNotEmpty()) {
                                parsedDesc = pList.joinToString("\n\n")
                            }
                        }
                        if (parsedDesc.isBlank()) {
                            parsedDesc = "Deskripsi tidak tersedia di websiteListing. Silakan hubungi agent terkait."
                        }

                        _listingPriceMap.update { current ->
                            current + (cleanId to parsedPrice)
                        }
                        _listingDescMap.update { current ->
                            current + (cleanId to cleanListingDescription(parsedDesc))
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
                            phone = getAgentPhoneByName(finalAgentName),
                            waUrl = finalWaUrl,
                            avatarUrl = agentAvatarUrl,
                            email = getAgentEmailByName(finalAgentName),
                            instagram = getAgentInstagramByName(finalAgentName)
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
        .map { list -> list.filter { it.idListing.trim() != "VERSION_CHECK" } }
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

    fun updateScheduleDirectly(schedule: Schedule, original: Schedule) {
        viewModelScope.launch {
            _syncStatus.value = SyncState.Loading
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
