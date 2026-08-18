package com.sarhansoftware.yazedteacherpro.data

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import android.provider.Settings
import java.io.InputStream
import java.io.OutputStream
import java.security.MessageDigest
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import javax.crypto.Mac
import javax.crypto.spec.SecretKeySpec
import kotlin.math.max

private fun now(): String = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US).format(Date())
private fun today(): String = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date())
private fun monthNow(): String = SimpleDateFormat("yyyy-MM", Locale.US).format(Date())

fun sha256(text: String): String = MessageDigest.getInstance("SHA-256")
    .digest(text.toByteArray())
    .joinToString("") { "%02X".format(it) }

private const val PASSWORD_SALT = "YazedTeacherPro-Santry-2026"
fun hashPassword(password: String): String = sha256(PASSWORD_SALT + password)

data class Student(
    val id: Long,
    val code: String,
    val name: String,
    val guardianPhone: String,
    val studentPhone: String,
    val grade: String,
    val status: String,
    val notes: String,
)

data class Center(val id: Long, val name: String, val address: String, val phone: String, val sharePercent: Double)
data class GroupInfo(
    val id: Long,
    val name: String,
    val subject: String,
    val grade: String,
    val day: String,
    val time: String,
    val monthlyPrice: Double,
    val centerId: Long?,
    val centerName: String,
    val sharePercent: Double,
)

data class AttendanceStudent(val id: Long, val name: String, val guardianPhone: String, val status: String)
data class PaymentRow(
    val id: Long,
    val studentId: Long,
    val student: String,
    val groupId: Long?,
    val group: String,
    val paid: Double,
    val due: Double,
    val date: String,
    val method: String,
    val receipt: String,
    val notes: String,
)
data class ExpenseRow(val id: Long, val date: String, val category: String, val amount: Double, val notes: String)
data class ExamRow(val id: Long, val name: String, val groupId: Long?, val group: String, val maxScore: Double, val date: String)
data class ExamStudent(val id: Long, val name: String, val score: Double)
data class WhatsAppRow(val id: Long, val phone: String, val type: String, val body: String, val status: String, val createdAt: String)
data class TemplateRow(val id: Long, val type: String, val body: String, val active: Boolean, val updatedAt: String)
data class AuditRow(val id: Long, val user: String, val action: String, val details: String, val createdAt: String)
data class DashboardStats(
    val students: Long,
    val groups: Long,
    val presentToday: Long,
    val absentToday: Long,
    val income: Double,
    val debt: Double,
    val expenses: Double,
    val queuedMessages: Long,
)
data class MonthlyReport(
    val month: String,
    val income: Double,
    val debt: Double,
    val expenses: Double,
    val centerShare: Double,
    val teacherNet: Double,
    val students: Long,
    val groups: Long,
    val queuedMessages: Long,
)

data class LicensePayload(
    val hardwareId: String,
    val customerName: String,
    val plan: String,
    val expiryDate: String,
    val features: String,
)

class YazedTeacherProDb(private val context: Context) : SQLiteOpenHelper(context, DB_NAME, null, DB_VERSION) {
    companion object {
        const val DB_NAME = "YazedTeacherPro.db"
        private const val DB_VERSION = 1
        const val TRIAL_DAYS = 15
        private const val LICENSE_SECRET = "CHANGE_THIS_SECRET_TEACHER_HUB_2026_SARHAN"
    }

