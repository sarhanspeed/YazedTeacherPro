package com.sarhansoftware.yazedteacherpro.professional

import com.sarhansoftware.yazedteacherpro.data.YazedTeacherProDb
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

private fun isoDate(): String = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date())
private fun isoMonth(): String = SimpleDateFormat("yyyy-MM", Locale.US).format(Date())

data class DashboardV2(
    val students: Long,
    val groups: Long,
    val lessonsToday: Int,
    val attendanceRate: Double,
    val income: Double,
    val debt: Double,
    val expenses: Double,
    val atRisk: Long,
)

data class TodayLesson(
    val groupId: Long,
    val groupName: String,
    val subject: String,
    val grade: String,
    val time: String,
    val center: String,
    val students: Long,
    val completed: Boolean,
    val topic: String,
    val homework: String,
)

data class StudentSummaryV2(
    val id: Long,
    val name: String,
    val grade: String,
    val guardianPhone: String,
    val groups: String,
    val attendanceRate: Double,
    val paid: Double,
    val debt: Double,
    val examAverage: Double,
)

data class ExamResultMini(val exam: String, val score: Double, val maxScore: Double, val percent: Double, val date: String)

data class Student360(
    val student: StudentSummaryV2,
    val studentPhone: String,
    val status: String,
    val notes: String,
    val present: Long,
    val absent: Long,
    val late: Long,
    val exams: List<ExamResultMini>,
    val lastPaymentDate: String,
)

data class SmartAlert(val level: String, val title: String, val detail: String, val studentId: Long? = null)
data class TrendPoint(val month: String, val income: Double, val expenses: Double)
data class DebtorRow(val studentId: Long, val name: String, val phone: String, val debt: Double, val lastPayment: String)

class ProfessionalRepository(private val db: YazedTeacherProDb) {
    init { ensureProfessionalSchema() }

    private fun ensureProfessionalSchema() {
        db.writableDatabase.execSQL(
            """CREATE TABLE IF NOT EXISTS ProfessionalLessonSessions(
                Id INTEGER PRIMARY KEY AUTOINCREMENT,
                GroupId INTEGER NOT NULL,
                SessionDate TEXT NOT NULL,
                Topic TEXT NOT NULL DEFAULT '',
                Homework TEXT NOT NULL DEFAULT '',
                Notes TEXT NOT NULL DEFAULT '',
                IsCompleted INTEGER NOT NULL DEFAULT 0,
                CreatedAt TEXT NOT NULL,
                UNIQUE(GroupId, SessionDate)
            )""".trimIndent()
        )
        db.writableDatabase.execSQL("CREATE INDEX IF NOT EXISTS IX_AttendanceSessions_Date ON AttendanceSessions(SessionDate)")
        db.writableDatabase.execSQL("CREATE INDEX IF NOT EXISTS IX_Payments_Date ON Payments(PaymentDate)")
        db.writableDatabase.execSQL("CREATE INDEX IF NOT EXISTS IX_ExamResults_Student ON ExamResults(StudentId)")
    }

    private fun long(sql: String, args: Array<String> = emptyArray()): Long = db.readableDatabase.rawQuery(sql, args).use { c -> if (c.moveToFirst()) c.getLong(0) else 0L }
    private fun double(sql: String, args: Array<String> = emptyArray()): Double = db.readableDatabase.rawQuery(sql, args).use { c -> if (c.moveToFirst()) c.getDouble(0) else 0.0 }
    private fun text(sql: String, args: Array<String> = emptyArray()): String = db.readableDatabase.rawQuery(sql, args).use { c -> if (c.moveToFirst() && !c.isNull(0)) c.getString(0) else "" }

