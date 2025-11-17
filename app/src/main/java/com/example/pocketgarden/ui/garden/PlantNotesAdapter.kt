package com.example.pocketgarden.ui.garden

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.pocketgarden.R
import com.example.pocketgarden.data.local.PlantNote
import java.text.SimpleDateFormat
import java.util.*

class PlantNotesAdapter(
    private val onDeleteClick: (PlantNote) -> Unit,
    private val onEditClick: (PlantNote) -> Unit
) : ListAdapter<PlantNote, PlantNotesAdapter.PlantNoteViewHolder>(PlantNoteDiffCallback) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PlantNoteViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_plant_note, parent, false)
        return PlantNoteViewHolder(view)
    }

    override fun onBindViewHolder(holder: PlantNoteViewHolder, position: Int) {
        val note = getItem(position)
        holder.bind(note)
    }

    inner class PlantNoteViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val noteContent: TextView = itemView.findViewById(R.id.tvNoteContent)
        private val noteDate: TextView = itemView.findViewById(R.id.tvNoteDate)
        private val deleteButton: ImageButton = itemView.findViewById(R.id.btnDeleteNote)
        private val editButton: ImageButton = itemView.findViewById(R.id.btnEditNote)

        fun bind(note: PlantNote) {
            noteContent.text = note.content
            noteDate.text = formatDate(note.createdAt)

            deleteButton.setOnClickListener { onDeleteClick(note) }
            editButton.setOnClickListener { onEditClick(note) }
        }

        private fun formatDate(timestamp: Long): String {
            val sdf = SimpleDateFormat("MMM dd, yyyy 'at' hh:mm a", Locale.getDefault())
            return sdf.format(Date(timestamp))
        }
    }

    companion object {
        private val PlantNoteDiffCallback = object : DiffUtil.ItemCallback<PlantNote>() {
            override fun areItemsTheSame(oldItem: PlantNote, newItem: PlantNote): Boolean {
                return oldItem.id == newItem.id
            }

            override fun areContentsTheSame(oldItem: PlantNote, newItem: PlantNote): Boolean {
                return oldItem == newItem
            }
        }
    }
}