    override fun onCreate(db: SQLiteDatabase) {
        db.execSQL("PRAGMA foreign_keys=ON")
        db.execSQL("CREATE TABLE Users(Id INTEGER PRIMARY KEY AUTOINCREMENT,UserName TEXT NOT NULL UNIQUE,PasswordHash TEXT NOT NULL,Role TEXT NOT NULL DEFAULT 'User',IsActive INTEGER NOT NULL DEFAULT 1,CreatedAt TEXT NOT NULL)")
        db.execSQL("CREATE TABLE Students(Id INTEGER PRIMARY KEY AUTOINCREMENT,Code TEXT,FullName TEXT NOT NULL,GuardianPhone TEXT,StudentPhone TEXT,Grade TEXT,Status TEXT NOT NULL DEFAULT 'نشط',Notes TEXT,CreatedAt TEXT NOT NULL)")
        db.execSQL("CREATE TABLE Centers(Id INTEGER PRIMARY KEY AUTOINCREMENT,Name TEXT NOT NULL,Address TEXT,Phone TEXT,CenterSharePercent REAL NOT NULL DEFAULT 0,CreatedAt TEXT NOT NULL)")
        db.execSQL("CREATE TABLE GroupsTbl(Id INTEGER PRIMARY KEY AUTOINCREMENT,Name TEXT NOT NULL,Subject TEXT,Grade TEXT,DayOfWeek TEXT,StartTime TEXT,MonthlyPrice REAL NOT NULL DEFAULT 0,CenterId INTEGER,CenterSharePercent REAL NOT NULL DEFAULT 0,IsActive INTEGER NOT NULL DEFAULT 1,CreatedAt TEXT NOT NULL,FOREIGN KEY(CenterId) REFERENCES Centers(Id))")
        db.execSQL("CREATE TABLE StudentGroups(StudentId INTEGER NOT NULL,GroupId INTEGER NOT NULL,PRIMARY KEY(StudentId,GroupId),FOREIGN KEY(StudentId) REFERENCES Students(Id),FOREIGN KEY(GroupId) REFERENCES GroupsTbl(Id))")
        db.execSQL("CREATE TABLE AttendanceSessions(Id INTEGER PRIMARY KEY AUTOINCREMENT,GroupId INTEGER NOT NULL,SessionDate TEXT NOT NULL,CreatedAt TEXT NOT NULL,FOREIGN KEY(GroupId) REFERENCES GroupsTbl(Id))")
        db.execSQL("CREATE TABLE AttendanceRecords(Id INTEGER PRIMARY KEY AUTOINCREMENT,SessionId INTEGER NOT NULL,StudentId INTEGER NOT NULL,Status TEXT NOT NULL,Notes TEXT,UNIQUE(SessionId,StudentId),FOREIGN KEY(SessionId) REFERENCES AttendanceSessions(Id),FOREIGN KEY(StudentId) REFERENCES Students(Id))")
        db.execSQL("CREATE TABLE Payments(Id INTEGER PRIMARY KEY AUTOINCREMENT,StudentId INTEGER NOT NULL,GroupId INTEGER,AmountPaid REAL NOT NULL DEFAULT 0,DueAmount REAL NOT NULL DEFAULT 0,PaymentDate TEXT NOT NULL,Method TEXT,ReceiptNo TEXT,Notes TEXT,FOREIGN KEY(StudentId) REFERENCES Students(Id),FOREIGN KEY(GroupId) REFERENCES GroupsTbl(Id))")
        db.execSQL("CREATE TABLE Expenses(Id INTEGER PRIMARY KEY AUTOINCREMENT,ExpenseDate TEXT NOT NULL,Category TEXT NOT NULL,Amount REAL NOT NULL DEFAULT 0,Notes TEXT)")
        db.execSQL("CREATE TABLE Exams(Id INTEGER PRIMARY KEY AUTOINCREMENT,Name TEXT NOT NULL,GroupId INTEGER,MaxScore REAL NOT NULL,ExamDate TEXT NOT NULL,FOREIGN KEY(GroupId) REFERENCES GroupsTbl(Id))")
        db.execSQL("CREATE TABLE ExamResults(Id INTEGER PRIMARY KEY AUTOINCREMENT,ExamId INTEGER NOT NULL,StudentId INTEGER NOT NULL,Score REAL NOT NULL DEFAULT 0,UNIQUE(ExamId,StudentId),FOREIGN KEY(ExamId) REFERENCES Exams(Id),FOREIGN KEY(StudentId) REFERENCES Students(Id))")
        db.execSQL("CREATE TABLE WhatsAppMessages(Id INTEGER PRIMARY KEY AUTOINCREMENT,StudentId INTEGER,Phone TEXT,MessageType TEXT NOT NULL,MessageBody TEXT NOT NULL,Status TEXT NOT NULL DEFAULT 'Queued',CreatedAt TEXT NOT NULL,SentAt TEXT,FOREIGN KEY(StudentId) REFERENCES Students(Id))")
        db.execSQL("CREATE TABLE BackupLogs(Id INTEGER PRIMARY KEY AUTOINCREMENT,BackupPath TEXT NOT NULL,CreatedAt TEXT NOT NULL)")
        db.execSQL("CREATE TABLE WhatsAppTemplates(Id INTEGER PRIMARY KEY AUTOINCREMENT,MessageType TEXT NOT NULL UNIQUE,Body TEXT NOT NULL,IsActive INTEGER NOT NULL DEFAULT 1,UpdatedAt TEXT NOT NULL)")
        db.execSQL("CREATE TABLE AuditLogs(Id INTEGER PRIMARY KEY AUTOINCREMENT,UserName TEXT,Action TEXT NOT NULL,Details TEXT,CreatedAt TEXT NOT NULL)")
        db.execSQL("CREATE TABLE Settings(Key TEXT PRIMARY KEY,Value TEXT)")
        seed(db)
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) = Unit

    override fun onConfigure(db: SQLiteDatabase) {
        super.onConfigure(db)
        db.setForeignKeyConstraintsEnabled(true)
    }

