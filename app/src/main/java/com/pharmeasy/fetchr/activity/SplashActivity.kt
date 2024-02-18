package com.pharmeasy.fetchr.activity

import android.content.Intent
import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import com.pharmeasy.fetchr.R
import com.pharmeasy.fetchr.greendao.TaskRepoNew
import io.reactivex.Observable
import io.reactivex.android.schedulers.AndroidSchedulers
import java.util.concurrent.TimeUnit

class SplashActivity : AppCompatActivity() {

    private val repo by lazy {
        TaskRepoNew(context = this)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.splash)

        repo.clearEvents()

        if (!isTaskRoot()
                && intent.hasCategory(Intent.CATEGORY_LAUNCHER)
                && intent.action != null
                && intent.action.equals(Intent.ACTION_MAIN)) {

            finish()
            return
        }

        delayedLogin()
    }

    private fun delayedLogin(){
        Observable.just("launch").delay(3, TimeUnit.SECONDS)
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe {
                    login()
                }
    }

    private fun login() {
        startActivity(Intent(this, LoginActivity::class.java))
        finish()
    }
}
