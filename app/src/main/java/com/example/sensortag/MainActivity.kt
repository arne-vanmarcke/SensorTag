package com.example.btsensortag

import android.Manifest
import android.bluetooth.*
import android.bluetooth.le.ScanCallback
import android.bluetooth.le.ScanResult
import android.bluetooth.le.ScanSettings
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.*
import android.util.Log
import android.widget.TextView
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.sensortag.DeviceActivity
import com.example.sensortag.databinding.ActivityMainBinding
import com.google.android.material.snackbar.Snackbar
import java.util.*


private const val ENABLE_BLUETOOTH_REQUEST_CODE = 1
private const val LOCATION_PERMISSION_REQUEST_CODE = 2
//private const val MyAddress :String  = "B0:B4:48:BF:B5:03"
var list : MutableList<Device> = arrayListOf()
private lateinit var BTdevice : BluetoothDevice

class MainActivity : AppCompatActivity() {

    private val bluetoothAdapter: BluetoothAdapter by lazy {
        val bluetoothManager = getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
        bluetoothManager.adapter
    }

    public var liste: MutableList<Int> = arrayListOf();

    fun byteArrayOfInts(vararg ints: Int) = ByteArray(ints.size) { pos -> ints[pos].toByte() }
    val arr = byteArrayOfInts(0x01)

    private val bleScanner by lazy {
        bluetoothAdapter.bluetoothLeScanner
    }

    val deviceClicked = { device: Device ->
        //Toast.makeText(this, "Clicked on device Name: ${device.btdevice.name ?: "Unnamed"}, address: ${device.btdevice.address}",Toast.LENGTH_SHORT).show()
        stopBleScan()
        intent = Intent(this,DeviceActivity::class.java)
        intent.putExtra("device",device.btdevice)
        startActivity(intent)
    }

    fun Logger(message: String){
        Log.d("log", message)
    }

    @RequiresApi(Build.VERSION_CODES.LOLLIPOP)
    private val scanSettings = ScanSettings.Builder()
            .setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY)
            .build()
    var adresses: MutableList<String> = arrayListOf()
    private val scanCallback = @RequiresApi(Build.VERSION_CODES.LOLLIPOP)
    object : ScanCallback() {
        @RequiresApi(Build.VERSION_CODES.LOLLIPOP)
        override fun onScanResult(callbackType: Int, result: ScanResult) {
            with(result.device) {
                Log.i(
                        "ScanCallback",
                        "Found BLE device Name: ${name ?: "Unnamed"}, address: $address"
                )
                if (!adresses.contains(address) && result.device!=null) {
                    adresses.add(address)
                    Log.d("adress", "created device with address $address")
                    list.add(Device(result.device))
                    binding.rvdevicelist.adapter = DeviceAdapter(list, deviceClicked)
                    binding.rvdevicelist.layoutManager= LinearLayoutManager(this@MainActivity)
                }
            }
        }
    }

    private fun startBleScan() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && !isLocationPermissionGranted) {
            requestLocationPermission()
        } else {
            bleScanner.startScan(null, scanSettings, scanCallback)
            isScanning = true
        }
    }


    private fun stopBleScan() {
        bleScanner.stopScan(scanCallback)
        isScanning = false
    }

    private var isScanning = false
        set(value) {
            field = value
            runOnUiThread { binding.scanButton.text = if (value) "Stop Scanning" else "Start Scanning" }
        }

    val isLocationPermissionGranted
        get() = hasPermission(Manifest.permission.ACCESS_FINE_LOCATION)

    fun Context.hasPermission(permissionType: String): Boolean {
        return ContextCompat.checkSelfPermission(this, permissionType) ==
                PackageManager.PERMISSION_GRANTED
    }

    private lateinit var binding: ActivityMainBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        val view = binding.root
        setContentView(view)
        binding.scanButton.setOnClickListener {
            //Toast.makeText(this,"Clicked on SCAN button", Toast.LENGTH_LONG).show()
            if (isScanning) {
                stopBleScan()
            } else {
                startBleScan()
            }
        }
    }

    override fun onResume() {
        super.onResume()
        //check if bluetooth is enabled
        if (!bluetoothAdapter.isEnabled) {
            promptEnableBluetooth()
        }
    }

    //request to enable bluetooth
    private fun promptEnableBluetooth() {
        if (!bluetoothAdapter.isEnabled) {
            val enableBtIntent = Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE)
            startActivityForResult(enableBtIntent, ENABLE_BLUETOOTH_REQUEST_CODE)
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if(requestCode== ENABLE_BLUETOOTH_REQUEST_CODE)
        {
            Log.v(
                    "ENABLE BT", when (resultCode) {
                android.app.Activity.RESULT_OK -> "BT enabled"
                else -> "BT not enabled"
            }
            )
        }
    }

    /**
     * Requests the [android.Manifest.permission.ACCESS_FINE_LOCATION] permission.
     * If an additional rationale should be displayed, the user has to launch the request from
     * a SnackBar that includes additional information.
     */
    private fun requestLocationPermission() {
        // Permission has not been granted and must be requested.
        if(ActivityCompat.shouldShowRequestPermissionRationale(
                        this,
                        Manifest.permission.ACCESS_FINE_LOCATION
                )) {
            // Provide an additional rationale to the user if the permission was not granted
            // and the user would benefit from additional context for the use of the permission.
            // Display a SnackBar with a button to request the missing permission.
            Snackbar.make(
                    this,
                    binding.layout,
                    "fine location permission is needed for using BLE", Snackbar.LENGTH_INDEFINITE
            )
                    .show()

            ActivityCompat.requestPermissions(
                    this,
                    arrayOf(Manifest.permission.ACCESS_FINE_LOCATION),
                    LOCATION_PERMISSION_REQUEST_CODE
            )
        } else {
            Snackbar.make(
                    this,
                    binding.layout,
                    "fine location permission not enabled",
                    Snackbar.LENGTH_SHORT
            )
                    .show()

            // Request the permission. The result will be received in onRequestPermissionResult().
            ActivityCompat.requestPermissions(
                    this,
                    arrayOf(Manifest.permission.ACCESS_FINE_LOCATION),
                    LOCATION_PERMISSION_REQUEST_CODE
            )
        }
    }

    override fun onRequestPermissionsResult(
            requestCode: Int,
            permissions: Array<out String>,
            grantResults: IntArray
    ) {
        //super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == LOCATION_PERMISSION_REQUEST_CODE) {
            // Request for camera permission.
            if (grantResults.size == 1 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                // Permission has been granted. Start camera preview Activity.
                Snackbar.make(
                        this,
                        binding.layout,
                        "fine location permission is granted",
                        Snackbar.LENGTH_SHORT
                )
                        .show()
            } else {
                // Permission request was denied.
                Snackbar.make(
                        this,
                        binding.layout,
                        "fine location permission was denied",
                        Snackbar.LENGTH_SHORT
                )
                        .show()
            }
        }
    }
}

