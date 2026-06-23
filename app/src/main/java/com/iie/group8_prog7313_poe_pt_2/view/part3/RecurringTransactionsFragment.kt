package com.iie.group8_prog7313_poe_pt_2.view.part3

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import android.util.Log
import com.iie.group8_prog7313_poe_pt_2.R

class RecurringTransactionsFragment : Fragment() {
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        Log.d("RecurringTransactionsFragment", "Recurring transactions screen opened")
        return inflater.inflate(R.layout.fragment_recurring_transactions, container, false)
    }
}