package com.pharmeasy.fetchr.greendao

import android.content.Context
import com.pharmeasy.fetchr.greendao.model.*
import com.pharmeasy.fetchr.model.Status
import com.pharmeasy.fetchr.model.Task
import com.pharmeasy.fetchr.retro.*
import com.pharmeasy.fetchr.type.ItemStatus
import com.pharmeasy.fetchr.type.ProcessingStatus
import com.pharmeasy.fetchr.type.TaskStatus
import com.pharmeasy.fetchr.type.TaskType
import java.math.RoundingMode
import java.text.DecimalFormat


class TaskRepoNew(val context: Context) {

    private val tasks_for_user = "WHERE uid = ? AND status != 'COMPLETED' "

    private val tasks_by_id = "WHERE _id = ? AND status != 'COMPLETED' "

    private val update_tray_for_task = "UPDATE ${UserTaskDao.TABLENAME} SET trayId = ? WHERE _id = ? "

    private val update_status_for_task = "UPDATE ${UserTaskDao.TABLENAME} SET status = ? WHERE _id = ? "

    private val update_dnd_for_task = "UPDATE ${UserTaskDao.TABLENAME} SET dnd = ? WHERE _id = ? "

    private val items_for_task = "WHERE taskId = ? "

    private val items_for_task_bin = "WHERE taskId = ? AND binId = ? AND processed <= ? "

    private val items_for_task_between_status = "WHERE taskId = ? AND processed >= ? AND processed <= ? "

    private val add_items_for_task_between_status = "WHERE taskId = ? AND processed >= ? AND processed <= ?"

    private val add_barcoder_items_for_task_between_status = "WHERE taskId = ? AND processed >= ? AND processed <= ? AND status = ?"

    //For racker
    private val items_for_task_between_status_racker = "WHERE taskId = ? AND processed >= ? AND processed <= ? "

    private val items_for_task_product = "WHERE taskId = ? AND ucode = ? AND processed <= ?"

    private val items_for_task_lot = "WHERE taskId = ? AND ucode = ? AND batchNumber = ? AND processed <= ?"

    private val add_item_to_tray = "UPDATE ${TaskItemDao.TABLENAME} SET returnReason = null, trayId = ?, status = ?, processed = ? WHERE taskId = ? AND barCode = ? "

    private val add_item_to_ucode = "UPDATE ${TaskItemDao.TABLENAME} SET returnReason = null, trayId = ?, status = ?, processed = ? WHERE taskId = ? AND ucode = ? AND batchNumber = ?"

    private val add_item_to_tray_veri = "UPDATE ${VerifierItemDao.TABLENAME} SET returnReason = null, trayId = ?, status = ?, processed = ? WHERE taskId = ? AND barCode = ? "

    private val add_item_to_tray_barcoder = "UPDATE ${BarcoderItemDao.TABLENAME} SET returnReason = null, trayId = ?, status = ?, processed = ? WHERE taskId = ? AND barCode = ? "

    private val add_issue_to_tray = "UPDATE ${TaskItemDao.TABLENAME} SET status = ?, returnReason = ?, processed = ? WHERE taskId = ? AND barCode = ? "

    private val add_issue_to_tray_veri = "UPDATE ${VerifierItemDao.TABLENAME} SET status = ?, returnReason = ?, processed = ? WHERE taskId = ? AND barCode = ? "

    private val add_barcoder_issue_to_tray_veri = "UPDATE ${BarcoderItemDao.TABLENAME} SET status = ?, returnReason = ?, processed = ? WHERE taskId = ? AND barCode = ? "

    private val update_item = "UPDATE ${TaskItemDao.TABLENAME} SET status = ?, returnReason = ?, processed = ? WHERE taskId = ? AND barCode = ? "

    private val update_item_verification_status_for_status = "UPDATE ${TaskItemDao.TABLENAME} SET processed = ? WHERE taskId = ? AND status = ? "

    //Update verify new tray
    private val update_item_verification_status_for_status_verify = "UPDATE ${VerifierItemDao.TABLENAME} SET processed = ? WHERE taskId = ? AND status = ? "

    private val update_item_verification_status_for_status_processed = "UPDATE ${TaskItemDao.TABLENAME} SET processed = ? WHERE taskId = ? AND status = ? AND processed = ? "

