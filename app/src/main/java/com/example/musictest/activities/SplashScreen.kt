package com.example.musictest.activities

import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.view.animation.Animation
import android.view.animation.AnimationUtils
import androidx.appcompat.app.AppCompatActivity
import com.example.musictest.R
import kotlinx.android.synthetic.main.activity_splash_screen.*

class SplashScreen : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_splash_screen)

        val handler = Handler()

        if(true || syncMusicController.initialized)
        {
            splashIcon.rotation = 90.0f
            handler.postDelayed({
                val intent = Intent(this@SplashScreen, MainActivity::class.java)
                startActivity(intent)
                finish()
            }, 50)
        }
        else
        {
            val rotation: Animation = AnimationUtils.loadAnimation(this, R.anim.rotation)
            splashIcon.startAnimation(rotation)
            rotation.fillAfter = true
            handler.postDelayed({
                val intent = Intent(this@SplashScreen, MainActivity::class.java)
                startActivity(intent)
                finish()
            }, 600)
        }
    }
}