package com.pharmeasy.fetchr.model

import com.pharmeasy.fetchr.greendao.model.VerifierItem

data class NextItemTask(

    val elementsCount: Long,
    var pagesCount: Int,
    val hasPrevious: Boolean,
    var hasNext: Boolean,
    val data: List<VerifierItem> = mutableListOf()

) {
    override fun toString(): String {
        return "NextItemTask(pageCount=$pagesCount, hasPrevious=$hasPrevious, hasNext=$hasNext, data=$data)"
    }
}
