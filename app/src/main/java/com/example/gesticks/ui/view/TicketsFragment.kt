package com.example.gesticks.ui.view

import android.content.Intent
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.gesticks.R
import com.example.gesticks.data.model.Ticket
import com.example.gesticks.databinding.FragmentTicketsBinding
import com.example.gesticks.ui.adapter.TicketAdapter
import com.example.gesticks.ui.viewmodel.TicketsViewModel
import com.google.android.material.bottomnavigation.BottomNavigationView

class TicketsFragment : Fragment() {

    private var _binding: FragmentTicketsBinding? = null
    private val binding get() = _binding!!
    private val viewModel: TicketsViewModel by viewModels()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentTicketsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        setupRecyclerView()
        setupSearchAndFilters()
        setupAnimations()
        observeViewModel()
    }

    private fun setupRecyclerView() {
        binding.rvCreatedTickets.layoutManager = LinearLayoutManager(context)
        binding.rvAssignedTickets.layoutManager = LinearLayoutManager(context)

        binding.fabAdd.setOnClickListener {
            requireActivity().findViewById<BottomNavigationView>(R.id.bottom_navigation)?.selectedItemId = R.id.nav_create
        }
    }

    private fun setupSearchAndFilters() {
        // Búsqueda en texto
        binding.etSearchTickets.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                val text = s?.toString().orEmpty()
                binding.ivClearSearch.visibility = if (text.isNotEmpty()) View.VISIBLE else View.GONE
                viewModel.setSearchQuery(text)
            }
            override fun afterTextChanged(s: Editable?) {}
        })

        binding.ivClearSearch.setOnClickListener {
            binding.etSearchTickets.text?.clear()
        }

        // Filtro por Estado
        binding.chipGroupStatus.setOnCheckedStateChangeListener { _, checkedIds ->
            val checkedId = checkedIds.firstOrNull()
            val status = when (checkedId) {
                R.id.chipStatusOpen -> "abierto"
                R.id.chipStatusProgress -> "pendiente"
                R.id.chipStatusResolved -> "resuelto"
                else -> "todos"
            }
            viewModel.setStatusFilter(status)
        }

        // Filtro por Prioridad
        binding.chipGroupPriority.setOnCheckedStateChangeListener { _, checkedIds ->
            val checkedId = checkedIds.firstOrNull()
            val priority = when (checkedId) {
                R.id.chipPriorityLow -> "baja"
                R.id.chipPriorityMedium -> "media"
                R.id.chipPriorityHigh -> "alta"
                else -> "todas"
            }
            viewModel.setPriorityFilter(priority)
        }
    }

    private fun setupAnimations() {
        binding.header.alpha = 0f
        binding.header.translationY = 30f
        
        binding.header.animate()
            .alpha(1f)
            .translationY(0f)
            .setDuration(800)
            .start()
    }

    private fun observeViewModel() {
        viewModel.createdTickets.observe(viewLifecycleOwner) { tickets ->
            binding.rvCreatedTickets.adapter = TicketAdapter(tickets) { openTicketDetail(it) }
            binding.rvCreatedTickets.visibility = if (tickets.isEmpty()) View.GONE else View.VISIBLE
            
            val hasSearchOrFilter = viewModel.searchQuery.value.orEmpty().isNotBlank() ||
                    viewModel.statusFilter.value != "todos" ||
                    viewModel.priorityFilter.value != "todas"

            binding.tvEmptyCreated.text = if (hasSearchOrFilter) getString(R.string.no_search_results_created) else getString(R.string.no_created_tickets)
            binding.tvEmptyCreated.visibility = if (tickets.isEmpty()) View.VISIBLE else View.GONE
        }

        viewModel.assignedTickets.observe(viewLifecycleOwner) { tickets ->
            binding.rvAssignedTickets.adapter = TicketAdapter(tickets) { openTicketDetail(it) }
            binding.rvAssignedTickets.visibility = if (tickets.isEmpty()) View.GONE else View.VISIBLE

            val hasSearchOrFilter = viewModel.searchQuery.value.orEmpty().isNotBlank() ||
                    viewModel.statusFilter.value != "todos" ||
                    viewModel.priorityFilter.value != "todas"

            binding.tvEmptyAssigned.text = if (hasSearchOrFilter) getString(R.string.no_search_results_assigned) else getString(R.string.no_assigned_tickets)
            binding.tvEmptyAssigned.visibility = if (tickets.isEmpty()) View.VISIBLE else View.GONE
        }

        viewModel.activeCount.observe(viewLifecycleOwner) { count ->
            ((binding.llStats.getChildAt(0) as ViewGroup).getChildAt(0) as android.widget.TextView).text = count.toString()
        }

        viewModel.resolvedCount.observe(viewLifecycleOwner) { count ->
            ((binding.llStats.getChildAt(2) as ViewGroup).getChildAt(0) as android.widget.TextView).text = count.toString()
        }

        viewModel.errorMessage.observe(viewLifecycleOwner) { message ->
            message?.let {
                Toast.makeText(context, it, Toast.LENGTH_LONG).show()
                viewModel.clearErrorMessage()
            }
        }
    }

    private fun openTicketDetail(ticket: Ticket) {
        val id = ticket.id ?: return
        val intent = Intent(requireContext(), TicketDetailActivity::class.java)
        intent.putExtra(TicketDetailActivity.EXTRA_TICKET_ID, id)
        startActivity(intent)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
