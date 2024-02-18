package com.pharmeasy.fetchr.retro

import okhttp3.Interceptor
import okhttp3.Response
import java.io.IOException

class GZipResponseInterceptor : Interceptor {

    @Throws(IOException::class)
    override fun intercept(chain: Interceptor.Chain): Response {

        val original = chain.request()
        val builder = original.newBuilder().header("Content-Encoding", "gzip")

        val request = builder.build()
        return chain.proceed(request)
    }
}
