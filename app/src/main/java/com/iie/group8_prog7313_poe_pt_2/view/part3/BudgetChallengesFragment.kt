package com.iie.group8_prog7313_poe_pt_2.view.part3

import android.content.res.ColorStateList
import android.os.Bundle
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.snackbar.Snackbar
import com.iie.group8_prog7313_poe_pt_2.R
import com.iie.group8_prog7313_poe_pt_2.databinding.FragmentBudgetChallengesBinding
import com.iie.group8_prog7313_poe_pt_2.model.Badge
import com.iie.group8_prog7313_poe_pt_2.model.entity.SavingsGoal
import com.iie.group8_prog7313_poe_pt_2.model.repository.GamificationRepository
import com.iie.group8_prog7313_poe_pt_2.model.repository.SavingsGoalRepository
import com.iie.group8_prog7313_poe_pt_2.session.SessionManager
import com.iie.group8_prog7313_poe_pt_2.util.BadgeIconMapper
import com.iie.group8_prog7313_poe_pt_2.util.DateTimeUtils
import com.iie.group8_prog7313_poe_pt_2.view.MainActivity
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.text.NumberFormat
import java.util.Locale

class BudgetChallengesFragment : Fragment() {

    private var _binding: FragmentBudgetChallengesBinding? = null
    private val binding get() = _binding!!

