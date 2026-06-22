package com.example.network

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Url

@JsonClass(generateAdapter = true)
data class SheetSchedule(
    @Json(name = "idListing") val idListing: String = "",
    @Json(name = "namaMe") val namaMe: String = "",
    @Json(name = "lokasi") val lokasi: String = "",
    @Json(name = "tanggal") val tanggal: String = "",
    @Json(name = "jam") val jam: String = "",
    @Json(name = "staff") val staff: String = "",
    @Json(name = "type") val type: String = "Foto",
    @Json(name = "status") val status: String = "Pending",
    @Json(name = "action") val action: String = "add",
    @Json(name = "originalIdListing") val originalIdListing: String = "",
    @Json(name = "originalNamaMe") val originalNamaMe: String = "",
    @Json(name = "originalTanggal") val originalTanggal: String = "",
    @Json(name = "originalJam") val originalJam: String = ""
)

@JsonClass(generateAdapter = true)
data class GeneralResponse(
    @Json(name = "status") val status: String = "",
    @Json(name = "message") val message: String = ""
)

interface SheetsApiService {
    @GET
    suspend fun getSchedules(@Url url: String): List<SheetSchedule>

    @POST
    suspend fun addSchedule(@Url url: String, @Body schedule: SheetSchedule): GeneralResponse
}
