package com.pharmeasy.fetchr.service

import com.pharmeasy.fetchr.model.*
import io.reactivex.Observable
import retrofit2.http.*

interface BarcoderService {

    @PUT("inward/verifierTasks/barcoder/initiate/itemBarcode/{itemBarcode}/trayId/{trayId}")
    fun barcoderTaskStarted(@Path("itemBarcode") itemBarcode: String,@Path("trayId") trayId: String): Observable<Task>

    @GET("inward/verifierTasks/assigned?")
    fun getAssignedTask(@Query("taskType") taskType: String): Observable<AssignedTask>

    @PUT("inward/verifierTasks/{id}/status/{status}")
    fun markStatus(@Path("id") id: Long, @Path("status") status: String): Observable<AssignedTray>

    @PUT("inward/verifierIssues/barcoder/add-all")
    fun addAllIssues(@Body task: BarcoderTask): Observable<AssignedTray>

    @PUT("inward/verifierIssues/barcoder/add")
    fun addIssueTray(@Body tray: BarcoderIssueTask): Observable<Task>

    @PUT("inward/verifierTasks/barcoder/{id}/complete")
    fun completeTask(@Path("id") id: Long): Observable<Task>

    /*@GET("inward/verifierTasks/{id}")
    fun getTask(@Path("id") id: Long): Observable<Task>*/

    @GET("inward/verifierTasks/{id}/verifierTaskItems")
    fun nextItem(@Path("id") id: Long,
                 @Query("ucode") ucode: String,
                 @Query("batchNumber") batchNumber: String,
                 @Query("page") page: Int,
                 @Query("size") size: Int): Observable<BarcoderNextItemTask>

    @GET("inward/verifierIssues")
    fun getTask(@Query("referenceId") ucode: String,
                 @Query("status") status: String,
                 @Query("taskView") taskView: String,
                 @Query("page") page: Int,
                 @Query("size") size: Int): Observable<BarcoderItemTask>

}
