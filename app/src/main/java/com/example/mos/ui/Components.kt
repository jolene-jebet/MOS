// OWNER: A — TODO
package com.example.mos.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.DirectionsWalk
import androidx.compose.material.icons.rounded.DirectionsBus
import androidx.compose.material.icons.rounded.DirectionsCar
import androidx.compose.material.icons.rounded.TwoWheeler
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.mos.data.Risk
import com.example.mos.data.Route
import com.example.mos.data.TransportMode

@Composable
fun RiskChip(risk: Risk, modifier: Modifier = Modifier) {
    // TODO
}

@Composable
fun RiskBar(risk: Risk, modifier: Modifier = Modifier) {
    // TODO
}

@Composable
fun ModeChip(mode: TransportMode, modifier: Modifier = Modifier) {
    // TODO
}

@Composable
fun RouteCard(route: Route, modifier: Modifier = Modifier) {
    // TODO
}

// ---------- Transport modes ----------

fun TransportMode.label(): String = when (this) {
    TransportMode.WALK -> "Walk"
    TransportMode.BODA -> "Boda Boda"
    TransportMode.MATATU -> "Matatu"
    TransportMode.PRIVATE -> "Car"
}

fun TransportMode.icon(): ImageVector = when (this) {
    TransportMode.WALK -> Icons.AutoMirrored.Rounded.DirectionsWalk
    TransportMode.BODA -> Icons.Rounded.TwoWheeler
    TransportMode.MATATU -> Icons.Rounded.DirectionsBus
    TransportMode.PRIVATE -> Icons.Rounded.DirectionsCar
}

// ---------- Buttons ----------

// Full-width green pill. Shows a spinner instead of the text while loading.
@Composable
fun PrimaryButton(text: String, loading: Boolean = false, onClick: () -> Unit) {
    Button(
        onClick = onClick,
        enabled = !loading,
        modifier = Modifier.fillMaxWidth().height(52.dp),
        shape = CircleShape
    ) {
        if (loading) {
            CircularProgressIndicator(
                modifier = Modifier.size(22.dp),
                color = MaterialTheme.colorScheme.onPrimary,
                strokeWidth = 2.dp
            )
        } else {
            Text(text, style = MaterialTheme.typography.titleMedium)
        }
    }
}

// Outlined pill with green text, for the less important action.
@Composable
fun SecondaryButton(text: String, onClick: () -> Unit) {
    OutlinedButton(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth().height(52.dp),
        shape = CircleShape,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary)
    ) {
        Text(text, style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary)
    }
}

// Light red pill with red text, e.g. "Log out".
@Composable
fun DangerButton(text: String, onClick: () -> Unit) {
    Button(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth().height(52.dp),
        shape = CircleShape,
        colors = ButtonDefaults.buttonColors(
            containerColor = MaterialTheme.colorScheme.errorContainer,
            contentColor = MaterialTheme.colorScheme.error
        )
    ) {
        Text(text, style = MaterialTheme.typography.titleMedium)
    }
}

// ---------- Text fields ----------

// Pill text field with the label above it, as in DESIGN.md.
@Composable
fun PillTextField(
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    icon: ImageVector,
    keyboardType: KeyboardType = KeyboardType.Text,
    prefix: String? = null,
    enabled: Boolean = true,
    isPassword: Boolean = false,
    trailing: @Composable (() -> Unit)? = null
) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(label, style = MaterialTheme.typography.bodySmall)
        TextField(
            value = value,
            onValueChange = onValueChange,
            modifier = Modifier.fillMaxWidth(),
            enabled = enabled,
            singleLine = true,
            shape = CircleShape,
            leadingIcon = { Icon(icon, contentDescription = null) },
            trailingIcon = trailing,
            prefix = prefix?.let { { Text(it) } },
            visualTransformation = if (isPassword) PasswordVisualTransformation() else VisualTransformation.None,
            keyboardOptions = KeyboardOptions(keyboardType = keyboardType),
            colors = TextFieldDefaults.colors(
                focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                disabledContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                focusedIndicatorColor = MaterialTheme.colorScheme.surfaceVariant,
                unfocusedIndicatorColor = MaterialTheme.colorScheme.surfaceVariant,
                disabledIndicatorColor = MaterialTheme.colorScheme.surfaceVariant
            )
        )
    }
}

// ---------- Layout ----------

// White card with a thin border and 16dp corners.
@Composable
fun SectionCard(content: @Composable () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = CardDefaults.outlinedCardBorder()
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            content()
        }
    }
}

// Small grey uppercase heading above a section, e.g. "ACCOUNT".
@Composable
fun SectionHeader(text: String) {
    Text(
        text.uppercase(),
        style = MaterialTheme.typography.labelSmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.fillMaxWidth()
    )
}

// Red message under a form. Shows nothing when there is no error.
@Composable
fun ErrorText(error: String?) {
    if (error != null) {
        Text(
            error,
            color = MaterialTheme.colorScheme.error,
            style = MaterialTheme.typography.bodyMedium,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth()
        )
    }
}
