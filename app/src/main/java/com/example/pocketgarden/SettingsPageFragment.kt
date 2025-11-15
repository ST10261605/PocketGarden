package com.example.pocketgarden

import android.content.Context
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.Spinner
import android.widget.Switch
import android.widget.Toast
import com.example.pocketgarden.databinding.FragmentMyGardenBinding
import com.example.pocketgarden.databinding.FragmentSettingsPageBinding
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
import androidx.core.content.ContextCompat
import com.google.android.material.materialswitch.MaterialSwitch


private const val ARG_PARAM1 = "param1"
private const val ARG_PARAM2 = "param2"
private var _binding: FragmentSettingsPageBinding? = null
private val binding get() = _binding!!

class SettingsPageFragment : Fragment() {
    private var param1: String? = null
    private var param2: String? = null
    private lateinit var googleSignInClient: GoogleSignInClient

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {
            param1 = it.getString(ARG_PARAM1)
            param2 = it.getString(ARG_PARAM2)
        }

        // Initialize GoogleSignInClient
        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestEmail()
            .build()
        googleSignInClient = GoogleSignIn.getClient(requireActivity(), gso)
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {

        val lang = LocaleHelper.loadLocale(requireContext())
        LocaleHelper.setLocale(requireContext(), lang!!)

        _binding = FragmentSettingsPageBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        //biometrics
        val switchFingerprint = view.findViewById< MaterialSwitch>(R.id.switchFingerprint)
        val prefs = requireActivity().getSharedPreferences("AppSettings", Context.MODE_PRIVATE)
        val isEnabled = prefs.getBoolean("biometric_enabled", false)
        switchFingerprint.isChecked = isEnabled
        val spinner = view.findViewById<Spinner>(R.id.spinner)

        val languages = resources.getStringArray(R.array.languages)
        val adapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_item, languages)
        spinner.adapter = adapter

        // Pre-select saved language
        val savedLang = LocaleHelper.loadLocale(requireContext())
        val langCodes = resources.getStringArray(R.array.language_codes)
        val index = langCodes.indexOf(savedLang)
        if (index >= 0) spinner.setSelection(index)

        // Spinner change listener
        spinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(
                parent: AdapterView<*>,
                view: View?,
                position: Int,
                id: Long
            ) {
                val selectedCode = langCodes[position]
                val activity = requireActivity()
                LocaleHelper.setLocale(activity, selectedCode)

                // Recreate activity to apply new locale
                activity.recreate()
            }

            override fun onNothingSelected(p0: AdapterView<*>?) {}
        }


        switchFingerprint.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked) {
                val biometricManager = BiometricManager.from(requireContext())
                if (biometricManager.canAuthenticate(BiometricManager.Authenticators.BIOMETRIC_STRONG or BiometricManager.Authenticators.BIOMETRIC_WEAK)
                    == BiometricManager.BIOMETRIC_SUCCESS) {

                    val executor = ContextCompat.getMainExecutor(requireContext())
                    val prompt = BiometricPrompt(this, executor,
                        object : BiometricPrompt.AuthenticationCallback() {
                            override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                                super.onAuthenticationSucceeded(result)
                                prefs.edit().putBoolean("biometric_enabled", true).apply()
                                Toast.makeText(requireContext(), "Fingerprint enabled", Toast.LENGTH_SHORT).show()
                            }

                            override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                                super.onAuthenticationError(errorCode, errString)
                                switchFingerprint.isChecked = false
                                Toast.makeText(requireContext(), "Error: $errString", Toast.LENGTH_SHORT).show()
                            }
                        })

                    val info = BiometricPrompt.PromptInfo.Builder()
                        .setTitle("Enable Fingerprint Login")
                        .setDescription("Authenticate to enable fingerprint login")
                        .setNegativeButtonText("Cancel")
                        .build()
                    prompt.authenticate(info)
                } else {
                    Toast.makeText(requireContext(), "No biometric features available", Toast.LENGTH_LONG).show()
                    switchFingerprint.isChecked = false
                }
            } else {
                prefs.edit().putBoolean("biometric_enabled", false).apply()
                Toast.makeText(requireContext(), "Fingerprint disabled", Toast.LENGTH_SHORT).show()
            }
        }

        // Update Profile button navigation
        val updateProfileBtn = view.findViewById<Button>(R.id.button24)
        updateProfileBtn.setOnClickListener {
            // Check if the user is logged in with Google
            val googleAccount = GoogleSignIn.getLastSignedInAccount(requireContext())
            if (googleAccount != null) {
                // Google user: show read-only profile fragment
                parentFragmentManager.beginTransaction()
                    .replace(R.id.fragment_container, ReadOnlyFragment())
                    .addToBackStack(null)
                    .commit()
                Toast.makeText(requireContext(), "Google account details cannot be edited", Toast.LENGTH_SHORT).show()
            } else {
                // Regular email/password user: allow editing
                parentFragmentManager.beginTransaction()
                    .replace(R.id.fragment_container, EditProfileFragment())
                    .addToBackStack(null)
                    .commit()
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

        // Logout button
        val logoutBtn = view.findViewById<Button>(R.id.Logoutbtn)
        logoutBtn.setOnClickListener {
            // Clear local session info
            val sharedPrefs = requireActivity().getSharedPreferences("UserPrefs", 0)
            sharedPrefs.edit().clear().apply()

            // Sign out Google if needed
            googleSignInClient.signOut().addOnCompleteListener {
                // Optional: revoke access to force account chooser next time
                googleSignInClient.revokeAccess()
            }

            // Navigate to LoginFragment
            parentFragmentManager.beginTransaction()
                .replace(R.id.fragment_container, LoginFragment())
                .commit()

            Toast.makeText(requireContext(), "Logged out successfully", Toast.LENGTH_SHORT).show()
        }
    }

    companion object {
        @JvmStatic
        fun newInstance(param1: String, param2: String) =
            SettingsPageFragment().apply {
                arguments = Bundle().apply {
                    putString(ARG_PARAM1, param1)
                    putString(ARG_PARAM2, param2)
                }
            }
    }
}
