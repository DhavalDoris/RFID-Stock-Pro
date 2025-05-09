package com.example.rfidstockpro.ui.activities

import android.graphics.Color
import android.os.Bundle
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.ViewModelProvider
import com.example.rfidstockpro.databinding.ActivityDemoBinding
import com.example.rfidstockpro.viewmodel.DashboardViewModel
import com.github.mikephil.charting.charts.PieChart
import com.github.mikephil.charting.data.PieData
import com.github.mikephil.charting.data.PieDataSet
import com.github.mikephil.charting.data.PieEntry
import android.graphics.Typeface
import android.text.Spannable
import android.text.SpannableString
import android.text.style.ForegroundColorSpan
import android.text.style.RelativeSizeSpan
import android.view.View
import android.widget.AdapterView
import androidx.core.content.ContextCompat
import androidx.core.content.res.ResourcesCompat
import com.example.rfidstockpro.R
import com.example.rfidstockpro.adapter.CustomSpinnerAdapter


class DemoActivity : AppCompatActivity() {

    private lateinit var binding: ActivityDemoBinding
    private lateinit var pieChart: PieChart
    private lateinit var viewModel: DashboardViewModel
    private val timeFilterOptions = listOf("Weekly", "Monthly", "Yearly")

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityDemoBinding.inflate(layoutInflater)
        setContentView(binding.root)
        enableEdgeToEdge()
        viewModel = ViewModelProvider(this).get(DashboardViewModel::class.java)

//        setupPieChart()
//        observeViewModel()
//        setupSpinner()
    }

    /*private fun setupPieChart() {

        // Load custom font
        val typeface: Typeface? = ResourcesCompat.getFont(this@DemoActivity, R.font.rethinksans_bold)

        // Create formatted center text
        val centerText = SpannableString("875\nTotal Stocks").apply {
            setSpan(RelativeSizeSpan(1.3f), 0, 3, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE) // Large size for "875"
            setSpan(ForegroundColorSpan(Color.BLACK), 0, 3, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE) // Black color
            setSpan(RelativeSizeSpan(0.5f), 4, length, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE) // Small size for "Total Stocks"
            setSpan(ForegroundColorSpan(Color.GRAY), 4, length, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE) // Gray color
        }
        pieChart = binding.pieChart

        // Apply to PieChart
        binding.pieChart.apply {
            setUsePercentValues(true)
            description.isEnabled = false
            holeRadius = 40f
            setHoleRadius(62f) // Increase for thicker ring
            setTransparentCircleRadius(70f) // Optional, for smooth edge
            setDrawHoleEnabled(true)
            setDrawCenterText(true) // Enable center text
            transparentCircleRadius = 45f
            setEntryLabelColor(Color.BLACK)
            setEntryLabelTextSize(12f)
            legend.isEnabled = false // Hide legend
            setDrawEntryLabels(false) // Hide labels inside the chart

            // Add Centered Text
            setCenterText(centerText) // Apply formatted text
            setCenterTextTypeface(typeface) // Apply custom font
            setCenterTextSize(24f) // Base size for scaling
            setCenterTextColor(Color.BLACK) // Ensures default color for unstyled text
            setTouchEnabled(false)
            animateXY(1000, 1000) // X and Y axis animation in milliseconds | 1-second animation
//            animateX(1000)
//            animateY(1000)
        }

    }

    private fun observeViewModel() {

        val entries = ArrayList<PieEntry>()
        entries.add(PieEntry(15f))  // Active
        entries.add(PieEntry(10f))  // Pending
        entries.add(PieEntry(30f))  // Inactive
        entries.add(PieEntry(30f))  // Return
        entries.add(PieEntry(30f))  // Sold

        val dataSet = PieDataSet(entries, "").apply {
            colors = listOf(
                ContextCompat.getColor(this@DemoActivity, R.color.colorActive),  // Active (Blue)
                ContextCompat.getColor(this@DemoActivity, R.color.colorPending), // Pending (Orange)
                ContextCompat.getColor(this@DemoActivity, R.color.colorInactive), // Inactive (Red)
                ContextCompat.getColor(this@DemoActivity, R.color.colorReturn), // Return (Blue)
                ContextCompat.getColor(this@DemoActivity, R.color.colorSold) // Sold (Gray)
            )
            setDrawValues(false) // **Disable text values (percentages)**
            valueTextSize = 14f
            valueTextColor = Color.TRANSPARENT // Hide values inside the chart
            sliceSpace = 2f // **Add white space between slices**
        }


        val data = PieData(dataSet).apply {
            setDrawValues(false) // **Ensure no values are shown**
        }

        pieChart.data = data
        pieChart.invalidate() // Refresh chart
    }

    private fun setupSpinner() {
        val adapter = CustomSpinnerAdapter(this, timeFilterOptions)
        binding.spinnerTimeFilter.adapter = adapter

        // Set default selection to "Monthly"
        binding.spinnerTimeFilter.setSelection(1)

        // Handle item selection
        binding.spinnerTimeFilter.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                val selectedOption = timeFilterOptions[position]
                // Handle selection if required
            }

            override fun onNothingSelected(parent: AdapterView<*>?) {}
        }
    }*/
}