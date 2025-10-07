package com.example.pocketgarden

import android.content.ContentValues
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.util.Base64
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageButton
import android.widget.Toast
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.example.pocketgarden.data.local.PlantEntity
import com.example.pocketgarden.network.IdentificationResponse
import com.example.pocketgarden.repository.IdentificationResult
import com.example.pocketgarden.repository.PlantRepository
import com.example.pocketgarden.ui.identify.SuggestionUiModel
import com.example.pocketgarden.ui.identify.SuggestionsFragment
import kotlinx.coroutines.launch
import java.io.ByteArrayOutputStream

class IdentifyPlantCameraFragment : Fragment() {

    private lateinit var previewView: PreviewView
    private lateinit var captureButton: Button
    private lateinit var galleryButton: ImageButton

    private lateinit var testSampleButton: Button

    private var imageCapture: ImageCapture? = null

    private val plantRepository: PlantRepository by lazy {
        PlantRepository.getInstance(requireContext())
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        return inflater.inflate(R.layout.fragment_identify_plant_camera, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        previewView = view.findViewById(R.id.previewView)
        captureButton = view.findViewById(R.id.button6)
        galleryButton = view.findViewById(R.id.imageButton9)
        testSampleButton = view.findViewById(R.id.btnTestSample)

        startCamera()

        captureButton.setOnClickListener { takePhoto() }
        galleryButton.setOnClickListener { /* TODO: open gallery */ }
        testSampleButton.setOnClickListener { testWithSamplePlant() }
    }

    private fun startCamera() {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(requireContext())
        cameraProviderFuture.addListener({
            val cameraProvider = cameraProviderFuture.get()

            val preview = Preview.Builder().build().also {
                it.setSurfaceProvider(previewView.surfaceProvider)
            }

            // Configure image capture for better plant identification
            imageCapture = ImageCapture.Builder()
                .setCaptureMode(ImageCapture.CAPTURE_MODE_MAXIMIZE_QUALITY)
                .setTargetRotation(previewView.display.rotation)
                .build()

            val imageAnalysis = ImageAnalysis.Builder()
                .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                .build()

            try {
                cameraProvider.unbindAll()
                cameraProvider.bindToLifecycle(
                    this,
                    CameraSelector.DEFAULT_BACK_CAMERA,
                    preview,
                    imageCapture,
                    imageAnalysis
                )
            } catch (exc: Exception) {
                Log.e("CameraX", "Use case binding failed", exc)
            }
        }, ContextCompat.getMainExecutor(requireContext()))
    }


    private fun takePhoto() {
        val imageCapture = imageCapture ?: return

        val name = "plant_${System.currentTimeMillis()}.jpg"
        val contentValues = ContentValues().apply {
            put(MediaStore.MediaColumns.DISPLAY_NAME, name)
            put(MediaStore.MediaColumns.MIME_TYPE, "image/jpeg")
            put(MediaStore.Images.Media.RELATIVE_PATH, "Pictures/PocketGarden")
        }

        val outputOptions = ImageCapture.OutputFileOptions.Builder(
            requireContext().contentResolver,
            MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
            contentValues
        ).build()

        imageCapture.takePicture(
            outputOptions,
            ContextCompat.getMainExecutor(requireContext()),
            object : ImageCapture.OnImageSavedCallback {
                override fun onImageSaved(output: ImageCapture.OutputFileResults) {
                    output.savedUri?.let { uri -> handlePhotoSaved(uri) }
                }

                override fun onError(exc: ImageCaptureException) {
                    Log.e("CameraX", "Photo capture failed: ${exc.message}", exc)
                }
            }
        )
    }

    // testing
    private fun testWithSamplePlant() {
        Toast.makeText(requireContext(), "Testing with sample plant...", Toast.LENGTH_SHORT).show()

        lifecycleScope.launch {
            try {
                Log.d("CameraFragment", "Loading sample plant image from resources...")

                // Load sample image from raw resources
                val inputStream = requireContext().resources.openRawResource(R.raw.sample_plant)
                val bytes = inputStream.readBytes()
                inputStream.close()

                Log.d("CameraFragment", "Sample image size: ${bytes.size} bytes")

                // Convert to PURE Base64 (no data URL prefix)
                val base64 = Base64.encodeToString(bytes, Base64.NO_WRAP)
                Log.d("CameraFragment", "Pure base64 length: ${base64.length}")

                // Identify the plant
                when (val result = plantRepository.identifyPlantFromBitmapBase64V3(base64)) {
                    is IdentificationResult.Success -> {
                        val response = result.response
                        Log.d("CameraFragment", "Sample plant identification successful!")

                        // Map suggestions
                        val suggestions = response?.result?.classification?.suggestions?.map { s ->
                            SuggestionUiModel(
                                name = s.name ?: "Unknown Plant",
                                probability = s.probability ?: 0.0,
                                commonNames = s.plantDetails?.commonNames ?: emptyList(),
                                imageUrl = s.similarImages?.firstOrNull()?.url ?: ""
                            )
                        } ?: emptyList()

                        Log.d("CameraFragment", "Found ${suggestions.size} suggestions for sample plant")

                        if (suggestions.isNotEmpty()) {
                            // Save to database with a placeholder URI
                            val samplePlantEntity = PlantEntity(
                                imageUri = "resource://sample_plant",
                                name = suggestions.first().name,
                                probability = suggestions.first().probability,
                                commonNames = suggestions.first().commonNames.joinToString(", "),
                                synced = true,
                                status = "IDENTIFIED"
                            )
                            plantRepository.savePlant(samplePlantEntity)

                            // Show suggestions
                            val bundle = Bundle().apply {
                                putParcelableArrayList("suggestions", ArrayList(suggestions))
                            }
                            val fragment = SuggestionsFragment().apply {
                                arguments = bundle
                            }

                            parentFragmentManager.beginTransaction()
                                .replace(R.id.fragment_container, fragment)
                                .addToBackStack(null)
                                .commit()
                        } else {
                            Toast.makeText(
                                requireContext(),
                                "No suggestions found with sample image. The API might not recognize this plant.",
                                Toast.LENGTH_LONG
                            ).show()
                        }
                    }
                    is IdentificationResult.Error -> {
                        Log.e("CameraFragment", "Sample plant test failed: ${result.message}")
                        Toast.makeText(
                            requireContext(),
                            "Test failed: ${result.message}",
                            Toast.LENGTH_LONG
                        ).show()
                    }
                }
            } catch (e: Exception) {
                Log.e("CameraFragment", "Error in testWithSamplePlant: ${e.message}", e)
                Toast.makeText(
                    requireContext(),
                    "Test error: ${e.localizedMessage}",
                    Toast.LENGTH_LONG
                ).show()
            }
        }
    }

    private fun handlePhotoSaved(savedUri: Uri) {
        Toast.makeText(requireContext(), "Photo saved!", Toast.LENGTH_SHORT).show()

        lifecycleScope.launch {
            try {
                // Save pending entity
                val localId = plantRepository.addPlantOffline(savedUri.toString())

                // Convert image to Base64
                val base64 = plantRepository.apiKeyProvider.readUriAsBase64(savedUri.toString())
                Log.d("CameraFragment", "Image converted to Base64, length: ${base64.length}")

                // Identify
                when (val result = plantRepository.identifyPlantFromBitmapBase64V3(base64)) {
                    is IdentificationResult.Success -> {
                        val response = result.response
                        Log.d("CameraFragment", "Identification successful, mapping suggestions...")

                        // Extract suggestions from different possible structures
                        val suggestions = extractSuggestions(response)
                        Log.d("CameraFragment", "Mapped ${suggestions.size} suggestions")

                        if (suggestions.isEmpty()) {
                            Toast.makeText(
                                requireContext(),
                                "No plant suggestions found. Try a clearer photo.",
                                Toast.LENGTH_LONG
                            ).show()
                            return@launch
                        }

                        // Update local DB for first suggestion
                        val firstSuggestion = suggestions.firstOrNull()
                        val updated = PlantEntity(
                            localId = localId,
                            remoteId = response?.id?.toString(),
                            imageUri = savedUri.toString(),
                            name = firstSuggestion?.name ?: "Unknown Plant",
                            synced = true,
                            status = "IDENTIFIED"
                        )
                        plantRepository.savePlant(updated)

                        // Pass suggestions to fragment
                        val bundle = Bundle().apply {
                            putParcelableArrayList("suggestions", ArrayList(suggestions))
                        }
                        val fragment = SuggestionsFragment().apply {
                            arguments = bundle
                        }

                        parentFragmentManager.beginTransaction()
                            .replace(R.id.fragment_container, fragment)
                            .addToBackStack(null)
                            .commit()
                    }

                    is IdentificationResult.Error -> {
                        Log.e("CameraFragment", "Identification failed: ${result.message}")
                        Toast.makeText(
                            requireContext(),
                            "Identification failed: ${result.message}",
                            Toast.LENGTH_LONG
                        ).show()
                    }
                }
            } catch (e: Exception) {
                Log.e("CameraFragment", "Error in handlePhotoSaved: ${e.message}", e)
                Toast.makeText(
                    requireContext(),
                    "Error: ${e.localizedMessage}",
                    Toast.LENGTH_LONG
                ).show()
            }
        }
    }

    private fun extractSuggestions(response: IdentificationResponse?): List<SuggestionUiModel> {
        return try {
            val suggestions = mutableListOf<SuggestionUiModel>()

            // Try v3 structure: result -> classification -> suggestions
            response?.result?.classification?.suggestions?.forEach { suggestion ->
                suggestions.add(
                    SuggestionUiModel(
                        name = suggestion.plantName ?: suggestion.name ?: "Unknown Plant",
                        probability = suggestion.probability ?: 0.0,
                        commonNames = suggestion.plantDetails?.commonNames ?: emptyList(),
                        imageUrl = suggestion.similarImages?.firstOrNull()?.url ?: ""
                    )
                )
            }

            // If no suggestions found in v3 structure, try v2 structure: direct suggestions
            if (suggestions.isEmpty()) {
                response?.suggestions?.forEach { suggestion ->
                    suggestions.add(
                        SuggestionUiModel(
                            name = suggestion.plantName ?: suggestion.name ?: "Unknown Plant",
                            probability = suggestion.probability ?: 0.0,
                            commonNames = suggestion.plantDetails?.commonNames ?: emptyList(),
                            imageUrl = suggestion.similarImages?.firstOrNull()?.url ?: ""
                        )
                    )
                }
            }

            // If still no suggestions, try v3 alternative: result -> suggestions
            if (suggestions.isEmpty()) {
                response?.result?.suggestions?.forEach { suggestion ->
                    suggestions.add(
                        SuggestionUiModel(
                            name = suggestion.plantName ?: suggestion.name ?: "Unknown Plant",
                            probability = suggestion.probability ?: 0.0,
                            commonNames = suggestion.plantDetails?.commonNames ?: emptyList(),
                            imageUrl = suggestion.similarImages?.firstOrNull()?.url ?: ""
                        )
                    )
                }
            }

            suggestions
        } catch (e: Exception) {
            Log.e("CameraFragment", "Error extracting suggestions: ${e.message}", e)
            emptyList()
        }
    }
}
