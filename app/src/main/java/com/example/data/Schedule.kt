package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.io.Serializable

@Entity(tableName = "schedules")
data class Schedule(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val idListing: String,
    val namaMe: String,
    val lokasi: String,
    val tanggal: String,
    val jam: String,
    val staff: String,
    val type: String = "Foto",
    val status: String = "Pending",
    val createdAt: Long = System.currentTimeMillis(),
    val synced: Boolean = false
) : Serializable