    private val update_veri_tray_for_status = "UPDATE ${VerifierItemDao.TABLENAME} SET trayId = ? WHERE taskId = ? AND status = ? "

    private val pending_events = "WHERE processed = 0 "

    private val mark_events = "UPDATE ${EventDao.TABLENAME} SET processed = 1 WHERE processed = 0 "

    private val update_item_by_ucode = "UPDATE ${TaskItemDao.TABLENAME} SET status = ? WHERE taskId = ? AND ucode = ? "

    private val update_item_by_taskId = "UPDATE ${TaskItemDao.TABLENAME} SET returnReason = ? WHERE taskId = ?"

    private val delete_event = "DELETE FROM ${EventDao.TABLENAME} WHERE processed = ? "

    private val delete_user_task = "DELETE FROM ${UserTaskDao.TABLENAME} WHERE uid = ? "

    //barcoder

    private val add_item_to_tray_task_item = "UPDATE ${TaskItemDao.TABLENAME} SET returnReason = null, trayId = ?, status = ?, processed = ? WHERE taskId = ? AND barCode = ? "

    private val add_issue_to_tray_task_item = "UPDATE ${TaskItemDao.TABLENAME} SET status = ?, returnReason = ?, processed = ? WHERE taskId = ? AND barCode = ? "

    private val update_item_task_status_for_status_verify = "UPDATE ${TaskItemDao.TABLENAME} SET processed = ? WHERE taskId = ? AND status = ? "

    private val update_barcoder_items_task_status_for_status_verify = "UPDATE ${BarcoderItemDao.TABLENAME} SET processed = ? WHERE taskId = ? AND status = ? "

    private val update_barcoder_items = "UPDATE ${BarcoderItemDao.TABLENAME} SET processed = ?, status = ? WHERE taskId = ? AND barCode = ?"

    private val update_task_item_tray_for_status = "UPDATE ${TaskItemDao.TABLENAME} SET trayId = ? WHERE taskId = ? AND status = ? "

    private val db by lazy {
        greenDao(context)
    }

    fun getTasksForUser(user: String): MutableList<UserTask> {

        return db.userTaskDao.queryRaw(tasks_for_user, user).toMutableList()
    }

    fun getTaskById(id: Long): MutableList<UserTask> {

        return db.userTaskDao.queryRaw(tasks_by_id, id.toString()).toMutableList()
    }

    fun getTrayList(): MutableList<UserTask> {

        return db.userTaskDao.loadAll()
    }

    fun addTask(user: String, type: TaskType, task: Task, trayId: String? = null, reference: String? = null, issueTrayId: String? = null): Long {

        /*val userTask = UserTask(uid = user, type = type.name, taskId = task.id!!, ucode = task.ucode, name = task.name, binId = task.binId,
                status = task.status!!.name, trayId = trayId, reference = reference, referenceType = task.referenceType,
                issueTrayId = issueTrayId, source = task.source, trayPickedZone = task.trayPickedZone)*/

        val userTask = UserTask.Builder(user)
                .withType(type.name)
                .withTaskId(task.id!!)
                .withUcode(task.ucode)
                .withName(task.name)
                .withBinId(task.binId)
                .withStatus(task.status!!.name)
                .withTrayId(trayId)
                .withReference(reference)
                .withReferenceType(task.referenceType)
                .withIssueTrayId(issueTrayId)
                .withSource(task.source)
                .withTrayPickedZone(task.trayPickedZone)
                .withNearExpiry(task.nearExpiry!!)
                .build()

        val taskId = db.userTaskDao.insert(userTask)
        if (taskId < 0)
            return -1

        task.items.forEach { item ->
            item.taskId = taskId
            item.mrp = roundOffDecimal(item.mrp)
            if(item.status != null) {
                item.processed = processingStatusOf(item.status!!)
            }else{
                item.status = ItemStatus.CREATED.name
            }

            addTaskItem(item)
        }

        db.clear()
        return taskId
    }

    fun addUserList(userTask: UserTask){
        db.userTaskDao.insert(userTask)
    }

