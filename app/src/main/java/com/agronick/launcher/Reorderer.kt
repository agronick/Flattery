package com.agronick.launcher

import android.animation.AnimatorSet
import android.animation.ValueAnimator
import android.graphics.Canvas
import androidx.core.animation.doOnEnd

class Reorderer(private val container: Container, app: App, val invalidate: () -> Unit) {
    private var activeAppCopy: App = app.copy()
    private var lastOverlap: App? = app
    private var suppressedAppCopy: App = app
    private var lastPosition = HashSet<App>()
    private var lastFreeSpace = Pair(app.left, app.top)

    init {
        lastPosition.add(app)
        suppressedAppCopy.hidden = true
        ValueAnimator.ofInt(activeAppCopy.size, (activeAppCopy.size * 1.4).toInt())
            .apply {
                duration = StaticValues.durationRise
                addUpdateListener { animator ->
                    activeAppCopy.size = animator.animatedValue as Int
                    invalidate()
                }
            }.start()
    }

    fun onMove(x: Float, y: Float) {
        activeAppCopy.left = x
        activeAppCopy.top = y
        val app = container.getAppAtPoint(x, y, lastPosition)
        if (app !== null) {
            lastPosition.add(app)
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
                lastPosition.remove(app)
            }
            start()
        }
    }

    fun onStopReorder() {
        suppressedAppCopy.left = lastFreeSpace.first
        suppressedAppCopy.top = lastFreeSpace.second
        suppressedAppCopy.hidden = false
    }

    fun draw(canvas: Canvas) {
        activeAppCopy.drawNormal(canvas)
    }
}