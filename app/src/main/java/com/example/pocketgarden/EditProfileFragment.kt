package com.example.pocketgarden

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.Toast
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.lifecycle.lifecycleScope
import androidx.room.Room
import com.example.pocketgarden.databinding.FragmentEditProfileBinding
import com.example.pocketgarden.databinding.FragmentMyGardenBinding
import kotlinx.coroutines.launch

// TODO: Rename parameter arguments, choose names that match
// the fragment initialization parameters, e.g. ARG_ITEM_NUMBER
private const val ARG_PARAM1 = "param1"
private const val ARG_PARAM2 = "param2"

/**
 * A simple [Fragment] subclass.
 * Use the [EditProfileFragment.newInstance] factory method to
 * create an instance of this fragment.
 */
class EditProfileFragment : Fragment() {
    // TODO: Rename and change types of parameters
    private var param1: String? = null
    private var param2: String? = null
    private lateinit var db: AppDatabase
    private lateinit var userDao: UserDAO
    private var currentUser: User? = null

    private lateinit var pickImageLauncher: ActivityResultLauncher<Intent>

    private var tempProfileImageUri: String? = null

    private var _binding: FragmentEditProfileBinding? = null
    private val binding get() = _binding!!



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
        _binding = FragmentEditProfileBinding.inflate(inflater, container, false)
        return binding.root
    }
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Initialize database
        db = AppDatabase.getDatabase(requireContext())
        userDao = db.userDao()

        // Get views
        val emailEdit = view.findViewById<EditText>(R.id.editTextEmail)
        val currentPasswordEdit = view.findViewById<EditText>(R.id.currentPassword)
        val newPasswordEdit = view.findViewById<EditText>(R.id.newPassword)
        val confirmNewPasswordEdit = view.findViewById<EditText>(R.id.confirmNewPassword)
        val updateButton = view.findViewById<Button>(R.id.buttonUpdate)

        pickImageLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == Activity.RESULT_OK) {
                val imageUri = result.data?.data
                if (imageUri != null) {
                    view.findViewById<ImageView>(R.id.profileImage).setImageURI(imageUri)
                    // Temporarily store URI to update when the user taps "Update"
                    tempProfileImageUri = imageUri.toString()
                }
            }
        }

        val changePhotoButton = view.findViewById<ImageButton>(R.id.changePhotoButton)
        changePhotoButton.setOnClickListener {
            val intent = Intent(Intent.ACTION_PICK)
            intent.type = "image/*"
            pickImageLauncher.launch(intent)
        }


        val loggedInEmail = SessionManager.getUserEmail(requireContext())

        if (loggedInEmail == null) {
            Toast.makeText(requireContext(), "No logged-in user found", Toast.LENGTH_SHORT).show()
            return
        }

        lifecycleScope.launch {
            currentUser = userDao.getUserByEmail(loggedInEmail)
            currentUser?.let { user ->
                emailEdit.setText(user.email)
                user.profileImageUri?.let { uri ->
                    view.findViewById<ImageView>(R.id.profileImage).setImageURI(Uri.parse(uri))
                }
            }
        }

        // Handle update button
        updateButton.setOnClickListener {
            val email = emailEdit.text.toString()
            val currentPassword = currentPasswordEdit.text.toString()
            val newPassword = newPasswordEdit.text.toString()
            val confirmPassword = confirmNewPasswordEdit.text.toString()

            lifecycleScope.launch {
                val user = currentUser

                if (user != null) {
                    var passwordToSave = user.password

                    // If user filled in the password section
                    if (currentPassword.isNotEmpty() || newPassword.isNotEmpty() || confirmPassword.isNotEmpty()) {
                        if (currentPassword != user.password) {
                            Toast.makeText(
                                requireContext(),
                                "Current password incorrect",
                                Toast.LENGTH_SHORT
                            ).show()
                            return@launch
                        }
                        if (newPassword != confirmPassword) {
                            Toast.makeText(
                                requireContext(),
                                "New passwords do not match",
                                Toast.LENGTH_SHORT
                            ).show()
                            return@launch
                        }
                        if (newPassword == currentPassword) {
                            Toast.makeText(
                                requireContext(),
                                "New password cannot be the same as current",
                                Toast.LENGTH_SHORT
                            ).show()
                            return@launch
                        }
                        if (newPassword.length < 6) {
                            Toast.makeText(
                                requireContext(),
                                "Password must be at least 6 characters",
                                Toast.LENGTH_SHORT
                            ).show()
                            return@launch
                        }
                        passwordToSave = newPassword
                    }
                    // Update user
                    val updatedUser = user.copy(
                        email = email,
                        password = passwordToSave,
                        profileImageUri = tempProfileImageUri ?: user.profileImageUri

                    )
                    userDao.updateUser(updatedUser)
                    SessionManager.saveUserEmail(requireContext(), email)
                    Toast.makeText(
                        requireContext(),
                        "Profile updated successfully",
                        Toast.LENGTH_SHORT
                    ).show()
                    parentFragmentManager.popBackStack()

                } else {
                    Toast.makeText(requireContext(), "User not found", Toast.LENGTH_SHORT).show()
                }
            }
        }
        binding.bottomNavigationView.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.nav_home -> {
                    parentFragmentManager.beginTransaction()
                        .replace(R.id.fragment_container, HomePageFragment())
                        .commit()
                    true
                }

                R.id.nav_garden -> {
                    parentFragmentManager.beginTransaction()
                        .replace(R.id.fragment_container, MyGardenFragment())
                        .commit()
                    true
                }

                R.id.nav_camera -> {
                    parentFragmentManager.beginTransaction()
                        .replace(R.id.fragment_container, IdentifyPlantCameraFragment())
                        .commit()
                    true
                }

                R.id.nav_settings -> {
                    parentFragmentManager.beginTransaction()
                        .replace(R.id.fragment_container, SettingsPageFragment())
                        .commit()
                    true
                }

                else -> false
            }
        }
    }
    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
    companion object {
        /**
         * Use this factory method to create a new instance of
         * this fragment using the provided parameters.
         *
         * @param param1 Parameter 1.
         * @param param2 Parameter 2.
         * @return A new instance of fragment EditProfileFragment.
         */
        // TODO: Rename and change types and number of parameters
        @JvmStatic
        fun newInstance(param1: String, param2: String) =
            EditProfileFragment().apply {
                arguments = Bundle().apply {
                    putString(ARG_PARAM1, param1)
                    putString(ARG_PARAM2, param2)
                }
            }
    }
}