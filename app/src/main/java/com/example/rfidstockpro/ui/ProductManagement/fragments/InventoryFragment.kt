package com.example.rfidstockpro.ui.ProductManagement.fragments

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.text.SpannableString
import android.text.style.ForegroundColorSpan
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.PopupMenu
import android.widget.PopupWindow
import android.widget.Toast
import androidx.appcompat.view.menu.MenuBuilder
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.rfidstockpro.R
import com.example.rfidstockpro.aws.models.ProductModel
import com.example.rfidstockpro.databinding.FragmentInventoryBinding
import com.example.rfidstockpro.ui.ProductManagement.viewmodels.InventoryViewModel
import com.example.rfidstockpro.ui.inventory.InventoryAdapter


class InventoryFragment : Fragment() {
    private lateinit var binding: FragmentInventoryBinding
    private lateinit var viewModel: InventoryViewModel
    private lateinit var inventoryAdapter: InventoryAdapter


    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        binding = FragmentInventoryBinding.inflate(inflater, container, false)
        viewModel = ViewModelProvider(this)[InventoryViewModel::class.java]

        setupRecyclerView()
        observeViewModel()

        viewModel.loadNextPage()
        return binding.root
    }

    private fun setupRecyclerView() {
        inventoryAdapter = InventoryAdapter(emptyList()) { product, anchorView ->
            showCustomPopupMenu(requireActivity(),anchorView, product)
        }

        val layoutManager = LinearLayoutManager(requireContext())
        binding.recyclerView.layoutManager = layoutManager
        binding.recyclerView.adapter = inventoryAdapter

        binding.recyclerView.addOnScrollListener(object : RecyclerView.OnScrollListener() {
            override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
                val layoutManager = recyclerView.layoutManager as LinearLayoutManager
                val visibleItemCount = layoutManager.childCount
                val totalItemCount = layoutManager.itemCount
                val firstVisibleItemPos = layoutManager.findFirstVisibleItemPosition()

                val isAtEnd = (visibleItemCount + firstVisibleItemPos) >= totalItemCount
                        && firstVisibleItemPos >= 0

                if (isAtEnd) {
                    viewModel.loadNextPage()
                }
            }
        })

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

        // This will show it aligned to the bottom-left of the anchor view
        popupWindow.showAsDropDown(anchor, -popupView.width + anchor.width, 0)

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
            Toast.makeText(context, "Delete clicked", Toast.LENGTH_SHORT).show()
            popupWindow.dismiss()
        }

        popupView.findViewById<LinearLayout>(R.id.action_locate).setOnClickListener {
            Toast.makeText(context, "Delete clicked", Toast.LENGTH_SHORT).show()
            popupWindow.dismiss()
        }

        popupView.findViewById<LinearLayout>(R.id.action_update).setOnClickListener {
            Toast.makeText(context, "Delete clicked", Toast.LENGTH_SHORT).show()
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
        viewModel.products.observe(viewLifecycleOwner) { productList ->
            Log.d("InventoryFragment", "Loaded ${productList.size} products")
            inventoryAdapter.updateList(productList)
        }
    }


}
