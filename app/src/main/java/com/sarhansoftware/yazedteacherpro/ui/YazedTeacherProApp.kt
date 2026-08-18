package com.sarhansoftware.yazedteacherpro.ui

import android.app.Activity
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
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
import com.sarhansoftware.yazedteacherpro.data.*
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private val Gold = Color(0xFFC7A23B)
private val GoldDark = Color(0xFF8E6E1D)
private val Dark = Color(0xFF17191F)
private val Soft = Color(0xFFF6F3EA)
private val Mint = Color(0xFFE2F5EF)
private val Pink = Color(0xFFFBE7EA)
private val Blue = Color(0xFFE7F1FA)
private val Purple = Color(0xFFEEE9FB)

private fun money(v: Double) = "%.2f ج.م".format(Locale.US, v)
private fun dateNow() = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date())
private fun receiptNow() = "R-" + SimpleDateFormat("yyMMddHHmmss", Locale.US).format(Date())

@Composable
fun YazedTeacherProApp(db: YazedTeacherProDb) {
    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
        MaterialTheme(
            colorScheme = lightColorScheme(
                primary = GoldDark,
                secondary = Gold,
                background = Soft,
                surface = Color.White,
                error = Color(0xFFB3261E),
            ),
            typography = Typography(
                titleLarge = androidx.compose.ui.text.TextStyle(fontWeight = FontWeight.Bold, fontSize = 22.sp),
                titleMedium = androidx.compose.ui.text.TextStyle(fontWeight = FontWeight.Bold, fontSize = 18.sp),
                bodyLarge = androidx.compose.ui.text.TextStyle(fontSize = 16.sp),
            )
        ) {
            Surface(Modifier.fillMaxSize(), color = Soft) {
                var unlockedRefresh by remember { mutableIntStateOf(0) }
                val unlocked = remember(unlockedRefresh) { db.isUnlocked() }
                if (!unlocked) {
                    LicenseScreen(db = db, lockedMode = true, onActivated = { unlockedRefresh++ })
                } else {
                    var user by remember { mutableStateOf<String?>(null) }
                    if (user == null) LoginScreen(db) { user = it }
                    else MainShell(db, user!!, onLogout = { user = null })
                }
            }
        }
    }
}

@Composable
private fun LoginScreen(db: YazedTeacherProDb, onLogin: (String) -> Unit) {
    val context = LocalContext.current
    var username by remember { mutableStateOf("1") }
    var password by remember { mutableStateOf("000000") }
    Box(Modifier.fillMaxSize().padding(24.dp), contentAlignment = Alignment.Center) {
        Card(
            Modifier.fillMaxWidth().widthIn(max = 480.dp),
            shape = RoundedCornerShape(28.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            elevation = CardDefaults.cardElevation(8.dp)
        ) {
            Column(Modifier.padding(28.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(Icons.Default.School, null, tint = GoldDark, modifier = Modifier.size(64.dp))
                Spacer(Modifier.height(12.dp))
                Text("YazedTeacherPro", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Black, color = Dark)
                Text("YazedTeacherPro - إدارة المدرس والسنتر", color = Color.Gray)
                Spacer(Modifier.height(24.dp))
                OutlinedTextField(username, { username = it }, label = { Text("اسم المستخدم") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                Spacer(Modifier.height(12.dp))
                OutlinedTextField(password, { password = it }, label = { Text("كلمة المرور") }, singleLine = true, visualTransformation = PasswordVisualTransformation(), modifier = Modifier.fillMaxWidth())
                Spacer(Modifier.height(18.dp))
                Button(
                    onClick = {
                        if (db.login(username, password)) onLogin(username.trim())
                        else Toast.makeText(context, "بيانات الدخول غير صحيحة", Toast.LENGTH_SHORT).show()
                    },
                    modifier = Modifier.fillMaxWidth().height(52.dp)
                ) { Text("دخول", fontWeight = FontWeight.Bold) }
                Spacer(Modifier.height(12.dp))
                if (!db.isLicenseValid().first) Text("متبقي من التجربة: ${db.trialDaysRemaining()} يوم", color = GoldDark)
            }
        }
    }
}

private enum class AppScreen(val title: String, val icon: ImageVector) {
    Dashboard("لوحة التحكم", Icons.Default.Home),
    Students("الطلبة", Icons.Default.Person),
    Groups("المجموعات والسناتر", Icons.Default.Group),
    Attendance("الحضور والغياب", Icons.Default.CheckCircle),
    Payments("المدفوعات", Icons.Default.Payment),
    Expenses("المصروفات", Icons.Default.RemoveCircle),
    Exams("الامتحانات والنتائج", Icons.Default.Star),
    WhatsApp("واتساب", Icons.Default.Message),
    Reports("التقارير والنسخ", Icons.Default.Assessment),
    Settings("الإعدادات", Icons.Default.Settings),
    Audit("سجل العمليات", Icons.Default.History),
    License("التفعيل", Icons.Default.Lock),
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun MainShell(db: YazedTeacherProDb, user: String, onLogout: () -> Unit) {
    var screen by remember { mutableStateOf(AppScreen.Dashboard) }
    val drawerState = rememberDrawerState(DrawerValue.Closed)
    val scope = rememberCoroutineScope()

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            ModalDrawerSheet(modifier = Modifier.width(310.dp), drawerContainerColor = Dark) {
                Column(Modifier.fillMaxWidth().padding(22.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(Icons.Default.School, null, tint = Gold, modifier = Modifier.size(54.dp))
                    Text("TEACHER HUB", color = Gold, fontWeight = FontWeight.Black, fontSize = 20.sp)
                    Text(db.businessName(), color = Color.White.copy(alpha = .8f), maxLines = 1)
                }
                HorizontalDivider(color = Color.White.copy(alpha = .12f))
                AppScreen.entries.forEach { item ->
                    NavigationDrawerItem(
                        label = { Text(item.title, fontWeight = FontWeight.SemiBold) },
                        selected = screen == item,
                        onClick = { screen = item; scope.launch { drawerState.close() } },
                        icon = { Icon(item.icon, null) },
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 2.dp),
                        colors = NavigationDrawerItemDefaults.colors(
                            selectedContainerColor = Gold,
                            selectedTextColor = Dark,
                            selectedIconColor = Dark,
                            unselectedContainerColor = Color.Transparent,
                            unselectedTextColor = Color.White,
                            unselectedIconColor = Color.White.copy(alpha = .8f)
                        )
                    )
                }
                Spacer(Modifier.weight(1f))
                TextButton(onClick = onLogout, modifier = Modifier.fillMaxWidth().padding(12.dp)) {
                    Icon(Icons.Default.ExitToApp, null, tint = Color.White)
                    Spacer(Modifier.width(8.dp))
                    Text("تسجيل خروج", color = Color.White)
                }
            }
        }
    ) {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = { Text(screen.title, fontWeight = FontWeight.Black) },
                    navigationIcon = { IconButton(onClick = { scope.launch { drawerState.open() } }) { Icon(Icons.Default.Menu, "القائمة") } },
                    colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.White)
                )
            },
            containerColor = Soft
        ) { padding ->
            Box(Modifier.fillMaxSize().padding(padding).padding(horizontal = 12.dp)) {
                when (screen) {
                    AppScreen.Dashboard -> DashboardScreen(db) { screen = it }
                    AppScreen.Students -> StudentsScreen(db, user)
                    AppScreen.Groups -> GroupsScreen(db)
                    AppScreen.Attendance -> AttendanceScreen(db)
                    AppScreen.Payments -> PaymentsScreen(db)
                    AppScreen.Expenses -> ExpensesScreen(db)
                    AppScreen.Exams -> ExamsScreen(db)
                    AppScreen.WhatsApp -> WhatsAppScreen(db)
                    AppScreen.Reports -> ReportsScreen(db)
                    AppScreen.Settings -> SettingsScreen(db, user)
                    AppScreen.Audit -> AuditScreen(db)
                    AppScreen.License -> LicenseScreen(db, lockedMode = false, onActivated = {})
                }
            }
        }
    }
}

