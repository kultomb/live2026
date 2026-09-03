package com.liveproduction.core.usb.buffer

import android.util.Log
import com.liveproduction.core.media.model.VideoFrame
import java.util.concurrent.ArrayBlockingQueue
import java.util.concurrent.atomic.AtomicLong

class UvcRingBuffer(private val capacity: Int = 4) {

    private val queue = ArrayBlockingQueue<VideoFrame>(capacity)
    private val totalPushedFrames = AtomicLong(0)
    private val totalDroppedFrames = AtomicLong(0)

    fun push(frame: VideoFrame): Boolean {
        totalPushedFrames.incrementAndGet()
        // Producer/consumer rule: If buffer is full, drop oldest unread frame to preserve low latency
        if (queue.size >= capacity) {
            val dropped = queue.poll()
            if (dropped != null) {
                totalDroppedFrames.incrementAndGet()
                Log.w(TAG, "RingBuffer overflow (capacity $capacity). Dropped oldest frame. Total dropped: ${totalDroppedFrames.get()}")
            }
        }
        return queue.offer(frame)
    }

    fun poll(): VideoFrame? {
        return queue.poll()
    }

    fun clear() {
        queue.clear()
    }

    fun getDroppedCount(): Long = totalDroppedFrames.get()

    companion object {
        private const val TAG = "UvcRingBuffer"
    }
}
