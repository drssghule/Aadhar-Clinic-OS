package com.example.aadharclinic.ui.screens

import android.content.Context
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.aadharclinic.data.model.ClinicProfile
import com.example.aadharclinic.data.model.ClinicUser
import com.example.aadharclinic.data.model.ClinicalDocument
import com.example.aadharclinic.data.model.Hospital
import com.example.aadharclinic.data.model.Patient
import com.example.aadharclinic.ui.viewmodel.ClinicViewModel
import java.io.File
import java.text.SimpleDateFormat
import java.util.*

enum class DocumentCategory(val title: String, val subtitle: String) {
    SELECTION("Select Document", "Choose document to issue"),
    SICK_CERTIFICATE("Sickness Certificate", "Medical rest certificate for work or school"),
    REFERRAL_LETTER("Referral Letter", "Formal clinical referral to higher center/specialist")
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LettersScreen(
    profile: ClinicProfile?,
    currentUser: ClinicUser?,
    patients: List<Patient>,
    hospitals: List<Hospital>,
    documentsHistory: List<ClinicalDocument>,
    onSaveAndProcess: (ClinicalDocument, ClinicViewModel.DocAction, Context) -> Unit,
    onReprint: (ClinicalDocument, ClinicViewModel.DocAction, Context) -> Unit,
    onSaveHospital: (Hospital) -> Unit,
    onDeleteDocument: (ClinicalDocument) -> Unit
) {
    val context = LocalContext.current
    var currentView by remember { mutableStateOf(DocumentCategory.SELECTION) }
    var selectedHistoryDocForView by remember { mutableStateOf<ClinicalDocument?>(null) }
    var showAddHospitalDialog by remember { mutableStateOf(false) }

    // Tab state: 0 -> New Document Generator, 1 -> History (or quick switch)
    var activeTab by remember { mutableIntStateOf(0) }

    Scaffold(
        topBar = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.surface)
            ) {
                Surface(
                    color = MaterialTheme.colorScheme.surface,
                    tonalElevation = 2.dp
                ) {
                    TabRow(
                        selectedTabIndex = activeTab,
                        containerColor = MaterialTheme.colorScheme.surface
                    ) {
                        Tab(
                            selected = activeTab == 0,
                            onClick = { activeTab = 0 },
                            text = {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Filled.Description, contentDescription = null, modifier = Modifier.size(18.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("Issue Document", fontWeight = if (activeTab == 0) FontWeight.Bold else FontWeight.Normal)
                                }
                            }
                        )
                        Tab(
                            selected = activeTab == 1,
                            onClick = { activeTab = 1 },
                            text = {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Filled.History, contentDescription = null, modifier = Modifier.size(18.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("History (${documentsHistory.size})", fontWeight = if (activeTab == 1) FontWeight.Bold else FontWeight.Normal)
                                }
                            }
                        )
                    }
                }
            }
        }
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            if (activeTab == 0) {
                when (currentView) {
                    DocumentCategory.SELECTION -> {
                        DocumentSelectionView(
                            onSelectSickness = { currentView = DocumentCategory.SICK_CERTIFICATE },
                            onSelectReferral = { currentView = DocumentCategory.REFERRAL_LETTER },
                            recentDocuments = documentsHistory.take(4),
                            onViewDocument = { selectedHistoryDocForView = it }
                        )
                    }
                    DocumentCategory.SICK_CERTIFICATE -> {
                        SickCertificateForm(
                            profile = profile,
                            currentUser = currentUser,
                            patients = patients,
                            onBack = { currentView = DocumentCategory.SELECTION },
                            onProcess = { doc, action ->
                                onSaveAndProcess(doc, action, context)
                            }
                        )
                    }
                    DocumentCategory.REFERRAL_LETTER -> {
                        ReferralLetterForm(
                            profile = profile,
                            currentUser = currentUser,
                            patients = patients,
                            hospitals = hospitals,
                            onBack = { currentView = DocumentCategory.SELECTION },
                            onAddNewHospital = { showAddHospitalDialog = true },
                            onProcess = { doc, action ->
                                onSaveAndProcess(doc, action, context)
                            }
                        )
                    }
                }
            } else {
                // Full Document History Screen
                DocumentHistoryView(
                    documents = documentsHistory,
                    onViewDoc = { selectedHistoryDocForView = it },
                    onReprintDoc = { doc, act -> onReprint(doc, act, context) },
                    onDeleteDoc = { onDeleteDocument(it) },
                    onNewDocumentClick = {
                        activeTab = 0
                        currentView = DocumentCategory.SELECTION
                    }
                )
            }
        }
    }

    // Modal Document Preview & Action Sheet
    if (selectedHistoryDocForView != null) {
        DocumentPreviewDialog(
            document = selectedHistoryDocForView!!,
            profile = profile,
            onDismiss = { selectedHistoryDocForView = null },
            onPrint = {
                onReprint(selectedHistoryDocForView!!, ClinicViewModel.DocAction.GENERATE_AND_PRINT, context)
            },
            onSavePdf = {
                onReprint(selectedHistoryDocForView!!, ClinicViewModel.DocAction.SAVE_PDF, context)
            },
            onShare = {
                onReprint(selectedHistoryDocForView!!, ClinicViewModel.DocAction.SHARE, context)
            }
        )
    }

    // Quick Add Hospital Dialog
    if (showAddHospitalDialog) {
        AddHospitalDialog(
            onDismiss = { showAddHospitalDialog = false },
            onSave = {
                onSaveHospital(it)
                showAddHospitalDialog = false
            }
        )
    }
}

/**
 * Clean Two-Option Document Chooser Screen:
 * 1. Sickness Certificate
 * 2. Referral Letter
 */
