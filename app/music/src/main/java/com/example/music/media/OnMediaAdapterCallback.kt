package com.example.music.media

import com.example.music.model.Song

interface OnMediaAdapterCallback {
    fun onSongChanged(song: Song)

    fun onPlaybackStateChanged(state: Int)

    fun setDuration(duration: Long, position: Long)

    fun addNewPlaylistToCurrent(songList: ArrayList<Song>)

    fun onShuffle(isShuffle: Boolean)

    fun onRepeat(isRepeat: Boolean)

    fun onRepeatAll(repeatAll: Boolean)
}