    fun updateTask(id: Long, task: Task) {

        updateTaskStatus(id, task.status!!)
        updateTaskTray(id, task.trayId)

        val items = task.items.filter {
            it.status == ItemStatus.LIVE.name || it.status == ItemStatus.READY_FOR_PUTAWAY.name || it.status == ItemStatus.IN_ISSUE.name || it.status == ItemStatus.IN_TRAY.name
        }

        items.forEach { item ->
            if (item.barCode != null)
                updateItem(id, item.barCode, item.returnReason, ItemStatus.valueOf(item.status!!), ProcessingStatus.SYNC)
            else
                updateItemByUCode(id, item.ucode, ItemStatus.valueOf(item.status!!))
        }
    }

    fun updateDND(id: Long, status: Boolean) {
        db.userTaskDao.database.execSQL(update_dnd_for_task, arrayOf(if (status) 1 else 0, id))
    }

    private fun processingStatusOf(status: String): Int {
        val itemStatus = ItemStatus.valueOf(status)
        val verificationStatus = when (itemStatus) {
            ItemStatus.LIVE, ItemStatus.READY_FOR_PUTAWAY, ItemStatus.IN_ISSUE, ItemStatus.COMPLETED, ItemStatus.COMPLETED_WITH_ISSUE -> ProcessingStatus.SYNC
            else -> ProcessingStatus.CREATED
        }

        return verificationStatus.value
    }

    fun updateTaskTray(taskId: Long, trayId: String?) {
        db.userTaskDao.database.execSQL(update_tray_for_task, arrayOf(trayId, taskId))

    }

    fun updateTaskStatus(taskId: Long, status: TaskStatus) {
        db.userTaskDao.database.execSQL(update_status_for_task, arrayOf(status.name, taskId))
    }

    fun clearTasksForUser(user: String) {
        db.userTaskDao.database.execSQL(delete_user_task, arrayOf(user))
    }

    fun addTaskItem(item: TaskItem) {
        return db.taskItemDao.insertInTx(item)
    }

    fun addVerifierItem(item: VerifierItem) {
        return db.verifierItemDao.insertOrReplaceInTx(item)
    }

    fun addUserInfo(info: UserInfo){
        return db.userInfoDao.insertOrReplaceInTx(info)
    }

    fun getItemsByTask(taskId: Long): MutableList<TaskItem> {

        return db.taskItemDao.queryRawCreate(items_for_task, taskId).list()
    }

    fun getVerifierItemsByTask(taskId: Long): MutableList<VerifierItem> {

        return db.verifierItemDao.queryRawCreate(items_for_task, taskId).list()
    }

    fun getVerifierItemsByTaskAndUCode(taskId: Long, ucode: String, batch: String, status: ProcessingStatus = ProcessingStatus.SYNC): MutableList<VerifierItem> {

        return db.verifierItemDao.queryRawCreate(items_for_task_lot, taskId, ucode, batch, status.value).list()
    }

    fun getTaskItemsByTask(taskId: Long): MutableList<TaskItem> {

        return db.taskItemDao.queryRawCreate(items_for_task, taskId).list()
    }

    fun getTaskItemsByTaskAndUCode(taskId: Long, ucode: String, batch: String, status: ProcessingStatus = ProcessingStatus.SYNC): MutableList<TaskItem> {

        return db.taskItemDao.queryRawCreate(items_for_task_lot, taskId, ucode, batch, status.value).list()
    }

    fun getItemsByTaskAndBin(taskId: Long, bin: String, status: ProcessingStatus): MutableList<TaskItem> {

        return db.taskItemDao.queryRawCreate(items_for_task_bin, taskId, bin, status.value).list()
    }

    fun getItemsByTaskAndBetweenStatus(taskId: Long, from: ProcessingStatus, to: ProcessingStatus): MutableList<VerifierItem> {

        return db.verifierItemDao.queryRawCreate(items_for_task_between_status, taskId, from.value, to.value).list()
    }

    fun getItemsByTaskAndBetweenStatusTaskItem(taskId: Long, from: ProcessingStatus, to: ProcessingStatus): MutableList<TaskItem> {

        return db.taskItemDao.queryRawCreate(items_for_task_between_status, taskId, from.value, to.value).list()
    }

    // at add verifier tray
    fun getAddItemsByTaskAndBetweenStatus(taskId: Long, from: ProcessingStatus, to: ProcessingStatus): MutableList<VerifierItem> {

        return db.verifierItemDao.queryRawCreate(add_items_for_task_between_status, taskId, from.value, to.value).list()
    }

