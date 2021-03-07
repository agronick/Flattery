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
        if (!hidden && lastCircle != null) {
            val radius =
                if (lastCircle !== null) (lastCircle!!.r).coerceAtLeast(10f) else size.toFloat()
            draw(canvas, radius, lastCircle!!.c.x, lastCircle!!.c.y)
        }
    }

    fun prepare(faceCircle: Circle) {
        lastCircle = getCircle(faceCircle)
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
            val arcMidpoint = getArcMidpoint(intersects) ?: return null
            var opposite = getPointClosestToCenter(faceCircle, appCircle)
            return Circle(
                arcMidpoint.midpoint(opposite),
                arcMidpoint.distance(opposite) * 0.5f
            )
        }
        return appCircle
    }

    private fun getArcMidpoint(intersection: CircleCircleIntersection): Vector2? {
        return when (intersection.type.intersectionPointCount) {
            1 -> intersection.intersectionPoint1
            2 -> {
                val AB = intersection.intersectionPoint2.sub(intersection.intersectionPoint1)
                val lAB = kotlin.math.sqrt(AB.x * AB.x + AB.y * AB.y)
                val uAB = Vector2(AB.x / lAB, AB.y / lAB)
                val mAB = intersection.intersectionPoint1.add(intersection.intersectionPoint2).div(
                    2f
                )
                val R = intersection.c1.r
                val F = R - kotlin.math.sqrt(R * R - lAB * lAB / 4)
                Vector2(mAB.x - uAB.y * F, mAB.y + uAB.x * F)
            }
            else -> null
        }
    }

    private fun getPointClosestToCenter(faceCircle: Circle, appCircle: Circle): Vector2 {
        val vX = faceCircle.c.x - appCircle.c.x
        val vY = faceCircle.c.y - appCircle.c.y
        val magV: Double = kotlin.math.sqrt((vX * vX + vY * vY).toDouble())
        val aX = appCircle.c.x + vX / magV * appCircle.r
        val aY = appCircle.c.y + vY / magV * appCircle.r
        return Vector2(aX.toFloat(), aY.toFloat())
    }

    fun intersects(x: Float, y: Float): Boolean {
        val circle = lastCircle
        if (circle != null) {
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
        return false
    }

}