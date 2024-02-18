package com.pharmeasy.fetchr.model

import com.pharmeasy.fetchr.greendao.model.*

data class RackerTasks(
        var elementsCount: Long? = null,
        var pagesCount: Long? = null,
        var hasPrevious: Boolean? = null,
        var hasNext: Boolean? = null,
        val data: List<UserTask> = mutableListOf()

) {
    override fun toString(): String {
        return "RackerTasks(elementsCount=$elementsCount, pagesCount=$pagesCount, hasPrevious=$hasPrevious, hasNext=$hasNext, data=$data)"
    }
}
