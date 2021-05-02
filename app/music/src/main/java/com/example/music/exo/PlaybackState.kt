package com.example.music.exo

class PlaybackState(state: Int) {

    var state = STATE_NONE

    init {
        this.state = state
    }

    companion object{
        const val STATE_NONE = 0

        const val STATE_STOPPED = 1

        const val STATE_PAUSED = 2

        const val STATE_PLAYING = 3

        const val STATE_BUFFERING = 4
    }
}