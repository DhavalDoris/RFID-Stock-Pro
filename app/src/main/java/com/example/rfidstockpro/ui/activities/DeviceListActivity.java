package com.example.rfidstockpro.ui.activities;

import static com.example.rfidstockpro.viewmodel.DashboardViewModel.SHOW_HISTORY_CONNECTED_LIST;

import android.Manifest;
import android.app.Activity;
import android.bluetooth.BluetoothDevice;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import com.example.rfidstockpro.R;
import com.example.rfidstockpro.Utils.ToastUtils;
import com.example.rfidstockpro.tools.FileUtils;
import com.rscja.deviceapi.RFIDWithUHFBLE;
import com.rscja.deviceapi.interfaces.ScanBTCallback;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class DeviceListActivity extends AppCompatActivity {

    // private BluetoothAdapter mBtAdapter;
    private TextView mEmptyList;
    public static final String TAG = "DeviceListActivityKT";

    private TextView tvTitle;

    private List<MyDevice> deviceList;
    private DeviceAdapter deviceAdapter;
    private Map<String, Integer> devRssiValues;
    private static final long SCAN_PERIOD = 10000; //10 seconds

    private Handler mHandler = new Handler();
    private boolean mScanning;

    RFIDWithUHFBLE uhf = RFIDWithUHFBLE.getInstance();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        this.requestWindowFeature(Window.FEATURE_NO_TITLE);
        super.onCreate(savedInstanceState);

        // 设置窗体背景透明
        getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));

        setContentView(R.layout.device_list);

        if (!getPackageManager().hasSystemFeature(PackageManager.FEATURE_BLUETOOTH_LE)) {
            Toast.makeText(this, R.string.ble_not_supported, Toast.LENGTH_SHORT).show();
            finish();
        }

        init();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == 999) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                scanLeDevice(true);
            } else {
                Toast.makeText(this, "Please grant Bluetooth permission", Toast.LENGTH_SHORT).show();
            }
        }
    }

    private void init() {
        tvTitle = findViewById(R.id.title_devices);
        mEmptyList = (TextView) findViewById(R.id.empty);
        findViewById(R.id.close).setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
            }
        });

        devRssiValues = new HashMap<String, Integer>();
        deviceList = new ArrayList<>();
        deviceAdapter = new DeviceAdapter(this, deviceList);

        Button cancelButton = (Button) findViewById(R.id.btn_cancel);
        cancelButton.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                if (mScanning == false) {
                    scanLeDevice(true);
                } else {
                    finish();
                }
            }
        });

        Button btnClearHistory = findViewById(R.id.btnClearHistory);
        btnClearHistory.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                FileUtils.clearXmlList();
                deviceList.clear();
                deviceAdapter.notifyDataSetChanged();
                mEmptyList.setVisibility(View.VISIBLE);
            }
        });

        boolean isHistoryList = getIntent().getBooleanExtra(SHOW_HISTORY_CONNECTED_LIST, false);
        if (isHistoryList) {
            tvTitle.setText(R.string.history_connected_device);
            mEmptyList.setText(R.string.no_history);
            cancelButton.setVisibility(View.GONE);
            List<String[]> deviceList = FileUtils.readXmlList();
            for (String[] device : deviceList) {
                MyDevice myDevice = new MyDevice(device[0], device[1]);
                addDevice(myDevice, 0);
            }
        } else { // 搜索蓝牙设备
            tvTitle.setText(R.string.select_device);
            mEmptyList.setText(R.string.scanning);
            btnClearHistory.setVisibility(View.GONE);

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                String[] permission = new String[]{
                        Manifest.permission.BLUETOOTH_SCAN,
                        Manifest.permission.BLUETOOTH_ADVERTISE,
                        Manifest.permission.BLUETOOTH_CONNECT
                };
                ActivityCompat.requestPermissions(this, permission, 999);
            } else {
                scanLeDevice(true);
            }
        }

        ListView newDevicesListView = (ListView) findViewById(R.id.new_devices);
        newDevicesListView.setAdapter(deviceAdapter);
        newDevicesListView.setOnItemClickListener(mDeviceClickListener);
    }

    private void scanLeDevice(final boolean enable) {
        final Button cancelButton = (Button) findViewById(R.id.btn_cancel);
        if (enable) {
            // Stops scanning after a pre-defined scan period.
            mHandler.postDelayed(new Runnable() {
                @Override
                public void run() {
                    mScanning = false;
                    uhf.stopScanBTDevices();
                    cancelButton.setText(R.string.scan);
                }
            }, SCAN_PERIOD);

            mScanning = true;
            Log.e("DeviceListTAG", "getDevices() 1 : "   );
            uhf.startScanBTDevices(new ScanBTCallback() {
                @Override
                public void getDevices(final BluetoothDevice bluetoothDevice, final int rssi, byte[] bytes) {
                    Log.e("DeviceListTAG", "getDevices() : "   );
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            Log.d(TAG, "Scan successful");

                            MyDevice myDevice = new MyDevice(bluetoothDevice.getAddress(), bluetoothDevice.getName());
                            addDevice(myDevice, rssi);
                            Log.e("DeviceTAG", "run: => " + myDevice );
                            Log.e("DeviceTAG", "run: -> " + rssi );
                        }
                    });
                }
            });
            cancelButton.setText(R.string.cancel);
        } else {
            mScanning = false;
            uhf.stopScanBTDevices();
            cancelButton.setText(R.string.scan);
        }
    }

    @Override
    public void onStop() {
        super.onStop();
        uhf.stopScanBTDevices();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        uhf.stopScanBTDevices();
    }

    private void addDevice(MyDevice device, int rssi) {
        Log.e("DeviceListTAG", "addDevice(): "  );
        boolean deviceFound = false;
        if (device.getName() == null || device.getName().equals("")) return;
        for (MyDevice listDev : deviceList) {
            if (listDev.getAddress().equals(device.getAddress())) {
                deviceFound = true;
                break;
            }
        }

        devRssiValues.put(device.getAddress(), rssi);
        if (!deviceFound) {
            deviceList.add(device);
            Log.e(TAG, "addDevice: ----> " + deviceList.size() );
            Log.e(TAG, "device: ====> " + device );
            mEmptyList.setVisibility(View.GONE);
        }

        // 根据信号强度重新排序
        Collections.sort(deviceList, new Comparator<MyDevice>() {
            @Override
            public int compare(MyDevice device1, MyDevice device2) {
                String key1 = device1.getAddress();
                String key2 = device2.getAddress();
                int v1 = devRssiValues.get(key1);
                int v2 = devRssiValues.get(key2);
                if (v1 > v2) {
                    return -1;
                } else if (v1 < v2) {
                    return 1;
                } else {
                    return 0;
                }
            }
        });
        if (!deviceFound) {
            deviceAdapter.notifyDataSetChanged();
        }
    }

    private OnItemClickListener mDeviceClickListener = new OnItemClickListener() {

        @Override
        public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
            MyDevice device = deviceList.get(position);
            uhf.stopScanBTDevices();

            String address = device.getAddress().trim();
            if (!TextUtils.isEmpty(address)) {
                Bundle b = new Bundle();
                b.putString(BluetoothDevice.EXTRA_DEVICE, device.getAddress());
                b.putBoolean("isSearch", true);
                Intent result = new Intent();
                result.putExtras(b);
                setResult(Activity.RESULT_OK, result);
                finish();
            } else {
                Toast.makeText(DeviceListActivity.this, R.string.invalid_bluetooth_address, Toast.LENGTH_SHORT).show();
            }
        }
    };

    protected void onPause() {
        super.onPause();
        Log.d(TAG, "scanLeDevice==============>");
        scanLeDevice(false);
    }

    class MyDevice {
        private String address;
        private String name;
        private int bondState;

        public MyDevice() {

        }

        public MyDevice(String address, String name) {
            this.address = address;
            this.name = name;
        }

        public String getAddress() {
            return address;
        }

        public void setAddress(String address) {
            this.address = address;
        }

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public int getBondState() {
            return bondState;
        }

        public void setBondState(int bondState) {
            this.bondState = bondState;
        }
    }

    class DeviceAdapter extends BaseAdapter {
        Context context;
        List<MyDevice> devices;
        LayoutInflater inflater;

        public DeviceAdapter(Context context, List<MyDevice> devices) {
            this.context = context;
            inflater = LayoutInflater.from(context);
            this.devices = devices;
        }

        @Override
        public int getCount() {
            return devices.size();
        }

        @Override
        public Object getItem(int position) {
            return devices.get(position);
        }

        @Override
        public long getItemId(int position) {
            return position;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            ViewGroup vg;

            if (convertView != null) {
                vg = (ViewGroup) convertView;
            } else {
                vg = (ViewGroup) inflater.inflate(R.layout.device_element, null);
            }

            MyDevice device = devices.get(position);
            final TextView tvadd = ((TextView) vg.findViewById(R.id.address));
            final TextView tvname = ((TextView) vg.findViewById(R.id.name));
            final TextView tvpaired = (TextView) vg.findViewById(R.id.paired);
            final TextView tvrssi = (TextView) vg.findViewById(R.id.rssi);

            int rssival = devRssiValues.get(device.getAddress()).intValue();
            if (rssival != 0) {
                tvrssi.setText(String.format("Rssi = %d", rssival));
                tvrssi.setTextColor(Color.BLACK);
                tvrssi.setVisibility(View.VISIBLE);
            }

            tvname.setText(device.getName());
            tvname.setTextColor(Color.BLACK);
            tvadd.setText(device.getAddress());
            tvadd.setTextColor(Color.BLACK);
            if (device.getBondState() == BluetoothDevice.BOND_BONDED) {
                Log.i(TAG, "device::" + device.getName());
                tvpaired.setText(R.string.paired);
                tvpaired.setTextColor(Color.RED);
                tvpaired.setVisibility(View.VISIBLE);
            } else {
                tvpaired.setVisibility(View.GONE);
            }
            return vg;
        }
    }
}
