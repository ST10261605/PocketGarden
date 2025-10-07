package com.example.pocketgarden

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.pocketgarden.data.local.PlantEntity
import com.example.pocketgarden.repository.PlantRepository
import com.example.pocketgarden.ui.garden.PlantAdapter
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

class MyGardenFragment : Fragment() {

    private lateinit var plantRepository: PlantRepository
    private lateinit var plantAdapter: PlantAdapter
    private lateinit var recyclerView: RecyclerView
    private lateinit var emptyState: View

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        // Inflate the layout using the resource ID
        return inflater.inflate(R.layout.fragment_my_garden, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Initialize views using findViewById
        recyclerView = view.findViewById(R.id.rvPlants)
        emptyState = view.findViewById(R.id.emptyState)

        plantRepository = PlantRepository.getInstance(requireContext())
        setupRecyclerView()
        observePlants()
    }

    private fun setupRecyclerView() {
        plantAdapter = PlantAdapter(
            onRemoveClick = { plant -> removePlant(plant) },
            onWaterReminderClick = { plant -> setWaterReminder(plant) },
            onFertilizerReminderClick = { plant -> setFertilizerReminder(plant) }
        )

        recyclerView.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = plantAdapter
        }
    }

    private fun observePlants() {
        lifecycleScope.launch {
            plantRepository.getAllPlantsFlow().collectLatest { plants ->
                Log.d("MyGardenFragment", "Observed ${plants.size} plants")

                if (plants.isEmpty()) {
                    showEmptyState()
                } else {
                    showPlantsList()
                    plantAdapter.submitList(plants)
                }
            }
        }
    }

    private fun showEmptyState() {
        recyclerView.visibility = View.GONE
        emptyState.visibility = View.VISIBLE
    }

    private fun showPlantsList() {
        recyclerView.visibility = View.VISIBLE
        emptyState.visibility = View.GONE
    }

    private fun removePlant(plant: PlantEntity) {
        lifecycleScope.launch {
            try {
                plantRepository.deletePlant(plant)
                // The list will automatically update via Flow observation
            } catch (e: Exception) {
                Log.e("MyGardenFragment", "Error removing plant: ${e.message}")
            }
        }
    }

    private fun setWaterReminder(plant: PlantEntity) {
        // TODO: Implement water reminder functionality
        Log.d("MyGardenFragment", "Set water reminder for: ${plant.name}")
    }

    private fun setFertilizerReminder(plant: PlantEntity) {
        // TODO: Implement fertilizer reminder functionality
        Log.d("MyGardenFragment", "Set fertilizer reminder for: ${plant.name}")
    }
}