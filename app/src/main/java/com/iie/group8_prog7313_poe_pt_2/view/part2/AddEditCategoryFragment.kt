// MM-26: Add/Edit Category Screen

package com.iie.group8_prog7313_poe_pt_2.view.part2

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import kotlinx.coroutines.launch
import com.iie.group8_prog7313_poe_pt_2.databinding.FragmentAddEditCategoryBinding
import com.iie.group8_prog7313_poe_pt_2.model.entity.BudgetGoal
import com.iie.group8_prog7313_poe_pt_2.model.entity.Category
import com.iie.group8_prog7313_poe_pt_2.model.repository.CategoryRepository
import com.iie.group8_prog7313_poe_pt_2.session.SessionManager
import com.iie.group8_prog7313_poe_pt_2.R
import java.util.Calendar

class AddEditCategoryFragment : Fragment() {
    private var _binding: FragmentAddEditCategoryBinding? = null
    private val binding get() = _binding!!
    private val args by navArgs<AddEditCategoryFragmentArgs>()

    private var existingCategory: Category? = null
    private var existingGoal: BudgetGoal? = null

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentAddEditCategoryBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val repository = CategoryRepository()
        val userId = SessionManager(requireContext()).getUserId() ?: ""
        val isEdit = args.categoryId.isNotEmpty()
        Log.d("AddEditCategoryFragment", if (isEdit) "Edit mode - loading category ${args.categoryId}" else "Add mode - creating new category")

        val now = Calendar.getInstance()
        val currentMonth = now.get(Calendar.MONTH) + 1
        val currentYear = now.get(Calendar.YEAR)

        binding.toolbar.title = if (isEdit) "Edit category" else "Add category"
        if (isEdit) binding.buttonSave.text = "Save changes"
        if (isEdit) binding.buttonDelete.visibility = View.VISIBLE
        binding.toolbar.setNavigationOnClickListener { findNavController().navigateUp() }

        if (isEdit) {
            viewLifecycleOwner.lifecycleScope.launch {
                existingCategory = repository.getCategoryById(userId, args.categoryId)
                existingGoal = repository.getBudgetGoalForCategory(
                    userId, args.categoryId, currentMonth, currentYear
                )
                existingCategory?.let { binding.editCategoryName.setText(it.name) }
                existingGoal?.let {
                    binding.editMinGoal.setText(it.minAmount.toString())
                    binding.editMaxGoal.setText(it.maxAmount.toString())
                }
            }
        }

        binding.buttonSave.setOnClickListener {
            val name = binding.editCategoryName.text?.toString()?.trim() ?: ""
            val minText = binding.editMinGoal.text?.toString()?.trim() ?: ""
            val maxText = binding.editMaxGoal.text?.toString()?.trim() ?: ""

            var valid = true

            if (name.isEmpty()) {
                binding.layoutCategoryName.error = "Category name is required."
                valid = false
            } else {
                binding.layoutCategoryName.error = null
            }

            val min = minText.toDoubleOrNull()
            if (min == null || min < 0.0) {
                binding.layoutMinGoal.error = "Enter a minimum amount of zero or more."
                valid = false
            } else {
                binding.layoutMinGoal.error = null
            }

            val max = maxText.toDoubleOrNull()
            if (max == null || max <= 0.0) {
                binding.layoutMaxGoal.error = "Enter a maximum amount greater than zero."
                valid = false
            } else {
                binding.layoutMaxGoal.error = null
            }

            if (valid && min != null && max != null && max < min) {
                binding.layoutMaxGoal.error = "Max must be greater than or equal to min."
                valid = false
            }

            if (!valid) return@setOnClickListener

            viewLifecycleOwner.lifecycleScope.launch {
                val categoryId = if (isEdit) {
                    repository.updateCategory(userId, Category(id = args.categoryId, name = name))
                    args.categoryId
                } else {
                    repository.insertCategory(userId, Category(name = name))
                }

                val goal = existingGoal
                if (goal != null) {
                    repository.updateBudgetGoal(userId, goal.copy(minAmount = min!!, maxAmount = max!!))
                } else {
                    repository.insertBudgetGoal(
                        userId,
                        BudgetGoal(
                            categoryId = categoryId,
                            minAmount = min!!,
                            maxAmount = max!!,
                            month = currentMonth,
                            year = currentYear
                        )
                    )
                }

                findNavController().navigateUp()
            }
        }

        binding.buttonDelete.setOnClickListener {
            androidx.appcompat.app.AlertDialog.Builder(requireContext())
                .setTitle("Delete category?")
                .setMessage("This cannot be undone.")
                .setNegativeButton(R.string.dialog_cancel, null)
                .setPositiveButton(R.string.dialog_delete) { _, _ ->
                    viewLifecycleOwner.lifecycleScope.launch {
                        existingCategory?.let { repository.deleteCategory(userId, it.id) }
                        findNavController().navigateUp()
                    }
                }
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
// 2. Google. 2026. Kotlin coroutines on Android. https://developer.android.com/kotlin/coroutines
