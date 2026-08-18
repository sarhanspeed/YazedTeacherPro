package com.sarhansoftware.yazedteacherpro.professional

import android.widget.Toast
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.sarhansoftware.yazedteacherpro.data.*

@Composable
fun ProfessionalRoot(db: YazedTeacherProDb) {
    val accounting = remember(db) { AccountingManager(db).also { it.install() } }
    var askLogin by remember { mutableStateOf(false) }
    var showManager by remember { mutableStateOf(false) }

    MaterialTheme {
        Box(Modifier.fillMaxSize()) {
            ProfessionalApp(db)
            if (db.isUnlocked()) {
                ExtendedFloatingActionButton(
                    onClick = { askLogin = true },
                    icon = { Icon(Icons.Default.Tune, null) },
                    text = { Text("تعديل") },
                    modifier = Modifier.align(Alignment.BottomEnd).padding(end = 14.dp, bottom = 88.dp)
                )
            }
        }
    }

    if (askLogin) {
        ManagerLoginDialog(
            db = db,
            onDismiss = { askLogin = false },
            onSuccess = { askLogin = false; showManager = true }
        )
    }
    if (showManager) {
        ManagementDialog(db, accounting) { showManager = false }
    }
}

@Composable
private fun ManagerLoginDialog(db: YazedTeacherProDb, onDismiss: () -> Unit, onSuccess: () -> Unit) {
    val context = LocalContext.current
    var user by remember { mutableStateOf("") }
    var pass by remember { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("الدخول إلى مركز التعديل", fontWeight = FontWeight.Black) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("استخدم بيانات دخول البرنامج لحماية التعديلات الحساسة.", style = MaterialTheme.typography.bodySmall)
                OutlinedTextField(user, { user = it }, label = { Text("اسم المستخدم") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(pass, { pass = it }, label = { Text("كلمة المرور") }, singleLine = true, visualTransformation = PasswordVisualTransformation(), modifier = Modifier.fillMaxWidth())
            }
        },
        confirmButton = {
            Button(onClick = {
                if (db.login(user, pass)) onSuccess()
                else Toast.makeText(context, "بيانات الدخول غير صحيحة", Toast.LENGTH_SHORT).show()
            }) { Text("دخول") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("إلغاء") } }
    )
}

