package com.example.testbluetooth

import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothDevice.BOND_BONDED
import android.bluetooth.BluetoothDevice.BOND_BONDING
import android.bluetooth.BluetoothDevice.BOND_NONE
import android.bluetooth.BluetoothGatt
import android.bluetooth.BluetoothGattCallback
import android.bluetooth.BluetoothGattCharacteristic
import android.bluetooth.BluetoothGattService
import android.bluetooth.BluetoothManager
import android.bluetooth.le.BluetoothLeScanner
import android.bluetooth.le.ScanCallback
import android.bluetooth.le.ScanResult
import android.content.Intent
import android.location.LocationManager
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import android.util.Log
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.ListView
import android.widget.TextView
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.SwitchCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.lifecycleScope
import com.hjq.permissions.Permission
import com.hjq.permissions.XXPermissions
import com.hjq.toast.Toaster
import kotlinx.coroutines.launch
import no.nordicsemi.android.ble.error.GattError.GATT_INTERNAL_ERROR
import java.util.Locale

class MainActivity : AppCompatActivity() {

    val TAG: String = "MyBleManager"

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

//    private var startScanningButton: Button ?= null
//    private var stopScanningButton: Button ?= null
    private var connectingButton: Button ?= null
    private var disconnectingButton: Button ?= null
    private var scanningSwitch: SwitchCompat ?= null
    private var textView1: TextView ?= null
    private var textView2: TextView ?= null
    private var textView3: TextView ?= null

    private var deviceListView: ListView ?= null
    var listAdapter: ArrayAdapter<String> ?= null
    var deviceList: ArrayList<BluetoothDevice?> ?= null

    private var listServChar: ListView ?= null
    var adapterServChar: ArrayAdapter<String> ?= null
    var listServCharUUID: ArrayList<String?> ?= null

    fun initParam() {
//        startScanningButton = findViewById(R.id.button1)
//        stopScanningButton = findViewById(R.id.button2)
        scanningSwitch = findViewById(R.id.switch1)
        connectingButton = findViewById(R.id.button3)
        disconnectingButton = findViewById(R.id.button4)
        textView1 = findViewById(R.id.textView1)
        textView2 = findViewById(R.id.textView2)
        textView3 = findViewById(R.id.textView3)

        deviceListView = findViewById(R.id.listView1)
        listAdapter = ArrayAdapter(this, android.R.layout.simple_list_item_1)
        deviceListView?.setAdapter(listAdapter)
        deviceList = ArrayList()

        listServChar = findViewById(R.id.listView2)
        adapterServChar = ArrayAdapter(this, android.R.layout.simple_list_item_1)
        listServChar?.setAdapter(adapterServChar)
        listServCharUUID = ArrayList()
    }

    var device: BluetoothDevice? = null
    fun initListener() {

        scanningSwitch?.setOnCheckedChangeListener { _, isChecked ->
            if (!isChecked) {
                stopScanning()
            } else startScanning()
        }

//        startScanningButton?.setOnClickListener {
//            startScanning()
//        }
//        stopScanningButton?.setOnClickListener {
//            stopScanning()
//        }

        deviceListView?.setOnItemClickListener { adapterView, view, position, id ->
            stopScanning()

            device = deviceList!!.get(position)

            scanningSwitch?.setChecked(false)
            textView1?.text = "Устройство: " + device.toString()
            connectingButton!!.setEnabled(true)
        }

        listServChar?.setOnItemClickListener { adapterView, view, position, id ->
            val uuid: String = listServCharUUID!!.get(position)!!
            charactRxData(uuid)
        }

        connectingButton?.setOnClickListener {
            tryConnectToDevice()
        }

        disconnectingButton?.setOnClickListener {
            disconnectDevice()
        }
    }

