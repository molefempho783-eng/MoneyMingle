package com.iie.group8_prog7313_poe_pt_2.view.part3

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
import com.google.android.material.button.MaterialButton
import com.iie.group8_prog7313_poe_pt_2.R
import com.iie.group8_prog7313_poe_pt_2.model.entity.SavingsGoal
import com.iie.group8_prog7313_poe_pt_2.util.DateTimeUtils
import java.text.DecimalFormat
import java.text.DecimalFormatSymbols

class WishlistAdapter(
    private val onGoalClick: (SavingsGoal) -> Unit,
    private val onAddContribution: (SavingsGoal) -> Unit
) : ListAdapter<SavingsGoal, WishlistAdapter.ViewHolder>(DiffCallback) {

    inner class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val goalName: TextView = itemView.findViewById(R.id.goalName)
        private val completedBadge: TextView = itemView.findViewById(R.id.goalCompletedBadge)
        private val amountText: TextView = itemView.findViewById(R.id.goalAmountText)
        private val progressPercent: TextView = itemView.findViewById(R.id.goalProgressPercent)
        private val progressBar: ProgressBar = itemView.findViewById(R.id.goalProgressBar)
        private val timeRemaining: TextView = itemView.findViewById(R.id.goalTimeRemaining)
        private val targetDate: TextView = itemView.findViewById(R.id.goalTargetDate)
        private val addContributionButton: MaterialButton = itemView.findViewById(R.id.buttonAddContribution)

        fun bind(goal: SavingsGoal) {
            val ctx = itemView.context
            val symbols = DecimalFormatSymbols().apply { groupingSeparator = ' ' }
            val fmt = DecimalFormat("#,##0", symbols)

            goalName.text = goal.name
            amountText.text = "R ${fmt.format(goal.savedAmount.toLong())} / R ${fmt.format(goal.targetAmount.toLong())}"
            progressPercent.text = ctx.getString(R.string.wishlist_progress_percent, goal.progressPercent)
            progressBar.progress = goal.progressPercent

            val completed = goal.isCompleted
            completedBadge.visibility = if (completed) View.VISIBLE else View.GONE
            addContributionButton.visibility = if (completed) View.GONE else View.VISIBLE

            val progressColor = if (completed) {
                R.color.accent_green_bright
            } else {
                R.color.accent_amber
            }
            progressBar.progressTintList = ColorStateList.valueOf(
                ContextCompat.getColor(ctx, progressColor)
            )

            timeRemaining.text = DateTimeUtils.formatTimeRemaining(goal.targetDate)
            targetDate.text = ctx.getString(
                R.string.wishlist_target_date,
                DateTimeUtils.formatDateShort(goal.targetDate)
            )

            itemView.setOnClickListener { onGoalClick(goal) }
            addContributionButton.setOnClickListener { onAddContribution(goal) }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_savings_goal, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    companion object {
        private val DiffCallback = object : DiffUtil.ItemCallback<SavingsGoal>() {
            override fun areItemsTheSame(oldItem: SavingsGoal, newItem: SavingsGoal) =
                oldItem.id == newItem.id

            override fun areContentsTheSame(oldItem: SavingsGoal, newItem: SavingsGoal) =
                oldItem == newItem
        }
    }
}
