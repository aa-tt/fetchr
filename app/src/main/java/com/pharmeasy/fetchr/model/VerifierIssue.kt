package com.pharmeasy.fetchr.model

import com.pharmeasy.fetchr.greendao.model.TaskItem

data class VerifierIssue(

    val id: Long,
    val trayId: String,
    var verifierTaskId: Long? = null,
    val items: List<TaskItem> = mutableListOf()
) {
    override fun toString(): String {
        return "VerifierIssue(id=$id, trayId='$trayId', verifierTaskId=$verifierTaskId, items=$items)"
    }
}