    private fun seed(db: SQLiteDatabase) {
        val user = ContentValues().apply {
            put("UserName", "1")
            put("PasswordHash", hashPassword("000000"))
            put("Role", "Admin")
            put("IsActive", 1)
            put("CreatedAt", now())
        }
        db.insert("Users", null, user)
        putSetting(db, "BusinessName", "YazedTeacherPro")
        putSetting(db, "DefaultCountryCode", "20")
        putSetting(db, "ShowTrialMessageOnStartup", "1")
        val templates = listOf(
            "غياب" to "نحيط سيادتكم علمًا بأن الطالب / {اسم الطالب} لم يحضر حصة اليوم في مجموعة {المجموعة} بتاريخ {التاريخ}. مع تحيات {اسم السنتر}.",
            "متأخرات" to "برجاء العلم بوجود مبلغ متأخر قدره {المبلغ} جنيه على الطالب / {اسم الطالب} عن مجموعة {المجموعة}. يرجى السداد في أقرب وقت. مع تحيات {اسم السنتر}.",
            "نتيجة" to "نتيجة الطالب / {اسم الطالب}\nامتحان: {اسم الامتحان}\nالدرجة: {الدرجة} من {النهاية}\nالنسبة: {النسبة}%\nالترتيب داخل المجموعة: {الترتيب}\nمع تمنيات {اسم السنتر} بدوام التفوق.",
            "ترحيب" to "مرحبًا بحضرتك، تم تسجيل الطالب / {اسم الطالب} بنجاح في {اسم السنتر}. نتمنى له عامًا دراسيًا موفقًا.",
        )
        templates.forEach { (type, body) ->
            db.execSQL("INSERT INTO WhatsAppTemplates(MessageType,Body,IsActive,UpdatedAt) VALUES(?,?,1,?)", arrayOf(type, body, now()))
        }
    }

    private fun putSetting(db: SQLiteDatabase, key: String, value: String) {
        db.execSQL("INSERT OR REPLACE INTO Settings(Key,Value) VALUES(?,?)", arrayOf(key, value))
    }

    private fun scalarLong(sql: String, args: Array<String> = emptyArray()): Long = readableDatabase.rawQuery(sql, args).use { c -> if (c.moveToFirst()) c.getLong(0) else 0L }
    private fun scalarDouble(sql: String, args: Array<String> = emptyArray()): Double = readableDatabase.rawQuery(sql, args).use { c -> if (c.moveToFirst()) c.getDouble(0) else 0.0 }
    private fun scalarString(sql: String, args: Array<String> = emptyArray()): String? = readableDatabase.rawQuery(sql, args).use { c -> if (c.moveToFirst() && !c.isNull(0)) c.getString(0) else null }

    fun login(username: String, password: String): Boolean {
        val hash = scalarString("SELECT PasswordHash FROM Users WHERE UserName=? AND IsActive=1", arrayOf(username.trim()))
        return hash != null && hash.equals(hashPassword(password), ignoreCase = true)
    }

    fun getSetting(key: String): String? = scalarString("SELECT Value FROM Settings WHERE Key=?", arrayOf(key))
    fun setSetting(key: String, value: String) = writableDatabase.execSQL("INSERT OR REPLACE INTO Settings(Key,Value) VALUES(?,?)", arrayOf(key, value))
    fun businessName(): String = getSetting("BusinessName")?.takeIf { it.isNotBlank() } ?: "YazedTeacherPro"

    fun hardwareId(): String {
        val androidId = Settings.Secure.getString(context.contentResolver, Settings.Secure.ANDROID_ID) ?: "UNKNOWN"
        return androidId.uppercase(Locale.US)
    }

    fun ensureTrialStarted() {
        if (!getSetting("LicenseKey").isNullOrBlank()) return
        if (!getSetting("TrialStartedAt").isNullOrBlank()) return
        setSetting("TrialStartedAt", today())
        setSetting("TrialHardwareId", hardwareId())
        setSetting("LastRunAt", today())
    }

    fun trialDaysRemaining(): Int {
        ensureTrialStarted()
        val start = parseDate(getSetting("TrialStartedAt")) ?: return 0
        val now = parseDate(today()) ?: return 0
        val diff = ((start.time + (TRIAL_DAYS.toLong() - 1L) * 86_400_000L - now.time) / 86_400_000L).toInt() + 1
        return max(0, diff)
    }

    fun isTrialValid(): Boolean {
        ensureTrialStarted()
        if (getSetting("TrialHardwareId")?.equals(hardwareId(), ignoreCase = true) == false) return false
        val last = parseDate(getSetting("LastRunAt"))
        val current = parseDate(today()) ?: return false
        if (last != null && current.before(last)) return false
        setSetting("LastRunAt", today())
        return trialDaysRemaining() > 0
    }

    fun isLicenseValid(key: String = getSetting("LicenseKey") ?: ""): Pair<Boolean, String> {
        if (key.isBlank()) return false to "لا يوجد مفتاح تفعيل."
        return try {
            val decoded = String(android.util.Base64.decode(key.replace('-', '+').replace('_', '/').padBase64(), android.util.Base64.DEFAULT))
            val parts = decoded.split('|')
            if (parts.size != 6) return false to "مفتاح التفعيل غير صحيح."
            val unsigned = parts.take(5).joinToString("|")
            val signature = sign(unsigned)
            if (!signature.equals(parts[5], ignoreCase = true)) return false to "توقيع التفعيل غير صحيح."
            if (!parts[0].equals(hardwareId(), ignoreCase = true)) return false to "السيريال خاص بجهاز آخر."
            val expiry = SimpleDateFormat("yyyyMMdd", Locale.US).parse(parts[3]) ?: return false to "تاريخ الترخيص غير صحيح."
            val current = parseDate(today()) ?: Date()
            if (expiry.before(current)) return false to "انتهت مدة الترخيص."
            true to "مفعل حتى ${parts[3]}"
        } catch (_: Exception) {
            false to "تعذر قراءة مفتاح التفعيل."
        }
    }

