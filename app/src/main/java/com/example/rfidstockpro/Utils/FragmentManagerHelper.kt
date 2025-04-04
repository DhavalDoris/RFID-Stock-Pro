package com.example.rfidstockpro.Utils

import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity

object FragmentManagerHelper {
    fun setFragment(
        activity: FragmentActivity,
        fragment: Fragment,
        containerId: Int,
        addToBackStack: Boolean = true
    ) {
        val fragmentTransaction = activity.supportFragmentManager.beginTransaction()
        fragmentTransaction.replace(containerId, fragment)

        if (addToBackStack) {
            fragmentTransaction.addToBackStack(null)
        }

        fragmentTransaction.commit()
    }
}
