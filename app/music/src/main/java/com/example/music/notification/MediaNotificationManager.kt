package com.example.music.notification

import android.app.*
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Build
import android.os.RemoteException
import android.view.View
import android.widget.RemoteViews
import androidx.annotation.RequiresApi
import androidx.core.app.NotificationCompat
import com.example.music.BaseSongPlayerActivity
import com.example.music.R
import com.example.music.exo.PlaybackState
import com.example.music.model.Song
import com.example.music.service.SongPlayerService
import java.io.File

class MediaNotificationManager @Throws(RemoteException::class)
constructor(private val mService: SongPlayerService) : BroadcastReceiver() {


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


    private fun getPackageName(): String {
        return mService.packageName
    }


    init {
        mNotificationManager =
            mService.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        mPauseIntent = PendingIntent.getBroadcast(
            mService, NOTIFICATION_REQUEST_CODE,
            Intent(ACTION_PAUSE).setPackage(getPackageName()), PendingIntent.FLAG_CANCEL_CURRENT
        )
        mPlayIntent = PendingIntent.getBroadcast(
            mService, NOTIFICATION_REQUEST_CODE,
            Intent(ACTION_PLAY).setPackage(getPackageName()), PendingIntent.FLAG_CANCEL_CURRENT
        )
        mPreviousIntent = PendingIntent.getBroadcast(
            mService, NOTIFICATION_REQUEST_CODE,
            Intent(ACTION_PREV).setPackage(getPackageName()), PendingIntent.FLAG_CANCEL_CURRENT
        )
        mNextIntent = PendingIntent.getBroadcast(
            mService, NOTIFICATION_REQUEST_CODE,
            Intent(ACTION_NEXT).setPackage(getPackageName()), PendingIntent.FLAG_CANCEL_CURRENT
        )
        mStopIntent = PendingIntent.getBroadcast(
            mService, NOTIFICATION_REQUEST_CODE,
            Intent(ACTION_STOP).setPackage(getPackageName()), PendingIntent.FLAG_CANCEL_CURRENT
        )

        mNotificationManager?.cancelAll()
    }


    fun createMediaNotification() {

        val filter = IntentFilter().apply {
            addAction(ACTION_NEXT)
            addAction(ACTION_PAUSE)
            addAction(ACTION_PLAY)
            addAction(ACTION_PREV)
            addAction(ACTION_STOP)
        }
        mService.registerReceiver(this, filter)

        if (!mStarted) {
            mStarted = true
            mService.startForeground(NOTIFICATION_ID, generateNotification())
        }
    }


    override fun onReceive(context: Context, intent: Intent) {
        when (intent.action) {
            ACTION_PAUSE -> mService.pause()
            ACTION_PLAY -> mService.playCurrentSong()
            ACTION_NEXT -> mService.skipToNext()
            ACTION_PREV -> mService.skipToPrevious()
            ACTION_STOP -> {
                mService.run {
                    unregisterReceiver(this@MediaNotificationManager)
                    stop()
                }
            }
        }
    }


    fun generateNotification(): Notification? {
        if (notificationBuilder == null) {
            notificationBuilder = NotificationCompat.Builder(mService, CHANNEL_ID)
            notificationBuilder?.setSmallIcon(R.drawable.itunes)
                ?.setLargeIcon(BitmapFactory.decodeResource(mService.resources, R.drawable.itunes))
                ?.setContentTitle(mService.getString(R.string.app_name))
                ?.setContentText(mService.getString(R.string.app_name))
                ?.setDeleteIntent(mStopIntent)
                ?.setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
                ?.setCategory(NotificationCompat.CATEGORY_TRANSPORT)
                ?.setOnlyAlertOnce(true)

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                createNotificationChannel()
            }
        }

        mCollapsedRemoteViews =
            RemoteViews(getPackageName(), R.layout.player_collapsed_notification)
        notificationBuilder?.setCustomContentView(mCollapsedRemoteViews)
        mExpandedRemoteViews = RemoteViews(getPackageName(), R.layout.player_expanded_notification)
        notificationBuilder?.setCustomBigContentView(mExpandedRemoteViews)
        notificationBuilder?.setContentIntent(createContentIntent())

        notificationBuilder?.setOngoing(true)

        mCollapsedRemoteViews?.let { createCollapsedRemoteViews(it) }
        mExpandedRemoteViews?.let { createExpandedRemoteViews(it) }



        mService.getCurrentSong()?.clipArt?.let {
            val bitmap = BitmapFactory.decodeFile(File(it).path)
            mCollapsedRemoteViews?.setImageViewBitmap(R.id.collapsed_notification_image_view, bitmap)
            mExpandedRemoteViews?.setImageViewBitmap(R.id.expanded_notification_image_view, bitmap)
        }


        if (mService.getPlayState() == PlaybackState.STATE_PLAYING ||
            mService.getPlayState() == PlaybackState.STATE_BUFFERING
        ) showPauseIcon() else showPlayIcon()

        mNotificationManager?.notify(NOTIFICATION_ID, notificationBuilder?.build())
        return notificationBuilder?.build()
    }


    private fun createContentIntent(): PendingIntent {
        val intent = Intent(Intent.ACTION_VIEW, Uri.parse("player://")).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP
            mService.getCurrentSong()?.let {
                putExtra(Song::class.java.name, it)
            }
            mService.getCurrentSongList()?.let {
                putExtra(BaseSongPlayerActivity.SONG_LIST_KEY, it)
            }
        }

        return TaskStackBuilder.create(mService).run {
            addNextIntentWithParentStack(intent)
            getPendingIntent(NOTIFICATION_REQUEST_INTENT_CODE, PendingIntent.FLAG_UPDATE_CURRENT)
        }
    }

    private fun showPlayIcon() {
        mCollapsedRemoteViews?.setViewVisibility(
            R.id.collapsed_notification_pause_image_view,
            View.GONE
        )
        mCollapsedRemoteViews?.setViewVisibility(
            R.id.collapsed_notification_play_image_view,
            View.VISIBLE
        )
        mExpandedRemoteViews?.setViewVisibility(
            R.id.expanded_notification_pause_image_view,
            View.GONE
        )
        mExpandedRemoteViews?.setViewVisibility(
            R.id.expanded_notification_play_image_view,
            View.VISIBLE
        )
    }

    private fun showPauseIcon() {
        mCollapsedRemoteViews?.setViewVisibility(
            R.id.collapsed_notification_pause_image_view,
            View.VISIBLE
        )
        mCollapsedRemoteViews?.setViewVisibility(
            R.id.collapsed_notification_play_image_view,
            View.GONE
        )
        mExpandedRemoteViews?.setViewVisibility(
            R.id.expanded_notification_pause_image_view,
            View.VISIBLE
        )
        mExpandedRemoteViews?.setViewVisibility(
            R.id.expanded_notification_play_image_view,
            View.GONE
        )
    }

    private fun createExpandedRemoteViews(expandedRemoteViews: RemoteViews) {
        if (isSupportExpand) {
            expandedRemoteViews.setOnClickPendingIntent(
                R.id.expanded_notification_skip_back_image_view,
                mPreviousIntent
            )
            expandedRemoteViews.setOnClickPendingIntent(
                R.id.expanded_notification_clear_image_view,
                mStopIntent
            )
            expandedRemoteViews.setOnClickPendingIntent(
                R.id.expanded_notification_pause_image_view,
                mPauseIntent
            )
            expandedRemoteViews.setOnClickPendingIntent(
                R.id.expanded_notification_skip_next_image_view,
                mNextIntent
            )
            expandedRemoteViews.setOnClickPendingIntent(
                R.id.expanded_notification_play_image_view,
                mPlayIntent
            )

            expandedRemoteViews.setImageViewResource(
                R.id.expanded_notification_image_view,
                R.drawable.placeholder
            )
        }

        expandedRemoteViews.setViewVisibility(
            R.id.expanded_notification_skip_next_image_view,
            View.VISIBLE
        )
        expandedRemoteViews.setViewVisibility(
            R.id.expanded_notification_skip_back_image_view,
            View.VISIBLE
        )
        expandedRemoteViews.setTextViewText(
            R.id.expanded_notification_song_name_text_view,
            mService.getCurrentSong()?.title
        )
        expandedRemoteViews.setTextViewText(
            R.id.expanded_notification_singer_name_text_view,
            mService.getCurrentSong()?.artist
        )

    }

    private fun createCollapsedRemoteViews(collapsedRemoteViews: RemoteViews) {

        collapsedRemoteViews.setOnClickPendingIntent(
            R.id.collapsed_notification_skip_back_image_view,
            mPreviousIntent
        )
        collapsedRemoteViews.setOnClickPendingIntent(
            R.id.collapsed_notification_clear_image_view,
            mStopIntent
        )
        collapsedRemoteViews.setOnClickPendingIntent(
            R.id.collapsed_notification_pause_image_view,
            mPauseIntent
        )
        collapsedRemoteViews.setOnClickPendingIntent(
            R.id.collapsed_notification_skip_next_image_view,
            mNextIntent
        )
        collapsedRemoteViews.setOnClickPendingIntent(
            R.id.collapsed_notification_play_image_view,
            mPlayIntent
        )

        collapsedRemoteViews.setImageViewResource(
            R.id.collapsed_notification_image_view,
            R.drawable.placeholder
        )
        collapsedRemoteViews.setViewVisibility(
            R.id.collapsed_notification_skip_next_image_view,
            View.VISIBLE
        )
        collapsedRemoteViews.setViewVisibility(
            R.id.collapsed_notification_skip_back_image_view,
            View.VISIBLE
        )
        collapsedRemoteViews.setTextViewText(
            R.id.collapsed_notification_song_name_text_view,
            mService.getCurrentSong()?.title
        )
        collapsedRemoteViews.setTextViewText(
            R.id.collapsed_notification_singer_name_text_view,
            mService.getCurrentSong()?.artist
        )
    }



    @RequiresApi(Build.VERSION_CODES.O)
    fun createNotificationChannel() {
        if (mNotificationManager?.getNotificationChannel(CHANNEL_ID) == null) {
            val notificationChannel = NotificationChannel(
                CHANNEL_ID,
                mService.getString(R.string.notification_channel),
                NotificationManager.IMPORTANCE_LOW
            )
            notificationChannel.description =
                mService.getString(R.string.notification_channel_description)
            mNotificationManager?.createNotificationChannel(notificationChannel)
        }
    }

    companion object {

        private val TAG = MediaNotificationManager::class.java.name
        private val isSupportExpand = Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN
        private const val ACTION_PAUSE = "app.pause"
        private const val ACTION_PLAY = "app.play"
        private const val ACTION_PREV = "app.prev"
        private const val ACTION_NEXT = "app.next"
        private const val ACTION_STOP = "app.stop"
        private const val CHANNEL_ID = "app.MUSIC_CHANNEL_ID"
        private const val NOTIFICATION_ID = 412
        private const val NOTIFICATION_REQUEST_CODE = 100
        private const val NOTIFICATION_REQUEST_INTENT_CODE = 125245
    }
}