    fun licensePayload(key: String = getSetting("LicenseKey") ?: ""): LicensePayload? {
        if (!isLicenseValid(key).first) return null
        return try {
            val decoded = String(android.util.Base64.decode(key.replace('-', '+').replace('_', '/').padBase64(), android.util.Base64.DEFAULT))
            val p = decoded.split('|')
            LicensePayload(p[0], p[1], p[2], p[3], p[4])
        } catch (_: Exception) { null }
    }

    private fun String.padBase64(): String {
        var s = this
        while (s.length % 4 != 0) s += "="
        return s
    }

    private fun sign(payload: String): String {
        val mac = Mac.getInstance("HmacSHA256")
        mac.init(SecretKeySpec(LICENSE_SECRET.toByteArray(), "HmacSHA256"))
        return mac.doFinal(payload.toByteArray()).joinToString("") { "%02X".format(it) }.take(32)
    }

    fun saveLicense(key: String) = setSetting("LicenseKey", key.trim())
    fun isUnlocked(): Boolean = isLicenseValid().first || isTrialValid()

    private fun parseDate(text: String?): Date? = try { if (text.isNullOrBlank()) null else SimpleDateFormat("yyyy-MM-dd", Locale.US).parse(text) } catch (_: Exception) { null }

    fun dashboardStats(): DashboardStats {
        val d = today(); val m = monthNow()
        return DashboardStats(
            students = scalarLong("SELECT COUNT(*) FROM Students WHERE Status='نشط'"),
            groups = scalarLong("SELECT COUNT(*) FROM GroupsTbl WHERE IsActive=1"),
            presentToday = scalarLong("SELECT COUNT(*) FROM AttendanceRecords ar JOIN AttendanceSessions s ON s.Id=ar.SessionId WHERE s.SessionDate=? AND ar.Status='حضور'", arrayOf(d)),
            absentToday = scalarLong("SELECT COUNT(*) FROM AttendanceRecords ar JOIN AttendanceSessions s ON s.Id=ar.SessionId WHERE s.SessionDate=? AND ar.Status='غياب'", arrayOf(d)),
            income = scalarDouble("SELECT COALESCE(SUM(AmountPaid),0) FROM Payments WHERE substr(PaymentDate,1,7)=?", arrayOf(m)),
            debt = scalarDouble("SELECT COALESCE(SUM(DueAmount),0) FROM Payments WHERE DueAmount>0"),
            expenses = scalarDouble("SELECT COALESCE(SUM(Amount),0) FROM Expenses WHERE substr(ExpenseDate,1,7)=?", arrayOf(m)),
            queuedMessages = scalarLong("SELECT COUNT(*) FROM WhatsAppMessages WHERE Status='Queued'"),
        )
    }

    fun students(search: String = ""): List<Student> {
        val q = "%${search.trim()}%"
        return readableDatabase.rawQuery("SELECT Id,COALESCE(Code,''),FullName,COALESCE(GuardianPhone,''),COALESCE(StudentPhone,''),COALESCE(Grade,''),Status,COALESCE(Notes,'') FROM Students WHERE FullName LIKE ? OR GuardianPhone LIKE ? ORDER BY Id DESC", arrayOf(q, q)).use { c ->
            buildList { while (c.moveToNext()) add(Student(c.getLong(0), c.getString(1), c.getString(2), c.getString(3), c.getString(4), c.getString(5), c.getString(6), c.getString(7))) }
        }
    }

    fun activeStudents(): List<Student> = readableDatabase.rawQuery("SELECT Id,COALESCE(Code,''),FullName,COALESCE(GuardianPhone,''),COALESCE(StudentPhone,''),COALESCE(Grade,''),Status,COALESCE(Notes,'') FROM Students WHERE Status='نشط' ORDER BY FullName", null).use { c ->
        buildList { while (c.moveToNext()) add(Student(c.getLong(0), c.getString(1), c.getString(2), c.getString(3), c.getString(4), c.getString(5), c.getString(6), c.getString(7))) }
    }

    fun addStudent(name: String, guardian: String, studentPhone: String, grade: String, status: String, notes: String, user: String) {
        val code = "TH-" + SimpleDateFormat("yyMMddHHmmss", Locale.US).format(Date())
        val cv = ContentValues().apply { put("Code", code); put("FullName", name.trim()); put("GuardianPhone", guardian.trim()); put("StudentPhone", studentPhone.trim()); put("Grade", grade.trim()); put("Status", status); put("Notes", notes.trim()); put("CreatedAt", now()) }
        val id = writableDatabase.insert("Students", null, cv)
        audit(user, "AddStudent", "StudentId=$id")
        if (guardian.isNotBlank()) queueWhatsApp(id, guardian, "ترحيب", buildMessage("ترحيب", mapOf("اسم الطالب" to name.trim(), "اسم السنتر" to businessName())))
    }