@Composable
private fun TrialBanner(db: YazedTeacherProDb) {
    if (!db.isLicenseValid().first) {
        Surface(color = Color(0xFFFFF3CD), shape = RoundedCornerShape(14.dp), modifier = Modifier.fillMaxWidth()) {
            Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Info, null, tint = GoldDark)
                Spacer(Modifier.width(8.dp))
                Text("نسخة تجريبية — متبقي ${db.trialDaysRemaining()} يوم", color = Dark, fontWeight = FontWeight.SemiBold)
            }
        }
    }
}

@Composable
private fun DashboardScreen(db: YazedTeacherProDb, navigate: (AppScreen) -> Unit) {
    var refresh by remember { mutableIntStateOf(0) }
    val stats = remember(refresh) { db.dashboardStats() }
    Column(Modifier.fillMaxSize().padding(vertical = 12.dp)) {
        TrialBanner(db)
        Spacer(Modifier.height(12.dp))
        Card(colors = CardDefaults.cardColors(containerColor = Color.White), shape = RoundedCornerShape(24.dp), modifier = Modifier.fillMaxWidth()) {
            Column(Modifier.padding(18.dp)) {
                Text("أهلاً بك في ${db.businessName()}", style = MaterialTheme.typography.titleLarge, color = Dark)
                Text("إدارة يومك الدراسي من الموبايل", color = Color.Gray)
                Spacer(Modifier.height(14.dp))
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    QuickAction("تسجيل حضور", Icons.Default.CheckCircle, Modifier.weight(1f)) { navigate(AppScreen.Attendance) }
                    QuickAction("تسجيل دفع", Icons.Default.Payment, Modifier.weight(1f)) { navigate(AppScreen.Payments) }
                    QuickAction("إضافة طالب", Icons.Default.PersonAdd, Modifier.weight(1f)) { navigate(AppScreen.Students) }
                }
            }
        }
        Spacer(Modifier.height(14.dp))
        Text("الأقسام الرئيسية", style = MaterialTheme.typography.titleLarge, modifier = Modifier.padding(horizontal = 6.dp))
        Spacer(Modifier.height(8.dp))
        val cards = listOf(
            Triple("الطلبة", stats.students.toString(), Blue),
            Triple("المجموعات", stats.groups.toString(), Pink),
            Triple("حضور اليوم", stats.presentToday.toString(), Mint),
            Triple("غياب اليوم", stats.absentToday.toString(), Color(0xFFFFE9E7)),
            Triple("دخل الشهر", money(stats.income), Purple),
            Triple("المتأخرات", money(stats.debt), Color(0xFFFFF0D8)),
            Triple("المصروفات", money(stats.expenses), Color(0xFFF5E9E9)),
            Triple("رسائل معلقة", stats.queuedMessages.toString(), Mint),
        )
        LazyVerticalGrid(columns = GridCells.Fixed(2), horizontalArrangement = Arrangement.spacedBy(10.dp), verticalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.weight(1f)) {
            items(cards) { (title, value, color) ->
                Card(shape = RoundedCornerShape(22.dp), colors = CardDefaults.cardColors(containerColor = Color.White), elevation = CardDefaults.cardElevation(2.dp)) {
                    Column(Modifier.fillMaxWidth().padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                        Box(Modifier.size(44.dp).background(color, RoundedCornerShape(14.dp)))
                        Spacer(Modifier.height(10.dp))
                        Text(title, fontWeight = FontWeight.Bold)
                        Text(value, color = GoldDark, fontWeight = FontWeight.Black, fontSize = 20.sp)
                    }
                }
            }
        }
        TextButton(onClick = { refresh++ }, modifier = Modifier.align(Alignment.CenterHorizontally)) { Icon(Icons.Default.Refresh, null); Text("تحديث") }
    }
}

@Composable
private fun QuickAction(text: String, icon: ImageVector, modifier: Modifier = Modifier, onClick: () -> Unit) {
    FilledTonalButton(onClick = onClick, modifier = modifier.height(72.dp), contentPadding = PaddingValues(8.dp)) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) { Icon(icon, null); Spacer(Modifier.height(4.dp)); Text(text, fontSize = 12.sp, fontWeight = FontWeight.Bold) }
    }
}

