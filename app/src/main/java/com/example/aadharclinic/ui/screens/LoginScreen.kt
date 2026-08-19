package com.example.aadharclinic.ui.screens

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import androidx.compose.animation.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.aadharclinic.data.model.ClinicProfile
import com.example.aadharclinic.data.model.ClinicUser
import com.example.aadharclinic.data.model.UserRole
import com.example.aadharclinic.util.AuthUtils

enum class AuthScreenState {
    WELCOME,
    NEW_ACCOUNT,
    CREATION_SUCCESS,
    EXISTING_ACCOUNT
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LoginScreen(
    profile: ClinicProfile?,
    users: List<ClinicUser>,
    isOnline: Boolean = true,
    errorMessage: String?,
    onClearError: () -> Unit = {},
    onLoginHospitalClick: (hospitalId: String, role: UserRole, password: String) -> Unit,
    onCreateHospitalAccount: (ClinicProfile, adminPassword: String) -> Unit
) {
    val context = LocalContext.current
    var authState by remember { mutableStateOf(AuthScreenState.WELCOME) }
    var createdHospitalId by remember { mutableStateOf("") }
    var createdAdminDoctorName by remember { mutableStateOf("") }

    // Clear any previous errors on state transition
    fun transitionTo(newState: AuthScreenState) {
        onClearError()
        authState = newState
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        MaterialTheme.colorScheme.primary.copy(alpha = 0.08f),
                        MaterialTheme.colorScheme.background,
                        MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
                    )
                )
            )
            .padding(16.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Box(
                modifier = Modifier.weight(1f, fill = false),
                contentAlignment = Alignment.Center
            ) {
                AnimatedContent(
                    targetState = authState,
                    label = "auth_flow_transition"
                ) { state ->
                    when (state) {
                        AuthScreenState.WELCOME -> {
                            WelcomeCardView(
                                profile = profile,
                                isOnline = isOnline,
                                onNewAccountClick = { transitionTo(AuthScreenState.NEW_ACCOUNT) },
                                onExistingAccountClick = { transitionTo(AuthScreenState.EXISTING_ACCOUNT) }
                            )
                        }

                        AuthScreenState.NEW_ACCOUNT -> {
                            CreateHospitalAccountCardView(
                                isOnline = isOnline,
                                errorMessage = errorMessage,
                                onBackClick = { transitionTo(AuthScreenState.WELCOME) },
                                onCreateAccount = { newProf, adminPass ->
                                    createdHospitalId = newProf.hospitalId
                                    createdAdminDoctorName = newProf.doctorName
                                    onCreateHospitalAccount(newProf, adminPass)
                                }
                            )
                        }

                        AuthScreenState.CREATION_SUCCESS -> {
                            HospitalCreationSuccessCardView(
                                hospitalId = createdHospitalId.ifBlank { profile?.hospitalId ?: "aadhar123" },
                                adminDoctorName = createdAdminDoctorName.ifBlank { profile?.doctorName ?: "Dr. Sanket Ghule" },
                                onProceed = {
                                    // Handled by viewmodel auto-login
                                }
                            )
                        }

                        AuthScreenState.EXISTING_ACCOUNT -> {
                            ExistingAccountLoginCardView(
                                profile = profile,
                                isOnline = isOnline,
                                errorMessage = errorMessage,
                                onBackClick = { transitionTo(AuthScreenState.WELCOME) },
                                onLoginClick = onLoginHospitalClick
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Subtle Developer Credit Footer
            Text(
                text = "Developed & Designed by Dr. Sanket Shivajirao Ghule",
                style = MaterialTheme.typography.labelSmall.copy(fontSize = 11.sp),
                color = MaterialTheme.colorScheme.outline.copy(alpha = 0.7f),
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .padding(horizontal = 16.dp, vertical = 4.dp)
                    .testTag("login_developer_credit_footer")
            )
        }
    }
}

// -------------------------------------------------------------------------
// 1. FIRST SCREEN: WELCOME VIEW
// -------------------------------------------------------------------------
@Composable
fun WelcomeCardView(
    profile: ClinicProfile?,
    isOnline: Boolean,
    onNewAccountClick: () -> Unit,
    onExistingAccountClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .widthIn(max = 440.dp)
            .testTag("welcome_card"),
        shape = RoundedCornerShape(28.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 6.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(32.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // App Emblem / Hospital Icon
            Box(
                modifier = Modifier
                    .size(76.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primaryContainer),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Filled.LocalHospital,
                    contentDescription = "Clinic OS Logo",
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(42.dp)
                )
            }

            Spacer(modifier = Modifier.height(20.dp))

            Text(
                text = "Welcome",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.ExtraBold,
                color = MaterialTheme.colorScheme.onSurface,
                textAlign = TextAlign.Center
            )

            Text(
                text = profile?.clinicName ?: "Aadhar Multi-Speciality Clinic OS",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.primary,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(top = 4.dp)
            )

            Text(
                text = "Hospital-ID Based EMR & Management System",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.outline,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(top = 4.dp)
            )

            Spacer(modifier = Modifier.height(24.dp))

            // Connectivity Badge
            Surface(
                shape = RoundedCornerShape(20.dp),
                color = if (isOnline) Color(0xFFE8F5E9) else MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.7f),
                modifier = Modifier.padding(bottom = 8.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                ) {
                    Icon(
                        imageVector = if (isOnline) Icons.Filled.CloudDone else Icons.Filled.CloudOff,
                        contentDescription = null,
                        tint = if (isOnline) Color(0xFF2E7D32) else MaterialTheme.colorScheme.error,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = if (isOnline) "Cloud Authentication Online" else "Offline • Connect internet to login",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium,
                        color = if (isOnline) Color(0xFF2E7D32) else MaterialTheme.colorScheme.error
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Primary Doctor / Admin: [ New Account ]
            Button(
                onClick = onNewAccountClick,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp)
                    .testTag("btn_new_account"),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
            ) {
                Icon(Icons.Filled.AddBusiness, contentDescription = null, modifier = Modifier.size(22.dp))
                Spacer(modifier = Modifier.width(10.dp))
                Text(
                    text = "New Account",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Existing Account: [ Existing Account ]
            OutlinedButton(
                onClick = onExistingAccountClick,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp)
                    .testTag("btn_existing_account"),
                shape = RoundedCornerShape(16.dp),
                border = BorderStroke(1.5.dp, MaterialTheme.colorScheme.primary)
            ) {
                Icon(Icons.Filled.Login, contentDescription = null, modifier = Modifier.size(22.dp), tint = MaterialTheme.colorScheme.primary)
                Spacer(modifier = Modifier.width(10.dp))
                Text(
                    text = "Existing Account",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
            }

            Spacer(modifier = Modifier.height(20.dp))

            Text(
                text = "Secure • Offline-First • Multi-Role EMR",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.outline
            )

            Spacer(modifier = Modifier.height(6.dp))

            Text(
                text = "Developed & Designed by Dr. Sanket Shivajirao Ghule",
                style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                color = MaterialTheme.colorScheme.outline.copy(alpha = 0.65f),
                textAlign = TextAlign.Center
            )
        }
    }
}

// -------------------------------------------------------------------------
// 2. NEW ACCOUNT: CREATE HOSPITAL ACCOUNT
// -------------------------------------------------------------------------
@Composable
fun CreateHospitalAccountCardView(
    isOnline: Boolean,
    errorMessage: String?,
    onBackClick: () -> Unit,
    onCreateAccount: (ClinicProfile, String) -> Unit
) {
    var clinicName by remember { mutableStateOf("Aadhar Multi-Speciality Clinic") }
    var doctorName by remember { mutableStateOf("Dr. Sanket Ghule") }
    var qualification by remember { mutableStateOf("BAMS EMS") }
    var regNumber by remember { mutableStateOf("MCIM/EMS-74892") }
    var address by remember { mutableStateOf("102, Shanti Complex, Station Road, Pune - 411001") }
    var contactNumber by remember { mutableStateOf("+91 98230 12345") }
    var email by remember { mutableStateOf("dr.s.s.ghule@gmail.com") }
    var feeStr by remember { mutableStateOf("300") }
    var customHospitalId by remember { mutableStateOf("aadhar123") }
    var adminPassword by remember { mutableStateOf("") }
    var confirmPassword by remember { mutableStateOf("") }
    var passwordVisible by remember { mutableStateOf(false) }
    var validationError by remember { mutableStateOf<String?>(null) }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .widthIn(max = 520.dp)
            .testTag("create_hospital_card"),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 6.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Header with Back Button
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onBackClick) {
                    Icon(Icons.Filled.ArrowBack, contentDescription = "Back")
                }
                Spacer(modifier = Modifier.width(6.dp))
                Column {
                    Text(
                        text = "Create Hospital Account",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Primary Doctor & Hospital Setup",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.outline
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Error Display
            val activeError = validationError ?: errorMessage
            if (activeError != null) {
                Surface(
                    color = MaterialTheme.colorScheme.errorContainer,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Filled.ErrorOutline,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.error,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = activeError,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onErrorContainer
                        )
                    }
                }
                Spacer(modifier = Modifier.height(12.dp))
            }

            // Hospital Name
            OutlinedTextField(
                value = clinicName,
                onValueChange = {
                    clinicName = it
                    if (customHospitalId == "aadhar123" || customHospitalId.isBlank()) {
                        customHospitalId = AuthUtils.generateHospitalIdSuggestion(it)
                    }
                },
                label = { Text("Hospital / Clinic Name *") },
                leadingIcon = { Icon(Icons.Outlined.LocalHospital, contentDescription = null) },
                singleLine = true,
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("input_hospital_name")
            )

            Spacer(modifier = Modifier.height(10.dp))

            // Doctor / Admin Name
            OutlinedTextField(
                value = doctorName,
                onValueChange = { doctorName = it },
                label = { Text("Primary Doctor / Admin Name *") },
                placeholder = { Text("e.g. Dr. Sanket Ghule") },
                leadingIcon = { Icon(Icons.Outlined.Person, contentDescription = null) },
                singleLine = true,
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("input_admin_name")
            )

            Spacer(modifier = Modifier.height(10.dp))

            // Registration & Degree
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                OutlinedTextField(
                    value = qualification,
                    onValueChange = { qualification = it },
                    label = { Text("Degree") },
                    placeholder = { Text("e.g. BAMS EMS") },
                    singleLine = true,
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier.weight(1f)
                )

                OutlinedTextField(
                    value = regNumber,
                    onValueChange = { regNumber = it },
                    label = { Text("Medical Reg. No.") },
                    placeholder = { Text("e.g. MCIM/74892") },
                    singleLine = true,
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier.weight(1f)
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Address
            OutlinedTextField(
                value = address,
                onValueChange = { address = it },
                label = { Text("Hospital Address *") },
                leadingIcon = { Icon(Icons.Outlined.LocationOn, contentDescription = null) },
                maxLines = 2,
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(10.dp))

            // Phone & Email
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                OutlinedTextField(
                    value = contactNumber,
                    onValueChange = { contactNumber = it },
                    label = { Text("Contact Phone") },
                    leadingIcon = { Icon(Icons.Outlined.Phone, contentDescription = null) },
                    singleLine = true,
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier.weight(1f)
                )

                OutlinedTextField(
                    value = feeStr,
                    onValueChange = { feeStr = it },
                    label = { Text("OPD Fee (₹)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier.weight(0.7f)
                )
            }

            Spacer(modifier = Modifier.height(14.dp))

            // --- HOSPITAL ID FIELD ---
            Surface(
                color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            Icons.Filled.Key,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Unique Hospital ID",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }

                    Spacer(modifier = Modifier.height(6.dp))

                    OutlinedTextField(
                        value = customHospitalId,
                        onValueChange = { customHospitalId = it.trim().lowercase() },
                        label = { Text("Hospital ID (e.g. aadhar123)") },
                        leadingIcon = { Icon(Icons.Outlined.Fingerprint, contentDescription = null) },
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("input_hospital_id")
                    )

                    Spacer(modifier = Modifier.height(4.dp))

                    Text(
                        text = "ℹ️ This Hospital ID must be unique. All doctors, staff, and receptionists in your clinic will use it to log in.",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // --- ADMIN PASSWORD ---
            OutlinedTextField(
                value = adminPassword,
                onValueChange = { adminPassword = it },
                label = { Text("Admin Password *") },
                placeholder = { Text("Set strong admin password") },
                leadingIcon = { Icon(Icons.Outlined.Lock, contentDescription = null) },
                trailingIcon = {
                    IconButton(onClick = { passwordVisible = !passwordVisible }) {
                        Icon(
                            imageVector = if (passwordVisible) Icons.Filled.Visibility else Icons.Filled.VisibilityOff,
                            contentDescription = null
                        )
                    }
                },
                visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                singleLine = true,
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("input_admin_password")
            )

            Spacer(modifier = Modifier.height(10.dp))

            OutlinedTextField(
                value = confirmPassword,
                onValueChange = { confirmPassword = it },
                label = { Text("Confirm Admin Password *") },
                leadingIcon = { Icon(Icons.Outlined.LockClock, contentDescription = null) },
                visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                singleLine = true,
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("input_confirm_password")
            )

            Spacer(modifier = Modifier.height(20.dp))

            // Create Button
            Button(
                onClick = {
                    if (clinicName.isBlank()) {
                        validationError = "Please enter Hospital / Clinic Name"
                        return@Button
                    }
                    if (doctorName.isBlank()) {
                        validationError = "Please enter Doctor / Admin Name"
                        return@Button
                    }
                    if (customHospitalId.isBlank()) {
                        validationError = "Please enter a unique Hospital ID"
                        return@Button
                    }
                    if (adminPassword.length < 4) {
                        validationError = "Admin Password must be at least 4 characters"
                        return@Button
                    }
                    if (adminPassword != confirmPassword) {
                        validationError = "Passwords do not match"
                        return@Button
                    }

                    validationError = null
                    val fee = feeStr.toDoubleOrNull() ?: 300.0
                    val newProfile = ClinicProfile(
                        id = 1,
                        hospitalId = customHospitalId.trim().lowercase(),
                        clinicName = clinicName.trim(),
                        doctorName = doctorName.trim(),
                        qualification = qualification.trim(),
                        regNumber = regNumber.trim(),
                        address = address.trim(),
                        contactNumber = contactNumber.trim(),
                        email = email.trim(),
                        defaultConsultationFee = fee
                    )
                    onCreateAccount(newProfile, adminPassword)
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
                    .testTag("btn_create_hospital_submit"),
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
            ) {
                Icon(Icons.Filled.CheckCircle, contentDescription = null, modifier = Modifier.size(20.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text("Create Hospital Account", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            }
        }
    }
}

// -------------------------------------------------------------------------
// 3. ACCOUNT CREATION SUCCESS VIEW
// -------------------------------------------------------------------------
@Composable
fun HospitalCreationSuccessCardView(
    hospitalId: String,
    adminDoctorName: String,
    onProceed: () -> Unit
) {
    val context = LocalContext.current

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .widthIn(max = 440.dp)
            .testTag("creation_success_card"),
        shape = RoundedCornerShape(28.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 6.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(32.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                modifier = Modifier
                    .size(72.dp)
                    .clip(CircleShape)
                    .background(Color(0xFFE8F5E9)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Filled.CheckCircle,
                    contentDescription = "Success",
                    tint = Color(0xFF2E7D32),
                    modifier = Modifier.size(46.dp)
                )
            }

            Spacer(modifier = Modifier.height(18.dp))

            Text(
                text = "Hospital Account Created!",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.ExtraBold,
                textAlign = TextAlign.Center
            )

            Text(
                text = "Primary Admin account initialized successfully.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.outline,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(top = 4.dp)
            )

            Spacer(modifier = Modifier.height(20.dp))

            // Details Box
            Surface(
                color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "HOSPITAL ID",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = hospitalId,
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.ExtraBold,
                            fontFamily = FontFamily.Monospace,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                        IconButton(
                            onClick = {
                                val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                val clip = ClipData.newPlainText("Hospital ID", hospitalId)
                                clipboard.setPrimaryClip(clip)
                                Toast.makeText(context, "Hospital ID copied to clipboard!", Toast.LENGTH_SHORT).show()
                            }
                        ) {
                            Icon(Icons.Filled.ContentCopy, contentDescription = "Copy", tint = MaterialTheme.colorScheme.primary)
                        }
                    }

                    Divider(modifier = Modifier.padding(vertical = 8.dp), color = MaterialTheme.colorScheme.primary.copy(alpha = 0.2f))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Role:", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.outline)
                        Text("Admin (Primary Doctor)", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold)
                    }

                    Spacer(modifier = Modifier.height(4.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Doctor:", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.outline)
                        Text(adminDoctorName, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold)
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = "📌 Please save your Hospital ID. Share it with your staff, nurses, and receptionists to log in.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(24.dp))

            Button(
                onClick = onProceed,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
                    .testTag("btn_enter_dashboard"),
                shape = RoundedCornerShape(14.dp)
            ) {
                Text("Enter Admin Dashboard", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.width(8.dp))
                Icon(Icons.Filled.ArrowForward, contentDescription = null)
            }
        }
    }
}

// -------------------------------------------------------------------------
// 4. EXISTING ACCOUNT: HOSPITAL LOGIN VIEW
// -------------------------------------------------------------------------
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExistingAccountLoginCardView(
    profile: ClinicProfile?,
    isOnline: Boolean,
    errorMessage: String?,
    onBackClick: () -> Unit,
    onLoginClick: (hospitalId: String, role: UserRole, password: String) -> Unit
) {
    var hospitalId by remember { mutableStateOf(profile?.hospitalId ?: "aadhar123") }
    var selectedRole by remember { mutableStateOf(UserRole.ADMIN) }
    var roleDropdownExpanded by remember { mutableStateOf(false) }
    var password by remember { mutableStateOf("") }
    var passwordVisible by remember { mutableStateOf(false) }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .widthIn(max = 450.dp)
            .testTag("existing_login_card"),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 6.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(26.dp)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onBackClick) {
                    Icon(Icons.Filled.ArrowBack, contentDescription = "Back")
                }
                Spacer(modifier = Modifier.width(6.dp))
                Column {
                    Text(
                        text = "Hospital Login",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Enter Hospital ID, Role & Password",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.outline
                    )
                }
            }

            Spacer(modifier = Modifier.height(18.dp))

            // Offline Warning if not connected
            if (!isOnline) {
                Surface(
                    color = MaterialTheme.colorScheme.errorContainer,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Filled.WifiOff,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.error,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "🔴 Internet connection required for login. Connect to Wi-Fi or Mobile Data.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onErrorContainer,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
                Spacer(modifier = Modifier.height(12.dp))
            }

            // Error Banner
            if (errorMessage != null) {
                Surface(
                    color = MaterialTheme.colorScheme.errorContainer,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = errorMessage,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onErrorContainer,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(10.dp)
                    )
                }
                Spacer(modifier = Modifier.height(12.dp))
            }

            // 1. HOSPITAL ID FIELD
            OutlinedTextField(
                value = hospitalId,
                onValueChange = { hospitalId = it.trim().lowercase() },
                label = { Text("Hospital ID *") },
                placeholder = { Text("e.g. aadhar123") },
                leadingIcon = {
                    Icon(Icons.Outlined.Fingerprint, contentDescription = "Hospital ID", tint = MaterialTheme.colorScheme.primary)
                },
                singleLine = true,
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("login_hospital_id_input")
            )

            Spacer(modifier = Modifier.height(12.dp))

            // 2. ROLE DROPDOWN SELECTOR
            ExposedDropdownMenuBox(
                expanded = roleDropdownExpanded,
                onExpandedChange = { roleDropdownExpanded = !roleDropdownExpanded },
                modifier = Modifier.fillMaxWidth()
            ) {
                OutlinedTextField(
                    value = when (selectedRole) {
                        UserRole.ADMIN -> "Admin (Primary Doctor)"
                        UserRole.DOCTOR -> "Doctor (Consultant)"
                        UserRole.STAFF -> "Staff / Nurse"
                        UserRole.RECEPTION -> "Reception (Front Desk)"
                    },
                    onValueChange = {},
                    readOnly = true,
                    label = { Text("Select Role *") },
                    leadingIcon = {
                        Icon(
                            imageVector = when (selectedRole) {
                                UserRole.ADMIN -> Icons.Filled.AdminPanelSettings
                                UserRole.DOCTOR -> Icons.Filled.MedicalServices
                                UserRole.STAFF -> Icons.Filled.Badge
                                UserRole.RECEPTION -> Icons.Filled.Desk
                            },
                            contentDescription = "Role",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    },
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = roleDropdownExpanded) },
                    modifier = Modifier
                        .menuAnchor()
                        .fillMaxWidth()
                        .testTag("login_role_dropdown"),
                    shape = RoundedCornerShape(14.dp)
                )

                ExposedDropdownMenu(
                    expanded = roleDropdownExpanded,
                    onDismissRequest = { roleDropdownExpanded = false }
                ) {
                    listOf(
                        UserRole.ADMIN to "Admin (Primary Doctor)",
                        UserRole.DOCTOR to "Doctor (Consultant)",
                        UserRole.STAFF to "Staff / Nurse",
                        UserRole.RECEPTION to "Reception (Front Desk)"
                    ).forEach { (role, label) ->
                        DropdownMenuItem(
                            text = {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = when (role) {
                                            UserRole.ADMIN -> Icons.Filled.AdminPanelSettings
                                            UserRole.DOCTOR -> Icons.Filled.MedicalServices
                                            UserRole.STAFF -> Icons.Filled.Badge
                                            UserRole.RECEPTION -> Icons.Filled.Desk
                                        },
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(20.dp)
                                    )
                                    Spacer(modifier = Modifier.width(10.dp))
                                    Text(label, fontWeight = if (selectedRole == role) FontWeight.Bold else FontWeight.Normal)
                                }
                            },
                            onClick = {
                                selectedRole = role
                                roleDropdownExpanded = false
                            }
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // 3. PASSWORD FIELD
            OutlinedTextField(
                value = password,
                onValueChange = { password = it },
                label = { Text("Password *") },
                placeholder = { Text("Enter account password") },
                leadingIcon = {
                    Icon(Icons.Outlined.Lock, contentDescription = "Password", tint = MaterialTheme.colorScheme.primary)
                },
                trailingIcon = {
                    IconButton(onClick = { passwordVisible = !passwordVisible }) {
                        Icon(
                            imageVector = if (passwordVisible) Icons.Filled.Visibility else Icons.Filled.VisibilityOff,
                            contentDescription = if (passwordVisible) "Hide" else "Show"
                        )
                    }
                },
                visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                keyboardActions = KeyboardActions(onDone = {
                    if (hospitalId.isNotBlank() && password.isNotBlank()) {
                        onLoginClick(hospitalId, selectedRole, password)
                    }
                }),
                singleLine = true,
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("login_password_input")
            )

            Spacer(modifier = Modifier.height(20.dp))

            // LOGIN BUTTON
            Button(
                onClick = { onLoginClick(hospitalId, selectedRole, password) },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
                    .testTag("btn_hospital_login"),
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
            ) {
                Icon(Icons.Filled.Login, contentDescription = null, modifier = Modifier.size(20.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text("Login", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Quick Demo Helper
            Text(
                text = "Quick Demo Credentials (Pre-seeded):",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.outline
            )
            Spacer(modifier = Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                AssistChip(
                    onClick = {
                        hospitalId = "aadhar123"
                        selectedRole = UserRole.ADMIN
                        password = "admin123"
                    },
                    label = { Text("Admin", fontSize = 11.sp) },
                    modifier = Modifier.weight(1f)
                )
                AssistChip(
                    onClick = {
                        hospitalId = "aadhar123"
                        selectedRole = UserRole.DOCTOR
                        password = "doctor123"
                    },
                    label = { Text("Doctor", fontSize = 11.sp) },
                    modifier = Modifier.weight(1f)
                )
                AssistChip(
                    onClick = {
                        hospitalId = "aadhar123"
                        selectedRole = UserRole.STAFF
                        password = "staff123"
                    },
                    label = { Text("Staff", fontSize = 11.sp) },
                    modifier = Modifier.weight(1f)
                )
                AssistChip(
                    onClick = {
                        hospitalId = "aadhar123"
                        selectedRole = UserRole.RECEPTION
                        password = "reception123"
                    },
                    label = { Text("Reception", fontSize = 11.sp) },
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}