private enum class ManageTab(val title: String) {
    Payments("المدفوعات"), Groups("المجموعات"), Centers("السناتر"), Expenses("المصروفات"), Exams("الامتحانات")
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ManagementDialog(db: YazedTeacherProDb, accounting: AccountingManager, onClose: () -> Unit) {
    var tab by remember { mutableStateOf(ManageTab.Payments) }
    Dialog(onDismissRequest = onClose, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Surface(Modifier.fillMaxSize()) {
            Column(Modifier.fillMaxSize()) {
                TopAppBar(
                    title = {
                        Column {
                            Text("مركز التعديل", fontWeight = FontWeight.Black)
                            Text("تعديل البيانات بدون حذف التاريخ", style = MaterialTheme.typography.labelSmall)
                        }
                    },
                    navigationIcon = { IconButton(onClick = onClose) { Icon(Icons.Default.Close, "إغلاق") } }
                )
                ScrollableTabRow(selectedTabIndex = tab.ordinal, edgePadding = 8.dp) {
                    ManageTab.entries.forEach { item ->
                        Tab(selected = tab == item, onClick = { tab = item }, text = { Text(item.title) })
                    }
                }
                Box(Modifier.weight(1f).fillMaxWidth()) {
                    when (tab) {
                        ManageTab.Payments -> PaymentsManager(db, accounting)
                        ManageTab.Groups -> GroupsManager(db, accounting)
                        ManageTab.Centers -> CentersManager(db, accounting)
                        ManageTab.Expenses -> ExpensesManager(db, accounting)
                        ManageTab.Exams -> ExamsManager(db, accounting)
                    }
                }
            }
        }
    }
}

@Composable
private fun PaymentsManager(db: YazedTeacherProDb, accounting: AccountingManager) {
    var refresh by remember { mutableIntStateOf(0) }
    var edit by remember { mutableStateOf<ManagedPayment?>(null) }
    var delete by remember { mutableStateOf<ManagedPayment?>(null) }
    val data = remember(refresh) { accounting.managedPayments() }
    Column(Modifier.fillMaxSize().padding(12.dp)) {
        Text("سجل المدفوعات", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Black)
        Text("المتأخر الحالي = ${accounting.debtSummary()} ج.م • أحدث دفعة فقط تحمل الرصيد الحالي.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(Modifier.height(8.dp))
        LazyColumn(Modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            items(data, key = { it.id }) { p ->
                Card(Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(12.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Column(Modifier.weight(1f)) {
                                Text(p.student, fontWeight = FontWeight.Bold)
                                Text((p.group.ifBlank { "بدون مجموعة" }) + " • " + p.date, style = MaterialTheme.typography.bodySmall)
                            }
                            IconButton(onClick = { edit = p }) { Icon(Icons.Default.Edit, "تعديل") }
                            IconButton(onClick = { delete = p }) { Icon(Icons.Default.Delete, "حذف", tint = MaterialTheme.colorScheme.error) }
                        }
                        Text("مدفوع: ${p.paid} ج.م • المتبقي وقت التسجيل: ${p.remainingAtEntry} ج.م")
                        if (p.currentRemaining > 0) Text("المتأخر الحالي: ${p.currentRemaining} ج.م", color = MaterialTheme.colorScheme.error, fontWeight = FontWeight.Bold)
                        if (p.notes.isNotBlank()) Text(p.notes, style = MaterialTheme.typography.bodySmall)
                    }
                }
            }
        }
    }
    edit?.let { row ->
        PaymentEditDialog(db, row, onDismiss = { edit = null }) { studentId, groupId, paid, due, date, method, receipt, notes ->
            accounting.updatePayment(row.id, studentId, groupId, paid, due, date, method, receipt, notes)
            edit = null; refresh++
        }
    }
    delete?.let { row ->
        ConfirmDelete("حذف الدفعة", "سيتم حذف دفعة ${row.paid} ج.م للطالب ${row.student}. سيتم إعادة احتساب المتأخرات تلقائيًا.", { delete = null }) {
            accounting.deletePayment(row.id); delete = null; refresh++
        }
    }
}

@Composable
private fun PaymentEditDialog(
    db: YazedTeacherProDb,
    row: ManagedPayment,
    onDismiss: () -> Unit,
    onSave: (Long, Long?, Double, Double, String, String, String, String) -> Unit,
) {
    val students = remember { db.activeStudents() }
    val groups = remember { db.groups() }
    var studentId by remember { mutableLongStateOf(row.studentId) }
    var groupId by remember { mutableStateOf(row.groupId) }
    var studentMenu by remember { mutableStateOf(false) }
    var groupMenu by remember { mutableStateOf(false) }
    var paid by remember { mutableStateOf(row.paid.toString()) }
    var due by remember { mutableStateOf(row.remainingAtEntry.toString()) }
    var date by remember { mutableStateOf(row.date) }
    var method by remember { mutableStateOf(row.method) }
    var receipt by remember { mutableStateOf(row.receipt) }
    var notes by remember { mutableStateOf(row.notes) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("تعديل الدفعة", fontWeight = FontWeight.Black) },
        text = {
            LazyColumn(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(7.dp)) {
                item {
                    SelectorField("الطالب", students.firstOrNull { it.id == studentId }?.name ?: row.student, studentMenu, { studentMenu = !studentMenu }, { studentMenu = false }) {
                        students.forEach { s -> DropdownMenuItem(text = { Text(s.name) }, onClick = { studentId = s.id; studentMenu = false }) }
                    }
                }
                item {
                    SelectorField("المجموعة", groups.firstOrNull { it.id == groupId }?.name ?: "بدون مجموعة", groupMenu, { groupMenu = !groupMenu }, { groupMenu = false }) {
                        DropdownMenuItem(text = { Text("بدون مجموعة") }, onClick = { groupId = null; groupMenu = false })
                        groups.forEach { g -> DropdownMenuItem(text = { Text(g.name) }, onClick = { groupId = g.id; groupMenu = false }) }
                    }
                }
                item { NumberField("المبلغ المدفوع", paid) { paid = it } }
                item { NumberField("المتبقي بعد الدفعة", due) { due = it } }
                item { TextFieldLine("التاريخ YYYY-MM-DD", date) { date = it } }
                item { TextFieldLine("طريقة الدفع", method) { method = it } }
                item { TextFieldLine("رقم الإيصال", receipt) { receipt = it } }
                item { TextFieldLine("ملاحظات", notes) { notes = it } }
            }
        },
        confirmButton = { Button(onClick = { onSave(studentId, groupId, paid.toDoubleOrNull() ?: 0.0, due.toDoubleOrNull() ?: 0.0, date, method, receipt, notes) }) { Text("حفظ") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("إلغاء") } }
    )
}

