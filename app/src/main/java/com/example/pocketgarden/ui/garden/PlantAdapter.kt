package com.example.pocketgarden.ui.garden

import android.net.Uri
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.pocketgarden.R
import com.example.pocketgarden.data.local.PlantEntity

class PlantAdapter(
    private val onRemoveClick: (PlantEntity) -> Unit,
    private val onWaterReminderClick: (PlantEntity) -> Unit,
    private val onFertilizerReminderClick: (PlantEntity) -> Unit
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

            //Set reminder status here for reminders feature
            reminderStatus.text = "No reminders set"
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