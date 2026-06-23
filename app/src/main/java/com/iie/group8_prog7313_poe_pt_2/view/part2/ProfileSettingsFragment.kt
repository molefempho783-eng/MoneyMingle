package com.iie.group8_prog7313_poe_pt_2.view.part2

import android.os.Bundle
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.navigation.fragment.findNavController
import com.google.android.material.button.MaterialButton
import com.iie.group8_prog7313_poe_pt_2.R
import com.iie.group8_prog7313_poe_pt_2.model.repository.AuthRepository
import com.iie.group8_prog7313_poe_pt_2.session.SessionManager

class ProfileSettingsFragment : Fragment() {

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_profile_settings, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        view.findViewById<MaterialButton>(R.id.buttonLogout).setOnClickListener {
            Log.d("ProfileSettingsFragment", "User logged out")
            AuthRepository().logout()
            SessionManager(requireContext()).clearSession()
            findNavController().navigate(R.id.action_profileSettingsFragment_to_splashFragment)
        }
    }
}
