package com.example.rfidstockpro

import android.app.Application
import com.example.rfidstockpro.aws.AwsManager


class RFIDApplication : Application() {
    override fun onCreate() {
        super.onCreate()

        instance = this
        //ToastUtil.init(mApp);
        AwsManager.init(this)
    }

    companion object {
        private const val TAG = "AppContext"

        var instance: RFIDApplication? = null
            private set
    }
}
