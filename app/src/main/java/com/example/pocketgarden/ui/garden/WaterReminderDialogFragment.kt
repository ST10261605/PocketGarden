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
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale
import java.util.Timer
import java.util.TimerTask

class WaterReminderDialogFragment(
    private val plant: PlantEntity,
    private val plantRepository: PlantRepository,
    private val onReminderSet: () -> Unit
) : DialogFragment() {

    private lateinit var binding: DialogWaterReminderBinding
    private var selectedTime: Long = System.currentTimeMillis()
    private var timer: Timer? = null // Add timer property

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        binding = DialogWaterReminderBinding.inflate(layoutInflater)

        setupFrequencyPicker()
        setupTimePicker()
        setupCurrentTimeDisplay()

        return AlertDialog.Builder(requireContext())
            .setTitle("Set Water Reminder for ${plant.name}")
            .setView(binding.root)
            .setPositiveButton("Set Reminder") { _, _ ->
                setWaterReminder()
            }
            .setNegativeButton("Cancel") { _, _ ->
                stopTimer()
                dismiss()
            }
            .setNeutralButton("Remove Reminder") { _, _ ->
                removeWaterReminder()
            }
            .setNeutralButton("Test Notification") { _, _ -> // test button
                testNotification()
            }
            .create()
    }

    private fun setupCurrentTimeDisplay() {
        // Update time immediately
        updateCurrentTime()

        // Update time every second
        timer = Timer()
        timer?.scheduleAtFixedRate(object : TimerTask() {
            override fun run() {
                activity?.runOnUiThread {
                    updateCurrentTime()
                }
            }
        }, 0, 1000)
    }

    private fun updateCurrentTime() {
        val currentTime = Calendar.getInstance().time
        val timeFormat = SimpleDateFormat("HH:mm:ss", Locale.getDefault())
        val dateFormat = SimpleDateFormat("EEE, MMM d, yyyy", Locale.getDefault())

        // Check if views exist in layout before trying to update them
        updateTimeDisplayOnButton(timeFormat.format(currentTime), dateFormat.format(currentTime))
    }

    private fun updateTimeDisplayOnButton(currentTime: String, currentDate: String) {
        // Temporarily display time info on the time picker button
        binding.timePickerButton.text = "Select Time\nNow: $currentTime"
        binding.timePickerButton.textSize = 12f
    }

    private fun stopTimer() {
        timer?.cancel()
        timer = null
    }

    private fun testNotification() {
        lifecycleScope.launch {
            try {
                // Test notification immediately
                val testReminderId = "test_${System.currentTimeMillis()}"

                // using notification helper to show a test notification
                val notificationHelper = com.example.pocketgarden.notifications.NotificationHelper(requireContext())
                notificationHelper.showWaterReminder(plant.name, testReminderId)

                Toast.makeText(requireContext(), "Test notification sent!", Toast.LENGTH_SHORT).show()
            } catch (e: Exception) {
                Toast.makeText(requireContext(), "Test failed: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        stopTimer()
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
        // Reset button text to just show the selected time
        binding.timePickerButton.text = "Selected: $timeText"
        binding.timePickerButton.textSize = 14f
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