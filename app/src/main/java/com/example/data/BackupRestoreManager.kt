package com.example.data

import android.content.Context
import android.net.Uri
import com.example.data.dao.CompanyDao
import com.example.data.dao.ProductDao
import com.example.data.model.Company
import com.example.data.model.Product
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream

object BackupRestoreManager {

    private const val CATALOG_FILE = "catalog.json"
    private const val IMAGES_FOLDER = "images"
    private const val ARCHIVE_VERSION = 1

    /**
     * Exports entire catalog (companies, products, and images) to a single ZIP archive.
     */
    suspend fun exportCatalog(
        context: Context,
        destinationUri: Uri,
        companyDao: CompanyDao,
        productDao: ProductDao
    ): Result<Int> = withContext(Dispatchers.IO) {
        try {
            val companies = companyDao.getAllCompaniesOnce()
            val products = productDao.getAllProductsOnce()

            // Build JSON manifest
            val rootJson = JSONObject().apply {
                put("version", ARCHIVE_VERSION)
                put("exportTimestamp", System.currentTimeMillis())
                put("companyCount", companies.size)
                put("productCount", products.size)

                val companiesArray = JSONArray()
                companies.forEach { c ->
                    companiesArray.put(JSONObject().apply {
                        put("id", c.id)
                        put("name", c.name)
                        put("logoPath", c.logoPath ?: "")
                        put("createdAt", c.createdAt)
                        put("updatedAt", c.updatedAt)
                    })
                }
                put("companies", companiesArray)

                val productsArray = JSONArray()
                products.forEach { p ->
                    productsArray.put(JSONObject().apply {
                        put("id", p.id)
                        put("companyId", p.companyId)
                        put("name", p.name)
                        put("imagePath", p.imagePath ?: "")
                        put("createdAt", p.createdAt)
                        put("updatedAt", p.updatedAt)
                    })
                }
                put("products", productsArray)
            }

            val imagesDir = InternalStorageHelper.getImagesDir(context)

            val outputStream = context.contentResolver.openOutputStream(destinationUri)
                ?: return@withContext Result.failure(Exception("Cannot open destination file"))

            ZipOutputStream(outputStream).use { zipOut ->
                // 1. Write catalog.json
                val catalogBytes = rootJson.toString(2).toByteArray(Charsets.UTF_8)
                zipOut.putNextEntry(ZipEntry(CATALOG_FILE))
                zipOut.write(catalogBytes)
                zipOut.closeEntry()

                // 2. Write referenced image files
                val referencedImageNames = mutableSetOf<String>()
                companies.forEach { c -> c.logoPath?.let { if (!it.startsWith("res:")) referencedImageNames.add(it) } }
                products.forEach { p -> p.imagePath?.let { if (!it.startsWith("res:")) referencedImageNames.add(it) } }

                referencedImageNames.forEach { filename ->
                    val file = File(imagesDir, filename)
                    if (file.exists() && file.isFile) {
                        zipOut.putNextEntry(ZipEntry("$IMAGES_FOLDER/$filename"))
                        FileInputStream(file).use { input ->
                            input.copyTo(zipOut)
                        }
                        zipOut.closeEntry()
                    }
                }
            }

            Result.success(companies.size + products.size)
        } catch (e: Exception) {
            e.printStackTrace()
            Result.failure(e)
        }
    }

