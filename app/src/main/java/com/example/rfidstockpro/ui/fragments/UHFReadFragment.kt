package com.example.rfidstockpro.ui.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import com.example.rfidstockpro.Utils.ViewUtils
import com.example.rfidstockpro.adapter.UHFTagAdapter
import com.example.rfidstockpro.databinding.FragmentUhfreadTagBinding
import com.example.rfidstockpro.viewmodel.UHFReadViewModel
import com.rscja.deviceapi.interfaces.ConnectionStatus
import com.example.rfidstockpro.repository.UHFRepository
import com.example.rfidstockpro.factores.UHFViewModelFactory
import com.example.rfidstockpro.ui.activities.DashboardActivity
import com.rscja.deviceapi.RFIDWithUHFBLE

class UHFReadFragment : Fragment() {
    private lateinit var viewModel: UHFReadViewModel
    private lateinit var binding: FragmentUhfreadTagBinding
    private lateinit var adapter: UHFTagAdapter

    // Interface for UHF device provider
    interface UHFDeviceProvider {
        fun provideUHFDevice(): RFIDWithUHFBLE
    }


    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentUhfreadTagBinding.inflate(inflater, container, false)
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

        (activity as? DashboardActivity)?.updateToolbarTitle("RFID Tags")
        setupUI()
        observeViewModel()
    }

    private fun setupUI() {
        adapter = UHFTagAdapter(requireContext())
        binding.LvTags.adapter = adapter

        binding.apply {
            btnSingleInventory.setOnClickListener { viewModel.singleInventory() }
            btStartScan.setOnClickListener { startInventory() }
            btStop.setOnClickListener { viewModel.stopInventory() }
            btClear.setOnClickListener { viewModel.clearData() }
            btCancel.setOnClickListener { requireActivity().supportFragmentManager.popBackStack() }


            LvTags.onItemClickListener = AdapterView.OnItemClickListener { _, _, position, _ ->
                // Handle tag selection
                Toast.makeText(requireActivity(), "item clicked $position ", Toast.LENGTH_SHORT).show()
            }
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

    companion object {
        fun newInstance(): UHFReadFragment {
            return UHFReadFragment()
        }
    }
}