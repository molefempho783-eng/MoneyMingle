package com.iie.group8_prog7313_poe_pt_2.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.FirebaseAuthUserCollisionException
import com.iie.group8_prog7313_poe_pt_2.model.repository.AuthRepository
import com.iie.group8_prog7313_poe_pt_2.session.SessionManager
import kotlinx.coroutines.launch

sealed class RegisterResult {
    object Success : RegisterResult()
    object EmailTaken : RegisterResult()
    data class Failure(val message: String) : RegisterResult()
}

class AuthViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = AuthRepository()
    private val sessionManager = SessionManager(application)

    private val _registerResult = MutableLiveData<RegisterResult>()
    val registerResult: LiveData<RegisterResult> = _registerResult

    fun register(email: String, password: String) {
        viewModelScope.launch {
            try {
                val user = repository.register(email, password)
                sessionManager.saveUserId(user.uid)
                _registerResult.postValue(RegisterResult.Success)
            } catch (e: FirebaseAuthUserCollisionException) {
                _registerResult.postValue(RegisterResult.EmailTaken)
            } catch (e: Exception) {
                _registerResult.postValue(RegisterResult.Failure(e.message ?: "Registration failed"))
            }
        }
    }
}
