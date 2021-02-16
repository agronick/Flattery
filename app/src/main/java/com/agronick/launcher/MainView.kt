package com.agronick.launcher

import android.animation.AnimatorSet
import android.animation.ValueAnimator
import android.content.Context
import android.graphics.Canvas
import android.util.Log
import android.view.MotionEvent
import android.view.View
import androidx.core.animation.doOnEnd
import java.util.*

class MainView(context: Context, appList: List<PInfo>) : View(context) {
    private var density: Float
    var onPackageClick: ((PInfo) -> Unit)? = null

    private lateinit var container: Container
    var offsetLeft = 0f
    var offsetTop = 0f
    private var previousX: Float = 0f
    private var previousY: Float = 0f
    private var hasMoved = false
    private var canvasSize: Float = 0f

    private var openingApp: App? = null

    init {
        density = context.resources.displayMetrics.density
        Runnable {
            container = Container(appList, density)
        }.run()
    }

    var holdTimer = Timer()
    var resetHold: () -> Unit = {
        reorderer = null
        holdTimer.cancel()
        holdTimer = Timer()
    }
    var reorderer: Reorderer? = null

    override fun onTouchEvent(event: MotionEvent?): Boolean {
        if (event != null) {
            when (event.action) {
                MotionEvent.ACTION_DOWN -> {
                    previousX = event.x
                    previousY = event.y
                    hasMoved = false
                    holdTimer.schedule(object : TimerTask() {
                        override fun run() {
                            val offset = getRelativePosition(Pair(event.x, event.y))
                            val app = container.getAppAtPoint(offset.first, offset.second)
                            if (app != null) {
                                post {
                                    reorderer = Reorderer(container, app, ::prepareInvalidate)
                                }
                            }
                        }
                    }, 3000)
                    return true
                }
                MotionEvent.ACTION_MOVE -> {
                    hasMoved = true
                    holdTimer.cancel()
                    if (reorderer == null) {
                        offsetLeft += event.x - previousX
                        offsetTop += event.y - previousY
                        previousX = event.x
                        previousY = event.y
                    } else {
                        val offset = getRelativePosition(Pair(event.x, event.y))
                        reorderer!!.onMove(offset.first, offset.second)
                    }
                    prepareInvalidate()
                    return true
                }
                MotionEvent.ACTION_UP -> {
                    if (reorderer != null) {
                        reorderer!!.onStopReorder()
                        invalidate()
                    }
                    resetHold()
                    if (!hasMoved) {
                        handleClick(event.x, event.y)
                    } else {
                        checkOverLimit()
                    }
                }
            }
        }
        return super.onTouchEvent(event)
    }

    fun checkOverLimit() {
        val limited = container.getLimit(offsetLeft, offsetTop, canvasSize)
        val animators = mutableListOf<ValueAnimator>()
        if (offsetLeft != limited.first) {
            animators.add(ValueAnimator.ofFloat(offsetLeft, limited.first)
                .apply {
                    addUpdateListener { animator ->
                        offsetLeft = animator.animatedValue as Float
                    }
                }
            )
        }
        if (offsetTop != limited.second) {
            animators.add(ValueAnimator.ofFloat(offsetTop, limited.second)
                .apply {
                    addUpdateListener { animator ->
                        offsetTop = animator.animatedValue as Float
                    }
                }
            )
        }
        if (animators.isNotEmpty()) {
            animators[0].addUpdateListener { prepareInvalidate() }
            AnimatorSet().apply {
                duration = StaticValues.durationOpen
                playTogether(*animators.toTypedArray())
                start()
            }
        }
    }

    fun handleClick(x: Float, y: Float) {
        val offset = getRelativePosition(Pair(x, y))
        val app = container.getAppAtPoint(offset.first, offset.second)
        if (app != null) {
            setupOpenAnim(app.copy())
        }
    }

    fun setupOpenAnim(app: App) {
        val face = container.lastCircle ?: return
        openingApp = app

        val xAnimator = ValueAnimator.ofFloat(app.left, face.c.x)
            .apply {
                addUpdateListener { animator ->
                    app.left = animator.animatedValue as Float
                }
            }
        val yAnimator = ValueAnimator.ofFloat(app.top, face.c.y)
            .apply {
                addUpdateListener { animator ->
                    app.top = animator.animatedValue as Float
                }
            }
        val radiusAnimator =
            ValueAnimator.ofFloat(app.size.toFloat(), canvasSize * 0.5f)
                .apply {
                    addUpdateListener { animator ->
                        app.size = ((animator.animatedValue as Float).toInt())
                        Log.d(TAG, "At size ${app.size}")
                        container.lastCircle?.let { app.prepare(it) }
                        invalidate()
                    }
                }

        AnimatorSet().apply {
            duration = 400
            playTogether(xAnimator, yAnimator, radiusAnimator)
            doOnEnd {
                postDelayed({
                    openingApp = null
                    onPackageClick?.let { it1 -> it1(app.pkgInfo) }
                }, 200)
            }
            start()
        }
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        canvasSize = Math.min(w, h).toFloat()
        container.prepare(offsetLeft, offsetTop, w.toFloat())
        super.onSizeChanged(w, h, oldw, oldh)
    }

    fun getRelativePosition(point: Pair<Float, Float>? = null): Pair<Float, Float> {
        val halfSize = canvasSize * 0.5f
        val pos = Pair(halfSize + offsetLeft, halfSize + offsetTop)
        if (point != null) {
            return Pair(point.first - pos.first, point.second - pos.second)
        }
        return pos
    }

    fun prepareInvalidate() {
        if (canvasSize == 0f) return
        Runnable {
            container.prepare(offsetLeft, offsetTop, canvasSize)
            invalidate()
        }.run()
    }

    override fun onDraw(canvas: Canvas?) {
        if (canvas == null) return
        Runnable {
            val offset = getRelativePosition()
            canvas.translate(offset.first, offset.second)
            container.draw(canvas)
            openingApp?.drawNormal(canvas)
            reorderer?.draw(canvas)
        }.run()
    }
}