    fun dashboard(): DashboardV2 {
        val month = isoMonth()
        val present = long("SELECT COUNT(*) FROM AttendanceRecords ar JOIN AttendanceSessions s ON s.Id=ar.SessionId WHERE substr(s.SessionDate,1,7)=? AND ar.Status='حاضر'", arrayOf(month))
        val totalAttendance = long("SELECT COUNT(*) FROM AttendanceRecords ar JOIN AttendanceSessions s ON s.Id=ar.SessionId WHERE substr(s.SessionDate,1,7)=?", arrayOf(month))
        val debtStudents = long("SELECT COUNT(DISTINCT StudentId) FROM Payments WHERE DueAmount>0")
        val absentRisk = long("SELECT COUNT(*) FROM (SELECT ar.StudentId FROM AttendanceRecords ar JOIN AttendanceSessions s ON s.Id=ar.SessionId WHERE ar.Status='غياب' AND s.SessionDate>=date('now','-30 day') GROUP BY ar.StudentId HAVING COUNT(*)>=3)")
        return DashboardV2(
            students = long("SELECT COUNT(*) FROM Students WHERE Status='نشط'"),
            groups = long("SELECT COUNT(*) FROM GroupsTbl WHERE IsActive=1"),
            lessonsToday = todayLessons().size,
            attendanceRate = if (totalAttendance == 0L) 0.0 else present * 100.0 / totalAttendance,
            income = double("SELECT COALESCE(SUM(AmountPaid),0) FROM Payments WHERE substr(PaymentDate,1,7)=?", arrayOf(month)),
            debt = double("SELECT COALESCE(SUM(DueAmount),0) FROM Payments WHERE DueAmount>0"),
            expenses = double("SELECT COALESCE(SUM(Amount),0) FROM Expenses WHERE substr(ExpenseDate,1,7)=?", arrayOf(month)),
            atRisk = debtStudents + absentRisk,
        )
    }

    private fun arabicToday(): Set<String> {
        val day = Calendar.getInstance().get(Calendar.DAY_OF_WEEK)
        return when (day) {
            Calendar.SATURDAY -> setOf("السبت", "سبت")
            Calendar.SUNDAY -> setOf("الأحد", "الاحد", "أحد", "احد")
            Calendar.MONDAY -> setOf("الإثنين", "الاثنين", "الإثنين", "اثنين")
            Calendar.TUESDAY -> setOf("الثلاثاء", "ثلاثاء")
            Calendar.WEDNESDAY -> setOf("الأربعاء", "الاربعاء", "أربعاء", "اربعاء")
            Calendar.THURSDAY -> setOf("الخميس", "خميس")
            else -> setOf("الجمعة", "جمعة")
        }
    }

    fun todayLessons(): List<TodayLesson> {
        val accepted = arabicToday()
        val date = isoDate()
        val output = mutableListOf<TodayLesson>()
        db.readableDatabase.rawQuery(
            """SELECT g.Id,g.Name,COALESCE(g.Subject,''),COALESCE(g.Grade,''),COALESCE(g.DayOfWeek,''),COALESCE(g.StartTime,''),COALESCE(c.Name,''),
               (SELECT COUNT(*) FROM StudentGroups sg WHERE sg.GroupId=g.Id),
               COALESCE(p.IsCompleted,0),COALESCE(p.Topic,''),COALESCE(p.Homework,'')
               FROM GroupsTbl g LEFT JOIN Centers c ON c.Id=g.CenterId
               LEFT JOIN ProfessionalLessonSessions p ON p.GroupId=g.Id AND p.SessionDate=?
               WHERE g.IsActive=1 ORDER BY g.StartTime,g.Name""".trimIndent(), arrayOf(date)
        ).use { c ->
            while (c.moveToNext()) {
                if (c.getString(4).trim() !in accepted) continue
                output += TodayLesson(
                    groupId = c.getLong(0), groupName = c.getString(1), subject = c.getString(2), grade = c.getString(3),
                    time = c.getString(5), center = c.getString(6), students = c.getLong(7), completed = c.getInt(8) == 1,
                    topic = c.getString(9), homework = c.getString(10)
                )
            }
        }
        return output
    }

