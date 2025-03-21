package com.example.rfidstockpro.ui.activities

import android.Manifest
import android.annotation.SuppressLint
import android.app.Activity
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.content.DialogInterface
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Color
import android.graphics.Typeface
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.os.Handler
import android.os.Looper
import android.os.Message
import android.os.SystemClock
import android.provider.Settings
import android.text.Spannable
import android.text.SpannableString
import android.text.TextUtils
import android.text.style.ForegroundColorSpan
import android.text.style.RelativeSizeSpan
import android.util.Log
import android.view.View
import android.widget.AdapterView
import androidx.activity.viewModels
import androidx.annotation.RequiresPermission
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.content.res.ResourcesCompat
import androidx.lifecycle.ViewModelProvider
import com.example.rfidstockpro.R
import com.example.rfidstockpro.Utils.SPUtils
import com.example.rfidstockpro.Utils.StatusBarUtils
import com.example.rfidstockpro.Utils.ToastUtils.showToast
import com.example.rfidstockpro.adapter.CustomSpinnerAdapter
import com.example.rfidstockpro.databinding.ActivityDashboardBinding
import com.example.rfidstockpro.tools.FileUtils
import com.example.rfidstockpro.viewmodel.DashboardViewModel
import com.github.mikephil.charting.charts.PieChart
import com.github.mikephil.charting.data.PieData
import com.github.mikephil.charting.data.PieDataSet
import com.github.mikephil.charting.data.PieEntry
import com.rscja.deviceapi.RFIDWithUHFBLE
import com.rscja.deviceapi.interfaces.ConnectionStatus
import com.rscja.deviceapi.interfaces.ConnectionStatusCallback
import java.util.Timer
import java.util.TimerTask

class DashboardActivity : AppCompatActivity() {

    private lateinit var binding: ActivityDashboardBinding
    private lateinit var pieChart: PieChart
    private lateinit var viewModel: DashboardViewModel
    private val dashboardViewModel: DashboardViewModel by viewModels()
    private val timeFilterOptions = listOf("Weekly", "Monthly", "Yearly")

    private val REQUEST_ENABLE_BT: Int = 2
    private val REQUEST_SELECT_DEVICE: Int = 1

    var mBtAdapter: BluetoothAdapter? = null

    var btStatus: BTStatus = BTStatus()


    private val connectStatusList: MutableList<IConnectStatus> =
        java.util.ArrayList<IConnectStatus>()
    private var timerTask: DisconnectTimerTask? = null

    interface IConnectStatus {
        fun getStatus(connectionStatus: ConnectionStatus?)
    }

