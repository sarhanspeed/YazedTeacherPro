package com.sarhansoftware.yazedteacherpro.professional

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.platform.LocalLayoutDirection
import com.sarhansoftware.yazedteacherpro.data.YazedTeacherProDb
import com.sarhansoftware.yazedteacherpro.ui.MainShell
import com.sarhansoftware.yazedteacherpro.ui.YazedTeacherProApp
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.max

private val ProBlue = Color(0xFF4668F2)
private val ProGreen = Color(0xFF179C74)
private val ProOrange = Color(0xFFF29C38)
private val ProRed = Color(0xFFD94B4B)
private val ProSoft = Color(0xFFF5F7FB)
private val ProDark = Color(0xFF111827)

private fun money2(v: Double) = "%,.0f ج.م".format(Locale.US, v)
private fun percent(v: Double) = "%.0f%%".format(Locale.US, v)
private fun todayLabel() = SimpleDateFormat("EEEE، d MMMM", Locale("ar", "EG")).format(Date())

private enum class ProTab(val title: String, val icon: ImageVector) {
    Dashboard("الرئيسية", Icons.Default.Home),
    Today("اليوم", Icons.Default.CalendarMonth),
    Students("الطلبة", Icons.Default.People),
    Finance("المالية", Icons.Default.AccountBalanceWallet),
    Full("المزيد", Icons.Default.Apps),
}

@Composable
fun ProfessionalApp(db: YazedTeacherProDb) {
    if (!db.isUnlocked()) {
        YazedTeacherProApp(db)
        return
    }
    val context = LocalContext.current
    val prefs = remember { context.getSharedPreferences("yazed_teacher_pro_v2", 0) }
    var dark by remember { mutableStateOf(prefs.getBoolean("dark_mode", false)) }
    val scheme = if (dark) darkColorScheme(primary = Color(0xFF91A7FF), secondary = Color(0xFF63D9B4))
    else lightColorScheme(primary = ProBlue, secondary = ProGreen, background = ProSoft, surface = Color.White)

    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
        MaterialTheme(colorScheme = scheme) {
            Surface(Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
                var user by remember { mutableStateOf<String?>(null) }
                if (user == null) {
                    ProLogin(db) { user = it }
                } else {
                    ProfessionalShell(
                        db = db,
                        user = user!!,
                        dark = dark,
                        onToggleDark = {
                            dark = !dark
                            prefs.edit().putBoolean("dark_mode", dark).apply()
                        },
                        onLogout = { user = null }
                    )
                }
            }
        }
    }
}