    fun saveLesson(groupId: Long, topic: String, homework: String, notes: String, completed: Boolean) {
        val date = isoDate()
        db.writableDatabase.execSQL(
            """INSERT INTO ProfessionalLessonSessions(GroupId,SessionDate,Topic,Homework,Notes,IsCompleted,CreatedAt)
               VALUES(?,?,?,?,?,?,datetime('now'))
               ON CONFLICT(GroupId,SessionDate) DO UPDATE SET Topic=excluded.Topic,Homework=excluded.Homework,Notes=excluded.Notes,IsCompleted=excluded.IsCompleted""".trimIndent(),
            arrayOf<Any?>(groupId, date, topic.trim(), homework.trim(), notes.trim(), if (completed) 1 else 0)
        )
    }

    fun studentSummaries(search: String = ""): List<StudentSummaryV2> {
        val q = "%${search.trim()}%"
        val list = mutableListOf<StudentSummaryV2>()
        db.readableDatabase.rawQuery(
            """SELECT s.Id,s.FullName,COALESCE(s.Grade,''),COALESCE(s.GuardianPhone,''),
               COALESCE((SELECT GROUP_CONCAT(g.Name, ' • ') FROM StudentGroups sg JOIN GroupsTbl g ON g.Id=sg.GroupId WHERE sg.StudentId=s.Id),''),
               COALESCE((SELECT SUM(CASE WHEN ar.Status='حاضر' THEN 1 ELSE 0 END)*100.0/NULLIF(COUNT(*),0) FROM AttendanceRecords ar WHERE ar.StudentId=s.Id),0),
               COALESCE((SELECT SUM(p.AmountPaid) FROM Payments p WHERE p.StudentId=s.Id),0),
               COALESCE((SELECT SUM(p.DueAmount) FROM Payments p WHERE p.StudentId=s.Id AND p.DueAmount>0),0),
               COALESCE((SELECT AVG(CASE WHEN e.MaxScore>0 THEN r.Score*100.0/e.MaxScore END) FROM ExamResults r JOIN Exams e ON e.Id=r.ExamId WHERE r.StudentId=s.Id),0)
               FROM Students s WHERE s.Status='نشط' AND (s.FullName LIKE ? OR s.GuardianPhone LIKE ? OR s.Code LIKE ?) ORDER BY s.FullName""".trimIndent(),
            arrayOf(q, q, q)
        ).use { c ->
            while (c.moveToNext()) list += StudentSummaryV2(c.getLong(0), c.getString(1), c.getString(2), c.getString(3), c.getString(4), c.getDouble(5), c.getDouble(6), c.getDouble(7), c.getDouble(8))
        }
        return list
    }

    fun student360(studentId: Long): Student360? {
        val summary = studentSummaries().firstOrNull { it.id == studentId } ?: return null
        var phone = ""; var status = ""; var notes = ""
        db.readableDatabase.rawQuery("SELECT COALESCE(StudentPhone,''),Status,COALESCE(Notes,'') FROM Students WHERE Id=?", arrayOf(studentId.toString())).use { c ->
            if (c.moveToFirst()) { phone = c.getString(0); status = c.getString(1); notes = c.getString(2) }
        }
        val present = long("SELECT COUNT(*) FROM AttendanceRecords WHERE StudentId=? AND Status='حاضر'", arrayOf(studentId.toString()))
        val absent = long("SELECT COUNT(*) FROM AttendanceRecords WHERE StudentId=? AND Status='غياب'", arrayOf(studentId.toString()))
        val late = long("SELECT COUNT(*) FROM AttendanceRecords WHERE StudentId=? AND Status IN ('متأخر','تأخير')", arrayOf(studentId.toString()))
        val exams = mutableListOf<ExamResultMini>()
        db.readableDatabase.rawQuery(
            "SELECT e.Name,r.Score,e.MaxScore,CASE WHEN e.MaxScore>0 THEN r.Score*100.0/e.MaxScore ELSE 0 END,e.ExamDate FROM ExamResults r JOIN Exams e ON e.Id=r.ExamId WHERE r.StudentId=? ORDER BY e.ExamDate DESC,e.Id DESC LIMIT 8",
            arrayOf(studentId.toString())
        ).use { c -> while (c.moveToNext()) exams += ExamResultMini(c.getString(0), c.getDouble(1), c.getDouble(2), c.getDouble(3), c.getString(4)) }
        val lastPayment = text("SELECT COALESCE(MAX(PaymentDate),'') FROM Payments WHERE StudentId=?", arrayOf(studentId.toString()))
        return Student360(summary, phone, status, notes, present, absent, late, exams, lastPayment)
    }

