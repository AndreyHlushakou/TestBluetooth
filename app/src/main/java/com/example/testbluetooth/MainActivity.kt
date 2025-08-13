package com.example.testbluetooth

import android.Manifest
import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothManager
import android.bluetooth.le.BluetoothLeScanner
import android.bluetooth.le.ScanCallback
import android.bluetooth.le.ScanFilter
import android.bluetooth.le.ScanResult
import android.bluetooth.le.ScanSettings
import android.content.DialogInterface
import android.content.Intent
import android.content.pm.PackageManager
import android.os.AsyncTask
import android.os.Bundle
import android.util.Log
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.ListView
import android.widget.TextView
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.ActivityResult
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.result.contract.ActivityResultContracts.StartActivityForResult
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.example.testbluetooth.databinding.ActivityMainBinding


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
        doNothing()
    }

    private lateinit var binding: ActivityMainBinding
    private var startScanningButton: Button ?= null
    private var stopScanningButton: Button ?= null
    private var deviceListView: ListView ?= null
    private var textViewTemp: TextView ?= null
    var listAdapter: ArrayAdapter<String> ?= null

    fun bind() {
//        startScanningButton = findViewById(R.id.button1)
//        stopScanningButton = findViewById(R.id.button2)
//        deviceListView = findViewById(R.id.listView)
//        textViewTemp = findViewById(R.id.textView2)
        startScanningButton = binding.button1
        stopScanningButton = binding.button2
        deviceListView = binding.listView
        textViewTemp = binding.textView2

        listAdapter = ArrayAdapter(this, android.R.layout.list_content);
        deviceListView?.setAdapter(listAdapter)
    }

    var device: BluetoothDevice? = null
    var deviceList: ArrayList<BluetoothDevice?>? = null

    fun doNothing() {
        deviceList = ArrayList()
        initializeBluetooth()
        bind()

        startScanningButton?.setOnClickListener {
            startScanning()
        }
        stopScanningButton?.setOnClickListener {
            stopScanning()
        }

        deviceListView?.setOnItemClickListener({ adapterView, view, position, id ->
            stopScanning()
            device = deviceList?.get(position)
            //mBluetoothGatt = device.connectGatt(MainActivity.this, false, gattCallback);
        })
    }

    fun setStrTextView2(str:String) {
        binding.textView2.text = str
    }

    fun startScanning(){
        setStrTextView2("Start Scan!")

        if (!bluetoothAdapter!!.isEnabled()) {
            promptEnableBluetooth()
        }

        // We only need location permission when we start scanning
        if (!hasPermission(Manifest.permission.ACCESS_FINE_LOCATION)) {
            requestLocationPermission()
        } else {
            deviceList!!.clear()
            listAdapter!!.clear()
            stopScanningButton!!.setEnabled(true)
            startScanningButton!!.setEnabled(false)
            textViewTemp!!.setText("Поиск устройства")

            val filters: MutableList<ScanFilter?> = ArrayList<ScanFilter?>() //android but can nordic
            val scanFilterBuilder: ScanFilter.Builder = ScanFilter.Builder()
            filters.add(scanFilterBuilder.build())

            val settingsBuilder: ScanSettings.Builder =  ScanSettings.Builder() //android but can nordic
            settingsBuilder.setLegacy(false)

            AsyncTask.execute(Runnable { if (ActivityCompat.checkSelfPermission(
                    this,
                    Manifest.permission.BLUETOOTH_SCAN
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                // TODO: Consider calling
                //    ActivityCompat#requestPermissions
                // here to request the missing permissions, and then overriding
                //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
                //                                          int[] grantResults)
                // to handle the case where the user grants the permission. See the documentation
                // for ActivityCompat#requestPermissions for more details.
                return@Runnable
            }
                bluetoothLeScanner!!.startScan(leScanCallBack) })
        }
    }

    private fun hasPermission(permissionType: String): Boolean {
        return ContextCompat.checkSelfPermission(
            this,
            permissionType
        ) == PackageManager.PERMISSION_GRANTED
    }

    private fun promptEnableBluetooth() {
        if (!bluetoothAdapter!!.isEnabled()) {
            val enableIntent = Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE)
            activityResultLauncher.launch(enableIntent)
        }
    }

    val RESULT_OK = 1

    val activityResultLauncher: ActivityResultLauncher<Intent> = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode != /*MainActivity.*/RESULT_OK) {
            promptEnableBluetooth()
        }
    }


    val LOCATION_PERMISSION_REQUEST_CODE: Int = 2
    private fun requestLocationPermission() {
        if (hasPermission(Manifest.permission.ACCESS_FINE_LOCATION)) {
            return;
        }
        runOnUiThread {
            val alertDialog = AlertDialog.Builder(this).create()
            alertDialog.setTitle("Location Permission Required")
            alertDialog.setMessage("This app needs location access to detect peripherals.")
            alertDialog.setButton(
                DialogInterface.BUTTON_POSITIVE,
                "OK",
                DialogInterface.OnClickListener { dialog: DialogInterface?, which: Int ->
                    ActivityCompat.requestPermissions(
                        this@MainActivity,
                        arrayOf<String>(Manifest.permission.ACCESS_FINE_LOCATION),
                        LOCATION_PERMISSION_REQUEST_CODE
                    )
                })
            alertDialog.show()
        }
    }

    //android but can nordic
    private val leScanCallBack = object : ScanCallback() {
        @SuppressLint("MissingPermission")
        override fun onScanResult(callbackType: Int, result: ScanResult) {
            result.device?.let {
                synchronized(it) {
                    //listShow(result, true, true)
                }
            }
        }

        override fun onScanFailed(errorCode: Int) {
            super.onScanFailed(errorCode)
            Log.e("TAG", "onScanFailed: code:$errorCode")
        }
    }


    fun stopScanning(){
        setStrTextView2("Stop Scan!")

    }

    var bluetoothLeScanner: BluetoothLeScanner? = null
    var bluetoothAdapter: BluetoothAdapter? = null
    var bluetoothManager: BluetoothManager? = null

    fun initializeBluetooth() {
        bluetoothManager = this@MainActivity.getSystemService(BLUETOOTH_SERVICE) as BluetoothManager
        bluetoothAdapter = bluetoothManager?.adapter
        bluetoothLeScanner = bluetoothAdapter?.getBluetoothLeScanner()
    }

}