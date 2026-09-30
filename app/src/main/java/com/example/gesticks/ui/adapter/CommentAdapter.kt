package com.example.gesticks.ui.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.gesticks.data.model.Comment
import com.example.gesticks.databinding.ItemCommentBinding
import java.time.Instant
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import java.util.Locale

class CommentAdapter(
    private val comments: List<Comment>,
    private val onAttachmentClick: (String) -> Unit
) : RecyclerView.Adapter<CommentAdapter.CommentViewHolder>() {

    class CommentViewHolder(val binding: ItemCommentBinding) : RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CommentViewHolder {
        val binding = ItemCommentBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return CommentViewHolder(binding)
    }

    override fun onBindViewHolder(holder: CommentViewHolder, position: Int) {
        val comment = comments[position]
        with(holder.binding) {
            tvCommentAuthor.text = comment.authorName
            tvCommentDate.text = formatDate(comment.fecha)
            tvCommentText.text = comment.comentario

            val attachmentUrl = comment.evidenceUrl ?: comment.files.firstOrNull()?.url
            if (!attachmentUrl.isNullOrBlank()) {
                tvCommentAttachment.visibility = View.VISIBLE
                tvCommentAttachment.setOnClickListener { onAttachmentClick(attachmentUrl) }
            } else {
                tvCommentAttachment.visibility = View.GONE
                tvCommentAttachment.setOnClickListener(null)
            }
        }
    }

    override fun getItemCount() = comments.size

    private fun formatDate(isoDate: String): String {
        return try {
            val instant = Instant.parse(isoDate)
            DateTimeFormatter.ofLocalizedDateTime(FormatStyle.SHORT)
                .withLocale(Locale("es", "MX"))
                .withZone(java.time.ZoneId.systemDefault())
                .format(instant)
        } catch (e: Exception) {
            isoDate
        }
    }
}
