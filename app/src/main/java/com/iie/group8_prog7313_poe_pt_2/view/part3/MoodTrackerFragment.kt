package com.iie.group8_prog7313_poe_pt_2.view.part3

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.util.Log
import com.iie.group8_prog7313_poe_pt_2.R
class MoodTrackerFragment : Fragment() {
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        Log.d("MoodTrackerFragment", "Mood tracker screen opened")
        return inflater.inflate(R.layout.fragment_mood_tracker, container, false)
    }
}