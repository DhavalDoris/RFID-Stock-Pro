package com.example.rfidstockpro.ui.fragments

import android.annotation.SuppressLint
import android.content.Intent
import android.os.Bundle
import android.text.InputType
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import com.example.rfidstockpro.R
import com.example.rfidstockpro.Utils.TextUtils
import com.example.rfidstockpro.databinding.FragmentSignupBinding
import com.example.rfidstockpro.ui.activities.DashboardActivity
import com.example.rfidstockpro.ui.activities.VerificationActivity
import com.example.rfidstockpro.viewmodel.AuthViewModel
import com.google.android.material.snackbar.Snackbar

class SignupFragment : Fragment() {

    private var binding: FragmentSignupBinding? = null
    private var isPasswordVisible = false
    private val authViewModel: AuthViewModel by viewModels()


    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentSignupBinding.inflate(inflater, container, false)

        TextUtils.applyUnderlineAndColor(requireActivity(), binding!!.tvTerms)

        setupPasswordToggle()
        observeValidationErrors()
        setupSignupButton()

        return binding!!.root
    }

    @SuppressLint("ClickableViewAccessibility")
    private fun setupPasswordToggle() {

        binding!!.etPassword.setOnTouchListener { _, event ->
            if (event.action == android.view.MotionEvent.ACTION_UP) {
                val drawableEnd = 2
                val drawable =
                    binding!!.etPassword.compoundDrawables[drawableEnd] // Get the drawableEnd icon

                if (drawable != null && event.rawX >= (binding!!.etPassword.right - drawable.bounds.width())) {
                    isPasswordVisible = !isPasswordVisible

                    // Toggle input type
                    binding!!.etPassword.inputType = if (isPasswordVisible) {
                        InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD
                    } else {
                        InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_PASSWORD
                    }

                    // Move cursor to the end
                    binding!!.etPassword.setSelection(binding!!.etPassword.text!!.length)

                    // Update the right drawable (eye icon)
                    val eyeIcon = if (isPasswordVisible) {
                        R.drawable.eye_hide_image // Replace with your hide icon
                    } else {
                        R.drawable.eye_visible_image // Replace with your show icon
                    }

                    binding!!.etPassword.setCompoundDrawablesWithIntrinsicBounds(
                        R.drawable.ic_password, 0, eyeIcon, 0
                    )

                    return@setOnTouchListener true
                }
            }
            false
        }

        binding!!.etConfirmPassword.setOnTouchListener { _, event ->
            if (event.action == android.view.MotionEvent.ACTION_UP) {
                val drawableEnd = 2
                val drawable =
                    binding!!.etConfirmPassword.compoundDrawables[drawableEnd] // Get the drawableEnd icon

                if (drawable != null && event.rawX >= (binding!!.etConfirmPassword.right - drawable.bounds.width())) {
                    isPasswordVisible = !isPasswordVisible

                    // Toggle input type
                    binding!!.etConfirmPassword.inputType = if (isPasswordVisible) {
                        InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD
                    } else {
                        InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_PASSWORD
                    }

                    // Move cursor to the end
                    binding!!.etConfirmPassword.setSelection(binding!!.etConfirmPassword.text!!.length)

                    // Update the right drawable (eye icon)
                    val eyeIcon = if (isPasswordVisible) {
                        R.drawable.eye_hide_image // Replace with your hide icon
                    } else {
                        R.drawable.eye_visible_image // Replace with your show icon
                    }

                    binding!!.etConfirmPassword.setCompoundDrawablesWithIntrinsicBounds(
                        R.drawable.ic_password, 0, eyeIcon, 0
                    )

                    return@setOnTouchListener true
                }
            }
            false
        }

    }

    private fun setupSignupButton() {
        binding!!.btnRegister.setOnClickListener {
            val username = binding!!.etUsername.text.toString().trim()
            val companyName = binding!!.etCompanyName.text.toString().trim()
            val email = binding!!.etEmail.text.toString().trim()
            val contactNumber = binding!!.etContactNumber.text.toString().trim()
            val password = binding!!.etPassword.text.toString().trim()
            val confirmPassword = binding!!.etConfirmPassword.text.toString().trim()

            if (authViewModel.validateSignup(
                    username,
                    companyName,
                    email,
                    contactNumber,
                    password,
                    confirmPassword
                )
            ) {
                Toast.makeText(requireContext(), "Signup Successful", Toast.LENGTH_SHORT).show()
                startActivity(Intent(requireActivity(), VerificationActivity::class.java))
                requireActivity().finish()
            }
        }
    }

    private fun observeValidationErrors() {
        authViewModel.signupError.observe(viewLifecycleOwner) { errorMessage ->
            showSnackbar(errorMessage)
        }
    }


    private fun showSnackbar(message: String) {
        Snackbar.make(requireView(), message, Snackbar.LENGTH_SHORT).show()
    }


    override fun onDestroyView() {
        super.onDestroyView()
        binding = null
    }
}
