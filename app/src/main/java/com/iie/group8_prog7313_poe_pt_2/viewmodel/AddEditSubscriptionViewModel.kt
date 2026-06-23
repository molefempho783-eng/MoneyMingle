package com.iie.group8_prog7313_poe_pt_2.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.iie.group8_prog7313_poe_pt_2.R
import com.iie.group8_prog7313_poe_pt_2.model.entity.Subscription
import com.iie.group8_prog7313_poe_pt_2.model.repository.SubscriptionRepository
import com.iie.group8_prog7313_poe_pt_2.session.SessionManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

sealed interface SubscriptionFormValidation {
    data object Ok : SubscriptionFormValidation
    data class Error(val messageRes: Int) : SubscriptionFormValidation
}

class AddEditSubscriptionViewModel(
    application: Application,
    private val subscriptionIdArg: String,
) : AndroidViewModel(application) {

    private val repository = SubscriptionRepository()
    private val userId: String = SessionManager(application).getUserId() ?: ""

    private val _existing = MutableStateFlow<Subscription?>(null)
    val existing: StateFlow<Subscription?> = _existing.asStateFlow()

    init {
        if (subscriptionIdArg.isNotEmpty()) {
            viewModelScope.launch {
                _existing.value = repository.getById(userId, subscriptionIdArg)
            }
        }
    }

    fun validate(name: String, amountText: String): SubscriptionFormValidation {
        if (name.isBlank()) {
            return SubscriptionFormValidation.Error(R.string.error_subscription_name)
        }
        val amount = amountText.trim().replace(",", ".").toDoubleOrNull()
        if (amount == null || amount <= 0) {
            return SubscriptionFormValidation.Error(R.string.error_subscription_amount)
        }
        return SubscriptionFormValidation.Ok
    }
    fun save(
        name: String,
        amount: Double,
        billingCycle: String,
        startDate: Long,
        onFinished: (Throwable?) -> Unit,
    ) {
        viewModelScope.launch {
            try {
                if (subscriptionIdArg.isNotEmpty()) {
                    val existing = repository.getById(userId, subscriptionIdArg)
                        ?: run { onFinished(IllegalStateException("subscription missing")); return@launch }
                    repository.update(
                        userId,
                        existing.copy(
                            name = name,
                            amount = amount,
                            billingCycle = billingCycle,
                            startDate = startDate,
                        ),
                    )
                } else {
                    repository.insert(
                        userId,
                        Subscription(
                            name = name,
                            amount = amount,
                            billingCycle = billingCycle,
                            startDate = startDate,
                        ),
                    )
                }
                onFinished(null)
            } catch (t: Throwable) {
                onFinished(t)
            }
        }
    }

    fun delete(onFinished: (Throwable?) -> Unit) {
        viewModelScope.launch {
            try {
                repository.delete(userId, subscriptionIdArg)
                onFinished(null)
            } catch (t: Throwable) {
                onFinished(t)
            }
        }
    }
}