    companion object {

        private val mDisconnectTimer = Timer()

        const val PERMISSION_REQUEST_ACCESS_FINE_LOCATION: Int = 100
        const val PERMISSION_REQUEST_ACTION_LOCATION_SETTINGS: Int = 103
        const val PERMISSION_REQUEST_BLUETOOTH: Int = 104
        const val PERMISSION_REQUEST_BLUETOOTH_CONNECT: Int = 105

        private val connectStatusList: List<IConnectStatus> = java.util.ArrayList()

        var mDevice: BluetoothDevice? = null

        public val TAG: String = "MainActivity"
        var isKeyDownUP: Boolean = false

        val RECONNECT_NUM: Int = Int.MAX_VALUE
        private var mReConnectCount: Int = RECONNECT_NUM


        const val SHOW_HISTORY_CONNECTED_LIST: String = "showHistoryConnectedList"
        const val PERMISSION_REQUEST_EXTERNAL_STORAGE: Int = 101
        const val REQUEST_ACTION_LOCATION_SETTINGS: Int = 99

        private var lastTouchTime = System.currentTimeMillis()
        private val period = (1000 * 30).toLong()
        var timeCountCur: Long = 0
        val RUNNING_DISCONNECT_TIMER: Int = 10
        var uhf: RFIDWithUHFBLE = RFIDWithUHFBLE.getInstance()
        var isScanning: Boolean = false
        private var mIsActiveDisconnect = true
        private var dashboardActivity: DashboardActivity? = null


        var remoteBTName: String = ""
        var remoteBTAdd: String = ""


        private val mHandler: Handler = object : Handler(Looper.getMainLooper()) {
            override fun handleMessage(msg: Message) {
                when (msg.what) {
                    RUNNING_DISCONNECT_TIMER -> {
                        val time = msg.obj as Long
                        dashboardActivity!!.formatConnectButton(time)
                    }
                }
            }
        }

        fun resetDisconnectTime() {
            timeCountCur =
                SPUtils.getInstance(dashboardActivity).getSPLong(SPUtils.DISCONNECT_TIME, 0)
            if (timeCountCur > 0) {
                dashboardActivity!!.formatConnectButton(timeCountCur)
            }
        }

        private var timerTask: DisconnectTimerTask? = null

        fun disconnect(isActiveDisconnect: Boolean) {
            cancelDisconnectTimer()
            mIsActiveDisconnect = isActiveDisconnect // 主动断开为true
            uhf.disconnect()
        }

        fun cancelDisconnectTimer() {
            timeCountCur = 0
            if (timerTask != null) {
                timerTask!!.cancel()
                timerTask = null
            }
        }

        private class DisconnectTimerTask : TimerTask() {
            override fun run() {
                Log.e("TAG", "timeCountCur = $timeCountCur")
                val msg = mHandler.obtainMessage(
                    RUNNING_DISCONNECT_TIMER,
                    timeCountCur
                )
                mHandler.sendMessage(msg)
                if (isScanning) {
                    resetDisconnectTime()
                } else if (timeCountCur <= 0) {
                    disconnect(true)
                }
                timeCountCur -= period
            }
        }

        private fun shouldShowDisconnected(): Boolean {
            return mIsActiveDisconnect || mReConnectCount == 0
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityDashboardBinding.inflate(layoutInflater)
        setContentView(binding.root)
        dashboardActivity = this // Assign activity instance

        viewModel = ViewModelProvider(this).get(DashboardViewModel::class.java)
        mBtAdapter = BluetoothAdapter.getDefaultAdapter()
        uhf.init(applicationContext)
        StatusBarUtils.setStatusBarColor(this)
        checkPermission()
        setupUI()
        setupPieChart()
        observeViewModel()
        setupSpinner()
    }

    public infix fun formatConnectButton(disconnectTime: Long) {
        if (uhf.connectStatus == ConnectionStatus.CONNECTED) {
            if (!isScanning && System.currentTimeMillis() - lastTouchTime > 1000 * 30 && Companion.timerTask != null) {
                val minute = disconnectTime / 1000 / 60
                if (minute > 0) {
                    binding.btnConnectScanner.setText(
                        getString(
                            R.string.disConnectForMinute,
                            minute
                        )
                    )
                } else {
                    binding.btnConnectScanner.setText(
                        getString(
                            R.string.disConnectForSecond,
                            disconnectTime / 1000
                        )
                    )
                }
            } else {
//                binding.btnConnectScanner.setText(R.string.disConnect)
                binding.footerView.visibility = View.GONE
            }
        } else {
            binding.btnConnectScanner.setText(R.string.Connect)
        }
    }

    private fun setupUI() {
        binding.btnConnectScanner.setOnClickListener @androidx.annotation.RequiresPermission(android.Manifest.permission.BLUETOOTH_CONNECT) {

            // Connect
          /*  if (uhf.connectStatus == ConnectionStatus.CONNECTING) {
                showToast(dashboardActivity!!, R.string.connecting.toString())
            } else if (uhf.connectStatus == ConnectionStatus.CONNECTED) {
                disconnect(true)
            } else {
                showBluetoothDevice(true)
            }*/

             // search
             if (isScanning) {
                 showToast(dashboardActivity!!, R.string.title_stop_read_card.toString())
             } else if (uhf.connectStatus == ConnectionStatus.CONNECTING) {
                 showToast(dashboardActivity!!, R.string.connecting.toString())
             } else {
                 showBluetoothDevice(false)
             }
        }

        binding.btnDisconnect.setOnClickListener {
            disconnect(true)
            binding.footerView.visibility = View.VISIBLE
        }

    }

    @RequiresPermission(Manifest.permission.BLUETOOTH_CONNECT)
    private fun showBluetoothDevice(isHistory: Boolean) {
        if (mBtAdapter == null) {
            showToast(dashboardActivity!!, "Bluetooth is not available")
            return
        }
        if (!mBtAdapter!!.isEnabled()) {
            Log.i(TAG, "onClick - BT not enabled yet")
            val enableIntent = Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE)
            startActivityForResult(enableIntent, REQUEST_ENABLE_BT)
        } else {
            val newIntent: Intent = Intent(this@DashboardActivity, DeviceListActivity::class.java)
            newIntent.putExtra(SHOW_HISTORY_CONNECTED_LIST, isHistory)
            startActivityForResult(newIntent, REQUEST_SELECT_DEVICE)
            cancelDisconnectTimer()
        }
    }


    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        Log.i(TAG, "onActivityResult requestCode=$requestCode resultCode=$resultCode data=$data")

