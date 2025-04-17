package com.example.rfidstockpro.ui.ProductManagement.adapters

import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.viewpager2.adapter.FragmentStateAdapter
import com.example.rfidstockpro.ui.ProductManagement.fragments.StockFragment
import com.example.rfidstockpro.ui.ProductManagement.fragments.InventoryProductsFragment

class ProductPagerAdapter(activity: AppCompatActivity) : FragmentStateAdapter(activity) {
    override fun getItemCount(): Int = 2

    override fun createFragment(position: Int): Fragment {
        return when (position) {
            0 -> StockFragment()
            1 -> InventoryProductsFragment()
            else -> Fragment()
        }
    }
}
