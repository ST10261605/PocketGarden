package com.example.pocketgarden.ui.garden

import android.app.Dialog
import android.app.TimePickerDialog
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.DialogFragment
import androidx.lifecycle.lifecycleScope
import com.example.pocketgarden.data.local.PlantEntity
import com.example.pocketgarden.databinding.DialogWaterReminderBinding
import com.example.pocketgarden.repository.PlantRepository
import kotlinx.coroutines.launch
import java.util.Calendar

class WaterReminderDialogFragment(
    private val plant: PlantEntity,
    private val plantRepository: PlantRepository,
    private val onReminderSet: () -> Unit
) : DialogFragment() {

    private lateinit var binding: DialogWaterReminderBinding
    private var selectedTime: Long = System.currentTimeMillis()

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        binding = DialogWaterReminderBinding.inflate(layoutInflater)

        setupFrequencyPicker()
        setupTimePicker()

        return AlertDialog.Builder(requireContext())
            .setTitle("Set Water Reminder for ${plant.name}")
            .setView(binding.root)
            .setPositiveButton("Set Reminder") { _, _ ->
                setWaterReminder()
            }
            .setNegativeButton("Cancel") { _, _ ->
                dismiss()
            }
            .setNeutralButton("Remove Reminder") { _, _ ->
                removeWaterReminder()
            }
            .create()
    }

    private fun setupFrequencyPicker() {
        binding.frequencyPicker.minValue = 1
        binding.frequencyPicker.maxValue = 30
        binding.frequencyPicker.value = plant.wateringFrequency
        binding.frequencyPicker.displayedValues = (1..30).map { "$it days" }.toTypedArray()
    }

    private fun setupTimePicker() {
        binding.timePickerButton.setOnClickListener {
            showTimePicker()
        }

        // Set initial time
        val calendar = Calendar.getInstance()
        selectedTime = plant.nextWatering ?: System.currentTimeMillis()
        calendar.timeInMillis = selectedTime
        updateTimeButton(calendar)
    }

    private fun showTimePicker() {
        val calendar = Calendar.getInstance().apply {
            timeInMillis = selectedTime
        }

        val timePicker = TimePickerDialog(
            requireContext(),
            { _, hourOfDay, minute ->
                val selectedCalendar = Calendar.getInstance().apply {
                    set(Calendar.HOUR_OF_DAY, hourOfDay)
                    set(Calendar.MINUTE, minute)
                    set(Calendar.SECOND, 0)

                    // If the time has already passed today, schedule for tomorrow
                    if (timeInMillis <= System.currentTimeMillis()) {
                        add(Calendar.DAY_OF_YEAR, 1)
                    }
                }
                selectedTime = selectedCalendar.timeInMillis
                updateTimeButton(selectedCalendar)
            },
            calendar.get(Calendar.HOUR_OF_DAY),
            calendar.get(Calendar.MINUTE),
            false
        )
        timePicker.show()
    }

    private fun updateTimeButton(calendar: Calendar) {
        val timeText = String.format("%02d:%02d", calendar.get(Calendar.HOUR_OF_DAY), calendar.get(Calendar.MINUTE))
        binding.timePickerButton.text = timeText
    }

    private fun setWaterReminder() {
        lifecycleScope.launch {
            try {
                val frequency = binding.frequencyPicker.value

                val calendar = Calendar.getInstance().apply {
                    timeInMillis = selectedTime
                }
                val timeOfDay = String.format("%02d:%02d", calendar.get(Calendar.HOUR_OF_DAY), calendar.get(Calendar.MINUTE))

                // Assume success for now
                plantRepository.setWaterReminder(plant, frequency, timeOfDay)

                // Check if fragment is still added first, then proceed
                if (!isAdded) {
                    return@launch
                }

                // For now, always show success since we don't have the boolean return
                Toast.makeText(requireContext(), "Water reminder set!", Toast.LENGTH_SHORT).show()
                onReminderSet()
                dismiss()

            } catch (e: Exception) {
                if (isAdded) {
                    val errorMessage = "Failed to set reminder: ${e.localizedMessage ?: e.message}"
                    Toast.makeText(requireContext(), errorMessage, Toast.LENGTH_LONG).show()
                }
                e.printStackTrace()
            }
        }
    }

    private fun removeWaterReminder() {
        lifecycleScope.launch {
            try {
                plantRepository.cancelWaterReminder(plant)

                if (isAdded) {
                    Toast.makeText(requireContext(), "Water reminder removed", Toast.LENGTH_SHORT).show()
                    onReminderSet()
                    dismiss()
                }
            } catch (e: Exception) {
                if (isAdded) {
                    Toast.makeText(requireContext(), "Failed to remove reminder: ${e.message}", Toast.LENGTH_SHORT).show()
                }
                e.printStackTrace()
            }
        }
    }

    companion object {
        const val TAG = "WaterReminderDialog"
    }
}