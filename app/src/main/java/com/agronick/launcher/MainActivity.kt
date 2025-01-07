package com.agronick.launcher

import StaticValues
import android.app.Activity
import android.content.ComponentName
import android.content.Intent
import android.os.Bundle
import android.transition.Fade
import android.util.Log
import android.view.GestureDetector
import android.view.MotionEvent
import android.view.Window
import androidx.core.view.GestureDetectorCompat
import com.google.android.wearable.input.RotaryEncoderHelper


class MainActivity : Activity(), GestureDetector.OnGestureListener {
    private var wasScrolling = false
    private lateinit var mDetector: GestureDetectorCompat
    private lateinit var mainView: MainView

    //Bezel scroll listener and conditional parameters for launcher zoom.
    override fun onGenericMotionEvent(event: MotionEvent): Boolean {

        if (event.action == MotionEvent.ACTION_SCROLL && RotaryEncoderHelper.isFromRotaryEncoder(event)) {

            val delta = -RotaryEncoderHelper.getRotaryAxisValue(event)
            Log.d("MainActivity", "Before modification: ${StaticValues.normalAppSize} ${StaticValues.margin}")

            //If bezel is turned clockwise, increase size of icons up to a size of 24.
            if (delta > 0 && StaticValues.normalAppSize <= 24) {
               StaticValues.normalAppSize++
            } else {
            //If bezel is turned counter-clockwise, decrease size of icons down to a size of 12.
                if (StaticValues.normalAppSize >= 12) {
                    StaticValues.normalAppSize--
                }
            }

            mainView.updateIconSize(StaticValues.normalAppSize)
            mainView.updateMarginSize(StaticValues.margin)

            Log.d("MainActivity", "After modification: ${StaticValues.normalAppSize} ${StaticValues.margin}")
            return true

        }
        return super.onGenericMotionEvent(event)

    }

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

        mDetector = GestureDetectorCompat(this, this)
        mDetector.setIsLongpressEnabled(true)
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        return if (mDetector.onTouchEvent(event)) {
            true
        } else if (mainView.reorderer != null) {
            mainView.handleLongPress(event)
            return true
        } else if (wasScrolling && event.action == MotionEvent.ACTION_UP) {
            mainView.checkOverPanLimit()
            wasScrolling = false
            return true
        } else {
            super.onTouchEvent(event)
        }
    }


    fun onPackageClick(pkg: PInfo) {
        if (pkg.pname != null && pkg.activityName != null) {
            val name = ComponentName(pkg.pname!!, pkg.activityName!!)
            val i = Intent(Intent.ACTION_MAIN)
            i.addCategory(Intent.CATEGORY_LAUNCHER)
            i.flags = Intent.FLAG_ACTIVITY_NEW_TASK or
                    Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED
            i.component = name
            startActivity(i)
        }
    }

    private fun getInstalledApps(): List<PInfo> {
        val mainIntent = Intent(Intent.ACTION_MAIN, null)
        mainIntent.addCategory(Intent.CATEGORY_LAUNCHER)
        return packageManager.queryIntentActivities(mainIntent, 0).mapNotNull {
         /* This is commented out because the line of code hides both the launcher and settings
         application. This bug would need to be fixed before uncommenting */

          //  if (it.activityInfo.packageName.startsWith("com.agronick.launcher")) {
          //      return@mapNotNull null
          //  }

            PInfo(
                appname = it.activityInfo.packageName,
                pname = it.activityInfo.packageName,
                icon = it.activityInfo.loadIcon(packageManager),
                activityName = it.activityInfo.name,
            )
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        outState.putFloat("offsetLeft", mainView.offsetLeft)
        outState.putFloat("offsetTop", mainView.offsetTop)
        super.onSaveInstanceState(outState)
    }

    override fun onPause() {
        super.onPause()
        mainView.openingApp = null
        mainView.allHidden = true
        mainView.invalidate()
    }

    override fun onResume() {
        super.onResume()
        if (mainView.allHidden) {
            mainView.allHidden = false
            mainView.invalidate()
        }
    }

    override fun onDown(e: MotionEvent): Boolean {
        return false
    }

    override fun onShowPress(e: MotionEvent) {

    }

    override fun onSingleTapUp(e: MotionEvent): Boolean {
        mainView.handleClick(e.x, e.y)
        return true
    }

    override fun onScroll(
        e1: MotionEvent?,
        e2: MotionEvent,
        distanceX: Float,
        distanceY: Float
    ): Boolean {
        mainView.offsetLeft -= distanceX
        mainView.offsetTop -= distanceY
        mainView.prepareInvalidate()
        wasScrolling = true
        return true
    }

    override fun onLongPress(e: MotionEvent) {
        mainView.handleLongPress(e)
    }

    override fun onFling(
        e1: MotionEvent?,
        e2: MotionEvent,
        velocityX: Float,
        velocityY: Float
    ): Boolean {
        return false
    }
}
