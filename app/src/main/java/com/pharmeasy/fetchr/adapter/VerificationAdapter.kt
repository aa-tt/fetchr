package com.pharmeasy.fetchr.adapter

import android.content.Context
import android.support.v4.content.ContextCompat
import android.support.v7.widget.RecyclerView
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import android.widget.Toast
import com.pharmeasy.fetchr.R
import com.pharmeasy.fetchr.event.ItemSelectedListener
import com.pharmeasy.fetchr.model.ProductLotItem
import com.pharmeasy.fetchr.type.ItemStatus
import kotlinx.android.synthetic.main.verification_list_item.view.*

class VerificationAdapter(private val context: Context, private val itemLotItems: List<ProductLotItem>, val itemSelectedListener: ItemSelectedListener) : RecyclerView.Adapter<VerificationAdapter.ViewHolder>() {
    private val TAG = "VerificationAdapter"

    private val green by lazy {
        ContextCompat.getColor(context, R.color.green)
    }

    private val orange by lazy {
        ContextCompat.getColor(context, R.color.orange)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VerificationAdapter.ViewHolder {

        val view = LayoutInflater.from(context).inflate(R.layout.verification_list_item, parent, false)
        return VerificationAdapter.ViewHolder(view)
    }

    override fun onBindViewHolder(holder: VerificationAdapter.ViewHolder, position: Int) {
        val lot = itemLotItems[position]

        holder.view.tag = lot

        holder.name.text = "${lot.name} ${lot.packForm}"
        holder.ucode.text = lot.ucode
        holder.batch.text = lot.batchNumber
        holder.status.text = lot.status.name

        with(holder.status) {
            when (lot.status) {
                ItemStatus.DONE -> setTextColor(green)
                else -> setTextColor(orange)
            }
        }

        holder.view.setOnClickListener {
            val item = holder.view.tag as ProductLotItem

            when (item.status) {
                ItemStatus.PENDING, ItemStatus.IN_PROGRESS -> {
                    itemSelectedListener.onItemSelected(item)
                }
                ItemStatus.DONE -> Toast.makeText(context, context.getString(R.string.already_processed), Toast.LENGTH_SHORT).show()
                else -> Toast.makeText(context, "Item is in ${item.status}", Toast.LENGTH_SHORT).show()
            }
        }
    }


    override fun getItemCount(): Int {
        return itemLotItems.size
    }

    class ViewHolder(v: View) : RecyclerView.ViewHolder(v) {
        val view: View = v
        val name: TextView = v.name
        val status: TextView = v.status
        val ucode: TextView = v.ucode
        val batch: TextView = v.batch
    }
}
