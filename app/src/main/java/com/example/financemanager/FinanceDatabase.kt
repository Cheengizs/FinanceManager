package com.example.financemanager

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Entity(tableName = "transactions_table")
data class TransactionItem(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val title: String,
    val amount: Double,
    val isIncome: Boolean,
    val date: String
)


@Dao
interface FinanceDao {
    @Query("SELECT * FROM transactions_table ORDER BY id DESC")
    fun getAllItems(): Flow<List<TransactionItem>>


    @Query("SELECT * FROM transactions_table WHERE id = :id")
    suspend fun getItemById(id: Int): TransactionItem?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(item: TransactionItem)

    @Update
    suspend fun update(item: TransactionItem)

    @Delete
    suspend fun delete(item: TransactionItem)
}

@Database(entities = [TransactionItem::class], version = 2, exportSchema = false)
abstract class AppDatabase : RoomDatabase() {
    abstract fun financeDao(): FinanceDao
}