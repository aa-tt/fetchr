package com.pharmeasy.fetchr.service

import com.pharmeasy.fetchr.greendao.model.Event
import io.reactivex.Observable
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.POST

interface EventService {

    @POST("config/event")
    fun events(@Body events: List<Event>): Observable<Response<Void>>
}
