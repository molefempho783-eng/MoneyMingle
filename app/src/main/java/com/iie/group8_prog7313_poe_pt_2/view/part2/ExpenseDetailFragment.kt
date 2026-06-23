package com.iie.group8_prog7313_poe_pt_2.view.part2

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import androidx.navigation.ui.setupWithNavController
import coil.load
import com.google.android.material.snackbar.Snackbar
import com.iie.group8_prog7313_poe_pt_2.R
import com.iie.group8_prog7313_poe_pt_2.databinding.FragmentExpenseDetailBinding
import com.iie.group8_prog7313_poe_pt_2.util.DateTimeUtils
import com.iie.group8_prog7313_poe_pt_2.util.ReceiptFileHelper
import com.iie.group8_prog7313_poe_pt_2.viewmodel.ExpenseDetailViewModel
import kotlinx.coroutines.launch
import java.io.File
import java.text.NumberFormat
import java.util.Locale

class ExpenseDetailFragment : Fragment() {

    private var _binding: FragmentExpenseDetailBinding? = null
    private val binding get() = _binding!!

    private val args: ExpenseDetailFragmentArgs by navArgs()

    private val viewModel: ExpenseDetailViewModel by viewModels {
        object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                return ExpenseDetailViewModel(
                    requireActivity().application,
                    args.expenseId,
                ) as T
            }
        }
    }

    private val currencyFormat: NumberFormat =
        NumberFormat.getCurrencyInstance(Locale.forLanguageTag("en-ZA"))

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        _binding = FragmentExpenseDetailBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.toolbar.setupWithNavController(findNavController())
        binding.toolbar.title = getString(R.string.add_edit_expense_title_edit)

        binding.buttonEdit.setOnClickListener {
            val action = ExpenseDetailFragmentDirections.actionExpenseDetailFragmentToAddEditExpenseFragment2(
                expenseId = args.expenseId,
            ) // (Android Developers, 2026)
            findNavController().navigate(action)
        }

        binding.buttonDelete.setOnClickListener {
            AlertDialog.Builder(requireContext())
                .setTitle(R.string.expense_detail_delete_confirm_title)
                .setMessage(R.string.expense_detail_delete_confirm_message)
                .setNegativeButton(R.string.dialog_cancel, null)
                .setPositiveButton(R.string.dialog_delete) { _, _ ->
                    viewModel.deleteExpense { err ->
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
                viewModel.detail.collect { ui ->
                    if (ui == null) {
                        binding.scrollContent.visibility = View.GONE
                        binding.textMissing.visibility = View.VISIBLE
                        return@collect
                    }
                    binding.scrollContent.visibility = View.VISIBLE
                    binding.textMissing.visibility = View.GONE
                    val e = ui.expense
                    Log.d("ExpenseDetailFragment", "Displaying expense #${e.id}: ${currencyFormat.format(e.amount)} in '${ui.categoryName}'")
                    binding.textAmount.text = currencyFormat.format(e.amount)
                    binding.textDate.text = DateTimeUtils.formatDateShort(e.date)
                    binding.textCategory.text = ui.categoryName
                    binding.textDescription.text = e.description

                    val path = e.receiptImagePath
                    // The receipt path stored in the DB may refer to a file that no longer
                    // exists (e.g. the user cleared app storage or the file was deleted
                    // externally). We verify existence before attempting to load to avoid a
                    // crash or a broken image placeholder.
                    if (!path.isNullOrBlank() && ReceiptFileHelper.uriPointsToExistingFile(path)) {
                        binding.imageReceipt.visibility = View.VISIBLE
                        binding.imageReceipt.load(File(path)) // (Coil Contributors, 2026)
                    } else {
                        binding.imageReceipt.visibility = View.GONE
                    }
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        // Refresh the expense data every time this screen becomes visible. This covers the
        // case where the user navigates to the edit screen, modifies the expense, and then
        // presses Back without a refresh the detail screen would display stale data.
        viewModel.refresh()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}

/*
  Reference list :
  - Android Developers, 2026. Navigation with Safe Args [online]. Available at:
    <https://developer.android.com/guide/navigation/use-graph/safe-args> [Accessed 20 April 2026].
  - Coil Contributors, 2026. Coil image loading library documentation [online]. Available at:
    <https://coil-kt.github.io/coil/> [Accessed 20 April 2026].
 */
