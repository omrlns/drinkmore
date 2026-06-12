package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "hydration_logs")
data class HydrationLogEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val userEmail: String,
    val amountMl: Int,
    val timestamp: Long,
    val dateKey: String // Format: "yyyy-MM-dd"
)
