package com.example.pocketgarden.ui.identify

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.pocketgarden.R

class SuggestionAdapter(
    private val suggestions: List<SuggestionUiModel>,
    private val onSelect: (SuggestionUiModel) -> Unit
) : RecyclerView.Adapter<SuggestionAdapter.Holder>() {

    inner class Holder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val txtName: TextView = itemView.findViewById(R.id.txtSuggestionName)
        val txtProb: TextView = itemView.findViewById(R.id.txtSuggestionProbability)

        init {
            itemView.setOnClickListener {
                val pos = adapterPosition
                if (pos != RecyclerView.NO_POSITION) {
                    val item = suggestions[pos]
                    onSelect(item)
                }
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): Holder {
        val v = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_suggestion, parent, false)
        return Holder(v)
    }

    override fun onBindViewHolder(holder: Holder, position: Int) {
        val s = suggestions[position]
        holder.txtName.text = s.name
        holder.txtProb.text = "${(s.probability * 100).toInt()}%"
    }

    override fun getItemCount(): Int = suggestions.size
}

data class SuggestionUiModel(
    val name: String,
    val probability: Double,
    val commonNames: List<String> = emptyList()
)
