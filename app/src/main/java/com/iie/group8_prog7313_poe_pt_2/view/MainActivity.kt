package com.iie.group8_prog7313_poe_pt_2.view

import android.os.Bundle
import android.util.Log
import android.view.View
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.ui.setupWithNavController
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.android.material.snackbar.Snackbar
import com.iie.group8_prog7313_poe_pt_2.R
import com.iie.group8_prog7313_poe_pt_2.util.BadgeIconMapper
import com.iie.group8_prog7313_poe_pt_2.viewmodel.SharedAwardViewModel
import kotlinx.coroutines.launch

class MainActivity : AppCompatActivity() {

    private val authDestinations = setOf(
        R.id.splashFragment,
        R.id.loginFragment,
        R.id.registerFragment
    )

    // Activity-scoped so all fragments can post to it
    val sharedAwardViewModel: SharedAwardViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_main)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        val navHost = supportFragmentManager
            .findFragmentById(R.id.navHostFragmentContainerView) as NavHostFragment
        val navController = navHost.navController
        val bottomNav = findViewById<BottomNavigationView>(R.id.bottomNav)

        bottomNav.setupWithNavController(navController)

        ViewCompat.setOnApplyWindowInsetsListener(bottomNav) { _, insets ->
            WindowInsetsCompat.CONSUMED
        }

        navController.addOnDestinationChangedListener { _, destination, _ ->
            Log.d("MainActivity", "Navigated to: ${destination.label}")
            bottomNav.visibility = if (destination.id in authDestinations) {
                View.GONE
            } else {
                View.VISIBLE
            }
        }

        // Show award notifications from any tab via the root content view
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                sharedAwardViewModel.awardEvent.collect { badgeKey ->
                    showGlobalAwardNotification(badgeKey)
                }
            }
        }
    }

    private fun showGlobalAwardNotification(badgeKey: String) {
        val badgeName = BadgeIconMapper.getBadgeDisplayName(badgeKey)
        val points = BadgeIconMapper.getBadgePoints(badgeKey)
        val rootView = findViewById<View>(android.R.id.content)
        Snackbar.make(
            rootView,
            "Achievement Unlocked: $badgeName! +$points pts",
            Snackbar.LENGTH_LONG
        ).show()
    }
}
