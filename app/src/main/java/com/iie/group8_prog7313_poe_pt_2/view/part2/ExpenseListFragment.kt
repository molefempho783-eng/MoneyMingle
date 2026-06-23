package com.iie.group8_prog7313_poe_pt_2.view.part2

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.datepicker.MaterialDatePicker
import com.iie.group8_prog7313_poe_pt_2.R
import com.iie.group8_prog7313_poe_pt_2.databinding.FragmentExpenseListBinding
import com.iie.group8_prog7313_poe_pt_2.util.DateTimeUtils
import com.iie.group8_prog7313_poe_pt_2.viewmodel.ExpenseListViewModel
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

class ExpenseListFragment : Fragment() {

    private var _binding: FragmentExpenseListBinding? = null
    private val binding get() = _binding!!

    private val viewModel: ExpenseListViewModel by viewModels()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        _binding = FragmentExpenseListBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val adapter = ExpenseListAdapter { item ->
            val action = ExpenseListFragmentDirections.actionExpenseListFragmentToExpenseDetailFragment2(
                expenseId = item.expense.id,
            )
            findNavController().navigate(action)
        }

        binding.recyclerExpenses.layoutManager = LinearLayoutManager(requireContext())
        binding.recyclerExpenses.adapter = adapter

        binding.fabAddExpense.setOnClickListener {
            val action = ExpenseListFragmentDirections.actionExpenseListFragmentToAddEditExpenseFragment(
                expenseId = "",
            )
            findNavController().navigate(action)
        }

        binding.chipThisMonth.setOnClickListener { applyThisMonthFilter() }
        binding.chipLastMonth.setOnClickListener { applyLastMonthFilter() }
        binding.chipCustom.setOnClickListener { showDateRangePicker() }

        // Default to the current month so users see relevant data immediately without having
        // to interact with the filter chips.
        applyThisMonthFilter()

        viewLifecycleOwner.lifecycleScope.launch {
            // repeatOnLifecycle(STARTED) ensures the Flow collector is active only while the
            // fragment is visible. Without this, the collector would continue running in the
            // background (e.g. while another fragment is on top), wasting resources and
            // potentially delivering updates to a detached view.
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.items.collect { list ->
                    adapter.submitList(list)
                    val empty = list.isEmpty()
                    binding.textEmpty.visibility = if (empty) View.VISIBLE else View.GONE
                    binding.recyclerExpenses.visibility = if (empty) View.INVISIBLE else View.VISIBLE
                }
            }
        }
    }

    private fun setActiveChip(chipId: Int) {
        binding.chipThisMonth.isChecked = chipId == R.id.chipThisMonth
        binding.chipLastMonth.isChecked = chipId == R.id.chipLastMonth
        binding.chipCustom.isChecked = chipId == R.id.chipCustom
    }

    private fun applyThisMonthFilter() {
        setActiveChip(R.id.chipThisMonth)
        val cal = Calendar.getInstance()
        cal.set(Calendar.DAY_OF_MONTH, 1)
        val start = DateTimeUtils.startOfDayMillis(cal.timeInMillis)
        cal.set(Calendar.DAY_OF_MONTH, cal.getActualMaximum(Calendar.DAY_OF_MONTH))
        val end = DateTimeUtils.endOfDayMillis(cal.timeInMillis)
        viewModel.setDateRangeStartInclusive(start)
        viewModel.setDateRangeEndInclusive(end)
        updateLabels(start)
    }

    private fun applyLastMonthFilter() {
        setActiveChip(R.id.chipLastMonth)
        val cal = Calendar.getInstance()
        cal.add(Calendar.MONTH, -1)
        cal.set(Calendar.DAY_OF_MONTH, 1)
        val start = DateTimeUtils.startOfDayMillis(cal.timeInMillis)
        cal.set(Calendar.DAY_OF_MONTH, cal.getActualMaximum(Calendar.DAY_OF_MONTH))
        val end = DateTimeUtils.endOfDayMillis(cal.timeInMillis)
        viewModel.setDateRangeStartInclusive(start)
        viewModel.setDateRangeEndInclusive(end)
        updateLabels(start)
    }

    private fun showDateRangePicker() {
        val picker = MaterialDatePicker.Builder.dateRangePicker()
            .setTitleText(getString(R.string.expense_filter_custom))
            .build()
        picker.addOnPositiveButtonClickListener { selection ->
            setActiveChip(R.id.chipCustom)
            val start = DateTimeUtils.startOfDayMillis(selection.first)
            val end = DateTimeUtils.endOfDayMillis(selection.second)
            viewModel.setDateRangeStartInclusive(start)
            viewModel.setDateRangeEndInclusive(end)
            updateLabels(start)
        }
        picker.show(childFragmentManager, "expense_date_range")
    }

    private fun updateLabels(fromMillis: Long) {
        val label = SimpleDateFormat("MMMM yyyy", Locale.getDefault())
            .format(fromMillis)
            .uppercase(Locale.getDefault())
        binding.textMonthLabel.text = label
        Log.d("ExpenseListFragment", "Expense filter applied: $label")
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}

/*
  Reference list :
  - Android Developers, 2026. MaterialDatePicker API reference [online]. Available at:
    <https://developer.android.com/reference/com/google/android/material/datepicker/MaterialDatePicker> [Accessed 20 April 2026].
  - Android Developers, 2026. Navigation with Safe Args [online]. Available at:
    <https://developer.android.com/guide/navigation/use-graph/safe-args> [Accessed 20 April 2026].
 */
