package com.example.music.notification

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Build
import android.os.RemoteException
import android.widget.RemoteViews
import androidx.annotation.RequiresApi
import androidx.core.app.NotificationCompat
import com.example.music.R
import com.example.music.model.Song
import com.example.music.service.SongPlayerService
import kotlin.jvm.Throws

class MediaNotificationManager @Throws(RemoteException::class)
constructor(private val mService: SongPlayerService): BroadcastReceiver(){

    private var mNotificationManager: NotificationManager? = null
    private val mPlayIntent: PendingIntent
    private val mPauseIntent: PendingIntent
    private val mPreviousIntent: PendingIntent
    private val mNextIntent: PendingIntent
    private val mStopIntent: PendingIntent
    private var mCollapsedRemoteViews: RemoteViews? = null
    private var mExpandedRemoteViews: RemoteViews? = null
    private var notificationBuilder: NotificationCompat.Builder? = null
    var mStarted = false


    private fun getPackageName(): String{
        return mService.packageName
    }
    init {

    }

    fun createMediaNotification(){
        val filter = IntentFilter().apply {
            addAction(ACTION_NEXT)
            addAction(ACTION_PAUSE)
            addAction(ACTION_PLAY)
            addAction(ACTION_PREV)
            addAction(ACTION_STOP)
        }
        mService.registerReceiver(this, filter)

        if(!mStarted)
        {
            mStarted = true
            mService.startForeground(NOTIFICATION_ID,  )
        }
    }
    override fun onReceive(context: Context?, intent: Intent?) {
        TODO("Not yet implemented")
    }


    fun generateNotification(): Notification? {
        if(notificationBuilder == null)
        {
            notificationBuilder = NotificationCompat.Builder(mService, CHANNEL_ID)
            notificationBuilder?.setSmallIcon(R.drawable.itunes)
                    ?.setLargeIcon(BitmapFactory.decodeResource(mService.resources, R.drawable.itunes))
                    ?.setContentTitle(mService.getString(R.string.player))
                    ?.setContentText(mService.getString(R.string.player))
                    ?.setDeleteIntent(mStopIntent)
                    ?.setVisibility(NotificationCompat.VISIBILITY_PRIVATE)
                    ?.setCategory(NotificationCompat.CATEGORY_TRANSPORT)
                    ?.setOnlyAlertOnce(true)

            if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.O){
                createNotificationChannel()
            }
        }

        mCollapsedRemoteViews =
            RemoteViews(getPackageName(), R.layout.player_collapsed_notification)
        notificationBuilder?.setCustomContentView(mCollapsedRemoteViews)
        mExpandedRemoteViews = RemoteViews(getPackageName(), R.layout.player_expanded_notification)
        notificationBuilder?.setCustomBigContentView(mExpandedRemoteViews)
        notificationBuilder?.setContentIntent(createContentIntent())
    }


    private fun createContentIntent():PendingIntent{
        val intent = Intent(Intent.ACTION_VIEW, Uri.parse("player://")).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP
            mService.getCurrentSong()?.let {
                putExtra(Song::class.java.name, it)
            }
            mService.getCurrentSong()?.let {
                putExtra(BaseSong)
            }
        }
    }

    @RequiresApi(Build.VERSION_CODES.O)
    fun createNotificationChannel(){
        if(mNotificationManager?.getNotificationChannel(CHANNEL_ID) == null){
            val notificationChannel = NotificationChannel(
                CHANNEL_ID,
                mService.getString(R.string.notification_channel),
                NotificationManager.IMPORTANCE_LOW
            )
            notificationChannel.description = mService.getString(R.string.notification_channel_description)
            mNotificationManager?.createNotificationChannel(notificationChannel)
        }
    }
    companion object{

        private val TAG =BaseSong
        private val isSupportedExpand = Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN
        private const val ACTION_PAUSE = "app.pause"
        private const val ACTION_PLAY = "app.play"
        private const val ACTION_PREV = "app.prev"
        private const val   ACTION_NEXT = "app.next"
        private const val ACTION_STOP = "app.stop"
        private const val CHANNEL_ID = "app.MUSIC_CHANNEL_ID"
        private const val NOTIFICATION_ID = 412
        private const val NOTIFICATION_REQUEST_CODE = 100
        private const val NOTIFICATION_REQUEST_INTENT_CODE = 125245
    }
}