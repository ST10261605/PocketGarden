package com.example.pocketgarden

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageButton
import android.widget.Toast
import com.example.pocketgarden.databinding.FragmentHomePageBinding
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions


// TODO: Rename parameter arguments, choose names that match
// the fragment initialization parameters, e.g. ARG_ITEM_NUMBER
private const val ARG_PARAM1 = "param1"
private const val ARG_PARAM2 = "param2"

/**
 * A simple [Fragment] subclass.
 * Use the [HomePageFragment.newInstance] factory method to
 * create an instance of this fragment.
 */
class HomePageFragment : Fragment() {
    // TODO: Rename and change types of parameters
    private var param1: String? = null
    private var param2: String? = null

    private lateinit var googleSignInClient: GoogleSignInClient
    private var _binding: FragmentHomePageBinding? = null
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
        _binding = FragmentHomePageBinding.inflate(inflater, container, false)
        return binding.root
    }

    // Inside HomePageFragment.kt

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Initialize GoogleSignInClient
        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestEmail()
            .build()

        GoogleSignIn.getClient(requireActivity(), gso)

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

        //Navigation buttons
        val identifyButton = view.findViewById<ImageButton>(R.id.imageButton2)
        val gardenButton = view.findViewById<ImageButton>(R.id.imageButton3)
        val diagnoseButton = view.findViewById<ImageButton>(R.id.imageButton4)
        val settingsButton = view.findViewById<Button>(R.id.button4)

        identifyButton.setOnClickListener {
            parentFragmentManager.beginTransaction()
                .replace(R.id.fragment_container, IdentifyPlantCameraFragment())
                .addToBackStack(null)
                .commit()
        }

        gardenButton.setOnClickListener {
            parentFragmentManager.beginTransaction()
                .replace(R.id.fragment_container, MyGardenFragment())
                .addToBackStack(null)
                .commit()
        }
        diagnoseButton.setOnClickListener {
            parentFragmentManager.beginTransaction()
                .replace(R.id.fragment_container, IdentifyPlantPageFragment())
                .addToBackStack(null)
                .commit()
        }
        settingsButton.setOnClickListener {
            parentFragmentManager.beginTransaction()
                .replace(R.id.fragment_container, SettingsPageFragment())
                .addToBackStack(null)
                .commit();
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
         * @return A new instance of fragment HomePageFragment.
         */
        // TODO: Rename and change types and number of parameters
        @JvmStatic
        fun newInstance(param1: String, param2: String) =
            HomePageFragment().apply {
                arguments = Bundle().apply {
                    putString(ARG_PARAM1, param1)
                    putString(ARG_PARAM2, param2)
                }
            }
    }
}