package com.example.financemanager.data.local

import androidx.room.*
import kotlinx.coroutines.flow.Flow
import java.util.UUID

@Entity(tableName = "transactions_table")
data class TransactionItem(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val userId: String,
    val title: String,
    val amount: Double,
    val isIncome: Boolean,
    val date: String,
    val imageUrl: String? = null,
    val location: String? = null
)


@Dao
interface FinanceDao {
    @Query("SELECT * FROM transactions_table ORDER BY date DESC")
    fun getAllItems(): Flow<List<TransactionItem>>

    @Query("SELECT * FROM transactions_table WHERE id = :id")
    suspend fun getItemById(id: String): TransactionItem?

    @Query("DELETE FROM transactions_table")
    suspend fun clearAll()

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(item: TransactionItem)

    @Update
    suspend fun update(item: TransactionItem)

    @Delete
    suspend fun delete(item: TransactionItem)
}

@Database(entities = [TransactionItem::class], version = 5, exportSchema = false)
abstract class AppDatabase : RoomDatabase() {
    abstract fun financeDao(): FinanceDao
}