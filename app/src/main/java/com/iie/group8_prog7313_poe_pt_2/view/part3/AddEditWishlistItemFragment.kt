package com.iie.group8_prog7313_poe_pt_2.view.part3

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import com.google.android.material.datepicker.MaterialDatePicker
import com.iie.group8_prog7313_poe_pt_2.R
import com.iie.group8_prog7313_poe_pt_2.databinding.FragmentAddEditWishlistItemBinding
import com.iie.group8_prog7313_poe_pt_2.model.entity.SavingsGoal
import com.iie.group8_prog7313_poe_pt_2.model.repository.SavingsGoalRepository
import com.iie.group8_prog7313_poe_pt_2.session.SessionManager
import com.iie.group8_prog7313_poe_pt_2.util.DateTimeUtils
import kotlinx.coroutines.launch

class AddEditWishlistItemFragment : Fragment() {

    private var _binding: FragmentAddEditWishlistItemBinding? = null
    private val binding get() = _binding!!
    private val args by navArgs<AddEditWishlistItemFragmentArgs>()

    private val repository = SavingsGoalRepository()
    private var existingGoal: SavingsGoal? = null
    private var selectedTargetDateMillis: Long = 0L

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentAddEditWishlistItemBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val userId = SessionManager(requireContext()).getUserId() ?: ""
        val isEdit = args.goalId.isNotEmpty()

        binding.toolbar.title = getString(
            if (isEdit) R.string.wishlist_add_edit_title_edit else R.string.wishlist_add_edit_title_add
        )
        binding.buttonSave.text = getString(
            if (isEdit) R.string.wishlist_save_changes else R.string.wishlist_save
        )
        if (isEdit) binding.buttonDelete.visibility = View.VISIBLE
        binding.toolbar.setNavigationOnClickListener { findNavController().navigateUp() }

        val openDatePicker = { showDatePicker() }
        binding.editTargetDate.setOnClickListener { openDatePicker() }
        binding.layoutTargetDate.setEndIconOnClickListener { openDatePicker() }

        if (isEdit) {
            viewLifecycleOwner.lifecycleScope.launch {
                existingGoal = repository.getById(userId, args.goalId)
                existingGoal?.let { goal ->
                    binding.editGoalName.setText(goal.name)
                    binding.editTargetAmount.setText(goal.targetAmount.toString())
                    selectedTargetDateMillis = goal.targetDate
                    binding.editTargetDate.setText(DateTimeUtils.formatDateShort(goal.targetDate))
                }
            }
        }

        binding.buttonSave.setOnClickListener {
            val name = binding.editGoalName.text?.toString()?.trim() ?: ""
            val targetText = binding.editTargetAmount.text?.toString()?.trim() ?: ""
            var valid = true

            if (name.isEmpty()) {
                binding.layoutGoalName.error = getString(R.string.wishlist_error_name)
                valid = false
            } else {
                binding.layoutGoalName.error = null
            }

            val targetAmount = targetText.toDoubleOrNull()
            if (targetAmount == null || targetAmount <= 0.0) {
                binding.layoutTargetAmount.error = getString(R.string.wishlist_error_target)
                valid = false
            } else {
                binding.layoutTargetAmount.error = null
            }

            if (selectedTargetDateMillis <= 0L) {
                binding.layoutTargetDate.error = getString(R.string.wishlist_error_date)
                valid = false
            } else {
                binding.layoutTargetDate.error = null
            }

            if (!valid || targetAmount == null) return@setOnClickListener

            viewLifecycleOwner.lifecycleScope.launch {
                if (isEdit) {
                    val savedAmount = existingGoal?.savedAmount ?: 0.0
                    repository.update(
                        userId,
                        SavingsGoal(
                            id = args.goalId,
                            name = name,
                            targetAmount = targetAmount,
                            savedAmount = savedAmount,
                            targetDate = selectedTargetDateMillis
                        )
                    )
                } else {
                    repository.insert(
                        userId,
                        SavingsGoal(
                            name = name,
                            targetAmount = targetAmount,
                            savedAmount = 0.0,
                            targetDate = selectedTargetDateMillis
                        )
                    )
                }
                findNavController().navigateUp()
            }
        }

        binding.buttonDelete.setOnClickListener {
            androidx.appcompat.app.AlertDialog.Builder(requireContext())
                .setTitle(R.string.wishlist_delete_confirm_title)
                .setMessage(R.string.wishlist_delete_confirm_message)
                .setNegativeButton(R.string.dialog_cancel, null)
                .setPositiveButton(R.string.dialog_delete) { _, _ ->
                    viewLifecycleOwner.lifecycleScope.launch {
                        repository.delete(userId, args.goalId)
                        findNavController().navigateUp()
                    }
                }
                .show()
        }
    }

    private fun showDatePicker() {
        val picker = MaterialDatePicker.Builder.datePicker()
            .setTitleText(getString(R.string.wishlist_select_date))
            .setSelection(
                selectedTargetDateMillis.takeIf { it > 0L }
                    ?: MaterialDatePicker.todayInUtcMilliseconds()
            )
            .build()
        picker.addOnPositiveButtonClickListener { utcMillis ->
            selectedTargetDateMillis = DateTimeUtils.startOfDayMillis(utcMillis)
            binding.editTargetDate.setText(DateTimeUtils.formatDateShort(selectedTargetDateMillis))
            binding.layoutTargetDate.error = null
        }
        picker.show(childFragmentManager, "wishlist_target_date")
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