        when (requestCode) {
            REQUEST_SELECT_DEVICE -> {
                // When the DeviceListActivity returns with the selected device address
                if (resultCode == Activity.RESULT_OK && data != null) {
                    if (uhf.connectStatus == ConnectionStatus.CONNECTED) {
                        disconnect(true)
                    }
                    val deviceAddress = data.getStringExtra(BluetoothDevice.EXTRA_DEVICE)
                    deviceAddress?.let {
                        mDevice = BluetoothAdapter.getDefaultAdapter().getRemoteDevice(it)
                        if (data.getBooleanExtra("isSearch", true)) {
                            uhf.startScanBTDevices { _, _, _ -> }
                            SystemClock.sleep(1500)
                            uhf.stopScanBTDevices()
                        }

                        dashboardActivity!!.binding.rlRfidStatus.visibility = View.VISIBLE
                        binding.tvRfidName.text =  mDevice?.name
                        binding.tvStaus.text = "Connecting..."
                        /* binding.btnConnectScanner.setText(
                            "connecting"
                        )*/
                        connect(it)
                    }
                }
            }

            REQUEST_ENABLE_BT -> {
                if (resultCode == Activity.RESULT_OK) {
                    showToast(dashboardActivity!!, "Bluetooth has turned on")
                } else {
                    showToast(dashboardActivity!!, "Problem in BT Turning ON")
                }
            }

            PERMISSION_REQUEST_EXTERNAL_STORAGE -> {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                    if (!Environment.isExternalStorageManager()) {
                        showPermissionAlertDialog(getString(R.string.permission_external_storage)) { _, _ ->
                            checkReadWritePermission()
                        }
                    }
                } else {
                    if (checkSelfPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
                        showPermissionAlertDialog(getString(R.string.permission_external_storage)) { _, _ ->
                            checkReadWritePermission()
                        }
                    }
                }
            }

            REQUEST_ACTION_LOCATION_SETTINGS -> {
                if (isLocationEnabled()) {
                    checkPermission()
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
        }
    }


    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        Log.v(
            TAG,
            "onRequestPermissionsResult requestCode=$requestCode permissions=${permissions.contentToString()} grantResults=${grantResults.contentToString()}"
        )

        when (requestCode) {
            PERMISSION_REQUEST_EXTERNAL_STORAGE -> {
                if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    // checkLocationPermission()
                    // BleApplication.getApplication().createDir()
                } else {
                    showPermissionAlertDialog(getString(R.string.permission_external_storage)) { _, _ ->
                        checkReadWritePermission()
                    }
                }
            }

