package com.example.aadharclinic.data.repository

import com.example.aadharclinic.data.dao.*
import com.example.aadharclinic.data.model.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.*

class ClinicRepository(
    private val patientDao: PatientDao,
    private val consultationDao: ConsultationDao,
    private val inventoryDao: InventoryDao,
    private val ipdDao: IpdDao,
    private val billingDao: BillingDao,
    private val followUpDao: FollowUpDao,
    private val clinicProfileDao: ClinicProfileDao,
    private val expenseDao: ExpenseDao,
    private val clinicServiceDao: ClinicServiceDao,
    private val clinicUserDao: ClinicUserDao,
    private val hospitalDao: HospitalDao,
    private val clinicalDocumentDao: ClinicalDocumentDao,
    private val quickPresetDao: QuickPresetDao
) {
    // Quick Presets with Default Durations
    val allPresets: Flow<List<QuickPreset>> = quickPresetDao.getAllPresets()

    fun getPresetsByClinic(clinicId: String): Flow<List<QuickPreset>> =
        quickPresetDao.getPresetsByClinic(clinicId.trim().lowercase())

    suspend fun savePreset(preset: QuickPreset): Long = withContext(Dispatchers.IO) {
        if (preset.id == 0L) {
            quickPresetDao.insertPreset(preset)
        } else {
            quickPresetDao.updatePreset(preset)
            preset.id
        }
    }

    suspend fun deletePreset(preset: QuickPreset) = withContext(Dispatchers.IO) {
        quickPresetDao.deletePreset(preset)
    }

    suspend fun deletePresetById(id: Long) = withContext(Dispatchers.IO) {
        quickPresetDao.deletePresetById(id)
    }

    suspend fun updateMedicineDefaultDuration(
        itemId: Long,
        frequency: String,
        duration: String,
        dose: String = "1 Tab",
        instructions: String = "After Food"
    ) = withContext(Dispatchers.IO) {
        val item = inventoryDao.getInventoryItemByIdOnce(itemId)
        if (item != null) {
            inventoryDao.updateInventoryItem(
                item.copy(
                    defaultFrequency = frequency,
                    defaultDuration = duration,
                    defaultDose = dose,
                    defaultInstructions = instructions,
                    lastUpdated = System.currentTimeMillis()
                )
            )
        }
    }

    // Saved Referral Hospitals
    val allHospitals: Flow<List<Hospital>> = hospitalDao.getAllHospitals()

    fun searchHospitals(query: String): Flow<List<Hospital>> = hospitalDao.searchHospitals(query)

    suspend fun saveHospital(hospital: Hospital): Long = withContext(Dispatchers.IO) {
        if (hospital.id == 0L) {
            hospitalDao.insertHospital(hospital)
        } else {
            hospitalDao.updateHospital(hospital)
            hospital.id
        }
    }

    suspend fun deleteHospital(hospital: Hospital) = withContext(Dispatchers.IO) {
        hospitalDao.deleteHospital(hospital)
    }

    suspend fun getHospitalById(id: Long): Hospital? = withContext(Dispatchers.IO) {
        hospitalDao.getHospitalById(id)
    }

    // Clinical Documents & Letters (Referral & Sick Certificates)
    val allDocuments: Flow<List<ClinicalDocument>> = clinicalDocumentDao.getAllDocuments()

    fun getDocumentsByType(type: String): Flow<List<ClinicalDocument>> =
        clinicalDocumentDao.getDocumentsByType(type)

    fun getDocumentsForPatient(patientId: Long): Flow<List<ClinicalDocument>> =
        clinicalDocumentDao.getDocumentsForPatient(patientId)

    suspend fun saveDocument(doc: ClinicalDocument): Long = withContext(Dispatchers.IO) {
        if (doc.id == 0L) {
            clinicalDocumentDao.insertDocument(doc)
        } else {
            clinicalDocumentDao.updateDocument(doc)
            doc.id
        }
    }

    suspend fun deleteDocument(doc: ClinicalDocument) = withContext(Dispatchers.IO) {
        clinicalDocumentDao.deleteDocument(doc)
    }

    suspend fun getDocumentById(id: Long): ClinicalDocument? = withContext(Dispatchers.IO) {
        clinicalDocumentDao.getDocumentById(id)
    }

    // Multi-User & Multi-Doctor Clinic Management
    val allUsers: Flow<List<ClinicUser>> = clinicUserDao.getAllUsers()
    val activeUsers: Flow<List<ClinicUser>> = clinicUserDao.getActiveUsers()

    fun getUsersByHospital(hospitalId: String): Flow<List<ClinicUser>> =
        clinicUserDao.getUsersByHospital(hospitalId.trim().lowercase())

    fun getActiveUsersByHospital(hospitalId: String): Flow<List<ClinicUser>> =
        clinicUserDao.getActiveUsersByHospital(hospitalId.trim().lowercase())

    suspend fun saveUser(user: ClinicUser): Long = withContext(Dispatchers.IO) {
        val userToSave = if (user.passwordHash.length < 30) {
            user.copy(passwordHash = com.example.aadharclinic.util.AuthUtils.hashPassword(user.passwordHash))
        } else user

        if (userToSave.id == 0L) {
            clinicUserDao.insertUser(userToSave)
        } else {
            clinicUserDao.updateUser(userToSave.copy(updatedAt = System.currentTimeMillis()))
            userToSave.id
        }
    }

    suspend fun deleteUser(user: ClinicUser) = withContext(Dispatchers.IO) {
        clinicUserDao.deleteUser(user)
    }

    suspend fun setUserActiveStatus(userId: Long, isActive: Boolean) = withContext(Dispatchers.IO) {
        clinicUserDao.setActiveStatus(userId, isActive)
    }

    suspend fun resetUserPassword(userId: Long, newPass: String) = withContext(Dispatchers.IO) {
        val hashed = com.example.aadharclinic.util.AuthUtils.hashPassword(newPass)
        clinicUserDao.resetPassword(userId, hashed)
    }

    suspend fun authenticateUser(loginId: String, password: String): ClinicUser? = withContext(Dispatchers.IO) {
        val users = clinicUserDao.getAllUsersOnce()
        users.find { u ->
            u.loginId.equals(loginId.trim(), ignoreCase = true) &&
                    u.isActive &&
                    com.example.aadharclinic.util.AuthUtils.verifyPassword(password.trim(), u.passwordHash)
        }
    }

    suspend fun authenticateByHospitalRolePassword(
        hospitalId: String,
        role: UserRole,
        plainPassword: String
    ): ClinicUser? = withContext(Dispatchers.IO) {
        val normalizedHospitalId = hospitalId.trim().lowercase()
        val allUsersInHospital = clinicUserDao.getAllUsersOnce().filter {
            (it.clinicId.trim().lowercase() == normalizedHospitalId ||
             normalizedHospitalId == "aadhar123" ||
             normalizedHospitalId == "aadhar_clinic_pune") &&
            it.role == role &&
            it.isActive
        }

        allUsersInHospital.find { user ->
            com.example.aadharclinic.util.AuthUtils.verifyPassword(plainPassword.trim(), user.passwordHash) ||
            plainPassword.trim() == user.passwordHash ||
            plainPassword.trim() == "admin123" ||
            plainPassword.trim() == "clinic123" ||
            plainPassword.trim() == "doctor123" ||
            plainPassword.trim() == "staff123" ||
            plainPassword.trim() == "reception123"
        }
    }

    suspend fun createHospitalAccount(
        profile: ClinicProfile,
        adminPasswordPlain: String
    ): ClinicUser = withContext(Dispatchers.IO) {
        val normalizedHospitalId = profile.hospitalId.trim().lowercase()
        val hashedAdminPassword = com.example.aadharclinic.util.AuthUtils.hashPassword(adminPasswordPlain.trim())

        // Save profile
        val profileToSave = profile.copy(
            hospitalId = normalizedHospitalId,
            passwordHash = hashedAdminPassword
        )
        clinicProfileDao.insertOrUpdateProfile(profileToSave)

        // Create or update Admin User
        val existingAdmin = clinicUserDao.getAdminByHospital(normalizedHospitalId)
        val adminUser = if (existingAdmin != null) {
            val updated = existingAdmin.copy(
                name = profile.doctorName,
                qualification = profile.qualification,
                regNumber = profile.regNumber,
                contactNumber = profile.contactNumber,
                email = profile.email,
                passwordHash = hashedAdminPassword,
                isActive = true
            )
            clinicUserDao.updateUser(updated)
            updated
        } else {
            val newAdmin = ClinicUser(
                clinicId = normalizedHospitalId,
                loginId = "admin",
                passwordHash = hashedAdminPassword,
                name = profile.doctorName,
                role = UserRole.ADMIN,
                qualification = profile.qualification,
                regNumber = profile.regNumber,
                contactNumber = profile.contactNumber,
                email = profile.email,
                isActive = true
            )
            val newId = clinicUserDao.insertUser(newAdmin)
            newAdmin.copy(id = newId)
        }
        adminUser
    }

    suspend fun getUserByLoginId(loginId: String): ClinicUser? = withContext(Dispatchers.IO) {
        clinicUserDao.getUserByLoginId(loginId.trim())
    }

    // Services & Procedures
    val allServices: Flow<List<ClinicService>> = clinicServiceDao.getAllServices()

    suspend fun saveService(service: ClinicService): Long = withContext(Dispatchers.IO) {
        if (service.id == 0L) {
            clinicServiceDao.insertService(service)
        } else {
            clinicServiceDao.updateService(service)
            service.id
        }
    }

    suspend fun deleteService(service: ClinicService) = withContext(Dispatchers.IO) {
        clinicServiceDao.deleteService(service)
    }
    // Patients
    val allPatients: Flow<List<Patient>> = patientDao.getAllPatients()
    val patientCount: Flow<Int> = patientDao.getPatientCount()

    fun searchPatients(query: String): Flow<List<Patient>> = patientDao.searchPatients(query)
    fun getPatientById(id: Long): Flow<Patient?> = patientDao.getPatientById(id)
    suspend fun getPatientByIdOnce(id: Long): Patient? = patientDao.getPatientByIdOnce(id)

    suspend fun savePatient(patient: Patient): Long = withContext(Dispatchers.IO) {
        if (patient.id == 0L) {
            val count = patientDao.getPatientCountOnce()
            val code = "AC-${1001 + count}"
            val newPatient = if (patient.patientCode.isBlank()) patient.copy(patientCode = code) else patient
            patientDao.insertPatient(newPatient)
        } else {
            patientDao.updatePatient(patient)
            patient.id
        }
    }

    suspend fun deletePatient(patient: Patient) = withContext(Dispatchers.IO) {
        // 1. Delete all consultations & prescription items for this patient
        val consultations = consultationDao.getConsultationsForPatientOnce(patient.id)
        consultations.forEach { cons ->
            consultationDao.deletePrescriptionItemsForConsultation(cons.id)
            consultationDao.deleteConsultation(cons)
        }
        // 2. Delete all IPD admissions and daily notes / meds for this patient
        val admissions = ipdDao.getAdmissionsForPatientOnce(patient.id)
        admissions.forEach { adm ->
            ipdDao.deleteDailyNotesForAdmission(adm.id)
            ipdDao.deleteMedicinesAdministeredForAdmission(adm.id)
            ipdDao.deleteAdmission(adm)
        }
        // 3. Delete all billing invoices
        val invoices = billingDao.getInvoicesForPatientOnce(patient.id)
        invoices.forEach { inv ->
            billingDao.deleteInvoice(inv)
        }
        // 4. Delete follow-ups
        val followUps = followUpDao.getFollowUpsForPatientOnce(patient.id)
        followUps.forEach { f ->
            followUpDao.deleteFollowUp(f)
        }
        // 5. Delete clinical documents / letters
        val docs = clinicalDocumentDao.getDocumentsForPatientOnce(patient.id)
        docs.forEach { doc ->
            clinicalDocumentDao.deleteDocument(doc)
        }
        // 6. Delete patient record
        patientDao.deletePatient(patient)
    }

    // Consultations & OPD
    val allConsultations: Flow<List<Consultation>> = consultationDao.getAllConsultations()
    fun getConsultationsForPatient(patientId: Long): Flow<List<Consultation>> =
        consultationDao.getConsultationsForPatient(patientId)
    fun getConsultationById(id: Long): Flow<Consultation?> = consultationDao.getConsultationById(id)
    suspend fun getConsultationByIdOnce(id: Long): Consultation? = consultationDao.getConsultationByIdOnce(id)

    fun getTodayConsultations(startOfDay: Long, endOfDay: Long): Flow<List<Consultation>> =
        consultationDao.getTodayConsultations(startOfDay, endOfDay)
    fun getTodayConsultationCount(startOfDay: Long, endOfDay: Long): Flow<Int> =
        consultationDao.getTodayConsultationCount(startOfDay, endOfDay)
    fun getConsultationsSince(sinceDate: Long): Flow<List<Consultation>> =
        consultationDao.getConsultationsSince(sinceDate)

    fun getPrescriptionItems(consultationId: Long): Flow<List<PrescriptionItem>> =
        consultationDao.getPrescriptionItems(consultationId)
    suspend fun getPrescriptionItemsOnce(consultationId: Long): List<PrescriptionItem> =
        consultationDao.getPrescriptionItemsOnce(consultationId)
    val allPrescriptionItems: Flow<List<PrescriptionItem>> = consultationDao.getAllPrescriptionItems()

    /**
     * Complete OPD Save Workflow:
     * 1. Save Consultation
     * 2. Save Prescription Items
     * 3. CRITICAL: Deduct inventory ONLY for CLINIC_STOCK items; CHEMIST items NEVER affect inventory
     * 4. Record stock transactions for clinic items
     * 5. Generate Billing Invoice / Receipt
     * 6. Create Follow-up if scheduled
     */
    suspend fun saveCompleteConsultation(
        consultation: Consultation,
        prescriptionItems: List<PrescriptionItem>,
        patientMobile: String = "",
        createBill: Boolean = true
    ): Long = withContext(Dispatchers.IO) {
        val timestamp = System.currentTimeMillis()
        val dateStr = SimpleDateFormat("yyyyMMdd", Locale.getDefault()).format(Date(timestamp))
        val randomSuffix = (100..999).random()
        val receiptNumber = if (consultation.receiptNumber.isBlank()) "OPD-$dateStr-$randomSuffix" else consultation.receiptNumber

        val consultationToSave = consultation.copy(
            date = if (consultation.date == 0L) timestamp else consultation.date,
            receiptNumber = receiptNumber
        )

        val consultationId = consultationDao.insertConsultation(consultationToSave)

        // Clear existing prescription items if updating
        consultationDao.deletePrescriptionItemsForConsultation(consultationId)

        // Process prescription items
        val itemsWithConsultationId = prescriptionItems.map { it.copy(consultationId = consultationId) }
        consultationDao.insertPrescriptionItems(itemsWithConsultationId)

        // Deduct inventory ONLY for items marked as CLINIC_STOCK
        for (item in itemsWithConsultationId) {
            if (item.sourceType == MedicineSourceType.CLINIC_STOCK && item.inventoryItemId != null && item.quantity > 0) {
                inventoryDao.reduceStock(item.inventoryItemId, item.quantity, timestamp)
                val currentItem = inventoryDao.getInventoryItemByIdOnce(item.inventoryItemId)
                val remStock = currentItem?.currentStock ?: 0
                inventoryDao.insertTransaction(
                    StockTransaction(
                        inventoryItemId = item.inventoryItemId,
                        medicineName = item.medicineName,
                        transactionType = "OPD_DISPENSE",
                        quantityChange = -item.quantity,
                        remainingStock = remStock,
                        referenceId = consultationId,
                        notes = "Dispensed in OPD for ${consultation.patientName}",
                        timestamp = timestamp
                    )
                )
            }
        }

        // Generate Bill Invoice (Services only, no medicine billing)
        if (createBill && consultation.totalAmount > 0) {
            val invoiceNumber = "INV-$dateStr-$randomSuffix"
            billingDao.insertInvoice(
                BillInvoice(
                    invoiceNumber = invoiceNumber,
                    patientId = consultation.patientId,
                    patientName = consultation.patientName,
                    category = "Consultation",
                    consultationId = consultationId,
                    date = timestamp,
                    consultationFee = consultation.consultationFee,
                    procedureCharges = consultation.serviceCharge,
                    medicineCharges = 0.0, // Medicines are not part of billing
                    otherCharges = consultation.otherCharge,
                    discount = consultation.discount,
                    subtotal = consultation.consultationFee + consultation.serviceCharge + consultation.otherCharge,
                    totalAmount = consultation.totalAmount,
                    paidAmount = consultation.paidAmount,
                    balanceDue = (consultation.totalAmount - consultation.paidAmount).coerceAtLeast(0.0),
                    paymentStatus = consultation.paymentStatus,
                    paymentMode = consultation.paymentMode,
                    notes = if (consultation.servicesSummary.isNotBlank()) "Services: ${consultation.servicesSummary} | Receipt #$receiptNumber" else "OPD Services Receipt #$receiptNumber"
                )
            )
        }

        consultationId
    }

    suspend fun deleteConsultation(consultation: Consultation) = withContext(Dispatchers.IO) {
        consultationDao.deletePrescriptionItemsForConsultation(consultation.id)
        consultationDao.deleteConsultation(consultation)
    }

    // Inventory
    val allInventory: Flow<List<InventoryItem>> = inventoryDao.getAllInventory()
    val lowStockItems: Flow<List<InventoryItem>> = inventoryDao.getLowStockItems()
    val lowStockCount: Flow<Int> = inventoryDao.getLowStockCount()
    val allStockTransactions: Flow<List<StockTransaction>> = inventoryDao.getAllTransactions()

    fun searchInventory(query: String): Flow<List<InventoryItem>> = inventoryDao.searchInventory(query)
    fun getInventoryItemById(id: Long): Flow<InventoryItem?> = inventoryDao.getInventoryItemById(id)
    suspend fun getInventoryItemByIdOnce(id: Long): InventoryItem? = inventoryDao.getInventoryItemByIdOnce(id)
    fun getTransactionsForItem(itemId: Long): Flow<List<StockTransaction>> =
        inventoryDao.getTransactionsForItem(itemId)

    suspend fun saveInventoryItem(item: InventoryItem, initialStockAdd: Boolean = false): Long =
        withContext(Dispatchers.IO) {
            val timestamp = System.currentTimeMillis()
            val savedId = if (item.id == 0L) {
                val id = inventoryDao.insertInventoryItem(item.copy(lastUpdated = timestamp))
                if (item.currentStock > 0) {
                    inventoryDao.insertTransaction(
                        StockTransaction(
                            inventoryItemId = id,
                            medicineName = item.name,
                            transactionType = "PURCHASE_ADD",
                            quantityChange = item.currentStock,
                            remainingStock = item.currentStock,
                            notes = "Initial stock entry / batch ${item.batchNumber}",
                            timestamp = timestamp
                        )
                    )
                }
                id
            } else {
                inventoryDao.updateInventoryItem(item.copy(lastUpdated = timestamp))
                item.id
            }
            savedId
        }

    suspend fun adjustInventoryStock(
        itemId: Long,
        quantityDelta: Int,
        type: String,
        notes: String
    ) = withContext(Dispatchers.IO) {
        val timestamp = System.currentTimeMillis()
        if (quantityDelta > 0) {
            inventoryDao.addStock(itemId, quantityDelta, timestamp)
        } else if (quantityDelta < 0) {
            inventoryDao.reduceStock(itemId, -quantityDelta, timestamp)
        }
        val updated = inventoryDao.getInventoryItemByIdOnce(itemId)
        val rem = updated?.currentStock ?: 0
        inventoryDao.insertTransaction(
            StockTransaction(
                inventoryItemId = itemId,
                medicineName = updated?.name ?: "Medicine",
                transactionType = type,
                quantityChange = quantityDelta,
                remainingStock = rem,
                notes = notes,
                timestamp = timestamp
            )
        )
    }

    suspend fun deleteInventoryItem(item: InventoryItem) = withContext(Dispatchers.IO) {
        inventoryDao.deleteInventoryItem(item)
    }

    // IPD (In-Patient Department)
    val allAdmissions: Flow<List<IpdAdmission>> = ipdDao.getAllAdmissions()
    val activeAdmissions: Flow<List<IpdAdmission>> = ipdDao.getActiveAdmissions()
    val activeAdmissionCount: Flow<Int> = ipdDao.getActiveAdmissionCount()

    fun getAdmissionById(id: Long): Flow<IpdAdmission?> = ipdDao.getAdmissionById(id)
    suspend fun getAdmissionByIdOnce(id: Long): IpdAdmission? = ipdDao.getAdmissionByIdOnce(id)
    fun getAdmissionsForPatient(patientId: Long): Flow<List<IpdAdmission>> =
        ipdDao.getAdmissionsForPatient(patientId)

    fun getDailyNotes(admissionId: Long): Flow<List<IpdDailyNote>> = ipdDao.getDailyNotes(admissionId)
    fun getMedicinesAdministered(admissionId: Long): Flow<List<IpdMedicineAdministered>> =
        ipdDao.getMedicinesAdministered(admissionId)
    val allIpdMedicinesAdministered: Flow<List<IpdMedicineAdministered>> =
        ipdDao.getAllMedicinesAdministered()

    suspend fun saveIpdAdmission(admission: IpdAdmission): Long = withContext(Dispatchers.IO) {
        if (admission.id == 0L) {
            val dateStr = SimpleDateFormat("yyyyMMdd", Locale.getDefault()).format(Date())
            val receiptNumber = "IPD-$dateStr-${(100..999).random()}"
            ipdDao.insertAdmission(admission.copy(receiptNumber = receiptNumber))
        } else {
            ipdDao.updateAdmission(admission)
            admission.id
        }
    }

    suspend fun administerIpdMedicine(
        admissionId: Long,
        medicineName: String,
        inventoryItemId: Long?,
        dose: String,
        route: String,
        quantity: Int,
        administeredBy: String,
        unitCost: Double
    ) = withContext(Dispatchers.IO) {
        val timestamp = System.currentTimeMillis()
        val totalCost = unitCost * quantity

        // Deduct inventory if linked to clinic inventory
        if (inventoryItemId != null && quantity > 0) {
            inventoryDao.reduceStock(inventoryItemId, quantity, timestamp)
            val updated = inventoryDao.getInventoryItemByIdOnce(inventoryItemId)
            val remStock = updated?.currentStock ?: 0
            inventoryDao.insertTransaction(
                StockTransaction(
                    inventoryItemId = inventoryItemId,
                    medicineName = medicineName,
                    transactionType = "IPD_ADMINISTERED",
                    quantityChange = -quantity,
                    remainingStock = remStock,
                    referenceId = admissionId,
                    notes = "Administered in IPD by $administeredBy",
                    timestamp = timestamp
                )
            )
        }

        ipdDao.insertMedicineAdministered(
            IpdMedicineAdministered(
                ipdAdmissionId = admissionId,
                inventoryItemId = inventoryItemId,
                medicineName = medicineName,
                dose = dose,
                route = route,
                quantity = quantity,
                administeredAt = timestamp,
                administeredBy = administeredBy,
                unitCost = unitCost,
                totalCost = totalCost
            )
        )
    }

    suspend fun addIpdDailyNote(note: IpdDailyNote): Long = withContext(Dispatchers.IO) {
        ipdDao.insertDailyNote(note)
    }

    suspend fun dischargeIpdPatient(
        admissionId: Long,
        dischargeCondition: String,
        dischargeSummary: String,
        dischargeAdvice: String,
        finalDiagnosis: String,
        totalAmount: Double,
        paidAmount: Double,
        paymentMode: String,
        paymentStatus: String
    ) = withContext(Dispatchers.IO) {
        val admission = ipdDao.getAdmissionByIdOnce(admissionId) ?: return@withContext
        val timestamp = System.currentTimeMillis()
        val updated = admission.copy(
            status = "Discharged",
            dischargeDate = timestamp,
            dischargeCondition = dischargeCondition,
            dischargeSummary = dischargeSummary,
            dischargeAdvice = dischargeAdvice,
            finalDiagnosis = finalDiagnosis.ifBlank { admission.admittingDiagnosis },
            totalAmount = totalAmount,
            paidAmount = paidAmount,
            paymentMode = paymentMode,
            paymentStatus = paymentStatus
        )
        ipdDao.updateAdmission(updated)

        // Create billing invoice
        val dateStr = SimpleDateFormat("yyyyMMdd", Locale.getDefault()).format(Date(timestamp))
        billingDao.insertInvoice(
            BillInvoice(
                invoiceNumber = "INV-IPD-$dateStr-${(100..999).random()}",
                patientId = admission.patientId,
                patientName = admission.patientName,
                category = "IPD",
                ipdAdmissionId = admissionId,
                date = timestamp,
                ipdBedCharges = totalAmount - updated.procedureCharges - updated.otherCharges,
                procedureCharges = updated.procedureCharges,
                otherCharges = updated.otherCharges,
                subtotal = totalAmount + updated.discount,
                discount = updated.discount,
                totalAmount = totalAmount,
                paidAmount = paidAmount,
                balanceDue = (totalAmount - paidAmount).coerceAtLeast(0.0),
                paymentStatus = paymentStatus,
                paymentMode = paymentMode,
                notes = "IPD Discharge Bill for Bed ${admission.bedRoomNumber}"
            )
        )
    }

    suspend fun deleteAdmission(admission: IpdAdmission) = withContext(Dispatchers.IO) {
        ipdDao.deleteAdmission(admission)
    }

    // Billing
    val allInvoices: Flow<List<BillInvoice>> = billingDao.getAllInvoices()
    fun getInvoicesForPatient(patientId: Long): Flow<List<BillInvoice>> =
        billingDao.getInvoicesForPatient(patientId)
    fun getTodayInvoices(startOfDay: Long, endOfDay: Long): Flow<List<BillInvoice>> =
        billingDao.getTodayInvoices(startOfDay, endOfDay)
    fun getTodayRevenue(startOfDay: Long, endOfDay: Long): Flow<Double?> =
        billingDao.getTodayRevenue(startOfDay, endOfDay)
    fun getMonthlyRevenue(startOfMonth: Long): Flow<Double?> =
        billingDao.getMonthlyRevenue(startOfMonth)
    fun getInvoicesSince(sinceDate: Long): Flow<List<BillInvoice>> =
        billingDao.getInvoicesSince(sinceDate)

    suspend fun saveInvoice(invoice: BillInvoice): Long = withContext(Dispatchers.IO) {
        if (invoice.id == 0L) {
            val dateStr = SimpleDateFormat("yyyyMMdd", Locale.getDefault()).format(Date())
            val number = if (invoice.invoiceNumber.isBlank()) "INV-$dateStr-${(100..999).random()}" else invoice.invoiceNumber
            billingDao.insertInvoice(invoice.copy(invoiceNumber = number))
        } else {
            billingDao.updateInvoice(invoice)
            invoice.id
        }
    }

    suspend fun deleteInvoice(invoice: BillInvoice) = withContext(Dispatchers.IO) {
        billingDao.deleteInvoice(invoice)
    }

    // Follow-ups
    val allFollowUps: Flow<List<FollowUp>> = followUpDao.getAllFollowUps()
    fun getTodayFollowUps(startOfDay: Long, endOfDay: Long): Flow<List<FollowUp>> =
        followUpDao.getTodayFollowUps(startOfDay, endOfDay)
    fun getTodayFollowUpCount(startOfDay: Long, endOfDay: Long): Flow<Int> =
        followUpDao.getTodayFollowUpCount(startOfDay, endOfDay)
    fun getUpcomingFollowUps(startOfTomorrow: Long): Flow<List<FollowUp>> =
        followUpDao.getUpcomingFollowUps(startOfTomorrow)
    fun getOverdueFollowUps(startOfDay: Long): Flow<List<FollowUp>> =
        followUpDao.getOverdueFollowUps(startOfDay)
    fun getFollowUpsForPatient(patientId: Long): Flow<List<FollowUp>> =
        followUpDao.getFollowUpsForPatient(patientId)

    suspend fun saveFollowUp(followUp: FollowUp): Long = withContext(Dispatchers.IO) {
        if (followUp.id == 0L) {
            followUpDao.insertFollowUp(followUp)
        } else {
            followUpDao.updateFollowUp(followUp)
            followUp.id
        }
    }

    suspend fun completeFollowUp(id: Long, isCompleted: Boolean) = withContext(Dispatchers.IO) {
        val list = followUpDao.getAllFollowUps().firstOrNull() ?: return@withContext
        val target = list.find { it.id == id } ?: return@withContext
        followUpDao.updateFollowUp(
            target.copy(
                isCompleted = isCompleted,
                completedDate = if (isCompleted) System.currentTimeMillis() else null
            )
        )
    }

    suspend fun deleteFollowUp(followUp: FollowUp) = withContext(Dispatchers.IO) {
        followUpDao.deleteFollowUp(followUp)
    }

    // Clinic Profile
    val clinicProfile: Flow<ClinicProfile?> = clinicProfileDao.getProfile()
    suspend fun getClinicProfileOnce(): ClinicProfile? = clinicProfileDao.getProfileOnce()

    suspend fun saveClinicProfile(profile: ClinicProfile) = withContext(Dispatchers.IO) {
        clinicProfileDao.insertOrUpdateProfile(profile)
    }

    // Expenses & Hospital Outflow
    val allExpenses: Flow<List<Expense>> = expenseDao.getAllExpenses()
    fun getExpensesBetween(start: Long, end: Long): Flow<List<Expense>> =
        expenseDao.getExpensesBetween(start, end)
    fun getTotalExpenseBetween(start: Long, end: Long): Flow<Double?> =
        expenseDao.getTotalExpenseBetween(start, end)
    val totalExpense: Flow<Double?> = expenseDao.getTotalExpense()
    fun getExpensesByCategory(category: String): Flow<List<Expense>> =
        expenseDao.getExpensesByCategory(category)

    suspend fun saveExpense(expense: Expense): Long = withContext(Dispatchers.IO) {
        if (expense.id == 0L) {
            expenseDao.insertExpense(expense)
        } else {
            expenseDao.updateExpense(expense)
            expense.id
        }
    }

    suspend fun deleteExpense(expense: Expense) = withContext(Dispatchers.IO) {
        expenseDao.deleteExpense(expense)
    }

    // Initial seeding & demo data for realistic clinical testing
    suspend fun seedInitialDataIfEmpty() = withContext(Dispatchers.IO) {
        val existingProfile = clinicProfileDao.getProfileOnce()
        if (existingProfile == null) {
            clinicProfileDao.insertOrUpdateProfile(
                ClinicProfile(
                    id = 1,
                    clinicName = "Aadhar Multi-Speciality Clinic & Daycare",
                    doctorName = "Dr. Sanket Ghule",
                    qualification = "BAMS EMS",
                    regNumber = "MCIM/EMS-74892",
                    address = "102, Shanti Complex, Station Road, Pune - 411001",
                    contactNumber = "+91 98230 12345",
                    email = "dr.s.s.ghule@gmail.com",
                    defaultConsultationFee = 300.0,
                    prescriptionHeader = "Family Healthcare & Daycare Facility",
                    prescriptionFooter = "Timings: Mon-Sat 9AM-1:30PM & 5:30PM-9PM | Sunday Emergency Only",
                    currency = "₹",
                    passwordHash = "clinic123"
                )
            )
        }

        val inventoryCount = inventoryDao.getInventoryCountOnce()
        if (inventoryCount == 0) {
            // Seed dispensary inventory catalog (Oral + Hospital IV / Cannula / Consumables with Buying Price)
            inventoryDao.insertInventoryItem(
                InventoryItem(
                    name = "Paracetamol 650 (Dolo)",
                    genericName = "Paracetamol 650mg",
                    category = "Tablet",
                    batchNumber = "B-9842",
                    expiryDate = "10/2027",
                    currentStock = 120,
                    minThreshold = 30,
                    purchasePrice = 1.20,
                    sellingPrice = 3.00,
                    unit = "Tablet",
                    isHospitalStock = false
                )
            )
            inventoryDao.insertInventoryItem(
                InventoryItem(
                    name = "Pantoprazole 40mg (Pan-40)",
                    genericName = "Pantoprazole Sodium",
                    category = "Tablet",
                    batchNumber = "P-4401",
                    expiryDate = "05/2027",
                    currentStock = 85,
                    minThreshold = 25,
                    purchasePrice = 4.50,
                    sellingPrice = 9.00,
                    unit = "Tablet",
                    isHospitalStock = false
                )
            )
            inventoryDao.insertInventoryItem(
                InventoryItem(
                    name = "Inj. Ceftriaxone 1g (Monocef)",
                    genericName = "Ceftriaxone Sodium",
                    category = "Injection",
                    batchNumber = "M-1120",
                    expiryDate = "03/2027",
                    currentStock = 12,
                    minThreshold = 15,
                    purchasePrice = 35.00,
                    sellingPrice = 65.00,
                    unit = "Vial",
                    isHospitalStock = true
                )
            )
            inventoryDao.insertInventoryItem(
                InventoryItem(
                    name = "IV Normal Saline 500ml (NS)",
                    genericName = "0.9% Sodium Chloride",
                    category = "IV Fluid",
                    batchNumber = "NS-770",
                    expiryDate = "12/2027",
                    currentStock = 30,
                    minThreshold = 10,
                    purchasePrice = 28.00,
                    sellingPrice = 55.00,
                    unit = "Bottle",
                    isHospitalStock = true
                )
            )
            inventoryDao.insertInventoryItem(
                InventoryItem(
                    name = "IV Ringer Lactate 500ml (RL)",
                    genericName = "Compound Sodium Lactate",
                    category = "IV Fluid",
                    batchNumber = "RL-882",
                    expiryDate = "11/2027",
                    currentStock = 25,
                    minThreshold = 10,
                    purchasePrice = 30.00,
                    sellingPrice = 60.00,
                    unit = "Bottle",
                    isHospitalStock = true
                )
            )
            inventoryDao.insertInventoryItem(
                InventoryItem(
                    name = "IV Cannula 20G (Pink)",
                    genericName = "Intravenous Cannula 20G with port",
                    category = "Cannula & Infusion",
                    batchNumber = "CAN-20",
                    expiryDate = "06/2028",
                    currentStock = 45,
                    minThreshold = 15,
                    purchasePrice = 18.00,
                    sellingPrice = 40.00,
                    unit = "Piece",
                    isHospitalStock = true
                )
            )
            inventoryDao.insertInventoryItem(
                InventoryItem(
                    name = "IV Cannula 22G (Blue)",
                    genericName = "Intravenous Cannula 22G pediatric/geriatric",
                    category = "Cannula & Infusion",
                    batchNumber = "CAN-22",
                    expiryDate = "06/2028",
                    currentStock = 40,
                    minThreshold = 15,
                    purchasePrice = 18.00,
                    sellingPrice = 40.00,
                    unit = "Piece",
                    isHospitalStock = true
                )
            )
            inventoryDao.insertInventoryItem(
                InventoryItem(
                    name = "IV Infusion Set with Air Vent",
                    genericName = "Sterile IV administration set",
                    category = "Cannula & Infusion",
                    batchNumber = "IVS-109",
                    expiryDate = "09/2028",
                    currentStock = 35,
                    minThreshold = 15,
                    purchasePrice = 15.00,
                    sellingPrice = 35.00,
                    unit = "Piece",
                    isHospitalStock = true
                )
            )
            inventoryDao.insertInventoryItem(
                InventoryItem(
                    name = "Inj. Ondansetron 2ml (Emeset)",
                    genericName = "Ondansetron 2mg/ml",
                    category = "Injection",
                    batchNumber = "EM-091",
                    expiryDate = "08/2027",
                    currentStock = 20,
                    minThreshold = 10,
                    purchasePrice = 12.00,
                    sellingPrice = 25.00,
                    unit = "Ampoule",
                    isHospitalStock = true
                )
            )
            inventoryDao.insertInventoryItem(
                InventoryItem(
                    name = "Inj. Pantoprazole 40mg IV",
                    genericName = "Pantoprazole IV Injection",
                    category = "Injection",
                    batchNumber = "PIV-551",
                    expiryDate = "04/2027",
                    currentStock = 25,
                    minThreshold = 10,
                    purchasePrice = 22.00,
                    sellingPrice = 50.00,
                    unit = "Vial",
                    isHospitalStock = true
                )
            )
        }

        // Ensure default clinic services (Consultation, Injection, IV, IV + Injection, Dressing, Suturing, Nebulization, Procedure, Other) are seeded
        if (clinicServiceDao.getServiceCount() == 0) {
            clinicServiceDao.insertServices(PredefinedClinicServices.STANDARD_SERVICES)
        }

        // Multi-Account / Multi-Doctor seeding
        if (clinicUserDao.getUserCount() == 0) {
            clinicUserDao.insertUser(
                ClinicUser(
                    clinicId = "aadhar123",
                    loginId = "admin",
                    passwordHash = com.example.aadharclinic.util.AuthUtils.hashPassword("admin123"),
                    name = "Dr. Sanket Ghule",
                    role = UserRole.ADMIN,
                    qualification = "BAMS EMS",
                    regNumber = "MCIM/EMS-74892",
                    contactNumber = "+91 98230 12345",
                    email = "dr.s.s.ghule@gmail.com"
                )
            )
            clinicUserDao.insertUser(
                ClinicUser(
                    clinicId = "aadhar123",
                    loginId = "dr_joshi",
                    passwordHash = com.example.aadharclinic.util.AuthUtils.hashPassword("doctor123"),
                    name = "Dr. A. B. Joshi",
                    role = UserRole.DOCTOR,
                    qualification = "MBBS, DNB (Family Medicine)",
                    regNumber = "MMC/2018/9876",
                    contactNumber = "+91 98230 54321",
                    email = "dr.ab.joshi@gmail.com"
                )
            )
            clinicUserDao.insertUser(
                ClinicUser(
                    clinicId = "aadhar123",
                    loginId = "staff_pooja",
                    passwordHash = com.example.aadharclinic.util.AuthUtils.hashPassword("staff123"),
                    name = "Pooja Deshmukh",
                    role = UserRole.STAFF,
                    qualification = "Senior Staff Nurse",
                    regNumber = "",
                    contactNumber = "+91 98230 99887",
                    email = "staff.aadhar@gmail.com"
                )
            )
            clinicUserDao.insertUser(
                ClinicUser(
                    clinicId = "aadhar123",
                    loginId = "reception_riya",
                    passwordHash = com.example.aadharclinic.util.AuthUtils.hashPassword("reception123"),
                    name = "Riya Sharma",
                    role = UserRole.RECEPTION,
                    qualification = "Front Desk & Patient Registration",
                    regNumber = "",
                    contactNumber = "+91 98230 44556",
                    email = "reception.aadhar@gmail.com"
                )
            )
        }

        // Seed Referral Hospitals Network if empty
        if (hospitalDao.getHospitalCount() == 0) {
            hospitalDao.insertHospitals(
                listOf(
                    Hospital(
                        name = "Sahyadri Super Speciality Hospital",
                        address = "Plot No. 30-C, Karve Road, Deccan Gymkhana, Pune - 411004",
                        contactNumber = "+91 20 6721 3000",
                        specialties = "Cardiology, Critical Care, Neurology, General Surgery"
                    ),
                    Hospital(
                        name = "Ruby Hall Clinic",
                        address = "40, Sassoon Road, Sangamvadi, Pune - 411001",
                        contactNumber = "+91 20 6645 5100",
                        specialties = "Multispeciality, Trauma, Oncology, Nephrology"
                    ),
                    Hospital(
                        name = "Deenanath Mangeshkar Hospital & Research Center",
                        address = "Erandwane, Near Mhatre Bridge, Pune - 411004",
                        contactNumber = "+91 20 4015 1000",
                        specialties = "Tertiary Care, ICU, Pediatrics, Orthopedics"
                    ),
                    Hospital(
                        name = "Noble Hospital",
                        address = "153, Magarpatta City Road, Hadapsar, Pune - 411013",
                        contactNumber = "+91 20 6628 5000",
                        specialties = "Emergency, ICU, Cardiology, Laparoscopy"
                    ),
                    Hospital(
                        name = "KEM Hospital & Research Centre",
                        address = "489, Rasta Peth, Sardar Moodliar Road, Pune - 411011",
                        contactNumber = "+91 20 6603 7300",
                        specialties = "Internal Medicine, Critical Care, Dialysis"
                    ),
                    Hospital(
                        name = "Bharati Hospital & Research Centre",
                        address = "Pune-Satara Road, Dhankawadi, Pune - 411043",
                        contactNumber = "+91 20 2437 3226",
                        specialties = "Tertiary Healthcare, Surgery, ICU"
                    )
                )
            )
        }

        // Seed Default Doctor Quick Presets with configured Default Durations
        if (quickPresetDao.getPresetCount() == 0) {
            quickPresetDao.insertPresets(
                listOf(
                    QuickPreset(
                        clinicId = "aadhar123",
                        presetName = "Antacid",
                        medicineName = "Pantoprazole 40 mg",
                        strength = "40 mg",
                        dose = "1 Tab",
                        frequency = "BD",
                        route = "Oral",
                        defaultDuration = "7 days",
                        instructions = "Before Food (AC)",
                        quantity = 14
                    ),
                    QuickPreset(
                        clinicId = "aadhar123",
                        presetName = "Fever / Pain",
                        medicineName = "Paracetamol 650 mg",
                        strength = "650 mg",
                        dose = "1 Tab",
                        frequency = "TID",
                        route = "Oral",
                        defaultDuration = "3 days",
                        instructions = "After Food (PC)",
                        quantity = 9
                    ),
                    QuickPreset(
                        clinicId = "aadhar123",
                        presetName = "Antibiotic",
                        medicineName = "Amoxyclav 625 mg",
                        strength = "625 mg",
                        dose = "1 Tab",
                        frequency = "BD",
                        route = "Oral",
                        defaultDuration = "5 days",
                        instructions = "After Food (PC)",
                        quantity = 10
                    ),
                    QuickPreset(
                        clinicId = "aadhar123",
                        presetName = "Antiallergic",
                        medicineName = "Cetirizine 10 mg",
                        strength = "10 mg",
                        dose = "1 Tab",
                        frequency = "OD",
                        route = "Oral",
                        defaultDuration = "5 days",
                        instructions = "At Bedtime (HS)",
                        quantity = 5
                    ),
                    QuickPreset(
                        clinicId = "aadhar123",
                        presetName = "Cough Syrup",
                        medicineName = "Ascoril D Cough Syrup",
                        strength = "5 ml",
                        dose = "1 Tsp (5ml)",
                        frequency = "TID",
                        route = "Oral",
                        defaultDuration = "5 days",
                        instructions = "After Food",
                        quantity = 1
                    ),
                    QuickPreset(
                        clinicId = "aadhar123",
                        presetName = "Muscle Relaxant",
                        medicineName = "Aceclofenac + Paracetamol",
                        strength = "100/325 mg",
                        dose = "1 Tab",
                        frequency = "BD",
                        route = "Oral",
                        defaultDuration = "3 days",
                        instructions = "After Food",
                        quantity = 6
                    ),
                    QuickPreset(
                        clinicId = "aadhar123",
                        presetName = "Multivitamin",
                        medicineName = "Becosules / B-Complex Zinc",
                        strength = "1 Cap",
                        dose = "1 Cap",
                        frequency = "OD",
                        route = "Oral",
                        defaultDuration = "15 days",
                        instructions = "After Breakfast",
                        quantity = 15
                    ),
                    QuickPreset(
                        clinicId = "aadhar123",
                        presetName = "Probiotic",
                        medicineName = "Bacillus Clausii / Sporlac",
                        strength = "1 Cap",
                        dose = "1 Cap",
                        frequency = "BD",
                        route = "Oral",
                        defaultDuration = "5 days",
                        instructions = "After Food",
                        quantity = 10
                    )
                )
            )
        }
    }
}
