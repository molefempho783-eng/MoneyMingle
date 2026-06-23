package com.iie.group8_prog7313_poe_pt_2.util

import android.content.Context
import com.iie.group8_prog7313_poe_pt_2.model.repository.GamificationRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

data class NewBadgeInfo(
    val badgeKey: String,
    val badgeName: String,
    val points: Int,
    val description: String
)

class AchievementChecker(private val repository: GamificationRepository) {

    suspend fun checkForNewBadges(
        userId: String,
        previousBadges: Set<String>
    ): List<NewBadgeInfo> {
        return withContext(Dispatchers.IO) {
            val currentBadges = repository.getEarnedBadges(userId).toSet()
            val newlyEarned = (currentBadges - previousBadges).toList()

            newlyEarned.map { badgeKey ->
                NewBadgeInfo(
                    badgeKey = badgeKey,
                    badgeName = getBadgeDisplayName(badgeKey),
                    points = getBadgePoints(badgeKey),
                    description = getBadgeDescription(badgeKey)
                )
            }
        }
    }

    fun getBadgeDisplayName(badgeKey: String): String {
        return BadgeIconMapper.getBadgeDisplayName(badgeKey)
    }

    fun getBadgePoints(badgeKey: String): Int {
        return BadgeIconMapper.getBadgePoints(badgeKey)
    }

    fun getBadgeDescription(badgeKey: String): String {
        return when (badgeKey) {
            "FIRST_EXPENSE" -> "Tracked your first expense"
            "STREAK_STARTER" -> "Logged expenses for 3 days in a row"
            "STREAK_MASTER" -> "Achieved a 7-day logging streak"
            "BUDGET_CHAMPION" -> "Maintained budget for 30 days"
            "RECEIPT_HUNTER" -> "Uploaded 5 receipts"
            "WISHLIST_ACHIEVER" -> "Saved enough for a wishlist item"
            else -> {
                if (badgeKey.startsWith("EARLY_BIRD")) {
                    "Logged an expense on the 1st of the month"
                } else {
                    "Awesome achievement unlocked!"
                }
            }
        }
    }
}