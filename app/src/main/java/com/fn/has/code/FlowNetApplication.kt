package com.fn.has.code

import android.app.Application
import com.fn.has.code.data.local.db.FlowNetDatabase
import dagger.hilt.android.HiltAndroidApp

@HiltAndroidApp
class FlowNetApplication : Application() {
    val database by lazy { FlowNetDatabase.getDatabase(this) }
}
