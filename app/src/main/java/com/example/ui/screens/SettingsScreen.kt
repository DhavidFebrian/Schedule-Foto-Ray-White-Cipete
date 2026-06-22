package com.example.ui.screens

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.graphics.Color
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.ui.ScheduleViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    viewModel: ScheduleViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val scrollState = rememberScrollState()

    val currentUrl by viewModel.appsScriptUrl.collectAsStateWithLifecycle()
    val currentSheetId by viewModel.spreadsheetId.collectAsStateWithLifecycle()

    var urlInput by remember { mutableStateOf(currentUrl) }
    var sheetIdInput by remember { mutableStateOf(currentSheetId) }

    LaunchedEffect(currentUrl, currentSheetId) {
        urlInput = currentUrl
        sheetIdInput = currentSheetId
    }

    // Apps Script Code Template
    val appsScriptCode = """
// SCRIPT DETEKSI & KONEKSI GOOGLE SHEETS UNTUK APLIKASI JADWAL FOTO (DENGAN RE-SYNC & DELETE ONLINE)
// Tempelkan kode ini di Google Apps Script Anda (Ekstensi -> Apps Script)

function doGet(e) {
  try {
    var sheet = SpreadsheetApp.getActiveSpreadsheet().getSheets()[0];
    // Batasi jangkauan baris 1 s/d 304, kolom 1 s/d 10 (A s/d J)
    var rows = sheet.getRange(1, 1, 304, 10).getValues();
    var data = [];
    
    // Mulai membaca dari baris 5 (index Array = 4) hingga baris 304 (index Array = 303)
    for (var i = 4; i < 304; i++) {
      var row = rows[i];
      if (!row) continue;
      
      var realIdListing = row[1] != null ? row[1].toString().trim() : "";
      var realNamaMe = row[2] != null ? row[2].toString().trim() : "";
      var realStaff = row[3] != null ? row[3].toString().trim() : "";
      var realTanggal = formatDateHelper(row[4]);
      var realLokasi = row[5] != null ? row[5].toString().trim() : "";
      var realJam = formatTimeHelper(row[6]);
      var rawType = row[7] != null ? row[7].toString().trim() : "";
      var rawStatus = row[8] != null ? row[8].toString().trim() : "";
      var realSource = row[9] != null ? row[9].toString().trim() : "";
      
      // Lewati baris kosong / belum terisi
      if (realIdListing === "" && realNamaMe === "" && realLokasi === "") {
        continue;
      }
      
      var obj = {
        idListing: realIdListing,
        namaMe: realNamaMe,
        lokasi: realLokasi,
        staff: realStaff,
        tanggal: realTanggal,
        jam: realJam,
        type: rawType === "" ? "Foto" : rawType,
        status: rawStatus === "" ? "Pending" : rawStatus,
        source: realSource === "" ? "Spreadsheet" : realSource
      };
      
      data.push(obj);
    }
    
    return ContentService.createTextOutput(JSON.stringify(data))
      .setMimeType(ContentService.MimeType.JSON);
  } catch (err) {
    return ContentService.createTextOutput(JSON.stringify({ status: "error", message: err.toString() }))
      .setMimeType(ContentService.MimeType.JSON);
  }
}

function formatDateHelper(val) {
  if (val == null) return "";
  if (val instanceof Date) {
    var year = val.getFullYear();
    var month = ("0" + (val.getMonth() + 1)).slice(-2);
    var date = ("0" + val.getDate()).slice(-2);
    if (year === 1899) return ""; 
    return year + "-" + month + "-" + date;
  }
  var str = val.toString().trim();
  if (str === "") return "";
  if (str.match(/^\d{4}-\d{2}-\d{2}/)) {
    return str.substring(0, 10);
  }
  
  // Deteksi dan konversi format lokal seperti dd/MM/yyyy atau dd-MM-yyyy
  var match = str.match(/^(\d{1,2})[\/\-](\d{1,2})[\/\-](\d{2,4})/);
  if (match) {
    var d = parseInt(match[1], 10);
    var m = parseInt(match[2], 10);
    var y = parseInt(match[3], 10);
    if (y < 100) {
      y += (y < 50 ? 2000 : 1900);
    }
    var mm = ("0" + m).slice(-2);
    var dd = ("0" + d).slice(-2);
    return y + "-" + mm + "-" + dd;
  }
  return str;
}

function formatTimeHelper(val) {
  if (val == null) return "";
  if (val instanceof Date) {
    var hours = ("0" + val.getHours()).slice(-2);
    var minutes = ("0" + val.getMinutes()).slice(-2);
    return hours + ":" + minutes;
  }
  var str = val.toString().trim();
  var match = str.match(/(\d{2}):(\d{2})/);
  if (match) {
    return match[1] + ":" + match[2];
  }
  return str;
}

function doPost(e) {
  try {
    var sheet = SpreadsheetApp.getActiveSpreadsheet().getSheets()[0];
    
    var params = {};
    if (e && e.postData && e.postData.contents) {
      params = JSON.parse(e.postData.contents);
    } else if (e && e.parameter) {
      params = e.parameter;
    }
    
    var action = params.action || "add";
    var lookupRows = sheet.getRange(1, 1, 304, 10).getValues();
    
    if (action === "delete") {
      var idListingTarget = params.idListing || "";
      var namaMeTarget = params.namaMe || "";
      var tanggalTarget = formatDateHelper(params.tanggal) || "";
      var jamTarget = formatTimeHelper(params.jam) || "";
      
      var foundRowIndex = -1;
      for (var i = 4; i < 304; i++) {
        var row = lookupRows[i];
        if (!row) continue;
        var rowIdListing = row[1] != null ? row[1].toString().trim() : "";
        var rowNamaMe = row[2] != null ? row[2].toString().trim() : "";
        var rowTanggal = formatDateHelper(row[4]);
        var rowJam = formatTimeHelper(row[6]);
        
        var matches = false;
        if (idListingTarget !== "" && rowIdListing === idListingTarget) {
          matches = true;
        } else if (idListingTarget === "" && rowNamaMe === namaMeTarget && rowTanggal === tanggalTarget && rowJam === jamTarget) {
          matches = true;
        }
        
        if (matches) {
          foundRowIndex = i;
          break;
        }
      }
      
      if (foundRowIndex !== -1) {
        var rowToClear = foundRowIndex + 1;
        // Kosongkan kolom B s/d J agar baris tsb teridentifikasi sebagai kosong
        sheet.getRange(rowToClear, 2).setValue(""); // ID Listing (B)
        sheet.getRange(rowToClear, 3).setValue(""); // Nama ME (C)
        sheet.getRange(rowToClear, 4).setValue(""); // Staff (D)
        sheet.getRange(rowToClear, 5).setValue(""); // Fix Date (E)
        sheet.getRange(rowToClear, 6).setValue(""); // Lokasi (F)
        sheet.getRange(rowToClear, 7).setValue(""); // Jam (G)
        sheet.getRange(rowToClear, 8).setValue(""); // Type (H)
        sheet.getRange(rowToClear, 9).setValue(""); // Status (I)
        sheet.getRange(rowToClear, 10).setValue(""); // Source (J)
        
        return ContentService.createTextOutput(JSON.stringify({
          status: "success",
          message: "Data berhasil dihapus dari baris " + rowToClear
        })).setMimeType(ContentService.MimeType.JSON);
      } else {
        return ContentService.createTextOutput(JSON.stringify({
          status: "error",
          message: "Gagal menemukan baris data yang cocok untuk dihapus."
        })).setMimeType(ContentService.MimeType.JSON);
      }
    }
    
    if (action === "edit" || action === "update") {
      var idListingTarget = params.originalIdListing || params.idListing || "";
      var namaMeTarget = params.originalNamaMe || params.namaMe || "";
      var tanggalTarget = formatDateHelper(params.originalTanggal || params.tanggal) || "";
      var jamTarget = formatTimeHelper(params.originalJam || params.jam) || "";
      
      var foundRowIndex = -1;
      for (var i = 4; i < 304; i++) {
        var row = lookupRows[i];
        if (!row) continue;
        var rowIdListing = row[1] != null ? row[1].toString().trim() : "";
        var rowNamaMe = row[2] != null ? row[2].toString().trim() : "";
        var rowTanggal = formatDateHelper(row[4]);
        var rowJam = formatTimeHelper(row[6]);
        
        var matches = false;
        if (idListingTarget !== "" && rowIdListing === idListingTarget) {
          matches = true;
        } else if (idListingTarget === "" && rowNamaMe === namaMeTarget && rowTanggal === tanggalTarget && rowJam === jamTarget) {
          matches = true;
        }
        
        if (matches) {
          foundRowIndex = i;
          break;
        }
      }
      
      if (foundRowIndex !== -1) {
        var targetRow = foundRowIndex + 1;
        
        var idListing = params.idListing || "";
        var namaMe = params.namaMe || "";
        var lokasi = params.lokasi || "";
        var tanggal = formatDateHelper(params.tanggal) || "";
        var jam = formatTimeHelper(params.jam) || "";
        var staff = params.staff || "";
        var type = params.type || "Foto";
        var status = params.status || "Pending";
        var source = params.source || "App";
        
        sheet.getRange(targetRow, 2).setValue(idListing);
        sheet.getRange(targetRow, 3).setValue(namaMe);
        sheet.getRange(targetRow, 4).setValue(staff);
        sheet.getRange(targetRow, 5).setValue(tanggal);
        sheet.getRange(targetRow, 6).setValue(lokasi);
        sheet.getRange(targetRow, 7).setValue(jam);
        sheet.getRange(targetRow, 8).setValue(type);
        sheet.getRange(targetRow, 9).setValue(status);
        sheet.getRange(targetRow, 10).setValue(source);
        
        return ContentService.createTextOutput(JSON.stringify({ 
          status: "success", 
          message: "Data berhasil diperbarui di baris " + targetRow,
          row: targetRow
        })).setMimeType(ContentService.MimeType.JSON);
      } else {
        action = "add";
      }
    }
    
    // ACTION ADD
    var idListing = params.idListing || "";
    var namaMe = params.namaMe || "";
    var lokasi = params.lokasi || "";
    var tanggal = formatDateHelper(params.tanggal) || "";
    var jam = formatTimeHelper(params.jam) || "";
    var staff = params.staff || "";
    var type = params.type || "Foto";
    var status = params.status || "Pending";
    var source = params.source || "App";
    
    var targetRow = -1;
    // Cari baris kosong dari baris 5 (index Array = 4) sampai baris 304 (index Array = 303)
    for (var i = 4; i < 304; i++) {
      var row = lookupRows[i];
      if (!row) continue;
      var checkListing = row[1] != null ? row[1].toString().trim() : "";
      var checkNamaMe = row[2] != null ? row[2].toString().trim() : "";
      var checkLokasi = row[5] != null ? row[5].toString().trim() : "";
      
      if (checkListing === "" && checkNamaMe === "" && checkLokasi === "") {
        targetRow = i + 1;
        break;
      }
    }
    
    if (targetRow === -1) {
      // Jika semua kapasitas penuh, letakkan di bawah baris 304 (fallback)
      targetRow = sheet.getLastRow() + 1;
    }
    
    // Simpan No di Kolom A (Nomor otomatis menyesuaikan)
    sheet.getRange(targetRow, 1).setValue(targetRow - 4);
    sheet.getRange(targetRow, 2).setValue(idListing);
    sheet.getRange(targetRow, 3).setValue(namaMe);
    sheet.getRange(targetRow, 4).setValue(staff);
    sheet.getRange(targetRow, 5).setValue(tanggal);
    sheet.getRange(targetRow, 6).setValue(lokasi);
    sheet.getRange(targetRow, 7).setValue(jam);
    sheet.getRange(targetRow, 8).setValue(type);
    sheet.getRange(targetRow, 9).setValue(status);
    sheet.getRange(targetRow, 10).setValue(source);
    
    return ContentService.createTextOutput(JSON.stringify({ 
      status: "success", 
      message: "Data berhasil disimpan di baris " + targetRow,
      row: targetRow,
      data: {
        idListing: idListing,
        namaMe: namaMe,
        lokasi: lokasi,
        tanggal: tanggal,
        jam: jam,
        staff: staff,
        type: type,
        status: status,
        source: source
      }
    })).setMimeType(ContentService.MimeType.JSON);
  } catch (error) {
    return ContentService.createTextOutput(JSON.stringify({ status: "error", message: error.toString() }))
      .setMimeType(ContentService.MimeType.JSON);
  }
}
""".trimIndent()

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
                            text = "Pengaturan",
                            style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.onSurface
                        )
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
            var isAccessGranted by rememberSaveable { mutableStateOf(false) }

            if (!isAccessGranted) {
                // Developer exclusive privacy lock screen
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(24.dp)
                        .verticalScroll(rememberScrollState()),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 16.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
                        ),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                        shape = RoundedCornerShape(24.dp)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(24.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(72.dp)
                                    .background(
                                        MaterialTheme.colorScheme.primary.copy(alpha = 0.15f),
                                        androidx.compose.foundation.shape.CircleShape
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Lock,
                                    contentDescription = "Lock Security",
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(36.dp)
                                )
                            }

                            Text(
                                text = "Akses Terbatas",
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary,
                                textAlign = TextAlign.Center
                            )

                            Text(
                                text = "Hanya boleh diakses oleh developer:\nDhavid Febrian Valentino",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface,
                                textAlign = TextAlign.Center,
                                modifier = Modifier.padding(horizontal = 8.dp)
                            )

                            var pinInput by remember { mutableStateOf("") }
                            var hasError by remember { mutableStateOf(false) }

                            OutlinedTextField(
                                value = pinInput,
                                onValueChange = {
                                    pinInput = it
                                    hasError = false
                                },
                                label = { Text("Masukkan PIN / Kode Akses") },
                                singleLine = true,
                                isError = hasError,
                                visualTransformation = PasswordVisualTransformation(),
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier.fillMaxWidth()
                            )

                            if (hasError) {
                                Text(
                                    text = "Kode akses salah! Silakan masukkan PIN yang benar.",
                                    color = MaterialTheme.colorScheme.error,
                                    style = MaterialTheme.typography.bodySmall,
                                    fontWeight = FontWeight.Bold
                                )
                            }

                            Button(
                                onClick = {
                                    val cleaned = pinInput.trim().lowercase()
                                    if (cleaned == "137946" || cleaned == "dhavid" || cleaned == "085169671344" || cleaned == "sfrd" || cleaned == "valentino") {
                                        isAccessGranted = true
                                    } else {
                                        hasError = true
                                    }
                                },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(48.dp),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Icon(Icons.Default.VpnKey, contentDescription = null)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Buka Akses Pengaturan", fontWeight = FontWeight.Bold)
                            }

                            Divider(modifier = Modifier.padding(vertical = 4.dp))

                            Text(
                                text = "If there are any problems, please contact the developer:",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.outline,
                                textAlign = TextAlign.Center
                            )

                            Button(
                                onClick = {
                                    try {
                                        val url = "https://wa.me/6285169671344"
                                        val intent = android.content.Intent(android.content.Intent.ACTION_VIEW).apply {
                                            data = android.net.Uri.parse(url)
                                        }
                                        context.startActivity(intent)
                                    } catch (e: Exception) {
                                        Toast.makeText(context, "Tidak dapat membuka WhatsApp", Toast.LENGTH_SHORT).show()
                                    }
                                },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = Color(0xFF25D366),
                                    contentColor = Color.White
                                ),
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Chat,
                                    contentDescription = "Contact Developer",
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Hubungi via WhatsApp", fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            } else {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(scrollState)
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                // Section 1: Connection Status info
                if (currentUrl.isNotBlank()) {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.CloudDone,
                                contentDescription = "Selesai dihubungkan",
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(36.dp)
                            )
                            Spacer(modifier = Modifier.width(16.dp))
                            Column {
                                Text(
                                    text = "Terhubung dengan Lancar",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer
                                )
                                Text(
                                    text = "Aplikasi akan selalu mengunggah otomatis jadwal foto baru Anda ke Google Sheets secara realtime.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
                                )
                            }
                        }
                    }
                } else {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.CloudQueue,
                                contentDescription = "Belum dikonfigurasi",
                                tint = MaterialTheme.colorScheme.outline,
                                modifier = Modifier.size(36.dp)
                            )
                            Spacer(modifier = Modifier.width(16.dp))
                            Column {
                                Text(
                                    text = "Mode Kerja Offline / Lokal",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Text(
                                    text = "Data disimpan di memori HP Anda. Silakan masukkan URL Google Apps Script di bawah untuk berkolaborasi online.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f)
                                )
                            }
                        }
                    }
                }

                // Section 2: Input URL and Spreadsheet ID
                Text(
                    text = "Konfigurasi API Link",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )

                OutlinedTextField(
                    value = urlInput,
                    onValueChange = { urlInput = it },
                    label = { Text("Google Apps Script Web App URL") },
                    placeholder = { Text("https://script.google.com/macros/s/.../exec") },
                    leadingIcon = { Icon(Icons.Default.Link, contentDescription = null) },
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("input_apps_script_url")
                )

                OutlinedTextField(
                    value = sheetIdInput,
                    onValueChange = { sheetIdInput = it },
                    label = { Text("Google Spreadsheet ID (Referensi)") },
                    placeholder = { Text("1L0ajG9dAmhisDQ7ADil5KKNRkzgXp_jdCi41-6mPkIw") },
                    leadingIcon = { Icon(Icons.Default.Description, contentDescription = null) },
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("input_spreadsheet_id")
                )

                // Save configuration button
                Button(
                    onClick = {
                        viewModel.saveSettings(urlInput, sheetIdInput)
                        Toast.makeText(context, "Konfigurasi koneksi tersimpan!", Toast.LENGTH_SHORT).show()
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp)
                        .testTag("save_settings_button"),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(Icons.Default.SettingsBackupRestore, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Terapkan & Simpan URL", fontWeight = FontWeight.Bold)
                }

                Divider(modifier = Modifier.padding(vertical = 8.dp))

                // Section 3: Setup guide integration
                Text(
                    text = "Panduan Pemasangan 1 Menit",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )

                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Text(
                            text = "Ikuti langkah berikut untuk menyambungkan Google Sheet Anda:",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Medium
                        )

                        Text(
                            text = "1. Buka spreadsheet Anda di laptop/komputer.\n" +
                                   "2. Di menu atas pilih Ekstensi (Extensions) -> Apps Script.\n" +
                                   "3. Hapus semua kode default, salin skrip di bawah dan tempelkan di file Code.gs.\n" +
                                   "4. Klik Simpan (Disket) lalu klik Terapkan (Deploy) -> Penerapan Baru (New Deployment).\n" +
                                   "5. Jenis penerapan: Aplikasi Web (Web App).\n" +
                                   "6. Jalankan sebagai: Saya (Me).\n" +
                                   "7. Yang memiliki akses: Siapa saja (Anyone / Everyone).\n" +
                                   "8. Klik Terapkan, berikan izin akses Google, salin URL Aplikasi Web Anda lalu tempelkan di kolom pengaturan di atas!",
                            style = MaterialTheme.typography.bodyMedium,
                            lineHeight = 22.sp
                        )
                    }
                }

                // Section 4: Deploys the code element + copy buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Kode Google Apps Script",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold
                    )

                    Button(
                        onClick = {
                            val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                            val clip = ClipData.newPlainText("Google Apps Script", appsScriptCode)
                            clipboard.setPrimaryClip(clip)
                            Toast.makeText(context, "Skrip berhasil disalin ke papan klip!", Toast.LENGTH_SHORT).show()
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.testTag("copy_script_button")
                    ) {
                        Icon(Icons.Default.ContentCopy, contentDescription = "Copy Icon", modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Salin Skrip", fontSize = 12.sp)
                    }
                }

                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(8.dp)),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Row(modifier = Modifier.padding(12.dp)) {
                        Text(
                            text = appsScriptCode,
                            style = MaterialTheme.typography.bodySmall.copy(
                                fontFamily = FontFamily.Monospace,
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            ),
                            modifier = Modifier
                                .weight(1f)
                                .height(250.dp)
                                .verticalScroll(rememberScrollState())
                        )
                    }
                }

                Divider(modifier = Modifier.padding(vertical = 8.dp))

                // Section 5: Cache cleanup utilities
                Text(
                    text = "Utilitas Penyimpanan",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.error
                )

                OutlinedButton(
                    onClick = {
                        viewModel.clearCache()
                        Toast.makeText(context, "Cache lokal berhasil dibersihkan!", Toast.LENGTH_SHORT).show()
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp)
                        .testTag("clear_cache_button"),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.error),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(Icons.Default.DeleteForever, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Bersihkan Cache Lokal", fontWeight = FontWeight.Bold)
                }

                Spacer(modifier = Modifier.height(48.dp))
            }
          }
        }
    }
}
