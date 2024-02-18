package com.pharmeasy.fetchr.event

import com.pharmeasy.fetchr.model.ProductLotItem

interface ItemSelectedListener {

    fun onItemSelected(item: ProductLotItem)

}