@Composable
private fun GroupsManager(db: YazedTeacherProDb, accounting: AccountingManager) {
    var refresh by remember { mutableIntStateOf(0) }
    var edit by remember { mutableStateOf<GroupInfo?>(null) }
    var disable by remember { mutableStateOf<GroupInfo?>(null) }
    val data = remember(refresh) { db.groups() }
    LazyColumn(Modifier.fillMaxSize().padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        item { Text("تعديل المجموعات والأسعار والنسب والسنتر", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Black) }
        items(data, key = { it.id }) { g ->
            Card(Modifier.fillMaxWidth()) {
                Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text(g.name, fontWeight = FontWeight.Bold)
                        Text("${g.subject} • ${g.grade} • ${g.day} ${g.time}", style = MaterialTheme.typography.bodySmall)
                        Text("${g.monthlyPrice} ج.م • نسبة السنتر ${g.sharePercent}% • ${g.centerName.ifBlank { "بدون سنتر" }}", style = MaterialTheme.typography.bodySmall)
                    }
                    IconButton(onClick = { edit = g }) { Icon(Icons.Default.Edit, "تعديل") }
                    IconButton(onClick = { disable = g }) { Icon(Icons.Default.Block, "تعطيل", tint = MaterialTheme.colorScheme.error) }
                }
            }
        }
    }
    edit?.let { g -> GroupEditDialog(db, g, { edit = null }) { accounting.updateGroup(it); edit = null; refresh++ } }
    disable?.let { g -> ConfirmDelete("تعطيل المجموعة", "سيتم إخفاء ${g.name} من المجموعات النشطة مع الاحتفاظ بالحضور والمدفوعات القديمة.", { disable = null }) { accounting.deactivateGroup(g.id); disable = null; refresh++ } }
}

