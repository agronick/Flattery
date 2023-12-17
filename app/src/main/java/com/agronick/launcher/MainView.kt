package com.agronick.launcher

import android.animation.AnimatorSet
import android.animation.ValueAnimator
import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Canvas
import android.view.HapticFeedbackConstants
import android.view.MotionEvent
import android.view.View
import androidx.core.animation.doOnEnd
import timber.log.Timber
import util.geometry.Vector2
import java.util.Timer
import java.util.TimerTask

@SuppressLint("ViewConstructor")
class MainView(context: Context, appList: List<PInfo>) : View(context) {
    private var edgeTimer: Timer? = null
    private var density: Float = context.resources.displayMetrics.density
    var onPackageClick: ((PInfo) -> Unit)? = null

    private lateinit var container: Container
    var offsetLeft = 0f
    var offsetTop = 0f
    private var canvasSize: Float = 0f
    private var edgeLimit = 100000f
    var allHidden = false

    private val STATE_NONE = 0
    private val STATE_REORDERING = 1
    private val STATE_OPENING = 2

    private fun getActiveState(): Int {
        if (reorderer != null) return STATE_REORDERING
        if (openingApp != null) return STATE_OPENING
        return STATE_NONE
    }

    var openingApp: App? = null

    init {
        Runnable {
            val appListProvider = AppListProvider(appList, context)
            appListProvider.load()
            container = Container(appListProvider, density)
        }.run()
    }

    var reorderer: Reorderer? = null

    fun resetReorderEdgeTimer() {
        edgeTimer?.cancel()
    }

    fun reorderAtEdge(newOffsets: Vector2) {
        resetReorderEdgeTimer()
        edgeTimer = Timer()
        edgeTimer?.schedule(object : TimerTask() {
            override fun run() {
                offsetLeft += newOffsets.x
                offsetTop += newOffsets.y
                val curReorderer = reorderer
                if (curReorderer !== null) {
                    val appPos = curReorderer.getAppPos()
                    post {
                        curReorderer.onMove(
                            Vector2(
                                appPos.x - newOffsets.x,
                                appPos.y - newOffsets.y
                            )
                        )
                    }
                }
                prepareInvalidate()
            }
        }, 0, 33)
    }

    fun handleLongPress(event: MotionEvent) {
        val state = getActiveState()
        if (state == STATE_OPENING) return
        val offset = getRelativePosition(Pair(event.x, event.y))
        if (state == STATE_REORDERING) {
            if (event.action == MotionEvent.ACTION_UP) {
                reorderer!!.onStopReorder(
                    when (container.getLimit(offsetLeft, offsetTop, canvasSize)) {
                        Pair(offsetLeft, offsetTop) -> {
                            Timber.i("Looking up what is at ${offset.x} ${offset.y} to end reorder")
                            container.getAppAtPoint(
                                Vector2(
                                    offset.x,
                                    offset.y
                                ),
                                reorderer!!.lastPosition
                            )?.also {
                                Timber.i("Found ${it.pkgInfo.pname} at ${it.left} ${it.top} to swap with")
                            }
                        }

                        else -> null
                    }
                )
                reorderer = null
                resetReorderEdgeTimer()
            } else {
                reorderer!!.onMove(offset)
                val newOffsets =
                    reorderer!!.checkAtEdge(offset, container.lastCircle, density * 0.3f)
                if (newOffsets != null) {
                    reorderAtEdge(newOffsets.scale(density * 3))
                } else {
                    resetReorderEdgeTimer()
                }
                prepareInvalidate()
            }
        } else if (state == STATE_NONE) {
            Timber.i("Looking up what is at ${offset.x} ${offset.y} to start reorder")
            container.getAppAtPoint(Vector2(offset.x, offset.y))?.let {
                Timber.i("Going to recorder ${it.pkgInfo.pname} at ${it.left} ${it.top}")
                post {
                    reorderer = Reorderer(container, it, ::prepareInvalidate)
                    performHapticFeedback(HapticFeedbackConstants.CONTEXT_CLICK)
                }
            }
        }
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
        if (getActiveState() != STATE_NONE) return
        val offset = getRelativePosition(Pair(x, y))
        val app = container.getAppAtPoint(offset)
        if (app != null && app.pkgInfo.activityName != null) {
            setupOpenAnim(app.copy())
        }
    }

    fun setupOpenAnim(app: App) {
        if (getActiveState() != STATE_NONE) return

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
                    openingApp = null
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
        return point?.let {
            Vector2(it.first - pos.x, it.second - pos.y)
        } ?: pos
    }

    fun prepareInvalidate() {
        if (canvasSize == 0f) return
        Runnable {
            container.prepare(offsetLeft, offsetTop, canvasSize)
            reorderer?.prepare()
            invalidate()
        }.run()
    }

    override fun onDraw(canvas: Canvas) {
        if (allHidden) return
        val offset = getRelativePosition()
        canvas.translate(offset.x, offset.y)
        container.draw(canvas)
        openingApp?.drawNormal(canvas)
    }
}