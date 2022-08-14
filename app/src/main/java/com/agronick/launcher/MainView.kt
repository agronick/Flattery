package com.agronick.launcher

import android.animation.AnimatorSet
import android.animation.ValueAnimator
import android.content.Context
import android.graphics.Canvas
import android.view.HapticFeedbackConstants
import android.view.MotionEvent
import android.view.View
import androidx.core.animation.doOnEnd
import util.geometry.Vector2
import java.util.*

class MainView(context: Context, appList: List<PInfo>) : View(context) {
    private var density: Float = context.resources.displayMetrics.density
    var onPackageClick: ((PInfo) -> Unit)? = null

    private lateinit var container: Container
    var offsetLeft = 0f
    var offsetTop = 0f
    private var canvasSize: Float = 0f
    private var edgeLimit = 100000f
    var allHidden = false

    var openingApp: App? = null

    init {
        Runnable {
            container = Container(appList, density)
        }.run()
    }

    var edgeTimer = Timer()
    val resetEdge: () -> Unit = {
        edgeTimer.cancel()
        edgeTimer = Timer()
    }
    var reorderer: Reorderer? = null

    fun handleLongPress(event: MotionEvent) {
        val offset = getRelativePosition(Pair(event.x, event.y))
        if (reorderer != null) {
            if (event.action == MotionEvent.ACTION_UP) {
                reorderer!!.onStopReorder(container.getAppAtPoint(Vector2(offset.x, offset.y)))
                reorderer = null
            } else {
                reorderer!!.onMove(offset)
                val newOffsets =
                    reorderer!!.checkAtEdge(offset, container.lastCircle, container.appCircleSize)
                if (newOffsets != null) {
                    offsetLeft += newOffsets.x
                    offsetTop += newOffsets.y
                }
                prepareInvalidate()
            }
        } else {
            val app = container.getAppAtPoint(Vector2(offset.x, offset.y))
            if (app != null) {
                post {
                    reorderer = Reorderer(container, app, ::prepareInvalidate)
                    performHapticFeedback(HapticFeedbackConstants.CONTEXT_CLICK)
                }
            }
        }
    }

    override fun onTouchEvent(event: MotionEvent?): Boolean {
        if (event != null) {
            when (event.action) {
                MotionEvent.ACTION_MOVE -> {
                    resetEdge()
                    return true
                }
            }
        }
        return super.onTouchEvent(event)
    }

    fun checkOverPanLimit() {
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
        val app = container.getAppAtPoint(offset)
        if (app != null && app.pkgInfo.activityName != null) {
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
                        container.lastCircle?.let { app.prepare(it, false) }
                        invalidate()
                    }
                }

        AnimatorSet().apply {
            duration = 400
            playTogether(xAnimator, yAnimator, radiusAnimator)
            doOnEnd {
                postDelayed({
                    onPackageClick?.let { it1 -> it1(app.pkgInfo) }
                }, 200)
            }
            start()
        }
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        canvasSize = w.coerceAtMost(h).toFloat()
        edgeLimit = canvasSize * 0.9f
        container.prepare(offsetLeft, offsetTop, w.toFloat())
        super.onSizeChanged(w, h, oldw, oldh)
    }

    fun getRelativePosition(point: Pair<Float, Float>? = null): Vector2 {
        val halfSize = canvasSize * 0.5f
        val pos = Vector2(halfSize + offsetLeft, halfSize + offsetTop)
        if (point != null) {
            return Vector2(point.first - pos.x, point.second - pos.y)
        }
        return pos
    }

    fun prepareInvalidate() {
        if (canvasSize == 0f) return
        Runnable {
            container.prepare(offsetLeft, offsetTop, canvasSize)
            reorderer?.prepare()
            invalidate()
        }.run()
    }

    override fun onDraw(canvas: Canvas?) {
        if (canvas == null || allHidden) return
        Runnable {
            val offset = getRelativePosition()
            canvas.translate(offset.x, offset.y)
            container.draw(canvas)
            openingApp?.drawNormal(canvas)
        }.run()
    }
}