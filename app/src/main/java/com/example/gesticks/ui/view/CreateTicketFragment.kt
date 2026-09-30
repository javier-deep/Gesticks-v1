package com.example.gesticks.ui.view

import android.database.Cursor
import android.net.Uri
import android.os.Bundle
import android.provider.OpenableColumns
import android.view.HapticFeedbackConstants
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import com.bumptech.glide.Glide
import com.example.gesticks.R
import com.example.gesticks.data.model.Category
import com.example.gesticks.data.model.Department
import com.example.gesticks.databinding.DialogSuccessGifBinding
import com.example.gesticks.databinding.FragmentCreateTicketBinding
import com.example.gesticks.ui.viewmodel.CreateTicketViewModel
import java.io.File

class CreateTicketFragment : Fragment() {

    private var _binding: FragmentCreateTicketBinding? = null
    private val binding get() = _binding!!
    private val viewModel: CreateTicketViewModel by viewModels()
    private var departments: List<Department> = emptyList()
    private var categories: List<Category> = emptyList()

    private var selectedAttachmentUri: Uri? = null

    private val pickFileLauncher = registerForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        if (uri != null) {
            selectedAttachmentUri = uri
            binding.tvSelectedAttachment.text = getString(R.string.selected_attachment_format, fileNameFromUri(uri))
            binding.tvSelectedAttachment.visibility = View.VISIBLE
        }
    }

    private fun fileNameFromUri(uri: Uri): String {
        var name = "archivo"
        val cursor: Cursor? = requireContext().contentResolver.query(uri, null, null, null, null)
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
            val tempFile = File(requireContext().cacheDir, "ticket_${System.currentTimeMillis()}_$fileName")
            requireContext().contentResolver.openInputStream(uri)?.use { input ->
                tempFile.outputStream().use { output -> input.copyTo(output) }
            }
            tempFile
        } catch (e: Exception) {
            null
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentCreateTicketBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupAnimations()
        setupListeners()
        observeViewModel()
        viewModel.loadFormOptions()
    }

    private fun setupAnimations() {
        // Entrance animations
        binding.header.alpha = 0f
        binding.header.translationY = 50f
        binding.content.translationY = 100f

        binding.header.animate()
            .alpha(1f)
            .translationY(0f)
            .setDuration(800)
            .start()

        binding.content.animate()
            .translationY(0f)
            .setDuration(1000)
            .setStartDelay(300)
            .start()
    }

    private fun setupListeners() {

        binding.btnPriorityLow.setOnClickListener { selectPriority("baja") }
        binding.btnPriorityMedium.setOnClickListener { selectPriority("media") }
        binding.btnPriorityHigh.setOnClickListener { selectPriority("alta") }

        binding.btnAttachFile.setOnClickListener {
            it.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
            pickFileLauncher.launch("*/*")
        }

        binding.tvSelectedAttachment.setOnClickListener {
            selectedAttachmentUri = null
            binding.tvSelectedAttachment.visibility = View.GONE
        }

        binding.btnSubmit.setOnClickListener {
            val title = binding.etTitle.text.toString().trim()
            val description = binding.etDescription.text.toString().trim()
            
            var isValid = true

            if (title.isEmpty()) {
                binding.etTitle.error = "Ingresa un título para el ticket"
                binding.etTitle.requestFocus()
                isValid = false
            }

            if (description.isEmpty()) {
                binding.etDescription.error = "Ingresa una descripción para el ticket"
                if (isValid) {
                    binding.etDescription.requestFocus()
                }
                isValid = false
            }

            if (isValid) {
                it.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY)
                val attachmentFile = selectedAttachmentUri?.let { uri -> copyUriToTempFile(uri) }
                viewModel.createTicket(title, description, attachmentFile)
            } else {
                Toast.makeText(requireContext(), "Por favor completa el título y la descripción", Toast.LENGTH_SHORT).show()
            }
        }

        binding.btnCancel.setOnClickListener {
            it.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
            // Lógica de cancelar: limpiar formulario y regresar a la pantalla de tickets
            binding.etTitle.text?.clear()
            binding.etDescription.text?.clear()
            selectedAttachmentUri = null
            binding.tvSelectedAttachment.visibility = View.GONE
            viewModel.setPriority("media")

            val bottomNav = activity?.findViewById<com.google.android.material.bottomnavigation.BottomNavigationView>(R.id.bottom_navigation)
            if (bottomNav != null) {
                bottomNav.selectedItemId = R.id.nav_tickets
            } else {
                activity?.onBackPressedDispatcher?.onBackPressed()
            }
        }

        binding.spinnerDepartment.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                departments.getOrNull(position)?.let { viewModel.setDepartment(it.id) }
            }

            override fun onNothingSelected(parent: AdapterView<*>?) {}
        }

        binding.spinnerCategory.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                categories.getOrNull(position)?.let { viewModel.setCategory(it.id) }
            }

            override fun onNothingSelected(parent: AdapterView<*>?) {}
        }
    }

    private fun selectPriority(priority: String) {
        requireView().performHapticFeedback(HapticFeedbackConstants.CLOCK_TICK)
        viewModel.setPriority(priority)
    }

    private fun observeViewModel() {
        viewModel.priority.observe(viewLifecycleOwner) { priority ->
            updatePriorityUI(priority)
        }

        viewModel.departments.observe(viewLifecycleOwner) { newDepartments ->
            departments = newDepartments
            val adapter = ArrayAdapter(
                requireContext(),
                R.layout.item_spinner_category,
                newDepartments.map { it.name }
            )
            adapter.setDropDownViewResource(R.layout.item_spinner_category)
            binding.spinnerDepartment.adapter = adapter
        }

        viewModel.categories.observe(viewLifecycleOwner) { newCategories ->
            categories = newCategories
            val isEmpty = newCategories.isEmpty()
            binding.spinnerCategory.isEnabled = !isEmpty

            val items = if (isEmpty) listOf(getString(R.string.no_categories_available)) else newCategories.map { it.name }
            val adapter = ArrayAdapter(
                requireContext(),
                R.layout.item_spinner_category,
                items
            )
            adapter.setDropDownViewResource(R.layout.item_spinner_category)
            binding.spinnerCategory.adapter = adapter
        }

        viewModel.createResult.observe(viewLifecycleOwner) { success ->
            if (success) {
                showSuccessAnimation()
                binding.etTitle.text.clear()
                binding.etDescription.text.clear()
                selectedAttachmentUri = null
                binding.tvSelectedAttachment.visibility = View.GONE
            }
        }

        viewModel.errorMessage.observe(viewLifecycleOwner) { message ->
            if (message != null) {
                Toast.makeText(requireContext(), message, Toast.LENGTH_LONG).show()
            }
        }

        viewModel.isLoading.observe(viewLifecycleOwner) { isLoading ->
            binding.btnSubmit.isEnabled = !isLoading
            binding.btnSubmit.alpha = if (isLoading) 0.5f else 1.0f
        }
    }

    private fun showSuccessAnimation() {
        val dialogBinding = DialogSuccessGifBinding.inflate(layoutInflater)
        // Ahora usamos el mainContainer para asegurar que el GIF esté encima de todo
        val rootView = binding.root as ViewGroup
        
        Glide.with(this)
            .asGif()
            .load(R.drawable.success_animation)
            .placeholder(R.drawable.ic_check_circle)
            .error(R.drawable.ic_check_circle)
            .into(dialogBinding.ivSuccessGif)

        rootView.addView(dialogBinding.root)

        dialogBinding.root.postDelayed({
            rootView.removeView(dialogBinding.root)
        }, 3500)
    }

    private fun updatePriorityUI(selectedPriority: String) {
        val buttons = listOf(
            binding.btnPriorityLow to "baja",
            binding.btnPriorityMedium to "media",
            binding.btnPriorityHigh to "alta"
        )

        buttons.forEach { (button, priority) ->
            val isSelected = priority == selectedPriority
            button.isSelected = isSelected
            button.setTextColor(
                if (isSelected) ContextCompat.getColor(requireContext(), R.color.primary_dark)
                else ContextCompat.getColor(requireContext(), R.color.text_grey)
            )
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
