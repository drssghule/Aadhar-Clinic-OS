package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import com.example.aadharclinic.ui.screens.LoginScreen
import com.example.aadharclinic.ui.screens.MainAppScaffold
import com.example.ui.theme.AadharClinicTheme
import com.example.aadharclinic.ui.viewmodel.ClinicViewModel

class MainActivity : ComponentActivity() {
    private val viewModel: ClinicViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            AadharClinicTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    val isAuthenticated by viewModel.isAuthenticated.collectAsState()
                    val profile by viewModel.clinicProfile.collectAsState()
                    val users by viewModel.allUsers.collectAsState()
                    val loginError by viewModel.loginError.collectAsState()
                    val isOnline by viewModel.isOnline.collectAsState()

                    if (!isAuthenticated) {
                        LoginScreen(
                            profile = profile,
                            users = users,
                            isOnline = isOnline,
                            errorMessage = loginError,
                            onClearError = { viewModel.clearLoginError() },
                            onLoginHospitalClick = { hospitalId, role, password ->
                                viewModel.loginHospital(hospitalId, role, password)
                            },
                            onCreateHospitalAccount = { newProf, adminPass ->
                                viewModel.createHospitalAccount(newProf, adminPass)
                            }
                        )
                    } else {
                        MainAppScaffold(viewModel = viewModel)
                    }
                }
            }
        }
    }
}
