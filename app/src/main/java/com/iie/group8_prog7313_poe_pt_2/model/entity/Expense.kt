package com.iie.group8_prog7313_poe_pt_2.model.entity

import com.google.firebase.firestore.DocumentId

data class Expense(
    @DocumentId val id: String = "",
    val categoryId: String? = null,
    val amount: Double = 0.0,
    val description: String = "",
    val date: Long = 0L,
    val receiptImagePath: String? = null
)
