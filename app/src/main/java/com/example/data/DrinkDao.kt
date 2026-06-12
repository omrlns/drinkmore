package com.example.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface DrinkDao {
    @Query("SELECT * FROM users WHERE email = :email LIMIT 1")
    suspend fun getUserByEmail(email: String): UserEntity?

    @Query("SELECT * FROM users WHERE isLoggedIn = 1 LIMIT 1")
    fun getActiveUserFlow(): Flow<UserEntity?>

    @Query("SELECT * FROM users WHERE isLoggedIn = 1 LIMIT 1")
    suspend fun getActiveUser(): UserEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertUser(user: UserEntity)

    @Update
    suspend fun updateUser(user: UserEntity)

    @Query("UPDATE users SET isLoggedIn = 0")
    suspend fun markAllLoggedOut()

    @Query("SELECT * FROM hydration_logs WHERE userEmail = :email AND dateKey = :dateKey ORDER BY timestamp DESC")
    fun getLogsForDate(email: String, dateKey: String): Flow<List<HydrationLogEntity>>

    @Query("SELECT SUM(amountMl) FROM hydration_logs WHERE userEmail = :email AND dateKey = :dateKey")
    suspend fun getSumForDate(email: String, dateKey: String): Int?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertLog(log: HydrationLogEntity)

    @Query("DELETE FROM hydration_logs WHERE id = :logId")
    suspend fun deleteLogById(logId: Int)

    @Query("DELETE FROM hydration_logs WHERE userEmail = :email")
    suspend fun clearAllUserLogs(email: String)
}
