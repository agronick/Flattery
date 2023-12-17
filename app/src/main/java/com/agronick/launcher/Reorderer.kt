package com.agronick.launcher

import android.animation.AnimatorSet
import android.animation.ValueAnimator
import androidx.core.animation.doOnEnd
import timber.log.Timber
import util.geometry.Circle
import util.geometry.Vector2

class Reorderer(
    private val container: Container,
    private var app: App,
    val invalidate: () -> Unit,
) {
    private var suppressedAppCopy: App = app.copy()
    var lastPosition = HashSet<App>()
    private val defaultCircleSize = container.appCircleSize
    private val appList = container.appList

    init {
        app.drawLast = true
        lastPosition.add(app)
        suppressedAppCopy.hidden = true
        ValueAnimator.ofInt(app.size, (app.size * 1.4).toInt())
            .apply {
                duration = StaticValues.durationRise
                addUpdateListener { animator ->
                    app.size = animator.animatedValue as Int
                    invalidate()
                }
            }.start()
    }

    fun onMove(position: Vector2) {
        app.left = position.x
        app.top = position.y
    }

    fun getAppPos(): Vector2 {
        return Vector2(app.left, app.top)
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
                app.drawLast = false
            }
            start()
        }
    }

    fun onStopReorder(overApp: App?) {
        if (overApp == null) {
            animateAppPosition(app, suppressedAppCopy.left, suppressedAppCopy.top)
        } else {
            Timber.i("Sending ${overApp} to ${suppressedAppCopy.left} ${suppressedAppCopy.top}")
            animateAppPosition(overApp, suppressedAppCopy.left, suppressedAppCopy.top)
            Timber.i("Sending ${app} to ${overApp.left} ${overApp.top}")
            animateAppPosition(app, overApp.left, overApp.top)
            appList.swap(app, overApp)
            appList.save()
        }
        ValueAnimator.ofInt((suppressedAppCopy.size * 1.4).toInt(), defaultCircleSize)
            .apply {
                duration = StaticValues.durationRise
                addUpdateListener { animator ->
                    app.size = animator.animatedValue as Int
                }
            }.start()
    }

    fun prepare() {
        container.lastCircle?.let { app.prepare(it, false) }
    }

    fun checkAtEdge(offsetVector: Vector2, lastCircle: Circle?, density: Float): Vector2? {
        if (lastCircle == null) return null
        val maxDistance = lastCircle.r * density
        if (offsetVector.distance(lastCircle.c) >= maxDistance) {
            val angle = Math.toRadians(lastCircle.c.angleBetween(offsetVector)).toFloat()
            return Vector2(kotlin.math.sin(angle) * -2, kotlin.math.cos(angle) * 2)
        }
        return null
    }
}