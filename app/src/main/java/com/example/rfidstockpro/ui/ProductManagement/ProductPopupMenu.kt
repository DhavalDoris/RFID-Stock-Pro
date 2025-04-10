package com.example.rfidstockpro.ui.ProductManagement

import android.content.Context
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.PopupWindow
import com.example.rfidstockpro.aws.models.ProductModel
import com.example.rfidstockpro.databinding.ItemMenuBinding

class ProductPopupMenu(
    private val context: Context,
    private val anchor: View,
    private val product: ProductModel,
    private val listener: PopupActionListener
) {

    interface PopupActionListener {
        fun onViewClicked(product: ProductModel)
        fun onEditClicked(product: ProductModel)
        fun onLocateClicked(product: ProductModel)
        fun onUpdateClicked(product: ProductModel)
        fun onDeleteClicked(product: ProductModel)
    }

    fun show() {
        val inflater = LayoutInflater.from(context)
        val binding = ItemMenuBinding.inflate(inflater) // ViewBinding

        val widthInDp = 150
        val scale = context.resources.displayMetrics.density
        val widthInPx = (widthInDp * scale + 0.5f).toInt()

        val popupWindow = PopupWindow(
            binding.root,
            widthInPx,
            ViewGroup.LayoutParams.WRAP_CONTENT,
            true
        )

        popupWindow.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
        popupWindow.isOutsideTouchable = true
        popupWindow.elevation = 8f

        // 🔗 Bind click listeners
        binding.actionView.setOnClickListener {
            listener.onViewClicked(product)
            popupWindow.dismiss()
        }

        binding.actionEdit.setOnClickListener {
            listener.onEditClicked(product)
            popupWindow.dismiss()
        }

        binding.actionLocate.setOnClickListener {
            listener.onLocateClicked(product)
            popupWindow.dismiss()
        }

        binding.actionUpdate.setOnClickListener {
            listener.onUpdateClicked(product)
            popupWindow.dismiss()
        }

        binding.actionDelete.setOnClickListener {
            listener.onDeleteClicked(product)
            popupWindow.dismiss()
        }

        popupWindow.showAsDropDown(anchor, 0, 0)
    }
}

