package com.example.ui.screens

import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.FileDownload
import androidx.compose.material.icons.filled.FileUpload
import androidx.compose.material.icons.filled.FormatSize
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.RestartAlt
import androidx.compose.material.icons.filled.SpaceBar
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.data.AppDatabase
import com.example.data.BackupRestoreManager
import com.example.data.model.DisplaySettings
import com.example.data.model.ImageScale
import com.example.data.model.RowSpacing
import com.example.data.model.TextScale
import com.example.data.model.ThemeMode
import com.example.ui.components.DeleteConfirmationDialog
import com.example.ui.components.SnackAsyncImage
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    settings: DisplaySettings,
    onSettingsChanged: (DisplaySettings) -> Unit,
    onResetDefaults: () -> Unit,
    onResetToSampleData: () -> Unit,
    onClearAllData: () -> Unit,
    onNavigateBack: () -> Unit
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    var showRestoreWarning by remember { mutableStateOf<Uri?>(null) }
    var showResetSamplePrompt by remember { mutableStateOf(false) }
    var showClearPrompt by remember { mutableStateOf(false) }
    var isProcessingBackup by remember { mutableStateOf(false) }

    // Export Document Launcher
    val exportLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/zip")
    ) { uri: Uri? ->
        if (uri != null) {
            isProcessingBackup = true
            coroutineScope.launch {
                val db = AppDatabase.getDatabase(context)
                val result = BackupRestoreManager.exportCatalog(
                    context = context,
                    destinationUri = uri,
                    companyDao = db.companyDao(),
                    productDao = db.productDao()
                )
                isProcessingBackup = false
                result.onSuccess { count ->
                    Toast.makeText(context, "Catalog exported successfully ($count records)", Toast.LENGTH_LONG).show()
                }.onFailure { err ->
                    Toast.makeText(context, "Export failed: ${err.message}", Toast.LENGTH_LONG).show()
                }
            }
        }
    }

    // Import Document Launcher
    val importLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        if (uri != null) {
            // Prompt explicit warning before replacing catalog
            showRestoreWarning = uri
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Display & Backup Settings") },
                navigationIcon = {
                    IconButton(
                        onClick = onNavigateBack,
                        modifier = Modifier.testTag("settings_back_button")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back"
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                    titleContentColor = MaterialTheme.colorScheme.onSurface
                )
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            // Live Preview Card
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.35f)
                ),
                shape = RoundedCornerShape(20.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "Live Display Preview",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.height(10.dp))

                    // Simulated Company Row
                    Card(
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(settings.rowSpacing.paddingDp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            SnackAsyncImage(
                                imagePath = "res:sample_chips",
                                contentDescription = null,
                                modifier = Modifier.size(settings.imageScale.companyLogoSize),
                                shape = RoundedCornerShape(12.dp)
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text(
                                    text = "Preview Snack Brand",
                                    fontSize = settings.textScale.titleSize,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = "4 products in catalog",
                                    fontSize = settings.textScale.subtitleSize,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    // Simulated Product Row
                    Card(
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(settings.rowSpacing.paddingDp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            SnackAsyncImage(
                                imagePath = "res:sample_cookies",
                                contentDescription = null,
                                modifier = Modifier.size(settings.imageScale.productImageSize),
                                shape = RoundedCornerShape(12.dp)
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Text(
                                text = "Preview Snack Product",
                                fontSize = settings.textScale.titleSize,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                }
            }

            // Text Size Controls
            SettingSection(
                title = "Text Size",
                subtitle = "Scales company and product names across screens",
                icon = Icons.Default.FormatSize
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    TextScale.values().forEach { scale ->
                        val isSelected = settings.textScale == scale
                        SettingPill(
                            label = scale.label,
                            isSelected = isSelected,
                            onClick = { onSettingsChanged(settings.copy(textScale = scale)) },
                            modifier = Modifier.weight(1f),
                            testTag = "text_scale_${scale.name}"
                        )
                    }
                }
            }

            // Image Size Controls
            SettingSection(
                title = "Image & Logo Size",
                subtitle = "Adjusts dimensions of company logos and snack photos",
                icon = Icons.Default.Image
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    ImageScale.values().forEach { scale ->
                        val isSelected = settings.imageScale == scale
                        SettingPill(
                            label = scale.label,
                            isSelected = isSelected,
                            onClick = { onSettingsChanged(settings.copy(imageScale = scale)) },
                            modifier = Modifier.weight(1f),
                            testTag = "image_scale_${scale.name}"
                        )
                    }
                }
            }

            // Row Spacing Controls
            SettingSection(
                title = "Row Spacing",
                subtitle = "Adjusts vertical padding between items in the list",
                icon = Icons.Default.SpaceBar
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    RowSpacing.values().forEach { spacing ->
                        val isSelected = settings.rowSpacing == spacing
                        SettingPill(
                            label = spacing.label,
                            isSelected = isSelected,
                            onClick = { onSettingsChanged(settings.copy(rowSpacing = spacing)) },
                            modifier = Modifier.weight(1f),
                            testTag = "row_spacing_${spacing.name}"
                        )
                    }
                }
            }

            // Theme Mode
            SettingSection(
                title = "Theme",
                subtitle = "Choose light, dark, or system default display",
                icon = Icons.Default.Palette
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    ThemeMode.values().forEach { mode ->
                        val isSelected = settings.themeMode == mode
                        SettingPill(
                            label = mode.label.substringBefore(" "),
                            isSelected = isSelected,
                            onClick = { onSettingsChanged(settings.copy(themeMode = mode)) },
                            modifier = Modifier.weight(1f),
                            testTag = "theme_mode_${mode.name}"
                        )
                    }
                }
            }

            // Reset Display Defaults
            OutlinedButton(
                onClick = onResetDefaults,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("reset_display_defaults")
            ) {
                Icon(Icons.Default.Tune, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Reset Display Defaults")
            }

            HorizontalDivider()

            // Backup & Restore Section
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
                ),
                shape = RoundedCornerShape(20.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(18.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Text(
                        text = "Backup & Recovery",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Export catalog data and photos into a single backup archive, or restore on a new device.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Button(
                            onClick = {
                                val timestamp = SimpleDateFormat("yyyyMMdd_HHmm", Locale.US).format(Date())
                                exportLauncher.launch("SalesBook_Backup_$timestamp.zip")
                            },
                            enabled = !isProcessingBackup,
                            modifier = Modifier
                                .weight(1f)
                                .testTag("export_backup_button")
                        ) {
                            Icon(Icons.Default.FileDownload, contentDescription = null)
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Export Backup")
                        }

                        OutlinedButton(
                            onClick = {
                                importLauncher.launch(arrayOf("application/zip", "application/octet-stream"))
                            },
                            enabled = !isProcessingBackup,
                            modifier = Modifier
                                .weight(1f)
                                .testTag("restore_backup_button")
                        ) {
                            Icon(Icons.Default.FileUpload, contentDescription = null)
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Restore Backup")
                        }
                    }

                    Spacer(modifier = Modifier.height(4.dp))

                    OutlinedButton(
                        onClick = { showResetSamplePrompt = true },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("restore_sample_catalog_button")
                    ) {
                        Icon(Icons.Default.RestartAlt, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Reload Default Snack Companies")
                    }

                    OutlinedButton(
                        onClick = { showClearPrompt = true },
                        colors = ButtonDefaults.outlinedButtonColors(
                            contentColor = MaterialTheme.colorScheme.error
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("clear_catalog_button")
                    ) {
                        Icon(Icons.Default.DeleteSweep, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Clear All Catalog Data")
                    }
                }
            }
        }
    }

    // Confirmation for restore (replaces existing catalog)
    if (showRestoreWarning != null) {
        DeleteConfirmationDialog(
            title = "Restore Backup?",
            message = "Restoring this archive will completely replace your current catalog and images with the backup data. This action cannot be reversed.",
            onConfirm = {
                val uri = showRestoreWarning!!
                showRestoreWarning = null
                isProcessingBackup = true
                coroutineScope.launch {
                    val db = AppDatabase.getDatabase(context)
                    val result = BackupRestoreManager.importCatalog(context, uri, db)
                    isProcessingBackup = false
                    result.onSuccess { msg ->
                        Toast.makeText(context, msg, Toast.LENGTH_LONG).show()
                        onNavigateBack()
                    }.onFailure { err ->
                        Toast.makeText(context, "Restore failed: ${err.message}", Toast.LENGTH_LONG).show()
                    }
                }
            },
            onDismiss = { showRestoreWarning = null }
        )
    }

    if (showResetSamplePrompt) {
        DeleteConfirmationDialog(
            title = "Reload Sample Catalog?",
            message = "This will restore default snack companies and sample items. Your current catalog will be reset.",
            onConfirm = {
                showResetSamplePrompt = false
                onResetToSampleData()
            },
            onDismiss = { showResetSamplePrompt = false }
        )
    }

    if (showClearPrompt) {
        DeleteConfirmationDialog(
            title = "Clear Entire Catalog?",
            message = "This will permanently remove all companies, products, and local images.",
            onConfirm = {
                showClearPrompt = false
                onClearAllData()
            },
            onDismiss = { showClearPrompt = false }
        )
    }
}

@Composable
private fun SettingSection(
    title: String,
    subtitle: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    content: @Composable () -> Unit
) {
    Card(
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)
        ),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(22.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Column {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = subtitle,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            Spacer(modifier = Modifier.height(12.dp))
            content()
        }
    }
}

@Composable
private fun SettingPill(
    label: String,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    testTag: String
) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(12.dp),
        color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surface,
        contentColor = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface,
        shadowElevation = if (isSelected) 2.dp else 0.dp,
        modifier = modifier.testTag(testTag)
    ) {
        Box(
            modifier = Modifier.padding(vertical = 10.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium,
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
            )
        }
    }
}
