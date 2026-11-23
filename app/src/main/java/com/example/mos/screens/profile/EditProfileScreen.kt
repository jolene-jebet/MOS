// OWNER: A — TODO
package com.example.mos.screens.profile

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Email
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material.icons.rounded.Phone
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import com.example.mos.data.TransportMode
import com.example.mos.ui.ErrorText
import com.example.mos.ui.PillTextField
import com.example.mos.ui.PrimaryButton
import com.example.mos.ui.SectionCard
import com.example.mos.ui.SectionHeader
import com.example.mos.ui.icon
import com.example.mos.ui.label

@Composable
fun EditProfileScreen(viewModel: EditProfileViewModel, onBack: () -> Unit, onSaved: () -> Unit) {
    val state by viewModel.uiState.collectAsState()

    LaunchedEffect(Unit) { viewModel.load() }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .verticalScroll(rememberScrollState())
            .imePadding()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Top bar
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onBack) {
                Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "Back")
            }
            Text("Edit profile", style = MaterialTheme.typography.titleLarge)
        }

        SectionHeader("Your details")
        SectionCard {
            PillTextField("Full name", state.name, viewModel::onNameChange, Icons.Rounded.Person)
            PillTextField(
                "Phone", state.phone, viewModel::onPhoneChange, Icons.Rounded.Phone, KeyboardType.Phone,
                prefix = "+254 "
            )
            PillTextField("Email (can't be changed)", state.email, {}, Icons.Rounded.Email, enabled = false)
        }

        SectionHeader("Default transport mode")
        ModePicker(selected = state.defaultMode, onSelect = viewModel::onModeChange)

        ErrorText(state.error)
        PrimaryButton("Save changes", loading = state.saving) { viewModel.save(onSaved) }
    }
}

// Walk / Boda Boda / Car picker. A green pill slides behind the selected option.
@Composable
private fun ModePicker(selected: TransportMode, onSelect: (TransportMode) -> Unit) {
    BoxWithConstraints(
        modifier = Modifier
            .fillMaxWidth()
            .height(48.dp)
            .background(MaterialTheme.colorScheme.surfaceVariant, CircleShape)
    ) {
        val slotWidth = maxWidth / profileModes.size
        val pillOffset by animateDpAsState(
            targetValue = slotWidth * profileModes.indexOf(selected),
            animationSpec = spring(dampingRatio = 0.8f, stiffness = Spring.StiffnessMediumLow),
            label = "pillOffset"
        )

        // The sliding green pill. The lambda offset moves it without re-laying out the screen.
        Box(
            modifier = Modifier
                .offset { IntOffset(pillOffset.roundToPx(), 0) }
                .width(slotWidth)
                .fillMaxHeight()
                .background(MaterialTheme.colorScheme.primary, CircleShape)
        )

        // The three options on top of the pill.
        Row(modifier = Modifier.fillMaxSize()) {
            profileModes.forEach { mode ->
                val isSelected = mode == selected
                val contentColor by animateColorAsState(
                    targetValue = if (isSelected) MaterialTheme.colorScheme.onPrimary
                    else MaterialTheme.colorScheme.onSurfaceVariant,
                    label = "modeColor"
                )

                Row(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .clip(CircleShape)
                        .selectable(
                            selected = isSelected,
                            onClick = { onSelect(mode) },
                            role = Role.RadioButton,
                            interactionSource = null,
                            indication = null
                        ),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(mode.icon(), contentDescription = null, tint = contentColor, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(6.dp))
                    Text(mode.label(), style = MaterialTheme.typography.labelMedium, color = contentColor)
                }
            }
        }
    }
}