@Composable
private fun ProLogin(db: YazedTeacherProDb, onLogin: (String) -> Unit) {
    val context = LocalContext.current
    var username by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    Box(Modifier.fillMaxSize().padding(22.dp), contentAlignment = Alignment.Center) {
        Card(
            modifier = Modifier.fillMaxWidth().widthIn(max = 480.dp),
            shape = RoundedCornerShape(30.dp),
            elevation = CardDefaults.cardElevation(10.dp)
        ) {
            Column(Modifier.padding(26.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                Surface(shape = RoundedCornerShape(22.dp), color = ProBlue.copy(alpha = .12f)) {
                    Icon(Icons.Default.School, null, tint = ProBlue, modifier = Modifier.padding(16.dp).size(42.dp))
                }
                Spacer(Modifier.height(12.dp))
                Text("YazedTeacherPro", fontWeight = FontWeight.Black, fontSize = 27.sp)
                Text("إدارة المدرس والسنتر باحتراف", color = MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(Modifier.height(24.dp))
                OutlinedTextField(username, { username = it }, label = { Text("اسم المستخدم") }, leadingIcon = { Icon(Icons.Default.Person, null) }, singleLine = true, modifier = Modifier.fillMaxWidth())
                Spacer(Modifier.height(10.dp))
                OutlinedTextField(password, { password = it }, label = { Text("كلمة المرور") }, leadingIcon = { Icon(Icons.Default.Lock, null) }, visualTransformation = PasswordVisualTransformation(), singleLine = true, modifier = Modifier.fillMaxWidth())
                Spacer(Modifier.height(18.dp))
                Button(
                    onClick = {
                        if (db.login(username, password)) onLogin(username.trim())
                        else Toast.makeText(context, "بيانات الدخول غير صحيحة", Toast.LENGTH_SHORT).show()
                    },
                    modifier = Modifier.fillMaxWidth().height(54.dp),
                    shape = RoundedCornerShape(16.dp)
                ) { Text("تسجيل الدخول", fontWeight = FontWeight.Bold) }
                if (!db.isLicenseValid().first) {
                    Spacer(Modifier.height(12.dp))
                    Text("نسخة تجريبية • متبقي ${db.trialDaysRemaining()} يوم", color = ProOrange, fontWeight = FontWeight.SemiBold)
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ProfessionalShell(
    db: YazedTeacherProDb,
    user: String,
    dark: Boolean,
    onToggleDark: () -> Unit,
    onLogout: () -> Unit,
) {
    val repo = remember(db) { ProfessionalRepository(db) }
    var tab by remember { mutableStateOf(ProTab.Dashboard) }

    if (tab == ProTab.Full) {
        Column(Modifier.fillMaxSize()) {
            Surface(tonalElevation = 3.dp) {
                Row(Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = { tab = ProTab.Dashboard }) { Icon(Icons.Default.ArrowForward, "عودة") }
                    Column(Modifier.weight(1f)) {
                        Text("النظام الكامل", fontWeight = FontWeight.Bold)
                        Text("كل وظائف YazedTeacherPro", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
            Box(Modifier.weight(1f)) { MainShell(db = db, user = user, onLogout = onLogout) }
        }
        return
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("YazedTeacherPro", fontWeight = FontWeight.Black, fontSize = 19.sp)
                        Text(todayLabel(), fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                },
                actions = {
                    IconButton(onClick = onToggleDark) { Icon(if (dark) Icons.Default.LightMode else Icons.Default.DarkMode, "الوضع") }
                    IconButton(onClick = onLogout) { Icon(Icons.Default.Logout, "خروج") }
                }
            )
        },
        bottomBar = {
            NavigationBar {
                ProTab.entries.forEach { item ->
                    NavigationBarItem(
                        selected = tab == item,
                        onClick = { tab = item },
                        icon = { Icon(item.icon, null) },
                        label = { Text(item.title, fontSize = 11.sp) }
                    )
                }
            }
        }
    ) { padding ->
        Box(Modifier.fillMaxSize().padding(padding)) {
            when (tab) {
                ProTab.Dashboard -> ProDashboard(db, repo, openTab = { tab = it })
                ProTab.Today -> TodayScreen(repo) { tab = ProTab.Full }
                ProTab.Students -> Students360Screen(repo)
                ProTab.Finance -> FinanceCenter(db, repo)
                ProTab.Full -> Unit
            }
        }
    }
}

@Composable
private fun ProDashboard(db: YazedTeacherProDb, repo: ProfessionalRepository, openTab: (ProTab) -> Unit) {
    var refresh by remember { mutableIntStateOf(0) }
    val stats = remember(refresh) { repo.dashboard() }
    val alerts = remember(refresh) { repo.smartAlerts() }
    val trend = remember(refresh) { repo.trend() }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(14.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Card(shape = RoundedCornerShape(24.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)) {
                Column(Modifier.padding(18.dp)) {
                    Text("أهلاً بك في ${db.businessName()}", fontWeight = FontWeight.Black, fontSize = 22.sp)
                    Text("ملخص سريع لما يحتاج انتباهك اليوم", color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = .75f))
                    Spacer(Modifier.height(14.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        DashboardQuick("حصص اليوم", Icons.Default.EventAvailable, Modifier.weight(1f)) { openTab(ProTab.Today) }
                        DashboardQuick("الطلبة", Icons.Default.PersonSearch, Modifier.weight(1f)) { openTab(ProTab.Students) }
                        DashboardQuick("المالية", Icons.Default.Payments, Modifier.weight(1f)) { openTab(ProTab.Finance) }
                    }
                }
            }
        }
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                MiniStat("الطلبة", stats.students.toString(), Icons.Default.People, ProBlue, Modifier.weight(1f))
                MiniStat("المجموعات", stats.groups.toString(), Icons.Default.Groups, ProGreen, Modifier.weight(1f))
            }
        }
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                MiniStat("حضور الشهر", percent(stats.attendanceRate), Icons.Default.CheckCircle, ProGreen, Modifier.weight(1f))
                MiniStat("حصص اليوم", stats.lessonsToday.toString(), Icons.Default.CalendarToday, ProOrange, Modifier.weight(1f))
            }
        }
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                MiniStat("دخل الشهر", money2(stats.income), Icons.Default.TrendingUp, ProBlue, Modifier.weight(1f))
                MiniStat("المتأخرات", money2(stats.debt), Icons.Default.WarningAmber, ProRed, Modifier.weight(1f))
            }
        }
        item {
            SectionTitle("الأداء المالي - آخر 6 أشهر", Icons.Default.BarChart)
            Spacer(Modifier.height(6.dp))
            TrendBars(trend)
        }
        item {
            SectionTitle("يحتاج انتباهك", Icons.Default.AutoAwesome)
            Spacer(Modifier.height(6.dp))
            if (alerts.isEmpty()) {
                InfoCard("ممتاز", "لا توجد تنبيهات مهمة حاليًا", ProGreen)
            } else {
                Column(verticalArrangement = Arrangement.spacedBy(7.dp)) {
                    alerts.forEach { a ->
                        val c = when (a.level) { "danger" -> ProRed; "warning" -> ProOrange; "success" -> ProGreen; else -> ProBlue }
                        InfoCard(a.title, a.detail, c)
                    }
                }
            }
        }
        item { TextButton(onClick = { refresh++ }, modifier = Modifier.fillMaxWidth()) { Icon(Icons.Default.Refresh, null); Spacer(Modifier.width(6.dp)); Text("تحديث لوحة التحكم") } }
    }
}

@Composable
private fun DashboardQuick(text: String, icon: ImageVector, modifier: Modifier, onClick: () -> Unit) {
    FilledTonalButton(onClick = onClick, modifier = modifier.height(72.dp), contentPadding = PaddingValues(6.dp), shape = RoundedCornerShape(16.dp)) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) { Icon(icon, null); Spacer(Modifier.height(3.dp)); Text(text, fontSize = 11.sp, fontWeight = FontWeight.Bold) }
    }
}

