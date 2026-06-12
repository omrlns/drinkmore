package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "users")
data class UserEntity(
    @PrimaryKey val email: String,
    val name: String,
    val passwordHash: String,
    val weightKg: Double,
    val dailyGoalMl: Int,
    val reminderIntervalMinutes: Int,
    val isLoggedIn: Boolean = false,
    val streakDays: Int = 0,
    val lastGoalAchievedDate: String = ""
)
