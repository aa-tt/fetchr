package com.pharmeasy.fetchr.model

import com.pharmeasy.fetchr.greendao.model.*
import com.pharmeasy.fetchr.type.Source
import com.pharmeasy.fetchr.type.TaskStatus

data class Task(

        val id: Long? = null,
        var trayId: String? = null,
        val status: TaskStatus? = TaskStatus.CREATED,
        var sourceId: String? = null,
        val sourceType: Source? = null,
        var referenceId: String? = null,
        var referenceType: String? = null,
        var ucode: String? = null,
        var binId: String? = null,
        var source: String? = null,
        val items: List<TaskItem> = mutableListOf(),
        val name: String? = null,
        val trayPickedZone: String? = null,
        val taskType: String? = null,
        val nearExpiry: Boolean? = false

) {
    override fun toString(): String {
        return "Task(id=$id, trayId=$trayId, status=$status, sourceId=$sourceId, sourceType=$sourceType, referenceId=$referenceId, referenceType=$referenceType, ucode=$ucode, binId=$binId, source=$source, items=$items, name=$name, trayPickedZone=$trayPickedZone, taskType=$taskType)"
    }

}
