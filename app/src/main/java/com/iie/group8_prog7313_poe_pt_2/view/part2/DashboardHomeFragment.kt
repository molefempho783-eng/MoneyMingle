package com.iie.group8_prog7313_poe_pt_2.view.part2

import android.content.res.ColorStateList
import android.util.Log
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
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
import com.google.android.material.card.MaterialCardView
import com.iie.group8_prog7313_poe_pt_2.R
import com.iie.group8_prog7313_poe_pt_2.model.repository.CategoryRepository
import com.iie.group8_prog7313_poe_pt_2.model.repository.ExpenseRepository
import com.iie.group8_prog7313_poe_pt_2.model.repository.GamificationRepository
import com.iie.group8_prog7313_poe_pt_2.session.SessionManager
import kotlinx.coroutines.launch
import java.text.NumberFormat
import java.util.*

class DashboardHomeFragment : Fragment() {

    private lateinit var sessionManager: SessionManager
    private var userId: String = ""

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_dashboard_home, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        sessionManager = SessionManager(requireContext())
        userId = sessionManager.getUserId() ?: ""

        setupQuickLinks()
        setupStreakCard()
        loadBudgetData()
        loadCategories()
        loadStreakData()
    }

    private fun setupQuickLinks() {
        view?.findViewById<MaterialCardView>(R.id.cardCategories)?.setOnClickListener {
            findNavController().navigate(
                DashboardHomeFragmentDirections.actionDashboardHomeFragmentToCategoriesFragment()
            )
        }
        view?.findViewById<MaterialCardView>(R.id.cardWishlist)?.setOnClickListener {
            findNavController().navigate(
                DashboardHomeFragmentDirections.actionDashboardHomeFragmentToWishlistFragment()
            )
        }
        view?.findViewById<MaterialCardView>(R.id.cardSubscriptions)?.setOnClickListener {
            findNavController().navigate(
                DashboardHomeFragmentDirections.actionDashboardHomeFragmentToSubscriptionManagerFragment()
            )
        }
    }

    private fun setupStreakCard() {
        view?.findViewById<MaterialCardView>(R.id.cardStreak)?.setOnClickListener {
            findNavController().navigate(
                DashboardHomeFragmentDirections.actionDashboardHomeFragmentToExpenseListFragment()
            )
        }
    }

    private fun loadStreakData() {
        if (userId.isEmpty()) return
        val gamificationRepo = GamificationRepository()
        lifecycleScope.launch {
            try {
                val stats = gamificationRepo.getStats(userId)
                val streak = stats.currentStreak
                val streakLabel = when (streak) {
                    0 -> "0 Day Streak"
                    1 -> "1 Day Streak"
                    else -> "$streak Day Streak"
                }
                val streakInfo = when {
                    streak == 0 -> "Log an expense to start your streak"
                    streak < 3  -> "Keep going! $streak day${if (streak == 1) "" else "s"} and counting"
                    streak < 7  -> "Great work! Log daily to reach 7 days"
                    streak < 30 -> "On fire! $streak days and climbing"
                    else        -> "Incredible – $streak day streak!"
                }
                view?.findViewById<TextView>(R.id.tvDashboardStreak)?.text = streakLabel
                view?.findViewById<TextView>(R.id.tvDashboardStreakInfo)?.text = streakInfo
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    private fun loadBudgetData() {
        if (userId.isEmpty()) return

        val categoryRepo = CategoryRepository()
        val expenseRepo = ExpenseRepository()

        lifecycleScope.launch {
            try {
                val calendar = Calendar.getInstance()
                val currentMonth = calendar.get(Calendar.MONTH) + 1
                val currentYear = calendar.get(Calendar.YEAR)

                categoryRepo.getBudgetGoalsByMonth(userId, currentMonth, currentYear).collect { goals ->
                    val totalBudget = goals.sumOf { it.maxAmount }

                    val startDate = getMonthStartDate(currentYear, currentMonth)
                    val endDate = getMonthEndDate(currentYear, currentMonth)
                    val expenses = expenseRepo.getByUserAndDateRange(userId, startDate, endDate)

                    val totalSpent = expenses.sumOf { it.amount }
                    val remaining = totalBudget - totalSpent
                    val percentage = if (totalBudget > 0) (totalSpent / totalBudget) * 100 else 0.0

                    updateBudgetUI(totalBudget, totalSpent, remaining, percentage)
                }
            } catch (e: Exception) {
                e.printStackTrace()
                updateBudgetUI(0.0, 0.0, 0.0, 0.0)
            }
        }
    }

    private fun updateBudgetUI(totalBudget: Double, totalSpent: Double, remaining: Double, percentage: Double) {
        val formatter = NumberFormat.getCurrencyInstance(Locale("en", "ZA"))

        view?.findViewById<TextView>(R.id.monthlyBudgetNumber)?.text = formatter.format(totalBudget)
        view?.findViewById<TextView>(R.id.textSpentRemaining)?.text =
            "${formatter.format(totalSpent)} spent · ${formatter.format(remaining)} remaining"

        val progressBar = view?.findViewById<ProgressBar>(R.id.progressBarDashBoard)
        progressBar?.progress = percentage.toInt()

        val progressColor = when {
            percentage < 50 -> ContextCompat.getColor(requireContext(), R.color.success_light)
            percentage < 80 -> ContextCompat.getColor(requireContext(), R.color.warning_light)
            else -> ContextCompat.getColor(requireContext(), R.color.danger_light)
        }

        progressBar?.progressTintList = ColorStateList.valueOf(progressColor)
        Log.d("DashboardHomeFragment", "Budget loaded: budget=$totalBudget, spent=$totalSpent, ${percentage.toInt()}% used")

        view?.findViewById<TextView>(R.id.textBudgetUsed)?.text = "${percentage.toInt()}% of budget used"
    }

    private fun loadCategories() {
        if (userId.isEmpty()) return

        val categoryRepo = CategoryRepository()
        val expenseRepo = ExpenseRepository()

        lifecycleScope.launch {
            try {
                val categoriesContainer = view?.findViewById<LinearLayout>(R.id.categoriesContainer)

                categoryRepo.getAllCategoriesByUser(userId).collect { categoryList ->
                    categoriesContainer?.removeAllViews()

                    if (categoryList.isNotEmpty()) {
                        view?.findViewById<TextView>(R.id.textCategoryProgress)?.visibility = View.VISIBLE

                        val calendar = Calendar.getInstance()
                        val currentMonth = calendar.get(Calendar.MONTH) + 1
                        val currentYear = calendar.get(Calendar.YEAR)
                        val startDate = getMonthStartDate(currentYear, currentMonth)
                        val endDate = getMonthEndDate(currentYear, currentMonth)

                        val allExpenses = expenseRepo.getByUserAndDateRange(userId, startDate, endDate)

                        for (category in categoryList) {
                            val budgetGoal = categoryRepo.getBudgetGoalForCategory(
                                userId, category.id, currentMonth, currentYear
                            )

                            if (budgetGoal != null) {
                                val spent = allExpenses
                                    .filter { it.categoryId == category.id }
                                    .sumOf { it.amount }
                                val budgetAmount = budgetGoal.maxAmount
                                val percentage = if (budgetAmount > 0) (spent / budgetAmount) * 100 else 0.0

                                val categoryCard = createCategoryCard(
                                    category.name, spent, budgetAmount, percentage, category.id
                                )
                                categoriesContainer?.addView(categoryCard)
                            }
                        }
                    } else {
                        view?.findViewById<TextView>(R.id.textCategoryProgress)?.visibility = View.GONE
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
                view?.findViewById<TextView>(R.id.textCategoryProgress)?.visibility = View.GONE
            }
        }
    }

    private fun createCategoryCard(
        categoryName: String,
        spent: Double,
        budget: Double,
        percentage: Double,
        categoryId: String
    ): MaterialCardView {
        val formatter = NumberFormat.getCurrencyInstance(Locale("en", "ZA"))

        val card = MaterialCardView(requireContext()).apply {
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                setMargins(16.dpToPx(), 0, 16.dpToPx(), 8.dpToPx())
            }
            radius = 12.dpToPx().toFloat()
            cardElevation = 0f
            isClickable = true
            isFocusable = true
            setOnClickListener {
                findNavController().navigate(
                    DashboardHomeFragmentDirections.actionDashboardHomeFragmentToExpenseListFragment()
                )
            }
        }

        val contentLayout = LinearLayout(requireContext()).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(16.dpToPx(), 16.dpToPx(), 16.dpToPx(), 16.dpToPx())
        }

        val titleRow = LinearLayout(requireContext()).apply {
            orientation = LinearLayout.HORIZONTAL
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
        }

        val nameText = TextView(requireContext()).apply {
            text = categoryName
            setTextColor(ContextCompat.getColor(requireContext(), R.color.text_muted_dark))
            textSize = 16f
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
        }

        val amountText = TextView(requireContext()).apply {
            text = "${formatter.format(spent)} / ${formatter.format(budget)}"
            setTextColor(ContextCompat.getColor(requireContext(), R.color.text_muted_dark))
            textSize = 14f
        }

        titleRow.addView(nameText)
        titleRow.addView(amountText)

        val progressLayout = LinearLayout(requireContext()).apply {
            orientation = LinearLayout.HORIZONTAL
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply { topMargin = 8.dpToPx() }
        }

        val progressFill = View(requireContext()).apply {
            val fillWeight = percentage.toFloat().coerceIn(0f, 100f)
            layoutParams = LinearLayout.LayoutParams(0, 6.dpToPx(), fillWeight)
            background = getProgressDrawable(percentage)
        }

        val progressEmpty = View(requireContext()).apply {
            val emptyWeight = (100 - percentage).toFloat().coerceIn(0f, 100f)
            layoutParams = LinearLayout.LayoutParams(0, 6.dpToPx(), emptyWeight)
            background = GradientDrawable().apply {
                setColor(0xA6FFFFFF.toInt())
                cornerRadius = 3f
            }
        }

        progressLayout.addView(progressFill)
        progressLayout.addView(progressEmpty)
        contentLayout.addView(titleRow)
        contentLayout.addView(progressLayout)
        card.addView(contentLayout)

        return card
    }

    private fun getProgressDrawable(percentage: Double): GradientDrawable {
        // Colour matches the "Actual spend" key in the Spending Graph legend
        val color = ContextCompat.getColor(requireContext(), R.color.accent_purple_light)
        return GradientDrawable().apply {
            setColor(color)
            cornerRadius = 3f
        }
    }

    private fun getMonthStartDate(year: Int, month: Int): Long {
        val calendar = Calendar.getInstance()
        calendar.set(year, month - 1, 1, 0, 0, 0)
        calendar.set(Calendar.MILLISECOND, 0)
        return calendar.timeInMillis
    }

    private fun getMonthEndDate(year: Int, month: Int): Long {
        val calendar = Calendar.getInstance()
        calendar.set(year, month - 1, 1, 0, 0, 0)
        calendar.set(Calendar.MILLISECOND, 0)
        calendar.set(Calendar.DAY_OF_MONTH, calendar.getActualMaximum(Calendar.DAY_OF_MONTH))
        calendar.set(Calendar.HOUR_OF_DAY, 23)
        calendar.set(Calendar.MINUTE, 59)
        calendar.set(Calendar.SECOND, 59)
        return calendar.timeInMillis
    }

    private fun Int.dpToPx(): Int = (this * resources.displayMetrics.density).toInt()

    override fun onDestroyView() {
        super.onDestroyView()
    }
}
