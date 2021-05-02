package com.example.music.exo

import android.content.*
import android.media.AudioAttributes
import android.media.AudioAttributes.CONTENT_TYPE_MUSIC
import android.media.AudioAttributes.USAGE_MEDIA
import android.media.AudioManager
import android.net.wifi.WifiManager
import android.os.Handler
import android.os.Looper
import android.os.Message
import com.example.music.model.Song
import com.example.music.service.SongPlayerService
import com.google.android.exoplayer2.BuildConfig
import com.google.android.exoplayer2.Player
import com.google.android.exoplayer2.SimpleExoPlayer
import com.google.android.exoplayer2.upstream.DataSource
import com.google.android.exoplayer2.upstream.DefaultBandwidthMeter
import com.google.android.exoplayer2.upstream.DefaultDataSourceFactory
import com.google.android.exoplayer2.util.Util

class ExoPlayerManager(val context: Context): OnExoPlayerManagerCallback {

    private var mWifiLock: WifiManager.WifiLock? = null
    private var mAudioManager: AudioManager? = null
    private val mAudioNoisyIntentFilter = IntentFilter(AudioManager.ACTION_AUDIO_BECOMING_NOISY)
    private var mExoSongStateCallback: OnExoPlayerManagerCallback.OnSongStateCallback? = null
    private var mAudioNoisyReceiverRegistered: Boolean = false
    private var mCurrentSong: Song? = null
    private var mCurrentAudioFocusState = AUDIO_NO_FOCUS_NO_DUCK
    private val mEventListener = ExoPlayerEventListener()
    private var mExoPlayer: SimpleExoPlayer? = null
    private var mPlayOnFocusGain: Boolean = false
    private val bandwidthMeter = DefaultBandwidthMeter()


    private val mAudioNoisyReceiver = object : BroadcastReceiver(){
        override fun onReceive(context: Context?, intent: Intent?) {
            if(AudioManager.ACTION_AUDIO_BECOMING_NOISY == intent?.action){
                if(mPlayOnFocusGain || mExoPlayer != null && mExoPlayer?.playWhenReady == true){
                    val i = Intent(context, SongPlayerService::class.java).apply {
                        action = SongPlayerService.ACTION_CMD
                        putExtra(SongPlayerService.CMD_NAME, SongPlayerService.CMD_PAUSE)
                    }
                    context?.applicationContext.startService(i)
                }
            }
        }
    }

    private val mUpdateProgressHandler = object : Handler(Looper.getMainLooper()){
        override fun handleMessage(msg: Message) {
            val duration = mExoPlayer?.duration ?: 0
            val position = mExoPlayer?.currentPosition ?: 0
            onUpdateProgress(position, duration)
            sendEmptyMessageDelayed(0, UPDATE_PROGRESS_DELAY)
        }
    }

    private var mExoPlayerIsStopped = false
    private val mOnAudioFocusChangeListener =
        AudioManager.OnAudioFocusChangeListener { focusChange ->

            when(focusChange){
                AudioManager.AUDIOFOCUS_GAIN -> mCurrentAudioFocusState = AUDIO_FOCUSED
                AudioManager.AUDIOFOCUS_LOSS_TRANSIENT_CAN_DUCK ->
                    mCurrentAudioFocusState = AUDIO_NO_FOCUS_CAN_DUCK
                AudioManager.AUDIOFOCUS_LOSS_TRANSIENT -> {
                    mCurrentAudioFocusState = AUDIO_NO_FOCUS_NO_DUCK
                    mPlayOnFocusGain = mExoPlayer != null && mExoPlayer?.playWhenReady ?: false
                }
                AudioManager.AUDIOFOCUS_LOSS ->
                    mCurrentAudioFocusState = AUDIO_NO_FOCUS_NO_DUCK
            }
            configurePlayerState()
        }

    init {
        this.mAudioManager =
                context.applicationContext?.getSystemService(Context.AUDIO_SERVICE) as AudioManager
        this.mWifiLock =
                (context.applicationContext?.getSystemService(Context.WIFI_SERVICE) as WifiManager)
                        .createWifiLock(WifiManager.WIFI_MODE_FULL, "app_lock")
    }

    private fun onUpdateProgress(position: Long, duration: Long){
        mExoSongStateCallback?.setCurrentPosition(position, duration)
    }

    override fun getCurrentStreamPosition(): Long {
        return mExoPlayer?.currentPosition ?: 0
    }

    override fun stop() {
        TODO("Not yet implemented")
    }


    override fun play(song: Song) {
        mPlayOnFocusGain = true
        tryToGetAudioFocus()
        registerAudioNoisyReceiver()

        val songHasChanged = song.songId != mCurrentSong?.songId
        if(songHasChanged) mCurrentSong = song

        if(songHasChanged || mExoPlayer == null){
            releaseResources(false)
            val source = mCurrentSong?.source
            if(mExoPlayer == null){
                mExoPlayer = SimpleExoPlayer.Builder(context).build()
                mExoPlayer?.addListener(mEventListener)
            }

            val audioAttributes = AudioAttributes.Builder()
                    .setContentType(CONTENT_TYPE_MUSIC)
                    .setUsage(USAGE_MEDIA)
                    .build()
            mExoPlayer?.setAudioAttributes(audioAttributes, false)

            val dataSourceFactory = buildDataSourceFactory(context)


        }
    }

