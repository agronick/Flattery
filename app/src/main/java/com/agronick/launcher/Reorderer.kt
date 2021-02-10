package com.agronick.launcher

import android.animation.AnimatorSet
import android.animation.ValueAnimator
import android.graphics.Canvas
import androidx.core.animation.doOnEnd

class Reorderer(private val container: Container, val invalidate: () -> Unit) {
    private var activeAppCopy: App? = null
    private var lastOverlap: App? = null
    private var suppressedAppCopy: App? = null
    private var lastPosition = HashMap<App, Pair<Float, Float>>()
    private var lastFreeSpace = Pair<Float, Float>(0f, 0f)

    fun onHoldDown(x: Float, y: Float): Boolean {
        val app = container.getAppAtPoint(x, y)
        if (app != null) {
            activeAppCopy = app.copy()
            suppressedAppCopy = app
            lastOverlap = suppressedAppCopy
            suppressedAppCopy!!.hidden = true
            lastPosition[app] = Pair(app.left, app.top)
            lastFreeSpace = Pair(app.left, app.top)
            ValueAnimator.ofInt(activeAppCopy!!.size, (activeAppCopy!!.size * 1.4).toInt())
                .apply {
                    duration = StaticValues.durationRise
                    addUpdateListener { animator ->
                        activeAppCopy!!.size = animator.animatedValue as Int
                        invalidate()
                    }
                }.start()
            return true
        }
        return false
    }

    fun onMove(x: Float, y: Float) {
        activeAppCopy!!.left = x
        activeAppCopy!!.top = y
        val app = container.getAppAtPoint(x, y)
        if (app !== null && activeAppCopy !== null && app != activeAppCopy && app != lastOverlap) {
            lastPosition[app] = Pair(app.left, app.top)
            val positions = lastFreeSpace
            lastFreeSpace = Pair(app.left, app.top)
            animateAppPosition(app, positions.first, positions.second)
            lastOverlap = app
        }
    }

    fun animateAppPosition(app: App, x: Float, y: Float) {
        val xAnim = ValueAnimator.ofFloat(app.left, x).apply {
            duration = StaticValues.durationSwap
            addUpdateListener { animator ->
                app.left = animator.animatedValue as Float
                invalidate()
            }
        }
        val yAnim = ValueAnimator.ofFloat(app.top, y).apply {
            duration = StaticValues.durationSwap
            addUpdateListener { animator ->
                app.top = animator.animatedValue as Float
            }
        }
        AnimatorSet().apply {
            duration = StaticValues.durationOpen
            playTogether(xAnim, yAnim)
            doOnEnd {
                lastOverlap = null
            }
            start()
        }
    }

    fun draw(canvas: Canvas) {
        if (activeAppCopy !== null) {
            activeAppCopy!!.drawNormal(canvas)
        }
    }
}