@Composable
private fun GroupEditDialog(db: YazedTeacherProDb, g: GroupInfo, onDismiss: () -> Unit, onSave: (GroupInfo) -> Unit) {
    val centers = remember { db.centers() }
    var name by remember { mutableStateOf(g.name) }; var subject by remember { mutableStateOf(g.subject) }; var grade by remember { mutableStateOf(g.grade) }
    var day by remember { mutableStateOf(g.day) }; var time by remember { mutableStateOf(g.time) }; var price by remember { mutableStateOf(g.monthlyPrice.toString()) }
    var percent by remember { mutableStateOf(g.sharePercent.toString()) }; var centerId by remember { mutableStateOf(g.centerId) }; var menu by remember { mutableStateOf(false) }
    AlertDialog(onDismissRequest = onDismiss, title = { Text("تعديل المجموعة", fontWeight = FontWeight.Black) }, text = {
        LazyColumn(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            item { TextFieldLine("اسم المجموعة", name) { name = it } }; item { TextFieldLine("المادة", subject) { subject = it } }; item { TextFieldLine("الصف", grade) { grade = it } }
            item { TextFieldLine("اليوم", day) { day = it } }; item { TextFieldLine("الوقت", time) { time = it } }; item { NumberField("السعر الشهري", price) { price = it } }; item { NumberField("نسبة السنتر %", percent) { percent = it } }
            item { SelectorField("السنتر", centers.firstOrNull { it.id == centerId }?.name ?: "بدون سنتر", menu, { menu = !menu }, { menu = false }) {
                DropdownMenuItem(text = { Text("بدون سنتر") }, onClick = { centerId = null; menu = false }); centers.forEach { c -> DropdownMenuItem(text = { Text(c.name) }, onClick = { centerId = c.id; menu = false }) }
            } }
        }
    }, confirmButton = { Button(onClick = { onSave(g.copy(name = name, subject = subject, grade = grade, day = day, time = time, monthlyPrice = price.toDoubleOrNull() ?: 0.0, centerId = centerId, centerName = centers.firstOrNull { it.id == centerId }?.name ?: "", sharePercent = percent.toDoubleOrNull() ?: 0.0)) }) { Text("حفظ") } }, dismissButton = { TextButton(onClick = onDismiss) { Text("إلغاء") } })
}

@Composable
private fun CentersManager(db: YazedTeacherProDb, accounting: AccountingManager) {
    var refresh by remember { mutableIntStateOf(0) }; var edit by remember { mutableStateOf<Center?>(null) }; var delete by remember { mutableStateOf<Center?>(null) }
    val data = remember(refresh) { db.centers() }
    LazyColumn(Modifier.fillMaxSize().padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        item { Text("تعديل السناتر والنسب", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Black) }
        items(data, key = { it.id }) { c -> Card(Modifier.fillMaxWidth()) { Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) { Text(c.name, fontWeight = FontWeight.Bold); Text("نسبة السنتر ${c.sharePercent}% • ${c.phone}", style = MaterialTheme.typography.bodySmall); if (c.address.isNotBlank()) Text(c.address, style = MaterialTheme.typography.bodySmall) }
            IconButton(onClick = { edit = c }) { Icon(Icons.Default.Edit, "تعديل") }; IconButton(onClick = { delete = c }) { Icon(Icons.Default.Delete, "حذف", tint = MaterialTheme.colorScheme.error) }
        } } }
    }
    edit?.let { c -> CenterEditDialog(c, { edit = null }) { updated, apply -> accounting.updateCenter(updated, apply); edit = null; refresh++ } }
    delete?.let { c -> ConfirmDelete("حذف السنتر", "سيتم حذف ${c.name} وفصل المجموعات عنه بدون حذف المجموعات أو تاريخها.", { delete = null }) { accounting.deleteCenter(c.id); delete = null; refresh++ } }
}

@Composable
private fun CenterEditDialog(c: Center, onDismiss: () -> Unit, onSave: (Center, Boolean) -> Unit) {
    var name by remember { mutableStateOf(c.name) }; var address by remember { mutableStateOf(c.address) }; var phone by remember { mutableStateOf(c.phone) }; var percent by remember { mutableStateOf(c.sharePercent.toString()) }; var apply by remember { mutableStateOf(false) }
    AlertDialog(onDismissRequest = onDismiss, title = { Text("تعديل السنتر", fontWeight = FontWeight.Black) }, text = { Column(verticalArrangement = Arrangement.spacedBy(7.dp)) {
        TextFieldLine("اسم السنتر", name) { name = it }; TextFieldLine("العنوان", address) { address = it }; TextFieldLine("الهاتف", phone) { phone = it }; NumberField("نسبة السنتر %", percent) { percent = it }
        Row(verticalAlignment = Alignment.CenterVertically) { Checkbox(apply, { apply = it }); Text("تطبيق النسبة الجديدة على كل مجموعات هذا السنتر") }
    } }, confirmButton = { Button(onClick = { onSave(c.copy(name = name, address = address, phone = phone, sharePercent = percent.toDoubleOrNull() ?: 0.0), apply) }) { Text("حفظ") } }, dismissButton = { TextButton(onClick = onDismiss) { Text("إلغاء") } })
}

