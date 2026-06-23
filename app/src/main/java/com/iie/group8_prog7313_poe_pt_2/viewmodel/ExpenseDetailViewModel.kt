package com.iie.group8_prog7313_poe_pt_2.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.iie.group8_prog7313_poe_pt_2.R
import com.iie.group8_prog7313_poe_pt_2.model.entity.Expense
import com.iie.group8_prog7313_poe_pt_2.model.repository.CategoryRepository
import com.iie.group8_prog7313_poe_pt_2.model.repository.ExpenseRepository
import com.iie.group8_prog7313_poe_pt_2.session.SessionManager
import com.iie.group8_prog7313_poe_pt_2.util.ReceiptFileHelper
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class ExpenseDetailUi(
    val expense: Expense,
    val categoryName: String,
)

class ExpenseDetailViewModel(
    application: Application,
    private val expenseId: String,
) : AndroidViewModel(application) {

    private val expenseRepository = ExpenseRepository()
    private val categoryRepository = CategoryRepository()

    private val userId: String = SessionManager(application).getUserId() ?: ""

    private val _detail = MutableStateFlow<ExpenseDetailUi?>(null)
    val detail: StateFlow<ExpenseDetailUi?> = _detail.asStateFlow()

    init {
        refresh()
    }

    fun refresh() {
        viewModelScope.launch {
            val exp = expenseRepository.getById(userId, expenseId) ?: run {
                _detail.value = null
                return@launch
            }
            val uncategorised = getApplication<Application>().getString(R.string.expense_category_uncategorised)
            val name = exp.categoryId?.let { id ->
                categoryRepository.getCategoryById(userId, id)?.name
            } ?: uncategorised
            _detail.value = ExpenseDetailUi(expense = exp, categoryName = name)
        }
    }

    fun deleteExpense(onDone: (Throwable?) -> Unit) {
        viewModelScope.launch {
            try {
                val exp = expenseRepository.getById(userId, expenseId) ?: run {
                    onDone(null)
                    return@launch
                }
                ReceiptFileHelper.deleteFileIfExists(exp.receiptImagePath)
                expenseRepository.delete(userId, exp)
                onDone(null)
            } catch (t: Throwable) {
                onDone(t)
            }
        }
    }
}
