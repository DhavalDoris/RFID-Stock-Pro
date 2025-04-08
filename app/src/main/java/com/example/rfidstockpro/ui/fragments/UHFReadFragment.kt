package com.example.rfidstockpro.ui.fragments

import android.content.Intent
import android.os.Bundle
import android.os.SystemClock
import android.util.Log
import android.view.KeyEvent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.widget.AppCompatTextView
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import com.example.rfidstockpro.R
import com.example.rfidstockpro.RFIDApplication.Companion.PRODUCT_TABLE
import com.example.rfidstockpro.Utils.ViewUtils
import com.example.rfidstockpro.adapter.UHFTagAdapter
import com.example.rfidstockpro.aws.AwsManager
import com.example.rfidstockpro.databinding.FragmentUhfreadTagBinding
import com.example.rfidstockpro.factores.UHFViewModelFactory
import com.example.rfidstockpro.repository.UHFRepository
import com.example.rfidstockpro.ui.activities.AddItemActivity
import com.example.rfidstockpro.ui.activities.DashboardActivity
import com.example.rfidstockpro.ui.activities.DashboardActivity.Companion.isKeyDownUP
import com.example.rfidstockpro.ui.activities.DashboardActivity.Companion.uhfDevice
import com.example.rfidstockpro.ui.activities.DeviceListActivity.TAG
import com.example.rfidstockpro.viewmodel.SharedProductViewModel
import com.example.rfidstockpro.viewmodel.UHFReadViewModel
import com.google.android.material.snackbar.Snackbar
import com.rscja.deviceapi.RFIDWithUHFBLE
import com.rscja.deviceapi.interfaces.ConnectionStatus
import com.rscja.deviceapi.interfaces.KeyEventCallback
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID

class UHFReadFragment : Fragment() {
    lateinit var viewModel: UHFReadViewModel
    private lateinit var binding: FragmentUhfreadTagBinding
    private lateinit var adapter: UHFTagAdapter
    private var isExit = false
    private var isProductSuccessfullyAdded = false

    private lateinit var sharedProductViewModel: SharedProductViewModel


    // Interface for UHF device provider
    interface UHFDeviceProvider {
        fun provideUHFDevice(): RFIDWithUHFBLE
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        sharedProductViewModel = ViewModelProvider(requireActivity()).get(SharedProductViewModel::class.java)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentUhfreadTagBinding.inflate(inflater, container, false)

        sharedProductViewModel.product.observe(viewLifecycleOwner) { product ->
            Log.d("UHFReadFragment", "Received Product: ${product}")
        }

        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Get UHF device from the hosting activity
        val uhfDeviceProvider = requireActivity() as? UHFDeviceProvider
            ?: throw IllegalStateException("Activity must implement UHFDeviceProvider")
        val uhfRepository = UHFRepository(uhfDeviceProvider.provideUHFDevice())
        viewModel = ViewModelProvider(
            this,
            UHFViewModelFactory(uhfRepository)
        )[UHFReadViewModel::class.java]

        (activity as? DashboardActivity)?.updateToolbarTitle(getString(R.string.rfid_tags))
        setupUI()
        observeViewModel()
    }


