package com.example.music.playlist


import com.example.music.model.Song
import java.util.*
import kotlin.collections.ArrayList
import kotlin.math.max



class PlaylistManager(private val mListener: OnSongUpdateListener) {

    private var playlist: Playlist? = null
    private var mCurrentIndex: Int = 0

    init {
        playlist = Playlist()
        mCurrentIndex = 0
    }

    fun getCurrentSong(): Song?{
        return playlist?.getItem(mCurrentIndex)
    }

    fun getCurrentSongList(): ArrayList<Song>{
        return playlist?.getShuffleOrNormalList() as ArrayList<Song>
    }

    private fun setCurrentPlayListIndex(index: Int){
        if(index >= 0 && index < playlist?.getShuffleOrNormalList()?.size ?: 0){
            mCurrentIndex = index
        }
        updateSong()
    }

    fun hasNext(): Boolean = mCurrentIndex < (playlist?.getCurrentPlayListSize()?.minus(1) ?: 0)

    fun skipPosition(amount: Int): Boolean{
        var index = mCurrentIndex + amount
        val currentPlayListSize = playlist?.getCurrentPlayListSize() ?: 0

        if (currentPlayListSize == 0 || index >= currentPlayListSize) return false
        if(index < 0){
            index = if (isRepeatAll()) currentPlayListSize else 0
        }else{
            if(currentPlayListSize != 0) index %= currentPlayListSize
        }
        return if (mCurrentIndex == index){
            setCurrentPlayListIndex(mCurrentIndex)
            false
        }else{
            mCurrentIndex = index
            setCurrentPlayListIndex(mCurrentIndex)
            true
        }
    }


    fun setCurrentPlayList(newPlayList: MutableList<Song>, initialSong: Song? = null){
        playlist = Playlist().setList(newPlayList)
        var index = 0
        initialSong?.let {
            index = getSongIndexOnPlayList(playlist?.getShuffleOrNormalList() as Iterable<Song>, it)
        }
        mCurrentIndex = max(index, 0)
        setCurrentPlayListIndex(index)
    }

    fun isRepeatAll(): Boolean = playlist?.isRepeatAll ?: false

    private fun getSongIndexOnPlayList(list: Iterable<Song>, song: Song): Int{
        for((index,item) in list.withIndex()){
            if(song.songId == item.songId){
                return index
            }
        }
        return -1
    }

    private fun updateSong(){
        val currentSong = getCurrentSong()
        if(currentSong == null){
            mListener.onSongRetrieverError()
            return
        }
        mListener.onSongChanged(currentSong)
    }

    fun addToPlayList(songList: ArrayList<Song>){
        playlist?.addItems(songList)
    }

    fun addToPlayList(song: Song){
        playlist?.addItem(song)
    }

    fun setRepeat(isRepeat: Boolean){
        playlist?.isRepeat = isRepeat
    }

    fun isRepeat(): Boolean{
        return playlist?.isRepeat ?: false
    }

    fun repeat(): Boolean{
        if(playlist?.isRepeat == true){
            setCurrentPlayListIndex(mCurrentIndex)
            return true
        }
        return false
    }

    fun setShuffle(isShuffle: Boolean){
        playlist?.isShuffle = isShuffle
    }

    fun setRepeatAll(isRepeatAll: Boolean){
        playlist?.isRepeatAll = isRepeatAll
    }

    fun getRandomIndex(list: List<Song>) = Random().nextInt(list.size)

    fun equals(list1: List<Song>?, list2: List<Song>?): Boolean{
        if(list1 == list2){
            return true
        }
        if(list1 == null || list2 == null){
            return false
        }
        if(list1.size != list2.size){
            return false
        }
        for(i in list1.indices){
            if(list1[i].songId != list2[i].songId){
                return false
            }
        }
        return true
    }
    interface OnSongUpdateListener{
        fun onSongChanged(song: Song)

        fun onSongRetrieverError()
    }
}