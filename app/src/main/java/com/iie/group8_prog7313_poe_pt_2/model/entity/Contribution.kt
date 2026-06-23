package com.iie.group8_prog7313_poe_pt_2.model.entity

import com.google.firebase.firestore.DocumentId

data class Contribution(
    @DocumentId val id: String = "",
    val amount: Double = 0.0,
    val date: Long = 0L
)