    fun debtors(): List<DebtorRow> {
        val list = mutableListOf<DebtorRow>()
        db.readableDatabase.rawQuery(
            """SELECT s.Id,s.FullName,COALESCE(s.GuardianPhone,''),SUM(p.DueAmount),COALESCE(MAX(p.PaymentDate),'')
               FROM Payments p JOIN Students s ON s.Id=p.StudentId WHERE p.DueAmount>0
               GROUP BY s.Id,s.FullName,s.GuardianPhone HAVING SUM(p.DueAmount)>0 ORDER BY SUM(p.DueAmount) DESC""".trimIndent(), null
        ).use { c -> while (c.moveToNext()) list += DebtorRow(c.getLong(0), c.getString(1), c.getString(2), c.getDouble(3), c.getString(4)) }
        return list
    }

    fun smartAlerts(): List<SmartAlert> {
        val alerts = mutableListOf<SmartAlert>()
        debtors().take(4).forEach { alerts += SmartAlert("warning", "متأخرات مالية", "${it.name} عليه ${"%.0f".format(Locale.US, it.debt)} ج.م", it.studentId) }
        db.readableDatabase.rawQuery(
            """SELECT s.Id,s.FullName,COUNT(*) c FROM AttendanceRecords ar JOIN AttendanceSessions ses ON ses.Id=ar.SessionId JOIN Students s ON s.Id=ar.StudentId
               WHERE ar.Status='غياب' AND ses.SessionDate>=date('now','-30 day') GROUP BY s.Id,s.FullName HAVING c>=3 ORDER BY c DESC LIMIT 4""".trimIndent(), null
        ).use { c -> while (c.moveToNext()) alerts += SmartAlert("danger", "غياب متكرر", "${c.getString(1)} غاب ${c.getLong(2)} مرات خلال آخر 30 يوم", c.getLong(0)) }
        db.readableDatabase.rawQuery(
            """SELECT s.Id,s.FullName,AVG(r.Score*100.0/e.MaxScore) avgp FROM ExamResults r JOIN Exams e ON e.Id=r.ExamId JOIN Students s ON s.Id=r.StudentId
               WHERE e.MaxScore>0 GROUP BY s.Id,s.FullName HAVING COUNT(*)>=2 AND avgp<60 ORDER BY avgp LIMIT 3""".trimIndent(), null
        ).use { c -> while (c.moveToNext()) alerts += SmartAlert("info", "يحتاج متابعة أكاديمية", "متوسط ${c.getString(1)} هو ${"%.0f".format(Locale.US, c.getDouble(2))}%", c.getLong(0)) }
        if (todayLessons().isNotEmpty()) alerts.add(0, SmartAlert("success", "حصص اليوم", "لديك ${todayLessons().size} حصة مجدولة اليوم"))
        return alerts.take(10)
    }

    fun trend(): List<TrendPoint> {
        val cal = Calendar.getInstance()
        val out = mutableListOf<TrendPoint>()
        repeat(6) {
            val month = SimpleDateFormat("yyyy-MM", Locale.US).format(cal.time)
            out += TrendPoint(
                month,
                double("SELECT COALESCE(SUM(AmountPaid),0) FROM Payments WHERE substr(PaymentDate,1,7)=?", arrayOf(month)),
                double("SELECT COALESCE(SUM(Amount),0) FROM Expenses WHERE substr(ExpenseDate,1,7)=?", arrayOf(month))
            )
            cal.add(Calendar.MONTH, -1)
        }
        return out.reversed()
    }

    fun quickPayment(studentId: Long, paid: Double, due: Double, method: String, notes: String) {
        db.addPayment(studentId, null, paid, due, method, "V2-${System.currentTimeMillis()}", notes)
    }
}