    private fun buildDataSourceFactory(context: Context): DataSource.Factory{
        val dataSourceFactory = DefaultDataSourceFactory(
            context,
            Util.getUserAgent(context, BuildConfig.APPLICATION_ID),
                bandwidthMeter
        )
        return DefaultDataSourceFactory(context, bandwidthMeter, dataSourceFactory)
    }
    private fun releaseResources(releasePlayer: Boolean){
        if(releasePlayer){
            mUpdateProgressHandler.removeMessages(0)
            mExoPlayer?.release()
            mExoPlayer?.removeListener(mEventListener)
            mExoPlayer = null
            mExoPlayerIsStopped = true
            mPlayOnFocusGain = false
        }

        if(mWifiLock?.isHeld == true){
            mWifiLock?.release()
        }
    }
    private fun tryToGetAudioFocus(){
        val result = mAudioManager?.requestAudioFocus(
                mOnAudioFocusChangeListener,
                AudioManager.STREAM_MUSIC,
                AudioManager.AUDIOFOCUS_GAIN
        )
        mCurrentAudioFocusState = if (result == AudioManager.AUDIOFOCUS_REQUEST_GRANTED){
            AUDIO_FOCUSED
        }else{
            AUDIO_NO_FOCUS_NO_DUCK
        }
    }

    private fun giveUpAudioFocus(){
        if(mAudioManager?.abandonAudioFocus(mOnAudioFocusChangeListener) == AudioManager.AUDIOFOCUS_REQUEST_GRANTED){
            mCurrentAudioFocusState = AUDIO_NO_FOCUS_NO_DUCK
        }
    }

    override fun pause() {
        TODO("Not yet implemented")
    }

    override fun seekTo(position: Long) {
        TODO("Not yet implemented")
    }

    override fun setCallback(callback: OnExoPlayerManagerCallback.OnSongStateCallback) {
        mExoSongStateCallback = callback
    }

    fun setCurrentSongState(){
        var state = 0
        if(mExoPlayer == null){
            state = if(mExoPlayerIsStopped) PlaybackState.STATE_STOPPED
            else PlaybackState.STATE_NONE
            mExoSongStateCallback?.onPlaybackStatusChanged(state)
        }
        state = when (mExoPlayer?.playbackState){
            Player.STATE_IDLE -> PlaybackState.STATE_PAUSED
            Player.STATE_BUFFERING -> PlaybackState.STATE_BUFFERING
            Player.STATE_READY -> {
                if(mExoPlayer?.playWhenReady == true) PlaybackState.STATE_PLAYING
                else PlaybackState.STATE_PAUSED
            }
            Player.STATE_ENDED -> PlaybackState.STATE_STOPPED
            else -> PlaybackState.STATE_NONE
        }
        mExoSongStateCallback?.onPlaybackStatusChanged(state)
    }

    private fun configurePlayerState(){
        if(mCurrentAudioFocusState == AUDIO_NO_FOCUS_NO_DUCK){
            pause()
        }else{
            registerAudioNoisyReceiver()

            if(mCurrentAudioFocusState == AUDIO_NO_FOCUS_CAN_DUCK)
                mExoPlayer?.volume = VOLUME_DUCK
            else
                mExoPlayer?.volume = VOLUME_NORMAL

            if(mPlayOnFocusGain){
                mExoPlayer?.playWhenReady = true
                mPlayOnFocusGain = false
            }
        }
    }

    private fun registerAudioNoisyReceiver(){
        if(!mAudioNoisyReceiverRegistered){
            context.applicationContext.registerReceiver(
                    mAudioNoisyReceiver,
                    mAudioNoisyIntentFilter
            )
            mAudioNoisyReceiverRegistered = true
        }
    }
    private inner class ExoPlayerEventListener: Player.EventListener {

        override fun onLoadingChanged(isLoading: Boolean) {

        }

        override fun onPlayerStateChanged(playWhenReady: Boolean, playbackState: Int) {
            when (playbackState){
                Player.STATE_IDLE, Player.STATE_BUFFERING, Player.STATE_READY ->{
                    setCurrentSongState()
                    mUp
                }
            }
        }
    }
    companion object{
        const val UPDATE_PROGRESS_DELAY = 500L

        private const val VOLUME_DUCK = 0.2f

        private const val VOLUME_NORMAL = 1.0f

        private const val AUDIO_NO_FOCUS_NO_DUCK = 0

        private const val AUDIO_NO_FOCUS_CAN_DUCK = 1

        private const val AUDIO_FOCUSED = 2
    }
}