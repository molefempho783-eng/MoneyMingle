package com.iie.group8_prog7313_poe_pt_2.util

import com.iie.group8_prog7313_poe_pt_2.R
import com.iie.group8_prog7313_poe_pt_2.model.entity.BadgeCategory

object BadgeIconMapper {
    fun getIconForBadge(badgeKey: String): Int {
        return when {
            badgeKey.contains("STREAK_STARTER") -> R.drawable.ic_fire_streak_dashboard
            badgeKey.contains("STREAK_MASTER") -> R.drawable.ic_lightning_bolt
            badgeKey.contains("BUDGET_CHAMPION") -> R.drawable.ic_trophy_badge
            badgeKey.contains("RECEIPT_WEEKLY") -> R.drawable.ic_receipt_hunter_badge
            badgeKey.contains("RECEIPT_MONTHLY") -> R.drawable.ic_receipt_hunter_badge
            badgeKey.contains("RECEIPT_HUNTER") -> R.drawable.ic_receipt_hunter_badge
            badgeKey.contains("EARLY_BIRD") -> R.drawable.ic_bonus_badge
            badgeKey.contains("WISHLIST_ACHIEVER") -> R.drawable.ic_wishlist_badge
            else -> R.drawable.ic_default_badge
        }
    }

    fun getBadgeDisplayName(badgeKey: String): String {
        return when {
            badgeKey == "FIRST_EXPENSE" -> "First Expense"
            badgeKey == "STREAK_STARTER" -> "3-Day Streak"
            badgeKey == "STREAK_MASTER" -> "7-Day Streak"
            badgeKey == "BUDGET_CHAMPION" -> "30-Day Champion"
            badgeKey == "RECEIPT_HUNTER" -> "Receipt Hunter"
            badgeKey.startsWith("EARLY_BIRD") -> "Early Bird"
            badgeKey.startsWith("RECEIPT_WEEKLY") -> "Weekly Receipt Hunter"
            badgeKey.startsWith("RECEIPT_MONTHLY") -> "Monthly Receipt Champion"
            badgeKey == "WISHLIST_ACHIEVER" -> "Wishlist Master"
            else -> badgeKey.replace("_", " ")
                .split(" ")
                .joinToString(" ") { word ->
                    if (word.isNotEmpty()) word[0].uppercaseChar() + word.substring(1).lowercase()
                    else word
                }
        }
    }

    fun getBadgePoints(badgeKey: String): Int {
        return when {
            badgeKey == "FIRST_EXPENSE" -> 50
            badgeKey == "STREAK_STARTER" -> 100
            badgeKey == "STREAK_MASTER" -> 200
            badgeKey == "BUDGET_CHAMPION" -> 500
            badgeKey == "RECEIPT_HUNTER" -> 150
            badgeKey.startsWith("EARLY_BIRD") -> 50
            badgeKey.startsWith("RECEIPT_WEEKLY") -> 75
            badgeKey.startsWith("RECEIPT_MONTHLY") -> 150
            badgeKey == "WISHLIST_ACHIEVER" -> 200
            else -> 50
        }
    }

    fun getCategoryFromBadgeKey(badgeKey: String): BadgeCategory {
        return when {
            badgeKey.contains("STREAK") -> BadgeCategory.STREAK
            badgeKey.contains("RECEIPT") -> BadgeCategory.RECEIPT
            badgeKey.contains("EARLY_BIRD") -> BadgeCategory.MONTHLY_BONUS
            badgeKey.contains("WISHLIST") -> BadgeCategory.WISHLIST
            badgeKey == "FIRST_EXPENSE" -> BadgeCategory.EXPENSE
            badgeKey == "BUDGET_CHAMPION" -> BadgeCategory.ACHIEVEMENT
            else -> BadgeCategory.ACHIEVEMENT
        }
    }
}

// References:
// 1. Google. 2026. Fragment lifecycle. https://developer.android.com/guide/fragments/lifecycle
// 2. Google. 2026. Kotlin coroutines on Android. https://developer.android.com/kotlin/coroutines