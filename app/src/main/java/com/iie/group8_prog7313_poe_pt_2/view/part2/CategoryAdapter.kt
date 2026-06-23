// MM-25: Categories Screen

package com.iie.group8_prog7313_poe_pt_2.view.part2

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.iie.group8_prog7313_poe_pt_2.R
import com.iie.group8_prog7313_poe_pt_2.model.entity.BudgetGoal
import com.iie.group8_prog7313_poe_pt_2.model.entity.Category
import java.text.DecimalFormat
import java.text.DecimalFormatSymbols

// Google (2026)
class CategoryAdapter(
    private val onItemClick: (Category) -> Unit
    ) : ListAdapter<Category, CategoryAdapter.ViewHolder>(DiffCallback) {

    // goals is a separate property rather than being embedded in the list items because
    // budget goals for the current month are fetched independently from categories and may
    // arrive at a different time. Populating it via a property update (plus
    // notifyDataSetChanged) keeps the two data streams decoupled.
    var goals: Map<String, BudgetGoal> = emptyMap()

    inner class ViewHolder(itemView : View) : RecyclerView.ViewHolder(itemView) {
        private val categoryName: TextView = itemView.findViewById(R.id.categoryName)
        private val goalRange: TextView = itemView.findViewById(R.id.goalRange)

        fun bind(category: Category) {
            categoryName.text = category.name
            val goal = goals[category.id]
            if (goal != null) {
                // A space is used as the grouping separator (e.g. "R 1 500") to match the
                // South African convention of spacing thousands groups rather than using a
                // comma, which could be confused with the decimal separator.
                val symbols = DecimalFormatSymbols().apply { groupingSeparator = ' ' }
                val fmt = DecimalFormat("#,##0", symbols)
                goalRange.visibility = View.VISIBLE
                goalRange.text = "Goal: R ${fmt.format(goal.minAmount.toLong())} – R ${fmt.format(goal.maxAmount.toLong())}"
            } else {
                goalRange.visibility = View.GONE
            }
            itemView.setOnClickListener { onItemClick(category) }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_category, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    // JetBrains (2025)
    companion object {
        private val DiffCallback = object : DiffUtil.ItemCallback<Category>() {
            override fun areItemsTheSame(oldItem: Category, newItem: Category) = oldItem.id == newItem.id
            override fun areContentsTheSame(oldItem: Category, newItem: Category) = oldItem == newItem
        }
    }
}

// References:
// 1. Google. 2026. Create dynamic lists with RecyclerView. https://developer.android.com/develop/ui/views/layout/recyclerview
// 2. JetBrains. 2025. Object declarations and expressions. https://kotlinlang.org/docs/object-declarations.html
