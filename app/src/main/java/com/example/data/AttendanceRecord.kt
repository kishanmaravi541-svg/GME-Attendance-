package com.example.data

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "attendance_records",
    indices = [
        Index(value = ["dateKey", "studentNameLower"], unique = true)
    ]
)
data class AttendanceRecord(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val studentName: String,
    val studentNameLower: String = studentName.trim().lowercase(),
    val dateKey: String, // e.g. "2026-09-14"
    val formattedDate: String, // e.g. "14 September 2026"
    val formattedTime: String, // e.g. "9:05 AM"
    val timestamp: Long = System.currentTimeMillis(),
    val status: String = "Present",
    val latitude: Double? = null,
    val longitude: Double? = null,
    val locationDescription: String = "Location captured"
)
