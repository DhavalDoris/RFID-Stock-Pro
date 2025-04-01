package com.example.rfidstockpro.ui.fragments

import android.annotation.SuppressLint
import android.app.AlertDialog
import android.app.ProgressDialog
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.text.InputType
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import com.example.rfidstockpro.R
import com.example.rfidstockpro.RFIDApplication.Companion.USER_TABLE
import com.example.rfidstockpro.Utils.TextUtils
import com.example.rfidstockpro.Utils.observeOnce
import com.example.rfidstockpro.aws.AwsManager
import com.example.rfidstockpro.aws.models.UserModel
import com.example.rfidstockpro.databinding.FragmentLoginBinding
import com.example.rfidstockpro.ui.activities.DashboardActivity
import com.example.rfidstockpro.viewmodel.AuthViewModel
import com.google.android.material.snackbar.Snackbar
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import software.amazon.awssdk.core.sync.RequestBody
import software.amazon.awssdk.services.s3.model.PutObjectRequest
import java.io.File
import java.util.UUID


class LoginFragment : Fragment() {

    private var binding: FragmentLoginBinding? = null
    private val authViewModel: AuthViewModel by viewModels()
    private var isPasswordVisible = false
    private var userDialog: AlertDialog? = null // Declare globally
    private var selectedFile: File? = null

