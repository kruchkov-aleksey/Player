package com.example.music.service

import com.example.music.model.Song

interface OnPlayerServiceCallback {
    fun updateSong(song: Song)

    fun updateSongProgress(duration: Long, position: Long)

    fun setBufferingData(isBuffering: Boolean)

    fun setVisibilityData(isVisibility: Boolean)

    fun setPlayStatus(isPlay: Boolean)

    fun stopService()
}