@Composable
private fun MiniStat(title: String, value: String, icon: ImageVector, color: Color, modifier: Modifier = Modifier) {
    Card(modifier = modifier, shape = RoundedCornerShape(20.dp)) {
        Column(Modifier.padding(14.dp)) {
            Surface(shape = RoundedCornerShape(12.dp), color = color.copy(alpha = .13f)) { Icon(icon, null, tint = color, modifier = Modifier.padding(9.dp).size(23.dp)) }
            Spacer(Modifier.height(8.dp))
            Text(title, color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 12.sp)
            Text(value, fontWeight = FontWeight.Black, fontSize = 18.sp, maxLines = 1)
        }
    }
}

@Composable
private fun SectionTitle(text: String, icon: ImageVector) {
    Row(verticalAlignment = Alignment.CenterVertically) { Icon(icon, null, tint = MaterialTheme.colorScheme.primary); Spacer(Modifier.width(7.dp)); Text(text, fontWeight = FontWeight.Black, fontSize = 18.sp) }
}

@Composable
private fun InfoCard(title: String, detail: String, color: Color) {
    Surface(shape = RoundedCornerShape(16.dp), color = color.copy(alpha = .09f)) {
        Row(Modifier.fillMaxWidth().padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(9.dp).background(color, RoundedCornerShape(10.dp)))
            Spacer(Modifier.width(10.dp))
            Column { Text(title, fontWeight = FontWeight.Bold); Text(detail, fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant) }
        }
    }
}

@Composable
private fun TrendBars(points: List<TrendPoint>) {
    val maxValue = max(1.0, points.maxOfOrNull { max(it.income, it.expenses) } ?: 1.0)
    Card(shape = RoundedCornerShape(20.dp)) {
        Row(
            Modifier.fillMaxWidth().height(150.dp).padding(14.dp),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.Bottom
        ) {
            points.forEach { p ->
                Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Bottom, modifier = Modifier.weight(1f).fillMaxHeight()) {
                    Row(verticalAlignment = Alignment.Bottom, horizontalArrangement = Arrangement.spacedBy(2.dp), modifier = Modifier.weight(1f)) {
                        Box(Modifier.width(10.dp).height((12 + 92 * (p.income / maxValue)).dp).background(ProGreen, RoundedCornerShape(topStart = 6.dp, topEnd = 6.dp)))
                        Box(Modifier.width(10.dp).height((12 + 92 * (p.expenses / maxValue)).dp).background(ProRed.copy(alpha = .75f), RoundedCornerShape(topStart = 6.dp, topEnd = 6.dp)))
                    }
                    Spacer(Modifier.height(5.dp))
                    Text(p.month.takeLast(2), fontSize = 10.sp)
                }
            }
        }
    }
}

