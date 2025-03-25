package com.example.rfidstockpro.viewmodel

import android.app.Application
import android.os.Handler
import android.os.Looper
import android.os.Message
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.example.rfidstockpro.R
import com.example.rfidstockpro.Utils.SPUtils
import com.github.mikephil.charting.data.PieEntry
import com.rscja.deviceapi.RFIDWithUHFBLE
import com.rscja.deviceapi.interfaces.ConnectionStatus
import java.util.Timer
import java.util.TimerTask

class DashboardViewModel(application: Application) : AndroidViewModel(application) {

    private val _toolbarTitle = MutableLiveData<String>()
    val toolbarTitle: LiveData<String> = _toolbarTitle

    private val _stockData = MutableLiveData<List<PieEntry>>()
    val stockData: LiveData<List<PieEntry>> get() = _stockData

    private val _rfidStatusText = MutableLiveData<String>()
    val rfidStatusText: LiveData<String> get() = _rfidStatusText

    private val _connectButtonText = MutableLiveData<String>()
    val connectButtonText: LiveData<String> get() = _connectButtonText

    private val _footerVisibility = MutableLiveData<Boolean>()
    val footerVisibility: LiveData<Boolean> get() = _footerVisibility

    private val _isConnected = MutableLiveData<Boolean>()
    val isConnected: LiveData<Boolean> get() = _isConnected

    private val uhf = RFIDWithUHFBLE.getInstance()
    private var timerTask: DisconnectTimerTask? = null
    private val disconnectTimer = Timer()

    private val applicationContext = getApplication<Application>().applicationContext

    init {
        loadStaticData()
        checkBluetoothConnection()
    }

    private fun loadStaticData() {
        val entries = listOf(
            PieEntry(15f, "Active"),
            PieEntry(10f, "Pending"),
            PieEntry(30f, "Inactive")
        )
        _stockData.value = entries
    }

    companion object {


        const val SHOW_HISTORY_CONNECTED_LIST: String = "showHistoryConnectedList"
        private const val RUNNING_DISCONNECT_TIMER = 10
        private var timeCountCur: Long = 0
        private val period: Long = (1000 * 30)

        private val mHandler = object : Handler(Looper.getMainLooper()) {
            override fun handleMessage(msg: Message) {
                if (msg.what == RUNNING_DISCONNECT_TIMER) {
                    val time = msg.obj as Long
                    (msg.obj as? DashboardViewModel)?.formatConnectButton(time)
                }
            }
        }
    }

    fun setToolbarTitle(title: String) {
        _toolbarTitle.value = title
    }

    fun checkBluetoothConnection() {
        if (uhf.connectStatus == ConnectionStatus.CONNECTED) {
            _isConnected.value = true
            _rfidStatusText.value = applicationContext.getString(R.string.connect_success)
//            _connectButtonText.value = applicationContext.getString(R.string.disConnect)
            _footerVisibility.value = false
        } else {
            _isConnected.value = false
            _rfidStatusText.value = applicationContext.getString(R.string.disConnect)
//            _connectButtonText.value = applicationContext.getString(R.string.Connect)
            _footerVisibility.value = true
        }
    }

    fun resetDisconnectTime() {
        timeCountCur = SPUtils.getInstance(applicationContext).getSPLong(SPUtils.DISCONNECT_TIME, 0)
        if (timeCountCur > 0) {
            formatConnectButton(timeCountCur)
        }
    }

    fun disconnect(isActiveDisconnect: Boolean) {
        cancelDisconnectTimer()
        uhf.disconnect()
        _isConnected.value = false
        _rfidStatusText.value = applicationContext.getString(R.string.disConnect)
        _footerVisibility.value = true
    }

    fun cancelDisconnectTimer() {
        timeCountCur = 0
        timerTask?.cancel()
        timerTask = null
    }

    private fun formatConnectButton(disconnectTime: Long) {
        if (uhf.connectStatus == ConnectionStatus.CONNECTED) {
            _connectButtonText.value = applicationContext.getString(R.string.disConnect)
        } else {
            _connectButtonText.value = applicationContext.getString(R.string.Connect)
        }
    }

    fun startDisconnectTimer(time: Long) {
        timeCountCur = time
        timerTask = DisconnectTimerTask()
        disconnectTimer.schedule(timerTask, 0, period)
    }


    private inner class DisconnectTimerTask : TimerTask() {
        override fun run() {
            Log.e("DashboardViewModel", "timeCountCur = $timeCountCur")
            val msg = mHandler.obtainMessage(RUNNING_DISCONNECT_TIMER, timeCountCur)
            mHandler.sendMessage(msg)

            if (timeCountCur <= 0) {
                disconnect(true)
            }
            timeCountCur -= period
        }
    }
}