    fun charactRxData(uuid: String) {
        Log.d(TAG, "UUID : $uuid")
        textView3?.text = uuid
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
                    scanningSwitch?.text = "Поиск устройства"

                    lifecycleScope.launch { getLeScanner().startScan(leScanCallBack) }

//                    с фильтрами:
//                    val filters = mutableListOf<ScanFilter>()
//                    val scanFilterBuilder = ScanFilter.Builder().build()
//                    filters.add(scanFilterBuilder)
//
//                    val settingsBuilder = ScanSettings.Builder()
//                        .setLegacy(false)
//                        .build()
//                    lifecycleScope.launch { getLeScanner().startScan(filters, settingsBuilder, leScanCallBack) }

                } else {
                    toastShow("Required permissions were not granted")
                }
            }

    }

    private fun toastShow(msg: String?) {
        Toaster.show(msg)
    }

    // Проверка, включен ли Bluetooth
    private fun isBluetoothEnabled(): Boolean {
        return bluetoothAdapter?.isEnabled == true
    }

    private val btLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == RESULT_OK) {
            // Bluetooth включен
        } else {
            // Пользователь отказался включать Bluetooth
            toastShow("Bluetooth is required for this feature")
        }
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
            Log.e(TAG, "onScanFailed: code:$errorCode")
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
                    scanningSwitch?.text = "Поиск остановлен"

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
                itemDetails = getItemDetails(res)

                Log.d(TAG, "Index:" + i + "/" + deviceList!!.size + " " + itemDetails)
                listAdapter!!.remove(listAdapter!!.getItem(i))
                listAdapter!!.insert(itemDetails, i)
                return true
            }
            ++i
        }

        itemDetails = getItemDetails(res)

        Log.e(TAG, "NEW:" + i + " " + itemDetails)
        listAdapter!!.add(itemDetails)
        deviceList!!.add(device)
        return false
    }

    @SuppressLint("MissingPermission")
    fun getItemDetails(res: ScanResult) :String{
        var itemDetails = device!!.getAddress() + " " + rssiStrengthPic(res.getRssi()) + "  " + res.getRssi()
        itemDetails += if (res.getDevice().getName() == null) "" else "\n       " + res.getDevice().getName()
        return itemDetails
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

    var bluetoothGatt:BluetoothGatt ?= null
    @SuppressLint("MissingPermission")
    fun tryConnectToDevice() {

        XXPermissions.with(this)
            .permission(
                Permission.BLUETOOTH_CONNECT
            )
            .request { _, allGranted ->
                if (allGranted) {
                    adapterServChar!!.clear()
                    listServCharUUID!!.clear()

                    scanningSwitch?.setChecked(false)
                    scanningSwitch!!.setEnabled(false)
                    textView2?.text = "Попытка подключиться"
                    connectingButton!!.setEnabled(false)
                    disconnectingButton!!.setEnabled(true)


                    bluetoothGatt = device?.connectGatt(this, false, gattCallback, BluetoothDevice.TRANSPORT_LE);
                    device?.createBond()
                }
            }

    }

    val bleHandler : Handler = Handler(Looper.getMainLooper())

    private val gattCallback = object : BluetoothGattCallback() {
        //*********************************************************************************
        @Volatile
        private var isOnCharacteristicReadRunning = false

        @SuppressLint("MissingPermission")
        override fun onConnectionStateChange(gatt: BluetoothGatt, status: Int, newState: Int) {
            super.onConnectionStateChange(gatt, status, newState)
            val address = gatt.device.address

            if (status == BluetoothGatt.GATT_SUCCESS) {
                if (newState == BluetoothGatt.STATE_CONNECTED) {

                    val bondstate = device!!.getBondState()
                    // Обрабатываем bondState
                    if(bondstate == BOND_NONE || bondstate == BOND_BONDED) {

                        Log.w(TAG, "onConnectionStateChangeMy() - Successfully connected to $address")
                        val discoverServicesOk = gatt.discoverServices()
                        Log.i(TAG, "onConnectionStateChange: discovered Services: $discoverServicesOk")

                        lifecycleScope.launch {
                            textView2?.text = "Успешно подключено"
                            disconnectingButton!!.setEnabled(true)
                        }

                    } else if (bondstate == BOND_BONDING) {
                        Log.i(TAG, "waiting for bonding to complete");
                    }

                } else if (newState == BluetoothGatt.STATE_DISCONNECTED) {
                    Log.w(TAG, "onConnectionStateChangeMy() - Successfully disconnected from $address")
                    gatt.close()

                    lifecycleScope.launch {
                        textView2?.text = "Отключено"
                        scanningSwitch!!.setEnabled(true)
                        connectingButton!!.setEnabled(true)
                        disconnectingButton!!.setEnabled(false)
                    }
                }
            } else {
                Log.w(TAG, "onConnectionStateChangeMy: Error $status encountered for $address")
                gatt.close()

                lifecycleScope.launch {
                    textView2?.text = "Ошибка: $status connect"
                    connectingButton!!.setEnabled(true)
                }
            }

            // Проверяем есть ли ошибки? Если да - отключаемся
            if (status == GATT_INTERNAL_ERROR) {
                Log.e(TAG, "Service discovery failed")
//                disconnect()
                return
            }
        }

        @SuppressLint("MissingPermission")
        override fun onServicesDiscovered(gatt: BluetoothGatt, status: Int) {

            super.onServicesDiscovered(gatt, status)
            val services: List<BluetoothGattService> = gatt.services
            runOnUiThread {
                for (i in services.indices) {
                    val service = services[i]
                    val characteristics: List<BluetoothGattCharacteristic> = service.characteristics
                    val log = StringBuilder("\nService Id: \nUUID: ${service.uuid}")

                    adapterServChar?.add("     Service UUID : ${service.uuid}")
                    listServCharUUID!!.add(service.uuid.toString())

                    for (j in characteristics.indices) {
                        val characteristic = characteristics[j]
                        val characteristicUuid = characteristic.uuid.toString()

                        log.append("\n   Characteristic: ")
                        log.append("\n   UUID: $characteristicUuid")

                        adapterServChar?.add("Charact UUID : $characteristicUuid")
                        listServCharUUID!!.add(service.uuid.toString())
                    }
                    Log.d(TAG, "\nonServicesDiscovered: New Service: $log")
                }
            }
        }

    }

    @SuppressLint("MissingPermission")
    fun disconnectDevice() {

        XXPermissions.with(this)
            .permission(
                Permission.BLUETOOTH_CONNECT
            )
            .request { _, allGranted ->
                if (allGranted) {
                    bluetoothGatt!!.disconnect()
                }
            }

    }

}