package com.iie.group8_prog7313_poe_pt_2.view.part3

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import com.google.android.material.datepicker.MaterialDatePicker
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.snackbar.Snackbar
import com.iie.group8_prog7313_poe_pt_2.R
import com.iie.group8_prog7313_poe_pt_2.databinding.FragmentAddEditSubscriptionBinding
import com.iie.group8_prog7313_poe_pt_2.util.DateTimeUtils
import com.iie.group8_prog7313_poe_pt_2.viewmodel.AddEditSubscriptionViewModel
import com.iie.group8_prog7313_poe_pt_2.viewmodel.SubscriptionFormValidation
import kotlinx.coroutines.launch

class AddEditSubscriptionFragment : Fragment() {

    private var _binding: FragmentAddEditSubscriptionBinding? = null
    private val binding get() = _binding!!

    private val args: AddEditSubscriptionFragmentArgs by navArgs()

    private val viewModel: AddEditSubscriptionViewModel by viewModels {
        object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T =
                AddEditSubscriptionViewModel(requireActivity().application, args.subscriptionId) as T
        }
    }

    private val billingCycleValues = listOf("monthly", "weekly", "yearly")
    private val billingCycleLabels = listOf("Monthly", "Weekly", "Yearly")

    private var selectedStartDate: Long = System.currentTimeMillis()
    private var formInitialized = false

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        _binding = FragmentAddEditSubscriptionBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val isEdit = args.subscriptionId.isNotEmpty()

        binding.toolbar.title = getString(
            if (isEdit) R.string.add_edit_subscription_title_edit
            else R.string.add_edit_subscription_title_new,
        )
        binding.toolbar.setNavigationOnClickListener { findNavController().navigateUp() }

        val cycleAdapter = ArrayAdapter(
            requireContext(),
            android.R.layout.simple_spinner_dropdown_item,
            billingCycleLabels,
        )
        binding.autoCompleteBillingCycle.setAdapter(cycleAdapter)
        binding.autoCompleteBillingCycle.threshold = Int.MAX_VALUE
        // Default selection: Monthly
        binding.autoCompleteBillingCycle.setText(billingCycleLabels[0], false)

        binding.textStartDate.setText(DateTimeUtils.formatDateShort(selectedStartDate))
        binding.layoutStartDate.setEndIconOnClickListener { showDatePicker() }
        binding.textStartDate.setOnClickListener { showDatePicker() }

        if (isEdit) {
            binding.buttonDelete.visibility = View.VISIBLE
        }

        binding.buttonSave.setOnClickListener { attemptSave() }

        binding.buttonDelete.setOnClickListener {
            MaterialAlertDialogBuilder(requireContext())
                .setTitle(R.string.subscription_delete_confirm_title)
                .setMessage(R.string.subscription_delete_confirm_message)
                .setNegativeButton(R.string.dialog_cancel, null)
                .setPositiveButton(R.string.dialog_delete) { _, _ ->
                    viewModel.delete { err ->
                        if (err != null) {
                            Snackbar.make(binding.root, err.message ?: "Error", Snackbar.LENGTH_LONG).show()
                        } else {
                            findNavController().navigateUp()
                        }
                    }
                }
                .show()
        }

        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.existing.collect { sub ->
                    if (sub != null && !formInitialized) {
                        formInitialized = true
                        binding.editName.setText(sub.name)
                        binding.editAmount.setText(
                            if (sub.amount % 1.0 == 0.0) sub.amount.toInt().toString()
                            else sub.amount.toString()
                        )
                        val cycleIndex = billingCycleValues.indexOf(sub.billingCycle)
                            .coerceAtLeast(0)
                        binding.autoCompleteBillingCycle.setText(
                            billingCycleLabels[cycleIndex], false,
                        )
                        selectedStartDate = sub.startDate
                        binding.textStartDate.setText(DateTimeUtils.formatDateShort(selectedStartDate))
                    }
                }
            }
        }
    }

    private fun showDatePicker() {
        val picker = MaterialDatePicker.Builder.datePicker()
            .setTitleText(getString(R.string.subscription_select_date_title))
            .setSelection(selectedStartDate.coerceAtLeast(0L))
            .build()
        picker.addOnPositiveButtonClickListener { utcMillis ->
            selectedStartDate = DateTimeUtils.startOfDayMillis(utcMillis)
            binding.textStartDate.setText(DateTimeUtils.formatDateShort(selectedStartDate))
        }
        picker.show(childFragmentManager, "subscription_date")
    }

    private fun attemptSave() {
        val name = binding.editName.text?.toString().orEmpty()
        val amountText = binding.editAmount.text?.toString().orEmpty()
        val cycleLabel = binding.autoCompleteBillingCycle.text?.toString().orEmpty()
        val cycleIndex = billingCycleLabels.indexOf(cycleLabel).coerceAtLeast(0)
        val billingCycle = billingCycleValues[cycleIndex]

        when (val result = viewModel.validate(name, amountText)) {
            is SubscriptionFormValidation.Error ->
                Snackbar.make(binding.root, result.messageRes, Snackbar.LENGTH_LONG).show()
            SubscriptionFormValidation.Ok -> {
                val amount = amountText.trim().replace(",", ".").toDoubleOrNull() ?: return
                viewModel.save(
                    name = name,
                    amount = amount,
                    billingCycle = billingCycle,
                    startDate = selectedStartDate,
                ) { err ->
                    if (err != null) {
                        Snackbar.make(binding.root, err.message ?: "Error", Snackbar.LENGTH_LONG).show()
                    } else {
                        findNavController().navigateUp()
                    }
                }
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}

/*
  Reference list :
  1. Android Developers. 2026. Safe Args. [Online]. Available at:
    https://developer.android.com/guide/navigation/use-graph/safe-args [Accessed 1 June 2026].

  2.  Android Developers. 2026. MaterialAlertDialogBuilder. [Online]. Available at:
    https://developer.android.com/reference/com/google/android/material/dialog/MaterialAlertDialogBuilder [Accessed 1 June 2026].
 */
