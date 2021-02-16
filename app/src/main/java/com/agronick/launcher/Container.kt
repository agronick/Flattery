package com.agronick.launcher

import android.graphics.Canvas
import util.geometry.Circle
import util.geometry.Vector2
import kotlin.math.ceil
import kotlin.math.roundToInt
import kotlin.math.sqrt

class Container(appList: List<PInfo>, density: Float) {
    private val rows: List<List<App>>
    val size = (StaticValues.normalAppSize * density).roundToInt()
    val margin = (StaticValues.margin * density).roundToInt()
    private val iterate: Sequence<Triple<App, Int, Int>>
    var lastCircle: Circle? = null

    var topLimit = 0f
    var bottomLimit = 0f
    var leftLimit = 0f
    var rightLimit = 0f

    private var equalizerOffset = 1.1f

    init {
        val squareSize = Math.ceil(sqrt(appList.size.toFloat()).toDouble()).toInt()
        val appIter = appList.iterator()
        rows = 1.rangeTo(squareSize).mapNotNull outer@{
            val cols = 1.rangeTo(squareSize).mapNotNull inner@{
                if (appIter.hasNext()) {
                    return@inner App(
                        appIter.next(),
                        size
                    )
                }
                return@inner null
            }
            return@outer if (cols.isNotEmpty()) cols else null
        }
        iterate = sequence {
            val iterator = this@Container.rows.iterator()
            var rowCount = 0
            while (iterator.hasNext()) {
                val row = iterator.next()
                val colIterator = row.iterator()
                var colCount = 0
                while (colIterator.hasNext()) {
                    val app = colIterator.next()
                    yield(Triple(app, rowCount, colCount))
                    colCount++
                }
                rowCount += 1
            }
        }
        position()
    }


    private fun position() {
        iterate.forEach {
            val app = it.first
            val positions = calcPositions(it.second, it.third)
            app.left = positions.first
            app.top = positions.second

            if (app.left < leftLimit) {
                leftLimit = app.left
            } else if (app.left > rightLimit) {
                rightLimit = app.left
            }
            if (app.top > topLimit) {
                topLimit = app.top
            } else if (app.top < bottomLimit) {
                bottomLimit = app.top
            }
        }
        val halfSize = 100
        leftLimit -= halfSize
        rightLimit += halfSize
        topLimit += halfSize
        bottomLimit -= halfSize
    }

    fun draw(canvas: Canvas) {
        iterate.forEach {
            it.first.drawNormal(canvas)
        }
    }

    private fun calcPositions(row: Int, col: Int): Pair<Float, Float> {
        var left = calcPosition(col) * equalizerOffset
        if (ceil(row * 0.5) % 2 == 1.0) {
            left -= size + margin
        }
        val top = calcPosition(row)
        return Pair(left, top)
    }

    private fun calcPosition(num: Int): Float {
        var pos = ceil(num * 0.5f)
        pos = (pos * (size * 2) + pos * margin)
        if (num % 2 == 0) {
            pos *= -1
        }
        return pos
    }

    fun getAppAtPoint(x: Float, y: Float, toIgnore: HashSet<App>? = null): App? {
        return iterate.find {
            (if (toIgnore != null) !toIgnore.contains(it.first) else true) && it.first.intersects(
                x,
                y
            )
        }?.first
    }

    fun prepare(offsetLeft: Float, offsetTop: Float, size: Float) {
        lastCircle = Circle(
            Vector2(
                -offsetLeft,
                -offsetTop
            ), ceil(size * 0.5).toFloat()
        )
        iterate.forEach {
            it.first.prepare(lastCircle!!)
        }
    }

    fun getLimit(x: Float, y: Float, size: Float): Pair<Float, Float> {
        val halfSize = (lastCircle?.r ?: size) * 0.75f
        var outX = x
        var outY = y
        if (x - halfSize < leftLimit) {
            outX = leftLimit + halfSize
        } else if (x + halfSize > rightLimit) {
            outX = rightLimit - halfSize
        }

        if (y + halfSize > topLimit) {
            outY = topLimit - halfSize
        } else if (y - halfSize < bottomLimit) {
            outY = bottomLimit + halfSize
        }
        return Pair(outX, outY)
    }
}