@Composable
private fun DocumentSelectionView(
    onSelectSickness: () -> Unit,
    onSelectReferral: () -> Unit,
    recentDocuments: List<ClinicalDocument>,
    onViewDocument: (ClinicalDocument) -> Unit
) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)
                ),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(44.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.primary),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Filled.LocalHospital, contentDescription = null, tint = Color.White)
                    }
                    Spacer(modifier = Modifier.width(14.dp))
                    Column {
                        Text(
                            text = "Certificates & Letters",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "A4 Hospital Letterhead format with official paragraph phrasing",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }

        item {
            Text(
                text = "SELECT DOCUMENT TYPE",
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary,
                letterSpacing = 1.sp
            )
        }

        // Option 1: Sickness Certificate
        item {
            OutlinedCard(
                onClick = onSelectSickness,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("btn_select_sickness_certificate"),
                shape = RoundedCornerShape(16.dp),
                border = BorderStroke(1.5.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.4f)),
                colors = CardDefaults.outlinedCardColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(18.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(52.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(MaterialTheme.colorScheme.primaryContainer),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            Icons.Filled.Healing,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(28.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(16.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = "1. Sickness Certificate",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Medical rest certificate with patient residence & number of rest days required.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Icon(
                        Icons.Filled.ArrowForwardIos,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
        }

        // Option 2: Referral Letter
        item {
            OutlinedCard(
                onClick = onSelectReferral,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("btn_select_referral_letter"),
                shape = RoundedCornerShape(16.dp),
                border = BorderStroke(1.5.dp, MaterialTheme.colorScheme.tertiary.copy(alpha = 0.4f)),
                colors = CardDefaults.outlinedCardColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(18.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(52.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(MaterialTheme.colorScheme.tertiaryContainer),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            Icons.Filled.Outbox,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.tertiary,
                            modifier = Modifier.size(28.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(16.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = "2. Referral Letter",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Official clinical transfer letter to saved hospital with complaints & treatment given.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Icon(
                        Icons.Filled.ArrowForwardIos,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.tertiary,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
        }

        // Recent Issued Quick Access
        if (recentDocuments.isNotEmpty()) {
            item {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "RECENTLY ISSUED DOCUMENTS",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.outline,
                    letterSpacing = 0.8.sp
                )
            }

            items(recentDocuments) { doc ->
                DocumentHistoryCard(
                    document = doc,
                    onView = { onViewDocument(doc) }
                )
            }
        }
    }
}

/**
 * 1. REFERRAL LETTER INPUT FORM
 * Exact required fields:
 * - Refer to → searchable saved hospital
 * - Patient name (searchable patient / manual entry)
 * - Age
 * - Sex
 * - Chief complaints
 * - Treatment given
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReferralLetterForm(
    profile: ClinicProfile?,
    currentUser: ClinicUser?,
    patients: List<Patient>,
    hospitals: List<Hospital>,
    onBack: () -> Unit,
    onAddNewHospital: () -> Unit,
    onProcess: (ClinicalDocument, ClinicViewModel.DocAction) -> Unit
) {
    var selectedPatient by remember { mutableStateOf<Patient?>(null) }
    var patientName by remember { mutableStateOf("") }
    var ageText by remember { mutableStateOf("") }
    var sex by remember { mutableStateOf("Male") }

    var selectedHospital by remember { mutableStateOf<Hospital?>(hospitals.firstOrNull()) }
    var hospitalSearchQuery by remember { mutableStateOf("") }
    var showHospitalDropdown by remember { mutableStateOf(false) }

    var chiefComplaints by remember { mutableStateOf("") }
    var treatmentGiven by remember { mutableStateOf("") }

    var patientSearchQuery by remember { mutableStateOf("") }
    var showPatientDropdown by remember { mutableStateOf(false) }

    // Filtered Hospitals
    val filteredHospitals = remember(hospitals, hospitalSearchQuery) {
        if (hospitalSearchQuery.isBlank()) hospitals
        else hospitals.filter {
            it.name.contains(hospitalSearchQuery, ignoreCase = true) ||
            it.address.contains(hospitalSearchQuery, ignoreCase = true)
        }
    }

    // Filtered Patients
    val filteredPatients = remember(patients, patientSearchQuery) {
        if (patientSearchQuery.isBlank()) emptyList()
        else patients.filter {
            it.name.contains(patientSearchQuery, ignoreCase = true) ||
            it.mobile.contains(patientSearchQuery) ||
            it.patientCode.contains(patientSearchQuery, ignoreCase = true)
        }.take(5)
    }

    val doctorName = currentUser?.name ?: profile?.doctorName ?: "Dr. Sanket Ghule"
    val docQualification = currentUser?.qualification ?: profile?.qualification ?: "BAMS EMS"
    val regNumber = currentUser?.regNumber ?: profile?.regNumber ?: "MCIM/EMS-74892"
    val clinicName = profile?.clinicName ?: "Aadhar Clinic"

    fun buildDocument(): ClinicalDocument {
        return ClinicalDocument(
            documentType = "REFERRAL_LETTER",
            patientId = selectedPatient?.id,
            patientName = patientName.trim(),
            age = ageText.toIntOrNull() ?: selectedPatient?.age ?: 30,
            sex = sex,
            doctorName = doctorName,
            doctorQualification = docQualification,
            doctorRegNumber = regNumber,
            clinicName = clinicName,
            clinicAddress = profile?.address ?: "Pune",
            clinicContact = profile?.contactNumber ?: "+91 98230 12345",
            hospitalId = selectedHospital?.id,
            hospitalName = selectedHospital?.name ?: "Higher Medical Center",
            hospitalAddress = selectedHospital?.address ?: "Pune",
            chiefComplaints = chiefComplaints.trim(),
            treatmentGiven = treatmentGiven.trim()
        )
    }

    val canGenerate = patientName.isNotBlank() && selectedHospital != null

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                IconButton(onClick = onBack) {
                    Icon(Icons.Filled.ArrowBack, contentDescription = "Back")
                }
                Column {
                    Text(
                        text = "Referral Letter",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Date & Subject auto-generated on Letterhead",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.outline
                    )
                }
            }
        }

        // 1. HOSPITAL SELECTION (Searchable Saved Hospitals)
        item {
            Card(
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "REFER TO HOSPITAL",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                        TextButton(onClick = onAddNewHospital) {
                            Icon(Icons.Filled.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Add Hospital", fontSize = 12.sp)
                        }
                    }

                    OutlinedTextField(
                        value = if (showHospitalDropdown) hospitalSearchQuery else (selectedHospital?.name ?: ""),
                        onValueChange = {
                            hospitalSearchQuery = it
                            showHospitalDropdown = true
                        },
                        label = { Text("Search Saved Hospital *") },
                        placeholder = { Text("Type hospital name (e.g. Sahyadri, Ruby Hall)") },
                        leadingIcon = { Icon(Icons.Filled.LocalHospital, contentDescription = null) },
                        trailingIcon = {
                            IconButton(onClick = { showHospitalDropdown = !showHospitalDropdown }) {
                                Icon(Icons.Filled.ArrowDropDown, contentDescription = null)
                            }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("input_referral_hospital"),
                        shape = RoundedCornerShape(12.dp),
                        singleLine = true
                    )

                    if (showHospitalDropdown) {
                        Surface(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 4.dp),
                            shape = RoundedCornerShape(10.dp),
                            tonalElevation = 4.dp,
                            shadowElevation = 4.dp
                        ) {
                            Column {
                                if (filteredHospitals.isEmpty()) {
                                    Text(
                                        text = "No saved hospital found. Tap '+ Add Hospital' above.",
                                        style = MaterialTheme.typography.bodySmall,
                                        modifier = Modifier.padding(12.dp),
                                        color = MaterialTheme.colorScheme.outline
                                    )
                                } else {
                                    filteredHospitals.forEach { hosp ->
                                        ListItem(
                                            headlineContent = { Text(hosp.name, fontWeight = FontWeight.SemiBold) },
                                            supportingContent = { Text(hosp.address, maxLines = 1, overflow = TextOverflow.Ellipsis) },
                                            leadingContent = { Icon(Icons.Filled.Domain, contentDescription = null) },
                                            modifier = Modifier.clickable {
                                                selectedHospital = hosp
                                                hospitalSearchQuery = hosp.name
                                                showHospitalDropdown = false
                                            }
                                        )
                                        HorizontalDivider()
                                    }
                                }
                            }
                        }
                    }

                    if (selectedHospital != null) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(modifier = Modifier.padding(10.dp), verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Filled.LocationOn, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "To: ${selectedHospital!!.name}\n${selectedHospital!!.address}",
                                    style = MaterialTheme.typography.bodySmall
                                )
                            }
                        }
                    }
                }
            }
        }

        // 2. PATIENT DETAILS
        item {
            Card(
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Text(
                        text = "PATIENT INFORMATION",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.height(8.dp))

                    // Search existing patient autocomplete
                    OutlinedTextField(
                        value = patientName,
                        onValueChange = {
                            patientName = it
                            patientSearchQuery = it
                            showPatientDropdown = it.isNotBlank()
                            if (selectedPatient != null && selectedPatient!!.name != it) {
                                selectedPatient = null
                            }
                        },
                        label = { Text("Patient Name *") },
                        placeholder = { Text("Search registered patient or enter name") },
                        leadingIcon = { Icon(Icons.Filled.Person, contentDescription = null) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("input_patient_name"),
                        shape = RoundedCornerShape(12.dp),
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Words)
                    )

                    if (showPatientDropdown && filteredPatients.isNotEmpty()) {
                        Surface(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 4.dp),
                            shape = RoundedCornerShape(10.dp),
                            tonalElevation = 4.dp
                        ) {
                            Column {
                                filteredPatients.forEach { pat ->
                                    ListItem(
                                        headlineContent = { Text(pat.name, fontWeight = FontWeight.SemiBold) },
                                        supportingContent = { Text("${pat.age} Yrs • ${pat.sex} • Ph: ${pat.mobile.ifBlank { "N/A" }}") },
                                        modifier = Modifier.clickable {
                                            selectedPatient = pat
                                            patientName = pat.name
                                            ageText = pat.age.toString()
                                            sex = pat.sex
                                            showPatientDropdown = false
                                        }
                                    )
                                    HorizontalDivider()
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        OutlinedTextField(
                            value = ageText,
                            onValueChange = { ageText = it },
                            label = { Text("Age (Yrs) *") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            modifier = Modifier
                                .weight(1f)
                                .testTag("input_patient_age"),
                            shape = RoundedCornerShape(12.dp),
                            singleLine = true
                        )

                        Column(modifier = Modifier.weight(1.5f)) {
                            Text("Sex *", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.outline)
                            Spacer(modifier = Modifier.height(4.dp))
                            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                listOf("Male", "Female", "Other").forEach { s ->
                                    FilterChip(
                                        selected = sex.equals(s, ignoreCase = true),
                                        onClick = { sex = s },
                                        label = { Text(s, fontSize = 11.sp) }
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        // 3. CLINICAL DETAILS (Chief Complaints & Treatment Given)
        item {
            Card(
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Text(
                        text = "CLINICAL SUMMARY",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.height(10.dp))

                    OutlinedTextField(
                        value = chiefComplaints,
                        onValueChange = { chiefComplaints = it },
                        label = { Text("Chief Complaints *") },
                        placeholder = { Text("e.g. Acute severe abdominal pain radiating to back for 2 days, persistent vomiting") },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("input_chief_complaints"),
                        shape = RoundedCornerShape(12.dp),
                        minLines = 2,
                        maxLines = 4,
                        keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences)
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    OutlinedTextField(
                        value = treatmentGiven,
                        onValueChange = { treatmentGiven = it },
                        label = { Text("Treatment Given *") },
                        placeholder = { Text("e.g. Inj. Pantoprazole 40mg IV, Inj. Tramadol 50mg IV, IV Normal Saline 500ml") },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("input_treatment_given"),
                        shape = RoundedCornerShape(12.dp),
                        minLines = 2,
                        maxLines = 4,
                        keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences)
                    )
                }
            }
        }

        // 4. ACTION BUTTONS: Generate & Print, Save PDF, Share
        item {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Button(
                    onClick = {
                        val doc = buildDocument()
                        onProcess(doc, ClinicViewModel.DocAction.GENERATE_AND_PRINT)
                    },
                    enabled = canGenerate,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp)
                        .testTag("btn_referral_generate_print"),
                    shape = RoundedCornerShape(14.dp)
                ) {
                    Icon(Icons.Filled.Print, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Generate & Print", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedButton(
                        onClick = {
                            val doc = buildDocument()
                            onProcess(doc, ClinicViewModel.DocAction.SAVE_PDF)
                        },
                        enabled = canGenerate,
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp)
                            .testTag("btn_referral_save_pdf"),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(Icons.Filled.FileDownload, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Save PDF")
                    }

                    OutlinedButton(
                        onClick = {
                            val doc = buildDocument()
                            onProcess(doc, ClinicViewModel.DocAction.SHARE)
                        },
                        enabled = canGenerate,
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp)
                            .testTag("btn_referral_share"),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(Icons.Filled.Share, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Share")
                    }
                }
            }
        }
    }
}

/**
 * 2. MEDICAL SICK CERTIFICATE INPUT FORM
 * Exact required fields:
 * - Patient name
 * - Age
 * - Sex
 * - Resident of
 * - Number of days required
 * (Date is automatic. Do NOT add a date range.)
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SickCertificateForm(
    profile: ClinicProfile?,
    currentUser: ClinicUser?,
    patients: List<Patient>,
    onBack: () -> Unit,
    onProcess: (ClinicalDocument, ClinicViewModel.DocAction) -> Unit
) {
    var selectedPatient by remember { mutableStateOf<Patient?>(null) }
    var patientName by remember { mutableStateOf("") }
    var ageText by remember { mutableStateOf("") }
    var sex by remember { mutableStateOf("Male") }
    var residentOf by remember { mutableStateOf("Pune") }
    var daysCountText by remember { mutableStateOf("3") }

    var patientSearchQuery by remember { mutableStateOf("") }
    var showPatientDropdown by remember { mutableStateOf(false) }

    val filteredPatients = remember(patients, patientSearchQuery) {
        if (patientSearchQuery.isBlank()) emptyList()
        else patients.filter {
            it.name.contains(patientSearchQuery, ignoreCase = true) ||
            it.mobile.contains(patientSearchQuery) ||
            it.patientCode.contains(patientSearchQuery, ignoreCase = true)
        }.take(5)
    }

    val doctorName = currentUser?.name ?: profile?.doctorName ?: "Dr. Sanket Ghule"
    val docQualification = currentUser?.qualification ?: profile?.qualification ?: "BAMS EMS"
    val regNumber = currentUser?.regNumber ?: profile?.regNumber ?: "MCIM/EMS-74892"
    val clinicName = profile?.clinicName ?: "Aadhar Clinic"

    fun buildDocument(): ClinicalDocument {
        return ClinicalDocument(
            documentType = "SICK_CERTIFICATE",
            patientId = selectedPatient?.id,
            patientName = patientName.trim(),
            age = ageText.toIntOrNull() ?: selectedPatient?.age ?: 25,
            sex = sex,
            doctorName = doctorName,
            doctorQualification = docQualification,
            doctorRegNumber = regNumber,
            clinicName = clinicName,
            clinicAddress = profile?.address ?: "Pune",
            clinicContact = profile?.contactNumber ?: "+91 98230 12345",
            residentOf = residentOf.trim(),
            daysCount = daysCountText.toIntOrNull() ?: 3
        )
    }

    val canGenerate = patientName.isNotBlank() && (daysCountText.toIntOrNull() ?: 0) > 0

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                IconButton(onClick = onBack) {
                    Icon(Icons.Filled.ArrowBack, contentDescription = "Back")
                }
                Column {
                    Text(
                        text = "Medical Sick Certificate",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "To Whom It May Concern • Date is auto-generated",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.outline
                    )
                }
            }
        }

        // 1. PATIENT INFORMATION
        item {
            Card(
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Text(
                        text = "PATIENT DETAILS",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.height(10.dp))

                    OutlinedTextField(
                        value = patientName,
                        onValueChange = {
                            patientName = it
                            patientSearchQuery = it
                            showPatientDropdown = it.isNotBlank()
                            if (selectedPatient != null && selectedPatient!!.name != it) {
                                selectedPatient = null
                            }
                        },
                        label = { Text("Patient Name *") },
                        placeholder = { Text("Search registered patient or enter name") },
                        leadingIcon = { Icon(Icons.Filled.Person, contentDescription = null) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("input_sick_patient_name"),
                        shape = RoundedCornerShape(12.dp),
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Words)
                    )

                    if (showPatientDropdown && filteredPatients.isNotEmpty()) {
                        Surface(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 4.dp),
                            shape = RoundedCornerShape(10.dp),
                            tonalElevation = 4.dp
                        ) {
                            Column {
                                filteredPatients.forEach { pat ->
                                    ListItem(
                                        headlineContent = { Text(pat.name, fontWeight = FontWeight.SemiBold) },
                                        supportingContent = { Text("${pat.age} Yrs • ${pat.sex} • ${pat.address.ifBlank { "Pune" }}") },
                                        modifier = Modifier.clickable {
                                            selectedPatient = pat
                                            patientName = pat.name
                                            ageText = pat.age.toString()
                                            sex = pat.sex
                                            if (pat.address.isNotBlank()) residentOf = pat.address
                                            showPatientDropdown = false
                                        }
                                    )
                                    HorizontalDivider()
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        OutlinedTextField(
                            value = ageText,
                            onValueChange = { ageText = it },
                            label = { Text("Age (Yrs) *") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            modifier = Modifier
                                .weight(1f)
                                .testTag("input_sick_patient_age"),
                            shape = RoundedCornerShape(12.dp),
                            singleLine = true
                        )

                        Column(modifier = Modifier.weight(1.5f)) {
                            Text("Sex *", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.outline)
                            Spacer(modifier = Modifier.height(4.dp))
                            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                listOf("Male", "Female", "Other").forEach { s ->
                                    FilterChip(
                                        selected = sex.equals(s, ignoreCase = true),
                                        onClick = { sex = s },
                                        label = { Text(s, fontSize = 11.sp) }
                                    )
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))
                    OutlinedTextField(
                        value = residentOf,
                        onValueChange = { residentOf = it },
                        label = { Text("Resident Of *") },
                        placeholder = { Text("e.g. Pune, Hadapsar, Kothrud") },
                        leadingIcon = { Icon(Icons.Filled.Home, contentDescription = null) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("input_resident_of"),
                        shape = RoundedCornerShape(12.dp),
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Words)
                    )
                }
            }
        }

        // 2. REST DURATION (Number of Days)
        item {
            Card(
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Text(
                        text = "REST PERIOD REQUIRED",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.height(10.dp))

                    OutlinedTextField(
                        value = daysCountText,
                        onValueChange = { daysCountText = it },
                        label = { Text("Number of Days *") },
                        placeholder = { Text("e.g. 3, 5, 7") },
                        leadingIcon = { Icon(Icons.Filled.CalendarMonth, contentDescription = null) },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("input_days_required"),
                        shape = RoundedCornerShape(12.dp),
                        singleLine = true
                    )

                    Spacer(modifier = Modifier.height(8.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        listOf(1, 2, 3, 5, 7, 10, 14).forEach { d ->
                            SuggestionChip(
                                onClick = { daysCountText = d.toString() },
                                label = { Text("$d ${if (d == 1) "day" else "days"}") }
                            )
                        }
                    }
                }
            }
        }

        // 3. ACTION BUTTONS: Generate & Print, Save PDF, Share
        item {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Button(
                    onClick = {
                        val doc = buildDocument()
                        onProcess(doc, ClinicViewModel.DocAction.GENERATE_AND_PRINT)
                    },
                    enabled = canGenerate,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp)
                        .testTag("btn_sick_generate_print"),
                    shape = RoundedCornerShape(14.dp)
                ) {
                    Icon(Icons.Filled.Print, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Generate & Print", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedButton(
                        onClick = {
                            val doc = buildDocument()
                            onProcess(doc, ClinicViewModel.DocAction.SAVE_PDF)
                        },
                        enabled = canGenerate,
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp)
                            .testTag("btn_sick_save_pdf"),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(Icons.Filled.FileDownload, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Save PDF")
                    }

                    OutlinedButton(
                        onClick = {
                            val doc = buildDocument()
                            onProcess(doc, ClinicViewModel.DocAction.SHARE)
                        },
                        enabled = canGenerate,
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp)
                            .testTag("btn_sick_share"),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(Icons.Filled.Share, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Share")
                    }
                }
            }
        }
    }
}

/**
 * Document History View showing: Document Type | Patient | Date | Doctor
 * Features: View -> Reprint -> Save PDF -> Share
 */
@Composable
fun DocumentHistoryView(
    documents: List<ClinicalDocument>,
    onViewDoc: (ClinicalDocument) -> Unit,
    onReprintDoc: (ClinicalDocument, ClinicViewModel.DocAction) -> Unit,
    onDeleteDoc: (ClinicalDocument) -> Unit,
    onNewDocumentClick: () -> Unit
) {
    var searchQuery by remember { mutableStateOf("") }
    var filterType by remember { mutableStateOf("ALL") }

    val filtered = remember(documents, searchQuery, filterType) {
        documents.filter { doc ->
            (filterType == "ALL" || doc.documentType == filterType) &&
            (searchQuery.isBlank() ||
             doc.patientName.contains(searchQuery, ignoreCase = true) ||
             doc.doctorName.contains(searchQuery, ignoreCase = true) ||
             doc.hospitalName.contains(searchQuery, ignoreCase = true))
        }
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Document History",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Reprinting preserves original records without duplication",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.outline
                    )
                }

                FilledTonalButton(onClick = onNewDocumentClick) {
                    Icon(Icons.Filled.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("New Document")
                }
            }
        }

        item {
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                label = { Text("Search by Patient, Doctor or Hospital") },
                leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null) },
                trailingIcon = {
                    if (searchQuery.isNotEmpty()) {
                        IconButton(onClick = { searchQuery = "" }) {
                            Icon(Icons.Filled.Clear, contentDescription = null)
                        }
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("input_history_search"),
                shape = RoundedCornerShape(12.dp),
                singleLine = true
            )
        }

        item {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FilterChip(
                    selected = filterType == "ALL",
                    onClick = { filterType = "ALL" },
                    label = { Text("All (${documents.size})") }
                )
                FilterChip(
                    selected = filterType == "SICK_CERTIFICATE",
                    onClick = { filterType = "SICK_CERTIFICATE" },
                    label = { Text("Medical Certificates") }
                )
                FilterChip(
                    selected = filterType == "REFERRAL_LETTER",
                    onClick = { filterType = "REFERRAL_LETTER" },
                    label = { Text("Referral Letters") }
                )
            }
        }

        if (filtered.isEmpty()) {
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 40.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            Icons.Outlined.Description,
                            contentDescription = null,
                            modifier = Modifier.size(56.dp),
                            tint = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = "No documents found",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.outline
                        )
                        Text(
                            text = "Generate your first Referral Letter or Medical Certificate",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.outline
                        )
                    }
                }
            }
        } else {
            items(filtered, key = { it.id }) { doc ->
                DocumentHistoryCard(
                    document = doc,
                    onView = { onViewDoc(doc) },
                    onReprint = { onReprintDoc(doc, ClinicViewModel.DocAction.GENERATE_AND_PRINT) },
                    onSavePdf = { onReprintDoc(doc, ClinicViewModel.DocAction.SAVE_PDF) },
                    onShare = { onReprintDoc(doc, ClinicViewModel.DocAction.SHARE) },
                    onDelete = { onDeleteDoc(doc) }
                )
            }
        }
    }
}