@Composable
private fun ExpensesManager(db: YazedTeacherProDb, accounting: AccountingManager) {
    var refresh by remember { mutableIntStateOf(0) }; var edit by remember { mutableStateOf<ExpenseRow?>(null) }; var delete by remember { mutableStateOf<ExpenseRow?>(null) }
    val data = remember(refresh) { db.expenses() }
    LazyColumn(Modifier.fillMaxSize().padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        item { Text("تعديل المصروفات", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Black) }
        items(data, key = { it.id }) { e -> Card { Row(Modifier.fillMaxWidth().padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) { Text(e.category, fontWeight = FontWeight.Bold); Text("${e.amount} ج.م • ${e.date}", style = MaterialTheme.typography.bodySmall); if (e.notes.isNotBlank()) Text(e.notes, style = MaterialTheme.typography.bodySmall) }
            IconButton(onClick = { edit = e }) { Icon(Icons.Default.Edit, "تعديل") }; IconButton(onClick = { delete = e }) { Icon(Icons.Default.Delete, "حذف", tint = MaterialTheme.colorScheme.error) }
        } } }
    }
    edit?.let { e -> ExpenseEditDialog(e, { edit = null }) { accounting.updateExpense(it); edit = null; refresh++ } }
    delete?.let { e -> ConfirmDelete("حذف المصروف", "حذف ${e.category} بقيمة ${e.amount} ج.م؟", { delete = null }) { accounting.deleteExpense(e.id); delete = null; refresh++ } }
}

@Composable
private fun ExpenseEditDialog(e: ExpenseRow, onDismiss: () -> Unit, onSave: (ExpenseRow) -> Unit) {
    var date by remember { mutableStateOf(e.date) }; var category by remember { mutableStateOf(e.category) }; var amount by remember { mutableStateOf(e.amount.toString()) }; var notes by remember { mutableStateOf(e.notes) }
    AlertDialog(onDismissRequest = onDismiss, title = { Text("تعديل المصروف") }, text = { Column(verticalArrangement = Arrangement.spacedBy(7.dp)) { TextFieldLine("التاريخ", date) { date = it }; TextFieldLine("التصنيف", category) { category = it }; NumberField("المبلغ", amount) { amount = it }; TextFieldLine("ملاحظات", notes) { notes = it } } }, confirmButton = { Button(onClick = { onSave(e.copy(date = date, category = category, amount = amount.toDoubleOrNull() ?: 0.0, notes = notes)) }) { Text("حفظ") } }, dismissButton = { TextButton(onClick = onDismiss) { Text("إلغاء") } })
}

@Composable
private fun ExamsManager(db: YazedTeacherProDb, accounting: AccountingManager) {
    var refresh by remember { mutableIntStateOf(0) }; var edit by remember { mutableStateOf<ExamRow?>(null) }; var delete by remember { mutableStateOf<ExamRow?>(null) }
    val data = remember(refresh) { db.exams() }
    LazyColumn(Modifier.fillMaxSize().padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        item { Text("تعديل الامتحانات", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Black) }
        items(data, key = { it.id }) { e -> Card { Row(Modifier.fillMaxWidth().padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) { Text(e.name, fontWeight = FontWeight.Bold); Text("${e.group.ifBlank { "بدون مجموعة" }} • من ${e.maxScore} • ${e.date}", style = MaterialTheme.typography.bodySmall) }
            IconButton(onClick = { edit = e }) { Icon(Icons.Default.Edit, "تعديل") }; IconButton(onClick = { delete = e }) { Icon(Icons.Default.Delete, "حذف", tint = MaterialTheme.colorScheme.error) }
        } } }
    }
    edit?.let { e -> ExamEditDialog(db, e, { edit = null }) { accounting.updateExam(it); edit = null; refresh++ } }
    delete?.let { e -> ConfirmDelete("حذف الامتحان", "سيتم حذف ${e.name} وكل درجاته المسجلة. هل أنت متأكد؟", { delete = null }) { accounting.deleteExam(e.id); delete = null; refresh++ } }
}

