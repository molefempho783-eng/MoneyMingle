package com.iie.group8_prog7313_poe_pt_2.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel

class AchievementAwareViewModel : ViewModel() {

    private val _newBadge = MutableLiveData<String?>()
    val newBadge: LiveData<String?> = _newBadge

    private val _showLevelUp = MutableLiveData<Int?>()
    val showLevelUp: LiveData<Int?> = _showLevelUp

    private val _newBadgesList = MutableLiveData<List<String>>(emptyList())
    val newBadgesList: LiveData<List<String>> = _newBadgesList

    fun notifyBadgeEarned(badgeKey: String) {
        _newBadge.value = badgeKey
    }

    fun notifyMultipleBadgesEarned(badgeKeys: List<String>) {
        _newBadgesList.value = badgeKeys
        if (badgeKeys.isNotEmpty()) {
            _newBadge.value = badgeKeys.first()
        }
    }

    fun notifyLevelUp(level: Int) {
        _showLevelUp.value = level
    }

    fun clearBadge() {
        _newBadge.value = null
        _newBadgesList.value = emptyList()
    }

    fun clearLevelUp() {
        _showLevelUp.value = null
    }
}