package com.iie.group8_prog7313_poe_pt_2.util

import android.content.Context
import android.os.Handler
import android.os.Looper
import android.view.View
import android.widget.Toast
import androidx.core.content.ContextCompat
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.snackbar.Snackbar
import com.iie.group8_prog7313_poe_pt_2.R

object AchievementNotification {
    private val mainHandler = Handler(Looper.getMainLooper())

    fun showAchievementToast(
        context: Context,
        badgeName: String,
        points: Int
    ) {
        mainHandler.post {
            val message = "Achievement Unlocked: $badgeName! +$points points"
            Toast.makeText(context, message, Toast.LENGTH_LONG).show()
        }
    }

    fun showAchievementSnackbar(
        view: View,
        badgeName: String,
        points: Int,
        onActionClick: (() -> Unit)? = null
    ) {
        mainHandler.post {
            val message = "$badgeName unlocked! +$points pts"

            val snackbar = Snackbar.make(view, message, Snackbar.LENGTH_LONG)
            if (onActionClick != null) {
                snackbar.setAction("VIEW") { onActionClick.invoke() }
                snackbar.setActionTextColor(ContextCompat.getColor(view.context, R.color.accent_cyan))
            }
            snackbar.show()
        }
    }

    fun showCelebrationDialog(
        context: Context,
        title: String,
        message: String,
        points: Int
    ) {
        mainHandler.post {
            MaterialAlertDialogBuilder(context)
                .setTitle("$title")
                .setMessage("$message\n\n+$points points awarded!")
                .setPositiveButton("AWESOME!") { dialog, _ -> dialog.dismiss() }
                .show()
        }
    }

    // Convenience method for badge notifications
    fun notifyBadgeUnlocked(
        context: Context,
        badgeKey: String,
        anchorView: View? = null
    ) {
        val badgeName = BadgeIconMapper.getBadgeDisplayName(badgeKey)
        val points = BadgeIconMapper.getBadgePoints(badgeKey)

        when {
            badgeKey == "STREAK_MASTER" || badgeKey == "BUDGET_CHAMPION" -> {
                showCelebrationDialog(
                    context,
                    badgeName,
                    "Congratulations! You've earned the $badgeName badge!",
                    points
                )
            }
            badgeKey == "FIRST_EXPENSE" && anchorView != null -> {
                showAchievementSnackbar(anchorView, badgeName, points)
            }
            else -> {
                showAchievementToast(context, badgeName, points)
            }
        }
    }
}