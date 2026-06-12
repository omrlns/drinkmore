package com.example.data

import kotlinx.coroutines.flow.Flow

class DrinkRepository(private val drinkDao: DrinkDao) {

    val activeUserFlow: Flow<UserEntity?> = drinkDao.getActiveUserFlow()

    suspend fun getActiveUser(): UserEntity? = drinkDao.getActiveUser()

    suspend fun getUserByEmail(email: String): UserEntity? = drinkDao.getUserByEmail(email)

    suspend fun insertUser(user: UserEntity) = drinkDao.insertUser(user)

    suspend fun updateUser(user: UserEntity) = drinkDao.updateUser(user)

    suspend fun logoutActiveUser() {
        drinkDao.markAllLoggedOut()
    }

    fun getLogsForDate(email: String, dateKey: String): Flow<List<HydrationLogEntity>> {
        return drinkDao.getLogsForDate(email, dateKey)
    }

    suspend fun getSumForDate(email: String, dateKey: String): Int {
        return drinkDao.getSumForDate(email, dateKey) ?: 0
    }

    suspend fun insertLog(log: HydrationLogEntity) {
        drinkDao.insertLog(log)
    }

    suspend fun deleteLog(logId: Int) {
        drinkDao.deleteLogById(logId)
    }

    suspend fun clearAllUserLogs(email: String) {
        drinkDao.clearAllUserLogs(email)
    }
}
