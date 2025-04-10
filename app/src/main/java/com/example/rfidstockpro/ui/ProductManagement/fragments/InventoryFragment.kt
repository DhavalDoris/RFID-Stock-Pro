package com.example.rfidstockpro.ui.ProductManagement.fragments

import android.content.Context
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.util.Log
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.PopupWindow
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.rfidstockpro.R
import com.example.rfidstockpro.aws.models.ProductModel
import com.example.rfidstockpro.databinding.FragmentInventoryBinding
import com.example.rfidstockpro.ui.ProductManagement.ProductPopupMenu
import com.example.rfidstockpro.ui.ProductManagement.viewmodels.InventoryViewModel
import com.example.rfidstockpro.ui.inventory.InventoryAdapter


class InventoryFragment : Fragment() {
    private lateinit var binding: FragmentInventoryBinding
    private lateinit var viewModel: InventoryViewModel
    private lateinit var inventoryAdapter: InventoryAdapter


    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentInventoryBinding.inflate(inflater, container, false)
        viewModel = ViewModelProvider(this)[InventoryViewModel::class.java]


        init()
        setupRecyclerView()
        observeViewModel()

        viewModel.loadNextPage()
        return binding.root
    }

    fun init() {
        binding.btnLoadMore.setOnClickListener {
            viewModel.loadNextPage()
        }
    }

    private fun setupRecyclerView() {


        inventoryAdapter = InventoryAdapter(emptyList()) { product, anchorView ->
            ProductPopupMenu(requireContext(), anchorView, product, object : ProductPopupMenu.PopupActionListener {
                override fun onViewClicked(product: ProductModel) {
                    Toast.makeText(requireContext(), "View: ${product.productName}", Toast.LENGTH_SHORT).show()
                }

                override fun onEditClicked(product: ProductModel) {}
                override fun onLocateClicked(product: ProductModel) {}
                override fun onUpdateClicked(product: ProductModel) {}
                override fun onDeleteClicked(product: ProductModel) {}
            }).show()
        }

        val layoutManager = LinearLayoutManager(requireContext())
        binding.recyclerView.layoutManager = layoutManager
        binding.recyclerView.adapter = inventoryAdapter


    }

    fun showCustomPopupMenu(context: Context, anchor: View, product: ProductModel) {
        val inflater = LayoutInflater.from(context)
        val popupView = inflater.inflate(R.layout.item_menu, null)

        val widthInDp = 150
        val scale = context.resources.displayMetrics.density
        val widthInPx = (widthInDp * scale + 0.5f).toInt()


        val popupWindow = PopupWindow(
            popupView,
            widthInPx,
            ViewGroup.LayoutParams.WRAP_CONTENT,
            true
        )

        val location = IntArray(2)
        anchor.getLocationOnScreen(location)

        popupWindow.showAtLocation(anchor, Gravity.NO_GRAVITY,
            location[0] + anchor.width - widthInPx,
            location[1] + anchor.height)

        // This will show it aligned to the bottom-left of the anchor view
//        popupWindow.showAsDropDown(anchor, -(widthInPx - anchor.width), 0)

        // Set background to dismiss on outside touch
        popupWindow.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
        popupWindow.isOutsideTouchable = true
        popupWindow.elevation = 8f

        // Set click listeners
        popupView.findViewById<LinearLayout>(R.id.action_view).setOnClickListener {
            Toast.makeText(context, "View clicked", Toast.LENGTH_SHORT).show()
            popupWindow.dismiss()
        }

        popupView.findViewById<LinearLayout>(R.id.action_edit).setOnClickListener {
            Toast.makeText(context, "edit clicked", Toast.LENGTH_SHORT).show()
            popupWindow.dismiss()
        }

        popupView.findViewById<LinearLayout>(R.id.action_locate).setOnClickListener {
            Toast.makeText(context, "locate clicked", Toast.LENGTH_SHORT).show()
            popupWindow.dismiss()
        }

        popupView.findViewById<LinearLayout>(R.id.action_update).setOnClickListener {
            Toast.makeText(context, "update clicked", Toast.LENGTH_SHORT).show()
            popupWindow.dismiss()
        }

        popupView.findViewById<LinearLayout>(R.id.action_delete).setOnClickListener {
            Toast.makeText(context, "Delete clicked", Toast.LENGTH_SHORT).show()
            popupWindow.dismiss()
        }

        // Show popup at anchor position
        popupWindow.showAsDropDown(anchor, 0, 0)
    }


    private fun observeViewModel() {

        viewModel.isLoading.observe(viewLifecycleOwner) { isLoading ->

            val productList = viewModel.products.value ?: emptyList()
            // Show progress only if loading and no data yet
            binding.progressBar.visibility =
                if (isLoading && productList.isEmpty()) View.VISIBLE else View.GONE

            // Show "No items" only if not loading and list is empty
            binding.noItemsText.visibility =
                if (!isLoading && productList.isEmpty()) View.VISIBLE else View.GONE

            // Show RecyclerView only if list is not empty
            binding.recyclerView.visibility =
                if (productList.isNotEmpty()) View.VISIBLE else View.GONE

            binding.btnLoadMore.visibility =
                if (!isLoading && productList.isNotEmpty()) View.VISIBLE else View.GONE

            binding.loadMoreProgress.visibility =
                if (isLoading) View.VISIBLE else View.GONE

        }

        viewModel.products.observe(viewLifecycleOwner) { productList ->
            inventoryAdapter.updateList(productList)
            binding.recyclerView.visibility =
                if (productList.isNotEmpty()) View.VISIBLE else View.GONE
            binding.noItemsText.visibility = if (productList.isEmpty()) View.VISIBLE else View.GONE

            // Update count
            val total = viewModel.totalCount.value ?: 0
            Log.e("TOTAL_TAG", "observeViewModel:==>> $total")
            binding.itemCountText.text = "${productList.size} of $total"

            // Show/hide load more
            binding.loadMoreContainer.visibility =
                if (productList.size < total) View.VISIBLE else View.GONE
        }

        viewModel.totalCount.observe(viewLifecycleOwner) { total ->
            val currentCount = viewModel.products.value?.size ?: 0
            Log.e("TOTAL_TAG", "observeViewModel:~~>> $total")
            binding.itemCountText.text = "$currentCount of $total"
            binding.loadMoreContainer.visibility =
                if (currentCount < total) View.VISIBLE else View.GONE
        }

    }


}
