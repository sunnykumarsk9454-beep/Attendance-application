package com.example.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.*
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

enum class Screen {
    Dashboard,
    Classes,
    Students,
    Attendance,
    Reports,
    Payments,
    Settings,
    Receipt
}

class SchoolViewModel(application: Application) : AndroidViewModel(application) {
    private val database = AppDatabase.getDatabase(application)
    val repository = SchoolRepository(database.schoolDao())

    // Current Screen Navigation state
    val currentScreen = MutableStateFlow(Screen.Dashboard)

    // Lists from DB
    val classes = repository.allClasses.stateIn(viewModelScope, SharingStarted.Lazily, emptyList())
    val students = repository.allStudents.stateIn(viewModelScope, SharingStarted.Lazily, emptyList())
    val allPayments = repository.allPayments.stateIn(viewModelScope, SharingStarted.Lazily, emptyList())
    val allAttendance = repository.allAttendance.stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    // Settings
    val schoolName = repository.getSettingFlow("school_name", "Greenwood International School")
        .stateIn(viewModelScope, SharingStarted.Eagerly, "Greenwood International School")
    
    val academicYear = repository.getSettingFlow("academic_year", "2026 - 2027")
        .stateIn(viewModelScope, SharingStarted.Eagerly, "2026 - 2027")

    // Active Selection State
    val selectedClassForAttendance = MutableStateFlow<SchoolClass?>(null)
    val attendanceDate = MutableStateFlow(getCurrentDateString())
    
    val selectedClassForReports = MutableStateFlow<SchoolClass?>(null)
    val reportDate = MutableStateFlow(getCurrentDateString())

    val selectedStudentForPayment = MutableStateFlow<Student?>(null)
    
    // Receipt Preview State
    val activeReceipt = MutableStateFlow<PaymentRecord?>(null)
    
    // Search & Filters
    val studentSearchText = MutableStateFlow("")
    val paymentSearchText = MutableStateFlow("")

    init {
        // Automatically preheat setting variables
        viewModelScope.launch {
            if (repository.getSettingValue("school_name", "").isEmpty()) {
                repository.insertSetting("school_name", "Greenwood International School")
            }
            if (repository.getSettingValue("academic_year", "").isEmpty()) {
                repository.insertSetting("academic_year", "2026 - 2027")
            }
            // Check if db is completely empty; if so, populate automatically!
            val currentClasses = repository.allClasses.first()
            if (currentClasses.isEmpty()) {
                populateSampleData()
            }
        }
    }

    // Navigation helper
    fun navigateTo(screen: Screen) {
        currentScreen.value = screen
    }

    // Classes Management
    fun addClass(name: String, subject: String) {
        viewModelScope.launch {
            if (name.isNotBlank() && subject.isNotBlank()) {
                repository.insertClass(SchoolClass(name = name, subject = subject))
            }
        }
    }

    fun deleteClass(schoolClass: SchoolClass) {
        viewModelScope.launch {
            repository.deleteClass(schoolClass)
            if (selectedClassForAttendance.value?.id == schoolClass.id) {
                selectedClassForAttendance.value = null
            }
            if (selectedClassForReports.value?.id == schoolClass.id) {
                selectedClassForReports.value = null
            }
        }
    }

    // Students Management
    fun addStudent(classId: Int, name: String, rollNumber: String, monthlyFee: Double) {
        viewModelScope.launch {
            if (name.isNotBlank() && rollNumber.isNotBlank()) {
                repository.insertStudent(
                    Student(
                        classId = classId,
                        name = name,
                        rollNumber = rollNumber,
                        monthlyFee = monthlyFee
                    )
                )
            }
        }
    }

    fun deleteStudent(studentId: Int) {
        viewModelScope.launch {
            repository.deleteStudent(studentId)
            if (selectedStudentForPayment.value?.id == studentId) {
                selectedStudentForPayment.value = null
            }
        }
    }

    // Attendance Management
    fun recordAttendance(studentId: Int, classId: Int, date: String, status: String) {
        viewModelScope.launch {
            val record = AttendanceRecord(
                studentId = studentId,
                classId = classId,
                date = date,
                status = status
            )
            repository.insertAttendanceRecord(record)
        }
    }

    fun recordBulkAttendance(records: List<AttendanceRecord>) {
        viewModelScope.launch {
            repository.insertAttendanceRecords(records)
        }
    }