    private val repository = GamificationRepository()
    private val savingsGoalRepository = SavingsGoalRepository()
    private lateinit var badgeAdapter: BadgeAdapter
    private var totalPoints = 0

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentBudgetChallengesBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupRecyclerView()
        setupClickListeners()
        loadAllData()
    }

    private fun setupRecyclerView() {
        binding.rvEarnedBadges.layoutManager =
            LinearLayoutManager(requireContext(), LinearLayoutManager.HORIZONTAL, false)
        badgeAdapter = BadgeAdapter(emptyList()) { badge -> showBadgeDetailsDialog(badge) }
        binding.rvEarnedBadges.adapter = badgeAdapter
    }

    // ─── Click listeners ──────────────────────────────────────────────────────

    private fun setupClickListeners() {

        // Wishlist Achiever — claim badge or navigate to wishlist
        binding.btnWishlistAchiever.setOnClickListener {
            lifecycleScope.launch {
                val userId = userId() ?: return@launch
                val goals = savingsGoalRepository.getAllByUser(userId).first()
                val completedCount = goals.count { it.isCompleted }
                val hasBadge = repository.getEarnedBadges(userId).contains("WISHLIST_ACHIEVER")

                if (!hasBadge && completedCount > 0) {
                    val badgeKey = repository.checkAndAwardWishlistBadge(userId, completedCount)
                    if (badgeKey != null) {
                        postGlobalAward(badgeKey)
                        Snackbar.make(
                            binding.root,
                            "Wishlist Achiever Badge Earned! +200 pts",
                            Snackbar.LENGTH_LONG
                        ).show()
                        loadAllData()
                    }
                } else {
                    findNavController().navigate(R.id.wishlistFragment)
                }
            }
        }

        // Monthly Early Bird bonus
        binding.btnMonthlyStatus.setOnClickListener {
            lifecycleScope.launch {
                val userId = userId() ?: return@launch
                val (hasEarned, canClaim) = repository.checkMonthlyBonusStatus(userId)
                when {
                    canClaim -> {
                        val badgeKey = repository.claimMonthlyBonus(userId)
                        if (badgeKey != null) {
                            postGlobalAward(badgeKey)
                            Snackbar.make(binding.root, "Early Bird Bonus Claimed! +50 pts", Snackbar.LENGTH_LONG).show()
                            loadAllData()
                        }
                    }
                    !hasEarned -> Snackbar.make(
                        binding.root,
                        "Log an expense on the 1st of the month to unlock this bonus!",
                        Snackbar.LENGTH_LONG
                    ).show()
                }
            }
        }

        // Weekly receipt claim / navigate
        binding.btnReceiptHunter.setOnClickListener {
            lifecycleScope.launch {
                val userId = userId() ?: return@launch
                val (_, completed, claimed) = repository.getWeeklyReceiptProgress(userId)
                if (completed && !claimed) {
                    val badgeKey = repository.claimWeeklyReceiptBonus(userId)
                    if (badgeKey != null) {
                        postGlobalAward(badgeKey)
                        Snackbar.make(binding.root, "Weekly Receipt Reward Claimed! +75 pts", Snackbar.LENGTH_LONG).show()
                        loadAllData()
                    }
                } else {
                    findNavController().navigate(R.id.expenseListFragment)
                }
            }
        }

        // Monthly receipt claim / navigate
        binding.btnMonthlyReceipt.setOnClickListener {
            lifecycleScope.launch {
                val userId = userId() ?: return@launch
                val (_, completed, claimed) = repository.getMonthlyReceiptProgress(userId)
                if (completed && !claimed) {
                    val badgeKey = repository.claimMonthlyReceiptBonus(userId)
                    if (badgeKey != null) {
                        postGlobalAward(badgeKey)
                        Snackbar.make(binding.root, "Monthly Receipt Reward Claimed! +150 pts", Snackbar.LENGTH_LONG).show()
                        loadAllData()
                    }
                } else {
                    findNavController().navigate(R.id.expenseListFragment)
                }
            }
        }
    }

    // ─── Data loading ─────────────────────────────────────────────────────────

    private fun loadAllData() {
        lifecycleScope.launch {
            val userId = userId() ?: return@launch
            loadStreakData(userId)
            loadMonthlyBonusData(userId)
            loadWeeklyReceiptData(userId)
            loadMonthlyReceiptData(userId)
            loadWishlistData(userId)
            loadTotalPoints(userId)
        }
    }

    private suspend fun loadTotalPoints(userId: String) {
        totalPoints = repository.getTotalPoints(userId)
        updatePointsUI()
    }

    private fun updatePointsUI() {
        binding.tvTotalPoints.text = totalPoints.toString()
        val level = calculateLevel(totalPoints)
        binding.tvCurrentLevel.text = level.toString()

        val floor = getPointsForLevel(level)
        val ceiling = getPointsForLevel(level + 1)
        val needed = ceiling - floor
        val earned = totalPoints - floor
        val progress = if (needed > 0) (earned.toFloat() / needed * 100).toInt() else 100

        binding.progressToNextLevel.progress = progress
        binding.tvLevelProgress.text = "Level $level · $earned/$needed points to Level ${level + 1}"
        binding.tvBadgesCount.text = badgeAdapter.itemCount.toString()
    }

    // ─── Streak ───────────────────────────────────────────────────────────────

    private suspend fun loadStreakData(userId: String) {
        val stats = repository.getStats(userId)

        val today = DateTimeUtils.startOfDayMillis(System.currentTimeMillis())
        val lastLogDay = DateTimeUtils.startOfDayMillis(stats.lastLogDate)

        when {
            stats.lastLogDate == 0L -> {
                binding.btnStreakStatus.text = "Log first expense to start streak"
                (binding.btnStreakStatus as? com.google.android.material.button.MaterialButton)
                    ?.backgroundTintList = ContextCompat.getColorStateList(requireContext(), R.color.text_muted_light)
            }
            today - lastLogDay > 86400000L -> {
                binding.btnStreakStatus.text = "Inactive – Log expense to continue"
                (binding.btnStreakStatus as? com.google.android.material.button.MaterialButton)
                    ?.backgroundTintList = ContextCompat.getColorStateList(requireContext(), R.color.text_muted_light)
            }
            else -> {
                binding.btnStreakStatus.text = "Active"
                (binding.btnStreakStatus as? com.google.android.material.button.MaterialButton)
                    ?.backgroundTintList = ContextCompat.getColorStateList(requireContext(), R.color.accent_cyan)
            }
        }

        binding.tvCurrentStreak.text = stats.currentStreak.toString()
        binding.tvBestStreak.text = stats.bestStreak.toString()
        binding.tvStreakProgress.text = "${stats.currentStreak}/7 days"
        binding.progressStreak.max = 7
        binding.progressStreak.progress = minOf(stats.currentStreak, 7)

        val badgeObjects = stats.earnedBadges.map { badgeKey ->
            Badge(
                id = badgeKey,
                name = BadgeIconMapper.getBadgeDisplayName(badgeKey),
                description = getBadgeDescription(badgeKey),
                iconResId = BadgeIconMapper.getIconForBadge(badgeKey),
                points = BadgeIconMapper.getBadgePoints(badgeKey),
                dateEarned = stats.lastLogDate,
                category = BadgeIconMapper.getCategoryFromBadgeKey(badgeKey)
            )
        }
        badgeAdapter.updateBadges(badgeObjects)
    }

    // ─── Monthly Early Bird bonus ──────────────────────────────────────────────

    private suspend fun loadMonthlyBonusData(userId: String) {
        val (hasEarned, canClaim) = repository.checkMonthlyBonusStatus(userId)

        when {
            hasEarned -> {
                binding.btnMonthlyStatus.text = "Earned ✓"
                (binding.btnMonthlyStatus as? com.google.android.material.button.MaterialButton)
                    ?.backgroundTintList = ContextCompat.getColorStateList(requireContext(), R.color.accent_green_bright)
                binding.btnMonthlyStatus.isEnabled = false
            }
            canClaim -> {
                binding.btnMonthlyStatus.text = "Claim Bonus!"
                (binding.btnMonthlyStatus as? com.google.android.material.button.MaterialButton)
                    ?.backgroundTintList = ContextCompat.getColorStateList(requireContext(), R.color.accent_amber)
                binding.btnMonthlyStatus.isEnabled = true
            }
            else -> {
                binding.btnMonthlyStatus.text = "Not Earned"
                (binding.btnMonthlyStatus as? com.google.android.material.button.MaterialButton)
                    ?.backgroundTintList = ContextCompat.getColorStateList(requireContext(), R.color.text_muted_light)
                binding.btnMonthlyStatus.isEnabled = true
            }
        }
    }

    // ─── Weekly receipt challenge ─────────────────────────────────────────────

    private suspend fun loadWeeklyReceiptData(userId: String) {
        val (count, completed, claimed) = repository.getWeeklyReceiptProgress(userId)

        try {
            binding.progressReceiptHunter.max = 5
            binding.progressReceiptHunter.progress = minOf(count, 5)
            binding.tvReceiptProgress.text = "${minOf(count, 5)}/5 receipts this week"

            when {
                claimed -> {
                    binding.btnReceiptHunter.text = getString(R.string.claimed_btn)
                    (binding.btnReceiptHunter as? com.google.android.material.button.MaterialButton)
                        ?.backgroundTintList = ContextCompat.getColorStateList(requireContext(), R.color.accent_green_bright)
                    binding.btnReceiptHunter.isEnabled = false
                }
                completed -> {
                    binding.btnReceiptHunter.text = getString(R.string.claim_reward_btn)
                    (binding.btnReceiptHunter as? com.google.android.material.button.MaterialButton)
                        ?.backgroundTintList = ContextCompat.getColorStateList(requireContext(), R.color.accent_amber)
                    binding.btnReceiptHunter.isEnabled = true
                }
                else -> {
                    binding.btnReceiptHunter.text = "Upload Receipts ($count/5)"
                    (binding.btnReceiptHunter as? com.google.android.material.button.MaterialButton)
                        ?.backgroundTintList = ContextCompat.getColorStateList(requireContext(), R.color.accent_pink)
                    binding.btnReceiptHunter.isEnabled = true
                }
            }
        } catch (e: Exception) {
            android.util.Log.e("BudgetChallenges", "Weekly receipt progress bar error", e)
        }
    }

    // ─── Monthly receipt challenge ────────────────────────────────────────────

    private suspend fun loadMonthlyReceiptData(userId: String) {
        val (count, completed, claimed) = repository.getMonthlyReceiptProgress(userId)

        try {
            binding.progressMonthlyReceipt.max = 10
            binding.progressMonthlyReceipt.progress = minOf(count, 10)
            binding.tvMonthlyReceiptProgress.text = "${minOf(count, 10)}/10 receipts this month"

            when {
                claimed -> {
                    binding.btnMonthlyReceipt.text = getString(R.string.claimed_btn)
                    (binding.btnMonthlyReceipt as? com.google.android.material.button.MaterialButton)
                        ?.backgroundTintList = ContextCompat.getColorStateList(requireContext(), R.color.accent_green_bright)
                    binding.btnMonthlyReceipt.isEnabled = false
                }
                completed -> {
                    binding.btnMonthlyReceipt.text = getString(R.string.claim_reward_btn)
                    (binding.btnMonthlyReceipt as? com.google.android.material.button.MaterialButton)
                        ?.backgroundTintList = ContextCompat.getColorStateList(requireContext(), R.color.accent_amber)
                    binding.btnMonthlyReceipt.isEnabled = true
                }
                else -> {
                    binding.btnMonthlyReceipt.text = "Upload Receipts ($count/10)"
                    (binding.btnMonthlyReceipt as? com.google.android.material.button.MaterialButton)
                        ?.backgroundTintList = ContextCompat.getColorStateList(requireContext(), R.color.accent_green_bright)
                    binding.btnMonthlyReceipt.isEnabled = true
                }
            }
        } catch (e: Exception) {
            android.util.Log.e("BudgetChallenges", "Monthly receipt progress bar error", e)
        }
    }

    // ─── Wishlist Achiever challenge ──────────────────────────────────────────

    private suspend fun loadWishlistData(userId: String) {
        val goals = savingsGoalRepository.getAllByUser(userId).first()
        val completedCount = goals.count { it.isCompleted }
        val totalCount = goals.size
        val hasBadge = repository.getEarnedBadges(userId).contains("WISHLIST_ACHIEVER")

        // Summary text and overall progress
        binding.tvWishlistProgress.text = when {
            totalCount == 0 -> "No savings goals yet — add some in the Wishlist tab"
            completedCount == 0 -> "0 / $totalCount goals completed"
            else -> "$completedCount / $totalCount goal${if (totalCount == 1) "" else "s"} completed"
        }
        binding.progressWishlist.max = totalCount.coerceAtLeast(1)
        binding.progressWishlist.progress = completedCount

        // Render individual goal rows
        binding.llWishlistGoals.removeAllViews()
        if (goals.isNotEmpty()) {
            binding.llWishlistGoals.visibility = View.VISIBLE
            goals.forEach { goal ->
                binding.llWishlistGoals.addView(createGoalRow(goal))
            }
        } else {
            binding.llWishlistGoals.visibility = View.GONE
        }

        // Button state
        when {
            hasBadge -> {
                binding.btnWishlistAchiever.text = "Earned ✓"
                (binding.btnWishlistAchiever as? com.google.android.material.button.MaterialButton)
                    ?.backgroundTintList = ContextCompat.getColorStateList(requireContext(), R.color.accent_green_bright)
                binding.btnWishlistAchiever.isEnabled = true  // still navigates to wishlist
            }
            completedCount > 0 -> {
                binding.btnWishlistAchiever.text = getString(R.string.claim_reward_btn)
                (binding.btnWishlistAchiever as? com.google.android.material.button.MaterialButton)
                    ?.backgroundTintList = ContextCompat.getColorStateList(requireContext(), R.color.accent_amber)
                binding.btnWishlistAchiever.isEnabled = true
            }
            else -> {
                binding.btnWishlistAchiever.text = getString(R.string.view_wishlist_btn)
                (binding.btnWishlistAchiever as? com.google.android.material.button.MaterialButton)
                    ?.backgroundTintList = ContextCompat.getColorStateList(requireContext(), R.color.accent_purple_light)
                binding.btnWishlistAchiever.isEnabled = true
            }
        }
    }

    /** Builds a compact goal row showing name, amount, progress bar, and status. */
    private fun createGoalRow(goal: SavingsGoal): View {
        val ctx = requireContext()
        val fmt = NumberFormat.getCurrencyInstance(Locale("en", "ZA"))
        val dp4 = (4 * resources.displayMetrics.density).toInt()
        val dp8 = (8 * resources.displayMetrics.density).toInt()
        val dp12 = (12 * resources.displayMetrics.density).toInt()

        val root = LinearLayout(ctx).apply {
            orientation = LinearLayout.VERTICAL
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply { bottomMargin = dp12 }
        }

        // Name + amount row
        val nameRow = LinearLayout(ctx).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
        }

        val goalName = TextView(ctx).apply {
            text = goal.name
            textSize = 14f
            setTextColor(ContextCompat.getColor(ctx, R.color.brand))
            setTypeface(null, android.graphics.Typeface.BOLD)
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
        }

        val goalAmount = TextView(ctx).apply {
            text = "${fmt.format(goal.savedAmount)} / ${fmt.format(goal.targetAmount)}"
            textSize = 12f
            setTextColor(ContextCompat.getColor(ctx, R.color.text_muted_light))
        }

        nameRow.addView(goalName)
        nameRow.addView(goalAmount)

        // Progress bar
        val progressColor = if (goal.isCompleted) R.color.accent_green_bright else R.color.accent_purple_light
        val progressBar = ProgressBar(ctx, null, android.R.attr.progressBarStyleHorizontal).apply {
            max = 100
            progress = goal.progressPercent
            progressTintList = ColorStateList.valueOf(ContextCompat.getColor(ctx, progressColor))
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, dp8
            ).apply { topMargin = dp4 }
        }

        // Status label
        val statusLabel = TextView(ctx).apply {
            text = if (goal.isCompleted) "✓ Completed!" else "${goal.progressPercent}% saved"
            textSize = 12f
            setTextColor(
                ContextCompat.getColor(ctx,
                    if (goal.isCompleted) R.color.accent_green_bright else R.color.text_muted_light)
            )
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply { topMargin = dp4 }
        }

        root.addView(nameRow)
        root.addView(progressBar)
        root.addView(statusLabel)
        return root
    }

    // ─── Helpers ──────────────────────────────────────────────────────────────

    private fun userId(): String? = SessionManager(requireContext()).getUserId()

    private fun postGlobalAward(badgeKey: String) {
        (activity as? MainActivity)?.sharedAwardViewModel?.postAward(badgeKey)
    }

    private fun getBadgeDescription(badgeKey: String): String = when {
        badgeKey == "FIRST_EXPENSE" -> "Tracked your first expense"
        badgeKey == "STREAK_STARTER" -> "Logged expenses for 3 days in a row"
        badgeKey == "STREAK_MASTER" -> "Achieved a 7-day logging streak"
        badgeKey == "BUDGET_CHAMPION" -> "Maintained budget for 30 days"
        badgeKey == "RECEIPT_HUNTER" -> "Uploaded 5 receipts (lifetime)"
        badgeKey.startsWith("EARLY_BIRD") -> "Logged an expense on the 1st of the month"
        badgeKey.startsWith("RECEIPT_WEEKLY") -> "Uploaded 5 receipts in one week"
        badgeKey.startsWith("RECEIPT_MONTHLY") -> "Uploaded 10 receipts in one month"
        badgeKey == "WISHLIST_ACHIEVER" -> "Fully saved up for a wishlist goal"
        else -> "Awesome achievement unlocked!"
    }

    private fun showBadgeDetailsDialog(badge: Badge) {
        MaterialAlertDialogBuilder(requireContext())
            .setTitle(badge.name)
            .setMessage("${badge.description}\n\nPoints: +${badge.points}\nEarned: ${formatDate(badge.dateEarned)}")
            .setPositiveButton("Awesome!") { dialog, _ -> dialog.dismiss() }
            .show()
    }

    private fun formatDate(timestamp: Long): String {
        val date = java.util.Date(timestamp)
        val fmt = java.text.SimpleDateFormat("MMM dd, yyyy", java.util.Locale.getDefault())
        return fmt.format(date)
    }

    private fun calculateLevel(points: Int): Int = when {
        points < 500 -> 1
        points < 1500 -> 2
        points < 3000 -> 3
        points < 5000 -> 4
        points < 7500 -> 5
        else -> 5 + ((points - 7500) / 2500)
    }

    private fun getPointsForLevel(level: Int): Int = when (level) {
        1 -> 0
        2 -> 500
        3 -> 1500
        4 -> 3000
        5 -> 5000
        else -> 5000 + ((level - 5) * 2500)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
