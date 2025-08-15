package com.example.testbluetooth

import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothManager
import android.bluetooth.le.BluetoothLeScanner
import android.bluetooth.le.ScanCallback
import android.bluetooth.le.ScanResult
import android.content.Intent
import android.location.LocationManager
import android.os.Bundle
import android.provider.Settings
import android.util.Log
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.ListView
import android.widget.TextView
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.lifecycleScope
import com.hjq.permissions.Permission
import com.hjq.permissions.XXPermissions
import com.hjq.toast.Toaster
import kotlinx.coroutines.launch


class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_main)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        initBluetooth()
        initParam()
        initListener()

    }

    var bluetoothManager: BluetoothManager? = null
    var bluetoothAdapter: BluetoothAdapter? = null

    fun initBluetooth() {
        bluetoothManager = getSystemService(BLUETOOTH_SERVICE) as BluetoothManager
        bluetoothAdapter = bluetoothManager?.adapter
    }

    fun getLeScanner() :BluetoothLeScanner {
        return bluetoothAdapter!!.getBluetoothLeScanner()
    }

    private var startScanningButton: Button ?= null
    private var stopScanningButton: Button ?= null
    private var deviceListView: ListView ?= null
    private var textView1: TextView ?= null
    private var textView2: TextView ?= null
    var listAdapter: ArrayAdapter<String> ?= null
    var deviceList: ArrayList<BluetoothDevice?>? = null


    fun initParam() {
        startScanningButton = findViewById(R.id.button1)
        stopScanningButton = findViewById(R.id.button2)
        deviceListView = findViewById(R.id.listView)
        textView1 = findViewById(R.id.textView1)
        textView2 = findViewById(R.id.textView2)

        stopScanningButton!!.setEnabled(false)

        listAdapter = ArrayAdapter(this, android.R.layout.simple_list_item_1);
        deviceListView?.setAdapter(listAdapter)

        deviceList = ArrayList()
    }

    var device: BluetoothDevice? = null
    fun initListener() {
        startScanningButton?.setOnClickListener {
            startScanning()
        }
        stopScanningButton?.setOnClickListener {
            stopScanning()
        }

        deviceListView?.setOnItemClickListener({ adapterView, view, position, id ->
            stopScanning()
            device = deviceList?.get(position)

            setStrTextView(device.toString(), 2)

            //mBluetoothGatt = device.connectGatt(MainActivity.this, false, gattCallback);
        })
    }

    fun setStrTextView(str:String, type:Int) {
        if (type == 1) textView1?.text = str
        else textView2?.text = str
    }

    @SuppressLint("MissingPermission")
    fun startScanning(){

        XXPermissions.with(this)
            .permission(
                Permission.BLUETOOTH_SCAN,
                Permission.BLUETOOTH_CONNECT, // android 15
                Permission.ACCESS_COARSE_LOCATION,
                Permission.ACCESS_FINE_LOCATION
            )
            .request { _, allGranted ->
                if (allGranted) {
                    if (!isBluetoothEnabled()) {
                        requestEnableBluetooth()
                        toastShow("Please enable Bluetooth to continue")
                        return@request
                    }

                    if (!isLocationEnabled()) {
                        requestEnableLocation()
                        toastShow("Please enable Location to continue")
                        return@request
                    }


                    deviceList!!.clear()
                    listAdapter!!.clear()
                    stopScanningButton!!.setEnabled(true)
                    startScanningButton!!.setEnabled(false)
                    setStrTextView("Поиск устройства", 1)

//                    val filters: MutableList<ScanFilter?> = ArrayList<ScanFilter?>() //android but can nordic
//                    val scanFilterBuilder: ScanFilter.Builder = ScanFilter.Builder()
//                    filters.add(scanFilterBuilder.build())
//
//                    val settingsBuilder: ScanSettings.Builder =  ScanSettings.Builder() //android but can nordic
//                    settingsBuilder.setLegacy(false)

                    lifecycleScope.launch { getLeScanner().startScan(leScanCallBack) }

                } else {
                    toastShow("Required permissions were not granted")
                }
            }

    }

    private fun toastShow(msg: String?) {
        Toaster.show(msg)
    }

    private val btLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == RESULT_OK) {
            // Bluetooth включен
        } else {
            // Пользователь отказался включать Bluetooth
            toastShow("Bluetooth is required for this feature")
        }
    }

    // Проверка, включен ли Bluetooth
    private fun isBluetoothEnabled(): Boolean {
        return bluetoothAdapter?.isEnabled == true
    }

    // Запрос на включение Bluetooth
    private fun requestEnableBluetooth() {
        if (!isBluetoothEnabled()) {
            val enableBtIntent = Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE)
            btLauncher.launch(enableBtIntent)
        }
    }

    // Проверка, включена ли геолокация
    private fun isLocationEnabled(): Boolean {
        val locationManager = getSystemService(LOCATION_SERVICE) as LocationManager
        return locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER) ||
                locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER)
    }

    // Запрос на включение геолокации
    private fun requestEnableLocation() {
        if (!isLocationEnabled()) {
            val intent = Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS)
            startActivity(intent)
        }
    }

    private val leScanCallBack = object : ScanCallback() { //android but can nordic
        @SuppressLint("MissingPermission")
        override fun onScanResult(callbackType: Int, result: ScanResult) {
            result.device?.let {
                synchronized(it) {
                    listShow(result)
                }
            }
        }

        override fun onScanFailed(errorCode: Int) {
            super.onScanFailed(errorCode)
            Log.e("TAG", "onScanFailed: code:$errorCode")
        }
    }

    @SuppressLint("MissingPermission")
    fun stopScanning(){

        XXPermissions.with(this)
            .permission(
                Permission.BLUETOOTH_SCAN
            )
            .request { _, allGranted ->
                if (allGranted) {
                    stopScanningButton!!.setEnabled(false)
                    startScanningButton!!.setEnabled(true)
                    setStrTextView("Поиск остановлен", 2)

                    lifecycleScope.launch { getLeScanner().stopScan(leScanCallBack) }
                }
            }



    }

    @SuppressLint("MissingPermission")
    private fun listShow(res: ScanResult): Boolean {
        device = res.device
        var itemDetails: String?
        var i = 0

        while (i < deviceList!!.size) {
            val addedDeviceDetail = deviceList!!.get(i)!!.getAddress()
            if (addedDeviceDetail == device!!.getAddress()) {
                itemDetails =
                    device!!.getAddress() + " " + rssiStrengthPic(res.getRssi()) + "  " + res.getRssi()
                itemDetails += if (res.getDevice()
                        .getName() == null
                ) "" else "\n       " + res.getDevice().getName()

                Log.d("TAG", "Index:" + i + "/" + deviceList!!.size + " " + itemDetails)
                listAdapter!!.remove(listAdapter!!.getItem(i))
                listAdapter!!.insert(itemDetails, i)
                return true
            }
            ++i
        }

        itemDetails =
            device!!.getAddress() + " " + rssiStrengthPic(res.getRssi()) + "  " + res.getRssi()
        itemDetails += if (res.getDevice().getName() == null) "" else "\n       " + res.getDevice()
            .getName()

        Log.e("TAG", "NEW:" + i + " " + itemDetails)
        listAdapter!!.add(itemDetails)
        deviceList!!.add(device)
        return false
    }

    private fun rssiStrengthPic(rs: Int): String {
        if (rs > -45) {
            return "▁▃▅▇"
        }
        if (rs > -62) {
            return "▁▃▅ "
        }
        if (rs > -80) {
            return "▁▃  "
        }
        if (rs > -95) {
            return "▁   "
        } else return "    "
    }

}