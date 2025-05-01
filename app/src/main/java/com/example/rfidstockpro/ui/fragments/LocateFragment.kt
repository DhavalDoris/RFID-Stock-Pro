package com.example.rfidstockpro.ui.fragments

import android.content.Context
import android.media.MediaPlayer
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.example.rfidstockpro.R
import com.example.rfidstockpro.Utils.Utils
import com.example.rfidstockpro.databinding.FragmentLocateBinding
import com.example.rfidstockpro.ui.ProductManagement.helper.ProductHolder
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

class LocateFragment : Fragment() {

    private lateinit var binding: FragmentLocateBinding
    private var playSoundThread: PlaySoundThread? = null
    private var soundJob: Job? = null

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
        binding.tvLabelTag.setText(getString(R.string.locate_tag) + " : " + ProductHolder.selectedProduct!!.tagId)
        playSuccessSound(requireContext())

    }

    companion object {

    }
    fun playSuccessSound(context: Context) {
        CoroutineScope(Dispatchers.Main).launch {
            if (playSoundThread == null) {
                playSoundThread = PlaySoundThread(context)
            }

            playSoundThread?.play(R.raw.barcodebeep)
                ?: Log.e("PlaySound", "PlaySoundThread is null!")
        }
    }
    class PlaySoundThread(private val context: Context) {

        fun play(soundResId: Int) {
            val mediaPlayer = MediaPlayer.create(context, soundResId)
            mediaPlayer?.setOnCompletionListener {
                it.release()
            }
            mediaPlayer?.start()
        }
    }
    override fun onResume() {
        super.onResume()
        soundJob = viewLifecycleOwner.lifecycleScope.launch {
            while (isActive) {
                playSuccessSound(requireContext())
                delay(1000)
            }
        }
    }

    override fun onPause() {
        super.onPause()
        soundJob?.cancel()
    }
}