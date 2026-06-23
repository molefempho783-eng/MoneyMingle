package com.iie.group8_prog7313_poe_pt_2.model.entity

import com.iie.group8_prog7313_poe_pt_2.R


enum class BadgeCategory {
    STREAK,
    RECEIPT,
    MONTHLY_BONUS,
    WISHLIST,
    EXPENSE,
    ACHIEVEMENT;

    fun getBackgroundColor(): Int {
        return when (this) {
            STREAK -> R.color.accent_orange_light
            RECEIPT -> R.color.accent_pink
            MONTHLY_BONUS -> R.color.accent_amber
            WISHLIST -> R.color.accent_purple_light
            EXPENSE -> R.color.accent_cyan
            ACHIEVEMENT -> R.color.accent_green_bright
        }
    }

    fun getTextColor(): Int {
        // All use white text for contrast against bright colors
        return R.color.text_light
    }
}