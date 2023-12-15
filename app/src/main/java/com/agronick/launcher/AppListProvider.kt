package com.agronick.launcher

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import org.json.JSONObject
import timber.log.Timber
import java.io.File
import java.io.FileReader
import java.io.FileWriter


class AppListProvider(appList: List<PInfo>, context: Context) {

    val positions = HashMap<Int, HashMap<Int, PInfo?>>().withDefault { HashMap() }
    val filePath = "${context.dataDir}/appPositions.json"
    val mapping = appList.map {
        it.asKey() to it
    }.toMap().toMutableMap()
    val unregisted = mapping.keys.toMutableList()
    val totalItems = appList.size

    fun load() {
        positions.clear()
        if (!File(filePath).exists()) {
            Timber.i("No file found. Will create first file shortly.")
            return
        }
        val file = FileReader(filePath)
        file.readText().let {
            val json = JSONObject(it)
            json.keys().forEach { rowKey ->
                val rowInt = rowKey.toInt()
                val row = json.getJSONObject(rowKey)
                val rowFinal = positions.getOrPut(
                    rowInt,
                ) {
                    HashMap()
                }
                row.keys().forEach { colId ->
                    val col = row.getString(colId)
                    unregisted.remove(col)
                    rowFinal[colId.toInt()] = mapping.getOrDefault(col, PInfo.getBlank())
                }
            }
        }
    }

    fun getPkgIterator(): PkgIterator {
        return PkgIterator(mapping, unregisted, positions)
    }

    class PkgIterator(
        val mapping: MutableMap<String, PInfo>,
        val unregistered: MutableList<String>,
        val positions: MutableMap<Int, HashMap<Int, PInfo?>>,
    ) {
        val touchedItems = mapping.keys.toMutableSet()

        fun get(x: Int, y: Int): PInfo? {
            val found = positions.getOrPut(x) {
                HashMap()
            }.getOrPut(y) {
                unregistered.removeLastOrNull()?.let {
                    mapping[it]
                }
            }
            Timber.d("Requesting app for $x $y got ${found?.appname}")
            return found?.also {
                touchedItems.remove(it.asKey())
            }
        }

        fun hasMore(): Boolean {
            return touchedItems.isNotEmpty()
        }
    }

    fun save() {
        GlobalScope.launch(Dispatchers.IO) {
            val jsonObject = JSONObject()
            positions.forEach {
                jsonObject.put(it.key.toString(), JSONObject().apply {
                    it.value.forEach {
                        put(it.key.toString(), it.value?.asKey())
                    }
                })
            }
            val file = FileWriter(filePath)
            file.write(jsonObject.toString())
            file.flush()
            file.close()
        }
    }

    fun swap(app1: App, app2: App) {
        (app1.assignedPos to app2.assignedPos).apply {
            app2.assignedPos = first
            app1.assignedPos = second
        }
        arrayOf(app1, app2).forEach {
            val toPlace = if (it.pkgInfo.isBlank()) null else it.pkgInfo
            positions[it.assignedPos!!.first]!![it.assignedPos!!.second] = toPlace
        }
    }
}