@Composable
private fun SectionHeader(title: String, actionText: String? = null, onAction: (() -> Unit)? = null) {
    Row(Modifier.fillMaxWidth().padding(vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
        Text(title, style = MaterialTheme.typography.titleLarge, modifier = Modifier.weight(1f))
        if (actionText != null && onAction != null) Button(onClick = onAction) { Text(actionText) }
    }
}

@Composable
private fun EmptyState(text: String) {
    Box(Modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) { Text(text, color = Color.Gray) }
}

@Composable
private fun StudentsScreen(db: YazedTeacherProDb, user: String) {
    var search by remember { mutableStateOf("") }
    var refresh by remember { mutableIntStateOf(0) }
    var edit by remember { mutableStateOf<Student?>(null) }
    var showAdd by remember { mutableStateOf(false) }
    val data = remember(search, refresh) { db.students(search) }
    Column(Modifier.fillMaxSize().padding(vertical = 8.dp)) {
        SectionHeader("إدارة الطلبة", "إضافة طالب") { showAdd = true }
        OutlinedTextField(search, { search = it }, label = { Text("بحث باسم الطالب أو رقم ولي الأمر") }, leadingIcon = { Icon(Icons.Default.Search, null) }, modifier = Modifier.fillMaxWidth(), singleLine = true)
        Spacer(Modifier.height(8.dp))
        if (data.isEmpty()) EmptyState("لا يوجد طلبة")
        else LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxSize()) {
            items(data, key = { it.id }) { s ->
                Card(colors = CardDefaults.cardColors(containerColor = Color.White), shape = RoundedCornerShape(18.dp), modifier = Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(14.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.AccountCircle, null, tint = GoldDark, modifier = Modifier.size(42.dp))
                            Spacer(Modifier.width(10.dp))
                            Column(Modifier.weight(1f)) { Text(s.name, fontWeight = FontWeight.Black, fontSize = 17.sp); Text("${s.grade} • ${s.status}", color = Color.Gray) }
                            IconButton(onClick = { edit = s }) { Icon(Icons.Default.Edit, "تعديل") }
                        }
                        if (s.guardianPhone.isNotBlank()) Text("ولي الأمر: ${s.guardianPhone}")
                        if (s.studentPhone.isNotBlank()) Text("الطالب: ${s.studentPhone}")
                        if (s.notes.isNotBlank()) Text(s.notes, color = Color.DarkGray)
                    }
                }
            }
        }
    }
    if (showAdd) StudentDialog(null, onDismiss = { showAdd = false }) { name, guardian, phone, grade, status, notes ->
        if (name.isNotBlank()) { db.addStudent(name, guardian, phone, grade, status, notes, user); refresh++; showAdd = false }
    }
    edit?.let { current ->
        StudentDialog(current, onDismiss = { edit = null }) { name, guardian, phone, grade, status, notes ->
            db.updateStudent(current.copy(name = name, guardianPhone = guardian, studentPhone = phone, grade = grade, status = status, notes = notes), user)
            refresh++; edit = null
        }
    }
}

@Composable
private fun StudentDialog(student: Student?, onDismiss: () -> Unit, onSave: (String, String, String, String, String, String) -> Unit) {
    var name by remember { mutableStateOf(student?.name ?: "") }
    var guardian by remember { mutableStateOf(student?.guardianPhone ?: "") }
    var phone by remember { mutableStateOf(student?.studentPhone ?: "") }
    var grade by remember { mutableStateOf(student?.grade ?: "") }
    var status by remember { mutableStateOf(student?.status ?: "نشط") }
    var notes by remember { mutableStateOf(student?.notes ?: "") }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (student == null) "إضافة طالب" else "تعديل الطالب") },
        text = {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                item { OutlinedTextField(name, { name = it }, label = { Text("اسم الطالب") }, modifier = Modifier.fillMaxWidth()) }
                item { OutlinedTextField(guardian, { guardian = it }, label = { Text("موبايل ولي الأمر") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone), modifier = Modifier.fillMaxWidth()) }
                item { OutlinedTextField(phone, { phone = it }, label = { Text("موبايل الطالب") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone), modifier = Modifier.fillMaxWidth()) }
                item { OutlinedTextField(grade, { grade = it }, label = { Text("الصف الدراسي") }, modifier = Modifier.fillMaxWidth()) }
                item { Picker("الحالة", listOf("نشط", "موقوف", "أرشيف"), status, { it }, { status = it }) }
                item { OutlinedTextField(notes, { notes = it }, label = { Text("ملاحظات") }, modifier = Modifier.fillMaxWidth(), minLines = 2) }
            }
        },
        confirmButton = { Button(onClick = { onSave(name, guardian, phone, grade, status, notes) }, enabled = name.isNotBlank()) { Text("حفظ") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("إلغاء") } }
    )
}

@Composable
private fun GroupsScreen(db: YazedTeacherProDb) {
    var tab by remember { mutableIntStateOf(0) }
    var refresh by remember { mutableIntStateOf(0) }
    var addCenter by remember { mutableStateOf(false) }
    var addGroup by remember { mutableStateOf(false) }
    Column(Modifier.fillMaxSize()) {
        TabRow(selectedTabIndex = tab) {
            Tab(tab == 0, onClick = { tab = 0 }, text = { Text("المجموعات") })
            Tab(tab == 1, onClick = { tab = 1 }, text = { Text("السناتر") })
        }
        if (tab == 0) {
            val groups = remember(refresh) { db.groups() }
            SectionHeader("المجموعات", "إضافة مجموعة") { addGroup = true }
            LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                items(groups, key = { it.id }) { g ->
                    SimpleCard(g.name, "${g.subject} • ${g.grade}\n${g.day} ${g.time} • ${money(g.monthlyPrice)}\nالسنتر: ${g.centerName.ifBlank { "بدون" }} • النسبة ${g.sharePercent}%")
                }
            }
        } else {
            val centers = remember(refresh) { db.centers() }
            SectionHeader("السناتر", "إضافة سنتر") { addCenter = true }
            LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                items(centers, key = { it.id }) { c -> SimpleCard(c.name, "${c.address}\n${c.phone} • نسبة السنتر ${c.sharePercent}%") }
            }
        }
    }
    if (addCenter) CenterDialog({ addCenter = false }) { name, address, phone, percent -> db.addCenter(name, address, phone, percent); refresh++; addCenter = false }
    if (addGroup) GroupDialog(db, { addGroup = false }) { name, subject, grade, day, time, price, centerId, percent -> db.addGroup(name, subject, grade, day, time, price, centerId, percent); refresh++; addGroup = false }
}

