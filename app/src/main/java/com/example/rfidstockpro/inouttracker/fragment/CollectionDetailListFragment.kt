package com.example.rfidstockpro.inouttracker.fragment

import android.os.Bundle
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.widget.AppCompatImageView
import androidx.appcompat.widget.AppCompatTextView
import androidx.lifecycle.ViewModelProvider
import com.example.rfidstockpro.R
import com.example.rfidstockpro.databinding.FragmentCollectionDetailListBinding
import com.example.rfidstockpro.databinding.FragmentUhfreadTagBinding
import com.example.rfidstockpro.inouttracker.CollectionUtils.selectedCollection
import com.example.rfidstockpro.inouttracker.activity.InOutTrackerActivity
import com.example.rfidstockpro.ui.activities.AddProductActivity
import com.example.rfidstockpro.ui.activities.DeviceListActivity.TAG

class CollectionDetailListFragment : Fragment() {

    private lateinit var binding: FragmentCollectionDetailListBinding


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        binding = FragmentCollectionDetailListBinding.inflate(inflater, container, false)

        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupUI()
    }

    private fun setupUI() {
        val collection = selectedCollection
//        (activity as? InOutTrackerActivity)?.updateToolbarTitleAddItem(collection!!.collectionName)

    }


    companion object {

    }
}