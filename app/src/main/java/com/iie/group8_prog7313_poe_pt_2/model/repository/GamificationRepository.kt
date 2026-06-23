package com.iie.group8_prog7313_poe_pt_2.model.repository

import android.util.Log
import com.google.firebase.firestore.FirebaseFirestore
import com.iie.group8_prog7313_poe_pt_2.model.entity.Gamification
import com.iie.group8_prog7313_poe_pt_2.util.BadgeIconMapper
import com.iie.group8_prog7313_poe_pt_2.util.DateTimeUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import java.util.Calendar

class GamificationRepository {

    private val db = FirebaseFirestore.getInstance()
    private val TAG = "GamificationRepo"

    private fun statsRef(userId: String) =
        db.collection("users")
            .document(userId)
            .collection("gamification")
            .document("stats")

    suspend fun getStats(userId: String): Gamification {
        return try {
            val snapshot = statsRef(userId).get().await()
            snapshot.toObject(Gamification::class.java) ?: Gamification()
        } catch (e: Exception) {
            Log.e(TAG, "Error getting stats: ${e.message}")
            Gamification()
        }
    }

    /**
     * Check monthly Early Bird bonus status.
     * Returns Pair(hasEarnedThisMonth, canClaimNow).
     * canClaimNow is true only on the 1st of the month after logging an expense.
     */
    suspend fun checkMonthlyBonusStatus(userId: String): Pair<Boolean, Boolean> {
        val stats = getStats(userId)
        val currentMonthStart = getCurrentMonthStartMillis()

        val hasEarnedThisMonth = stats.lastMonthBonusEarned == currentMonthStart

        val today = DateTimeUtils.startOfDayMillis(System.currentTimeMillis())
        val isFirstDayOfMonth = isFirstDayOfMonth(today)
        val hasLoggedToday = stats.lastLogDate == today

        val canClaimNow = isFirstDayOfMonth && hasLoggedToday && !hasEarnedThisMonth

        return Pair(hasEarnedThisMonth, canClaimNow)
    }

    /**
     * Manually claim the first-of-month Early Bird bonus.
     * Returns the badge key on success, null if already claimed or error.
     */
    suspend fun claimMonthlyBonus(userId: String): String? {
        return try {
            val stats = getStats(userId)
            val currentMonthStart = getCurrentMonthStartMillis()

            if (stats.lastMonthBonusEarned == currentMonthStart) return null

            val bonusBadge = "EARLY_BIRD_${formatMonthYear(currentMonthStart)}"
            val badges = stats.earnedBadges.toMutableList()
            if (!badges.contains(bonusBadge)) {
                badges.add(bonusBadge)
            }

            val updated = stats.copy(
                lastMonthBonusEarned = currentMonthStart,
                earnedBadges = badges
            )
            statsRef(userId).set(updated).await()
            updateUserTotalPoints(userId, 50)
            bonusBadge
        } catch (e: Exception) {
            Log.e(TAG, "Error claiming monthly bonus: ${e.message}")
            null
        }
    }

    /**
     * Update receipt count (lifetime total, used for the one-time RECEIPT_HUNTER badge).
     */
    suspend fun updateReceiptCount(userId: String, receiptCount: Int) {
        try {
            val stats = getStats(userId)
            val updated = stats.copy(receiptUploadCount = receiptCount)
            statsRef(userId).set(updated).await()
        } catch (e: Exception) {
            Log.e(TAG, "Error updating receipt count: ${e.message}")
        }
    }

    /**
     * Lifetime Receipt Hunter progress (5 total receipts → one-time badge).
     */
    suspend fun getReceiptHunterProgress(userId: String): Pair<Int, Boolean> {
        return try {
            val stats = getStats(userId)
            val progress = stats.receiptUploadCount.coerceAtMost(5)
            val completed = stats.receiptUploadCount >= 5

            if (completed && !stats.earnedBadges.contains("RECEIPT_HUNTER")) {
                val badges = stats.earnedBadges.toMutableList()
                badges.add("RECEIPT_HUNTER")
                val updated = stats.copy(earnedBadges = badges, receiptHunterCompleted = true)
                statsRef(userId).set(updated).await()
                updateUserTotalPoints(userId, 150)
                return Pair(5, true)
            }

            Pair(progress, completed)
        } catch (e: Exception) {
            Log.e(TAG, "Error getting receipt hunter progress: ${e.message}")
            Pair(0, false)
        }
    }

