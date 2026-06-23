package com.iie.group8_prog7313_poe_pt_2.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.iie.group8_prog7313_poe_pt_2.model.entity.Subscription
import com.iie.group8_prog7313_poe_pt_2.model.repository.SubscriptionRepository
import com.iie.group8_prog7313_poe_pt_2.session.SessionManager
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

class SubscriptionManagerViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = SubscriptionRepository()
    private val userId: String = SessionManager(application).getUserId() ?: ""

    val subscriptions: StateFlow<List<Subscription>> =
        repository.getAllByUser(userId).stateIn(
            viewModelScope,
            SharingStarted.WhileSubscribed(5_000),
            emptyList(),
        )

    // MM-218: convert every subscription to a monthly cost, then sum them
    val monthlyCost: StateFlow<Double> =
        repository.getAllByUser(userId).map { list ->
            list.sumOf { sub ->
                when (sub.billingCycle) {
                    "weekly" -> sub.amount * 52.0 / 12.0
                    "yearly" -> sub.amount / 12.0
                    else -> sub.amount
                }
            }
        }.stateIn(
            viewModelScope,
            SharingStarted.WhileSubscribed(5_000),
            0.0,
        )
}