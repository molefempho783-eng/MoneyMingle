package com.iie.group8_prog7313_poe_pt_2.view.part3

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.textfield.TextInputLayout
import com.iie.group8_prog7313_poe_pt_2.R
import com.iie.group8_prog7313_poe_pt_2.databinding.FragmentWishlistBinding
import com.iie.group8_prog7313_poe_pt_2.model.repository.SavingsGoalRepository
import com.iie.group8_prog7313_poe_pt_2.session.SessionManager
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

class WishlistFragment : Fragment() {

    private var _binding: FragmentWishlistBinding? = null
    private val binding get() = _binding!!

    private val repository = SavingsGoalRepository()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentWishlistBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val userId = SessionManager(requireContext()).getUserId() ?: ""

        val adapter = WishlistAdapter(
            onGoalClick = { goal ->
                findNavController().navigate(
                    WishlistFragmentDirections.actionWishlistFragmentToAddEditWishlistItemFragment(goal.id)
                )
            },
            onAddContribution = { goal -> showContributionDialog(userId, goal.id) }
        )

        binding.recyclerGoals.apply {
            layoutManager = LinearLayoutManager(requireContext())
            this.adapter = adapter
        }

        binding.addGoalButton.setOnClickListener {
            findNavController().navigate(
                WishlistFragmentDirections.actionWishlistFragmentToAddEditWishlistItemFragment()
            )
        }

        viewLifecycleOwner.lifecycleScope.launch {
            repository.getAllByUser(userId).collectLatest { goals ->
                adapter.submitList(goals)
                val empty = goals.isEmpty()
                binding.textEmpty.visibility = if (empty) View.VISIBLE else View.GONE
                binding.recyclerGoals.visibility = if (empty) View.GONE else View.VISIBLE
            }
        }
    }

    private fun showContributionDialog(userId: String, goalId: String) {
        val inputLayout = TextInputLayout(requireContext()).apply {
            hint = getString(R.string.wishlist_contribution_amount)
            setPadding(48, 16, 48, 0)
        }
        val editText = EditText(requireContext()).apply {
            inputType = android.text.InputType.TYPE_CLASS_NUMBER or
                android.text.InputType.TYPE_NUMBER_FLAG_DECIMAL
        }
        inputLayout.addView(editText)

        MaterialAlertDialogBuilder(requireContext())
            .setTitle(R.string.wishlist_contribution_title)
            .setView(inputLayout)
            .setNegativeButton(R.string.dialog_cancel, null)
            .setPositiveButton(R.string.wishlist_contribution_add) { _, _ ->
                val amount = editText.text?.toString()?.trim()?.toDoubleOrNull()
                if (amount == null || amount <= 0.0) {
                    inputLayout.error = getString(R.string.wishlist_error_contribution)
                    return@setPositiveButton
                }
                viewLifecycleOwner.lifecycleScope.launch {
                    repository.addContribution(userId, goalId, amount)
                }
            }
            .show()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
