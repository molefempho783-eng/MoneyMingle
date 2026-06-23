package com.iie.group8_prog7313_poe_pt_2.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.iie.group8_prog7313_poe_pt_2.R
import com.iie.group8_prog7313_poe_pt_2.model.entity.Category
import com.iie.group8_prog7313_poe_pt_2.model.entity.Expense
import com.iie.group8_prog7313_poe_pt_2.model.repository.CategoryRepository
import com.iie.group8_prog7313_poe_pt_2.model.repository.ExpenseRepository
import com.iie.group8_prog7313_poe_pt_2.session.SessionManager
import com.iie.group8_prog7313_poe_pt_2.util.ReceiptFileHelper
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import com.iie.group8_prog7313_poe_pt_2.model.repository.GamificationRepository

sealed interface ExpenseFormValidation {
    data object Ok : ExpenseFormValidation
    data class Error(val messageRes: Int) : ExpenseFormValidation
}

class AddEditExpenseViewModel(
    application: Application,
    private val expenseIdArg: String,
) : AndroidViewModel(application) {

    //GAMIFICATION
    private val gamificationRepository = GamificationRepository()

    private val _achievementEvents =
        MutableSharedFlow<String>()

    val achievementEvents =
        _achievementEvents.asSharedFlow()

    private val expenseRepository = ExpenseRepository()
    private val categoryRepository = CategoryRepository()

    private val userId: String = SessionManager(application).getUserId() ?: ""

    private val _existingExpense = MutableStateFlow<Expense?>(null)
    val existingExpense: StateFlow<Expense?> = _existingExpense.asStateFlow()

    val categories: StateFlow<List<Category>> =
        categoryRepository.getAllCategoriesByUser(userId).stateIn(
            viewModelScope,
            SharingStarted.Eagerly,
            emptyList(),
        )

    init {
        if (expenseIdArg.isNotEmpty()) {
            viewModelScope.launch {
                _existingExpense.value = expenseRepository.getById(userId, expenseIdArg)
            }
        }
    }

    fun validate(
        amountText: String,
        description: String,
        categoryId: String?,
        dateMillis: Long,
        categoriesNotEmpty: Boolean,
    ): ExpenseFormValidation {
        if (!categoriesNotEmpty) {
            return ExpenseFormValidation.Error(R.string.error_expense_no_categories)
        }
        val amount = amountText.trim().replace(",", ".").toDoubleOrNull()
        if (amount == null || amount <= 0) {
            return ExpenseFormValidation.Error(R.string.error_expense_amount)
        }
        if (description.isBlank()) {
            return ExpenseFormValidation.Error(R.string.error_expense_description)
        }
        if (categoryId == null) {
            return ExpenseFormValidation.Error(R.string.error_expense_category_required)
        }
        if (dateMillis <= 0L) {
            return ExpenseFormValidation.Error(R.string.error_expense_date)
        }
        return ExpenseFormValidation.Ok
    }

    fun save(
        amount: Double,
        description: String,
        categoryId: String,
        dateMillis: Long,
        receiptPath: String?,
        onFinished: (Throwable?) -> Unit,
    ) {
        viewModelScope.launch {
            try {
                if (expenseIdArg.isNotEmpty()) {
                    val existing = expenseRepository.getById(userId, expenseIdArg)
                    if (existing == null) {
                        onFinished(IllegalStateException("expense missing"))
                        return@launch
                    }
                    val oldPath = existing.receiptImagePath
                    if (oldPath != null && oldPath != receiptPath) {
                        ReceiptFileHelper.deleteFileIfExists(oldPath)
                    }
                    expenseRepository.update(
                        userId,
                        existing.copy(
                            amount = amount,
                            description = description.trim(),
                            categoryId = categoryId,
                            date = dateMillis,
                            receiptImagePath = receiptPath,
                        ),
                    )
                } else {
                    expenseRepository.insert(
                        userId,
                        Expense(
                            categoryId = categoryId,
                            amount = amount,
                            description = description.trim(),
                            date = dateMillis,
                            receiptImagePath = receiptPath,
                        ),
                    )
                    val unlockedBadges =
                        gamificationRepository.updateAfterExpense(
                            userId = userId,
                            hasReceipt = receiptPath != null
                        )
                    unlockedBadges.forEach { badge ->
                        _achievementEvents.emit(badge)
                    }
                }
                onFinished(null)
            } catch (t: Throwable) {
                onFinished(t)
            }
        }
    }
}
