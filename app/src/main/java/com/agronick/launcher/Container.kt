package com.agronick.launcher

import android.graphics.Canvas
import util.geometry.Circle
import util.geometry.Vector2
import kotlin.math.ceil
import kotlin.math.floor
import kotlin.math.roundToInt
import kotlin.math.sqrt

class Container(val appList: AppListProvider, density: Float) {
    val appCircleSize = (StaticValues.normalAppSize * density).roundToInt()
    val appCircleMargin = (StaticValues.margin * density).roundToInt()
    private val flatAppList: List<App>
    var lastCircle: Circle? = null

    var topLimit = 0f
    var bottomLimit = 0f
    var leftLimit = 0f
    var rightLimit = 0f

    private var equalizerOffset = 1.1f

    init {/*
        Tries to set up a rough circle shape
        Rows are stored using the outer array index as y pos and inner array as x pos
        The middle array holds up to 2 items, one to be drawn at positive y and one at negative y
        rows = [
           [[row at y], [row at y index * -1]]
        ]
         */
        // Area of a circle to radius
        val appRadius = sqrt(appList.totalItems.toFloat() / Math.PI)
        val appDiam = appRadius * 2
        val appRadiusSquared = appRadius * appRadius
        val pkgIterator = appList.getPkgIterator()

        flatAppList = sequence {
            0.rangeTo(floor(appRadius).toInt()).forEach { row ->
                // Pythagorean theorem - row length at each level
                val rowDiam = (
                        if (row == 0) {
                            appDiam
                        } else {
                            (sqrt(appRadiusSquared - (row * row)) * 2) - 1
                        }
                        ).roundToInt()
                (0..kotlin.math.min(row, 1)).forEach { sect ->
                    (0..rowDiam).forEach { col ->
                        if (pkgIterator.hasMore()) {
                            val maybeNegativeRow = if (sect == 0) {
                                row
                            } else {
                                -row
                            }
                            yield(App(
                                pkgIterator.get(
                                    maybeNegativeRow, col
                                ) ?: PInfo.getBlank(), appCircleSize
                            ).apply {
                                assignedPos = Pair(maybeNegativeRow, col)
                            })
                        }
                    }
                }
            }
        }.toList()

        appList.save()

        position()
    }


    private fun position() {
        flatAppList.forEach { app ->
            val positions = calcPositions(app.assignedPos!!.first, app.assignedPos!!.second)
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
        flatAppList.filter {
            return@filter if (it.drawLast) {
                true
            } else {
                it.drawNormal(canvas)
                false
            }
        }.forEach {
            it.drawNormal(canvas)
        }
    }

    private fun calcPositions(row: Int, col: Int): Pair<Float, Float> {
        var left = calcPosition(col) * equalizerOffset
        if (kotlin.math.abs(row) % 2 == 1) {
            // Add offset for haxagon shape
            left -= appCircleSize + appCircleMargin
        }
        val top = (row * (appCircleSize * 2) + row * appCircleMargin).toFloat()
        return Pair(left, top)
    }

    private fun calcPosition(num: Int): Float {
        var pos = ceil(num * 0.5f)
        pos = pos * (appCircleSize * 2) + pos * appCircleMargin
        if (num % 2 == 0) {
            // Position right, the left of center
            pos *= -1
        }
        return pos
    }

    fun getAppAtPoint(point: Vector2, toIgnore: HashSet<App>? = null): App? {
        return flatAppList.find {
            (if (toIgnore != null) !toIgnore.contains(it) else true) && it.intersects(
                point
            )
        }
    }

    fun prepare(offsetLeft: Float, offsetTop: Float, size: Float) {
        lastCircle = Circle(
            Vector2(
                -offsetLeft, -offsetTop
            ), ceil(size * 0.5f)
        )
        flatAppList.forEach {
            it.prepare(lastCircle!!)
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