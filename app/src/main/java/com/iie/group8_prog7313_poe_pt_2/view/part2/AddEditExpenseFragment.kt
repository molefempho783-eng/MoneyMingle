package com.iie.group8_prog7313_poe_pt_2.view.part2

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import coil.load
import com.google.android.material.datepicker.MaterialDatePicker
import com.google.android.material.snackbar.Snackbar
import com.iie.group8_prog7313_poe_pt_2.R
import com.iie.group8_prog7313_poe_pt_2.databinding.FragmentAddEditExpenseBinding
import com.iie.group8_prog7313_poe_pt_2.model.entity.Category
import com.iie.group8_prog7313_poe_pt_2.model.entity.Expense
import com.iie.group8_prog7313_poe_pt_2.session.SessionManager
import com.iie.group8_prog7313_poe_pt_2.util.DateTimeUtils
import com.iie.group8_prog7313_poe_pt_2.view.MainActivity
import com.iie.group8_prog7313_poe_pt_2.util.ReceiptFileHelper
import com.iie.group8_prog7313_poe_pt_2.viewmodel.AddEditExpenseViewModel
import com.iie.group8_prog7313_poe_pt_2.viewmodel.ExpenseFormValidation
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch
import java.io.File

class AddEditExpenseFragment : Fragment() {

    private var _binding: FragmentAddEditExpenseBinding? = null
    private val binding get() = _binding!!

    private val args: AddEditExpenseFragmentArgs by navArgs()