    private fun setupUI() {
        isExit = false
        // Initialize adapter with callback
        adapter = UHFTagAdapter(requireContext()) { selectedTag ->
            Log.d("UHFReadFragment", "Selected Tag: ${selectedTag.generateTagString()}")
//            Toast.makeText(requireContext(), "Selected: ${selectedTag.generateTagString()}", Toast.LENGTH_SHORT).show()
            binding.centerLine.visibility = View.VISIBLE
            binding.rlTotalFound.visibility = View.VISIBLE
            binding.btAdd.visibility = View.VISIBLE
            binding.llSpace.visibility = View.VISIBLE

            binding.buttons.visibility = View.GONE

            // ✅ Update tagId via ViewModel function
            sharedProductViewModel.updateTagId(selectedTag.generateTagString())


            sharedProductViewModel.product.observe(viewLifecycleOwner) { product ->
                Log.d("UHFReadFragment", "✅ Received Product: $product")
            }
        }
        binding.LvTags.adapter = adapter

        binding.apply {
            btnSingleInventory.setOnClickListener { viewModel.singleInventory() }
            btStartScan.setOnClickListener { startInventory() }
            btStop.setOnClickListener { viewModel.stopInventory() }
            btClear.setOnClickListener { viewModel.clearData() }
            btCancel.setOnClickListener { requireActivity().supportFragmentManager.popBackStack() }


            LvTags.onItemClickListener = AdapterView.OnItemClickListener { _, _, position, _ ->
                // Handle tag selection
                Log.e(TAG, "item clicked $position "   )
                Toast.makeText(requireActivity(), "item clicked $position ", Toast.LENGTH_SHORT).show()
            }
        }

        binding.btAdd.setOnClickListener {
            AddProductToAWS()
        }


        uhfDevice.setKeyEventCallback(object : KeyEventCallback {
            override fun onKeyDown(keycode: Int) {
                Log.d(TAG, "keycode = $keycode , isExit = $isExit")
                if (!isExit) {
                    viewModel.handleKeyDown(keycode)
                }
            }

            override fun onKeyUp(keycode: Int) {
                Log.d(TAG, "keycode = $keycode , isExit = $isExit")
                viewModel.handleKeyUp(keycode)
            }
        })
    }

    private fun AddProductToAWS() {
      /*  val product = sharedProductViewModel.product.value

        if (product != null) {
            val scope = CoroutineScope(Dispatchers.Main)

            scope.launch {
                // 🔍 Check if tagId already exists BEFORE uploading images/videos
                val (exists, message) = withContext(Dispatchers.IO) {
                    AwsManager.checkIfTagIdExists(PRODUCT_TABLE, product.tagId)
                }

                if (exists) {
                    Log.e("AWS_SAVE", message)
                    Snackbar.make(binding.root, message, Snackbar.LENGTH_LONG).show()
                    return@launch // 🔁 Exit early, don't continue
                }

                // ✅ Proceed to upload only if tagId is unique
                val imageFiles = product.selectedImages.map { File(it) }
                val videoFile = product.selectedVideo?.let { File(it) }

                AwsManager.uploadMediaToS3(
                    scope = scope,
                    context = requireContext(),
                    imageFiles = imageFiles,
                    videoFile = videoFile,
                    onSuccess = { imageUrls, videoUrl ->
                        val updatedProduct = product.copy(
                            selectedImages = imageUrls,
                            selectedVideo = videoUrl
                        )

                        scope.launch {
                            val (isSuccess, saveMessage) = withContext(Dispatchers.IO) {
                                AwsManager.saveProduct(PRODUCT_TABLE, updatedProduct)
                            }
                            if (isSuccess) {
                                Log.d("AWS_SAVE", saveMessage)
                                binding.rlSuccessFullAdded.visibility = View.VISIBLE
                                isProductSuccessfullyAdded = true
                                if (isAdded && activity != null) {
                                    (activity as? AddItemActivity)?.updateToolbarTitleAddItem("")
                                }
                            } else {
                                Log.e("AWS_SAVE", saveMessage)
                                Toast.makeText(requireContext(), saveMessage, Toast.LENGTH_SHORT).show()
                            }
                        }
                    },
                    onError = { errorMessage ->
                        Log.e("AWS_UPLOAD", "❌ Upload failed: $errorMessage")
                        Toast.makeText(requireContext(), errorMessage, Toast.LENGTH_SHORT).show()
                    }
                )
            }
        } else {
            Toast.makeText(requireContext(), "Product info not found!", Toast.LENGTH_SHORT).show()
        }*/

        val product = sharedProductViewModel.product.value
        if (product != null) {
            viewModel.addProductToAWS(
                context = requireContext(),
                product = product,
                onSuccess = {
                    binding.rlSuccessFullAdded.visibility = View.VISIBLE
                    isProductSuccessfullyAdded = true
                    (activity as? AddItemActivity)?.updateToolbarTitleAddItem("")
                },
                onError = { errorMessage ->
                    Snackbar.make(binding.root, errorMessage, Snackbar.LENGTH_LONG).show()
                }
            )
        } else {
            Toast.makeText(requireContext(), "Product info not found!", Toast.LENGTH_SHORT).show()
        }
    }


