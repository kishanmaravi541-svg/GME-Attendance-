package com.example.data

import kotlinx.coroutines.flow.Flow

class AttendanceRepository(private val dao: AttendanceDao) {

    fun getRecordsForDate(dateKey: String): Flow<List<AttendanceRecord>> {
        return dao.getRecordsForDate(dateKey)
    }

    fun getCountForDate(dateKey: String): Flow<Int> {
        return dao.getCountForDate(dateKey)
    }

    suspend fun getRecordForStudentToday(dateKey: String, studentName: String): AttendanceRecord? {
        return dao.getRecordForStudentToday(dateKey, studentName.trim().lowercase())
    }

    fun getAllRecordedDates(): Flow<List<String>> {
        return dao.getAllRecordedDates()
    }

    fun getAllRecords(): Flow<List<AttendanceRecord>> {
        return dao.getAllRecords()
    }

    suspend fun markAttendance(record: AttendanceRecord): Long {
        return dao.insertRecord(record)
    }

    suspend fun deleteRecord(id: Long) {
        dao.deleteRecord(id)
    }

    suspend fun clearRecordsForDate(dateKey: String) {
        dao.clearRecordsForDate(dateKey)
    }
}
