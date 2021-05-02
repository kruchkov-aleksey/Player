package com.example.music.exo

import com.example.music.model.Song
import java.time.Duration

interface OnExoPlayerManagerCallback {
    fun getCurrentStreamPosition(): Long

    fun stop()

    fun play(song:Song)

    fun pause()

    fun seekTo(position: Long)

    fun setCallback(callback: OnSongStateCallback)

    interface OnSongStateCallback{

        fun onCompletion()

        fun onPlaybackStatusChanged(state: Int)

        fun setCurrentPosition(position: Long, duration: Long)

        fun getCurrentSong(): Song?

        fun getCurrentSongList(): ArrayList<Song>?

        fun shuffle(isShuffle: Boolean)

        fun repeat(isRepeat: Boolean)

        fun repeatAll(isRepeatAll: Boolean)

    }
}