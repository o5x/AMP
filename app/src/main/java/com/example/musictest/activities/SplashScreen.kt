package com.example.musictest.activities

import android.Manifest.permission.READ_EXTERNAL_STORAGE
import android.Manifest.permission.RECORD_AUDIO
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.os.Handler
import android.view.View
import android.view.animation.Animation
import android.view.animation.Transformation
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.example.musictest.R
import kotlinx.android.synthetic.main.activity_splash_screen.*

class SplashScreen : AppCompatActivity() {

    private fun openMainActivity()
    {
        Handler().postDelayed({
            // Init things here ?
            val myIntent: Intent = Intent(this@SplashScreen, MainActivity::class.java)
            this@SplashScreen.startActivity(myIntent)
        }, 1)
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)

        if(grantResults[0] == PackageManager.PERMISSION_GRANTED
                && grantResults[1] == PackageManager.PERMISSION_GRANTED) openMainActivity()
        else Toast.makeText(baseContext,
                "You cannot continue without authorizations !",
                Toast.LENGTH_LONG).show()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_splash_screen)

        ll_auth.visibility = View.INVISIBLE

        if (ContextCompat.checkSelfPermission(this, READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED
                || ContextCompat.checkSelfPermission(this, RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
            val a: Animation = object : Animation() {
                override fun applyTransformation(interpolatedTime: Float, t: Transformation?) {
                    val splashLayout = splashIcon.layoutParams as ConstraintLayout.LayoutParams
                    splashLayout.verticalBias = 0.42f - 0.32f * interpolatedTime
                    splashIcon.layoutParams = splashLayout
                    splashIcon.alpha = 1f - interpolatedTime
                    textView.alpha = 1f - interpolatedTime

                    val authLayout = ll_auth.layoutParams as ConstraintLayout.LayoutParams
                    authLayout.verticalBias = 1f - 0.5f * interpolatedTime
                    ll_auth.layoutParams = authLayout
                    ll_auth.alpha = interpolatedTime
                }
            }
            a.duration = 800
            a.fillAfter = true
            splashIcon.startAnimation(a)
            ll_auth.visibility = View.VISIBLE
        }
        else openMainActivity()
    }

    fun request(v: View)
    {
        ActivityCompat.requestPermissions(this, arrayOf(READ_EXTERNAL_STORAGE, RECORD_AUDIO), 0)
    }
}