package com.example.rfidstockpro.Utils

import android.view.View
import android.view.animation.AlphaAnimation
import android.view.animation.Animation

object AnimationUtils {

    fun fadeInView(view: View) {
        view.visibility = View.VISIBLE
        val fadeIn = AlphaAnimation(0f, 1f).apply {
            duration = 500
        }
        view.startAnimation(fadeIn)
    }

    fun fadeOutView(view: View) {
        val fadeOut = AlphaAnimation(1f, 0f).apply {
            duration = 500
            setAnimationListener(object : Animation.AnimationListener {
                override fun onAnimationEnd(animation: Animation) {
                    view.visibility = View.GONE
                }

                override fun onAnimationRepeat(animation: Animation) {}
                override fun onAnimationStart(animation: Animation) {}
            })
        }
        view.startAnimation(fadeOut)
    }
}
