package com.sarhansoftware.yazedteacherpro.professional

import com.sarhansoftware.yazedteacherpro.data.Center
import com.sarhansoftware.yazedteacherpro.data.ExpenseRow
import com.sarhansoftware.yazedteacherpro.data.ExamRow
import com.sarhansoftware.yazedteacherpro.data.GroupInfo
import com.sarhansoftware.yazedteacherpro.data.YazedTeacherProDb
import java.util.Locale

data class ManagedPayment(
    val id: Long,
    val studentId: Long,
    val student: String,
    val groupId: Long?,
    val group: String,
    val paid: Double,
    val remainingAtEntry: Double,
    val currentRemaining: Double,
    val date: String,
    val method: String,
    val receipt: String,
    val notes: String,
)

class AccountingManager(private val db: YazedTeacherProDb) {

    fun install() {
        val sqlDb = db.writableDatabase
        sqlDb.execSQL(
            """CREATE TABLE IF NOT EXISTS PaymentDueHistory(
                PaymentId INTEGER PRIMARY KEY,
                OriginalDue REAL NOT NULL DEFAULT 0
            )""".trimIndent()
        )
        sqlDb.execSQL(
            "INSERT OR IGNORE INTO PaymentDueHistory(PaymentId,OriginalDue) SELECT Id,MAX(0,DueAmount) FROM Payments"
        )

        // Only the latest transaction for each student/group account carries the current debt.
        // Older remaining values stay preserved in PaymentDueHistory for editing/audit purposes.
        sqlDb.execSQL(
            """UPDATE Payments SET DueAmount=0
               WHERE Id NOT IN (
                   SELECT MAX(Id) FROM Payments GROUP BY StudentId,IFNULL(GroupId,-1)
               )""".trimIndent()
        )

        sqlDb.execSQL("DROP TRIGGER IF EXISTS TRG_YTP_PaymentBalance_Insert")
        sqlDb.execSQL(
            """CREATE TRIGGER TRG_YTP_PaymentBalance_Insert
               AFTER INSERT ON Payments
               BEGIN
                   UPDATE Payments
                   SET DueAmount = CASE
                       WHEN NEW.DueAmount > 0 THEN NEW.DueAmount
                       ELSE MAX(
                           0,
                           COALESCE((
                               SELECT SUM(DueAmount)
                               FROM Payments
                               WHERE StudentId=NEW.StudentId
                                 AND IFNULL(GroupId,-1)=IFNULL(NEW.GroupId,-1)
                                 AND Id<>NEW.Id
                                 AND DueAmount>0
                           ),0) - NEW.AmountPaid
                       )
                   END
                   WHERE Id=NEW.Id;

                   INSERT OR REPLACE INTO PaymentDueHistory(PaymentId,OriginalDue)
                   SELECT NEW.Id,MAX(0,DueAmount) FROM Payments WHERE Id=NEW.Id;

                   UPDATE Payments
                   SET DueAmount=0
                   WHERE StudentId=NEW.StudentId
                     AND IFNULL(GroupId,-1)=IFNULL(NEW.GroupId,-1)
                     AND Id<>NEW.Id;
               END""".trimIndent()
        )
    }

    fun currentDebt(studentId: Long, groupId: Long? = null): Double {
        val whereGroup = if (groupId == null) "" else " AND IFNULL(GroupId,-1)=?"
        val args = if (groupId == null) arrayOf(studentId.toString()) else arrayOf(studentId.toString(), groupId.toString())
        return db.readableDatabase.rawQuery(
            "SELECT COALESCE(SUM(DueAmount),0) FROM Payments WHERE StudentId=? AND DueAmount>0$whereGroup",
            args
        ).use { c -> if (c.moveToFirst()) c.getDouble(0) else 0.0 }
    }

