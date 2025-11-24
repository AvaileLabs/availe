package com.availelabs.availe.ui

class RefreshStateHolder {
    private var currentPullDistance: Float = 0f
    private val triggerThreshold: Float = 160f
    private val maxPullDistance: Float = 240f

    val pullProgress: Float
        get() = (currentPullDistance / triggerThreshold).coerceIn(0f, 1f)

    fun onPull(delta: Float): Float {
        val newDistance = (currentPullDistance + delta * 0.5f).coerceIn(0f, maxPullDistance)
        val consumed = (newDistance - currentPullDistance) / 0.5f
        currentPullDistance = newDistance
        return consumed
    }

    fun onRelease(): Boolean {
        val shouldTrigger = currentPullDistance >= triggerThreshold
        currentPullDistance = 0f
        return shouldTrigger
    }
}