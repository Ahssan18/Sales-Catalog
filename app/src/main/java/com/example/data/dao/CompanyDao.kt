package com.example.data.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.data.model.Company
import kotlinx.coroutines.flow.Flow

@Dao
interface CompanyDao {
    @Query("SELECT * FROM companies ORDER BY name COLLATE NOCASE ASC")
    fun getCompaniesOrderNameAsc(): Flow<List<Company>>

    @Query("SELECT * FROM companies ORDER BY name COLLATE NOCASE DESC")
    fun getCompaniesOrderNameDesc(): Flow<List<Company>>

    @Query("SELECT * FROM companies ORDER BY createdAt DESC")
    fun getCompaniesOrderRecent(): Flow<List<Company>>

    @Query("SELECT * FROM companies WHERE name LIKE '%' || :query || '%' ORDER BY name COLLATE NOCASE ASC")
    fun searchCompaniesOrderNameAsc(query: String): Flow<List<Company>>

    @Query("SELECT * FROM companies WHERE name LIKE '%' || :query || '%' ORDER BY name COLLATE NOCASE DESC")
    fun searchCompaniesOrderNameDesc(query: String): Flow<List<Company>>

    @Query("SELECT * FROM companies WHERE name LIKE '%' || :query || '%' ORDER BY createdAt DESC")
    fun searchCompaniesOrderRecent(query: String): Flow<List<Company>>

    @Query("SELECT * FROM companies WHERE id = :id LIMIT 1")
    fun getCompanyById(id: Long): Flow<Company?>

    @Query("SELECT * FROM companies WHERE id = :id LIMIT 1")
    suspend fun getCompanyByIdOnce(id: Long): Company?

    @Query("SELECT * FROM companies")
    suspend fun getAllCompaniesOnce(): List<Company>

    @Query("SELECT COUNT(*) FROM companies WHERE LOWER(TRIM(name)) = LOWER(TRIM(:name)) AND id != :excludeId")
    suspend fun countCompaniesWithName(name: String, excludeId: Long = 0): Int

    @Query("SELECT COUNT(*) FROM companies")
    suspend fun getCompanyCount(): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCompany(company: Company): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCompanies(companies: List<Company>): List<Long>

    @Update
    suspend fun updateCompany(company: Company)

    @Delete
    suspend fun deleteCompany(company: Company)

    @Query("DELETE FROM companies WHERE id = :id")
    suspend fun deleteCompanyById(id: Long)

    @Query("DELETE FROM companies")
    suspend fun deleteAllCompanies()
}
