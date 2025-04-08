package com.example.rfidstockpro.ui.ProductManagement.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.example.rfidstockpro.databinding.FragmentScannedProductsBinding


class ScannedProductsFragment : Fragment() {
    private lateinit var binding: FragmentScannedProductsBinding
//    private val viewModel by viewModels<AllProductsViewModel>() // Same ViewModel as AllProductsFragment

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        binding = FragmentScannedProductsBinding.inflate(inflater, container, false)

//        val adapter = ProductAdapter()
//        binding.recyclerView.adapter = adapter

        // Scan logic (optional scan button)
       /* binding.btnStartScan.setOnClickListener {
            viewModel.startRFIDScan() // Will update RFIDTagManager
        }*/

       /* viewModel.products.observe(viewLifecycleOwner) { allProducts ->
            val filtered = allProducts.filter { product ->
                product.tagId in RFIDTagManager.getTags()
            }
            adapter.submitList(filtered)
        }

        viewModel.fetchAllProducts()*/

        return binding.root
    }
}
