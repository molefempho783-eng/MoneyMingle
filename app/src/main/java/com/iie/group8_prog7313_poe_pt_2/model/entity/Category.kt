package com.iie.group8_prog7313_poe_pt_2.model.entity

import com.google.firebase.firestore.DocumentId

data class Category(
    @DocumentId val id: String = "",
    val name: String = ""
)
