package com.pharmeasy.fetchr.greendao

import android.content.Context
import android.util.Log

import com.pharmeasy.fetchr.greendao.model.DaoMaster
import com.pharmeasy.fetchr.greendao.model.DaoMaster.dropAllTables

import org.greenrobot.greendao.database.Database

class DbOpenHelper(context: Context, name: String) : DaoMaster.OpenHelper(context, name) {

    override fun onUpgrade(db: Database?, oldVersion: Int, newVersion: Int) {
        super.onUpgrade(db, oldVersion, newVersion)
        Log.d("DEBUG", "DB_OLD_VERSION : $oldVersion, DB_NEW_VERSION : $newVersion")
        if (oldVersion < newVersion) {
            dropAllTables(db, true)
            onCreate(db)
        }
    }
}