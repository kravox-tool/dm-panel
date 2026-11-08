package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "logs")
data class LogEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val type: String, // "INFO", "SUCCESS", "WARN", "ERROR", "BURST", "CHAT", "FLASH"
    val title: String,
    val detail: String,
    val timestamp: Long = System.currentTimeMillis()
)
