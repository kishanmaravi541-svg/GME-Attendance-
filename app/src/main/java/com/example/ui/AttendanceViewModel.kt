package com.example.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.AppDatabase
import com.example.data.AttendanceRecord
import com.example.data.AttendanceRepository
import com.example.location.LocationHelper
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

sealed interface AttendanceSubmissionState {
    data object Idle : AttendanceSubmissionState
    data object LocatingAndSaving : AttendanceSubmissionState
    data class Success(val record: AttendanceRecord) : AttendanceSubmissionState
    data class AlreadyMarked(val record: AttendanceRecord) : AttendanceSubmissionState
    data class LocationPermissionRequired(val message: String) : AttendanceSubmissionState
    data class Error(val message: String) : AttendanceSubmissionState
}

class AttendanceViewModel(application: Application) : AndroidViewModel(application) {

    private val repository: AttendanceRepository

    init {
        val database = AppDatabase.getDatabase(application)
        repository = AttendanceRepository(database.attendanceDao())
    }

    // Date formats
    private val dateKeyFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
    private val displayDateFormat = SimpleDateFormat("d MMMM yyyy", Locale.getDefault())
    private val headerDateFormat = SimpleDateFormat("EEEE, d MMMM yyyy", Locale.getDefault())
    private val timeFormat = SimpleDateFormat("h:mm a", Locale.getDefault())

    // Today's values (Students cannot manually alter or change attendance date)
    val todayDateKey: String get() = dateKeyFormat.format(Date())
    val todayFormattedDate: String get() = displayDateFormat.format(Date())
    val todayHeaderDate: String get() = headerDateFormat.format(Date())

    // Student UI state
    private val _studentNameInput = MutableStateFlow("")
    val studentNameInput: StateFlow<String> = _studentNameInput.asStateFlow()

    private val _submissionState = MutableStateFlow<AttendanceSubmissionState>(AttendanceSubmissionState.Idle)
    val submissionState: StateFlow<AttendanceSubmissionState> = _submissionState.asStateFlow()

    // Admin UI state
    private val _selectedDateKey = MutableStateFlow(todayDateKey)
    val selectedDateKey: StateFlow<String> = _selectedDateKey.asStateFlow()

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    @OptIn(ExperimentalCoroutinesApi::class)
    val recordsForSelectedDate: StateFlow<List<AttendanceRecord>> = _selectedDateKey
        .flatMapLatest { key -> repository.getRecordsForDate(key) }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    @OptIn(ExperimentalCoroutinesApi::class)
    val presentCountForSelectedDate: StateFlow<Int> = _selectedDateKey
        .flatMapLatest { key -> repository.getCountForDate(key) }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = 0
        )

    val allRecordedDates: StateFlow<List<String>> = repository.getAllRecordedDates()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = listOf(todayDateKey)
        )

    fun onStudentNameChange(newName: String) {
        _studentNameInput.value = newName
        if (_submissionState.value !is AttendanceSubmissionState.LocatingAndSaving) {
            _submissionState.value = AttendanceSubmissionState.Idle
        }
    }

    fun onSearchQueryChange(query: String) {
        _searchQuery.value = query
    }

    fun setSelectedDate(dateKey: String) {
        _selectedDateKey.value = dateKey
    }

    fun goToPreviousDay() {
        try {
            val cal = Calendar.getInstance()
            val parsed = dateKeyFormat.parse(_selectedDateKey.value) ?: Date()
            cal.time = parsed
            cal.add(Calendar.DAY_OF_YEAR, -1)
            _selectedDateKey.value = dateKeyFormat.format(cal.time)
        } catch (_: Exception) {
            // keep current
        }
    }

    fun goToNextDay() {
        try {
            val cal = Calendar.getInstance()
            val parsed = dateKeyFormat.parse(_selectedDateKey.value) ?: Date()
            cal.time = parsed
            cal.add(Calendar.DAY_OF_YEAR, 1)
            _selectedDateKey.value = dateKeyFormat.format(cal.time)
        } catch (_: Exception) {
            // keep current
        }
    }

    fun goToToday() {
        _selectedDateKey.value = todayDateKey
    }

    fun formatKeyToDisplay(dateKey: String): String {
        return try {
            val parsed = dateKeyFormat.parse(dateKey)
            if (parsed != null) displayDateFormat.format(parsed) else dateKey
        } catch (_: Exception) {
            dateKey
        }
    }

    fun markAttendance(hasLocationPermission: Boolean) {
        val trimmedName = _studentNameInput.value.trim()
        if (trimmedName.isEmpty()) {
            _submissionState.value = AttendanceSubmissionState.Error("Please enter your name to mark attendance.")
            return
        }

        if (!hasLocationPermission) {
            _submissionState.value = AttendanceSubmissionState.LocationPermissionRequired(
                "Please allow location access to mark attendance."
            )
            return
        }

        viewModelScope.launch {
            _submissionState.value = AttendanceSubmissionState.LocatingAndSaving

            val currentKey = todayDateKey
            val formattedDate = todayFormattedDate
            val formattedTime = timeFormat.format(Date())

            // 1. Check if student has already marked attendance today
            val existing = repository.getRecordForStudentToday(currentKey, trimmedName)
            if (existing != null) {
                _submissionState.value = AttendanceSubmissionState.AlreadyMarked(existing)
                return@launch
            }

            // 2. Fetch current GPS location
            val locationResult = LocationHelper.getCurrentLocation(getApplication())
            if (locationResult == null) {
                _submissionState.value = AttendanceSubmissionState.LocationPermissionRequired(
                    "Please allow location access to mark attendance."
                )
                return@launch
            }

            // 3. Save student attendance record
            val record = AttendanceRecord(
                studentName = trimmedName,
                studentNameLower = trimmedName.lowercase(),
                dateKey = currentKey,
                formattedDate = formattedDate,
                formattedTime = formattedTime,
                timestamp = System.currentTimeMillis(),
                status = "Present",
                latitude = locationResult.latitude,
                longitude = locationResult.longitude,
                locationDescription = locationResult.description
            )

            try {
                repository.markAttendance(record)
                _submissionState.value = AttendanceSubmissionState.Success(record)
            } catch (e: Exception) {
                // In case of unique constraint violation race
                val doubleCheck = repository.getRecordForStudentToday(currentKey, trimmedName)
                if (doubleCheck != null) {
                    _submissionState.value = AttendanceSubmissionState.AlreadyMarked(doubleCheck)
                } else {
                    _submissionState.value = AttendanceSubmissionState.Error(
                        e.localizedMessage ?: "Failed to save attendance. Please try again."
                    )
                }
            }
        }
    }

    fun resetSubmission() {
        _studentNameInput.value = ""
        _submissionState.value = AttendanceSubmissionState.Idle
    }

    fun deleteRecord(recordId: Long) {
        viewModelScope.launch {
            repository.deleteRecord(recordId)
        }
    }
}
