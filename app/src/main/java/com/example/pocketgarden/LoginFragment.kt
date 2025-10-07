package com.example.pocketgarden

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch
import android.content.Intent
import android.util.Log
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.SignInButton
import com.google.android.gms.common.api.ApiException

// TODO: Rename parameter arguments, choose names that match
// the fragment initialization parameters, e.g. ARG_ITEM_NUMBER
private const val ARG_PARAM1 = "param1"
private const val ARG_PARAM2 = "param2"

/**
 * A simple [Fragment] subclass.
 * Use the [LoginFragment.newInstance] factory method to
 * create an instance of this fragment.
 */
class LoginFragment : Fragment() {
    // TODO: Rename and change types of parameters
    private var param1: String? = null
    private var param2: String? = null
    private lateinit var db: AppDatabase
    private lateinit var userDao: UserDAO
    private lateinit var googleSignInClient: GoogleSignInClient

    private lateinit var googleSignInLauncher: androidx.activity.result.ActivityResultLauncher<Intent>



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
        return inflater.inflate(R.layout.fragment_login, container, false)
    }

    override fun onViewCreated(view:View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        db = AppDatabase.getDatabase(requireContext())
        userDao = db.userDao()


        setupEmailPasswordLogin(view)
        setupGoogleLogin(view)
    }

    private fun setupEmailPasswordLogin(view: View)
    {
        val emailEdit = view.findViewById<EditText>(R.id.editTextTextEmailAddress2)
        val passwordEdit = view.findViewById<EditText>(R.id.editTextTextPassword2)
        val loginButton = view.findViewById<Button>(R.id.Loginbtn)

        loginButton.setOnClickListener {
            val email = emailEdit.text.toString()
            val password = passwordEdit.text.toString()

            if (email.isNotEmpty() && password.isNotEmpty()) {
                lifecycleScope.launch {
                    val user = userDao.getUserByEmail(email) // fetch user from DB


                    if (user != null && user.password == password) {
                        // Login successful, navigate to home
                        SessionManager.saveUserEmail(requireContext(), email)

                        parentFragmentManager.beginTransaction()
                            .replace(R.id.fragment_container, HomePageFragment())
                            .commit()
                    } else {
                        Toast.makeText(
                            requireContext(),
                            "Invalid email or password",
                            Toast.LENGTH_SHORT
                        )
                            .show()
                    }
                }
            } else {
                Toast.makeText(requireContext(), "Please fill all fields", Toast.LENGTH_SHORT)
                    .show()
            }
        }
    }

    private fun setupGoogleLogin(view: View) {
        val googleBtn = view.findViewById<SignInButton>(R.id.GoogleSignInbtn)

        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestEmail()
            .build()

        googleSignInClient = GoogleSignIn.getClient(requireActivity(), gso)

        googleSignInLauncher = registerForActivityResult(
            androidx.activity.result.contract.ActivityResultContracts.StartActivityForResult()
        ) { result ->
            val data = result.data
            val task = GoogleSignIn.getSignedInAccountFromIntent(data)
            try {
                val account = task.getResult(ApiException::class.java)
                val email = account.email
                val name = account?.displayName
                Toast.makeText(requireContext(), "Welcome $name!", Toast.LENGTH_SHORT).show()
                navigateToHome()
            } catch (e: ApiException) {
                Log.w("LoginFragment", "Google sign in failed", e)
                Toast.makeText(requireContext(), "Google Sign-In Failed", Toast.LENGTH_SHORT).show()
            }
        }
        googleBtn.setOnClickListener {
            googleSignInClient.revokeAccess().addOnCompleteListener {
                val signInIntent = googleSignInClient.signInIntent
                googleSignInLauncher.launch(signInIntent)
            }
        }
    }
    private fun navigateToHome()
    {
        parentFragmentManager.beginTransaction()
            .replace(R.id.fragment_container, HomePageFragment())
            .commit()
    }


    companion object {
        /**
         * Use this factory method to create a new instance of
         * this fragment using the provided parameters.
         *
         * @param param1 Parameter 1.
         * @param param2 Parameter 2.
         * @return A new instance of fragment LoginFragment.
         */
        // TODO: Rename and change types and number of parameters
        @JvmStatic
        fun newInstance(param1: String, param2: String) =
            LoginFragment().apply {
                arguments = Bundle().apply {
                    putString(ARG_PARAM1, param1)
                    putString(ARG_PARAM2, param2)
                }
            }
    }
}