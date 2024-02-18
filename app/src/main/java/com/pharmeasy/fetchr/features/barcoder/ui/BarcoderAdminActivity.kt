package com.pharmeasy.fetchr.features.barcoder.ui

import android.content.Intent
import android.os.Bundle
import android.os.CountDownTimer
import android.view.View
import android.widget.ProgressBar
import com.pharmeasy.fetchr.R
import com.pharmeasy.fetchr.activity.BaseActivity
import com.pharmeasy.fetchr.constants.SCREEN_TIMEOUT
import com.pharmeasy.fetchr.main.ui.MainActivity
import kotlinx.android.synthetic.main.activity_barcoder_admin.*

class BarcoderAdminActivity : BaseActivity(), View.OnClickListener {

    override val rootView: View?
        get() = root_panel

    override val progressIndicator: ProgressBar?
        get() = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_barcoder_admin)

        iv_tick.setOnClickListener(this)

        startTimer()
    }

    override fun onClick(v: View?) {
        when(v?.id){
            R.id.iv_tick -> openMainActivity()
        }
    }

    private fun startTimer() {

        val countDownTimer = object : CountDownTimer((SCREEN_TIMEOUT * 1000).toLong(), 100) {

            override fun onTick(millisUntilFinished: Long) {

            }

            override fun onFinish() {
                openMainActivity()
            }
        }

        countDownTimer.start()
    }

    private fun openMainActivity(){
        val intent = Intent(this, MainActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP
        startActivity(intent)
        finishAffinity()
    }
}