/**
 * History List Item Card with Document Type | Patient | Date | Doctor
 */
@Composable
fun DocumentHistoryCard(
    document: ClinicalDocument,
    onView: () -> Unit,
    onReprint: (() -> Unit)? = null,
    onSavePdf: (() -> Unit)? = null,
    onShare: (() -> Unit)? = null,
    onDelete: (() -> Unit)? = null
) {
    val dateFormat = SimpleDateFormat("dd/MM/yyyy • hh:mm a", Locale.getDefault())
    val isReferral = document.documentType == "REFERRAL_LETTER"

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("doc_history_card_${document.id}"),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f))
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = if (isReferral) MaterialTheme.colorScheme.tertiaryContainer else MaterialTheme.colorScheme.primaryContainer
                ) {
                    Text(
                        text = if (isReferral) "REFERRAL LETTER" else "MEDICAL CERTIFICATE",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = if (isReferral) MaterialTheme.colorScheme.onTertiaryContainer else MaterialTheme.colorScheme.onPrimaryContainer,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }

                Text(
                    text = dateFormat.format(Date(document.date)),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.outline
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Patient Name & Details
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    Icons.Filled.Person,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp),
                    tint = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "${document.patientName} (${document.age} Yrs, ${document.sex})",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            }

            Spacer(modifier = Modifier.height(4.dp))

            // Doctor Details
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    Icons.Filled.MedicalServices,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp),
                    tint = MaterialTheme.colorScheme.outline
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "Issued by: ${document.doctorName.ifBlank { "Dr. Sanket Ghule" }}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.outline
                )
            }

            if (isReferral && document.hospitalName.isNotBlank()) {
                Spacer(modifier = Modifier.height(4.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Filled.LocalHospital,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp),
                        tint = MaterialTheme.colorScheme.tertiary
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "Referred to: ${document.hospitalName}",
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            } else if (!isReferral && document.daysCount > 0) {
                Spacer(modifier = Modifier.height(4.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Filled.Bedtime,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp),
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "Medical rest: ${document.daysCount} days • Resident of ${document.residentOf.ifBlank { "Pune" }}",
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
            Spacer(modifier = Modifier.height(6.dp))

            // Action Row: View, Reprint, Save PDF, Share
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                TextButton(
                    onClick = onView,
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Icon(Icons.Filled.Visibility, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("View", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }

                Row {
                    if (onReprint != null) {
                        IconButton(onClick = onReprint, modifier = Modifier.size(36.dp)) {
                            Icon(Icons.Filled.Print, contentDescription = "Reprint", tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(18.dp))
                        }
                    }
                    if (onSavePdf != null) {
                        IconButton(onClick = onSavePdf, modifier = Modifier.size(36.dp)) {
                            Icon(Icons.Filled.FileDownload, contentDescription = "Save PDF", modifier = Modifier.size(18.dp))
                        }
                    }
                    if (onShare != null) {
                        IconButton(onClick = onShare, modifier = Modifier.size(36.dp)) {
                            Icon(Icons.Filled.Share, contentDescription = "Share", modifier = Modifier.size(18.dp))
                        }
                    }
                    if (onDelete != null) {
                        IconButton(onClick = onDelete, modifier = Modifier.size(36.dp)) {
                            Icon(Icons.Outlined.Delete, contentDescription = "Delete", tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(18.dp))
                        }
                    }
                }
            }
        }
    }
}

/**
 * Clean In-App Preview Dialog showing the EXACT Hospital Letterhead layout in paragraph form!
 */
@Composable
fun DocumentPreviewDialog(
    document: ClinicalDocument,
    profile: ClinicProfile?,
    onDismiss: () -> Unit,
    onPrint: () -> Unit,
    onSavePdf: () -> Unit,
    onShare: () -> Unit
) {
    val isReferral = document.documentType == "REFERRAL_LETTER"
    val dateFormat = SimpleDateFormat("dd MMMM yyyy", Locale.getDefault())
    val dateString = dateFormat.format(Date(document.date))

    val clinicName = document.clinicName.ifBlank { profile?.clinicName ?: "Aadhar Clinic" }
    val clinicAddress = document.clinicAddress.ifBlank { profile?.address ?: "Pune" }
    val clinicPhone = document.clinicContact.ifBlank { profile?.contactNumber ?: "+91 98230 12345" }
    val doctorName = document.doctorName.ifBlank { profile?.doctorName ?: "Dr. Sanket Ghule" }
    val qualification = document.doctorQualification.ifBlank { profile?.qualification ?: "BAMS EMS" }
    val regNumber = document.doctorRegNumber.ifBlank { profile?.regNumber ?: "MCIM/EMS-74892" }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.95f)
                .fillMaxHeight(0.92f),
            shape = RoundedCornerShape(18.dp),
            color = MaterialTheme.colorScheme.background,
            tonalElevation = 8.dp
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                // Header Bar
                Surface(
                    color = MaterialTheme.colorScheme.surface,
                    tonalElevation = 3.dp
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Filled.Article, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = if (isReferral) "Referral Letter Preview" else "Medical Certificate Preview",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        IconButton(onClick = onDismiss) {
                            Icon(Icons.Filled.Close, contentDescription = "Close")
                        }
                    }
                }

                // Document Body (Exact A4 Letterhead Styled Simulation)
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .padding(16.dp)
                        .background(Color.White, RoundedCornerShape(8.dp))
                        .border(1.dp, Color(0xFFE0E0E0), RoundedCornerShape(8.dp))
                ) {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(24.dp)
                    ) {
                        // Letterhead
                        item {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(38.dp)
                                        .clip(CircleShape)
                                        .background(Color(0xFF006A60)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        Icons.Filled.LocalHospital,
                                        contentDescription = null,
                                        tint = Color.White,
                                        modifier = Modifier.size(22.dp)
                                    )
                                }
                                Spacer(modifier = Modifier.width(12.dp))
                                Column {
                                    Text(
                                        text = clinicName.uppercase(),
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = Color(0xFF006A60),
                                        fontFamily = FontFamily.Serif
                                    )
                                    Text(
                                        text = "$clinicAddress | Ph: $clinicPhone",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = Color(0xFF555555)
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(10.dp))
                            HorizontalDivider(thickness = 2.dp, color = Color(0xFF006A60))
                            Spacer(modifier = Modifier.height(2.dp))
                            HorizontalDivider(thickness = 0.5.dp, color = Color(0xFFB0C4C2))
                            Spacer(modifier = Modifier.height(18.dp))
                        }

                        if (isReferral) {
                            // Referral Format
                            item {
                                Text(
                                    text = "To,",
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF111111),
                                    fontFamily = FontFamily.Serif
                                )
                                Text(
                                    text = document.hospitalName,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF111111),
                                    fontFamily = FontFamily.Serif
                                )
                                Text(
                                    text = document.hospitalAddress,
                                    color = Color(0xFF333333),
                                    fontFamily = FontFamily.Serif,
                                    style = MaterialTheme.typography.bodyMedium
                                )
                                Spacer(modifier = Modifier.height(14.dp))
                                Text(
                                    text = "Date: $dateString",
                                    color = Color(0xFF333333),
                                    fontFamily = FontFamily.Serif
                                )
                                Spacer(modifier = Modifier.height(14.dp))
                                Text(
                                    text = "Subject: Referral for Further Evaluation and Management",
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF111111),
                                    fontFamily = FontFamily.Serif
                                )
                                Spacer(modifier = Modifier.height(14.dp))
                                Text(
                                    text = "Dear Sir/Madam,",
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF111111),
                                    fontFamily = FontFamily.Serif
                                )
                                Spacer(modifier = Modifier.height(14.dp))

                                val complaints = document.chiefComplaints.ifBlank { "clinical symptoms" }
                                val treatment = document.treatmentGiven.ifBlank { "supportive symptomatic therapy" }
                                Text(
                                    text = "This is to refer ${document.patientName}, aged ${document.age} years, ${document.sex}, who presented with $complaints. The patient was examined and treated at our clinic with $treatment. In view of the patient's clinical condition, further evaluation and appropriate management at your hospital is advised.",
                                    style = MaterialTheme.typography.bodyMedium,
                                    lineHeight = 22.sp,
                                    color = Color(0xFF222222),
                                    fontFamily = FontFamily.Serif
                                )
                                Spacer(modifier = Modifier.height(14.dp))
                                Text(
                                    text = "Kindly evaluate the patient and provide further management as deemed appropriate.",
                                    style = MaterialTheme.typography.bodyMedium,
                                    lineHeight = 22.sp,
                                    color = Color(0xFF222222),
                                    fontFamily = FontFamily.Serif
                                )
                                Spacer(modifier = Modifier.height(16.dp))
                                Text(
                                    text = "Thank you.",
                                    color = Color(0xFF222222),
                                    fontFamily = FontFamily.Serif
                                )
                            }
                        } else {
                            // Sickness Certificate Format
                            item {
                                Text(
                                    text = "Date: $dateString",
                                    color = Color(0xFF333333),
                                    fontFamily = FontFamily.Serif
                                )
                                Spacer(modifier = Modifier.height(20.dp))
                                Text(
                                    text = "To Whom It May Concern",
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF111111),
                                    fontFamily = FontFamily.Serif
                                )
                                Spacer(modifier = Modifier.height(20.dp))

                                Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                        Text(
                                            text = "MEDICAL CERTIFICATE",
                                            style = MaterialTheme.typography.titleMedium,
                                            fontWeight = FontWeight.Bold,
                                            color = Color(0xFF004D40),
                                            fontFamily = FontFamily.Serif,
                                            letterSpacing = 1.sp
                                        )
                                        Spacer(modifier = Modifier.height(4.dp))
                                        Box(
                                            modifier = Modifier
                                                .width(180.dp)
                                                .height(1.5.dp)
                                                .background(Color(0xFF006A60))
                                        )
                                    }
                                }

                                Spacer(modifier = Modifier.height(24.dp))

                                val residence = document.residentOf.ifBlank { "Pune" }
                                val days = if (document.daysCount > 0) document.daysCount else 1
                                val daysStr = if (days == 1) "1 day" else "$days days"

                                Text(
                                    text = "This is to certify that ${document.patientName}, aged ${document.age} years, ${document.sex}, resident of $residence, was examined at $clinicName and is advised medical rest for $daysStr on medical grounds.",
                                    style = MaterialTheme.typography.bodyMedium,
                                    lineHeight = 24.sp,
                                    color = Color(0xFF222222),
                                    fontFamily = FontFamily.Serif
                                )
                                Spacer(modifier = Modifier.height(16.dp))
                                Text(
                                    text = "This certificate is issued upon request for the purpose for which it may be required.",
                                    style = MaterialTheme.typography.bodyMedium,
                                    lineHeight = 24.sp,
                                    color = Color(0xFF222222),
                                    fontFamily = FontFamily.Serif
                                )
                            }
                        }

                        // Signature Block
                        item {
                            Spacer(modifier = Modifier.height(40.dp))
                            Text(
                                text = "Signature: __________________________",
                                style = MaterialTheme.typography.bodyMedium,
                                color = Color(0xFF444444),
                                fontFamily = FontFamily.Serif
                            )
                            Spacer(modifier = Modifier.height(14.dp))
                            Text(
                                text = doctorName,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF111111),
                                fontFamily = FontFamily.Serif
                            )
                            Text(
                                text = qualification,
                                style = MaterialTheme.typography.bodySmall,
                                color = Color(0xFF555555),
                                fontFamily = FontFamily.Serif
                            )
                            Text(
                                text = "Reg. No: $regNumber",
                                style = MaterialTheme.typography.bodySmall,
                                color = Color(0xFF555555),
                                fontFamily = FontFamily.Serif
                            )
                            Text(
                                text = clinicName,
                                style = MaterialTheme.typography.bodySmall,
                                color = Color(0xFF555555),
                                fontFamily = FontFamily.Serif
                            )
                        }
                    }
                }

                // Action Footer
                Surface(
                    color = MaterialTheme.colorScheme.surface,
                    tonalElevation = 4.dp
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Button(
                            onClick = onPrint,
                            modifier = Modifier
                                .weight(1.3f)
                                .height(46.dp)
                                .testTag("btn_modal_print"),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Icon(Icons.Filled.Print, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Reprint A4", fontWeight = FontWeight.Bold)
                        }

                        OutlinedButton(
                            onClick = onSavePdf,
                            modifier = Modifier
                                .weight(1f)
                                .height(46.dp)
                                .testTag("btn_modal_save_pdf"),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Icon(Icons.Filled.FileDownload, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Save")
                        }

                        OutlinedButton(
                            onClick = onShare,
                            modifier = Modifier
                                .weight(1f)
                                .height(46.dp)
                                .testTag("btn_modal_share"),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Icon(Icons.Filled.Share, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Share")
                        }
                    }
                }
            }
        }
    }
}

