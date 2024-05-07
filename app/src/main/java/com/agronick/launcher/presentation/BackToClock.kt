
package com.agronick.launcher.presentation

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity


class BackToClock : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {

        super.onCreate(savedInstanceState)
        val i = Intent(Intent.ACTION_MAIN)
        i.addCategory(Intent.CATEGORY_HOME)
        startActivity(i)

        finish()
    }
}