    private val viewModel: AddEditExpenseViewModel by viewModels {
        object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                return AddEditExpenseViewModel(
                    requireActivity().application,
                    args.expenseId,
                ) as T
            }
        }
    }

    private var selectedDateMillis: Long = System.currentTimeMillis()
    private var pendingReceiptPath: String? = null
    private var categoriesList: List<Category> = emptyList()
    private var formInitialized = false
    private var selectedCategoryId: String? = null

    private var pendingCameraFile: File? = null

    private val takePicture = registerForActivityResult(ActivityResultContracts.TakePicture()) { ok ->
        val file = pendingCameraFile
        pendingCameraFile = null
        if (ok && file != null) {
            pendingReceiptPath = file.absolutePath
        } else {
            file?.delete()
        }
        updateReceiptUi()
    }

    private val pickGallery = registerForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        if (uri != null) {
            pendingReceiptPath = ReceiptFileHelper.copyContentUriToReceiptFile(requireContext(), uri)
        }
        updateReceiptUi()
    }

    private val requestCameraPermission =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
            if (granted) launchCameraIntent()
        }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        _binding = FragmentAddEditExpenseBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.toolbar.setNavigationOnClickListener { findNavController().navigateUp() }
        binding.toolbar.title = getString(
            if (args.expenseId.isNotEmpty()) R.string.add_edit_expense_title_edit else R.string.add_edit_expense_title_new,
        )

        binding.textSelectedDate.setText(DateTimeUtils.formatDateShort(selectedDateMillis))

        binding.autoCompleteCategory.threshold = Int.MAX_VALUE
        binding.autoCompleteCategory.setOnItemClickListener { parent, _, position, _ ->
            val name = parent.getItemAtPosition(position) as CharSequence
            selectedCategoryId = categoriesList.find { it.name == name.toString() }?.id
        }

        binding.buttonPickDate.setOnClickListener { showDatePicker() }
        binding.layoutDate.setEndIconOnClickListener { showDatePicker() }
        binding.textSelectedDate.setOnClickListener { showDatePicker() }

        binding.buttonCamera.setOnClickListener {
            when {
                ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.CAMERA) ==
                        PackageManager.PERMISSION_GRANTED -> launchCameraIntent()
                else -> requestCameraPermission.launch(Manifest.permission.CAMERA)
            }
        }
        binding.buttonGallery.setOnClickListener {
            pickGallery.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
        }
        binding.buttonRemoveReceipt.setOnClickListener {
            pendingReceiptPath = null
            updateReceiptUi()
        }

        binding.buttonSave.setOnClickListener { attemptSave() }

        binding.buttonManageCategories.setOnClickListener {
            val action = AddEditExpenseFragmentDirections.actionAddEditExpenseFragmentToCategoriesFragment()
            findNavController().navigate(action)
        }

        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                // Route badge events to the Activity-level ViewModel so the
                // notification persists even after this fragment navigates away.
                launch {
                    viewModel.achievementEvents.collect { badgeKey ->
                        (activity as? MainActivity)?.sharedAwardViewModel?.postAward(badgeKey)
                    }
                }

                launch {
                    combine(viewModel.categories, viewModel.existingExpense) { cats, exp -> cats to exp }
                        .collect { (cats, exp) ->
                            categoriesList = cats
                            val labels = cats.map { it.name }
                            val adapter = ArrayAdapter(
                                requireContext(),
                                android.R.layout.simple_spinner_dropdown_item,
                                labels,
                            )
                            adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
                            binding.autoCompleteCategory.setAdapter(adapter)

                            if (exp != null && !formInitialized && cats.isNotEmpty()) {
                                bindExistingExpense(exp, cats)
                            }
                        }
                }
            }
        }
    }

    private fun bindExistingExpense(exp: Expense, cats: List<Category>) {
        formInitialized = true
        binding.editAmount.setText(formatAmountForEdit(exp.amount))
        binding.editDescription.setText(exp.description)
        selectedDateMillis = exp.date
        binding.textSelectedDate.setText(DateTimeUtils.formatDateShort(selectedDateMillis))
        pendingReceiptPath = exp.receiptImagePath
        val cat = cats.find { it.id == exp.categoryId }
        if (cat != null) {
            selectedCategoryId = cat.id
            binding.autoCompleteCategory.setText(cat.name, false)
        } else {
            selectedCategoryId = null
            binding.autoCompleteCategory.setText("", false)
        }
        updateReceiptUi()
    }

    private fun showDatePicker() {
        val picker = MaterialDatePicker.Builder.datePicker()
            .setTitleText(getString(R.string.expense_select_date_title))
            .setSelection(selectedDateMillis.coerceAtLeast(0L))
            .build()
        picker.addOnPositiveButtonClickListener { utcMillis ->
            selectedDateMillis = DateTimeUtils.startOfDayMillis(utcMillis)
            binding.textSelectedDate.setText(DateTimeUtils.formatDateShort(selectedDateMillis))
        }
        picker.show(childFragmentManager, "expense_date")
    }

    private fun launchCameraIntent() {
        val file = ReceiptFileHelper.createCameraDestinationFile(requireContext())
        pendingCameraFile = file
        val uri = FileProvider.getUriForFile(
            requireContext(),
            "${requireContext().packageName}.fileprovider",
            file,
        )
        takePicture.launch(uri)
    }

    private fun updateReceiptUi() {
        val path = pendingReceiptPath
        if (path.isNullOrBlank()) {
            binding.textReceiptStatus.setText(R.string.expense_photo_none)
            binding.buttonRemoveReceipt.visibility = View.GONE
            binding.imageReceiptPreview.visibility = View.GONE
            return
        }
        binding.textReceiptStatus.text = path.substringAfterLast('/')
        binding.buttonRemoveReceipt.visibility = View.VISIBLE
        val f = File(path)
        if (f.exists()) {
            binding.imageReceiptPreview.visibility = View.VISIBLE
            binding.imageReceiptPreview.load(f)
        } else {
            binding.imageReceiptPreview.visibility = View.GONE
        }
    }

    private fun resolvedCategoryId(): String? {
        selectedCategoryId?.let { return it }
        val text = binding.autoCompleteCategory.text?.toString()?.trim().orEmpty()
        if (text.isEmpty()) return null
        return categoriesList.find { it.name == text }?.id
    }

    private fun attemptSave() {
        val amountText = binding.editAmount.text?.toString().orEmpty()
        val description = binding.editDescription.text?.toString().orEmpty()
        val catId = resolvedCategoryId()
        val validation = viewModel.validate(
            amountText = amountText,
            description = description,
            categoryId = catId,
            dateMillis = selectedDateMillis,
            categoriesNotEmpty = categoriesList.isNotEmpty(),
        )
        when (validation) {
            is ExpenseFormValidation.Error -> {
                Snackbar.make(binding.root, validation.messageRes, Snackbar.LENGTH_LONG).show()
            }
            ExpenseFormValidation.Ok -> {
                val amount = amountText.trim().replace(",", ".").toDoubleOrNull() ?: return
                Log.d("AddEditExpenseFragment", "Saving expense: amount=$amount, categoryId=$catId")
                viewModel.save(
                    amount = amount,
                    description = description,
                    categoryId = catId!!,
                    dateMillis = selectedDateMillis,
                    receiptPath = pendingReceiptPath,
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

    private fun formatAmountForEdit(value: Double): String {
        return if (value % 1.0 == 0.0) value.toInt().toString() else value.toString()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}