@Composable
private fun CenterDialog(onDismiss: () -> Unit, onSave: (String, String, String, Double) -> Unit) {
    var name by remember { mutableStateOf("") }; var address by remember { mutableStateOf("") }; var phone by remember { mutableStateOf("") }; var percent by remember { mutableStateOf("0") }
    AlertDialog(onDismissRequest = onDismiss, title = { Text("إضافة سنتر") }, text = { Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        OutlinedTextField(name, { name = it }, label = { Text("اسم السنتر") }, modifier = Modifier.fillMaxWidth())
        OutlinedTextField(address, { address = it }, label = { Text("العنوان") }, modifier = Modifier.fillMaxWidth())
        OutlinedTextField(phone, { phone = it }, label = { Text("الهاتف") }, modifier = Modifier.fillMaxWidth())
        OutlinedTextField(percent, { percent = it }, label = { Text("نسبة السنتر %") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal), modifier = Modifier.fillMaxWidth())
    } }, confirmButton = { Button(onClick = { onSave(name, address, phone, percent.toDoubleOrNull() ?: 0.0) }, enabled = name.isNotBlank()) { Text("حفظ") } }, dismissButton = { TextButton(onClick = onDismiss) { Text("إلغاء") } })
}

@Composable
private fun GroupDialog(db: YazedTeacherProDb, onDismiss: () -> Unit, onSave: (String, String, String, String, String, Double, Long?, Double) -> Unit) {
    val centers = remember { db.centers() }
    var name by remember { mutableStateOf("") }; var subject by remember { mutableStateOf("") }; var grade by remember { mutableStateOf("") }; var day by remember { mutableStateOf("") }; var time by remember { mutableStateOf("") }; var price by remember { mutableStateOf("0") }; var center by remember { mutableStateOf<Center?>(null) }; var percent by remember { mutableStateOf("0") }
    AlertDialog(onDismissRequest = onDismiss, title = { Text("إضافة مجموعة") }, text = { LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        item { OutlinedTextField(name, { name = it }, label = { Text("اسم المجموعة") }, modifier = Modifier.fillMaxWidth()) }
        item { OutlinedTextField(subject, { subject = it }, label = { Text("المادة") }, modifier = Modifier.fillMaxWidth()) }
        item { OutlinedTextField(grade, { grade = it }, label = { Text("الصف") }, modifier = Modifier.fillMaxWidth()) }
        item { OutlinedTextField(day, { day = it }, label = { Text("اليوم") }, modifier = Modifier.fillMaxWidth()) }
        item { OutlinedTextField(time, { time = it }, label = { Text("الساعة") }, modifier = Modifier.fillMaxWidth()) }
        item { OutlinedTextField(price, { price = it }, label = { Text("السعر الشهري") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal), modifier = Modifier.fillMaxWidth()) }
        item { Picker("السنتر - اختياري", centers, center, { it.name }, { center = it }) }
        item { OutlinedTextField(percent, { percent = it }, label = { Text("نسبة السنتر %") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal), modifier = Modifier.fillMaxWidth()) }
    } }, confirmButton = { Button(onClick = { onSave(name, subject, grade, day, time, price.toDoubleOrNull() ?: 0.0, center?.id, percent.toDoubleOrNull() ?: 0.0) }, enabled = name.isNotBlank()) { Text("حفظ") } }, dismissButton = { TextButton(onClick = onDismiss) { Text("إلغاء") } })
}

@Composable
private fun AttendanceScreen(db: YazedTeacherProDb) {
    val groups = remember { db.groups() }
    var group by remember { mutableStateOf<GroupInfo?>(groups.firstOrNull()) }
    var date by remember { mutableStateOf(dateNow()) }
    var refresh by remember { mutableIntStateOf(0) }
    var showLink by remember { mutableStateOf(false) }
    val rows = remember(group?.id, date, refresh) { group?.let { db.attendanceStudents(it.id, date) } ?: emptyList() }
    Column(Modifier.fillMaxSize().padding(vertical = 8.dp)) {
        Picker("اختر المجموعة", groups, group, { it.name }, { group = it; refresh++ })
        Spacer(Modifier.height(8.dp))
        OutlinedTextField(date, { date = it }, label = { Text("التاريخ YYYY-MM-DD") }, modifier = Modifier.fillMaxWidth(), singleLine = true)
        Spacer(Modifier.height(8.dp))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            Button(onClick = { showLink = true }, enabled = group != null, modifier = Modifier.weight(1f)) { Text("ربط طالب") }
            FilledTonalButton(onClick = { group?.let { g -> rows.forEach { db.markAttendance(g.id, g.name, it.id, "حضور", date) }; refresh++ } }, modifier = Modifier.weight(1f)) { Text("حضور الكل") }
            FilledTonalButton(onClick = { group?.let { g -> rows.forEach { db.markAttendance(g.id, g.name, it.id, "غياب", date) }; refresh++ } }, modifier = Modifier.weight(1f)) { Text("غياب الكل") }
        }
        Spacer(Modifier.height(8.dp))
        if (group == null) EmptyState("أضف مجموعة أولاً") else if (rows.isEmpty()) EmptyState("اربط الطلبة بالمجموعة") else LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            items(rows, key = { it.id }) { s ->
                Card(colors = CardDefaults.cardColors(containerColor = Color.White), shape = RoundedCornerShape(16.dp)) {
                    Row(Modifier.fillMaxWidth().padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) { Text(s.name, fontWeight = FontWeight.Bold); Text(s.status, color = if (s.status == "غياب") MaterialTheme.colorScheme.error else GoldDark) }
                        TextButton(onClick = { group?.let { db.markAttendance(it.id, it.name, s.id, "حضور", date); refresh++ } }) { Text("حضور") }
                        TextButton(onClick = { group?.let { db.markAttendance(it.id, it.name, s.id, "غياب", date); refresh++ } }) { Text("غياب", color = MaterialTheme.colorScheme.error) }
                    }
                }
            }
        }
    }
    if (showLink && group != null) LinkStudentDialog(db, onDismiss = { showLink = false }) { student -> db.linkStudentToGroup(student.id, group!!.id); refresh++; showLink = false }
}

@Composable
private fun LinkStudentDialog(db: YazedTeacherProDb, onDismiss: () -> Unit, onSelect: (Student) -> Unit) {
    val students = remember { db.activeStudents() }
    var selected by remember { mutableStateOf<Student?>(students.firstOrNull()) }
    AlertDialog(onDismissRequest = onDismiss, title = { Text("ربط طالب بالمجموعة") }, text = { Picker("اختر الطالب", students, selected, { it.name }, { selected = it }) }, confirmButton = { Button(onClick = { selected?.let(onSelect) }, enabled = selected != null) { Text("ربط") } }, dismissButton = { TextButton(onClick = onDismiss) { Text("إلغاء") } })
}

