// MM-25: Categories Screen

package com.iie.group8_prog7313_poe_pt_2.view.part2

import android.os.Bundle
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import java.util.Calendar
import com.iie.group8_prog7313_poe_pt_2.R
import com.iie.group8_prog7313_poe_pt_2.databinding.FragmentCategoriesBinding
import com.iie.group8_prog7313_poe_pt_2.model.repository.CategoryRepository
import com.iie.group8_prog7313_poe_pt_2.session.SessionManager

class CategoriesFragment : Fragment() {
    private var _binding: FragmentCategoriesBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentCategoriesBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val repository = CategoryRepository()
        val adapter = CategoryAdapter { category ->
            findNavController().navigate(
                CategoriesFragmentDirections.actionCategoriesFragmentToAddEditCategoryFragment(category.id)
            )
        }

        binding.categories.apply {
            layoutManager = LinearLayoutManager(requireContext())
            this.adapter = adapter
        }

        binding.addCategoryButton.setOnClickListener {
            findNavController().navigate(
                CategoriesFragmentDirections.actionCategoriesFragmentToAddEditCategoryFragment()
            )
        }

        val userId = SessionManager(requireContext()).getUserId() ?: ""
        val now = Calendar.getInstance()
        val currentMonth = now.get(Calendar.MONTH) + 1
        val currentYear = now.get(Calendar.YEAR)

        viewLifecycleOwner.lifecycleScope.launch {
            repository.getAllCategoriesByUser(userId).collectLatest { list ->
                Log.d("CategoriesFragment", "Loaded ${list.size} categories for user $userId")
                adapter.submitList(list)
            }
        }

        viewLifecycleOwner.lifecycleScope.launch {
            repository.getBudgetGoalsByMonth(userId, currentMonth, currentYear).collectLatest { goals ->
                adapter.goals = goals.associateBy { it.categoryId }
                adapter.notifyDataSetChanged()
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}

// References:
// 1. Google. 2026. Fragment lifecycle. https://developer.android.com/guide/fragments/lifecycle
// 2. Google. 2026. Kotlin flows on Android. https://developer.android.com/kotlin/flow
