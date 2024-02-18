package com.pharmeasy.fetchr.adapter

import android.content.Context
import android.support.v4.content.ContextCompat
import android.support.v7.widget.RecyclerView
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.CheckBox
import android.widget.TextView
import android.widget.Toast
import com.pharmeasy.fetchr.R
import com.pharmeasy.fetchr.event.TrayItemSelectedListener
import com.pharmeasy.fetchr.model.Status
import com.pharmeasy.fetchr.model.TrayStatus
import com.pharmeasy.fetchr.model.assigned_status
import com.pharmeasy.fetchr.model.ready_for_putaway_status
import com.pharmeasy.fetchr.type.TaskStatus
import kotlinx.android.synthetic.main.rack_scan_multiple_trays_item.view.*

class PutawayMultipleTrayStepAdapter(private val context: Context, private val tray: List<String> , val trayItemSelectedListener: TrayItemSelectedListener): RecyclerView.Adapter<PutawayMultipleTrayStepAdapter.ViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {

        val view = LayoutInflater.from(context).inflate(R.layout.rack_scan_multiple_trays_item, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {

        val lot = tray[position]
        holder.trayName.text = lot

        holder.status.setOnCheckedChangeListener{ _,isChecked ->
            Log.d("checkbox", "$isChecked")
            holder.status.isChecked = isChecked
            if(isChecked) {
                trayItemSelectedListener.onTrayItemSelected(lot,  assigned_status)
            }else {
                trayItemSelectedListener.onTrayItemSelected(lot, ready_for_putaway_status)
            }
        }
    }

    override fun getItemCount(): Int {
        return tray.size
    }

    class ViewHolder(v: View) : RecyclerView.ViewHolder(v) {
        val view: View = v
        val trayName: TextView = v.tray_name
        val status: CheckBox = v.checkbox

    }
}
