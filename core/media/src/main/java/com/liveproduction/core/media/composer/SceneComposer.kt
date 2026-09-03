package com.liveproduction.core.media.composer

import android.util.Log

class SceneComposer {

    data class ViewportRect(
        val x: Int,
        val y: Int,
        val width: Int,
        val height: Int
    )

    fun calculateLayout(
        mode: SceneMode,
        canvasWidth: Int,
        canvasHeight: Int
    ): Pair<ViewportRect, ViewportRect?> {
        Log.i(TAG, "Calculating SceneComposer layout for mode: $mode ($canvasWidth x $canvasHeight)")
        return when (mode) {
            SceneMode.HDMI_FULLSCREEN, SceneMode.FRONT_CAM_FULLSCREEN, SceneMode.REAR_CAM_FULLSCREEN -> {
                val primary = ViewportRect(0, 0, canvasWidth, canvasHeight)
                Pair(primary, null)
            }
            SceneMode.PICTURE_IN_PICTURE -> {
                val primary = ViewportRect(0, 0, canvasWidth, canvasHeight)
                // PiP inset window in top-right corner (30% width, 30% height)
                val pipWidth = (canvasWidth * 0.30f).toInt()
                val pipHeight = (canvasHeight * 0.30f).toInt()
                val pipX = canvasWidth - pipWidth - 24
                val pipY = 24
                val inset = ViewportRect(pipX, pipY, pipWidth, pipHeight)
                Pair(primary, inset)
            }
            SceneMode.SPLIT_SCREEN -> {
                val halfWidth = canvasWidth / 2
                val left = ViewportRect(0, 0, halfWidth, canvasHeight)
                val right = ViewportRect(halfWidth, 0, halfWidth, canvasHeight)
                Pair(left, right)
            }
        }
    }

    companion object {
        private const val TAG = "SceneComposer"
    }
}
