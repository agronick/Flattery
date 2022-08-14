package com.agronick.launcher

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager

object PreferenceManager {
    private var preferences =
        Launcher.appContext.getSharedPreferences("prefs", Context.MODE_PRIVATE)
    private var editor = preferences.edit()
    private var appOrder = preferences.getString("app_order", "")?.split(";")?.toTypedArray()!!
        .toCollection(ArrayList())
    private var sep = ";"

    fun getSetDiam(value: Double): Double {
        val cur = preferences.getFloat("max_diam", 0f)
        if (value > cur) {
            editor.putFloat("max_diam", value.toFloat())
            editor.apply()
            return value
        }
        return cur.toDouble()
    }

    fun getAppList(packageManager: PackageManager, intent: Intent): List<PInfo> {
        val asMapp = packageManager.queryIntentActivities(intent, 0).map {
            it.activityInfo.packageName to PInfo(
                appname = it.activityInfo.packageName,
                pname = it.activityInfo.packageName,
                icon = it.activityInfo.loadIcon(packageManager),
                activityName = it.activityInfo.name,
                order = null
            )
        }.toMap().toMutableMap()
        val prefList = preferences.getString("app_order", null) ?: return asMapp.values.toList()
        val blankPinfo = PInfo(
            appname = null,
            pname = null,
            icon = null,
            activityName = null,
            order = null
        )
        val matchedItems =
            prefList.split(sep).toTypedArray().map { asMapp.remove(it) ?: blankPinfo }
                .toMutableList()
        matchedItems.addAll(asMapp.values)
        matchedItems.mapIndexed { order, pInfo -> pInfo.order = order }
        return matchedItems
    }

    fun swap(pinfo1: PInfo, pinfo2: PInfo) {
        // Swap in array
        appOrder[pinfo1.order!!] = pinfo2.pname!!
        appOrder[pinfo2.order!!] = pinfo1.pname!!

        // Swap on objects
        val temp = pinfo1.order
        pinfo1.order = pinfo2.order
        pinfo2.order = temp

        // Create new list
        editor.putString("app_order", appOrder.joinToString(separator = sep))
        editor.apply()
    }
}