    fun getAddItemsByTaskAndBetweenStatusTaskItem(taskId: Long, from: ProcessingStatus, to: ProcessingStatus): MutableList<TaskItem> {

        return db.taskItemDao.queryRawCreate(add_items_for_task_between_status, taskId, from.value, to.value).list()
    }

    fun getItemsByTaskUcodeAndBatch(taskId: Long, from: ProcessingStatus, to: ProcessingStatus): MutableList<TaskItem> {

        return db.taskItemDao.queryRawCreate(items_for_task_between_status_racker, taskId, from.value,to.value).list()
    }

    fun getItemsByTaskUcode(taskId: Long, ucode: String, status: ProcessingStatus = ProcessingStatus.SYNC): MutableList<TaskItem> {

        return db.taskItemDao.queryRawCreate(items_for_task_product, taskId, ucode, status.value).list()
    }

    fun markCompletedItem(taskId: Long, barcode: String, trayId: String, status: ItemStatus = ItemStatus.READY_FOR_PUTAWAY) {

        db.taskItemDao.database.execSQL(add_item_to_tray, arrayOf(trayId, status.name, ProcessingStatus.MARKED.value, taskId, barcode))
    }

    fun markCompletedUcode(taskId: Long, ucode: String, batch: String, trayId: String, status: ItemStatus = ItemStatus.READY_FOR_PUTAWAY) {

        db.taskItemDao.database.execSQL(add_item_to_ucode, arrayOf(trayId, status.name, ProcessingStatus.MARKED.value, taskId, ucode, batch))
    }

    fun markVerifierCompletedItem(taskId: Long, barcode: String, trayId: String, status: ItemStatus = ItemStatus.READY_FOR_PUTAWAY) {
        db.verifierItemDao.database.execSQL(add_item_to_tray_veri, arrayOf(trayId, status.name, ProcessingStatus.MARKED.value, taskId, barcode))
    }

    fun markIssueItem(taskId: Long, barcode: String? = null, reason: String? = null, status: ItemStatus = ItemStatus.IN_ISSUE) {
        db.taskItemDao.database.execSQL(add_issue_to_tray, arrayOf(status.name, reason, ProcessingStatus.MARKED.value, taskId, barcode))
    }

    fun markTaskIssueItem(taskId: Long, ucode: String, batch: String, trayId: String, status: ItemStatus = ItemStatus.IN_ISSUE) {
        db.taskItemDao.database.execSQL(add_item_to_ucode, arrayOf(trayId, status.name, ProcessingStatus.MARKED.value, taskId, ucode, batch))
    }

    fun markVeriferIssueItem(taskId: Long, barcode: String, reason: String, status: ItemStatus = ItemStatus.IN_ISSUE) {
        db.verifierItemDao.database.execSQL(add_issue_to_tray_veri, arrayOf(status.name, reason, ProcessingStatus.MARKED.value, taskId, barcode))
    }

    fun markTaskItemIssueItem(taskId: Long, barcode: String, reason: String, status: ItemStatus = ItemStatus.IN_ISSUE) {
        db.taskItemDao.database.execSQL(add_issue_to_tray_task_item, arrayOf(status.name, reason, ProcessingStatus.MARKED.value, taskId, barcode))
    }

    fun updateItem(taskId: Long, barcode: String?, reason: String?, status: ItemStatus = ItemStatus.IN_ISSUE, processed: ProcessingStatus) {
        db.taskItemDao.database.execSQL(update_item, arrayOf(status.name, reason, processed.value, taskId, barcode))

    }

    fun updateTaskProcessingStatusByStatus(taskId: Long, processingStatus: ProcessingStatus, status: ItemStatus) {
        db.taskItemDao.database.execSQL(update_item_verification_status_for_status, arrayOf(processingStatus.value, taskId, status.name))
    }

    //adding to update verifier table after add tray
    fun updateVerifyTaskProcessingStatusByStatus(taskId: Long, processingStatus: ProcessingStatus, status: ItemStatus) {
        db.verifierItemDao.database.execSQL(update_item_verification_status_for_status_verify, arrayOf(processingStatus.value, taskId, status.name))
    }


