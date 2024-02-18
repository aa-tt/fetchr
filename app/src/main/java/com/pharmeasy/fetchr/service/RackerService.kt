package com.pharmeasy.fetchr.service

import com.pharmeasy.fetchr.model.StartRacking
import com.pharmeasy.fetchr.greendao.model.UserTask
import com.pharmeasy.fetchr.model.*
import io.reactivex.Observable
import retrofit2.http.*

interface RackerService {

    @GET("inward/rackerTasks/assigned")
    fun getAssignedTask(): Observable<AssignedTask>

    @GET("inward/rackerTasks/v2/zone")
    fun getTrayZone(): Observable<UserTask>

    @GET("inward/rackerTasks/v2/{trayId}/{binId}/getItemsToScan")
    fun getItemToScan(@Path("trayId") trayId: String, @Path("binId") binId: String): Observable<Task>

    @PUT("inward/rackerTasks/v2/trayId/{trayId}/status")
    fun markTrayAsPicked(@Path("trayId") trayId: String, @Body status: Status): Observable<UserTask>

    @PUT("inward/rackerTasks/v2/{id}/items/live")
    fun completeBin(@Path("id") taskId: Long, @Body liveInfo: ProductLotLiveInfo): Observable<StartRacking>

    @PUT("inward/rackerTasks/completed/v2")
    fun completeTask(): Observable<Result>

    @PUT("inward/rackerTasks/v2/trayId/{trayId}/status")
    fun updateBinStatus(@Path("trayId") trayId: String, @Body binStatus: BinLock): Observable<Result>


    @GET("inward/rackerTasks?")
    fun getListOfTrays(@Query("size") size: Int,
                       @Query("status") status: String,
                       @Query("rackerId") rackerId: String
                       ): Observable<RackerTasks>

    @PUT("inward/rackerTasks/v2/rackerId/status")
    fun startRacking(@Body status: Status): Observable<StartRacking>

    @PUT("inward/rackerTasks/v3/binId/{binId}/trayId/{trayId}/completed")
    fun getJitItemToScan(@Path("binId") binId: String, @Path("trayId") trayId: String): Observable<Task>
}
