package com.agronick.launcher

import android.animation.AnimatorSet
import android.animation.ValueAnimator
import android.content.Context
import android.graphics.Canvas
import android.view.MotionEvent
import android.view.View
import androidx.core.animation.doOnEnd
import timber.log.Timber

class MainView(context: Context, appList: List<PInfo>) : View(context) {
    init {
        Runnable {
            container = Container(appList)
        }.run()
    }

    var onPackageClick: ((PInfo) -> Unit)? = null

    private lateinit var container: Container
    private var offsetLeft = 0f
    private var offsetTop = 0f

    private var previousX: Float = 0f
    private var previousY: Float = 0f
    private var hasMoved = false
    private var canvasSize: Float = 0f

    private var openingApp: App? = null

    override fun onTouchEvent(event: MotionEvent?): Boolean {
        if (event != null) {
            when (event.action) {
                MotionEvent.ACTION_DOWN -> {
                    previousX = event.x
                    previousY = event.y
                    hasMoved = false
                    return true
                }
                MotionEvent.ACTION_MOVE -> {
                    val newOffsetLeft = offsetLeft + event.x - previousX
                    val newOffsetTop = offsetTop + event.y - previousY
                    val limited = container.getLimit(newOffsetLeft, newOffsetTop, canvasSize)
                    offsetLeft = limited.first
                    offsetTop = limited.second
                    if (newOffsetLeft == limited.first) {
                        previousX = event.x
                    }
                    if (newOffsetTop == limited.second) {
                        previousY = event.y
                    }
                    hasMoved = true
                    prepareInvalidate()
                    return true
                }
                MotionEvent.ACTION_UP -> {
                    if (!hasMoved) {
                        handleClick(event.x, event.y)
                    }
                }
            }
        }
        return super.onTouchEvent(event)
    }

    fun handleClick(x: Float, y: Float) {
        val offset = getOffset()
        if (offset != null) {
            val app = container.getClickedPackage(x - offset.first, y - offset.second)
            if (app != null) {
                setupOpenAnim(app.asOpenAnimator(canvasSize.toInt()))
            }
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
            ValueAnimator.ofFloat(app.size.toFloat(), canvasSize)
                .apply {
                    addUpdateListener { animator ->
                        app.size = ((animator.animatedValue as Float).toInt())
                        Timber.d("At size ${app.size}")
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

    fun getOffset(): Pair<Float, Float>? {
        val halfSize = canvasSize * 0.5f
        return Pair(halfSize + offsetLeft, halfSize + offsetTop)
    }

    fun prepareInvalidate() {
        if (canvasSize == 0f) return
        Runnable {
            container.prepare(offsetLeft, offsetTop, canvasSize)
            invalidate()
        }.run()
    }

    override fun onDraw(canvas: Canvas) {
        Timber.d(
            "draw called $offsetLeft $offsetTop"
        )
        val offset = getOffset()
        if (offset != null) {
            canvas.translate(offset.first, offset.second)
            container.draw(canvas)
            openingApp?.drawNormal(canvas)
        }
    }
}