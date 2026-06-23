// MM-29: Budget Overview Screen

package com.iie.group8_prog7313_poe_pt_2.view.part2

import android.content.res.ColorStateList
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ProgressBar
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.iie.group8_prog7313_poe_pt_2.R
import java.text.DecimalFormat
import java.text.DecimalFormatSymbols

data class BudgetOverviewItem(
    val categoryName: String,
    val spent: Double,
    val minAmount: Double,
    val maxAmount: Double
)

// Google (2026)
class BudgetOverviewAdapter : ListAdapter<BudgetOverviewItem, BudgetOverviewAdapter.ViewHolder>(DiffCallback) {

    inner class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val categoryName: TextView = itemView.findViewById(R.id.budgetCategoryName)
        private val amountText: TextView = itemView.findViewById(R.id.budgetAmountText)
        private val goalText: TextView = itemView.findViewById(R.id.budgetGoalText)
        private val progressBar: ProgressBar = itemView.findViewById(R.id.budgetProgressBar)

        fun bind(item: BudgetOverviewItem) {
            val ctx = itemView.context
            val symbols = DecimalFormatSymbols().apply { groupingSeparator = ' ' }
            val fmt = DecimalFormat("#,##0", symbols)

            categoryName.text = item.categoryName
            amountText.text = "R ${fmt.format(item.spent.toLong())} / R ${fmt.format(item.maxAmount.toLong())}"
            goalText.text = "Goal: R ${fmt.format(item.minAmount.toLong())} – R ${fmt.format(item.maxAmount.toLong())}"

            // coerceIn(0.0, 100.0) prevents the progress bar from overflowing when spending
            // exceeds the maximum budget. Without clamping, a value above 100 would make the
            // ProgressBar draw past its bounds or throw an exception on some API levels.
            val progress = if (item.maxAmount > 0)
                (item.spent / item.maxAmount * 100).coerceIn(0.0, 100.0).toInt()
            else 0
            progressBar.progress = progress

            // Three-colour progress logic communicates budget health at a glance:
            //   brand colour spending is below the minimum target (under-spending)
            //   success_light (green) spending is within the goal range (on track)
            //   danger_light (red) spending has exceeded the maximum budget (over budget)
            val colorRes = when {
                item.spent > item.maxAmount -> R.color.danger_light
                item.spent >= item.minAmount -> R.color.success_light
                else -> R.color.brand
            }
            progressBar.progressTintList = ColorStateList.valueOf(ContextCompat.getColor(ctx, colorRes))
            progressBar.progressBackgroundTintList = ColorStateList.valueOf(0xA6FFFFFF.toInt())
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_budget_overview, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    // JetBrains (2025)
    companion object {
        private val DiffCallback = object : DiffUtil.ItemCallback<BudgetOverviewItem>() {
            override fun areItemsTheSame(oldItem: BudgetOverviewItem, newItem: BudgetOverviewItem) =
                oldItem.categoryName == newItem.categoryName
            override fun areContentsTheSame(oldItem: BudgetOverviewItem, newItem: BudgetOverviewItem) =
                oldItem == newItem
        }
    }
}

// References:
// 1. Google. 2026. Create dynamic lists with RecyclerView. https://developer.android.com/develop/ui/views/layout/recyclerview
// 2. JetBrains. 2025. Object declarations and expressions. https://kotlinlang.org/docs/object-declarations.html
