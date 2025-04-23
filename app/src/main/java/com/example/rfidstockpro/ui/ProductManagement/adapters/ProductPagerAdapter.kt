package com.example.rfidstockpro.ui.ProductManagement.adapters

import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.viewpager2.adapter.FragmentStateAdapter
import com.example.rfidstockpro.ui.ProductManagement.fragments.StockFragment
import com.example.rfidstockpro.ui.ProductManagement.fragments.InventoryProductsFragment

class ProductPagerAdapter(
    activity: AppCompatActivity,
    private val showBothTabs: Boolean
) : FragmentStateAdapter(activity) {

    override fun getItemCount(): Int = if (showBothTabs) 2 else 1

    override fun createFragment(position: Int): Fragment {
        return if (showBothTabs) {
            when (position) {
                0 -> StockFragment()
                1 -> InventoryProductsFragment()
                else -> Fragment()
            }
        } else {
            InventoryProductsFragment() // Only show Inventory
        }
    }
}