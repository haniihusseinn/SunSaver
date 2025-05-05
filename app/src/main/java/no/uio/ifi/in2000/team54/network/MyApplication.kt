package no.uio.ifi.in2000.team54.network

import android.content.Context
import android.app.Application


class MyApplication : Application() {

    override fun onCreate() {
        super.onCreate()
        appContext = applicationContext
    }

    companion object {
        lateinit var appContext: Context
    }

}