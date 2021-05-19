package com.example.music.media

import android.util.Log
import com.example.music.exo.OnExoPlayerManagerCallback
import com.example.music.model.Song
import com.example.music.playlist.PlaylistManager

class MediaAdapter(
    private val onExoPlayerManagerCallBack: OnExoPlayerManagerCallback,
    private val mediaAdapterCallback: OnMediaAdapterCallback)
    :OnExoPlayerManagerCallback.OnSongStateCallback, PlaylistManager.OnSongUpdateListener{

    private var playlistManager: PlaylistManager? = null

    init {
        onExoPlayerManagerCallBack.setCallback(this)
        playlistManager = PlaylistManager(this)
    }

    fun play(song: Song){
        onExoPlayerManagerCallBack.play(song)
    }

    fun play(songList: MutableList<Song>,song: Song){
        playlistManager?.setCurrentPlayList(songList, song)
    }

    fun pause(){
        onExoPlayerManagerCallBack.pause()
    }

    fun seekTo(position: Long){
        onExoPlayerManagerCallBack.seekTo(position)
    }

    fun stop(){
        onExoPlayerManagerCallBack.stop()
    }

    fun skipToNext(){
        playlistManager?.skipPosition(1)
    }

    fun skipPrevious(){
        playlistManager?.skipPosition(-1)
    }

    fun addToCurrentPlaylist(songList: ArrayList<Song>){
        Log.d(TAG,"addToCurrentPlaylist() called with: songList = $songList")
        playlistManager?.addToPlayList(songList)
    }

    fun addToCurrentPlaylist(song: Song){
        Log.d(TAG,"addToCurrentPlaylist() called with: song = $song")
        playlistManager?.addToPlayList(song)
    }
    override fun onCompletion() {
        if(playlistManager?.isRepeat() == true){
            onExoPlayerManagerCallBack.stop()
            playlistManager?.repeat()
            return
        }

        if(playlistManager?.hasNext() == true){
            playlistManager?.skipPosition(1)
            return
        }

        if(playlistManager?.isRepeatAll() == true){
            playlistManager?.skipPosition(-1)
            return
        }

        onExoPlayerManagerCallBack.stop()
    }

    override fun onPlaybackStatusChanged(state: Int) {
        mediaAdapterCallback.onPlaybackStateChanged(state)
    }

    override fun setCurrentPosition(position: Long, duration: Long) {
        mediaAdapterCallback.setDuration(duration, position)
    }

    override fun getCurrentSong(): Song? {
        return playlistManager?.getCurrentSong()
    }

    override fun getCurrentSongList(): ArrayList<Song>? {
        return playlistManager?.getCurrentSongList()
    }

    override fun shuffle(isShuffle: Boolean) {
        playlistManager?.setShuffle(isShuffle)
    }

    override fun repeat(isRepeat: Boolean) {
        playlistManager?.setRepeat(isRepeat)
    }

    override fun repeatAll(isRepeatAll: Boolean) {
        playlistManager?.setRepeatAll(isRepeatAll)
    }


    companion object{
        private val TAG = MediaAdapter::class.java.name
    }

    override fun onSongChanged(song: Song) {
        play(song)
        mediaAdapterCallback.onSongChanged(song)
    }

    override fun onSongRetrieverError() {

    }
}