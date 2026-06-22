package com.example.data

import android.content.Context
import android.content.SharedPreferences

class PreferenceManager(context: Context) {
    private val prefs: SharedPreferences = context.getSharedPreferences("jadwal_foto_prefs", Context.MODE_PRIVATE)

    companion object {
        private const val KEY_APPS_SCRIPT_URL = "apps_script_url"
        private const val KEY_SPREADSHEET_ID = "spreadsheet_id"
        
        // DEFAULT_URL: Paste your Google Apps Script URL here so new devices connect auto-magically!
        const val DEFAULT_APPS_SCRIPT_URL = "https://script.google.com/macros/s/AKfycbzHgc3R_K2UF_mB9ATwSNN9D-SlRZ5NAng4PHB4-bNlpw-vHNzi-DgZdN0p1dL6P_PO/exec"
    }

    var appsScriptUrl: String
        get() {
            val url = prefs.getString(KEY_APPS_SCRIPT_URL, DEFAULT_APPS_SCRIPT_URL)
            val oldUrl1 = "https://script.google.com/macros/s/AKfycbwrCQKzNu047yMnQQEhkty1rvCsujrAHtyZGrczgB5-awZAh0R03XnMBIxqA1sHXwgL/exec"
            val oldUrl2 = "https://script.google.com/macros/s/AKfycbwimHayI2ub4x6xabamVvCribr97G3CUIJgEAlFF0MVqOVSHrsni6Zvs6A5MUY2UQMh/exec"
            val oldUrl3 = "https://script.google.com/macros/s/AKfycbziazZC-ynPJ5jDNGPGzdkEKd2Cjh3fmmsaskuLf6ZexclD8bGUJnfDcTge5F0n9GSc/exec"
            val oldUrl4 = "https://script.google.com/macros/s/AKfycbwi54YGMF62kRa5S3TqDe_4SlNSRxrxNXCPFt62kIz5-ZxuJ_W5Pw7c6L6_9kIg0Ca2/exec"
            return if (url.isNullOrBlank() || url == oldUrl1 || url == oldUrl2 || url == oldUrl3 || url == oldUrl4) DEFAULT_APPS_SCRIPT_URL else url
        }
        set(value) = prefs.edit().putString(KEY_APPS_SCRIPT_URL, value.trim()).apply()

    var spreadsheetId: String
        get() = prefs.getString(KEY_SPREADSHEET_ID, "1L0ajG9dAmhisDQ7ADil5KKNRkzgXp_jdCi41-6mPkIw") ?: "1L0ajG9dAmhisDQ7ADil5KKNRkzgXp_jdCi41-6mPkIw"
        set(value) = prefs.edit().putString(KEY_SPREADSHEET_ID, value.trim()).apply()

    var isFirstLaunch: Boolean
        get() = prefs.getBoolean("is_first_launch", true)
        set(value) = prefs.edit().putBoolean("is_first_launch", value).apply()
}
