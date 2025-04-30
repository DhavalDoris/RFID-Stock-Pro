package com.example.rfidstockpro.ui.activities

import UHFConnectionManager
import android.Manifest
import android.app.Activity
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Color
import android.graphics.Typeface
import android.location.LocationManager
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.provider.Settings
import android.text.Spannable
import android.text.SpannableString
import android.text.style.ForegroundColorSpan
import android.text.style.RelativeSizeSpan
import android.util.Log
import android.view.View
import android.widget.AdapterView
import androidx.activity.viewModels
import androidx.annotation.RequiresPermission
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.AppCompatTextView
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.content.res.ResourcesCompat
import com.example.rfidstockpro.R
import com.example.rfidstockpro.Utils.AnimationUtils
import com.example.rfidstockpro.Utils.FragmentManagerHelper
import com.example.rfidstockpro.Utils.StatusBarUtils
import com.example.rfidstockpro.Utils.ToastUtils.showToast
import com.example.rfidstockpro.adapter.CustomSpinnerAdapter
import com.example.rfidstockpro.bulkupload.activity.BulkUploadActivity
import com.example.rfidstockpro.databinding.ActivityDashboardBinding
import com.example.rfidstockpro.inouttracker.activity.CreateCollectionActivity
import com.example.rfidstockpro.inouttracker.activity.InOutTrackerActivity
import com.example.rfidstockpro.sharedpref.SessionManager
import com.example.rfidstockpro.ui.ProductManagement.activity.ProductManagementActivity
import com.example.rfidstockpro.ui.activities.DeviceListActivity.TAG
import com.example.rfidstockpro.ui.fragments.UHFReadFragment
import com.example.rfidstockpro.viewmodel.DashboardViewModel
import com.example.rfidstockpro.viewmodel.DashboardViewModel.Companion.SHOW_HISTORY_CONNECTED_LIST
import com.github.mikephil.charting.charts.PieChart
import com.github.mikephil.charting.data.PieData
import com.github.mikephil.charting.data.PieDataSet
import com.github.mikephil.charting.data.PieEntry
import com.rscja.deviceapi.RFIDWithUHFBLE
import com.rscja.deviceapi.interfaces.ConnectionStatus
import com.rscja.deviceapi.interfaces.ConnectionStatusCallback


class DashboardActivity : AppCompatActivity(), UHFReadFragment.UHFDeviceProvider {

    //    var tvToolbarTitle: TextView? = null // Declare as public
    private var mDevice: BluetoothDevice? = null
    private lateinit var binding: ActivityDashboardBinding
    private lateinit var pieChart: PieChart
    private val dashboardViewModel: DashboardViewModel by viewModels()
    private val REQUEST_ENABLE_BT: Int = 2
    private val REQUEST_SELECT_DEVICE: Int = 1
    private val PERMISSION_REQUEST_CODE = 100
    var mBtAdapter: BluetoothAdapter? = null
    private val REQUEST_ACTION_LOCATION_SETTINGS = 99
    private val timeFilterOptions = listOf("Weekly", "Monthly", "Yearly")

    override fun provideUHFDevice(): RFIDWithUHFBLE {
        return uhfDevice
    }


    companion object {
        lateinit var uhfDevice: RFIDWithUHFBLE
        var isKeyDownUP: Boolean = false
        var isShowDuplicateTagId: Boolean? = true
        var ShowCheckBoxinProduct: Boolean? = false
    }


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityDashboardBinding.inflate(layoutInflater)
        setContentView(binding.root)

        mBtAdapter = BluetoothAdapter.getDefaultAdapter()
        uhfDevice = RFIDWithUHFBLE.getInstance()
        uhfDevice.init(applicationContext)
        StatusBarUtils.setStatusBarColor(this)

        setupPieChart()
        checkPermissions()
        observeViewModel()
        setupUI()
        setupSpinner()
        initClick()
        loadUserData()
        uhfTrigger()
        val toolbarView = findViewById<View>(R.id.commonToolbar)
//        tvToolbarTitle = toolbarView.findViewById(R.id.tvToolbarTitle)

