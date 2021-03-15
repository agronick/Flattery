package com.agronick.launcher

import android.animation.AnimatorSet
import android.animation.ValueAnimator
import android.graphics.Canvas
import androidx.core.animation.doOnEnd
import util.geometry.Circle
import util.geometry.Vector2

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

    fun onMove(position: Vector2) {
        activeAppCopy.left = position.x
        activeAppCopy.top = position.y
        val app = container.getAppAtPoint(position, lastPosition)
        if (app !== null) {
            lastPosition.add(app)
            val positions = lastFreeSpace
            lastFreeSpace = Pair(app.left, app.top)
            animateAppPosition(app, positions.first, positions.second)
            lastOverlap = app
        }
    }

    fun getAppPos(): Vector2 {
        return Vector2(activeAppCopy.left, activeAppCopy.top)
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
        container.lastCircle?.let { suppressedAppCopy.prepare(it) }
    }

    fun prepare() {
        container.lastCircle?.let { activeAppCopy.prepare(it, false) }
    }

    fun draw(canvas: Canvas) {
        activeAppCopy.drawNormal(canvas)
    }

    fun checkAtEdge(offsetVector: Vector2, lastCircle: Circle?): Vector2? {
        if (lastCircle == null) return null
        val maxDistance = lastCircle.r * 0.9
        if (offsetVector.distance(lastCircle.c) >= maxDistance) {
            val angle = Math.toRadians(lastCircle.c.angleBetween(offsetVector)).toFloat()
            return Vector2(kotlin.math.sin(angle) * -1, kotlin.math.cos(angle))
        }
        return null
    }
}