package com.agronick.launcher

import android.graphics.drawable.Drawable

class PInfo(
    var appname: String?,
    var pname: String?,
    var icon: Drawable?,
    var activityName: String?,
) {
    companion object {
        fun getBlank(): PInfo {
            return PInfo(
                appname = null,
                pname = null,
                icon = null,
                activityName = null
            )
        }
    }
    fun asKey(): String {
        return "${appname}:${activityName}"
    }

    fun isBlank(): Boolean {
        return appname == null
    }
}