package com.example.data

import android.content.Context
import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Entity(tableName = "classes")
data class SchoolClass(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val name: String,
    val subject: String
)

@Entity(
    tableName = "students",
    foreignKeys = [
        ForeignKey(
            entity = SchoolClass::class,
            parentColumns = ["id"],
            childColumns = ["classId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("classId")]
)
data class Student(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val classId: Int,
    val name: String,
    val rollNumber: String,
    val monthlyFee: Double
)

@Entity(
    tableName = "attendance_records",
    foreignKeys = [
        ForeignKey(
            entity = Student::class,
            parentColumns = ["id"],
            childColumns = ["studentId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("studentId"), Index(value = ["studentId", "date"], unique = true)]
)
data class AttendanceRecord(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val studentId: Int,
    val classId: Int,
    val date: String, // Format: YYYY-MM-DD
    val status: String // "PRESENT", "ABSENT", "LATE"
)

@Entity(
    tableName = "payment_records",
    foreignKeys = [
        ForeignKey(
            entity = Student::class,
            parentColumns = ["id"],
            childColumns = ["studentId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("studentId")]
)
data class PaymentRecord(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val studentId: Int,
    val amount: Double,
    val month: String, // E.g., "May"
    val year: String, // E.g., "2026"
    val note: String,
    val paymentDate: Long = System.currentTimeMillis(),
    val receiptNumber: Int
)

@Entity(tableName = "settings")
data class SchoolSetting(
    @PrimaryKey val key: String,
    val value: String
)

@Dao
interface SchoolDao {
    // Classes
    @Query("SELECT * FROM classes ORDER BY name ASC")
    fun getAllClasses(): Flow<List<SchoolClass>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertClass(schoolClass: SchoolClass): Long

    @Delete
    suspend fun deleteClass(schoolClass: SchoolClass)

    // Students
    @Query("SELECT * FROM students ORDER BY name ASC")
    fun getAllStudents(): Flow<List<Student>>

    @Query("SELECT * FROM students WHERE classId = :classId ORDER BY name ASC")
    fun getStudentsByClass(classId: Int): Flow<List<Student>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertStudent(student: Student): Long

    @Query("DELETE FROM students WHERE id = :studentId")
    suspend fun deleteStudent(studentId: Int)

    // Attendance
    @Query("SELECT * FROM attendance_records WHERE classId = :classId AND date = :date")
    fun getAttendanceForClassAndDate(classId: Int, date: String): Flow<List<AttendanceRecord>>

    @Query("SELECT * FROM attendance_records WHERE studentId = :studentId")
    fun getAttendanceForStudent(studentId: Int): Flow<List<AttendanceRecord>>

    @Query("SELECT * FROM attendance_records")
    fun getAllAttendanceRecords(): Flow<List<AttendanceRecord>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAttendanceRecord(record: AttendanceRecord)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAttendanceRecords(records: List<AttendanceRecord>)

    // Payments
    @Query("SELECT * FROM payment_records ORDER BY paymentDate DESC")
    fun getAllPayments(): Flow<List<PaymentRecord>>

    @Query("SELECT * FROM payment_records WHERE studentId = :studentId ORDER BY paymentDate DESC")
    fun getPaymentsByStudent(studentId: Int): Flow<List<PaymentRecord>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPayment(payment: PaymentRecord)

    @Query("DELETE FROM payment_records WHERE id = :paymentId")
    suspend fun deletePayment(paymentId: Int)

    @Query("SELECT MAX(receiptNumber) FROM payment_records")
    suspend fun getMaxReceiptNumber(): Int?

    // Settings
    @Query("SELECT value FROM settings WHERE `key` = :key")
    suspend fun getSettingValue(key: String): String?

    @Query("SELECT value FROM settings WHERE `key` = :key")
    fun getSettingFlow(key: String): Flow<String?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSetting(setting: SchoolSetting)
    
    // Clear all tables for demo/wipe functionality
    @Query("DELETE FROM classes")
    suspend fun clearClasses()

    @Query("DELETE FROM students")
    suspend fun clearStudents()

    @Query("DELETE FROM attendance_records")
    suspend fun clearAttendance()

    @Query("DELETE FROM payment_records")
    suspend fun clearPayments()

    @Query("DELETE FROM settings")
    suspend fun clearSettings()
}

@Database(
    entities = [
        SchoolClass::class,
        Student::class,
        AttendanceRecord::class,
        PaymentRecord::class,
        SchoolSetting::class
    ],
    version = 1,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun schoolDao(): SchoolDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "school_database"
                )
                .fallbackToDestructiveMigration()
                .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
