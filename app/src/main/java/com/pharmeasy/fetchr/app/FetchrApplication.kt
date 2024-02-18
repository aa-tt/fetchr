package com.pharmeasy.fetchr.app

import android.app.Application
import com.crashlytics.android.Crashlytics
import com.pharmeasy.fetchr.greendao.DbOpenHelper
import com.pharmeasy.fetchr.greendao.model.DaoMaster
import com.pharmeasy.fetchr.greendao.model.DaoSession
import com.pharmeasy.fetchr.scanner.ScannerService
import io.fabric.sdk.android.Fabric
import org.greenrobot.greendao.query.QueryBuilder

class FetchrApplication : Application() {

    var daoSession: DaoSession? = null

    override fun onTerminate() {
        super.onTerminate()
        ScannerService.disconnect()
    }

    override fun onCreate() {
        super.onCreate()

        val helper= DbOpenHelper(applicationContext,"fetchr-db") //The fetchr-db here is the name of our database.
        val db = helper.writableDb

        daoSession = DaoMaster(db).newSession()

        QueryBuilder.LOG_SQL = true
        QueryBuilder.LOG_VALUES = true

        initLeakcanary()
    }

    private fun initLeakcanary() {
        setUpScrashlytics()

    }

    private fun setUpScrashlytics(){
        Fabric.with(this,Crashlytics())
    }

    public fun daoSession():DaoSession{

        return daoSession!!
    }
}
