package com.pharmeasy.fetchr.service

import com.pharmeasy.fetchr.model.*
import io.reactivex.Observable
import retrofit2.http.*

interface VerifierService {

    @PUT("inward/verifierTasks/{id}/status")
    fun markTaskStarted(@Path("id") id: Long, @Body status: Status): Observable<Task>

    @PUT("inward/verifierTasks/{barcode}/barcodeStatus")
    fun markTaskStartedV2(@Path("barcode") id: String, @Body status: Status): Observable<Task>

    @GET("inward/verifierTasks/assigned")
    fun getAssignedTask(): Observable<AssignedTask>

    @POST("inward/rackerTasks")
    fun addInitialTray(@Body taskTray: TaskTray): Observable<AssignedTray>

    @PUT("inward/verifierTasks/{id}/addTray")
    fun addNewTray(@Path("id") id: Long, @Body task: NewTray): Observable<AssignedTray>

    @PUT("inward/verifierTasks/{id}/sync")
    fun syncItems(@Path("id") id: Long, @Body task: VerifierTask): Observable<AssignedTray>

    @POST("inward/verifierIssues")
    fun addIssueTray(@Body tray: AssignedTray): Observable<VerifierIssue>

    @PUT("inward/verifierTasks/{id}/completed")
    fun completeTask(@Path("id") id: Long, @Body verifierTask: VerifierTask): Observable<Result>

    @GET("inward/verifierTasks/{id}/verifierTaskItems")
    fun nextItem(@Path("id") id: Long,
                 @Query("ucode") ucode: String,
                 @Query("batchNumber") batchNumber: String,
                 @Query("page") page: Int,
                 @Query("size") size: Int): Observable<NextItemTask>
}
