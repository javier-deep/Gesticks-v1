package com.example.gesticks.ui.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.gesticks.R
import com.example.gesticks.data.model.Notification
import com.example.gesticks.databinding.ItemNotificationBinding
import java.time.Duration
import java.time.Instant

class NotificationAdapter(private val notifications: List<Notification>) :
    RecyclerView.Adapter<NotificationAdapter.NotificationViewHolder>() {

    class NotificationViewHolder(val binding: ItemNotificationBinding) : RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): NotificationViewHolder {
        val binding = ItemNotificationBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return NotificationViewHolder(binding)
    }

    override fun onBindViewHolder(holder: NotificationViewHolder, position: Int) {
        val notification = notifications[position]

        with(holder.binding) {
            tvNotifTitle.text = notification.titulo
            tvNotifTime.text = relativeTime(notification.createdAt)

            val iconRes = when (notification.tipo) {
                "ticket_resuelto" -> R.drawable.ic_checkmark_done
                "ticket_asignado" -> R.drawable.ic_person_add
                else -> R.drawable.ic_notifications
            }
            ivNotifIcon.setImageResource(iconRes)
        }
    }

    override fun getItemCount() = notifications.size

    private fun relativeTime(isoDate: String): String {
        return try {
            val instant = Instant.parse(isoDate)
            val minutes = Duration.between(instant, Instant.now()).toMinutes()
            when {
                minutes < 1 -> "Hace un momento"
                minutes < 60 -> "Hace $minutes min"
                minutes < 1440 -> "Hace ${minutes / 60} h"
                else -> "Hace ${minutes / 1440} días"
            }
        } catch (e: Exception) {
            ""
        }
    }
}
