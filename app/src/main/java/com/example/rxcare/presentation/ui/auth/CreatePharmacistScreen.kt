package com.example.rxcare.presentation.ui.auth

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import org.koin.androidx.compose.koinViewModel
import com.example.rxcare.presentation.viewmodel.AuthViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreatePharmacistScreen(
    onNavigateBack: () -> Unit,
    onNavigateToHome: () -> Unit,
    authViewModel: AuthViewModel = koinViewModel()
) {
    var name by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    
    val uiState by authViewModel.uiState.collectAsState()
    val createPharmacistSuccess by remember { derivedStateOf { uiState.createPharmacistSuccess } }
    
    // Name validation
    val nameError = when {
        name.isBlank() -> null
        name.length < 2 -> "Name must be at least 2 characters"
        uiState.error?.contains("name", ignoreCase = true) == true -> uiState.error
        else -> null
    }
    
    // Email validation
    val emailError = when {
        email.isBlank() -> null
        !email.contains("@") -> "Invalid email format"
        uiState.error?.contains("email", ignoreCase = true) == true -> uiState.error
        else -> null
    }
    
    // Password validation
    val passwordError = when {
        password.isBlank() -> null
        password.length < 6 -> "Password must be at least 6 characters"
        uiState.error?.contains("password", ignoreCase = true) == true -> uiState.error
        else -> null
    }
    
    // Navigate to home when pharmacist creation is successful
    LaunchedEffect(createPharmacistSuccess) {
        if (createPharmacistSuccess) {
            onNavigateToHome()
            authViewModel.clearSuccessStates()
        }
    }
    
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = Icons.Default.Person,
            contentDescription = null,
            modifier = Modifier.size(80.dp),
            tint = MaterialTheme.colorScheme.primary
        )
        
        Spacer(modifier = Modifier.height(16.dp))
        
        Text(
            text = "Create Pharmacist Account",
            style = MaterialTheme.typography.headlineMedium
        )
        
        Spacer(modifier = Modifier.height(8.dp))
        
        Text(
            text = "Register a new pharmacist",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        
        Spacer(modifier = Modifier.height(32.dp))
        
        OutlinedTextField(
            value = name,
            onValueChange = { name = it },
            label = { Text("Full Name") },
            leadingIcon = { Icon(Icons.Default.Person, contentDescription = null) },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            isError = nameError != null,
            supportingText = nameError?.let { { Text(it, color = MaterialTheme.colorScheme.error) } }
        )
        
        Spacer(modifier = Modifier.height(16.dp))
        
        OutlinedTextField(
            value = email,
            onValueChange = { email = it },
            label = { Text("Email") },
            leadingIcon = { Icon(Icons.Default.Email, contentDescription = null) },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            isError = emailError != null,
            supportingText = emailError?.let { { Text(it, color = MaterialTheme.colorScheme.error) } }
        )
        
        Spacer(modifier = Modifier.height(16.dp))
        
        OutlinedTextField(
            value = password,
            onValueChange = { password = it },
            label = { Text("Password") },
            leadingIcon = { Icon(Icons.Default.Lock, contentDescription = null) },
            visualTransformation = PasswordVisualTransformation(),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            isError = passwordError != null,
            supportingText = passwordError?.let { { Text(it, color = MaterialTheme.colorScheme.error) } }
        )
        
        Spacer(modifier = Modifier.height(24.dp))
        
        Button(
            onClick = {
                authViewModel.createPharmacist(name, email, password)
            },
            enabled = !uiState.isLoading && name.isNotBlank() && email.isNotBlank() && password.isNotBlank() && nameError == null && emailError == null && passwordError == null,
            modifier = Modifier.fillMaxWidth()
        ) {
            if (uiState.isLoading) {
                CircularProgressIndicator(
                    modifier = Modifier.size(20.dp),
                    color = MaterialTheme.colorScheme.onPrimary
                )
            } else {
                Text("Create Pharmacist")
            }
        }
        
        Spacer(modifier = Modifier.height(16.dp))
        
        TextButton(
            onClick = onNavigateBack
        ) {
            Text("Cancel")
        }
    }
}
