package com.pharmeasy.fetchr.adapter

import android.content.Context
import android.support.v7.app.AlertDialog
import android.support.v7.widget.RecyclerView
import android.support.v7.widget.SwitchCompat
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import com.pharmeasy.fetchr.R
import com.pharmeasy.fetchr.model.Device
import com.pharmeasy.fetchr.scanner.ScannerService
import io.reactivex.Observable
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.subjects.PublishSubject
import kotlinx.android.synthetic.main.devices_item.view.*
import java.util.concurrent.TimeUnit

class DevicesAdapter(private val context: Context, private val items: List<Device>, private val enabled: Boolean) : RecyclerView.Adapter<DevicesAdapter.ViewHolder>() {

    private val subject = PublishSubject.create<String>()

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): DevicesAdapter.ViewHolder {

        val view = LayoutInflater.from(context).inflate(R.layout.devices_item, parent, false)
        return DevicesAdapter.ViewHolder(view)
    }

    override fun onBindViewHolder(holder: DevicesAdapter.ViewHolder, position: Int) {

        val device = items[position]

        holder.view.tag = device
        holder.name.text = device.name
        holder.connect.isChecked = device.connected
        holder.connect.isEnabled = enabled

        holder.connect.setOnCheckedChangeListener { _, checked ->

            val connected = items.firstOrNull { it.connected }
            val item = holder.view.tag as Device

            if (checked) {
                if (connected != null && connected.address != item.address) {
                    val builder = AlertDialog.Builder(context)
                        .setTitle("Switch Scanner")
                        .setMessage("Are you sure to switch from ${connected.name} to ${item.name}?")
                        .setPositiveButton(R.string.continu) { _, _ -> onSwitch(item.address) }
                            .setNegativeButton(R.string.cancel) { _, _ -> holder.connect.isChecked = false }

                    builder.create().show()
                } else
                    ScannerService.connect(item.address)
            } else if (item.connected) {

                val builder = AlertDialog.Builder(context)
                    .setTitle("Confirmation")
                    .setMessage("Are you sure to disconnect ${item.name}?")
                    .setPositiveButton(R.string.disconnect, { _, _ -> onDisconnect() })
                    .setNegativeButton(R.string.cancel, { _, _ -> holder.connect.isChecked = true })

                builder.create().show()
            }
        }
    }

    override fun getItemCount(): Int {
        return items.size
    }

    private fun onSwitch(address: String) {
        ScannerService.disconnect()

        Observable.just("switch").delay(5, TimeUnit.SECONDS)
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe {
                ScannerService.connect(address)
            }
    }

    private fun onDisconnect() {
        ScannerService.disconnect()
    }

    class ViewHolder(v: View) : RecyclerView.ViewHolder(v) {
        val view: View = v
        val name: TextView = v.name
        val connect: SwitchCompat = v.connect
    }
}
