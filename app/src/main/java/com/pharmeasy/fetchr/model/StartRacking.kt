package com.pharmeasy.fetchr.model

import com.pharmeasy.fetchr.type.TaskStatus
import org.greenrobot.greendao.annotation.Entity
import org.greenrobot.greendao.annotation.Id
import org.greenrobot.greendao.annotation.Index

@Entity
data class StartRacking (
    @Index(unique = true)
    @Id(autoincrement = true)
    val id: Long? = null,
    var taskId: Long?,
    var trayId: String? = null,
    val status: TaskStatus? = TaskStatus.CREATED,
    var bin: String? = null,
    val ucode: String? = null,
    var referenceId: String? = null,
    var referenceType: String? = null,
    var rackerId: String? = null,
    var ucodeScanRequired: Boolean? =null
) {
    override fun toString(): String {
        return "StartRacking(id=$id, taskId=$taskId, trayId=$trayId, status=$status, bin=$bin, ucode=$ucode, referenceId=$referenceId, referenceType=$referenceType, rackerId=$rackerId, ucodeScanRequired=$ucodeScanRequired)"
    }
}

