package com.agronick.launcher

import android.app.Activity
import android.content.ComponentName
import android.content.Intent
import android.os.Bundle
import android.transition.Fade
import android.view.Window


var TAG = "main"

class MainActivity : Activity() {
    private lateinit var mainView: MainView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        with(window) {
            requestFeature(Window.FEATURE_ACTIVITY_TRANSITIONS)

            // set an exit transition
            exitTransition = Fade()
            enterTransition = Fade()
        }

        mainView = MainView(this.baseContext, getInstalledApps())
        if (savedInstanceState != null) {
            mainView.offsetLeft = savedInstanceState.getFloat("offsetLeft")
            mainView.offsetTop = savedInstanceState.getFloat("offsetTop")
        }

        setContentView(mainView)
        mainView.onPackageClick = this::onPackageClick
    }

    override fun onPause() {
        super.onPause()
        mainView.invalidate()
    }

    fun onPackageClick(pkg: PInfo) {
        val name = ComponentName(pkg.pname, pkg.activityName)
        val i = Intent(Intent.ACTION_MAIN)
        i.addCategory(Intent.CATEGORY_LAUNCHER)
        i.flags = Intent.FLAG_ACTIVITY_NEW_TASK or
                Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED
        i.component = name
        startActivity(i)
    }

    private fun getInstalledApps(): List<PInfo> {
        val mainIntent = Intent(Intent.ACTION_MAIN, null)
        mainIntent.addCategory(Intent.CATEGORY_LAUNCHER)
        return packageManager.queryIntentActivities(mainIntent, 0).mapNotNull {
            return@mapNotNull PInfo(
                appname = it.activityInfo.packageName,
                pname = it.activityInfo.packageName,
                icon = it.activityInfo.loadIcon(packageManager),
                activityName = it.activityInfo.name
            )
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        outState.putFloat("offsetLeft", mainView.offsetLeft)
        outState.putFloat("offsetTop", mainView.offsetTop)
        super.onSaveInstanceState(outState)
    }
}
