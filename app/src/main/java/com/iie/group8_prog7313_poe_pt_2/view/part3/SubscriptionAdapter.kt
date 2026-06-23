package com.iie.group8_prog7313_poe_pt_2.view.part3

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.iie.group8_prog7313_poe_pt_2.R
import com.iie.group8_prog7313_poe_pt_2.databinding.ItemSubscriptionRowBinding
import com.iie.group8_prog7313_poe_pt_2.model.entity.Subscription
import com.iie.group8_prog7313_poe_pt_2.util.DateTimeUtils
import java.text.NumberFormat
import java.util.Calendar
import java.util.Locale

class SubscriptionAdapter(
    private val onItemClick: (Subscription) -> Unit,
) : ListAdapter<Subscription, SubscriptionAdapter.ViewHolder>(DIFF) {

    private val currencyFormat: NumberFormat =
        NumberFormat.getCurrencyInstance(Locale.forLanguageTag("en-ZA"))

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemSubscriptionRowBinding.inflate(
            LayoutInflater.from(parent.context), parent, false,
        )
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(getItem(position), currencyFormat, onItemClick)
    }

    class ViewHolder(private val binding: ItemSubscriptionRowBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(
            item: Subscription,
            currencyFormat: NumberFormat,
            onItemClick: (Subscription) -> Unit,
        ) {
            val context = binding.root.context

            // Colour varies by billing cycle
            // Monthly = purple, Yearly = green, Weekly = amber
            val accentColor = ContextCompat.getColor(
                context,
                when (item.billingCycle) {
                    "yearly" -> R.color.accent_green_bright
                    "weekly" -> R.color.accent_amber
                    else -> R.color.brand
                },
            )

            binding.cardSubscription.strokeColor = accentColor
            binding.textName.text = item.name
            binding.textAmount.text = currencyFormat.format(item.amount)
            binding.textAmount.setTextColor(accentColor)
            binding.textBillingCycle.text = item.billingCycle.replaceFirstChar { it.uppercase() }
            binding.textBillingCycle.setTextColor(accentColor)

            val nextDate = nextPaymentDate(item.startDate, item.billingCycle)
            binding.textRenews.text = if (nextDate != null) {
                "Renews ${DateTimeUtils.formatDateShort(nextDate)}"
            } else {
                ""
            }

            binding.root.setOnClickListener { onItemClick(item) }
        }

        private fun nextPaymentDate(startDate: Long, billingCycle: String): Long? {
            if (startDate == 0L) return null
            val today = System.currentTimeMillis()
            val cal = Calendar.getInstance()
            cal.timeInMillis = startDate
            while (cal.timeInMillis <= today) {
                when (billingCycle) {
                    "weekly" -> cal.add(Calendar.WEEK_OF_YEAR, 1)
                    "yearly" -> cal.add(Calendar.YEAR, 1)
                    else -> cal.add(Calendar.MONTH, 1)
                }
            }
            return cal.timeInMillis
        }
    }

    companion object {
        private val DIFF = object : DiffUtil.ItemCallback<Subscription>() {
            override fun areItemsTheSame(oldItem: Subscription, newItem: Subscription): Boolean =
                oldItem.id == newItem.id

            override fun areContentsTheSame(oldItem: Subscription, newItem: Subscription): Boolean =
                oldItem == newItem
        }
    }
}