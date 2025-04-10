package com.example.rfidstockpro.ui.ProductManagement

import android.Manifest
import android.app.Activity
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.content.Context
import android.content.Intent
import androidx.activity.ComponentActivity
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.RequiresPermission
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.lifecycleScope
import com.example.rfidstockpro.R
import com.example.rfidstockpro.Utils.ToastUtils.showToast
import com.example.rfidstockpro.ui.activities.DashboardActivity.Companion.uhfDevice
import com.example.rfidstockpro.ui.activities.DeviceListActivity
import com.rscja.deviceapi.interfaces.ConnectionStatus
import com.rscja.deviceapi.interfaces.ConnectionStatusCallback
import kotlinx.coroutines.launch

object BluetoothConnectionManager {
    private var mBtAdapter: BluetoothAdapter? = BluetoothAdapter.getDefaultAdapter()
    private var mDevice: BluetoothDevice? = null

    private var enableBluetoothLauncher: ActivityResultLauncher<Intent>? = null
    private var selectDeviceLauncher: ActivityResultLauncher<Intent>? = null

    private var onDeviceConnected: ((BluetoothDevice) -> Unit)? = null
    private var onStatusUpdate: ((ConnectionStatus, Any?) -> Unit)? = null

    fun registerLaunchers(
        owner: ComponentActivity,
        onDeviceConnected: (BluetoothDevice) -> Unit,
        onStatusUpdate: (ConnectionStatus, Any?) -> Unit
    ) {
        this.onDeviceConnected = onDeviceConnected
        this.onStatusUpdate = onStatusUpdate

        enableBluetoothLauncher = owner.registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
            if (it.resultCode == Activity.RESULT_OK) {
                showBluetoothDevice(owner)
            }
        }

        selectDeviceLauncher = owner.registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == Activity.RESULT_OK) {
                val data = result.data
                val deviceAddress = data?.getStringExtra(BluetoothDevice.EXTRA_DEVICE)
                if (!deviceAddress.isNullOrEmpty()) {
                    mDevice = BluetoothAdapter.getDefaultAdapter().getRemoteDevice(deviceAddress)
                    connectToDevice(owner, deviceAddress)
                }
            }
        }
    }

    @RequiresPermission(Manifest.permission.BLUETOOTH_CONNECT)
    fun showBluetoothDevice(activity: Activity) {
        if (mBtAdapter == null) return

        if (!mBtAdapter!!.isEnabled) {
            val enableIntent = Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE)
            enableBluetoothLauncher?.launch(enableIntent)
        } else {
            val intent = Intent(activity, DeviceListActivity::class.java).apply {
                putExtra("show_history_connected_list", false)
            }
            selectDeviceLauncher?.launch(intent)
        }
    }

    private fun connectToDevice(context: Context, deviceAddress: String) {

        if (uhfDevice.connectStatus == ConnectionStatus.CONNECTING) {
            showToast(context, context.getString(R.string.connecting))
        } else {
            uhfDevice.connect(deviceAddress, object : ConnectionStatusCallback<Any?> {
                override fun getStatus(connectionStatus: ConnectionStatus, device: Any?) {
                    (context as? LifecycleOwner)?.lifecycleScope?.launch {
                        if (connectionStatus == ConnectionStatus.CONNECTED) {
                            onDeviceConnected?.invoke(mDevice!!)
                            onStatusUpdate?.invoke(connectionStatus, device)
                        } else {
                            onStatusUpdate?.invoke(ConnectionStatus.DISCONNECTED, device)
                        }
                    }
                }
            })
        }
    }
}
