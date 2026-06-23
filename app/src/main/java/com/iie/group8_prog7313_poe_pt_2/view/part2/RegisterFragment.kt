package com.iie.group8_prog7313_poe_pt_2.view.part2

import android.os.Bundle
import android.util.Patterns
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.text.SpannableString
import android.text.Spanned
import android.text.style.UnderlineSpan
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import com.iie.group8_prog7313_poe_pt_2.R
import com.iie.group8_prog7313_poe_pt_2.databinding.FragmentRegisterBinding
import com.iie.group8_prog7313_poe_pt_2.viewmodel.AuthViewModel
import com.iie.group8_prog7313_poe_pt_2.viewmodel.RegisterResult

class RegisterFragment : Fragment() {

    private var _binding: FragmentRegisterBinding? = null
    private val binding get() = _binding!!

    private val viewModel: AuthViewModel by viewModels()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentRegisterBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val signInText = "Already have an account? Sign In"
        val spannable = SpannableString(signInText)
        spannable.setSpan(UnderlineSpan(), signInText.indexOf("Sign In"), signInText.length, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
        binding.textSignIn.text = spannable

        binding.textSignIn.setOnClickListener {
            findNavController().navigate(R.id.action_registerFragment_to_loginFragment)
        }

        binding.buttonRegister.setOnClickListener {
            attemptRegister()
        }

        viewModel.registerResult.observe(viewLifecycleOwner) { result ->
            when (result) {
                is RegisterResult.Success -> {
                    findNavController().navigate(R.id.action_registerFragment_to_dashboardHomeFragment)
                }
                is RegisterResult.EmailTaken -> {
                    binding.layoutUsername.error = "An account with this email already exists"
                }
                is RegisterResult.Failure -> {
                    binding.layoutUsername.error = result.message
                }
            }
        }
    }

    private fun attemptRegister() {
        val email    = binding.editUsername.text.toString().trim()
        val password = binding.editPassword.text.toString()
        val confirm  = binding.editConfirmPassword.text.toString()

        binding.layoutUsername.error = null
        binding.layoutPassword.error = null
        binding.layoutConfirmPassword.error = null

        if (email.isBlank()) {
            binding.layoutUsername.error = "Email is required"
            return
        }
        if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            binding.layoutUsername.error = "Enter a valid email address"
            return
        }
        if (password.isBlank()) {
            binding.layoutPassword.error = "Password is required"
            return
        }
        if (password.length < 6) {
            binding.layoutPassword.error = "Password must be at least 6 characters"
            return
        }
        if (password != confirm) {
            binding.layoutConfirmPassword.error = "Passwords do not match"
            return
        }

        viewModel.register(email, password)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
