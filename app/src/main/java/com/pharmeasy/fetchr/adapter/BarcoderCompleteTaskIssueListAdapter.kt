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
import com.pharmeasy.fetchr.greendao.model.BarcoderItem
import com.pharmeasy.fetchr.greendao.model.TaskItem
import com.pharmeasy.fetchr.model.BarcoderTask
import com.pharmeasy.fetchr.model.BarcoderTasks
import com.pharmeasy.fetchr.model.ProductLotItem
import com.pharmeasy.fetchr.type.ItemStatus
import kotlinx.android.synthetic.main.paste_barcode_item.view.*
import kotlinx.android.synthetic.main.verification_list_item.view.*

class BarcoderCompleteTaskIssueListAdapter(private val context: Context, private val itemLotItems: List<BarcoderTasks>) : RecyclerView.Adapter<BarcoderCompleteTaskIssueListAdapter.ViewHolder>() {

    private val TAG = "BarcoderCompleteTaskIssueListAdapter"

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {

            val lot = itemLotItems[position]

            holder.view.tag = lot

            holder.tv_issue_detail.text = "${lot.reason}"
            holder.tv_issue_quantity.text = "${lot.quantity}"

        }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {

        val view = LayoutInflater.from(context).inflate(R.layout.paste_barcode_item, parent, false)
        return ViewHolder(view)
    }


    override fun getItemCount(): Int {
        return itemLotItems.size
    }

    class ViewHolder(v: View) : RecyclerView.ViewHolder(v) {
        val view: View = v
        val tv_issue_detail: TextView = v.tv_issue_detail
        val tv_issue_quantity: TextView = v.tv_issue_quantity

    }
}
