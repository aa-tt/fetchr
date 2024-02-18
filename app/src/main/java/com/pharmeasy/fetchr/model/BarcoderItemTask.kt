package com.pharmeasy.fetchr.model


data class BarcoderItemTask(

    val elementsCount: Long,
    var pagesCount: Int,
    val hasPrevious: Boolean,
    var hasNext: Boolean,
    val data: List<Task> = mutableListOf()

) {
    override fun toString(): String {
        return "BarcoderItemTask(elementsCount=$elementsCount, pagesCount=$pagesCount, hasPrevious=$hasPrevious, hasNext=$hasNext, data=$data)"
    }
}