    fun updateStudent(student: Student, user: String) {
        val cv = ContentValues().apply { put("FullName", student.name.trim()); put("GuardianPhone", student.guardianPhone.trim()); put("StudentPhone", student.studentPhone.trim()); put("Grade", student.grade.trim()); put("Status", student.status); put("Notes", student.notes.trim()) }
        writableDatabase.update("Students", cv, "Id=?", arrayOf(student.id.toString()))
        audit(user, "UpdateStudent", "StudentId=${student.id}")
    }

    fun centers(): List<Center> = readableDatabase.rawQuery("SELECT Id,Name,COALESCE(Address,''),COALESCE(Phone,''),CenterSharePercent FROM Centers ORDER BY Id DESC", null).use { c -> buildList { while (c.moveToNext()) add(Center(c.getLong(0), c.getString(1), c.getString(2), c.getString(3), c.getDouble(4))) } }
    fun addCenter(name: String, address: String, phone: String, percent: Double) {
        writableDatabase.execSQL("INSERT INTO Centers(Name,Address,Phone,CenterSharePercent,CreatedAt) VALUES(?,?,?,?,?)", arrayOf<Any?>(name.trim(), address.trim(), phone.trim(), percent, now()))
    }

    fun groups(): List<GroupInfo> = readableDatabase.rawQuery("SELECT g.Id,g.Name,COALESCE(g.Subject,''),COALESCE(g.Grade,''),COALESCE(g.DayOfWeek,''),COALESCE(g.StartTime,''),g.MonthlyPrice,g.CenterId,COALESCE(c.Name,''),g.CenterSharePercent FROM GroupsTbl g LEFT JOIN Centers c ON c.Id=g.CenterId WHERE g.IsActive=1 ORDER BY g.Id DESC", null).use { c ->
        buildList { while (c.moveToNext()) add(GroupInfo(c.getLong(0), c.getString(1), c.getString(2), c.getString(3), c.getString(4), c.getString(5), c.getDouble(6), if (c.isNull(7)) null else c.getLong(7), c.getString(8), c.getDouble(9))) }
    }

    fun addGroup(name: String, subject: String, grade: String, day: String, time: String, price: Double, centerId: Long?, percent: Double) {
        writableDatabase.execSQL("INSERT INTO GroupsTbl(Name,Subject,Grade,DayOfWeek,StartTime,MonthlyPrice,CenterId,CenterSharePercent,IsActive,CreatedAt) VALUES(?,?,?,?,?,?,?,?,1,?)", arrayOf<Any?>(name.trim(), subject.trim(), grade.trim(), day.trim(), time.trim(), price, centerId, percent, now()))
    }

    fun linkStudentToGroup(studentId: Long, groupId: Long) = writableDatabase.execSQL("INSERT OR IGNORE INTO StudentGroups(StudentId,GroupId) VALUES(?,?)", arrayOf(studentId, groupId))

    fun attendanceStudents(groupId: Long, date: String): List<AttendanceStudent> = readableDatabase.rawQuery(
        "SELECT s.Id,s.FullName,COALESCE(s.GuardianPhone,''),COALESCE(ar.Status,'لم يسجل') FROM StudentGroups sg JOIN Students s ON s.Id=sg.StudentId LEFT JOIN AttendanceSessions sess ON sess.GroupId=sg.GroupId AND sess.SessionDate=? LEFT JOIN AttendanceRecords ar ON ar.SessionId=sess.Id AND ar.StudentId=s.Id WHERE sg.GroupId=? AND s.Status='نشط' ORDER BY s.FullName",
        arrayOf(date, groupId.toString())
    ).use { c -> buildList { while (c.moveToNext()) add(AttendanceStudent(c.getLong(0), c.getString(1), c.getString(2), c.getString(3))) } }

    private fun attendanceSessionId(groupId: Long, date: String): Long {
        val existing = scalarLong("SELECT COALESCE(MAX(Id),0) FROM AttendanceSessions WHERE GroupId=? AND SessionDate=?", arrayOf(groupId.toString(), date))
        if (existing > 0) return existing
        val cv = ContentValues().apply { put("GroupId", groupId); put("SessionDate", date); put("CreatedAt", now()) }
        return writableDatabase.insert("AttendanceSessions", null, cv)
    }

    fun markAttendance(groupId: Long, groupName: String, studentId: Long, status: String, date: String) {
        val sessionId = attendanceSessionId(groupId, date)
        writableDatabase.execSQL("INSERT OR REPLACE INTO AttendanceRecords(Id,SessionId,StudentId,Status,Notes) VALUES((SELECT Id FROM AttendanceRecords WHERE SessionId=? AND StudentId=?),?,?,?,'')", arrayOf<Any?>(sessionId, studentId, sessionId, studentId, status))
        if (status == "غياب") {
            val s = students().firstOrNull { it.id == studentId } ?: return
            val body = buildMessage("غياب", mapOf("اسم الطالب" to s.name, "المجموعة" to groupName, "التاريخ" to date, "اسم السنتر" to businessName()))
            queueWhatsApp(studentId, s.guardianPhone, "غياب", body)
        }
    }

