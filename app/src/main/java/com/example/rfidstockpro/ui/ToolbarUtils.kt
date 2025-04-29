package com.example.rfidstockpro.ui

import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.AppCompatImageView
import androidx.appcompat.widget.AppCompatTextView
import com.example.rfidstockpro.R

object ToolbarUtils {

    fun setupToolbar(activity: AppCompatActivity, config: ToolbarConfig) {
        val toolbarTitle = activity.findViewById<AppCompatTextView>(R.id.tvToolbarTitle)
        val toolbarSearch = activity.findViewById<AppCompatImageView>(R.id.ivSearch)
        val toolbarFilter = activity.findViewById<AppCompatImageView>(R.id.ivFilter)

        toolbarTitle?.text = config.title

        toolbarSearch?.visibility = if (config.showSearch) View.VISIBLE else View.GONE
        toolbarFilter?.visibility = if (config.showFilter) View.VISIBLE else View.GONE

        toolbarFilter?.setOnClickListener {
            config.onFilterClick?.invoke() ?: run {
                Toast.makeText(activity, "Filter clicked", Toast.LENGTH_SHORT).show()
            }
        }
    }
}

data class ToolbarConfig(
    val title: String,
    val showSearch: Boolean = false,
    val showFilter: Boolean = false,
    val onFilterClick: (() -> Unit)? = null
)

