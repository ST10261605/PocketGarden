package com.example.pocketgarden

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.Toast
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions

private const val ARG_PARAM1 = "param1"
private const val ARG_PARAM2 = "param2"

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
        return inflater.inflate(R.layout.fragment_settings_page, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

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
