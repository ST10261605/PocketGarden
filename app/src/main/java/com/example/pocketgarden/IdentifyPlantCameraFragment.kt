package com.example.pocketgarden

import android.content.ContentValues
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageButton
import android.widget.Toast
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.example.pocketgarden.data.local.PlantEntity
import com.example.pocketgarden.repository.IdentificationResult
import com.example.pocketgarden.repository.PlantRepository
import com.example.pocketgarden.ui.identify.SuggestionUiModel
import com.example.pocketgarden.ui.identify.SuggestionsFragment
import kotlinx.coroutines.launch

class IdentifyPlantCameraFragment : Fragment() {

    private lateinit var previewView: PreviewView
    private lateinit var captureButton: Button
    private lateinit var galleryButton: ImageButton
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

        startCamera()

        captureButton.setOnClickListener { takePhoto() }
        galleryButton.setOnClickListener { /* TODO: open gallery */ }
    }

    private fun startCamera() {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(requireContext())
        cameraProviderFuture.addListener({
            val cameraProvider = cameraProviderFuture.get()

            val preview = Preview.Builder().build().also {
                it.setSurfaceProvider(previewView.surfaceProvider)
            }

            imageCapture = ImageCapture.Builder().build()

            try {
                cameraProvider.unbindAll()
                cameraProvider.bindToLifecycle(
                    this,
                    CameraSelector.DEFAULT_BACK_CAMERA,
                    preview,
                    imageCapture
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

    private fun handlePhotoSaved(savedUri: Uri) {
        Toast.makeText(requireContext(), "Photo saved!", Toast.LENGTH_SHORT).show()

        lifecycleScope.launch {
            try {
                // Save pending entity
                val localId = plantRepository.addPlantOffline(savedUri.toString())

                // Convert image to Base64
                val base64 = plantRepository.apiKeyProvider.readUriAsBase64(savedUri.toString())

                // Identify
                when (val result = plantRepository.identifyPlantFromBitmapBase64V3(base64)) {
                    is IdentificationResult.Success -> {
                        val response = result.response

                        // map suggestions safely, including imageUrl extraction
                        val suggestions = response?.result?.classification?.suggestions?.map { s ->
                            val imageUrl = (s.details?.get("image") as? Map<*, *>)?.get("value") as? String
                            SuggestionUiModel(
                                name = s.plant_name ?: s.name ?: "Unknown",
                                probability = s.probability ?: 0.0,
                                commonNames = s.common_names ?: emptyList(),
                                imageUrl = imageUrl ?: ""
                            )
                        } ?: emptyList()

                        // Update local DB for first suggestion
                        val firstSuggestion = suggestions.firstOrNull()
                        val updated = PlantEntity(
                            localId = localId,
                            remoteId = response?.id,
                            imageUri = savedUri.toString(),
                            name = firstSuggestion?.name ?: "Unknown",
                            synced = true,
                            status = "IDENTIFIED"
                        )
                        plantRepository.savePlant(updated)

                        // Pass suggestions to fragment
                        val bundle = Bundle().apply {
                            putSerializable("suggestions", ArrayList(suggestions))
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
                        Toast.makeText(
                            requireContext(),
                            "Identification failed: ${result.message}",
                            Toast.LENGTH_LONG
                        ).show()
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
                Toast.makeText(
                    requireContext(),
                    "Error: ${e.localizedMessage}",
                    Toast.LENGTH_LONG
                ).show()
            }
        }
    }

}