@Composable
private fun TodayScreen(repo: ProfessionalRepository, openFullSystem: () -> Unit) {
    var refresh by remember { mutableIntStateOf(0) }
    var editing by remember { mutableStateOf<TodayLesson?>(null) }
    val lessons = remember(refresh) { repo.todayLessons() }
    LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        item {
            SectionTitle("حصص اليوم", Icons.Default.Today)
            Text("سجّل موضوع الحصة والواجب وحالة التنفيذ من نفس الشاشة", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 13.sp)
        }
        if (lessons.isEmpty()) item { InfoCard("لا توجد حصص", "لا توجد مجموعات مطابقة لجدول اليوم. راجع أيام المجموعات من النظام الكامل.", ProBlue) }
        items(lessons, key = { it.groupId }) { l ->
            Card(shape = RoundedCornerShape(20.dp), modifier = Modifier.fillMaxWidth()) {
                Column(Modifier.padding(15.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Surface(shape = RoundedCornerShape(13.dp), color = if (l.completed) ProGreen.copy(alpha = .13f) else ProOrange.copy(alpha = .13f)) {
                            Icon(if (l.completed) Icons.Default.TaskAlt else Icons.Default.Schedule, null, tint = if (l.completed) ProGreen else ProOrange, modifier = Modifier.padding(10.dp))
                        }
                        Spacer(Modifier.width(10.dp))
                        Column(Modifier.weight(1f)) {
                            Text(l.groupName, fontWeight = FontWeight.Black, fontSize = 17.sp)
                            Text(listOf(l.subject, l.grade, l.center).filter { it.isNotBlank() }.joinToString(" • "), fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        Text(l.time.ifBlank { "--:--" }, fontWeight = FontWeight.Bold)
                    }
                    Spacer(Modifier.height(10.dp))
                    Text("${l.students} طالب" + if (l.topic.isNotBlank()) " • ${l.topic}" else "", fontSize = 13.sp)
                    if (l.homework.isNotBlank()) Text("الواجب: ${l.homework}", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Spacer(Modifier.height(10.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Button(onClick = { editing = l }, modifier = Modifier.weight(1f)) { Icon(Icons.Default.EditNote, null); Spacer(Modifier.width(5.dp)); Text("تفاصيل الحصة") }
                        OutlinedButton(onClick = openFullSystem, modifier = Modifier.weight(1f)) { Icon(Icons.Default.HowToReg, null); Spacer(Modifier.width(5.dp)); Text("الحضور") }
                    }
                }
            }
        }
    }
    editing?.let { lesson ->
        LessonDialog(lesson, onDismiss = { editing = null }, onSave = { topic, homework, notes, done ->
            repo.saveLesson(lesson.groupId, topic, homework, notes, done)
            editing = null
            refresh++
        })
    }
}

@Composable
private fun LessonDialog(lesson: TodayLesson, onDismiss: () -> Unit, onSave: (String, String, String, Boolean) -> Unit) {
    var topic by remember(lesson) { mutableStateOf(lesson.topic) }
    var homework by remember(lesson) { mutableStateOf(lesson.homework) }
    var notes by remember(lesson) { mutableStateOf("") }
    var done by remember(lesson) { mutableStateOf(lesson.completed) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(lesson.groupName, fontWeight = FontWeight.Black) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(9.dp)) {
                OutlinedTextField(topic, { topic = it }, label = { Text("موضوع الحصة") }, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(homework, { homework = it }, label = { Text("الواجب") }, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(notes, { notes = it }, label = { Text("ملاحظات") }, modifier = Modifier.fillMaxWidth(), minLines = 2)
                Row(verticalAlignment = Alignment.CenterVertically) { Checkbox(done, { done = it }); Text("تم تنفيذ الحصة") }
            }
        },
        confirmButton = { Button(onClick = { onSave(topic, homework, notes, done) }) { Text("حفظ") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("إلغاء") } }
    )
}

@Composable
private fun Students360Screen(repo: ProfessionalRepository) {
    var search by remember { mutableStateOf("") }
    var refresh by remember { mutableIntStateOf(0) }
    var selectedId by remember { mutableStateOf<Long?>(null) }
    val students = remember(search, refresh) { repo.studentSummaries(search) }
    LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(14.dp), verticalArrangement = Arrangement.spacedBy(9.dp)) {
        item {
            SectionTitle("ملف الطالب 360°", Icons.Default.Badge)
            Spacer(Modifier.height(8.dp))
            OutlinedTextField(search, { search = it }, label = { Text("بحث باسم الطالب أو ولي الأمر") }, leadingIcon = { Icon(Icons.Default.Search, null) }, singleLine = true, modifier = Modifier.fillMaxWidth())
        }
        items(students, key = { it.id }) { s ->
            Card(shape = RoundedCornerShape(18.dp), modifier = Modifier.fillMaxWidth().clickable { selectedId = s.id }) {
                Column(Modifier.padding(14.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Surface(shape = RoundedCornerShape(50.dp), color = ProBlue.copy(alpha = .12f)) { Icon(Icons.Default.Person, null, tint = ProBlue, modifier = Modifier.padding(10.dp)) }
                        Spacer(Modifier.width(10.dp))
                        Column(Modifier.weight(1f)) { Text(s.name, fontWeight = FontWeight.Black); Text(listOf(s.grade, s.groups).filter { it.isNotBlank() }.joinToString(" • "), fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 2) }
                        Icon(Icons.Default.ChevronLeft, null)
                    }
                    Spacer(Modifier.height(9.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        SmallMetric("حضور", percent(s.attendanceRate), ProGreen, Modifier.weight(1f))
                        SmallMetric("متأخر", money2(s.debt), if (s.debt > 0) ProRed else ProGreen, Modifier.weight(1f))
                        SmallMetric("متوسط", percent(s.examAverage), ProBlue, Modifier.weight(1f))
                    }
                }
            }
        }
        if (students.isEmpty()) item { InfoCard("لا توجد نتائج", "جرّب تغيير كلمة البحث", ProBlue) }
    }
    selectedId?.let { id ->
        val profile = remember(id, refresh) { repo.student360(id) }
        if (profile != null) Student360Dialog(profile) { selectedId = null }
    }
}

@Composable
private fun SmallMetric(title: String, value: String, color: Color, modifier: Modifier = Modifier) {
    Surface(modifier = modifier, shape = RoundedCornerShape(12.dp), color = color.copy(alpha = .09f)) {
        Column(Modifier.padding(vertical = 7.dp), horizontalAlignment = Alignment.CenterHorizontally) { Text(value, color = color, fontWeight = FontWeight.Black, fontSize = 12.sp); Text(title, fontSize = 10.sp) }
    }
}

@Composable
private fun Student360Dialog(p: Student360, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(p.student.name, fontWeight = FontWeight.Black) },
        text = {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(9.dp), modifier = Modifier.heightIn(max = 520.dp)) {
                item {
                    Text(listOf(p.student.grade, p.student.groups).filter { it.isNotBlank() }.joinToString(" • "), color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Spacer(Modifier.height(8.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        SmallMetric("حاضر", p.present.toString(), ProGreen, Modifier.weight(1f))
                        SmallMetric("غائب", p.absent.toString(), ProRed, Modifier.weight(1f))
                        SmallMetric("متأخر", p.late.toString(), ProOrange, Modifier.weight(1f))
                    }
                }
                item { HorizontalDivider(); Text("المالية", fontWeight = FontWeight.Bold); Text("إجمالي المدفوع: ${money2(p.student.paid)}"); Text("المتبقي: ${money2(p.student.debt)}", color = if (p.student.debt > 0) ProRed else ProGreen); if (p.lastPaymentDate.isNotBlank()) Text("آخر دفعة: ${p.lastPaymentDate}", fontSize = 12.sp) }
                item { HorizontalDivider(); Text("المستوى الدراسي", fontWeight = FontWeight.Bold); Text("متوسط الامتحانات: ${percent(p.student.examAverage)}") }
                if (p.exams.isNotEmpty()) items(p.exams) { e -> Text("${e.exam}: ${e.score}/${e.maxScore} (${percent(e.percent)})", fontSize = 12.sp) }
                if (p.student.guardianPhone.isNotBlank() || p.studentPhone.isNotBlank()) item { HorizontalDivider(); Text("التواصل", fontWeight = FontWeight.Bold); if (p.student.guardianPhone.isNotBlank()) Text("ولي الأمر: ${p.student.guardianPhone}"); if (p.studentPhone.isNotBlank()) Text("الطالب: ${p.studentPhone}") }
                if (p.notes.isNotBlank()) item { HorizontalDivider(); Text("ملاحظات", fontWeight = FontWeight.Bold); Text(p.notes) }
            }
        },
        confirmButton = { Button(onClick = onDismiss) { Text("إغلاق") } }
    )
}

@Composable
private fun FinanceCenter(db: YazedTeacherProDb, repo: ProfessionalRepository) {
    var refresh by remember { mutableIntStateOf(0) }
    var showPayment by remember { mutableStateOf(false) }
    val stats = remember(refresh) { repo.dashboard() }
    val debtors = remember(refresh) { repo.debtors() }
    LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        item {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) { SectionTitle("المركز المالي", Icons.Default.AccountBalanceWallet); Text("تحصيل، متأخرات ومتابعة سريعة", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant) }
                Button(onClick = { showPayment = true }) { Icon(Icons.Default.AddCard, null); Spacer(Modifier.width(5.dp)); Text("دفعة") }
            }
        }
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(9.dp)) {
                MiniStat("دخل الشهر", money2(stats.income), Icons.Default.ArrowUpward, ProGreen, Modifier.weight(1f))
                MiniStat("المصروفات", money2(stats.expenses), Icons.Default.ArrowDownward, ProOrange, Modifier.weight(1f))
            }
        }
        item { MiniStat("إجمالي المتأخرات", money2(stats.debt), Icons.Default.PendingActions, ProRed, Modifier.fillMaxWidth()) }
        item { SectionTitle("أعلى المتأخرات", Icons.Default.PriorityHigh) }
        items(debtors.take(20), key = { it.studentId }) { d ->
            Card(shape = RoundedCornerShape(16.dp)) {
                Row(Modifier.fillMaxWidth().padding(13.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.AccountCircle, null, tint = ProRed)
                    Spacer(Modifier.width(9.dp))
                    Column(Modifier.weight(1f)) { Text(d.name, fontWeight = FontWeight.Bold); Text(if (d.lastPayment.isBlank()) "لا توجد دفعة مسجلة" else "آخر دفعة: ${d.lastPayment}", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant) }
                    Text(money2(d.debt), color = ProRed, fontWeight = FontWeight.Black)
                }
            }
        }
        if (debtors.isEmpty()) item { InfoCard("التحصيل ممتاز", "لا توجد متأخرات مسجلة", ProGreen) }
    }
    if (showPayment) {
        QuickPaymentDialog(db, repo, onDismiss = { showPayment = false }, onSaved = { showPayment = false; refresh++ })
    }
}