    // Weekly receipt challenge

    /**
     * Returns Triple(count, isCompleted, hasClaimed) for the current week.
     * Resets automatically when the week changes.
     */
    suspend fun getWeeklyReceiptProgress(userId: String): Triple<Int, Boolean, Boolean> {
        return try {
            val stats = getStats(userId)
            val currentWeekStart = getCurrentWeekStartMillis()

            val count = if (stats.lastWeekStart == currentWeekStart) stats.weeklyReceiptCount else 0
            val hasClaimed = if (stats.lastWeekStart == currentWeekStart) stats.weeklyReceiptClaimed else false
            Triple(count, count >= 5, hasClaimed)
        } catch (e: Exception) {
            Log.e(TAG, "Error getting weekly receipt progress: ${e.message}")
            Triple(0, false, false)
        }
    }

    /**
     * Claim the weekly receipt reward (5 receipts uploaded this week).
     * Returns the badge key on success, null on failure.
     */
    suspend fun claimWeeklyReceiptBonus(userId: String): String? {
        return try {
            val stats = getStats(userId)
            val currentWeekStart = getCurrentWeekStartMillis()

            val weeklyCount = if (stats.lastWeekStart == currentWeekStart) stats.weeklyReceiptCount else 0
            val hasClaimed = if (stats.lastWeekStart == currentWeekStart) stats.weeklyReceiptClaimed else false

            if (weeklyCount < 5 || hasClaimed) return null

            val weekBadge = "RECEIPT_WEEKLY_${formatWeekYear(currentWeekStart)}"
            val badges = stats.earnedBadges.toMutableList()
            if (!badges.contains(weekBadge)) badges.add(weekBadge)

            val updated = stats.copy(
                earnedBadges = badges,
                weeklyReceiptClaimed = true,
                lastWeekStart = currentWeekStart
            )
            statsRef(userId).set(updated).await()
            updateUserTotalPoints(userId, 75)
            weekBadge
        } catch (e: Exception) {
            Log.e(TAG, "Error claiming weekly receipt bonus: ${e.message}")
            null
        }
    }

    // Monthly receipt challenge

    /**
     * Returns Triple(count, isCompleted, hasClaimed) for the current month.
     * Resets automatically when the month changes.
     */
    suspend fun getMonthlyReceiptProgress(userId: String): Triple<Int, Boolean, Boolean> {
        return try {
            val stats = getStats(userId)
            val currentMonthStart = getCurrentMonthStartMillis()

            val count = if (stats.lastMonthlyReceiptStart == currentMonthStart) stats.monthlyReceiptCount else 0
            val hasClaimed = if (stats.lastMonthlyReceiptStart == currentMonthStart) stats.monthlyReceiptClaimed else false
            Triple(count, count >= 10, hasClaimed)
        } catch (e: Exception) {
            Log.e(TAG, "Error getting monthly receipt progress: ${e.message}")
            Triple(0, false, false)
        }
    }

    /**
     * Claim the monthly receipt reward (10 receipts uploaded this month).
     * Returns the badge key on success, null on failure.
     */
    suspend fun claimMonthlyReceiptBonus(userId: String): String? {
        return try {
            val stats = getStats(userId)
            val currentMonthStart = getCurrentMonthStartMillis()

            val monthlyCount = if (stats.lastMonthlyReceiptStart == currentMonthStart) stats.monthlyReceiptCount else 0
            val hasClaimed = if (stats.lastMonthlyReceiptStart == currentMonthStart) stats.monthlyReceiptClaimed else false

            if (monthlyCount < 10 || hasClaimed) return null

            val monthBadge = "RECEIPT_MONTHLY_${formatMonthYear(currentMonthStart)}"
            val badges = stats.earnedBadges.toMutableList()
            if (!badges.contains(monthBadge)) badges.add(monthBadge)

            val updated = stats.copy(
                earnedBadges = badges,
                monthlyReceiptClaimed = true,
                lastMonthlyReceiptStart = currentMonthStart
            )
            statsRef(userId).set(updated).await()
            updateUserTotalPoints(userId, 150)
            monthBadge
        } catch (e: Exception) {
            Log.e(TAG, "Error claiming monthly receipt bonus: ${e.message}")
            null
        }
    }

    // Core update after expense