    // Payments Management
    fun recordPayment(studentId: Int, amount: Double, month: String, year: String, note: String) {
        viewModelScope.launch {
            val rcNo = repository.getNextReceiptNumber()
            val payment = PaymentRecord(
                studentId = studentId,
                amount = amount,
                month = month,
                year = year,
                note = note,
                receiptNumber = rcNo
            )
            repository.insertPayment(payment)
            // Immediately transition to Receipt Screen to display the printed bill!
            activeReceipt.value = payment
            navigateTo(Screen.Receipt)
        }
    }

    fun deletePayment(paymentId: Int) {
        viewModelScope.launch {
            repository.deletePayment(paymentId)
        }
    }

    // Settings updates
    fun updateSettings(name: String, year: String) {
        viewModelScope.launch {
            repository.insertSetting("school_name", name.trim())
            repository.insertSetting("academic_year", year.trim())
        }
    }

    // Wipe all and restore defaults
    fun deleteEverything() {
        viewModelScope.launch {
            repository.wipeData()
            // Reset local states
            selectedClassForAttendance.value = null
            selectedClassForReports.value = null
            selectedStudentForPayment.value = null
            activeReceipt.value = null
            // Navigate to Dashboard
            navigateTo(Screen.Dashboard)
        }
    }

    fun populateSampleData() {
        viewModelScope.launch {
            repository.wipeData()
            
            // Insert Classes
            val c1 = repository.insertClass(SchoolClass(name = "Grade 10 - Science", subject = "Chemistry"))
            val c2 = repository.insertClass(SchoolClass(name = "Grade 11 - Commerce", subject = "Economics"))
            val c3 = repository.insertClass(SchoolClass(name = "Grade 12 - Humanities", subject = "English Literature"))

            repository.insertSetting("school_name", "Greenwood International School")
            repository.insertSetting("academic_year", "2026 - 2027")

            // Insert Students for Class 1 (Grade 10)
            val s1 = repository.insertStudent(Student(classId = c1.toInt(), name = "John Doe", rollNumber = "101", monthlyFee = 150.0))
            val s2 = repository.insertStudent(Student(classId = c1.toInt(), name = "Alice Smith", rollNumber = "102", monthlyFee = 150.0))
            val s3 = repository.insertStudent(Student(classId = c1.toInt(), name = "Bob Johnson", rollNumber = "103", monthlyFee = 150.0))
            val s4 = repository.insertStudent(Student(classId = c1.toInt(), name = "Clara Oswald", rollNumber = "104", monthlyFee = 150.0))

            // Insert Students for Class 2 (Grade 11)
            val s5 = repository.insertStudent(Student(classId = c2.toInt(), name = "Ethan Hunt", rollNumber = "201", monthlyFee = 180.0))
            val s6 = repository.insertStudent(Student(classId = c2.toInt(), name = "Fiona Gallagher", rollNumber = "202", monthlyFee = 180.0))
            val s7 = repository.insertStudent(Student(classId = c2.toInt(), name = "George Clark", rollNumber = "203", monthlyFee = 180.0))

            // Insert Students for Class 3 (Grade 12)
            val s8 = repository.insertStudent(Student(classId = c3.toInt(), name = "Hannah Baker", rollNumber = "301", monthlyFee = 200.0))
            val s9 = repository.insertStudent(Student(classId = c3.toInt(), name = "Ian McKellen", rollNumber = "302", monthlyFee = 200.0))

            // Dates for sample attendance
            val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
            val cal = Calendar.getInstance()
            
            val dates = (0..4).map { i ->
                cal.time = Date()
                cal.add(Calendar.DATE, -i)
                sdf.format(cal.time)
            }

            // Attendance mock entries
            // John Doe: 4 Present, 1 Absent (80% rate)
            repository.insertAttendanceRecord(AttendanceRecord(studentId = s1.toInt(), classId = c1.toInt(), date = dates[0], status = "PRESENT"))
            repository.insertAttendanceRecord(AttendanceRecord(studentId = s1.toInt(), classId = c1.toInt(), date = dates[1], status = "PRESENT"))
            repository.insertAttendanceRecord(AttendanceRecord(studentId = s1.toInt(), classId = c1.toInt(), date = dates[2], status = "PRESENT"))
            repository.insertAttendanceRecord(AttendanceRecord(studentId = s1.toInt(), classId = c1.toInt(), date = dates[3], status = "ABSENT"))
            repository.insertAttendanceRecord(AttendanceRecord(studentId = s1.toInt(), classId = c1.toInt(), date = dates[4], status = "PRESENT"))

            // Alice Smith: 5 Present (100% rate)
            for (date in dates) {
                repository.insertAttendanceRecord(AttendanceRecord(studentId = s2.toInt(), classId = c1.toInt(), date = date, status = "PRESENT"))
            }

            // Bob Johnson: 2 Present, 3 Absent (40% rate -> Low Attendance)
            repository.insertAttendanceRecord(AttendanceRecord(studentId = s3.toInt(), classId = c1.toInt(), date = dates[0], status = "ABSENT"))
            repository.insertAttendanceRecord(AttendanceRecord(studentId = s3.toInt(), classId = c1.toInt(), date = dates[1], status = "PRESENT"))
            repository.insertAttendanceRecord(AttendanceRecord(studentId = s3.toInt(), classId = c1.toInt(), date = dates[2], status = "ABSENT"))
            repository.insertAttendanceRecord(AttendanceRecord(studentId = s3.toInt(), classId = c1.toInt(), date = dates[3], status = "ABSENT"))
            repository.insertAttendanceRecord(AttendanceRecord(studentId = s3.toInt(), classId = c1.toInt(), date = dates[4], status = "PRESENT"))

            // Clara Oswald: 3 Present, 2 Late
            repository.insertAttendanceRecord(AttendanceRecord(studentId = s4.toInt(), classId = c1.toInt(), date = dates[0], status = "PRESENT"))
            repository.insertAttendanceRecord(AttendanceRecord(studentId = s4.toInt(), classId = c1.toInt(), date = dates[1], status = "LATE"))
            repository.insertAttendanceRecord(AttendanceRecord(studentId = s4.toInt(), classId = c1.toInt(), date = dates[2], status = "PRESENT"))
            repository.insertAttendanceRecord(AttendanceRecord(studentId = s4.toInt(), classId = c1.toInt(), date = dates[3], status = "LATE"))
            repository.insertAttendanceRecord(AttendanceRecord(studentId = s4.toInt(), classId = c1.toInt(), date = dates[4], status = "PRESENT"))

            // Grade 11 Class Mock Attendance
            for (date in dates) {
                repository.insertAttendanceRecord(AttendanceRecord(studentId = s5.toInt(), classId = c2.toInt(), date = date, status = "PRESENT"))
                repository.insertAttendanceRecord(AttendanceRecord(studentId = s6.toInt(), classId = c2.toInt(), date = date, status = "LATE"))
                repository.insertAttendanceRecord(AttendanceRecord(studentId = s7.toInt(), classId = c2.toInt(), date = date, status = "ABSENT"))
            }

            // Mock Payments
            // John Doe PAID full for May 2026
            repository.insertPayment(PaymentRecord(studentId = s1.toInt(), amount = 150.0, month = "May", year = "2026", note = "Paid in full via Cash", receiptNumber = 1001))
            
            // Alice Smith PAID full for May 2026 and April 2026
            repository.insertPayment(PaymentRecord(studentId = s2.toInt(), amount = 150.0, month = "May", year = "2026", note = "Online G-Pay transfer", receiptNumber = 1002))
            repository.insertPayment(PaymentRecord(studentId = s2.toInt(), amount = 150.0, month = "April", year = "2026", note = "Bank deposit", receiptNumber = 1003))

            // Bob Johnson PARTIAL payment for May 2026
            repository.insertPayment(PaymentRecord(studentId = s3.toInt(), amount = 70.0, month = "May", year = "2026", note = "Part payment - balance pending", receiptNumber = 1004))

            // Ethan Hunt PAID full for May 2026
            repository.insertPayment(PaymentRecord(studentId = s5.toInt(), amount = 180.0, month = "May", year = "2026", note = "Monthly fee cash", receiptNumber = 1005))
            
            // George Clark UNPAID (No payments)

            // Select default class so screens aren't showing select prompts
            selectedClassForAttendance.value = repository.allClasses.firstOrNull()?.firstOrNull()
            selectedClassForReports.value = repository.allClasses.firstOrNull()?.firstOrNull()
        }
    }

    private fun getCurrentDateString(): String {
        val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        return sdf.format(Date())
    }
}
