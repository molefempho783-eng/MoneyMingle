package com.iie.group8_prog7313_poe_pt_2.model.entity

import com.google.firebase.firestore.DocumentId

data class Gamification(
    val currentStreak: Int = 0,
    val bestStreak: Int = 0,
    val lastLogDate: Long = 0L,
    val earnedBadges: List<String> = emptyList(),
    val lastMonthBonusEarned: Long = 0L,
    val receiptUploadCount: Int = 0,
    val receiptHunterCompleted: Boolean = false,
    // Weekly receipt challenge (resets every Monday)
    val weeklyReceiptCount: Int = 0,
    val lastWeekStart: Long = 0L,
    val weeklyReceiptClaimed: Boolean = false,
    // Monthly receipt challenge (resets every 1st of month)
    val monthlyReceiptCount: Int = 0,
    val lastMonthlyReceiptStart: Long = 0L,
    val monthlyReceiptClaimed: Boolean = false
)