package com.pharmeasy.fetchr.service

import android.widget.ImageView
import com.pharmeasy.fetchr.type.BreakStatus

object SessionService {

    var token: String = ""
    var userId: String = ""
    var name: String = ""
    var username: String = ""
    var role: String = ""
    var referenceId = ""

    var breakStatus: BreakStatus = BreakStatus.AVAILABLE
    var breakStart: Long = System.currentTimeMillis()

    var scannerEnabled = true
    var deviceId = ""
    var jitPickingEnabled: Boolean? = false

    var title: String= ""
    var description: String= ""
    var priority: Int= 0
    var cc: String= ""
    var screenshot: Int = 0

}
