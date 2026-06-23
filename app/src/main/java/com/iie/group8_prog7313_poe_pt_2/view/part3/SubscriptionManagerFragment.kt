package com.iie.group8_prog7313_poe_pt_2.view.part3

import android.os.Bundle
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
import com.iie.group8_prog7313_poe_pt_2.databinding.FragmentSubscriptionManagerBinding
import com.iie.group8_prog7313_poe_pt_2.viewmodel.SubscriptionManagerViewModel
import kotlinx.coroutines.launch
import java.text.NumberFormat
import java.util.Locale

class SubscriptionManagerFragment : Fragment() {

    private var _binding: FragmentSubscriptionManagerBinding? = null
    private val binding get() = _binding!!

    private val viewModel: SubscriptionManagerViewModel by viewModels()

    private val currencyFormat: NumberFormat =
        NumberFormat.getCurrencyInstance(Locale.forLanguageTag("en-ZA"))

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        _binding = FragmentSubscriptionManagerBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val adapter = SubscriptionAdapter { subscription ->
            val action = SubscriptionManagerFragmentDirections
                .actionSubscriptionManagerFragmentToAddEditSubscriptionFragment(
                    subscriptionId = subscription.id,
                )
            findNavController().navigate(action)
        }

        binding.recyclerSubscriptions.layoutManager = LinearLayoutManager(requireContext())
        binding.recyclerSubscriptions.adapter = adapter

        binding.fabAddSubscription.setOnClickListener {
            val action = SubscriptionManagerFragmentDirections
                .actionSubscriptionManagerFragmentToAddEditSubscriptionFragment(
                    subscriptionId = "",
                )
            findNavController().navigate(action)
        }

        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch {
                    viewModel.subscriptions.collect { list ->
                        adapter.submitList(list)
                        val empty = list.isEmpty()
                        binding.textEmpty.visibility = if (empty) View.VISIBLE else View.GONE
                        binding.recyclerSubscriptions.visibility =
                            if (empty) View.INVISIBLE else View.VISIBLE
                        binding.textSubscriptionCount.text = list.size.toString()
                    }
                }
                launch {
                    viewModel.monthlyCost.collect { total ->
                        binding.textMonthlyTotal.text = currencyFormat.format(total)
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