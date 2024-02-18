package com.pharmeasy.fetchr.event


interface OnItemCheckListener {

        fun onItemCheck(position: Int)


        fun onItemUncheck(position: Int)
}