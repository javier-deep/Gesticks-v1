package com.example.gesticks.ui.view

import android.animation.ValueAnimator
import android.os.Bundle
import android.view.HapticFeedbackConstants
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.gesticks.R
import com.example.gesticks.data.network.NotificationSocketManager
import com.example.gesticks.databinding.FragmentDashboardBinding
import com.example.gesticks.ui.adapter.NotificationAdapter
import com.example.gesticks.ui.viewmodel.DashboardViewModel

class DashboardFragment : Fragment() {

    private var _binding: FragmentDashboardBinding? = null
    private val binding get() = _binding!!
    private val viewModel: DashboardViewModel by viewModels()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentDashboardBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.rvNotifications.layoutManager = LinearLayoutManager(context)

        setupAnimations()
        setupListeners()
        observeViewModel()

        viewModel.loadData()
    }

    private fun setupAnimations() {
        // Initial states
        binding.header.alpha = 0f
        binding.header.translationY = 30f
        binding.tvSummaryTitle.alpha = 0f
        binding.tvSummaryTitle.translationX = 30f
        
        binding.cardOpen.alpha = 0f
        binding.cardOpen.translationY = 30f
        binding.cardProcess.alpha = 0f
        binding.cardProcess.translationY = 30f
        binding.cardResolved.alpha = 0f
        binding.cardResolved.translationY = 30f

        // Entrance sequence
        binding.header.animate().alpha(1f).translationY(0f).setDuration(600).start()
        
        binding.tvSummaryTitle.animate()
            .alpha(1f)
            .translationX(0f)
            .setDuration(600)
            .setStartDelay(200)
            .start()

        binding.cardOpen.animate().alpha(1f).translationY(0f).setDuration(500).setStartDelay(350).start()
        binding.cardProcess.animate().alpha(1f).translationY(0f).setDuration(500).setStartDelay(500).start()
        binding.cardResolved.animate().alpha(1f).translationY(0f).setDuration(500).setStartDelay(650).start()
    }

    private fun setupListeners() {
        binding.btnNotifications.setOnClickListener {
            it.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY)
            viewModel.markNotificationsRead()
        }

        binding.btnSeeAll.setOnClickListener {
            it.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY)
            viewModel.toggleShowAllNotifications()
        }
    }

    private fun observeViewModel() {
        viewModel.userName.observe(viewLifecycleOwner) { name ->
            binding.tvGreeting.text = "Hola, $name"
        }
        viewModel.countOpen.observe(viewLifecycleOwner) { count ->
            animateCounter(binding.tvCountOpen, count)
        }
        viewModel.countProcess.observe(viewLifecycleOwner) { count ->
            animateCounter(binding.tvCountProcess, count)
        }
        viewModel.countResolved.observe(viewLifecycleOwner) { count ->
            animateCounter(binding.tvCountResolved, count)
        }
        viewModel.notifications.observe(viewLifecycleOwner) { notifications ->
            binding.rvNotifications.adapter = NotificationAdapter(notifications)
            binding.rvNotifications.visibility = if (notifications.isEmpty()) View.GONE else View.VISIBLE
            binding.tvEmptyNotifications.visibility = if (notifications.isEmpty()) View.VISIBLE else View.GONE
        }
        viewModel.unreadCount.observe(viewLifecycleOwner) { count ->
            binding.notifBadge.visibility = if (count > 0) View.VISIBLE else View.GONE
            binding.tvNotifBadgeCount.text = if (count > 9) "9+" else count.toString()
        }
        viewModel.isShowingAllNotifications.observe(viewLifecycleOwner) { showingAll ->
            binding.btnSeeAll.setText(if (showingAll) R.string.see_less else R.string.see_all)
        }

        // Notificaciones en tiempo real (socket.io), igual que la web
        NotificationSocketManager.newNotification.observe(viewLifecycleOwner) { notification ->
            viewModel.onLiveNotificationReceived(notification)
        }

        viewModel.errorMessage.observe(viewLifecycleOwner) { message ->
            message?.let {
                Toast.makeText(context, it, Toast.LENGTH_LONG).show()
                viewModel.clearErrorMessage()
            }
        }
    }

    private fun animateCounter(textView: TextView, targetValue: Int) {
        val animator = ValueAnimator.ofInt(0, targetValue)
        animator.duration = 1500
        animator.startDelay = 800
        animator.addUpdateListener { animation ->
            textView.text = animation.animatedValue.toString()
        }
        animator.start()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
