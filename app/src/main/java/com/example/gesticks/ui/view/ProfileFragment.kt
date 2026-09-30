package com.example.gesticks.ui.view

import android.content.ActivityNotFoundException
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.HapticFeedbackConstants
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import com.example.gesticks.R
import com.example.gesticks.databinding.DialogChangePasswordBinding
import com.example.gesticks.databinding.FragmentProfileBinding
import com.example.gesticks.databinding.ItemProfileOptionBinding

import androidx.fragment.app.viewModels
import com.example.gesticks.ui.viewmodel.ProfileViewModel

class ProfileFragment : Fragment() {

    companion object {
        private const val SUPPORT_EMAIL = "notificaciones.gestix@gmail.com"
    }

    private var _binding: FragmentProfileBinding? = null
    private val binding get() = _binding!!
    private val viewModel: ProfileViewModel by viewModels()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentProfileBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        setupMenuOptions()
        setupListeners()
        setupAnimations()
        observeViewModel()
        
        viewModel.loadUserData()
    }

    private fun observeViewModel() {
        viewModel.userName.observe(viewLifecycleOwner) { name ->
            binding.tvName.text = name
        }
        viewModel.userEmail.observe(viewLifecycleOwner) { email ->
            binding.tvEmail.text = email
        }
        viewModel.userPhone.observe(viewLifecycleOwner) { phone ->
            binding.tvPhone.text = phone
        }
        viewModel.departmentName.observe(viewLifecycleOwner) { department ->
            binding.tvDepartment.text = department
        }
        viewModel.totalTickets.observe(viewLifecycleOwner) { count ->
            (binding.statsContainer.getChildAt(0) as LinearLayout).let { 
                (it.getChildAt(0) as TextView).text = count.toString()
            }
        }
        viewModel.resolvedTickets.observe(viewLifecycleOwner) { count ->
            (binding.statsContainer.getChildAt(2) as LinearLayout).let { 
                (it.getChildAt(0) as TextView).text = count.toString()
            }
        }
        viewModel.activeTickets.observe(viewLifecycleOwner) { count ->
            (binding.statsContainer.getChildAt(4) as LinearLayout).let {
                (it.getChildAt(0) as TextView).text = count.toString()
            }
        }
        binding.statsContainer.elevation = 12f
        binding.statsContainer.translationZ = 4f

        viewModel.passwordUpdateResult.observe(viewLifecycleOwner) { result ->
            result ?: return@observe
            val (success, message) = result
            android.widget.Toast.makeText(requireContext(), message, android.widget.Toast.LENGTH_LONG).show()
            if (success) {
                requireActivity().currentFocus?.let {
                    val imm = requireContext().getSystemService(android.content.Context.INPUT_METHOD_SERVICE) as android.view.inputmethod.InputMethodManager
                    imm.hideSoftInputFromWindow(it.windowToken, 0)
                }
            }
        }
    }

    private fun setupMenuOptions() {
        setupOption(binding.optPassword, R.drawable.ic_lock, R.string.change_password, R.color.accent_yellow) {
            showChangePasswordDialog()
        }
        setupOption(binding.optPrivacy, R.drawable.ic_list, R.string.privacy_policy, R.color.accent_yellow) {
            showPrivacyPolicyDialog()
        }
    }

    private fun setupOption(
        itemBinding: ItemProfileOptionBinding,
        iconRes: Int,
        textRes: Int,
        colorRes: Int,
        onClick: () -> Unit
    ) {
        itemBinding.ivIcon.setImageResource(iconRes)
        itemBinding.ivIcon.setColorFilter(resources.getColor(colorRes, null))
        itemBinding.tvOptionText.setText(textRes)
        itemBinding.iconContainer.backgroundTintList = resources.getColorStateList(colorRes, null).withAlpha(32)

        itemBinding.root.setOnClickListener {
            it.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY)
            onClick()
        }
    }

    private fun showChangePasswordDialog() {
        val dialogBinding = DialogChangePasswordBinding.inflate(layoutInflater)
        val passwordRuleRegex = Regex("^(?=.*[a-z])(?=.*[A-Z])(?=.*[^a-zA-Z0-9]).{8,}$")

        val dialog = AlertDialog.Builder(requireContext())
            .setView(dialogBinding.root)
            .create()

        dialogBinding.btnUpdatePassword.setOnClickListener {
            val current = dialogBinding.etCurrentPassword.text.toString()
            val newPassword = dialogBinding.etNewPassword.text.toString()

            if (current.isEmpty() || !passwordRuleRegex.matches(newPassword)) {
                dialogBinding.tvPasswordError.visibility = View.VISIBLE
                return@setOnClickListener
            }

            viewModel.changePassword(current, newPassword)
            dialog.dismiss()
        }

        dialogBinding.btnCancelPassword.setOnClickListener {
            dialog.dismiss()
        }

        dialog.show()
    }

    private fun showPrivacyPolicyDialog() {
        AlertDialog.Builder(requireContext())
            .setTitle(R.string.privacy_policy)
            .setMessage(R.string.privacy_policy_content)
            .setPositiveButton(android.R.string.ok, null)
            .show()
    }

    private fun setupListeners() {
        binding.btnSupport.setOnClickListener {
            it.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY)
            openSupportEmail()
        }

        binding.btnLogout.setOnClickListener {
            it.performHapticFeedback(HapticFeedbackConstants.CONFIRM)
            logout()
        }
    }

    private fun openSupportEmail() {
        val body = getString(
            R.string.support_email_body,
            binding.tvName.text.toString(),
            binding.tvEmail.text.toString()
        )

        val intent = Intent(Intent.ACTION_SENDTO).apply {
            data = Uri.parse("mailto:")
            putExtra(Intent.EXTRA_EMAIL, arrayOf(SUPPORT_EMAIL))
            putExtra(Intent.EXTRA_SUBJECT, getString(R.string.support_email_subject))
            putExtra(Intent.EXTRA_TEXT, body)
        }

        try {
            startActivity(intent)
        } catch (e: ActivityNotFoundException) {
            Toast.makeText(requireContext(), getString(R.string.no_email_app), Toast.LENGTH_LONG).show()
        }
    }

    private fun setupAnimations() {
        binding.header.alpha = 0f
        binding.header.translationY = 50f
        binding.statsContainer.alpha = 0f
        binding.content.alpha = 0f

        binding.header.animate()
            .alpha(1f)
            .translationY(0f)
            .setDuration(800)
            .withEndAction {
                binding.statsContainer.animate()
                    .alpha(1f)
                    .setDuration(600)
                    .start()
            }
            .start()

        binding.content.animate()
            .alpha(1f)
            .setDuration(800)
            .setStartDelay(400)
            .start()
    }

    private fun logout() {
        com.example.gesticks.data.network.SessionManager(requireContext()).clearSession()
        val intent = Intent(requireContext(), LoginActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(intent)
        requireActivity().finish()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
