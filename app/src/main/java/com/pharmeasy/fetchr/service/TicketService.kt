package com.pharmeasy.fetchr.service

import android.service.quicksettings.Tile
import com.pharmeasy.fetchr.model.RaiseTicket
import com.pharmeasy.fetchr.model.Result
import io.reactivex.Observable
import okhttp3.MultipartBody
import okhttp3.RequestBody
import okhttp3.ResponseBody
import retrofit2.http.*


interface TicketService {
    @Multipart
    @POST("support/ticket/create")
    fun raiseTicket(@Part screenshot : MultipartBody.Part, @Part("title") title: RequestBody, @Part("description") description: RequestBody, @Part("priority") priority: RequestBody, @Part("cc") cc: RequestBody): Observable<ResponseBody>
}