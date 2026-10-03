package com.example.data

import android.content.Context
import com.example.data.dao.CompanyDao
import com.example.data.dao.ProductDao
import com.example.data.model.Company
import com.example.data.model.Product
import com.example.data.model.SortOrder
import kotlinx.coroutines.flow.Flow

class SnackRepository(
    private val companyDao: CompanyDao,
    private val productDao: ProductDao,
    private val context: Context
) {
    fun getCompanies(query: String, sortOrder: SortOrder): Flow<List<Company>> {
        val trimmed = query.trim()
        return if (trimmed.isEmpty()) {
            when (sortOrder) {
                SortOrder.NAME_ASC -> companyDao.getCompaniesOrderNameAsc()
                SortOrder.NAME_DESC -> companyDao.getCompaniesOrderNameDesc()
                SortOrder.RECENT -> companyDao.getCompaniesOrderRecent()
            }
        } else {
            when (sortOrder) {
                SortOrder.NAME_ASC -> companyDao.searchCompaniesOrderNameAsc(trimmed)
                SortOrder.NAME_DESC -> companyDao.searchCompaniesOrderNameDesc(trimmed)
                SortOrder.RECENT -> companyDao.searchCompaniesOrderRecent(trimmed)
            }
        }
    }

    fun getProducts(companyId: Long, query: String, sortOrder: SortOrder = SortOrder.NAME_ASC): Flow<List<Product>> {
        val trimmed = query.trim()
        return if (trimmed.isEmpty()) {
            when (sortOrder) {
                SortOrder.RECENT -> productDao.getProductsByCompanyIdRecent(companyId)
                else -> productDao.getProductsByCompanyId(companyId)
            }
        } else {
            productDao.searchProducts(companyId, trimmed)
        }
    }

    fun getProductCountForCompany(companyId: Long): Flow<Int> =
        productDao.getProductCountForCompany(companyId)

    suspend fun getProductCountForCompanyOnce(companyId: Long): Int =
        productDao.getProductCountForCompanyOnce(companyId)

    suspend fun isCompanyNameDuplicate(name: String, excludeId: Long = 0): Boolean {
        return companyDao.countCompaniesWithName(name.trim(), excludeId) > 0
    }

    suspend fun isProductNameDuplicate(companyId: Long, name: String, excludeId: Long = 0): Boolean {
        return productDao.countProductsWithNameInCompany(companyId, name.trim(), excludeId) > 0
    }

    suspend fun insertCompany(company: Company): Long = companyDao.insertCompany(company)

    suspend fun updateCompany(company: Company, oldLogoPath: String?) {
        companyDao.updateCompany(company)
        if (oldLogoPath != null && oldLogoPath != company.logoPath) {
            InternalStorageHelper.deleteInternalImage(context, oldLogoPath)
        }
    }

    suspend fun deleteCompany(company: Company) {
        // Collect product images before deletion
        val products = productDao.getAllProductsOnce().filter { it.companyId == company.id }
        products.forEach { p ->
            InternalStorageHelper.deleteInternalImage(context, p.imagePath)
        }
        InternalStorageHelper.deleteInternalImage(context, company.logoPath)
        companyDao.deleteCompany(company)
    }

    suspend fun insertProduct(product: Product): Long = productDao.insertProduct(product)

    suspend fun updateProduct(product: Product, oldImagePath: String?) {
        productDao.updateProduct(product)
        if (oldImagePath != null && oldImagePath != product.imagePath) {
            InternalStorageHelper.deleteInternalImage(context, oldImagePath)
        }
    }

    suspend fun deleteProduct(product: Product) {
        InternalStorageHelper.deleteInternalImage(context, product.imagePath)
        productDao.deleteProduct(product)
    }

    suspend fun resetToSampleData() {
        SampleDataSeeder.resetToSampleData(companyDao, productDao)
    }

    suspend fun clearAllData() {
        productDao.deleteAllProducts()
        companyDao.deleteAllCompanies()
    }
}
