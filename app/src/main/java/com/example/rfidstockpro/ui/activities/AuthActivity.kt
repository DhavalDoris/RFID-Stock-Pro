package com.example.rfidstockpro.ui.activities

import android.animation.ValueAnimator
import android.os.Bundle
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.res.ResourcesCompat
import androidx.viewpager2.widget.ViewPager2
import com.example.rfidstockpro.R
import com.example.rfidstockpro.Utils.StatusBarUtils
import com.example.rfidstockpro.adapter.AuthPagerAdapter
import com.example.rfidstockpro.databinding.ActivityAuthBinding
import com.google.android.material.tabs.TabLayoutMediator

class AuthActivity : AppCompatActivity() {

    private lateinit var binding: ActivityAuthBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        binding = ActivityAuthBinding.inflate(layoutInflater)
        setContentView(binding.root)

        StatusBarUtils.setTransparentStatusBar(this)

        val adapter = AuthPagerAdapter(this)
        binding.viewPager.adapter = adapter


        // Connect TabLayout with ViewPager2
        TabLayoutMediator(binding.tabLayout, binding.viewPager) { tab, position ->
            tab.text = if (position == 0) "Login" else "Sign up"
        }.attach()
        setTabFont()

        // Animate Header when switching tabs
        binding.viewPager.registerOnPageChangeCallback(object : ViewPager2.OnPageChangeCallback() {
            override fun onPageSelected(position: Int) {
                super.onPageSelected(position)  // -180f else 0f
               /* val targetHeight  = if (position == 1)  -80f else 0f  // Adjusted height for sign-up tab
                ObjectAnimator.ofFloat(binding.topLayout, "translationY", targetHeight).apply {
                    duration = 300
                    start()
                }*/
//                animateHeight(binding.topLayout, targetHeight)

                // Set height based on tab selection
                val newTopHeight = if (position == 1) resources.getDimensionPixelSize(R.dimen._200sdp)
                else resources.getDimensionPixelSize(R.dimen._300sdp)
                animateHeight(binding.topLayout, newTopHeight)

                // Set margin top based on tab selection
                val newMarginTop = if (position == 1) resources.getDimensionPixelSize(R.dimen._150sdp)
                else resources.getDimensionPixelSize(R.dimen._250sdp)
                animateMarginTop(binding.viewPager.parent as View, newMarginTop)
            }
        })
    }

    private fun animateHeight(view: View, targetHeight: Int) {
        val startHeight = view.height
        val animator = ValueAnimator.ofInt(startHeight, targetHeight)
        animator.duration = 300
        animator.addUpdateListener { animation ->
            val value = animation.animatedValue as Int
            val layoutParams = view.layoutParams
            layoutParams.height = value
            view.layoutParams = layoutParams
        }
        animator.start()
    }

    private fun setTabFont() {
        val tabLayout = binding.tabLayout
        val font = ResourcesCompat.getFont(this, R.font.rethinksans_bold)

        for (i in 0 until tabLayout.tabCount) {
            val tab = tabLayout.getTabAt(i)
            val textView =
                (tab?.view?.getChildAt(1) as? TextView) // Default TabLayout uses TextView at index 1
            textView?.typeface = font
        }
    }

    private fun animateMarginTop(view: View, targetMarginTop: Int) {
        val params = view.layoutParams as ViewGroup.MarginLayoutParams
        val startMargin = params.topMargin

        val animator = ValueAnimator.ofInt(startMargin, targetMarginTop)
        animator.duration = 300
        animator.addUpdateListener { animation ->
            params.topMargin = animation.animatedValue as Int
            view.layoutParams = params
        }
        animator.start()
    }

}