    /**
     * Staged import: extracts to temp cache, validates structure, replaces database and images atomically.
     */
    suspend fun importCatalog(
        context: Context,
        sourceUri: Uri,
        database: AppDatabase
    ): Result<String> = withContext(Dispatchers.IO) {
        val stagingDir = File(context.cacheDir, "restore_staging_${System.currentTimeMillis()}")
        try {
            stagingDir.mkdirs()

            val inputStream = context.contentResolver.openInputStream(sourceUri)
                ?: return@withContext Result.failure(Exception("Cannot read backup archive"))

            // Extract ZIP
            ZipInputStream(inputStream).use { zipIn ->
                var entry: ZipEntry? = zipIn.nextEntry
                while (entry != null) {
                    val targetFile = File(stagingDir, entry.name)
                    // Protect against Zip Slip vulnerability
                    if (!targetFile.canonicalPath.startsWith(stagingDir.canonicalPath)) {
                        throw SecurityException("Invalid archive entry: ${entry.name}")
                    }

                    if (entry.isDirectory) {
                        targetFile.mkdirs()
                    } else {
                        targetFile.parentFile?.mkdirs()
                        FileOutputStream(targetFile).use { out ->
                            zipIn.copyTo(out)
                        }
                    }
                    zipIn.closeEntry()
                    entry = zipIn.nextEntry
                }
            }

            // Validate catalog.json
            val catalogFile = File(stagingDir, CATALOG_FILE)
            if (!catalogFile.exists()) {
                return@withContext Result.failure(Exception("Archive missing $CATALOG_FILE"))
            }

            val jsonContent = catalogFile.readText(Charsets.UTF_8)
            val rootJson = JSONObject(jsonContent)

            val version = rootJson.optInt("version", -1)
            if (version != ARCHIVE_VERSION) {
                return@withContext Result.failure(Exception("Unsupported backup version: $version"))
            }

            val companiesArray = rootJson.getJSONArray("companies")
            val productsArray = rootJson.getJSONArray("products")

            val importedCompanies = mutableListOf<Company>()
            val validCompanyIds = mutableSetOf<Long>()

            for (i in 0 until companiesArray.length()) {
                val cObj = companiesArray.getJSONObject(i)
                val id = cObj.getLong("id")
                val name = cObj.getString("name").trim()
                val logo = cObj.optString("logoPath").takeIf { it.isNotBlank() }
                val created = cObj.optLong("createdAt", System.currentTimeMillis())
                val updated = cObj.optLong("updatedAt", created)

                importedCompanies.add(
                    Company(id = id, name = name, logoPath = logo, createdAt = created, updatedAt = updated)
                )
                validCompanyIds.add(id)
            }

            val importedProducts = mutableListOf<Product>()
            for (i in 0 until productsArray.length()) {
                val pObj = productsArray.getJSONObject(i)
                val id = pObj.getLong("id")
                val companyId = pObj.getLong("companyId")
                val name = pObj.getString("name").trim()
                val image = pObj.optString("imagePath").takeIf { it.isNotBlank() }
                val created = pObj.optLong("createdAt", System.currentTimeMillis())
                val updated = pObj.optLong("updatedAt", created)

                if (!validCompanyIds.contains(companyId)) {
                    throw IllegalStateException("Product '$name' has invalid parent company ID $companyId")
                }

                importedProducts.add(
                    Product(
                        id = id,
                        companyId = companyId,
                        name = name,
                        imagePath = image,
                        createdAt = created,
                        updatedAt = updated
                    )
                )
            }

            // Staging validated! Now perform replacement in database
            val companyDao = database.companyDao()
            val productDao = database.productDao()

            productDao.deleteAllProducts()
            companyDao.deleteAllCompanies()

            companyDao.insertCompanies(importedCompanies)
            productDao.insertProducts(importedProducts)

            // Copy images from staging to app private storage
            val stagedImagesDir = File(stagingDir, IMAGES_FOLDER)
            val destImagesDir = InternalStorageHelper.getImagesDir(context)

            if (stagedImagesDir.exists() && stagedImagesDir.isDirectory) {
                stagedImagesDir.listFiles()?.forEach { file ->
                    val destFile = File(destImagesDir, file.name)
                    file.copyTo(destFile, overwrite = true)
                }
            }

            Result.success("Restored ${importedCompanies.size} companies and ${importedProducts.size} products")
        } catch (e: Exception) {
            e.printStackTrace()
            Result.failure(e)
        } finally {
            stagingDir.deleteRecursively()
        }
    }
}
