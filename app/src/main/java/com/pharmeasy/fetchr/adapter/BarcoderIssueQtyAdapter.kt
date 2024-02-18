package com.pharmeasy.fetchr.adapter

import android.content.Context
import android.support.v7.widget.RecyclerView
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.RadioButton
import android.widget.TextView
import com.pharmeasy.fetchr.R
import com.pharmeasy.fetchr.fragments.BottomSheetSpecifyIssueQtyFragment
import kotlinx.android.synthetic.main.issue_type_item.view.*

class BarcoderIssueQtyAdapter (private val onItemCheckListener: BottomSheetSpecifyIssueQtyFragment, private val context: Context, private val items: List<String>, private val isBin: Boolean) : RecyclerView.Adapter<BarcoderRaiseIssueAdapter.ViewHolder>() {

    private val TAG = BarcoderRaiseIssueAdapter::class.java.simpleName

    private var selected = -1


    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): BarcoderRaiseIssueAdapter.ViewHolder {

        val view = LayoutInflater.from(context).inflate(R.layout.issue_type_item, parent, false)
        return BarcoderRaiseIssueAdapter.ViewHolder(view)
    }

    override fun onBindViewHolder(holder: BarcoderRaiseIssueAdapter.ViewHolder, position: Int) {
        val lot = items[position]

        holder.view.tag = lot

        holder.reasons.text = lot

        holder.reasonCheckBox.isChecked = selected == position

        holder.reasonCheckBox.setOnCheckedChangeListener { _, isChecked ->
            holder.reasonCheckBox.isChecked = isChecked

            if (isChecked) {
                selected = position
                onItemCheckListener.onItemCheck(position)
            } else {
                onItemCheckListener.onItemUncheck(position)
            }
        }
    }

    override fun getItemCount(): Int {
        return items.size
    }

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val view: View = view
        val reasons: TextView = view.issueTypeText
        val reasonCheckBox: RadioButton = view.radio_issue

    }
}


