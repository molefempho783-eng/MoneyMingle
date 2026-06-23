package com.iie.group8_prog7313_poe_pt_2.util

import android.content.Context
import android.widget.Toast
import com.iie.group8_prog7313_poe_pt_2.model.repository.GamificationRepository
import kotlinx.coroutines.*

object AchievementNotifier {
    private var previousBadgeSet: Set<String> = emptySet()

    suspend fun checkAndNotifyNewBadges(
        context: Context,
        repository: GamificationRepository,
        userId: String
    ): List<String> {
        return withContext(Dispatchers.IO) {
            val currentBadges = repository.getEarnedBadges(userId).toSet()
            val newBadges = (currentBadges - previousBadgeSet).toList()

            if (newBadges.isNotEmpty() && previousBadgeSet.isNotEmpty()) {
                withContext(Dispatchers.Main) {
                    newBadges.forEach { badgeKey ->
                        val badgeName = BadgeIconMapper.getBadgeDisplayName(badgeKey)
                        val points = BadgeIconMapper.getBadgePoints(badgeKey)

                        Toast.makeText(
                            context,
                            "Achievement: $badgeName! +$points points",
                            Toast.LENGTH_LONG
                        ).show()
                    }
                }
            }

            previousBadgeSet = currentBadges
            newBadges
        }
    }

    fun reset() {
        previousBadgeSet = emptySet()
    }
}