package com.example.rfidstockpro.ui.fragments

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.example.rfidstockpro.R
import com.example.rfidstockpro.databinding.FragmentLocateBinding
import com.example.rfidstockpro.databinding.FragmentProductDetailBinding
import com.example.rfidstockpro.ui.ProductManagement.helper.ProductHolder

class LocateFragment : Fragment() {

    private lateinit var binding: FragmentLocateBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        binding = FragmentLocateBinding.inflate(inflater, container, false)

        initView()

        return binding.root
    }

    private fun initView() {

        ProductHolder.selectedProduct
        binding.tvLabelTag.setText(getString(R.string.locate_tag) +" : " + ProductHolder.selectedProduct!!.tagId)
    }

    companion object {

    }
}