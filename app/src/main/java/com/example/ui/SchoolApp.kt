package com.example.ui

import android.app.DatePickerDialog
import android.content.Context
import android.print.PrintAttributes
import android.print.PrintManager
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.Toast
import androidx.compose.animation.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.*
import java.text.SimpleDateFormat
import java.util.*

// Navigation list item helper matching our responsive system
data class NavItem(val screen: Screen, val label: String, val icon: androidx.compose.ui.graphics.vector.ImageVector)

val navItems = listOf(
    NavItem(Screen.Dashboard, "Dash", Icons.Default.Home),
    NavItem(Screen.Classes, "Classes", Icons.Default.List),
    NavItem(Screen.Students, "Students", Icons.Default.Person),
    NavItem(Screen.Attendance, "Attend", Icons.Default.Check),
    NavItem(Screen.Reports, "Reports", Icons.Default.Refresh),
    NavItem(Screen.Payments, "Fees", Icons.Default.ShoppingCart),
    NavItem(Screen.Settings, "Settings", Icons.Default.Settings)
)

@Composable
fun MainLayout(viewModel: SchoolViewModel, modifier: Modifier = Modifier) {
    val currentScreen by viewModel.currentScreen.collectAsStateWithLifecycle()
    val schoolName by viewModel.schoolName.collectAsStateWithLifecycle()
    val academicYear by viewModel.academicYear.collectAsStateWithLifecycle()

    BoxWithConstraints(modifier = modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
        val isWide = maxWidth > 600.dp
        Row(modifier = Modifier.fillMaxSize()) {
            if (isWide) {
                // Wide Screen Column Sidebar
                Surface(
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                    modifier = Modifier.width(220.dp).fillMaxHeight()
                ) {
                    Column(modifier = Modifier.padding(12.dp).verticalScroll(rememberScrollState())) {
                        Text(schoolName, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        Text(academicYear, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Divider(modifier = Modifier.padding(vertical = 12.dp))
                        navItems.forEach { item ->
                            val active = currentScreen == item.screen
                            NavigationDrawerItem(
                                icon = { Icon(item.icon, item.label) },
                                label = { Text(item.label) },
                                selected = active,
                                onClick = { viewModel.navigateTo(item.screen) },
                                modifier = Modifier.padding(vertical = 2.dp).testTag("nav_wide_${item.screen.name}")
                            )
                        }
                    }
                }
                VerticalDivider(color = MaterialTheme.colorScheme.outlineVariant)
            }

            Scaffold(
                bottomBar = {
                    if (!isWide && currentScreen != Screen.Receipt) {
                        NavigationBar(modifier = Modifier.navigationBarsPadding()) {
                            navItems.forEach { item ->
                                NavigationBarItem(
                                    icon = { Icon(item.icon, item.label) },
                                    label = { Text(item.label, maxLines = 1, overflow = TextOverflow.Ellipsis, fontSize = 10.sp) },
                                    selected = currentScreen == item.screen,
                                    onClick = { viewModel.navigateTo(item.screen) },
                                    modifier = Modifier.testTag("nav_bottom_${item.screen.name}")
                                )
                            }
                        }
                    }
                },
                modifier = Modifier.weight(1f).fillMaxHeight()
            ) { paddingValues ->
                Box(modifier = Modifier.padding(paddingValues).fillMaxSize()) {
                    AnimatedContent(targetState = currentScreen, label = "screen_tr") { screen ->
                        when (screen) {
                            Screen.Dashboard -> DashboardScreen(viewModel)
                            Screen.Classes -> ClassesScreen(viewModel)
                            Screen.Students -> StudentsScreen(viewModel)
                            Screen.Attendance -> AttendanceScreen(viewModel)
                            Screen.Reports -> ReportsScreen(viewModel)
                            Screen.Payments -> PaymentsScreen(viewModel)
                            Screen.Settings -> SettingsScreen(viewModel)
                            Screen.Receipt -> ReceiptScreen(viewModel)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun DashboardScreen(viewModel: SchoolViewModel) {
    val classes by viewModel.classes.collectAsStateWithLifecycle()
    val students by viewModel.students.collectAsStateWithLifecycle()
    val payments by viewModel.allPayments.collectAsStateWithLifecycle()
    val attendance by viewModel.allAttendance.collectAsStateWithLifecycle()

    val totalStudents = students.size
    val totalClasses = classes.size
    val collectedForMay = remember(payments) { payments.filter { it.month == "May" && it.year == "2026" }.sumOf { it.amount } }
    val expectedForMay = remember(students) { students.sumOf { it.monthlyFee } }

    val rate = remember(attendance) {
        if (attendance.isEmpty()) 100 else {
            val pres = attendance.count { it.status == "PRESENT" || it.status == "LATE" }
            (pres.toDouble() / attendance.size * 100).toInt()
        }
    }

    LazyColumn(contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        item {
            Column {
                Text("Dashboard", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Black)
                Text("Student hub & Financial Health Overview", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }

        item {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Column(modifier = Modifier.weight(1f)) {
                    StatsCard("Total Students", totalStudents.toString(), Icons.Default.Person, MaterialTheme.colorScheme.primaryContainer)
                    Spacer(modifier = Modifier.height(12.dp))
                    StatsCard("Avg. Attendance", "$rate%", Icons.Default.Check, MaterialTheme.colorScheme.tertiaryContainer)
                }
                Column(modifier = Modifier.weight(1f)) {
                    StatsCard("Active Classes", totalClasses.toString(), Icons.Default.List, MaterialTheme.colorScheme.secondaryContainer)
                    Spacer(modifier = Modifier.height(12.dp))
                    StatsCard("May Collection", String.format("₹%.0f/₹%.0f", collectedForMay, expectedForMay), Icons.Default.ShoppingCart, Color(0xFFE8F5E9))
                }
            }
        }

        item {
            ElevatedCard(shape = RoundedCornerShape(12.dp)) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("Low Attendance Warnings (<75%)", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.error)
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    val warnings = remember(students, attendance) {
                        students.mapNotNull { st ->
                            val att = attendance.filter { it.studentId == st.id }
                            if (att.size >= 3) {
                                val pre = att.count { it.status == "PRESENT" || it.status == "LATE" }
                                val pRate = (pre.toDouble() / att.size * 100).toInt()
                                if (pRate < 75) Pair(st, pRate) else null
                            } else null
                        }
                    }

                    if (warnings.isEmpty()) {
                        Text("All students have healthy attendance rates.", style = MaterialTheme.typography.bodyMedium)
                    } else {
                        warnings.forEach { (st, pct) ->
                            Row(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text(st.name, fontWeight = FontWeight.Medium)
                                Text("$pct% Rate", color = MaterialTheme.colorScheme.error, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun StatsCard(title: String, valStr: String, icon: androidx.compose.ui.graphics.vector.ImageVector, containerColor: Color) {
    Card(colors = CardDefaults.cardColors(containerColor = containerColor), modifier = Modifier.fillMaxWidth()) {
        Row(modifier = Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(icon, title, modifier = Modifier.size(28.dp))
            Spacer(modifier = Modifier.width(10.dp))
            Column {
                Text(title, style = MaterialTheme.typography.bodySmall, maxLines = 1)
                Text(valStr, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
fun ClassesScreen(viewModel: SchoolViewModel) {
    val list by viewModel.classes.collectAsStateWithLifecycle()
    val students by viewModel.students.collectAsStateWithLifecycle()
    var showAddD by remember { mutableStateOf(false) }
    var className by remember { mutableStateOf("") }
    var classSub by remember { mutableStateOf("") }
    var expandedId by remember { mutableStateOf<Int?>(null) }

    Scaffold(
        floatingActionButton = {
            FloatingActionButton(onClick = { showAddD = true }) { Icon(Icons.Default.Add, "Add Class") }
        }
    ) { p ->
        LazyColumn(contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.padding(p).fillMaxSize()) {
            item {
                Text("Classes/Subjects", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
            }

            if (list.isEmpty()) {
                item { Text("No classes established. Add a class below.", modifier = Modifier.padding(16.dp), textAlign = TextAlign.Center) }
            } else {
                items(list, key = { it.id }) { cls ->
                    val enrolled = students.filter { it.classId == cls.id }
                    Card(modifier = Modifier.fillMaxWidth().clickable { expandedId = if (expandedId == cls.id) null else cls.id }) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Row(horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(cls.name, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                                    Text("Course: ${cls.subject} • ${enrolled.size} Enrolled", style = MaterialTheme.typography.bodySmall)
                                }
                                Row {
                                    IconButton(onClick = { viewModel.deleteClass(cls) }) { Icon(Icons.Default.Delete, "Delete", tint = MaterialTheme.colorScheme.error) }
                                    Icon(if (expandedId == cls.id) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown, "Arrow")
                                }
                            }
                            if (expandedId == cls.id) {
                                Divider(modifier = Modifier.padding(vertical = 8.dp))
                                if (enrolled.isEmpty()) {
                                    Text("No students in class.", style = MaterialTheme.typography.bodySmall)
                                } else {
                                    enrolled.forEach { st ->
                                        Row(modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                                            Text("${st.rollNumber} • ${st.name}", fontSize = 14.sp)
                                            Text(String.format("₹%.1f/mo", st.monthlyFee), fontSize = 14.sp, color = MaterialTheme.colorScheme.secondary)
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    if (showAddD) {
        Dialog(onDismissRequest = { showAddD = false }) {
            Surface(shape = RoundedCornerShape(12.dp), color = MaterialTheme.colorScheme.surface, modifier = Modifier.padding(16.dp)) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("Add Class & Course", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    OutlinedTextField(value = className, onValueChange = { className = it }, label = { Text("Class Name (e.g. Class 10-A)") })
                    OutlinedTextField(value = classSub, onValueChange = { classSub = it }, label = { Text("Subject (e.g. Calculus)") })
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                        TextButton(onClick = { showAddD = false }) { Text("Cancel") }
                        Button(onClick = {
                            if (className.isNotBlank() && classSub.isNotBlank()) {
                                viewModel.addClass(className, classSub)
                                className = ""; classSub = ""
                                showAddD = false
                            }
                        }) { Text("Create") }
                    }
                }
            }
        }
    }
}

@Composable
fun StudentsScreen(viewModel: SchoolViewModel) {
    val students by viewModel.students.collectAsStateWithLifecycle()
    val classes by viewModel.classes.collectAsStateWithLifecycle()
    var searchTxt by remember { mutableStateOf("") }
    var filterClassId by remember { mutableStateOf<Int?>(null) }
    var showAddS by remember { mutableStateOf(false) }

    var nameInput by remember { mutableStateOf("") }
    var rollInput by remember { mutableStateOf("") }
    var feeInput by remember { mutableStateOf("") }
    var selectedClassId by remember { mutableStateOf<Int?>(null) }

    Scaffold(
        floatingActionButton = {
            FloatingActionButton(onClick = {
                if (classes.isNotEmpty()) {
                    selectedClassId = classes[0].id
                    showAddS = true
                }
            }) { Icon(Icons.Default.Add, "Add Student") }
        }
    ) { p ->
        Column(modifier = Modifier.padding(p).fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text("Students", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
            
            OutlinedTextField(value = searchTxt, onValueChange = { searchTxt = it }, label = { Text("Search by name or roll...") }, modifier = Modifier.fillMaxWidth())

            // Class filters
            Row(modifier = Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                FilterChip(selected = filterClassId == null, onClick = { filterClassId = null }, label = { Text("All Classes") })
                classes.forEach { cls ->
                    FilterChip(selected = filterClassId == cls.id, onClick = { filterClassId = cls.id }, label = { Text(cls.name) })
                }
            }

            val filtered = remember(students, searchTxt, filterClassId) {
                students.filter { st ->
                    val matchesClass = filterClassId == null || st.classId == filterClassId
                    val matchesSearch = st.name.contains(searchTxt, true) || st.rollNumber.contains(searchTxt, true)
                    matchesClass && matchesSearch
                }
            }

            LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.weight(1f).fillMaxWidth()) {
                if (filtered.isEmpty()) {
                    item { Text("No matching students loaded.", modifier = Modifier.padding(16.dp)) }
                } else {
                    items(filtered, key = { it.id }) { st ->
                        val parentCls = classes.find { it.id == st.classId }
                        Card(modifier = Modifier.fillMaxWidth()) {
                            Row(modifier = Modifier.padding(12.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                                Column {
                                    Text(st.name, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                                    Text("Roll: ${st.rollNumber} • ${parentCls?.name ?: "No Class"}", style = MaterialTheme.typography.bodySmall)
                                    Text(String.format("Fee Rate: ₹%.2f / mo", st.monthlyFee), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.primary)
                                }
                                IconButton(onClick = { viewModel.deleteStudent(st.id) }) {
                                    Icon(Icons.Default.Delete, "Delete Student", tint = MaterialTheme.colorScheme.error)
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    if (showAddS) {
        Dialog(onDismissRequest = { showAddS = false }) {
            Surface(shape = RoundedCornerShape(12.dp), color = MaterialTheme.colorScheme.surface, modifier = Modifier.padding(16.dp)) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("Register Student", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    
                    OutlinedTextField(value = nameInput, onValueChange = { nameInput = it }, label = { Text("Full Name") })
                    OutlinedTextField(value = rollInput, onValueChange = { rollInput = it }, label = { Text("Roll Number") })
                    OutlinedTextField(value = feeInput, onValueChange = { feeInput = it }, label = { Text("Monthly Fee (₹)") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number))
                    
                    Text("Select Class:", style = MaterialTheme.typography.bodySmall)
                    Row(modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        classes.forEach { cls ->
                            FilterChip(selected = selectedClassId == cls.id, onClick = { selectedClassId = cls.id }, label = { Text(cls.name) })
                        }
                    }

                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                        TextButton(onClick = { showAddS = false }) { Text("Cancel") }
                        Button(onClick = {
                            val feeVal = feeInput.toDoubleOrNull() ?: 0.0
                            val clsId = selectedClassId
                            if (nameInput.isNotBlank() && rollInput.isNotBlank() && clsId != null) {
                                viewModel.addStudent(clsId, nameInput, rollInput, feeVal)
                                nameInput = ""; rollInput = ""; feeInput = ""
                                showAddS = false
                            }
                        }) { Text("Register") }
                    }
                }
            }
        }
    }
}

@Composable
fun AttendanceScreen(viewModel: SchoolViewModel) {
    val classes by viewModel.classes.collectAsStateWithLifecycle()
    val students by viewModel.students.collectAsStateWithLifecycle()
    val currentSelectedClass by viewModel.selectedClassForAttendance.collectAsStateWithLifecycle()
    val date by viewModel.attendanceDate.collectAsStateWithLifecycle()
    val context = LocalContext.current

    // Local dictionary tracker for active selections
    val attendanceStates = remember { mutableStateMapOf<Int, String>() }

    LaunchedEffect(currentSelectedClass) {
        attendanceStates.clear()
    }

    Column(modifier = Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text("Mark Daily Attendance", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)

        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
            // Class selector Row Chips
            Box(modifier = Modifier.weight(1f).horizontalScroll(rememberScrollState())) {
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    classes.forEach { cls ->
                        FilterChip(
                            selected = currentSelectedClass?.id == cls.id,
                            onClick = { viewModel.selectedClassForAttendance.value = cls },
                            label = { Text(cls.name) }
                        )
                    }
                }
            }
            
            // Date Picker Clickable Button
            Button(onClick = {
                val cal = Calendar.getInstance()
                val picker = DatePickerDialog(context, { _, y, m, d ->
                    val format = String.format(Locale.getDefault(), "%04d-%02d-%02d", y, m + 1, d)
                    viewModel.attendanceDate.value = format
                }, cal.get(Calendar.YEAR), cal.get(Calendar.MONTH), cal.get(Calendar.DAY_OF_MONTH))
                picker.show()
            }) {
                Icon(Icons.Default.DateRange, date)
                Spacer(modifier = Modifier.width(4.dp))
                Text(date)
            }
        }

        if (currentSelectedClass == null) {
            Text("Select class first.", modifier = Modifier.align(Alignment.CenterHorizontally).padding(24.dp))
        } else {
            val enrolled = remember(students, currentSelectedClass) { students.filter { it.classId == currentSelectedClass!!.id } }
            
            LazyColumn(modifier = Modifier.weight(1f).fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                if (enrolled.isEmpty()) {
                    item { Text("No student roster found.", modifier = Modifier.padding(16.dp)) }
                } else {
                    items(enrolled, key = { it.id }) { st ->
                        val currentStatus = attendanceStates[st.id] ?: "PRESENT"
                        Card(modifier = Modifier.fillMaxWidth()) {
                            Row(modifier = Modifier.padding(12.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                                Column(modifier = Modifier.width(100.dp)) {
                                    Text(st.name, fontWeight = FontWeight.Bold)
                                    Text("Roll: ${st.rollNumber}", style = MaterialTheme.typography.bodySmall)
                                }
                                
                                // Color-coded buttons row
                                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                    AttendanceSegmentButton("P", active = currentStatus == "PRESENT", activeColor = Color(0xFF2E7D32)) {
                                        attendanceStates[st.id] = "PRESENT"
                                    }
                                    AttendanceSegmentButton("L", active = currentStatus == "LATE", activeColor = Color(0xFFF9A825)) {
                                        attendanceStates[st.id] = "LATE"
                                    }
                                    AttendanceSegmentButton("A", active = currentStatus == "ABSENT", activeColor = Color(0xFFC62828)) {
                                        attendanceStates[st.id] = "ABSENT"
                                    }
                                }
                            }
                        }
                    }
                }
            }

            if (enrolled.isNotEmpty()) {
                Button(
                    onClick = {
                        val recordList = enrolled.map { st ->
                            AttendanceRecord(
                                studentId = st.id,
                                classId = currentSelectedClass!!.id,
                                date = date,
                                status = attendanceStates[st.id] ?: "PRESENT"
                            )
                        }
                        viewModel.recordBulkAttendance(recordList)
                        Toast.makeText(context, "Attendance Sheet Saved Successfully!", Toast.LENGTH_SHORT).show()
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Default.Check, "Save")
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Save Attendance Sheet")
                }
            }
        }
    }
}

@Composable
fun AttendanceSegmentButton(label: String, active: Boolean, activeColor: Color, onClick: () -> Unit) {
    Button(
        onClick = onClick,
        colors = ButtonDefaults.buttonColors(
            containerColor = if (active) activeColor else MaterialTheme.colorScheme.surfaceVariant,
            contentColor = if (active) Color.White else MaterialTheme.colorScheme.onSurfaceVariant
        ),
        shape = RoundedCornerShape(8.dp),
        contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp),
        modifier = Modifier.height(40.dp)
    ) {
        Text(label, fontWeight = FontWeight.Bold)
    }
}

@Composable
fun ReportsScreen(viewModel: SchoolViewModel) {
    val classes by viewModel.classes.collectAsStateWithLifecycle()
    val students by viewModel.students.collectAsStateWithLifecycle()
    val allAttendance by viewModel.allAttendance.collectAsStateWithLifecycle()
    
    var activeTab by remember { mutableStateOf(0) } // 0 = Class Sheets, 1 = student percentage summaries
    var selectedClassReport by remember { mutableStateOf<SchoolClass?>(null) }
    var reportDateString by remember { mutableStateOf("2026-05-30") }
    val context = LocalContext.current

    Column(modifier = Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Text("Attendance & Statistics Reports", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)

        TabRow(selectedTabIndex = activeTab) {
            Tab(selected = activeTab == 0, onClick = { activeTab = 0 }) { Text("Daily Logs", modifier = Modifier.padding(12.dp)) }
            Tab(selected = activeTab == 1, onClick = { activeTab = 1 }) { Text("Student Ledger", modifier = Modifier.padding(12.dp)) }
        }

        // Class list select
        Row(modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            classes.forEach { cls ->
                FilterChip(selected = selectedClassReport?.id == cls.id, onClick = { selectedClassReport = cls }, label = { Text(cls.name) })
            }
        }

        if (selectedClassReport == null) {
            Text("Select class to display report diagnostics.", modifier = Modifier.padding(16.dp))
        } else {
            val enrolled = remember(students, selectedClassReport) { students.filter { it.classId == selectedClassReport!!.id } }
            
            if (activeTab == 0) {
                // Class Sheet review screen
                Row(horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                    Text("Date Filter:", fontWeight = FontWeight.Bold)
                    Button(onClick = {
                        val cal = Calendar.getInstance()
                        val picker = DatePickerDialog(context, { _, y, m, d ->
                            reportDateString = String.format(Locale.getDefault(), "%04d-%02d-%02d", y, m + 1, d)
                        }, cal.get(Calendar.YEAR), cal.get(Calendar.MONTH), cal.get(Calendar.DAY_OF_MONTH))
                        picker.show()
                    }) {
                        Text(reportDateString)
                    }
                }

                val currentSheet = remember(allAttendance, selectedClassReport, reportDateString) {
                    allAttendance.filter { it.classId == selectedClassReport!!.id && it.date == reportDateString }
                }

                if (currentSheet.isEmpty()) {
                    Text("No attendance recorded on this date.", modifier = Modifier.padding(16.dp))
                } else {
                    LazyColumn(verticalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.weight(1f).fillMaxWidth()) {
                        items(enrolled) { st ->
                            val record = currentSheet.find { it.studentId == st.id }
                            val (badgeName, badgeColor) = when (record?.status) {
                                "PRESENT" -> "Present" to Color(0xFF2E7D32)
                                "LATE" -> "Late" to Color(0xFFF9A825)
                                "ABSENT" -> "Absent" to Color(0xFFC62828)
                                else -> "Unreported" to Color.Gray
                            }

                            Card(modifier = Modifier.fillMaxWidth()) {
                                Row(modifier = Modifier.padding(12.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                                    Column {
                                        Text(st.name, fontWeight = FontWeight.Bold)
                                        Text("Roll: ${st.rollNumber}", fontSize = 12.sp)
                                    }
                                    Badge(containerColor = badgeColor) {
                                        Text(badgeName, color = Color.White, modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp), fontSize = 12.sp)
                                    }
                                }
                            }
                        }
                    }
                }
            } else {
                // Percentage summary ledger
                LazyColumn(verticalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.weight(1f).fillMaxWidth()) {
                    items(enrolled) { st ->
                        val history = allAttendance.filter { it.studentId == st.id }
                        val totalDays = history.size
                        val presentDays = history.count { it.status == "PRESENT" || it.status == "LATE" }
                        val pct = if (totalDays == 0) 100 else (presentDays.toDouble() / totalDays * 100).toInt()
                        val underLimit = pct < 75

                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = if (underLimit) CardDefaults.cardColors(containerColor = Color(0xFFFFEBEE)) else CardDefaults.cardColors()
                        ) {
                            Row(modifier = Modifier.padding(12.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                                Column {
                                    Text(st.name, fontWeight = FontWeight.Bold, color = if (underLimit) Color(0xFFC62828) else Color.Unspecified)
                                    Text("Attendance Score: $presentDays / $totalDays Recorded Days", fontSize = 12.sp, color = if (underLimit) Color(0xFFC62828) else Color.Unspecified)
                                }
                                Column(horizontalAlignment = Alignment.End) {
                                    Text("$pct%", fontWeight = FontWeight.Black, fontSize = 18.sp, color = if (underLimit) Color(0xFFC62828) else Color(0xFF2E7D32))
                                    if (underLimit) {
                                        Text("Low! (<75%)", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Color(0xFFC62828))
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun PaymentsScreen(viewModel: SchoolViewModel) {
    val students by viewModel.students.collectAsStateWithLifecycle()
    val classes by viewModel.classes.collectAsStateWithLifecycle()
    val allPayments by viewModel.allPayments.collectAsStateWithLifecycle()
    
    var feeMonth by remember { mutableStateOf("May") }
    var feeYear by remember { mutableStateOf("2026") }
    var searchQuery by remember { mutableStateOf("") }
    
    var recordingPaymentStudent by remember { mutableStateOf<Student?>(null) }
    var historyStudent by remember { mutableStateOf<Student?>(null) }

    // Recorded Modal Fields
    var collectionAmt by remember { mutableStateOf("") }
    var collectionNote by remember { mutableStateOf("") }

    val totalExpected = remember(students) { students.sumOf { it.monthlyFee } }
    val totalCollected = remember(allPayments, feeMonth, feeYear) {
        allPayments.filter { it.month == feeMonth && it.year == feeYear }.sumOf { it.amount }
    }
    val totalPending = remember(totalExpected, totalCollected) {
        val b = totalExpected - totalCollected
        if (b < 0) 0.0 else b
    }

    Column(modifier = Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Text("Fees & Payments Dashboard", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)

        // Financial summary banner
        Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer), modifier = Modifier.fillMaxWidth()) {
            Row(modifier = Modifier.padding(14.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Column {
                    Text("Expected Fees for $feeMonth $feeYear", style = MaterialTheme.typography.titleSmall)
                    Text(String.format("Collected: ₹%.0f • Pending: ₹%.0f", totalCollected, totalPending), fontWeight = FontWeight.Bold)
                }
                Text(String.format("₹%.0f", totalExpected), style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Black)
            }
        }

        OutlinedTextField(value = searchQuery, onValueChange = { searchQuery = it }, label = { Text("Filter student by name...") }, modifier = Modifier.fillMaxWidth())

        val filteredPatients = remember(students, searchQuery) {
            students.filter { it.name.contains(searchQuery, true) }
        }

        LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.weight(1f).fillMaxWidth()) {
            items(filteredPatients) { st ->
                val monthPayments = allPayments.filter { it.studentId == st.id && it.month == feeMonth && it.year == feeYear }
                val paidAmt = monthPayments.sumOf { it.amount }
                val remBal = st.monthlyFee - paidAmt
                val parentCls = classes.find { it.id == st.classId }

                val (statusText, statusColor) = when {
                    paidAmt >= st.monthlyFee -> "Paid" to Color(0xFF2E7D32)
                    paidAmt > 0 -> "Partial" to Color(0xFFF9A825)
                    else -> "Unpaid" to Color(0xFFC62828)
                }

                Card(modifier = Modifier.fillMaxWidth()) {
                    Row(modifier = Modifier.padding(12.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                        Column {
                            Text(st.name, fontWeight = FontWeight.Bold)
                            Text("Class: ${parentCls?.name ?: "N/A"}", fontSize = 12.sp)
                            Text(String.format("Charged: ₹%.1f • Paid: ₹%.1f", st.monthlyFee, paidAmt), fontSize = 12.sp)
                            if (remBal > 0) {
                                Text(String.format("Bal Owed: ₹%.1f", remBal), fontSize = 12.sp, color = Color(0xFFC62828), fontWeight = FontWeight.Bold)
                            }
                        }

                        Column(horizontalAlignment = Alignment.End) {
                            Badge(containerColor = statusColor) {
                                Text(statusText, color = Color.White, modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp), fontWeight = FontWeight.Bold)
                            }
                            Spacer(modifier = Modifier.height(8.dp))
                            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                TextButton(onClick = { historyStudent = st }, contentPadding = PaddingValues(horizontal = 8.dp)) { Text("Logs") }
                                Button(onClick = {
                                    recordingPaymentStudent = st
                                    collectionAmt = st.monthlyFee.toString()
                                }, contentPadding = PaddingValues(horizontal = 8.dp)) { Text("Pay") }
                            }
                        }
                    }
                }
            }
        }
    }

    // Modal to record payment
    if (recordingPaymentStudent != null) {
        val student = recordingPaymentStudent!!
        Dialog(onDismissRequest = { recordingPaymentStudent = null }) {
            Surface(shape = RoundedCornerShape(12.dp), color = MaterialTheme.colorScheme.surface, modifier = Modifier.padding(16.dp)) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("Collect Fee payment", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    Text("Student: ${student.name} • Roll: ${student.rollNumber}", fontSize = 12.sp)

                    OutlinedTextField(value = collectionAmt, onValueChange = { collectionAmt = it }, label = { Text("Payment Amount (₹)") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number))
                    OutlinedTextField(value = collectionNote, onValueChange = { collectionNote = it }, label = { Text("Reference Note (e.g. Cash, G-Pay)") })

                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        OutlinedTextField(value = feeMonth, onValueChange = { feeMonth = it }, label = { Text("Month") }, modifier = Modifier.weight(1f))
                        OutlinedTextField(value = feeYear, onValueChange = { feeYear = it }, label = { Text("Year") }, modifier = Modifier.weight(1f))
                    }

                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                        TextButton(onClick = { recordingPaymentStudent = null }) { Text("Cancel") }
                        Button(onClick = {
                            val paidVal = collectionAmt.toDoubleOrNull() ?: 0.0
                            viewModel.recordPayment(student.id, paidVal, feeMonth, feeYear, collectionNote)
                            recordingPaymentStudent = null
                            collectionNote = ""
                        }) { Text("Confirm & Print") }
                    }
                }
            }
        }
    }

    // Modal to view history logs
    if (historyStudent != null) {
        val student = historyStudent!!
        val studentHistory = allPayments.filter { it.studentId == student.id }
        Dialog(onDismissRequest = { historyStudent = null }) {
            Surface(shape = RoundedCornerShape(12.dp), color = MaterialTheme.colorScheme.surface, modifier = Modifier.fillMaxWidth().padding(16.dp)) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Payment Logs: ${student.name}", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    
                    Divider()
                    
                    if (studentHistory.isEmpty()) {
                        Text("No transaction history recorded.")
                    } else {
                        LazyColumn(modifier = Modifier.heightIn(max = 280.dp)) {
                            items(studentHistory) { p ->
                                Card(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
                                    Row(modifier = Modifier.padding(8.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                                        Column {
                                            Text(String.format("₹%.1f for ${p.month} ${p.year}", p.amount), fontWeight = FontWeight.Bold)
                                            Text("Rec No: #${p.receiptNumber} • ${p.note}", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                        }
                                        IconButton(onClick = { viewModel.deletePayment(p.id) }) { Icon(Icons.Default.Delete, "Refund", tint = MaterialTheme.colorScheme.error) }
                                    }
                                }
                            }
                        }
                    }
                    Button(onClick = { historyStudent = null }, modifier = Modifier.align(Alignment.End)) { Text("Done") }
                }
            }
        }
    }
}

@Composable
fun ReceiptScreen(viewModel: SchoolViewModel) {
    val activeReceipt by viewModel.activeReceipt.collectAsStateWithLifecycle()
    val students by viewModel.students.collectAsStateWithLifecycle()
    val classes by viewModel.classes.collectAsStateWithLifecycle()
    val schoolName by viewModel.schoolName.collectAsStateWithLifecycle()
    val academicYear by viewModel.academicYear.collectAsStateWithLifecycle()
    val context = LocalContext.current

    if (activeReceipt == null) {
        Text("No active receipt selected.", modifier = Modifier.padding(24.dp))
    } else {
        val receipt = activeReceipt!!
        val student = remember(students, receipt) { students.find { it.id == receipt.studentId } } ?: Student(name = "Unknown Student", classId = 0, rollNumber = "00", monthlyFee = 0.0)
        val parentClass = remember(classes, student) { classes.find { it.id == student.classId } }
        val remBal = student.monthlyFee - receipt.amount
        val displayBal = if (remBal < 0) 0.0 else remBal

        Column(modifier = Modifier.fillMaxSize().padding(16.dp).verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(16.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                IconButton(onClick = { viewModel.navigateTo(Screen.Payments) }) { Icon(Icons.Default.ArrowBack, "Back") }
                Text("Transaction Receipt", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.width(48.dp))
            }

            // High Fidelity printable receipt visual
            Surface(
                modifier = Modifier.fillMaxWidth().border(2.dp, Color.Black, RoundedCornerShape(8.dp)),
                color = Color.White,
                shape = RoundedCornerShape(8.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    // Header banner
                    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
                        Text(schoolName, fontWeight = FontWeight.Black, fontSize = 20.sp, color = Color(0xFF1976D2), textAlign = TextAlign.Center)
                        Text("Academic Year: $academicYear", fontSize = 11.sp, color = Color.DarkGray)
                        Spacer(modifier = Modifier.height(4.dp))
                        Text("OFFICIAL FEE RECEIPT", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold, modifier = Modifier.border(1.dp, Color.Black).padding(6.dp))
                    }

                    Spacer(modifier = Modifier.height(10.dp))
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("Receipt No: #${receipt.receiptNumber}", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                        val sdf = SimpleDateFormat("dd-MM-yyyy hh:mm a", Locale.getDefault())
                        Text("Date: ${sdf.format(Date(receipt.paymentDate))}", fontSize = 12.sp)
                    }
                    Divider(color = Color.Black)

                    ReceiptLine("Student Name:", student.name)
                    ReceiptLine("Roll Number:", student.rollNumber)
                    ReceiptLine("Class / Group:", "${parentClass?.name ?: "N/A"} (${parentClass?.subject ?: ""})")
                    Divider(color = Color.LightGray)

                    Column(modifier = Modifier.background(Color(0xFFF5F5F5)).padding(10.dp).fillMaxWidth()) {
                        Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                            Text("Standard Tuition fee:")
                            Text(String.format("₹%.2f", student.monthlyFee))
                        }
                        Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                            Text("Billed Term:")
                            Text("${receipt.month} ${receipt.year}", fontWeight = FontWeight.Bold, color = Color(0xFF1976D2))
                        }
                        Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                            Text("Amount Collected:", fontWeight = FontWeight.Bold)
                            Text(String.format("₹%.2f", receipt.amount), fontWeight = FontWeight.Bold, color = Color(0xFF2E7D32), fontSize = 17.sp)
                        }
                        Divider(modifier = Modifier.padding(vertical = 4.dp))
                        Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                            Text("Outstanding Balance Outstanding:")
                            Text(String.format("₹%.2f", displayBal), color = Color(0xFFD32F2F), fontWeight = FontWeight.Bold)
                        }
                    }

                    if (receipt.note.isNotBlank()) {
                        Text("Note: ${receipt.note}", style = MaterialTheme.typography.bodySmall, color = Color.Gray)
                    }

                    Divider(color = Color.Black)
                    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
                        Text("Thank you for your valuable contribution!", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                        Text("This is an official computer-generated receipt voucher.", fontSize = 10.sp, color = Color.Gray)
                    }
                }
            }

            Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth()) {
                OutlinedButton(onClick = { viewModel.navigateTo(Screen.Payments) }, modifier = Modifier.weight(1f)) {
                    Text("Close Receipt")
                }
                Button(
                    onClick = {
                        printReceiptHtml(
                            context = context,
                            schoolName = schoolName,
                            academicYear = academicYear,
                            receipt = receipt,
                            student = student,
                            classObj = parentClass
                        )
                    },
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(Icons.Default.Share, "Print")
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Trigger Print")
                }
            }
        }
    }
}

@Composable
fun ReceiptLine(label: String, valStr: String) {
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(label, color = Color.DarkGray, fontSize = 13.sp)
        Text(valStr, fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
    }
}

@Composable
fun SettingsScreen(viewModel: SchoolViewModel) {
    val schoolName by viewModel.schoolName.collectAsStateWithLifecycle()
    val academicYear by viewModel.academicYear.collectAsStateWithLifecycle()
    val context = LocalContext.current

    var nameInput by remember { mutableStateOf("") }
    var yearInput by remember { mutableStateOf("") }

    LaunchedEffect(schoolName, academicYear) {
        nameInput = schoolName
        yearInput = academicYear
    }

    Column(modifier = Modifier.fillMaxSize().padding(16.dp).verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Text("App Settings", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)

        Card(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text("School Info (Appeared on Receipts)", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                
                OutlinedTextField(value = nameInput, onValueChange = { nameInput = it }, label = { Text("School/Institute Name") }, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(value = yearInput, onValueChange = { yearInput = it }, label = { Text("Academic Year") }, modifier = Modifier.fillMaxWidth())

                Button(
                    onClick = {
                        viewModel.updateSettings(nameInput, yearInput)
                        Toast.makeText(context, "Settings updated successfully!", Toast.LENGTH_SHORT).show()
                    },
                    modifier = Modifier.align(Alignment.End)
                ) {
                    Text("Save Changes")
                }
            }
        }

        Spacer(modifier = Modifier.height(10.dp))
        Text("Developer & Administrator Utilities", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
            OutlinedButton(
                onClick = {
                    viewModel.deleteEverything()
                    Toast.makeText(context, "Database Completely Cleared", Toast.LENGTH_SHORT).show()
                },
                modifier = Modifier.weight(1f),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error)
            ) {
                Text("Clear Database")
            }

            Button(
                onClick = {
                    viewModel.populateSampleData()
                    Toast.makeText(context, "Sample student rosters loaded!", Toast.LENGTH_SHORT).show()
                },
                modifier = Modifier.weight(1f)
            ) {
                Text("Load Mock Rosters")
            }
        }
    }
}

// Android system print dialog driver utilizing HTML WebViews
fun printReceiptHtml(
    context: Context,
    schoolName: String,
    academicYear: String,
    receipt: PaymentRecord,
    student: Student,
    classObj: SchoolClass?
) {
    val className = classObj?.name ?: "Unknown Class"
    val classSubject = classObj?.subject ?: "Default Course"
    val sdf = SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale.getDefault())
    val formattedDate = sdf.format(Date(receipt.paymentDate))
    val remainingBalance = student.monthlyFee - receipt.amount
    val balanceToDisplay = if (remainingBalance < 0) 0.0 else remainingBalance
    
    val htmlContent = """
        <!DOCTYPE html>
        <html>
        <head>
        <meta charset="utf-8">
        <title>Receipt - ${receipt.receiptNumber}</title>
        <style>
          body { font-family: 'Helvetica Neue', Arial, sans-serif; padding: 24px; color: #202124; background-color: #ffffff; }
          .receipt-box { max-width: 440px; margin: auto; border: 2px solid #000000; padding: 20px; background: #ffffff; }
          .header { text-align: center; border-bottom: 2px dashed #000000; padding-bottom: 12px; margin-bottom: 16px; }
          .school-name { font-size: 20px; font-weight: bold; margin: 0 0 2px 0; text-transform: uppercase; }
          .school-sub { font-size: 12px; color: #5f6368; margin: 0; }
          .receipt-title { font-size: 14px; font-weight: bold; margin-top: 10px; border: 1.5px solid #000000; display: inline-block; padding: 4px 12px; background: #f1f3f4; }
          .details-row { display: flex; justify-content: space-between; margin: 6px 0; font-size: 13px; }
          .divider { border-top: 1.5px solid #000000; margin: 12px 0; }
          .fee-block { background: #f8f9fa; border: 1px solid #dae0e5; padding: 10px; margin: 12px 0; }
          .amount-row { display: flex; justify-content: space-between; font-size: 13px; margin: 4px 0; }
          .amount-total { font-weight: bold; font-size: 15px; border-top: 1.5px solid #000000; padding-top: 6px; margin-top: 6px; }
          .footer { text-align: center; font-size: 11px; color: #5f6368; margin-top: 18px; border-top: 2px dashed #000000; padding-top: 12px; }
        </style>
        </head>
        <body>
          <div class="receipt-box">
            <div class="header">
              <div class="school-name">$schoolName</div>
              <div class="school-sub">Academic Year: $academicYear</div>
              <div class="receipt-title">FEE RECEIPT RECORD</div>
            </div>
            
            <div class="details-row">
              <div><strong>Receipt No:</strong> #${receipt.receiptNumber}</div>
              <div><strong>Date:</strong> $formattedDate</div>
            </div>
            
            <div class="divider"></div>
            
            <div class="details-row"><span>Student Name:</span> <strong>${student.name}</strong></div>
            <div class="details-row"><span>Roll Number:</span> <strong>${student.rollNumber}</strong></div>
            <div class="details-row"><span>Class & Subject:</span> <strong>$className ($classSubject)</strong></div>
            
            <div class="divider"></div>
            
            <div class="fee-block">
              <div class="amount-row"><span>Standard Monthly Fee:</span> <span>$${String.format(Locale.getDefault(), "%.2f", student.monthlyFee)}</span></div>
              <div class="amount-row"><span>Billed Statement Term:</span> <strong>${receipt.month} ${receipt.year}</strong></div>
              <div class="amount-row" style="color: #137333; font-weight: bold;"><span>Amount Billed Paid:</span> <span>$${String.format(Locale.getDefault(), "%.2f", receipt.amount)}</span></div>
              <div class="amount-row amount-total" style="color: #b06000;"><span>Remaining Balance Outstanding:</span> <span>$${String.format(Locale.getDefault(), "%.2f", balanceToDisplay)}</span></div>
            </div>
            
            <div class="details-row" style="font-size: 12px; font-style: italic;">
              <span>Reference Term Notes:</span>
              <span>${if (receipt.note.isNotBlank()) receipt.note else "None"}</span>
            </div>
            
            <div class="footer">
              <div style="font-weight: bold; margin-bottom: 2px;">★ Thank You for Your Contribution ★</div>
              <div>This is an official transaction voucher processed digitally via Student Hub.</div>
            </div>
          </div>
        </body>
        </html>
    """.trimIndent()

    val webView = WebView(context)
    webView.webViewClient = object : WebViewClient() {
        override fun onPageFinished(view: WebView?, url: String?) {
            val printManager = context.getSystemService(Context.PRINT_SERVICE) as PrintManager
            val jobName = "Receipt_${receipt.receiptNumber}_StudentHub"
            val printAdapter = webView.createPrintDocumentAdapter(jobName)
            printManager.print(jobName, printAdapter, PrintAttributes.Builder().build())
        }
    }
    webView.loadDataWithBaseURL(null, htmlContent, "text/html", "UTF-8", null)
}