    private fun startInventory() {
        val time = binding.etTime.text.toString()
        val maxRunTime = if (time.isNotEmpty()) {
            (time.toFloat() * 1000).toInt()
        } else {
            binding.etTime.hint.toString().toInt() * 1000
        }

        viewModel.startInventory(
            maxRunTime = maxRunTime,
            needPhase = binding.cbPhase.isChecked
        )
    }

    private fun observeViewModel() {
        viewModel.apply {
            tagList.observe(viewLifecycleOwner) { tags ->
                adapter.updateTags(tags)
                binding.tvCount.text = tags.size.toString()
            }

            totalTagCount.observe(viewLifecycleOwner) { total ->
                binding.tvTotal.text = total.toString()
            }

            isScanning.observe(viewLifecycleOwner) { isScanning ->
                updateUIForScanningState(isScanning)
            }

            useTime.observe(viewLifecycleOwner) { time ->
                binding.tvTime.text = "$time s"
            }

            connectionStatus.observe(viewLifecycleOwner) { status ->
                updateUIForConnectionStatus(status)
                Log.e("KEY_TAG", "observeViewModel: " + status )
            }
        }
    }

    private fun updateUIForScanningState(isScanning: Boolean) {
        binding.apply {
            btStop.isEnabled = isScanning
            btStop.alpha = if (isScanning) 1f else 0.3f

            btStartScan.isEnabled = !isScanning
            btStartScan.alpha = if (!isScanning) 1f else 0.3f

            btnSingleInventory.isEnabled = !isScanning
            btnSingleInventory.alpha = if (!isScanning) 1f else 0.3f

        }
    }

    private fun updateUIForConnectionStatus(status: ConnectionStatus) {
        binding.apply {
            when (status) {
                ConnectionStatus.CONNECTED -> {
                    btStartScan.isEnabled = true
                    ViewUtils.setViewAlpha(btStartScan, 1f);
                    btnSingleInventory.isEnabled = true
                    cbFilter.isEnabled = true
                }

                ConnectionStatus.DISCONNECTED -> {
                    btStartScan.isEnabled = false
                    ViewUtils.setViewAlpha(btStartScan, 0.3f);
                    btnSingleInventory.isEnabled = false
                    cbFilter.isChecked = false
                    cbFilter.isEnabled = false
                }

                else -> {}
            }
        }
    }

    override fun onResume() {
        super.onResume()
        requireActivity().let { activity ->
            activity.window.decorView.setOnKeyListener { _, keyCode, event ->
                when (event.action) {
                    KeyEvent.ACTION_DOWN -> {
                        viewModel.handleKeyDown(keyCode)
                        Log.e("KEY_TAG", "onResume: ACTION_DOWN"   )
                        true
                    }
                    KeyEvent.ACTION_UP -> {
                        viewModel.handleKeyUp(keyCode)
                        Log.e("KEY_TAG", "onResume: ACTION_UP"   )
                        true
                    }
                    else -> false
                }
            }
        }
        val callback = object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                if (binding.rlSuccessFullAdded.visibility == View.VISIBLE) {
                    // Finish the current activity and go to Dashboard
                    startActivity(Intent(requireContext(), DashboardActivity::class.java))
                    requireActivity().finish()
                } else {
                    // Pop the fragment back stack
                    requireActivity().supportFragmentManager.popBackStack()
                }
            }
        }

        requireActivity().onBackPressedDispatcher.addCallback(viewLifecycleOwner, callback)
    }



    companion object {
        fun newInstance(): UHFReadFragment {
            return UHFReadFragment()
        }
    }

    override fun onDestroyView() {
        Log.i(TAG, "UHFReadTagFragment.onDestroyView")
        super.onDestroyView()
        isExit = true

    }



}