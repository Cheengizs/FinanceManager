package com.example.financemanager

import androidx.room.*
import kotlinx.coroutines.flow.Flow

// 1. Новая сущность: Транзакция
@Entity(tableName = "transactions_table")
data class TransactionItem(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val title: String,      // Название операции ("Продукты", "Стипендия")
    val amount: Double,     // Сумма (например, 150.50)
    val isIncome: Boolean,  // true = Доход (Плюс), false = Расход (Минус)
    val date: String        // Дата
)

// 2. Обновленный DAO (Пульт управления)
@Dao
interface FinanceDao {
    // Получаем все транзакции от новых к старым
    @Query("SELECT * FROM transactions_table ORDER BY id DESC")
    fun getAllItems(): Flow<List<TransactionItem>>

    // Найти одну транзакцию для экрана деталей
    @Query("SELECT * FROM transactions_table WHERE id = :id")
    suspend fun getItemById(id: Int): TransactionItem?

    // Добавить новую
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(item: TransactionItem)

    // Обновить существующую
    @Update
    suspend fun update(item: TransactionItem)

    // Удалить
    @Delete
    suspend fun delete(item: TransactionItem)
}

// 3. Класс базы данных
@Database(entities = [TransactionItem::class], version = 2, exportSchema = false) // Повысили версию до 2!
abstract class AppDatabase : RoomDatabase() {
    abstract fun financeDao(): FinanceDao
}