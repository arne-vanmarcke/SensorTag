package com.example.btsensortag

import android.bluetooth.BluetoothDevice
import android.os.Parcelable
//import kotlinx.parcelize.Parcelize
//import kotlinx.android.parcelize.Parcelize


/*data class Device (val name:String, val address:String){
}*/

data class Device (val btdevice: BluetoothDevice){
}