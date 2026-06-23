package com.example.data

import com.example.network.SheetSchedule
import com.example.network.SheetsApiService
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.util.UUID

class ScheduleRepository(
    private val scheduleDao: ScheduleDao,
    private val apiService: SheetsApiService,
    private val preferenceManager: PreferenceManager
) {
    private val syncMutex = Mutex()
    val allSchedules: Flow<List<Schedule>> = scheduleDao.getAllSchedules()

    // Seeds beautiful mock data if database is totally empty, so they see a gorgeous dashboard on first launch
    suspend fun seedMockDataIfEmpty() {
        // Hapus data mock bawaan jika ada dari peluncuran aplikasi sebelumnya
        val mockIds = listOf("L-0912", "L-1102", "L-2241", "L-1133", "L-3412", "L-2591")
        scheduleDao.deleteSchedulesByIds(mockIds)
        preferenceManager.isFirstLaunch = false
    }

    // Sync from Google Sheets API with robust filtering and backward compatibility
    suspend fun syncFromGoogleSheets(): Result<Unit> = syncMutex.withLock {
        val url = preferenceManager.appsScriptUrl
        if (url.isBlank()) {
            return Result.failure(Exception("Apps Script URL belum dikonfigurasi. Silakan atur di Pengaturan."))
        }

        return try {
            val sheetSchedules = apiService.getSchedules(url)

            val mapped = sheetSchedules.map {
                val finalIdListing = it.idListing.trim()
                val finalNamaMe = it.namaMe.trim()
                val finalLokasi = it.lokasi.trim()
                val finalStaff = it.staff.trim()
                val finalTanggal = normalizeDate(it.tanggal)
                val finalJam = it.jam.trim()
                val finalType = it.type.trim()
                val finalStatus = it.status.trim()

                Schedule(
                    idListing = finalIdListing,
                    namaMe = finalNamaMe,
                    lokasi = finalLokasi,
                    tanggal = finalTanggal,
                    jam = finalJam,
                    staff = finalStaff,
                    type = finalType,
                    status = finalStatus,
                    synced = true
                )
            }.toMutableList()
            
            // Ambil semua jadwal yang belum tersinkronisasi agar tidak terhapus
            val unsynced = scheduleDao.getAllSchedules().first().filter { !it.synced }
            
            // Gunakan transaksi agar Flow UI tidak berkedip kosong saat disinkronisasi
            scheduleDao.updateSchedulesTransaction(mapped, unsynced)
            
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private fun parseTimeFromOldString(raw: String): String {
        if (raw.isBlank()) return ""
        try {
            val parts = raw.split(" ")
            for (p in parts) {
                if (p.contains(":")) {
                    val subParts = p.split(":")
                    if (subParts.size >= 2) {
                        return "${subParts[0]}:${subParts[1]}"
                    }
                }
            }
        } catch (e: Exception) {
            // fallback
        }
        return raw.trim()
    }

    // Insert schedule (saves locally, then attempts remote sync if configured)
    suspend fun addSchedule(schedule: Schedule): Result<Unit> = syncMutex.withLock {
        // 1. Insert local first for instant UI response
        val insertedId = scheduleDao.insertSchedule(schedule)
        val insertedSchedule = schedule.copy(id = insertedId.toInt())

        val url = preferenceManager.appsScriptUrl
        if (url.isBlank()) {
            // No sync but saved locally, return warning-as-success
            return Result.success(Unit)
        }

        // 2. Perform remote posting to Sheets API
        return try {
            val sheetModel = SheetSchedule(
                idListing = schedule.idListing,
                namaMe = schedule.namaMe,
                lokasi = schedule.lokasi,
                tanggal = schedule.tanggal,
                jam = schedule.jam,
                staff = schedule.staff,
                type = schedule.type,
                status = schedule.status
            )
            val result = apiService.addSchedule(url, sheetModel)
            if (result.status.lowercase() == "success" || result.status.lowercase() == "ok" || result.message.lowercase() == "success") {
                // Update local status as synced
                scheduleDao.updateSchedule(insertedSchedule.copy(synced = true))
                Result.success(Unit)
            } else {
                Result.failure(Exception(result.message.ifBlank { "Gagal mengirim data ke Google Sheets" }))
            }
        } catch (e: Exception) {
            // Keep local as unsynced so they can re-try later
            Result.failure(e)
        }
    }

    // Update existing schedule locally and remotely on Google Sheets
    suspend fun updateSchedule(schedule: Schedule, originalSchedule: Schedule): Result<Unit> = syncMutex.withLock {
        // 1. Update locally
        scheduleDao.updateSchedule(schedule)

        val url = preferenceManager.appsScriptUrl
        if (url.isBlank()) {
            return Result.success(Unit)
        }

        // 2. Sync edit to Google Sheets
        return try {
            val sheetModel = SheetSchedule(
                idListing = schedule.idListing,
                namaMe = schedule.namaMe,
                lokasi = schedule.lokasi,
                tanggal = schedule.tanggal,
                jam = schedule.jam,
                staff = schedule.staff,
                type = schedule.type,
                status = schedule.status,
                action = "edit",
                originalIdListing = originalSchedule.idListing,
                originalNamaMe = originalSchedule.namaMe,
                originalTanggal = originalSchedule.tanggal,
                originalJam = originalSchedule.jam
            )
            val result = apiService.addSchedule(url, sheetModel)
            if (result.status.lowercase() == "success" || result.status.lowercase() == "ok" || result.message.lowercase() == "success") {
                scheduleDao.updateSchedule(schedule.copy(synced = true))
                Result.success(Unit)
            } else {
                Result.failure(Exception(result.message.ifBlank { "Gagal memperbarui data di Google Sheets" }))
            }
        } catch (e: Exception) {
            // Mark as unsynced if failed
            scheduleDao.updateSchedule(schedule.copy(synced = false))
            Result.failure(e)
        }
    }

    // Resync pending schedules
    suspend fun syncPendingSchedules(): Result<Int> = syncMutex.withLock {
        val url = preferenceManager.appsScriptUrl
        if (url.isBlank()) {
            return Result.failure(Exception("Apps Script URL belum diatur"))
        }

         val all = scheduleDao.getAllSchedules().first()
        val pending = all.filter { !it.synced }
        if (pending.isEmpty()) return Result.success(0)

        var successCount = 0
        for (item in pending) {
            try {
                val sheetModel = SheetSchedule(
                    idListing = item.idListing,
                    namaMe = item.namaMe,
                    lokasi = item.lokasi,
                    tanggal = item.tanggal,
                    jam = item.jam,
                    staff = item.staff,
                    type = item.type,
                    status = item.status
                )
                val result = apiService.addSchedule(url, sheetModel)
                val statusLow = result.status.lowercase()
                val msgLow = result.message.lowercase()
                if (statusLow == "success" || statusLow == "ok" || msgLow == "success" || msgLow == "ok") {
                    scheduleDao.updateSchedule(item.copy(synced = true))
                    successCount++
                }
            } catch (e: Exception) {
                // Ignore and continue
            }
        }
        return Result.success(successCount)
    }

    // Clear all tables locally
    suspend fun clearLocal() {
        preferenceManager.isFirstLaunch = false
        scheduleDao.clearSchedules()
    }

    // Delete a specific schedule (locally and remotely if synced)
    suspend fun deleteSchedule(schedule: Schedule): Result<Unit> = syncMutex.withLock {
        // 1. Delete locally first
        scheduleDao.deleteSchedule(schedule)

        val url = preferenceManager.appsScriptUrl
        if (url.isBlank()) {
            return Result.success(Unit)
        }

        // 2. Clear from Google Sheets remotely
        return try {
            val sheetModel = SheetSchedule(
                idListing = schedule.idListing,
                namaMe = schedule.namaMe,
                lokasi = schedule.lokasi,
                tanggal = schedule.tanggal,
                jam = schedule.jam,
                staff = schedule.staff,
                type = schedule.type,
                status = schedule.status,
                action = "delete"
            )
            val result = apiService.addSchedule(url, sheetModel)
            val statusLow = result.status.lowercase()
            val msgLow = result.message.lowercase()
            if (statusLow == "success" || statusLow == "ok" || msgLow == "success" || msgLow == "ok") {
                Result.success(Unit)
            } else {
                Result.failure(Exception(result.message.ifBlank { "Gagal menghapus data di Google Sheets secara online" }))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}

fun normalizeDate(dateStr: String): String {
    val trimmed = dateStr.trim()
    if (trimmed.isEmpty()) return ""
    
    // Check if format is "yyyy-MM-dd"
    if (trimmed.matches(Regex("\\d{4}-\\d{2}-\\d{2}"))) {
        return trimmed
    }
    
    // Check if format is "yyyy-M-d" and pad it
    if (trimmed.matches(Regex("\\d{4}-\\d{1,2}-\\d{1,2}"))) {
        try {
            val parts = trimmed.split("-")
            val y = parts[0]
            val m = String.format(java.util.Locale.US, "%02d", parts[1].toInt())
            val d = String.format(java.util.Locale.US, "%02d", parts[2].toInt())
            return "$y-$m-$d"
        } catch (e: Exception) { }
    }
    
    // Check if dd/MM/yyyy or dd-MM-yyyy
    val localMatch = Regex("^(\\d{1,2})[/-](\\d{1,2})[/-](\\d{2,4})").find(trimmed)
    if (localMatch != null) {
        try {
            val day = localMatch.groupValues[1].toInt()
            val month = localMatch.groupValues[2].toInt()
            var year = localMatch.groupValues[3].toInt()
            if (year < 100) {
                year += if (year < 50) 2000 else 1900
            }
            return String.format(java.util.Locale.US, "%04d-%02d-%02d", year, month, day)
        } catch (e: Exception) { }
    }

    // Capitalize words to support case-insensitive months (e.g. "22 juni 2026" -> "22 Juni 2026")
    val capitalizedStr = trimmed.split(" ").joinToString(" ") { word ->
        word.lowercase().replaceFirstChar { if (it.isLowerCase()) it.titlecase(java.util.Locale.getDefault()) else it.toString() }
    }
    
    // Parse common standard date/time formats
    try {
        val formats = listOf(
            java.text.SimpleDateFormat("EEE MMM dd HH:mm:ss zzz yyyy", java.util.Locale.US),
            java.text.SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", java.util.Locale.US),
            java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss", java.util.Locale.US),
            java.text.SimpleDateFormat("dd MMMM yyyy", java.util.Locale("id", "ID")),
            java.text.SimpleDateFormat("d MMMM yyyy", java.util.Locale("id", "ID")),
            java.text.SimpleDateFormat("dd-MMM-yyyy", java.util.Locale.US),
            java.text.SimpleDateFormat("yyyy/MM/dd", java.util.Locale.US)
        )
        for (f in formats) {
            try {
                val parsed = f.parse(capitalizedStr)
                if (parsed != null) {
                    return java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.US).format(parsed)
                }
            } catch (e: Exception) { }
            try {
                val parsed = f.parse(trimmed)
                if (parsed != null) {
                    return java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.US).format(parsed)
                }
            } catch (e: Exception) { }
        }
    } catch (e: Exception) { }

    return trimmed
}