@Composable
private fun ExamEditDialog(db: YazedTeacherProDb, e: ExamRow, onDismiss: () -> Unit, onSave: (ExamRow) -> Unit) {
    val groups = remember { db.groups() }; var name by remember { mutableStateOf(e.name) }; var groupId by remember { mutableStateOf(e.groupId) }; var maxScore by remember { mutableStateOf(e.maxScore.toString()) }; var date by remember { mutableStateOf(e.date) }; var menu by remember { mutableStateOf(false) }
    AlertDialog(onDismissRequest = onDismiss, title = { Text("تعديل الامتحان") }, text = { Column(verticalArrangement = Arrangement.spacedBy(7.dp)) {
        TextFieldLine("اسم الامتحان", name) { name = it }; SelectorField("المجموعة", groups.firstOrNull { it.id == groupId }?.name ?: "بدون مجموعة", menu, { menu = !menu }, { menu = false }) { DropdownMenuItem(text = { Text("بدون مجموعة") }, onClick = { groupId = null; menu = false }); groups.forEach { g -> DropdownMenuItem(text = { Text(g.name) }, onClick = { groupId = g.id; menu = false }) } }; NumberField("النهاية العظمى", maxScore) { maxScore = it }; TextFieldLine("التاريخ", date) { date = it }
    } }, confirmButton = { Button(onClick = { onSave(e.copy(name = name, groupId = groupId, group = groups.firstOrNull { it.id == groupId }?.name ?: "", maxScore = maxScore.toDoubleOrNull() ?: 0.0, date = date)) }) { Text("حفظ") } }, dismissButton = { TextButton(onClick = onDismiss) { Text("إلغاء") } })
}

@Composable
private fun SelectorField(label: String, value: String, expanded: Boolean, onToggle: () -> Unit, onDismiss: () -> Unit, menuContent: @Composable ColumnScope.() -> Unit) {
    Box(Modifier.fillMaxWidth()) {
        OutlinedButton(onClick = onToggle, modifier = Modifier.fillMaxWidth()) {
            Column(Modifier.weight(1f), horizontalAlignment = Alignment.Start) { Text(label, style = MaterialTheme.typography.labelSmall); Text(value, fontWeight = FontWeight.Bold) }
            Icon(Icons.Default.ArrowDropDown, null)
        }
        DropdownMenu(expanded = expanded, onDismissRequest = onDismiss, modifier = Modifier.fillMaxWidth(.88f), content = menuContent)
    }
}

@Composable
private fun TextFieldLine(label: String, value: String, onValue: (String) -> Unit) {
    OutlinedTextField(value, onValue, label = { Text(label) }, modifier = Modifier.fillMaxWidth(), singleLine = true)
}

@Composable
private fun NumberField(label: String, value: String, onValue: (String) -> Unit) {
    OutlinedTextField(value, onValue, label = { Text(label) }, modifier = Modifier.fillMaxWidth(), singleLine = true, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal))
}

@Composable
private fun ConfirmDelete(title: String, text: String, onDismiss: () -> Unit, onConfirm: () -> Unit) {
    AlertDialog(onDismissRequest = onDismiss, title = { Text(title, fontWeight = FontWeight.Black) }, text = { Text(text) }, confirmButton = { Button(onClick = onConfirm, colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)) { Text("تأكيد") } }, dismissButton = { TextButton(onClick = onDismiss) { Text("إلغاء") } })
}