    /**
     * Called every time a new expense is saved.
     * Updates streak, receipt counts, and auto-awards streak/first-expense badges.
     * Returns list of newly unlocked badge keys (empty if none).
     *
     * Monthly Early Bird and receipt challenges are claimed manually via their buttons.
     */
    suspend fun updateAfterExpense(userId: String, hasReceipt: Boolean): List<String> {
        return try {
            val stats = getStats(userId)

            val today = DateTimeUtils.startOfDayMillis(System.currentTimeMillis())
            val lastLogDay = DateTimeUtils.startOfDayMillis(stats.lastLogDate)

            // ── Streak ──
            var streak = stats.currentStreak
            if (lastLogDay != today) {
                val daysDifference = getDaysDifference(lastLogDay, today)
                streak = when {
                    daysDifference == 1 -> streak + 1
                    else -> 1
                }
            }
            val best = maxOf(streak, stats.bestStreak)

            // ── Lifetime receipt count ──
            var receiptCount = stats.receiptUploadCount
            if (hasReceipt) receiptCount++

            // ── Weekly receipt count (reset if new week) ──
            val currentWeekStart = getCurrentWeekStartMillis()
            var weeklyReceiptCount = if (stats.lastWeekStart == currentWeekStart) stats.weeklyReceiptCount else 0
            val weeklyReceiptClaimed = if (stats.lastWeekStart == currentWeekStart) stats.weeklyReceiptClaimed else false
            if (hasReceipt) weeklyReceiptCount++

            // ── Monthly receipt count (reset if new month) ──
            val currentMonthStart = getCurrentMonthStartMillis()
            var monthlyReceiptCount = if (stats.lastMonthlyReceiptStart == currentMonthStart) stats.monthlyReceiptCount else 0
            val monthlyReceiptClaimed = if (stats.lastMonthlyReceiptStart == currentMonthStart) stats.monthlyReceiptClaimed else false
            if (hasReceipt) monthlyReceiptCount++

            // ── Badge unlocks ──
            val badges = stats.earnedBadges.toMutableList()
            val newBadges = mutableListOf<String>()
            var pointsToAward = 0

            fun checkUnlock(badge: String, condition: Boolean, points: Int) {
                if (condition && !badges.contains(badge)) {
                    badges.add(badge)
                    newBadges.add(badge)
                    pointsToAward += points
                }
            }

            checkUnlock("FIRST_EXPENSE", stats.earnedBadges.isEmpty(), 50)
            checkUnlock("STREAK_STARTER", streak >= 3, 100)
            checkUnlock("STREAK_MASTER", streak >= 7, 200)
            checkUnlock("BUDGET_CHAMPION", streak >= 30, 500)
            checkUnlock("RECEIPT_HUNTER", receiptCount >= 5, 150)

            val updated = stats.copy(
                currentStreak = streak,
                bestStreak = best,
                lastLogDate = today,
                earnedBadges = badges,
                receiptUploadCount = receiptCount,
                receiptHunterCompleted = receiptCount >= 5,
                weeklyReceiptCount = weeklyReceiptCount,
                lastWeekStart = currentWeekStart,
                weeklyReceiptClaimed = weeklyReceiptClaimed,
                monthlyReceiptCount = monthlyReceiptCount,
                lastMonthlyReceiptStart = currentMonthStart,
                monthlyReceiptClaimed = monthlyReceiptClaimed
            )

            statsRef(userId).set(updated).await()

            if (pointsToAward > 0) {
                updateUserTotalPoints(userId, pointsToAward)
            }

            newBadges
        } catch (e: Exception) {
            Log.e(TAG, "Error updating after expense: ${e.message}")
            emptyList()
        }
    }

    // Points & level

    suspend fun getTotalPoints(userId: String): Int {
        return withContext(Dispatchers.IO) {
            try {
                var totalPoints = 0

                val badges = getEarnedBadges(userId)
                badges.forEach { badgeKey ->
                    totalPoints += BadgeIconMapper.getBadgePoints(badgeKey)
                }

                // Streak days also contribute 10 pts each (continuous bonus)
                val stats = getStats(userId)
                totalPoints += (stats.currentStreak * 10)

                Log.d(TAG, "Total points for $userId: $totalPoints")
                totalPoints
            } catch (e: Exception) {
                Log.e(TAG, "Error calculating total points: ${e.message}")
                0
            }
        }
    }

    suspend fun getEarnedBadges(userId: String): List<String> {
        return withContext(Dispatchers.IO) {
            try {
                val stats = getStats(userId)
                stats.earnedBadges.toList()
            } catch (e: Exception) {
                Log.e(TAG, "Error loading badges: ${e.message}")
                emptyList()
            }
        }
    }