@Composable
private fun PaymentsScreen(db: YazedTeacherProDb) {
    val context = LocalContext.current
    var refresh by remember { mutableIntStateOf(0) }
    var add by remember { mutableStateOf(false) }
    val rows = remember(refresh) { db.payments() }
    Column(Modifier.fillMaxSize().padding(vertical = 8.dp)) {
        SectionHeader("المدفوعات والمتأخرات", "تسجيل دفع") { add = true }
        LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            items(rows, key = { it.id }) { p ->
                Card(colors = CardDefaults.cardColors(containerColor = Color.White), shape = RoundedCornerShape(18.dp)) {
                    Column(Modifier.padding(14.dp)) {
                        Row { Text(p.student, fontWeight = FontWeight.Black, modifier = Modifier.weight(1f)); Text(money(p.paid), color = GoldDark, fontWeight = FontWeight.Bold) }
                        Text("${p.group} • ${p.date} • ${p.method}", color = Color.Gray)
                        if (p.due > 0) Text("متبقي: ${money(p.due)}", color = MaterialTheme.colorScheme.error, fontWeight = FontWeight.Bold)
                        Row { Text("إيصال: ${p.receipt}", modifier = Modifier.weight(1f)); TextButton(onClick = { shareText(context, receiptText(db, p)) }) { Icon(Icons.Default.Share, null); Text("مشاركة") } }
                    }
                }
            }
        }
    }
    if (add) PaymentDialog(db, { add = false }) { studentId, groupId, paid, due, method, receipt, notes -> db.addPayment(studentId, groupId, paid, due, method, receipt, notes); refresh++; add = false }
}

private fun receiptText(db: YazedTeacherProDb, p: PaymentRow) = """${db.businessName()}
إيصال دفع
============================
الطالب: ${p.student}
المجموعة: ${p.group}
المدفوع: ${money(p.paid)}
المتبقي: ${money(p.due)}
التاريخ: ${p.date}
طريقة الدفع: ${p.method}
رقم الإيصال: ${p.receipt}
============================
شكرًا لتعاملكم معنا"""

@Composable
private fun PaymentDialog(db: YazedTeacherProDb, onDismiss: () -> Unit, onSave: (Long, Long?, Double, Double, String, String, String) -> Unit) {
    val students = remember { db.activeStudents() }; val groups = remember { db.groups() }
    var student by remember { mutableStateOf<Student?>(students.firstOrNull()) }; var group by remember { mutableStateOf<GroupInfo?>(groups.firstOrNull()) }
    var paid by remember { mutableStateOf("") }; var due by remember { mutableStateOf("0") }; var method by remember { mutableStateOf("نقدي") }; var receipt by remember { mutableStateOf(receiptNow()) }; var notes by remember { mutableStateOf("") }
    AlertDialog(onDismissRequest = onDismiss, title = { Text("تسجيل دفع") }, text = { LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        item { Picker("الطالب", students, student, { it.name }, { student = it }) }
        item { Picker("المجموعة", groups, group, { it.name }, { group = it }) }
        item { OutlinedTextField(paid, { paid = it }, label = { Text("المبلغ المدفوع") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal), modifier = Modifier.fillMaxWidth()) }
        item { OutlinedTextField(due, { due = it }, label = { Text("المتبقي") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal), modifier = Modifier.fillMaxWidth()) }
        item { OutlinedTextField(method, { method = it }, label = { Text("طريقة الدفع") }, modifier = Modifier.fillMaxWidth()) }
        item { OutlinedTextField(receipt, { receipt = it }, label = { Text("رقم الإيصال") }, modifier = Modifier.fillMaxWidth()) }
        item { OutlinedTextField(notes, { notes = it }, label = { Text("ملاحظات") }, modifier = Modifier.fillMaxWidth()) }
    } }, confirmButton = { Button(onClick = { student?.let { onSave(it.id, group?.id, paid.toDoubleOrNull() ?: 0.0, due.toDoubleOrNull() ?: 0.0, method, receipt, notes) } }, enabled = student != null) { Text("حفظ") } }, dismissButton = { TextButton(onClick = onDismiss) { Text("إلغاء") } })
}

@Composable
private fun ExpensesScreen(db: YazedTeacherProDb) {
    var refresh by remember { mutableIntStateOf(0) }; var add by remember { mutableStateOf(false) }; val rows = remember(refresh) { db.expenses() }
    Column(Modifier.fillMaxSize().padding(vertical = 8.dp)) {
        SectionHeader("المصروفات", "إضافة مصروف") { add = true }
        LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) { items(rows, key = { it.id }) { e -> SimpleCard(e.category, "${money(e.amount)} • ${e.date}\n${e.notes}") } }
    }
    if (add) ExpenseDialog({ add = false }) { category, amount, notes -> db.addExpense(category, amount, notes); refresh++; add = false }
}

@Composable
private fun ExpenseDialog(onDismiss: () -> Unit, onSave: (String, Double, String) -> Unit) {
    var category by remember { mutableStateOf("") }; var amount by remember { mutableStateOf("") }; var notes by remember { mutableStateOf("") }
    AlertDialog(onDismissRequest = onDismiss, title = { Text("إضافة مصروف") }, text = { Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        OutlinedTextField(category, { category = it }, label = { Text("البند") }, modifier = Modifier.fillMaxWidth())
        OutlinedTextField(amount, { amount = it }, label = { Text("المبلغ") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal), modifier = Modifier.fillMaxWidth())
        OutlinedTextField(notes, { notes = it }, label = { Text("ملاحظات") }, modifier = Modifier.fillMaxWidth())
    } }, confirmButton = { Button(onClick = { onSave(category, amount.toDoubleOrNull() ?: 0.0, notes) }, enabled = category.isNotBlank()) { Text("حفظ") } }, dismissButton = { TextButton(onClick = onDismiss) { Text("إلغاء") } })
}

