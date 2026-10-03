package com.example.data

import android.content.Context
import com.example.data.dao.CompanyDao
import com.example.data.dao.ProductDao
import com.example.data.model.Company
import com.example.data.model.Product
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

object SampleDataSeeder {

    suspend fun seedInitialDataIfEmpty(companyDao: CompanyDao, productDao: ProductDao, context: Context) {
        withContext(Dispatchers.IO) {
            val count = companyDao.getCompanyCount()
            if (count == 0) {
                populateSampleData(companyDao, productDao)
            }
        }
    }

    suspend fun resetToSampleData(companyDao: CompanyDao, productDao: ProductDao) {
        withContext(Dispatchers.IO) {
            productDao.deleteAllProducts()
            companyDao.deleteAllCompanies()
            populateSampleData(companyDao, productDao)
        }
    }

    private suspend fun populateSampleData(companyDao: CompanyDao, productDao: ProductDao) {
        val c1Id = companyDao.insertCompany(
            Company(name = "Lay's & Doritos", logoPath = "res:sample_chips")
        )

        val c2Id = companyDao.insertCompany(
            Company(name = "Oreo & Ritz", logoPath = "res:sample_cookies")
        )

        val c3Id = companyDao.insertCompany(
            Company(name = "Pringles", logoPath = "res:sample_chips")
        )

        val c4Id = companyDao.insertCompany(
            Company(name = "Fizz Soda Beverages", logoPath = "res:sample_drink")
        )

        // Products for Company 1
        productDao.insertProducts(
            listOf(
                Product(companyId = c1Id, name = "Lay's Classic Salted Chips", imagePath = "res:sample_chips"),
                Product(companyId = c1Id, name = "Doritos Nacho Cheese", imagePath = "res:sample_chips"),
                Product(companyId = c1Id, name = "Lay's Barbecue Flavour", imagePath = "res:sample_chips"),
                Product(companyId = c1Id, name = "Cheetos Crunchy Cheddar", imagePath = "res:sample_chips")
            )
        )

        // Products for Company 2
        productDao.insertProducts(
            listOf(
                Product(companyId = c2Id, name = "Oreo Double Stuf Cookies", imagePath = "res:sample_cookies"),
                Product(companyId = c2Id, name = "Ritz Crisp & Thin Crackers", imagePath = "res:sample_cookies"),
                Product(companyId = c2Id, name = "Oreo Golden Vanilla Cookies", imagePath = "res:sample_cookies")
            )
        )

        // Products for Company 3
        productDao.insertProducts(
            listOf(
                Product(companyId = c3Id, name = "Pringles Sour Cream & Onion", imagePath = "res:sample_chips"),
                Product(companyId = c3Id, name = "Pringles Original Salted Can", imagePath = "res:sample_chips")
            )
        )

        // Products for Company 4
        productDao.insertProducts(
            listOf(
                Product(companyId = c4Id, name = "Sparkling Wild Berry Red Soda", imagePath = "res:sample_drink"),
                Product(companyId = c4Id, name = "Citrus Lime Fizz Cooler", imagePath = "res:sample_drink")
            )
        )
    }
}