    fun updateTaskProcessingStatusByStatusProcessed(taskId: Long, processingStatus: ProcessingStatus, status: ItemStatus, sourceStatus: ProcessingStatus) {
        db.taskItemDao.database.execSQL(update_item_verification_status_for_status_processed, arrayOf(processingStatus.value, taskId, status.name, sourceStatus.value))
    }

    fun updateVeriTrayForStatus(taskId: Long, trayId: String, status: ItemStatus) {
        db.verifierItemDao.database.execSQL(update_veri_tray_for_status, arrayOf(trayId, taskId, status.name))
    }

    fun addEvent(event: Event): Long {
        return db.eventDao.insert(event)
    }

    fun getPendingEvents(): MutableList<Event> {
        return db.eventDao.queryRaw(pending_events).toMutableList()
    }

    fun markEvents() {
        db.eventDao.database.execSQL(mark_events)
    }

    fun clearEvents() {
        db.eventDao.database.execSQL(delete_event, arrayOf(1))
    }

    fun updateItemByUCode(taskId: Long, ucode: String, status: ItemStatus) {
        db.taskItemDao.database.execSQL(update_item_by_ucode, arrayOf(status.name, taskId, ucode))
    }

    fun updateItemByTaskId(reason: String, taskId: Long) {
        db.taskItemDao.database.execSQL(update_item_by_taskId, arrayOf(reason, taskId))
    }


    /** BARCODER QUERIES **/

    fun addBarcoderItem(item: BarcoderItem) {
        return db.barcoderItemDao.insertOrReplaceInTx(item)
    }

    fun updateBarcodeTaskProcessingStatusByStatus(taskId: Long, processingStatus: ProcessingStatus, status: ItemStatus) {
        db.barcoderItemDao.database.execSQL(update_barcoder_items_task_status_for_status_verify, arrayOf(processingStatus.value, taskId, status.name))
    }

    fun markBarcoderIssueItem(taskId: Long, barcode: String, reason: String, status: ItemStatus = ItemStatus.IN_ISSUE) {
        db.barcoderItemDao.database.execSQL(add_barcoder_issue_to_tray_veri, arrayOf(status.name, reason, ProcessingStatus.MARKED.value, taskId, barcode))
    }

    fun getBarcoderItemsByTaskAndUCode(taskId: Long, ucode: String, batch: String, status: ProcessingStatus = ProcessingStatus.SYNC): MutableList<BarcoderItem> {

        return db.barcoderItemDao.queryRawCreate(items_for_task_lot, taskId, ucode, batch, status.value).list()
    }

    fun markBarcoderCompletedItem(taskId: Long, barcode: String, trayId: String, status: ItemStatus = ItemStatus.READY_FOR_PUTAWAY) {
        db.barcoderItemDao.database.execSQL(add_item_to_tray_barcoder, arrayOf(trayId, status.name, ProcessingStatus.MARKED.value, taskId, barcode))
    }

    fun getBarcoderItemsByTask(taskId: Long): MutableList<BarcoderItem> {

        return db.barcoderItemDao.queryRawCreate(items_for_task, taskId).list()
    }

    // at add barcoder tray
    fun getAddBarcoderItemsByTaskAndBetweenStatus(taskId: Long, from: ProcessingStatus, to: ProcessingStatus): MutableList<BarcoderItem> {
        return db.barcoderItemDao.queryRawCreate(add_barcoder_items_for_task_between_status, taskId, from.value, to.value, ItemStatus.IN_ISSUE.name).list()
    }

    fun updateBarcodeItems(taskId: Long, processingStatus: ProcessingStatus, status: ItemStatus, barcode: String) {
        db.barcoderItemDao.database.execSQL(update_barcoder_items, arrayOf(processingStatus.value, status.name, taskId , barcode))
    }



    fun refresh(){
        db.clear()
    }

    fun clearItem(){
        db.taskItemDao.deleteAll()
    }

    fun clearAllUserTasks(){
        db.userTaskDao.deleteAll()
    }

    fun clearVerifyItem(){
        db.taskItemDao.deleteAll()
        db.verifierItemDao.deleteAll()
    }

    fun clearBarcoderItem(){
        db.taskItemDao.deleteAll()

    }

    private fun roundOffDecimal(number: Double): Double? {
        val df = DecimalFormat("#.##")
        df.roundingMode = RoundingMode.CEILING
        return df.format(number).toDouble()
    }
}


