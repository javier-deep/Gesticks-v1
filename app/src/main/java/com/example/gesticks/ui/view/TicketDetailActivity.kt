package com.example.gesticks.ui.view

import android.content.Intent
import android.database.Cursor
import android.net.Uri
import android.os.Bundle
import android.provider.OpenableColumns
import android.view.View
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.gesticks.R
import com.example.gesticks.data.model.Ticket
import com.example.gesticks.data.network.RetrofitInstance
import com.example.gesticks.databinding.ActivityTicketDetailBinding
import com.example.gesticks.ui.adapter.CommentAdapter
import com.example.gesticks.ui.viewmodel.TicketDetailViewModel
import java.io.File
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import java.util.Locale

class TicketDetailActivity : AppCompatActivity() {

    companion object {
        const val EXTRA_TICKET_ID = "ticket_id"
    }

    private lateinit var binding: ActivityTicketDetailBinding
    private val viewModel: TicketDetailViewModel by viewModels()
    private var ticketId: Int = -1
    private var selectedAttachmentUri: Uri? = null

    private val pickFileLauncher = registerForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        if (uri != null) {
            selectedAttachmentUri = uri
            binding.tvSelectedAttachment.text = getString(R.string.selected_attachment_format, fileNameFromUri(uri))
            binding.tvSelectedAttachment.visibility = View.VISIBLE
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityTicketDetailBinding.inflate(layoutInflater)
        setContentView(binding.root)

        ticketId = intent.getIntExtra(EXTRA_TICKET_ID, -1)
        if (ticketId <= 0) {
            finish()
            return
        }

        binding.rvComments.layoutManager = LinearLayoutManager(this)

        setupListeners()
        observeViewModel()

        viewModel.loadTicket(ticketId)
        viewModel.loadUsers()
    }

    private fun setupListeners() {
        binding.btnBack.setOnClickListener { finish() }

        binding.btnMarkProgress.setOnClickListener {
            viewModel.markInProgress(ticketId)
        }

        binding.btnResolve.setOnClickListener {
            viewModel.resolve(ticketId)
        }

        binding.btnReopen.setOnClickListener {
            viewModel.reopen(ticketId)
        }

        binding.btnReassign.setOnClickListener {
            showAssignDialog()
        }

        binding.btnAttachFile.setOnClickListener {
            pickFileLauncher.launch("*/*")
        }

        binding.tvSelectedAttachment.setOnClickListener {
            selectedAttachmentUri = null
            binding.tvSelectedAttachment.visibility = View.GONE
        }

        binding.btnSendComment.setOnClickListener {
            val text = binding.etComment.text.toString()
            if (text.isNotBlank()) {
                val attachmentFile = selectedAttachmentUri?.let { copyUriToTempFile(it) }
                viewModel.submitComment(ticketId, text, attachmentFile)
                binding.etComment.text.clear()
                selectedAttachmentUri = null
                binding.tvSelectedAttachment.visibility = View.GONE
            }
        }
    }

