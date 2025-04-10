import com.rscja.deviceapi.interfaces.ConnectionStatus

object UHFConnectionManager {


    private var connectionStatus: ConnectionStatus = ConnectionStatus.DISCONNECTED
    private var connectedDevice: Any? = null
    private var statusChangeListeners = mutableListOf<ConnectionStatusListener>()

    fun updateConnectionStatus(status: ConnectionStatus, device: Any?) {
        connectionStatus = status
        connectedDevice = device
        notifyListeners()
    }

    fun getConnectionStatus() = connectionStatus
    fun getConnectedDevice() = connectedDevice

    fun addStatusChangeListener(listener: ConnectionStatusListener) {
        if (!statusChangeListeners.contains(listener)) {
            statusChangeListeners.add(listener)
        }
    }

    fun removeStatusChangeListener(listener: ConnectionStatusListener) {
        statusChangeListeners.remove(listener)
    }

    private fun notifyListeners() {
        for (listener in statusChangeListeners.toList()) {
                listener.onConnectionStatusChanged(connectionStatus, connectedDevice)
        }
    }

    interface ConnectionStatusListener {
        fun onConnectionStatusChanged(status: ConnectionStatus, device: Any?)
    }
}
