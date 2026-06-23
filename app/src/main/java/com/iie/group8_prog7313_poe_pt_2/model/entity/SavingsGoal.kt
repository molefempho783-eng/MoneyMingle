package com.iie.group8_prog7313_poe_pt_2.model.entity

import com.google.firebase.firestore.DocumentId

data class SavingsGoal(
    @DocumentId val id: String = "",
    val name: String = "",
    val targetAmount: Double = 0.0,
    val savedAmount: Double = 0.0,
    val targetDate: Long = 0L
) {
    val progressPercent: Int
        get() = if (targetAmount > 0) {
            (savedAmount / targetAmount * 100).coerceIn(0.0, 100.0).toInt()
        } else 0

    val isCompleted: Boolean
        get() = targetAmount > 0 && savedAmount >= targetAmount
}
