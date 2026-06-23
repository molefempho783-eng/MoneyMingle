package com.iie.group8_prog7313_poe_pt_2.view.part2

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.google.android.material.button.MaterialButton
import com.google.firebase.auth.FirebaseAuth
import com.iie.group8_prog7313_poe_pt_2.R
import com.iie.group8_prog7313_poe_pt_2.session.SessionManager

class SplashFragment : Fragment() {

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_splash, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val sessionManager = SessionManager(requireContext())
        val firebaseUser = FirebaseAuth.getInstance().currentUser

        // FirebaseAuth is the source of truth. If Firebase has a live token we sync it into
        // SessionManager (handles the reinstall case) and go straight to the dashboard.
        if (firebaseUser != null) {
            sessionManager.saveUserId(firebaseUser.uid)
            Log.d("SplashFragment", "Active Firebase session found - going to dashboard")
            findNavController().navigate(R.id.action_splashFragment_to_dashboardHomeFragment)
            return
        }

        Log.d("SplashFragment", "No active session - showing splash screen")

        view.findViewById<MaterialButton>(R.id.buttonGetStarted).setOnClickListener {
            findNavController().navigate(R.id.action_splashFragment_to_registerFragment)
        }

        view.findViewById<MaterialButton>(R.id.buttonLogin).setOnClickListener {
            findNavController().navigate(R.id.action_splashFragment_to_loginFragment)
        }
    }
}
