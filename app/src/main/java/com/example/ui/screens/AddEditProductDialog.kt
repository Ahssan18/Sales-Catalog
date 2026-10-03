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
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Fastfood
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
import com.example.data.model.Product
import com.example.ui.components.ImagePickerBottomSheet
import com.example.ui.components.SnackAsyncImage

@Composable
fun AddEditProductDialog(
    companyId: Long,
    companyName: String,
    productToEdit: Product? = null,
    onDismiss: () -> Unit,
    onSave: (name: String, imagePath: String?) -> Unit
) {
    var name by remember { mutableStateOf(productToEdit?.name ?: "") }
    var imagePath by remember { mutableStateOf<String?>(productToEdit?.imagePath) }

    var errorMessage by remember { mutableStateOf<String?>(null) }
    var showImagePicker by remember { mutableStateOf(false) }
    var showDiscardPrompt by remember { mutableStateOf(false) }

    val initialName = productToEdit?.name ?: ""
    val initialImage = productToEdit?.imagePath
    val isDirty = name.trim() != initialName.trim() || imagePath != initialImage

    AlertDialog(
        onDismissRequest = {
            if (isDirty) {
                showDiscardPrompt = true
            } else {
                onDismiss()
            }
        },
        title = {
            Column {
                Text(
                    text = if (productToEdit == null) "Add Snack Product" else "Edit Product",
                    style = MaterialTheme.typography.headlineSmall
                )
                Text(
                    text = "For $companyName",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.primary
                )
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Product Photo preview & change action
                Box(
                    modifier = Modifier
                        .size(120.dp)
                        .clip(RoundedCornerShape(18.dp))
                        .clickable { showImagePicker = true }
                        .testTag("product_photo_trigger"),
                    contentAlignment = Alignment.Center
                ) {
                    SnackAsyncImage(
                        imagePath = imagePath,
                        contentDescription = "Product photo",
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(18.dp),
                        fallbackIcon = {
                            Icon(
                                imageVector = Icons.Default.Fastfood,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(52.dp)
                            )
                        }
                    )
                    Surface(
                        modifier = Modifier
                            .align(Alignment.BottomEnd)
                            .padding(4.dp)
                            .size(32.dp),
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
                    Text(if (imagePath.isNullOrBlank()) "Add Product Photo" else "Change Photo")
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Product Name input
                OutlinedTextField(
                    value = name,
                    onValueChange = {
                        name = it
                        if (errorMessage != null) errorMessage = null
                    },
                    label = { Text("Product Name *") },
                    placeholder = { Text("e.g. Lay's Classic Salted, Oreo Vanilla") },
                    leadingIcon = { Icon(Icons.Default.Fastfood, contentDescription = null) },
                    isError = errorMessage != null,
                    supportingText = {
                        errorMessage?.let {
                            Text(it, color = MaterialTheme.colorScheme.error)
                        }
                    },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("product_name_input")
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val trimmed = name.trim()
                    if (trimmed.isBlank()) {
                        errorMessage = "Product name cannot be blank"
                        return@Button
                    }
                    onSave(trimmed, imagePath?.takeIf { it.isNotBlank() })
                },
                modifier = Modifier.testTag("save_product_button")
            ) {
                Text(if (productToEdit == null) "Add Product" else "Save Changes")
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
                modifier = Modifier.testTag("cancel_product_button")
            ) {
                Text("Cancel")
            }
        },
        modifier = Modifier.testTag("add_edit_product_dialog")
    )

    if (showImagePicker) {
        ImagePickerBottomSheet(
            onDismissRequest = { showImagePicker = false },
            onImageSelected = { relativeFilename ->
                imagePath = relativeFilename
                showImagePicker = false
            },
            onImageRemoved = {
                imagePath = null
            },
            hasExistingImage = !imagePath.isNullOrBlank()
        )
    }

    if (showDiscardPrompt) {
        AlertDialog(
            onDismissRequest = { showDiscardPrompt = false },
            title = { Text("Discard Unsaved Changes?") },
            text = { Text("You have entered new details for this product. Are you sure you want to discard them?") },
            confirmButton = {
                TextButton(
                    onClick = {
                        showDiscardPrompt = false
                        onDismiss()
                    },
                    modifier = Modifier.testTag("confirm_discard_product")
                ) {
                    Text("Discard", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { showDiscardPrompt = false },
                    modifier = Modifier.testTag("keep_editing_product")
                ) {
                    Text("Keep Editing")
                }
            }
        )
    }
}
