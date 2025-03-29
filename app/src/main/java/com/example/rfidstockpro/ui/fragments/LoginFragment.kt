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
import androidx.lifecycle.lifecycleScope
import com.amazonaws.mobileconnectors.s3.transferutility.TransferListener
import com.amazonaws.mobileconnectors.s3.transferutility.TransferState
import com.amazonaws.mobileconnectors.s3.transferutility.TransferUtility
import com.example.rfidstockpro.R
import com.example.rfidstockpro.Utils.TextUtils
import com.example.rfidstockpro.Utils.observeOnce
import com.example.rfidstockpro.aws.AwsManager
import com.example.rfidstockpro.aws.AwsManager.BUCKET_NAME
import com.example.rfidstockpro.aws.AwsManager.awsS3Client
import com.example.rfidstockpro.aws.models.UserModel
import com.example.rfidstockpro.databinding.FragmentLoginBinding
import com.example.rfidstockpro.ui.activities.DeviceListActivity
import com.example.rfidstockpro.viewmodel.AuthViewModel
import com.google.android.material.snackbar.Snackbar
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.util.UUID


class LoginFragment : Fragment() {

    private lateinit var progressDialog: ProgressDialog
    private var binding: FragmentLoginBinding? = null
    private val authViewModel: AuthViewModel by viewModels()
    private var isPasswordVisible = false
    private var userDialog: AlertDialog? = null // Declare globally
    private var selectedFile: File? = null

    private val pickImageLauncher =
        registerForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
            uri?.let { selectedImageUri ->
//            imageView.setImageURI(selectedImageUri)  // Show image in ImageView
                selectedFile = uriToFile(selectedImageUri)  // Convert URI to File
            }
        }


    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding = FragmentLoginBinding.bind(view)

        authViewModel.loginResult.observe(viewLifecycleOwner) { result ->
            Toast.makeText(requireContext(), result, Toast.LENGTH_SHORT).show()
            if (result == "Login successful") {
                navigateToDashboard()
            }
        }
    }

    private fun navigateToDashboard() {
        startActivity(Intent(requireActivity(), DeviceListActivity::class.java))
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
//            selectedFile?.let { file ->
//                uploadToS3(file)
//            } ?: Toast.makeText(requireContext(), "Select an image first!", Toast.LENGTH_SHORT).show()

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
            setMessage("Uploading... 0%")
            setCancelable(false)
            show()
        }

        val transferUtility = TransferUtility.builder()
            .context(requireContext())
            .s3Client(awsS3Client)
            .build()

        val key = "rfid-uploads/${UUID.randomUUID()}_${file.name}"

        val uploadObserver = transferUtility.upload(AwsManager.BUCKET_NAME, key, file)

        uploadObserver.setTransferListener(object : TransferListener {
            override fun onStateChanged(id: Int, state: TransferState?) {
                if (state == TransferState.COMPLETED) {
                    progressDialog.dismiss()
                    val imageUrl = "https://$BUCKET_NAME.s3.amazonaws.com/$key"
                    Log.e("AWS_TAG", "onStateChanged: ->> " + imageUrl)
                    Toast.makeText(requireContext(), "Upload Complete!", Toast.LENGTH_SHORT).show()
                } else if (state == TransferState.FAILED) {
                    progressDialog.dismiss()
                    Toast.makeText(requireContext(), "Upload Failed!", Toast.LENGTH_SHORT).show()
                }
            }

            override fun onProgressChanged(id: Int, bytesCurrent: Long, bytesTotal: Long) {
                val progress = if (bytesTotal > 0) (bytesCurrent * 100 / bytesTotal).toInt() else 0
                progressDialog.setMessage("Uploading... $progress%")
            }

            override fun onError(id: Int, ex: Exception?) {
                progressDialog.dismiss()
                Toast.makeText(requireContext(), "Upload Error: ${ex?.message}", Toast.LENGTH_SHORT)
                    .show()
            }
        })
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

            /*if (!authViewModel.validateLogin(email, password)) {
                return@setOnClickListener // Stop execution if validation fails
            }*/
            if (authViewModel.validateLogin(email, password)) {
                authViewModel.loginUser(email, password)
            }

            /*  CoroutineScope(Dispatchers.Main).launch {
                  // Check if the email exists in DynamoDB

              }*/

//            val userId = UUID.randomUUID().toString() // Generate unique ID
//            val user = UserModel(email, password) // Email as userId
//
//            CoroutineScope(Dispatchers.Main).launch {
//                AwsManager.ensureTableExists { status ->
//                    when (status) {
//                        "creating" -> Log.e("AWS_TAG", "Creating Table...")
//                        "created", "exists" -> {
//                            Log.e("AWS_TAG", "Table Ready! Adding User...")
//                            authViewModel.createUser(user)
//                        }
//
//                        else -> Log.e("AWS_TAG", "Error: $status")
//                    }
//                }
//            }

//          Toast.makeText(requireContext(), "Login Successful", Toast.LENGTH_SHORT).show()
//          startActivity(Intent(requireActivity(), VerificationActivity::class.java))
//          requireActivity().finish()
//          startActivity(Intent(requireActivity(), DashboardActivity::class.java))
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
                            authViewModel.updateUserData(UserModel(user.email, newPassword))
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
