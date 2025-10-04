package com.example.pocketgarden.ui.identify

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.pocketgarden.MyGardenFragment
import com.example.pocketgarden.R
import com.example.pocketgarden.repository.PlantRepository

class SuggestionsFragment : Fragment() {

    private lateinit var adapter: SuggestionAdapter
    private val viewModel: SuggestionsViewModel by viewModels {
        // factory if you need repository injected
        object : ViewModelProvider.Factory {
            override fun <T : androidx.lifecycle.ViewModel> create(modelClass: Class<T>): T {
                val repo = PlantRepository.getInstance(requireContext())
                return SuggestionsViewModel(repo) as T
            }
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val root = inflater.inflate(R.layout.screen_suggestions, container, false)

        val suggestionsArg: ArrayList<SuggestionUiModel>? =
            arguments?.getParcelableArrayList("suggestions")

        suggestionsArg?.let {
            viewModel.setSuggestions(it)
        }

        val rv: RecyclerView = root.findViewById(R.id.rvSuggestions)
        val txtSelected: TextView = root.findViewById(R.id.tvSelected)
        val btnConfirm: Button = root.findViewById(R.id.btnConfirm)


        adapter = SuggestionAdapter(emptyList()) { suggestion ->
            viewModel.selectSuggestion(suggestion)
        }

        rv.layoutManager = LinearLayoutManager(requireContext())
        rv.adapter = adapter

        // observe suggestions
        viewModel.suggestions.observe(viewLifecycleOwner) { list ->
            adapter = SuggestionAdapter(list) { s -> viewModel.selectSuggestion(s) }
            rv.adapter = adapter
        }

        // observe selection
        viewModel.selected.observe(viewLifecycleOwner) { sel ->
            txtSelected.text = sel?.name ?: "No plant selected"
            btnConfirm.isEnabled = sel != null
        }

        // confirm button saves plant + navigate
        btnConfirm.setOnClickListener {
            viewModel.confirmSelection()
            // example: navigate to My Garden
            parentFragmentManager.beginTransaction()
                .replace(R.id.fragment_container, MyGardenFragment())
                .addToBackStack(null)
                .commit()
        }

        return root
    }
}
