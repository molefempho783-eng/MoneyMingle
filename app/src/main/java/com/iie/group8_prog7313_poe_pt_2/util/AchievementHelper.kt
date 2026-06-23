package com.iie.group8_prog7313_poe_pt_2.util

import android.content.Context
import android.content.SharedPreferences
import android.view.View
import android.widget.Toast
import androidx.core.content.ContextCompat
import com.google.android.material.snackbar.Snackbar
import com.iie.group8_prog7313_poe_pt_2.R
import com.iie.group8_prog7313_poe_pt_2.model.repository.GamificationRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Helper for checking newly earned badges and showing notifications.
 */
object AchievementHelper {

    private const val PREFS_NAME = "achievement_prefs"
    private const val KEY_SEEN_BADGES = "seen_badges"

    private fun getSeenBadges(context: Context): MutableSet<String> {
        val prefs: SharedPreferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return prefs.getStringSet(KEY_SEEN_BADGES, emptySet())?.toMutableSet() ?: mutableSetOf()
    }

    private fun saveSeenBadges(context: Context, seen: Set<String>) {
        val prefs: SharedPreferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().putStringSet(KEY_SEEN_BADGES, seen).apply()
    }

    /**
     * Checks for newly earned badges and shows appropriate notifications.
     * Returns the list of newly earned badge keys.
     */
    suspend fun checkAndNotifyNewBadges(
        context: Context,
        repository: GamificationRepository,
        userId: String,
        anchorView: View? = null
    ): List<String> {
        return withContext(Dispatchers.IO) {
            val currentBadges = repository.getEarnedBadges(userId).toSet()
            val seenBadges = getSeenBadges(context)
            val newBadges = (currentBadges - seenBadges).toList()

            if (newBadges.isNotEmpty()) {
                saveSeenBadges(context, currentBadges)
                withContext(Dispatchers.Main) {
                    showNotifications(context, newBadges, anchorView)
                }
            }
            newBadges
        }
    }

    private fun showNotifications(context: Context, badgeKeys: List<String>, anchorView: View?) {
        for (badgeKey in badgeKeys) {
            val name = BadgeIconMapper.getBadgeDisplayName(badgeKey)
            val points = BadgeIconMapper.getBadgePoints(badgeKey)

            when {
                badgeKey == "STREAK_MASTER" || badgeKey == "BUDGET_CHAMPION" -> {
                    showCelebrationDialog(context, name, points)
                }
                badgeKey == "FIRST_EXPENSE" && anchorView != null -> {
                    showAchievementSnackbar(anchorView, name, points)
                }
                else -> {
                    showAchievementToast(context, name, points)
                }
            }
        }

        if (badgeKeys.size > 1 && anchorView != null) {
            Snackbar.make(
                anchorView,
                "${badgeKeys.size} new achievements! +${badgeKeys.sumOf { BadgeIconMapper.getBadgePoints(it) }} points",
                Snackbar.LENGTH_LONG
            ).show()
        }
    }

    fun showAchievementToast(context: Context, badgeName: String, points: Int) {
        Toast.makeText(context, "Achievement Unlocked: $badgeName! +$points points", Toast.LENGTH_LONG).show()
    }

    fun showAchievementSnackbar(view: View, badgeName: String, points: Int, onAction: (() -> Unit)? = null) {
        val snackbar = Snackbar.make(view, "$badgeName unlocked! +$points pts", Snackbar.LENGTH_LONG)
        if (onAction != null) {
            snackbar.setAction("VIEW") { onAction.invoke() }
            snackbar.setActionTextColor(ContextCompat.getColor(view.context, R.color.accent_cyan))
        }
        snackbar.show()
    }

    fun showCelebrationDialog(context: Context, badgeName: String, points: Int) {
        com.google.android.material.dialog.MaterialAlertDialogBuilder(context)
            .setTitle("🎉 Achievement Unlocked!")
            .setMessage("$badgeName\n\n+$points points awarded!")
            .setPositiveButton("AWESOME!") { dialog, _ -> dialog.dismiss() }
            .show()
    }
}