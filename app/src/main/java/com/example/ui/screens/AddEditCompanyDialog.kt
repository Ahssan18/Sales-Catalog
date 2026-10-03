package com.example.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Business
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.example.data.model.Company
import com.example.ui.components.ImagePickerBottomSheet
import com.example.ui.components.SnackAsyncImage

@Composable
fun AddEditCompanyDialog(
    companyToEdit: Company? = null,
    onDismiss: () -> Unit,
    onCheckDuplicate: suspend (name: String, excludeId: Long) -> Boolean,
    onSave: (name: String, logoPath: String?) -> Unit
) {
    var name by remember { mutableStateOf(companyToEdit?.name ?: "") }
    var logoPath by remember { mutableStateOf<String?>(companyToEdit?.logoPath) }

    var errorMessage by remember { mutableStateOf<String?>(null) }
    var showImagePicker by remember { mutableStateOf(false) }
    var showDiscardPrompt by remember { mutableStateOf(false) }

    val initialName = companyToEdit?.name ?: ""
    val initialLogo = companyToEdit?.logoPath
    val isDirty = name.trim() != initialName.trim() || logoPath != initialLogo

    AlertDialog(
        onDismissRequest = {
            if (isDirty) {
                showDiscardPrompt = true
            } else {
                onDismiss()
            }
        },
        title = {
            Text(
                text = if (companyToEdit == null) "Add Snack Company" else "Edit Company",
                style = MaterialTheme.typography.headlineSmall
            )
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Logo preview & change action
                Box(
                    modifier = Modifier
                        .size(110.dp)
                        .clip(RoundedCornerShape(18.dp))
                        .clickable { showImagePicker = true }
                        .testTag("company_logo_trigger"),
                    contentAlignment = Alignment.Center
                ) {
                    SnackAsyncImage(
                        imagePath = logoPath,
                        contentDescription = "Company logo",
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(18.dp),
                        fallbackIcon = {
                            Icon(
                                imageVector = Icons.Default.Business,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(48.dp)
                            )
                        }
                    )
                    Surface(
                        modifier = Modifier
                            .align(Alignment.BottomEnd)
                            .padding(4.dp)
                            .size(30.dp),
                        shape = CircleShape,
                        color = MaterialTheme.colorScheme.primary,
                        contentColor = MaterialTheme.colorScheme.onPrimary
                    ) {
                        Icon(
                            imageVector = Icons.Default.CameraAlt,
                            contentDescription = "Change photo",
                            modifier = Modifier.padding(6.dp)
                        )
                    }
                }

                TextButton(
                    onClick = { showImagePicker = true },
                    modifier = Modifier.padding(top = 4.dp)
                ) {
                    Text(if (logoPath.isNullOrBlank()) "Add Company Logo" else "Change Logo")
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Company Name input
                OutlinedTextField(
                    value = name,
                    onValueChange = {
                        name = it
                        if (errorMessage != null) errorMessage = null
                    },
                    label = { Text("Company Name *") },
                    placeholder = { Text("e.g. Lay's, Oreo, Pringles") },
                    leadingIcon = { Icon(Icons.Default.Business, contentDescription = null) },
                    isError = errorMessage != null,
                    supportingText = {
                        errorMessage?.let {
                            Text(it, color = MaterialTheme.colorScheme.error)
                        }
                    },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("company_name_input")
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val trimmed = name.trim()
                    if (trimmed.isBlank()) {
                        errorMessage = "Company name cannot be blank"
                        return@Button
                    }
                    onSave(trimmed, logoPath?.takeIf { it.isNotBlank() })
                },
                modifier = Modifier.testTag("save_company_button")
            ) {
                Text(if (companyToEdit == null) "Create Company" else "Save Changes")
            }
        },
        dismissButton = {
            TextButton(
                onClick = {
                    if (isDirty) {
                        showDiscardPrompt = true
                    } else {
                        onDismiss()
                    }
                },
                modifier = Modifier.testTag("cancel_company_button")
            ) {
                Text("Cancel")
            }
        },
        modifier = Modifier.testTag("add_edit_company_dialog")
    )

    if (showImagePicker) {
        ImagePickerBottomSheet(
            onDismissRequest = { showImagePicker = false },
            onImageSelected = { relativeFilename ->
                logoPath = relativeFilename
                showImagePicker = false
            },
            onImageRemoved = {
                logoPath = null
            },
            hasExistingImage = !logoPath.isNullOrBlank()
        )
    }

    if (showDiscardPrompt) {
        AlertDialog(
            onDismissRequest = { showDiscardPrompt = false },
            title = { Text("Discard Unsaved Changes?") },
            text = { Text("You have entered new details for this company. Are you sure you want to discard them?") },
            confirmButton = {
                TextButton(
                    onClick = {
                        showDiscardPrompt = false
                        onDismiss()
                    },
                    modifier = Modifier.testTag("confirm_discard_company")
                ) {
                    Text("Discard", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { showDiscardPrompt = false },
                    modifier = Modifier.testTag("keep_editing_company")
                ) {
                    Text("Keep Editing")
                }
            }
        )
    }
}
