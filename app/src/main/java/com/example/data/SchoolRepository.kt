package com.example.data

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow

class SchoolRepository(private val schoolDao: SchoolDao) {
    // Classes
    val allClasses: Flow<List<SchoolClass>> = schoolDao.getAllClasses()
    suspend fun insertClass(schoolClass: SchoolClass): Long = schoolDao.insertClass(schoolClass)
    suspend fun deleteClass(schoolClass: SchoolClass) = schoolDao.deleteClass(schoolClass)

    // Students
    val allStudents: Flow<List<Student>> = schoolDao.getAllStudents()
    fun getStudentsByClass(classId: Int): Flow<List<Student>> = schoolDao.getStudentsByClass(classId)
    suspend fun insertStudent(student: Student): Long = schoolDao.insertStudent(student)
    suspend fun deleteStudent(studentId: Int) = schoolDao.deleteStudent(studentId)

    // Attendance
    val allAttendance: Flow<List<AttendanceRecord>> = schoolDao.getAllAttendanceRecords()
    fun getAttendanceForClassAndDate(classId: Int, date: String): Flow<List<AttendanceRecord>> =
        schoolDao.getAttendanceForClassAndDate(classId, date)
    fun getAttendanceForStudent(studentId: Int): Flow<List<AttendanceRecord>> =
        schoolDao.getAttendanceForStudent(studentId)
    suspend fun insertAttendanceRecord(record: AttendanceRecord) = schoolDao.insertAttendanceRecord(record)
    suspend fun insertAttendanceRecords(records: List<AttendanceRecord>) = schoolDao.insertAttendanceRecords(records)

    // Payments
    val allPayments: Flow<List<PaymentRecord>> = schoolDao.getAllPayments()
    fun getPaymentsByStudent(studentId: Int): Flow<List<PaymentRecord>> = schoolDao.getPaymentsByStudent(studentId)
    suspend fun insertPayment(payment: PaymentRecord) = schoolDao.insertPayment(payment)
    suspend fun deletePayment(paymentId: Int) = schoolDao.deletePayment(paymentId)
    suspend fun getNextReceiptNumber(): Int {
        val max = schoolDao.getMaxReceiptNumber() ?: 1000
        return max + 1
    }

    // Settings
    suspend fun getSettingValue(key: String, defaultValue: String): String {
        return schoolDao.getSettingValue(key) ?: defaultValue
    }

    fun getSettingFlow(key: String, defaultValue: String): Flow<String> {
        return flow {
            schoolDao.getSettingFlow(key).collect { value ->
                emit(value ?: defaultValue)
            }
        }
    }

    suspend fun insertSetting(key: String, value: String) {
        schoolDao.insertSetting(SchoolSetting(key, value))
    }

    // Database Wiping/Reset
    suspend fun wipeData() {
        schoolDao.clearPayments()
        schoolDao.clearAttendance()
        schoolDao.clearStudents()
        schoolDao.clearClasses()
        schoolDao.clearSettings()
    }
}
