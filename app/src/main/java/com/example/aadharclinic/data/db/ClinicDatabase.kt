package com.example.aadharclinic.data.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.example.aadharclinic.data.dao.*
import com.example.aadharclinic.data.model.*

@Database(
    entities = [
        Patient::class,
        Consultation::class,
        PrescriptionItem::class,
        InventoryItem::class,
        StockTransaction::class,
        IpdAdmission::class,
        IpdDailyNote::class,
        IpdMedicineAdministered::class,
        BillInvoice::class,
        FollowUp::class,
        ClinicProfile::class,
        Expense::class,
        ClinicService::class,
        ClinicUser::class,
        Hospital::class,
        ClinicalDocument::class,
        QuickPreset::class
    ],
    version = 9,
    exportSchema = false
)
@TypeConverters(Converters::class)
abstract class ClinicDatabase : RoomDatabase() {
    abstract fun patientDao(): PatientDao
    abstract fun consultationDao(): ConsultationDao
    abstract fun inventoryDao(): InventoryDao
    abstract fun ipdDao(): IpdDao
    abstract fun billingDao(): BillingDao
    abstract fun followUpDao(): FollowUpDao
    abstract fun clinicProfileDao(): ClinicProfileDao
    abstract fun expenseDao(): ExpenseDao
    abstract fun clinicServiceDao(): ClinicServiceDao
    abstract fun clinicUserDao(): ClinicUserDao
    abstract fun hospitalDao(): HospitalDao
    abstract fun clinicalDocumentDao(): ClinicalDocumentDao
    abstract fun quickPresetDao(): QuickPresetDao

    companion object {
        @Volatile
        private var INSTANCE: ClinicDatabase? = null

        fun getDatabase(context: Context): ClinicDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    ClinicDatabase::class.java,
                    "aadhar_clinic_database.db"
                ).fallbackToDestructiveMigration().build()
                INSTANCE = instance
                instance
            }
        }
    }
}
