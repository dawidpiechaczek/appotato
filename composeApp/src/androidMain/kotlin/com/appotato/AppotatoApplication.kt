package com.appotato

import android.app.Application
import com.appotato.di.setupKoin

class AppotatoApplication : Application() {

    override fun onCreate() {
        super.onCreate()
        setupKoin(this)
    }
}
