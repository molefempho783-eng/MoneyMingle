// MM-29: Budget Overview Screen
// MM-30: Category Spending Totals over a period

package com.iie.group8_prog7313_poe_pt_2.view.part2

import android.os.Bundle
import android.util.Log
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.NumberPicker
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.chip.Chip
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.iie.group8_prog7313_poe_pt_2.R
import com.iie.group8_prog7313_poe_pt_2.databinding.FragmentBudgetOverviewBinding
import com.iie.group8_prog7313_poe_pt_2.model.repository.CategoryRepository
import com.iie.group8_prog7313_poe_pt_2.model.repository.ExpenseRepository
import com.iie.group8_prog7313_poe_pt_2.session.SessionManager
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch
import java.util.Calendar

// Google (2026)
class BudgetOverviewFragment : Fragment() {
    private var _binding: FragmentBudgetOverviewBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentBudgetOverviewBinding.inflate(inflater, container, false)
        return binding.root
    }

    // Google (2026)
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val categoryRepo = CategoryRepository()
        val expenseRepo = ExpenseRepository()
        val adapter = BudgetOverviewAdapter()

        binding.budgetList.apply {
            layoutManager = LinearLayoutManager(requireContext())
            this.adapter = adapter
        }

        binding.navManageCategories.setOnClickListener {
            findNavController().navigate(
                BudgetOverviewFragmentDirections.actionBudgetOverviewFragmentToCategoriesFragment2()
            )
        }

        binding.navSpendingGraph.setOnClickListener {
            findNavController().navigate(
                BudgetOverviewFragmentDirections.actionBudgetOverviewFragmentToSpendingGraphFragment()
            )
        }

        val userId = SessionManager(requireContext()).getUserId() ?: ""

        // MM-30: Returns start/end timestamps and the month/year for budget goal lookup
        fun getMonthRange(offset: Int): Triple<Long, Long, Pair<Int, Int>> {
            val cal = Calendar.getInstance().apply { add(Calendar.MONTH, offset) }
            val year = cal.get(Calendar.YEAR)
            val month = cal.get(Calendar.MONTH)
            val start = Calendar.getInstance().apply {
                set(year, month, 1, 0, 0, 0)
                set(Calendar.MILLISECOND, 0)
            }.timeInMillis
            val end = Calendar.getInstance().apply {
                set(year, month, 1)
                val lastDay = getActualMaximum(Calendar.DAY_OF_MONTH)
                set(year, month, lastDay, 23, 59, 59)
                set(Calendar.MILLISECOND, 999)
            }.timeInMillis
            return Triple(start, end, Pair(month + 1, year))
        }

        fun monthRangeFromParts(month: Int, year: Int): Pair<Long, Long> {
            val start = Calendar.getInstance().apply {
                set(year, month, 1, 0, 0, 0)
                set(Calendar.MILLISECOND, 0)
            }.timeInMillis
            val end = Calendar.getInstance().apply {
                set(year, month, 1)
                val lastDay = getActualMaximum(Calendar.DAY_OF_MONTH)
                set(year, month, lastDay, 23, 59, 59)
                set(Calendar.MILLISECOND, 999)
            }.timeInMillis
            return Pair(start, end)
        }

        // MM-30: Toggle selected/unselected appearance on period chips
        fun setSelectedButton(selected: Chip) {
            listOf(binding.btnThisMonth, binding.btnLastMonth, binding.btnCustom).forEach { chip ->
                chip.isChecked = (chip == selected)
            }
        }

        val initial = getMonthRange(0)
        var startDate = initial.first
        var endDate = initial.second
        var currentMonth = initial.third.first
        var currentYear = initial.third.second

        // MM-30: Reload budget list for the current period
        fun reload() {
            Log.d("BudgetOverviewFragment", "Loading budget overview for month=$currentMonth year=$currentYear")
            viewLifecycleOwner.lifecycleScope.launch {
                // combine() is used so that both the budget goals and the category list are
                // fetched concurrently. The UI is rendered only when both Flows have emitted,
                // ensuring category names are always available to label the goal rows.
                combine(
                    categoryRepo.getBudgetGoalsByMonth(userId, currentMonth, currentYear),
                    categoryRepo.getAllCategoriesByUser(userId)
                ) { goals, categories -> Pair(goals, categories) }
                .collectLatest { (goals, categories) ->
                    // Expense totals are scoped to the selected date range, not the goal month,
                    // so they are fetched separately after the combined emit.
                    // filter { it.categoryId != null } guards against any uncategorised expense
                    // rows that may have been inserted without a valid category reference.
                    val totalsMap = expenseRepo.getCategoryTotals(userId, startDate, endDate)
                        .filter { it.categoryId != null }
                        .associate { it.categoryId!! to it.total }
                    val categoryMap = categories.associateBy { it.id }

                    val items = goals.mapNotNull { goal ->
                        val category = categoryMap[goal.categoryId] ?: return@mapNotNull null
                        BudgetOverviewItem(
                            categoryName = category.name,
                            spent = totalsMap[goal.categoryId] ?: 0.0,
                            minAmount = goal.minAmount,
                            maxAmount = goal.maxAmount
                        )
                    }

                    adapter.submitList(items)
                    binding.emptyState.visibility = if (items.isEmpty()) View.VISIBLE else View.GONE
                    binding.budgetList.visibility = if (items.isEmpty()) View.GONE else View.VISIBLE
                }
            }
        }

        reload()

        // MM-30: Period selector buttons
        binding.btnThisMonth.setOnClickListener {
            val range = getMonthRange(0)
            startDate = range.first
            endDate = range.second
            currentMonth = range.third.first
            currentYear = range.third.second
            setSelectedButton(binding.btnThisMonth)
            reload()
        }

        binding.btnLastMonth.setOnClickListener {
            val range = getMonthRange(-1)
            startDate = range.first
            endDate = range.second
            currentMonth = range.third.first
            currentYear = range.third.second
            setSelectedButton(binding.btnLastMonth)
            reload()
        }

        // MM-30: Month/year picker for custom period.
        // A custom NumberPicker dialog is used instead of MaterialDatePicker because budget
        // periods are full calendar months the user should pick a month and year, not an
        // arbitrary date range. MaterialDatePicker operates at the day level, which would
        // require extra validation to constrain it to whole-month selections.
        binding.btnCustom.setOnClickListener {
            val monthNames = arrayOf(
                "January", "February", "March", "April", "May", "June",
                "July", "August", "September", "October", "November", "December"
            )
            val now = Calendar.getInstance()

            val monthPicker = NumberPicker(requireContext()).apply {
                minValue = 0
                maxValue = 11
                displayedValues = monthNames
                value = now.get(Calendar.MONTH)
            }
            val yearPicker = NumberPicker(requireContext()).apply {
                minValue = 2020
                maxValue = now.get(Calendar.YEAR)
                value = now.get(Calendar.YEAR)
            }

            val pickerLayout = LinearLayout(requireContext()).apply {
                orientation = LinearLayout.HORIZONTAL
                gravity = Gravity.CENTER
                setPadding(32, 24, 32, 8)
                addView(monthPicker)
                addView(yearPicker)
            }

            MaterialAlertDialogBuilder(requireContext())
                .setTitle("Select Month")
                .setView(pickerLayout)
                .setPositiveButton("OK") { _, _ ->
                    val selectedMonth = monthPicker.value
                    val selectedYear = yearPicker.value
                    val range = monthRangeFromParts(selectedMonth, selectedYear)
                    startDate = range.first
                    endDate = range.second
                    currentMonth = selectedMonth + 1
                    currentYear = selectedYear
                    setSelectedButton(binding.btnCustom)
                    reload()
                }
                .setNegativeButton("Cancel", null)
                .show()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}

// References:
// 1. Google. 2026. Fragment lifecycle. https://developer.android.com/guide/fragments/lifecycle
// 2. Google. 2026. Kotlin flows on Android. https://developer.android.com/kotlin/flow
// 3. Google. 2026. MaterialAlertDialogBuilder. https://developer.android.com/reference/com/google/android/material/dialog/MaterialAlertDialogBuilder
