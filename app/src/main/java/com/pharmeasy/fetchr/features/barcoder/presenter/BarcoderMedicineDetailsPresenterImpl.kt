package com.pharmeasy.fetchr.features.barcoder.presenter

import android.annotation.SuppressLint
import android.content.Context
import android.util.Log
import com.pharmeasy.fetchr.features.barcoder.presenter.contracts.BarcoderMedicineDetailsPresenter
import com.pharmeasy.fetchr.features.barcoder.view.BarcoderMedicineDetailsView
import com.pharmeasy.fetchr.greendao.TaskRepoNew
import com.pharmeasy.fetchr.greendao.model.TaskItem
import com.pharmeasy.fetchr.greendao.model.UserTask
import com.pharmeasy.fetchr.model.BarcoderTask
import com.pharmeasy.fetchr.retro.retroWithToken
import com.pharmeasy.fetchr.service.BarcoderService
import com.pharmeasy.fetchr.type.ItemStatus
import com.pharmeasy.fetchr.type.TaskStatus
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.schedulers.Schedulers
import java.util.concurrent.TimeUnit
import java.util.regex.Pattern

class BarcoderMedicineDetailsPresenterImpl(context: Context, val mainTaskId: Long): BarcoderMedicineDetailsPresenter<BarcoderMedicineDetailsView>() {

    private lateinit var task: UserTask
    private lateinit var items: MutableList<TaskItem>

    private var compositeDisposable: CompositeDisposable = CompositeDisposable()

    private val service by lazy {
        retroWithToken(BarcoderService::class.java)
    }

    private val repo by lazy {
        TaskRepoNew(context = context)
    }

    override fun tasksOnViewResumed() {
        repo.refresh()

        task = repo.getTaskById(mainTaskId).first()
        items = repo.getItemsByTask(mainTaskId)
    }

    override fun tasksOnViewDestroyed() {
        compositeDisposable.clear()
    }

    override fun validateNearExpiry(): Boolean {
        return task.isNearExpiry
    }

    override fun batchValidation(batchNo: String): Boolean {

        return Pattern.matches("[A-Z0-9]{1,256}", batchNo)

    }

    override fun validateWrongBarcode(barcode: String, mrp: String): Boolean {

        if (!barcode.equals(batchValidationCompare()) || mrp.toInt() != items.first().mrp.toInt()){
            getView().bottomSheetWrongMedicine()
            return false
        }

        return true
    }

    override fun batchValidationCompare(): String {
        val re = Regex("[^0-9A-Z]")
        val batchNo = re.replace(items.first().batchNumber, "")
        return batchNo
    }


    @SuppressLint("CheckResult")
    override fun markStatus() {
        getView().showProgressBar()
        compositeDisposable.add(service.markStatus(task.taskId, TaskStatus.IN_PROGRESS.name)
                .debounce(500, TimeUnit.MILLISECONDS)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(
                        { updateTaskStatus()},
                        { error ->  getView().showError(error)}

                )
        )
    }

    @SuppressLint("CheckResult")
    override fun markNearExpiry() {
        getView().showProgressBar()

        val task = BarcoderTask(id = task.taskId, status = TaskStatus.COMPLETED, items = setReturnReason("NEAR EXPIRY"))
        compositeDisposable.add(service.addAllIssues(task)
                .debounce(500, TimeUnit.MILLISECONDS)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(
                        { result ->  openIssueActivity()},
                        { error ->  getView().showError(error)}

                )
        )
    }

    private fun setReturnReason(returnReason: String): List<TaskItem>{
        items.forEach {
            it.returnReason = returnReason
            it.status = ItemStatus.CREATED.name
            repo.updateItemByTaskId(returnReason, mainTaskId)
        }
        Log.d("returnReason", "$items")

        repo.refresh()
        return items
    }

    private fun openIssueActivity(){
        repo.updateTaskStatus(mainTaskId, TaskStatus.COMPLETED)

        getView().launchBarcodeAdminActivity()
    }

    private fun updateTaskStatus(){
        repo.updateTaskStatus(mainTaskId, TaskStatus.IN_PROGRESS)

        getView().launchPasteBarcodeActivity()
    }
}

