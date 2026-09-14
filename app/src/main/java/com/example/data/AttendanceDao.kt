package com.example.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface AttendanceDao {

    @Query("SELECT * FROM attendance_records WHERE dateKey = :dateKey ORDER BY timestamp DESC")
    fun getRecordsForDate(dateKey: String): Flow<List<AttendanceRecord>>

    @Query("SELECT COUNT(*) FROM attendance_records WHERE dateKey = :dateKey")
    fun getCountForDate(dateKey: String): Flow<Int>

    @Query("SELECT * FROM attendance_records WHERE dateKey = :dateKey AND studentNameLower = :nameLower LIMIT 1")
    suspend fun getRecordForStudentToday(dateKey: String, nameLower: String): AttendanceRecord?

    @Query("SELECT DISTINCT dateKey FROM attendance_records ORDER BY dateKey DESC")
    fun getAllRecordedDates(): Flow<List<String>>

    @Query("SELECT * FROM attendance_records ORDER BY timestamp DESC")
    fun getAllRecords(): Flow<List<AttendanceRecord>>

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertRecord(record: AttendanceRecord): Long

    @Query("DELETE FROM attendance_records WHERE id = :id")
    suspend fun deleteRecord(id: Long)

    @Query("DELETE FROM attendance_records WHERE dateKey = :dateKey")
    suspend fun clearRecordsForDate(dateKey: String)
}
