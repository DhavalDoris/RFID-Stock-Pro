package com.example.rfidstockpro.ui.fragments

import android.annotation.SuppressLint
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
import com.example.rfidstockpro.databinding.FragmentLoginBinding
import com.example.rfidstockpro.viewmodel.AuthViewModel
import com.google.android.material.snackbar.Snackbar


class LoginFragment : Fragment() {

    private var binding: FragmentLoginBinding? = null
    private val authViewModel: AuthViewModel by viewModels()
    private var isPasswordVisible = false


    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        binding = FragmentLoginBinding.inflate(inflater, container, false)

        TextUtils.applyUnderlineAndColor(requireActivity(), binding!!.tvTerms)
        setupPasswordToggle()
        observeValidationErrors()
        setupLoginButton()


        return binding!!.root
    }

    @SuppressLint("ClickableViewAccessibility")
    private fun setupPasswordToggle() {
        binding!!.etPassword.setOnTouchListener { _, event ->
            if (event.action == android.view.MotionEvent.ACTION_UP) {
                val drawableEnd = 2
                val drawable = binding!!.etPassword.compoundDrawables[drawableEnd] // Get the drawableEnd icon

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
    }

    private fun setupLoginButton() {
        binding!!.btnLogin.setOnClickListener {
            val email = binding!!.etEmail.text.toString().trim()
            val password = binding!!.etPassword.text.toString().trim()

            if (!authViewModel.validateLogin(email, password)) {
                return@setOnClickListener // Stop execution if validation fails
            }
            Toast.makeText(requireContext(), "Login Successful", Toast.LENGTH_SHORT).show()

        }
    }

    private fun observeValidationErrors() {

        authViewModel.emailError.observe(viewLifecycleOwner) { errorMessage ->
            errorMessage?.let {
                showSnackbar(it)
                return@observe // Stop further execution
            }
        }
        authViewModel.passwordError.observe(viewLifecycleOwner) { errorMessage ->
            errorMessage?.let {
                showSnackbar(it)
            }
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
