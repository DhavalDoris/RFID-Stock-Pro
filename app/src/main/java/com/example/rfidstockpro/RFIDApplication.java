package com.example.rfidstockpro;

import android.app.Application;

public class RFIDApplication extends Application {
    private static final String TAG = "AppContext";

    private static RFIDApplication mApp;

    public static RFIDApplication getInstance() {
        return mApp;
    }

    @Override
    public void onCreate() {
        super.onCreate();

        mApp = this;
//        ToastUtil.init(mApp);
    }
}
