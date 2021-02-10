package com.agronick.launcher

import android.graphics.Canvas
import android.graphics.Rect
import util.geometry.Circle
import util.geometry.CircleCircleIntersection
import util.geometry.Vector2

class App(val pkgInfo: PInfo, var size: Int) {
    var left = 0.0f
    var top = 0.0f
    var hidden = false
    private var lastCircle: Circle? = null

    fun copy(): App {
        val other = App(pkgInfo, size)
        other.left = left
        other.top = top
        return other
    }

    fun drawNormal(canvas: Canvas) {
        if (!hidden) {
            val radius =
                if (lastCircle !== null) (lastCircle!!.r).coerceAtLeast(10f) else size.toFloat()
            draw(canvas, radius, left, top)
        }
    }

    fun prepare(faceCircle: Circle) {
        lastCircle = getCircle(faceCircle) ?: return
    }

    fun draw(canvas: Canvas, radius: Float, x: Float, y: Float) {
        pkgInfo.icon.bounds = Rect(
            (x - radius).toInt(),
            (y - radius).toInt(),
            (x + radius).toInt(),
            (y + radius).toInt()
        )
        pkgInfo.icon.draw(canvas)
    }

    fun getCircle(faceCircle: Circle): Circle? {
        val startSize = size
        val appCircle = Circle(
            Vector2(
                left,
                top
            ), startSize.toFloat()
        )
        val intersects =
            CircleCircleIntersection(faceCircle, appCircle)
        if (!intersects.type.isContained) {
            if (intersects.type.intersectionPointCount == 0) return null
            for (i in startSize.toLong() downTo 1) {
                val testCircle = Circle(
                    Vector2(
                        left,
                        top
                    ), i.toFloat()
                )
                val intersector = CircleCircleIntersection(
                    faceCircle,
                    testCircle
                )
                if (intersector.type.intersectionPointCount == 0) {
                    return testCircle
                }
            }
            return null
        }
        return appCircle
    }

    fun intersects(x: Float, y: Float): Boolean {
        if (null == lastCircle) return false
        val circle = getCircle(lastCircle!!)
        val point = Circle(
            Vector2(
                x,
                y
            ), 3.0f
        )
        return CircleCircleIntersection(
            circle,
            point
        ).type == CircleCircleIntersection.Type.ECCENTRIC_CONTAINED
    }

}