// OWNER: A — TODO
package com.example.mos.screens.auth

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier

@Composable
fun LoginScreen(onLogin: () -> Unit, onGoToSignup: () -> Unit) {
    // TODO: real login form, wired to AuthViewModel
    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text("Login")
        Button(onClick = onLogin) { Text("Log in") }
        TextButton(onClick = onGoToSignup) { Text("Go to Signup") }
    }
}

@Composable
fun SignupScreen(onSignup: () -> Unit, onGoToLogin: () -> Unit) {
    // TODO: real signup form, wired to AuthViewModel
    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text("Signup")
        Button(onClick = onSignup) { Text("Sign up") }
        TextButton(onClick = onGoToLogin) { Text("Go to Login") }
    }
}
