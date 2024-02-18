package com.pharmeasy.fetchr.retro

import com.pharmeasy.fetchr.BuildConfig
import com.pharmeasy.fetchr.service.SessionService
import okhttp3.Interceptor
import okhttp3.Response
import java.io.IOException

class AuthenticationInterceptor(private val authToken: String) : Interceptor {

    @Throws(IOException::class)
    override fun intercept(chain: Interceptor.Chain): Response {

        val original = chain.request()
        val builder = original.newBuilder()
                                    .header("Authorization", authToken)
                                    .header("deviceId", SessionService.deviceId)
                                    .header("version", BuildConfig.VERSION_NAME)

        val request = builder.build()

        return chain.proceed(request)
    }
}
