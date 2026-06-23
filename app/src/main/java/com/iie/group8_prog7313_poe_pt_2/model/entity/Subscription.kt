package com.iie.group8_prog7313_poe_pt_2.model.entity

import com.google.firebase.firestore.DocumentId

data class Subscription(
    @DocumentId val id: String = "",
    val name: String = "",
    val amount: Double = 0.0,
    val billingCycle: String = "monthly",
    val startDate: Long = 0L,
)
