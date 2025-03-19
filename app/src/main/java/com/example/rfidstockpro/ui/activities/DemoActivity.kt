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
import androidx.core.content.res.ResourcesCompat
import com.example.rfidstockpro.R


class DemoActivity : AppCompatActivity() {

    private lateinit var binding: ActivityDemoBinding
    private lateinit var pieChart: PieChart
    private lateinit var viewModel: DashboardViewModel


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityDemoBinding.inflate(layoutInflater)
        setContentView(binding.root)
        enableEdgeToEdge()
        viewModel = ViewModelProvider(this).get(DashboardViewModel::class.java)

        setupPieChart()
        observeViewModel()

    }

    private fun setupPieChart() {

        // Load custom font
        val typeface: Typeface? = ResourcesCompat.getFont(this@DemoActivity, R.font.rethinksans_bold)

        // Create formatted center text
        val centerText = SpannableString("875\nTotal Stocks").apply {
            setSpan(RelativeSizeSpan(1.5f), 0, 3, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE) // Large size for "875"
            setSpan(ForegroundColorSpan(Color.BLACK), 0, 3, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE) // Black color
            setSpan(RelativeSizeSpan(0.6f), 4, length, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE) // Small size for "Total Stocks"
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
        }




    }

    private fun observeViewModel() {
        /*viewModel.stockData.observe(this) { data ->
            val dataSet = PieDataSet(data, "")
            dataSet.colors = listOf(
                Color.parseColor("#20C4B3"), // Active (Teal)
                Color.parseColor("#F1875F"), // Pending (Orange)
                Color.parseColor("#E6504B")  // Inactive (Red)
            )
//            dataSet.valueTextSize = 14f
//            dataSet.valueTextColor = Color.WHITE

            val pieData = PieData(dataSet)
            pieChart.data = pieData
            pieChart.invalidate() // Refresh



        }*/

        val entries = ArrayList<PieEntry>()
        entries.add(PieEntry(15f))  // Active
        entries.add(PieEntry(10f))  // Pending
        entries.add(PieEntry(30f))  // Inactive

        val dataSet = PieDataSet(entries, "").apply {
            colors = listOf(
                Color.parseColor("#28C5F1"), // Active (Blue)
                Color.parseColor("#F39C12"), // Pending (Orange)
                Color.parseColor("#E74C3C")  // Inactive (Red)
            )
            setDrawValues(false) // **Disable text values (percentages)**
        }

        val data = PieData(dataSet).apply {
            setDrawValues(false) // **Ensure no values are shown**
        }

        pieChart.data = data
        pieChart.invalidate() // Refresh chart
    }
}