            PERMISSION_REQUEST_ACCESS_FINE_LOCATION -> {
                if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    checkBluetoothPermission()
                } else {
                    showPermissionAlertDialog(getString(R.string.permission_location)) { _, _ ->
                        checkLocationPermission()
                    }
                }
            }

            PERMISSION_REQUEST_BLUETOOTH -> {
                if (grantResults.isEmpty() || grantResults[0] == PackageManager.PERMISSION_DENIED) {
                    showPermissionAlertDialog(getString(R.string.permission_bluetooth)) { _, _ ->
                        checkBluetoothPermission()
                    }
                } else {
                    checkReadWritePermission()
                }
            }

            PERMISSION_REQUEST_BLUETOOTH_CONNECT -> {
                if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    val enableIntent = Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE)
                    startActivityForResult(enableIntent, REQUEST_ENABLE_BT)
                } else {
                    showPermissionAlertDialog(getString(R.string.permission_bluetooth)) { _, _ ->
                        requestPermissions(
                            arrayOf(Manifest.permission.BLUETOOTH),
                            PERMISSION_REQUEST_BLUETOOTH_CONNECT
                        )
                    }
                }
            }
        }
    }


    private fun checkReadWritePermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            if (!Environment.isExternalStorageManager()) {
                val intent = Intent(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION)
                intent.setData(Uri.parse("package:$packageName"))
                startActivityForResult(
                    intent,
                    PERMISSION_REQUEST_EXTERNAL_STORAGE
                )
                //                finish();
            }
        } else {
            if (checkSelfPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE) !== PackageManager.PERMISSION_GRANTED) {
                requestPermissions(
                    arrayOf<String>(
                        Manifest.permission.WRITE_EXTERNAL_STORAGE,
                        Manifest.permission.READ_EXTERNAL_STORAGE
                    ),
                    PERMISSION_REQUEST_EXTERNAL_STORAGE
                )
            }
        }
    }

    private fun isLocationEnabled(): Boolean {
        try {
            val locationMode =
                Settings.Secure.getInt(contentResolver, Settings.Secure.LOCATION_MODE)
            return locationMode != Settings.Secure.LOCATION_MODE_OFF
        } catch (e: Exception) {
            e.printStackTrace()
            return false
        }
    }

    fun connect(deviceAddress: String) {
        if (uhf.connectStatus == ConnectionStatus.CONNECTING) {
            showToast(this@DashboardActivity, R.string.connecting.toString())
        } else {
            showToast(this@DashboardActivity, getString(R.string.Connect) + " " + deviceAddress)
            uhf.connect(deviceAddress, btStatus)
        }
    }

    private fun showPermissionAlertDialog(msg: String?, listener: DialogInterface.OnClickListener) {
        if (msg == null) return
        val dialog = AlertDialog.Builder(this)
            .setTitle(getString(R.string.permission))
            .setMessage(msg)
            .setPositiveButton(getString(R.string.permission_enable), listener)
            .setNegativeButton(getString(R.string.permission_cancel)) { dialog1, which -> finish() }
            .setCancelable(false)
            .show()
//            Button positiveButton = dialog.getButton(DialogInterface.BUTTON_POSITIVE);
//            positiveButton.setTextColor(getColor(R.color.font_default));
    }


    class BTStatus : ConnectionStatusCallback<Any?> {
        override fun getStatus(connectionStatus: ConnectionStatus, device1: Any?) {
            dashboardActivity!!.runOnUiThread(Runnable {
                @SuppressLint("MissingPermission")
                val device = device1 as BluetoothDevice?
                Log.i(
                    TAG,
                    "getStatus connectionStatus=$connectionStatus device=$device"
                )
                remoteBTName = ""
                remoteBTAdd = ""
                if (connectionStatus == ConnectionStatus.CONNECTED) {
                    remoteBTName = device!!.name
                    remoteBTAdd = device!!.address
                    Log.i(TAG, "remoteBTName=$remoteBTName remoteBTAdd=$remoteBTAdd")

                    dashboardActivity!!.binding.rlRfidStatus.visibility = View.VISIBLE
                    dashboardActivity!!.binding.tvRfidName.setText(remoteBTName)
                    dashboardActivity!!.binding.tvStaus.text = "Connected"
//                    dashboardActivity!!.binding.tvRfidName.setText(String.format("%s(%s)\nconnected", remoteBTName, remoteBTAdd))

                    if (shouldShowDisconnected()) {
                        showToast(dashboardActivity!!, R.string.connect_success.toString())
                    }

                    timeCountCur = SPUtils.getInstance(dashboardActivity)
                        .getSPLong(SPUtils.DISCONNECT_TIME, 0)
                    if (timeCountCur > 0) {
                        startDisconnectTimer(timeCountCur)
                    } else {
                        dashboardActivity!! formatConnectButton (timeCountCur)
                    }

                    // 保存已链接记录
                    if (!TextUtils.isEmpty(remoteBTAdd)) {
                        dashboardActivity!!.saveConnectedDevice(remoteBTAdd, remoteBTName)
                    }

                    mIsActiveDisconnect = false
                    mReConnectCount = RECONNECT_NUM
                } else if (connectionStatus == ConnectionStatus.DISCONNECTED) {
                    isKeyDownUP = false
                    cancelDisconnectTimer()
                    dashboardActivity!! formatConnectButton (timeCountCur)
                    if (device != null && device.name != null) {
                        remoteBTName = device.name
                        remoteBTAdd = device.address

                        dashboardActivity!!.binding.rlRfidStatus.visibility = View.GONE
                        dashboardActivity!!.binding.footerView.visibility = View.VISIBLE
                        /* dashboardActivity!!.binding.tvRfidName.setText( String.format(
                             "%s(%s)\ndisconnected",
                             remoteBTName,
                             remoteBTAdd
                         ))*/
                    } else {
                        //  if (shouldShowDisconnected())
//                        tvAddress.setText("disconnected")
//                        dashboardActivity!!.binding.tvRfidName.setText("disconnected")
//                        dashboardActivity!!.binding.rlRfidStatus.visibility = View.GONE
                        dashboardActivity!!.binding.rlRfidStatus.visibility = View.GONE
                        dashboardActivity!!.binding.footerView.visibility = View.VISIBLE
                    }
                    if (shouldShowDisconnected()) showToast(
                        dashboardActivity!!,
                        R.string.disConnect.toString()
                    )

                    val reconnect = SPUtils.getInstance(dashboardActivity)
                        .getSPBoolean(SPUtils.AUTO_RECONNECT, false)
                    if (mDevice != null && reconnect) {
                        dashboardActivity!!.reConnect(mDevice!!.getAddress()) // 重连
                    }
                }
                for (iConnectStatus in connectStatusList) {
                    iConnectStatus?.getStatus(connectionStatus)
                }
            })
        }

        private fun startDisconnectTimer(time: Long) {
            timeCountCur = time
            timerTask = DisconnectTimerTask()
            mDisconnectTimer.schedule(timerTask, 0, period)
        }
    }


    fun saveConnectedDevice(address: String, name: String?) {
        val list: MutableList<Array<String?>> = FileUtils.readXmlList()
        var oldItem: Array<String?>? = null
        for (i in list.indices) {
            if (address == list[i][0]) {
                oldItem = list[i]
                list.remove(list[i])
                break
            }
        }
        var strArr = arrayOf(address, name)
        if (name == null && oldItem != null) {
            strArr = oldItem
            val btName = oldItem[1]
            mHandler.post {
                binding.rlRfidStatus.visibility = View.VISIBLE
                binding.tvRfidName.setText(btName)
                binding.tvStaus.text = "Connected"
            }
        }
        list.add(0, strArr)
        FileUtils.saveXmlList(list)
    }

    private fun reConnect(deviceAddress: String) {
        Log.i(
            TAG,
            "自动重连" + deviceAddress + " " + (!mIsActiveDisconnect && mReConnectCount > 0)
        )
        if (!mIsActiveDisconnect && mReConnectCount > 0) {
            connect(deviceAddress)
            mReConnectCount--
        }
    }

    private fun checkPermission() {
        if (!isLocationEnabled()) {
            val intent = Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS)
            startActivityForResult(
                intent,
                REQUEST_ACTION_LOCATION_SETTINGS
            )
        }
        if (checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION) !== PackageManager.PERMISSION_GRANTED) {
            checkLocationPermission()
        } else if ((Build.VERSION.SDK_INT >= Build.VERSION_CODES.R && !Environment.isExternalStorageManager()) ||
            (Build.VERSION.SDK_INT < Build.VERSION_CODES.R && checkSelfPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE) !== PackageManager.PERMISSION_GRANTED)
        ) {
            checkReadWritePermission()
        } else if (checkSelfPermission(Manifest.permission.BLUETOOTH) !== PackageManager.PERMISSION_GRANTED) {
            checkBluetoothPermission()
        }
    }

    private fun checkLocationPermission(): Boolean {
        if (checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION) !== PackageManager.PERMISSION_GRANTED) {
            requestPermissions(
                arrayOf<String>(Manifest.permission.ACCESS_FINE_LOCATION),
                PERMISSION_REQUEST_ACCESS_FINE_LOCATION
            )
            return false
        }
        return true
    }


    private fun checkBluetoothPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            requestPermissions(
                arrayOf<String>(
                    Manifest.permission.BLUETOOTH_SCAN,
                    Manifest.permission.BLUETOOTH_ADVERTISE,
                    Manifest.permission.BLUETOOTH_CONNECT
                ),
                PERMISSION_REQUEST_BLUETOOTH
            )
        } else {
            requestPermissions(
                arrayOf<String>(Manifest.permission.BLUETOOTH),
                PERMISSION_REQUEST_BLUETOOTH
            )
        }
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
        /*viewModel.stockData.observe(this) { data ->
            val dataSet = PieDataSet(data, "")
            dataSet.colors = listOf(
                Color.parseColor("#20C4B3"), // Active (Teal)
                Color.parseColor("#F1875F"), // Pending (Orange)
                Color.parseColor("#E6504B")  // Inactive (Red)
            )
//            dataSet.valueTextSize = 14f
//            dataSet.valueTextColor = Color.WHITE

            val pieData = PieData(dataSet)
            pieChart.data = pieData
            pieChart.invalidate() // Refresh



        }*/

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

    override fun onDestroy() {
        dashboardActivity = null // Prevent memory leaks
        super.onDestroy()
    }
}
