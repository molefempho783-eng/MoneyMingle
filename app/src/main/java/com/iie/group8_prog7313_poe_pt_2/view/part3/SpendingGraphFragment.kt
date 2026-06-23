package com.iie.group8_prog7313_poe_pt_2.view.part3

import android.graphics.Color
import android.os.Bundle
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.ViewTreeObserver
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.google.android.material.card.MaterialCardView
import com.google.android.material.chip.Chip
import com.iie.group8_prog7313_poe_pt_2.R
import com.iie.group8_prog7313_poe_pt_2.model.repository.CategoryRepository
import com.iie.group8_prog7313_poe_pt_2.model.repository.ExpenseRepository
import com.iie.group8_prog7313_poe_pt_2.session.SessionManager
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.text.DecimalFormat
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

data class CategorySpendingItem(
    val categoryName: String,
    val spent: Double,
    val minGoal: Double,
    val maxGoal: Double,
    val percentage: Double
)

class SpendingGraphFragment : Fragment() {

    private lateinit var sessionManager: SessionManager
    private var userId: String = ""

    private val categoryRepo = CategoryRepository()
    private val expenseRepo = ExpenseRepository()

    private lateinit var categoryColors: List<Int>

    private var selectedMonths = 1

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        return inflater.inflate(R.layout.fragment_spending_graph, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        categoryColors = listOf(
            ContextCompat.getColor(requireContext(), R.color.accent_purple_light),
            ContextCompat.getColor(requireContext(), R.color.accent_cyan),
            ContextCompat.getColor(requireContext(), R.color.accent_orange_light),
            ContextCompat.getColor(requireContext(), R.color.danger_light),
            ContextCompat.getColor(requireContext(), R.color.accent_pink),
            ContextCompat.getColor(requireContext(), R.color.accent_green_bright),
        )

        sessionManager = SessionManager(requireContext())
        userId = sessionManager.getUserId() ?: ""

        val chip1M = view.findViewById<Chip>(R.id.chip_1m)
        val chip3M = view.findViewById<Chip>(R.id.chip_3m)
        val chip6M = view.findViewById<Chip>(R.id.chip_6m)
        val chip1Y = view.findViewById<Chip>(R.id.chip_1y)

        chip1M.setOnClickListener { selectedMonths = 1; load() }
        chip3M.setOnClickListener { selectedMonths = 3; load() }
        chip6M.setOnClickListener { selectedMonths = 6; load() }
        chip1Y.setOnClickListener { selectedMonths = 12; load() }

        load()
    }

    private fun load() {
        if (userId.isEmpty()) return

        lifecycleScope.launch {
            val container = view?.findViewById<LinearLayout>(R.id.categoriesContainer)
            val card = view?.findViewById<MaterialCardView>(R.id.spendingPerCategoryCard)
            val noData = view?.findViewById<TextView>(R.id.textNoCategories)
            val title = view?.findViewById<TextView>(R.id.textSpendingTitle)

            categoryRepo.getAllCategoriesByUser(userId).collectLatest { categories ->

                if (categories.isEmpty()) {
                    card?.visibility = View.GONE
                    noData?.visibility = View.VISIBLE
                    return@collectLatest
                }

                card?.visibility = View.VISIBLE
                noData?.visibility = View.GONE
                container?.removeAllViews()

                val formatter = SimpleDateFormat("MMMM yyyy", Locale.getDefault())
                title?.text = "Spending per Category - ${formatter.format(Calendar.getInstance().time)}"

                val cal = Calendar.getInstance()
                val endDate = cal.timeInMillis
                cal.add(Calendar.MONTH, -(selectedMonths - 1))
                cal.set(Calendar.DAY_OF_MONTH, 1)
                cal.set(Calendar.HOUR_OF_DAY, 0)
                cal.set(Calendar.MINUTE, 0)
                cal.set(Calendar.SECOND, 0)
                val startDate = cal.timeInMillis

                val expenses = expenseRepo.getByUserAndDateRange(userId, startDate, endDate)

                val goals = categoryRepo
                    .getBudgetGoalsByMonth(
                        userId,
                        Calendar.getInstance().get(Calendar.MONTH) + 1,
                        Calendar.getInstance().get(Calendar.YEAR)
                    )
                    .first()

                val items = categories.mapNotNull { cat ->
                    val spent = expenses.filter { it.categoryId == cat.id }.sumOf { it.amount }
                    val goal = goals.firstOrNull { it.categoryId == cat.id }
                    val min = goal?.minAmount ?: 0.0
                    val max = goal?.maxAmount ?: 0.0

                    // Show category if it has spending OR a budget goal
                    if (spent <= 0 && max <= 0) return@mapNotNull null

                    val percent = if (max > 0) (spent / max) * 100 else 0.0

                    CategorySpendingItem(cat.name, spent, min, max, percent)
                }

                items.forEachIndexed { index, item ->
                    container?.addView(createRow(item, categoryColors[index % categoryColors.size]))
                }
            }
        }
    }

