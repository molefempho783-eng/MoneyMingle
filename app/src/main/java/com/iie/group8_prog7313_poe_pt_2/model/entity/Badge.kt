package com.iie.group8_prog7313_poe_pt_2.model

import com.iie.group8_prog7313_poe_pt_2.R
import com.iie.group8_prog7313_poe_pt_2.model.entity.BadgeCategory

data class Badge(
    val id: String,
    val name: String,
    val description: String,
    val iconResId: Int,
    val points: Int,
    val dateEarned: Long,
    val category: BadgeCategory
)