@Composable
private fun QuickPaymentDialog(db: YazedTeacherProDb, repo: ProfessionalRepository, onDismiss: () -> Unit, onSaved: () -> Unit) {
    val context = LocalContext.current
    val students = remember { repo.studentSummaries() }
    var selected by remember { mutableStateOf(students.firstOrNull()) }
    var menu by remember { mutableStateOf(false) }
    var paidText by remember { mutableStateOf("") }
    var dueText by remember { mutableStateOf("") }
    var notes by remember { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("تسجيل دفعة سريعة", fontWeight = FontWeight.Black) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(9.dp)) {
                Box {
                    OutlinedButton(onClick = { menu = true }, modifier = Modifier.fillMaxWidth()) { Text(selected?.name ?: "اختر الطالب", modifier = Modifier.weight(1f)); Icon(Icons.Default.ArrowDropDown, null) }
                    DropdownMenu(expanded = menu, onDismissRequest = { menu = false }) {
                        students.take(100).forEach { s -> DropdownMenuItem(text = { Text(s.name) }, onClick = { selected = s; menu = false }) }
                    }
                }
                OutlinedTextField(paidText, { paidText = it }, label = { Text("المبلغ المدفوع") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal), modifier = Modifier.fillMaxWidth(), singleLine = true)
                OutlinedTextField(dueText, { dueText = it }, label = { Text("المتبقي بعد الدفعة") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal), modifier = Modifier.fillMaxWidth(), singleLine = true)
                OutlinedTextField(notes, { notes = it }, label = { Text("ملاحظات") }, modifier = Modifier.fillMaxWidth())
            }
        },
        confirmButton = {
            Button(onClick = {
                val s = selected
                val paid = paidText.toDoubleOrNull()
                val due = dueText.toDoubleOrNull() ?: 0.0
                if (s == null || paid == null || paid <= 0) {
                    Toast.makeText(context, "اختر الطالب وأدخل مبلغًا صحيحًا", Toast.LENGTH_SHORT).show()
                } else {
                    repo.quickPayment(s.id, paid, due, "نقدي", notes)
                    Toast.makeText(context, "تم تسجيل الدفعة", Toast.LENGTH_SHORT).show()
                    onSaved()
                }
            }) { Text("حفظ الدفعة") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("إلغاء") } }
    )
}