@Composable
private fun ExamsScreen(db: YazedTeacherProDb) {
    var refresh by remember { mutableIntStateOf(0) }; var add by remember { mutableStateOf(false) }
    val exams = remember(refresh) { db.exams() }; var selected by remember(exams) { mutableStateOf<ExamRow?>(exams.firstOrNull()) }
    val students = remember(selected?.id, refresh) { selected?.let { db.examStudents(it.id) } ?: emptyList() }
    var scoring by remember { mutableStateOf<ExamStudent?>(null) }
    Column(Modifier.fillMaxSize().padding(vertical = 8.dp)) {
        SectionHeader("الامتحانات والنتائج", "إضافة امتحان") { add = true }
        Picker("اختر الامتحان", exams, selected, { "${it.name} - ${it.group}" }, { selected = it })
        Spacer(Modifier.height(8.dp))
        selected?.let { e -> Text("النهاية: ${e.maxScore} • ${e.date}", color = Color.Gray, modifier = Modifier.padding(4.dp)) }
        if (selected == null) EmptyState("أضف امتحان أولاً") else if (students.isEmpty()) EmptyState("اربط طلبة بمجموعة الامتحان أولاً") else LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            items(students, key = { it.id }) { s ->
                Card(colors = CardDefaults.cardColors(containerColor = Color.White), shape = RoundedCornerShape(16.dp), modifier = Modifier.clickable { scoring = s }) {
                    Row(Modifier.fillMaxWidth().padding(14.dp), verticalAlignment = Alignment.CenterVertically) { Text(s.name, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f)); Text("${s.score} / ${selected?.maxScore ?: 0.0}", color = GoldDark) }
                }
            }
        }
    }
    if (add) ExamDialog(db, { add = false }) { name, groupId, max -> db.addExam(name, groupId, max); refresh++; add = false }
    scoring?.let { s -> ScoreDialog(s, selected?.maxScore ?: 0.0, { scoring = null }) { value -> selected?.let { db.saveExamScore(it.id, s.id, value) }; refresh++; scoring = null } }
}

@Composable
private fun ExamDialog(db: YazedTeacherProDb, onDismiss: () -> Unit, onSave: (String, Long?, Double) -> Unit) {
    val groups = remember { db.groups() }; var name by remember { mutableStateOf("") }; var group by remember { mutableStateOf<GroupInfo?>(groups.firstOrNull()) }; var max by remember { mutableStateOf("100") }
    AlertDialog(onDismissRequest = onDismiss, title = { Text("إضافة امتحان") }, text = { Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        OutlinedTextField(name, { name = it }, label = { Text("اسم الامتحان") }, modifier = Modifier.fillMaxWidth())
        Picker("المجموعة", groups, group, { it.name }, { group = it })
        OutlinedTextField(max, { max = it }, label = { Text("الدرجة النهائية") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal), modifier = Modifier.fillMaxWidth())
    } }, confirmButton = { Button(onClick = { onSave(name, group?.id, max.toDoubleOrNull() ?: 0.0) }, enabled = name.isNotBlank() && group != null) { Text("حفظ") } }, dismissButton = { TextButton(onClick = onDismiss) { Text("إلغاء") } })
}

@Composable
private fun ScoreDialog(student: ExamStudent, max: Double, onDismiss: () -> Unit, onSave: (Double) -> Unit) {
    var score by remember { mutableStateOf(student.score.toString()) }
    AlertDialog(onDismissRequest = onDismiss, title = { Text(student.name) }, text = { OutlinedTextField(score, { score = it }, label = { Text("الدرجة من $max") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal), modifier = Modifier.fillMaxWidth()) }, confirmButton = { Button(onClick = { onSave(score.toDoubleOrNull() ?: 0.0) }) { Text("حفظ وإضافة رسالة النتيجة") } }, dismissButton = { TextButton(onClick = onDismiss) { Text("إلغاء") } })
}

