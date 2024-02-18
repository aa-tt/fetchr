package com.pharmeasy.fetchr.model

import com.pharmeasy.fetchr.greendao.model.BarcoderItem
import com.pharmeasy.fetchr.greendao.model.TaskItem
import com.pharmeasy.fetchr.greendao.model.VerifierItem
import com.pharmeasy.fetchr.type.Source
import com.pharmeasy.fetchr.type.TaskStatus

data class BarcoderTask(
        val id: Long? = null,
        val status: TaskStatus? = TaskStatus.CREATED,
        val referenceType: String? = null,
        val referenceId: String? = null,
        val trayId: String? = null,
        val verifierTaskId: Long? = null,
        val items: List<TaskItem> = mutableListOf(),
        val issueItems: List<BarcoderItem> = mutableListOf()
) {
    override fun toString(): String {
        return "Task(id=$id, status=$status, items=$items)"
    }

}
