package com.example.rfidstockpro.ui.ProductManagement.adapters

import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.viewpager2.adapter.FragmentStateAdapter
import com.example.rfidstockpro.inouttracker.model.CollectionModel
import com.example.rfidstockpro.ui.ProductManagement.fragments.StockFragment
import com.example.rfidstockpro.ui.ProductManagement.fragments.InventoryProductsFragment

class ProductPagerAdapter(
    activity: AppCompatActivity,
    private val showBothTabs: Boolean,
    private val comesFrom: String?,
    private val collectionName: String?,
    private val collectionId: String?,
    private val productIds: List<String>?, // 👈 Pass selected product IDs
    private val selectedItems: ArrayList<CollectionModel>? = null // Add this
) : FragmentStateAdapter(activity) {

    override fun getItemCount(): Int = if (showBothTabs) 2 else 1

    override fun createFragment(position: Int): Fragment {
        return if (showBothTabs) {
            when (position) {
               /* 0 -> StockFragment()
                1 -> InventoryProductsFragment()*/
                0 -> StockFragment.newInstance(
                    comesFrom = comesFrom,
                    collectionName = collectionName,
                    collectionId = collectionId,
                    productIds = productIds
                )
                1 -> InventoryProductsFragment.newInstance(
                    tabType = "Inventory",
                    comesFrom = comesFrom,
                    collectionName = collectionName,
                    collectionId = collectionId,
                    productIds = productIds,
                    selectedItems = selectedItems
                )
                else -> Fragment()
            }
        } else {
//            InventoryProductsFragment() // Only show Inventory
            InventoryProductsFragment.newInstance(
                tabType = "Inventory",
                comesFrom = comesFrom,
                collectionName = collectionName,
                collectionId = collectionId,
                productIds = productIds,
                selectedItems = selectedItems
            )
        }
    }
}