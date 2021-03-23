package com.example.sensortag

import android.bluetooth.*
import android.os.Bundle
import android.util.Log
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

/*private val UUID_Lux_Service=UUID.fromString("f000aa70-0451-4000-b000-000000000000")
private val UUID_Lux_Config=UUID.fromString("f000aa72-0451-4000-b000-000000000000")
private val UUID_Lux_Data=UUID.fromString("f000aa71-0451-4000-b000-000000000000")*/

class DeviceActivity : AppCompatActivity() {

    private lateinit var binding: ActivityDeviceBinding;
    private lateinit var btDevice: BluetoothDevice;
    private var bleGatt : BluetoothGatt? = null;
    private var bleService : BluetoothGattService? = null;



    fun connect(bleDevice: BluetoothDevice){
        bleDevice.connectGatt(this, false, mGattCallback)
    }

    fun dissconnect(){

    }

    fun char(){
        bleGatt?.services?.forEach { service ->
            val characteristicsTable = service.characteristics.joinToString(
                separator = "\n|--",
                prefix = "|--"
            ) { char ->
                var description = "${char.uuid}"
                if (char.descriptors.isNotEmpty()) {
                    description += "\n" + char.descriptors.joinToString(
                        separator = "\n|------",
                        prefix = "|------"
                    ) { descriptor ->
                        "${descriptor.uuid}"
                    }
                }
                description
            }
            Log.i("service","Service ${service.uuid}\nCharacteristics:\n$characteristicsTable")
        }
    }

    fun writeConfig(service_UUID:UUID, config_UUID: UUID){
        bleService = bleGatt?.getService(service_UUID)
        val characteristic = bleService?.getCharacteristic(config_UUID)
        val setValueSuccess = characteristic?.setValue(byteArrayOf(0x01))
        Log.i("CONF_LOCAL", setValueSuccess.toString())
        characteristic?.writeType=BluetoothGattCharacteristic.WRITE_TYPE_DEFAULT
        Thread.sleep(1000)
        val writeSuccess = bleGatt?.writeCharacteristic(characteristic)//writesucces wordt false
        Log.i("CONF_WRITE", writeSuccess.toString())
    }

    fun enableNotification(service_UUID:UUID, char_UUID: UUID){
        bleService = bleGatt?.getService(service_UUID)
        val characteristic = bleService?.getCharacteristic(char_UUID)
        bleGatt?.setCharacteristicNotification(characteristic, true)
        Log.d("debug","hier raak ik $characteristic ${char_UUID.toString()}")

        val descriptor = characteristic?.getDescriptor(UUID_CLIENT_CHARACTERISTIC_CONFIG)
        descriptor?.setValue(BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE)
        bleGatt?.writeDescriptor(descriptor)
    }

    fun setConnectionState(device: BluetoothDevice, state: Int){
        Log.d("connect", "${device?.name}")
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
        Log.d("debug", "${device?.name}")
        btDevice=device!!
        connect(btDevice)
    }

    private val mGattCallback = object : BluetoothGattCallback() {

        override fun onConnectionStateChange(gatt: BluetoothGatt?, status: Int, newState: Int) {
            super.onConnectionStateChange(gatt, status, newState)
            if (newState == BluetoothProfile.STATE_CONNECTED) {
                Log.i("DeviceActivity", "Connected to GATT server.");
                Log.i("CONF_LOCAL", "connected")
                bleGatt=gatt!!;
                gatt.discoverServices()
                Log.i("services", "${btDevice.name}")
                runOnUiThread(Runnable {
                    Toast.makeText(this@DeviceActivity, "Connected", Toast.LENGTH_SHORT).show()
                    setConnectionState(btDevice,newState)
                })
            }
        }

        override fun onServicesDiscovered(gatt: BluetoothGatt?, status: Int) {
            super.onServicesDiscovered(gatt, status)
            if (status == BluetoothGatt.GATT_SUCCESS) {
                Log.i("DeviceActivity", "Services discovered succesfully")
                //char()
                enableNotification(UUID_Button_Service,UUID_Button)// notificaties voor button werken

                writeConfig(UUID_TempService, UUID_TempConfig)
                enableNotification(UUID_TempService, UUID_TempData)

                /*writeConfig(UUID_Lux_Service, UUID_Lux_Config)
                enableNotification(UUID_Lux_Service, UUID_Lux_Data)*/
            }
        }

        override fun onCharacteristicChanged(
            gatt: BluetoothGatt?,
            characteristic: BluetoothGattCharacteristic?
        ) {
            super.onCharacteristicChanged(gatt, characteristic)
            Log.i("DeviceActivity", "${characteristic?.uuid.toString()}")
            if(characteristic?.uuid== UUID_TempData) {
                Log.i("DeviceActivity", "characteristic changed ${characteristic?.value?.size}")
                val temp=characteristic?.getIntValue(BluetoothGattCharacteristic.FORMAT_UINT16, 0)
                Log.i("DeviceActivity", "temp is $temp")
                //binding.barometerTextView.text=(temp!!/100.0).toString()
                runOnUiThread(Runnable {
                    binding.tvTemp.text=characteristic?.value?.get(0).toString()
                })
            }else if(characteristic?.uuid== UUID_Button){
                runOnUiThread(Runnable {
                    binding.tvButton.text=characteristic?.value?.get(0).toString()
                })
            }/*else if(characteristic?.uuid== UUID_Lux_Data){
                Log.i("DeviceActivity", "characteristic changed ${characteristic?.value?.size}")
                /*runOnUiThread(Runnable {
                    binding.tvButton.text=characteristic?.value?.get(0).toString()
                })*/
            }*/
        }
    }
}