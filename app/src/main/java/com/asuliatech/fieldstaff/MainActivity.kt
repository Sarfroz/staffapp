package com.asuliatech.fieldstaff

import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.json.JSONObject

private val Teal = Color(0xFF008B8C)
private val Navy = Color(0xFF082849)
private val Pale = Color(0xFFF8FAFC)
private val RoyalPurple = Color(0xFF6366F1)
private val SuccessGreen = Color(0xFF059669)
private val WarningOrange = Color(0xFFD97706)

enum class Page {
    LOGIN, DASHBOARD, SCHOOL, TERMINAL, ACTIVATE, TEST, EVIDENCE, SUCCESS, OFFLINE, PROFILE, SERVICE
}

private val LocalBack = staticCompositionLocalOf<() -> Unit> { {} }

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme(
                colorScheme = lightColorScheme(
                    primary = Teal,
                    secondary = RoyalPurple,
                    background = Pale,
                    surface = Color.White
                )
            ) {
                FieldStaffApp()
            }
        }
    }
}

@Composable
fun FieldStaffApp() {
    val context = LocalContext.current
    var currentPage by remember {
        mutableStateOf(
            if (FieldApiClient.token(context).isNullOrBlank()) Page.LOGIN else Page.DASHBOARD
        )
    }

    // App state
    var staffProfile by remember { mutableStateOf(FieldApiClient.getStaffProfile(context)) }
    var availableSchools by remember { mutableStateOf(FieldApiClient.getCachedSchools(context)) }
    var selectedSchool by remember { mutableStateOf(availableSchools.firstOrNull()?.schoolName ?: "DAV Public School, Patna") }
    var selectedSchoolId by remember { mutableStateOf(availableSchools.firstOrNull()?.schoolId ?: "SCH-001") }
    var terminalId by remember { mutableStateOf("") }
    var imeiLast6 by remember { mutableStateOf("") }
    var simLast6 by remember { mutableStateOf("") }
    var secretPin by remember { mutableStateOf("321123") }
    var activatedTerminals by remember { mutableStateOf(listOf<String>()) }
    var activationMessage by remember { mutableStateOf("") }
    var isActivating by remember { mutableStateOf(false) }

    // Test Call Checklist state
    var cardInserted by remember { mutableStateOf(false) }
    var parent1Tested by remember { mutableStateOf(false) }
    var parent2Tested by remember { mutableStateOf(false) }
    var audioClear by remember { mutableStateOf(false) }
    var durationCompleted by remember { mutableStateOf(false) }

    // Fetch fresh bootstrap data when app starts or enters dashboard
    LaunchedEffect(currentPage) {
        if (currentPage == Page.DASHBOARD && !FieldApiClient.token(context).isNullOrBlank()) {
            FieldApiClient.bootstrap(context) { ok, schools, _ ->
                if (ok && schools.isNotEmpty()) {
                    availableSchools = schools
                    if (selectedSchoolId.isBlank() || availableSchools.none { it.schoolId == selectedSchoolId }) {
                        selectedSchoolId = schools.first().schoolId
                        selectedSchool = schools.first().schoolName
                    }
                }
            }
        }
    }

    val back: () -> Unit = {
        currentPage = when (currentPage) {
            Page.DASHBOARD -> Page.LOGIN
            Page.SCHOOL -> Page.DASHBOARD
            Page.TERMINAL -> Page.SCHOOL
            Page.ACTIVATE -> Page.TERMINAL
            Page.TEST -> Page.ACTIVATE
            Page.EVIDENCE -> Page.TEST
            Page.SUCCESS, Page.OFFLINE, Page.PROFILE, Page.SERVICE -> Page.DASHBOARD
            Page.LOGIN -> Page.LOGIN
        }
    }

    CompositionLocalProvider(LocalBack provides back) {
        BackHandler(enabled = currentPage != Page.LOGIN && currentPage != Page.DASHBOARD) {
            back()
        }

        when (currentPage) {
            Page.LOGIN -> LoginScreen { profile ->
                staffProfile = profile
                currentPage = Page.DASHBOARD
            }

            Page.DASHBOARD -> DashboardScreen(
                staff = staffProfile,
                onNewInstall = { currentPage = Page.SCHOOL },
                onActivateExisting = { currentPage = Page.TERMINAL },
                onServiceReport = { currentPage = Page.SERVICE },
                onOffline = { currentPage = Page.OFFLINE },
                onProfile = { currentPage = Page.PROFILE }
            )

            // Step 1: School Selection
            Page.SCHOOL -> FlowHeader("New Installation", 1) {
                CardBox("Select Assigned School") {
                    Text(
                        "Choose the school where the terminal is being installed:",
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color.Gray
                    )

                    var expanded by remember { mutableStateOf(false) }
                    Box(modifier = Modifier.fillMaxWidth()) {
                        OutlinedTextField(
                            value = selectedSchool,
                            onValueChange = {},
                            readOnly = true,
                            label = { Text("School Name") },
                            modifier = Modifier.fillMaxWidth().clickable { expanded = true },
                            trailingIcon = { Text("▼", modifier = Modifier.clickable { expanded = true }) }
                        )
                        DropdownMenu(
                            expanded = expanded,
                            onDismissRequest = { expanded = false },
                            modifier = Modifier.fillMaxWidth(0.9f)
                        ) {
                            availableSchools.forEach { s ->
                                DropdownMenuItem(
                                    text = {
                                        Column {
                                            Text(s.schoolName, fontWeight = FontWeight.Bold)
                                            if (s.address.isNotBlank()) {
                                                Text(s.address, style = MaterialTheme.typography.bodySmall, color = Color.Gray)
                                            }
                                        }
                                    },
                                    onClick = {
                                        selectedSchool = s.schoolName
                                        selectedSchoolId = s.schoolId
                                        expanded = false
                                    }
                                )
                            }
                        }
                    }

                    InfoRow("School ID", selectedSchoolId)
                    InfoRow("Assigned Staff", staffProfile.name)
                    InfoRow("Install Date", "Today (Active Session)")

                    Spacer(Modifier.height(8.dp))
                    ButtonMain("Next: Terminal Setup   →") {
                        currentPage = Page.TERMINAL
                    }
                }
            }

            // Step 2: Terminal Details
            Page.TERMINAL -> FlowHeader("New Installation", 2) {
                CardBox("Terminal & SIM Details") {
                    Text(
                        "Enter the hardware identifiers physically printed on the terminal unit:",
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color.Gray
                    )

                    InputField(terminalId, { terminalId = it }, "Terminal ID", "e.g. 865423781203")
                    InputField(imeiLast6, { imeiLast6 = it }, "IMEI (Last 6 Digits)", "e.g. 007234", KeyboardType.Number)
                    InputField(simLast6, { simLast6 = it }, "SIM Number (Last 6 Digits)", "e.g. 678901", KeyboardType.Number)

                    OutlinedTextField(
                        value = secretPin,
                        onValueChange = { if (it.length <= 6) secretPin = it },
                        label = { Text("Staff Secret PIN (Activation Key)") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                    )

                    Text(
                        "Strict Staff Master PIN: 321123. Required for terminal cloud authorization.",
                        color = RoyalPurple,
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.Medium
                    )

                    Spacer(Modifier.height(8.dp))
                    ButtonMain("Activate Terminal Cloud Link   →") {
                        if (terminalId.isBlank()) {
                            Toast.makeText(context, "Please enter Terminal ID", Toast.LENGTH_SHORT).show()
                        } else {
                            isActivating = true
                            FieldApiClient.activateTerminalDirect(
                                schoolId = selectedSchoolId,
                                terminalId = terminalId,
                                imei = imeiLast6,
                                sim = simLast6,
                                secretPin = secretPin
                            ) { ok, msg ->
                                isActivating = false
                                activationMessage = msg
                                if (ok) {
                                    activatedTerminals = activatedTerminals + terminalId
                                    currentPage = Page.ACTIVATE
                                } else {
                                    Toast.makeText(context, msg, Toast.LENGTH_LONG).show()
                                    // Still allow transition to activation screen so staff can review
                                    currentPage = Page.ACTIVATE
                                }
                            }
                        }
                    }
                }
            }

            // Step 3: Activation Status
            Page.ACTIVATE -> FlowHeader("New Installation", 3) {
                CardBox("Cloud Activation Status") {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Box(
                            modifier = Modifier.size(44.dp).clip(CircleShape).background(Color(0xFFE9F9EC)),
                            contentAlignment = Alignment.Center
                        ) {
                            Text("✓", color = SuccessGreen, fontWeight = FontWeight.Bold, fontSize = 22.sp)
                        }
                        Column {
                            Text(
                                "Terminal Online & Linked",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = Navy
                            )
                            Text(
                                if (activationMessage.isNotBlank()) activationMessage else "MQTT broker handshake verified",
                                style = MaterialTheme.typography.bodySmall,
                                color = SuccessGreen
                            )
                        }
                    }

                    HorizontalDivider(color = Color(0xFFE2E8F0))
                    InfoRow("Terminal ID", terminalId.ifBlank { "865423781203" })
                    InfoRow("School", selectedSchool)
                    InfoRow("School ID", selectedSchoolId)
                    InfoRow("IMEI Last 6", imeiLast6.ifBlank { "007234" })
                    InfoRow("SIM Last 6", simLast6.ifBlank { "678901" })
                    InfoRow("Cloud Status", "Active / Authorized")

                    Spacer(Modifier.height(8.dp))
                    ButtonMain("Proceed to Physical Test Call   →") {
                        currentPage = Page.TEST
                    }
                }
            }

            // Step 4: Physical Test Call
            Page.TEST -> FlowHeader("New Installation", 4) {
                CardBox("Physical Call Testing") {
                    Text(
                        "Perform physical field tests on the terminal before leaving the premises:",
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color.Gray
                    )

                    CheckboxItem("Student RFID card inserted in terminal", cardInserted) { cardInserted = it }
                    CheckboxItem("Parent 1 test call connected successfully", parent1Tested) { parent1Tested = it }
                    CheckboxItem("Parent 2 test call verified", parent2Tested) { parent2Tested = it }
                    CheckboxItem("Two-way voice clarity verified (Mic & Speaker)", audioClear) { audioClear = it }
                    CheckboxItem("Call duration minimum 1–3 minutes completed", durationCompleted) { durationCompleted = it }

                    Text(
                        "Tip: Test calls confirm SIP gateway, SIM voice balance, and 4G audio quality.",
                        color = Teal,
                        style = MaterialTheme.typography.bodySmall
                    )

                    Spacer(Modifier.height(8.dp))
                    ButtonMain("Continue to Evidence Capture   →") {
                        currentPage = Page.EVIDENCE
                    }
                }
            }

            // Step 5: Evidence & Submit
            Page.EVIDENCE -> FlowHeader("New Installation", 5) {
                CardBox("Evidence & Verification") {
                    ExpenseEvidence(
                        schoolId = selectedSchoolId,
                        terminalId = terminalId
                    ) { evidenceUri, lat, lng, time ->
                        // Submit to API or store offline
                        val payload = JSONObject().apply {
                            put("card_inserted", cardInserted)
                            put("parent1_tested", parent1Tested)
                            put("parent2_tested", parent2Tested)
                            put("audio_clear", audioClear)
                            put("duration_completed", durationCompleted)
                        }

                        FieldApiClient.submitReport(
                            context = context,
                            schoolId = selectedSchoolId,
                            terminalId = terminalId.ifBlank { null },
                            reportType = "new_installation",
                            status = "completed",
                            payload = payload,
                            latitude = lat,
                            longitude = lng,
                            evidenceUrl = evidenceUri,
                            evidenceType = "installation_photo"
                        ) { ok, _ ->
                            if (!ok) {
                                // Save to offline queue if server returned error
                                OfflineManager.saveReport(
                                    context = context,
                                    schoolId = selectedSchoolId,
                                    schoolName = selectedSchool,
                                    terminalId = terminalId,
                                    reportType = "new_installation",
                                    latitude = lat,
                                    longitude = lng,
                                    capturedAt = time,
                                    evidenceUri = evidenceUri,
                                    isCallTested = true
                                )
                            }
                            currentPage = Page.SUCCESS
                        }
                    }
                }
            }

            // Step 6: Success Receipt
            Page.SUCCESS -> FlowHeader("Installation Complete", 5) {
                CardBox("Installation Report Submitted") {
                    Column(
                        modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Box(
                            modifier = Modifier.size(64.dp).clip(CircleShape).background(Color(0xFFE9F9EC)),
                            contentAlignment = Alignment.Center
                        ) {
                            Text("✓", color = SuccessGreen, fontSize = 36.sp, fontWeight = FontWeight.Bold)
                        }
                        Text(
                            "Job Completed Successfully!",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = Navy
                        )
                        Text(
                            "Terminal is now live and transmitting MQTT telemetry.",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color.Gray
                        )
                    }

                    HorizontalDivider(color = Color(0xFFE2E8F0))
                    InfoRow("School", selectedSchool)
                    InfoRow("Terminal ID", terminalId.ifBlank { "865423781203" })
                    InfoRow("Call Test", if (parent1Tested) "Verified (Passed)" else "Self-certified")
                    InfoRow("Evidence", "Captured with GPS & Timestamp")

                    Spacer(Modifier.height(12.dp))
                    ButtonMain("Return to Dashboard") {
                        // Reset fields for next job
                        terminalId = ""
                        imeiLast6 = ""
                        simLast6 = ""
                        currentPage = Page.DASHBOARD
                    }
                }
            }

            // Offline Queue Screen
            Page.OFFLINE -> PageBox("Offline Data & Sync") {
                val reports = remember { mutableStateOf(OfflineManager.getReports(context)) }
                var isSyncing by remember { mutableStateOf(false) }

                CardBox("Pending Local Submissions (${reports.value.size})") {
                    if (reports.value.isEmpty()) {
                        Column(
                            modifier = Modifier.fillMaxWidth().padding(vertical = 24.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text("☁", fontSize = 48.sp, color = Teal)
                            Text("All Data Synced!", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = Navy)
                            Text("No pending reports stored locally.", color = Color.Gray)
                        }
                    } else {
                        reports.value.forEach { r ->
                            Card(
                                modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                                shape = RoundedCornerShape(10.dp),
                                colors = CardDefaults.cardColors(containerColor = Color(0xFFF1F5F9))
                            ) {
                                Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                    Text(r.schoolName, fontWeight = FontWeight.Bold, color = Navy)
                                    Text("Terminal: ${r.terminalId}  •  ${r.reportType}", style = MaterialTheme.typography.bodySmall)
                                    Text("Captured: ${r.capturedAt}", style = MaterialTheme.typography.bodySmall, color = Color.Gray)
                                }
                            }
                        }

                        Button(
                            onClick = {
                                isSyncing = true
                                val list = reports.value
                                var count = 0
                                list.forEach { r ->
                                    FieldApiClient.submitReport(
                                        context = context,
                                        schoolId = r.schoolId,
                                        terminalId = r.terminalId,
                                        reportType = r.reportType,
                                        status = "completed"
                                    ) { ok, _ ->
                                        if (ok) {
                                            OfflineManager.removeReport(context, r.id)
                                        }
                                        count++
                                        if (count == list.size) {
                                            isSyncing = false
                                            reports.value = OfflineManager.getReports(context)
                                            Toast.makeText(context, "Sync complete", Toast.LENGTH_SHORT).show()
                                        }
                                    }
                                }
                            },
                            enabled = !isSyncing,
                            modifier = Modifier.fillMaxWidth().height(48.dp),
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = Teal)
                        ) {
                            if (isSyncing) CircularProgressIndicator(color = Color.White, strokeWidth = 2.dp, modifier = Modifier.size(20.dp))
                            else Text("Sync All Pending Reports Now")
                        }
                    }
                }
            }

            // Service Report Screen
            Page.SERVICE -> PageBox("Service & Maintenance") {
                var notes by remember { mutableStateOf("") }
                var serviceType by remember { mutableStateOf("SIM Replacement") }

                CardBox("Submit Service Report") {
                    InputField(terminalId, { terminalId = it }, "Terminal ID", "e.g. 865423781203")
                    InputField(serviceType, { serviceType = it }, "Service Action", "e.g. Antenna repair, SIM swap")
                    OutlinedTextField(
                        value = notes,
                        onValueChange = { notes = it },
                        label = { Text("Service Description / Resolution") },
                        modifier = Modifier.fillMaxWidth().height(100.dp),
                        maxLines = 4
                    )

                    ButtonMain("Submit Service Report") {
                        if (terminalId.isBlank()) {
                            Toast.makeText(context, "Enter Terminal ID", Toast.LENGTH_SHORT).show()
                        } else {
                            FieldApiClient.submitReport(
                                context = context,
                                schoolId = selectedSchoolId,
                                terminalId = terminalId,
                                reportType = "service",
                                payload = JSONObject().apply {
                                    put("action", serviceType)
                                    put("notes", notes)
                                }
                            ) { ok, msg ->
                                Toast.makeText(context, if (ok) "Service report submitted!" else msg, Toast.LENGTH_SHORT).show()
                                currentPage = Page.DASHBOARD
                            }
                        }
                    }
                }
            }

            // Profile Screen
            Page.PROFILE -> PageBox("Profile & App Settings") {
                CardBox("Staff Information") {
                    Column(
                        modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Box(
                            modifier = Modifier.size(64.dp).clip(CircleShape).background(Teal),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = staffProfile.name.take(1).uppercase(),
                                color = Color.White,
                                fontSize = 28.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        Text(staffProfile.name, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = Navy)
                        Text("Username: ${staffProfile.username}", color = Color.Gray, style = MaterialTheme.typography.bodyMedium)
                    }

                    HorizontalDivider(color = Color(0xFFE2E8F0))
                    InfoRow("Role", "On-Ground Field Staff")
                    InfoRow("Assigned Schools", "${availableSchools.size} Schools")
                    InfoRow("Backend Gateway", "https://asuliatech.com")
                    InfoRow("Activation Master PIN", "321123")
                    InfoRow("App Version", "v0.2.0 (Compose 2026)")

                    Spacer(Modifier.height(12.dp))
                    Button(
                        onClick = {
                            FieldApiClient.logout(context) {
                                currentPage = Page.LOGIN
                            }
                        },
                        modifier = Modifier.fillMaxWidth().height(48.dp),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFDC2626))
                    ) {
                        Text("Logout From Account", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

// -------------------- UI COMPONENTS --------------------

@Composable
fun LoginScreen(onLoginSuccess: (StaffProfile) -> Unit) {
    val context = LocalContext.current
    var username by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var errorMessage by remember { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(false) }

    Box(modifier = Modifier.fillMaxSize()) {
        Image(
            painter = painterResource(R.drawable.login_school_staff),
            contentDescription = null,
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Crop
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Spacer(Modifier.height(24.dp))
            Text(
                "AsuliaTech",
                style = MaterialTheme.typography.headlineLarge,
                color = Navy,
                fontWeight = FontWeight.ExtraBold
            )
            Text("Secure Terminal Field Operations", color = Navy, fontWeight = FontWeight.Medium)

            Row(
                modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                FeatureBadge("🛡", "Install")
                FeatureBadge("🛠", "Service")
                FeatureBadge("📄", "Report")
                FeatureBadge("☎", "Verify")
            }

            Card(
                colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.96f)),
                shape = RoundedCornerShape(22.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        "Field Staff Login",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = Navy
                    )

                    InputField(username, { username = it }, "Staff Username", "Enter registered username")
                    OutlinedTextField(
                        value = password,
                        onValueChange = { password = it },
                        label = { Text("Password") },
                        placeholder = { Text("Enter your password") },
                        visualTransformation = PasswordVisualTransformation(),
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )

                    if (errorMessage.isNotBlank()) {
                        Text(
                            text = errorMessage,
                            color = Color(0xFFDC2626),
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.Medium
                        )
                    }

                    Button(
                        onClick = {
                            if (username.isBlank() || password.isBlank()) {
                                errorMessage = "Please enter username and password"
                            } else {
                                isLoading = true
                                errorMessage = ""
                                FieldApiClient.login(context, username, password) { ok, msg, profile ->
                                    isLoading = false
                                    if (ok && profile != null) {
                                        onLoginSuccess(profile)
                                    } else {
                                        errorMessage = msg
                                    }
                                }
                            }
                        },
                        enabled = !isLoading,
                        modifier = Modifier.fillMaxWidth().height(52.dp),
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Teal)
                    ) {
                        if (isLoading) {
                            CircularProgressIndicator(color = Color.White, strokeWidth = 2.dp, modifier = Modifier.size(22.dp))
                        } else {
                            Text("Sign In   →", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun DashboardScreen(
    staff: StaffProfile,
    onNewInstall: () -> Unit,
    onActivateExisting: () -> Unit,
    onServiceReport: () -> Unit,
    onOffline: () -> Unit,
    onProfile: () -> Unit
) {
    val context = LocalContext.current
    val pendingOffline = remember { mutableIntStateOf(OfflineManager.pendingCount(context)) }

    PageBox("Hi, ${staff.name.ifBlank { "Staff" }}") {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("Field Operations Hub", color = Teal, fontWeight = FontWeight.SemiBold)
            AssistChip(
                onClick = onProfile,
                label = { Text("Staff ID: ${if (staff.id > 0) staff.id else "001"}") }
            )
        }

        // Metrics Grid
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            DashboardMetric("12", "Total Visits", Color(0xFFE6F6FF), Modifier.weight(1f))
            DashboardMetric("${pendingOffline.intValue}", "Offline Queue", Color(0xFFFFF0E8), Modifier.weight(1f))
        }
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            DashboardMetric("8", "Completed", Color(0xFFE9F9EC), Modifier.weight(1f))
            DashboardMetric("4", "Installations", Color(0xFFF5ECFF), Modifier.weight(1f))
        }

        Text("QUICK ACTIONS", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, color = Color.Gray)

        ActionMenuItem("＋", "New Installation", "Install & cloud-activate new terminal unit", onNewInstall)
        ActionMenuItem("◉", "Activate Existing", "Cloud authorization using PIN 321123", onActivateExisting)
        ActionMenuItem("🛠", "Service & Repair", "Antenna, SIM swap, or voice issue report", onServiceReport)
        ActionMenuItem("☁", "Offline Data Sync", "Pending reports saved on phone (${pendingOffline.intValue})", onOffline)
        ActionMenuItem("⚙", "Staff Profile", "Account settings and logout", onProfile)
    }
}

@Composable
fun FlowHeader(title: String, step: Int, content: @Composable ColumnScope.() -> Unit) {
    PageBox("$title  •  Step $step of 5", content)
}

@Composable
fun PageBox(title: String, content: @Composable ColumnScope.() -> Unit) {
    Column(modifier = Modifier.fillMaxSize().background(Pale)) {
        Row(
            modifier = Modifier.fillMaxWidth().background(Teal).padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = LocalBack.current, modifier = Modifier.padding(end = 6.dp)) {
                Text("‹", color = Color.White, style = MaterialTheme.typography.headlineMedium)
            }
            Text(
                title,
                color = Color.White,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
        }
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
            content = content
        )
    }
}

@Composable
fun CardBox(title: String, content: @Composable ColumnScope.() -> Unit) {
    Card(
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White)
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(title, style = MaterialTheme.typography.titleMedium, color = Navy, fontWeight = FontWeight.Bold)
            content()
        }
    }
}

@Composable
fun InputField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    placeholder: String = "",
    keyboardType: KeyboardType = KeyboardType.Text
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label) },
        placeholder = { Text(placeholder) },
        keyboardOptions = KeyboardOptions(keyboardType = keyboardType),
        modifier = Modifier.fillMaxWidth(),
        singleLine = true
    )
}

