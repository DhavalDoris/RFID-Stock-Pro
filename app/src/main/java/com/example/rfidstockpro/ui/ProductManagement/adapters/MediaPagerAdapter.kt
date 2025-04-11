package com.example.rfidstockpro.ui.ProductManagement.adapters

import android.net.Uri
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.media3.common.MediaItem
import androidx.media3.exoplayer.ExoPlayer
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.rfidstockpro.databinding.ItemMediaPageBinding

class MediaPagerAdapter(
    private val mediaList: List<String>,
    private val videoUrl: String?
) : RecyclerView.Adapter<MediaPagerAdapter.MediaViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MediaViewHolder {
        val binding = ItemMediaPageBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return MediaViewHolder(binding)
    }

    override fun getItemCount(): Int = if (videoUrl != null) mediaList.size + 1 else mediaList.size

    override fun onBindViewHolder(holder: MediaViewHolder, position: Int) {
        if (videoUrl != null && position == 0) {
            holder.bindVideo(videoUrl)
        } else {
            val index = if (videoUrl != null) position - 1 else position
            holder.bindImage(mediaList[index])
        }
    }

    inner class MediaViewHolder(private val binding: ItemMediaPageBinding) :
        RecyclerView.ViewHolder(binding.root) {

        private var player: ExoPlayer? = null

        fun bindImage(imageUrl: String) {
            with(binding) {
                imageView.visibility = View.VISIBLE
                playerView.visibility = View.GONE
                playIcon.visibility = View.GONE
                Glide.with(root.context).load(imageUrl).into(imageView)
            }
        }

        fun bindVideo(videoUrl: String) {
            with(binding) {
                imageView.visibility = View.VISIBLE
                playerView.visibility = View.GONE
                playIcon.visibility = View.VISIBLE
                Glide.with(root.context)
                    .load(videoUrl)
                    .thumbnail(0.1f)
                    .into(imageView)

                playIcon.setOnClickListener {
                    playIcon.visibility = View.GONE
                    imageView.visibility = View.GONE
                    playerView.visibility = View.VISIBLE

                    player = ExoPlayer.Builder(root.context).build().also {
                        playerView.player = it
                        val mediaItem = MediaItem.fromUri(Uri.parse(videoUrl))
                        it.setMediaItem(mediaItem)
                        it.prepare()
                        it.play()
                    }
                }
            }
        }

        fun releasePlayer() {
            player?.release()
            player = null
        }
    }

    override fun onViewRecycled(holder: MediaViewHolder) {
        holder.releasePlayer()
        super.onViewRecycled(holder)
    }
}