    fun payments(): List<PaymentRow> = readableDatabase.rawQuery("SELECT p.Id,p.StudentId,s.FullName,p.GroupId,COALESCE(g.Name,''),p.AmountPaid,p.DueAmount,p.PaymentDate,COALESCE(p.Method,''),COALESCE(p.ReceiptNo,''),COALESCE(p.Notes,'') FROM Payments p JOIN Students s ON s.Id=p.StudentId LEFT JOIN GroupsTbl g ON g.Id=p.GroupId ORDER BY p.Id DESC", null).use { c ->
        buildList { while (c.moveToNext()) add(PaymentRow(c.getLong(0), c.getLong(1), c.getString(2), if (c.isNull(3)) null else c.getLong(3), c.getString(4), c.getDouble(5), c.getDouble(6), c.getString(7), c.getString(8), c.getString(9), c.getString(10))) }
    }

    fun addPayment(studentId: Long, groupId: Long?, paid: Double, due: Double, method: String, receipt: String, notes: String) {
        writableDatabase.execSQL("INSERT INTO Payments(StudentId,GroupId,AmountPaid,DueAmount,PaymentDate,Method,ReceiptNo,Notes) VALUES(?,?,?,?,?,?,?,?)", arrayOf<Any?>(studentId, groupId, paid, due, today(), method.trim(), receipt.trim(), notes.trim()))
        if (due > 0) {
            val s = students().firstOrNull { it.id == studentId }
            val g = groups().firstOrNull { it.id == groupId }
            if (s != null) queueWhatsApp(studentId, s.guardianPhone, "متأخرات", buildMessage("متأخرات", mapOf("اسم الطالب" to s.name, "المجموعة" to (g?.name ?: ""), "المبلغ" to "%.2f".format(Locale.US, due), "اسم السنتر" to businessName())))
        }
    }

    fun expenses(): List<ExpenseRow> = readableDatabase.rawQuery("SELECT Id,ExpenseDate,Category,Amount,COALESCE(Notes,'') FROM Expenses ORDER BY Id DESC", null).use { c -> buildList { while (c.moveToNext()) add(ExpenseRow(c.getLong(0), c.getString(1), c.getString(2), c.getDouble(3), c.getString(4))) } }
    fun addExpense(category: String, amount: Double, notes: String) = writableDatabase.execSQL("INSERT INTO Expenses(ExpenseDate,Category,Amount,Notes) VALUES(?,?,?,?)", arrayOf<Any?>(today(), category.trim(), amount, notes.trim()))

    fun exams(): List<ExamRow> = readableDatabase.rawQuery("SELECT e.Id,e.Name,e.GroupId,COALESCE(g.Name,''),e.MaxScore,e.ExamDate FROM Exams e LEFT JOIN GroupsTbl g ON g.Id=e.GroupId ORDER BY e.Id DESC", null).use { c -> buildList { while (c.moveToNext()) add(ExamRow(c.getLong(0), c.getString(1), if (c.isNull(2)) null else c.getLong(2), c.getString(3), c.getDouble(4), c.getString(5))) } }
    fun addExam(name: String, groupId: Long?, maxScore: Double) = writableDatabase.execSQL("INSERT INTO Exams(Name,GroupId,MaxScore,ExamDate) VALUES(?,?,?,?)", arrayOf<Any?>(name.trim(), groupId, maxScore, today()))

    fun examStudents(examId: Long): List<ExamStudent> = readableDatabase.rawQuery("SELECT s.Id,s.FullName,COALESCE(r.Score,0) FROM Exams e JOIN StudentGroups sg ON sg.GroupId=e.GroupId JOIN Students s ON s.Id=sg.StudentId LEFT JOIN ExamResults r ON r.ExamId=e.Id AND r.StudentId=s.Id WHERE e.Id=? ORDER BY s.FullName", arrayOf(examId.toString())).use { c -> buildList { while (c.moveToNext()) add(ExamStudent(c.getLong(0), c.getString(1), c.getDouble(2))) } }

    fun saveExamScore(examId: Long, studentId: Long, score: Double) {
        writableDatabase.execSQL("INSERT OR REPLACE INTO ExamResults(Id,ExamId,StudentId,Score) VALUES((SELECT Id FROM ExamResults WHERE ExamId=? AND StudentId=?),?,?,?)", arrayOf<Any?>(examId, studentId, examId, studentId, score))
        queueExamMessage(examId, studentId)
    }