    fun managedPayments(): List<ManagedPayment> = db.readableDatabase.rawQuery(
        """SELECT p.Id,p.StudentId,s.FullName,p.GroupId,COALESCE(g.Name,''),p.AmountPaid,
           COALESCE(h.OriginalDue,p.DueAmount),p.DueAmount,p.PaymentDate,
           COALESCE(p.Method,''),COALESCE(p.ReceiptNo,''),COALESCE(p.Notes,'')
           FROM Payments p
           JOIN Students s ON s.Id=p.StudentId
           LEFT JOIN GroupsTbl g ON g.Id=p.GroupId
           LEFT JOIN PaymentDueHistory h ON h.PaymentId=p.Id
           ORDER BY p.Id DESC""".trimIndent(), null
    ).use { c ->
        buildList {
            while (c.moveToNext()) add(
                ManagedPayment(
                    c.getLong(0), c.getLong(1), c.getString(2),
                    if (c.isNull(3)) null else c.getLong(3), c.getString(4),
                    c.getDouble(5), c.getDouble(6), c.getDouble(7), c.getString(8),
                    c.getString(9), c.getString(10), c.getString(11)
                )
            )
        }
    }

    fun updatePayment(
        paymentId: Long,
        studentId: Long,
        groupId: Long?,
        paid: Double,
        remainingAfterPayment: Double,
        date: String,
        method: String,
        receipt: String,
        notes: String,
    ) {
        val old = db.readableDatabase.rawQuery(
            "SELECT StudentId,GroupId FROM Payments WHERE Id=?",
            arrayOf(paymentId.toString())
        ).use { c ->
            if (!c.moveToFirst()) return
            c.getLong(0) to if (c.isNull(1)) null else c.getLong(1)
        }
        val w = db.writableDatabase
        w.beginTransaction()
        try {
            w.execSQL(
                """UPDATE Payments SET StudentId=?,GroupId=?,AmountPaid=?,DueAmount=?,PaymentDate=?,Method=?,ReceiptNo=?,Notes=? WHERE Id=?""",
                arrayOf<Any?>(studentId, groupId, paid.coerceAtLeast(0.0), remainingAfterPayment.coerceAtLeast(0.0), date.trim(), method.trim(), receipt.trim(), notes.trim(), paymentId)
            )
            w.execSQL(
                "INSERT OR REPLACE INTO PaymentDueHistory(PaymentId,OriginalDue) VALUES(?,?)",
                arrayOf<Any?>(paymentId, remainingAfterPayment.coerceAtLeast(0.0))
            )
            normalizeAccount(old.first, old.second)
            normalizeAccount(studentId, groupId)
            w.setTransactionSuccessful()
        } finally {
            w.endTransaction()
        }
    }

    fun deletePayment(paymentId: Long) {
        val old = db.readableDatabase.rawQuery(
            "SELECT StudentId,GroupId FROM Payments WHERE Id=?",
            arrayOf(paymentId.toString())
        ).use { c ->
            if (!c.moveToFirst()) return
            c.getLong(0) to if (c.isNull(1)) null else c.getLong(1)
        }
        val w = db.writableDatabase
        w.beginTransaction()
        try {
            w.delete("Payments", "Id=?", arrayOf(paymentId.toString()))
            w.delete("PaymentDueHistory", "PaymentId=?", arrayOf(paymentId.toString()))
            normalizeAccount(old.first, old.second)
            w.setTransactionSuccessful()
        } finally {
            w.endTransaction()
        }
    }

    private fun normalizeAccount(studentId: Long, groupId: Long?) {
        val w = db.writableDatabase
        val condition = if (groupId == null) "StudentId=? AND GroupId IS NULL" else "StudentId=? AND GroupId=?"
        val args = if (groupId == null) arrayOf(studentId.toString()) else arrayOf(studentId.toString(), groupId.toString())
        w.execSQL("UPDATE Payments SET DueAmount=0 WHERE $condition", args)
        val latest = db.readableDatabase.rawQuery(
            "SELECT Id FROM Payments WHERE $condition ORDER BY Id DESC LIMIT 1",
            args
        ).use { c -> if (c.moveToFirst()) c.getLong(0) else null }
        if (latest != null) {
            val due = db.readableDatabase.rawQuery(
                "SELECT COALESCE(OriginalDue,0) FROM PaymentDueHistory WHERE PaymentId=?",
                arrayOf(latest.toString())
            ).use { c -> if (c.moveToFirst()) c.getDouble(0) else 0.0 }
            w.execSQL("UPDATE Payments SET DueAmount=? WHERE Id=?", arrayOf<Any?>(due.coerceAtLeast(0.0), latest))
        }
    }

