package com.example.pocketgarden.ui.garden

import android.net.Uri
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import android.widget.TextView
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.pocketgarden.R
import com.example.pocketgarden.data.local.PlantEntity
import com.example.pocketgarden.data.local.PlantNote
import com.example.pocketgarden.network.NetworkHelper
import com.example.pocketgarden.repository.PlantRepository
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class PlantAdapter(
    private val onRemoveClick: (PlantEntity) -> Unit,
    private val onWaterReminderClick: (PlantEntity) -> Unit,
    private val onFertilizerReminderClick: (PlantEntity) -> Unit,
    private val plantRepository: PlantRepository, // Added repository
    private val lifecycleOwner: LifecycleOwner, // Added lifecycle owner for coroutines
    private val networkHelper: NetworkHelper,
) : ListAdapter<PlantEntity, PlantAdapter.PlantViewHolder>(PlantDiffCallback) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PlantViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_plant_card, parent, false)
        return PlantViewHolder(view)
    }

    override fun onBindViewHolder(holder: PlantViewHolder, position: Int) {
        val plant = getItem(position)
        holder.bind(plant)
    }

    inner class PlantViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val plantImage: ImageView = itemView.findViewById(R.id.ivPlantImage)
        private val plantName: TextView = itemView.findViewById(R.id.tvPlantName)
        private val removeButton: Button = itemView.findViewById(R.id.btnRemovePlant)
        private val waterButton: Button = itemView.findViewById(R.id.btnWaterReminder)
        private val fertilizerButton: Button = itemView.findViewById(R.id.btnFertilizerReminder)
        private val reminderStatus: TextView = itemView.findViewById(R.id.tvReminderStatus)

        // Plant note views - also handle the case where these might not exist
        private val newNoteInput: EditText? = itemView.findViewById(R.id.etNewNote)
        private val addNoteButton: Button? = itemView.findViewById(R.id.btnAddNote)
        private val notesRecyclerView: RecyclerView? = itemView.findViewById(R.id.rvPlantNotes)
        private val notesLabel: TextView? = itemView.findViewById(R.id.tvNotesLabel)

        private lateinit var notesAdapter: PlantNotesAdapter

        fun bind(plant: PlantEntity) {
            // Load the plant image from the saved URI
            val uri = Uri.parse(plant.imageUri)
            Glide.with(itemView.context)
                .load(uri)
                .placeholder(R.drawable.ic_plant_placeholder)
                .error(R.drawable.ic_plant_placeholder)
                .centerCrop()
                .into(plantImage)

            // Set plant name
            plantName.text = plant.name

            // Set up button click listeners
            removeButton.setOnClickListener { onRemoveClick(plant) }
            waterButton.setOnClickListener { onWaterReminderClick(plant) }
            fertilizerButton.setOnClickListener { onFertilizerReminderClick(plant) }

            // Set reminder status here for reminders feature
            reminderStatus.text = "No reminders set"

            // Setup notes if the views exist
            setupNotes(plant)

            // Setting up water reminder button
            waterButton.setOnClickListener {
                onWaterReminderClick(plant)
            }

            // Update reminder status text
            updateReminderStatus(plant)
        }

        private fun updateReminderStatus(plant: PlantEntity) {
            val statusText = if (plant.waterReminderEnabled && plant.nextWatering != null) {
                val nextWateringDate = SimpleDateFormat(
                    "MMM dd, yyyy 'at' hh:mm a",
                    Locale.getDefault()
                )
                    .format(Date(plant.nextWatering!!))
                "Next watering: $nextWateringDate"
            } else {
                "No water reminder set"
            }
            reminderStatus.text = statusText
        }

        private fun setupNotes(plant: PlantEntity) {
            // Check if note views exist (they might not be in the layout yet)
            if (notesRecyclerView == null || newNoteInput == null || addNoteButton == null) {
                return // Notes feature not implemented in layout yet
            }

            // Initialize notes adapter
            notesAdapter = PlantNotesAdapter(
                onDeleteClick = { note ->
                    // Delete note
                    lifecycleOwner.lifecycleScope.launch {
                        plantRepository.deletePlantNote(note)
                    }
                },
                onEditClick = { note ->
                    // Edit note
                    newNoteInput.setText(note.content)
                    lifecycleOwner.lifecycleScope.launch {
                        plantRepository.deletePlantNote(note)
                    }
                }
            )

            notesRecyclerView.apply {
                layoutManager = LinearLayoutManager(itemView.context)
                adapter = notesAdapter
            }

            // Load notes for this plant
            lifecycleOwner.lifecycleScope.launch {
                plantRepository.getPlantNotes(plant.localId).collect { notes ->
                    notesAdapter.submitList(notes)
                    // Show/hide notes label based on whether there are notes
                    notesLabel?.visibility = if (notes.isNotEmpty()) View.VISIBLE else View.GONE
                }
            }

            // Add note button click
            addNoteButton.setOnClickListener {
                val noteContent = newNoteInput.text.toString().trim()
                if (noteContent.isNotEmpty()) {
                    // Access through the outer class reference
                    this@PlantAdapter.lifecycleOwner.lifecycleScope.launch {
                        val note = PlantNote(
                            plantLocalId = plant.localId,
                            content = noteContent
                        )
                        this@PlantAdapter.plantRepository.addPlantNote(note)
                        newNoteInput.text.clear()
                    }
                }
            }
        }
    }

    companion object {
        private val PlantDiffCallback = object : DiffUtil.ItemCallback<PlantEntity>() {
            override fun areItemsTheSame(oldItem: PlantEntity, newItem: PlantEntity): Boolean {
                return oldItem.localId == newItem.localId
            }

            override fun areContentsTheSame(oldItem: PlantEntity, newItem: PlantEntity): Boolean {
                return oldItem == newItem
            }
        }
    }
}