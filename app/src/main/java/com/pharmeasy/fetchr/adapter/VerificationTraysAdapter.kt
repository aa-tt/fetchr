package com.pharmeasy.fetchr.adapter

import android.content.Context
import android.support.v7.widget.CardView
import android.support.v7.widget.RecyclerView
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import com.pharmeasy.fetchr.R
import com.pharmeasy.fetchr.model.TrayItem
import kotlinx.android.synthetic.main.verification_tray_item.view.*

class VerificationTraysAdapter(private val context: Context, private val trayItems: List<TrayItem>) : RecyclerView.Adapter<VerificationTraysAdapter.ViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VerificationTraysAdapter.ViewHolder {

        val view = LayoutInflater.from(context).inflate(R.layout.verification_tray_item, parent, false)
        return VerificationTraysAdapter.ViewHolder(view)
    }

    override fun onBindViewHolder(holder: VerificationTraysAdapter.ViewHolder, position: Int) {

        val item = trayItems[position]

        if(item.count == 0){
            holder.header.visibility = View.VISIBLE
            holder.content.visibility = View.GONE
        }else{
            holder.header.visibility = View.GONE
            holder.content.visibility = View.VISIBLE
        }

        holder.header.text = item.trayId
        holder.name.text = "${item.name} ${item.packForm}"
        holder.ucode.text = item.ucode
        holder.batch.text = item.batch
        holder.count.text = item.count.toString()
    }

    override fun getItemCount(): Int {
        return trayItems.size
    }

    class ViewHolder(v: View) : RecyclerView.ViewHolder(v) {
        val name: TextView = v.name
        val ucode: TextView = v.ucode
        val batch: TextView = v.batch
        val count: TextView = v.count
        val header: TextView = v.header
        val content: CardView = v.content
    }
}
