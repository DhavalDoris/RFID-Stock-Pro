package com.example.rfidstockpro.ui.ProductManagement.adapters

import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.viewpager2.adapter.FragmentStateAdapter
import com.example.rfidstockpro.ui.ProductManagement.fragments.InventoryFragment
import com.example.rfidstockpro.ui.ProductManagement.fragments.ScannedProductsFragment

class ProductPagerAdapter(activity: AppCompatActivity) : FragmentStateAdapter(activity) {
    override fun getItemCount(): Int = 2

    override fun createFragment(position: Int): Fragment {
        return when (position) {
            0 -> InventoryFragment()
            1 -> ScannedProductsFragment()
            else -> Fragment()
        }
    }
}
