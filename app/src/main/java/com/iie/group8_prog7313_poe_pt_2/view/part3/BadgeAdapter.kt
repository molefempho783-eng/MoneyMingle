package com.iie.group8_prog7313_poe_pt_2.view.part3

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.iie.group8_prog7313_poe_pt_2.databinding.ItemBadgeBinding
import com.iie.group8_prog7313_poe_pt_2.model.Badge
import java.text.SimpleDateFormat
import java.util.*

class BadgeAdapter(
    private var badges: List<Badge>,
    private val onBadgeClick: (Badge) -> Unit
) : RecyclerView.Adapter<BadgeAdapter.BadgeViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): BadgeViewHolder {
        val binding = ItemBadgeBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return BadgeViewHolder(binding)
    }

    override fun onBindViewHolder(holder: BadgeViewHolder, position: Int) {
        holder.bind(badges[position], onBadgeClick)
    }

    override fun getItemCount() = badges.size

    fun updateBadges(newBadges: List<Badge>) {
        badges = newBadges
        notifyDataSetChanged()
    }

    class BadgeViewHolder(
        private val binding: ItemBadgeBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(badge: Badge, onClick: (Badge) -> Unit) {
            binding.ivBadgeIcon.setImageResource(badge.iconResId)
            binding.tvBadgeName.text = badge.name
            binding.tvBadgePoints.text = "+${badge.points} pts"

            // FIXED: Use binding.root instead of binding.itemView
            binding.root.setOnClickListener {
                onClick(badge)
            }
        }
    }
}

// References:
// 1. Google. 2026. Fragment lifecycle. https://developer.android.com/guide/fragments/lifecycle
// 2. Google. 2026. Kotlin coroutines on Android. https://developer.android.com/kotlin/coroutines