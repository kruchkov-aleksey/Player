package com.example.music.playlist

import com.example.music.model.Song

class Playlist {
    private var list: MutableList<Song> = ArrayList()
    private var shuffleList: MutableList<Song> = ArrayList()
    var isShuffle = false
    var isRepeat = false
    var isRepeatAll = false

    fun getShuffleOrNormalList(): MutableList<Song>{
        return if(isShuffle) shuffleList else list
    }

    fun getCurrentPlayListSize():Int = getShuffleOrNormalList().size

    fun setList(list: MutableList<Song>): Playlist{
        clearList()
        this.list = list
        list.shuffle()
        this.shuffleList = ArrayList(list)
        return this
    }

    fun addItems(songList: ArrayList<Song>){
        this.list.addAll(songList)
        songList.shuffle()
        this.shuffleList.addAll(songList)
    }

    fun addItem(song:Song){
        this.list.add(song)
        this.shuffleList.add(song)
    }
    fun getItem(index:Int):Song?{
        if(index >=  getCurrentPlayListSize()) return null
        return getShuffleOrNormalList()[index]
    }

    private fun clearList(){
        this.list.clear()
        this.shuffleList.clear()
    }
}