package com.pharmeasy.fetchr.activity

import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import android.view.View
import com.pharmeasy.fetchr.R
import kotlinx.android.synthetic.main.complete_task.*

class CompleteTask : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.complete_task)

        complete_task_content.visibility = View.GONE
    }
}
