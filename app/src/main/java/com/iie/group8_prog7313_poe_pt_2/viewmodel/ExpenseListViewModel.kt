package com.iie.group8_prog7313_poe_pt_2.viewmodel

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.iie.group8_prog7313_poe_pt_2.R
import com.iie.group8_prog7313_poe_pt_2.model.entity.Expense
import com.iie.group8_prog7313_poe_pt_2.model.repository.CategoryRepository
import com.iie.group8_prog7313_poe_pt_2.model.repository.ExpenseRepository
import com.iie.group8_prog7313_poe_pt_2.session.SessionManager
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.stateIn

data class ExpenseListItem(
    val expense: Expense,
    val categoryName: String,
)

@OptIn(ExperimentalCoroutinesApi::class)
class ExpenseListViewModel(application: Application) : AndroidViewModel(application) {

    private val expenseRepository = ExpenseRepository()
    private val categoryRepository = CategoryRepository()

    private val userId: String = SessionManager(application).getUserId() ?: ""

    private val rangeStartMillis = MutableStateFlow(0L)
    private val rangeEndMillis = MutableStateFlow(Long.MAX_VALUE)

    val filterStartMillis: StateFlow<Long> = rangeStartMillis.asStateFlow()
    val filterEndMillis: StateFlow<Long> = rangeEndMillis.asStateFlow()

    private val expensesInRange = combine(rangeStartMillis, rangeEndMillis) { s, e -> s to e }
        .flatMapLatest { (start, end) ->
            expenseRepository.getByDateRange(userId, start, end)
        }

    val items: StateFlow<List<ExpenseListItem>> = combine(
        expensesInRange,
        categoryRepository.getAllCategoriesByUser(userId),
    ) { expenses, categories ->
        val byId = categories.associateBy { it.id }
        val uncategorised = getApplication<Application>().getString(R.string.expense_category_uncategorised)
        expenses.map { e ->
            ExpenseListItem(
                expense = e,
                categoryName = e.categoryId?.let { cid -> byId[cid]?.name } ?: uncategorised,
            )
        }
    }.onEach { rows ->
        Log.d(TAG, "Expense list updated: ${rows.size} row(s) for userId=$userId")
    }.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5_000),
        emptyList(),
    )

    fun setDateRangeStartInclusive(millis: Long) {
        rangeStartMillis.value = millis
    }

    fun setDateRangeEndInclusive(millis: Long) {
        rangeEndMillis.value = millis
    }

    fun clearDateFilter() {
        rangeStartMillis.value = 0L
        rangeEndMillis.value = Long.MAX_VALUE
    }

    private companion object {
        private const val TAG = "ExpenseListViewModel"
    }
}
