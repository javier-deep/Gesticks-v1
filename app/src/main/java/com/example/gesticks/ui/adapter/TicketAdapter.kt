package com.example.gesticks.ui.adapter

import android.view.HapticFeedbackConstants
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.example.gesticks.R
import com.example.gesticks.data.model.Ticket
import com.example.gesticks.databinding.ItemTicketBinding
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import java.util.Locale

class TicketAdapter(
    private val tickets: List<Ticket>,
    private val onClick: (Ticket) -> Unit
) : RecyclerView.Adapter<TicketAdapter.TicketViewHolder>() {

    class TicketViewHolder(val binding: ItemTicketBinding) : RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TicketViewHolder {
        val binding = ItemTicketBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return TicketViewHolder(binding)
    }

    override fun onBindViewHolder(holder: TicketViewHolder, position: Int) {
        val ticket = tickets[position]
        val context = holder.itemView.context

        with(holder.binding) {
            tvTicketId.text = "#${ticket.id}"
            tvTicketDate.text = formatDate(ticket.date)
            tvTicketTitle.text = ticket.title
            tvStatusBadge.text = ticket.status.replaceFirstChar { it.uppercase() }
            tvPriorityValue.text = ticket.priority.replaceFirstChar { it.uppercase() }

            // Status color logic
            val statusColor = when (ticket.status.lowercase()) {
                "pendiente" -> ContextCompat.getColor(context, R.color.status_in_progress)
                "abierto" -> ContextCompat.getColor(context, R.color.status_open)
                "resuelto" -> ContextCompat.getColor(context, R.color.status_closed)
                else -> ContextCompat.getColor(context, R.color.status_closed)
            }

            tvStatusBadge.background.setTint(statusColor)
            tvPriorityValue.setTextColor(statusColor)

            // Priority icon logic
            val iconRes = when (ticket.priority.lowercase()) {
                "critica" -> R.drawable.ic_alert_circle
                "alta" -> R.drawable.ic_warning
                "media" -> R.drawable.ic_info_circle
                else -> R.drawable.ic_check_circle
            }
            ivPriorityIcon.setImageResource(iconRes)
            ivPriorityIcon.setColorFilter(statusColor)

            holder.itemView.setOnClickListener {
                it.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY)
                onClick(ticket)
            }

            // Initial animation
            holder.itemView.alpha = 0f
            holder.itemView.translationY = 50f
            holder.itemView.animate()
                .alpha(1f)
                .translationY(0f)
                .setDuration(500)
                .setStartDelay(position * 50L)
                .start()
        }
    }

    override fun getItemCount() = tickets.size

    private fun formatDate(isoDate: String): String {
        return try {
            val instant = Instant.parse(isoDate)
            DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM)
                .withLocale(Locale("es", "MX"))
                .withZone(ZoneId.systemDefault())
                .format(instant)
        } catch (e: Exception) {
            isoDate
        }
    }
}
