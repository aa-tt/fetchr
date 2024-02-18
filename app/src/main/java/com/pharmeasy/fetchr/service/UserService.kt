package com.pharmeasy.fetchr.service

import com.pharmeasy.fetchr.model.Credentials
import com.pharmeasy.fetchr.model.Authorization
import com.pharmeasy.fetchr.model.Profile
import com.pharmeasy.fetchr.model.UserStatus
import io.reactivex.Observable
import retrofit2.http.Body
import retrofit2.http.POST

interface UserService {

    @POST("config/batch/mobile/signIn")
    fun login(@Body credentials: Credentials): Observable<Authorization>

    @POST("config/users/mobile/signIn")
    fun signIn(@Body credentials: Credentials): Observable<Profile>

    @POST("config/users/activity")
    fun status(@Body status: UserStatus): Observable<UserStatus>

    @POST("config/batch/mobile/logout")
    fun logout(@Body status: UserStatus): Observable<UserStatus>
}