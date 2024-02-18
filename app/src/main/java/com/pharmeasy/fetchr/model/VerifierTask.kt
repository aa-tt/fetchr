package com.pharmeasy.fetchr.model

import com.pharmeasy.fetchr.greendao.model.VerifierItem
import com.pharmeasy.fetchr.type.Source
import com.pharmeasy.fetchr.type.TaskStatus

data class VerifierTask(
        val id: Long? = null,
        var trayId: String? = null,
        val status: TaskStatus? = TaskStatus.CREATED,
        var sourceId: String? = null,
        val sourceType: Source? = null,
        var referenceId: String? = null,
        var referenceType: String? = null,
        var ucode: String? = null,
        var binId: String? = null,
        var tags: List<String>? = null,
        var source: String? = null,
        val items: List<VerifierItem> = mutableListOf()
) {
    override fun toString(): String {
        return "Task(id=$id, trayId=$trayId, status=$status, items=$items)"
    }

}