    private fun createRow(item: CategorySpendingItem, color: Int): View {
        val fmt = DecimalFormat("#,##0")

        val root = LinearLayout(requireContext()).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(0, 0, 0, 26)
        }

        // ── Top row: category name + spent / max ──────────────────────────────
        val top = LinearLayout(requireContext()).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
        }

        val name = TextView(requireContext()).apply {
            text = item.categoryName
            textSize = 16f
            layoutParams = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f)
        }

        val valueLabel = if (item.maxGoal > 0)
            "R ${fmt.format(item.spent)} / R ${fmt.format(item.maxGoal)}"
        else
            "R ${fmt.format(item.spent)}"

        val value = TextView(requireContext()).apply {
            text = valueLabel
        }

        top.addView(name)
        top.addView(value)

        // ── Progress bar container (FrameLayout for overlay positioning) ──────
        val barContainerH = 40.dpToPx()
        val trackH = 12.dpToPx()
        val markerMinH = 30.dpToPx()   // taller than track so they're clearly visible
        val markerMaxH = 40.dpToPx()   // full container height for max marker
        val markerW = 5.dpToPx()       // thick enough to be noticeable

        val bar = FrameLayout(requireContext()).apply {
            layoutParams = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, barContainerH
            ).apply { topMargin = 10 }
        }

        // Gray track (background)
        val track = View(requireContext()).apply {
            setBackgroundColor(Color.parseColor("#E0E0E0"))
            layoutParams = FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, trackH, Gravity.CENTER_VERTICAL
            )
        }

        // Actual spend fill — matches "Actual spend" key colour (accent_purple_light)
        val fill = View(requireContext()).apply {
            setBackgroundColor(ContextCompat.getColor(requireContext(), R.color.accent_purple_light))
            layoutParams = FrameLayout.LayoutParams(0, trackH, Gravity.CENTER_VERTICAL)
        }

        // Min goal marker — green vertical line (initially invisible, positioned after layout)
        val minMarker = View(requireContext()).apply {
            setBackgroundColor(ContextCompat.getColor(requireContext(), R.color.accent_green_bright))
            layoutParams = FrameLayout.LayoutParams(markerW, markerMinH, Gravity.CENTER_VERTICAL)
            visibility = if (item.minGoal > 0 && item.maxGoal > 0) View.VISIBLE else View.GONE
        }

        // Max goal marker — danger_dark vertical line at right edge
        val maxMarker = View(requireContext()).apply {
            setBackgroundColor(ContextCompat.getColor(requireContext(), R.color.danger_dark))
            layoutParams = FrameLayout.LayoutParams(markerW, markerMaxH, Gravity.CENTER_VERTICAL)
            visibility = if (item.maxGoal > 0) View.VISIBLE else View.GONE
        }

        bar.addView(track)
        bar.addView(fill)
        bar.addView(minMarker)
        bar.addView(maxMarker)

        // Position markers and fill after layout is measured (ViewTreeObserver is reliable)
        bar.viewTreeObserver.addOnGlobalLayoutListener(object : ViewTreeObserver.OnGlobalLayoutListener {
            override fun onGlobalLayout() {
                bar.viewTreeObserver.removeOnGlobalLayoutListener(this)
                val barW = bar.width
                if (barW <= 0) return

                // Actual spend fill width
                val fillPx = (barW * (item.percentage / 100.0).coerceIn(0.0, 1.0)).toInt()
                (fill.layoutParams as FrameLayout.LayoutParams).width = fillPx
                fill.requestLayout()

                // Min goal marker position (all Double arithmetic, then convert to Float for x)
                if (item.minGoal > 0 && item.maxGoal > 0) {
                    val minRatio = (item.minGoal / item.maxGoal).coerceIn(0.0, 1.0)
                    minMarker.x = (barW * minRatio - markerW / 2.0).coerceAtLeast(0.0).toFloat()
                }

                // Max goal marker at right edge
                if (item.maxGoal > 0) {
                    maxMarker.x = (barW - markerW).toFloat().coerceAtLeast(0f)
                }
            }
        })

        root.addView(top)
        root.addView(bar)
        return root
    }

    private fun Int.dpToPx(): Int = (this * resources.displayMetrics.density).toInt()
}

// References:
// 1. Google. 2026. Create dynamic lists with RecyclerView. https://developer.android.com/develop/ui/views/layout/recyclerview
// 2. JetBrains. 2025. Object declarations and expressions. https://kotlinlang.org/docs/object-declarations.html
