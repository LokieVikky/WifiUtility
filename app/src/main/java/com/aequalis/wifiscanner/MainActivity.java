package com.aequalis.wifiscanner;

import android.Manifest;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.TrafficStats;
import android.net.wifi.ScanResult;
import android.net.wifi.WifiConfiguration;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.util.Log;
import android.view.View;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.databinding.DataBindingUtil;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.aequalis.wifiscanner.databinding.ActivityMainBinding;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class MainActivity extends AppCompatActivity implements ItemClickListener {
    ActivityMainBinding mBinding;
    List<ScanResult> mScanResult;
    WifiManager mWifiManager;
    WifiDetailAdapter mAdapter;
    List<NetworkModel> mNetworkList;
    ConnectivityManager mConnectivityManager;
    long mStartRx = 0, mStartTx = 0;
    BroadcastReceiver mWifiDetailsReceiver = new WifiDetailsReciver();
    private long mLastRxBytes = 0;
    private long mLastTxBytes = 0;
    private long mLastTime = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mBinding = DataBindingUtil.setContentView(this, R.layout.activity_main);
        // Initializing wifi manager
        mWifiManager = (WifiManager) getApplicationContext().getSystemService(Context.WIFI_SERVICE);
        mScanResult = new ArrayList<>();
        mNetworkList = new ArrayList<>();
        mAdapter = new WifiDetailAdapter(this);
        mBinding.recyclerView.setLayoutManager(new LinearLayoutManager(this));
        mBinding.recyclerView.setAdapter(mAdapter);

        mConnectivityManager = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo mWifi = mConnectivityManager.getNetworkInfo(ConnectivityManager.TYPE_WIFI);

        if (mWifi != null) {
            if (mWifi.isConnected()) {
                WifiInfo info = mWifiManager.getConnectionInfo();
                mBinding.txtSSID.setText(info.getSSID());
                mBinding.btnDisconnect.setVisibility(View.VISIBLE);
            } else {
                mBinding.txtSSID.setText("Not Connected");
                mBinding.btnDisconnect.setVisibility(View.GONE);
            }
        }

        mBinding.btnDisconnect.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                mWifiManager.disconnect();
            }
        });

        mLastRxBytes = TrafficStats.getTotalRxBytes();
        mLastTxBytes = TrafficStats.getTotalTxBytes();
        mLastTime = System.currentTimeMillis();

        startScan();
        startNetworkMonitor();
    }

    private void startNetworkMonitor() {
        mStartRx = TrafficStats.getTotalRxBytes();
        mStartTx = TrafficStats.getTotalTxBytes();
        new CountDownTimer(Long.MAX_VALUE, 500) {
            @Override
            public void onTick(long l) {
                updateSpeed();
            }

            @Override
            public void onFinish() {

            }
        }.start();
    }

    private void updateSpeed() {

        long currentRxBytes = TrafficStats.getTotalRxBytes();
        long currentTxBytes = TrafficStats.getTotalTxBytes();
        long usedRxBytes = currentRxBytes - mLastRxBytes;
        long usedTxBytes = currentTxBytes - mLastTxBytes;
        long currentTime = System.currentTimeMillis();
        long usedTime = currentTime - mLastTime;

        mLastRxBytes = currentRxBytes;
        mLastTxBytes = currentTxBytes;
        mLastTime = currentTime;

        long totalSpeed = 0;
        long downSpeed = 0;
        long upSpeed = 0;

        long totalBytes = usedRxBytes + usedTxBytes;

        if (usedTime > 0) {
            totalSpeed = totalBytes * 1000 / usedTime;
            downSpeed = usedRxBytes * 1000 / usedTime;
            upSpeed = usedTxBytes * 1000 / usedTime;
        }
        mBinding.txtSpeed.setText("Down: "+getSpeed(downSpeed)+"\n"+"Up: "+getSpeed(upSpeed));
    }

    private String getSpeed(long speed){
        return speed / 1000 +"kB/s";
    }

    private void startScan() {
        mWifiManager.startScan();
    }

    @Override
    protected void onResume() {
        super.onResume();
        try {
            IntentFilter filter = new IntentFilter();
            filter.addAction(WifiManager.SUPPLICANT_CONNECTION_CHANGE_ACTION);
            filter.addAction(WifiManager.NETWORK_STATE_CHANGED_ACTION);
            filter.addAction(WifiManager.SCAN_RESULTS_AVAILABLE_ACTION);
            registerReceiver(mWifiDetailsReceiver, filter);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void unregisterWifiReceiver() {
        try {
            unregisterReceiver(mWifiDetailsReceiver);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        unregisterWifiReceiver();
    }

    public void connectWiFi(ScanResult scanResult) {
        try {

            Log.v("rht", "Item clicked, SSID " + scanResult.SSID + " Security : " + scanResult.capabilities);

            String networkSSID = scanResult.SSID;
            String networkPass = "12345678";

            WifiConfiguration conf = new WifiConfiguration();
            conf.SSID = "\"" + networkSSID + "\"";   // Please note the quotes. String should contain ssid in quotes
            conf.status = WifiConfiguration.Status.ENABLED;
            conf.priority = 40;

            if (scanResult.capabilities.toUpperCase().contains("WEP")) {
                Log.v("rht", "Configuring WEP");
                conf.allowedKeyManagement.set(WifiConfiguration.KeyMgmt.NONE);
                conf.allowedProtocols.set(WifiConfiguration.Protocol.RSN);
                conf.allowedProtocols.set(WifiConfiguration.Protocol.WPA);
                conf.allowedAuthAlgorithms.set(WifiConfiguration.AuthAlgorithm.OPEN);
                conf.allowedAuthAlgorithms.set(WifiConfiguration.AuthAlgorithm.SHARED);
                conf.allowedPairwiseCiphers.set(WifiConfiguration.PairwiseCipher.CCMP);
                conf.allowedPairwiseCiphers.set(WifiConfiguration.PairwiseCipher.TKIP);
                conf.allowedGroupCiphers.set(WifiConfiguration.GroupCipher.WEP40);
                conf.allowedGroupCiphers.set(WifiConfiguration.GroupCipher.WEP104);

                if (networkPass.matches("^[0-9a-fA-F]+$")) {
                    conf.wepKeys[0] = networkPass;
                } else {
                    conf.wepKeys[0] = "\"".concat(networkPass).concat("\"");
                }

                conf.wepTxKeyIndex = 0;

            } else if (scanResult.capabilities.toUpperCase().contains("WPA")) {
                Log.v("rht", "Configuring WPA");

                conf.allowedProtocols.set(WifiConfiguration.Protocol.RSN);
                conf.allowedProtocols.set(WifiConfiguration.Protocol.WPA);
                conf.allowedKeyManagement.set(WifiConfiguration.KeyMgmt.WPA_PSK);
                conf.allowedPairwiseCiphers.set(WifiConfiguration.PairwiseCipher.CCMP);
                conf.allowedPairwiseCiphers.set(WifiConfiguration.PairwiseCipher.TKIP);
                conf.allowedGroupCiphers.set(WifiConfiguration.GroupCipher.WEP40);
                conf.allowedGroupCiphers.set(WifiConfiguration.GroupCipher.WEP104);
                conf.allowedGroupCiphers.set(WifiConfiguration.GroupCipher.CCMP);
                conf.allowedGroupCiphers.set(WifiConfiguration.GroupCipher.TKIP);

                conf.preSharedKey = "\"" + networkPass + "\"";

            } else {
                Log.v("rht", "Configuring OPEN network");
                conf.allowedKeyManagement.set(WifiConfiguration.KeyMgmt.NONE);
                conf.allowedProtocols.set(WifiConfiguration.Protocol.RSN);
                conf.allowedProtocols.set(WifiConfiguration.Protocol.WPA);
                conf.allowedAuthAlgorithms.clear();
                conf.allowedPairwiseCiphers.set(WifiConfiguration.PairwiseCipher.CCMP);
                conf.allowedPairwiseCiphers.set(WifiConfiguration.PairwiseCipher.TKIP);
                conf.allowedGroupCiphers.set(WifiConfiguration.GroupCipher.WEP40);
                conf.allowedGroupCiphers.set(WifiConfiguration.GroupCipher.WEP104);
                conf.allowedGroupCiphers.set(WifiConfiguration.GroupCipher.CCMP);
                conf.allowedGroupCiphers.set(WifiConfiguration.GroupCipher.TKIP);
            }

            int networkId = mWifiManager.addNetwork(conf);

            Log.v("rht", "Add result " + networkId);

            if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {

                return;
            }
            List<WifiConfiguration> list = mWifiManager.getConfiguredNetworks();
            for (WifiConfiguration i : list) {
                if (i.SSID != null && i.SSID.equals("\"" + networkSSID + "\"")) {
                    Log.v("rht", "WifiConfiguration SSID " + i.SSID);

                    boolean isDisconnected = mWifiManager.disconnect();
                    Log.v("rht", "isDisconnected : " + isDisconnected);

                    boolean isEnabled = mWifiManager.enableNetwork(i.networkId, true);
                    Log.v("rht", "isEnabled : " + isEnabled);

                    boolean isReconnected = mWifiManager.reconnect();
                    Log.v("rht", "isReconnected : " + isReconnected);

                    break;
                }
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public void OnItemClick(int pos, Connection connectionStatus) {
        connectWiFi(mScanResult.get(pos));
    }

    public class WifiDetailsReciver extends BroadcastReceiver {

        @Override
        public void onReceive(Context context, Intent intent) {
            try {
                final String action = intent.getAction();
                if (action.equals(WifiManager.NETWORK_STATE_CHANGED_ACTION)) {
                    NetworkInfo info = intent.getParcelableExtra(WifiManager.EXTRA_NETWORK_INFO);
                    if (info != null) {
                        if (info.isConnected()) {
                            WifiInfo wifiInfo = mWifiManager.getConnectionInfo();
                            mBinding.txtSSID.setText(wifiInfo.getSSID());
                            mBinding.btnDisconnect.setVisibility(View.VISIBLE);
                        } else {
                            mBinding.txtSSID.setText("Not Connected");
                            mBinding.btnDisconnect.setVisibility(View.GONE);
                        }
                    }
                } else {
                    mNetworkList.clear();
                    mScanResult = mWifiManager.getScanResults();
                    for (ScanResult result : mScanResult) {
                        NetworkModel model = new NetworkModel();
                        model.setNetworkName(result.SSID.equals("") ? "Unknown SSID" : result.SSID);
                        model.setNetworkMac(result.BSSID.equals("") ? "Unknown Mac address" : result.BSSID);
                        mNetworkList.add(model);
                    }
                    mAdapter.setData(mNetworkList);
                    if (mNetworkList.size() == 0) {
                        Toast.makeText(MainActivity.this, "No Networks found", Toast.LENGTH_SHORT).show();
                    } else {
                        // Do Something
                    }
                }


            } catch (Exception e) {
                e.printStackTrace();
                Toast.makeText(MainActivity.this, "Error occurred", Toast.LENGTH_SHORT).show();
            }
        }
    }
}