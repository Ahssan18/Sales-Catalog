package com.example.data.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.data.model.Product
import kotlinx.coroutines.flow.Flow

@Dao
interface ProductDao {
    @Query("SELECT * FROM products WHERE companyId = :companyId ORDER BY name COLLATE NOCASE ASC")
    fun getProductsByCompanyId(companyId: Long): Flow<List<Product>>

    @Query("SELECT * FROM products WHERE companyId = :companyId ORDER BY createdAt DESC")
    fun getProductsByCompanyIdRecent(companyId: Long): Flow<List<Product>>

    @Query("SELECT * FROM products WHERE companyId = :companyId AND name LIKE '%' || :query || '%' ORDER BY name COLLATE NOCASE ASC")
    fun searchProducts(companyId: Long, query: String): Flow<List<Product>>

    @Query("SELECT * FROM products")
    suspend fun getAllProductsOnce(): List<Product>

    @Query("SELECT * FROM products WHERE id = :id LIMIT 1")
    fun getProductById(id: Long): Flow<Product?>

    @Query("SELECT COUNT(*) FROM products WHERE companyId = :companyId")
    fun getProductCountForCompany(companyId: Long): Flow<Int>

    @Query("SELECT COUNT(*) FROM products WHERE companyId = :companyId")
    suspend fun getProductCountForCompanyOnce(companyId: Long): Int

    @Query("SELECT COUNT(*) FROM products WHERE companyId = :companyId AND LOWER(TRIM(name)) = LOWER(TRIM(:name)) AND id != :excludeId")
    suspend fun countProductsWithNameInCompany(companyId: Long, name: String, excludeId: Long = 0): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertProduct(product: Product): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertProducts(products: List<Product>): List<Long>

    @Update
    suspend fun updateProduct(product: Product)

    @Delete
    suspend fun deleteProduct(product: Product)

    @Query("DELETE FROM products WHERE id = :id")
    suspend fun deleteProductById(id: Long)

    @Query("DELETE FROM products WHERE companyId = :companyId")
    suspend fun deleteProductsByCompanyId(companyId: Long)

    @Query("DELETE FROM products")
    suspend fun deleteAllProducts()
}