    private val pickImageLauncher =
        registerForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
            uri?.let { selectedImageUri ->
                selectedFile = uriToFile(selectedImageUri)  // Convert URI to File
            }
        }


    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding = FragmentLoginBinding.bind(view)

        authViewModel.loginResult.observe(viewLifecycleOwner) { result ->
            Toast.makeText(requireContext(), result, Toast.LENGTH_SHORT).show()
            if (result == getString(R.string.login_successful)) {
                navigateToDashboard()
            } else if (result == getString(R.string.account_is_not_active)) {
                binding!!.etEmail.error =
                    getString(R.string.your_account_is_not_active_contact_support)
            }
        }
    }

    private fun navigateToDashboard() {
        startActivity(Intent(requireActivity(), DashboardActivity::class.java))
//        startActivity(Intent(requireActivity(), DeviceListActivity::class.java))
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentLoginBinding.inflate(inflater, container, false)

        TextUtils.applyUnderlineAndColor(requireActivity(), binding!!.tvTerms)
        setupPasswordToggle()
        setupLoginButton()
        observeValidationErrors()
        observeAuthResult()  // ✅ Move observer out of click listener
        setupReadUserButton()
        binding!!.btnSelectMedia.setOnClickListener {
//            pickMedia()
//            lifecycleScope.launch {
//                val fileUrls = AwsManager.getAllFileUrls()
//                Log.d("S3Helper", "Fetching files from S3 bucket: $BUCKET_NAME")
//                Log.d("AWS_TAG", "Files: $fileUrls")
//            }


            // Upload file
            selectedFile?.let { file ->
                uploadToS3(file)
            } ?: Toast.makeText(requireContext(), "Select an image first!", Toast.LENGTH_SHORT)
                .show()

            // Download file
            /* AwsManager.getAllImageUrls { imageUrls ->
                 if (imageUrls != null) {
                     for (url in imageUrls) {
                         Log.d("AWS_TAG", "Image URL: $url")
                     }
                 } else {
                     Log.e("AWS_TAG", "Failed to fetch images from S3")
                 }
             }*/
        }

        return binding!!.root
    }

    private fun pickMedia() {
        pickImageLauncher.launch("image/*")
    }

    private fun uploadToS3(file: File) {
        val progressDialog = ProgressDialog(requireActivity()).apply {
            setMessage(getString(R.string.uploading_please_wait))
            setCancelable(false)
            show()
        }

        CoroutineScope(Dispatchers.IO).launch {
            try {
                val key = "rfid-uploads/${UUID.randomUUID()}_${file.name}"

                val putRequest = PutObjectRequest.builder()
                    .bucket(AwsManager.BUCKET_NAME)
                    .key(key)
                    .build()

                AwsManager.s3Client.putObject(putRequest, RequestBody.fromFile(file))

                withContext(Dispatchers.Main) {
                    progressDialog.dismiss()
                    val imageUrl = "https://${AwsManager.BUCKET_NAME}.s3.amazonaws.com/$key"
                    Log.e("AWS_TAG", "Upload Success: $imageUrl")
                    Toast.makeText(requireContext(),
                        getString(R.string.upload_successful), Toast.LENGTH_SHORT)
                        .show()
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    progressDialog.dismiss()
                    Log.e("AWS_TAG", "Upload Failed: ${e.message}")
                    Toast.makeText(
                        requireContext(),
                        getString(R.string.upload_failed, e.message),
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
        }
    }

    private fun uriToFile(uri: Uri): File {
        val inputStream = requireActivity().contentResolver.openInputStream(uri)
            ?: throw IllegalArgumentException("Cannot open URI")
        val tempFile = File.createTempFile("selected_image", ".jpg", requireActivity().cacheDir)
        tempFile.outputStream().use { output ->
            inputStream.copyTo(output)
        }
        return tempFile
    }

    private fun setupReadUserButton() {
        /*binding!!.btnReadUser.setOnClickListener {
            Log.e("AWS_TAG", "setupReadUserButton: " )
            val email = binding!!.etEmail.text.toString().trim()
            if (email.isNotEmpty()) {
                showUserDataDialog(email)
            } else {
                Toast.makeText(requireContext(), "Enter email", Toast.LENGTH_SHORT).show()
            }
        }*/

        binding!!.btnReadUser.setOnClickListener {
            /*lifecycleScope.launch {
                fetchAllUsers()
            }*/
            pickMedia()
        }
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
    }

    private fun setupLoginButton() {
        binding!!.btnLogin.setOnClickListener {
            val email = binding!!.etEmail.text.toString().trim()
            val password = binding!!.etPassword.text.toString().trim()

            if (authViewModel.validateLogin(email, password)) {
                authViewModel.loginUser(email, password, requireActivity())
            }
        }
    }


    private fun observeAuthResult() {
        authViewModel.operationResult.observe(viewLifecycleOwner) { result ->
            Log.e("AWS_TAG", "Auth Result: $result")
            Toast.makeText(requireContext(), "$result", Toast.LENGTH_SHORT).show()
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

    private fun showUserDataDialog(email: String) {
        userDialog?.dismiss()
        authViewModel.userData.removeObservers(viewLifecycleOwner) // Remove previous observers
        authViewModel.readUserData(email)  // Fetch user data
        Log.e("AWS_TAG", "showUserDataDialog: 1")
        authViewModel.userData.observeOnce(viewLifecycleOwner) { user ->
            Log.e("AWS_TAG", "showUserDataDialog: 2")
            if (user != null) {
                val inputPassword = EditText(requireContext()).apply {
                    setText(user.password)
                }

                val dialogBuilder = AlertDialog.Builder(requireContext())
                    .setTitle("User Details")
                    .setMessage("Email: ${user.email}")
                    .setView(inputPassword)
                    .setPositiveButton("Update") { _, _ ->
                        val newPassword = inputPassword.text.toString().trim()
                        if (newPassword.isNotEmpty()) {
                            authViewModel.updateUserData(USER_TABLE,UserModel(user.email, newPassword))
                        } else {
                            Toast.makeText(
                                requireContext(),
                                "Password cannot be empty",
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                    }
                    .setNegativeButton("Delete") { _, _ ->
                        authViewModel.deleteUserData(user.email)
                    }
                    .setNeutralButton("Cancel", null)
                userDialog = dialogBuilder.create() // Store the dialog
                userDialog?.show()


            } else {
                Toast.makeText(requireContext(), "User not found", Toast.LENGTH_SHORT).show()
            }
        }
    }


}