/**
 * Add / Edit Referral Hospital Dialog
 */
@Composable
fun AddHospitalDialog(
    initialHospital: Hospital? = null,
    onDismiss: () -> Unit,
    onSave: (Hospital) -> Unit
) {
    var name by remember { mutableStateOf(initialHospital?.name ?: "") }
    var address by remember { mutableStateOf(initialHospital?.address ?: "") }
    var contact by remember { mutableStateOf(initialHospital?.contactNumber ?: "") }
    var specialties by remember { mutableStateOf(initialHospital?.specialties ?: "") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = if (initialHospital == null) "Add Referral Hospital" else "Edit Referral Hospital",
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text(
                    text = "Saved hospitals can be quickly selected in Referral Letters.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.outline
                )

                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Hospital Name *") },
                    placeholder = { Text("e.g. Sahyadri Super Speciality Hospital") },
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("input_hospital_name"),
                    singleLine = true
                )

                OutlinedTextField(
                    value = address,
                    onValueChange = { address = it },
                    label = { Text("Hospital Address *") },
                    placeholder = { Text("e.g. Karve Road, Deccan Gymkhana, Pune - 411004") },
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("input_hospital_address"),
                    minLines = 2,
                    maxLines = 3
                )

                OutlinedTextField(
                    value = contact,
                    onValueChange = { contact = it },
                    label = { Text("Phone / Emergency Contact") },
                    placeholder = { Text("+91 20 6721 3000") },
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone)
                )

                OutlinedTextField(
                    value = specialties,
                    onValueChange = { specialties = it },
                    label = { Text("Specialties (Optional)") },
                    placeholder = { Text("ICU, Cardiology, Trauma, Surgery") },
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (name.isNotBlank() && address.isNotBlank()) {
                        val hosp = (initialHospital ?: Hospital(name = "", address = "")).copy(
                            name = name.trim(),
                            address = address.trim(),
                            contactNumber = contact.trim(),
                            specialties = specialties.trim()
                        )
                        onSave(hosp)
                    }
                },
                enabled = name.isNotBlank() && address.isNotBlank(),
                modifier = Modifier.testTag("btn_save_hospital")
            ) {
                Text("Save Hospital")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}