        if (!isLocationEnabled()) {
            AlertDialog.Builder(this)
                .setTitle(getString(R.string.permission))
                .setMessage(getString(R.string.open_location_msg))
                .setPositiveButton(getString(R.string.open_location)) { _, _ ->
                    val intent = Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS)
                    startActivityForResult(intent, REQUEST_ACTION_LOCATION_SETTINGS)
                }
                .setNegativeButton(getString(R.string.permission_cancel)) { _, _ -> finish() }
                .setCancelable(false)
                .show()
        }
    }

    private fun uhfTrigger() {


    }

    private fun loadUserData() {
        val sessionManager = SessionManager.getInstance(this) // Get Singleton Instance
        val userName = sessionManager.getUserName()
        val userRole = sessionManager.getUserRole()

        binding.tvUserName.text = userName
        binding.tvUserRole.text = userRole.toString()
        when (userRole) {
            1 -> { // Owner
                binding.tvUserRole.text = getString(R.string.owner)
//                binding.tvUserRole.setBackgroundTintList(ContextCompat.getColorStateList(this, R.color.owner_background))
//                binding.tvUserRole.setTextColor(ContextCompat.getColor(this, R.color.owner_text))
            }

            2 -> { // Manager
                binding.tvUserRole.text = getString(R.string.manager)
//                binding.tvUserRole.setBackgroundTintList(ContextCompat.getColorStateList(this, R.color.manager_background))
//                binding.tvUserRole.setTextColor(ContextCompat.getColor(this, R.color.manager_text))
            }

            3 -> { // Staff
                binding.tvUserRole.text = getString(R.string.staff)
//                binding.tvUserRole.setBackgroundTintList(ContextCompat.getColorStateList(this, R.color.staff_bg_color))
//                binding.tvUserRole.setTextColor(ContextCompat.getColor(this, R.color.staff_text_color))
            }
        }
    }

    fun updateToolbarTitle(title: String) {
        val toolbarTitle = findViewById<AppCompatTextView>(R.id.tvToolbarTitle)
        Log.e(TAG, "updateToolbarTitle: ")
        toolbarTitle!!.text = title
    }

    private fun initClick() {
        binding.rlBuy.setOnClickListener {
            val intent = Intent(this, AddProductActivity::class.java)
            intent.putExtra("source", "Dashboard")
            startActivity(intent)
        }

        binding.rlSell.setOnClickListener {
//            setFragment(UHFReadFragment())
            FragmentManagerHelper.setFragment(this, UHFReadFragment(), R.id.realtabcontent)
        }

        binding.productManagement.setOnClickListener {
//            startActivity(Intent(this, ProductManagementActivity::class.java))
            val intent = Intent(this, ProductManagementActivity::class.java)
            intent.putExtra("comesFrom", "Dashboard")
            startActivity(intent)

        }

        binding.btnInOut.setOnClickListener {
            startActivity(Intent(this, InOutTrackerActivity::class.java))
        }

        binding.btnImport.setOnClickListener {
            startActivity(Intent(this, BulkUploadActivity::class.java))
        }
    }

    private fun setupUI() {
        dashboardViewModel.checkBluetoothConnection()
        UHFConnectionManager.addStatusChangeListener(connectionListener)

        binding.btnConnectScanner.setOnClickListener {

            if (isLocationEnabled()) {
//                checkPermission()
                if (dashboardViewModel.isConnected.value == true) {
                    dashboardViewModel.disconnect(true)
                } else {
                    showBluetoothDevice()
                }
            } else {
                AlertDialog.Builder(this)
                    .setTitle(getString(R.string.permission))
                    .setMessage(getString(R.string.open_location_msg))
                    .setPositiveButton(getString(R.string.open_location)) { _, _ ->
                        val intent = Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS)
                        startActivityForResult(intent, REQUEST_ACTION_LOCATION_SETTINGS)
                    }
                    .setNegativeButton(getString(R.string.permission_cancel)) { _, _ -> finish() }
                    .setCancelable(false)
                    .show()
            }


        }

        binding.btnDisconnect.setOnClickListener {
            dashboardViewModel.disconnect(true)
        }
        binding.btnInOut.setOnClickListener {

        }
    }
    private fun isLocationEnabled(): Boolean {
        val locationManager = getSystemService(Context.LOCATION_SERVICE) as LocationManager
        return locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER) ||
                locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER)
    }

    private fun checkPermission() {
        if (!isLocationEnabled()) {
            val intent = Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS)
            startActivityForResult(intent, REQUEST_ACTION_LOCATION_SETTINGS)
            return
        }

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
            != PackageManager.PERMISSION_GRANTED
        ) {
//            checkLocationPermission()
        } else if ((Build.VERSION.SDK_INT >= Build.VERSION_CODES.R && !Environment.isExternalStorageManager()) ||
            (Build.VERSION.SDK_INT < Build.VERSION_CODES.R &&
                    ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE)
                    != PackageManager.PERMISSION_GRANTED)
        ) {
//            checkReadWritePermission()
        } else if (ContextCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH)
            != PackageManager.PERMISSION_GRANTED
        ) {
//            checkBluetoothPermission()
        }
    }


    @RequiresPermission(Manifest.permission.BLUETOOTH_CONNECT)
    private fun showBluetoothDevice() {
        if (mBtAdapter == null) {
            return
        }
        if (!mBtAdapter!!.isEnabled) {
            val enableIntent = Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE)
            startActivityForResult(enableIntent, REQUEST_ENABLE_BT)
        } else {
            val newIntent = Intent(this@DashboardActivity, DeviceListActivity::class.java)
            newIntent.putExtra(SHOW_HISTORY_CONNECTED_LIST, false)
            startActivityForResult(newIntent, REQUEST_SELECT_DEVICE)
            dashboardViewModel.cancelDisconnectTimer()
        }
    }

    override fun onResume() {
        super.onResume()
        // Force recheck of the connection status
//        UHFConnectionManager.recheckConnectionStatus()

        Log.e("ONRESUME_TAG", "onResume: " + UHFConnectionManager.getConnectionStatus() )

        // Immediately update the UI based on current status
        updateConnectionUI(
            UHFConnectionManager.getConnectionStatus(),
            UHFConnectionManager.getConnectedDevice()
        )
    }

    private val connectionListener = object : UHFConnectionManager.ConnectionStatusListener {
        override fun onConnectionStatusChanged(status: ConnectionStatus, device: Any?) {
            updateConnectionUI(status, device)
        }
    }

    private fun updateConnectionUI(status: ConnectionStatus, device: Any?) {
        if (isFinishing || isDestroyed) return

        if (status == ConnectionStatus.CONNECTED) {
            binding.tvStaus.text = getString(R.string.connected)
            AnimationUtils.fadeInView(binding.rlRfidStatus)
            AnimationUtils.fadeOutView(binding.footerView)
        } else {
            binding.tvStaus.text = getString(R.string.disconnected)
            AnimationUtils.fadeOutView(binding.rlRfidStatus)
            AnimationUtils.fadeInView(binding.footerView)
        }
    }

    private fun checkPermissions() {
        val permissions = mutableListOf<String>()

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            if (ContextCompat.checkSelfPermission(
                    this,
                    Manifest.permission.BLUETOOTH_CONNECT
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                permissions.add(Manifest.permission.BLUETOOTH_CONNECT)
            }
            if (ContextCompat.checkSelfPermission(
                    this,
                    Manifest.permission.BLUETOOTH_SCAN
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                permissions.add(Manifest.permission.BLUETOOTH_SCAN)
            }
        } else {
            if (ContextCompat.checkSelfPermission(
                    this,
                    Manifest.permission.BLUETOOTH
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                permissions.add(Manifest.permission.BLUETOOTH)
            }
        }

        if (ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            permissions.add(Manifest.permission.ACCESS_FINE_LOCATION)
        }

        if (permissions.isNotEmpty()) {
            ActivityCompat.requestPermissions(
                this,
                permissions.toTypedArray(),
                PERMISSION_REQUEST_CODE
            )
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == PERMISSION_REQUEST_CODE) {
            for (i in permissions.indices) {
                Log.e(TAG, "onRequestPermissionsResult: out if ")
                if (grantResults[i] != PackageManager.PERMISSION_GRANTED) {
                    Log.e(TAG, "onRequestPermissionsResult: if ")
                    showPermissionDialog()
                    return
                }
            }
        }
    }

    private fun showPermissionDialog() {
        Log.e(TAG, "showPermissionDialog:  ")
        AlertDialog.Builder(this)
            .setTitle("Permission Required")
            .setMessage("This app requires Bluetooth and Location permissions to function properly.")
            .setPositiveButton("Grant") { _, _ ->
                checkPermissions()
            }
            .setNegativeButton("Exit") { _, _ ->
                finish()
            }
            .setCancelable(false)
            .show()
    }


    private fun setupPieChart() {

        // Load custom font
        val typeface: Typeface? =
            ResourcesCompat.getFont(this@DashboardActivity, R.font.rethinksans_bold)

        // Create formatted center text
        val centerText = SpannableString("875\nTotal Stocks").apply {
            setSpan(
                RelativeSizeSpan(1.3f),
                0,
                3,
                Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
            ) // Large size for "875"
            setSpan(
                ForegroundColorSpan(Color.BLACK),
                0,
                3,
                Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
            ) // Black color
            setSpan(
                RelativeSizeSpan(0.5f),
                4,
                length,
                Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
            ) // Small size for "Total Stocks"
            setSpan(
                ForegroundColorSpan(Color.GRAY),
                4,
                length,
                Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
            ) // Gray color
        }
        pieChart = binding.pieChart

        // Apply to PieChart
        binding.pieChart.apply {
            setUsePercentValues(true)
            description.isEnabled = false
            holeRadius = 40f
            setHoleRadius(62f) // Increase for thicker ring
            setTransparentCircleRadius(70f) // Optional, for smooth edge
            setDrawHoleEnabled(true)
            setDrawCenterText(true) // Enable center text
            transparentCircleRadius = 45f
            setEntryLabelColor(Color.BLACK)
            setEntryLabelTextSize(12f)
            legend.isEnabled = false // Hide legend
            setDrawEntryLabels(false) // Hide labels inside the chart

            // Add Centered Text
            setCenterText(centerText) // Apply formatted text
            setCenterTextTypeface(typeface) // Apply custom font
            setCenterTextSize(24f) // Base size for scaling
            setCenterTextColor(Color.BLACK) // Ensures default color for unstyled text
            setTouchEnabled(false)
            animateXY(1000, 1000) // X and Y axis animation in milliseconds | 1-second animation
//            animateX(1000)
//            animateY(1000)
        }

    }

    private fun observeViewModel() {
        val entries = ArrayList<PieEntry>()
        entries.add(PieEntry(15f))  // Active
        entries.add(PieEntry(10f))  // Pending
        entries.add(PieEntry(30f))  // Inactive
        entries.add(PieEntry(30f))  // Return
        entries.add(PieEntry(30f))  // Sold

        val dataSet = PieDataSet(entries, "").apply {
            colors = listOf(
                ContextCompat.getColor(
                    this@DashboardActivity,
                    R.color.colorActive
                ),  // Active (Blue)
                ContextCompat.getColor(
                    this@DashboardActivity,
                    R.color.colorPending
                ), // Pending (Orange)
                ContextCompat.getColor(
                    this@DashboardActivity,
                    R.color.colorInactive
                ), // Inactive (Red)
                ContextCompat.getColor(
                    this@DashboardActivity,
                    R.color.colorReturn
                ), // Return (Blue)
                ContextCompat.getColor(this@DashboardActivity, R.color.colorSold) // Sold (Gray)
            )
            setDrawValues(false) // **Disable text values (percentages)**
            valueTextSize = 14f
            valueTextColor = Color.TRANSPARENT // Hide values inside the chart
            sliceSpace = 2f // **Add white space between slices**
        }

        val data = PieData(dataSet).apply {
            setDrawValues(false) // **Ensure no values are shown**
        }

        pieChart.data = data
        pieChart.invalidate() // Refresh chart

        dashboardViewModel.connectButtonText.observe(this) { text ->
            binding.btnConnectScanner.text = text
        }
        dashboardViewModel.rfidStatusText.observe(this) { text ->
//            binding.tvStaus.text = text
        }
        dashboardViewModel.footerVisibility.observe(this) { isVisible ->
//            binding.footerView.visibility = if (isVisible) View.VISIBLE else View.GONE
//            binding.rlRfidStatus.visibility = if (!isVisible) View.GONE else View.VISIBLE
        }
    }

    private fun setupSpinner() {
        val adapter = CustomSpinnerAdapter(this, timeFilterOptions)
        binding.spinnerTimeFilter.adapter = adapter

        // Set default selection to "Monthly"
        binding.spinnerTimeFilter.setSelection(1)

        // Handle item selection
        binding.spinnerTimeFilter.onItemSelectedListener =
            object : AdapterView.OnItemSelectedListener {
                override fun onItemSelected(
                    parent: AdapterView<*>?,
                    view: View?,
                    position: Int,
                    id: Long
                ) {
                    val selectedOption = timeFilterOptions[position]
                    // Handle selection if required
                }

                override fun onNothingSelected(parent: AdapterView<*>?) {}
            }
    }


    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        when (requestCode) {
            REQUEST_SELECT_DEVICE -> {
                if (resultCode == Activity.RESULT_OK && data != null) {
                    val deviceAddress = data.getStringExtra(BluetoothDevice.EXTRA_DEVICE)
                    if (!deviceAddress.isNullOrEmpty()) {
                        mDevice =
                            BluetoothAdapter.getDefaultAdapter().getRemoteDevice(deviceAddress)
                        binding.tvRfidName.text = mDevice?.name ?: "Unknown Device"
                        binding.tvStaus.text = getString(R.string.connecting)
//                        binding.rlRfidStatus.visibility = View.VISIBLE
                        AnimationUtils.fadeInView(binding.rlRfidStatus);
                        connectToDevice(deviceAddress)
                    }
                }
            }

            REQUEST_ENABLE_BT -> {
                if (resultCode == Activity.RESULT_OK) {
                    showBluetoothDevice()
                } else {
                }
            }

        }
    }


    private fun connectToDevice(deviceAddress: String) {
        if (uhfDevice.connectStatus == ConnectionStatus.CONNECTING) {
            showToast(this, getString(R.string.connecting))
        } else {
            uhfDevice.connect(deviceAddress, object : ConnectionStatusCallback<Any?> {
                override fun getStatus(connectionStatus: ConnectionStatus, device: Any?) {
                    runOnUiThread {
                        if (connectionStatus == ConnectionStatus.CONNECTED) {
                            Log.e("ConetionTAG", "getStatus: " + "IF")
                            UHFConnectionManager.updateConnectionStatus(connectionStatus, device)
                            binding.tvStaus.text = getString(R.string.connected)
                            AnimationUtils.fadeInView(binding.rlRfidStatus);
                            AnimationUtils.fadeOutView(binding.footerView);
                        } else {
                            UHFConnectionManager.updateConnectionStatus(ConnectionStatus.DISCONNECTED, device)
                            Log.e("ConetionTAG", "getStatus: " + "ELSE")
//                            showToast(this@DashboardActivity, getString(R.string.disConnect))
                            binding.tvStaus.text = getString(R.string.disconnected)
                            AnimationUtils.fadeInView(binding.footerView);
                            AnimationUtils.fadeOutView(binding.rlRfidStatus);
                        }
                    }
                }
            })
        }
    }


    override fun onDestroy() {
        dashboardViewModel.disconnect(true)
        UHFConnectionManager.removeStatusChangeListener(connectionListener)
        super.onDestroy()
    }

    override fun onBackPressed() {
        val fragmentManager = supportFragmentManager
        if (fragmentManager.backStackEntryCount > 0) {
            fragmentManager.popBackStack() // Removes the top fragment
            updateToolbarTitle(getString(R.string.dashboard));
        } else {
            super.onBackPressed() // Exits the activity and goes back to MainActivity
        }
    }


}
