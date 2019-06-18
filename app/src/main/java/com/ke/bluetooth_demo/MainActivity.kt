package com.ke.bluetooth_demo

import android.Manifest
import android.app.Activity
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.location.LocationManager
import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import android.provider.Settings
import android.support.v7.app.AlertDialog
import com.tbruyelle.rxpermissions2.RxPermissions
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.rxkotlin.addTo
import kotlinx.android.synthetic.main.activity_main.*

class MainActivity : AppCompatActivity() {

    private val compositeDisposable = CompositeDisposable()


    private lateinit var bluetoothAdapter: BluetoothAdapter


    private val bluetoothReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {

            "收到广播 ${intent.action}".log()

        }


    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        bluetoothAdapter = BluetoothAdapter.getDefaultAdapter()


        val intentFilter = IntentFilter()
        intentFilter.addAction(BluetoothAdapter.ACTION_DISCOVERY_FINISHED)
        intentFilter.addAction(BluetoothDevice.ACTION_FOUND)

        registerReceiver(bluetoothReceiver, intentFilter)

        start.setOnClickListener {
            openBluetooth()
        }

    }

    override fun onDestroy() {
        super.onDestroy()
        unregisterReceiver(bluetoothReceiver)

        compositeDisposable.dispose()
    }

    private fun openBluetooth() {
        if (bluetoothAdapter.isEnabled) {
            //蓝牙已经出于打开状态
            checkPermission()
        } else {
            requestBluetoothEnable()

//            enableBluetooth()
        }
    }

    /**
     * 使用enable的方式打开蓝牙
     */
    private fun enableBluetooth() {
        //蓝牙没有打开 需要打开蓝牙
        val openResult = bluetoothAdapter.enable()

        //结果仅仅表示消息是否发送成功 和系统弹窗是否打开蓝牙没有任何关系 定制系统上会弹出一个对话框询问用户是否打开蓝牙 我们无法得知用户选了是是还是否
        "打开蓝牙结果 $openResult".log()
    }

    /**
     * 启动系统蓝牙设置页面去打开蓝牙
     */
    private fun requestBluetoothEnable() {
        val intent = Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE)
        startActivityForResult(intent, REQUEST_CODE_BLUETOOTH_ENABLE)
    }


    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (requestCode == REQUEST_CODE_BLUETOOTH_ENABLE) {
            val enableBluetoothResult = resultCode == Activity.RESULT_OK

            "调用系统页面打开蓝牙结果 $enableBluetoothResult 蓝牙是否已经完全处于开启状态 ${bluetoothAdapter.isEnabled}".log()


            if (enableBluetoothResult) {
                checkPermission()
            } else {
                AlertDialog.Builder(this)
                    .setTitle("提示")
                    .setMessage("连接设备需要打开蓝牙，请允许打开蓝牙")
                    .setPositiveButton("打开") { dialog, _ ->
                        dialog.dismiss()
                        requestBluetoothEnable()

                    }
                    .setNegativeButton("取消") { dialog, _ ->
                        dialog.dismiss()
                    }
                    .show()
            }
        } else if (requestCode == REQUEST_CODE_LOCATION_SETTINGS) {
            checkLocationEnable()
        }
    }


    /**
     * 判断是否有权限
     */
    private fun checkPermission() {
        RxPermissions(this)
            .request(Manifest.permission.ACCESS_COARSE_LOCATION)
            .subscribe { grantPermissionResult ->
                if (grantPermissionResult) {
                    checkLocationEnable()

                } else {
                    AlertDialog.Builder(this)
                        .setTitle("提示")
                        .setMessage("蓝牙搜索需要定位权限，请允许定位权限")
                        .setPositiveButton("授权") { dialog, _ ->
                            dialog.dismiss()
                            checkPermission()

                        }
                        .setNegativeButton("取消") { dialog, _ ->
                            dialog.dismiss()

                        }
                        .show()
                }


            }
            .addTo(compositeDisposable)
    }

    /**
     * 判断是否打开了定位
     */
    private fun checkLocationEnable() {

        if (isLocationEnable()) {
            startDiscovery()
        } else {
            AlertDialog.Builder(this)
                .setTitle("提示")
                .setMessage("搜索蓝牙设备需要打开定位，请打开设备定位")
                .setPositiveButton("去打开") { dialog, _ ->
                    dialog.dismiss()
                    startLocationSettingsActivity()
                }
                .setNegativeButton("取消") { dialog, _ ->
                    dialog.dismiss()

                }.show()
        }
    }


    /**
     * 跳转到系统定位设置页面
     */
    private fun startLocationSettingsActivity() {
        val intent = Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS)
        startActivityForResult(intent, REQUEST_CODE_LOCATION_SETTINGS)

    }

    /**
     * 开始搜索蓝牙设备
     */
    private fun startDiscovery() {
        "开始扫描附近的蓝牙设备".log()

        if (bluetoothAdapter.isDiscovering) {
            bluetoothAdapter.cancelDiscovery()
        }

        bluetoothAdapter.startDiscovery()

    }


    /**
     * 判断定位是否打开
     */
    private fun isLocationEnable(): Boolean {
        val locationManager = getSystemService(Context.LOCATION_SERVICE) as LocationManager

        if (locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER) || locationManager.isProviderEnabled(
                LocationManager.NETWORK_PROVIDER
            )
        ) {
            return true
        }

        return false

    }

    companion object {
        const val REQUEST_CODE_BLUETOOTH_ENABLE = 1001
        const val REQUEST_CODE_LOCATION_SETTINGS = 1002
    }
}