    fun updateCenter(center: Center, applyPercentToGroups: Boolean) {
        val w = db.writableDatabase
        w.execSQL(
            "UPDATE Centers SET Name=?,Address=?,Phone=?,CenterSharePercent=? WHERE Id=?",
            arrayOf<Any?>(center.name.trim(), center.address.trim(), center.phone.trim(), center.sharePercent.coerceIn(0.0, 100.0), center.id)
        )
        if (applyPercentToGroups) {
            w.execSQL(
                "UPDATE GroupsTbl SET CenterSharePercent=? WHERE CenterId=?",
                arrayOf<Any?>(center.sharePercent.coerceIn(0.0, 100.0), center.id)
            )
        }
    }

    fun deleteCenter(centerId: Long) {
        val w = db.writableDatabase
        w.beginTransaction()
        try {
            w.execSQL("UPDATE GroupsTbl SET CenterId=NULL WHERE CenterId=?", arrayOf<Any?>(centerId))
            w.delete("Centers", "Id=?", arrayOf(centerId.toString()))
            w.setTransactionSuccessful()
        } finally { w.endTransaction() }
    }

    fun updateGroup(group: GroupInfo) {
        db.writableDatabase.execSQL(
            """UPDATE GroupsTbl SET Name=?,Subject=?,Grade=?,DayOfWeek=?,StartTime=?,MonthlyPrice=?,CenterId=?,CenterSharePercent=? WHERE Id=?""",
            arrayOf<Any?>(
                group.name.trim(), group.subject.trim(), group.grade.trim(), group.day.trim(), group.time.trim(),
                group.monthlyPrice.coerceAtLeast(0.0), group.centerId, group.sharePercent.coerceIn(0.0, 100.0), group.id
            )
        )
    }

    fun deactivateGroup(groupId: Long) {
        db.writableDatabase.execSQL("UPDATE GroupsTbl SET IsActive=0 WHERE Id=?", arrayOf<Any?>(groupId))
    }

    fun updateExpense(row: ExpenseRow) {
        db.writableDatabase.execSQL(
            "UPDATE Expenses SET ExpenseDate=?,Category=?,Amount=?,Notes=? WHERE Id=?",
            arrayOf<Any?>(row.date.trim(), row.category.trim(), row.amount.coerceAtLeast(0.0), row.notes.trim(), row.id)
        )
    }

    fun deleteExpense(id: Long) {
        db.writableDatabase.delete("Expenses", "Id=?", arrayOf(id.toString()))
    }

    fun updateExam(row: ExamRow) {
        db.writableDatabase.execSQL(
            "UPDATE Exams SET Name=?,GroupId=?,MaxScore=?,ExamDate=? WHERE Id=?",
            arrayOf<Any?>(row.name.trim(), row.groupId, row.maxScore.coerceAtLeast(0.0), row.date.trim(), row.id)
        )
    }

    fun deleteExam(id: Long) {
        val w = db.writableDatabase
        w.beginTransaction()
        try {
            w.delete("ExamResults", "ExamId=?", arrayOf(id.toString()))
            w.delete("Exams", "Id=?", arrayOf(id.toString()))
            w.setTransactionSuccessful()
        } finally { w.endTransaction() }
    }

    fun debtSummary(): String {
        val total = db.readableDatabase.rawQuery("SELECT COALESCE(SUM(DueAmount),0) FROM Payments WHERE DueAmount>0", null)
            .use { c -> if (c.moveToFirst()) c.getDouble(0) else 0.0 }
        return "%.2f".format(Locale.US, total)
    }
}
