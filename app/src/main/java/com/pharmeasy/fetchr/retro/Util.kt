package com.pharmeasy.fetchr.retro

import android.content.Context
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.util.Log
import com.google.gson.GsonBuilder
import com.pharmeasy.fetchr.BuildConfig
import com.pharmeasy.fetchr.app.FetchrApplication
import com.pharmeasy.fetchr.greendao.model.DaoSession
import com.pharmeasy.fetchr.greendao.model.Event
import com.pharmeasy.fetchr.model.Error
import com.pharmeasy.fetchr.service.SessionService
import com.pharmeasy.fetchr.type.*
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.HttpException
import retrofit2.Retrofit
import retrofit2.adapter.rxjava2.RxJava2CallAdapterFactory
import retrofit2.converter.gson.GsonConverterFactory
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.TimeUnit

var success_time = 1
var failure_time = 2

private var daoSession: DaoSession? = null


val iso_format = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.US)

private fun retrofit(interceptor: AuthenticationInterceptor? = null): Retrofit {

    val loggingInterceptor = HttpLoggingInterceptor()
    loggingInterceptor.level = HttpLoggingInterceptor.Level.BODY

    val builder = OkHttpClient.Builder()

        .readTimeout(20, TimeUnit.SECONDS)
        .connectTimeout(20, TimeUnit.SECONDS)
        .addNetworkInterceptor(NetworkHeadersInterceptor())
        //.addInterceptor(GzipRequestInterceptor())
        .addInterceptor(GZipResponseInterceptor())
        .addInterceptor(loggingInterceptor)

    if (interceptor != null)
        builder.addInterceptor(interceptor)

    return Retrofit.Builder().baseUrl(BuildConfig.API_URL)
            .client(builder.build())
            .addConverterFactory(GsonConverterFactory.create())
            .addCallAdapterFactory(RxJava2CallAdapterFactory.create())
            .build()
}

fun <T> retro(service: Class<T>): T {
    return retrofit().create(service)
}

fun <T> retroWithToken(service: Class<T>): T {

    val retrofit =
            if (SessionService.token.isNotEmpty()) {
                val interceptor = AuthenticationInterceptor(SessionService.token)
                retrofit(interceptor)
            } else
                retrofit()

    return retrofit.create(service)
}

val gson by lazy {
    GsonBuilder().create()
}

fun httpErrorMessage(error: Throwable): String? {

    val res = (error as HttpException).response()
    val code = res.code()
    if (code == 400) {
        val response = res.errorBody()

        val body = response?.string()
        if (body != null) {
            Log.e("U", body)

            val e = gson.fromJson(body, Error::class.java)
            return e?.message
        } else
            return "Unexpected error. Please try again"
    } else if (code == 401) {
        return code.toString()
    }

    return "Server error $code. Please try again"
}

fun httpErrorCode(error: Throwable): Int? {

    val res = (error as HttpException).response()
    val code = res.code()
    if (code == 400) {
        return code
    }
    return code
}

fun greenDao(context: Context): DaoSession {

    daoSession = (context.applicationContext as FetchrApplication).daoSession()
    return daoSession!!
}

/*fun db(context: Context): DaoMaster {

    val helper= DaoMaster.DevOpenHelper(context,"fetchr-db"); //The fetchr-db here is the name of our database.
    val db = helper.getWritableDb()

    val daoMaster = DaoMaster(db)
    return daoMaster
}*/



fun toBreakStatus(status: String?): BreakStatus {

    return when (status) {
        BreakStatus.BREAK.value -> BreakStatus.BREAK
        else -> BreakStatus.AVAILABLE
    }
}

fun vibrate(context: Context) {

    val time = 2000L
    val v = context.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
    // Vibrate for 500 milliseconds
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
        v.vibrate(VibrationEffect.createOneShot(time, VibrationEffect.DEFAULT_AMPLITUDE))
    } else
        v.vibrate(time)
}

fun eventOf(action: UserAction, meta: String, jsonData: String? = null): com.pharmeasy.fetchr.greendao.model.Event {

    val now = iso_format.format(Date())
    //return com.pharmeasy.fetchr.greendao.greenModel.Event(action = action.indicator, meta = meta, createdOn = now, category = SessionService.role, user = SessionService.userId, referenceId = SessionService.referenceId, jsonData = jsonData)
    return Event.Builder(action.indicator)
            .withMeta(meta)
            .withCategory(SessionService.role)
            .withCreatedOn(now)
            .withReferenceId(SessionService.referenceId)
            .withJsonData(jsonData)
            .build()
}

fun processingStatusOf(status: String): Int {
    val itemStatus = ItemStatus.valueOf(status)
    val verificationStatus = when (itemStatus) {
        ItemStatus.LIVE, ItemStatus.READY_FOR_PUTAWAY, ItemStatus.IN_ISSUE -> ProcessingStatus.SYNC
        else -> ProcessingStatus.CREATED
    }

    return verificationStatus.value
}

fun isAlphaNumeric(s: String): Boolean {
    val pattern = "^[a-zA-Z0-9]*$"
    return s.matches(pattern.toRegex())
}

fun translateStatus(status: String): ItemStatus {

    return when (status) {
        ItemStatus.CREATED.name, ItemStatus.ASSIGNED.name -> ItemStatus.PENDING
        ItemStatus.IN_TRAY.name -> ItemStatus.DONE
        ItemStatus.LIVE.name, ItemStatus.IN_ISSUE.name, ItemStatus.PICKED.name -> ItemStatus.IN_PROGRESS
        else -> ItemStatus.valueOf(status)
    }
}
