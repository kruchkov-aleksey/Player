package com.example.music.service

import android.app.Service
import android.content.Intent
import android.os.Binder
import android.os.IBinder
import com.example.music.exo.ExoPlayerManager
import com.example.music.media.MediaAdapter
import com.example.music.media.OnMediaAdapterCallback
import com.example.music.model.Song
import com.example.music.notification.MediaNotificationManager

class SongPlayerService: Service(), OnMediaAdapterCallback {

    private var mMediaAdapter: MediaAdapter? = null
    private var mNotificationManager: MediaNotificationManager? = null
    private val binder = LocalBinder()
    private var playState = 0
    var mCallback: OnPlayerServiceCallback? = null
    var command: String? = null
    override fun onCreate() {
        super.onCreate()
        val exoPlayerManager = ExoPlayerManager(this)
        mMediaAdapter = MediaAdapter(exoPlayerManager, this)
        mNotificationManager = MediaNotificationManager(this)
        mNotificationManager?.createMediaNotifiocation()
    }

    override fun onBind(intent: Intent?): IBinder? {
        TODO("Not yet implemented")
    }


    fun getCurrentSong(): Song?{
        return mMediaAdapter?.getCurrentSong()
    }

    inner class LocalBinder: Binder(){
        val service: SongPlayerService
            get() = this@SongPlayerService
    }
    companion object{

        private val TAG = SongPlayerService::class.java.name
        const val ACTION_CMD = "app.ACTION_CMD"
        const val CMD_NAME = "CMD_NAME"
        const val CMD_PAUSE = "CMD_PAUSE"
    }

}