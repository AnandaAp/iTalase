package com.anlian.italase

import android.app.Application
import com.anlian.italase.shared.di.AndroidAppContext
import com.anlian.italase.shared.di.initKoin
import org.koin.android.ext.koin.androidContext

class ITalaseApplication: Application() {
    override fun onCreate() {
        super.onCreate()

        AndroidAppContext.ctx = this
        initKoin {
            androidContext(this@ITalaseApplication)
        }
    }
}