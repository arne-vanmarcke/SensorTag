package com.example.sensortag

import android.bluetooth.*
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.sensortag.databinding.ActivityDeviceBinding
import java.util.*



private val UUID_Button_Service= UUID.fromString("0000ffe0-0000-1000-8000-00805f9b34fb")
private val UUID_Button= UUID.fromString("0000ffe1-0000-1000-8000-00805f9b34fb")

private val UUID_CLIENT_CHARACTERISTIC_CONFIG = UUID.fromString("00002902-0000-1000-8000-00805f9b34fb")

private val UUID_TempData= UUID.fromString("f000aa01-0451-4000-b000-000000000000")
private val UUID_TempConfig= UUID.fromString("f000aa02-0451-4000-b000-000000000000")
private val UUID_TempService= UUID.fromString("f000aa00-0451-4000-b000-000000000000")

class DeviceActivity : AppCompatActivity() {

    private lateinit var binding: ActivityDeviceBinding;
    private lateinit var btDevice: BluetoothDevice;
    private var bleGatt : BluetoothGatt? = null;
    private var bleService : BluetoothGattService? = null;

    fun enableTempSensor(){
        Handler(Looper.getMainLooper()).postDelayed({
            writeConfig(UUID_TempService, UUID_TempConfig)

        },100)
        Handler(Looper.getMainLooper()).postDelayed({
            enableNotification(UUID_TempService, UUID_TempData)
        },200)
    }

    fun  convertTemptoCelsius(value:Int):Double {
        var temp=value shr 2;
        return temp*0.03125;
    }

    fun connect(bleDevice: BluetoothDevice){
        bleDevice.connectGatt(this, false, mGattCallback)
    }

    fun disconnect(){
        bleGatt?.disconnect();
    }

    fun writeConfig(service_UUID:UUID, config_UUID: UUID){
        bleService = bleGatt?.getService(service_UUID)
        val characteristic = bleService?.getCharacteristic(config_UUID)
        val setValueSuccess = characteristic?.setValue(byteArrayOf(0x01))
        val writeSuccess = bleGatt?.writeCharacteristic(characteristic)//writesucces wordt false
    }

    fun enableNotification(service_UUID:UUID, char_UUID: UUID){
        bleService = bleGatt?.getService(service_UUID)
        val characteristic = bleService?.getCharacteristic(char_UUID)
        bleGatt?.setCharacteristicNotification(characteristic, true)
        val descriptor = characteristic?.getDescriptor(UUID_CLIENT_CHARACTERISTIC_CONFIG)
        descriptor?.setValue(BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE)
        bleGatt?.writeDescriptor(descriptor)
    }

    fun setConnectionState(device: BluetoothDevice, state: Int){
        binding.tvName.text = device.name
        binding.tvAdress.text = device.address
        binding.tvState.text = if (state == 2)
            "Connected"
        else
            "Disconnected"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityDeviceBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val device = intent.getParcelableExtra<BluetoothDevice>("device")
        btDevice=device!!
        connect(btDevice)

        binding.dissconnectBtn.setOnClickListener {
            disconnect()
        }
    }

    private val mGattCallback = object : BluetoothGattCallback() {

        override fun onConnectionStateChange(gatt: BluetoothGatt?, status: Int, newState: Int) {
            super.onConnectionStateChange(gatt, status, newState)
            if (newState == BluetoothProfile.STATE_CONNECTED) {
                bleGatt=gatt!!;
                gatt.discoverServices()
                runOnUiThread(Runnable {
                    Toast.makeText(this@DeviceActivity, "Connected", Toast.LENGTH_SHORT).show()
                    setConnectionState(btDevice,newState)
                })
            }else if (newState== BluetoothProfile.STATE_DISCONNECTED){
                runOnUiThread(Runnable {
                    Toast.makeText(this@DeviceActivity, "Disconnected", Toast.LENGTH_SHORT).show()
                })
                finish()
            }
        }

        override fun onServicesDiscovered(gatt: BluetoothGatt?, status: Int) {
            super.onServicesDiscovered(gatt, status)
            if (status == BluetoothGatt.GATT_SUCCESS) {
                enableNotification(UUID_Button_Service,UUID_Button)
                enableTempSensor()
            }
        }

        override fun onCharacteristicChanged(
            gatt: BluetoothGatt?,
            characteristic: BluetoothGattCharacteristic?
        ) {
            super.onCharacteristicChanged(gatt, characteristic)
            if(characteristic?.uuid== UUID_TempData) {
                val temp=characteristic?.getIntValue(BluetoothGattCharacteristic.FORMAT_UINT16, 0)
                val tempInCelsius:Double=convertTemptoCelsius(temp!!)
                runOnUiThread(Runnable {
                    binding.tvTemp.text="$tempInCelsius °C"
                })
            }else if(characteristic?.uuid== UUID_Button){
                runOnUiThread(Runnable {
                    binding.tvButton.text=characteristic?.value?.get(0).toString()
                })
            }
        }
    }
}