    private fun queueExamMessage(examId: Long, studentId: Long) {
        readableDatabase.rawQuery("SELECT s.FullName,COALESCE(s.GuardianPhone,''),e.Name,e.MaxScore,r.Score,(SELECT COUNT(*)+1 FROM ExamResults r2 WHERE r2.ExamId=e.Id AND r2.Score>r.Score) FROM ExamResults r JOIN Students s ON s.Id=r.StudentId JOIN Exams e ON e.Id=r.ExamId WHERE r.ExamId=? AND r.StudentId=?", arrayOf(examId.toString(), studentId.toString())).use { c ->
            if (!c.moveToFirst()) return
            val score = c.getDouble(4); val maxScore = c.getDouble(3); val percent = if (maxScore <= 0) 0.0 else score / maxScore * 100.0
            val body = buildMessage("نتيجة", mapOf("اسم الطالب" to c.getString(0), "اسم الامتحان" to c.getString(2), "الدرجة" to score.toString(), "النهاية" to maxScore.toString(), "النسبة" to "%.2f".format(Locale.US, percent), "الترتيب" to c.getLong(5).toString(), "اسم السنتر" to businessName()))
            queueWhatsApp(studentId, c.getString(1), "نتيجة", body)
        }
    }

    fun whatsappMessages(): List<WhatsAppRow> = readableDatabase.rawQuery("SELECT Id,COALESCE(Phone,''),MessageType,MessageBody,Status,CreatedAt FROM WhatsAppMessages ORDER BY Id DESC", null).use { c -> buildList { while (c.moveToNext()) add(WhatsAppRow(c.getLong(0), c.getString(1), c.getString(2), c.getString(3), c.getString(4), c.getString(5))) } }
    fun markWhatsApp(id: Long, status: String) = writableDatabase.execSQL("UPDATE WhatsAppMessages SET Status=?,SentAt=? WHERE Id=?", arrayOf<Any?>(status, if (status == "Sent") now() else null, id))

    fun normalizePhone(phone: String): String {
        var digits = phone.filter { it.isDigit() }
        if (digits.startsWith("00")) digits = digits.drop(2)
        val code = (getSetting("DefaultCountryCode") ?: "20").trim().trimStart('+')
        if (digits.startsWith("0") && code.isNotBlank()) digits = code + digits.drop(1)
        return digits
    }

    fun templates(): List<TemplateRow> = readableDatabase.rawQuery("SELECT Id,MessageType,Body,IsActive,UpdatedAt FROM WhatsAppTemplates ORDER BY MessageType", null).use { c -> buildList { while (c.moveToNext()) add(TemplateRow(c.getLong(0), c.getString(1), c.getString(2), c.getInt(3) == 1, c.getString(4))) } }
    fun saveTemplate(type: String, body: String, active: Boolean, user: String) {
        writableDatabase.execSQL("INSERT OR REPLACE INTO WhatsAppTemplates(Id,MessageType,Body,IsActive,UpdatedAt) VALUES((SELECT Id FROM WhatsAppTemplates WHERE MessageType=?),?,?,?,?)", arrayOf<Any?>(type.trim(), type.trim(), body, if (active) 1 else 0, now()))
        audit(user, "UpdateWhatsAppTemplate", type.trim())
    }

    private fun buildMessage(type: String, values: Map<String, String>): String {
        var template = scalarString("SELECT Body FROM WhatsAppTemplates WHERE MessageType=? AND IsActive=1", arrayOf(type)) ?: "{اسم السنتر}"
        values.forEach { (k, v) -> template = template.replace("{$k}", v) }
        return template.replace("{اسم السنتر}", businessName())
    }

    private fun queueWhatsApp(studentId: Long, phone: String, type: String, body: String) {
        if (body.isBlank()) return
        writableDatabase.execSQL("INSERT INTO WhatsAppMessages(StudentId,Phone,MessageType,MessageBody,Status,CreatedAt) VALUES(?,?,?,?, 'Queued',?)", arrayOf<Any?>(studentId, phone.trim(), type, body, now()))
    }

    fun monthlyReport(): MonthlyReport {
        val m = monthNow()
        val income = scalarDouble("SELECT COALESCE(SUM(AmountPaid),0) FROM Payments WHERE substr(PaymentDate,1,7)=?", arrayOf(m))
        val debt = scalarDouble("SELECT COALESCE(SUM(DueAmount),0) FROM Payments WHERE DueAmount>0")
        val expenses = scalarDouble("SELECT COALESCE(SUM(Amount),0) FROM Expenses WHERE substr(ExpenseDate,1,7)=?", arrayOf(m))
        val centerShare = scalarDouble("SELECT COALESCE(SUM(p.AmountPaid*(g.CenterSharePercent/100.0)),0) FROM Payments p LEFT JOIN GroupsTbl g ON g.Id=p.GroupId WHERE substr(p.PaymentDate,1,7)=?", arrayOf(m))
        return MonthlyReport(m, income, debt, expenses, centerShare, income - expenses - centerShare, scalarLong("SELECT COUNT(*) FROM Students WHERE Status='نشط'"), scalarLong("SELECT COUNT(*) FROM GroupsTbl WHERE IsActive=1"), scalarLong("SELECT COUNT(*) FROM WhatsAppMessages WHERE Status='Queued'"))
    }

