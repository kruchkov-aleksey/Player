package com.example.music.util

import android.content.Context
import android.view.GestureDetector
import android.view.MotionEvent
import android.view.View
import java.lang.Exception
import java.lang.Math.abs

abstract class OnSwipeTouchListener(context: Context?): View.OnTouchListener {

    private val gestureDetector: GestureDetector

    init {
        gestureDetector = GestureDetector(context, GestureListener())
    }

    override fun onTouch(v: View?, event: MotionEvent?): Boolean {
        return gestureDetector.onTouchEvent(event)
    }

    private inner class GestureListener: GestureDetector.SimpleOnGestureListener(){
        override fun onDown(e: MotionEvent?): Boolean {
            return true
        }

        override fun onFling(
            e1: MotionEvent,
            e2: MotionEvent,
            velocityX: Float,
            velocityY: Float
        ): Boolean {
            var result = false
            try{
                val diffY = e2.y - e1.y
                val diffX = e2.x - e1.x
                if(abs(diffX) > SWIPE_THRESHOLD && abs(velocityX) > SWIPE_VELOCITY_THRESHOLD){
                    if(diffX > 0){
                        onSwipeRight()
                    }else{
                        onSwipeLeft()
                    }
                    result = true
                }
            } catch (e: Exception){
                e.printStackTrace()
            }
            return result
        }
    }

    companion object {
        private const val SWIPE_THRESHOLD = 100
        private const val SWIPE_VELOCITY_THRESHOLD = 100
    }

    abstract fun onSwipeRight()
    abstract fun onSwipeLeft()
}