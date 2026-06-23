package com.iie.group8_prog7313_poe_pt_2.view.part2

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.iie.group8_prog7313_poe_pt_2.databinding.ItemExpenseRowBinding
import com.iie.group8_prog7313_poe_pt_2.viewmodel.ExpenseListItem
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class ExpenseListAdapter(
    private val onItemClick: (ExpenseListItem) -> Unit,
) : ListAdapter<ExpenseListItem, ExpenseListAdapter.ExpenseRowViewHolder>(DIFF) {

    // South African Rand locale is used because MoneyMingle is a ZA-focused app.
    // "en-ZA" produces the "R" currency symbol with comma as the decimal separator.
    private val currencyFormat: NumberFormat =
        NumberFormat.getCurrencyInstance(Locale.forLanguageTag("en-ZA"))
    private val dateFormat = SimpleDateFormat("d MMM", Locale.getDefault())

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ExpenseRowViewHolder {
        val binding = ItemExpenseRowBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ExpenseRowViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ExpenseRowViewHolder, position: Int) {
        holder.bind(getItem(position), currencyFormat, dateFormat, onItemClick)
    }

    class ExpenseRowViewHolder(
        private val binding: ItemExpenseRowBinding,
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(
            item: ExpenseListItem,
            currencyFormat: NumberFormat,
            dateFormat: SimpleDateFormat,
            onItemClick: (ExpenseListItem) -> Unit,
        ) {
            val e = item.expense
            binding.textAmount.text = currencyFormat.format(e.amount)
            binding.textDescription.text = e.description
            binding.textSubtitle.text = "${item.categoryName} · ${dateFormat.format(Date(e.date))}"
            binding.root.setOnClickListener { onItemClick(item) }
        }
    }

    companion object {
        private val DIFF = object : DiffUtil.ItemCallback<ExpenseListItem>() {
            // areItemsTheSame checks only the stable database ID to determine whether two list
            // entries represent the same expense row (identity check). This allows DiffUtil to
            // detect moves and avoid recreating ViewHolders unnecessarily.
            override fun areItemsTheSame(oldItem: ExpenseListItem, newItem: ExpenseListItem): Boolean =
                oldItem.expense.id == newItem.expense.id

            // areContentsTheSame uses full data class equality to decide whether the visible
            // content has changed and a rebind is needed. Because ExpenseListItem is a data
            // class, the compiler-generated equals() covers all fields automatically.
            override fun areContentsTheSame(oldItem: ExpenseListItem, newItem: ExpenseListItem): Boolean =
                oldItem == newItem
        }
    }
}

/*
  Reference list :
  - Android Developers, 2026. Create dynamic lists with RecyclerView [online]. Available at:
    <https://developer.android.com/develop/ui/views/layout/recyclerview> [Accessed 20 April 2026].
 */
