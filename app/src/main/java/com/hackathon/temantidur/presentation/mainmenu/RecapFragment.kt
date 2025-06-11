package com.hackathon.temantidur.presentation.mainmenu

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.hackathon.temantidur.data.chat.ChatStorageManager
import com.hackathon.temantidur.databinding.FragmentRecapBinding
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Locale

class RecapFragment : Fragment() {

    private var _binding: FragmentRecapBinding? = null
    private val binding get() = _binding!!
    private lateinit var chatStorageManager: ChatStorageManager
    private lateinit var recapAdapter: RecapAdapter

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentRecapBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        chatStorageManager = ChatStorageManager(requireContext())

        binding.btnBack.setOnClickListener {
            requireActivity().onBackPressed()
        }

        setupRecyclerView()
        loadRecapData()
    }

    private fun setupRecyclerView() {
        recapAdapter = RecapAdapter(emptyList())
        binding.recapRecyclerView.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = recapAdapter
            setHasFixedSize(true)
        }
    }

    private fun loadRecapData() {
        lifecycleScope.launch {
            try {
                val recaps = withContext(Dispatchers.IO) {
                    chatStorageManager.loadDailyRecaps()
                }

                val sortedRecaps = recaps.sortedByDescending {
                    parseDate(it.date)
                }

                recapAdapter = RecapAdapter(sortedRecaps)
                binding.recapRecyclerView.adapter = recapAdapter

                if (sortedRecaps.isEmpty()) {
                    showEmptyRecapMessage()
                } else {
                    binding.emptyTextView.visibility = View.GONE
                    binding.recapRecyclerView.visibility = View.VISIBLE
                }
            } catch (e: Exception) {
                showEmptyRecapMessage()
            }
        }
    }

    private fun parseDate(dateString: String): Long {
        return try {
            val format = SimpleDateFormat("dd.MM.yyyy", Locale.getDefault())
            format.parse(dateString)?.time ?: 0L
        } catch (e: Exception) {
            0L
        }
    }

    private fun showEmptyRecapMessage() {
        binding.emptyTextView.visibility = View.VISIBLE
        binding.recapRecyclerView.visibility = View.GONE
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}