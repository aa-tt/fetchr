package com.pharmeasy.fetchr.adapter

import android.content.Context
import android.content.Intent
import android.support.v4.content.ContextCompat
import android.support.v7.widget.RecyclerView
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import android.widget.Toast
import com.pharmeasy.fetchr.R
import com.pharmeasy.fetchr.features.racker.PutawayMultipleTrayStep
import com.pharmeasy.fetchr.model.BinItem
import com.pharmeasy.fetchr.type.ItemStatus
import kotlinx.android.synthetic.main.putaway_list_item.view.*
import java.io.Serializable

class PutawayAdapter(private val context: Context, private val bins: List<BinItem>, val taskId: Long) : RecyclerView.Adapter<PutawayAdapter.ViewHolder>() {

    private val green by lazy {
        ContextCompat.getColor(context, R.color.green)
    }

    private val orange by lazy {
        ContextCompat.getColor(context, R.color.orange)
    }

    private val gray by lazy {
        ContextCompat.getColor(context, R.color.gray_line)
    }

    private val white by lazy {
        ContextCompat.getColor(context, R.color.white)
    }

    private val black by lazy {
        ContextCompat.getColor(context, R.color.text_primary)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PutawayAdapter.ViewHolder {

        val view = LayoutInflater.from(context).inflate(R.layout.putaway_list_item, parent, false)
        return PutawayAdapter.ViewHolder(view)
    }

    override fun onBindViewHolder(holder: PutawayAdapter.ViewHolder, position: Int) {

        val lot = bins[position]

        holder.view.tag = lot

        holder.name.text = "BIN# ${lot.binId}"
        holder.status.text = lot.status.name

        with(holder.status) {
            when (lot.status) {
                ItemStatus.DONE -> setTextColor(white)
                else -> setTextColor(orange)
            }
        }

        with(holder.name) {
            when (lot.status) {
                ItemStatus.DONE  -> setTextColor(white)
                else -> setTextColor(black)
            }
        }

        with(holder.view) {
            when (lot.status) {
                ItemStatus.IN_PROGRESS -> setBackgroundColor(white)
                ItemStatus.DONE -> setBackgroundColor(green)
                else -> setBackgroundColor(gray)
            }
        }

        holder.view.setOnClickListener {
            val item = holder.view.tag as BinItem

            when (item.status) {

                ItemStatus.PENDING,ItemStatus.IN_PROGRESS -> {
                    val intent = Intent(context, PutawayMultipleTrayStep::class.java)
                    intent.putExtra("data", item as Serializable)
                    intent.putExtra("taskId", taskId)
                    intent.putExtra("count", bins.size)
                    context.startActivity(intent)
                }
                ItemStatus.DONE -> Toast.makeText(context, context.getString(R.string.already_processed), Toast.LENGTH_SHORT).show()
                else -> Toast.makeText(context, "Item is in ${item.status}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    override fun getItemCount(): Int {
        return bins.size
    }

    class ViewHolder(v: View) : RecyclerView.ViewHolder(v) {
        val view: View = v
        val name: TextView = v.name
        val status: TextView = v.status
    }
}
