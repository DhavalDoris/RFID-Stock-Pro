package com.example.rfidstockpro.ui.fragments

import android.annotation.SuppressLint
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.text.InputType
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import com.example.rfidstockpro.R
import com.example.rfidstockpro.RFIDApplication.Companion.ENCRYPTION_KEY
import com.example.rfidstockpro.RFIDApplication.Companion.USER_TABLE
import com.example.rfidstockpro.Utils.TextUtils
import com.example.rfidstockpro.aws.models.UserModel
import com.example.rfidstockpro.databinding.FragmentSignupBinding
import com.example.rfidstockpro.encryption.AESUtils
import com.example.rfidstockpro.ui.activities.VerificationActivity
import com.example.rfidstockpro.viewmodel.AuthViewModel
import com.google.android.material.snackbar.Snackbar
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.util.Date
import java.util.UUID

class SignupFragment : Fragment() {

    private var binding: FragmentSignupBinding? = null
    private var isPasswordVisible = false
    private val authViewModel: AuthViewModel by viewModels()
    private var email: String = ""

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentSignupBinding.inflate(inflater, container, false)

        TextUtils.applyUnderlineAndColor(
            requireActivity(),
            binding!!.tvTerms,
            onTermsClick = {
                // Open Terms of Service link
                val url = "https://www.yourwebsite.com/terms"
                val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
                startActivity(intent)
            },
            onPrivacyClick = {
                // Open Privacy Policy link
                val url = "https://www.yourwebsite.com/privacy"
                val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
                startActivity(intent)
            }
        )

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

    @RequiresApi(Build.VERSION_CODES.O)
    private fun setupSignupButton() {
        binding!!.btnSignup.setOnClickListener {

            val userRole =
                1  // Default Role: Owner      // 0 = Admin, 1 = Owner, 2 = Manager, 3 = Staff

            val username = binding!!.etUsername.text.toString().trim()
            val companyName = binding!!.etCompanyName.text.toString().trim()
            email = binding!!.etEmail.text.toString().trim()
            val contactNumber = binding!!.etContactNumber.text.toString().trim()
            val password = binding!!.etPassword.text.toString().trim()
            val confirmPassword = binding!!.etConfirmPassword.text.toString().trim()

            // Validate user role range
            if (userRole !in 0..3) {
                Snackbar.make(
                    binding!!.root,
                    "Invalid role selected. Please choose a valid role.",
                    Snackbar.LENGTH_LONG
                ).show()
                return@setOnClickListener
            }

            if (!authViewModel.validateSignup(
                    username,
                    companyName,
                    email,
                    contactNumber,
                    password,
                    confirmPassword
                )
            ) {
                return@setOnClickListener
            }


            val encryptedPassword = AESUtils.encrypt(password, ENCRYPTION_KEY)  // Encrypt password

            val user = UserModel(
                id = UUID.randomUUID().toString(),
                userName = username,
                companyName = companyName,
                email = email,
                mobile = contactNumber.toLong(),
                password = encryptedPassword, // Store the encrypted password
                otp = generateOTP(),
                createdDate = Date().toString(),
                updatedDate = Date().toString(),
                role = userRole,  // Default Role: Owner      // 0 = Admin, 1 = Owner, 2 = Manager, 3 = Staff
                // todo temp set to Active
                status = "active",
                permissions = emptyList()
            )
            CoroutineScope(Dispatchers.IO).launch {
                authViewModel.createUser(user, USER_TABLE)
            }

            /*  if (authViewModel.validateSignup(
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
              }*/
        }
    }

    private fun observeValidationErrors() {
        authViewModel.signupError.observe(viewLifecycleOwner) { errorMessage ->
            showSnackbar(errorMessage)
        }

        authViewModel.operationResult.observe(viewLifecycleOwner) { successMessage ->
            Toast.makeText(requireContext(), successMessage, Toast.LENGTH_SHORT).show()
            if (successMessage == getString(R.string.user_created_successfully)) {
                authViewModel.setUserEmail(email)  // Store email in ViewModel
                val intent = Intent(requireContext(), VerificationActivity::class.java)
                intent.putExtra("email", email) // Pass the email
                startActivity(intent)
                requireActivity().finish()
            } else if (successMessage == getString(R.string.email_already_exists)) {
                binding!!.etEmail.error =
                    getString(R.string.this_email_is_already_registered) // ✅ Show error on EditText
            }

        }
    }


    private fun generateOTP(): Int {
        return (1000..9999).random()
    }

    private fun showSnackbar(message: String) {
        Snackbar.make(requireView(), message, Snackbar.LENGTH_SHORT).show()
    }


    override fun onDestroyView() {
        super.onDestroyView()
        binding = null
    }
}