    suspend fun getDetailedBadges(userId: String): List<DetailedBadge> {
        return withContext(Dispatchers.IO) {
            try {
                val stats = getStats(userId)
                stats.earnedBadges.map { badgeKey ->
                    DetailedBadge(
                        badgeKey = badgeKey,
                        name = BadgeIconMapper.getBadgeDisplayName(badgeKey),
                        description = getBadgeDescription(badgeKey),
                        points = BadgeIconMapper.getBadgePoints(badgeKey),
                        dateEarned = stats.lastLogDate,
                        iconResId = BadgeIconMapper.getIconForBadge(badgeKey)
                    )
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error loading detailed badges: ${e.message}")
                emptyList()
            }
        }
    }

    private suspend fun updateUserTotalPoints(userId: String, pointsToAdd: Int) {
        try {
            if (pointsToAdd <= 0) return
            val userRef = db.collection("users").document(userId)
            db.runTransaction { transaction ->
                val snapshot = transaction.get(userRef)
                val currentPoints = snapshot.getLong("totalPoints") ?: 0
                transaction.update(userRef, "totalPoints", currentPoints + pointsToAdd)
                null
            }.await()
            Log.d(TAG, "Added $pointsToAdd points to $userId")
        } catch (e: Exception) {
            Log.e(TAG, "Error updating total points: ${e.message}")
        }
    }

    suspend fun getUserLevel(userId: String): Int = calculateLevel(getTotalPoints(userId))

    fun calculateLevel(points: Int): Int = when {
        points < 500 -> 1
        points < 1500 -> 2
        points < 3000 -> 3
        points < 5000 -> 4
        points < 7500 -> 5
        else -> 5 + ((points - 7500) / 2500)
    }

    fun getPointsForNextLevel(currentPoints: Int): Int {
        val level = calculateLevel(currentPoints)
        return getPointsForLevel(level + 1) - getPointsForLevel(level)
    }

    private fun getPointsForLevel(level: Int): Int = when (level) {
        1 -> 0
        2 -> 500
        3 -> 1500
        4 -> 3000
        5 -> 5000
        else -> 5000 + ((level - 5) * 2500)
    }

    fun getLevelProgress(currentPoints: Int): Int {
        val level = calculateLevel(currentPoints)
        val floor = getPointsForLevel(level)
        val ceiling = getPointsForLevel(level + 1)
        val needed = ceiling - floor
        return if (needed > 0) ((currentPoints - floor).toFloat() / needed * 100).toInt() else 100
    }

    //Badge helpers

    private fun getBadgeDescription(badgeKey: String): String = when {
        badgeKey == "FIRST_EXPENSE" -> "Tracked your first expense"
        badgeKey == "STREAK_STARTER" -> "Logged expenses for 3 days in a row"
        badgeKey == "STREAK_MASTER" -> "Achieved a 7-day logging streak"
        badgeKey == "BUDGET_CHAMPION" -> "Maintained budget for 30 days"
        badgeKey == "RECEIPT_HUNTER" -> "Uploaded 5 receipts (lifetime)"
        badgeKey.startsWith("EARLY_BIRD") -> "Logged an expense on the 1st of the month"
        badgeKey.startsWith("RECEIPT_WEEKLY") -> "Uploaded 5 receipts in one week"
        badgeKey.startsWith("RECEIPT_MONTHLY") -> "Uploaded 10 receipts in one month"
        badgeKey == "WISHLIST_ACHIEVER" -> "Saved enough for a wishlist item"
        else -> "Awesome achievement unlocked!"
    }

    suspend fun getNewlyEarnedBadges(userId: String, previousBadges: Set<String>): List<String> {
        return withContext(Dispatchers.IO) {
            try {
                val current = getEarnedBadges(userId).toSet()
                (current - previousBadges).toList()
            } catch (e: Exception) {
                emptyList()
            }
        }
    }

    /**
     * Awards the one-time WISHLIST_ACHIEVER badge when the user has completed at least one
     * savings goal. Returns the badge key on first award, null if already earned or not eligible.
     */
    suspend fun checkAndAwardWishlistBadge(userId: String, completedGoalsCount: Int): String? {
        if (completedGoalsCount < 1) return null
        return try {
            val stats = getStats(userId)
            if (stats.earnedBadges.contains("WISHLIST_ACHIEVER")) return null

            val badges = stats.earnedBadges.toMutableList()
            badges.add("WISHLIST_ACHIEVER")
            statsRef(userId).set(stats.copy(earnedBadges = badges)).await()
            updateUserTotalPoints(userId, 200)
            "WISHLIST_ACHIEVER"
        } catch (e: Exception) {
            Log.e(TAG, "Error awarding wishlist badge: ${e.message}")
            null
        }
    }

    suspend fun checkAndAwardBadges(userId: String): List<String> {
        return withContext(Dispatchers.IO) {
            val oldBadges = getEarnedBadges(userId).toSet()
            val stats = getStats(userId)
            val badges = stats.earnedBadges.toMutableList()
            var updated = false
            var points = 0

            fun tryAdd(badge: String, condition: Boolean, pts: Int) {
                if (condition && !badges.contains(badge)) {
                    badges.add(badge); points += pts; updated = true
                }
            }

            tryAdd("STREAK_STARTER", stats.currentStreak >= 3, 100)
            tryAdd("STREAK_MASTER", stats.currentStreak >= 7, 200)
            tryAdd("BUDGET_CHAMPION", stats.currentStreak >= 30, 500)
            tryAdd("RECEIPT_HUNTER", stats.receiptUploadCount >= 5, 150)

            if (updated) {
                statsRef(userId).set(stats.copy(earnedBadges = badges)).await()
                if (points > 0) updateUserTotalPoints(userId, points)
            }

            (getEarnedBadges(userId).toSet() - oldBadges).toList()
        }
    }

    //Date utilities

    private fun getDaysDifference(fromDate: Long, toDate: Long): Int {
        val cal1 = Calendar.getInstance().apply {
            timeInMillis = fromDate
            set(Calendar.HOUR_OF_DAY, 0); set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0); set(Calendar.MILLISECOND, 0)
        }
        val cal2 = Calendar.getInstance().apply {
            timeInMillis = toDate
            set(Calendar.HOUR_OF_DAY, 0); set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0); set(Calendar.MILLISECOND, 0)
        }
        return ((cal2.timeInMillis - cal1.timeInMillis) / 86400000L).toInt()
    }

    private fun getCurrentMonthStartMillis(): Long {
        val cal = Calendar.getInstance()
        cal.set(Calendar.DAY_OF_MONTH, 1)
        cal.set(Calendar.HOUR_OF_DAY, 0); cal.set(Calendar.MINUTE, 0)
        cal.set(Calendar.SECOND, 0); cal.set(Calendar.MILLISECOND, 0)
        return cal.timeInMillis
    }

    private fun getCurrentWeekStartMillis(): Long {
        val cal = Calendar.getInstance()
        val dayOfWeek = cal.get(Calendar.DAY_OF_WEEK)
        // Treat Monday as week start (ISO 8601)
        val daysFromMonday = when (dayOfWeek) {
            Calendar.MONDAY -> 0
            Calendar.TUESDAY -> 1
            Calendar.WEDNESDAY -> 2
            Calendar.THURSDAY -> 3
            Calendar.FRIDAY -> 4
            Calendar.SATURDAY -> 5
            Calendar.SUNDAY -> 6
            else -> 0
        }
        cal.add(Calendar.DAY_OF_YEAR, -daysFromMonday)
        cal.set(Calendar.HOUR_OF_DAY, 0); cal.set(Calendar.MINUTE, 0)
        cal.set(Calendar.SECOND, 0); cal.set(Calendar.MILLISECOND, 0)
        return cal.timeInMillis
    }

    private fun isFirstDayOfMonth(timestamp: Long): Boolean {
        val cal = Calendar.getInstance().apply { timeInMillis = timestamp }
        return cal.get(Calendar.DAY_OF_MONTH) == 1
    }

    private fun formatMonthYear(timestamp: Long): String {
        val cal = Calendar.getInstance().apply { timeInMillis = timestamp }
        return "${cal.get(Calendar.MONTH) + 1}_${cal.get(Calendar.YEAR)}"
    }

    private fun formatWeekYear(timestamp: Long): String {
        val cal = Calendar.getInstance().apply { timeInMillis = timestamp }
        return "${cal.get(Calendar.WEEK_OF_YEAR)}_${cal.get(Calendar.YEAR)}"
    }
}

data class DetailedBadge(
    val badgeKey: String,
    val name: String,
    val description: String,
    val points: Int,
    val dateEarned: Long,
    val iconResId: Int
)