@Composable
private fun WhatsAppScreen(db: YazedTeacherProDb) {
    val context = LocalContext.current
    var refresh by remember { mutableIntStateOf(0) }; val rows = remember(refresh) { db.whatsappMessages() }
    val clipboard = remember { context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager }
    Column(Modifier.fillMaxSize().padding(vertical = 8.dp)) {
        SectionHeader("رسائل واتساب")
        if (rows.isEmpty()) EmptyState("لا توجد رسائل") else LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            items(rows, key = { it.id }) { row ->
                Card(colors = CardDefaults.cardColors(containerColor = Color.White), shape = RoundedCornerShape(18.dp)) {
                    Column(Modifier.padding(14.dp)) {
                        Row { Text(row.type, fontWeight = FontWeight.Black, modifier = Modifier.weight(1f)); Text(row.status, color = if (row.status == "Queued") GoldDark else Color.Gray) }
                        Text(row.phone, color = Color.Gray)
                        Text(row.body, modifier = Modifier.padding(vertical = 8.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            TextButton(onClick = { openWhatsApp(context, db.normalizePhone(row.phone), row.body) }) { Text("فتح واتساب") }
                            TextButton(onClick = { clipboard.setPrimaryClip(ClipData.newPlainText("message", row.body)); Toast.makeText(context, "تم النسخ", Toast.LENGTH_SHORT).show() }) { Text("نسخ") }
                            TextButton(onClick = { db.markWhatsApp(row.id, "Sent"); refresh++ }) { Text("تم الإرسال") }
                            TextButton(onClick = { db.markWhatsApp(row.id, "Cancelled"); refresh++ }) { Text("إلغاء", color = MaterialTheme.colorScheme.error) }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ReportsScreen(db: YazedTeacherProDb) {
    val context = LocalContext.current
    val activity = context as? Activity
    var pendingText by remember { mutableStateOf("") }
    val createText = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("text/plain")) { uri -> uri?.let { context.contentResolver.openOutputStream(it)?.use { out -> out.write(pendingText.toByteArray(Charsets.UTF_8)) } } }
    val createCsv = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("text/csv")) { uri -> uri?.let { context.contentResolver.openOutputStream(it)?.use { out -> out.write(pendingText.toByteArray(Charsets.UTF_8)) } } }
    val backup = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/octet-stream")) { uri -> uri?.let { context.contentResolver.openOutputStream(it)?.use(db::copyDatabaseTo); Toast.makeText(context, "تم إنشاء النسخة الاحتياطية", Toast.LENGTH_SHORT).show() } }
    val restore = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri -> uri?.let { context.contentResolver.openInputStream(it)?.use(db::restoreDatabaseFrom); Toast.makeText(context, "تم الاسترجاع وسيعاد تشغيل التطبيق", Toast.LENGTH_LONG).show(); activity?.recreate() } }
    var refresh by remember { mutableIntStateOf(0) }; val report = remember(refresh) { db.monthlyReport() }
    Column(Modifier.fillMaxSize().padding(vertical = 8.dp)) {
        SectionHeader("تقرير شهر ${report.month}")
        Card(colors = CardDefaults.cardColors(containerColor = Color.White), shape = RoundedCornerShape(20.dp)) {
            Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                ReportLine("إجمالي الدخل", money(report.income)); ReportLine("إجمالي المتأخرات", money(report.debt)); ReportLine("مصروفات الشهر", money(report.expenses)); ReportLine("نصيب السناتر", money(report.centerShare)); HorizontalDivider(); ReportLine("صافي المدرس", money(report.teacherNet), true); ReportLine("الطلبة النشطين", report.students.toString()); ReportLine("المجموعات", report.groups.toString()); ReportLine("رسائل معلقة", report.queuedMessages.toString())
            }
        }
        Spacer(Modifier.height(12.dp))
        LazyVerticalGrid(columns = GridCells.Fixed(2), horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxSize()) {
            item { ActionCard("نسخة احتياطية", Icons.Default.Backup) { backup.launch("YazedTeacherPro_backup_${System.currentTimeMillis()}.db") } }
            item { ActionCard("استرجاع نسخة", Icons.Default.Restore) { restore.launch("*/*") } }
            item { ActionCard("تقرير TXT", Icons.Default.Description) { pendingText = db.reportText(); createText.launch("YazedTeacherPro_Report_${report.month}.txt") } }
            item { ActionCard("الطلبة CSV", Icons.Default.Person) { pendingText = db.studentsCsv(); createCsv.launch("students.csv") } }
            item { ActionCard("المدفوعات CSV", Icons.Default.Payment) { pendingText = db.paymentsCsv(); createCsv.launch("payments.csv") } }
            item { ActionCard("المتأخرات CSV", Icons.Default.Warning) { pendingText = db.debtsCsv(); createCsv.launch("debts_report.csv") } }
            item { ActionCard("حضور الشهر CSV", Icons.Default.CheckCircle) { pendingText = db.attendanceMonthCsv(); createCsv.launch("attendance_month.csv") } }
            item { ActionCard("واتساب CSV", Icons.Default.Message) { pendingText = db.whatsappCsv(); createCsv.launch("whatsapp_messages.csv") } }
            item { ActionCard("سجل العمليات CSV", Icons.Default.History) { pendingText = db.auditCsv(); createCsv.launch("audit_log.csv") } }
            item { ActionCard("تحديث التقرير", Icons.Default.Refresh) { refresh++ } }
        }
    }
}

@Composable
private fun ReportLine(label: String, value: String, strong: Boolean = false) { Row(Modifier.fillMaxWidth()) { Text(label, modifier = Modifier.weight(1f), fontWeight = if (strong) FontWeight.Black else FontWeight.Normal); Text(value, color = if (strong) GoldDark else Dark, fontWeight = if (strong) FontWeight.Black else FontWeight.SemiBold) } }

@Composable
private fun ActionCard(text: String, icon: ImageVector, onClick: () -> Unit) {
    Card(colors = CardDefaults.cardColors(containerColor = Color.White), shape = RoundedCornerShape(18.dp), modifier = Modifier.height(100.dp).clickable(onClick = onClick)) {
        Column(Modifier.fillMaxSize().padding(12.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) { Icon(icon, null, tint = GoldDark); Spacer(Modifier.height(6.dp)); Text(text, fontWeight = FontWeight.Bold) }
    }
}

@Composable
private fun SettingsScreen(db: YazedTeacherProDb, user: String) {
    val context = LocalContext.current
    var tab by remember { mutableIntStateOf(0) }
    Column(Modifier.fillMaxSize()) {
        TabRow(tab) { Tab(tab == 0, { tab = 0 }, text = { Text("بيانات السنتر") }); Tab(tab == 1, { tab = 1 }, text = { Text("قوالب واتساب") }) }
        if (tab == 0) {
            var businessName by remember { mutableStateOf(db.businessName()) }; var code by remember { mutableStateOf(db.getSetting("DefaultCountryCode") ?: "20") }; var trialMsg by remember { mutableStateOf((db.getSetting("ShowTrialMessageOnStartup") ?: "1") == "1") }
            Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(businessName, { businessName = it }, label = { Text("اسم السنتر / المدرس") }, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(code, { code = it }, label = { Text("كود الدولة للواتساب مثال 20") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), modifier = Modifier.fillMaxWidth())
                Row(verticalAlignment = Alignment.CenterVertically) { Checkbox(trialMsg, { trialMsg = it }); Text("إظهار رسالة النسخة التجريبية عند التشغيل") }
                Button(onClick = { db.setSetting("BusinessName", businessName.trim()); db.setSetting("DefaultCountryCode", code.trim()); db.setSetting("ShowTrialMessageOnStartup", if (trialMsg) "1" else "0"); db.audit(user, "UpdateSettings", "General settings updated"); Toast.makeText(context, "تم الحفظ", Toast.LENGTH_SHORT).show() }, modifier = Modifier.fillMaxWidth()) { Text("حفظ الإعدادات") }
            }
        } else TemplatesScreen(db, user)
    }
}

@Composable
private fun TemplatesScreen(db: YazedTeacherProDb, user: String) {
    var refresh by remember { mutableIntStateOf(0) }; val templates = remember(refresh) { db.templates() }; var edit by remember { mutableStateOf<TemplateRow?>(null) }
    Column(Modifier.fillMaxSize().padding(8.dp)) {
        Text("المتغيرات: {اسم الطالب} {المجموعة} {التاريخ} {المبلغ} {اسم الامتحان} {الدرجة} {النهاية} {النسبة} {الترتيب} {اسم السنتر}", color = Color.Gray, fontSize = 12.sp)
        Spacer(Modifier.height(8.dp))
        LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) { items(templates, key = { it.id }) { t -> Card(colors = CardDefaults.cardColors(containerColor = Color.White), shape = RoundedCornerShape(16.dp), modifier = Modifier.clickable { edit = t }) { Column(Modifier.padding(14.dp)) { Row { Text(t.type, fontWeight = FontWeight.Black, modifier = Modifier.weight(1f)); Text(if (t.active) "مفعل" else "متوقف", color = if (t.active) GoldDark else Color.Gray) }; Text(t.body, maxLines = 3) } } } }
    }
    edit?.let { t -> TemplateDialog(t, { edit = null }) { type, body, active -> db.saveTemplate(type, body, active, user); refresh++; edit = null } }
}

@Composable
private fun TemplateDialog(t: TemplateRow, onDismiss: () -> Unit, onSave: (String, String, Boolean) -> Unit) {
    var type by remember { mutableStateOf(t.type) }; var body by remember { mutableStateOf(t.body) }; var active by remember { mutableStateOf(t.active) }
    AlertDialog(onDismissRequest = onDismiss, title = { Text("قالب واتساب") }, text = { Column(verticalArrangement = Arrangement.spacedBy(8.dp)) { OutlinedTextField(type, { type = it }, label = { Text("نوع الرسالة") }, modifier = Modifier.fillMaxWidth()); OutlinedTextField(body, { body = it }, label = { Text("القالب") }, minLines = 5, modifier = Modifier.fillMaxWidth()); Row(verticalAlignment = Alignment.CenterVertically) { Checkbox(active, { active = it }); Text("مفعل") } } }, confirmButton = { Button(onClick = { onSave(type, body, active) }) { Text("حفظ") } }, dismissButton = { TextButton(onClick = onDismiss) { Text("إلغاء") } })
}

@Composable
private fun AuditScreen(db: YazedTeacherProDb) {
    var search by remember { mutableStateOf("") }; val rows = remember(search) { db.auditRows(search) }
    Column(Modifier.fillMaxSize().padding(vertical = 8.dp)) {
        OutlinedTextField(search, { search = it }, label = { Text("بحث في العملية أو التفاصيل") }, leadingIcon = { Icon(Icons.Default.Search, null) }, modifier = Modifier.fillMaxWidth())
        Spacer(Modifier.height(8.dp))
        LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) { items(rows, key = { it.id }) { a -> SimpleCard(a.action, "${a.user} • ${a.createdAt}\n${a.details}") } }
    }
}

@Composable
private fun LicenseScreen(db: YazedTeacherProDb, lockedMode: Boolean, onActivated: () -> Unit) {
    val context = LocalContext.current
    val clipboard = remember { context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager }
    var key by remember { mutableStateOf(db.getSetting("LicenseKey") ?: "") }
    var refresh by remember { mutableIntStateOf(0) }
    val validity = remember(key, refresh) { db.isLicenseValid(key) }
    val payload = remember(key, refresh) { db.licensePayload(key) }
    Box(Modifier.fillMaxSize().padding(18.dp), contentAlignment = if (lockedMode) Alignment.Center else Alignment.TopCenter) {
        Card(colors = CardDefaults.cardColors(containerColor = Color.White), shape = RoundedCornerShape(24.dp), modifier = Modifier.fillMaxWidth().widthIn(max = 620.dp)) {
            Column(Modifier.padding(20.dp)) {
                Icon(Icons.Default.Lock, null, tint = GoldDark, modifier = Modifier.size(48.dp).align(Alignment.CenterHorizontally))
                Text("التفعيل والترخيص", style = MaterialTheme.typography.titleLarge, modifier = Modifier.align(Alignment.CenterHorizontally))
                if (lockedMode) Text("انتهت الفترة التجريبية. أدخل سيريال صالح لهذا الجهاز.", color = MaterialTheme.colorScheme.error, modifier = Modifier.padding(vertical = 8.dp))
                Text("Hardware ID", fontWeight = FontWeight.Bold)
                Surface(color = Soft, shape = RoundedCornerShape(12.dp), modifier = Modifier.fillMaxWidth()) { Text(db.hardwareId(), modifier = Modifier.padding(12.dp), fontWeight = FontWeight.Black) }
                TextButton(onClick = { clipboard.setPrimaryClip(ClipData.newPlainText("Hardware ID", db.hardwareId())); Toast.makeText(context, "تم نسخ Hardware ID", Toast.LENGTH_SHORT).show() }) { Icon(Icons.Default.ContentCopy, null); Text("نسخ Hardware ID") }
                OutlinedTextField(key, { key = it }, label = { Text("مفتاح التفعيل") }, modifier = Modifier.fillMaxWidth(), minLines = 3)
                Spacer(Modifier.height(8.dp))
                Button(onClick = { val result = db.isLicenseValid(key); if (result.first) { db.saveLicense(key); refresh++; Toast.makeText(context, "تم حفظ الترخيص", Toast.LENGTH_SHORT).show(); onActivated() } else Toast.makeText(context, result.second, Toast.LENGTH_LONG).show() }, modifier = Modifier.fillMaxWidth()) { Text("حفظ التفعيل") }
                Spacer(Modifier.height(12.dp))
                Text(if (validity.first) "الحالة: مفعل" else "الحالة: غير مفعل — ${validity.second}", color = if (validity.first) Color(0xFF2E7D32) else MaterialTheme.colorScheme.error, fontWeight = FontWeight.Bold)
                payload?.let { Text("العميل: ${it.customerName}\nالخطة: ${it.plan}\nينتهي: ${it.expiryDate}\nالمميزات: ${it.features}") }
                if (!lockedMode && !validity.first) Text("التجربة المتبقية: ${db.trialDaysRemaining()} يوم", color = GoldDark)
            }
        }
    }
}

@Composable
private fun SimpleCard(title: String, subtitle: String) {
    Card(colors = CardDefaults.cardColors(containerColor = Color.White), shape = RoundedCornerShape(18.dp), modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(14.dp)) { Text(title, fontWeight = FontWeight.Black, fontSize = 17.sp); if (subtitle.isNotBlank()) Text(subtitle, color = Color.DarkGray) }
    }
}

@Composable
private fun <T> Picker(label: String, items: List<T>, selected: T?, text: (T) -> String, onSelect: (T) -> Unit) {
    var expanded by remember { mutableStateOf(false) }
    Box(Modifier.fillMaxWidth()) {
        OutlinedButton(onClick = { expanded = true }, modifier = Modifier.fillMaxWidth().heightIn(min = 52.dp), enabled = items.isNotEmpty()) {
            Text(selected?.let(text) ?: label, modifier = Modifier.weight(1f)); Icon(Icons.Default.ArrowDropDown, null)
        }
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }, modifier = Modifier.fillMaxWidth(.9f)) {
            items.forEach { item -> DropdownMenuItem(text = { Text(text(item)) }, onClick = { onSelect(item); expanded = false }) }
        }
    }
}

private fun openWhatsApp(context: Context, phone: String, message: String) {
    if (phone.isBlank()) { Toast.makeText(context, "رقم ولي الأمر غير موجود", Toast.LENGTH_SHORT).show(); return }
    val uri = Uri.parse("https://wa.me/$phone?text=${Uri.encode(message)}")
    try { context.startActivity(Intent(Intent.ACTION_VIEW, uri)) } catch (_: Exception) { Toast.makeText(context, "تعذر فتح واتساب", Toast.LENGTH_SHORT).show() }
}

private fun shareText(context: Context, text: String) {
    val intent = Intent(Intent.ACTION_SEND).apply { type = "text/plain"; putExtra(Intent.EXTRA_TEXT, text) }
    context.startActivity(Intent.createChooser(intent, "مشاركة"))
}
