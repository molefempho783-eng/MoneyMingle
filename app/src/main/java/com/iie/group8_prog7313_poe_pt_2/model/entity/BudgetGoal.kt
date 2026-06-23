package com.iie.group8_prog7313_poe_pt_2.model.entity

import com.google.firebase.firestore.DocumentId

data class BudgetGoal(
    @DocumentId val id: String = "",
    val categoryId: String = "",
    val minAmount: Double = 0.0,
    val maxAmount: Double = 0.0,
    val month: Int = 0,
    val year: Int = 0
)
