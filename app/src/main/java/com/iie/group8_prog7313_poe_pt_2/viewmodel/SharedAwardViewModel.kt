package com.iie.group8_prog7313_poe_pt_2.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.launch

/**
 * Activity-scoped ViewModel that bridges badge events from any fragment
 * to a single notification point in MainActivity. This ensures awards
 * are shown regardless of which tab the user is on.
 */
class SharedAwardViewModel : ViewModel() {

    private val _awardEvent = MutableSharedFlow<String>()
    val awardEvent: SharedFlow<String> = _awardEvent.asSharedFlow()

    fun postAward(badgeKey: String) {
        viewModelScope.launch {
            _awardEvent.emit(badgeKey)
        }
    }
}
