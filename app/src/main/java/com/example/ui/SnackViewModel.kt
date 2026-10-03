package com.example.ui

import android.app.Application
import android.content.Context
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.AppDatabase
import com.example.data.InternalStorageHelper
import com.example.data.SampleDataSeeder
import com.example.data.SnackRepository
import com.example.data.model.Company
import com.example.data.model.DisplaySettings
import com.example.data.model.ImageScale
import com.example.data.model.Product
import com.example.data.model.RowSpacing
import com.example.data.model.SortOrder
import com.example.data.model.TextScale
import com.example.data.model.ThemeMode
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

@OptIn(ExperimentalCoroutinesApi::class)
class SnackViewModel(application: Application) : AndroidViewModel(application) {

    private val repository: SnackRepository
    private val prefs = application.getSharedPreferences("salesbook_settings", Context.MODE_PRIVATE)

    // Navigation state
    private val _selectedCompany = MutableStateFlow<Company?>(null)
    val selectedCompany: StateFlow<Company?> = _selectedCompany.asStateFlow()

    val isSettingsOpen = MutableStateFlow(false)

    // Search & Sort states
    val companySearchQuery = MutableStateFlow("")
    val productSearchQuery = MutableStateFlow("")
    val companySortOrder = MutableStateFlow(SortOrder.NAME_ASC)

    // Display Settings State
    private val _displaySettings = MutableStateFlow(loadSavedSettings())
    val displaySettings: StateFlow<DisplaySettings> = _displaySettings.asStateFlow()

    // Dialog states
    val companyDialogState = MutableStateFlow<CompanyDialogState?>(null)
    val productDialogState = MutableStateFlow<ProductDialogState?>(null)

    init {
        val database = AppDatabase.getDatabase(application)
        repository = SnackRepository(database.companyDao(), database.productDao(), application)

        viewModelScope.launch {
            SampleDataSeeder.seedInitialDataIfEmpty(
                database.companyDao(),
                database.productDao(),
                application
            )
        }
    }

    // Reactive Companies list
    val companies: StateFlow<List<Company>> = combine(
        companySearchQuery,
        companySortOrder
    ) { query, sort ->
        query to sort
    }.flatMapLatest { (query, sort) ->
        repository.getCompanies(query, sort)
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    // Map of Company ID -> Product Count
    val productCounts: StateFlow<Map<Long, Int>> = repository.getCompanies("", SortOrder.NAME_ASC)
        .flatMapLatest { companiesList ->
            val database = AppDatabase.getDatabase(getApplication())
            val productDao = database.productDao()
            combine(
                companiesList.map { company ->
                    productDao.getProductCountForCompany(company.id)
                }
            ) { countsArray ->
                companiesList.mapIndexed { index, company ->
                    company.id to (countsArray.getOrNull(index) ?: 0)
                }.toMap()
            }
        }.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyMap()
        )

