// OWNER: A — TODO
package com.example.mos.screens.profile

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
import androidx.compose.material.icons.automirrored.rounded.Logout
import androidx.compose.material.icons.rounded.DeleteForever
import androidx.compose.material.icons.rounded.Email
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.Phone
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.mos.data.User
import com.example.mos.ui.DangerButton
import com.example.mos.ui.ErrorText
import com.example.mos.ui.SectionCard
import com.example.mos.ui.SectionHeader
import com.example.mos.ui.icon
import com.example.mos.ui.label
import com.example.mos.ui.theme.PrimaryDark

@Composable
fun ProfileScreen(
    viewModel: ProfileViewModel,
    onEditProfile: () -> Unit,
    onSignOut: () -> Unit,
    onAccountDeleted: () -> Unit
) {
    val state by viewModel.uiState.collectAsState()
    var showDeleteDialog by remember { mutableStateOf(false) }

    // Runs each time Profile opens, so changes made on Edit profile show up.
    LaunchedEffect(Unit) { viewModel.load() }

    val user = state.user
    if (user == null) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            if (state.loading) CircularProgressIndicator() else ErrorText(state.error)
        }
        return
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .verticalScroll(rememberScrollState())
    ) {
        ProfileHeader(user, onEditProfile, onSignOut)

        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            SectionHeader("Details")
            SectionCard {
                InfoRow(Icons.Rounded.Phone, "Phone", user.phone.ifBlank { "Not added" })
                InfoRow(Icons.Rounded.Email, "Email", user.email)
            }

            SectionHeader("Transport preferences")
            SectionCard {
                InfoRow(user.defaultMode.icon(), "Default mode", user.defaultMode.label())
            }

            SectionHeader("Account")
            SectionCard {
                if (state.usesPassword) {
                    ActionRow(Icons.Rounded.Lock, "Change password", onClick = viewModel::sendPasswordReset)
                }
                ActionRow(
                    Icons.Rounded.DeleteForever,
                    "Delete account",
                    color = MaterialTheme.colorScheme.error,
                    onClick = { showDeleteDialog = true }
                )
            }

            ErrorText(state.error)

            Spacer(Modifier.height(8.dp))
            DangerButton("Log out", onClick = onSignOut)
        }
    }

    if (state.resetEmailSent) {
        AlertDialog(
            onDismissRequest = viewModel::dismissResetDialog,
            title = { Text("Check your email") },
            text = { Text("We've sent a link to reset your password to ${user.email}.") },
            confirmButton = {
                TextButton(onClick = viewModel::dismissResetDialog) { Text("OK") }
            }
        )
    }

    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text("Delete your account?") },
            text = { Text("Your profile and login will be removed. This can't be undone.") },
            confirmButton = {
                TextButton(onClick = {
                    showDeleteDialog = false
                    viewModel.deleteAccount(onAccountDeleted)
                }) {
                    Text("Delete", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = false }) { Text("Cancel") }
            }
        )
    }
}

// Green gradient banner with the initials avatar, name, email, "Edit profile" and a logout icon.
@Composable
private fun ProfileHeader(user: User, onEditProfile: () -> Unit, onSignOut: () -> Unit) {
    val onPrimary = MaterialTheme.colorScheme.onPrimary

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(Brush.verticalGradient(listOf(MaterialTheme.colorScheme.primary, PrimaryDark)))
    ) {
        IconButton(onClick = onSignOut, modifier = Modifier.align(Alignment.TopEnd).padding(8.dp)) {
            Icon(Icons.AutoMirrored.Rounded.Logout, contentDescription = "Log out", tint = onPrimary)
        }
        ProfileHeaderContent(user, onEditProfile)
    }
}

@Composable
private fun ProfileHeaderContent(user: User, onEditProfile: () -> Unit) {
    val onPrimary = MaterialTheme.colorScheme.onPrimary

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 32.dp, horizontal = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .size(88.dp)
                .background(MaterialTheme.colorScheme.surface, CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Text(
                initials(user.name),
                style = MaterialTheme.typography.headlineSmall,
                color = MaterialTheme.colorScheme.primary
            )
        }
        Spacer(Modifier.height(12.dp))
        Text(user.name.ifBlank { "MOS user" }, style = MaterialTheme.typography.headlineSmall, color = onPrimary)
        Text(user.email, style = MaterialTheme.typography.bodyMedium, color = onPrimary.copy(alpha = 0.8f))
        Spacer(Modifier.height(16.dp))
        OutlinedButton(
            onClick = onEditProfile,
            shape = CircleShape,
            border = BorderStroke(1.dp, onPrimary)
        ) {
            Text("Edit profile", color = onPrimary)
        }
    }
}

// Icon tile + small label + value, e.g. Phone / +254712345678.
@Composable
private fun InfoRow(icon: ImageVector, label: String, value: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        IconTile(icon, MaterialTheme.colorScheme.primary)
        Spacer(Modifier.width(12.dp))
        Column {
            Text(label, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(value, style = MaterialTheme.typography.bodyMedium)
        }
    }
}

// Tappable row with a chevron, e.g. "Change password".
@Composable
private fun ActionRow(
    icon: ImageVector,
    title: String,
    color: Color = MaterialTheme.colorScheme.onSurface,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(48.dp)
            .clickable(onClick = onClick),
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconTile(icon, color)
        Spacer(Modifier.width(12.dp))
        Text(title, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium, color = color)
        Spacer(Modifier.weight(1f))
        Icon(
            Icons.AutoMirrored.Rounded.KeyboardArrowRight,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun IconTile(icon: ImageVector, tint: Color) {
    Box(
        modifier = Modifier
            .size(40.dp)
            .background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(12.dp)),
        contentAlignment = Alignment.Center
    ) {
        Icon(icon, contentDescription = null, tint = tint, modifier = Modifier.size(22.dp))
    }
}

// "Jane Wanjiku" -> "JW"
private fun initials(name: String): String =
    name.trim().split(" ").filter { it.isNotBlank() }.take(2)
        .joinToString("") { it.first().uppercase() }
        .ifBlank { "?" }