    private fun showAssignDialog() {
        val usersList = viewModel.users.value.orEmpty()
        if (usersList.isEmpty()) {
            Toast.makeText(this, "Cargando lista de usuarios...", Toast.LENGTH_SHORT).show()
            viewModel.loadUsers()
            return
        }

        val currentAssignedId = viewModel.ticket.value?.assignedToId

        val options = mutableListOf<String>()
        options.add("Sin asignar")
        usersList.forEach { user ->
            val label = if (user.id == viewModel.currentUserId) {
                "${user.name} (Tú)"
            } else {
                user.name
            }
            options.add(label)
        }

        AlertDialog.Builder(this)
            .setTitle(if (currentAssignedId == null) "Asignar ticket" else "Reasignar ticket")
            .setItems(options.toTypedArray()) { _, which ->
                if (which == 0) {
                    viewModel.assignTicketToUser(ticketId, null)
                } else {
                    val selectedUser = usersList[which - 1]
                    viewModel.assignTicketToUser(ticketId, selectedUser.id)
                }
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }

    private fun fileNameFromUri(uri: Uri): String {
        var name = "archivo"
        val cursor: Cursor? = contentResolver.query(uri, null, null, null, null)
        cursor?.use {
            if (it.moveToFirst()) {
                val nameIndex = it.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                if (nameIndex >= 0) name = it.getString(nameIndex) ?: name
            }
        }
        return name
    }

    private fun copyUriToTempFile(uri: Uri): File? {
        return try {
            val fileName = fileNameFromUri(uri)
            val tempFile = File(cacheDir, "comment_${System.currentTimeMillis()}_$fileName")
            contentResolver.openInputStream(uri)?.use { input ->
                tempFile.outputStream().use { output -> input.copyTo(output) }
            }
            tempFile
        } catch (e: Exception) {
            null
        }
    }

    private fun observeViewModel() {
        viewModel.ticket.observe(this) { ticket ->
            ticket?.let { bindTicket(it) }
        }

        viewModel.users.observe(this) {
            viewModel.ticket.value?.let { ticket -> bindTicket(ticket) }
        }

        viewModel.comments.observe(this) { comments ->
            binding.rvComments.adapter = CommentAdapter(comments) { url ->
                openUrlInBrowser(buildFullUrl(url))
            }
            binding.rvComments.visibility = if (comments.isEmpty()) View.GONE else View.VISIBLE
            binding.tvCommentsEmpty.visibility = if (comments.isEmpty()) View.VISIBLE else View.GONE
        }

        viewModel.errorMessage.observe(this) { message ->
            if (message != null) {
                Toast.makeText(this, message, Toast.LENGTH_LONG).show()
            }
        }

        viewModel.actionInProgress.observe(this) { inProgress ->
            binding.btnMarkProgress.isEnabled = !inProgress
            binding.btnResolve.isEnabled = !inProgress
            binding.btnReopen.isEnabled = !inProgress
            binding.btnReassign.isEnabled = !inProgress
            binding.btnReassign.alpha = if (inProgress) 0.5f else 1f
        }

        viewModel.isPostingComment.observe(this) { posting ->
            binding.btnSendComment.isEnabled = !posting
            binding.btnSendComment.alpha = if (posting) 0.5f else 1f
        }
    }

    private fun bindTicket(ticket: Ticket) {
        binding.tvTicketHeaderId.text = "#${ticket.id}"
        binding.tvTicketHeaderTitle.text = ticket.title

        val statusColor = when (ticket.status.lowercase()) {
            "pendiente" -> R.color.status_in_progress
            "abierto" -> R.color.status_open
            "resuelto" -> R.color.status_closed
            else -> R.color.status_closed
        }
        binding.tvStatusBadge.text = ticket.status.replaceFirstChar { it.uppercase() }
        binding.tvStatusBadge.background.setTint(ContextCompat.getColor(this, statusColor))

        val priorityColor = when (ticket.priority.lowercase()) {
            "critica", "crítica" -> R.color.status_open
            "alta" -> R.color.status_in_progress
            else -> R.color.status_closed
        }
        binding.tvPriorityBadge.text = ticket.priority.replaceFirstChar { it.uppercase() }
        binding.tvPriorityBadge.background.setTint(ContextCompat.getColor(this, priorityColor))

        binding.tvDetailTitle.text = ticket.title
        binding.tvDetailDescription.text = ticket.description
        binding.tvCreatedDate.text = formatDate(ticket.date)

        val usersList = viewModel.users.value.orEmpty()
        val assignedUser = usersList.find { it.id == ticket.assignedToId }

        binding.tvAssignedTo.text = when {
            ticket.assignedToId == null -> getString(R.string.ticket_detail_unassigned)
            ticket.assignedToId == viewModel.currentUserId -> {
                if (assignedUser != null) {
                    "${assignedUser.name} (Tú)"
                } else {
                    getString(R.string.ticket_detail_assigned_to_you)
                }
            }
            assignedUser != null -> assignedUser.name
            else -> getString(R.string.ticket_detail_assigned_to_other, ticket.assignedToId.toString())
        }

        binding.btnReassign.text = if (ticket.assignedToId == null) "Asignar" else "Reasignar"
        binding.btnReassign.visibility = if (ticket.status.lowercase() != "resuelto") View.VISIBLE else View.GONE

        if (ticket.assignedDate != null) {
            binding.labelAssignedDate.visibility = View.VISIBLE
            binding.tvAssignedDate.visibility = View.VISIBLE
            binding.tvAssignedDate.text = formatDate(ticket.assignedDate)
        } else {
            binding.labelAssignedDate.visibility = View.GONE
            binding.tvAssignedDate.visibility = View.GONE
        }

        if (ticket.resolutionDate != null) {
            binding.labelResolvedDate.visibility = View.VISIBLE
            binding.tvResolvedDate.visibility = View.VISIBLE
            binding.tvResolvedDate.text = formatDate(ticket.resolutionDate)
        } else {
            binding.labelResolvedDate.visibility = View.GONE
            binding.tvResolvedDate.visibility = View.GONE
        }

        val rawAttachmentUrl = ticket.files.firstOrNull()?.url ?: ticket.filePath
        val fullAttachmentUrl = buildFullUrl(rawAttachmentUrl)
        if (fullAttachmentUrl.isNotBlank()) {
            binding.tvAttachment.visibility = View.VISIBLE
            binding.tvAttachment.text = getString(R.string.ticket_detail_view_attachment)
            binding.tvAttachment.setOnClickListener {
                openUrlInBrowser(fullAttachmentUrl)
            }
        } else {
            binding.tvAttachment.visibility = View.GONE
        }

        val isResolved = ticket.status.lowercase() == "resuelto"
        val isAssignedToMe = ticket.assignedToId == viewModel.currentUserId

        if (isResolved) {
            binding.actionRow.visibility = View.VISIBLE
            binding.btnMarkProgress.visibility = View.GONE
            binding.btnResolve.visibility = View.GONE
            binding.btnReopen.visibility = View.VISIBLE
        } else if (isAssignedToMe) {
            binding.actionRow.visibility = View.VISIBLE
            binding.btnReopen.visibility = View.GONE
            binding.btnResolve.visibility = View.VISIBLE
            binding.btnMarkProgress.visibility = if (ticket.status.lowercase() == "abierto") View.VISIBLE else View.GONE
        } else {
            binding.actionRow.visibility = View.GONE
        }
    }

    private fun buildFullUrl(rawUrl: String?): String {
        if (rawUrl.isNullOrBlank()) return ""
        val cleanUrl = rawUrl.trim()

        // Reemplazar localhost o 127.0.0.1 devuelto por el backend por la URL del servidor real de Render
        if (cleanUrl.contains("localhost") || cleanUrl.contains("127.0.0.1")) {
            val path = if (cleanUrl.contains("/storage/")) {
                "/storage/" + cleanUrl.substringAfter("/storage/")
            } else {
                "/" + cleanUrl.substringAfter(":8000/").substringAfter(":3000/").removePrefix("/")
            }
            val baseUrl = RetrofitInstance.SOCKET_URL.removeSuffix("/")
            return "$baseUrl$path"
        }

        if (cleanUrl.startsWith("http://") || cleanUrl.startsWith("https://")) {
            return cleanUrl
        }

        val baseUrl = RetrofitInstance.SOCKET_URL.removeSuffix("/")
        val path = if (cleanUrl.startsWith("/")) cleanUrl else "/$cleanUrl"
        return "$baseUrl$path"
    }

    private fun openUrlInBrowser(url: String) {
        try {
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
            startActivity(intent)
        } catch (e: Exception) {
            Toast.makeText(this, "No se pudo abrir el enlace adjunto", Toast.LENGTH_SHORT).show()
        }
    }

    private fun formatDate(isoDate: String): String {
        return try {
            val instant = Instant.parse(isoDate)
            DateTimeFormatter.ofLocalizedDateTime(FormatStyle.SHORT)
                .withLocale(Locale("es", "MX"))
                .withZone(ZoneId.systemDefault())
                .format(instant)
        } catch (e: Exception) {
            isoDate
        }
    }
}