    fun audit(user: String, action: String, details: String) = writableDatabase.execSQL("INSERT INTO AuditLogs(UserName,Action,Details,CreatedAt) VALUES(?,?,?,?)", arrayOf(user, action, details, now()))
    fun auditRows(search: String = ""): List<AuditRow> {
        val q = "%${search.trim()}%"
        return readableDatabase.rawQuery("SELECT Id,COALESCE(UserName,''),Action,COALESCE(Details,''),CreatedAt FROM AuditLogs WHERE Action LIKE ? OR Details LIKE ? OR UserName LIKE ? ORDER BY Id DESC LIMIT 500", arrayOf(q, q, q)).use { c -> buildList { while (c.moveToNext()) add(AuditRow(c.getLong(0), c.getString(1), c.getString(2), c.getString(3), c.getString(4))) } }
    }

    fun reportText(): String {
        val r = monthlyReport()
        return """${businessName()} - تقرير شهر ${r.month}
====================================
إجمالي الدخل:        ${"%.2f".format(Locale.US, r.income)} جنيه
إجمالي المتأخرات:    ${"%.2f".format(Locale.US, r.debt)} جنيه
مصروفات الشهر:       ${"%.2f".format(Locale.US, r.expenses)} جنيه
نصيب السناتر:        ${"%.2f".format(Locale.US, r.centerShare)} جنيه
صافي المدرس:         ${"%.2f".format(Locale.US, r.teacherNet)} جنيه

عدد الطلبة النشطين:  ${r.students}
عدد المجموعات:       ${r.groups}
رسائل واتساب المعلقة:${r.queuedMessages}
""".trimIndent()
    }

    private fun csvCell(value: Any?): String = "\"${(value?.toString() ?: "").replace("\"", "\"\"")}\""
    private fun csv(headers: List<String>, rows: List<List<Any?>>): String = buildString {
        append('\uFEFF')
        appendLine(headers.joinToString(",") { csvCell(it) })
        rows.forEach { row -> appendLine(row.joinToString(",") { csvCell(it) }) }
    }

    fun studentsCsv(): String = csv(listOf("الكود","اسم الطالب","ولي الأمر","الطالب","الصف","الحالة","ملاحظات"), students().map { listOf(it.code,it.name,it.guardianPhone,it.studentPhone,it.grade,it.status,it.notes) })
    fun paymentsCsv(): String = csv(listOf("الطالب","المجموعة","مدفوع","متأخر","التاريخ","الطريقة","الإيصال","ملاحظات"), payments().map { listOf(it.student,it.group,it.paid,it.due,it.date,it.method,it.receipt,it.notes) })
    fun whatsappCsv(): String = csv(listOf("رقم ولي الأمر","النوع","الرسالة","الحالة","تاريخ الإنشاء"), whatsappMessages().map { listOf(it.phone,it.type,it.body,it.status,it.createdAt) })
    fun auditCsv(): String = csv(listOf("المستخدم","العملية","التفاصيل","التاريخ"), auditRows().map { listOf(it.user,it.action,it.details,it.createdAt) })
    fun debtsCsv(): String {
        val rows = readableDatabase.rawQuery("SELECT s.FullName,COALESCE(s.GuardianPhone,''),COALESCE(g.Name,''),p.DueAmount,p.PaymentDate FROM Payments p JOIN Students s ON s.Id=p.StudentId LEFT JOIN GroupsTbl g ON g.Id=p.GroupId WHERE p.DueAmount>0 ORDER BY p.DueAmount DESC", null).use { c -> buildList<List<Any?>> { while (c.moveToNext()) add(listOf(c.getString(0),c.getString(1),c.getString(2),c.getDouble(3),c.getString(4))) } }
        return csv(listOf("الطالب","ولي الأمر","المجموعة","المتأخر","تاريخ آخر تسجيل"), rows)
    }
    fun attendanceMonthCsv(): String {
        val rows = readableDatabase.rawQuery("SELECT sess.SessionDate,g.Name,s.FullName,ar.Status FROM AttendanceRecords ar JOIN AttendanceSessions sess ON sess.Id=ar.SessionId JOIN GroupsTbl g ON g.Id=sess.GroupId JOIN Students s ON s.Id=ar.StudentId WHERE substr(sess.SessionDate,1,7)=? ORDER BY sess.SessionDate DESC", arrayOf(monthNow())).use { c -> buildList<List<Any?>> { while (c.moveToNext()) add(listOf(c.getString(0),c.getString(1),c.getString(2),c.getString(3))) } }
        return csv(listOf("التاريخ","المجموعة","الطالب","الحالة"), rows)
    }

    fun copyDatabaseTo(output: OutputStream) {
        close()
        context.getDatabasePath(DB_NAME).inputStream().use { it.copyTo(output) }
    }

    fun restoreDatabaseFrom(input: InputStream) {
        close()
        val dbFile = context.getDatabasePath(DB_NAME)
        dbFile.parentFile?.mkdirs()
        dbFile.outputStream().use { out -> input.copyTo(out) }
    }
}
