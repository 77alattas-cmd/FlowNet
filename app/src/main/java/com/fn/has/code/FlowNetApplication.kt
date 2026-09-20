package com.fn.has.code

import android.app.Application
import com.fn.has.code.data.local.db.FlowNetDatabase

class FlowNetApplication : Application() {

    val database by lazy { FlowNetDatabase.getDatabase(this) }

    override fun onCreate() {
        super.onCreate()
    }
}