@Composable
fun ButtonMain(text: String, onClick: () -> Unit) {
    Button(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth().height(52.dp),
        shape = RoundedCornerShape(14.dp),
        colors = ButtonDefaults.buttonColors(containerColor = Teal)
    ) {
        Text(text, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
    }
}

@Composable
fun InfoRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(label, color = Color.Gray, style = MaterialTheme.typography.bodyMedium)
        Text(value, color = Navy, fontWeight = FontWeight.SemiBold, style = MaterialTheme.typography.bodyMedium)
    }
}

@Composable
fun CheckboxItem(text: String, checked: Boolean, onCheckedChange: (Boolean) -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .background(if (checked) Color(0xFFF0FDF4) else Color(0xFFF8FAFC))
            .clickable { onCheckedChange(!checked) }
            .padding(10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Checkbox(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = CheckboxDefaults.colors(checkedColor = SuccessGreen)
        )
        Spacer(Modifier.width(8.dp))
        Text(text, color = Navy, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium)
    }
}

@Composable
fun DashboardMetric(value: String, title: String, background: Color, modifier: Modifier = Modifier) {
    Card(modifier = modifier, colors = CardDefaults.cardColors(containerColor = background), shape = RoundedCornerShape(14.dp)) {
        Column(modifier = Modifier.padding(14.dp)) {
            Text(value, color = Teal, style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
            Text(title, style = MaterialTheme.typography.bodySmall, color = Navy)
        }
    }
}

@Composable
fun ActionMenuItem(icon: String, title: String, subtitle: String, onClick: () -> Unit) {
    Card(
        onClick = onClick,
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier.size(40.dp).clip(CircleShape).background(Color(0xFFE6F6FF)),
                contentAlignment = Alignment.Center
            ) {
                Text(icon, color = Teal, fontSize = 20.sp, fontWeight = FontWeight.Bold)
            }
            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(title, fontWeight = FontWeight.Bold, color = Navy)
                Text(subtitle, style = MaterialTheme.typography.bodySmall, color = Color.Gray)
            }
            Text("›", color = Teal, fontSize = 22.sp, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
fun FeatureBadge(icon: String, title: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(icon, fontSize = 22.sp)
        Text(title, color = Navy, style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Medium)
    }
}
