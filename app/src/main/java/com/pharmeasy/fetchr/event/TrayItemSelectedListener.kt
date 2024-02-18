package com.pharmeasy.fetchr.event

import com.pharmeasy.fetchr.model.Status
import com.pharmeasy.fetchr.type.TaskStatus

interface TrayItemSelectedListener {

   fun onTrayItemSelected(trayId: String, status: Status)

}