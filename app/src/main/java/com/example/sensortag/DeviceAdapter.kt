package com.example.btsensortag

import android.bluetooth.BluetoothDevice
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.sensortag.R

class DeviceAdapter(private  val devices: MutableList<Device>, private val clickListener: (Device)->Unit) : RecyclerView.Adapter<DeviceAdapter.deviceViewHolder>() {
    class deviceViewHolder(private val itemView: View) : RecyclerView.ViewHolder(itemView){
        val deviceNameTxt: TextView = itemView.findViewById(R.id.deviceName)
        val deviceAdressTxt: TextView = itemView.findViewById(R.id.deviceAdress)

        fun bind(device: Device, clickListener: (Device)->Unit) {
            deviceNameTxt.text = if(device.btdevice.name==null) "Unnamed" else device.btdevice.name
            deviceAdressTxt.text= device.btdevice.address
            itemView.setOnClickListener { clickListener(device)}
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): deviceViewHolder {
        return deviceViewHolder(LayoutInflater.from(parent.context).inflate(R.layout.rvrow,parent,false))
    }

    override fun onBindViewHolder(holder: deviceViewHolder, position: Int) {
        var curDevice = devices[position]
        holder.bind(curDevice, clickListener)
    }

    override fun getItemCount(): Int {
        return devices.size;
    }
}