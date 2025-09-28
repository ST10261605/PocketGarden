package com.example.pocketgarden

import android.content.ContentValues
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.util.Log
import androidx.fragment.app.Fragment
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
import androidx.core.os.requestProfiling
import android.Manifest

// TODO: Rename parameter arguments, choose names that match
// the fragment initialization parameters, e.g. ARG_ITEM_NUMBER
private const val ARG_PARAM1 = "param1"
private const val ARG_PARAM2 = "param2"

private lateinit var previewView: PreviewView
private  lateinit var captureButton: Button
private lateinit var galleryButton: ImageButton
private var imageCapture: ImageCapture? = null

/**
 * A simple [Fragment] subclass.
 * Use the [IdentifyPlantCameraFragment.newInstance] factory method to
 * create an instance of this fragment.
 */
class IdentifyPlantCameraFragment : Fragment() {
    // TODO: Rename and change types of parameters
    private var param1: String? = null
    private var param2: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {
            param1 = it.getString(ARG_PARAM1)
            param2 = it.getString(ARG_PARAM2)
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_identify_plant_camera, container, false)
    }

    override fun onViewCreated(view:View, savedInstanceState: Bundle?)
    {
        super.onViewCreated(view, savedInstanceState)

        previewView = view.findViewById(R.id.previewView)
        captureButton = view.findViewById(R.id.button6)
        galleryButton = view.findViewById(R.id.imageButton9)

        if(ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.CAMERA)
            != PackageManager.PERMISSION_GRANTED)
        {
            requestPermissions(arrayOf(Manifest.permission.CAMERA),101)
        }
        else{
            startCamera()
        }
        captureButton.setOnClickListener { takePhoto() }
        galleryButton.setOnClickListener { openGallery() }
    }
    private fun startCamera() {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(requireContext())
        cameraProviderFuture.addListener({
            val cameraProvider = cameraProviderFuture.get()

            val preview = Preview.Builder().build().also()
            {
                it.setSurfaceProvider(previewView.surfaceProvider)
            }

            imageCapture = ImageCapture.Builder().build()

            try {
                cameraProvider.unbindAll()
                cameraProvider.bindToLifecycle(
                    this,
                    CameraSelector.DEFAULT_BACK_CAMERA, preview, imageCapture
                )

            } catch (exc: Exception) {
                Log.e("CameraX", "Use case binding failed", exc)

            }

        }, ContextCompat.getMainExecutor(requireContext()))

    }
        private fun takePhoto()
        {
            val imageCapture = imageCapture ?: return

            val name = "plant_${System.currentTimeMillis()}.jpg"
            val contentValues = ContentValues().apply()
            {
                put(MediaStore.MediaColumns.DISPLAY_NAME,name)
                put(MediaStore.MediaColumns.MIME_TYPE,"image/jpeg")
                put(MediaStore.Images.Media.RELATIVE_PATH,"Pictures/PocketGarden")
            }
            val outputOptions = ImageCapture.OutputFileOptions
                .Builder(requireContext().contentResolver,
                    MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                    contentValues
                ).build()

            imageCapture.takePicture(outputOptions,
                ContextCompat.getMainExecutor(requireContext()),
                object: ImageCapture.OnImageSavedCallback
                {
                    override fun onImageSaved(output: ImageCapture.OutputFileResults)
                    {
                        Toast.makeText(requireContext(), "Photo saved!", Toast.LENGTH_SHORT).show()

                        output.savedUri?.let{ uri ->
                            identifyPlant(uri)
                        }
                    }
                    override fun onError(exc: ImageCaptureException)
                    {
                        Log.e("CameraX","Photo capture failed: ${exc.message}",exc)
                    }
                })
        }
        private fun openGallery()
        {
            val intent = Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
            intent.type = "image/*"
            startActivity(intent)
        }

        private fun identifyPlant(imageUri: Uri)
        {
            Log.d("IdentifyPlant","Capture image: $imageUri")
        }

    companion object {
        /**
         * Use this factory method to create a new instance of
         * this fragment using the provided parameters.
         *
         * @param param1 Parameter 1.
         * @param param2 Parameter 2.
         * @return A new instance of fragment IdentifyPlantCameraFragment.
         */
        // TODO: Rename and change types and number of parameters
        @JvmStatic
        fun newInstance(param1: String, param2: String) =
            IdentifyPlantCameraFragment().apply {
                arguments = Bundle().apply {
                    putString(ARG_PARAM1, param1)
                    putString(ARG_PARAM2, param2)
                }
            }
    }
}