package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.ui.SnackViewModel
import com.example.ui.screens.AddEditCompanyDialog
import com.example.ui.screens.AddEditProductDialog
import com.example.ui.screens.CompanyProductsScreen
import com.example.ui.screens.DashboardScreen
import com.example.ui.screens.SettingsScreen
import com.example.ui.theme.MyApplicationTheme

class MainActivity : ComponentActivity() {

    private val snackViewModel: SnackViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val displaySettings by snackViewModel.displaySettings.collectAsStateWithLifecycle()

            MyApplicationTheme(themeMode = displaySettings.themeMode) {
                Surface(modifier = Modifier.fillMaxSize()) {
                    SalesBookApp(viewModel = snackViewModel)
                }
            }
        }
    }
}

@Composable
fun SalesBookApp(viewModel: SnackViewModel) {
    val companies by viewModel.companies.collectAsStateWithLifecycle()
    val productCounts by viewModel.productCounts.collectAsStateWithLifecycle()
    val selectedCompany by viewModel.selectedCompany.collectAsStateWithLifecycle()
    val products by viewModel.currentProducts.collectAsStateWithLifecycle()
    val companySearchQuery by viewModel.companySearchQuery.collectAsStateWithLifecycle()
    val productSearchQuery by viewModel.productSearchQuery.collectAsStateWithLifecycle()
    val companySortOrder by viewModel.companySortOrder.collectAsStateWithLifecycle()
    val displaySettings by viewModel.displaySettings.collectAsStateWithLifecycle()

    val isSettingsOpen by viewModel.isSettingsOpen.collectAsStateWithLifecycle()
    val companyDialogState by viewModel.companyDialogState.collectAsStateWithLifecycle()
    val productDialogState by viewModel.productDialogState.collectAsStateWithLifecycle()

    val currentCompany = selectedCompany

    // Back handling
    BackHandler(enabled = isSettingsOpen || currentCompany != null) {
        if (isSettingsOpen) {
            viewModel.isSettingsOpen.value = false
        } else if (currentCompany != null) {
            viewModel.selectCompany(null)
        }
    }

    when {
        isSettingsOpen -> {
            SettingsScreen(
                settings = displaySettings,
                onSettingsChanged = { viewModel.updateDisplaySettings(it) },
                onResetDefaults = { viewModel.resetDisplayDefaults() },
                onResetToSampleData = { viewModel.resetToSampleData() },
                onClearAllData = { viewModel.clearAllData() },
                onNavigateBack = { viewModel.isSettingsOpen.value = false }
            )
        }
        currentCompany != null -> {
            CompanyProductsScreen(
                company = currentCompany,
                products = products,
                searchQuery = productSearchQuery,
                onSearchQueryChange = { viewModel.productSearchQuery.value = it },
                displaySettings = displaySettings,
                onNavigateBack = { viewModel.selectCompany(null) },
                onAddProductClick = { viewModel.openAddProductDialog(currentCompany) },
                onEditCompanyClick = { viewModel.openEditCompanyDialog(currentCompany) },
                onDeleteCompanyClick = { viewModel.deleteCompany(currentCompany) },
                onEditProductClick = { product -> viewModel.openEditProductDialog(currentCompany, product) },
                onDeleteProductClick = { product -> viewModel.deleteProduct(product) },
                onOpenSettingsClick = { viewModel.isSettingsOpen.value = true }
            )
        }
        else -> {
            DashboardScreen(
                companies = companies,
                productCounts = productCounts,
                searchQuery = companySearchQuery,
                onSearchQueryChange = { viewModel.companySearchQuery.value = it },
                sortOrder = companySortOrder,
                onSortOrderChange = { viewModel.companySortOrder.value = it },
                displaySettings = displaySettings,
                onCompanyClick = { company -> viewModel.selectCompany(company) },
                onAddCompanyClick = { viewModel.openAddCompanyDialog() },
                onEditCompanyClick = { company -> viewModel.openEditCompanyDialog(company) },
                onDeleteCompanyClick = { company -> viewModel.deleteCompany(company) },
                onOpenSettingsClick = { viewModel.isSettingsOpen.value = true }
            )
        }
    }

    // Add / Edit Company Dialog
    companyDialogState?.let { state ->
        AddEditCompanyDialog(
            companyToEdit = state.companyToEdit,
            onDismiss = { viewModel.dismissCompanyDialog() },
            onCheckDuplicate = { name, excludeId ->
                viewModel.isCompanyNameDuplicate(name, excludeId)
            },
            onSave = { name, logoPath ->
                viewModel.saveCompany(name, logoPath)
            }
        )
    }

    // Add / Edit Product Dialog
    productDialogState?.let { state ->
        AddEditProductDialog(
            companyId = state.company.id,
            companyName = state.company.name,
            productToEdit = state.productToEdit,
            onDismiss = { viewModel.dismissProductDialog() },
            onSave = { name, imagePath ->
                viewModel.saveProduct(name, imagePath)
            }
        )
    }
}