    // Reactive Products list for selected company
    val currentProducts: StateFlow<List<Product>> = combine(
        _selectedCompany,
        productSearchQuery
    ) { company, query ->
        company to query
    }.flatMapLatest { (company, query) ->
        if (company == null) {
            kotlinx.coroutines.flow.flowOf(emptyList())
        } else {
            repository.getProducts(company.id, query)
        }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    fun selectCompany(company: Company?) {
        _selectedCompany.value = company
        productSearchQuery.value = ""
    }

    // Settings actions
    fun updateDisplaySettings(newSettings: DisplaySettings) {
        _displaySettings.value = newSettings
        prefs.edit().apply {
            putString("text_scale", newSettings.textScale.name)
            putString("image_scale", newSettings.imageScale.name)
            putString("row_spacing", newSettings.rowSpacing.name)
            putString("theme_mode", newSettings.themeMode.name)
            apply()
        }
    }

    fun resetDisplayDefaults() {
        updateDisplaySettings(DisplaySettings())
    }

    private fun loadSavedSettings(): DisplaySettings {
        return try {
            val text = TextScale.valueOf(prefs.getString("text_scale", TextScale.MEDIUM.name) ?: TextScale.MEDIUM.name)
            val image = ImageScale.valueOf(prefs.getString("image_scale", ImageScale.STANDARD.name) ?: ImageScale.STANDARD.name)
            val spacing = RowSpacing.valueOf(prefs.getString("row_spacing", RowSpacing.COMFORTABLE.name) ?: RowSpacing.COMFORTABLE.name)
            val theme = ThemeMode.valueOf(prefs.getString("theme_mode", ThemeMode.SYSTEM.name) ?: ThemeMode.SYSTEM.name)
            DisplaySettings(text, image, spacing, theme)
        } catch (e: Exception) {
            DisplaySettings()
        }
    }

    // Company CRUD
    fun openAddCompanyDialog() {
        companyDialogState.value = CompanyDialogState(companyToEdit = null)
    }

    fun openEditCompanyDialog(company: Company) {
        companyDialogState.value = CompanyDialogState(companyToEdit = company)
    }

    fun dismissCompanyDialog() {
        companyDialogState.value = null
    }

    suspend fun isCompanyNameDuplicate(name: String, excludeId: Long): Boolean {
        return repository.isCompanyNameDuplicate(name, excludeId)
    }

    fun saveCompany(name: String, logoPath: String?) {
        val state = companyDialogState.value ?: return
        viewModelScope.launch {
            if (state.companyToEdit == null) {
                repository.insertCompany(Company(name = name, logoPath = logoPath))
            } else {
                val updated = state.companyToEdit.copy(
                    name = name,
                    logoPath = logoPath,
                    updatedAt = System.currentTimeMillis()
                )
                repository.updateCompany(updated, state.companyToEdit.logoPath)
                if (_selectedCompany.value?.id == updated.id) {
                    _selectedCompany.value = updated
                }
            }
            dismissCompanyDialog()
        }
    }

    fun deleteCompany(company: Company) {
        viewModelScope.launch {
            repository.deleteCompany(company)
            if (_selectedCompany.value?.id == company.id) {
                _selectedCompany.value = null
            }
        }
    }

    // Product CRUD
    fun openAddProductDialog(company: Company) {
        productDialogState.value = ProductDialogState(company = company, productToEdit = null)
    }

    fun openEditProductDialog(company: Company, product: Product) {
        productDialogState.value = ProductDialogState(company = company, productToEdit = product)
    }

    fun dismissProductDialog() {
        productDialogState.value = null
    }

    suspend fun isProductNameDuplicate(companyId: Long, name: String, excludeId: Long): Boolean {
        return repository.isProductNameDuplicate(companyId, name, excludeId)
    }

    fun saveProduct(name: String, imagePath: String?) {
        val state = productDialogState.value ?: return
        viewModelScope.launch {
            if (state.productToEdit == null) {
                repository.insertProduct(
                    Product(
                        companyId = state.company.id,
                        name = name,
                        imagePath = imagePath
                    )
                )
            } else {
                val updated = state.productToEdit.copy(
                    name = name,
                    imagePath = imagePath,
                    updatedAt = System.currentTimeMillis()
                )
                repository.updateProduct(updated, state.productToEdit.imagePath)
            }
            dismissProductDialog()
        }
    }

    fun deleteProduct(product: Product) {
        viewModelScope.launch {
            repository.deleteProduct(product)
        }
    }

    fun resetToSampleData() {
        viewModelScope.launch {
            repository.resetToSampleData()
            _selectedCompany.value = null
        }
    }

    fun clearAllData() {
        viewModelScope.launch {
            repository.clearAllData()
            _selectedCompany.value = null
        }
    }
}

data class CompanyDialogState(val companyToEdit: Company?)
data class ProductDialogState(val company: Company, val productToEdit: Product?)
