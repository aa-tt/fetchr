package com.pharmeasy.fetchr.model

import com.pharmeasy.fetchr.type.TaskStatus

data class Status(val status: String)

val in_progress_status = Status(TaskStatus.IN_PROGRESS.name)
val picked_status = Status(TaskStatus.PICKED.name)
val returned_status = Status(TaskStatus.RETURNED.name)
val complete_status = Status(TaskStatus.COMPLETED.name)
val sidelined_status = Status(TaskStatus.SIDELINED.name)
val assigned_status = Status(TaskStatus.ASSIGNED.name)
val ready_for_putaway_status = Status(TaskStatus.READY_FOR_PUTAWAY.name)
